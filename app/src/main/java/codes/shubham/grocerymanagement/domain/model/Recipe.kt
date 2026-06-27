package codes.shubham.grocerymanagement.domain.model

data class Recipe(
    val id: Long = 0,
    val name: String,
    val notes: String? = null,
    val ingredients: List<RecipeIngredient> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val missingIngredients: List<RecipeIngredient>
        get() = ingredients.filter { it.missingQuantity > 0.0 }

    val canMake: Boolean
        get() = ingredients.isNotEmpty() && missingIngredients.isEmpty()
}

data class RecipeIngredient(
    val id: Long = 0,
    val productId: Long,
    val productName: String,
    val requiredQuantity: Double,
    val unit: String,
    val availableQuantity: Double
) {
    val missingQuantity: Double
        get() = (requiredQuantity - availableQuantity).coerceAtLeast(0.0)
}

data class RecipeIngredientInput(
    val productId: Long,
    val quantity: Double
)
