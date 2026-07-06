package at.designer2k2.nearscan.location

/** A single acquired GPS fix, including its reported accuracy for display in the location dialog. */
data class GpsFix(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val accuracyMeters: Float?,
)
