package kz.chaykin.potracheno.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.chaykin.potracheno.BuildConfig
import kz.chaykin.potracheno.R
import kz.chaykin.potracheno.data.prefs.SyncState
import kz.chaykin.potracheno.data.prefs.ThemeMode
import kz.chaykin.potracheno.ui.components.ConfirmDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val sync by viewModel.sync.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    var pendingImport by remember { mutableStateOf<Uri?>(null) }
    var confirmDriveRestore by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri -> if (uri != null) viewModel.export(uri) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) pendingImport = uri }

    // Экран согласия Google возвращается сюда: результат отдаём обратно во ViewModel,
    // она сама доделает то, ради чего согласие и спрашивали.
    val consentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result -> viewModel.onDriveConsent(result.data) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is BackupEvent.DriveConsentNeeded) {
                consentLauncher.launch(IntentSenderRequest.Builder(event.intent).build())
                return@collect
            }

            val message = when (event) {
                is BackupEvent.Exported -> context.getString(R.string.export_done)
                is BackupEvent.ExportFailed -> context.getString(R.string.export_failed, event.reason)
                is BackupEvent.Imported ->
                    context.getString(R.string.import_done, event.trips, event.operations)
                is BackupEvent.ImportFailed -> context.getString(R.string.import_failed, event.reason)
                is BackupEvent.DriveConnected -> context.getString(R.string.drive_connected_done)
                is BackupEvent.DriveDisconnected -> context.getString(R.string.drive_disconnected_done)
                is BackupEvent.DriveUploaded -> context.getString(R.string.drive_uploaded_done)
                is BackupEvent.DriveRestored ->
                    context.getString(R.string.drive_restored_done, event.trips, event.operations)
                is BackupEvent.DriveFailed -> context.getString(R.string.drive_failed, event.reason)
                is BackupEvent.DemoCreated -> context.getString(R.string.demo_created)
                is BackupEvent.DriveConsentNeeded -> return@collect
            }
            snackbarHost.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SectionTitle(stringResource(R.string.settings_theme))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    ThemeMode.SYSTEM to R.string.theme_system,
                    ThemeMode.LIGHT to R.string.theme_light,
                    ThemeMode.DARK to R.string.theme_dark,
                ).forEach { (mode, labelRes) ->
                    FilterChip(
                        selected = settings.themeMode == mode,
                        onClick = { viewModel.setThemeMode(mode) },
                        label = { Text(stringResource(labelRes)) },
                    )
                }
            }


            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))


            SectionTitle(stringResource(R.string.settings_backup))
            ActionRow(
                title = stringResource(R.string.settings_export),
                subtitle = stringResource(R.string.settings_export_hint),
                onClick = { exportLauncher.launch(defaultBackupName()) },
            )
            ActionRow(
                title = stringResource(R.string.settings_import),
                subtitle = stringResource(R.string.settings_import_hint),
                onClick = { importLauncher.launch(arrayOf("application/zip", "application/octet-stream")) },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            DriveSection(
                sync = sync,
                busy = busy,
                onConnect = viewModel::connectDrive,
                onDisconnect = viewModel::disconnectDrive,
                onUpload = viewModel::uploadToDrive,
                onRestore = { confirmDriveRestore = true },
                onAutoDaily = viewModel::setAutoDaily,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SectionTitle(stringResource(R.string.settings_demo))
            ActionRow(
                title = stringResource(R.string.demo_create),
                subtitle = stringResource(R.string.demo_create_hint),
                onClick = viewModel::createDemoTrip,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = stringResource(R.string.settings_about_text, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }

    if (confirmDriveRestore) {
        ConfirmDialog(
            title = stringResource(R.string.drive_restore_confirm_title),
            text = stringResource(R.string.drive_restore_confirm_text),
            confirmLabel = stringResource(R.string.drive_restore),
            onConfirm = {
                confirmDriveRestore = false
                viewModel.restoreFromDrive()
            },
            onDismiss = { confirmDriveRestore = false },
        )
    }

    pendingImport?.let { uri ->
        ConfirmDialog(
            title = stringResource(R.string.import_confirm_title),
            text = stringResource(R.string.import_confirm_text),
            confirmLabel = stringResource(R.string.settings_import),
            onConfirm = {
                pendingImport = null
                viewModel.import(uri)
            },
            onDismiss = { pendingImport = null },
        )
    }
}

private fun defaultBackupName(): String {
    val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    return "potracheno-backup-$date.zip"
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun ActionRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.outline
            },
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Раздел Google Диска. Пока Диск не подключён, показываем одну строку: остальные действия
 * без доступа всё равно ничего не сделают, а лишние пункты только отвлекают.
 */
@Composable
private fun DriveSection(
    sync: SyncState,
    busy: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onUpload: () -> Unit,
    onRestore: () -> Unit,
    onAutoDaily: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.settings_drive),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        if (busy) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        }
    }

    if (!sync.connected) {
        ActionRow(
            title = stringResource(R.string.drive_connect),
            subtitle = stringResource(R.string.drive_connect_hint),
            enabled = !busy,
            onClick = onConnect,
        )
        return
    }

    Text(
        text = sync.accountEmail
            ?.let { stringResource(R.string.drive_account, it) }
            ?: stringResource(R.string.drive_account_unknown),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )

    ActionRow(
        title = stringResource(R.string.drive_upload),
        subtitle = sync.lastSyncAt
            .takeIf { it > 0 }
            ?.let { stringResource(R.string.drive_upload_hint, formatMoment(it)) }
            ?: stringResource(R.string.drive_upload_hint_never),
        enabled = !busy,
        onClick = onUpload,
    )

    SwitchRow(
        title = stringResource(R.string.drive_auto),
        subtitle = stringResource(R.string.drive_auto_hint),
        checked = sync.autoDaily,
        onCheckedChange = onAutoDaily,
    )

    ActionRow(
        title = stringResource(R.string.drive_restore),
        subtitle = stringResource(R.string.drive_restore_hint),
        enabled = !busy,
        onClick = onRestore,
    )

    ActionRow(
        title = stringResource(R.string.drive_disconnect),
        subtitle = stringResource(R.string.drive_disconnect_hint),
        enabled = !busy,
        onClick = onDisconnect,
    )

    sync.lastError?.let { error ->
        Text(
            text = stringResource(R.string.drive_last_error, error),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }
}

private fun formatMoment(millis: Long): String =
    SimpleDateFormat("d MMMM, HH:mm", Locale.forLanguageTag("ru")).format(Date(millis))

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
