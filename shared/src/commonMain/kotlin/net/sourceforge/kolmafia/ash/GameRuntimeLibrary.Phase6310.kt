package net.sourceforge.kolmafia.ash

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong
import net.sourceforge.kolmafia.data.AdventureDatabase
import net.sourceforge.kolmafia.modifiers.DoubleModifier

/**
 * Phases 6251–6310 — ASH behavioral deepen XL (non-Crimbo legacy thin hubs + ASH edges).
 *
 * 6251–6265 eight_bit_points(loc,color) live-mod overload ·
 * 6266–6285 LegacyCoinmasterResponseParse (Mr Store / Big Brother / Fudge / Isotope / AWOL /
 * Dedigitizer / Bat / Disco / Friars / Swagger) ·
 * 6286–6300 hub parseResponse delegates + visit-hook wiring ·
 * 6301–6310 corpus + revision bump
 */
internal fun GameRuntimeLibrary.registerPhase6310(scope: AshScope) {
    data class EightBitZone(val mod: DoubleModifier, val base: Int, val color: String)
    val eightBitZones = mapOf(
        563 to EightBitZone(DoubleModifier.MEATDROP, 150, "red"),
        564 to EightBitZone(DoubleModifier.ITEMDROP, 100, "green"),
        565 to EightBitZone(DoubleModifier.INITIATIVE, 300, "black"),
        566 to EightBitZone(DoubleModifier.DAMAGE_ABSORPTION, 300, "blue"),
    )
    fun eightBitPoints(zone: EightBitZone, color: String, modValue: Double): Long {
        val isBonus = zone.color.equals(color, ignoreCase = true)
        val base = if (isBonus) 100 else 50
        val divisor = if (isBonus) 10.0 else 20.0
        val bonus = (min(300.0, max(0.0, modValue - zone.base)) / divisor).roundToLong() * 10
        return base + bonus
    }
    fun resolveEightBitZone(locationName: String): EightBitZone? {
        val zone = AdventureDatabase.getByName(locationName) ?: return null
        val snarf = zone.snarfblat?.toIntOrNull() ?: zone.adventureId.toIntOrNull() ?: return null
        return eightBitZones[snarf]
    }
    // eight_bit_points(loc, color) — speculate color with live modifiers (1-arg + 3-arg already live).
    regFn(
        scope,
        "eight_bit_points",
        AshType.INT,
        listOf("loc" to AshType.LOCATION, "color" to AshType.STRING),
    ) { _, args ->
        val zone = resolveEightBitZone(args[0].toString()) ?: return@regFn AshValue.ZERO
        val modValue = buildCurrentModifiers().values.get(zone.mod)
        AshValue.of(eightBitPoints(zone, args[1].toString(), modValue))
    }
}
