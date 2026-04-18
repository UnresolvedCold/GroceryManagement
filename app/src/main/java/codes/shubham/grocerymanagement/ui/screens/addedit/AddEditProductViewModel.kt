package codes.shubham.grocerymanagement.ui.screens.addedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import codes.shubham.grocerymanagement.data.remote.ScanResultStore
import codes.shubham.grocerymanagement.data.repository.GroceryRepository
import codes.shubham.grocerymanagement.domain.model.Product
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

data class AddEditUiState(
    val id: Long = 0,
    val barcode: String = "",
    val name: String = "",
    val brand: String = "",
    val category: String = "Other",
    val imagePath: String = "",
    val quantity: String = "1",
    val unit: String = "pcs",
    val lowQuantityThreshold: String = "2",
    val expiryDate: LocalDate? = null,
    val notes: String = "",
    val isEditMode: Boolean = false,
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val nameError: String? = null
)

class AddEditProductViewModel(
    private val groceryRepository: GroceryRepository,
    private val scanResultStore: ScanResultStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditUiState())
    val uiState: StateFlow<AddEditUiState> = _uiState.asStateFlow()

    fun loadForNewProduct() {
        scanResultStore.consumeAndGet()?.let { pending ->
            _uiState.update {
                it.copy(
                    barcode = pending.barcode ?: "",
                    imagePath = pending.imagePath ?: ""
                )
            }
            pending.scanResult?.let { result ->
                _uiState.update {
                    it.copy(
                        name = result.name,
                        brand = result.brand ?: "",
                        category = result.category,
                        unit = result.unit,
                        expiryDate = result.estimatedExpiryDays?.let { d ->
                            LocalDate.now().plusDays(d.toLong())
                        }
                    )
                }
            }
        }
    }

    fun loadProduct(productId: Long) {
        viewModelScope.launch {
            groceryRepository.getProductById(productId).filterNotNull().first().let { p ->
                _uiState.update {
                    it.copy(
                        id = p.id,
                        barcode = p.barcode ?: "",
                        name = p.name,
                        brand = p.brand ?: "",
                        category = p.category,
                        imagePath = p.imagePath ?: "",
                        quantity = p.quantity.toString(),
                        unit = p.unit,
                        lowQuantityThreshold = p.lowQuantityThreshold.toString(),
                        expiryDate = p.expiryDate,
                        notes = p.notes ?: "",
                        isEditMode = true
                    )
                }
            }
        }
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value, nameError = null) }
    fun onBarcodeChange(value: String) = _uiState.update { it.copy(barcode = value) }
    fun onBrandChange(value: String) = _uiState.update { it.copy(brand = value) }
    fun onCategoryChange(value: String) = _uiState.update { it.copy(category = value) }
    fun onQuantityChange(value: String) = _uiState.update { it.copy(quantity = value) }
    fun onUnitChange(value: String) = _uiState.update { it.copy(unit = value) }
    fun onLowThresholdChange(value: String) = _uiState.update { it.copy(lowQuantityThreshold = value) }
    fun onExpiryDateChange(date: LocalDate?) = _uiState.update { it.copy(expiryDate = date) }
    fun onNotesChange(value: String) = _uiState.update { it.copy(notes = value) }
    fun onImagePathChange(value: String) = _uiState.update { it.copy(imagePath = value) }

    fun save() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.update { it.copy(nameError = "Product name is required") }
            return
        }
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val product = Product(
                id = state.id,
                barcode = state.barcode.takeIf { it.isNotBlank() },
                name = state.name.trim(),
                brand = state.brand.takeIf { it.isNotBlank() },
                category = state.category,
                imagePath = state.imagePath.takeIf { it.isNotBlank() },
                quantity = state.quantity.toDoubleOrNull() ?: 0.0,
                unit = state.unit,
                lowQuantityThreshold = state.lowQuantityThreshold.toDoubleOrNull() ?: 2.0,
                expiryDate = state.expiryDate,
                notes = state.notes.takeIf { it.isNotBlank() }
            )
            groceryRepository.upsertProduct(product)
            _uiState.update { it.copy(isSaving = false, savedSuccessfully = true) }
        }
    }
}
