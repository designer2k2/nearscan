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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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

@Composable
fun AdvancedSettingsCard(
    settings: NearScanSettings,
    onSettingsChange: (NearScanSettings) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    isExporting: Boolean,
    onExportNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                        onEnabledChange = { onSettingsChange(settings.copy(scanWifiEnabled = it)) },
                        onIntervalChange = { onSettingsChange(settings.copy(intervalWifiSec = it)) },
                    )
                    ScanTypeRow(
                        label = stringResource(R.string.scan_bt),
                        enabled = settings.scanBtEnabled,
                        intervalSec = settings.intervalBtSec,
                        onEnabledChange = { onSettingsChange(settings.copy(scanBtEnabled = it)) },
                        onIntervalChange = { onSettingsChange(settings.copy(intervalBtSec = it)) },
                    )
                    ScanTypeRow(
                        label = stringResource(R.string.scan_ble),
                        enabled = settings.scanBleEnabled,
                        intervalSec = settings.intervalBleSec,
                        onEnabledChange = { onSettingsChange(settings.copy(scanBleEnabled = it)) },
                        onIntervalChange = { onSettingsChange(settings.copy(intervalBleSec = it)) },
                    )
                    ScanTypeRow(
                        label = stringResource(R.string.scan_cell),
                        enabled = settings.scanCellEnabled,
                        intervalSec = settings.intervalCellSec,
                        onEnabledChange = { onSettingsChange(settings.copy(scanCellEnabled = it)) },
                        onIntervalChange = { onSettingsChange(settings.copy(intervalCellSec = it)) },
                    )

                    ExportFormatDropdown(
                        selected = settings.exportFormat,
                        onSelected = { onSettingsChange(settings.copy(exportFormat = it)) },
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
                }
            }
        }
    }
}

@Composable
private fun ScanTypeRow(
    label: String,
    enabled: Boolean,
    intervalSec: Int,
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
            valueRange = 1f..300f,
            enabled = enabled,
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
