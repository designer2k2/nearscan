package at.designer2k2.nearscan.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import at.designer2k2.nearscan.R
import at.designer2k2.nearscan.prefs.ExportFormat
import at.designer2k2.nearscan.prefs.NearScanSettings

/** Wireless radios (WiFi/BT/BLE) rescan often enough that a 300s interval is pointless; cell towers change slowly. */
private const val WIRELESS_MAX_INTERVAL_SEC = 60
private const val CELL_MAX_INTERVAL_SEC = 300

@Composable
fun AdvancedSettingsCard(
    settings: NearScanSettings,
    onSettingsChange: (NearScanSettings) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    isExporting: Boolean,
    onExportNow: () -> Unit,
    onClearData: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showClearConfirm by remember { mutableStateOf(false) }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandedChange(!expanded) },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.advanced_settings),
                    style = MaterialTheme.typography.titleLarge,
                )
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ScanTypeRow(
                        label = stringResource(R.string.scan_wifi),
                        enabled = settings.scanWifiEnabled,
                        intervalSec = settings.intervalWifiSec,
                        maxIntervalSec = WIRELESS_MAX_INTERVAL_SEC,
                        onEnabledChange = { onSettingsChange(settings.copy(scanWifiEnabled = it)) },
                        onIntervalChange = { onSettingsChange(settings.copy(intervalWifiSec = it)) },
                    )
                    WifiMinRssiRow(
                        minRssi = settings.wifiMinRssi,
                        onChange = { onSettingsChange(settings.copy(wifiMinRssi = it)) },
                    )
                    WifiBandsRow(
                        selectedBands = settings.wifiBands,
                        onBandsChange = { onSettingsChange(settings.copy(wifiBands = it)) },
                    )
                    ScanTypeRow(
                        label = stringResource(R.string.scan_bt),
                        enabled = settings.scanBtEnabled,
                        intervalSec = settings.intervalBtSec,
                        maxIntervalSec = WIRELESS_MAX_INTERVAL_SEC,
                        onEnabledChange = { onSettingsChange(settings.copy(scanBtEnabled = it)) },
                        onIntervalChange = { onSettingsChange(settings.copy(intervalBtSec = it)) },
                    )
                    ScanTypeRow(
                        label = stringResource(R.string.scan_ble),
                        enabled = settings.scanBleEnabled,
                        intervalSec = settings.intervalBleSec,
                        maxIntervalSec = WIRELESS_MAX_INTERVAL_SEC,
                        onEnabledChange = { onSettingsChange(settings.copy(scanBleEnabled = it)) },
                        onIntervalChange = { onSettingsChange(settings.copy(intervalBleSec = it)) },
                    )
                    ScanTypeRow(
                        label = stringResource(R.string.scan_cell),
                        enabled = settings.scanCellEnabled,
                        intervalSec = settings.intervalCellSec,
                        maxIntervalSec = CELL_MAX_INTERVAL_SEC,
                        onEnabledChange = { onSettingsChange(settings.copy(scanCellEnabled = it)) },
                        onIntervalChange = { onSettingsChange(settings.copy(intervalCellSec = it)) },
                    )

                    ExportFormatDropdown(
                        selected = settings.exportFormat,
                        onSelected = { onSettingsChange(settings.copy(exportFormat = it)) },
                    )
                    AutoExportRow(
                        intervalMin = settings.autoExportIntervalMin,
                        onChange = { onSettingsChange(settings.copy(autoExportIntervalMin = it)) },
                    )

                    Button(
                        onClick = onExportNow,
                        enabled = !isExporting,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (isExporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Text(stringResource(R.string.export_now))
                        }
                    }

                    ToggleRow(
                        label = stringResource(R.string.mqtt_enable),
                        checked = settings.mqttEnabled,
                        onChange = { onSettingsChange(settings.copy(mqttEnabled = it)) },
                    )
                    AnimatedVisibility(visible = settings.mqttEnabled) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = settings.mqttBroker,
                                onValueChange = { onSettingsChange(settings.copy(mqttBroker = it)) },
                                label = { Text(stringResource(R.string.mqtt_broker)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = settings.mqttTopic,
                                onValueChange = { onSettingsChange(settings.copy(mqttTopic = it)) },
                                label = { Text(stringResource(R.string.mqtt_topic)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    ToggleRow(
                        label = stringResource(R.string.keep_screen_on),
                        checked = settings.keepScreenOn,
                        onChange = { onSettingsChange(settings.copy(keepScreenOn = it)) },
                    )
                    ToggleRow(
                        label = stringResource(R.string.dedup),
                        checked = settings.dedupEnabled,
                        onChange = { onSettingsChange(settings.copy(dedupEnabled = it)) },
                    )

                    Button(
                        onClick = { showClearConfirm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.clear_data))
                    }
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(stringResource(R.string.clear_data_title)) },
            text = { Text(stringResource(R.string.clear_data_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirm = false
                        onClearData()
                    },
                ) {
                    Text(stringResource(R.string.clear_data_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun ScanTypeRow(
    label: String,
    enabled: Boolean,
    intervalSec: Int,
    maxIntervalSec: Int,
    onEnabledChange: (Boolean) -> Unit,
    onIntervalChange: (Int) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = label, fontWeight = FontWeight.Medium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "${intervalSec}s")
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
        Slider(
            value = intervalSec.toFloat(),
            onValueChange = { onIntervalChange(it.toInt()) },
            valueRange = 1f..maxIntervalSec.toFloat(),
            enabled = enabled,
        )
    }
}

@Composable
private fun WifiMinRssiRow(
    minRssi: Int,
    onChange: (Int) -> Unit,
) {
    Column {
        Text(text = stringResource(R.string.wifi_min_rssi, minRssi), fontWeight = FontWeight.Medium)
        Slider(
            value = minRssi.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = -100f..-30f,
        )
    }
}

@Composable
private fun WifiBandsRow(
    selectedBands: Set<String>,
    onBandsChange: (Set<String>) -> Unit,
) {
    Column {
        Text(text = stringResource(R.string.wifi_bands), fontWeight = FontWeight.Medium)
        Row(
            modifier = Modifier.padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf("2.4", "5", "6").forEach { band ->
                FilterChip(
                    selected = band in selectedBands,
                    onClick = {
                        val updated = if (band in selectedBands) selectedBands - band else selectedBands + band
                        // Never allow deselecting every band — that would silently drop all WiFi results.
                        if (updated.isNotEmpty()) onBandsChange(updated)
                    },
                    label = { Text("$band GHz") },
                )
            }
        }
    }
}

@Composable
private fun AutoExportRow(
    intervalMin: Int,
    onChange: (Int) -> Unit,
) {
    Column {
        val label = if (intervalMin <= 0) {
            stringResource(R.string.auto_export_off)
        } else {
            stringResource(R.string.auto_export_every, intervalMin)
        }
        Text(text = label, fontWeight = FontWeight.Medium)
        Slider(
            value = intervalMin.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = 0f..120f,
        )
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ExportFormatDropdown(
    selected: ExportFormat,
    onSelected: (ExportFormat) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = stringResource(R.string.export_format))
        TextButton(onClick = { open = true }) {
            Text(selected.label)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            ExportFormat.entries.forEach { format ->
                DropdownMenuItem(
                    text = { Text(format.label) },
                    onClick = {
                        onSelected(format)
                        open = false
                    },
                )
            }
        }
    }
}
