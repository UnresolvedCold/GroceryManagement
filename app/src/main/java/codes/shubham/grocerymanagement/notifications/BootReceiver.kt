package codes.shubham.grocerymanagement.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import codes.shubham.grocerymanagement.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val prefs = GlobalContext.get()
                    .get<UserPreferencesRepository>()
                    .userPreferences
                    .first()
                ConsumptionReminderScheduler(context).scheduleDailyReminder(
                    enabled = prefs.regressiveConsumptionEnabled,
                    hour = prefs.regressiveConsumptionReminderHour,
                    minute = prefs.regressiveConsumptionReminderMinute
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
