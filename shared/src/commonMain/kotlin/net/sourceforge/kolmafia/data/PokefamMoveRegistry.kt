package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.session.SessionLogger

/** Desktop `FightRequest` pokefam move slot maps + `registerPokefamMove`. */
object PokefamMoveRegistry {

    private val moveToAction1 = mutableMapOf<String, String>()
    private val moveToAction2 = mutableMapOf<String, String>()
    private val moveToAction3 = mutableMapOf<String, String>()
    private val actionToMove1 = mutableMapOf<String, String>()
    private val actionToMove2 = mutableMapOf<String, String>()
    private val actionToMove3 = mutableMapOf<String, String>()
    private val moveDescriptions = mutableMapOf<String, String>()

    init {
        seedMoves()
    }

    fun moveToAction(slot: Int, move: String): String? = moveMaps(slot)[move]

    fun registerMove(
        slot: Int,
        move: String?,
        action: String?,
        description: String?,
        sessionLogger: SessionLogger? = null,
    ) {
        if (move.isNullOrBlank() || action.isNullOrBlank()) return
        val current = moveToAction(slot, move)
        if (current == action) return
        val line = "Pokefam move$slot '$move' -> '$action': ${description.orEmpty()}"
        sessionLogger?.appendRawLine(line)
        mapMoveToAction(slot, move, action, description.orEmpty())
    }

    internal fun resetForTest() {
        moveToAction1.clear()
        moveToAction2.clear()
        moveToAction3.clear()
        actionToMove1.clear()
        actionToMove2.clear()
        actionToMove3.clear()
        moveDescriptions.clear()
        seedMoves()
    }

    private fun moveMaps(slot: Int): MutableMap<String, String> = when (slot) {
        1 -> moveToAction1
        2 -> moveToAction2
        3 -> moveToAction3
        else -> mutableMapOf()
    }

    private fun actionMaps(slot: Int): MutableMap<String, String> = when (slot) {
        1 -> actionToMove1
        2 -> actionToMove2
        3 -> actionToMove3
        else -> mutableMapOf()
    }

    private fun mapMoveToAction(slot: Int, move: String, action: String, description: String) {
        if (slot !in 1..3) return
        moveMaps(slot)[move] = action
        actionMaps(slot)[action] = move
        if (description.isNotBlank()) {
            moveDescriptions[move] = description
        }
    }

    private fun seedMoves() {
        mapMoveToAction(1, "Bite", "bite", "Deal [power] damage to a random enemy.")
        mapMoveToAction(1, "Bonk", "bonk", "Deal [power] damage to the frontmost enemy.")
        mapMoveToAction(1, "Claw", "claw", "Deal [power] damage to the frontmost enemy and 1 damage to a random enemy.")
        mapMoveToAction(1, "Peck", "peck", "Deal [power] damage to the frontmost enemy.")
        mapMoveToAction(1, "Punch", "punch", "Deal [power] damage to the frontmost enemy and reduce its power by 1.")
        mapMoveToAction(1, "Sting", "sting", "Deal [power] damage to the frontmost enemy and poison it.")

        mapMoveToAction(2, "Armor Up", "armorup", "Become Armored.")
        mapMoveToAction(2, "Backstab", "backstab", "Deal 1 damage to the rearmost enemy and poison it.")
        mapMoveToAction(2, "Breathe Fire", "flame", "Deal 1 damage to all enemies.")
        mapMoveToAction(2, "Chill Out", "chill", "Make a random enemy Tired.")
        mapMoveToAction(2, "Embarrass", "embarrass", "Reduce a random enemy's power by 1.")
        mapMoveToAction(2, "Encourage", "encourage", "Increase the frontmost ally's power by 1.")
        mapMoveToAction(2, "Frighten", "spook", "Reduce the frontmost enemy's power by 1.")
        mapMoveToAction(2, "Growl", "growl", "Reduce 2 random enemies' power by 1.")
        mapMoveToAction(2, "Howl", "howl", "Deal 1 damage to all enemies.")
        mapMoveToAction(2, "Laser Beam", "laser", "Deal 2 damage to a random enemy.")
        mapMoveToAction(2, "Lick", "lick", "Heal all allies for 1.")
        mapMoveToAction(2, "Regrow", "regrow", "Heal itself by [power]")
        mapMoveToAction(2, "Retreat", "retreat", "Move to the back.")
        mapMoveToAction(2, "Splash", "splash", "Deal 1 damage to two random enemies.")
        mapMoveToAction(2, "Stinkblast", "stinker", "Make a random enemy Tired.")
        mapMoveToAction(2, "Swoop", "swoop", "Avoid all attack damage this turn.")
        mapMoveToAction(2, "Tackle", "tackle", "Knock the frontmost enemy to the back.")

        mapMoveToAction(3, "Bear Hug", "ult_bearhug", "Heal self for 3, teammates for 2.")
        mapMoveToAction(3, "Blood Bath", "ult_bloodbath", "Deal 12 damage spread out among all foes randomly.")
        mapMoveToAction(3, "Defense Matrix", "ult_protect", "Give all allies Armored.")
        mapMoveToAction(3, "Deluxe Impale", "ult_impale", "Deal 5 damage to the frontmost enemy.")
        mapMoveToAction(3, "Empowering Cheer", "ult_powerall", "Give all allies +1 Power.")
        mapMoveToAction(3, "Healing Rain", "ult_regenall", "Give all allies Regeneration.")
        mapMoveToAction(3, "Nasty Cloud", "ult_sporecloud", "Poisons all enemies.")
        mapMoveToAction(3, "Nuclear Bomb", "ult_nuke", "Deal 5 damage to the rearmost enemy.")
        mapMoveToAction(3, "Owl Stare", "ult_owlstare", "Heal all allies for 1 and increase power by 1.")
        mapMoveToAction(3, "Pepperscorn", "ult_pepperscorn", "Give allies Spiked.")
        mapMoveToAction(3, "Rainbow Storm", "ult_rainbowstorm", "Give allies regeneration and armor.")
        mapMoveToAction(3, "Spiky Burst", "ult_crazyblast", "Deal 8 damage spread out among all foes randomly.")
        mapMoveToAction(3, "Stick Treats", "ult_stickytreats", "Heal allies for 1, tire front enemy.")
        mapMoveToAction(3, "Universal Backrub", "ult_superheal", "Heals all allies for 2.")
        mapMoveToAction(3, "Violent Shred", "ult_savage", "Deals 2 damage to all enemies.")
        mapMoveToAction(3, "Vulgar Display", "ult_weakenall", "Reduce all enemy Power by 1.")
    }
}
