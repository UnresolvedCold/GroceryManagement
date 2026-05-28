package codes.shubham.grocerymanagement.data.db.model

data class ConsumptionSuggestionRow(
    val productId: Long,
    val productName: String,
    val unit: String,
    val currentQuantity: Double,
    val suggestedQuantity: Double
)
