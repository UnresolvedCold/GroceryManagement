package codes.shubham.grocerymanagement.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class ConsumptionReminderScheduler(private val context: Context) {

    fun scheduleDailyReminder(enabled: Boolean, hour: Int, minute: Int) {
        createNotificationChannel(context)
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pendingIntent = reminderPendingIntent(context)

        if (!enabled) {
            alarmManager.cancel(pendingIntent)
            return
        }

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            nextReminderAt(hour, minute),
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    companion object {
        const val CHANNEL_ID = "regressive_consumption_reminders"
        const val NOTIFICATION_ID = 2001

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Consumption reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily suggested pantry consumption updates"
            }

            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        fun reminderPendingIntent(context: Context): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                0,
                Intent(context, ConsumptionReminderReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
    }

    private fun nextReminderAt(hour: Int, minute: Int): Long {
        val zone = ZoneId.systemDefault()
        val safeTime = LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
        var reminderDateTime = LocalDate.now().atTime(safeTime)
        if (reminderDateTime.atZone(zone).toInstant().toEpochMilli() <= System.currentTimeMillis()) {
            reminderDateTime = reminderDateTime.plusDays(1)
        }
        return reminderDateTime.atZone(zone).toInstant().toEpochMilli()
    }
}
