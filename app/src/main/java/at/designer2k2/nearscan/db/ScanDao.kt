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

    @Query("SELECT * FROM wifi_scans ORDER BY timestamp ASC")
    suspend fun getAll(): List<WifiScanEntity>

    @Query("DELETE FROM wifi_scans WHERE timestamp < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)
}

@Dao
interface BtScanDao {
    @Insert
    suspend fun insertAll(rows: List<BtScanEntity>)

    @Query("SELECT COUNT(*) FROM bt_scans")
    fun count(): Flow<Long>

    @Query("SELECT * FROM bt_scans ORDER BY timestamp ASC")
    suspend fun getAll(): List<BtScanEntity>

    @Query("DELETE FROM bt_scans WHERE timestamp < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)
}

@Dao
interface CellScanDao {
    @Insert
    suspend fun insertAll(rows: List<CellScanEntity>)

    @Query("SELECT COUNT(*) FROM cell_scans")
    fun count(): Flow<Long>

    @Query("SELECT * FROM cell_scans ORDER BY timestamp ASC")
    suspend fun getAll(): List<CellScanEntity>

    @Query("DELETE FROM cell_scans WHERE timestamp < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)
}
