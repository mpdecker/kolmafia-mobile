package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.banish.BanishManager
import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.CharacterClass
import net.sourceforge.kolmafia.character.CharpaneValhallaSync
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.character.ZodiacSign
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.SkillDefinitionDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.session.SessionLogger
import net.sourceforge.kolmafia.session.ValhallaManager

/** Desktop AfterLifeRequest headless parse + session-log (Phases 3306–3320, residual 7471–7490). */
object AfterLifeRequest {

    const val EMPTY_RESPONSE_ERROR =
        "Received an empty response from afterlife.php. You are probably not in Valhalla."

    private val ITEM_PATTERN = Regex(
        """<span onclick='descitem\(([\d]+)\)'>([^<]*)<.*?name=whichitem value=([\d]+)>""",
        RegexOption.DOT_MATCHES_ALL,
    )
    private val KARMA_PATTERN = Regex("""You gain ([0123456789,]+) Karma""", RegexOption.DOT_MATCHES_ALL)

    fun parseResponse(
        url: String,
        responseText: String,
        preferences: Preferences?,
        sessionLogger: SessionLogger? = null,
        character: KoLCharacter? = null,
        banishManager: BanishManager? = null,
        questDatabase: QuestDatabase? = null,
        adventureSpentReset: () -> Unit = {},
    ): Boolean {
        if (!url.startsWith("afterlife.php")) return false

        if (responseText.isBlank()) {
            log(sessionLogger, EMPTY_RESPONSE_ERROR)
            return false
        }

        if ((preferences?.getInt("lastBreakfast", -1) ?: -1) != -1) {
            ValhallaManager.onAscension(
                character,
                preferences,
                banishManager,
                questDatabase,
                adventureSpentReset,
            )
        }

        if (url == "afterlife.php") {
            CharpaneValhallaSync.markInValhalla()
            return true
        }

        ITEM_PATTERN.findAll(responseText).forEach { match ->
            val descId = match.groupValues[1]
            val itemName = match.groupValues[2]
            val itemId = match.groupValues[3].toIntOrNull() ?: return@forEach
            val dataName = ItemDatabase.getById(itemId)?.name
            if (dataName == null || !dataName.equals(itemName, ignoreCase = true)) {
                ItemDatabase.registerItem(itemId, itemName, descId)
            }
        }

        val action = actionFromUrl(url) ?: return false
        if (action == "pearlygates") {
            var karma = preferences?.getInt("bankedKarma", 0) ?: 0
            log(sessionLogger, "You have $karma banked Karma.")
            KARMA_PATTERN.findAll(responseText).forEach {
                val delta = it.groupValues[1].replace(",", "").toIntOrNull() ?: 0
                log(sessionLogger, "You gain $delta Karma")
                karma += delta
            }
            log(sessionLogger, "Your new Karma balance is $karma")
            preferences?.setInt("bankedKarma", karma)
            CharpaneValhallaSync.markInValhalla()
            return true
        }

        val delta = karmaDelta(action, responseText, url)
        if (delta != 0) {
            preferences?.incrementInt("bankedKarma", delta)
            val message = if (delta < 0) "You spend ${-delta} Karma" else "You gain $delta Karma"
            log(sessionLogger, message)
        }
        return true
    }

    fun registerRequest(
        url: String,
        sessionLogger: SessionLogger? = null,
        preferences: Preferences? = null,
    ): Boolean {
        if (!url.startsWith("afterlife.php")) return false
        val action = actionFromUrl(url)
        val karma = preferences?.getInt("bankedKarma", 0) ?: 0
        val message = when (action) {
            null -> null
            "pearlygates" -> "Welcome to Valhalla!"
            "scperm", "hcperm" -> permSkillMessage(url, action.startsWith("hc"), karma)
            "returnskill" -> returnSkillMessage(url, karma)
            "buydeli", "buyarmory" -> buyMessage(url, action, karma)
            "delireturn", "armoryreturn" -> returnMessage(url, action, karma)
            "ascend" -> if (url.contains("confirmascend=1")) ascendMessage(url, karma) else null
            else -> null
        }
        if (message != null) {
            sessionLogger?.appendRawLine(message)
        }
        return true
    }

    /**
     * Desktop GenericRequest afterlife reincarnate redirect — [ValhallaManager.postAscension]
     * or deferred [net.sourceforge.kolmafia.session.ChoiceCombatAshState.ascendAfterChoice].
     */
    fun handleReincarnateConfirm(
        url: String,
        redirectLocation: String?,
        deps: ValhallaManager.AscensionDeps,
    ) {
        if (!url.contains("afterlife.php") || !url.contains("confirmascend=1")) return
        ValhallaManager.handleAfterlifeRedirect(redirectLocation, deps)
    }

    private fun actionFromUrl(url: String): String? =
        Regex("""[?&]action=([^&]+)""").find(url)?.groupValues?.get(1)

