package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [SkateParkRequest.BUFF_DATA] for maximizer skate-park buff availability. */
object SkateParkAvailability {

    const val LUTZ = 0
    const val COMET = 1
    const val BAND_SHELL = 2
    const val ECLECTIC_EELS = 3
    const val MERRY_GO_ROUND = 4

    data class Buff(
        val place: String,
        val canonicalPlace: String,
        val action: String,
        val buff: Int,
        val setting: String,
        val error: String,
        val state: String,
    )

    val BUFF_DATA = arrayOf(
        Buff(
            "Lutz, the Ice Skate",
            "lutz, the ice skate",
            "state2buff1",
            LUTZ,
            "_skateBuff1",
            "You've already dined with Lutz",
            "ice",
        ),
        Buff(
            "Comet, the Roller Skate",
            "comet, the roller skate",
            "state3buff1",
            COMET,
            "_skateBuff2",
            "You should probably leave Comet alone for the rest of the day",
            "roller",
        ),
        Buff(
            "the Band Shell",
            "the band shell",
            "state4buff1",
            BAND_SHELL,
            "_skateBuff3",
            "You've had about all of that crap you can stand today",
            "peace",
        ),
        Buff(
            "the Eclectic Eels",
            "the eclectic eels",
            "state4buff2",
            ECLECTIC_EELS,
            "_skateBuff4",
            "You should probably leave those guys alone until tomorrow",
            "peace",
        ),
        Buff(
            "the Merry-Go-Round",
            "the merry-go-round",
            "state4buff3",
            MERRY_GO_ROUND,
            "_skateBuff5",
            "Wait until tomorrow",
            "peace",
        ),
    )

    fun placeToBuff(place: String): Int? {
        val normalized = place.trim().lowercase()
        var match: Buff? = null
        for (data in BUFF_DATA) {
            if (!data.canonicalPlace.contains(normalized)) continue
            if (match != null) return null
            match = data
        }
        return match?.buff
    }

    fun buffToData(buff: Int): Buff? = BUFF_DATA.firstOrNull { it.buff == buff }

    fun buffPrefUsed(prefs: Preferences?, buffIndex: Int): Boolean {
        val data = buffToData(buffIndex) ?: return false
        return prefs?.getBoolean(data.setting, false) == true
    }
}
