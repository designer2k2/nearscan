package at.designer2k2.nearscan.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds BLE advertising-data columns to `bt_scans` (nullable, so existing rows are unaffected).
 * Column names must match [BtScanEntity]'s Kotlin property names exactly — there's no
 * `@ColumnInfo` mapping anywhere in this schema, so Room uses the property name verbatim.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE bt_scans ADD COLUMN serviceUuids TEXT")
        db.execSQL("ALTER TABLE bt_scans ADD COLUMN manufacturerData TEXT")
    }
}

/**
 * Adds the optional self-logged "extra fields" (battery, screen, network, sensors, memory,
 * scan duration) as nullable columns to all three scan tables, so they can be persisted per
 * row instead of only reaching MQTT payloads.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        for (table in listOf("wifi_scans", "bt_scans", "cell_scans")) {
            db.execSQL("ALTER TABLE $table ADD COLUMN extraBatteryLevel INTEGER")
            db.execSQL("ALTER TABLE $table ADD COLUMN extraBatteryCharging INTEGER")
            db.execSQL("ALTER TABLE $table ADD COLUMN extraBatteryTemperature REAL")
            db.execSQL("ALTER TABLE $table ADD COLUMN extraScreenOn INTEGER")
            db.execSQL("ALTER TABLE $table ADD COLUMN extraMobileDataActive INTEGER")
            db.execSQL("ALTER TABLE $table ADD COLUMN extraActiveNetworkType TEXT")
            db.execSQL("ALTER TABLE $table ADD COLUMN extraConnectedSsid TEXT")
            db.execSQL("ALTER TABLE $table ADD COLUMN extraHeading REAL")
            db.execSQL("ALTER TABLE $table ADD COLUMN extraTilt REAL")
            db.execSQL("ALTER TABLE $table ADD COLUMN extraScanDurationMs INTEGER")
            db.execSQL("ALTER TABLE $table ADD COLUMN extraMemoryAvailableMb INTEGER")
        }
    }
}

/** Adds registration status + per-technology signal-quality columns to `cell_scans`. */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE cell_scans ADD COLUMN isRegistered INTEGER")
        db.execSQL("ALTER TABLE cell_scans ADD COLUMN asuLevel INTEGER")
        db.execSQL("ALTER TABLE cell_scans ADD COLUMN signalLevel INTEGER")
        db.execSQL("ALTER TABLE cell_scans ADD COLUMN rsrp INTEGER")
        db.execSQL("ALTER TABLE cell_scans ADD COLUMN rsrq INTEGER")
        db.execSQL("ALTER TABLE cell_scans ADD COLUMN snr INTEGER")
        db.execSQL("ALTER TABLE cell_scans ADD COLUMN ecNo INTEGER")
        db.execSQL("ALTER TABLE cell_scans ADD COLUMN bitErrorRate INTEGER")
        db.execSQL("ALTER TABLE cell_scans ADD COLUMN timingAdvance INTEGER")
    }
}
