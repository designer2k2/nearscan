package at.designer2k2.nearscan.export

import android.content.Context
import at.designer2k2.nearscan.db.AppDatabase
import at.designer2k2.nearscan.db.BtScanDao
import at.designer2k2.nearscan.db.CellScanDao
import at.designer2k2.nearscan.db.WifiScanDao
import at.designer2k2.nearscan.prefs.ExportFormat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Orchestrates exporting the logged database into the user-selected [ExportFormat]. */
@Singleton
class ExportManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val wigleCsvExporter: WigleCsvExporter,
    private val customCsvExporter: CustomCsvExporter,
    private val geoJsonExporter: GeoJsonExporter,
    private val wifiScanDao: WifiScanDao,
    private val btScanDao: BtScanDao,
    private val cellScanDao: CellScanDao,
) {
    suspend fun export(format: ExportFormat, outputDir: File): File {
        return when (format) {
            ExportFormat.WIGLE_CSV -> wigleCsvExporter.export(outputDir)
            ExportFormat.CUSTOM_CSV -> customCsvExporter.export(outputDir)
            ExportFormat.GEOJSON -> geoJsonExporter.export(outputDir)
            ExportFormat.SQLITE -> exportSqlite(outputDir)
        }
    }

    /** Copies the Room database file directly to [outputDir] with a timestamped name. */
    private fun exportSqlite(outputDir: File): File {
        if (!outputDir.exists()) outputDir.mkdirs()
        val dbFile = context.getDatabasePath(AppDatabase.NAME)
        val target = File(outputDir, "nearscan_db_${System.currentTimeMillis()}.sqlite")
        dbFile.copyTo(target, overwrite = true)
        return target
    }
}
