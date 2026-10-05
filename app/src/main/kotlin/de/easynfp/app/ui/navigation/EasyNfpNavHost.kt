package de.easynfp.app.ui.navigation

import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import de.easynfp.app.R
import de.easynfp.app.quickentry.QuickEntryActivity
import de.easynfp.app.ui.calendar.CalendarScreen
import de.easynfp.app.ui.chart.CycleSheetScreen
import de.easynfp.app.ui.day.DayEntryScreen
import de.easynfp.app.ui.settings.DisturbancesScreen
import de.easynfp.app.ui.settings.InfoScreen
import de.easynfp.app.ui.settings.SettingsScreen
import de.easynfp.app.ui.statistics.StatisticsScreen
import de.easynfp.app.ui.today.TodayScreen
import de.easynfp.data.settings.AppSettings
import java.time.LocalDate

object Routes {
    const val TODAY = "today"
    const val CHART = "chart?start={start}"
    const val CALENDAR = "calendar"
    const val STATISTICS = "statistics"
    const val SETTINGS = "settings"
    const val DISTURBANCES = "settings/disturbances"
    const val INFO = "settings/info?topic={topic}"
    const val DAY = "day/{date}"

    fun day(date: LocalDate) = "day/$date"

    fun chart(start: LocalDate? = null) = if (start == null) "chart" else "chart?start=$start"

    fun info(topic: String) = "settings/info?topic=$topic"
}

/** N-04: four destinations in the bottom navigation. */
private enum class TopLevel(val route: String, @StringRes val label: Int, val icon: ImageVector) {
    TODAY(Routes.TODAY, R.string.nav_today, Icons.Outlined.Today),
    CHART(Routes.CHART, R.string.nav_chart, Icons.AutoMirrored.Outlined.ShowChart),
    CALENDAR(Routes.CALENDAR, R.string.nav_calendar, Icons.Outlined.CalendarMonth),
    STATISTICS(Routes.STATISTICS, R.string.nav_statistics, Icons.Outlined.BarChart),
}

/** Callbacks shared by all screens, so screens do not depend on the navigation library. */
class AppNavigator(private val navController: NavHostController, private val context: Context) {
    fun openDay(date: LocalDate) = navController.navigate(Routes.day(date))

    /** Switches to another day without growing the back stack. */
    fun replaceDay(date: LocalDate) = navController.navigate(Routes.day(date)) {
        popUpTo(Routes.DAY) { inclusive = true }
    }

    fun openChart(start: LocalDate? = null) = navController.navigate(Routes.chart(start)) {
        popUpTo(navController.graph.findStartDestination().id) { saveState = false }
        launchSingleTop = true
    }

    fun openSettings() = navController.navigate(Routes.SETTINGS)

    fun openDisturbances() = navController.navigate(Routes.DISTURBANCES)

    fun openInfo(topic: String) = navController.navigate(Routes.info(topic))

    fun openQuickEntry() {
        context.startActivity(Intent(context, QuickEntryActivity::class.java))
    }

    fun back() {
        navController.popBackStack()
    }
}

@Composable
fun EasyNfpNavHost(settings: AppSettings, openDayRequest: LocalDate? = null, onOpenDayHandled: () -> Unit = {}) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val navigator = remember(navController, context) { AppNavigator(navController, context) }
    LaunchedEffect(openDayRequest) {
        if (openDayRequest != null) {
            navigator.openDay(openDayRequest)
            onOpenDayHandled()
        }
    }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = TopLevel.entries.any { it.route == currentRoute }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    TopLevel.entries.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route.substringBefore('?')) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(stringResource(item.label)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.TODAY,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.TODAY) { TodayScreen(navigator) }
            composable(
                Routes.CHART,
                arguments = listOf(
                    navArgument("start") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) { CycleSheetScreen(navigator, settings) }
            composable(Routes.CALENDAR) { CalendarScreen(navigator) }
            composable(Routes.STATISTICS) { StatisticsScreen(navigator) }
            composable(Routes.SETTINGS) { SettingsScreen(navigator) }
            composable(Routes.DISTURBANCES) { DisturbancesScreen(navigator) }
            composable(
                Routes.INFO,
                arguments = listOf(
                    navArgument("topic") {
                        type = NavType.StringType
                        defaultValue = "about"
                    },
                ),
            ) { entry -> InfoScreen(navigator, entry.arguments?.getString("topic") ?: "about") }
            composable(
                Routes.DAY,
                arguments = listOf(navArgument("date") { type = NavType.StringType }),
            ) { DayEntryScreen(navigator, settings) }
        }
    }
}
