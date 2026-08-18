package at.designer2k2.nearscan.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import at.designer2k2.nearscan.R

/**
 * Full-screen, in-app usage guide covering the UI and the Tasker/automation integration — so
 * every user can discover both without hunting through external docs. Shown as a plain state
 * toggle from [MainScreen] rather than via a navigation library, matching this app's
 * single-screen architecture.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.help_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.help_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                HelpSection(
                    title = stringResource(R.string.help_section_basics),
                    body = stringResource(R.string.help_basics_body),
                )
            }
            item {
                HelpSection(
                    title = stringResource(R.string.help_section_location),
                    body = stringResource(R.string.help_location_body),
                )
            }
            item {
                HelpSection(
                    title = stringResource(R.string.help_section_scan_types),
                    body = stringResource(R.string.help_scan_types_body),
                )
            }
            item {
                HelpSection(
                    title = stringResource(R.string.help_section_export),
                    body = stringResource(R.string.help_export_body),
                )
            }
            item {
                HelpSection(
                    title = stringResource(R.string.help_section_battery),
                    body = stringResource(R.string.help_battery_body),
                )
            }
            item {
                HelpSection(
                    title = stringResource(R.string.help_section_tasker),
                    body = stringResource(R.string.help_tasker_intro),
                )
            }
            items(taskerReference) { block -> TaskerReferenceBlock(block) }
        }
    }
}

@Composable
private fun HelpSection(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(text = body, style = MaterialTheme.typography.bodyMedium)
    }
}

private data class TaskerBlock(val heading: String, val lines: List<String>)

/**
 * Action names, extras, and URIs are literal API identifiers that Tasker users copy verbatim into
 * its fields — deliberately hardcoded rather than string resources so a translation pass can never
 * mangle them. Keep in sync with [at.designer2k2.nearscan.ipc.CommandReceiver],
 * [at.designer2k2.nearscan.ipc.TaskerBroadcaster], and
 * [at.designer2k2.nearscan.ipc.NearScanContentProvider].
 */
private val taskerReference = listOf(
    TaskerBlock(
        "Send commands — Action › Send Intent, Package: at.designer2k2.nearscan",
        listOf(
            "CMD_START — start scanning",
            "CMD_STOP — stop scanning",
            "CMD_TOGGLE — start if stopped, stop if running",
            "CMD_EXPORT — export now. Extra: format = wigle_csv | custom_csv | geojson | sqlite",
            "CMD_SET_LOCATION — extras: lat, lon, alt (accepts Tasker %variables as strings)",
            "CMD_SET_INTERVAL — extras: type = wifi | bt | ble | cell, interval_sec",
            "CMD_CLEAR_DATA — permanently deletes all logged WiFi/BT/Cell records",
        ),
    ),
    TaskerBlock(
        "Receive events — Event › Intent Received",
        listOf(
            "SCAN_STARTED",
            "SCAN_STOPPED — extras: wifi_total, bt_total, cell_total, duration_s",
            "NEW_WIFI_FOUND — extras: ssid, bssid, rssi, channel, lat, lon",
            "NEW_BT_FOUND — extras: address, name, rssi",
            "NEW_BLE_FOUND — extras: address, name, rssi",
            "NEW_CELL_FOUND — extras: mcc, mnc, cid, rssi, tech",
            "ROUND_COMPLETE — extras: wifi_count, bt_count, cell_count, timestamp",
            "EXPORT_COMPLETE — extras: file_path, format, record_count",
        ),
    ),
    TaskerBlock(
        "Query live data — Action › Content Query",
        listOf(
            "content://at.designer2k2.nearscan.provider/wifi?limit=50",
            "content://at.designer2k2.nearscan.provider/bt?limit=50",
            "content://at.designer2k2.nearscan.provider/cell?limit=50",
            "content://at.designer2k2.nearscan.provider/stats",
            "Requires permission at.designer2k2.nearscan.permission.READ_DATA (auto-granted, normal protection level)",
        ),
    ),
)

@Composable
private fun TaskerReferenceBlock(block: TaskerBlock) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        SelectionContainer {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = block.heading,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                block.lines.forEach { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }
    }
}
