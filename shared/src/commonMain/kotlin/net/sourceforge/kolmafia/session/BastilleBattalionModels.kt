package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.data.BastilleDatabase.Castle
import net.sourceforge.kolmafia.data.BastilleDatabase.Stat
import net.sourceforge.kolmafia.data.BastilleDatabase.Stats

/** Cheese-granting encounter metadata (desktop BastilleBattalionManager.CheeseEncounter). */
data class BastilleCheeseEncounter(
    val name: String,
    val stat: Stat = Stat.NONE,
    val positive: Boolean = true,
    val formula: BastilleCheeseFormula? = null,
) {
    fun expectedCheese(statValue: Int): Int = formula?.estimate(statValue)
        ?: when (name) {
            "Grab the boulder" -> 20
            "Scrape out the mine" -> 50
            "Raid the cart" -> 100
            "Use the wishing well" -> 100 // 1/3 chance of about 300
            else -> 0
        }

    companion object {
        private val registry = mutableMapOf<String, BastilleCheeseEncounter>()

        val unknown = BastilleCheeseEncounter("UNKNOWN")

        fun register(name: String, stat: Stat = Stat.NONE, positive: Boolean = true) {
            val formula = if (stat == Stat.NONE) null else BastilleCheeseFormula(
                slope = if (positive) 10 else -10,
                intercept = if (positive) 75 else 175,
            )
            registry[name] = BastilleCheeseEncounter(name, stat, positive, formula)
        }

        fun forName(name: String): BastilleCheeseEncounter = registry[name] ?: unknown
        fun all(): Collection<BastilleCheeseEncounter> = registry.values
        fun scalingEncounters(): List<BastilleCheeseEncounter> =
            registry.values.filter { it.stat != Stat.NONE }

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

/**
 * Headless estimate used by the choice advisor. KoL adds fuzz to these linear yields, so this
 * deliberately represents the center estimate rather than promising an exact payout.
 */
data class BastilleCheeseFormula(
    val slope: Int,
    val intercept: Int,
    val minimum: Int = 1,
) {
    fun estimate(statValue: Int): Int = (intercept + slope * statValue).coerceAtLeast(minimum)
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
        Stat.MA, Stat.MD -> countFor('M')
        Stat.CA, Stat.CD -> countFor('C')
        Stat.PA, Stat.PD -> countFor('P')
        Stat.NONE -> 0
    }

    private fun countFor(code: Char): Int {
        val match = Regex("""(\d*)$code""").find(value) ?: return 0
        return match.groupValues[1].toIntOrNull() ?: 1
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
        winCount() >= 2

    fun winCount(): Int =
        (if (military) 1 else 0) + (if (castle) 1 else 0) + (if (psychological) 1 else 0)

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
