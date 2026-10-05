package de.easynfp.app.reminders

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import de.easynfp.app.MainActivity
import de.easynfp.app.R
import de.easynfp.app.quickentry.QuickEntryActivity
import de.easynfp.data.DataContainer
import de.easynfp.data.settings.AppSettings
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

enum class ReminderKind(val requestCode: Int, val action: String) {
    /** F-23: temperature in the morning. */
    MORNING(1, "de.easynfp.action.REMINDER_MORNING"),

    /** F-24: cervical mucus in the evening. */
    EVENING(2, "de.easynfp.action.REMINDER_EVENING"),
}

/** Schedules the daily reminders with inexact, doze-friendly alarms. No network needed (F-23, F-24). */
object ReminderScheduler {

    fun schedule(context: Context, settings: AppSettings) {
        scheduleOne(context, ReminderKind.MORNING, settings.morningReminderEnabled, settings.morningReminderTime)
        scheduleOne(context, ReminderKind.EVENING, settings.eveningReminderEnabled, settings.eveningReminderTime)
    }

    private fun scheduleOne(context: Context, kind: ReminderKind, enabled: Boolean, time: LocalTime) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val intent = pendingIntent(context, kind)
        alarmManager.cancel(intent)
        if (!enabled) return
        val trigger = nextOccurrence(LocalDateTime.now(), time)
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            trigger.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            intent,
        )
    }

    fun nextOccurrence(now: LocalDateTime, time: LocalTime): LocalDateTime {
        val today = now.toLocalDate().atTime(time)
        return if (today.isAfter(now)) today else today.plusDays(1)
    }

    private fun pendingIntent(context: Context, kind: ReminderKind): PendingIntent = PendingIntent.getBroadcast(
        context,
        kind.requestCode,
        Intent(context, ReminderReceiver::class.java).setAction(kind.action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

/** Shows a reminder unless the entry was already made, then schedules the next one. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val kind = ReminderKind.entries.firstOrNull { it.action == intent.action } ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val container = DataContainer.get(context)
                val settings = container.settings.current()
                val today = LocalDate.now(container.clock)
                val entry = container.repository.getDay(today)
                val needed = when (kind) {
                    ReminderKind.MORNING -> settings.morningReminderEnabled && entry?.temperature == null
                    ReminderKind.EVENING -> settings.eveningReminderEnabled && entry?.hasMucusEntry != true
                }
                if (needed) Notifications.showReminder(context, kind)
                ReminderScheduler.schedule(context, settings)
            } finally {
                pending.finish()
            }
        }
    }
}

/** Re-creates alarms after reboot, app update or clock changes. */
class SystemEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                ReminderScheduler.schedule(context, DataContainer.get(context).settings.current())
            } finally {
                pending.finish()
            }
        }
    }
}

object Notifications {
    private const val CHANNEL_REMINDERS = "reminders"

    fun createChannels(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_REMINDERS,
            context.getString(R.string.notification_channel_reminders),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.notification_channel_reminders_description) }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    fun hasPermission(context: Context): Boolean =
        android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    // The permission is checked by hasPermission() right below.
    @SuppressLint("MissingPermission")
    fun showReminder(context: Context, kind: ReminderKind) {
        if (!hasPermission(context)) return
        val target = when (kind) {
            ReminderKind.MORNING -> Intent(context, QuickEntryActivity::class.java)
            ReminderKind.EVENING -> Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_OPEN_DAY, LocalDate.now().toString())
        }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val contentIntent = PendingIntent.getActivity(
            context,
            kind.requestCode,
            target,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val (title, text) = when (kind) {
            ReminderKind.MORNING -> R.string.reminder_morning_title to R.string.reminder_morning_text
            ReminderKind.EVENING -> R.string.reminder_evening_title to R.string.reminder_evening_text
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(title))
            .setContentText(context.getString(text))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(kind.requestCode, notification)
        } catch (e: SecurityException) {
            // Permission was revoked in the meantime; nothing to do.
        }
    }
}
