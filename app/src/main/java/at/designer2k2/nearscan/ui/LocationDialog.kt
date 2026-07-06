package at.designer2k2.nearscan.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import at.designer2k2.nearscan.R
import at.designer2k2.nearscan.location.GpsFix

@Composable
fun LocationDialog(
    initialLat: Double?,
    initialLon: Double?,
    initialAlt: Double?,
    gpsFix: GpsFix? = null,
    onGpsFixConsumed: () -> Unit = {},
    isGettingFix: Boolean = false,
    onGetGpsFix: () -> Unit,
    onSave: (lat: Double, lon: Double, alt: Double) -> Unit,
    onDismiss: () -> Unit,
) {
    var lat by remember { mutableStateOf(initialLat?.toString() ?: "") }
    var lon by remember { mutableStateOf(initialLon?.toString() ?: "") }
    var alt by remember { mutableStateOf(initialAlt?.toString() ?: "") }
    var accuracyMeters by remember { mutableStateOf<Float?>(null) }
    var validationError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(gpsFix) {
        if (gpsFix != null) {
            lat = gpsFix.latitude.toString()
            lon = gpsFix.longitude.toString()
            alt = gpsFix.altitude.toString()
            accuracyMeters = gpsFix.accuracyMeters
            validationError = null
            onGpsFixConsumed()
        }
    }

    val decimalKeyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    val invalidLocationMessage = stringResource(R.string.location_invalid)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.set_location)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = lat,
                    onValueChange = { lat = it; validationError = null },
                    label = { Text(stringResource(R.string.latitude)) },
                    keyboardOptions = decimalKeyboard,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = lon,
                    onValueChange = { lon = it; validationError = null },
                    label = { Text(stringResource(R.string.longitude)) },
                    keyboardOptions = decimalKeyboard,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = alt,
                    onValueChange = { alt = it },
                    label = { Text(stringResource(R.string.altitude)) },
                    keyboardOptions = decimalKeyboard,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = onGetGpsFix,
                        enabled = !isGettingFix,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.get_gps_fix))
                    }
                    if (isGettingFix) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
                accuracyMeters?.let { accuracy ->
                    Text(
                        text = stringResource(R.string.gps_accuracy, accuracy),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                validationError?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val latV = lat.toDoubleOrNull()
                    val lonV = lon.toDoubleOrNull()
                    val altV = alt.toDoubleOrNull() ?: 0.0
                    if (latV != null && lonV != null) {
                        onSave(latV, lonV, altV)
                    } else {
                        validationError = invalidLocationMessage
                    }
                },
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
