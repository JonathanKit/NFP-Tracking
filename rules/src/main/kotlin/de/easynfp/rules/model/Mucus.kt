package de.easynfp.rules.model

/** F-05: what the user felt at the vulva during the day. */
public enum class MucusSensation {
    /** trocken */
    DRY,

    /** nichts gefühlt */
    NOTHING_FELT,

    /** feucht */
    MOIST,

    /** nass / schlüpfrig */
    WET_SLIPPERY,
}

/** F-05: what the user saw during the day. */
public enum class MucusAppearance {
    /** nichts gesehen */
    NOTHING_SEEN,

    /** dicklich, weißlich, trüb, cremig, klumpig, gelblich */
    THICK,

    /** glasig, dehnbar, fadenziehend, flüssig */
    CLEAR_STRETCHY,
}

/**
 * R-10: mucus categories in ascending order of quality: t < ø < f < S < S+.
 * The enum order is the ranking, so `compareTo` can be used directly.
 */
public enum class MucusCategory(public val symbol: String) {
    DRY("t"),
    NOTHING("ø"),
    MOIST("f"),
    S("S"),
    S_PLUS("S+"),
    ;

    /** R-34: f, S and S+ end the infertile phase at the beginning of the cycle. */
    public val isFertileQuality: Boolean get() = this >= MOIST

    public companion object {
        /**
         * R-11: derives the category of a day from its best observation.
         *
         * Returns null when nothing usable was recorded. A missing sensation combined with
         * "nothing seen" is not enough to decide between t, ø and f, so it yields null as well.
         */
        public fun derive(sensation: MucusSensation?, appearance: MucusAppearance?): MucusCategory? {
            if (sensation == MucusSensation.WET_SLIPPERY || appearance == MucusAppearance.CLEAR_STRETCHY) {
                return S_PLUS
            }
            if (appearance == MucusAppearance.THICK) return S
            return when (sensation) {
                MucusSensation.DRY -> DRY
                MucusSensation.NOTHING_FELT -> NOTHING
                MucusSensation.MOIST -> MOIST
                MucusSensation.WET_SLIPPERY -> S_PLUS
                null -> null
            }
        }
    }
}
