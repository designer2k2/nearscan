package at.designer2k2.nearscan.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WifiScanDao {
    @Insert
    suspend fun insertAll(rows: List<WifiScanEntity>)

    @Query("SELECT COUNT(*) FROM wifi_scans")
    fun count(): Flow<Long>

    @Query("SELECT * FROM wifi_scans ORDER BY timestamp ASC LIMIT :limit OFFSET :offset")
    suspend fun getPage(limit: Int, offset: Int): List<WifiScanEntity>

    /** Most recent [limit] rows, newest first — for the ContentProvider's bounded live queries. */
    @Query("SELECT * FROM wifi_scans ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<WifiScanEntity>

    @Query("DELETE FROM wifi_scans WHERE timestamp < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)
}

@Dao
interface BtScanDao {
    @Insert
    suspend fun insertAll(rows: List<BtScanEntity>)

    @Query("SELECT COUNT(*) FROM bt_scans")
    fun count(): Flow<Long>

    @Query("SELECT * FROM bt_scans ORDER BY timestamp ASC LIMIT :limit OFFSET :offset")
    suspend fun getPage(limit: Int, offset: Int): List<BtScanEntity>

    /** Most recent [limit] rows, newest first — for the ContentProvider's bounded live queries. */
    @Query("SELECT * FROM bt_scans ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<BtScanEntity>

    @Query("DELETE FROM bt_scans WHERE timestamp < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)
}

@Dao
interface CellScanDao {
    @Insert
    suspend fun insertAll(rows: List<CellScanEntity>)

    @Query("SELECT COUNT(*) FROM cell_scans")
    fun count(): Flow<Long>

    @Query("SELECT * FROM cell_scans ORDER BY timestamp ASC LIMIT :limit OFFSET :offset")
    suspend fun getPage(limit: Int, offset: Int): List<CellScanEntity>

    /** Most recent [limit] rows, newest first — for the ContentProvider's bounded live queries. */
    @Query("SELECT * FROM cell_scans ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<CellScanEntity>

    @Query("DELETE FROM cell_scans WHERE timestamp < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)
}
