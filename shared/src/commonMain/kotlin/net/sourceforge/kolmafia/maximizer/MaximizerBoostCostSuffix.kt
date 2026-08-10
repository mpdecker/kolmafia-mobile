package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ConsumableDatabase
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.ConcoctionCreationCost
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.SkillDefinitionProxy
import net.sourceforge.kolmafia.item.CreatableTurns
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop Maximizer boost cost suffixes (~1576–1648). */
object MaximizerBoostCostSuffix {

    data class BoostCosts(
        val full: Int = 0,
        val drunk: Int = 0,
        val spleen: Int = 0,
        val mp: Int = 0,
        val soulsauce: Int = 0,
        val thunder: Int = 0,
        val rain: Int = 0,
        val lightning: Int = 0,
        val hp: Int = 0,
        val fuel: Int = 0,
        val adv: Int = 0,
        val meat: Long = 0L,
    ) {
        operator fun plus(other: BoostCosts): BoostCosts = BoostCosts(
            full = full + other.full,
            drunk = drunk + other.drunk,
            spleen = spleen + other.spleen,
            mp = mp + other.mp,
            soulsauce = soulsauce + other.soulsauce,
            thunder = thunder + other.thunder,
            rain = rain + other.rain,
            lightning = lightning + other.lightning,
            hp = hp + other.hp,
            fuel = fuel + other.fuel,
            adv = adv + other.adv,
            meat = meat + other.meat,
        )
    }

    data class Context(
        val gameDatabase: GameDatabase,
        val charState: CharacterState,
        val preferences: Preferences? = null,
        val skillMpCost: (String) -> Int = { name ->
            SkillDefinitionProxy.getByIdOrName(name)?.mpCost ?: 0
        },
        val physicalAccessible: (Int) -> Int = { 0 },
        val mallPrice: (Int) -> Long = { 0L },
        val extraMeat: Long = 0L,
        val extraAdv: Int = 0,
    )

    fun accumulateFromCmd(cmd: String, ctx: Context): BoostCosts {
        var costs = BoostCosts(adv = ctx.extraAdv, meat = ctx.extraMeat)
        if (cmd.isBlank()) return costs
        for (segment in cmd.split(';')) {
            costs += segmentCosts(segment.trim(), ctx)
        }
        return costs
    }

    fun shouldSkipBoost(costs: BoostCosts, preferences: Preferences?): Boolean =
        costs.adv > 0 && preferences?.getBoolean("maximizerNoAdventures", false) == true

    fun appendToText(text: String, costs: BoostCosts): String {
        var result = text
        if (costs.adv > 0) result += "${costs.adv} adv, "
        if (costs.full != 0) result += "${costs.full} full, "
        if (costs.drunk != 0) result += "${costs.drunk} drunk, "
        if (costs.spleen != 0) result += "${costs.spleen} spleen, "
        if (costs.mp > 0) result += "${costs.mp} mp, "
        if (costs.soulsauce > 0) result += "${costs.soulsauce} soulsauce, "
        when {
            costs.thunder > 0 -> result += "${costs.thunder} dB of thunder, "
            costs.rain > 0 -> result += "${costs.rain} drops of rain, "
            costs.lightning > 0 -> result += "${costs.lightning} bolts of lightning, "
        }
        if (costs.hp > 0) result += "${costs.hp} hp, "
        if (costs.fuel > 0) result += "${costs.fuel} fuel, "
        if (costs.meat > 0) result += "${formatMeat(costs.meat)} meat, "
        return result
    }

