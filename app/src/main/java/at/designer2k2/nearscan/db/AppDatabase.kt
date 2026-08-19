package at.designer2k2.nearscan.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        WifiScanEntity::class,
        BtScanEntity::class,
        CellScanEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wifiScanDao(): WifiScanDao
    abstract fun btScanDao(): BtScanDao
    abstract fun cellScanDao(): CellScanDao

    companion object {
        const val NAME = "nearscan.db"
    }
}
