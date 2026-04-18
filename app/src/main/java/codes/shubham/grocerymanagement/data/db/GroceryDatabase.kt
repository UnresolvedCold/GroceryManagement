package codes.shubham.grocerymanagement.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import codes.shubham.grocerymanagement.data.db.dao.ProductDao
import codes.shubham.grocerymanagement.data.db.dao.TransactionDao
import codes.shubham.grocerymanagement.data.db.entity.ProductEntity
import codes.shubham.grocerymanagement.data.db.entity.TransactionEntity

@Database(
    entities = [ProductEntity::class, TransactionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class GroceryDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun transactionDao(): TransactionDao
}
