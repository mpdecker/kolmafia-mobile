package net.sourceforge.kolmafia.character

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Models the JSON returned by https://www.kingdomofloathing.com/api.php?what=status
// All numeric fields come back as strings in the KoL API.
@Serializable
data class CharacterApiResponse(
    val name: String = "",
    val playerid: String = "0",
    val level: String = "1",
    @SerialName("class") val classId: String = "0",
    val hp: String = "0",
    val hpmax: String = "0",
    val mp: String = "0",
    val mpmax: String = "0",
    val meat: String = "0",
    val adventures: String = "0",
    val fullness: String = "0",
    val drunk: String = "0",
    val spleen: String = "0",

    // Base stats
    val mus: String = "0",
    val musexp: String = "0",
    val mys: String = "0",
    val mysexp: String = "0",
    val mox: String = "0",
    val moxexp: String = "0",

    // Buffed stats (equipment + effect modifiers applied)
    val buffedmus: String = "0",
    val buffedmys: String = "0",
    val buffedmox: String = "0",

    // Turn / day counters
    val turnsplayed: String = "0",
    val daycount: String = "0",

    // PvP
    val pvpfights: String = "0",

    // Identity / mode
    val sign: String = "",
    val path: String = "",
    val roninleft: String = "0",
    val hardcore: String = "0",
    val ascensions: String = "0",
    val limitmode: String = "",

    // Resource caps (stomach/liver/spleen size may differ by sign or path perks)
    val stomachsize: String = "15",
    val liversize: String = "14",
    val spleensize: String = "15",

    // Current equipment (item names per slot)
    val hat: String = "",
    val weapon: String = "",
    val offhand: String = "",
    val shirt: String = "",
    val pants: String = "",
    val acc1: String = "",
    val acc2: String = "",
    val acc3: String = "",
    val familiarequip: String = "",
    val container: String = ""
)
