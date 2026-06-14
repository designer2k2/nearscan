package at.designer2k2.nearscan.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = CyanPrimary,
    onPrimary = DeepNavy,
    secondary = CyanSecondary,
    onSecondary = DeepNavy,
    background = DeepNavy,
    onBackground = OnDark,
    surface = NavySurface,
    onSurface = OnDark,
    surfaceVariant = NavySurfaceVariant,
    onSurfaceVariant = OnDarkMuted,
    error = ErrorRed,
)

@Composable
fun NearScanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // NearScan is a dark-first app; we use the dark scheme regardless.
    val colorScheme = DarkColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
