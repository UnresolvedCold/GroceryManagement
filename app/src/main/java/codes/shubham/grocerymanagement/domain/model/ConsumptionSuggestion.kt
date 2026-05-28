package codes.shubham.grocerymanagement.domain.model

data class ConsumptionSuggestion(
    val productId: Long,
    val productName: String,
    val quantity: Double,
    val unit: String,
    val currentQuantity: Double
)
