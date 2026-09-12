package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.data.SkillDefinitionDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.ChoiceCombatAshState
import net.sourceforge.kolmafia.session.FightSessionLog
import net.sourceforge.kolmafia.session.RequestLogger
import net.sourceforge.kolmafia.session.SessionLogger

/**
 * Phases 5156–5170 — thin HTTP residual registerRequest hubs (Behavioral Deepen XXI).
 * Phases 5926–5948 — mail/gift/fight session-log deepen (Behavioral Deepen XXXIV).
 */

object SendGiftRequestHub {
    fun registerRequest(
        url: String,
        sessionLogger: SessionLogger? = null,
        formFields: Map<String, String> = emptyMap(),
    ): Boolean {
        if (!url.contains("town_sendgift.php", ignoreCase = true)) {
            if (!url.contains("sendmessage.php", ignoreCase = true)) return false
            if (param(url, formFields, "towngift") == null &&
                param(url, formFields, "sendgift") == null
            ) {
                return false
            }
        }
        val recipient = param(url, formFields, "towho")
        val items = parseGiftItems(url, formFields)
        val command = if (recipient != null) "send a gift to $recipient" else "send a gift"
        sessionLogger?.appendRawLine(RequestLogger.formatTransferLog(command, items))
        return true
    }

    private fun parseGiftItems(url: String, formFields: Map<String, String>): List<Pair<Int, Int>> {
        val fromStorage = param(url, formFields, "fromwhere") == "1"
        val prefix = if (fromStorage) "hagnks_" else ""
        val result = mutableListOf<Pair<Int, Int>>()
        for (i in 1..100) {
            val itemId = param(url, formFields, "${prefix}whichitem$i")?.toIntOrNull() ?: break
            if (itemId <= 0) continue
            val qty = param(url, formFields, "${prefix}howmany$i")?.toIntOrNull()?.coerceAtLeast(1) ?: 1
            result.add(itemId to qty)
        }
        if (result.isNotEmpty()) return result
        return RequestLogger.parseTransferItems(url, formFields)
    }
}

object SendMailRequestHub {
    fun registerRequest(
        url: String,
        sessionLogger: SessionLogger? = null,
        formFields: Map<String, String> = emptyMap(),
    ): Boolean {
        if (!url.contains("sendmessage.php", ignoreCase = true) &&
            !url.contains("sendkmail.php", ignoreCase = true)
        ) {
            return false
        }
        if (SendGiftRequestHub.registerRequest(url, sessionLogger, formFields)) return true
        val action = param(url, formFields, "action")
        if (action != null && !action.equals("send", ignoreCase = true)) return false
        val recipient = param(url, formFields, "towho")
        val items = RequestLogger.parseTransferItems(url, formFields)
        val meat = param(url, formFields, "sendmeat")?.toLongOrNull() ?: 0L
        if (recipient == null && items.isEmpty() && meat == 0L) return false
        val detail = RequestLogger.formatTransferLog("send a kmail", items, meat)
        val line = if (recipient != null) {
            detail.replace("send a kmail", "send a kmail to $recipient")
        } else {
            detail
        }
        sessionLogger?.appendRawLine(line)
        return true
    }
}

object FightRequestHub {
    fun registerRequest(
        url: String,
        sessionLogger: SessionLogger? = null,
        preferences: Preferences? = null,
        formFields: Map<String, String> = emptyMap(),
    ): Boolean {
        if (!url.startsWith("fight.php", ignoreCase = true) &&
            !url.startsWith("fambattle.php", ignoreCase = true)
        ) {
            return false
        }
        if (url.equals("fight.php", ignoreCase = true) ||
            url.equals("fight.php?", ignoreCase = true) ||
            url.contains("ireallymeanit=", ignoreCase = true)
        ) {
            val action = param(url, formFields, "action")
            if (action.isNullOrBlank()) return true
        }
        if (preferences?.getBoolean("logBattleAction", true) != true) return true
        val actor = fightActorName(preferences)
        val message = when {
            url.startsWith("fambattle.php", ignoreCase = true) ->
                fambattleAction(url, formFields, actor)
            else -> combatAction(url, formFields, actor)
        } ?: return true
        FightSessionLog.logText(message, sessionLogger, ChoiceCombatAshState.currentRound)
        return true
    }

    private fun fightActorName(preferences: Preferences?): String {
        if (preferences?.getString("limitMode", "")?.equals("batman", ignoreCase = true) == true ||
            preferences?.getString("limit_mode", "")?.equals("batman", ignoreCase = true) == true
        ) {
            return "Batfellow"
        }
        // Avoid RequestLogger.fightActorName() call-site (circular import resolution).
        return RequestLogger.fightActorName.invoke()
    }

