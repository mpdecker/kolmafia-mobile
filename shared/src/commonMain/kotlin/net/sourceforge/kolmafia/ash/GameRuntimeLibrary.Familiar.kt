package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.data.FamiliarDefinitionDatabase
import net.sourceforge.kolmafia.familiar.FamiliarUsability

internal fun GameRuntimeLibrary.registerFamiliarQueries(scope: AshScope) {

    regFn(scope, "have_familiar", AshType.BOOLEAN,
        listOf("fam" to AshType.FAMILIAR)) { _, args ->
        val race = args[0].toString()
        val fm = familiarManager ?: return@regFn AshValue.of(false)
        val charState = character?.state?.value
        runBlocking { ensureRestrictionListsInitialized(charState) }
        val usable = FamiliarUsability.usableByRace(
            familiarState = fm.state.value,
            race = race,
            characterState = charState,
            preferences = preferences,
        )
        AshValue.of(usable != null)
    }

    regFn(scope, "in_terrarium", AshType.BOOLEAN,
        listOf("fam" to AshType.FAMILIAR)) { _, args ->
        val race = args[0].toString()
        if (race.equals("none", ignoreCase = true)) return@regFn AshValue.of(false)
        val owned = familiarManager?.state?.value?.ownedFamiliars
            ?.any { it.race.equals(race, ignoreCase = true) } ?: false
        AshValue.of(owned)
    }

    regFn(scope, "my_familiar_weight", AshType.INT, emptyList()) { _, _ ->
        val charState = character?.state?.value
        var weight = charState?.familiarWeight ?: 0
        val familiarId = familiarManager?.state?.value?.activeFamiliar?.id
            ?: charState?.familiarId
            ?: 0
        if (familiarId > 0) {
            val soupWeight = familiarManager?.state?.value?.ownedFamiliars
                ?.firstOrNull { it.id == familiarId }
                ?.soupWeight ?: 0
            weight += soupWeight
        }
        AshValue.of(weight.toLong())
    }

    regFn(scope, "my_enthroned_familiar", AshType.FAMILIAR, emptyList()) { _, _ ->
        val name = character?.state?.value?.enthronedFamiliarName?.takeIf { it.isNotBlank() }
            ?: "none"
        AshValue.familiar(name)
    }

    regFn(scope, "my_bjornified_familiar", AshType.FAMILIAR, emptyList()) { _, _ ->
        val name = character?.state?.value?.bjornedFamiliarName?.takeIf { it.isNotBlank() }
            ?: "none"
        AshValue.familiar(name)
    }

    regFn(scope, "my_poke_fam", AshType.FAMILIAR,
        listOf("slot" to AshType.INT)) { _, args ->
        val slot = args[0].toLong().toInt()
        val char = character ?: return@regFn AshValue.familiar("none")
        if (slot !in 0..2) return@regFn AshValue.familiar("none")
        val teamSlot = char.pokeFamSlot(slot)
        if (teamSlot.isEmpty) return@regFn AshValue.familiar("none")
        val name = teamSlot.name.takeIf { it.isNotBlank() }
            ?: FamiliarDefinitionDatabase.getById(teamSlot.familiarId)?.name
            ?: "none"
        AshValue.familiar(name)
    }

    regFn(scope, "to_familiar", AshType.FAMILIAR,
        listOf("name" to AshType.STRING)) { _, args ->
        AshValue.familiar(args[0].toString())
    }

    regFn(scope, "use_familiar", AshType.BOOLEAN,
        listOf("fam" to AshType.FAMILIAR)) { _, args ->
        val fm = familiarManager ?: return@regFn AshValue.of(false)
        val race = args[0].toString()
        val success = runBlocking {
            val usable = resolveUsableFamiliarRace(race) ?: return@runBlocking false
            fm.setFamiliar(usable.race).isSuccess
        }
        AshValue.of(success)
    }

    regFn(scope, "enthrone_familiar", AshType.BOOLEAN,
        listOf("fam" to AshType.FAMILIAR)) { _, args ->
        val fm = familiarManager ?: return@regFn AshValue.of(false)
        val race = args[0].toString()
        val success = runBlocking {
            val usable = resolveUsableFamiliarRace(race) ?: return@runBlocking false
            fm.setEnthroned(usable.race).isSuccess
        }
        AshValue.of(success)
    }

    regFn(scope, "bjornify_familiar", AshType.BOOLEAN,
        listOf("fam" to AshType.FAMILIAR)) { _, args ->
        val fm = familiarManager ?: return@regFn AshValue.of(false)
        val race = args[0].toString()
        val success = runBlocking {
            val usable = resolveUsableFamiliarRace(race) ?: return@runBlocking false
            fm.setBjornified(usable.race).isSuccess
        }
        AshValue.of(success)
    }

    // steal(it: item, n: int) → int — familiar steal; returns count gained
    regFn(scope, "steal", AshType.INT,
        listOf("it" to AshType.ITEM, "n" to AshType.INT)) { _, args ->
        val itemId = gameDatabase?.item(args[0].toString())?.id ?: return@regFn AshValue.of(0L)
        val count = args[1].toLong().toInt().coerceAtLeast(0)
        if (count == 0) return@regFn AshValue.of(0L)
        val req = familiarRequest ?: return@regFn AshValue.of(0L)
        var gained = 0L
        runBlocking {
            repeat(count) {
                val before = inventoryManager?.state?.value?.items?.get(itemId)?.quantity ?: 0
                if (req.stealItem(itemId).isFailure) return@runBlocking
                inventoryManager?.fetchInventory()
                val after = inventoryManager?.state?.value?.items?.get(itemId)?.quantity ?: before
                val delta = (after - before).coerceAtLeast(0)
                if (delta <= 0) return@runBlocking
                gained += delta
            }
        }
        AshValue.of(gained)
    }
}

internal suspend fun GameRuntimeLibrary.resolveUsableFamiliarRace(race: String): net.sourceforge.kolmafia.familiar.FamiliarData? {
    val fm = familiarManager ?: return null
    val charState = character?.state?.value
    ensureRestrictionListsInitialized(charState)
    return FamiliarUsability.usableByRace(
        familiarState = fm.state.value,
        race = race,
        characterState = charState,
        preferences = preferences,
    )
}
