package codes.shubham.grocerymanagement.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import codes.shubham.grocerymanagement.data.preferences.UserPreferencesRepository
import codes.shubham.grocerymanagement.data.repository.GroceryRepository
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
    val searchResults: List<Product> = emptyList(),
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class HomeViewModel(
    private val groceryRepository: GroceryRepository,
    private val prefsRepository: UserPreferencesRepository
) : ViewModel() {

    private val _searchState = MutableStateFlow(SearchState())

    val uiState: StateFlow<HomeUiState> = combine(
        groceryRepository.getAllProducts(),
        groceryRepository.getLowStockProducts(),
        prefsRepository.userPreferences.flatMapLatest { prefs ->
            groceryRepository.getExpiringSoonProducts(prefs.expiryWarningDays)
        },
        _searchState.debounce(300).flatMapLatest { ss ->
            if (!ss.isActive || ss.query.isBlank()) flowOf(emptyList())
            else groceryRepository.searchProducts(ss.query)
        },
        _searchState
    ) { all, lowStock, expiring, searchResults, searchState ->
        HomeUiState(
            allProducts = all,
            lowStockProducts = lowStock,
            expiringSoonProducts = expiring,
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
}
