package net.sourceforge.kolmafia.character

data class CharacterState(
    val name: String = "",
    val playerId: Int = 0,
    val level: Int = 1,
    val characterClass: Int = 0,
    val currentHp: Int = 0,
    val maxHp: Int = 0,
    val currentMp: Int = 0,
    val maxMp: Int = 0,
    val meat: Int = 0,
    val adventuresLeft: Int = 0,
    val fullness: Int = 0,
    val inebriety: Int = 0,
    val spleenUsed: Int = 0,
    // Combat stats
    val baseMusc: Int = 0,
    val muscSubpoints: Int = 0,
    val baseMyst: Int = 0,
    val mystSubpoints: Int = 0,
    val baseMoxie: Int = 0,
    val moxieSubpoints: Int = 0,
    // Identity
    val zodiacSign: String = "",
    val challengePath: String = "",
    val roninLeft: Int = 0,
    val isHardcore: Boolean = false,
    val isLoggedIn: Boolean = false
)
