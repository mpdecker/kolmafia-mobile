package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.adventure.AdventurePrep

internal fun GameRuntimeLibrary.registerCharacterExtensions(scope: AshScope) {

    regFn(scope, "my_class", AshType.CLASS, emptyList()) { _, _ ->
        AshValue(AshType.CLASS,
            character?.state?.value?.characterClassEnum?.displayName ?: "")
    }

    regFn(scope, "my_path", AshType.PATH, emptyList()) { _, _ ->
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

    // Stub: no THRALL AshType yet
    regFn(scope, "my_thrall", AshType.STRING, emptyList()) { _, _ ->
        AshValue.EMPTY_STRING
    }

    // can_adventure(location) → boolean
    // Returns true if the character has adventures remaining.
    regFn(scope, "can_adventure", AshType.BOOLEAN,
        listOf("loc" to AshType.LOCATION)) { _, _ ->
        AshValue.of((character?.state?.value?.adventuresLeft ?: 0) > 0)
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
            )
        }
        AshValue.of(ok)
    }
}
