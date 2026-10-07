package de.easynfp.rules.model

/**
 * F-05: what the user felt at the vulva during the day, ordered from worst to best quality.
 * The finer steps only help recording; the category follows R-11.
 */
public enum class MucusSensation(internal val category: MucusCategory) {
    /** trocken */
    DRY(MucusCategory.DRY),

    /** nichts gefühlt */
    NOTHING_FELT(MucusCategory.NOTHING),

    /** feucht */
    MOIST(MucusCategory.MOIST),

    /** nass */
    WET(MucusCategory.S_PLUS),

    /** schlüpfrig, glitschig */
    WET_SLIPPERY(MucusCategory.S_PLUS),
}

/**
 * F-05: what the user saw during the day, ordered from worst to best quality.
 * [category] is null for "nothing seen": then the sensation decides (R-11).
 */
public enum class MucusAppearance(internal val category: MucusCategory?) {
    /** nichts gesehen */
    NOTHING_SEEN(null),

    /** weißlich, trüb */
    WHITISH_CLOUDY(MucusCategory.S),

    /** dicklich, cremig */
    THICK(MucusCategory.S),

    /** klumpig, gelblich */
    LUMPY_YELLOWISH(MucusCategory.S),

    /** glasig, durchsichtig */
    GLASSY(MucusCategory.S_PLUS),

    /** dehnbar, fadenziehend */
    CLEAR_STRETCHY(MucusCategory.S_PLUS),

    /** flüssig, wässrig */
    LIQUID(MucusCategory.S_PLUS),
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
         * S+ if the sensation is wet/slippery or the appearance glassy, stretchy or liquid; S if the
         * appearance is whitish, thick or lumpy; otherwise the sensation decides (t, ø, f).
         * Returns null when nothing usable was recorded. A missing sensation combined with
         * "nothing seen" is not enough to decide between t, ø and f, so it yields null as well.
         */
        public fun derive(sensation: MucusSensation?, appearance: MucusAppearance?): MucusCategory? {
            val fromAppearance = appearance?.category
            val fromSensation = sensation?.category
            if (fromAppearance == null) return fromSensation
            return if (fromSensation != null) maxOf(fromSensation, fromAppearance) else fromAppearance
        }
    }
}
