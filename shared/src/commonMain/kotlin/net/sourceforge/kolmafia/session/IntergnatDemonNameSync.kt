package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.familiar.FamiliarIds
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Tracks Intergnat demon name fragments from eldritch fight HTML.
 * Mirrors desktop [SummoningChamberRequest.updateIntergnatName].
 */
class IntergnatDemonNameSync(private val preferences: Preferences) {

    fun demonName(): String =
        preferences.getString(Preferences.DEMON_NAME_12, "")

    fun updateFromFight(
        fightHtml: String,
        familiarId: Int,
        randomModifiers: List<String>,
    ) {
        if (familiarId != FamiliarIds.INTERGNAT || "eldritch" !in randomModifiers) return
        val fragment = extractFragment(fightHtml) ?: return
        if (fragment.isEmpty() || fragment == "Neil") return
        val isContact = !fragment.contains("'")
        updateIntergnatName(fragment, isContact)
    }

    private fun extractFragment(fightHtml: String): String? =
        INTERGNAT1.find(fightHtml)?.groupValues?.getOrNull(1)
            ?: INTERGNAT2.find(fightHtml)?.groupValues?.getOrNull(1)
            ?: INTERGNAT3.find(fightHtml)?.groupValues?.getOrNull(1)
            ?: INTERGNAT4.find(fightHtml)?.groupValues?.getOrNull(1)

    private fun updateIntergnatName(name: String, isContact: Boolean) {
        var demonName = preferences.getString(Preferences.DEMON_NAME_12, "")
        if (demonName.startsWith("Neil")) {
            return
        }
        if (demonName.isEmpty()) {
            preferences.setString(Preferences.DEMON_NAME_12, name)
            return
        }

        val hasContact = !demonName.contains("'")
        if (isContact == hasContact) {
            return
        }

        demonName = if (isContact) {
            "$demonName $name"
        } else {
            "$name $demonName"
        }
        preferences.setString(Preferences.DEMON_NAME_12, "Neil $demonName")
    }

    companion object {
        private val INTERGNAT1 = Regex("""used to be a ([A-Za-z0-9 '_]*?) but then I took""")
        private val INTERGNAT2 = Regex("""All your ([A-Za-z0-9 '_]*?) are belong to us""")
        private val INTERGNAT3 = Regex("""I'm a' chargin' mah ([A-Za-z0-9 '_]*?)!\" it shouts\.""")
        private val INTERGNAT4 = Regex("""I made you a ([A-Za-z0-9 '_]*?) but I eated it!""")
    }
}
