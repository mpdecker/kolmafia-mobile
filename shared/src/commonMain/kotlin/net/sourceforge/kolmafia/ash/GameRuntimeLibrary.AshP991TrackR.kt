package net.sourceforge.kolmafia.ash

/**
 * AshP991–996 Track R — Combat / result residuals.
 *
 * Phase 991: extract_items, extract_meat
 * Phase 992: last_monster_message, last_encounter
 * Phase 993: attack, steal (0-arg combat), twiddle
 * Phase 994: curse (item,player[,message][,count])
 * Phase 995: pickpocket, runaway
 * Phase 996: monster_level_adjustment (expected_damage is registered by AshP39)
 */
internal fun GameRuntimeLibrary.registerAshP991TrackRBatch(scope: AshScope) {
    // ── Phase 991: extract_items / extract_meat ─────────────────────
    val itemToInt = AggregateType(AshType.ITEM, AshType.INT)
    regFn(scope, "extract_items", itemToInt,
        listOf("html" to AshType.STRING)) { _, args ->
        // Phase 4488: ResultProcessor.parseItems parity (equip/qty/stored comments).
        val result = AggregateValue(itemToInt)
        val html = args[0].toString().replace("- ", "-")
        for ((name, qty) in net.sourceforge.kolmafia.session.ResultProcessor.parseItems(html)) {
            result[AshValue.item(name)] = AshValue.of(qty.toLong())
        }
        result
    }

    regFn(scope, "extract_meat", AshType.INT,
        listOf("html" to AshType.STRING)) { _, args ->
        val html = args[0].toString()
        val regex = Regex("""You gain (\d[\d,]*) Meat""")
        val match = regex.find(html)
        val meat = match?.groupValues?.getOrNull(1)
            ?.replace(",", "")?.toLongOrNull() ?: 0L
        AshValue.of(meat)
    }

    // ── Phase 992: last_monster_message / last_encounter ────────────
    regFn(scope, "last_monster_message", AshType.STRING, emptyList()) { _, _ ->
        AshValue.of(preferences?.getString("lastEncounter", "").orEmpty())
    }

    regFn(scope, "last_encounter", AshType.STRING, emptyList()) { _, _ ->
        AshValue.of(preferences?.getString("lastEncounter", "").orEmpty())
    }

    // ── Phase 993: attack / steal / twiddle ─────────────────────────
    fun fightAction(action: String): AshValue {
        val html = visitKolPage("fight.php?action=$action")
        if (httpClient == null) {
            // Keep the desktop-compatible action buffer useful in offline
            // runtimes where there is no request abstraction to execute it.
            return AshValue(AshType.BUFFER, StringBuilder(action))
        }
        val result = html.orEmpty()
        if (result.isNotBlank()) {
            net.sourceforge.kolmafia.session.ChoiceCombatAshState.noteFightRound(result)
        }
        return AshValue(AshType.BUFFER, StringBuilder(result))
    }

    regFn(scope, "attack", AshType.BUFFER, emptyList()) { _, _ ->
        fightAction("attack")
    }

    regFn(scope, "steal", AshType.BUFFER, emptyList()) { _, _ ->
        fightAction("steal")
    }

    regFn(scope, "twiddle", AshType.BUFFER, emptyList()) { _, _ ->
        fightAction("twiddle")
    }

    // ── Phase 994: curse ────────────────────────────────────────────
    regFn(scope, "curse", AshType.BOOLEAN,
        listOf("itemId" to AshType.ITEM, "target" to AshType.STRING)) { _, args ->
        curseItem(args[0].toString(), args[1].toString(), "")
    }

    regFn(scope, "curse", AshType.BOOLEAN,
        listOf("itemId" to AshType.ITEM, "target" to AshType.STRING,
            "message" to AshType.STRING)) { _, args ->
        curseItem(args[0].toString(), args[1].toString(), args[2].toString())
    }

    regFn(scope, "curse", AshType.BOOLEAN,
        listOf("quantity" to AshType.INT, "itemId" to AshType.ITEM,
            "target" to AshType.STRING, "message" to AshType.STRING)) { _, args ->
        val itemId = args[1].toLong().toInt()
        val quantity = args[0].toLong().toInt().coerceAtLeast(1)
        var succeeded = true
        repeat(quantity) {
            succeeded = curseItem(
                itemId.toString(), args[2].toString(), args[3].toString(),
            ).toBoolean() && succeeded
        }
        AshValue.of(succeeded)
    }

    // ── Phase 995 / 4469: pickpocket — live fight action (desktop steal/pickpocket) ─
    regFn(scope, "pickpocket", AshType.BUFFER, emptyList()) { _, _ ->
        fightAction("steal")
    }
    // runaway() already registered in AshP894 (Track A) with live HTTP

    // ── Phase 996: monster_level_adjustment ─────────────────────────
    // expected_damage is deliberately not duplicated here: AshP39 owns the
    // overloads and evaluates the live monster/combat modifiers.
    regFn(scope, "monster_level_adjustment", AshType.INT, emptyList()) { _, _ ->
        AshValue.of(
            net.sourceforge.kolmafia.ash.CombatAdjustment.monsterLevelAdjustment(
                buildCurrentModifiers(),
                character?.state?.value,
                lastLocationName(),
            ).toLong(),
        )
    }
}

private fun GameRuntimeLibrary.curseItem(itemToken: String, target: String, message: String): AshValue {
    val itemId = itemToken.toIntOrNull()
        ?: gameDatabase?.item(itemToken)?.id
        ?: return AshValue.FALSE
    val item = gameDatabase?.item(itemId) ?: return AshValue.FALSE
    if (!item.secondaryUses.any { it.equals("curse", ignoreCase = true) }) {
        return AshValue.FALSE
    }
    val post = buildString {
        append("action=use&whichitem=$itemId&targetplayer=")
        append(target.replace(" ", "%20"))
        if (message.isNotBlank()) {
            message.split(Regex("""\s*\|\s*""")).forEachIndexed { index, part ->
                append("&text${('a'.code + index).toChar()}=")
                append(part.replace(" ", "%20"))
            }
        }
    }
    val html = visitKolPost("curse.php", post) ?: return AshValue.FALSE
    return AshValue.of(
        !Regex("""could not be found|cannot be used|can't use|no message""")
            .containsMatchIn(html.lowercase()),
    )
}
