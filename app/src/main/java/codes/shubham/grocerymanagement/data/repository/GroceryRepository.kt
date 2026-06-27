package codes.shubham.grocerymanagement.data.repository

import androidx.room.withTransaction
import codes.shubham.grocerymanagement.data.db.GroceryDatabase
import codes.shubham.grocerymanagement.data.db.dao.ProductDao
import codes.shubham.grocerymanagement.data.db.dao.RecipeDao
import codes.shubham.grocerymanagement.data.db.dao.TransactionDao
import codes.shubham.grocerymanagement.data.db.model.ConsumptionSuggestionRow
import codes.shubham.grocerymanagement.data.db.entity.ProductEntity
import codes.shubham.grocerymanagement.data.db.entity.RecipeEntity
import codes.shubham.grocerymanagement.data.db.entity.RecipeIngredientEntity
import codes.shubham.grocerymanagement.data.db.entity.TransactionEntity
import codes.shubham.grocerymanagement.domain.model.ConsumptionSuggestion
import codes.shubham.grocerymanagement.domain.model.Product
import codes.shubham.grocerymanagement.domain.model.Recipe
import codes.shubham.grocerymanagement.domain.model.RecipeIngredient
import codes.shubham.grocerymanagement.domain.model.RecipeIngredientInput
import codes.shubham.grocerymanagement.domain.model.Transaction
import codes.shubham.grocerymanagement.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId

