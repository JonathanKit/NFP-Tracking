package de.easynfp.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import de.easynfp.app.R
import de.easynfp.app.text.Formats
import de.easynfp.app.ui.components.SectionCard
import de.easynfp.app.ui.components.SubScreenScaffold
import de.easynfp.app.ui.components.appViewModelFactory
import de.easynfp.app.ui.navigation.AppNavigator
import de.easynfp.data.DataContainer
import de.easynfp.data.model.Disturbance
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DisturbancesViewModel(private val container: DataContainer) : ViewModel() {
    val disturbances: StateFlow<List<Disturbance>> = container.repository.disturbances
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    fun add(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { container.repository.addDisturbance(name) }
    }

    fun rename(disturbance: Disturbance, name: String) {
        viewModelScope.launch {
            container.repository.updateDisturbance(disturbance.copy(customName = name.trim().ifEmpty { null }))
        }
    }

    fun setArchived(disturbance: Disturbance, archived: Boolean) {
        viewModelScope.launch { container.repository.updateDisturbance(disturbance.copy(archived = archived)) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

/** F-09: create, rename and archive disturbances. Archived ones stay on the days they were used. */
@Composable
fun DisturbancesScreen(
    navigator: AppNavigator,
    viewModel: DisturbancesViewModel = viewModel(factory = appViewModelFactory { c, _, _ -> DisturbancesViewModel(c) }),
) {
    val disturbances by viewModel.disturbances.collectAsStateWithLifecycle()
    val resources = LocalContext.current.resources
    var editing by remember { mutableStateOf<Disturbance?>(null) }
    var adding by remember { mutableStateOf(false) }

    SubScreenScaffold(
        title = stringResource(R.string.manage_disturbances),
        onBack = navigator::back,
        actions = {
            IconButton(onClick = { adding = true }) {
                Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.disturbance_add))
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val (archived, active) = disturbances.partition { it.archived }
            SectionCard(title = stringResource(R.string.disturbances_active)) {
                active.forEach { disturbance ->
                    DisturbanceRow(
                        name = Formats.disturbanceName(resources, disturbance),
                        archived = false,
                        onRename = { editing = disturbance },
                        onToggleArchive = { viewModel.setArchived(disturbance, true) },
                    )
                }
                TextButton(onClick = { adding = true }) { Text(stringResource(R.string.disturbance_add)) }
            }
            if (archived.isNotEmpty()) {
                SectionCard(title = stringResource(R.string.disturbances_archived)) {
                    archived.forEach { disturbance ->
                        DisturbanceRow(
                            name = Formats.disturbanceName(resources, disturbance),
                            archived = true,
                            onRename = { editing = disturbance },
                            onToggleArchive = { viewModel.setArchived(disturbance, false) },
                        )
                    }
                }
            }
        }
    }

    if (adding) {
        NameDialog(
            title = stringResource(R.string.disturbance_add),
            initial = "",
            onConfirm = {
                viewModel.add(it)
                adding = false
            },
            onDismiss = { adding = false },
        )
    }
    editing?.let { disturbance ->
        NameDialog(
            title = stringResource(R.string.disturbance_rename),
            initial = Formats.disturbanceName(resources, disturbance),
            onConfirm = {
                viewModel.rename(disturbance, it)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun DisturbanceRow(name: String, archived: Boolean, onRename: () -> Unit, onToggleArchive: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(
            name,
            modifier = Modifier.weight(1f),
            color = if (archived) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        )
        IconButton(onClick = onRename) {
            Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.disturbance_rename))
        }
        IconButton(onClick = onToggleArchive) {
            Icon(
                if (archived) Icons.Outlined.Unarchive else Icons.Outlined.Archive,
                contentDescription = stringResource(
                    if (archived) R.string.disturbance_unarchive else R.string.disturbance_archive,
                ),
            )
        }
    }
}

@Composable
private fun NameDialog(title: String, initial: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text(stringResource(R.string.disturbance_name)) },
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank(),
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