    fun applyCapacityGreyout(
        cmd: String,
        costs: BoostCosts,
        charState: CharacterState,
        checkMeat: Boolean = true,
    ): String {
        if (cmd.isBlank()) return cmd
        var result = cmd
        if (costs.adv > charState.adventuresLeft) result = ""
        if (costs.full != 0 && charState.fullness + costs.full > charState.fullnessLimit) result = ""
        if (costs.drunk != 0 && charState.inebriety + costs.drunk > charState.inebrietyLimit) result = ""
        if (costs.spleen != 0 && charState.spleenUsed + costs.spleen > charState.spleenLimit) result = ""
        if (costs.soulsauce > 0 && costs.soulsauce > charState.soulsauce) result = ""
        if (costs.thunder > 0 && costs.thunder > charState.thunder) result = ""
        if (costs.rain > 0 && costs.rain > charState.rain) result = ""
        if (costs.lightning > 0 && costs.lightning > charState.lightning) result = ""
        if (costs.hp > 0 && costs.hp > charState.currentHp) result = ""
        if (checkMeat && costs.meat > 0) {
            if (result.startsWith("buy using storage")) {
                if (costs.meat > charState.storageMeat) result = ""
            } else if (
                result.startsWith("acquire") ||
                result.startsWith("make") ||
                result.startsWith("buy")
            ) {
                if (costs.meat > charState.meat) result = ""
            }
        }
        return result
    }

    private fun segmentCosts(segment: String, ctx: Context): BoostCosts {
        if (segment.isBlank()) return BoostCosts()
        val parts = segment.split(Regex("\\s+"))
        val verb = parts[0].lowercase()
        val rest = parts.drop(1).joinToString(" ")
        return when (verb) {
            "eat" -> BoostCosts(full = organHit(rest, ctx, ConsumableDatabase::getFullnessByName))
            "drink" -> BoostCosts(drunk = organHit(rest, ctx, ConsumableDatabase::getInebrietyByName))
            "chew" -> BoostCosts(spleen = organHit(rest, ctx, ConsumableDatabase::getSpleenByName))
            "use" -> BoostCosts()
            "cast" -> BoostCosts(mp = ctx.skillMpCost(rest).coerceAtLeast(0))
            "make", "create" -> {
                val itemId = itemIdFrom(rest, ctx) ?: return BoostCosts()
                BoostCosts(adv = adventureCost(itemId, ctx))
            }
            "buy" -> {
                val itemId = itemIdFrom(rest, ctx) ?: return BoostCosts()
                BoostCosts(meat = concoctionPrice(itemId, ctx))
            }
            "acquire" -> {
                val itemId = itemIdFrom(rest, ctx) ?: return BoostCosts()
                BoostCosts(meat = ctx.mallPrice(itemId))
            }
            else -> BoostCosts()
        }
    }

    private fun organHit(
        target: String,
        ctx: Context,
        lookup: (String) -> Int,
    ): Int {
        val name = itemNameFrom(target, ctx) ?: return 0
        return lookup(name)
    }

    private fun adventureCost(itemId: Int, ctx: Context): Int {
        val name = ctx.gameDatabase.item(itemId)?.name ?: return 0
        val concoction = ConcoctionDatabase.getByResult(name) ?: return 0
        if (concoction.ingredients.isEmpty()) return 0
        return CreatableTurns.adventuresNeeded(
            itemId = itemId,
            quantityNeeded = 1,
            context = CreatableTurns.Context(
                inventoryCount = { id -> ctx.physicalAccessible(id) },
                isPermitted = { true },
            ),
        )
    }

    private fun concoctionPrice(itemId: Int, ctx: Context): Long {
        val name = ctx.gameDatabase.item(itemId)?.name ?: return 0L
        val concoction = ConcoctionDatabase.getByResult(name) ?: return 0L
        return ConcoctionCreationCost.creationCost(concoction.methods)
    }

    private fun itemIdFrom(target: String, ctx: Context): Int? {
        val trimmed = target.trim()
        val idToken = trimmed.substringAfterLast(' ').removePrefix("\u00B6")
        idToken.toIntOrNull()?.let { return it }
        val qtyPrefix = Regex("""^(\d+)\s+(.+)$""")
        qtyPrefix.matchEntire(trimmed)?.let { match ->
            match.groupValues[2].removePrefix("\u00B6").toIntOrNull()?.let { return it }
            return ctx.gameDatabase.item(match.groupValues[2])?.id
        }
        return ctx.gameDatabase.item(trimmed)?.id
    }

    private fun itemNameFrom(target: String, ctx: Context): String? {
        val itemId = itemIdFrom(target, ctx)
        if (itemId != null) return ctx.gameDatabase.item(itemId)?.name
        return target.trim().takeIf { it.isNotBlank() }
    }

    private fun formatMeat(price: Long): String =
        price.toString().reversed().chunked(3).joinToString(",").reversed()
}
