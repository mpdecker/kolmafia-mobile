package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.MummeryChoiceSync
import net.sourceforge.kolmafia.quest.PantogramChoiceSync
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.session.WildfireCampManager
import net.sourceforge.kolmafia.skill.UseSkillSync

/** Desktop [net.sourceforge.kolmafia.request.UseSkillRequest] skills.php hub. */
object UseSkillRequest {
    fun parseResponse(url: String, html: String, preferences: Preferences? = null) {
        if (!url.contains("skills.php", ignoreCase = true)) return
        UseSkillSync.parseResponse(
            urlString = url,
            responseText = html,
            preferences = preferences,
        )
    }

    fun registerRequest(url: String): Boolean =
        url.contains("skills.php", ignoreCase = true)
}

/** Desktop [net.sourceforge.kolmafia.request.WildfireCampRequest] place.php wildfire_camp. */
object WildfireCampRequest {
    fun parseResponse(url: String, html: String, preferences: Preferences?) {
        if (preferences == null) return
        if (!url.contains("whichplace=wildfire_camp", ignoreCase = true) &&
            !url.contains("wildfire_", ignoreCase = true)
        ) {
            return
        }
        val action = Regex("""action=([^&]+)""", RegexOption.IGNORE_CASE)
            .find(url)?.groupValues?.get(1)?.lowercase()
        when (action) {
            "wildfire_rainbarrel" -> {
                preferences.setBoolean("_wildfireBarrelHarvested", true)
                if (html.contains("You collect", ignoreCase = true)) {
                    preferences.setBoolean(
                        "wildfireBarrelCaulked",
                        html.contains("You collect 150 water", ignoreCase = true),
                    )
                }
            }
            "wildfire_oldpump" -> {
                if (html.contains("You collect", ignoreCase = true)) {
                    preferences.setBoolean(
                        "wildfirePumpGreased",
                        html.contains("You collect 50 water", ignoreCase = true),
                    )
                }
            }
            else -> WildfireCampManager.parseCaptainHtml(preferences, html)
        }
    }

    fun registerRequest(url: String): Boolean =
        url.contains("whichplace=wildfire_camp", ignoreCase = true) ||
            url.contains("wildfire_", ignoreCase = true)
}

/** Desktop [net.sourceforge.kolmafia.request.ArtistRequest] town_wrong artist quest. */
object ArtistRequest {
    private const val RAT_WHISKER = 197
    private const val PRETENTIOUS_PAINTBRUSH = 450
    private const val PRETENTIOUS_PALETTE = 451
    private const val PRETENTIOUS_PAIL = 1258

    fun parseResponse(
        url: String,
        html: String,
        questDatabase: QuestDatabase?,
        inventory: InventoryManager? = null,
    ) {
        if (!url.contains("place.php", ignoreCase = true)) return
        if (!url.contains("townwrong_artist", ignoreCase = true)) return
        when {
            html.contains("If I'm going to work, I'll need my paintbrush") ||
                html.contains("still need to find my tools") ->
                questDatabase?.setQuestIfBetter(Quest.ARTIST, QuestDatabase.STARTED)
            html.contains("do you want this empty pail") -> {
                inventory?.consumeItemLocally(PRETENTIOUS_PALETTE, 1)
                inventory?.consumeItemLocally(PRETENTIOUS_PAINTBRUSH, 1)
                inventory?.consumeItemLocally(PRETENTIOUS_PAIL, 1)
                questDatabase?.setQuestIfBetter(Quest.ARTIST, QuestDatabase.FINISHED)
            }
            url.contains("subaction=whisker", ignoreCase = true) &&
                html.contains("Thanks, Adventurer") -> {
                val whiskers = inventory?.getCount(RAT_WHISKER) ?: 0
                if (whiskers > 0) inventory?.consumeItemLocally(RAT_WHISKER, whiskers)
            }
        }
    }

    fun registerRequest(url: String): Boolean =
        url.contains("place.php", ignoreCase = true) &&
            url.contains("townwrong_artist", ignoreCase = true)
}

/** Desktop [net.sourceforge.kolmafia.request.AltarOfLiteracyRequest] town_altar.php. */
object AltarOfLiteracyRequest {
    const val CHAT_LITERATE_PREF = "chatLiterate"

    fun parseResponse(url: String, html: String, preferences: Preferences?) {
        if (preferences == null) return
        if (!url.contains("town_altar.php", ignoreCase = true)) return
        when {
            html.contains("You have been granted access", ignoreCase = true) ||
                html.contains("You have already proven yourself literate", ignoreCase = true) ->
                preferences.setBoolean(CHAT_LITERATE_PREF, true)
            html.contains("you are not allowed to enter the chat", ignoreCase = true) ->
                preferences.setBoolean(CHAT_LITERATE_PREF, false)
        }
    }

    fun registerRequest(url: String): Boolean =
        url.contains("town_altar.php", ignoreCase = true)
}

