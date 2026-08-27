package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.adventure.AdventureManager
import net.sourceforge.kolmafia.adventure.choice.ChoiceUtilities
import net.sourceforge.kolmafia.character.CharacterClass
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.session.ChoiceCombatAshState
import net.sourceforge.kolmafia.session.FightCombatModeSync
import net.sourceforge.kolmafia.skill.SkillType
import net.sourceforge.kolmafia.track.TrackManager

/**
 * AshP889–AshP897 — Combat / choice ASH surface (Track A).
 */
internal fun GameRuntimeLibrary.registerAshP889Batch(scope: AshScope) {
    regFn(scope, "current_round", AshType.INT, emptyList()) { _, _ ->
        AshValue.of(ChoiceCombatAshState.currentRound)
    }
    regFn(scope, "handling_choice", AshType.BOOLEAN, emptyList()) { _, _ ->
        AshValue.of(
            ChoiceCombatAshState.handlingChoice ||
                adventureManager?.inChoiceResolution == true,
        )
    }
    // Phase 1371+: desktop FightRequest.wonInitiative()
    regFn(scope, "won_initiative", AshType.BOOLEAN, emptyList()) { _, _ ->
        AshValue.of(FightCombatModeSync.wonInitiativeThisFight())
    }
}

internal fun GameRuntimeLibrary.registerAshP890Batch(scope: AshScope) {
    regFn(scope, "last_choice", AshType.INT, emptyList()) { _, _ ->
        val fromState = ChoiceCombatAshState.lastChoice
        if (fromState != 0) AshValue.of(fromState)
        else AshValue.of(preferences?.getInt(AdventureManager.LAST_CHOICE_ID, 0) ?: 0)
    }
    regFn(scope, "last_decision", AshType.INT, emptyList()) { _, _ ->
        AshValue.of(ChoiceCombatAshState.lastDecision)
    }
}

internal fun GameRuntimeLibrary.registerAshP891Batch(scope: AshScope) {
    val intToString = AggregateType(AshType.INT, AshType.STRING)
    fun optionsMap(spoilers: Boolean): AggregateValue {
        val result = AggregateValue(intToString)
        val html = ChoiceCombatAshState.lastChoiceResponseText
        if (html.isBlank()) return result
        val parsed = if (spoilers) {
            ChoiceUtilities.parseChoicesWithSpoilers(html)
        } else {
            ChoiceUtilities.parseChoices(html)
        }
        for ((key, value) in parsed) {
            result[AshValue.of(key)] = AshValue.of(value)
        }
        return result
    }
    regFn(scope, "available_choice_options", intToString, emptyList()) { _, _ ->
        optionsMap(spoilers = false)
    }
    regFn(
        scope,
        "available_choice_options",
        intToString,
        listOf("spoilers" to AshType.BOOLEAN),
    ) { _, args ->
        optionsMap(spoilers = args[0].toBoolean())
    }
}

internal fun GameRuntimeLibrary.registerAshP892Batch(scope: AshScope) {
    fun bufferResult(text: String) = AshValue(AshType.BUFFER, StringBuilder(text))

    fun submitChoice(option: Int, extraFields: String, handleFights: Boolean): String {
        if (ChoiceCombatAshState.choiceFollowsFight || adventureManager?.fightFollowsChoice == true) {
            visitKolPage("choice.php")?.let { ChoiceCombatAshState.noteChoiceVisit(
                ChoiceUtilities.extractChoiceId(it) ?: ChoiceCombatAshState.lastChoice,
                it,
            ) }
        }
        val choiceId = ChoiceCombatAshState.lastChoice.takeIf { it != 0 }
            ?: preferences?.getInt(AdventureManager.LAST_CHOICE_ID, 0)
            ?: 0
        if (!ChoiceCombatAshState.handlingChoice ||
            ChoiceCombatAshState.lastChoiceResponseText.isEmpty() ||
            option == 0
        ) {
            return ChoiceCombatAshState.lastChoiceResponseText
        }
        if (option < 0) {
            return ChoiceCombatAshState.lastChoiceResponseText
        }
        val post = buildString {
            append("whichchoice=$choiceId&option=$option")
            if (extraFields.isNotBlank()) append('&').append(extraFields.trimStart('&'))
        }
        ChoiceCombatAshState.setFormFieldsFromPostData(post)
        val response = visitKolPost("choice.php", post) ?: return ChoiceCombatAshState.lastChoiceResponseText
        ChoiceCombatAshState.noteChoiceDecision(option, response)
        val nextId = ChoiceUtilities.extractChoiceId(response)
        if (nextId != null) {
            ChoiceCombatAshState.noteChoiceVisit(nextId, response)
        } else {
            ChoiceCombatAshState.handlingChoice = false
            ChoiceCombatAshState.lastChoiceResponseText = response
            // When custom=true and a fight follows, allow a single combat step (desktop CHOICE_HANDLER).
            if (handleFights && (
                    ChoiceCombatAshState.fightFollowsChoice ||
                        adventureManager?.fightFollowsChoice == true ||
                        response.contains("fight.php", ignoreCase = true)
                    )
            ) {
                ChoiceCombatAshState.fightFollowsChoice = true
                ChoiceCombatAshState.noteFightStart(response)
            }
        }
        return response
    }

    regFn(scope, "run_choice", AshType.BUFFER, listOf("decision" to AshType.INT)) { _, args ->
        bufferResult(submitChoice(args[0].toLong().toInt(), "", handleFights = true))
    }
    regFn(
        scope,
        "run_choice",
        AshType.BUFFER,
        listOf("decision" to AshType.INT, "custom" to AshType.BOOLEAN),
    ) { _, args ->
        bufferResult(submitChoice(args[0].toLong().toInt(), "", handleFights = args[1].toBoolean()))
    }
    regFn(
        scope,
        "run_choice",
        AshType.BUFFER,
        listOf("decision" to AshType.INT, "extra" to AshType.STRING),
    ) { _, args ->
        bufferResult(submitChoice(args[0].toLong().toInt(), args[1].toString(), handleFights = true))
    }
    regFn(
        scope,
        "run_choice",
        AshType.BUFFER,
        listOf("decision" to AshType.INT, "custom" to AshType.BOOLEAN, "more" to AshType.STRING),
    ) { _, args ->
        bufferResult(
            submitChoice(
                args[0].toLong().toInt(),
                args[2].toString(),
                handleFights = args[1].toBoolean(),
            ),
        )
    }
}

