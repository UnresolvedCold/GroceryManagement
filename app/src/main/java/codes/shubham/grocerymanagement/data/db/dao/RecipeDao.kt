package codes.shubham.grocerymanagement.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import codes.shubham.grocerymanagement.data.db.entity.RecipeEntity
import codes.shubham.grocerymanagement.data.db.entity.RecipeIngredientEntity
import codes.shubham.grocerymanagement.data.db.model.RecipeIngredientRow
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {
    @Query("SELECT * FROM recipes ORDER BY name ASC")
    fun getAllRecipes(): Flow<List<RecipeEntity>>

    @Query("SELECT * FROM recipes WHERE id = :recipeId")
    fun getRecipeById(recipeId: Long): Flow<RecipeEntity?>

    @Query("SELECT * FROM recipes WHERE id = :recipeId")
    suspend fun getRecipeByIdSnapshot(recipeId: Long): RecipeEntity?

    @Query("""
        SELECT
            ri.id AS id,
            ri.recipe_id AS recipeId,
            ri.product_id AS productId,
            p.name AS productName,
            ri.quantity AS requiredQuantity,
            p.unit AS unit,
            p.quantity AS availableQuantity
        FROM recipe_ingredients ri
        INNER JOIN products p ON p.id = ri.product_id
        ORDER BY p.name ASC
    """)
    fun getAllIngredientRows(): Flow<List<RecipeIngredientRow>>

    @Query("""
        SELECT
            ri.id AS id,
            ri.recipe_id AS recipeId,
            ri.product_id AS productId,
            p.name AS productName,
            ri.quantity AS requiredQuantity,
            p.unit AS unit,
            p.quantity AS availableQuantity
        FROM recipe_ingredients ri
        INNER JOIN products p ON p.id = ri.product_id
        WHERE ri.recipe_id = :recipeId
        ORDER BY p.name ASC
    """)
    fun getIngredientRowsForRecipe(recipeId: Long): Flow<List<RecipeIngredientRow>>

    @Query("""
        SELECT
            ri.id AS id,
            ri.recipe_id AS recipeId,
            ri.product_id AS productId,
            p.name AS productName,
            ri.quantity AS requiredQuantity,
            p.unit AS unit,
            p.quantity AS availableQuantity
        FROM recipe_ingredients ri
        INNER JOIN products p ON p.id = ri.product_id
        WHERE ri.recipe_id = :recipeId
        ORDER BY p.name ASC
    """)
    suspend fun getIngredientRowsForRecipeSnapshot(recipeId: Long): List<RecipeIngredientRow>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecipe(recipe: RecipeEntity): Long

    @Update
    suspend fun updateRecipe(recipe: RecipeEntity)

    @Delete
    suspend fun deleteRecipe(recipe: RecipeEntity)

    @Query("DELETE FROM recipe_ingredients WHERE recipe_id = :recipeId")
    suspend fun deleteIngredientsForRecipe(recipeId: Long)

    @Insert
    suspend fun insertIngredients(ingredients: List<RecipeIngredientEntity>)
}
