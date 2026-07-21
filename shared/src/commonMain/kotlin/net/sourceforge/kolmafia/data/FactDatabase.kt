package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.CharacterClass
import net.sourceforge.kolmafia.modifiers.ExpressionContext
import net.sourceforge.kolmafia.modifiers.ModifierExpression
import net.sourceforge.kolmafia.shared.generated.resources.Res
import net.sourceforge.kolmafia.utilities.PHPMTRandom
import org.jetbrains.compose.resources.ExperimentalResourceApi

/** Book of Facts lookup. Mirrors desktop [FactDatabase]. */
@OptIn(ExperimentalResourceApi::class)
object FactDatabase {

    enum class FactType {
        NONE,
        EFFECT,
        ITEM,
        STATS,
        HP,
        MP,
        MEAT,
        MODIFIER,
        ;

        override fun toString(): String = name.lowercase()

        companion object {
            fun find(name: String): FactType? =
                entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
        }
    }

    enum class MonsterPhylum(val token: String) {
        NONE("none"),
        BEAST("beast"),
        BUG("bug"),
        CONSTELLATION("constellation"),
        CONSTRUCT("construct"),
        DEMON("demon"),
        DUDE("dude"),
        ELEMENTAL("elemental"),
        ELF("elf"),
        FISH("fish"),
        GOBLIN("goblin"),
        HIPPY("hippy"),
        HOBO("hobo"),
        HORROR("horror"),
        HUMANOID("humanoid"),
        MER_KIN("mer-kin"),
        ORC("orc"),
        PENGUIN("penguin"),
        PIRATE("pirate"),
        PLANT("plant"),
        SLIME("slime"),
        UNDEAD("undead"),
        WEIRD("weird"),
        ;

        companion object {
            fun find(name: String): MonsterPhylum {
                if (name.equals("none", ignoreCase = true)) return NONE
                val normalized = name.replace("-", "")
                return entries.firstOrNull { phylum ->
                    phylum != NONE && (
                        name.equals(phylum.token, ignoreCase = true) ||
                            normalized.equals(phylum.token.replace("-", ""), ignoreCase = true)
                        )
                } ?: NONE
            }

            fun fromMonsterPhylum(phylum: String): MonsterPhylum = find(phylum)
        }
    }

    data class ResolvedFact(val type: FactType, val display: String)

    private data class PoolOption(
        val display: String,
        val effectId: Int = 0,
    )

    private sealed class FactTemplate {
        abstract val type: FactType
        abstract fun display(): String

        abstract fun resolve(
            characterClass: CharacterClass,
            ascensionPath: AscensionPath,
            monster: MonsterDefinition,
            stateful: Boolean,
            expressionContext: ExpressionContext,
        ): ResolvedFact

        class SimpleFact(
            override val type: FactType,
            private val value: String,
        ) : FactTemplate() {
            override fun display(): String = when (type) {
                FactType.HP -> "$value% HP restore"
                FactType.MP -> "$value% MP restore"
                FactType.STATS -> "$value substats"
                else -> value
            }

            override fun resolve(
                characterClass: CharacterClass,
                ascensionPath: AscensionPath,
                monster: MonsterDefinition,
                stateful: Boolean,
                expressionContext: ExpressionContext,
            ): ResolvedFact = ResolvedFact(type, display())
        }

        class StatsFact(
            stat: String,
            statValue: Int,
        ) : FactTemplate() {
            override val type = FactType.STATS
            private val value = "+$statValue $stat"

            override fun display(): String = "$value substats"

            override fun resolve(
                characterClass: CharacterClass,
                ascensionPath: AscensionPath,
                monster: MonsterDefinition,
                stateful: Boolean,
                expressionContext: ExpressionContext,
            ): ResolvedFact = ResolvedFact(type, display())
        }

        class MeatFact(
            private val baseMeat: Boolean,
            private val presetMeat: Int = 0,
        ) : FactTemplate() {
            override val type = FactType.MEAT

            override fun display(): String = "$presetMeat Meat"

            override fun resolve(
                characterClass: CharacterClass,
                ascensionPath: AscensionPath,
                monster: MonsterDefinition,
                stateful: Boolean,
                expressionContext: ExpressionContext,
            ): ResolvedFact {
                val meat = if (baseMeat) {
                    monster.meatDrop
                } else {
                    val seed = calculateSeed(characterClass, ascensionPath, monster) + 12L
                    PHPMTRandom(seed).nextInt(0, 50) + 100
                }
                return ResolvedFact(type, "$meat Meat")
            }
        }

