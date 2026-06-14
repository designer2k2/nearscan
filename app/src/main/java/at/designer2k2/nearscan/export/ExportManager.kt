package at.designer2k2.nearscan.export

import at.designer2k2.nearscan.prefs.ExportFormat
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Orchestrates exporting the logged database into the user-selected [ExportFormat]. */
@Singleton
class ExportManager @Inject constructor(
    private val wigleCsvExporter: WigleCsvExporter,
) {
    suspend fun export(format: ExportFormat, outputDir: File): File {
        return when (format) {
            ExportFormat.WIGLE_CSV -> wigleCsvExporter.export(outputDir)
            ExportFormat.CUSTOM_CSV -> TODO("Custom CSV export not yet implemented")
            ExportFormat.GEOJSON -> TODO("GeoJSON export not yet implemented")
            ExportFormat.SQLITE -> TODO("SQLite dump export not yet implemented")
        }
    }
}
