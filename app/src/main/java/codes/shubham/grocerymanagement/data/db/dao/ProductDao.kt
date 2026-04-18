package codes.shubham.grocerymanagement.data.db.dao

import androidx.room.*
import codes.shubham.grocerymanagement.data.db.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Query("SELECT * FROM products ORDER BY updated_at DESC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id")
    fun getProductById(id: Long): Flow<ProductEntity?>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductByIdSnapshot(id: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): ProductEntity?

    @Query("""
        SELECT * FROM products
        WHERE quantity <= low_quantity_threshold
        ORDER BY quantity ASC
    """)
    fun getLowStockProducts(): Flow<List<ProductEntity>>

    @Query("""
        SELECT * FROM products
        WHERE expiry_date IS NOT NULL
        AND expiry_date <= :cutoffEpochDay
        ORDER BY expiry_date ASC
    """)
    fun getExpiringSoonProducts(cutoffEpochDay: Long): Flow<List<ProductEntity>>

    @Query("""
        SELECT * FROM products
        WHERE name LIKE '%' || :query || '%'
        OR brand LIKE '%' || :query || '%'
        ORDER BY name ASC
    """)
    fun searchProducts(query: String): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProduct(product: ProductEntity): Long

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Delete
    suspend fun deleteProduct(product: ProductEntity)

    @Query("UPDATE products SET quantity = :quantity, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateQuantity(
        id: Long,
        quantity: Double,
        updatedAt: Long = System.currentTimeMillis()
    )
}
