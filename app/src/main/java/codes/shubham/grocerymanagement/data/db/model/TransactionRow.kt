package codes.shubham.grocerymanagement.data.db.model

data class TransactionRow(
    val id: Long,
    val productId: Long,
    val type: String,
    val quantity: Double,
    val timestamp: Long,
    val notes: String?,
    val recipeId: Long?,
    val recipeName: String?
)
