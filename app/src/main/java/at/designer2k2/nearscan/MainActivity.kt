package at.designer2k2.nearscan

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import at.designer2k2.nearscan.ui.MainScreen
import at.designer2k2.nearscan.ui.MainViewModel
import at.designer2k2.nearscan.ui.theme.NearScanTheme
import at.designer2k2.nearscan.util.RequiredPermissions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        requestRequiredPermissions()

        setContent {
            NearScanTheme {
                Surface(
                    // Lets UI Automator / Robo Test see our `Modifier.testTag(...)` values as the
                    // element's `resource-id`, so a `--robo-directives ignore:<tag>` (or the
                    // Firebase console's Robo Directives field) can target them. Used to keep
                    // Robo's autonomous crawler off the export/share button — tapping it opens a
                    // real Android share sheet, and the crawler has wandered into Quick Share's
                    // account-registration flow there before, stalling the whole test run.
                    modifier = Modifier.fillMaxSize().semantics { testTagsAsResourceId = true },
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

    companion object {
        private const val REQUEST_CODE = 1001
    }
}