        class PoolFact(
            override val type: FactType,
            private val options: List<PoolOption>,
            private val condition: String?,
            private val isHeap: Boolean,
        ) : FactTemplate() {
            override fun display(): String = options.joinToString(" or ") { it.display }

            override fun resolve(
                characterClass: CharacterClass,
                ascensionPath: AscensionPath,
                monster: MonsterDefinition,
                stateful: Boolean,
                expressionContext: ExpressionContext,
            ): ResolvedFact {
                if (stateful && condition != null && !evaluateCondition(condition, expressionContext)) {
                    return ResolvedFact(FactType.NONE, "")
                }
                val seedMod = if (isHeap) 11L else 13L
                val seed = calculateSeed(characterClass, ascensionPath, monster) + seedMod
                val rng = PHPMTRandom(seed)
                var option = options[rng.nextInt(0, options.size - 1)]
                if (
                    ascensionPath == AscensionPath.UNDER_THE_SEA &&
                    type == FactType.EFFECT &&
                    option.effectId == FISHY_EFFECT_ID
                ) {
                    option = PoolOption("Fishy Fortification (10)", FISHY_FORTIFICATION_EFFECT_ID)
                }
                return ResolvedFact(type, option.display)
            }
        }
    }

    private val facts = mutableMapOf<MonsterPhylum, MutableList<FactTemplate>>()
    private var loaded = false

    val isLoaded: Boolean get() = loaded
    val loadedFactCount: Int get() = facts.values.sumOf { it.size }

    private const val FISHY_EFFECT_ID = 549
    private const val FISHY_FORTIFICATION_EFFECT_ID = 468

    private val heapItems = listOf(
        "big rock",
        "pretty flower",
        "ice-cold sir schlitz",
        "hermit permit",
        "worthless trinket",
        "worthless gewgaw",
        "worthless knick-knack",
        "ice-cold Willer",
        "rusty metal ring",
        "rusty metal shaft",
        "meat from yesterday",
        "spring",
        "sprocket",
        "cog",
        "empty meat tank",
        "ice-cold six-pack",
        "valuable trinket",
        "barbed-wire fence",
        "ghuol egg",
        "skeleton bone",
        "skewer",
        "lihc eye",
        "uncooked chorizo",
        "ice-cold fotie",
        "batgut",
        "briefcase",
        "fat stacks of cash",
        "loose teeth",
        "bat guano",
        "rat appendix",
        "hemp string",
        "gnoll teeth",
        "ten-leaf clover",
        "dead guy's watch",
        "white lightning",
        "mullet wig",
        "tenderizing hammer",
        "linoleum sword hilt",
        "linoleum stick",
        "linoleum crossbow string",
        "asbestos sword hilt",
        "asbestos stick",
        "asbestos crossbow string",
        "chrome sword hilt",
        "chrome stick",
        "chrome crossbow string",
        "yeti fur",
        "penguin skin",
        "yak skin",
        "hippopotamus skin",
        "pirate pelvis",
        "box",
        "bloody clown pants",
        "beer lens",
        "disease",
        "flaming crutch",
        "cast",
        "leather mask",
        "mesh cap",
        "enormous belt buckle",
        "catgut",
        "pr0n legs",
        "pine-fresh air freshener",
        "razor-sharp can lid",
        "Mad Train wine",
        "dirty hobo gloves",
        "furry pants",
        "disturbing fanfic",
        "fruitcake",
        "spiked femur",
        "filthy hippy poncho",
        "broken skull",
        "foon",
        "mob penguin cellular phone",
        "lead necklace",
        "pine tar",
        "tasket",
        "urinal cake",
        "blood flower",
        "lovecat tail",
        "plastic passion fruit",
        "picture of a dead guy's girlfriend",
        "length of string",
        "googly eye",
        "stuffing",
        "felt",
        "wooden block",
        "stench powder",
        "sleaze nuggets",
        "squashed frog",
        "alphabet gum",
        "old leather wallet",
        "old coin purse",
        "gob of wet hair",
        "[2108]rock",
        "stringy sinew",
        "stick",
        "tooth",
        "filthy poultice",
    )

