package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger

/** Desktop [ReplicaMrStoreRequest.visitShop] year pref sync. */
object ReplicaMrStoreSync {

    const val SHOP_ID = "mrreplica"

    private val REPLICA_YEAR_PATTERN = Regex("""&mdash; <b>(\d+)</b> &mdash;""")

    fun applyVisitShop(
        html: String,
        url: String?,
        prefs: Preferences?,
        sessionLogger: SessionLogger?,
        state: CharacterState?,
    ) {
        if (prefs == null) return
        syncFromShopHtml(html, prefs)
    }

    fun syncFromShopHtml(html: String, prefs: Preferences) {
        REPLICA_YEAR_PATTERN.find(html)?.let { match ->
            val year = match.groupValues[1].toIntOrNull() ?: return
            prefs.setInt("currentReplicaStoreYear", year)
        }
    }
}