internal fun GameRuntimeLibrary.registerAshP893Batch(scope: AshScope) {
    fun bufferResult(text: String) = AshValue(AshType.BUFFER, StringBuilder(text))

    fun runCombatOnce(): String {
        val inFight = ChoiceCombatAshState.currentRound > 0 ||
            ChoiceCombatAshState.inMultiFight ||
            adventureManager?.inMultiFight == true
        if (!inFight) return ChoiceCombatAshState.lastFightResponseText
        val zoneId = preferences?.getString(net.sourceforge.kolmafia.preferences.Preferences.LAST_LOCATION, "")
            ?.ifBlank { preferences?.getString("lastAdventure", "") }
            .orEmpty()
        val macro = resolveCombatMacro(zoneId.ifBlank { "0" })
        val response = if (macro.isNotBlank()) {
            visitKolFightMacro(macro) ?: ChoiceCombatAshState.lastFightResponseText
        } else {
            visitKolPage("fight.php") ?: ChoiceCombatAshState.lastFightResponseText
        }
        if (response.isNotBlank()) ChoiceCombatAshState.noteFightRound(response)
        return response
    }

    regFn(scope, "run_combat", AshType.BUFFER, emptyList()) { _, _ ->
        ChoiceCombatAshState.combatFilterOverride = null
        bufferResult(runCombatOnce())
    }
    regFn(scope, "run_combat", AshType.BUFFER, listOf("filter_function" to AshType.STRING)) { _, args ->
        ChoiceCombatAshState.combatFilterOverride = args[0].toString().ifBlank { null }
        try {
            bufferResult(runCombatOnce())
        } finally {
            ChoiceCombatAshState.combatFilterOverride = null
        }
    }
    regFn(scope, "run_turn", AshType.BUFFER, emptyList()) { _, _ ->
        when {
            ChoiceCombatAshState.currentRound > 0 || ChoiceCombatAshState.inMultiFight ||
                adventureManager?.inMultiFight == true ->
                bufferResult(runCombatOnce())
            ChoiceCombatAshState.handlingChoice ||
                adventureManager?.inChoiceResolution == true ||
                ChoiceCombatAshState.choiceFollowsFight ->
                bufferResult(ChoiceCombatAshState.lastChoiceResponseText)
            else -> bufferResult("")
        }
    }
}

internal fun GameRuntimeLibrary.registerAshP894Batch(scope: AshScope) {
    fun bufferResult(text: String) = AshValue(AshType.BUFFER, StringBuilder(text))
    fun fightGet(query: String): String {
        val response = visitKolPage("fight.php?$query") ?: ""
        if (response.isNotBlank()) ChoiceCombatAshState.noteFightRound(response)
        return response.ifBlank { ChoiceCombatAshState.lastFightResponseText }
    }

    regFn(scope, "runaway", AshType.BUFFER, emptyList()) { _, _ ->
        bufferResult(fightGet("action=runaway"))
    }
    regFn(scope, "throw_item", AshType.BUFFER, listOf("item" to AshType.ITEM)) { _, args ->
        val id = itemIdFromAsh(args[0])
        bufferResult(fightGet("action=useitem&whichitem=$id"))
    }
    regFn(
        scope,
        "throw_items",
        AshType.BUFFER,
        listOf("item1" to AshType.ITEM, "item2" to AshType.ITEM),
    ) { _, args ->
        val id1 = itemIdFromAsh(args[0])
        val id2 = itemIdFromAsh(args[1])
        bufferResult(fightGet("action=useitem&whichitem=$id1&whichitem2=$id2"))
    }
}

