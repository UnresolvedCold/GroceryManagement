package codes.shubham.grocerymanagement.domain.model

import java.time.LocalDate

enum class TransactionType { ADD, CONSUME }

data class Transaction(
    val id: Long = 0,
    val productId: Long,
    val type: TransactionType,
    val quantity: Double,
    val date: LocalDate = LocalDate.now(),
    val notes: String? = null
)
