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
    // Combat stats (base adjusted values, all returned as strings)
    val mus: String = "0",
    val musexp: String = "0",
    val mys: String = "0",
    val mysexp: String = "0",
    val mox: String = "0",
    val moxexp: String = "0",
    // Character identity
    val sign: String = "",
    val path: String = "",
    val roninleft: String = "0",
    val hardcore: String = "0"
)
