package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.maximizer.MaximizerNonEquipmentBoosts
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop BoomBoxCommand — play a SongBoom BoomBox track via choice 1312. */
open class BoomBoxRequest(
    private val useItemRequest: UseItemRequest,
    private val preferences: Preferences? = null,
) {
    open suspend fun play(songParams: String): Result<Unit> {
        val choice = resolveChoice(songParams)
            ?: return Result.failure(IllegalArgumentException("Unknown boombox song: $songParams"))
        val songName = canonicalSongName(choice)
        val current = preferences?.getString("boomBoxSong", "").orEmpty()
        if (songName.equals(current, ignoreCase = true) ||
            (choice == 6 && current.isBlank())
        ) {
            return Result.failure(IllegalStateException("You already have $songName playing."))
        }
        val previousChoice = preferences?.getInt("choiceAdventure1312", 0) ?: 0
        preferences?.setInt("choiceAdventure1312", choice)
        val result = useItemRequest.use(MaximizerNonEquipmentBoosts.BOOMBOX_ITEM_ID, 1)
        preferences?.setInt("choiceAdventure1312", previousChoice)
        return result.map {
            preferences?.setString("boomBoxSong", if (choice == 6) "" else songName)
            Unit
        }
    }

    private fun resolveChoice(params: String): Int? {
        val lower = params.lowercase()
        lower.toIntOrNull()?.takeIf { it in 1..6 }?.let { return it }
        return when {
            lower.contains("giger") || lower.contains("spooky") -> 1
            lower.contains("food") -> 2
            lower.contains("alive") || lower.contains("dr") -> 3
            lower.contains("fists") || lower.contains("damage") -> 4
            lower.contains("meat") -> 5
            lower.contains("silent") || lower.contains("off") -> 6
            else -> null
        }
    }

    private fun canonicalSongName(choice: Int): String = when (choice) {
        1 -> "Eye of the Giger"
        2 -> "Food Vibrations"
        3 -> "Remainin' Alive"
        4 -> "These Fists Were Made for Punchin'"
        5 -> "Total Eclipse of Your Meat"
        else -> ""
    }
}
