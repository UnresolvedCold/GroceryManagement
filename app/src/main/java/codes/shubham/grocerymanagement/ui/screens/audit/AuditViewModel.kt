package codes.shubham.grocerymanagement.ui.screens.audit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import codes.shubham.grocerymanagement.data.repository.GroceryRepository
import codes.shubham.grocerymanagement.domain.model.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuditDraft(
    val countedQuantity: String,
    val notes: String = "",
    val isChecked: Boolean = false,
    val isSkipped: Boolean = false
)

data class AuditItemUiState(
    val product: Product,
    val countedQuantity: String,
    val notes: String,
    val isChecked: Boolean,
    val isSkipped: Boolean,
    val hasQuantityChange: Boolean,
    val hasChanges: Boolean,
    val errorMessage: String? = null
)

data class AuditUiState(
    val items: List<AuditItemUiState> = emptyList(),
    val currentIndex: Int = 0,
    val currentItem: AuditItemUiState? = null,
    val isReviewing: Boolean = false,
    val isSaving: Boolean = false,
    val hasAuditedItems: Boolean = false,
    val checkedCount: Int = 0,
    val skippedCount: Int = 0,
    val changedCount: Int = 0,
    val invalidCount: Int = 0,
    val progress: Float = 0f,
    val feedbackMessage: String? = null
)

private data class AuditItemsState(
    val items: List<AuditItemUiState>
)

