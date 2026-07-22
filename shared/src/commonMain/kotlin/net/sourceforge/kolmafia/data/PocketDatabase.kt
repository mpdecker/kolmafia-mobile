package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * Full cargo-cult pocket data from [cultshorts.txt].
 * Mirrors desktop [PocketDatabase].
 */
@OptIn(ExperimentalResourceApi::class)
object PocketDatabase {

    enum class PocketType(val tag: String, val displayName: String = tag) {
        STATS("stats"),
        MONSTER("monster"),
        COMMON("common", "a common effect"),
        EFFECT("effect", "a rare effect"),
        RESTORE("restore", "a full HP/MP restoration and an effect"),
        BUFF("buff", "an accordion buff"),
        ELEMENT("element", "an elemental resistance effect"),
        JOKE("joke"),
        CANDY1("candy1", "a type 1 candy effect"),
        CANDY2("candy2", "a type 2 candy effect"),
        CHIPS1("chips1", "a potato chip effect"),
        GUM1("gum1", "a gum effect"),
        LENS1("lens1", "a contact lens effect"),
        NEEDLE1("needle1", "a needles effect"),
        TEETH1("teeth1", "a teeth effect"),
        CANDY("candy", "2 candy effects"),
        CHIPS("chips", "2 potato chip effects"),
        GUM("gum", "2 gum effects"),
        LENS("lens", "2 contact lens effects"),
        NEEDLE("needle", "2 needles effects"),
        TEETH("teeth", "2 teeth effects"),
        ITEM("item", "an item"),
        ITEM2("item2", "two items"),
        AVATAR("avatar", "an avatar potion"),
        BELL("bell", "a desk bell"),
        BOOZE("booze"),
        CASH("cash", "an item that is usable for meat"),
        CHESS("chess", "a chess piece"),
        CHOCO("choco", "some chocolate"),
        FOOD("food"),
        FRUIT("fruit"),
        OYSTER("oyster", "an oyster egg"),
        POTION("potion", "a potion"),
        YEG("yeg", "an item from Yeg's Motel"),
        SCRAP("scrap", "part of demon name"),
        POEM("poem", "an encrypted half-line of a poem"),
        MEAT("meat", "Meat and a puzzle clue"),
        ;

        val pockets: MutableMap<Int, Pocket> = mutableMapOf()
    }

    data class EffectResult(val name: String, val duration: Int)

    data class ItemResult(val name: String, val count: Int = 1)

    sealed class Pocket(open val pocket: Int, open val type: PocketType) {
        abstract override fun toString(): String
    }

    data class StatsPocket(
        override val pocket: Int,
        val muscle: Int,
        val mysticality: Int,
        val moxie: Int,
    ) : Pocket(pocket, PocketType.STATS) {
        fun count(stat: String): Int = when (stat.lowercase()) {
            "muscle" -> muscle
            "mysticality" -> mysticality
            "moxie" -> moxie
            else -> 0
        }

        override fun toString(): String = "stats: $muscle/$mysticality/$moxie"
    }

    data class MonsterPocket(
        override val pocket: Int,
        val monster: MonsterDefinition,
    ) : Pocket(pocket, PocketType.MONSTER) {
        override fun toString(): String = "a ${monster.name}"
    }

    data class MeatPocket(
        override val pocket: Int,
        val meat: Int,
        val text: String,
    ) : Pocket(pocket, PocketType.MEAT) {
        override fun toString(): String = "$meat Meat and a clue: $text"
    }

    data class PoemPocket(
        override val pocket: Int,
        val index: Int,
        val text: String,
    ) : Pocket(pocket, PocketType.POEM) {
        override fun toString(): String = "encrypted half-line #$index of a poem: $text"
    }

    data class ScrapPocket(
        override val pocket: Int,
        val scrap: Int,
    ) : Pocket(pocket, PocketType.SCRAP) {
        override fun toString(): String = "part #$scrap of a demon name"
    }

    data class OneResultPocket(
        override val pocket: Int,
        override val type: PocketType,
        val result1: EffectResult,
    ) : Pocket(pocket, type) {
        constructor(pocket: Int, type: PocketType, item: ItemResult) : this(
            pocket,
            type,
            EffectResult(item.name, item.count),
        )

        fun count(name: String): Int =
            if (result1.name.equals(name, ignoreCase = true)) result1.duration else 0

        override fun toString(): String =
            "${type.displayName}: ${result1.name} (${result1.duration})"
    }

