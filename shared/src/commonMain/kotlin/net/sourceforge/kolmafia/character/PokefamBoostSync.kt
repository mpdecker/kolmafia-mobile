package net.sourceforge.kolmafia.character

import net.sourceforge.kolmafia.data.FamiliarDefinitionDatabase
import net.sourceforge.kolmafia.data.PokefamDatabase
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop `FamTeamRequest.getPokeBoost` + famteam feed pref sync. */
object PokefamBoostSync {

    const val POKEFAM_BOOSTS_PREF = "pokefamBoosts"

    private val famParamPattern = Regex("""[?&]fam=(\d+)""", RegexOption.IGNORE_CASE)
    private val itemParamPattern = Regex("""[?&]iid=(\d+)""", RegexOption.IGNORE_CASE)
    private val actionFeedPattern = Regex("""[?&]action=feed""", RegexOption.IGNORE_CASE)

    fun getPokeBoost(race: String, preferences: Preferences?): PokeBoost {
        if (race.isBlank() || preferences == null) return PokeBoost.NONE
        val boosts = preferences.getString(POKEFAM_BOOSTS_PREF)
        if (boosts.isBlank()) return PokeBoost.NONE

        val start = boosts.indexOf(race)
        if (start < 0) return PokeBoost.NONE
        val colon = boosts.indexOf(':', start)
        if (colon < 0) return PokeBoost.NONE
        val end = boosts.indexOf('|', colon)
        val label = if (end < 0) {
            boosts.substring(colon + 1)
        } else {
            boosts.substring(colon + 1, end)
        }
        return PokeBoost.fromLabel(label)
    }

    fun syncFromFeed(
        url: String?,
        html: String,
        preferences: Preferences?,
        inventoryManager: InventoryManager? = null,
    ) {
        if (preferences == null || url == null) return
        if (!url.contains("famteam.php", ignoreCase = true)) return
        if (!actionFeedPattern.containsMatchIn(url)) return
        if (!html.contains("Familiar powered up.")) return

        val familiarId = famParamPattern.find(url)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: return
        val itemId = itemParamPattern.find(url)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: return
        val boost = PokeBoost.fromItemId(itemId) ?: return

        val familiarName = FamiliarDefinitionDatabase.getById(familiarId)?.name ?: return
        var boostLabel = boost.label
        val naturalAttribute = PokefamDatabase.getById(familiarId)?.attribute
        if (!naturalAttribute.isNullOrBlank() && naturalAttribute.equals(boostLabel, ignoreCase = true)) {
            boostLabel = PokeBoost.NONE.label
        }

        val existing = preferences.getString(POKEFAM_BOOSTS_PREF)
        val entry = "$familiarName:$boostLabel"
        val updated = if (existing.isBlank()) {
            entry
        } else {
            "$existing|$entry"
        }
        preferences.setString(POKEFAM_BOOSTS_PREF, updated)
        inventoryManager?.consumeItemLocally(itemId, 1)
    }

    fun adjustStats(
        race: String,
        power: Int,
        hp: Int,
        attributes: List<String>,
        preferences: Preferences?,
    ): Triple<Int, Int, List<String>> {
        val boost = getPokeBoost(race, preferences)
        return when (boost) {
            PokeBoost.POWER -> Triple((power - 1).coerceAtLeast(0), hp, attributes)
            PokeBoost.HP -> Triple(power, (hp - 1).coerceAtLeast(0), attributes)
            PokeBoost.ARMOR,
            PokeBoost.REGENERATING,
            PokeBoost.SMART,
            PokeBoost.SPIKED,
            -> Triple(power, hp, attributes.filterNot { it.equals(boost.label, ignoreCase = true) })
            PokeBoost.NONE -> Triple(power, hp, attributes)
        }
    }
}
