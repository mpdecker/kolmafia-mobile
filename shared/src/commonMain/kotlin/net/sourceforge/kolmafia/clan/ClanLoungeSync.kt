package net.sourceforge.kolmafia.clan

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.FloundryAvailability
import net.sourceforge.kolmafia.data.FloundryDatabase
import net.sourceforge.kolmafia.data.HotDogAvailability
import net.sourceforge.kolmafia.data.HotDogDatabase
import net.sourceforge.kolmafia.data.RestrictedItemType
import net.sourceforge.kolmafia.data.SpeakeasyAvailability
import net.sourceforge.kolmafia.data.SpeakeasyDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.FloundryRequest
import net.sourceforge.kolmafia.request.StandardRequest

/**
 * Parses clan VIP lounge HTML for lounge item availability and daily-limit prefs.
 * Mirrors desktop [ClanLoungeRequest] floundry/photo booth, speakeasy drink counts,
 * and post-action `_fancyHotDogEaten` / `_speakeasyDrinksDrunk` updates.
 */
object ClanLoungeSync {

    const val CLAN_HAS_FLOUNDRY_PREF = "_clanHasFloundry"
    const val CLAN_HAS_PHOTO_BOOTH_PREF = "_clanHasPhotoBooth"
    const val CLAN_HAS_HOT_DOG_STAND_PREF = "_clanHasHotDogStand"
    const val CLAN_HAS_SPEAKEASY_PREF = "_clanHasSpeakeasy"
    const val FANCY_HOT_DOG_EATEN_PREF = "_fancyHotDogEaten"
    const val SPEAKEASY_DRINKS_DRUNK_PREF = "_speakeasyDrinksDrunk"
    const val MIME_ARMY_SHOTGLASS_USED_PREF = "_mimeArmyShotglassUsed"
    const val HOT_TUB_SOAKS_PREF = "_hotTubSoaks"
    const val FLOUNDRY_CARP_LOCATION_PREF = "_floundryCarpLocation"
    const val FLOUNDRY_ITEM_USED_PREF = "_floundryItemUsed"
    const val CLAN_HOT_DOG_STAND_RESTRICTION = "Clan hot dog stand"
    const val CLAN_SPEAKEASY_RESTRICTION = "Clan speakeasy"

    fun isHotDogStandAllowed(state: CharacterState?): Boolean =
        state == null ||
            StandardRequest.isAllowed(
                RestrictedItemType.CLAN_ITEMS,
                CLAN_HOT_DOG_STAND_RESTRICTION,
                state,
            )

    fun isSpeakeasyAllowed(state: CharacterState?): Boolean =
        state == null ||
            StandardRequest.isAllowed(
                RestrictedItemType.CLAN_ITEMS,
                CLAN_SPEAKEASY_RESTRICTION,
                state,
            )