    private fun karmaDelta(action: String, responseText: String, url: String): Int = when (action) {
        "scperm", "hcperm" ->
            if (responseText.contains("don't have enough Karma")) 0 else if (action == "scperm") -100 else -200
        "returnskill" -> if (url.contains("hc=1")) 200 else 100
        "buydeli" -> -1
        "delireturn" -> 1
        "buyarmory" -> -10
        "armoryreturn" -> 10
        else -> 0
    }

    private fun log(sessionLogger: SessionLogger?, message: String) {
        sessionLogger?.appendRawLine(message)
    }

    private fun permSkillMessage(url: String, hc: Boolean, karma: Int): String? {
        val skillId = Regex("""whichskill=(\d+)""").find(url)?.groupValues?.get(1)?.toIntOrNull()
            ?: return null
        val type = if (hc) "Hard" else "Soft"
        val cost = if (hc) "200" else "100"
        val name = SkillDefinitionDatabase.getById(skillId)?.name ?: "Skill #$skillId"
        return "${type}core perm $name for $cost Karma (initial balance = $karma)"
    }

    private fun returnSkillMessage(url: String, karma: Int): String? {
        val classId = Regex("""classid=(\d+)""").find(url)?.groupValues?.get(1)?.toIntOrNull()
            ?: return null
        val skillId = Regex("""skillid=(\d+)""").find(url)?.groupValues?.get(1)?.toIntOrNull()
            ?: return null
        val hc = Regex("""hc=(\d+)""").find(url)?.groupValues?.get(1) == "1"
        val id = classId * 1000 + skillId
        val type = if (hc) "Hard" else "Soft"
        val cost = if (hc) "200" else "100"
        val name = SkillDefinitionDatabase.getById(id)?.name ?: "Skill #$id"
        return "Return ${type}core Skill $name for $cost Karma (initial balance = $karma)"
    }

    private fun buyMessage(url: String, action: String, karma: Int): String? {
        val itemId = Regex("""whichitem=(\d+)""").find(url)?.groupValues?.get(1)?.toIntOrNull()
            ?: return null
        val itemName = ItemDatabase.getById(itemId)?.name ?: "item #$itemId"
        val cost = if (action == "buydeli") 1 else 10
        return "Buy $itemName for $cost Karma (initial balance = $karma)"
    }

    private fun returnMessage(url: String, action: String, karma: Int): String? {
        val itemId = Regex("""whichitem=(\d+)""").find(url)?.groupValues?.get(1)?.toIntOrNull()
            ?: return null
        val itemName = ItemDatabase.getById(itemId)?.name ?: "item #$itemId"
        val cost = if (action.startsWith("deli")) 1 else 10
        return "Return $itemName for $cost Karma (initial balance = $karma)"
    }

    internal fun ascendMessage(url: String, karma: Int = 0): String {
        val type = Regex("""asctype=(\d+)""").find(url)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val typeLabel = when (type) {
            1 -> "Casual"
            2 -> "Normal"
            3 -> "Hardcore"
            else -> "(Type $type)"
        }
        val gender = Regex("""gender=(\d+)""").find(url)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val genderLabel = when (gender) {
            1 -> "Male"
            2 -> "Female"
            else -> "(Gender $gender)"
        }
        val classId = Regex("""whichclass=(\d+)""").find(url)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val classLabel = reincarnateClassName(classId)
        val signId = Regex("""whichsign=(\d+)""").find(url)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val sign = ZodiacSign.find(signId)
        val signLabel = sign?.signName ?: "(Sign $signId)"
        val pathId = Regex("""whichpath=(\d+)""").find(url)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val path = AscensionPath.fromPathId(pathId)
        val pathLabel = if (path == AscensionPath.NONE || path == AscensionPath.UNKNOWN) {
            "path #$pathId"
        } else {
            path.apiName
        }
        return "Ascend as a $typeLabel $genderLabel $classLabel under the $signLabel sign on $pathLabel, banking $karma Karma."
    }

    /** Desktop AfterLifeRequest.registerRequest whichclass switch (KoL reincarnate ids). */
    internal fun reincarnateClassName(classId: Int): String = when (classId) {
        1 -> "Seal Clubber"
        2 -> "Turtle Tamer"
        3 -> "Pastamancer"
        4 -> "Sauceror"
        5 -> "Disco Bandit"
        6 -> "Accordion Thief"
        11 -> "Avatar of Boris"
        12 -> "Zombie Master"
        14 -> "Avatar of Jarlsberg"
        15 -> "Avatar of Sneaky Pete"
        17 -> "Ed the Undying"
        18 -> "Cow Puncher"
        19 -> "Beanslinger"
        20 -> "Snake Oiler"
        23 -> "Gelatinous Noob"
        24 -> "Vampyre"
        25 -> "Plumber"
        27 -> "Grey Goo"
        28 -> "Pig Skinner"
        29 -> "Cheese Wizard"
        30 -> "Jazz Agent"
        31 -> "WereProfessor"
        32 -> "Zootomist"
        else -> CharacterClass.fromId(classId).let { clazz ->
            if (clazz == CharacterClass.UNKNOWN) "(Class $classId)" else clazz.displayName
        }
    }
}

private fun Preferences.incrementInt(key: String, delta: Int) {
    setInt(key, getInt(key, 0) + delta)
}
