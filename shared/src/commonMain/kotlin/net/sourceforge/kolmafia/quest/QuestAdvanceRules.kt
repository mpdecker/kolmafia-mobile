package net.sourceforge.kolmafia.quest

/**
 * Inline quest step bumps from adventure response text (before full questlog sync).
 * High-traffic council/quest signals only — full sync remains the fallback.
 */
object QuestAdvanceRules {

    data class Rule(val prefKey: String, val step: String, val signal: String)

    private val rules = listOf(
        Rule(Quest.LARVA.prefKey, QuestDatabase.STARTED, "You acquire an item: larva"),
        Rule(Quest.RAT.prefKey, QuestDatabase.STARTED, "You acquire an item: rat appendix"),
        Rule(Quest.BAT.prefKey, QuestDatabase.STARTED, "You acquire an item: bat guano"),
        Rule(Quest.GOBLIN.prefKey, QuestDatabase.STARTED, "You acquire an item: goblin ear"),
        Rule(Quest.FRIAR.prefKey, QuestDatabase.STARTED, "You acquire an item: bottle of wine"),
        Rule(Quest.CYRPT.prefKey, QuestDatabase.STARTED, "You acquire an item: dusty bone"),
        Rule(Quest.TRAPPER.prefKey, QuestDatabase.STARTED, "You acquire an item: yeti skin"),
        Rule(Quest.GARBAGE.prefKey, QuestDatabase.STARTED, "You acquire an item: swanky swag"),
        Rule(Quest.TOPPING.prefKey, QuestDatabase.STARTED, "You acquire an item: rainbow aglet"),
        Rule(Quest.FRIAR.prefKey, "step2", "You acquire an item: hot buttered rum"),
        Rule(Quest.CYRPT.prefKey, "step2", "You acquire an item: dusty bone"),
        Rule(Quest.TRAPPER.prefKey, "step2", "You acquire an item: yeti skin"),
        Rule(Quest.MANOR.prefKey, QuestDatabase.STARTED, "You acquire an item: Spookyraven library key"),
        Rule(Quest.PALINDOME.prefKey, QuestDatabase.STARTED, "You acquire an item: photograph of a dog"),
        Rule(Quest.GOBLIN.prefKey, QuestDatabase.FINISHED, "You have slain the Goblin King"),
        Rule(Quest.BAT.prefKey, QuestDatabase.FINISHED, "You have slain the Boss Bat"),
        Rule(Quest.FRIAR.prefKey, QuestDatabase.FINISHED, "You have cleansed the taint"),
        Rule(Quest.CYRPT.prefKey, QuestDatabase.FINISHED, "You acquire an item: boné"),
        Rule(Quest.TRAPPER.prefKey, "step2", "You acquire an item: Groar's fur"),
        Rule(Quest.TRAPPER.prefKey, QuestDatabase.FINISHED, "peace to Mt. McLargeHuge"),
        Rule(Quest.MANOR.prefKey, QuestDatabase.FINISHED, "You acquire an item: Eye of Ed"),
        Rule(Quest.BLACK.prefKey, QuestDatabase.STARTED, "You acquire an item: black paint"),
        Rule(Quest.WORSHIP.prefKey, QuestDatabase.STARTED, "You acquire an item: mysterious incantation"),
        Rule(Quest.PYRAMID.prefKey, QuestDatabase.STARTED, "You acquire an item: wet stunt nut stew"),
        Rule(Quest.SHEN.prefKey, QuestDatabase.STARTED, "You acquire an item: small leather briefcase"),
        Rule(Quest.GARBAGE.prefKey, QuestDatabase.FINISHED, "You have stopped the rain of giant garbage"),
        Rule(Quest.MACGUFFIN.prefKey, QuestDatabase.FINISHED, "You have unlocked the Forbidden Zone"),
        Rule(Quest.FINAL.prefKey, QuestDatabase.FINISHED, "You have defeated the Naughty Sorceress"),
    )

    /** Apply matching rules; returns true if any quest was advanced. */
    fun apply(responseText: String, questDatabase: QuestDatabase): Boolean {
        var advanced = false
        for (rule in rules) {
            if (!responseText.contains(rule.signal, ignoreCase = true)) continue
            val current = questDatabase.progressFor(rule.prefKey)
            if (QuestDatabase.stepOrdinal(rule.step) > QuestDatabase.stepOrdinal(current)) {
                questDatabase.setProgressByPrefKey(rule.prefKey, rule.step)
                advanced = true
            }
        }
        return advanced
    }
}
