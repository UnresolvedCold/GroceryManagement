package codes.shubham.grocerymanagement.data.db.model

data class RecipeIngredientRow(
    val id: Long,
    val recipeId: Long,
    val productId: Long,
    val productName: String,
    val requiredQuantity: Double,
    val unit: String,
    val availableQuantity: Double
)
