package codes.shubham.grocerymanagement.data.db.dao

import androidx.room.*
import codes.shubham.grocerymanagement.data.db.model.ConsumptionSuggestionRow
import codes.shubham.grocerymanagement.data.db.model.TransactionRow
import codes.shubham.grocerymanagement.data.db.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("""
        SELECT
            t.id AS id,
            t.product_id AS productId,
            t.type AS type,
            t.quantity AS quantity,
            t.timestamp AS timestamp,
            t.notes AS notes,
            t.recipe_id AS recipeId,
            r.name AS recipeName
        FROM transactions t
        LEFT JOIN recipes r ON r.id = t.recipe_id
        WHERE t.product_id = :productId
        ORDER BY t.timestamp DESC
    """)
    fun getTransactionsForProduct(productId: Long): Flow<List<TransactionRow>>

    @Query("SELECT * FROM transactions WHERE id = :transactionId")
    suspend fun getTransactionById(transactionId: Long): TransactionEntity?

    @Query("""
        SELECT
            p.id AS productId,
            p.name AS productName,
            p.unit AS unit,
            p.quantity AS currentQuantity,
            (
                SELECT candidate.quantity
                FROM transactions candidate
                WHERE candidate.product_id = p.id
                AND candidate.type = :type
                AND candidate.timestamp >= :sinceTimestamp
                AND candidate.timestamp < :todayStartTimestamp
                GROUP BY candidate.quantity
                ORDER BY COUNT(*) DESC, MAX(candidate.timestamp) DESC, candidate.quantity DESC
                LIMIT 1
            ) AS suggestedQuantity
        FROM products p
        WHERE p.quantity > 0
        AND EXISTS (
            SELECT 1 FROM transactions t
            WHERE t.product_id = p.id
            AND t.type = :type
            AND t.timestamp >= :sinceTimestamp
            AND t.timestamp < :todayStartTimestamp
        )
        AND NOT EXISTS (
            SELECT 1 FROM transactions today
            WHERE today.product_id = p.id
            AND today.type = :type
            AND today.timestamp >= :todayStartTimestamp
        )
        ORDER BY suggestedQuantity DESC
    """)
    fun getConsumptionSuggestionRows(
        sinceTimestamp: Long,
        todayStartTimestamp: Long,
        type: String
    ): Flow<List<ConsumptionSuggestionRow>>

    @Query("""
        SELECT
            p.id AS productId,
            p.name AS productName,
            p.unit AS unit,
            p.quantity AS currentQuantity,
            (
                SELECT candidate.quantity
                FROM transactions candidate
                WHERE candidate.product_id = p.id
                AND candidate.type = :type
                AND candidate.timestamp >= :sinceTimestamp
                AND candidate.timestamp < :todayStartTimestamp
                GROUP BY candidate.quantity
                ORDER BY COUNT(*) DESC, MAX(candidate.timestamp) DESC, candidate.quantity DESC
                LIMIT 1
            ) AS suggestedQuantity
        FROM products p
        WHERE p.quantity > 0
        AND EXISTS (
            SELECT 1 FROM transactions t
            WHERE t.product_id = p.id
            AND t.type = :type
            AND t.timestamp >= :sinceTimestamp
            AND t.timestamp < :todayStartTimestamp
        )
        AND NOT EXISTS (
            SELECT 1 FROM transactions today
            WHERE today.product_id = p.id
            AND today.type = :type
            AND today.timestamp >= :todayStartTimestamp
        )
        ORDER BY suggestedQuantity DESC
    """)
    suspend fun getConsumptionSuggestionRowsSnapshot(
        sinceTimestamp: Long,
        todayStartTimestamp: Long,
        type: String
    ): List<ConsumptionSuggestionRow>

    @Insert
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Query("""
        UPDATE transactions
        SET quantity = :quantity, timestamp = :timestamp, notes = :notes
        WHERE id = :transactionId
    """)
    suspend fun updateConsumptionTransaction(
        transactionId: Long,
        quantity: Double,
        timestamp: Long,
        notes: String?
    )

    @Query("DELETE FROM transactions WHERE id = :transactionId")
    suspend fun deleteTransaction(transactionId: Long)
}
