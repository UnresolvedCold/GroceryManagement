package codes.shubham.grocerymanagement.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import codes.shubham.grocerymanagement.data.preferences.UserPreferencesRepository
import codes.shubham.grocerymanagement.data.repository.GroceryRepository
import codes.shubham.grocerymanagement.domain.model.ConsumptionSuggestion
import codes.shubham.grocerymanagement.domain.model.Product
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SearchState(val query: String = "", val isActive: Boolean = false)

data class HomeUiState(
    val allProducts: List<Product> = emptyList(),
    val lowStockProducts: List<Product> = emptyList(),
    val expiringSoonProducts: List<Product> = emptyList(),
    val consumptionSuggestions: List<ConsumptionSuggestion> = emptyList(),
    val searchResults: List<Product> = emptyList(),
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val isLoading: Boolean = true
)

private data class HomeProductState(
    val allProducts: List<Product>,
    val lowStockProducts: List<Product>,
    val expiringSoonProducts: List<Product>,
    val consumptionSuggestions: List<ConsumptionSuggestion>
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class HomeViewModel(
    private val groceryRepository: GroceryRepository,
    private val prefsRepository: UserPreferencesRepository
) : ViewModel() {

    private val _searchState = MutableStateFlow(SearchState())
    private val _messages = MutableSharedFlow<String>()
    val messages = _messages.asSharedFlow()

    private val productState = combine(
        groceryRepository.getAllProducts(),
        groceryRepository.getLowStockProducts(),
        prefsRepository.userPreferences.flatMapLatest { prefs ->
            groceryRepository.getExpiringSoonProducts(prefs.expiryWarningDays)
        },
        prefsRepository.userPreferences.flatMapLatest { prefs ->
            if (!prefs.regressiveConsumptionEnabled) {
                flowOf(emptyList())
            } else {
                groceryRepository.getRegressiveConsumptionSuggestions(prefs.regressiveConsumptionLookbackDays)
            }
        },
        prefsRepository.dismissedConsumptionSuggestionIds
    ) { all, lowStock, expiring, suggestions, dismissedSuggestionIds ->
        HomeProductState(
            allProducts = all,
            lowStockProducts = lowStock,
            expiringSoonProducts = expiring,
            consumptionSuggestions = suggestions.filterNot { it.productId in dismissedSuggestionIds }
        )
    }

    val uiState: StateFlow<HomeUiState> = combine(
        productState,
        _searchState.debounce(300).flatMapLatest { ss ->
            if (!ss.isActive || ss.query.isBlank()) flowOf(emptyList())
            else groceryRepository.searchProducts(ss.query)
        },
        _searchState
    ) { products, searchResults, searchState ->
        HomeUiState(
            allProducts = products.allProducts,
            lowStockProducts = products.lowStockProducts,
            expiringSoonProducts = products.expiringSoonProducts,
            consumptionSuggestions = products.consumptionSuggestions,
            searchResults = searchResults,
            searchQuery = searchState.query,
            isSearchActive = searchState.isActive,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun onSearchQueryChange(query: String) {
        _searchState.update { it.copy(query = query) }
    }

    fun setSearchActive(active: Boolean) {
        _searchState.update { it.copy(isActive = active, query = if (!active) "" else it.query) }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch { groceryRepository.deleteProduct(product) }
    }

    fun applyConsumptionSuggestion(
        suggestion: ConsumptionSuggestion,
        quantity: Double = suggestion.quantity,
        notes: String? = null
    ) {
        viewModelScope.launch {
            groceryRepository.applyRegressiveConsumptionSuggestion(
                productId = suggestion.productId,
                quantity = quantity,
                notes = notes
            )
            _messages.emit("Suggestion applied")
        }
    }

    fun applyAllConsumptionSuggestions() {
        val suggestions = uiState.value.consumptionSuggestions
        viewModelScope.launch {
            suggestions.forEach { suggestion ->
                groceryRepository.applyRegressiveConsumptionSuggestion(
                    productId = suggestion.productId,
                    quantity = suggestion.quantity
                )
            }
            _messages.emit("Applied ${suggestions.size} suggestion(s)")
        }
    }

    fun dismissConsumptionSuggestion(suggestion: ConsumptionSuggestion) {
        viewModelScope.launch {
            prefsRepository.dismissConsumptionSuggestion(suggestion.productId)
            _messages.emit("Recommendation removed for today")
        }
    }

    fun showExternalMessage(message: String) {
        viewModelScope.launch {
            _messages.emit(message)
        }
    }
}