/** Desktop [net.sourceforge.kolmafia.request.DreadsylvaniaRequest] clan_dreadsylvania.php. */
object DreadsylvaniaRequest {
    private val SHORTCUTS = listOf(
        "ghostPencil1", "ghostPencil2", "ghostPencil3",
        "ghostPencil4", "ghostPencil5", "ghostPencil6",
        "ghostPencil7", "ghostPencil8", "ghostPencil9",
    )

    fun parseResponse(url: String, html: String, preferences: Preferences?) {
        if (preferences == null) return
        if (!url.contains("clan_dreadsylvania.php", ignoreCase = true)) return
        SHORTCUTS.forEachIndexed { index, pref ->
            val image = "shortcut${index + 1}.gif"
            if (html.contains(image, ignoreCase = true)) {
                preferences.setBoolean(pref, true)
            }
        }
        if (url.contains("whichbooze=", ignoreCase = true) &&
            html.contains("You acquire", ignoreCase = true)
        ) {
            val itemId = Regex("""whichbooze=(\d+)""", RegexOption.IGNORE_CASE)
                .find(url)?.groupValues?.get(1)?.toIntOrNull()
            val qty = Regex("""boozequantity=(\d+)""", RegexOption.IGNORE_CASE)
                .find(url)?.groupValues?.get(1)?.toIntOrNull() ?: 1
            if (itemId != null) preferences.setInt("_dreadLastBooze", itemId)
            preferences.setInt("_dreadLastBoozeQty", qty)
        }
    }

    fun registerRequest(url: String): Boolean =
        url.contains("clan_dreadsylvania.php", ignoreCase = true)

    fun getAdventuresUsed(url: String): Int =
        if (url.contains("action=adventure", ignoreCase = true) ||
            Regex("""loc=\d+""", RegexOption.IGNORE_CASE).containsMatchIn(url)
        ) 1 else 0
}

/** Desktop [net.sourceforge.kolmafia.request.PantogramRequest] choice 1270 hub. */
object PantogramRequest {
    fun parseResponse(
        url: String,
        html: String,
        preferences: Preferences?,
        inventory: InventoryManager? = null,
    ) {
        PantogramChoiceSync.apply(
            choiceId = PantogramChoiceSync.CHOICE_ID,
            html = html,
            preferences = preferences,
            choiceUrl = url,
            consumeItem = { id, qty -> inventory?.consumeItemLocally(id, qty) },
            gainItem = { id, qty -> inventory?.gainItemLocally(id, qty) },
        )
    }

    fun registerRequest(url: String): Boolean =
        url.contains("whichchoice=1270", ignoreCase = true)
}

/** Desktop [net.sourceforge.kolmafia.request.MummeryRequest] choice 1271 hub. */
object MummeryRequest {
    fun parseResponse(
        url: String,
        html: String,
        preferences: Preferences?,
        familiarRace: String = "",
        familiarHasAttribute: (String) -> Boolean = { false },
    ) {
        val option = Regex("""option=(\d+)""", RegexOption.IGNORE_CASE)
            .find(url)?.groupValues?.get(1)?.toIntOrNull() ?: return
        MummeryChoiceSync.apply(
            choiceId = MummeryChoiceSync.CHOICE_ID,
            decision = option,
            html = html,
            preferences = preferences,
            familiarRace = familiarRace,
            familiarHasAttribute = familiarHasAttribute,
        )
    }

    fun registerRequest(url: String): Boolean =
        url.contains("whichchoice=1271", ignoreCase = true)
}

/** Desktop [net.sourceforge.kolmafia.request.concoction.BurningLeavesRequest] choice 1510 hub. */
object BurningLeavesRequest {
    fun registerRequest(url: String): Boolean =
        url.contains("whichchoice=1510", ignoreCase = true) ||
            url.contains("burningleaves", ignoreCase = true)
}

/** Desktop [net.sourceforge.kolmafia.request.concoction.SausageOMaticRequest] grind sausage. */
object SausageOMaticRequest {
    fun registerRequest(url: String): Boolean =
        url.contains("sausage", ignoreCase = true) &&
            (url.contains("inv_use.php", ignoreCase = true) ||
                url.contains("whichchoice=1480", ignoreCase = true))
}

/** Desktop [net.sourceforge.kolmafia.request.concoction.FantasyRealmRequest]. */
object FantasyRealmRequest {
    fun registerRequest(url: String): Boolean =
        url.contains("whichshop=fantasyrealm", ignoreCase = true) ||
            url.contains("fr_armor", ignoreCase = true)
}

/** Desktop [net.sourceforge.kolmafia.request.concoction.GnomePartRequest]. */
object GnomePartRequest {
    fun registerRequest(url: String): Boolean =
        url.contains("gnomes.php", ignoreCase = true) ||
            url.contains("tinksomething", ignoreCase = true)
}
