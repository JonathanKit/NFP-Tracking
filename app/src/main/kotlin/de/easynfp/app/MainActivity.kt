package de.easynfp.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.easynfp.app.lock.AppLock
import de.easynfp.app.ui.navigation.EasyNfpNavHost
import de.easynfp.app.ui.theme.EasyNfpTheme
import de.easynfp.data.DataContainer
import de.easynfp.data.settings.AppSettings
import java.time.LocalDate

class MainActivity : FragmentActivity() {
    /** Day requested by a notification (F-24), consumed by the navigation. */
    private val openDayRequest = mutableStateOf<LocalDate?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handleIntent(intent)
        val settingsFlow = DataContainer.get(this).settings.settings
        setContent {
            EasyNfpTheme {
                val settings by settingsFlow.collectAsStateWithLifecycle(initialValue = null)
                val unlocked by AppLock.unlocked.collectAsState()
                when {
                    settings == null -> Surface(Modifier.fillMaxSize()) {}
                    settings?.appLockEnabled == true && !unlocked -> LockScreen(activity = this)
                    else -> EasyNfpNavHost(
                        settings = settings ?: AppSettings(),
                        openDayRequest = openDayRequest.value,
                        onOpenDayHandled = { openDayRequest.value = null },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val day = intent?.getStringExtra(EXTRA_OPEN_DAY) ?: return
        openDayRequest.value = runCatching { LocalDate.parse(day) }.getOrNull()
    }

    companion object {
        const val EXTRA_OPEN_DAY = "de.easynfp.extra.OPEN_DAY"
    }
}

@Composable
fun LockScreen(activity: FragmentActivity) {
    val title = stringResource(R.string.lock_prompt_title)
    val unlock = { AppLock.authenticate(activity, title) {} }
    LaunchedEffect(Unit) { unlock() }
    Surface(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(48.dp))
            Text(
                stringResource(R.string.lock_message),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Button(onClick = unlock) { Text(stringResource(R.string.lock_unlock)) }
        }
    }
}
