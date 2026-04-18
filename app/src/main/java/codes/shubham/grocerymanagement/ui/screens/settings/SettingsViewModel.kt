package codes.shubham.grocerymanagement.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import codes.shubham.grocerymanagement.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val lowStockThreshold: String = "2.0",
    val expiryWarningDays: String = "7",
    val geminiApiKey: String = "",
    val showApiKey: Boolean = false,
    val savedFeedback: Boolean = false
)

class SettingsViewModel(
    private val prefsRepository: UserPreferencesRepository
) : ViewModel() {

    private val _showApiKey = MutableStateFlow(false)
    private val _savedFeedback = MutableStateFlow(false)
    private val _localState = MutableStateFlow<SettingsUiState?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        prefsRepository.userPreferences,
        _showApiKey,
        _savedFeedback,
        _localState
    ) { prefs, showKey, feedback, local ->
        local ?: SettingsUiState(
            lowStockThreshold = prefs.lowStockThreshold.toString(),
            expiryWarningDays = prefs.expiryWarningDays.toString(),
            geminiApiKey = prefs.geminiApiKey,
            showApiKey = showKey,
            savedFeedback = feedback
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun onLowStockThresholdChange(value: String) =
        _localState.update { (it ?: uiState.value).copy(lowStockThreshold = value) }

    fun onExpiryDaysChange(value: String) =
        _localState.update { (it ?: uiState.value).copy(expiryWarningDays = value) }

    fun onApiKeyChange(value: String) =
        _localState.update { (it ?: uiState.value).copy(geminiApiKey = value) }

    fun toggleApiKeyVisibility() = _showApiKey.update { !it }

    fun save() {
        val state = _localState.value ?: return
        viewModelScope.launch {
            state.lowStockThreshold.toDoubleOrNull()?.let { prefsRepository.setLowStockThreshold(it) }
            state.expiryWarningDays.toIntOrNull()?.let { prefsRepository.setExpiryWarningDays(it) }
            prefsRepository.setGeminiApiKey(state.geminiApiKey.trim())
            _localState.value = null
            _savedFeedback.value = true
            kotlinx.coroutines.delay(2000)
            _savedFeedback.value = false
        }
    }
}
