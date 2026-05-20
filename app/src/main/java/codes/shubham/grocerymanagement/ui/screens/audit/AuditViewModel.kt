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
    val notes: String = ""
)

data class AuditItemUiState(
    val product: Product,
    val countedQuantity: String,
    val notes: String,
    val hasChanges: Boolean,
    val errorMessage: String? = null
)

data class AuditUiState(
    val items: List<AuditItemUiState> = emptyList(),
    val isSaving: Boolean = false,
    val hasPendingChanges: Boolean = false,
    val feedbackMessage: String? = null
)

class AuditViewModel(
    private val groceryRepository: GroceryRepository
) : ViewModel() {

    private val _drafts = MutableStateFlow<Map<Long, AuditDraft>>(emptyMap())
    private val _isSaving = MutableStateFlow(false)
    private val _feedbackMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<AuditUiState> = combine(
        groceryRepository.getAllProducts(),
        _drafts,
        _isSaving,
        _feedbackMessage
    ) { products, drafts, isSaving, feedbackMessage ->
        val sortedProducts = products.sortedBy { it.name.lowercase() }
        val items = sortedProducts.map { product ->
            val draft = drafts[product.id]
            val countedQuantity = draft?.countedQuantity ?: formatQuantity(product.quantity)
            val notes = draft?.notes.orEmpty()
            val parsedQuantity = countedQuantity.toDoubleOrNull()
            val hasChanges = draft != null && (
                parsedQuantity == null ||
                    parsedQuantity != product.quantity ||
                    notes.isNotBlank()
                )

            AuditItemUiState(
                product = product,
                countedQuantity = countedQuantity,
                notes = notes,
                hasChanges = hasChanges,
                errorMessage = if (draft != null && countedQuantity.isNotBlank() && parsedQuantity == null) {
                    "Enter a valid quantity"
                } else {
                    null
                }
            )
        }

        AuditUiState(
            items = items,
            isSaving = isSaving,
            hasPendingChanges = items.any { it.hasChanges },
            feedbackMessage = feedbackMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AuditUiState()
    )

    fun updateCountedQuantity(product: Product, value: String) {
        _drafts.update { drafts ->
            drafts + (product.id to AuditDraft(
                countedQuantity = value,
                notes = drafts[product.id]?.notes.orEmpty()
            ))
        }
    }

    fun updateNotes(product: Product, value: String) {
        _drafts.update { drafts ->
            drafts + (product.id to AuditDraft(
                countedQuantity = drafts[product.id]?.countedQuantity ?: formatQuantity(product.quantity),
                notes = value
            ))
        }
    }

    fun resetDraft(productId: Long) {
        _drafts.update { drafts -> drafts - productId }
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    fun saveAudit() {
        viewModelScope.launch {
            val products = groceryRepository.getAllProducts().first()
            val drafts = _drafts.value
            if (drafts.isEmpty()) {
                _feedbackMessage.value = "No audit changes to save."
                return@launch
            }

            val invalidProductIds = drafts
                .filterValues { it.countedQuantity.toDoubleOrNull() == null }
                .keys
            if (invalidProductIds.isNotEmpty()) {
                _feedbackMessage.value = "Fix invalid quantities before saving."
                return@launch
            }

            _isSaving.value = true
            var savedCount = 0

            drafts.forEach { (productId, draft) ->
                val product = products.firstOrNull { it.id == productId } ?: return@forEach
                val countedQuantity = draft.countedQuantity.toDouble()
                val notes = draft.notes.trim().takeIf { it.isNotBlank() }
                val shouldSave = countedQuantity != product.quantity || notes != null
                if (!shouldSave) return@forEach

                groceryRepository.auditQuantity(
                    productId = productId,
                    countedQuantity = countedQuantity,
                    notes = notes
                )
                savedCount++
            }

            _drafts.value = emptyMap()
            _isSaving.value = false
            _feedbackMessage.value = if (savedCount == 0) {
                "No audit changes to save."
            } else {
                "Saved audit for $savedCount item(s)."
            }
        }
    }

    private fun formatQuantity(quantity: Double): String =
        if (quantity == quantity.toLong().toDouble()) {
            quantity.toLong().toString()
        } else {
            "%.2f".format(quantity)
        }
}