    suspend fun load() {
        if (loaded) return
        val text = Res.readBytes("files/data/bookoffacts.txt").decodeToString()
        applyParse(parse(text))
        loaded = true
    }

    fun factString(
        monster: MonsterDefinition?,
        characterClass: CharacterClass,
        ascensionPath: AscensionPath,
        expressionContext: ExpressionContext?,
    ): String {
        if (monster == null) return ""
        return getFact(monster, characterClass, ascensionPath, stateful = true, expressionContext).display
    }

    fun factTypeString(
        monster: MonsterDefinition?,
        characterClass: CharacterClass,
        ascensionPath: AscensionPath,
        expressionContext: ExpressionContext?,
    ): String {
        if (monster == null) return ""
        return getFact(
            monster,
            characterClass,
            ascensionPath,
            stateful = true,
            expressionContext,
        ).type.toString()
    }

    fun getFact(
        monster: MonsterDefinition?,
        characterClass: CharacterClass,
        ascensionPath: AscensionPath,
        stateful: Boolean,
        expressionContext: ExpressionContext?,
    ): ResolvedFact {
        if (monster == null) {
            return ResolvedFact(FactType.NONE, "")
        }
        val ctx = expressionContext ?: ExpressionContext.EMPTY
        val seed = calculateSeed(characterClass, ascensionPath, monster)
        val rng = PHPMTRandom(seed.toLong())
        val phylum = MonsterPhylum.fromMonsterPhylum(monster.phylum)
        val effectivePhylum = if (isPhylumEffect(ascensionPath, phylum, seed)) phylum else MonsterPhylum.NONE
        val pool = facts[effectivePhylum] ?: facts[MonsterPhylum.NONE].orEmpty()
        if (pool.isEmpty()) {
            return ResolvedFact(FactType.NONE, "")
        }
        val template = pool[rng.nextInt(0, pool.size - 1)]
        return template.resolve(characterClass, ascensionPath, monster, stateful, ctx)
    }

    fun calculateSeed(
        characterClass: CharacterClass,
        ascensionPath: AscensionPath,
        monster: MonsterDefinition,
    ): Int {
        val classId = if (characterClass == CharacterClass.UNKNOWN) 0 else characterClass.id
        return (421 * classId) + (11 * ascensionPath.pathId) + monster.id
    }

    internal fun parseForTest(text: String): Int = parse(text).values.sumOf { it.size }

    internal fun injectParsedForTest(text: String) {
        applyParse(parse(text))
        loaded = true
    }

    internal fun resetForTest() {
        facts.clear()
        loaded = false
    }

    private fun applyParse(snapshot: Map<MonsterPhylum, List<FactTemplate>>) {
        facts.clear()
        snapshot.forEach { (phylum, list) -> facts[phylum] = list.toMutableList() }
    }

