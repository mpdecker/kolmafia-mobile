package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [QuestManager.parseProtonicGhost] fight / walkie-talkie HTML.
 */
object ProtonicGhostSync {

    const val PROTON_ACCELERATOR = 9082
    const val WALKIE_TALKIE = 9083

    private val PARANORMAL_PATTERN =
        Regex("""(?:&quot;|")Paranormal disturbance reported (.*?)\.(?:&quot;|")""")

    private const val CRACKLE =
        "The walkie-talkie on your proton accelerator crackles to life"

    fun hasPackEquipped(
        equipment: Map<EquipmentSlot, String>,
        itemName: (Int) -> String = { ItemDatabase.getItemName(it) },
    ): Boolean {
        val name = itemName(PROTON_ACCELERATOR)
        if (name.isBlank()) return false
        return equipment.values.any { it.equals(name, ignoreCase = true) }
    }

    fun applyFromFight(
        html: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
        turnsPlayed: Int,
        equipment: Map<EquipmentSlot, String>,
        itemName: (Int) -> String = { ItemDatabase.getItemName(it) },
    ): Boolean {
        if (!hasPackEquipped(equipment, itemName)) return false
        return parse(html, questDatabase, preferences, turnsPlayed)
    }

    fun applyFromWalkieTalkie(
        html: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
        turnsPlayed: Int,
    ): Boolean = parse(html, questDatabase, preferences, turnsPlayed)

    fun parse(
        html: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
        turnsPlayed: Int,
    ): Boolean {
        if (questDatabase == null || preferences == null) return false
        var ghostLocation: String? = null
        for (match in PARANORMAL_PATTERN.findAll(html)) {
            ghostLocation = canonicalLocation(match.groupValues[1]) ?: ghostLocation
        }
        if (ghostLocation == null && html.contains(CRACKLE)) {
            ghostLocation = preferences.getString("ghostLocation", "").takeIf { it.isNotBlank() }
        }
        if (ghostLocation == null) return false
        questDatabase.setProgress(Quest.GHOST, QuestDatabase.STARTED)
        preferences.setString("ghostLocation", ghostLocation)
        preferences.setInt("nextParanormalActivity", turnsPlayed + 51)
        return true
    }

    internal fun canonicalLocation(reported: String): String? = when {
        reported.contains("Overgrown Lot") -> "The Overgrown Lot"
        reported.contains("Skeleton Store") -> "The Skeleton Store"
        reported.contains("Madness Bakery") -> "Madness Bakery"
        reported.contains("Spooky Forest") -> "The Spooky Forest"
        reported.contains("Kitchen") -> "The Haunted Kitchen"
        reported.contains("Knob Treasury") -> "Cobb's Knob Treasury"
        reported.contains("Conservatory") -> "The Haunted Conservatory"
        reported.contains("Landfill") -> "The Old Landfill"
        reported.contains("Icy Peak") -> "The Icy Peak"
        reported.contains("Smut Orc Logging Camp") -> "The Smut Orc Logging Camp"
        reported.contains("Gallery") -> "The Haunted Gallery"
        reported.contains("Palindome") -> "Inside the Palindome"
        reported.contains("Wine Cellar") -> "The Haunted Wine Cellar"
        else -> null
    }
}
