package codes.shubham.grocerymanagement.ui.screens.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import codes.shubham.grocerymanagement.data.repository.GroceryRepository
import codes.shubham.grocerymanagement.domain.model.Product
import codes.shubham.grocerymanagement.domain.model.Transaction
import codes.shubham.grocerymanagement.domain.model.TransactionType
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ProductDetailUiState(
    val product: Product? = null,
    val transactions: List<Transaction> = emptyList(),
    val isLoading: Boolean = true,
    val showAddDialog: Boolean = false,
    val showConsumeDialog: Boolean = false,
    val deleted: Boolean = false
)

class ProductDetailViewModel(
    private val groceryRepository: GroceryRepository
) : ViewModel() {

    private val _productId = MutableStateFlow<Long?>(null)
    private val _showAddDialog = MutableStateFlow(false)
    private val _showConsumeDialog = MutableStateFlow(false)
    private val _deleted = MutableStateFlow(false)

    val uiState: StateFlow<ProductDetailUiState> = combine(
        _productId.filterNotNull().flatMapLatest { id ->
            groceryRepository.getProductById(id)
        },
        _productId.filterNotNull().flatMapLatest { id ->
            groceryRepository.getTransactionsForProduct(id)
        },
        _showAddDialog,
        _showConsumeDialog,
        _deleted
    ) { product, transactions, addDialog, consumeDialog, deleted ->
        ProductDetailUiState(
            product = product,
            transactions = transactions,
            isLoading = false,
            showAddDialog = addDialog,
            showConsumeDialog = consumeDialog,
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
    }

    fun addQuantity(amount: Double, notes: String?) {
        val id = _productId.value ?: return
        viewModelScope.launch {
            groceryRepository.adjustQuantity(id, amount, TransactionType.ADD, notes)
        }
        dismissDialogs()
    }

    fun consumeQuantity(amount: Double, notes: String?) {
        val id = _productId.value ?: return
        viewModelScope.launch {
            groceryRepository.adjustQuantity(id, -amount, TransactionType.CONSUME, notes)
        }
        dismissDialogs()
    }

    fun deleteProduct() {
        viewModelScope.launch {
            uiState.value.product?.let {
                groceryRepository.deleteProduct(it)
                _deleted.value = true
            }
        }
    }
}
