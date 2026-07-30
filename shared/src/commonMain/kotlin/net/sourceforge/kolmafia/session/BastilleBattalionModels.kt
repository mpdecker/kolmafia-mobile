package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.data.BastilleDatabase.Castle
import net.sourceforge.kolmafia.data.BastilleDatabase.Stat
import net.sourceforge.kolmafia.data.BastilleDatabase.Stats

/** Cheese-granting encounter metadata (desktop BastilleBattalionManager.CheeseEncounter). */
data class BastilleCheeseEncounter(
    val name: String,
    val stat: Stat = Stat.NONE,
    val positive: Boolean = true,
) {
    companion object {
        private val registry = mutableMapOf<String, BastilleCheeseEncounter>()

        val unknown = BastilleCheeseEncounter("UNKNOWN")

        fun register(name: String, stat: Stat = Stat.NONE, positive: Boolean = true) {
            registry[name] = BastilleCheeseEncounter(name, stat, positive)
        }

        fun forName(name: String): BastilleCheeseEncounter = registry[name] ?: unknown

        init {
            register("Raid the cave", Stat.MA, positive = true)
            register("Enter the Weakest Army competition", Stat.MA, positive = false)
            register("Convert the barracks", Stat.MD, positive = true)
            register("Let the cheese horse in", Stat.MD, positive = false)
            register("Shoot the glacier", Stat.CA, positive = true)
            register("Submit embarrassing catapult photos", Stat.CA, positive = false)
            register("Try the wall thing", Stat.CD, positive = true)
            register("Stand in the waterfall", Stat.CD, positive = false)
            register("Rob the suburb", Stat.PA, positive = true)
            register("Enter the childrens' art contest", Stat.PA, positive = false)
            register("Have the cheese contest", Stat.PD, positive = true)
            register("Put on the bad art show", Stat.PD, positive = false)
            register("Grab the boulder")
            register("Scrape out the mine")
            register("Raid the cart")
            register("Use the wishing well")
            register("Levy the tax")
            register("Let the citizens hurl cheese at you")
            register("Trade soldiers for cheese")
        }
    }
}

enum class BastilleStance(val option: Int, val label: String) {
    OFFENSE(1, "offensive"),
    BIDE(2, "waiting"),
    DEFENSE(3, "defensive"),
    ;

    companion object {
        fun fromOption(option: Int): BastilleStance? =
            entries.firstOrNull { it.option == option }
    }

    override fun toString(): String = label
}

/** Active Bastille potion boosts from `_bastilleBoosts` pref (M/C/P). */
data class BastilleBoosts(val value: String) {
    fun boostedBy(stat: Stat): Int = when (stat) {
        Stat.MA, Stat.MD -> if (value.contains('M')) 1 else 0
        Stat.CA, Stat.CD -> if (value.contains('C')) 1 else 0
        Stat.PA, Stat.PD -> if (value.contains('P')) 1 else 0
        Stat.NONE -> 0
    }

    override fun toString(): String = value
}

data class BastilleBattleResults(
    val aggressor: Boolean,
    val military: Boolean,
    val castle: Boolean,
    val psychological: Boolean,
) {
    val value: String
        get() {
            val buf = StringBuilder()
            appendPair(buf, 'M', aggressor, military)
            buf.append(',')
            appendPair(buf, 'C', aggressor, castle)
            buf.append(',')
            appendPair(buf, 'P', aggressor, psychological)
            return buf.toString()
        }

    fun won(): Boolean =
        (if (military) 1 else 0) + (if (castle) 1 else 0) + (if (psychological) 1 else 0) >= 2

    private fun appendPair(buf: StringBuilder, prefix: Char, aggressor: Boolean, won: Boolean) {
        buf.append(prefix)
        buf.append(if (aggressor) 'A' else 'D')
        buf.append(if (won) '>' else '<')
        buf.append(prefix)
        buf.append(if (aggressor) 'D' else 'A')
    }
}

data class BastilleBattle(
    val number: Int,
    val stats: Stats,
    val boosts: BastilleBoosts,
    val enemy: Castle?,
    val stance: BastilleStance?,
    var results: BastilleBattleResults? = null,
    var cheese: Int = 0,
) {
    fun toCheese(): BastilleCheeseRecord =
        BastilleCheeseRecord(
            turn = number * 3,
            cheese = cheese,
            encounter = BastilleCheeseEncounter(enemy?.prefix ?: "unknown"),
            stat = Stat.NONE,
            statBonus = number,
            potion = 0,
        )
}

data class BastilleCheeseRecord(
    val turn: Int,
    val cheese: Int,
    val encounter: BastilleCheeseEncounter,
    val stat: Stat,
    val statBonus: Int,
    val potion: Int,
) {
    companion object {
        fun fromEncounter(
            turn: Int,
            encounterName: String,
            cheese: Int,
            currentStat: (Stat) -> Int,
            boosts: BastilleBoosts,
        ): BastilleCheeseRecord {
            val encounter = BastilleCheeseEncounter.forName(encounterName)
            val stat = encounter.stat
            return BastilleCheeseRecord(
                turn = turn,
                cheese = cheese,
                encounter = encounter,
                stat = stat,
                statBonus = if (stat == Stat.NONE) 0 else currentStat(stat),
                potion = if (stat == Stat.NONE) 0 else boosts.boostedBy(stat),
            )
        }
    }
}