    data class TwoResultPocket(
        override val pocket: Int,
        override val type: PocketType,
        val result1: EffectResult,
        val result2: EffectResult,
    ) : Pocket(pocket, type) {
        constructor(
            pocket: Int,
            type: PocketType,
            item1: ItemResult,
            item2: ItemResult,
        ) : this(
            pocket,
            type,
            EffectResult(item1.name, item1.count),
            EffectResult(item2.name, item2.count),
        )

        fun count(name: String): Int = when {
            result1.name.equals(name, ignoreCase = true) -> result1.duration
            result2.name.equals(name, ignoreCase = true) -> result2.duration
            else -> 0
        }

        override fun toString(): String =
            "${type.displayName}: ${result1.name} (${result1.duration}) and " +
                "${result2.name} (${result2.duration})"
    }

    data class JokePocket(
        override val pocket: Int,
        val joke: String,
    ) : Pocket(pocket, PocketType.JOKE) {
        val result1 = EffectResult("Joke-Mad", 20)

        fun count(name: String): Int =
            if (name.equals("Joke-Mad", ignoreCase = true)) 20 else 0

        override fun toString(): String = "Joke-Mad (20) and a joke: $joke"
    }

    private val tagToType = PocketType.entries.associateBy { it.tag.lowercase() }

    private val allPocketsMap = mutableMapOf<Int, Pocket>()
    val allPockets: Map<Int, Pocket> get() = allPocketsMap

    val effectPockets = mutableMapOf<String, MutableSet<OneResultPocket>>()
    val itemPockets = mutableMapOf<String, MutableSet<OneResultPocket>>()
    val monsterPockets = mutableMapOf<String, MonsterPocket>()
    val statsPockets = mutableMapOf<String, MutableSet<StatsPocket>>()

    val allEffectPockets = mutableSetOf<Int>()
    val allItemPockets = mutableSetOf<Int>()
    val allMonsterPockets = mutableSetOf<Int>()
    val allStatsPockets = mutableSetOf<Int>()

    var meatPockets: List<Pocket> = emptyList()
        private set
    var poemHalfLines: List<Pocket> = emptyList()
        private set
    var scrapSyllables: List<Pocket> = emptyList()
        private set

    private var loaded = false
    val isLoaded: Boolean get() = loaded

    suspend fun load() {
        if (loaded) return
        val text = Res.readBytes("files/data/cultshorts.txt").decodeToString()
        applyParse(parse(text))
        loaded = true
    }

    internal fun parseForTest(text: String): ParseResult = parse(text)

    internal fun applyParseForTest(result: ParseResult) {
        applyParse(result)
        loaded = true
    }

    internal fun resetForTest() {
        allPocketsMap.clear()
        PocketType.entries.forEach { it.pockets.clear() }
        effectPockets.clear()
        itemPockets.clear()
        monsterPockets.clear()
        statsPockets.clear()
        allEffectPockets.clear()
        allItemPockets.clear()
        allMonsterPockets.clear()
        allStatsPockets.clear()
        meatPockets = emptyList()
        poemHalfLines = emptyList()
        scrapSyllables = emptyList()
        loaded = false
    }

    data class ParseResult(val pockets: List<Pocket>)

    fun getPockets(type: PocketType?): Map<Int, Pocket>? = type?.pockets

    fun getPocketType(tag: String): PocketType? = tagToType[tag.lowercase()]

    fun pocketByNumber(pocket: Int): Pocket? = allPocketsMap[pocket]

    fun monsterByNumber(pocket: Int): MonsterDefinition? =
        (pocketByNumber(pocket) as? MonsterPocket)?.monster

    fun removePickedPockets(
        pockets: Map<Int, Pocket>,
        picked: Set<Int>,
    ): Map<Int, Pocket> = pockets.filterKeys { it !in picked }

    fun firstUnpickedPocket(pockets: List<Pocket>?, picked: Set<Int>): Pocket? {
        if (pockets == null) return null
        return pockets.firstOrNull { it.pocket !in picked }
    }

