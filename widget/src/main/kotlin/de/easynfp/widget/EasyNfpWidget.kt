package de.easynfp.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import de.easynfp.data.DataContainer
import de.easynfp.rules.Phase

/** What the widget shows. With active app lock (F-54) no values are shown. */
data class WidgetState(
    val locked: Boolean = false,
    val cycleDay: Int? = null,
    val phase: Phase? = null,
    val infertileFromEvening: Boolean = false,
    val demo: Boolean = false,
)

/** F-20: small widget with cycle day and today's status; a tap anywhere opens the app. */
class EasyNfpWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = loadState(context)
        provideContent {
            WidgetContent(state)
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
            demo = container.isDemo,
        )
    }

    companion object {
        /** Re-renders all widgets, e.g. after data changed. */
        suspend fun updateAll(context: Context) {
            EasyNfpWidget().updateAll(context)
        }
    }
}

/** Colors matching the app theme (day / night). */
private object WidgetColors {
    val background = ColorProvider(day = Color(0xFFFFF8F5), night = Color(0xFF221E28))
    val accent = ColorProvider(day = Color(0xFF9A4530), night = Color(0xFFFFB59F))
    val text = ColorProvider(day = Color(0xFF201A1D), night = Color(0xFFEDE5EE))
    val muted = ColorProvider(day = Color(0xFF6B5F66), night = Color(0xFFBCB1BF))
    val infertile = ColorProvider(day = Color(0xFF00695C), night = Color(0xFF7FE0C8))
    val fertile = ColorProvider(day = Color(0xFFA0306A), night = Color(0xFFFFA8CB))
}

@Composable
private fun WidgetContent(state: WidgetState) {
    val context = LocalContext.current
    val app = context.packageManager.getLaunchIntentForPackage(context.packageName)
        ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    var modifier = GlanceModifier
        .fillMaxSize()
        .cornerRadius(20.dp)
        .background(WidgetColors.background)
        .padding(horizontal = 12.dp, vertical = 6.dp)
    if (app != null) modifier = modifier.clickable(actionStartActivity(app))
    Column(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        val cycleDay = state.cycleDay
        val title = when {
            state.locked -> context.getString(R.string.widget_name)
            cycleDay != null -> context.getString(R.string.widget_cycle_day, cycleDay)
            else -> context.getString(R.string.widget_no_cycle)
        }
        Text(
            text = if (state.demo) "$title · ${context.getString(R.string.widget_demo)}" else title,
            style = TextStyle(color = WidgetColors.accent, fontSize = 15.sp, fontWeight = FontWeight.Bold),
            maxLines = 1,
        )
        val status = if (state.locked) null else statusText(context, state)
        Text(
            text = status ?: context.getString(R.string.widget_open_app),
            style = TextStyle(
                color = when {
                    status == null -> WidgetColors.muted
                    state.phase == Phase.FERTILE -> WidgetColors.fertile
                    else -> WidgetColors.infertile
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            ),
            maxLines = 1,
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

class EasyNfpWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = EasyNfpWidget()
}
