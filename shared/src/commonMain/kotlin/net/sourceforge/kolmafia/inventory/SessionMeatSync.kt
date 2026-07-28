package net.sourceforge.kolmafia.inventory

import net.sourceforge.kolmafia.character.KoLCharacter

/**
 * Tracks net meat gained this session from adventure/visit HTML.
 * Mirrors desktop [KoLCharacter.incrementSessionMeat] on [AdventureResult.MEAT].
 */
object SessionMeatSync {

    private val MEAT_GAINED = Regex("""You gain ([\d,]+) Meat""")

    fun parseMeatGained(html: String): Int =
        MEAT_GAINED.find(html)?.groupValues?.get(1)?.replace(",", "")?.toIntOrNull() ?: 0

    fun apply(character: KoLCharacter, html: String) {
        val meat = parseMeatGained(html)
        if (meat > 0) character.addSessionMeat(meat.toLong())
    }
}
