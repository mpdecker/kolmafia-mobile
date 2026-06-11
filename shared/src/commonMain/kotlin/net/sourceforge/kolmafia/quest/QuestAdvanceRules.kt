package net.sourceforge.kolmafia.quest

/**
 * Inline quest step bumps from adventure response text (before full questlog sync).
 * High-traffic council/quest signals only — full sync remains the fallback.
 */
object QuestAdvanceRules {

    data class Rule(val prefKey: String, val step: String, val signal: String)

    private val rules = listOf(
        Rule(Quest.LARVA.prefKey, QuestDatabase.STARTED, "You acquire an item: larva"),
        Rule(Quest.LARVA.prefKey, QuestDatabase.FINISHED, "Thanks for the larva"),
        Rule(Quest.RAT.prefKey, QuestDatabase.STARTED, "You acquire an item: rat appendix"),
        Rule(Quest.RAT.prefKey, QuestDatabase.FINISHED, "You've solved the rat problem at the Typical Tavern"),
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
        Rule(Quest.GOBLIN.prefKey, QuestDatabase.FINISHED, "Thank you for slaying the Goblin King"),
        Rule(Quest.BAT.prefKey, QuestDatabase.FINISHED, "You have slain the Boss Bat"),
        Rule(Quest.BAT.prefKey, QuestDatabase.FINISHED, "Well done!  You have slain the Boss Bat"),
        Rule(Quest.BAT.prefKey, QuestDatabase.FINISHED, "You really did kill the Boss Bat"),
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
        Rule(Quest.TOPPING.prefKey, QuestDatabase.FINISHED, "pizza delivered to his stupid tower"),
        Rule(Quest.TOPPING.prefKey, "step2", "signal fires"),
        Rule(Quest.WORSHIP.prefKey, QuestDatabase.FINISHED, "claimed his ancient amulet"),
        Rule(Quest.PALINDOME.prefKey, QuestDatabase.FINISHED, "recovered the long-lost Staff of Fats"),
        Rule(Quest.PYRAMID.prefKey, QuestDatabase.FINISHED, "Ed the Undying has fallen"),
        Rule(Quest.SHEN.prefKey, QuestDatabase.FINISHED, "Talisman o' Namsilat from Shen Copperhead"),
        Rule(Quest.BLACK.prefKey, "step3", "travel agency on Desert Beach"),
        Rule(Quest.MACGUFFIN.prefKey, QuestDatabase.STARTED, "find the Black Market"),
        Rule(Quest.MACGUFFIN.prefKey, "step2", "picked up your father's diary"),
        Rule(Quest.BLACK.prefKey, "step2", "forged identification documents"),
        Rule(Quest.MACGUFFIN.prefKey, QuestDatabase.FINISHED, "You have unlocked the Forbidden Zone"),
        Rule(Quest.FINAL.prefKey, QuestDatabase.FINISHED, "You have defeated the Naughty Sorceress"),
        // Council assignment text — bump unstarted → started on first visit
        Rule(Quest.RAT.prefKey, QuestDatabase.STARTED, "owner of The Typical Tavern"),
        Rule(Quest.BAT.prefKey, QuestDatabase.STARTED, "You must slay the Boss Bat"),
        Rule(Quest.GOBLIN.prefKey, QuestDatabase.STARTED, "neutralizing the Goblin King"),
        Rule(Quest.FRIAR.prefKey, QuestDatabase.STARTED, "Deep Fat Friars"),
        Rule(Quest.CYRPT.prefKey, QuestDatabase.STARTED, "Bonerdagon"),
        // Guild quests — signals from desktop GuildRequest.handleGuildQuests()
        Rule(Quest.MEATCAR.prefKey, QuestDatabase.STARTED, "Degrassi Knoll"),
        Rule(Quest.MEATCAR.prefKey, QuestDatabase.FINISHED, "South of the Border"),
        Rule(Quest.CITADEL.prefKey, QuestDatabase.STARTED, "Whitey's Grove"),
        Rule(Quest.FACTORY.prefKey, QuestDatabase.STARTED, "7-foot Dwarves"),
        Rule(Quest.FACTORY.prefKey, QuestDatabase.STARTED, "McLargeHuge"),
        Rule(Quest.EGO.prefKey, QuestDatabase.STARTED, "the location of the Cemetary"),
        Rule(Quest.EGO.prefKey, QuestDatabase.STARTED, "already know where the Cemetary is"),
        Rule(Quest.EGO.prefKey, "step1", "hand over Fernswarthy's key"),
        Rule(Quest.EGO.prefKey, "step1", "returned with Fernswarthy's key"),
        Rule(Quest.EGO.prefKey, "step2", "gives you the key"),
        Rule(Quest.EGO.prefKey, QuestDatabase.FINISHED, "Manual of Dexterity"),
        Rule(Quest.NEMESIS.prefKey, QuestDatabase.STARTED, "The Tomb is within the Misspelled"),
        Rule(Quest.NEMESIS.prefKey, "step5", "Clownlord Beelzebozo"),
        Rule(Quest.NEMESIS.prefKey, "step7", "a Meatsmithing hammer"),
        Rule(Quest.NEMESIS.prefKey, "step10", "in the Big Mountains"),
        Rule(Quest.MUSCLE.prefKey, QuestDatabase.STARTED, "biggest sausage"),
        Rule(Quest.MUSCLE.prefKey, QuestDatabase.FINISHED, "Eleven inches"),
        Rule(Quest.MYST.prefKey, QuestDatabase.STARTED, "poltersandwich"),
        Rule(Quest.MYST.prefKey, QuestDatabase.FINISHED, "captured poltersandwich"),
        Rule(Quest.MOXIE.prefKey, QuestDatabase.STARTED, "check out the Sleazy Back Alley"),
        Rule(Quest.MOXIE.prefKey, QuestDatabase.FINISHED, "stole my own pants"),
        // Guild / misc quest signals
        Rule(Quest.SEA_OLD_GUY.prefKey, QuestDatabase.STARTED, "I lost my favorite boot"),
        Rule(Quest.SEA_OLD_GUY.prefKey, QuestDatabase.STARTED, "old guy by the sea"),
        Rule(Quest.SEA_OLD_GUY.prefKey, QuestDatabase.FINISHED, "The old man snores fitfully"),
        Rule(Quest.SEA_MONKEES.prefKey, QuestDatabase.STARTED, "The Sea Monkees"),
        Rule(Quest.SEA_MONKEES.prefKey, "step1", "wish my big brother was here"),
        Rule(Quest.PARTY_FAIR.prefKey, QuestDatabase.STARTED, "Party Fair"),
        Rule(Quest.TELEGRAM.prefKey, QuestDatabase.STARTED, "telegram for you"),
        Rule(Quest.DOCTOR_BAG.prefKey, QuestDatabase.STARTED, "doctor bag"),
        Rule(Quest.PIRATEREALM.prefKey, QuestDatabase.STARTED, "You grab an eyepatch"),
        Rule(Quest.PIRATEREALM.prefKey, QuestDatabase.STARTED, "Pirate Realm"),
        Rule(Quest.PIRATEREALM.prefKey, QuestDatabase.FINISHED, "an envelope with your name on it"),
        Rule(Quest.GARBAGE.prefKey, "step2", "You acquire an item: swanky swag"),
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
