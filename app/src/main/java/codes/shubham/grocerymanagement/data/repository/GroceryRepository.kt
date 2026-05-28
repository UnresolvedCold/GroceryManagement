package codes.shubham.grocerymanagement.data.repository

import codes.shubham.grocerymanagement.data.db.dao.ProductDao
import codes.shubham.grocerymanagement.data.db.dao.TransactionDao
import codes.shubham.grocerymanagement.data.db.model.ConsumptionSuggestionRow
import codes.shubham.grocerymanagement.data.db.entity.ProductEntity
import codes.shubham.grocerymanagement.data.db.entity.TransactionEntity
import codes.shubham.grocerymanagement.domain.model.ConsumptionSuggestion
import codes.shubham.grocerymanagement.domain.model.Product
import codes.shubham.grocerymanagement.domain.model.Transaction
import codes.shubham.grocerymanagement.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId

class GroceryRepository(
    private val productDao: ProductDao,
    private val transactionDao: TransactionDao
) {
    fun getAllProducts(): Flow<List<Product>> =
        productDao.getAllProducts().map { list -> list.map(::entityToProduct) }

    fun getProductById(id: Long): Flow<Product?> =
        productDao.getProductById(id).map { it?.let(::entityToProduct) }

    suspend fun getProductByBarcode(barcode: String): Product? =
        productDao.getProductByBarcode(barcode)?.let(::entityToProduct)

    fun getLowStockProducts(): Flow<List<Product>> =
        productDao.getLowStockProducts().map { list -> list.map(::entityToProduct) }

    fun getExpiringSoonProducts(withinDays: Int): Flow<List<Product>> {
        val cutoffDay = LocalDate.now().plusDays(withinDays.toLong()).toEpochDay()
        return productDao.getExpiringSoonProducts(cutoffDay).map { list -> list.map(::entityToProduct) }
    }

    fun searchProducts(query: String): Flow<List<Product>> =
        productDao.searchProducts(query).map { list -> list.map(::entityToProduct) }

    fun getRegressiveConsumptionSuggestions(lookbackDays: Int): Flow<List<ConsumptionSuggestion>> {
        val (sinceTimestamp, todayStartTimestamp) = consumptionWindow(lookbackDays)
        return transactionDao.getConsumptionSuggestionRows(
            sinceTimestamp = sinceTimestamp,
            todayStartTimestamp = todayStartTimestamp,
            type = TransactionType.CONSUME.name,
            lookbackDays = lookbackDays.coerceAtLeast(1).toDouble()
        ).map { rows -> rows.mapNotNull(::rowToConsumptionSuggestion) }
    }

    suspend fun getRegressiveConsumptionSuggestionsSnapshot(lookbackDays: Int): List<ConsumptionSuggestion> {
        val (sinceTimestamp, todayStartTimestamp) = consumptionWindow(lookbackDays)
        return transactionDao.getConsumptionSuggestionRowsSnapshot(
            sinceTimestamp = sinceTimestamp,
            todayStartTimestamp = todayStartTimestamp,
            type = TransactionType.CONSUME.name,
            lookbackDays = lookbackDays.coerceAtLeast(1).toDouble()
        ).mapNotNull(::rowToConsumptionSuggestion)
    }

    suspend fun upsertProduct(product: Product): Long =
        productDao.upsertProduct(productToEntity(product))

    suspend fun deleteProduct(product: Product) =
        productDao.deleteProduct(productToEntity(product))

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
        quantity: Double
    ): Double =
        adjustQuantity(
            productId = productId,
            delta = -quantity,
            type = TransactionType.CONSUME,
            notes = "Regressive consumption suggestion"
        )

    fun getTransactionsForProduct(productId: Long): Flow<List<Transaction>> =
        transactionDao.getTransactionsForProduct(productId).map { list ->
            list.map { e ->
                Transaction(
                    id = e.id,
                    productId = e.productId,
                    type = TransactionType.valueOf(e.type),
                    quantity = e.quantity,
                    date = LocalDate.ofEpochDay(e.timestamp / 86_400_000),
                    notes = e.notes
                )
            }
        }

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
        val quantity = row.suggestedQuantity
            .coerceAtMost(row.currentQuantity)
            .roundToTwoDecimals()
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
        barcode = e.barcode,
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
        barcode = p.barcode,
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
}
