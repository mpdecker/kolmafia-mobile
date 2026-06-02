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
    val spleen: String = "0"
)
