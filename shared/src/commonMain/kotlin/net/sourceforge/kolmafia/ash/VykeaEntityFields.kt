package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.modifiers.VykeaCompanionData

/**
 * Resolves `$vykea[field]` bracket access. Mirrors desktop [VykeaProxy].
 */
internal object VykeaEntityFields {

    private const val VYKEA_FRENZY_RUNE = 8722
    private const val VYKEA_BLOOD_RUNE = 8723
    private const val VYKEA_LIGHTNING_RUNE = 8724

    fun resolve(vykeaName: String, fieldName: String): AshValue {
        val companion = VykeaCompanionData.companionFor(vykeaName)
            ?: throw ScriptException("invalid vykea '$vykeaName'")
        val catalogName = VykeaCompanionData.resolve(vykeaName) ?: vykeaName
        return when (fieldName.lowercase()) {
            "id" -> AshValue.of(catalogId(catalogName).toLong())
            "name" -> AshValue.of(companion.name.ifBlank { catalogName })
            "type" -> AshValue.of(typeLabel(companion.type))
            "level" -> AshValue.of(companion.level.toLong())
            "modifiers" -> AshValue.of(companion.modifiers)
            "rune" -> AshValue(AshType.ITEM, runeItemName(companion.rune))
            "image" -> AshValue.EMPTY_STRING
            "attack_element" -> AshValue(AshType.ELEMENT, "none")
            else -> throw ScriptException("vykea has no field '$fieldName'")
        }
    }

    private fun catalogId(catalogName: String): Int {
        val index = VykeaCompanionData.catalog.indexOfFirst { it.equals(catalogName, ignoreCase = true) }
        return if (index >= 0) index + 1 else 0
    }

    private fun typeLabel(type: VykeaCompanionData.VykeaType): String = when (type) {
        VykeaCompanionData.VykeaType.BOOKSHELF -> "bookshelf"
        VykeaCompanionData.VykeaType.CEILING_FAN -> "ceiling fan"
        VykeaCompanionData.VykeaType.COUCH -> "couch"
        VykeaCompanionData.VykeaType.DISHRACK -> "dishrack"
        VykeaCompanionData.VykeaType.DRESSER -> "dresser"
        VykeaCompanionData.VykeaType.LAMP -> "lamp"
        VykeaCompanionData.VykeaType.NONE -> "unknown"
    }

    private fun runeItemName(rune: VykeaCompanionData.VykeaRune): String = when (rune) {
        VykeaCompanionData.VykeaRune.FRENZY -> itemNameForId(VYKEA_FRENZY_RUNE)
        VykeaCompanionData.VykeaRune.BLOOD -> itemNameForId(VYKEA_BLOOD_RUNE)
        VykeaCompanionData.VykeaRune.LIGHTNING -> itemNameForId(VYKEA_LIGHTNING_RUNE)
        VykeaCompanionData.VykeaRune.NONE -> ""
    }

    private fun itemNameForId(itemId: Int): String = when (itemId) {
        VYKEA_FRENZY_RUNE -> "VYKEA frenzy rune"
        VYKEA_BLOOD_RUNE -> "VYKEA blood rune"
        VYKEA_LIGHTNING_RUNE -> "VYKEA lightning rune"
        else -> ""
    }
}
