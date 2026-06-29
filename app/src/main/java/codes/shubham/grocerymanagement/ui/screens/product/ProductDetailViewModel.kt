package codes.shubham.grocerymanagement.ui.screens.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import codes.shubham.grocerymanagement.data.repository.GroceryRepository
import codes.shubham.grocerymanagement.domain.model.Product
import codes.shubham.grocerymanagement.domain.model.Transaction
import codes.shubham.grocerymanagement.domain.model.TransactionType
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ProductDetailUiState(
    val product: Product? = null,
    val transactions: List<Transaction> = emptyList(),
    val isLoading: Boolean = true,
    val showAddDialog: Boolean = false,
    val showConsumeDialog: Boolean = false,
    val editingConsumptionEntry: Transaction? = null,
    val deleted: Boolean = false
)

class ProductDetailViewModel(
    private val groceryRepository: GroceryRepository
) : ViewModel() {

    private val _productId = MutableStateFlow<Long?>(null)
    private val _showAddDialog = MutableStateFlow(false)
    private val _showConsumeDialog = MutableStateFlow(false)
    private val _editingConsumptionEntry = MutableStateFlow<Transaction?>(null)
    private val _deleted = MutableStateFlow(false)
    private val _messages = MutableSharedFlow<String>()
    val messages = _messages.asSharedFlow()

    private val dialogState = combine(
        _showAddDialog,
        _showConsumeDialog,
        _editingConsumptionEntry
    ) { addDialog, consumeDialog, editingConsumptionEntry ->
        Triple(addDialog, consumeDialog, editingConsumptionEntry)
    }

    val uiState: StateFlow<ProductDetailUiState> = combine(
        _productId.filterNotNull().flatMapLatest { id ->
            groceryRepository.getProductById(id)
        },
        _productId.filterNotNull().flatMapLatest { id ->
            groceryRepository.getTransactionsForProduct(id)
        },
        dialogState,
        _deleted
    ) { product, transactions, dialogs, deleted ->
        ProductDetailUiState(
            product = product,
            transactions = transactions,
            isLoading = false,
            showAddDialog = dialogs.first,
            showConsumeDialog = dialogs.second,
            editingConsumptionEntry = dialogs.third,
            deleted = deleted
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProductDetailUiState()
    )

    fun loadProduct(productId: Long) {
        _productId.value = productId
    }

    fun showAddDialog() = _showAddDialog.update { true }
    fun showConsumeDialog() = _showConsumeDialog.update { true }
    fun dismissDialogs() {
        _showAddDialog.value = false
        _showConsumeDialog.value = false
        _editingConsumptionEntry.value = null
    }

    fun addQuantity(amount: Double, notes: String?) {
        val id = _productId.value ?: return
        val unit = uiState.value.product?.unit.orEmpty()
        viewModelScope.launch {
            groceryRepository.adjustQuantity(id, amount, TransactionType.ADD, notes)
            _messages.emit("Added ${amount.formatQuantity()} $unit".trim())
        }
        dismissDialogs()
    }

    fun consumeQuantity(amount: Double, notes: String?, date: LocalDate = LocalDate.now()) {
        val id = _productId.value ?: return
        val unit = uiState.value.product?.unit.orEmpty()
        viewModelScope.launch {
            groceryRepository.addConsumptionEntry(id, amount, date, notes)
            _messages.emit("Consumed ${amount.formatQuantity()} $unit".trim())
        }
        dismissDialogs()
    }

    fun editConsumptionEntry(entry: Transaction) {
        if (entry.type == TransactionType.CONSUME) {
            _editingConsumptionEntry.value = entry
        }
    }

    fun updateConsumptionEntry(amount: Double, notes: String?, date: LocalDate) {
        val entry = _editingConsumptionEntry.value ?: return
        viewModelScope.launch {
            groceryRepository.updateConsumptionEntry(entry.id, amount, date, notes)
            _messages.emit("Consumed entry updated")
        }
        dismissDialogs()
    }

    fun deleteConsumptionEntry(entry: Transaction) {
        if (entry.type != TransactionType.CONSUME) return
        viewModelScope.launch {
            groceryRepository.deleteConsumptionEntry(entry.id)
            _messages.emit("Consumed entry deleted")
        }
    }

    fun deleteProduct() {
        viewModelScope.launch {
            uiState.value.product?.let {
                groceryRepository.deleteProduct(it)
                _deleted.value = true
            }
        }
    }

    private fun Double.formatQuantity(): String =
        if (this == toLong().toDouble()) toLong().toString() else "%.2f".format(this)
}
