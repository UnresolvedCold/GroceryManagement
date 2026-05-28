package codes.shubham.grocerymanagement.data.db.dao

import androidx.room.*
import codes.shubham.grocerymanagement.data.db.model.ConsumptionSuggestionRow
import codes.shubham.grocerymanagement.data.db.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions WHERE product_id = :productId ORDER BY timestamp DESC")
    fun getTransactionsForProduct(productId: Long): Flow<List<TransactionEntity>>

    @Query("""
        SELECT
            p.id AS productId,
            p.name AS productName,
            p.unit AS unit,
            p.quantity AS currentQuantity,
            SUM(t.quantity) / :lookbackDays AS suggestedQuantity
        FROM transactions t
        INNER JOIN products p ON p.id = t.product_id
        WHERE t.type = :type
        AND t.timestamp >= :sinceTimestamp
        AND t.timestamp < :todayStartTimestamp
        AND p.quantity > 0
        AND NOT EXISTS (
            SELECT 1 FROM transactions today
            WHERE today.product_id = p.id
            AND today.type = :type
            AND today.timestamp >= :todayStartTimestamp
        )
        GROUP BY p.id, p.name, p.unit, p.quantity
        HAVING suggestedQuantity > 0
        ORDER BY suggestedQuantity DESC
    """)
    fun getConsumptionSuggestionRows(
        sinceTimestamp: Long,
        todayStartTimestamp: Long,
        type: String,
        lookbackDays: Double
    ): Flow<List<ConsumptionSuggestionRow>>

    @Query("""
        SELECT
            p.id AS productId,
            p.name AS productName,
            p.unit AS unit,
            p.quantity AS currentQuantity,
            SUM(t.quantity) / :lookbackDays AS suggestedQuantity
        FROM transactions t
        INNER JOIN products p ON p.id = t.product_id
        WHERE t.type = :type
        AND t.timestamp >= :sinceTimestamp
        AND t.timestamp < :todayStartTimestamp
        AND p.quantity > 0
        AND NOT EXISTS (
            SELECT 1 FROM transactions today
            WHERE today.product_id = p.id
            AND today.type = :type
            AND today.timestamp >= :todayStartTimestamp
        )
        GROUP BY p.id, p.name, p.unit, p.quantity
        HAVING suggestedQuantity > 0
        ORDER BY suggestedQuantity DESC
    """)
    suspend fun getConsumptionSuggestionRowsSnapshot(
        sinceTimestamp: Long,
        todayStartTimestamp: Long,
        type: String,
        lookbackDays: Double
    ): List<ConsumptionSuggestionRow>

    @Insert
    suspend fun insertTransaction(transaction: TransactionEntity): Long
}
