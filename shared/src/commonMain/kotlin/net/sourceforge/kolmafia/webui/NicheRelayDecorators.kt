package net.sourceforge.kolmafia.webui

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop niche Relay decorators: Nemesis raver dance, Villain Lair color/symbology,
 * Memories element prefill, Clan Fortune question/answer fill, Mine sparkle deepen.
 */
object NemesisDecorator {
    private val SPECIAL_MOVES = listOf(
        arrayOf(
            "Breakdancing Raver",
            "Break It On Down",
            "dbNemesisSkill1",
            "the raver drops to the ground and whirls his legs around like a windmill",
            "The raver drops to the ground and starts spinning his legs wildly",
        ),
        arrayOf(
            "Pop-and-Lock Raver",
            "Pop and Lock It",
            "dbNemesisSkill2",
            "The raver's movements suddenly became spastic and jerky",
            "The raver's movements suddenly become spastic and jerky",
        ),
        arrayOf(
            "Running Man",
            "Run Like the Wind",
            "dbNemesisSkill3",
            "You watch him go, and soon realize he isn't actually running anywhere",
            "You start to give chase, but stop short when you realize that he hasn't actually gone anywhere at all",
        ),
    )

    fun decorateRaverFight(buffer: StringBuilder, monsterName: String?, preferences: Preferences?) {
        if (monsterName.isNullOrBlank()) return
        val moves = SPECIAL_MOVES.firstOrNull { it[0].equals(monsterName, ignoreCase = true) }
            ?: return
        val skill = moves[1]
        val setting = moves[2]
        val known = preferences?.getBoolean("hasSkill_$skill", false) == true
        if (known) return
        val count = preferences?.getString(setting, "0").orEmpty()
        val hint = "$skill ($count)"
        val hit = moves[3]
        val miss = moves[4]
        val text = buffer.toString()
        when {
            text.contains(hit) ->
                RequestEditorKit.replaceOnce(buffer, hit, "$hit <font size=1 color=red>[$hint]</font>")
            text.contains(miss) ->
                RequestEditorKit.replaceOnce(buffer, miss, "$miss <font size=1>[$hint]</font>")
        }
        if (buffer.indexOf("mafia-nemesis") < 0) {
            RequestEditorKit.insertBefore(
                buffer,
                "</body>",
                """<div class="mafia-nemesis">Raver dance: $hint</div>""",
            )
        }
    }

    fun isRaver(monster: String): Boolean =
        SPECIAL_MOVES.any { it[0].equals(monster, ignoreCase = true) }
}

object VillainLairDecorator {
    private val LABEL_PATTERN = Regex(
        """from top to bottom:<br\s*/?\s*><center>"(.*?)"<br\s*/?\s*>"(.*?)"<br\s*/?\s*>"(.*?)"</center>""",
        RegexOption.IGNORE_CASE,
    )
    private val SYMBOLOGY_OPTIONS = listOf(
        "Vent Poisonous Gas", "Monorail Shutdown", "Roaring Fire", "Poison Gas",
    )
    private val GREEN_CLUES = listOf(
        "aqua button", "disconnect the jade", "remove the Sub", "press the periwinkle",
        "moss", "disable the indigo button", "hit the pine button", "tangerine",
        "bathrooms are full",
    )
    private val BLUE_CLUES = listOf(
        "press the navy button", "don't hit the navy", "green means alert",
        "magma heating system", "off the gondola", "no pay", "avoid pressing the green",
        "press the vermilion", "Jello", "pumpkin-colored", "engage the flood-wash", "seafoam",
    )
    private val ORANGE_CLUES = listOf(
        "powder blue", "pine button sounds", "silo",
    )

    fun symbology(responseText: String): String {
        if (!responseText.contains("Symbology")) return "0"
        val m = LABEL_PATTERN.find(responseText) ?: return "0"
        for (option in SYMBOLOGY_OPTIONS) {
            for (i in 1..3) {
                if (option == m.groupValues[i]) return i.toString()
            }
        }
        return "0"
    }

    fun parseColorClue(text: String, preferences: Preferences?) {
        val prefs = preferences ?: return
        when {
            GREEN_CLUES.any { text.contains(it) } -> prefs.setString("_villainLairColor", "green")
            BLUE_CLUES.any { text.contains(it) } -> prefs.setString("_villainLairColor", "blue")
            ORANGE_CLUES.any { text.contains(it) } -> prefs.setString("_villainLairColor", "orange")
        }
    }

