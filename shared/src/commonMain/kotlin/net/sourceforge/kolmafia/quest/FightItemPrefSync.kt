package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.data.MonsterDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.TurnCounter

/**
 * Desktop [FightRequest] combat-item pref writers for flyers, DNA, mayo, beehive, knife, blank-out, and Zombo's empty eye.
 */
object FightItemPrefSync {

    const val JAM_BAND_FLYERS = 2404
    const val ROCK_BAND_FLYERS = 2405
    const val DNA_SYRINGE = 7383
    const val MAYO_LANCE = 8269
    const val BEEHIVE = 7969
    const val ELECTRIC_BONING_KNIFE = 7970
    const val GLOB_OF_BLANK_OUT = 4872
    const val EMPTY_EYE = 3388
    const val ZOMBO_EYE_COUNTER = "Zombo's Empty Eye"

    fun apply(
        html: String,
        monster: String,
        preferences: Preferences?,
        combatItemId: Int? = null,
        consumeItem: (Int, Int) -> Unit = { _, _ -> },
        currentRun: Int = 0,
        monsterAttack: (String) -> Int = { name ->
            MonsterDatabase.getByName(name)?.attack?.coerceAtLeast(0) ?: 0
        },
        monsterPhylum: (String) -> String = { name ->
            val phylum = MonsterDatabase.getByName(name)?.phylum.orEmpty()
            phylum.ifBlank { "none" }
        },
    ): Boolean {
        if (preferences == null) return false
        var changed = false
        if (html.contains("You slap a flyer")) {
            val ml = monsterAttack(monster).coerceAtLeast(0)
            preferences.setInt("flyeredML", preferences.getInt("flyeredML", 0) + ml)
            changed = true
        } else if (html.contains("Rock Promoters are long gone") &&
            combatItemId in setOf(JAM_BAND_FLYERS, ROCK_BAND_FLYERS)
        ) {
            consumeItem(combatItemId!!, 1)
            changed = true
        }
        if (html.contains("plunge the syringe") &&
            (combatItemId == null || combatItemId == DNA_SYRINGE)
        ) {
            preferences.setString("dnaSyringe", monsterPhylum(monster))
            changed = true
        }
        if (html.contains("Everything Looks Yellow") && combatItemId == MAYO_LANCE) {
            preferences.setInt(
                "mayoLevel",
                (preferences.getInt("mayoLevel", 0) - 30).coerceAtLeast(0),
            )
            changed = true
        }
        if (html.contains("entire wall fattens") && combatItemId == BEEHIVE) {
            consumeItem(BEEHIVE, 1)
            changed = true
        }
        if (html.contains("knife's motor burns out") && combatItemId == ELECTRIC_BONING_KNIFE) {
            consumeItem(ELECTRIC_BONING_KNIFE, 1)
            changed = true
        }
        if (html.contains("You smear part of your handful") &&
            (combatItemId == null || combatItemId == GLOB_OF_BLANK_OUT)
        ) {
            preferences.setInt("blankOutUsed", preferences.getInt("blankOutUsed", 0) + 1)
            changed = true
        }
        if (combatItemId == EMPTY_EYE &&
            html.contains(
                "You hold Zombo's eye out toward your opponent, whose gaze is transfixed by it.",
            )
        ) {
            preferences.setInt("_lastZomboEye", currentRun + 1)
            TurnCounter.stopCounting(preferences, ZOMBO_EYE_COUNTER)
            TurnCounter.startCounting(
                preferences,
                currentRun,
                50,
                "$ZOMBO_EYE_COUNTER loc=*",
                "zomboeye.gif",
            )
            changed = true
        }
        return changed
    }
}
