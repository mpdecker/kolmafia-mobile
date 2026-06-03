package net.sourceforge.kolmafia.modifiers

enum class BooleanModifier(val tag: String) {
    SOFTCORE("Softcore Only"),
    SINGLE("Single Equip"),
    ALWAYS_FUMBLE("Always Fumble"),
    NEVER_FUMBLE("Never Fumble"),
    WEAKENS("Weakens Monster"),
    FREE_PULL("Free Pull"),
    VARIABLE("Variable"),
    NONSTACKABLE_WATCH("Nonstackable Watch"),
    COLD_IMMUNITY("Cold Immunity"),
    HOT_IMMUNITY("Hot Immunity"),
    SLEAZE_IMMUNITY("Sleaze Immunity"),
    SPOOKY_IMMUNITY("Spooky Immunity"),
    STENCH_IMMUNITY("Stench Immunity"),
    COLD_VULNERABILITY("Cold Vulnerability"),
    HOT_VULNERABILITY("Hot Vulnerability"),
    SLEAZE_VULNERABILITY("Sleaze Vulnerability"),
    SPOOKY_VULNERABILITY("Spooky Vulnerability"),
    STENCH_VULNERABILITY("Stench Vulnerability"),
    MOXIE_CONTROLS_MP("Moxie Controls MP"),
    MOXIE_MAY_CONTROL_MP("Moxie May Control MP"),
    FOUR_SONGS("Four Songs"),
    ADVENTURE_RANDOMLY("Adventure Randomly"),
    ADVENTURE_UNDERWATER("Adventure Underwater"),
    UNDERWATER_FAMILIAR("Underwater Familiar"),
    GENERIC("Generic"),
    NOPULL("No Pull"),
    LASTS_ONE_DAY("Lasts Until Rollover"),
    ALTERS_PAGE_TEXT("Alters Page Text"),
    ATTACKS_CANT_MISS("Attacks Can't Miss"),
    LOOK_LIKE_A_PIRATE("Pirate"),
    BLIND("Blind"),
    BREAKABLE("Breakable"),
    DROPS_ITEMS("Drops Items"),
    DROPS_MEAT("Drops Meat"),
    VOLLEYBALL_OR_SOMBRERO("Volleyball or Sombrero"),
    EXTRA_PICKPOCKET("Extra Pickpocket"),
    NEGATIVE_STATUS_RESIST("Negative Status Resist"),
    WEAKENS_MONSTER_ON_CRITICAL_HIT("Weakens Monster on Critical Hit");

    companion object {
        private val byTagLower: Map<String, BooleanModifier> =
            entries.associateBy { it.tag.lowercase() }

        fun byTag(tag: String): BooleanModifier? = byTagLower[tag.lowercase()]
    }
}