    private fun combatAction(
        url: String,
        formFields: Map<String, String>,
        actor: String,
    ): String? {
        val action = param(url, formFields, "action")?.lowercase()
            ?: urlParamAction(url)?.lowercase()
        return when {
            action == "macro" || url.contains("macro", ignoreCase = true) ->
                "$actor executes a macro!"
            action == "runaway" || url.contains("runaway", ignoreCase = true) ->
                "$actor casts RETURN!"
            action == "steal" || url.contains("steal", ignoreCase = true) ->
                "$actor tries to steal an item!"
            action == "attack" || url.contains("attack", ignoreCase = true) ->
                "$actor attacks!"
            action == "chefstaff" || url.contains("chefstaff", ignoreCase = true) ->
                "$actor jiggles the staff!"
            action == "skill" || url.contains("whichskill", ignoreCase = true) -> {
                val skillId = param(url, formFields, "whichskill")?.toIntOrNull()
                    ?: queryInt(url, "whichskill")
                val skill = skillId?.let { SkillDefinitionDatabase.getById(it)?.name }
                if (skill.isNullOrBlank()) "$actor casts CHANCE!" else "$actor casts ${skill.uppercase()}!"
            }
            action == "useitem" || url.contains("whichitem", ignoreCase = true) -> {
                val itemId = param(url, formFields, "whichitem")?.toIntOrNull()
                    ?: queryInt(url, "whichitem")
                val itemName = itemId?.let { RequestLogger.itemNameById(it) } ?: "item"
                val item2 = param(url, formFields, "whichitem2")?.toIntOrNull()
                    ?: queryInt(url, "whichitem2")
                val second = item2?.let { RequestLogger.itemNameById(it) }
                buildString {
                    append(actor)
                    append(" uses the ")
                    append(itemName)
                    if (second != null) {
                        append(" and uses the ")
                        append(second)
                    }
                    append('!')
                }
            }
            else -> null
        }
    }

    private fun fambattleAction(url: String, formFields: Map<String, String>, actor: String): String? {
        val move = Regex("""famaction\[([^]]+)]=([^&]+)""").find(url)?.groupValues?.getOrNull(2)
            ?: formFields.entries.firstOrNull { it.key.startsWith("famaction[") }?.value
        return move?.let { "$actor's familiar uses ${decode(it)}!" }
    }

    private fun urlParamAction(url: String): String? = when {
        url.contains("runaway", ignoreCase = true) -> "runaway"
        url.contains("steal", ignoreCase = true) -> "steal"
        url.contains("attack", ignoreCase = true) -> "attack"
        else -> null
    }

    private fun queryInt(url: String, key: String): Int? =
        param(url, emptyMap(), key)?.toIntOrNull()
}

internal fun param(url: String, formFields: Map<String, String>, key: String): String? =
    formFields[key] ?: queryParamLocal(url, key)

private fun queryParamLocal(url: String, key: String): String? {
    val qIndex = url.indexOf('?')
    val query = if (qIndex >= 0) url.substring(qIndex + 1) else return null
    for (part in query.split('&')) {
        val eq = part.indexOf('=')
        if (eq < 0) continue
        if (part.substring(0, eq).equals(key, ignoreCase = true)) {
            return decode(part.substring(eq + 1))
        }
    }
    return null
}

private fun decode(s: String): String =
    s.replace('+', ' ').replace(Regex("%([0-9A-Fa-f]{2})")) {
        it.groupValues[1].toInt(16).toChar().toString()
    }

object GourdRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("town_right.php", ignoreCase = true) &&
            !url.contains("gourd", ignoreCase = true)
        ) {
            return false
        }
        if (!url.contains("gourd", ignoreCase = true) &&
            !url.contains("action=gourd", ignoreCase = true)
        ) {
            return false
        }
        sessionLogger?.appendRawLine("Visiting the Gourd")
        return true
    }
}

object FriarRequestHub {
    /** Desktop FriarRequest.registerRequest — `friars blessing N` when bro= is present. */
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("friars.php", ignoreCase = true)) return false
        val bro = Regex("""(?:^|[?&])bro=(\d+)""", RegexOption.IGNORE_CASE)
            .find(url)?.groupValues?.getOrNull(1)
            ?: Regex("""action=buffs.*?bro=(\d+)""", RegexOption.IGNORE_CASE)
                .find(url)?.groupValues?.getOrNull(1)
        if (bro != null) {
            sessionLogger?.appendRawLine("friars blessing $bro")
        }
        return true
    }

    fun parseResponse(url: String, html: String, preferences: Preferences?) {
        LegacyCoinmasterResponseParse.parseResponse(url, html, preferences)
    }
}

object FamiliarRequestHub {
    /**
     * Desktop FamiliarRequest.registerRequest fallback for familiars.php /
     * bare terrarium visits. Detailed familiar.php actions are handled in
     * [RequestLogger.registerLongTail].
     */
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("familiar.php", ignoreCase = true) &&
            !url.contains("familiars.php", ignoreCase = true)
        ) {
            return false
        }
        // familiar.php with an action is owned by RequestLogger long-tail detail.
        if (url.startsWith("familiar.php", ignoreCase = true) &&
            (url.contains("action=", ignoreCase = true) ||
                url.contains("newfam=", ignoreCase = true) ||
                url.contains("whichfam=", ignoreCase = true))
        ) {
            return false
        }
        sessionLogger?.appendRawLine("Visiting Terrarium")
        return true
    }
}

