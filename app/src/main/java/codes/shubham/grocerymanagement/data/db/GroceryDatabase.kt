package codes.shubham.grocerymanagement.data.db

import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import codes.shubham.grocerymanagement.data.db.dao.ProductDao
import codes.shubham.grocerymanagement.data.db.dao.RecipeDao
import codes.shubham.grocerymanagement.data.db.dao.TransactionDao
import codes.shubham.grocerymanagement.data.db.entity.ProductEntity
import codes.shubham.grocerymanagement.data.db.entity.RecipeEntity
import codes.shubham.grocerymanagement.data.db.entity.RecipeIngredientEntity
import codes.shubham.grocerymanagement.data.db.entity.TransactionEntity

@Database(
    entities = [ProductEntity::class, TransactionEntity::class, RecipeEntity::class, RecipeIngredientEntity::class],
    version = 2,
    exportSchema = false
)
abstract class GroceryDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun transactionDao(): TransactionDao
    abstract fun recipeDao(): RecipeDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `recipes` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `notes` TEXT,
                        `created_at` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `recipe_ingredients` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `recipe_id` INTEGER NOT NULL,
                        `product_id` INTEGER NOT NULL,
                        `quantity` REAL NOT NULL,
                        FOREIGN KEY(`recipe_id`) REFERENCES `recipes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`product_id`) REFERENCES `products`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_recipe_ingredients_recipe_id` ON `recipe_ingredients` (`recipe_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_recipe_ingredients_product_id` ON `recipe_ingredients` (`product_id`)")
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `recipe_id` INTEGER")
            }
        }
    }
}
