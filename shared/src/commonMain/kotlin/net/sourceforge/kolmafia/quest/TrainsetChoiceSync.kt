package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.campground.CampgroundItemSync
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [TrainsetManager.visitChoice] for choice 1485.
 */
object TrainsetChoiceSync {

    const val CHOICE_ID = 1485
    const val MODEL_TRAIN_SET_ID = 11045
    private const val TURNS_BETWEEN_CONFIGURE = 40

    private val SELECTED_STATION = Regex(
        """data-slot="(\d+)" class="trainslot dragtospot"[^>]*><div data-id="(\d+)"""",
    )
    private val CURRENT_STATION = Regex("""<br>Your train is about to pass station (\d)\.<""")
    private val LAPS_BEFORE_RECONFIGURE = Regex(
        """Let the train finish (\d+|this) (?:more laps|lap) before rearranging it\.</p>""",
    )

    enum class Piece(val displayName: String, val shortName: String, val id: Int) {
        UNKNOWN("", "unknown", -1),
        EMPTY_TRACK("Empty track", "empty", 0),
        MEAT_MINE("Meat Mine Sluice", "meat_mine", 1),
        TOWER_FIZZY("Water Tower, Fizzy", "tower_fizzy", 2),
        VIEWING_PLATFORM("Viewing Platform", "viewing_platform", 3),
        TOWER_FROZEN("Water Tower, Frozen", "tower_frozen", 4),
        SPOOKY_GRAVEYARD("Spooky Graveyard", "spooky_graveyard", 5),
        LOGGING_MILL("Logging Mill", "logging_mill", 6),
        CANDY_FACTORY("Candy Factory", "candy_factory", 7),
        COAL_HOPPER("Coal Hopper", "coal_hopper", 8),
        TOWER_SEWAGE("Water Tower, Sewage", "tower_sewage", 9),
        OIL_REFINERY("Ectoplasmic Oil Refinery", "oil_refinery", 11),
        OIL_BRIDGE("Bridge over Flaming Oil", "oil_bridge", 12),
        WATER_BRIDGE("Bridge over Troubled Water", "water_bridge", 13),
        GROIN_SILO("Groin Silo", "groin_silo", 14),
        GRAIN_SILO("Grain Silo", "grain_silo", 15),
        BRAIN_SILO("Brain Silo", "brain_silo", 16),
        BRAWN_SILO("Brawn Silo", "brawn_silo", 17),
        PRAWN_SILO("Prawn Silo", "prawn_silo", 18),
        TRACKSIDE_DINER("Trackside Diner", "trackside_diner", 19),
        ORE_HOPPER("Ore Hopper Feeder", "ore_hopper", 20),
        ;

        companion object {
            fun byId(id: Int): Piece = entries.firstOrNull { it.id == id } ?: UNKNOWN
        }
    }

    fun applyVisit(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        preferences.setInt(CampgroundItemSync.CURRENT_WORKSHED_ITEM_ID_PREF, MODEL_TRAIN_SET_ID)

        var lastPosition = preferences.getInt("trainsetPosition", 0)
        val stationMatch = CURRENT_STATION.find(html)
        if (stationMatch != null) {
            val currentPosition = stationMatch.groupValues[1].toInt() - 1
            if (currentPosition != (lastPosition % 8)) {
                lastPosition = lastPosition + (8 - (lastPosition % 8)) + currentPosition
                preferences.setInt("trainsetPosition", lastPosition)
            }
        }

        if (html.contains(">Train set reconfigured.</span>")) {
            preferences.setInt("lastTrainsetConfiguration", preferences.getInt("trainsetPosition", 0))
        } else {
            val lastConfiguration = preferences.getInt("lastTrainsetConfiguration", 0)
            val expectedTurnConfigurable = lastConfiguration + TURNS_BETWEEN_CONFIGURE
            val laps = LAPS_BEFORE_RECONFIGURE.find(html)
            if (laps == null) {
                if (lastPosition == lastConfiguration) {
                    // already configured this turn
                } else if (lastPosition < expectedTurnConfigurable) {
                    preferences.setInt(
                        "lastTrainsetConfiguration",
                        lastPosition - TURNS_BETWEEN_CONFIGURE,
                    )
                }
            } else {
                val expectedLapsRemaining =
                    kotlin.math.ceil((expectedTurnConfigurable - lastPosition) / 8.0).toInt()
                val actualLapsRemaining =
                    if (laps.groupValues[1] == "this") 1 else laps.groupValues[1].toInt()
                if (expectedLapsRemaining != actualLapsRemaining || lastConfiguration == lastPosition) {
                    preferences.setInt(
                        "lastTrainsetConfiguration",
                        lastPosition - (((5 - actualLapsRemaining) * 8) + 1),
                    )
                }
            }
        }

        val pieces = Array(8) { Piece.EMPTY_TRACK }
        SELECTED_STATION.findAll(html).forEach { match ->
            val index = match.groupValues[1].toIntOrNull() ?: return@forEach
            val pieceId = match.groupValues[2].toIntOrNull() ?: return@forEach
            if (index in 0..7) pieces[index] = Piece.byId(pieceId)
        }
        preferences.setString(
            "trainsetConfiguration",
            pieces.joinToString(",") { it.shortName },
        )
        return true
    }
}
