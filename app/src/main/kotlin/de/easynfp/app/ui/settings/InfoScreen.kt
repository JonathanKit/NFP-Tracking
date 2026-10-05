package de.easynfp.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.easynfp.app.R
import de.easynfp.app.ui.components.SectionCard
import de.easynfp.app.ui.components.SubScreenScaffold
import de.easynfp.app.ui.navigation.AppNavigator

/** R-08 hints and information about the app (not a medical device, rules, privacy). */
@Composable
fun InfoScreen(navigator: AppNavigator, topic: String) {
    val sections = if (topic == "hints") {
        listOf(
            R.string.hints_measure_title to R.string.hints_measure_text,
            R.string.hints_exclude_title to R.string.hints_exclude_text,
            R.string.hints_mucus_title to R.string.hints_mucus_text,
        )
    } else {
        listOf(
            R.string.about_disclaimer_title to R.string.about_disclaimer_text,
            R.string.about_rules_title to R.string.about_rules_text,
            R.string.about_privacy_title to R.string.about_privacy_text,
        )
    }
    SubScreenScaffold(
        title = stringResource(if (topic == "hints") R.string.hints_title else R.string.about_title),
        onBack = navigator::back,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            sections.forEach { (title, text) ->
                SectionCard(title = stringResource(title)) { Text(stringResource(text)) }
            }
        }
    }
}
