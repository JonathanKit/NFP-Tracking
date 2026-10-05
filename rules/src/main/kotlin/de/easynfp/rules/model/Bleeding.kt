package de.easynfp.rules.model

/** F-07: bleeding intensity. */
public enum class Bleeding {
    SPOTTING,
    LIGHT,
    MEDIUM,
    HEAVY,
    ;

    /** Spotting alone does not mark a new period. */
    public val isPeriodBleeding: Boolean get() = this != SPOTTING
}
