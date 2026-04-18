package codes.shubham.grocerymanagement.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val barcode: String? = null,
    val name: String,
    val brand: String? = null,
    val category: String = "Other",
    @ColumnInfo(name = "image_path")
    val imagePath: String? = null,
    val quantity: Double = 0.0,
    val unit: String = "pcs",
    @ColumnInfo(name = "low_quantity_threshold")
    val lowQuantityThreshold: Double = 2.0,
    @ColumnInfo(name = "expiry_date")
    val expiryDate: Long? = null,
    val notes: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
