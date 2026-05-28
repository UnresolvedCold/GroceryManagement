package codes.shubham.grocerymanagement.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

data class UserPreferences(
    val lowStockThreshold: Double = 2.0,
    val expiryWarningDays: Int = 7,
    val geminiApiKey: String = "",
    val regressiveConsumptionEnabled: Boolean = true,
    val regressiveConsumptionLookbackDays: Int = 7,
    val regressiveConsumptionReminderHour: Int = 20,
    val regressiveConsumptionReminderMinute: Int = 0
)

class UserPreferencesRepository(private val context: Context) {
    private val LOW_STOCK_THRESHOLD = doublePreferencesKey("low_stock_threshold")
    private val EXPIRY_WARNING_DAYS = intPreferencesKey("expiry_warning_days")
    private val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
    private val REGRESSIVE_CONSUMPTION_ENABLED = booleanPreferencesKey("regressive_consumption_enabled")
    private val REGRESSIVE_CONSUMPTION_LOOKBACK_DAYS = intPreferencesKey("regressive_consumption_lookback_days")
    private val REGRESSIVE_CONSUMPTION_REMINDER_HOUR = intPreferencesKey("regressive_consumption_reminder_hour")
    private val REGRESSIVE_CONSUMPTION_REMINDER_MINUTE = intPreferencesKey("regressive_consumption_reminder_minute")

    val userPreferences: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            lowStockThreshold = prefs[LOW_STOCK_THRESHOLD] ?: 2.0,
            expiryWarningDays = prefs[EXPIRY_WARNING_DAYS] ?: 7,
            geminiApiKey = prefs[GEMINI_API_KEY] ?: "",
            regressiveConsumptionEnabled = prefs[REGRESSIVE_CONSUMPTION_ENABLED] ?: true,
            regressiveConsumptionLookbackDays = prefs[REGRESSIVE_CONSUMPTION_LOOKBACK_DAYS] ?: 7,
            regressiveConsumptionReminderHour = prefs[REGRESSIVE_CONSUMPTION_REMINDER_HOUR] ?: 20,
            regressiveConsumptionReminderMinute = prefs[REGRESSIVE_CONSUMPTION_REMINDER_MINUTE] ?: 0
        )
    }

    suspend fun setLowStockThreshold(value: Double) {
        context.dataStore.edit { it[LOW_STOCK_THRESHOLD] = value }
    }

    suspend fun setExpiryWarningDays(value: Int) {
        context.dataStore.edit { it[EXPIRY_WARNING_DAYS] = value }
    }

    suspend fun setGeminiApiKey(key: String) {
        context.dataStore.edit { it[GEMINI_API_KEY] = key }
    }

    suspend fun setRegressiveConsumptionEnabled(enabled: Boolean) {
        context.dataStore.edit { it[REGRESSIVE_CONSUMPTION_ENABLED] = enabled }
    }

    suspend fun setRegressiveConsumptionLookbackDays(days: Int) {
        context.dataStore.edit { it[REGRESSIVE_CONSUMPTION_LOOKBACK_DAYS] = days.coerceIn(1, 60) }
    }

    suspend fun setRegressiveConsumptionReminderTime(hour: Int, minute: Int) {
        context.dataStore.edit {
            it[REGRESSIVE_CONSUMPTION_REMINDER_HOUR] = hour.coerceIn(0, 23)
            it[REGRESSIVE_CONSUMPTION_REMINDER_MINUTE] = minute.coerceIn(0, 59)
        }
    }
}
