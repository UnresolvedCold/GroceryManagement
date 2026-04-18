package codes.shubham.grocerymanagement.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
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
    val geminiApiKey: String = ""
)

class UserPreferencesRepository(private val context: Context) {
    private val LOW_STOCK_THRESHOLD = doublePreferencesKey("low_stock_threshold")
    private val EXPIRY_WARNING_DAYS = intPreferencesKey("expiry_warning_days")
    private val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")

    val userPreferences: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            lowStockThreshold = prefs[LOW_STOCK_THRESHOLD] ?: 2.0,
            expiryWarningDays = prefs[EXPIRY_WARNING_DAYS] ?: 7,
            geminiApiKey = prefs[GEMINI_API_KEY] ?: ""
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
}
