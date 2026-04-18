package codes.shubham.grocerymanagement.domain.model

import java.time.LocalDate

data class Product(
    val id: Long = 0,
    val barcode: String? = null,
    val name: String,
    val brand: String? = null,
    val category: String = "Other",
    val imagePath: String? = null,
    val quantity: Double = 0.0,
    val unit: String = "pcs",
    val lowQuantityThreshold: Double = 2.0,
    val expiryDate: LocalDate? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

val PRODUCT_CATEGORIES = listOf(
    "Dairy", "Produce", "Bakery", "Beverages",
    "Snacks", "Frozen", "Canned", "Meat",
    "Seafood", "Condiments", "Grains", "Other"
)

val QUANTITY_UNITS = listOf("pcs", "kg", "g", "L", "ml", "pack", "box", "bottle", "can")
