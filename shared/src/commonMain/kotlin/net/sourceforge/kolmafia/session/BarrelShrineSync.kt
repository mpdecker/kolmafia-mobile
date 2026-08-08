package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [net.sourceforge.kolmafia.session.ChoiceControl] case 1100 visit/post-choice pref sync. */
object BarrelShrineSync {
    fun syncUnlockFromHtml(html: String, prefs: Preferences) {
        if (html.contains("barrelshrine", ignoreCase = true)) {
            prefs.setBoolean("barrelShrineUnlocked", true)
        }
    }

    fun syncFromVisit(html: String, prefs: Preferences) {
        if (html.contains("You already prayed to the Barrel god today", ignoreCase = true)) {
            prefs.setBoolean("_barrelPrayer", true)
            return
        }
        if (!html.contains("barrel lid shield", ignoreCase = true)) {
            prefs.setBoolean("prayedForProtection", true)
        }
        if (!html.contains("barrel hoop earring", ignoreCase = true)) {
            prefs.setBoolean("prayedForGlamour", true)
        }
        if (!html.contains("bankruptcy barrel", ignoreCase = true)) {
            prefs.setBoolean("prayedForVigor", true)
        }
    }

    fun syncPostChoice(option: Int, prefs: Preferences) {
        if (option <= 4) {
            prefs.setBoolean("_barrelPrayer", true)
            ConcoctionDatabase.refreshConcoctionsNowFromLastContext()
        }
    }
}
