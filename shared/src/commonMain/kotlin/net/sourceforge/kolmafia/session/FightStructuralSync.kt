package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.combat.MonsterStatusTracker
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Ordered, headless counterparts for the high-value processNode/processP
 * handlers in desktop FightRequest. The parser is deliberately tolerant and
 * callbacks keep equipment/inventory ownership with the caller.
 */
object FightStructuralSync {
    const val ICEBALL = 3391
    const val GLITCH_ITEM = 10207
    const val SHRUNKEN_HEAD = 12048

    data class Context(
        val html: String,
        val location: String = "",
        val adventureId: String = "",
        val monsterName: String = MonsterStatusTracker.getLastMonsterName(),
        val won: Boolean = false,
        val crimboShrub: Boolean = false,
        val familiarName: String = "",
        val enthronedName: String = "",
        val bjornedName: String = "",
        val preferences: Preferences? = null,
        val inventory: InventoryManager? = null,
        val sessionLogger: SessionLogger? = null,
        val consumeItem: (Int, Int) -> Unit = { id, qty -> inventory?.consumeItemLocally(id, qty) },
        val clearEquipment: (EquipmentSlot) -> Unit = {},
        val itemCount: (Int) -> Int = { id -> inventory?.state?.value?.items?.get(id)?.quantity ?: 0 },
    )

    private val CHAKRA = mapOf(
        "Your Bung Chakra" to "crimbo16BungChakraCleanliness",
        "Your Guts Chakra" to "crimbo16GutsChakraCleanliness",
        "Your Liver Chakra" to "crimbo16LiverChakraCleanliness",
        "Your Nipple Chakra" to "crimbo16NippleChakraCleanliness",
        "Your Nose Chakra" to "crimbo16NoseChakraCleanliness",
        "Your Hat Chakra" to "crimbo16HatChakraCleanliness",
        "Crimbo's Sack" to "crimbo16SackChakraCleanliness",
        "Crimbo's Boots" to "crimbo16BootsChakraCleanliness",
        "Crimbo's Jelly" to "crimbo16JellyChakraCleanliness",
        "Crimbo's Reindeer" to "crimbo16ReindeerChakraCleanliness",
        "Crimbo's Beard" to "crimbo16BeardChakraCleanliness",
        "Crimbo's Hat" to "crimbo16CrimboHatChakraCleanliness",
    )
    private val PEARL_PROGRESS = Regex("""\(([0-9.]+)% progress made towards shiny thing\)""")
    private val PEARL_DONE = listOf(
        "You finally screw your courage to the sticking point and dive into the deeps." to
            "_unblemishedPearlAnemoneMine",
        "You finally overcome your inhibitions enough to grab the urinal treasure." to
            "_unblemishedPearlDiveBar",
        "You finally manage to fight through the stench and grab the shiny thing." to
            "_unblemishedPearlMadnessReef",
        "You finally manage to power through the boiling water and grab the shiny object." to
            "_unblemishedPearlMarinaraTrench",
        "You finally manage to brave the frigid current and retrieve the precious shiny object," to
            "_unblemishedPearlTheBriniestDeepests",
    )
    private val PROSELYTIZATION = Regex("""^\+1 ([^<]+) Proselytization$""")
    private val MEAT = Regex("""(?:gain|got|find|receive) ([\d,]+) Meat""", RegexOption.IGNORE_CASE)

    fun apply(ctx: Context): Boolean {
        val prefs = ctx.preferences ?: return false
        if (ctx.html.isBlank()) return false
        var changed = false
        var dicePending = false
        val document = FightHtmlParser.parse(ctx.html)
        for (event in document.events) {
            val text = when (event) {
                is FightHtmlParser.Event.Paragraph -> event.text
                is FightHtmlParser.Event.Table -> event.text
                is FightHtmlParser.Event.Comment -> event.text
                is FightHtmlParser.Event.HorizontalRule -> ""
            }
            if (text.isBlank()) continue

            if (text.contains("begins to roll.", ignoreCase = true)) {
                dicePending = true
                continue
            }
            if (dicePending) {
                dicePending = false
                val damage = FightDamageParser.parseNormalDamage(text)
                if (damage > 0) MonsterStatusTracker.damageMonster(damage)
                changed = true
                continue
            }

            if (event is FightHtmlParser.Event.Paragraph || event is FightHtmlParser.Event.Table) {
                changed = applyText(text, event.raw, ctx, prefs) || changed
            }
        }
        // These messages can be outside a paragraph in malformed/partial HTML.
        changed = applyPearlDone(FightHtmlParser.text(ctx.html), prefs) || changed
        return changed
    }

