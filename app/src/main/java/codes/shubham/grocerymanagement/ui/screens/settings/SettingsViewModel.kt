package codes.shubham.grocerymanagement.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import codes.shubham.grocerymanagement.data.preferences.UserPreferencesRepository
import codes.shubham.grocerymanagement.notifications.ConsumptionReminderScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val lowStockThreshold: String = "2.0",
    val expiryWarningDays: String = "7",
    val geminiApiKey: String = "",
    val regressiveConsumptionEnabled: Boolean = true,
    val regressiveConsumptionLookbackDays: String = "7",
    val regressiveConsumptionReminderHour: String = "20",
    val regressiveConsumptionReminderMinute: String = "00",
    val showApiKey: Boolean = false,
    val savedFeedback: Boolean = false
)

class SettingsViewModel(
    private val prefsRepository: UserPreferencesRepository,
    private val consumptionReminderScheduler: ConsumptionReminderScheduler
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
            regressiveConsumptionEnabled = prefs.regressiveConsumptionEnabled,
            regressiveConsumptionLookbackDays = prefs.regressiveConsumptionLookbackDays.toString(),
            regressiveConsumptionReminderHour = prefs.regressiveConsumptionReminderHour.toString().padStart(2, '0'),
            regressiveConsumptionReminderMinute = prefs.regressiveConsumptionReminderMinute.toString().padStart(2, '0'),
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

    fun onRegressiveConsumptionEnabledChange(value: Boolean) =
        _localState.update { (it ?: uiState.value).copy(regressiveConsumptionEnabled = value) }

    fun onRegressiveLookbackDaysChange(value: String) =
        _localState.update { (it ?: uiState.value).copy(regressiveConsumptionLookbackDays = value) }

    fun onRegressiveReminderHourChange(value: String) =
        _localState.update { (it ?: uiState.value).copy(regressiveConsumptionReminderHour = value) }

    fun onRegressiveReminderMinuteChange(value: String) =
        _localState.update { (it ?: uiState.value).copy(regressiveConsumptionReminderMinute = value) }

    fun toggleApiKeyVisibility() = _showApiKey.update { !it }

    fun save() {
        val state = _localState.value ?: return
        viewModelScope.launch {
            state.lowStockThreshold.toDoubleOrNull()?.let { prefsRepository.setLowStockThreshold(it) }
            state.expiryWarningDays.toIntOrNull()?.let { prefsRepository.setExpiryWarningDays(it) }
            prefsRepository.setGeminiApiKey(state.geminiApiKey.trim())
            prefsRepository.setRegressiveConsumptionEnabled(state.regressiveConsumptionEnabled)
            state.regressiveConsumptionLookbackDays.toIntOrNull()?.let {
                prefsRepository.setRegressiveConsumptionLookbackDays(it)
            }
            val hour = state.regressiveConsumptionReminderHour.toIntOrNull()?.coerceIn(0, 23) ?: 20
            val minute = state.regressiveConsumptionReminderMinute.toIntOrNull()?.coerceIn(0, 59) ?: 0
            prefsRepository.setRegressiveConsumptionReminderTime(hour, minute)
            consumptionReminderScheduler.scheduleDailyReminder(
                enabled = state.regressiveConsumptionEnabled,
                hour = hour,
                minute = minute
            )
            _localState.value = null
            _savedFeedback.value = true
            kotlinx.coroutines.delay(2000)
            _savedFeedback.value = false
        }
    }
}
