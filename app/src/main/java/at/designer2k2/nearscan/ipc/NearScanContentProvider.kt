package at.designer2k2.nearscan.ipc

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import at.designer2k2.nearscan.db.BtScanDao
import at.designer2k2.nearscan.db.CellScanDao
import at.designer2k2.nearscan.db.WifiScanDao
import at.designer2k2.nearscan.prefs.SettingsDataStore
import at.designer2k2.nearscan.service.ScanService
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Read-only ContentProvider that exposes live scan data to automation apps.
 *
 * Authority: at.designer2k2.nearscan.provider
 *
 * URIs:
 *   content://.../wifi   — WiFi records, newest first
 *   content://.../bt     — BT Classic + BLE records, newest first
 *   content://.../cell   — Cell records, newest first
 *   content://.../stats  — Single-row session summary
 *
 * Requires caller to hold at.designer2k2.nearscan.permission.READ_DATA (normal protection level).
 *
 * Note: query() is called on a Binder thread (not the main thread), so runBlocking is safe here.
 */
class NearScanContentProvider : ContentProvider() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Deps {
        fun wifiScanDao(): WifiScanDao
        fun btScanDao(): BtScanDao
        fun cellScanDao(): CellScanDao
        fun settingsDataStore(): SettingsDataStore
    }

    private val deps: Deps
        get() = EntryPointAccessors.fromApplication(
            context!!.applicationContext,
            Deps::class.java,
        )

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = when (MATCHER.match(uri)) {
        CODE_WIFI -> buildWifiCursor()
        CODE_BT -> buildBtCursor()
        CODE_CELL -> buildCellCursor()
        CODE_STATS -> buildStatsCursor()
        else -> null
    }

    private fun buildWifiCursor(): MatrixCursor = runBlocking {
        val rows = deps.wifiScanDao().getAll().reversed()
        MatrixCursor(COLS_WIFI).apply {
            rows.forEach { r ->
                addRow(arrayOf(r.bssid, r.ssid, r.rssi, r.channel, r.latitude, r.longitude, r.timestamp))
            }
        }
    }

    private fun buildBtCursor(): MatrixCursor = runBlocking {
        val rows = deps.btScanDao().getAll().reversed()
        MatrixCursor(COLS_BT).apply {
            rows.forEach { r ->
                addRow(arrayOf(r.address, r.name, r.rssi, r.latitude, r.longitude, r.timestamp))
            }
        }
    }

    private fun buildCellCursor(): MatrixCursor = runBlocking {
        val rows = deps.cellScanDao().getAll().reversed()
        MatrixCursor(COLS_CELL).apply {
            rows.forEach { r ->
                addRow(arrayOf(r.mcc, r.mnc, r.cid, r.rssi, r.technology, r.latitude, r.longitude, r.timestamp))
            }
        }
    }

    private fun buildStatsCursor(): MatrixCursor = runBlocking {
        val s = ScanService.statusFlow.value
        val durationS = if (s.isRunning && s.sessionStartMs > 0L)
            (System.currentTimeMillis() - s.sessionStartMs) / 1000L else 0L
        val settings = deps.settingsDataStore().settings.first()
        MatrixCursor(COLS_STATS).apply {
            addRow(arrayOf(
                if (s.isRunning) 1 else 0,
                s.wifiCount,
                s.btCount,
                s.cellCount,
                durationS,
                settings.latitude,
                settings.longitude,
            ))
        }
    }

    // Write operations not supported.
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0

    companion object {
        const val AUTHORITY = "at.designer2k2.nearscan.provider"

        private const val CODE_WIFI = 1
        private const val CODE_BT = 2
        private const val CODE_CELL = 3
        private const val CODE_STATS = 4

        private val MATCHER = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, "wifi", CODE_WIFI)
            addURI(AUTHORITY, "bt", CODE_BT)
            addURI(AUTHORITY, "cell", CODE_CELL)
            addURI(AUTHORITY, "stats", CODE_STATS)
        }

        val COLS_WIFI = arrayOf("bssid", "ssid", "rssi", "channel", "lat", "lon", "timestamp")
        val COLS_BT = arrayOf("address", "name", "rssi", "lat", "lon", "timestamp")
        val COLS_CELL = arrayOf("mcc", "mnc", "cid", "rssi", "tech", "lat", "lon", "timestamp")
        val COLS_STATS = arrayOf("is_running", "wifi_total", "bt_total", "cell_total", "duration_s", "lat", "lon")
    }
}