    fun sortPockets(type: PocketType, pockets: Map<Int, Pocket> = type.pockets): List<Pocket> =
        when (type) {
            PocketType.SCRAP ->
                pockets.values.sortedBy { (it as ScrapPocket).scrap }
            PocketType.MEAT ->
                pockets.values.sortedBy { (it as MeatPocket).meat }
            PocketType.POEM ->
                pockets.values.sortedBy { (it as PoemPocket).index }
            PocketType.MONSTER ->
                pockets.values.sortedBy { (it as MonsterPocket).monster.name.lowercase() }
            PocketType.ITEM, PocketType.AVATAR, PocketType.BELL, PocketType.BOOZE,
            PocketType.CASH, PocketType.CHESS, PocketType.CHOCO, PocketType.FOOD,
            PocketType.FRUIT, PocketType.OYSTER, PocketType.POTION, PocketType.YEG,
            PocketType.EFFECT, PocketType.RESTORE, PocketType.BUFF,
            PocketType.CANDY1, PocketType.CANDY2, PocketType.CHIPS1, PocketType.GUM1,
            PocketType.LENS1, PocketType.NEEDLE1, PocketType.TEETH1,
            ->
                pockets.values.sortedBy { (it as OneResultPocket).result1.name }
            PocketType.COMMON, PocketType.ELEMENT ->
                pockets.values.sortedWith(
                    compareBy<Pocket> { (it as OneResultPocket).result1.name }
                        .thenBy { it.pocket },
                )
            PocketType.ITEM2, PocketType.CANDY, PocketType.CHIPS, PocketType.GUM,
            PocketType.LENS, PocketType.NEEDLE, PocketType.TEETH,
            ->
                pockets.values.sortedWith(
                    compareBy<Pocket> { (it as TwoResultPocket).result1.name }
                        .thenBy { (it as TwoResultPocket).result2.name },
                )
            PocketType.STATS, PocketType.JOKE ->
                pockets.values.sortedBy { it.pocket }
        }

    fun sortStats(stat: String, pockets: Set<StatsPocket>): List<Pocket> =
        pockets.filter { it.count(stat) > 0 }
            .sortedWith(compareByDescending<StatsPocket> { it.count(stat) }.thenBy { it.pocket })

    fun sortResults(name: String, pockets: Set<OneResultPocket>): List<Pocket> =
        pockets.sortedWith(
            compareByDescending<OneResultPocket> { it.count(name) }.thenBy { it.pocket },
        )

    private fun applyParse(result: ParseResult) {
        resetForTest()
        for (pocket in result.pockets) {
            registerPocket(pocket)
        }
        scrapSyllables = sortPockets(PocketType.SCRAP)
        poemHalfLines = sortPockets(PocketType.POEM)
        meatPockets = sortPockets(PocketType.MEAT)
        loaded = true
    }

    private fun registerPocket(pocket: Pocket) {
        allPocketsMap[pocket.pocket] = pocket
        pocket.type.pockets[pocket.pocket] = pocket
        when (pocket) {
            is OneResultPocket -> when (pocket.type) {
                PocketType.COMMON, PocketType.EFFECT, PocketType.BUFF, PocketType.ELEMENT,
                PocketType.JOKE, PocketType.RESTORE, PocketType.CANDY1, PocketType.CANDY2,
                PocketType.CHIPS1, PocketType.GUM1, PocketType.LENS1, PocketType.NEEDLE1,
                PocketType.TEETH1,
                -> {
                    addEffectPocket(pocket)
                    allEffectPockets.add(pocket.pocket)
                }
                PocketType.ITEM, PocketType.AVATAR, PocketType.BELL, PocketType.BOOZE,
                PocketType.CASH, PocketType.CHESS, PocketType.CHOCO, PocketType.FOOD,
                PocketType.FRUIT, PocketType.OYSTER, PocketType.POTION, PocketType.YEG,
                -> {
                    addItemPocket(pocket)
                    allItemPockets.add(pocket.pocket)
                }
                else -> Unit
            }
            is TwoResultPocket -> when (pocket.type) {
                PocketType.CANDY, PocketType.CHIPS, PocketType.GUM, PocketType.LENS,
                PocketType.NEEDLE, PocketType.TEETH,
                -> {
                    addEffectPocket(
                        OneResultPocket(pocket.pocket, pocket.type, pocket.result1),
                    )
                    addEffectPocket(
                        OneResultPocket(pocket.pocket, pocket.type, pocket.result2),
                    )
                    allEffectPockets.add(pocket.pocket)
                }
                PocketType.ITEM2 -> {
                    addItemPocket(
                        OneResultPocket(pocket.pocket, pocket.type, pocket.result1),
                    )
                    addItemPocket(
                        OneResultPocket(pocket.pocket, pocket.type, pocket.result2),
                    )
                    allItemPockets.add(pocket.pocket)
                }
                else -> Unit
            }
            is JokePocket -> {
                addEffectPocket(OneResultPocket(pocket.pocket, PocketType.JOKE, pocket.result1))
                allEffectPockets.add(pocket.pocket)
            }
            is MonsterPocket -> {
                monsterPockets[pocket.monster.name.lowercase()] = pocket
                allMonsterPockets.add(pocket.pocket)
            }
            is StatsPocket -> {
                addStatsPocket("muscle", pocket.muscle, pocket)
                addStatsPocket("mysticality", pocket.mysticality, pocket)
                addStatsPocket("moxie", pocket.moxie, pocket)
                allStatsPockets.add(pocket.pocket)
            }
            else -> Unit
        }
    }

    private fun addEffectPocket(orp: OneResultPocket) {
        effectPockets.getOrPut(orp.result1.name) { mutableSetOf() }.add(orp)
    }

