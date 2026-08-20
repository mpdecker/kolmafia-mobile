package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.modifiers.VykeaCompanionData
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.VykeaChoiceMapper
import net.sourceforge.kolmafia.vykea.VykeaCompanionManager

/**
 * Desktop [VYKEACompanionData.assembleCompanion] for choices 1120–1123.
 */
object VykeaChoiceSync {

    val CHOICE_IDS = setOf(1120, 1121, 1122, 1123)

    private val CREATION_PATTERN = Regex(
        """<span class='guts'>.*?It's a (bookshelf|ceiling fan|couch|dishrack|dresser|lamp).*?<b>(.*?)</b></span>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )

    fun apply(
        choiceId: Int,
        decision: Int,
        html: String,
        preferences: Preferences?,
        consumeItem: (Int, Int) -> Unit = { _, _ -> },
    ): Boolean {
        if (choiceId !in CHOICE_IDS || preferences == null) return false
        return when (choiceId) {
            1120 -> applyStart(decision, preferences, consumeItem)
            1121 -> applyRune(decision, preferences, consumeItem)
            1122 -> applyDowels(decision, preferences, consumeItem)
            1123 -> applyFinish(decision, html, preferences, consumeItem)
            else -> false
        }
    }

    private fun applyStart(
        decision: Int,
        preferences: Preferences,
        consumeItem: (Int, Int) -> Unit,
    ): Boolean {
        when (decision) {
            1 -> consumeItem(VykeaChoiceMapper.PLANK_ID, 5)
            2 -> consumeItem(VykeaChoiceMapper.RAIL_ID, 5)
            else -> return false
        }
        consumeItem(VykeaChoiceMapper.INSTRUCTIONS_ID, 1)
        preferences.setString(VykeaCompanionManager.NAME_PREF, "")
        preferences.setInt(VykeaCompanionManager.LEVEL_PREF, 0)
        preferences.setString(VykeaCompanionManager.TYPE_PREF, "")
        preferences.setString(VykeaCompanionManager.RUNE_PREF, "")
        return true
    }

    private fun applyRune(
        decision: Int,
        preferences: Preferences,
        consumeItem: (Int, Int) -> Unit,
    ): Boolean {
        val rune = when (decision) {
            1 -> {
                consumeItem(VykeaChoiceMapper.FRENZY_RUNE_ID, 1)
                "frenzy"
            }
            2 -> {
                consumeItem(VykeaChoiceMapper.BLOOD_RUNE_ID, 1)
                "blood"
            }
            3 -> {
                consumeItem(VykeaChoiceMapper.LIGHTNING_RUNE_ID, 1)
                "lightning"
            }
            6 -> ""
            else -> return false
        }
        preferences.setString(VykeaCompanionManager.RUNE_PREF, rune)
        return true
    }

    private fun applyDowels(
        decision: Int,
        preferences: Preferences,
        consumeItem: (Int, Int) -> Unit,
    ): Boolean {
        val (level, dowels) = when (decision) {
            1 -> 2 to 1
            2 -> 3 to 11
            3 -> 4 to 23
            4 -> 5 to 37
            6 -> 1 to 0
            else -> return false
        }
        preferences.setInt(VykeaCompanionManager.LEVEL_PREF, level)
        if (dowels > 0) consumeItem(VykeaChoiceMapper.DOWEL_ID, dowels)
        return true
    }

    private fun applyFinish(
        decision: Int,
        html: String,
        preferences: Preferences,
        consumeItem: (Int, Int) -> Unit,
    ): Boolean {
        when (decision) {
            1 -> consumeItem(VykeaChoiceMapper.PLANK_ID, 5)
            2 -> consumeItem(VykeaChoiceMapper.RAIL_ID, 5)
            3 -> consumeItem(VykeaChoiceMapper.BRACKET_ID, 5)
            else -> return false
        }
        val match = CREATION_PATTERN.find(html) ?: return true
        val typeString = match.groupValues[1]
        val name = match.groupValues[2]
        val type = VykeaCompanionData.typeFromString(typeString) ?: return true
        val level = preferences.getInt(VykeaCompanionManager.LEVEL_PREF, 0).coerceAtLeast(1)
        val rune = VykeaCompanionData.runeFromString(
            preferences.getString(VykeaCompanionManager.RUNE_PREF, ""),
        )
        preferences.setString(VykeaCompanionManager.NAME_PREF, name)
        preferences.setString(VykeaCompanionManager.TYPE_PREF, typeString)
        val companion = VykeaCompanionData.Companion(type, level, rune, name)
        preferences.setString(
            VykeaCompanionManager.CURRENT_VYKEA_PREF,
            VykeaCompanionData.toAshString(companion),
        )
        return true
    }
}
