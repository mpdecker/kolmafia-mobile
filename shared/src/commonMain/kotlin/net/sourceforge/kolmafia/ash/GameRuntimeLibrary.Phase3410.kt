package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.campground.MushroomManager
import net.sourceforge.kolmafia.campground.MushroomPlotSync
import net.sourceforge.kolmafia.data.JourneymanDatabase
import net.sourceforge.kolmafia.session.HaciendaManager
import net.sourceforge.kolmafia.session.JourneyManager
import net.sourceforge.kolmafia.session.LeprecondoManager

/** ASH helpers for Hacienda / Leprecondo / Mushroom / Journey mega (Phases 3351–3410). */
internal fun GameRuntimeLibrary.registerPhase3410(scope: AshScope) {
    regFn(scope, "hacienda_spoiler", AshType.STRING, listOf("index" to AshType.INT)) { _, args ->
        val index = args[0].toLong().toInt()
        AshValue.of(HaciendaManager.getSpoiler(index, preferences, questDatabase))
    }
    regFn(scope, "leprecondo_furniture_count", AshType.INT, emptyList()) { _, _ ->
        AshValue.of(preferences?.getInt("_leprecondoFurniture", 0) ?: 0)
    }
    regFn(scope, "leprecondo_current_need", AshType.STRING, emptyList()) { _, _ ->
        AshValue.of(preferences?.getString("leprecondoCurrentNeed", "").orEmpty())
    }
    regFn(scope, "leprecondo_undiscovered", AshType.STRING, listOf("zone" to AshType.STRING)) { _, args ->
        AshValue.of(LeprecondoManager.getUndiscoveredFurnitureForLocation(args[0].toString(), preferences))
    }
    regFn(scope, "mushroom_plot", AshType.STRING, emptyList()) { _, _ ->
        AshValue.of(MushroomManager.getMushroomManager(true, preferences))
    }
    regFn(scope, "mushroom_forecast", AshType.STRING, emptyList()) { _, _ ->
        AshValue.of(MushroomManager.getForecastedPlot(true, preferences))
    }
    regFn(scope, "mushroom_square", AshType.STRING, listOf("square" to AshType.INT)) { _, args ->
        val square = args[0].toLong().toInt() - 1
        AshValue.of(MushroomManager.squareShorthand(preferences, square))
    }
    regFn(scope, "journeyman_zone_count", AshType.INT, emptyList()) { _, _ ->
        AshValue.of(JourneymanDatabase.zoneNames.size)
    }
    regFn(scope, "journeyman_next_skill_turn", AshType.INT, listOf("location" to AshType.STRING)) { _, args ->
        val turns = adventureSpentTracker?.getTurns(args[0].toString()) ?: 0
        AshValue.of((JourneyManager.nextSkillThreshold(turns) ?: -1).toLong())
    }
    regFn(scope, "journeyman_skill_at_turn", AshType.STRING, listOf("location" to AshType.STRING, "turns" to AshType.INT)) { _, args ->
        val location = args[0].toString()
        val turns = args[1].toLong().toInt()
        val characterClass = character?.state?.value?.characterClassEnum ?: return@regFn AshValue.of("")
        AshValue.of(JourneyManager.expectedSkillAtTurn(location, characterClass, turns).orEmpty())
    }
    regFn(scope, "in_journeyman_zone", AshType.BOOLEAN, listOf("location" to AshType.STRING)) { _, args ->
        AshValue.of(JourneyManager.isJourneymanZone(args[0].toString()))
    }
}
