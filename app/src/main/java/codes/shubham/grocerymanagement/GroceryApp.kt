package codes.shubham.grocerymanagement

import android.app.Application
import codes.shubham.grocerymanagement.data.preferences.UserPreferencesRepository
import codes.shubham.grocerymanagement.di.appModule
import codes.shubham.grocerymanagement.notifications.ConsumptionReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.android.get
import org.koin.core.context.startKoin

class GroceryApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@GroceryApp)
            modules(appModule)
        }
        ConsumptionReminderScheduler.createNotificationChannel(this)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val prefs = get<UserPreferencesRepository>().userPreferences.first()
            get<ConsumptionReminderScheduler>().scheduleDailyReminder(
                enabled = prefs.regressiveConsumptionEnabled,
                hour = prefs.regressiveConsumptionReminderHour,
                minute = prefs.regressiveConsumptionReminderMinute
            )
        }
    }
}