class AuditViewModel(
    private val groceryRepository: GroceryRepository
) : ViewModel() {

    private val _drafts = MutableStateFlow<Map<Long, AuditDraft>>(emptyMap())
    private val _currentIndex = MutableStateFlow(0)
    private val _isReviewing = MutableStateFlow(false)
    private val _isSaving = MutableStateFlow(false)
    private val _feedbackMessage = MutableStateFlow<String?>(null)

    private val auditItemsState = combine(
        groceryRepository.getAllProducts(),
        _drafts
    ) { products, drafts ->
        AuditItemsState(items = buildAuditItems(products, drafts))
    }

    val uiState: StateFlow<AuditUiState> = combine(
        auditItemsState,
        _currentIndex,
        _isReviewing,
        _isSaving,
        _feedbackMessage
    ) { auditItems, currentIndex, isReviewing, isSaving, feedbackMessage ->
        val items = auditItems.items
        val boundedIndex = currentIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
        val checkedCount = items.count { it.isChecked }
        val skippedCount = items.count { it.isSkipped }
        val completedCount = checkedCount + skippedCount

        AuditUiState(
            items = items,
            currentIndex = boundedIndex,
            currentItem = items.getOrNull(boundedIndex),
            isReviewing = isReviewing,
            isSaving = isSaving,
            hasAuditedItems = checkedCount > 0,
            checkedCount = checkedCount,
            skippedCount = skippedCount,
            changedCount = items.count { it.hasChanges },
            invalidCount = items.count { it.errorMessage != null },
            progress = if (items.isEmpty()) 0f else completedCount.toFloat() / items.size.toFloat(),
            feedbackMessage = feedbackMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
            initialValue = AuditUiState()
    )

    private fun buildAuditItems(
        products: List<Product>,
        drafts: Map<Long, AuditDraft>
    ): List<AuditItemUiState> =
        products.sortedBy { it.name.lowercase() }.map { product ->
            val draft = drafts[product.id]
            val countedQuantity = draft?.countedQuantity ?: formatQuantity(product.quantity)
            val notes = draft?.notes.orEmpty()
            val parsedQuantity = countedQuantity.toDoubleOrNull()
            val isChecked = draft?.isChecked == true
            val isSkipped = draft?.isSkipped == true
            val hasQuantityChange = parsedQuantity != null && parsedQuantity != product.quantity
            val hasChanges = isChecked && (hasQuantityChange || notes.isNotBlank())

            AuditItemUiState(
                product = product,
                countedQuantity = countedQuantity,
                notes = notes,
                isChecked = isChecked,
                isSkipped = isSkipped,
                hasQuantityChange = hasQuantityChange,
                hasChanges = hasChanges,
                errorMessage = if (isChecked && parsedQuantity == null) {
                    "Enter a valid quantity"
                } else {
                    null
                }
            )
        }

    fun updateCountedQuantity(product: Product, value: String) {
        _drafts.update { drafts ->
            val existing = drafts[product.id]
            drafts + (product.id to AuditDraft(
                countedQuantity = value,
                notes = existing?.notes.orEmpty(),
                isChecked = true,
                isSkipped = false
            ))
        }
    }

    fun updateNotes(product: Product, value: String) {
        _drafts.update { drafts ->
            val existing = drafts[product.id]
            drafts + (product.id to AuditDraft(
                countedQuantity = existing?.countedQuantity ?: formatQuantity(product.quantity),
                notes = value,
                isChecked = true,
                isSkipped = false
            ))
        }
    }

    fun markSame(product: Product) {
        setDraft(product, formatQuantity(product.quantity), isChecked = true, isSkipped = false)
        goNext()
    }

    fun markEmpty(product: Product) {
        setDraft(product, "0", isChecked = true, isSkipped = false)
        goNext()
    }

    fun skipProduct(productId: Long) {
        _drafts.update { drafts ->
            val existing = drafts[productId]
            drafts + (productId to AuditDraft(
                countedQuantity = existing?.countedQuantity.orEmpty(),
                notes = existing?.notes.orEmpty(),
                isChecked = false,
                isSkipped = true
            ))
        }
        goNext()
    }

    fun adjustQuantity(product: Product, delta: Double) {
        val currentValue = _drafts.value[product.id]?.countedQuantity
            ?: formatQuantity(product.quantity)
        val adjusted = maxOf(0.0, (currentValue.toDoubleOrNull() ?: product.quantity) + delta)
        setDraft(product, formatQuantity(adjusted), isChecked = true, isSkipped = false)
    }

    fun resetDraft(productId: Long) {
        _drafts.update { drafts -> drafts - productId }
    }

    fun goNext() {
        val lastIndex = (uiState.value.items.size - 1).coerceAtLeast(0)
        _currentIndex.update { (it + 1).coerceAtMost(lastIndex) }
    }

    fun goPrevious() {
        _currentIndex.update { (it - 1).coerceAtLeast(0) }
    }

    fun showReview() {
        _isReviewing.value = true
    }

    fun returnToAudit() {
        _isReviewing.value = false
    }

    fun editProduct(productId: Long) {
        val index = uiState.value.items.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            _currentIndex.value = index
        }
        _isReviewing.value = false
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    fun saveAudit() {
        viewModelScope.launch {
            val products = groceryRepository.getAllProducts().first()
            val drafts = _drafts.value
            val checkedDrafts = drafts.filterValues { it.isChecked && !it.isSkipped }
            if (checkedDrafts.isEmpty()) {
                _feedbackMessage.value = "No audited items to save."
                return@launch
            }

            val invalidProductIds = checkedDrafts
                .filterValues { it.countedQuantity.toDoubleOrNull() == null }
                .keys
            if (invalidProductIds.isNotEmpty()) {
                _feedbackMessage.value = "Fix invalid quantities before saving."
                return@launch
            }

            _isSaving.value = true
            var savedCount = 0

            checkedDrafts.forEach { (productId, draft) ->
                val product = products.firstOrNull { it.id == productId } ?: return@forEach
                val countedQuantity = draft.countedQuantity.toDouble()
                val notes = draft.notes.trim().takeIf { it.isNotBlank() }

                groceryRepository.auditQuantity(
                    productId = productId,
                    countedQuantity = countedQuantity,
                    notes = notes
                )
                savedCount++
            }

            _drafts.value = emptyMap()
            _currentIndex.value = 0
            _isReviewing.value = false
            _isSaving.value = false
            _feedbackMessage.value = "Saved audit for $savedCount item(s)."
        }
    }

    private fun setDraft(
        product: Product,
        countedQuantity: String,
        isChecked: Boolean,
        isSkipped: Boolean
    ) {
        _drafts.update { drafts ->
            val existing = drafts[product.id]
            drafts + (product.id to AuditDraft(
                countedQuantity = countedQuantity,
                notes = existing?.notes.orEmpty(),
                isChecked = isChecked,
                isSkipped = isSkipped
            ))
        }
    }

    private fun formatQuantity(quantity: Double): String =
        if (quantity == quantity.toLong().toDouble()) {
            quantity.toLong().toString()
        } else {
            "%.2f".format(quantity)
        }
}
