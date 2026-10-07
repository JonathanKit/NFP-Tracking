package de.easynfp.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import de.easynfp.data.DataContainer
import de.easynfp.rules.Phase
import de.easynfp.rules.model.Temperature
import java.text.DecimalFormatSymbols

/** What the widget shows. With active app lock (F-54) no values are shown. */
data class WidgetState(
    val locked: Boolean = false,
    val cycleDay: Int? = null,
    val phase: Phase? = null,
    val infertileFromEvening: Boolean = false,
    val temperatureToday: Temperature? = null,
    val demo: Boolean = false,
)

/** F-20: cycle day and today's status; a tap opens the app, the temperature line the quick entry. */
class EasyNfpWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = loadState(context)
        provideContent {
            GlanceTheme {
                WidgetContent(state)
            }
        }
    }

    private suspend fun loadState(context: Context): WidgetState {
        val container = DataContainer.get(context)
        if (container.settings.current().appLockEnabled) return WidgetState(locked = true)
        val snapshot = container.evaluations.current()
        val current = snapshot.current
        val phase = current?.phaseOn(snapshot.today)
        return WidgetState(
            cycleDay = current?.cycle?.cycleDayOf(snapshot.today),
            phase = phase?.phase,
            infertileFromEvening = phase?.infertileFromEvening == true,
            temperatureToday = snapshot.entries[snapshot.today]?.temperature,
            demo = container.isDemo,
        )
    }

    companion object {
        /** Handled by the quick entry activity of the app module. */
        const val ACTION_QUICK_ENTRY = "de.easynfp.action.QUICK_ENTRY"

        /** Re-renders all widgets, e.g. after data changed. */
        suspend fun updateAll(context: Context) {
            EasyNfpWidget().updateAll(context)
        }
    }
}

@Composable
private fun WidgetContent(state: WidgetState) {
    val context = LocalContext.current
    val quickEntry = Intent(EasyNfpWidget.ACTION_QUICK_ENTRY)
        .setPackage(context.packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    // A tap on the widget opens the app; the temperature line opens the quick entry (F-20, F-21).
    val app = context.packageManager.getLaunchIntentForPackage(context.packageName)
        ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ?: quickEntry
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(16.dp)
            .background(GlanceTheme.colors.widgetBackground)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .clickable(actionStartActivity(app)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val secondary = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp)
        val primary = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        if (!state.locked) {
            val cycleDay = state.cycleDay
            val dayText = if (cycleDay != null) {
                context.getString(R.string.widget_cycle_day, cycleDay)
            } else {
                context.getString(R.string.widget_no_cycle)
            }
            Text(
                text = if (state.demo) "$dayText · ${context.getString(R.string.widget_demo)}" else dayText,
                style = secondary,
            )
            statusText(context, state)?.let { Text(text = it, style = primary, maxLines = 2) }
            Spacer(GlanceModifier.height(4.dp))
        }
        val temperature = state.temperatureToday
        Text(
            text = if (temperature != null && !state.locked) {
                context.getString(R.string.widget_temperature_today, formatTemperature(temperature))
            } else {
                context.getString(R.string.widget_enter_temperature)
            },
            style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 14.sp, fontWeight = FontWeight.Medium),
            modifier = GlanceModifier
                .cornerRadius(12.dp)
                .background(GlanceTheme.colors.secondaryContainer)
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .clickable(actionStartActivity(quickEntry)),
        )
    }
}

private fun statusText(context: Context, state: WidgetState): String? {
    val phase = state.phase ?: return null
    return context.getString(
        when {
            state.infertileFromEvening -> R.string.widget_status_fertile_until_evening
            phase == Phase.INFERTILE_START -> R.string.widget_status_infertile_start
            phase == Phase.FERTILE -> R.string.widget_status_fertile
            else -> R.string.widget_status_infertile_end
        },
    )
}

private const val HUNDRED = 100

private fun formatTemperature(temperature: Temperature): String {
    val separator = DecimalFormatSymbols.getInstance().decimalSeparator
    val hundredths = temperature.hundredths
    return "${hundredths / HUNDRED}$separator${(hundredths % HUNDRED).toString().padStart(2, '0')} °C"
}

class EasyNfpWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = EasyNfpWidget()
}
