package at.designer2k2.nearscan

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import at.designer2k2.nearscan.prefs.SettingsDataStore
import at.designer2k2.nearscan.ui.MainScreen
import at.designer2k2.nearscan.ui.MainViewModel
import at.designer2k2.nearscan.ui.theme.NearScanTheme
import at.designer2k2.nearscan.util.RequiredPermissions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @Inject lateinit var settingsDataStore: SettingsDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        requestRequiredPermissions()
        requestBatteryOptimizationExemption()

        setContent {
            NearScanTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    MainScreen()
                }
            }
        }

        // Keep screen on while running, if enabled in settings.
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                if (state.isRunning && state.settings.keepScreenOn) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
        }
    }

    private fun requestRequiredPermissions() {
        val required = buildList {
            addAll(RequiredPermissions.forScanning())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        val toRequest = required.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (toRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, toRequest.toTypedArray(), REQUEST_CODE)
        }
    }

    /**
     * One-time prompt to exempt NearScan from battery optimization / Doze, since the whole
     * point of the app is a foreground service scanning continuously for hours or days.
     * Gated on a persisted flag so it only ever prompts once, even across app restarts.
     */
    private fun requestBatteryOptimizationExemption() {
        lifecycleScope.launch {
            val alreadyShown = settingsDataStore.settings.first().batteryOptPromptShown
            val pm = getSystemService(POWER_SERVICE) as? PowerManager
            val alreadyExempt = pm?.isIgnoringBatteryOptimizations(packageName) == true
            if (!alreadyShown && !alreadyExempt) {
                runCatching {
                    startActivity(
                        Intent(
                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.parse("package:$packageName"),
                        ),
                    )
                }
                settingsDataStore.markBatteryOptPromptShown()
            }
        }
    }

    companion object {
        private const val REQUEST_CODE = 1001
    }
}