class GroceryRepository(
    private val database: GroceryDatabase,
    private val productDao: ProductDao,
    private val recipeDao: RecipeDao,
    private val transactionDao: TransactionDao
) {
    fun getAllProducts(): Flow<List<Product>> =
        productDao.getAllProducts().map { list -> list.map(::entityToProduct) }

    fun getProductById(id: Long): Flow<Product?> =
        productDao.getProductById(id).map { it?.let(::entityToProduct) }

    suspend fun getProductByBarcode(barcode: String): Product? =
        barcode.trim()
            .takeIf { it.isNotEmpty() }
            ?.let { productDao.getProductByBarcode(it) }
            ?.let(::entityToProduct)

    fun getLowStockProducts(): Flow<List<Product>> =
        productDao.getLowStockProducts().map { list -> list.map(::entityToProduct) }

    fun getExpiringSoonProducts(withinDays: Int): Flow<List<Product>> {
        val cutoffDay = LocalDate.now().plusDays(withinDays.toLong()).toEpochDay()
        return productDao.getExpiringSoonProducts(cutoffDay).map { list -> list.map(::entityToProduct) }
    }

    fun searchProducts(query: String): Flow<List<Product>> =
        productDao.searchProducts(query).map { list -> list.map(::entityToProduct) }

    fun getRecipes(): Flow<List<Recipe>> =
        combine(recipeDao.getAllRecipes(), recipeDao.getAllIngredientRows()) { recipes, ingredients ->
            val ingredientsByRecipe = ingredients.groupBy { it.recipeId }
            recipes.map { recipe ->
                entityToRecipe(
                    recipe = recipe,
                    ingredients = ingredientsByRecipe[recipe.id].orEmpty().map { row ->
                        RecipeIngredient(
                            id = row.id,
                            productId = row.productId,
                            productName = row.productName,
                            requiredQuantity = row.requiredQuantity,
                            unit = row.unit,
                            availableQuantity = row.availableQuantity
                        )
                    }
                )
            }
        }

    fun getRecipeById(recipeId: Long): Flow<Recipe?> =
        combine(recipeDao.getRecipeById(recipeId), recipeDao.getIngredientRowsForRecipe(recipeId)) { recipe, ingredients ->
            recipe?.let {
                entityToRecipe(
                    recipe = it,
                    ingredients = ingredients.map { row ->
                        RecipeIngredient(
                            id = row.id,
                            productId = row.productId,
                            productName = row.productName,
                            requiredQuantity = row.requiredQuantity,
                            unit = row.unit,
                            availableQuantity = row.availableQuantity
                        )
                    }
                )
            }
        }

    fun getRegressiveConsumptionSuggestions(lookbackDays: Int): Flow<List<ConsumptionSuggestion>> {
        val (sinceTimestamp, todayStartTimestamp) = consumptionWindow(lookbackDays)
        return transactionDao.getConsumptionSuggestionRows(
            sinceTimestamp = sinceTimestamp,
            todayStartTimestamp = todayStartTimestamp,
            type = TransactionType.CONSUME.name
        ).map { rows -> rows.mapNotNull(::rowToConsumptionSuggestion) }
    }

    suspend fun getRegressiveConsumptionSuggestionsSnapshot(lookbackDays: Int): List<ConsumptionSuggestion> {
        val (sinceTimestamp, todayStartTimestamp) = consumptionWindow(lookbackDays)
        return transactionDao.getConsumptionSuggestionRowsSnapshot(
            sinceTimestamp = sinceTimestamp,
            todayStartTimestamp = todayStartTimestamp,
            type = TransactionType.CONSUME.name
        ).mapNotNull(::rowToConsumptionSuggestion)
    }

    suspend fun upsertProduct(product: Product): Long =
        productDao.upsertProduct(productToEntity(product))

    suspend fun deleteProduct(product: Product) =
        productDao.deleteProduct(productToEntity(product))

    suspend fun upsertRecipe(
        recipeId: Long?,
        name: String,
        notes: String?,
        ingredients: List<RecipeIngredientInput>
    ): Long = database.withTransaction {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return@withTransaction 0L

        val now = System.currentTimeMillis()
        val existingRecipe = recipeId
            ?.takeIf { it > 0L }
            ?.let { recipeDao.getRecipeByIdSnapshot(it) }

        val savedRecipeId = if (existingRecipe != null) {
            recipeDao.upsertRecipe(
                existingRecipe.copy(
                    name = cleanName,
                    notes = notes?.trim()?.takeIf { it.isNotBlank() },
                    updatedAt = now
                )
            )
            existingRecipe.id
        } else {
            recipeDao.upsertRecipe(
                RecipeEntity(
                    name = cleanName,
                    notes = notes?.trim()?.takeIf { it.isNotBlank() },
                    createdAt = now,
                    updatedAt = now
                )
            )
        }

        recipeDao.deleteIngredientsForRecipe(savedRecipeId)
        val ingredientEntities = ingredients
            .filter { it.quantity > 0.0 }
            .groupBy { it.productId }
            .map { (productId, rows) ->
                RecipeIngredientEntity(
                    recipeId = savedRecipeId,
                    productId = productId,
                    quantity = rows.sumOf { it.quantity }
                )
            }
        if (ingredientEntities.isNotEmpty()) {
            recipeDao.insertIngredients(ingredientEntities)
        }
        savedRecipeId
    }

    suspend fun deleteRecipe(recipe: Recipe) {
        recipeDao.deleteRecipe(
            RecipeEntity(
                id = recipe.id,
                name = recipe.name,
                notes = recipe.notes,
                createdAt = recipe.createdAt,
                updatedAt = recipe.updatedAt
            )
        )
    }

    suspend fun consumeRecipe(recipeId: Long): Boolean = database.withTransaction {
        val recipe = recipeDao.getRecipeByIdSnapshot(recipeId) ?: return@withTransaction false
        val ingredients = recipeDao.getIngredientRowsForRecipeSnapshot(recipeId)
        if (ingredients.isEmpty()) return@withTransaction false

        for (ingredient in ingredients) {
            val product = productDao.getProductByIdSnapshot(ingredient.productId)
                ?: return@withTransaction false
            if (product.quantity + 0.0001 < ingredient.requiredQuantity) {
                return@withTransaction false
            }
        }

        for (ingredient in ingredients) {
            val product = productDao.getProductByIdSnapshot(ingredient.productId)
                ?: return@withTransaction false
            productDao.updateQuantity(
                ingredient.productId,
                maxOf(0.0, product.quantity - ingredient.requiredQuantity)
            )
            transactionDao.insertTransaction(
                TransactionEntity(
                    productId = ingredient.productId,
                    type = TransactionType.CONSUME.name,
                    quantity = ingredient.requiredQuantity,
                    recipeId = recipe.id,
                    notes = "Recipe: ${recipe.name}"
                )
            )
        }
        true
    }

    suspend fun adjustQuantity(
        productId: Long,
        delta: Double,
        type: TransactionType,
        notes: String? = null
    ): Double {
        val entity = productDao.getProductByIdSnapshot(productId) ?: return 0.0
        val newQuantity = maxOf(0.0, entity.quantity + delta)
        productDao.updateQuantity(productId, newQuantity)
        transactionDao.insertTransaction(
            TransactionEntity(
                productId = productId,
                type = type.name,
                quantity = kotlin.math.abs(delta),
                notes = notes
            )
        )
        return newQuantity
    }

    suspend fun auditQuantity(
        productId: Long,
        countedQuantity: Double,
        notes: String? = null
    ): Double {
        productDao.getProductByIdSnapshot(productId) ?: return 0.0
        val newQuantity = maxOf(0.0, countedQuantity)
        productDao.updateQuantity(productId, newQuantity)
        transactionDao.insertTransaction(
            TransactionEntity(
                productId = productId,
                type = TransactionType.AUDIT.name,
                quantity = newQuantity,
                notes = notes
            )
        )
        return newQuantity
    }

    suspend fun applyRegressiveConsumptionSuggestion(
        productId: Long,
        quantity: Double,
        notes: String? = null
    ): Double =
        adjustQuantity(
            productId = productId,
            delta = -quantity,
            type = TransactionType.CONSUME,
            notes = notes ?: "Consumption suggestion"
        )

    suspend fun addConsumptionEntry(
        productId: Long,
        quantity: Double,
        date: LocalDate,
        notes: String? = null,
        recipeId: Long? = null
    ): Long = database.withTransaction {
        val product = productDao.getProductByIdSnapshot(productId) ?: return@withTransaction 0L
        productDao.updateQuantity(productId, maxOf(0.0, product.quantity - quantity))
        transactionDao.insertTransaction(
            TransactionEntity(
                productId = productId,
                type = TransactionType.CONSUME.name,
                quantity = quantity,
                timestamp = date.toEpochDay() * 86_400_000,
                notes = notes,
                recipeId = recipeId
            )
        )
    }

    suspend fun updateConsumptionEntry(
        transactionId: Long,
        quantity: Double,
        date: LocalDate,
        notes: String? = null
    ): Boolean = database.withTransaction {
        val transaction = transactionDao.getTransactionById(transactionId)
            ?.takeIf { it.type == TransactionType.CONSUME.name }
            ?: return@withTransaction false
        val product = productDao.getProductByIdSnapshot(transaction.productId)
            ?: return@withTransaction false

        productDao.updateQuantity(
            transaction.productId,
            maxOf(0.0, product.quantity + transaction.quantity - quantity)
        )
        transactionDao.updateConsumptionTransaction(
            transactionId = transactionId,
            quantity = quantity,
            timestamp = date.toEpochDay() * 86_400_000,
            notes = notes
        )
        true
    }

    suspend fun deleteConsumptionEntry(transactionId: Long): Boolean = database.withTransaction {
        val transaction = transactionDao.getTransactionById(transactionId)
            ?.takeIf { it.type == TransactionType.CONSUME.name }
            ?: return@withTransaction false
        val product = productDao.getProductByIdSnapshot(transaction.productId)
            ?: return@withTransaction false

        productDao.updateQuantity(transaction.productId, product.quantity + transaction.quantity)
        transactionDao.deleteTransaction(transactionId)
        true
    }

    fun getTransactionsForProduct(productId: Long): Flow<List<Transaction>> =
        transactionDao.getTransactionsForProduct(productId).map { list ->
            list.map { e ->
                Transaction(
                    id = e.id,
                    productId = e.productId,
                    type = TransactionType.valueOf(e.type),
                    quantity = e.quantity,
                    date = LocalDate.ofEpochDay(e.timestamp / 86_400_000),
                    notes = e.notes,
                    recipeId = e.recipeId,
                    recipeName = e.recipeName
                )
            }
        }

    private fun entityToRecipe(recipe: RecipeEntity, ingredients: List<RecipeIngredient>) = Recipe(
        id = recipe.id,
        name = recipe.name,
        notes = recipe.notes,
        ingredients = ingredients,
        createdAt = recipe.createdAt,
        updatedAt = recipe.updatedAt
    )

    private fun consumptionWindow(lookbackDays: Int): Pair<Long, Long> {
        val safeLookbackDays = lookbackDays.coerceAtLeast(1)
        val todayStart = LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val since = LocalDate.now()
            .minusDays(safeLookbackDays.toLong())
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        return since to todayStart
    }

    private fun rowToConsumptionSuggestion(row: ConsumptionSuggestionRow): ConsumptionSuggestion? {
        val quantity = row.suggestedQuantity.roundToTwoDecimals()
        if (quantity <= 0.0) return null
        return ConsumptionSuggestion(
            productId = row.productId,
            productName = row.productName,
            quantity = quantity,
            unit = row.unit,
            currentQuantity = row.currentQuantity
        )
    }

    private fun Double.roundToTwoDecimals(): Double =
        kotlin.math.round(this * 100.0) / 100.0

    private fun entityToProduct(e: ProductEntity) = Product(
        id = e.id,
        barcodes = parseBarcodes(e.barcode),
        name = e.name,
        brand = e.brand,
        category = e.category,
        imagePath = e.imagePath,
        quantity = e.quantity,
        unit = e.unit,
        lowQuantityThreshold = e.lowQuantityThreshold,
        expiryDate = e.expiryDate?.let { LocalDate.ofEpochDay(it) },
        notes = e.notes,
        createdAt = e.createdAt,
        updatedAt = e.updatedAt
    )

    private fun productToEntity(p: Product) = ProductEntity(
        id = p.id,
        barcode = encodeBarcodes(p.barcodes),
        name = p.name,
        brand = p.brand,
        category = p.category,
        imagePath = p.imagePath,
        quantity = p.quantity,
        unit = p.unit,
        lowQuantityThreshold = p.lowQuantityThreshold,
        expiryDate = p.expiryDate?.toEpochDay(),
        notes = p.notes,
        createdAt = p.createdAt,
        updatedAt = System.currentTimeMillis()
    )

    private fun parseBarcodes(raw: String?): List<String> =
        raw.orEmpty()
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .toList()

    private fun encodeBarcodes(barcodes: List<String>): String? =
        barcodes
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .joinToString(separator = "\n")
            .takeIf { it.isNotEmpty() }
}
