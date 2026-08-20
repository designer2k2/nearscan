package at.designer2k2.nearscan.export

import android.content.Context
import at.designer2k2.nearscan.db.AppDatabase
import at.designer2k2.nearscan.db.BtScanDao
import at.designer2k2.nearscan.db.CellScanDao
import at.designer2k2.nearscan.db.WifiScanDao
import at.designer2k2.nearscan.prefs.ExportFormat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.io.File
import java.util.zip.GZIPOutputStream
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
    /** Generates the requested format, then gzips it — all formats compress well and sessions can get large. */
    suspend fun export(format: ExportFormat, outputDir: File): File {
        val file = when (format) {
            ExportFormat.WIGLE_CSV -> wigleCsvExporter.export(outputDir)
            ExportFormat.CUSTOM_CSV -> customCsvExporter.export(outputDir)
            ExportFormat.GEOJSON -> geoJsonExporter.export(outputDir)
            ExportFormat.SQLITE -> exportSqlite(outputDir)
        }
        return gzip(file)
    }

    /** Compresses [source] to a sibling `<name>.gz` file and deletes the uncompressed original. */
    private fun gzip(source: File): File {
        val target = File(source.parentFile, "${source.name}.gz")
        GZIPOutputStream(target.outputStream()).use { gz ->
            source.inputStream().use { input -> input.copyTo(gz) }
        }
        source.delete()
        return target
    }

    /**
     * The user's configured output folder, or the app-specific external files dir (`.../NearScan`)
     * if none was set. Shared by every export entry point (manual, Tasker `CMD_EXPORT`, and the
     * auto-export scheduler) so they can't drift out of sync with each other.
     */
    fun resolveOutputDir(outputFolder: String): File =
        if (outputFolder.isNotBlank()) File(outputFolder)
        else File(context.getExternalFilesDir(null), "NearScan")

    /** Sum of all records across the three scan tables. */
    suspend fun totalRecordCount(): Long =
        wifiScanDao.count().first() + btScanDao.count().first() + cellScanDao.count().first()

    /** Copies the Room database file directly to [outputDir] with a timestamped name. */
    private fun exportSqlite(outputDir: File): File {
        if (!outputDir.exists()) outputDir.mkdirs()
        val dbFile = context.getDatabasePath(AppDatabase.NAME)
        val target = File(outputDir, "nearscan_db_${System.currentTimeMillis()}.sqlite")
        dbFile.copyTo(target, overwrite = true)
        return target
    }
}
