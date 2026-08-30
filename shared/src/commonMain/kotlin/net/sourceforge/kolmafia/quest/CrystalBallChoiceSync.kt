package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.data.AdventureDatabase
import net.sourceforge.kolmafia.data.MonsterDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.CrystalBallManager

/**
 * Desktop [CrystalBallManager.parsePonder] via choice 1462 visit.
 */
object CrystalBallChoiceSync {

    const val CHOICE_ID = 1462

    const val PREDICTIONS_PREF = CrystalBallManager.PREDICTIONS_PREF

    fun applyVisit(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
        currentRun: Int = 0,
        findLocation: (String) -> String? = { AdventureDatabase.getByName(it)?.locationName },
        findMonster: (String) -> String? = { MonsterDatabase.getByName(it)?.name },
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        return CrystalBallManager.parsePonder(
            html, preferences, currentRun, findLocation, findMonster,
        )
    }
}
