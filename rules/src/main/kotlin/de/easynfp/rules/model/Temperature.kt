package de.easynfp.rules.model

/**
 * A basal body temperature in hundredths of a degree Celsius (36.55 °C == 3655).
 *
 * Integer arithmetic avoids floating point surprises in rule comparisons.
 */
@JvmInline
public value class Temperature(public val hundredths: Int) : Comparable<Temperature> {

    /** R-01: values are rounded to 0.05 °C for evaluation. 36.21–36.22 → 36.20, 36.23–36.24 → 36.25. */
    public fun roundedForEvaluation(): Temperature = Temperature(((hundredths + 2) / ROUNDING_STEP) * ROUNDING_STEP)

    public val isInPlausibleRange: Boolean get() = hundredths in MIN.hundredths..MAX.hundredths

    public operator fun plus(other: Temperature): Temperature = Temperature(hundredths + other.hundredths)

    public operator fun minus(other: Temperature): Temperature = Temperature(hundredths - other.hundredths)

    override fun compareTo(other: Temperature): Int = hundredths.compareTo(other.hundredths)

    /** Locale independent representation, e.g. "36.55". */
    override fun toString(): String {
        val sign = if (hundredths < 0) "-" else ""
        val abs = kotlin.math.abs(hundredths)
        return "$sign${abs / HUNDRED}.${(abs % HUNDRED).toString().padStart(2, '0')}"
    }

    public companion object {
        private const val ROUNDING_STEP = 5
        private const val HUNDRED = 100

        /** F-01: lower bound of the accepted input range. */
        public val MIN: Temperature = Temperature(3400)

        /** F-01: upper bound of the accepted input range. */
        public val MAX: Temperature = Temperature(4200)

        /** R-04, R-06: "mindestens 0,2 °C über der Hilfslinie". */
        public val TWO_TENTHS: Temperature = Temperature(20)

        /** Parses "36.55", "36,55" or "36.5". Returns null for anything else. */
        public fun parse(text: String): Temperature? {
            val normalized = text.trim().replace(',', '.')
            val match = Regex("""^(\d{1,2})(?:\.(\d{1,2}))?$""").matchEntire(normalized) ?: return null
            val whole = match.groupValues[1].toInt()
            val fraction = match.groupValues[2].padEnd(2, '0').ifEmpty { "00" }.toInt()
            return Temperature(whole * HUNDRED + fraction)
        }
    }
}
