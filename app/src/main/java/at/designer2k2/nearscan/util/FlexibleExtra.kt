package at.designer2k2.nearscan.util

/**
 * Parses a numeric Intent extra that may have arrived as a String — Tasker's Send Intent action
 * sends extras as plain strings by default (e.g. `lat:%LOCN`), so reading them with
 * `Intent.getDoubleExtra`/`getIntExtra` alone silently returns the typed getter's default value
 * instead of the real one, since those getters type-check and don't parse strings.
 *
 * Pure Kotlin (no Android deps) so this fallback logic — the actual bug fix — is unit-testable
 * without depending on a real (non-mocked) [android.content.Intent], which this project's unit
 * tests can't reliably round-trip (see the "Unit Testing Notes" section of CLAUDE.md).
 */
object FlexibleExtra {
    /** [typedValue] should be the intent's typed getter result using [Double.NaN] as its default. */
    fun parseDouble(stringValue: String?, typedValue: Double): Double? {
        stringValue?.toDoubleOrNull()?.let { return it }
        return typedValue.takeUnless { it.isNaN() }
    }

    /** [typedValue] should be the intent's typed getter result using [Int.MIN_VALUE] as its default. */
    fun parseInt(stringValue: String?, typedValue: Int): Int? {
        stringValue?.toIntOrNull()?.let { return it }
        return typedValue.takeUnless { it == Int.MIN_VALUE }
    }
}
