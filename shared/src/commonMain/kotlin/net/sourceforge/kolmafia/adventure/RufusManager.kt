package net.sourceforge.kolmafia.adventure

import net.sourceforge.kolmafia.adventure.choice.ChoiceUtilities
import net.sourceforge.kolmafia.adventure.choice.ItemPool
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase

class RufusManager(private val preferences: Preferences) {

    val questType: String
        get() = preferences.getString(Preferences.RUFUS_QUEST_TYPE, "entity").lowercase()

    /**
     * Choice 1498: maps the desired quest type to an option number.
     * Desktop option order: 1=entity, 2=artifact, 3=monument.
     * Returns null if the desired type's label is not found in [responseText].
     */
    fun chooseQuestOption(responseText: String): Int? {
        val desired = questType
        val optionLabels = listOf(
            "entity"   to 1,
            "artifact" to 2,
            "monument" to 3,
        )
        return optionLabels
            .firstOrNull { (label, _) ->
                label == desired && responseText.contains(label, ignoreCase = true)
            }
            ?.second
    }

    /** Store the target name after quest is accepted (extracted from post-choice response). */
    fun recordQuestTarget(target: String) {
        preferences.setString(Preferences.RUFUS_QUEST_TARGET, target)
    }

    fun specialChoiceDecision(
        choiceId: Int,
        responseText: String,
        questDatabase: QuestDatabase,
    ): Int? = when (choiceId) {
        1498 -> if (questDatabase.getProgress(Quest.RUFUS) == "step1") 1 else 6
        1499 -> shadowLabyrinthChoiceDecision(responseText, questDatabase)
        else -> null
    }

    fun shadowLabyrinthChoiceDecision(
        responseText: String,
        questDatabase: QuestDatabase,
    ): Int {
        val artifactQuest = questDatabase.getProgress(Quest.RUFUS) == QuestDatabase.STARTED &&
            questType == "artifact"
        val needed = if (artifactQuest) {
            ShadowTheme.findArtifact(preferences.getString(Preferences.RUFUS_QUEST_TARGET, ""))
        } else {
            ShadowTheme.findGoal(preferences.getString("shadowLabyrinthGoal", ""))
        } ?: return 0

        val choices = ChoiceUtilities.parseChoices(responseText)
        for (option in 2..4) {
            val text = choices[option] ?: continue
            if (shadowLabyrinthTheme(text) == needed) return option
        }
        return 1
    }

    fun shadowLabyrinthTheme(text: String): ShadowTheme? {
        val the = text.indexOf(" the ")
        val place = text.lastIndexOf(' ')
        if (the == -1 || place == -1 || place <= the) return null
        val adjective = text.substring(the + 5, place)
        return ShadowTheme.adjectiveToTheme[adjective]
    }

    /**
     * Parse Rufus quest-log detail text and update quest prefs.
     * Returns a quest step for [QuestDatabase.advance], or null when text is unrelated.
     */
    fun handleQuestLog(details: String, gameDatabase: GameDatabase? = null): String? {
        val text = details.trim()
        if (text.startsWith("Rufus wants you")) {
            RUFUS_ENTITY_PATTERN.find(text)?.let { match ->
                preferences.setString(Preferences.RUFUS_QUEST_TYPE, "entity")
                preferences.setString(Preferences.RUFUS_QUEST_TARGET, match.groupValues[1])
                return QuestDatabase.STARTED
            }
            RUFUS_ARTIFACT_PATTERN.find(text)?.let { match ->
                preferences.setString(Preferences.RUFUS_QUEST_TYPE, "artifact")
                preferences.setString(Preferences.RUFUS_QUEST_TARGET, match.groupValues[1])
                return QuestDatabase.STARTED
            }
            RUFUS_ITEMS_PATTERN.find(text)?.let { match ->
                preferences.setString(Preferences.RUFUS_QUEST_TYPE, "items")
                val items = match.groupValues[1]
                val itemName = resolveItemName(items, gameDatabase)
                preferences.setString(Preferences.RUFUS_QUEST_TARGET, itemName)
                return QuestDatabase.STARTED
            }
            return QuestDatabase.STARTED
        }
        if (text.startsWith("Call Rufus")) {
            if (RUFUS_ENTITY_DONE_PATTERN.containsMatchIn(text)) {
                preferences.setString(Preferences.RUFUS_QUEST_TYPE, "entity")
                return "step1"
            }
            RUFUS_ARTIFACT_DONE_PATTERN.find(text)?.let { match ->
                preferences.setString(Preferences.RUFUS_QUEST_TYPE, "artifact")
                preferences.setString(Preferences.RUFUS_QUEST_TARGET, match.groupValues[1])
                return "step1"
            }
            RUFUS_ITEMS_DONE_PATTERN.find(text)?.let { match ->
                preferences.setString(Preferences.RUFUS_QUEST_TYPE, "items")
                val items = match.groupValues[1]
                val itemName = resolveItemName(items, gameDatabase)
                preferences.setString(Preferences.RUFUS_QUEST_TARGET, itemName)
                return "step1"
            }
            return "step1"
        }
        return null
    }

    private fun resolveItemName(items: String, gameDatabase: GameDatabase?): String {
        gameDatabase?.item(items)?.name?.let { return it }
        ItemDatabase.getByPluralOrName(items)?.name?.let { return it }
        return items
    }