    private fun parse(text: String): Map<MonsterPhylum, List<FactTemplate>> {
        val parsed = mutableMapOf<MonsterPhylum, MutableList<FactTemplate>>()
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isBlank() || line.startsWith("#")) continue
            if (line.toIntOrNull() != null && !line.contains('\t')) continue
            val parts = line.split('\t')
            if (parts.size < 2) continue
            val phylum = MonsterPhylum.find(parts[0])
            if (phylum == MonsterPhylum.NONE && !parts[0].equals("none", ignoreCase = true)) continue
            val type = FactType.find(parts[1]) ?: continue
            val fact = parseFactData(type, parts) ?: continue
            parsed.getOrPut(phylum) { mutableListOf() }.add(fact)
        }
        return parsed
    }

    private fun parseFactData(type: FactType, data: List<String>): FactTemplate? {
        return when (type) {
            FactType.NONE -> null
            FactType.EFFECT, FactType.ITEM -> parsePoolFact(type, data)
            FactType.MEAT -> FactTemplate.MeatFact(data.size >= 3 && data[2].equals("Base", ignoreCase = true))
            FactType.HP, FactType.MP, FactType.MODIFIER -> {
                if (data.size < 3) null
                else FactTemplate.SimpleFact(type, decodeEntities(data[2]))
            }
            FactType.STATS -> {
                if (data.size < 4) null
                else {
                    val value = data[2].toIntOrNull() ?: return null
                    val stat = data[3].lowercase()
                    if (stat != "all" && !isValidStat(stat)) null
                    else FactTemplate.StatsFact(stat, value)
                }
            }
        }
    }

    private fun parsePoolFact(type: FactType, data: List<String>): FactTemplate? {
        if (data.size < 3) return null
        val thirdPart = data[2]
        if (thirdPart == "HEAP") {
            val options = heapItems.map { PoolOption(it) }
            return FactTemplate.PoolFact(type, options, condition = null, isHeap = true)
        }
        var condition: String? = null
        val valueParts = data.toMutableList()
        if (thirdPart.startsWith("[") && thirdPart.endsWith("]")) {
            condition = thirdPart
            valueParts[2] = ""
            if (valueParts.count { it.isNotBlank() } < 3) return null
        }
        val options = valueParts.drop(2)
            .filter { it.isNotBlank() }
            .mapNotNull { raw ->
                val decoded = decodeEntities(raw)
                if (type == FactType.ITEM) parseItemDisplay(decoded) else parseEffectDisplay(decoded)
            }
        if (options.isEmpty()) return null
        val poolOptions = options.map { display ->
            if (type == FactType.EFFECT) {
                val effectName = display.substringBefore(" (").trim()
                PoolOption(display, effectId = EffectDatabase.getByName(effectName)?.id ?: 0)
            } else {
                PoolOption(display)
            }
        }
        return FactTemplate.PoolFact(type, poolOptions, condition, isHeap = false)
    }

    private fun parseEffectDisplay(raw: String): String? {
        var name = raw
        var duration = 0
        val lparen = raw.lastIndexOf('(')
        val rparen = raw.lastIndexOf(')')
        if (lparen >= 0 && rparen > lparen) {
            val durationString = raw.substring(lparen + 1, rparen)
            if (durationString.all { it.isDigit() }) {
                name = raw.substring(0, lparen).trim()
                duration = durationString.toInt()
            }
        }
        if (EffectDatabase.getByName(name) == null) return null
        return if (duration > 0 && duration != 1) "$name ($duration)" else name
    }

    private fun parseItemDisplay(raw: String): String? {
        val trimmed = raw.trim()
        if (ItemDatabase.getByName(trimmed) != null && !trimmed.matches(Regex("^\\[\\d+].*"))) {
            return trimmed
        }
        val parts = trimmed.split('(', ')').map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.isEmpty()) return null
        val nameBuilder = StringBuilder(parts[0])
        var count = 1
        for (i in 1 until parts.size) {
            val next = parts[i]
            if (i == parts.lastIndex && next.all { it.isDigit() }) {
                count = next.toInt()
            } else if (next.isNotEmpty()) {
                nameBuilder.append(" (").append(next).append(")")
            }
        }
        val name = nameBuilder.toString()
        val lookupName = name.substringBefore(" (").trim().removePrefix("[2108]")
        if (ItemDatabase.getByName(lookupName) == null && ItemDatabase.getByName(name) == null) {
            return null
        }
        return if (count == 1) name else "${name.substringBefore(" (").trim()} ($count)"
    }

    private fun isValidStat(stat: String): Boolean =
        stat in setOf("muscle", "mysticality", "moxie")

    private fun isPhylumEffect(
        ascensionPath: AscensionPath,
        phylum: MonsterPhylum,
        seed: Int,
    ): Boolean {
        val mod = if (ascensionPath == AscensionPath.SMALL && phylum == MonsterPhylum.BUG) 2 else 3
        return seed % mod == 1
    }

    private fun evaluateCondition(condition: String, ctx: ExpressionContext): Boolean {
        val inner = condition.trim().removePrefix("[").removeSuffix("]")
        return ModifierExpression(inner).evaluate(ctx) > 0
    }

    private fun decodeEntities(value: String): String =
        value.replace("&quot;", "\"")
}
