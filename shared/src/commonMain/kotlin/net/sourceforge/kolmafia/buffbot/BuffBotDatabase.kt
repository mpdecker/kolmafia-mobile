package net.sourceforge.kolmafia.buffbot

class BuffBotDatabase(costs: List<BuffCost>) {

    private val costMap: Map<Int, BuffCost> = costs.associateBy { it.buffId }

    fun find(buffId: Int): BuffCost? = costMap[buffId]

    companion object {
        val default: BuffBotDatabase = BuffBotDatabase(listOf(
            BuffCost(buffId = 3004, buffName = "Empathy of the Newt",       meatCost = 100L, turns = 10),
            BuffCost(buffId = 3009, buffName = "Elemental Saucesphere",      meatCost = 100L, turns = 10),
            BuffCost(buffId = 1003, buffName = "Seal Clubbing Frenzy",       meatCost = 50L,  turns = 5),
            BuffCost(buffId = 1005, buffName = "Patience of the Tortoise",   meatCost = 50L,  turns = 5),
            BuffCost(buffId = 6014, buffName = "Fat Leon's Phat Loot Lyric", meatCost = 100L, turns = 10),
            BuffCost(buffId = 6003, buffName = "Ode to Booze",               meatCost = 100L, turns = 10)
        ))
    }
}