    private val WHICHDOG_PATTERN = Regex("""whichdog=(-\d+)""")
    private val DRINK_PATTERN = Regex("""drink=(\d+)""")
    private val SPEAKEASY_DRINK_ROW_PATTERN = Regex("""name="drink"\s+value="\d+"""")
    private val FISH_LOCATION_PATTERN = Regex(
        """<br><b>(carp|cod|trout|bass|hatchetfish|tuna):</b> ([^<]+)""",
    )
    private val HOTTUB_PATTERN = Regex("""hottub(\d)\.gif""", RegexOption.IGNORE_CASE)

    /** Desktop ClanLoungeRequest.parseLounge — hottub(N).gif → _hotTubSoaks = 5 - N. */
    fun syncHotTubSoaksFromHtml(html: String, prefs: Preferences?) {
        if (prefs == null) return
        val match = HOTTUB_PATTERN.find(html) ?: return
        val digit = match.groupValues[1].toIntOrNull() ?: return
        prefs.setInt(HOT_TUB_SOAKS_PREF, 5 - digit)
    }

    fun hasFloundry(prefs: Preferences?): Boolean =
        prefs?.getBoolean(CLAN_HAS_FLOUNDRY_PREF, false) == true

    fun hasPhotoBooth(prefs: Preferences?): Boolean =
        prefs?.getBoolean(CLAN_HAS_PHOTO_BOOTH_PREF, false) == true

    fun syncFromHtml(html: String, prefs: Preferences?) {
        prefs?.setBoolean(CLAN_HAS_FLOUNDRY_PREF, html.contains("vipfloundry.gif", ignoreCase = true))
        prefs?.setBoolean(CLAN_HAS_PHOTO_BOOTH_PREF, html.contains("photobooth.gif", ignoreCase = true))
        prefs?.setBoolean(CLAN_HAS_HOT_DOG_STAND_PREF, html.contains("hotdogstand.gif", ignoreCase = true))
        prefs?.setBoolean(CLAN_HAS_SPEAKEASY_PREF, html.contains("speakeasy.gif", ignoreCase = true))
    }

    /** Desktop ClanLoungeRequest.parseSpeakeasy availability rebuild. */
    fun syncSpeakeasyAvailabilityFromHtml(html: String) {
        SpeakeasyAvailability.reset()
        SpeakeasyAvailability.addFromHtml(html)
    }

    /** Desktop ClanLoungeRequest.parseHotDogStand availability rebuild. */
    fun syncHotDogAvailabilityFromHtml(html: String) {
        HotDogAvailability.reset()
        HotDogAvailability.addFromHtml(html)
    }

    /** Desktop ClanLoungeRequest.parseFloundry availability rebuild. */
    fun syncFloundryFromHtml(html: String, prefs: Preferences?) {
        FloundryAvailability.reset()
        FloundryAvailability.addFromHtml(html)
        syncFloundryLocationsFromHtml(html, prefs)
        ConcoctionDatabase.refreshAfterLoungeMutation(prefs)
    }

    /** Desktop ClanLoungeRequest.parseFloundryLocations — once per day when carp location unset. */
    fun syncFloundryLocationsFromHtml(html: String, prefs: Preferences?) {
        if (prefs == null) return
        if (prefs.getString(FLOUNDRY_CARP_LOCATION_PREF, "").isNotEmpty()) return
        for (match in FISH_LOCATION_PATTERN.findAll(html)) {
            val fish = match.groupValues.getOrNull(1)?.trim().orEmpty()
            val location = match.groupValues.getOrNull(2)?.trim().orEmpty()
            if (fish.isEmpty() || location.isEmpty()) continue
            val prefKey = FloundryDatabase.locationPrefForFish(fish) ?: continue
            prefs.setString(prefKey, location)
        }
    }

    /** Desktop ClanLoungeRequest.parseSpeakeasy drink-count strings. */
    fun syncSpeakeasyDrinksDrunkFromHtml(html: String, prefs: Preferences?) {
        if (prefs == null) return
        when {
            html.contains("have 3 more drinks") -> prefs.setInt(SPEAKEASY_DRINKS_DRUNK_PREF, 0)
            html.contains("have 2 more drinks") -> prefs.setInt(SPEAKEASY_DRINKS_DRUNK_PREF, 1)
            html.contains("have one more drink") -> prefs.setInt(SPEAKEASY_DRINKS_DRUNK_PREF, 2)
            html.contains("had your limit") -> prefs.setInt(SPEAKEASY_DRINKS_DRUNK_PREF, 3)
        }
    }

    /** Desktop ClanLoungeRequest.processRequest eathotdog branch. Returns true when daily-limit pref changed. */
    fun syncHotDogEatFromResponse(html: String, url: String, prefs: Preferences?): Boolean {
        if (prefs == null) return false
        if (html.contains("You aren't in the mood for any more fancy dogs today")) {
            prefs.setBoolean(FANCY_HOT_DOG_EATEN_PREF, true)
            return true
        }
        if (html.contains("You don't feel up to eating that")) return false
        if (html.contains("You're too full")) return false
        if (html.contains("You lose") &&
            !html.contains("You lose some of an effect") &&
            !html.contains("You lose an effect") &&
            !html.contains("Mayodiol kicks in")
        ) {
            return false
        }
        val cafeId = WHICHDOG_PATTERN.find(url)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: return false
        val index = HotDogDatabase.cafeIdToIndex(cafeId)
        if (index > 0) {
            prefs.setBoolean(FANCY_HOT_DOG_EATEN_PREF, true)
            return true
        }
        return false
    }

    /** Desktop ClanLoungeRequest processRequest speakeasydrink branch. */
    fun syncSpeakeasyDrinkFromResponse(html: String, url: String, prefs: Preferences?) {
        if (prefs == null) return
        if (html.contains("We don't serve minors here, kid")) return
        if (html.contains("You can't afford that")) return
        if (html.contains("You pour your drink into your mime army shotglass")) {
            prefs.setBoolean(MIME_ARMY_SHOTGLASS_USED_PREF, true)
        }
        val drinkId = DRINK_PATTERN.find(url)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: return
        if (SpeakeasyDatabase.loungeIdToIndex(drinkId) >= 0) {
            prefs.setInt(SPEAKEASY_DRINKS_DRUNK_PREF, prefs.getInt(SPEAKEASY_DRINKS_DRUNK_PREF) + 1)
        }
    }

    fun apply(preferences: Preferences?, html: String, url: String?) {
        if (url == null || !url.contains("clan_viplounge.php", ignoreCase = true)) return
        syncFromHtml(html, preferences)
        syncHotTubSoaksFromHtml(html, preferences)
        val preaction = queryParam(url, "preaction")
        val action = queryParam(url, "action")
        if (action.equals("hotdogstand", ignoreCase = true)) {
            syncHotDogAvailabilityFromHtml(html)
            ClanHotdogMenuCache.saveMenu(preferences)
            ConcoctionDatabase.refreshAfterLoungeMutation(preferences)
        }
        if (action.equals("speakeasy", ignoreCase = true) ||
            SPEAKEASY_DRINK_ROW_PATTERN.containsMatchIn(html)
        ) {
            syncSpeakeasyAvailabilityFromHtml(html)
            syncSpeakeasyDrinksDrunkFromHtml(html, preferences)
            ConcoctionDatabase.refreshAfterLoungeMutation(preferences)
        }
        if (action.equals("floundry", ignoreCase = true)) {
            preferences?.setBoolean(CLAN_HAS_FLOUNDRY_PREF, true)
            syncFloundryFromHtml(html, preferences)
        }
        when (preaction?.lowercase()) {
            "eathotdog" -> {
                if (syncHotDogEatFromResponse(html, url, preferences)) {
                    ConcoctionDatabase.refreshAfterLoungeMutation(preferences)
                }
            }
            "speakeasydrink" -> syncSpeakeasyDrinkFromResponse(html, url, preferences)
            "buyfloundryitem" -> {
                if (html.contains("You acquire")) {
                    preferences?.setBoolean(FloundryRequest.FLOUNDRY_ITEM_CREATED_PREF, true)
                    ConcoctionDatabase.refreshAfterLoungeMutation(preferences)
                }
            }
        }
    }

    private fun queryParam(url: String, key: String): String? {
        val pattern = Regex("""[?&]${Regex.escape(key)}=([^&]+)""")
        return pattern.find(url)?.groupValues?.getOrNull(1)
    }
}
