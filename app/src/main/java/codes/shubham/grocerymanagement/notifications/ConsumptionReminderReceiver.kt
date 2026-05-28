package codes.shubham.grocerymanagement.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import codes.shubham.grocerymanagement.MainActivity
import codes.shubham.grocerymanagement.R
import codes.shubham.grocerymanagement.data.preferences.UserPreferencesRepository
import codes.shubham.grocerymanagement.data.repository.GroceryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

class ConsumptionReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                sendReminderIfNeeded(context)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun sendReminderIfNeeded(context: Context) {
        val koin = GlobalContext.get()
        val prefsRepository = koin.get<UserPreferencesRepository>()
        val prefs = prefsRepository.userPreferences.first()
        if (!prefs.regressiveConsumptionEnabled) return

        val suggestions = koin.get<GroceryRepository>()
            .getRegressiveConsumptionSuggestionsSnapshot(prefs.regressiveConsumptionLookbackDays)
        if (suggestions.isEmpty()) return
        if (!canPostNotifications(context)) return

        ConsumptionReminderScheduler.createNotificationChannel(context)
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val summary = suggestions
            .take(3)
            .joinToString { "${it.productName} ${formatQuantity(it.quantity)} ${it.unit}" }
            .let { text ->
                if (suggestions.size > 3) "$text, +${suggestions.size - 3} more" else text
            }

        val detail = suggestions.joinToString(separator = "\n") {
            "- ${it.productName}: ${formatQuantity(it.quantity)} ${it.unit}"
        }

        val notification = NotificationCompat.Builder(context, ConsumptionReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Suggested pantry updates")
            .setContentText(summary)
            .setStyle(NotificationCompat.BigTextStyle().bigText(detail))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context)
            .notify(ConsumptionReminderScheduler.NOTIFICATION_ID, notification)
    }

    private fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun formatQuantity(quantity: Double): String =
        if (quantity == quantity.toLong().toDouble()) {
            quantity.toLong().toString()
        } else {
            "%.2f".format(quantity)
        }
}
