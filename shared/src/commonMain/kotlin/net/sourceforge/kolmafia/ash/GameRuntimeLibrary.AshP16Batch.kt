package net.sourceforge.kolmafia.ash

/**
 * ASH-P16 behavioral batch — quest-pref and war helpers beyond overload-count parity.
 */
internal fun GameRuntimeLibrary.registerAshP16Batch(scope: AshScope) {
    regFn(scope, "war_progress", AshType.STRING, emptyList()) { _, _ ->
        AshValue.of(preferences?.getString("warProgress", "unstarted") ?: "unstarted")
    }
    regFn(scope, "pyramid_bomb_used", AshType.BOOLEAN, emptyList()) { _, _ ->
        AshValue.of(preferences?.getBoolean("pyramidBombUsed") == true)
    }
    regFn(scope, "middle_chamber_unlock", AshType.BOOLEAN, emptyList()) { _, _ ->
        AshValue.of(preferences?.getBoolean("middleChamberUnlock") == true)
    }
    regFn(scope, "lower_chamber_unlock", AshType.BOOLEAN, emptyList()) { _, _ ->
        AshValue.of(preferences?.getBoolean("lowerChamberUnlock") == true)
    }
    regFn(scope, "control_room_unlock", AshType.BOOLEAN, emptyList()) { _, _ ->
        AshValue.of(preferences?.getBoolean("controlRoomUnlock") == true)
    }
    regFn(scope, "big_brother_rescued", AshType.BOOLEAN, emptyList()) { _, _ ->
        AshValue.of(preferences?.getBoolean("bigBrotherRescued") == true)
    }
    regFn(scope, "ns_contestants_left", AshType.INT, listOf("contest" to AshType.STRING)) { _, args ->
        val contest = args[0].toString().toIntOrNull() ?: -1
        AshValue.of((preferences?.getInt("nsContestants$contest", -1) ?: -1).toLong())
    }
}
