package net.sourceforge.kolmafia.ash

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

    // Stub: CharacterState has no underStandard field
    regFn(scope, "under_standard", AshType.BOOLEAN, emptyList()) { _, _ ->
        AshValue.of(false)
    }

    regFn(scope, "ascension_number", AshType.INT, emptyList()) { _, _ ->
        AshValue.of((character?.state?.value?.ascensionNumber ?: 0).toLong())
    }

    regFn(scope, "can_interact", AshType.BOOLEAN, emptyList()) { _, _ ->
        val cs = character?.state?.value
        AshValue.of(cs != null && !cs.isHardcore && !cs.isInRonin)
    }

    // Stub: no THRALL AshType yet
    regFn(scope, "my_thrall", AshType.STRING, emptyList()) { _, _ ->
        AshValue.EMPTY_STRING
    }
}
