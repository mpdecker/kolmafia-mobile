package net.sourceforge.kolmafia.adventure.choice

import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.quest.MonorailChoiceSync
import net.sourceforge.kolmafia.session.ChoiceCombatAshState
import net.sourceforge.kolmafia.session.DynamicChoiceSpoilers
import net.sourceforge.kolmafia.session.GoalManager

/**
 * Desktop [ChoiceAdventures] static catalog (Phases 3771–3830).
 *
 * Configurable [ChoiceAdventure] rows plus [ChoiceSpoiler] metadata. Dynamic spoilers
 * (Violet Fog / Louvre / Hacienda / etc.) stay on their existing managers.
 */
object ChoiceAdventures {

    /** Desktop [ChoiceAdventures.SKIP_ADVENTURE]. */
    val SKIP_ADVENTURE: ChoiceOption = ChoiceOption.SKIP_ADVENTURE

    data class Spoilers(
        val choice: Int,
        val name: String,
        val options: List<ChoiceOption?>,
    )

    data class Entry(
        val choice: Int,
        val zone: String,
        val name: String,
        val options: List<ChoiceOption>,
        val configurable: Boolean,
        val ordering: Int = 0,
    ) {
        val property: String get() = "choiceAdventure$choice"
        fun spoilers(): Spoilers = Spoilers(choice, name, options)
    }

    private val adventures = linkedMapOf<Int, Entry>()
    private val spoilers = linkedMapOf<Int, Entry>()

    init {
        registerCatalog001()
        registerCatalog401()
        registerCatalog801()
        registerCatalog1201()
    }

    val configurableCount: Int get() = adventures.size
    val spoilerCount: Int get() = spoilers.size

    fun adventure(choice: Int): Entry? = adventures[choice]
    fun spoiler(choice: Int): Entry? = spoilers[choice]

    fun entry(choice: Int): Entry? = adventures[choice] ?: spoilers[choice]

    fun choiceSpoilers(
        choice: Int,
        html: String = ChoiceCombatAshState.lastChoiceResponseText,
    ): Spoilers? {
        if (choice <= 0) return null
        // Desktop order: dynamic first, then Violet Fog / Louvre / Monorail.
        DynamicChoiceSpoilers.choiceSpoilers(choice)?.let { return it }
        VioletFogManager.choiceSpoilers(choice)?.let { return it }
        LouvreManager.choiceSpoilers(choice)?.let { return it }
        MonorailChoiceSync.choiceSpoilers(choice, html)?.let { return it }
        // Desktop returns null so catalog/solver goals are not appended as spoilers.
        if (choice == 535 || choice == 536 || choice == 546 || choice == 594) return null
        return adventures[choice]?.spoilers() ?: spoilers[choice]?.spoilers()
    }

    /**
     * Desktop [ChoiceAdventure.getOptions] — empty catalog rows fall through to
     * [DynamicChoiceSpoilers.dynamicChoiceOptions].
     */
    fun optionsFor(choice: Int): List<ChoiceOption>? {
        val registered = entry(choice)
        if (registered != null && registered.options.isNotEmpty()) {
            return registered.options
        }
        val dynamic = DynamicChoiceSpoilers.dynamicChoiceOptions(choice)
            .mapNotNull { it }
        if (dynamic.isNotEmpty()) return dynamic
        return registered?.options
    }

    fun findOption(options: Iterable<ChoiceOption?>, decision: Int): ChoiceOption? {
        options.forEachIndexed { index, opt ->
            if (opt != null && opt.decision(index + 1) == decision) return opt
        }
        return null
    }

    fun choiceSpoiler(choice: Int, decision: Int, options: Iterable<ChoiceOption?>? = choiceSpoilers(choice)?.options): ChoiceOption? {
        if (choice == 105 && decision == 3) {
            net.sourceforge.kolmafia.character.KoLCharacter.ensureUpdatedGuyMadeOfBees(
                DynamicChoiceSpoilers.preferences,
                DynamicChoiceSpoilers.ascensions(),
            )
            val prefs = DynamicChoiceSpoilers.preferences
            if (prefs?.getBoolean("guyMadeOfBeesDefeated", false) == true) {
                return ChoiceOption("guy made of bees: defeated")
            }
            val count = prefs?.getString("guyMadeOfBeesCount", "0") ?: "0"
            return ChoiceOption("guy made of bees: called $count times")
        }
        if (choice == 182 && decision == 4) {
            return ChoiceOption("model airship")
        }
        if (options == null) return null
        return findOption(options, decision)
    }

    fun choiceDescription(choice: Int, decision: Int, responseText: String = ChoiceCombatAshState.lastChoiceResponseText): String {
        val spoilers = choiceSpoilers(choice)
        if (spoilers != null) {
            val spoiler = choiceSpoiler(choice, decision, spoilers.options)
            if (spoiler != null) return spoiler.name
        }
        return ChoiceUtilities.parseChoices(responseText)[decision] ?: "unknown"
    }

    /**
     * Desktop [ChoiceManager.pickGoalChoice] — item goals override the configured decision;
     * "complete the outfit" fills the first missing piece.
     */
    fun pickGoalChoice(
        choice: Int,
        decision: Int,
        hasItemGoal: (String) -> Boolean,
        hasItem: (String) -> Boolean,
    ): Int {
        if (decision == 0) return 0
        val options = optionsFor(choice) ?: return decision
        if (options.isEmpty()) return decision
        var anyItems = false
        options.forEachIndexed { index, opt ->
            val first = opt.itemNames.firstOrNull() ?: return@forEachIndexed
            anyItems = true
            if (hasItemGoal(first)) return opt.decision(index + 1)
        }
        if (!anyItems) return decision
        val chosen = findOption(options, decision)
        if (chosen == null || chosen.name != "complete the outfit") return decision
        options.forEachIndexed { index, opt ->
            val first = opt.itemNames.firstOrNull() ?: return@forEachIndexed
            if (!hasItem(first)) return opt.decision(index + 1)
        }
        return 1
    }

    fun pickGoalChoice(
        choice: Int,
        decision: Int,
        goals: GoalManager?,
        inventory: InventoryState?,
    ): Int = pickGoalChoice(
        choice,
        decision,
        hasItemGoal = { name -> hasItemGoalNamed(goals, name) },
        hasItem = { name ->
            val id = ItemDatabase.getByName(name)?.id ?: return@pickGoalChoice false
            (inventory?.items?.get(id)?.quantity ?: 0) > 0
        },
    )

    internal fun registerAdventure(
        choice: Int,
        zone: String,
        name: String,
        options: List<ChoiceOption>,
        ordering: Int = 0,
    ) {
        adventures[choice] = Entry(choice, zone, name, options, configurable = true, ordering = ordering)
    }

    internal fun registerSpoiler(
        choice: Int,
        zone: String,
        name: String,
        options: List<ChoiceOption>,
    ) {
        spoilers[choice] = Entry(choice, zone, name, options, configurable = false)
    }

    private fun hasItemGoalNamed(goals: GoalManager?, name: String): Boolean {
        if (goals == null) return false
        if (goals.hasItemGoalByName(name)) return true
        val id = ItemDatabase.getByName(name)?.id ?: return false
        return goals.hasItemGoal(id)
    }
}