object Crimbo20BoozeRequest {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=crimbo20booze", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Crimbo20 Booze")
        return true
    }
}

object Crimbo20FoodRequest {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=crimbo20food", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Crimbo20 Food")
        return true
    }
}

object Crimbo20CandyRequest {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=crimbo20candy", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Crimbo20 Candy")
        return true
    }
}

object DedigitizerRequest {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=dedigitizer", ignoreCase = true) &&
            !url.contains("whichshop=cyber_dedigitizer", ignoreCase = true) &&
            !url.contains("dedigitizer", ignoreCase = true)
        ) {
            return false
        }
        sessionLogger?.appendRawLine("Visiting Dedigitizer")
        return true
    }

    fun parseResponse(url: String, html: String, preferences: Preferences?) {
        LegacyCoinmasterResponseParse.parseResponse(url, html, preferences)
    }
}

object BatFabricatorRequest {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=batman_cave", ignoreCase = true) &&
            !url.contains("batfabricator", ignoreCase = true) &&
            !url.contains("whichshop=batman", ignoreCase = true)
        ) {
            return false
        }
        sessionLogger?.appendRawLine("Visiting Bat Fabricator")
        return true
    }

    fun parseResponse(url: String, html: String, preferences: Preferences?) {
        LegacyCoinmasterResponseParse.parseResponse(url, html, preferences)
    }
}

object DiscoGiftCoRequest {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=cgold", ignoreCase = true) &&
            !url.contains("whichshop=infernodisco", ignoreCase = true) &&
            !url.contains("discogift", ignoreCase = true)
        ) {
            return false
        }
        sessionLogger?.appendRawLine("Visiting Disco GiftCo")
        return true
    }

    fun parseResponse(url: String, html: String, preferences: Preferences?) {
        LegacyCoinmasterResponseParse.parseResponse(url, html, preferences)
    }
}

object RenaissanceGiftShopRequest {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=chateau", ignoreCase = true) &&
            !url.contains("whichshop=rsg", ignoreCase = true) &&
            !url.contains("renaissance", ignoreCase = true)
        ) {
            return false
        }
        sessionLogger?.appendRawLine("Visiting Renaissance Gift Shop")
        return true
    }
}

object SummoningChamberRequestHub {
    /**
     * Desktop SummoningChamberRequest.registerRequest — choice 922 option 1
     * logs `summon <demonname>`; other chamber visits claim without a line.
     */
    fun registerRequest(
        url: String,
        sessionLogger: SessionLogger? = null,
        formFields: Map<String, String> = emptyMap(),
    ): Boolean {
        val isChoice922 = (url.contains("choice.php", ignoreCase = true) ||
            formFields["whichchoice"] != null) &&
            (url.contains("whichchoice=922", ignoreCase = true) ||
                formFields["whichchoice"] == "922")
        if (isChoice922) {
            val option = param(url, formFields, "option")
            if (option != null && option != "1") return false
            if (option == "1" || url.contains("option=1", ignoreCase = true)) {
                val demon = param(url, formFields, "demonname")?.trim().orEmpty()
                if (demon.isNotEmpty()) {
                    sessionLogger?.appendRawLine("summon $demon")
                }
            }
            return true
        }
        if (url.contains("manor4_chamber", ignoreCase = true) ||
            url.contains("summoningchamber", ignoreCase = true) ||
            (url.contains("manor4", ignoreCase = true) &&
                url.contains("chamber", ignoreCase = true))
        ) {
            return true
        }
        return false
    }
}

object SafetyShelterRequest {
    /**
     * Desktop FalloutShelterRequest.registerRequest — action-specific lines;
     * bare visits claim without logging.
     */
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("place.php", ignoreCase = true)) return false
        if (!url.contains("whichplace=falloutshelter", ignoreCase = true) &&
            !url.contains("falloutshelter", ignoreCase = true)
        ) {
            return false
        }
        val action = Regex("""(?:^|[?&])action=([^&]+)""", RegexOption.IGNORE_CASE)
            .find(url)?.groupValues?.getOrNull(1)?.let { decode(it) }
        val message = when (action) {
            null, "" -> null // simple visit — claimed, no line
            "vault1" -> "Rest in your Cryo-Sleep Chamber"
            "vault3" -> "Visiting your Spa Simulation Chamber"
            "vault5" -> "Visiting your Chronodynamics Laboratory"
            "vault8" -> "Visiting your Main Reactor"
            else -> null // unknown/shop vault actions — claim quietly
        }
        if (message != null) {
            sessionLogger?.appendRawLine(message)
        }
        return true
    }
}