    fun handleShadowRiftFight(monsterName: String) {
        if (monsterName.lowercase() in SHADOW_BOSSES) {
            preferences.setInt("encountersUntilSRChoice", DEFAULT_SR_ENCOUNTERS)
        }
        val current = preferences.getInt("encountersUntilSRChoice", DEFAULT_SR_ENCOUNTERS)
        preferences.setInt("encountersUntilSRChoice", maxOf(0, current - 1))
    }

    /**
     * Shadow Rift non-combat choices do not advance the zone turn counter.
     * Choice 1500 consumes Rufus's shadow lodestone.
     */
    fun handleShadowRiftNC(choiceId: Int, inventoryManager: InventoryManager? = null) {
        when (choiceId) {
            1499 -> preferences.setInt("encountersUntilSRChoice", DEFAULT_SR_ENCOUNTERS)
            1500 -> inventoryManager?.consumeItemLocally(ItemPool.RUFUS_SHADOW_LODESTONE, 1)
        }
    }

    enum class ShadowTheme(val goal: String, val artifact: String?) {
        FIRE("muscle", "shadow lighter"),
        MATH("mysticality", "shadow heptahedron"),
        WATER("moxie", "shadow bucket"),
        TIME("effects", null),
        BLOOD("maxHP", "shadow heart"),
        COLD("maxMP", "shadow snowflake"),
        GHOST("resistance", "shadow wave"),
        ;

        companion object {
            private val goalToTheme = entries.associateBy { it.goal }
            private val artifactToTheme = entries.mapNotNull { theme ->
                theme.artifact?.let { it to theme }
            }.toMap()

            val adjectiveToTheme: Map<String, ShadowTheme> = buildMap {
                putAll(fireAdjectives().associateWith { FIRE })
                putAll(mathAdjectives().associateWith { MATH })
                putAll(waterAdjectives().associateWith { WATER })
                putAll(timeAdjectives().associateWith { TIME })
                putAll(bloodAdjectives().associateWith { BLOOD })
                putAll(coldAdjectives().associateWith { COLD })
                putAll(ghostAdjectives().associateWith { GHOST })
            }

            fun findGoal(goal: String): ShadowTheme? = goalToTheme[goal]
            fun findArtifact(artifact: String): ShadowTheme? = artifactToTheme[artifact]

            private fun fireAdjectives() = listOf(
                "blazing", "blistering", "burning", "burnt", "charred", "ember-lit",
                "flame-choked", "scalded", "scalding", "scorched", "scorching", "seared",
                "singed", "sizzling", "smoldering", "steaming", "white-hot",
            )

            private fun mathAdjectives() = listOf(
                "algebraic", "angular", "binomial", "boolean", "Cartesian", "cubic",
                "decimal", "divided", "Euclidean", "exponential", "Fibonacci", "fractal",
                "fractional", "geometric", "hyperbolic", "integer", "irrational",
                "logarithmic", "monomial", "multiplicative", "ordinal", "parabolic",
                "periodic", "prime", "Pythagorean", "quadratic", "Riemannian",
                "self-referential", "sinusoidal", "trigonometric", "vector",
            )

            private fun waterAdjectives() = listOf(
                "aqueous", "damp", "drenched", "dripping", "drowned", "drowning", "foggy",
                "humid", "moist", "runny", "soaked", "sodden", "underwater", "wet",
                "water-logged", "watery",
            )

            private fun timeAdjectives() = listOf(
                "ancient", "antique", "broken-down", "crumbling", "decaying", "derelict",
                "dilapidated", "old", "ramshackle", "rickety", "ruined", "shabby", "unkempt",
            )

            private fun bloodAdjectives() = listOf(
                "bleeding", "blood-drenched", "blood-soaked", "bloodstained", "bloody",
                "crimson", "hematic", "pulsing", "sanguine", "vein-shot", "veiny",
            )

            private fun coldAdjectives() = listOf(
                "arctic", "chilly", "cold-numbed", "freezing", "frigid", "frost-rimed",
                "frosty", "frozen", "hyperborean", "iced-over", "icy", "snow-covered", "wintry",
            )

            private fun ghostAdjectives() = listOf(
                "diaphanous", "ephemeral", "ghostly", "gossamer", "half-there", "insubstantial",
                "nearly invisible", "see-through", "spectral", "translucent", "transparent", "wispy",
            )
        }
    }

    companion object {
        private const val DEFAULT_SR_ENCOUNTERS = 11
        private val SHADOW_BOSSES = setOf(
            "shadow spire",
            "shadow orrery",
            "shadow tongue",
            "shadow scythe",
            "shadow cauldron",
            "shadow matrix",
        )
        private val RUFUS_ENTITY_PATTERN = Regex("""defeat a (.*?)\.""")
        private val RUFUS_ENTITY_DONE_PATTERN = Regex("""you defeated that monster\.""")
        private val RUFUS_ARTIFACT_PATTERN = Regex("""find a (.*?)\.""")
        private val RUFUS_ARTIFACT_DONE_PATTERN = Regex("""you found his (.*?)\.""")
        private val RUFUS_ITEMS_PATTERN = Regex("""find him 3 (.*?) from Shadow Rifts\.""")
        private val RUFUS_ITEMS_DONE_PATTERN = Regex("""you've got the 3 (.*?) he wanted\.""")
    }
}
