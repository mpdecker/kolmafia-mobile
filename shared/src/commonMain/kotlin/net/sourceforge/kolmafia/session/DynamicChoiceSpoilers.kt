package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.adventure.choice.ChoiceAdventures
import net.sourceforge.kolmafia.adventure.choice.ChoiceOption
import net.sourceforge.kolmafia.character.CharacterClass
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.QuestDatabase

/**
 * Desktop [ChoiceAdventures.dynamicChoiceSpoilers] / [dynamicChoiceOptions] router
 * (Behavioral Deepen XXVIII).
 *
 * Inventory/prefs providers are set from [GameRuntimeLibrary] so static managers
 * can emit inventory-aware spoiler text without DI constructors.
 */
object DynamicChoiceSpoilers {

    var preferences: Preferences? = null
    var questDatabase: QuestDatabase? = null
    var itemCount: (Int) -> Int = { 0 }
    var ascensions: () -> Int = { 0 }
    var characterClass: () -> CharacterClass? = { null }

    // Desktop ItemPool ids used by dynamic spoilers.
    const val BEAUTIFUL_SOUP = 4511
    const val WALRUS_ICE_CREAM = 4510
    const val HUMPTY_DUMPLINGS = 4514
    const val LOBSTER_QUA_GRILL = 4515
    const val MISSING_WINE = 4516
    const val NOSTRIL_OF_THE_SERPENT = 5645
    const val WAX_BANANA = 6492
    const val REPLICA_KEY = 6494
    const val OLD_DRY_BONE = 6540

    fun choiceSpoilers(choice: Int): ChoiceAdventures.Spoilers? {
        if (choice <= 0) return null
        HaciendaManager.getSpoilers(choice)?.let { return it }
        when (choice) {
            360 -> return wumpusSpoilers()
            442 -> return rabbitHoleSpoilers()
            579 -> return suchGreatHeightsSpoilers()
            in 721..724 -> return dreadsylvaniaCabinSpoilers(choice)
        }
        return null
    }

    private fun wumpusSpoilers(): ChoiceAdventures.Spoilers {
        val warnings = WumpusManager.dynamicChoiceOptions()
        val options = if (warnings.isEmpty()) {
            listOf(ChoiceOption(""), ChoiceOption(""))
        } else {
            warnings.map { ChoiceOption(it) }
        }
        return ChoiceAdventures.Spoilers(360, "The Jungles of Ancient Loathing", options)
    }

    private fun rabbitHoleSpoilers(): ChoiceAdventures.Spoilers {
        var count = 0
        if (itemCount(BEAUTIFUL_SOUP) > 0) count++
        if (itemCount(LOBSTER_QUA_GRILL) > 0) count++
        if (itemCount(MISSING_WINE) > 0) count++
        if (itemCount(WALRUS_ICE_CREAM) > 0) count++
        if (itemCount(HUMPTY_DUMPLINGS) > 0) count++
        val options = listOf(
            ChoiceOption("Seal Clubber/Pastamancer item, or yellow matter custard"),
            ChoiceOption("Sauceror/Accordion Thief item, or delicious comfit?"),
            ChoiceOption("Disco Bandit/Turtle Tamer item, or fight croqueteer"),
            ChoiceOption("you have $count/5 of the items needed for an ittah bittah hookah"),
            ChoiceOption("get a chess cookie"),
            ChoiceOption("skip adventure"),
        )
        return ChoiceAdventures.Spoilers(442, "Rabbit Hole", options)
    }

    private fun suchGreatHeightsSpoilers(): ChoiceAdventures.Spoilers {
        val prefs = preferences
        val haveNostril = itemCount(NOSTRIL_OF_THE_SERPENT) > 0
        val asc = ascensions()
        val gainNostril = !haveNostril &&
            (prefs?.getInt("lastTempleButtonsUnlock", 0) ?: 0) != asc
        val templeAdvs = (prefs?.getInt("lastTempleAdventures", 0) ?: 0) == asc
        val options = listOf(
            ChoiceOption("mysticality substats"),
            if (gainNostril) ChoiceOption("gain the Nostril of the Serpent")
            else ChoiceOption("skip adventure"),
            if (templeAdvs) ChoiceOption("skip adventure")
            else ChoiceOption("gain 3 adventures"),
        )
        return ChoiceAdventures.Spoilers(579, "Such Great Heights", options)
    }

    private fun dreadsylvaniaCabinSpoilers(choice: Int): ChoiceAdventures.Spoilers? {
        val muscle = characterClass()?.let {
            it == CharacterClass.SEAL_CLUBBER || it == CharacterClass.TURTLE_TAMER
        } == true
        val accordion = characterClass() == CharacterClass.ACCORDION_THIEF
        val bones = itemCount(OLD_DRY_BONE)
        val replica = itemCount(REPLICA_KEY)
        val banana = itemCount(WAX_BANANA)
        val prefs = preferences
        val ghostPencil = when (choice) {
            721 -> prefs?.getBoolean("ghostPencil1", false) == true
            else -> false
        }
        val options = when (choice) {
            721 -> {
                val kitchen = buildString {
                    append("dread tarragon")
                    if (muscle) append(", old dry bone ($bones) -> bone flour")
                    append(", -stench")
                }
                val cellar = "Freddies, Bored Stiff (+100 spooky damage), " +
                    "replica key ($replica) -> Dreadsylvanian auditor's badge, " +
                    "wax banana ($banana) -> complicated lock impression"
                val attic = buildString {
                    append("locked: -spooky")
                    if (accordion) append(" + intricate music box parts")
                    append(", fewer werewolves, fewer vampires, +Moxie")
                }
                listOf(
                    ChoiceOption(kitchen),
                    ChoiceOption(cellar),
                    ChoiceOption(attic),
                    ChoiceOption(""),
                    ChoiceOption(if (ghostPencil) "shortcut (ghost pencil)" else "learn shortcut"),
                    ChoiceOption("Leave this noncombat"),
                )
            }
            722 -> listOf(
                ChoiceOption("dread tarragon"),
                ChoiceOption("old dry bone ($bones) -> bone flour"),
                ChoiceOption("-stench"),
                ChoiceOption(""),
                ChoiceOption(""),
                ChoiceOption("Return to The Cabin"),
            )
            723 -> listOf(
                ChoiceOption("Freddies"),
                ChoiceOption("Bored Stiff (+100 spooky damage)"),
                ChoiceOption("replica key ($replica) -> Dreadsylvanian auditor's badge"),
                ChoiceOption("wax banana ($banana) -> complicated lock impression"),
                ChoiceOption(""),
                ChoiceOption("Return to The Cabin"),
            )
            724 -> listOf(
                ChoiceOption(
                    buildString {
                        append("-spooky")
                        if (accordion) append(" + intricate music box parts")
                    },
                ),
                ChoiceOption("fewer werewolves"),
                ChoiceOption("fewer vampires"),
                ChoiceOption("+Moxie"),
                ChoiceOption(""),
                ChoiceOption("Return to The Cabin"),
            )
            else -> return null
        }
        val name = when (choice) {
            721 -> "The Cabin in the Dreadsylvanian Woods"
            722 -> "The Kitchen in the Woods"
            723 -> "What Lies Beneath (the Cabin)"
            724 -> "Where it's Attic"
            else -> "Dreadsylvania"
        }
        return ChoiceAdventures.Spoilers(choice, name, options)
    }
}
