package net.sourceforge.kolmafia.modifiers

enum class StringModifier(val tag: String, val multiple: Boolean = false) {
    CLASS("Class"),
    INTRINSIC_EFFECT("Intrinsic Effect"),
    EQUALIZE("Equalize"),
    WIKI_NAME("Wiki Name"),
    MODIFIERS("Modifiers"),
    OUTFIT("Outfit"),
    STAT_TUNING("Stat Tuning"),
    EFFECT("Effect", multiple = true),
    EQUIPS_ON("Equips On"),
    FAMILIAR_EFFECT("Familiar Effect"),
    JIGGLE("Jiggle"),
    EQUALIZE_MUSCLE("Equalize Muscle"),
    EQUALIZE_MYST("Equalize Mysticality"),
    EQUALIZE_MOXIE("Equalize Moxie"),
    AVATAR("Avatar"),
    ROLLOVER_EFFECT("Rollover Effect", multiple = true),
    SKILL("Skill"),
    FLOOR_BUFFED_MUSCLE("Floor Buffed Muscle"),
    FLOOR_BUFFED_MYST("Floor Buffed Mysticality"),
    FLOOR_BUFFED_MOXIE("Floor Buffed Moxie"),
    PLUMBER_STAT("Plumber Stat"),
    RECIPE("Recipe"),
    EVALUATED_MODIFIERS("Evaluated Modifiers"),
    LAST_AVAILABLE_DATE("Last Available"),
    CONDITIONAL_SKILL_EQUIPPED("Conditional Skill (Equipped)", multiple = true),
    CONDITIONAL_SKILL_INVENTORY("Conditional Skill (Inventory)", multiple = true),
    LANTERN_ELEMENT("Lantern Element", multiple = true);

    companion object {
        private val byTagLower: Map<String, StringModifier> =
            entries.associateBy { it.tag.lowercase() }

        fun byTag(tag: String): StringModifier? = byTagLower[tag.lowercase()]
    }
}