    private fun applyText(
        text: String,
        raw: String,
        ctx: Context,
        prefs: Preferences,
    ): Boolean {
        var changed = false
        if (raw.contains("factbook", ignoreCase = true)) {
            if (!text.contains("rythm", ignoreCase = true) &&
                !text.contains("rhythm", ignoreCase = true)
            ) {
                FightSessionLog.logText(text, ctx.sessionLogger)
                when {
                    text.contains("quick cheat sheet", ignoreCase = true) ->
                        prefs.setInt("_bookOfFactsTatters", (prefs.getInt("_bookOfFactsTatters", 0) + 1).coerceAtMost(11))
                    text.contains("stick it up your nose", ignoreCase = true) ->
                        prefs.setInt("_bookOfFactsWishes", (prefs.getInt("_bookOfFactsWishes", 0) + 1).coerceAtMost(3))
                    text.contains("gummy material", ignoreCase = true) ->
                        prefs.setInt("bookOfFactsGummi", (prefs.getInt("bookOfFactsGummi", 0) + 1).coerceAtMost(4))
                    text.contains("piñata", ignoreCase = true) ->
                        prefs.setInt("bookOfFactsPinata", (prefs.getInt("bookOfFactsPinata", 0) + 1).coerceAtMost(2))
                }
                changed = true
            }
        }
        if (ctx.monsterName.equals("Sssshhsssblllrrggghsssssggggrrgglsssshhssslblgl", true) &&
            (text.startsWith("You hear in your mind") ||
                text.startsWith("Combat rages around you") ||
                text.startsWith("You survey the battle around you:"))
        ) {
            FightSessionLog.logText(text, ctx.sessionLogger)
            changed = true
        }
        if (ctx.crimboShrub && Regex("""It's from (.*?)!""").containsMatchIn(text)) {
            FightSessionLog.logText(text, ctx.sessionLogger)
            changed = true
        }
        CHAKRA.entries.firstOrNull { (name, _) -> ctx.location.equals(name, true) }
            ?.value?.let { property ->
                Regex("""This Chakra is now (\d+)% clean\.""").find(text)?.let { match ->
                    prefs.setString(property, match.groupValues[1])
                    FightSessionLog.logText(text, ctx.sessionLogger)
                    changed = true
                }
            }
        if (text.contains("progress made towards shiny thing")) {
            changed = applyPearlProgress(text, prefs) || changed
        }
        if (text.startsWith("You toss your shrunken head at your foe")) {
            ctx.consumeItem(SHRUNKEN_HEAD, 1)
            ctx.clearEquipment(EquipmentSlot.OFFHAND)
            FightSessionLog.logText(text, ctx.sessionLogger)
            changed = true
        }
        PROSELYTIZATION.find(text)?.let { match ->
            val monster = ctx.monsterName.ifBlank { "Your opponent" }
            FightSessionLog.logText("$monster proselytized for the ${match.groupValues[1]} faction.", ctx.sessionLogger)
            changed = true
        }
        if (ctx.enthronedName.isNotBlank() && ctx.bjornedName.isNotBlank() &&
            text.startsWith(ctx.enthronedName) && text.contains(ctx.bjornedName)
        ) {
            FightSessionLog.logText(text, ctx.sessionLogger)
            changed = true
        }
        if (ctx.monsterName == "%monster%" && ctx.won) {
            val meat = MEAT.find(text)?.groupValues?.getOrNull(1)?.replace(",", "")?.toIntOrNull()
            val have = ctx.itemCount(GLITCH_ITEM)
            if (meat != null && have > 0) {
                val count = meat / (5 * have)
                prefs.setInt("glitchItemImplementationCount", count)
                prefs.setInt("glitchItemImplementationLevel", glitchLevel(count))
                changed = true
            }
        }
        if (ctx.location.contains("Outside the Club", true) &&
            (text.contains("special move", true) || text.contains("raver", true))
        ) {
            FightSessionLog.logText(text, ctx.sessionLogger)
            changed = true
        }
        return changed
    }

    private fun applyPearlProgress(text: String, prefs: Preferences): Boolean {
        val property = when {
            text.contains("glint of something", true) -> "_unblemishedPearlAnemoneMineProgress"
            text.contains("stop in the bathroom", true) -> "_unblemishedPearlDiveBarProgress"
            text.contains("see something shiny", true) -> "_unblemishedPearlMadnessReefProgress"
            text.contains("spot something shiny", true) -> "_unblemishedPearlMarinaraTrenchProgress"
            text.contains("catch a glint", true) -> "_unblemishedPearlTheBriniestDeepestsProgress"
            else -> null
        } ?: return false
        val value = PEARL_PROGRESS.find(text)?.groupValues?.getOrNull(1)?.toDoubleOrNull() ?: return false
        val previous = prefs.getString(property, "0").toDoubleOrNull() ?: 0.0
        prefs.setString(property, (previous + value).toString())
        return true
    }

    private fun applyPearlDone(text: String, prefs: Preferences): Boolean {
        val property = PEARL_DONE.firstOrNull { text.startsWith(it.first) }?.second ?: return false
        prefs.setString("${property}Progress", "0.0")
        prefs.setBoolean(property, true)
        return true
    }

    private fun glitchLevel(count: Int): Int = when {
        count >= 111 -> 7
        count >= 69 -> 6
        count >= 37 -> 5
        count >= 11 -> 4
        count >= 4 -> 3
        count >= 2 -> 2
        count >= 1 -> 1
        else -> 0
    }

}
