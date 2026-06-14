package at.designer2k2.nearscan.ui

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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import at.designer2k2.nearscan.R

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showLocationDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { /* TODO: open settings */ }) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.settings),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.primary,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            LocationRow(
                latitude = state.latitude,
                longitude = state.longitude,
                onSetLocation = { showLocationDialog = true },
            )

            StartStopButton(
                isRunning = state.isRunning,
                onStart = viewModel::startScan,
                onStop = viewModel::stopScan,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CounterCard(
                    label = stringResource(R.string.counter_wifi),
                    count = state.wifiCount,
                    modifier = Modifier.weight(1f),
                )
                CounterCard(
                    label = stringResource(R.string.counter_bt),
                    count = state.btCount,
                    modifier = Modifier.weight(1f),
                )
                CounterCard(
                    label = stringResource(R.string.counter_cell),
                    count = state.cellCount,
                    modifier = Modifier.weight(1f),
                )
            }

            SessionStats(
                sessionSeconds = state.sessionSeconds,
                totalRecords = state.totalRecords,
            )

            AdvancedSettingsCard(
                settings = state.settings,
                onSettingsChange = viewModel::updateSettings,
            )
        }
    }

    if (showLocationDialog) {
        LocationDialog(
            initialLat = state.latitude,
            initialLon = state.longitude,
            initialAlt = state.altitude,
            gpsFix = state.gpsFixResult,
            onGpsFixConsumed = viewModel::consumeGpsFix,
            isGettingFix = state.isGettingFix,
            onGetGpsFix = viewModel::getGpsFix,
            onSave = { lat, lon, alt ->
                viewModel.updateLocation(lat, lon, alt)
                showLocationDialog = false
            },
            onDismiss = { showLocationDialog = false },
        )
    }
}

@Composable
private fun LocationRow(
    latitude: Double?,
    longitude: Double?,
    onSetLocation: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val coords = if (latitude != null && longitude != null) {
                "%.5f, %.5f".format(latitude, longitude)
            } else {
                stringResource(R.string.no_location_set)
            }
            Text(text = coords, style = MaterialTheme.typography.bodyLarge)
            OutlinedButton(onClick = onSetLocation) {
                Text(stringResource(R.string.set_location))
            }
        }
    }
}

@Composable
private fun StartStopButton(
    isRunning: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    Button(
        onClick = { if (isRunning) onStop() else onStart() },
        modifier = Modifier
            .fillMaxWidth()
            .size(width = 0.dp, height = 96.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isRunning) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            },
        ),
    ) {
        Text(
            text = if (isRunning) stringResource(R.string.stop) else stringResource(R.string.start),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun CounterCard(
    label: String,
    count: Int,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = count.toString(),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(text = label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun SessionStats(
    sessionSeconds: Long,
    totalRecords: Long,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = stringResource(R.string.session_timer),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(text = formatDuration(sessionSeconds), style = MaterialTheme.typography.titleLarge)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.total_records),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(text = totalRecords.toString(), style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return "%02d:%02d:%02d".format(h, m, s)
}
