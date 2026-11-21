package com.vknxworks.jellyctrl.ui.settings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.vknxworks.jellyctrl.BuildConfig
import com.vknxworks.jellyctrl.R
import com.vknxworks.jellyctrl.container
import com.vknxworks.jellyctrl.data.settings.BackupFormatException
import com.vknxworks.jellyctrl.data.settings.SettingsBackup
import com.vknxworks.jellyctrl.data.settings.SettingsBackupFile
import com.vknxworks.jellyctrl.ui.components.CtrlAlertDialog
import com.vknxworks.jellyctrl.ui.components.Group
import com.vknxworks.jellyctrl.ui.components.GroupRow
import com.vknxworks.jellyctrl.ui.lock.AppLock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDate
import java.time.OffsetDateTime
import kotlin.coroutines.cancellation.CancellationException

/**
 * Back up and restore settings: a small JSON file in a place the user picks (Downloads, a cloud folder),
 * with JellyCtrl's own settings and nothing secret. Restoring shows what would change and asks first.
 */
@Composable
fun BackupGroup() {
    val context = LocalContext.current
    val c = context.container
    val scope = rememberCoroutineScope()
    /** A file that was read and is waiting for the "replace your settings?" answer. */
    var pending by remember { mutableStateOf<SettingsBackupFile?>(null) }

    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = try {
                val text = SettingsBackupFile.write(BuildConfig.VERSION_NAME, OffsetDateTime.now().toString(), SettingsBackup.valuesOf(c.settings.settings.value))
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray()) } ?: throw IOException("no stream")
                }
                true
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                false
            }
            Toast.makeText(context, if (ok) R.string.backup_saved else R.string.backup_save_failed, Toast.LENGTH_LONG).show()
        }
    }

    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val text = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        val out = java.io.ByteArrayOutputStream()
                        val buf = ByteArray(8192)
                        while (true) {
                            val n = input.read(buf)
                            if (n < 0) break
                            out.write(buf, 0, n)
                            if (out.size() > SettingsBackupFile.MAX_BYTES) throw BackupFormatException(newer = false)
                        }
                        out.toString(Charsets.UTF_8.name())
                    } ?: throw IOException("no stream")
                }
                val file = SettingsBackupFile.read(text)
                if (SettingsBackup.changedLabels(c.settings.settings.value, file).isEmpty()) {
                    Toast.makeText(context, R.string.backup_nothing_to_change, Toast.LENGTH_LONG).show()
                } else {
                    pending = file
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                val msg = if (e is BackupFormatException && e.newer) R.string.backup_too_new else R.string.backup_not_valid
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    Group(stringResource(R.string.backup_title), footer = stringResource(R.string.backup_footer)) {
        GroupRow(
            stringResource(R.string.backup_save),
            stringResource(R.string.backup_save_sub),
            Icons.Outlined.FileDownload,
            // Android's file picker opens over the app: that isn't leaving it, for the app lock.
            onClick = { AppLock.expectReturn(); save.launch("jellyctrl-settings-${LocalDate.now()}.json") },
        )
        GroupRow(
            stringResource(R.string.backup_restore),
            stringResource(R.string.backup_restore_sub),
            Icons.Outlined.FileUpload,
            onClick = { AppLock.expectReturn(); open.launch(arrayOf("application/json", "text/*", "application/octet-stream")) },
        )
    }

    pending?.let { file ->
        // Worked out against the settings as they are now, which is what "Replace" will change.
        val labels = SettingsBackup.changedLabels(c.settings.settings.value, file).map { stringResource(it) }
        CtrlAlertDialog(
            onDismissRequest = { pending = null },
            title = { Text(stringResource(R.string.backup_restore_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.backup_restore_body,
                        file.createdAt?.take(10) ?: "?",
                        file.appVersion ?: "?",
                        labels.joinToString("\n") { "• $it" },
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pending = null
                    scope.launch {
                        val ok = runCatching { c.settings.update { SettingsBackup.apply(it, file) } }.isSuccess
                        Toast.makeText(context, if (ok) R.string.backup_restored else R.string.backup_restore_failed, Toast.LENGTH_LONG).show()
                    }
                }) { Text(stringResource(R.string.backup_restore_confirm)) }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}
