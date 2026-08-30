package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.TrainsetChoiceSync

/**
 * Desktop [net.sourceforge.kolmafia.session.TrainsetManager] — configuration piece API
 * and fight-move position increment.
 */
object TrainsetManager {
    fun getTrainsetPieces(preferences: Preferences?): Array<TrainsetChoiceSync.Piece> {
        val raw = preferences?.getString("trainsetConfiguration", "").orEmpty()
        if (raw.isBlank()) return emptyArray()
        return raw.split(",").map { token ->
            TrainsetChoiceSync.Piece.entries.firstOrNull {
                it.displayName.equals(token, ignoreCase = true) ||
                    it.shortName.equals(token, ignoreCase = true)
            } ?: TrainsetChoiceSync.Piece.UNKNOWN
        }.toTypedArray()
    }

    /** Desktop [TrainsetManager.onTrainsetMove] — always increments position. */
    fun onTrainsetMove(pieceName: String, preferences: Preferences): Boolean {
        val newPos = preferences.getInt("trainsetPosition", 0) + 1
        preferences.setInt("trainsetPosition", newPos)
        val pieces = getTrainsetPieces(preferences)
        if (pieces.size != 8) return false
        val expected = pieces[newPos % 8]
        return expected.displayName.equals(pieceName, ignoreCase = true) ||
            expected.shortName.equals(pieceName, ignoreCase = true) ||
            pieceName.equals("empty track", ignoreCase = true) &&
            expected == TrainsetChoiceSync.Piece.EMPTY_TRACK
    }
}