    fun spoilColorChoice(preferences: Preferences?): String =
        when (preferences?.getString("_villainLairColor", "").orEmpty()) {
            "blue" -> "1"
            "green" -> "2"
            "orange" -> "3"
            else -> "0"
        }

    fun decorate(buffer: StringBuilder, preferences: Preferences?) {
        parseColorClue(buffer.toString(), preferences)
        val color = preferences?.getString("_villainLairColor", "").orEmpty()
        val sym = symbology(buffer.toString())
        if (color.isEmpty() && sym == "0") return
        if (buffer.indexOf("mafia-villainlair") >= 0) return
        RequestEditorKit.insertBefore(
            buffer,
            "</body>",
            """<div class="mafia-villainlair">Color=$color Symbology option=$sym → ${spoilColorChoice(preferences)}</div>""",
        )
    }
}

object MemoriesDecorator {
    private val ELEMENT_PATTERN = Regex(
        """<select name="slot[12345]">.*?</select>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private const val JERKS = "absolute jerks. </p>"
    private const val SECRET =
        """<center><table class="item" style="float: none" rel="id=4114&s=0&q=0&d=0&g=0&t=0&n=1"><tr><td><img src="/images/itemimages/futurebox.gif" alt="secret from the future" title="secret from the future" class=hand></td><td valign=center class=effect>You acquire an item: <b>secret from the future</b></td></tr></table></center>"""

    fun decorateElements(choice: Int, buffer: StringBuilder) {
        if (choice != 392) return
        var text = buffer.toString()
        for (element in listOf("sleaze", "spooky", "stench", "cold", "hot")) {
            val m = ELEMENT_PATTERN.find(text) ?: break
            val old = m.value
            if (old.contains("selected>$element") || old.contains("selected >$element")) {
                // already selected — advance past this select by replacing once with itself tagged
                text = text.replaceFirst(old, old.replace("<select", "<select data-mafia-done=\"$element\""))
                continue
            }
            val neu = old.replace(">$element", " selected>$element")
            text = text.replaceFirst(old, neu)
        }
        buffer.setLength(0)
        buffer.append(text)
    }

    fun decorateElementsResponse(buffer: StringBuilder) {
        val idx = buffer.indexOf(JERKS)
        if (idx >= 0 && buffer.indexOf("secret from the future") < 0) {
            buffer.insert(idx + JERKS.length, SECRET)
        }
    }
}

object ClanFortuneDecorator {
    private val QUESTION_PATTERN = Regex(
        """name="q(\d)" required (?:value="(.*?)"|)""",
    )

    fun decorateQuestion(buffer: StringBuilder, preferences: Preferences?) {
        fillFields(buffer, preferences, "clanFortuneWord")
    }

    fun decorateAnswer(buffer: StringBuilder, preferences: Preferences?) {
        fillFields(buffer, preferences, "clanFortuneReply")
    }

    private fun fillFields(buffer: StringBuilder, preferences: Preferences?, prefix: String) {
        val prefs = preferences ?: return
        val q = Array(3) { i -> prefs.getString("$prefix${i + 1}", "") }
        if (q.all { it.isEmpty() }) return
        var text = buffer.toString()
        for (m in QUESTION_PATTERN.findAll(text).toList()) {
            val num = m.groupValues[1].toIntOrNull() ?: continue
            if (num !in 1..3) continue
            val existing = m.groupValues.getOrNull(2)
            val fill = q[num - 1]
            if (fill.isEmpty() || !existing.isNullOrEmpty()) continue
            val find = """name="q$num" required """
            val replace = """name="q$num" required value="$fill""""
            text = text.replaceFirst(find, replace)
        }
        buffer.setLength(0)
        buffer.append(text)
    }
}

object MineDecoratorDepth {
    private val SPARKLE = Regex(
        """alt=["']sparkle["']""",
        RegexOption.IGNORE_CASE,
    )

    fun decorate(buffer: StringBuilder) {
        var text = buffer.toString()
        text = SPARKLE.replace(text) { """alt="sparkle" class="mafia-mine-sparkle" title="Mine sparkle"""" }
        // Mark open squares for headless mining helpers
        text = text.replace(
            """mining.php?mine=""",
            """mining.php?mine=""",
        )
        if (!text.contains("mafia-mine-depth")) {
            text = text.replace(
                "</body>",
                """<span class="mafia-mine-depth"></span></body>""",
                ignoreCase = true,
            )
        }
        buffer.setLength(0)
        buffer.append(text)
    }
}
