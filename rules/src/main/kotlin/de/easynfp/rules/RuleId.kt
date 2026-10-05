package de.easynfp.rules

/**
 * Identifiers of the Sensiplan rules as documented in docs/rules.md.
 * Every explanation step references the rule that produced it (F-41).
 */
public enum class RuleId(public val code: String) {
    R01("R-01"),
    R02("R-02"),
    R03("R-03"),
    R04("R-04"),
    R05("R-05"),
    R06("R-06"),
    R07("R-07"),
    R10("R-10"),
    R11("R-11"),
    R12("R-12"),
    R13("R-13"),
    R14("R-14"),
    R15("R-15"),
    R16("R-16"),
    R20("R-20"),
    R21("R-21"),
    R22("R-22"),
    R30("R-30"),
    R31("R-31"),
    R32("R-32"),
    R33("R-33"),
    R34("R-34"),

    /** F-42: manual override by the user. */
    MANUAL("F-42"),
}