internal fun GameRuntimeLibrary.registerAshP895Batch(scope: AshScope) {
    val stringArray = AggregateType(AshType.INT, AshType.STRING)
    fun stringListToArray(names: List<String>): AggregateValue {
        val result = AggregateValue(AggregateType(AshType.INT, AshType.STRING, names.size))
        names.forEachIndexed { i, name ->
            result[AshValue.of(i)] = AshValue.of(name)
        }
        return result
    }

    regFn(scope, "banished_by", stringArray, listOf("monster" to AshType.MONSTER)) { _, args ->
        val name = args[0].monsterRefName()
        val turn = character?.state?.value?.currentRun ?: 0
        val names = banishManager?.banishedBy(name, turn)?.map { it.canonicalName }.orEmpty()
        stringListToArray(names)
    }
    regFn(scope, "banished_by", stringArray, listOf("monster" to AshType.STRING)) { _, args ->
        val name = args[0].toString()
        val turn = character?.state?.value?.currentRun ?: 0
        val names = banishManager?.banishedBy(name, turn)?.map { it.canonicalName }.orEmpty()
        stringListToArray(names)
    }
    regFn(scope, "tracked_by", stringArray, listOf("monster" to AshType.MONSTER)) { _, args ->
        val name = args[0].monsterRefName()
        val prefs = preferences ?: return@regFn stringListToArray(emptyList())
        val turn = character?.state?.value?.currentRun ?: 0
        stringListToArray(TrackManager.trackedBy(prefs, name, turn))
    }
    regFn(scope, "tracked_by", stringArray, listOf("monster" to AshType.STRING)) { _, args ->
        val name = args[0].toString()
        val prefs = preferences ?: return@regFn stringListToArray(emptyList())
        val turn = character?.state?.value?.currentRun ?: 0
        stringListToArray(TrackManager.trackedBy(prefs, name, turn))
    }
    regFn(scope, "track_copy_count", AshType.INT, listOf("monster" to AshType.MONSTER)) { _, args ->
        val name = args[0].monsterRefName()
        val prefs = preferences ?: return@regFn AshValue.of(0)
        val turn = character?.state?.value?.currentRun ?: 0
        AshValue.of(TrackManager.countCopies(prefs, name, turn).toLong())
    }
    regFn(scope, "track_copy_count", AshType.INT, listOf("monster" to AshType.STRING)) { _, args ->
        val name = args[0].toString()
        val prefs = preferences ?: return@regFn AshValue.of(0)
        val turn = character?.state?.value?.currentRun ?: 0
        AshValue.of(TrackManager.countCopies(prefs, name, turn).toLong())
    }
    regFn(scope, "track_ignore_queue", AshType.BOOLEAN, listOf("monster" to AshType.MONSTER)) { _, args ->
        val name = args[0].monsterRefName()
        val prefs = preferences ?: return@regFn AshValue.of(false)
        val turn = character?.state?.value?.currentRun ?: 0
        AshValue.of(TrackManager.isQueueIgnored(prefs, name, turn))
    }
    regFn(scope, "track_ignore_queue", AshType.BOOLEAN, listOf("monster" to AshType.STRING)) { _, args ->
        val name = args[0].toString()
        val prefs = preferences ?: return@regFn AshValue.of(false)
        val turn = character?.state?.value?.currentRun ?: 0
        AshValue.of(TrackManager.isQueueIgnored(prefs, name, turn))
    }
}

internal fun GameRuntimeLibrary.registerAshP896Batch(scope: AshScope) {
    regFn(scope, "combat_skill_available", AshType.BOOLEAN, listOf("skill" to AshType.SKILL)) { _, args ->
        val skillName = args[0].toString()
        val skills = skillManager?.state?.value?.skills.orEmpty()
        val found = skills.find { it.name.equals(skillName, ignoreCase = true) }
        AshValue.of(found != null && found.type == SkillType.COMBAT)
    }
    regFn(scope, "stun_skill", AshType.SKILL, emptyList()) { _, _ ->
        val cls = character?.state?.value?.characterClassEnum ?: CharacterClass.UNKNOWN
        AshValue.skill(stunSkillForClass(cls))
    }
}

internal fun GameRuntimeLibrary.registerAshP897Batch(scope: AshScope) {
    // Track A corpus registration anchor (no additional symbols).
}

private fun GameRuntimeLibrary.itemIdFromAsh(value: AshValue): Int {
    val name = value.toString()
    if (name.isBlank()) return 0
    return gameDatabase?.item(name)?.id
        ?: ItemDatabase.getByName(name)?.id
        ?: name.toIntOrNull()
        ?: 0
}

private fun stunSkillForClass(cls: CharacterClass): String = when (cls) {
    CharacterClass.SEAL_CLUBBER -> "Club Foot"
    CharacterClass.TURTLE_TAMER -> "Shell Up"
    CharacterClass.PASTAMANCER -> "Entangling Noodles"
    CharacterClass.SAUCEROR -> "Soul Bubble"
    CharacterClass.ACCORDION_THIEF -> "Accordion Bash"
    else -> "none"
}