    private fun addItemPocket(orp: OneResultPocket) {
        itemPockets.getOrPut(orp.result1.name) { mutableSetOf() }.add(orp)
    }

    private fun addStatsPocket(name: String, stat: Int, sp: StatsPocket) {
        if (stat == 0) return
        statsPockets.getOrPut(name) { mutableSetOf() }.add(sp)
    }

    private fun parse(text: String): ParseResult {
        val pockets = mutableListOf<Pocket>()
        for (line in text.lines()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue
            val parts = trimmed.split('\t')
            if (parts.size < 2) continue
            val pocketId = parts[0].toIntOrNull() ?: continue
            if (pocketId !in 1..666) continue
            val type = tagToType[parts[1].lowercase()] ?: continue
            parsePocketData(pocketId, type, parts)?.let { pockets += it }
        }
        return ParseResult(pockets)
    }

    private fun parsePocketData(pocketId: Int, type: PocketType, data: List<String>): Pocket? {
        return when (type) {
            PocketType.STATS -> {
                if (data.size < 5) return null
                val muscle = data[2].toIntOrNull() ?: return null
                val mysticality = data[3].toIntOrNull() ?: return null
                val moxie = data[4].toIntOrNull() ?: return null
                StatsPocket(pocketId, muscle, mysticality, moxie)
            }
            PocketType.MONSTER -> {
                if (data.size < 3) return null
                val monster = MonsterDatabase.getByName(data[2]) ?: return null
                MonsterPocket(pocketId, monster)
            }
            PocketType.COMMON, PocketType.EFFECT, PocketType.RESTORE, PocketType.BUFF,
            PocketType.ELEMENT, PocketType.CANDY1, PocketType.CANDY2, PocketType.CHIPS1,
            PocketType.GUM1, PocketType.LENS1, PocketType.NEEDLE1, PocketType.TEETH1,
            -> {
                if (data.size < 3) return null
                val effect = parseEffectString(data[2]) ?: return null
                OneResultPocket(pocketId, type, effect)
            }
            PocketType.CANDY, PocketType.CHIPS, PocketType.GUM, PocketType.LENS,
            PocketType.NEEDLE, PocketType.TEETH,
            -> {
                if (data.size < 4) return null
                val effect1 = parseEffectString(data[2]) ?: return null
                val effect2 = parseEffectString(data[3]) ?: return null
                TwoResultPocket(pocketId, type, effect1, effect2)
            }
            PocketType.ITEM, PocketType.AVATAR, PocketType.BELL, PocketType.BOOZE,
            PocketType.CASH, PocketType.CHESS, PocketType.CHOCO, PocketType.FOOD,
            PocketType.FRUIT, PocketType.OYSTER, PocketType.POTION, PocketType.YEG,
            -> {
                if (data.size < 3) return null
                val item = parseItemString(data[2]) ?: return null
                OneResultPocket(pocketId, type, item)
            }
            PocketType.ITEM2 -> {
                if (data.size < 4) return null
                val item1 = parseItemString(data[2]) ?: return null
                val item2 = parseItemString(data[3]) ?: return null
                TwoResultPocket(
                    pocketId,
                    type,
                    EffectResult(item1.name, item1.count),
                    EffectResult(item2.name, item2.count),
                )
            }
            PocketType.JOKE -> {
                if (data.size < 3) return null
                JokePocket(pocketId, data[2])
            }
            PocketType.MEAT -> {
                if (data.size < 4) return null
                val meat = data[2].toIntOrNull() ?: return null
                MeatPocket(pocketId, meat, data[3])
            }
            PocketType.POEM -> {
                if (data.size < 4) return null
                val index = data[2].toIntOrNull() ?: return null
                PoemPocket(pocketId, index, data[3])
            }
            PocketType.SCRAP -> {
                if (data.size < 3) return null
                val scrap = data[2].toIntOrNull() ?: return null
                ScrapPocket(pocketId, scrap)
            }
        }
    }

    private val effectPattern = Regex("""^(.+?) \((\d+)\)$""")

    internal fun parseEffectString(raw: String): EffectResult? {
        val match = effectPattern.matchEntire(raw.trim()) ?: return null
        val name = match.groupValues[1].trim()
        val duration = match.groupValues[2].toIntOrNull() ?: return null
        if (EffectDatabase.getByName(name) == null) return null
        return EffectResult(name, duration)
    }

    internal fun parseItemString(raw: String): ItemResult? {
        val name = raw.trim()
        if (ItemDatabase.getByName(name) == null && ItemDatabase.getByPluralOrName(name) == null) {
            return null
        }
        return ItemResult(name)
    }
}
