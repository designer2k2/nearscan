package at.designer2k2.nearscan.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
                    ToggleRow(
                        label = stringResource(R.string.capture_ble_advertising_data),
                        checked = settings.captureBleAdvertisingData,
                        onChange = { onSettingsChange(settings.copy(captureBleAdvertisingData = it)) },
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
                            val brokerLabel = stringResource(R.string.mqtt_broker)
                            OutlinedTextField(
                                value = settings.mqttBroker,
                                onValueChange = { onSettingsChange(settings.copy(mqttBroker = it)) },
                                label = { Text(brokerLabel) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics { contentDescription = brokerLabel },
                            )
                            val topicLabel = stringResource(R.string.mqtt_topic)
                            OutlinedTextField(
                                value = settings.mqttTopic,
                                onValueChange = { onSettingsChange(settings.copy(mqttTopic = it)) },
                                label = { Text(topicLabel) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics { contentDescription = topicLabel },
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

                    ExtraFieldsSection(settings = settings, onSettingsChange = onSettingsChange)

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
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(value = enabled, role = Role.Switch, onValueChange = onEnabledChange),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = label, fontWeight = FontWeight.Medium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "${intervalSec}s")
                Switch(
                    checked = enabled,
                    onCheckedChange = null,
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

/** Collapsible group for the optional self-logged fields (battery, screen, network, sensors, memory). */
@Composable
private fun ExtraFieldsSection(
    settings: NearScanSettings,
    onSettingsChange: (NearScanSettings) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = stringResource(R.string.extra_fields_section), fontWeight = FontWeight.Medium)
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ToggleRow(
                    label = stringResource(R.string.extra_battery_level),
                    checked = settings.extraBatteryLevel,
                    onChange = { onSettingsChange(settings.copy(extraBatteryLevel = it)) },
                )
                ToggleRow(
                    label = stringResource(R.string.extra_battery_charging),
                    checked = settings.extraBatteryCharging,
                    onChange = { onSettingsChange(settings.copy(extraBatteryCharging = it)) },
                )
                ToggleRow(
                    label = stringResource(R.string.extra_battery_temperature),
                    checked = settings.extraBatteryTemp,
                    onChange = { onSettingsChange(settings.copy(extraBatteryTemp = it)) },
                )
                ToggleRow(
                    label = stringResource(R.string.extra_screen_on),
                    checked = settings.extraScreenOn,
                    onChange = { onSettingsChange(settings.copy(extraScreenOn = it)) },
                )
                ToggleRow(
                    label = stringResource(R.string.extra_mobile_data_active),
                    checked = settings.extraMobileData,
                    onChange = { onSettingsChange(settings.copy(extraMobileData = it)) },
                )
                ToggleRow(
                    label = stringResource(R.string.extra_active_network_type),
                    checked = settings.extraNetworkType,
                    onChange = { onSettingsChange(settings.copy(extraNetworkType = it)) },
                )
                ToggleRow(
                    label = stringResource(R.string.extra_connected_ssid),
                    checked = settings.extraConnectedSsid,
                    onChange = { onSettingsChange(settings.copy(extraConnectedSsid = it)) },
                )
                ToggleRow(
                    label = stringResource(R.string.extra_heading),
                    checked = settings.extraHeading,
                    onChange = { onSettingsChange(settings.copy(extraHeading = it)) },
                )
                ToggleRow(
                    label = stringResource(R.string.extra_tilt),
                    checked = settings.extraTilt,
                    onChange = { onSettingsChange(settings.copy(extraTilt = it)) },
                )
                ToggleRow(
                    label = stringResource(R.string.extra_scan_duration),
                    checked = settings.extraScanDuration,
                    onChange = { onSettingsChange(settings.copy(extraScanDuration = it)) },
                )
                ToggleRow(
                    label = stringResource(R.string.extra_memory_available),
                    checked = settings.extraMemory,
                    onChange = { onSettingsChange(settings.copy(extraMemory = it)) },
                )
            }
        }
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
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label)
        Switch(checked = checked, onCheckedChange = null)
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
