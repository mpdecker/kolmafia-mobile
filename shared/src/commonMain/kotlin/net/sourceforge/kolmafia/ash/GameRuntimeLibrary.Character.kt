package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.adventure.AdventurePrep
import net.sourceforge.kolmafia.modifiers.StatNames
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase

internal fun GameRuntimeLibrary.registerCharacterExtensions(scope: AshScope) {

    regFn(scope, "my_class", AshType.CLASS, emptyList()) { _, _ ->
        AshValue(AshType.CLASS,
            character?.state?.value?.characterClassEnum?.displayName ?: "")
    }

    regFn(scope, "my_path", AshType.PATH, emptyList()) { _, _ ->
        AshValue(AshType.PATH,
            character?.state?.value?.ascensionPath?.apiName ?: "None")
    }

    regFn(scope, "get_path", AshType.PATH, emptyList()) { _, _ ->
        AshValue(AshType.PATH,
            character?.state?.value?.ascensionPath?.apiName ?: "None")
    }

    regFn(scope, "my_sign", AshType.STRING, emptyList()) { _, _ ->
        AshValue.of(character?.state?.value?.zodiacSign ?: "")
    }

    regFn(scope, "my_primestat", AshType.STAT, emptyList()) { _, _ ->
        AshValue(AshType.STAT,
            character?.state?.value?.characterClassEnum?.primeStatName ?: "Muscle")
    }

    regFn(scope, "in_run", AshType.BOOLEAN, emptyList()) { _, _ ->
        AshValue.of(!(character?.state?.value?.kingLiberated ?: true))
    }

    regFn(scope, "under_standard", AshType.BOOLEAN, emptyList()) { _, _ ->
        AshValue.of(character?.state?.value?.isUnderStandard ?: false)
    }

    regFn(scope, "ascension_number", AshType.INT, emptyList()) { _, _ ->
        AshValue.of((character?.state?.value?.ascensionNumber ?: 0).toLong())
    }

    regFn(scope, "can_interact", AshType.BOOLEAN, emptyList()) { _, _ ->
        val cs = character?.state?.value
        AshValue.of(cs != null && !cs.isHardcore && !cs.isInRonin)
    }

    regFn(scope, "is_dark_mode", AshType.BOOLEAN, emptyList()) { _, _ ->
        AshValue.of(false)
    }

    regFn(scope, "my_thrall", AshType.THRALL, emptyList()) { _, _ ->
        val name = preferences?.getString("_currentThrall", "") ?: ""
        AshValue(AshType.THRALL, name)
    }

    regFn(scope, "turnsleft", AshType.INT, emptyList()) { _, _ ->
        AshValue.of((character?.state?.value?.adventuresLeft ?: 0).toLong())
    }

    regFn(scope, "turnsleft", AshType.INT, listOf("label" to AshType.STRING)) { _, args ->
        val label = args[0].toString()
        val prefs = preferences
        if (prefs == null) return@regFn AshValue.of(-1L)
        val currentRun = character?.state?.value?.currentRun ?: 0
        val entry = net.sourceforge.kolmafia.session.TurnCounter.findByLabel(prefs, label)
        AshValue.of(net.sourceforge.kolmafia.session.TurnCounter.turnsRemaining(entry, currentRun).toLong())
    }

    regFn(scope, "my_absorbs", AshType.INT, emptyList()) { _, _ ->
        AshValue.of((character?.state?.value?.absorbs ?: 0).toLong())
    }

    // can_adventure(location) → boolean
    regFn(scope, "can_adventure", AshType.BOOLEAN,
        listOf("loc" to AshType.LOCATION)) { _, args ->
        val locationName = args[0].toString()
        AshValue.of(
            AdventurePrep.canAdventureAt(locationName, character?.state?.value)
        )
    }

    // prepare_for_adventure() → boolean (no location — always succeeds)
    regFn(scope, "prepare_for_adventure", AshType.BOOLEAN, emptyList()) { _, _ ->
        AshValue.of(true)
    }

    // prepare_for_adventure(loc: location) → boolean
    regFn(scope, "prepare_for_adventure", AshType.BOOLEAN,
        listOf("loc" to AshType.LOCATION)) { _, args ->
        val locationName = args[0].toString()
        val ok = kotlinx.coroutines.runBlocking {
            AdventurePrep.prepareForAdventure(
                locationName,
                outfitManager,
                preferences,
                retrieveItemService,
                useItemRequest,
                gameDatabase,
                familiarManager,
                character?.state?.value,
            )
        }
        AshValue.of(ok)
    }

    // set_location(loc: location) → boolean — travel without adventuring
    regFn(scope, "set_location", AshType.BOOLEAN,
        listOf("loc" to AshType.LOCATION)) { _, args ->
        val locationName = args[0].toString()
        val loc = resolveLocation(locationName) ?: return@regFn AshValue.of(false)
        preferences?.setString(Preferences.LAST_LOCATION, loc.name)
        val ok = kotlinx.coroutines.runBlocking {
            adventureRequest?.travel(loc.id)?.isSuccess ?: false
        }
        AshValue.of(ok)
    }

    regFn(scope, "guild_available", AshType.BOOLEAN, emptyList()) { _, _ ->
        val cls = character?.state?.value?.characterClassEnum
        AshValue.of(cls?.isStandardClass == true)
    }

    regFn(scope, "knoll_available", AshType.BOOLEAN, emptyList()) { _, _ ->
        val sign = character?.state?.value?.zodiacSign.orEmpty()
        AshValue.of(sign.equals("Mongoose", ignoreCase = true) ||
            sign.equals("Wallaby", ignoreCase = true) ||
            sign.equals("Vole", ignoreCase = true))
    }

    regFn(scope, "white_citadel_available", AshType.BOOLEAN, emptyList()) { _, _ ->
        val progress = questDatabase?.getProgress(Quest.CITADEL) ?: QuestDatabase.UNSTARTED
        AshValue.of(progress != QuestDatabase.UNSTARTED)
    }

    regFn(scope, "canadia_available", AshType.BOOLEAN, emptyList()) { _, _ ->
        val sign = character?.state?.value?.zodiacSign.orEmpty()
        AshValue.of(sign.equals("Platypus", ignoreCase = true) ||
            sign.equals("Opossum", ignoreCase = true) ||
            sign.equals("Marmot", ignoreCase = true))
    }

    regFn(scope, "gnomads_available", AshType.BOOLEAN, emptyList()) { _, _ ->
        val sign = character?.state?.value?.zodiacSign.orEmpty()
        AshValue.of(sign.equals("Wombat", ignoreCase = true) ||
            sign.equals("Blender", ignoreCase = true) ||
            sign.equals("Packrat", ignoreCase = true))
    }

    regFn(scope, "friars_available", AshType.BOOLEAN, emptyList()) { _, _ ->
        val available = questDatabase?.isQuestFinished(Quest.FRIAR) == true
        if (available) {
            val asc = character?.state?.value?.ascensionNumber ?: 0
            preferences?.setInt("lastFriarCeremonyAscension", asc)
        }
        AshValue.of(available)
    }

    regFn(scope, "black_market_available", AshType.BOOLEAN, emptyList()) { _, _ ->
        val asc = character?.state?.value?.ascensionNumber ?: 0
        if (preferences?.getInt("lastWuTangDefeated", -1) == asc) {
            return@regFn AshValue.of(false)
        }
        val progress = questDatabase?.getProgress(Quest.MACGUFFIN) ?: QuestDatabase.UNSTARTED
        AshValue.of(
            progress == QuestDatabase.FINISHED || progress.contains("step"),
        )
    }

    regFn(scope, "guild_store_available", AshType.BOOLEAN, emptyList()) { _, _ ->
        val cls = character?.state?.value?.characterClassEnum
        if (cls?.isStandardClass != true) return@regFn AshValue.of(false)
        val asc = character?.state?.value?.ascensionNumber ?: 0
        AshValue.of(preferences?.getInt("lastGuildStoreOpen", -1) == asc)
    }

    regFn(scope, "hippy_store_available", AshType.BOOLEAN, emptyList()) { _, _ ->
        val progress = questDatabase?.getProgress(Quest.ISLAND_WAR) ?: QuestDatabase.UNSTARTED
        AshValue.of(progress != "step1")
    }

    regFn(scope, "dispensary_available", AshType.BOOLEAN, emptyList()) { _, _ ->
        val asc = character?.state?.value?.ascensionNumber ?: 0
        if (preferences?.getInt("lastDispensaryOpen", -1) != asc) {
            return@regFn AshValue.of(false)
        }
        val hasLabKey = inventoryManager?.state?.value?.items?.containsKey(339) == true
        AshValue.of(hasLabKey)
    }

    regFn(scope, "hidden_temple_unlocked", AshType.BOOLEAN, emptyList()) { _, _ ->
        val asc = character?.state?.value?.ascensionNumber ?: 0
        AshValue.of(preferences?.getInt("lastTempleUnlock", -1) == asc)
    }

    regFn(scope, "my_stat", AshType.INT, listOf("stat" to AshType.STAT)) { _, args ->
        AshValue.of(buffedStatValue(args[0]))
    }

    regFn(scope, "my_stat", AshType.INT, listOf("stat" to AshType.STRING)) { _, args ->
        AshValue.of(buffedStatValue(AshValue(AshType.STAT, args[0].toString())))
    }

    regFn(scope, "my_buffedstat", AshType.INT, listOf("stat" to AshType.STAT)) { _, args ->
        AshValue.of(buffedStatValue(args[0]))
    }

    regFn(scope, "my_buffedstat", AshType.INT, listOf("stat" to AshType.STRING)) { _, args ->
        AshValue.of(buffedStatValue(AshValue(AshType.STAT, args[0].toString())))
    }

    regFn(scope, "my_discoball", AshType.BOOLEAN, emptyList()) { _, _ ->
        val has = familiarManager?.state?.value?.ownedFamiliars?.any {
            it.race.equals("Autonomous Disco Ball", ignoreCase = true)
        } ?: false
        AshValue.of(has)
    }

    regFn(scope, "my_rolodex", AshType.BOOLEAN, emptyList()) { _, _ ->
        AshValue.of(preferences?.getBoolean("hasRolodex", false) ?: false)
    }
}

private fun GameRuntimeLibrary.buffedStatValue(stat: AshValue): Long {
    val cs = character?.state?.value ?: return 0L
    return StatNames.buffedValue(cs, stat.toString())
}
