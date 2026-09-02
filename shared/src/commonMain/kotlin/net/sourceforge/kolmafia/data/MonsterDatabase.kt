package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.shared.generated.resources.Res
import net.sourceforge.kolmafia.utilities.leetify
import org.jetbrains.compose.resources.ExperimentalResourceApi

/** Desktop [MonsterDatabase.BlueVsRedTeam] from the BvR: token. */
enum class BlueVsRedTeam(val teamName: String) {
    BLUE("blue"),
    RED("red"),
    ENEMY("enemy"),
    UNKNOWN("unknown");

    companion object {
        fun from(name: String): BlueVsRedTeam =
            entries.firstOrNull { it.teamName.equals(name, ignoreCase = true) } ?: UNKNOWN
    }
}

// Parses monsters.txt from the bundled compose resources.
// Format (tab-separated): name  id  image  parameters  [drop1  drop2  ...]
// Parameters is a space-separated list of Key: value pairs and flags.
// Call load() once at app startup (or lazily on first access).
@OptIn(ExperimentalResourceApi::class)
object MonsterDatabase {

    private val _byId = mutableMapOf<Int, MonsterDefinition>()
    private val _byName = mutableMapOf<String, MonsterDefinition>()
    private val _byLeetName = mutableMapOf<String, MonsterDefinition>()
    private var loaded = false

    val byId: Map<Int, MonsterDefinition> get() = _byId
    val byName: Map<String, MonsterDefinition> get() = _byName

    suspend fun load() {
        if (loaded) return
        val text = Res.readBytes("files/data/monsters.txt").decodeToString()
        parse(text)
        loaded = true
    }

    fun getById(id: Int): MonsterDefinition? = _byId[id]
    fun getByName(name: String): MonsterDefinition? = _byName[name.lowercase()]

    fun translateLeetMonsterName(leetName: String): String =
        _byLeetName[leetName]?.name ?: leetName

    /** Desktop [MonsterData] beeCount from name; wandering bees excluded. */
    fun computeBeeCount(name: String, id: Int): Int {
        if (id in 1075..1083) return 0
        return name.count { it == 'b' || it == 'B' }
    }

    fun all(): Collection<MonsterDefinition> = _byId.values
    fun byPhylum(phylum: String): List<MonsterDefinition> =
        _byId.values.filter { it.phylum.equals(phylum, ignoreCase = true) }
    fun bosses(): List<MonsterDefinition> = _byId.values.filter { it.isBoss }

    private data class ParsedParams(
        val attack: Int = 0,
        val attackExpression: String? = null,
        val hasAttack: Boolean = false,
        val defense: Int = 0,
        val defenseExpression: String? = null,
        val hasDefense: Boolean = false,
        val hp: Int = 0,
        val hpExpression: String? = null,
        val hasHp: Boolean = false,
        val initiative: Int = 0,
        val hasInitiative: Boolean = false,
        val initiativeExpression: String? = null,
        val meatDrop: Int = 0,
        val phylum: String = "",
        val isBoss: Boolean = false,
        val isGhost: Boolean = false,
        val subTypes: List<String> = emptyList(),
        val isLucky: Boolean = false,
        val isScaling: Boolean = false,
        val scale: Int = 0,
        val scaleExpression: String? = null,
        val cap: Int = 0,
        val capExpression: String? = null,
        val floor: Int = 0,
        val floorExpression: String? = null,
        val experience: Int = 0,
        val experienceExpression: String? = null,
        val hasExperience: Boolean = false,
        val mlMult: Int = 1,
        val mlMultExpression: String? = null,
        val hasMlMult: Boolean = false,
        val article: String = "",
        val isCopyable: Boolean = true,
        val isWishable: Boolean = true,
        val poison: Int = Int.MAX_VALUE,
        val group: Int = 1,
        val manuelName: String? = null,
        val wikiName: String? = null,
        val attackElements: List<String> = emptyList(),
        val attackElement: String = "",
        val defenseElement: String = "",
        val physicalResistance: Int = 0,
        val physicalResistanceExpression: String? = null,
        val elementalResistance: Int = 0,
        val elementalResistanceExpression: String? = null,
        val hotResistance: Int = 0,
        val hotResistanceExpression: String? = null,
        val coldResistance: Int = 0,
        val coldResistanceExpression: String? = null,
        val stenchResistance: Int = 0,
        val stenchResistanceExpression: String? = null,
        val spookyResistance: Int = 0,
        val spookyResistanceExpression: String? = null,
        val sleazeResistance: Int = 0,
        val sleazeResistanceExpression: String? = null,
        val minSprinkles: Int = 0,
        val minSprinklesExpression: String? = null,
        val maxSprinkles: Int = 0,
        val maxSprinklesExpression: String? = null,
        val blueVsRedTeam: BlueVsRedTeam = BlueVsRedTeam.UNKNOWN,
    )

    private fun parseParams(params: String): ParsedParams {
        val tokens = params.split(' ').filter { it.isNotEmpty() }
        var attack = 0
        var attackExpression: String? = null
        var hasAttack = false
        var defense = 0
        var defenseExpression: String? = null
        var hasDefense = false
        var hp = 0
        var hpExpression: String? = null
        var hasHp = false
        var initiative = 0
        var hasInitiative = false
        var initiativeExpression: String? = null
        var meatDrop = 0
        var phylum = ""
        var isBoss = false
        var isGhost = false
        val subTypes = mutableListOf<String>()
        var isLucky = false
        var isScaling = false
        var scale = 0
        var scaleExpression: String? = null
        var cap = 0
        var capExpression: String? = null
        var floor = 0
        var floorExpression: String? = null
        var experience = 0
        var experienceExpression: String? = null
        var hasExperience = false
        var mlMult = 1
        var mlMultExpression: String? = null
        var hasMlMult = false
        var sawCap = false
        var sawFloor = false
        var article = ""
        var isCopyable = true
        var isWishable = true
        var poison = Int.MAX_VALUE
        var group = 1
        var manuelName: String? = null
        var wikiName: String? = null
        val attackElements = mutableListOf<String>()
        var defenseElement = ""
        var physicalResistance = 0
        var physicalResistanceExpression: String? = null
        var elementalResistance = 0
        var elementalResistanceExpression: String? = null
        var hotResistance = 0
        var hotResistanceExpression: String? = null
        var coldResistance = 0
        var coldResistanceExpression: String? = null
        var stenchResistance = 0
        var stenchResistanceExpression: String? = null
        var spookyResistance = 0
        var spookyResistanceExpression: String? = null
        var sleazeResistance = 0
        var sleazeResistanceExpression: String? = null
        var minSprinkles = 0
        var minSprinklesExpression: String? = null
        var maxSprinkles = 0
        var maxSprinklesExpression: String? = null
        var blueVsRedTeam = BlueVsRedTeam.UNKNOWN

        var i = 0
        while (i < tokens.size) {
            val token = tokens[i]
            when {
                token == "BOSS" -> isBoss = true
                token == "GHOST" -> {
                    isGhost = true
                    subTypes.add("ghost")
                }
                token == "BUGBEAR" -> subTypes.add("bugbear")
                token == "SKELETON" -> subTypes.add("skeleton")
                token == "VAMPIRE" -> subTypes.add("vampire")
                token == "WEREWOLF" -> subTypes.add("werewolf")
                token == "ZOMBIE" -> subTypes.add("zombie")
                token == "SEAL" -> subTypes.add("seal")
                token == "SNAKE" -> subTypes.add("snake")
                token == "DRIPPY" -> subTypes.add("drippy")
                token == "LUCKY" -> isLucky = true
                token == "NOCOPY" -> isCopyable = false
                token == "NOWISH" -> isWishable = false
                token == "ULTRARARE" -> { /* ignored */ }
                token.endsWith(':') -> {
                    val peek = tokens.getOrNull(i + 1) ?: ""
                    when (token) {
                        "Atk:" -> {
                            hasAttack = true
                            val (value, skip) = readBracketOrToken(tokens, i + 1)
                            if (value.startsWith("[")) {
                                attackExpression = value.removePrefix("[").removeSuffix("]")
                                attack = 0
                            } else {
                                attackExpression = null
                                attack = value.toIntOrNull() ?: 0
                            }
                            i += skip
                        }
                        "Def:" -> {
                            hasDefense = true
                            val (value, skip) = readBracketOrToken(tokens, i + 1)
                            if (value.startsWith("[")) {
                                defenseExpression = value.removePrefix("[").removeSuffix("]")
                                defense = 0
                            } else {
                                defenseExpression = null
                                defense = value.toIntOrNull() ?: 0
                            }
                            i += skip
                        }
                        "HP:" -> {
                            hasHp = true
                            val (value, skip) = readBracketOrToken(tokens, i + 1)
                            if (value.startsWith("[")) {
                                hpExpression = value.removePrefix("[").removeSuffix("]")
                                hp = 0
                            } else {
                                hpExpression = null
                                hp = value.toIntOrNull() ?: 0
                            }
                            i += skip
                        }
                        "Init:" -> {
                            hasInitiative = true
                            val (value, skip) = readBracketOrToken(tokens, i + 1)
                            if (value.startsWith("[")) {
                                initiativeExpression = value.removePrefix("[").removeSuffix("]")
                                initiative = 0
                            } else {
                                initiativeExpression = null
                                initiative = value.toIntOrNull() ?: 0
                            }
                            i += skip
                        }
                        "Meat:" -> { meatDrop = peek.toIntOrNull() ?: 0; i++ }
                        "P:" -> { phylum = peek; i++ }
                        "Scale:" -> {
                            isScaling = true
                            val (value, skip) = readBracketOrToken(tokens, i + 1)
                            if (value.startsWith("[")) {
                                scaleExpression = value.removePrefix("[").removeSuffix("]")
                                scale = 0
                            } else {
                                scaleExpression = null
                                scale = value.toIntOrNull() ?: 0
                            }
                            i += skip
                        }
                        "Cap:" -> {
                            sawCap = true
                            val (value, skip) = readBracketOrToken(tokens, i + 1)
                            if (value.startsWith("[")) {
                                capExpression = value.removePrefix("[").removeSuffix("]")
                                cap = MonsterDefinition.DEFAULT_CAP
                            } else {
                                capExpression = null
                                cap = when {
                                    value == "?" -> MonsterDefinition.DEFAULT_CAP
                                    else -> value.toIntOrNull() ?: MonsterDefinition.DEFAULT_CAP
                                }
                            }
                            i += skip
                        }
                        "Floor:" -> {
                            sawFloor = true
                            val (value, skip) = readBracketOrToken(tokens, i + 1)
                            if (value.startsWith("[")) {
                                floorExpression = value.removePrefix("[").removeSuffix("]")
                                floor = MonsterDefinition.DEFAULT_FLOOR
                            } else {
                                floorExpression = null
                                floor = when {
                                    value == "?" -> MonsterDefinition.DEFAULT_FLOOR
                                    else -> value.toIntOrNull() ?: MonsterDefinition.DEFAULT_FLOOR
                                }
                            }
                            i += skip
                        }
                        "Exp:" -> {
                            hasExperience = true
                            val (value, skip) = readBracketOrToken(tokens, i + 1)
                            if (value.startsWith("[")) {
                                experienceExpression = value.removePrefix("[").removeSuffix("]")
                                experience = 0
                            } else {
                                experienceExpression = null
                                experience = value.toIntOrNull() ?: 0
                            }
                            i += skip
                        }
                        "MLMult:" -> {
                            hasMlMult = true
                            val r = readNumOrExpr(tokens, i + 1)
                            mlMultExpression = r.expression
                            mlMult = if (r.expression != null) 1 else r.value
                            i += r.skip
                        }
                        "Phys:" -> {
                            val r = readNumOrExpr(tokens, i + 1)
                            physicalResistance = r.value
                            physicalResistanceExpression = r.expression
                            i += r.skip
                        }
                        "Elem:" -> {
                            val r = readNumOrExpr(tokens, i + 1)
                            elementalResistance = r.value
                            elementalResistanceExpression = r.expression
                            i += r.skip
                        }
                        "ElemHot:" -> {
                            val r = readNumOrExpr(tokens, i + 1)
                            hotResistance = r.value
                            hotResistanceExpression = r.expression
                            i += r.skip
                        }
                        "ElemCold:" -> {
                            val r = readNumOrExpr(tokens, i + 1)
                            coldResistance = r.value
                            coldResistanceExpression = r.expression
                            i += r.skip
                        }
                        "ElemStench:" -> {
                            val r = readNumOrExpr(tokens, i + 1)
                            stenchResistance = r.value
                            stenchResistanceExpression = r.expression
                            i += r.skip
                        }
                        "ElemSpooky:" -> {
                            val r = readNumOrExpr(tokens, i + 1)
                            spookyResistance = r.value
                            spookyResistanceExpression = r.expression
                            i += r.skip
                        }
                        "ElemSleaze:" -> {
                            val r = readNumOrExpr(tokens, i + 1)
                            sleazeResistance = r.value
                            sleazeResistanceExpression = r.expression
                            i += r.skip
                        }
                        "Article:" -> { article = peek; i++ }
                        "BvR:" -> {
                            val team = BlueVsRedTeam.from(peek)
                            if (team != BlueVsRedTeam.UNKNOWN) {
                                blueVsRedTeam = team
                            }
                            i++
                        }
                        "Group:" -> {
                            group = peek.toIntOrNull() ?: 1
                            i++
                        }
                        "Poison:" -> {
                            val poisonName = readQuotedOrSingleValue(tokens, i + 1)
                            poison = PoisonLevels.levelForEffectName(poisonName)
                            i += poisonTokenSkip(tokens, i + 1)
                        }
                        "Manuel:" -> {
                            manuelName = readQuotedOrSingleValue(tokens, i + 1)
                            i += poisonTokenSkip(tokens, i + 1)
                        }
                        "Wiki:" -> {
                            wikiName = readQuotedOrSingleValue(tokens, i + 1)
                            i += poisonTokenSkip(tokens, i + 1)
                        }
                        "SprinkleMin:" -> {
                            val r = readNumOrExpr(tokens, i + 1)
                            minSprinklesExpression = r.expression
                            minSprinkles = if (r.expression != null) 0 else r.value
                            i += r.skip
                        }
                        "SprinkleMax:" -> {
                            val r = readNumOrExpr(tokens, i + 1)
                            maxSprinklesExpression = r.expression
                            maxSprinkles = if (r.expression != null) 0 else r.value
                            i += r.skip
                        }
                        "EA:" -> {
                            val raw = readQuotedOrSingleValue(tokens, i + 1)
                            val elem = raw.lowercase()
                            if (elem in ELEMENT_VALUES) {
                                attackElements.add(elem)
                            }
                            i += poisonTokenSkip(tokens, i + 1)
                        }
                        "ED:" -> {
                            if (peek.startsWith("\"")) {
                                i += poisonTokenSkip(tokens, i + 1)
                            } else {
                                val elem = peek.lowercase()
                                if (defenseElement.isEmpty() && elem in ELEMENT_VALUES) {
                                    defenseElement = elem
                                }
                                i++
                            }
                        }
                        // Manuel: and other unknown key: value pairs — skip the value
                        else -> {
                            when {
                                peek.startsWith("\"") -> i += poisonTokenSkip(tokens, i + 1)
                                peek.startsWith("[") -> {
                                    val (_, skip) = readBracketOrToken(tokens, i + 1)
                                    i += skip
                                }
                                else -> i++
                            }
                        }
                    }
                }
                // Unknown bare tokens — ignore
            }
            i++
        }

        val effectiveCap = when {
            !isScaling -> cap
            sawCap -> cap
            else -> MonsterDefinition.DEFAULT_CAP
        }
        val effectiveFloor = when {
            !isScaling -> floor
            sawFloor -> floor
            else -> MonsterDefinition.DEFAULT_FLOOR
        }

        return ParsedParams(
            attack = attack,
            attackExpression = attackExpression,
            hasAttack = hasAttack,
            defense = defense,
            defenseExpression = defenseExpression,
            hasDefense = hasDefense,
            hp = hp,
            hpExpression = hpExpression,
            hasHp = hasHp,
            initiative = initiative,
            hasInitiative = hasInitiative,
            initiativeExpression = initiativeExpression,
            meatDrop = meatDrop,
            phylum = phylum,
            isBoss = isBoss,
            isGhost = isGhost,
            subTypes = subTypes,
            isLucky = isLucky,
            isScaling = isScaling,
            scale = scale,
            scaleExpression = scaleExpression,
            cap = effectiveCap,
            capExpression = capExpression,
            floor = effectiveFloor,
            floorExpression = floorExpression,
            experience = experience,
            experienceExpression = experienceExpression,
            hasExperience = hasExperience,
            mlMult = mlMult,
            mlMultExpression = mlMultExpression,
            hasMlMult = hasMlMult,
            article = article,
            isCopyable = isCopyable,
            isWishable = isWishable,
            poison = poison,
            group = group,
            manuelName = manuelName,
            wikiName = wikiName,
            attackElements = attackElements,
            attackElement = primaryAttackElement(attackElements),
            defenseElement = defenseElement,
            physicalResistance = physicalResistance,
            physicalResistanceExpression = physicalResistanceExpression,
            elementalResistance = elementalResistance,
            elementalResistanceExpression = elementalResistanceExpression,
            hotResistance = hotResistance,
            hotResistanceExpression = hotResistanceExpression,
            coldResistance = coldResistance,
            coldResistanceExpression = coldResistanceExpression,
            stenchResistance = stenchResistance,
            stenchResistanceExpression = stenchResistanceExpression,
            spookyResistance = spookyResistance,
            spookyResistanceExpression = spookyResistanceExpression,
            sleazeResistance = sleazeResistance,
            sleazeResistanceExpression = sleazeResistanceExpression,
            minSprinkles = minSprinkles,
            minSprinklesExpression = minSprinklesExpression,
            maxSprinkles = maxSprinkles,
            maxSprinklesExpression = maxSprinklesExpression,
            blueVsRedTeam = blueVsRedTeam,
        )
    }

    private data class NumOrExpr(val value: Int, val expression: String?, val skip: Int)

    private fun readNumOrExpr(tokens: List<String>, start: Int): NumOrExpr {
        val (value, skip) = readBracketOrToken(tokens, start)
        return if (value.startsWith("[")) {
            NumOrExpr(0, value.removePrefix("[").removeSuffix("]"), skip)
        } else {
            NumOrExpr(value.toIntOrNull() ?: 0, null, skip)
        }
    }

    /**
     * Read a single token, or a `[...]` value that may span space-split tokens
     * (e.g. `equipped(PARTY HARD T-shirt)`).
     * @return pair of (joined value, token count consumed from [start])
     */
    private fun readBracketOrToken(tokens: List<String>, start: Int): Pair<String, Int> {
        val first = tokens.getOrNull(start) ?: return "" to 0
        if (!first.startsWith("[")) return first to 1
        var depth = first.count { it == '[' } - first.count { it == ']' }
        if (depth <= 0) return first to 1
        val parts = mutableListOf(first)
        var idx = start + 1
        while (idx < tokens.size && depth > 0) {
            val t = tokens[idx]
            parts.add(t)
            depth += t.count { it == '[' } - t.count { it == ']' }
            idx++
        }
        return parts.joinToString(" ") to (idx - start)
    }

    private fun readQuotedOrSingleValue(tokens: List<String>, start: Int): String {
        val first = tokens.getOrNull(start) ?: return ""
        if (!first.startsWith("\"")) return first
        if (first.endsWith("\"") && first.length > 1) {
            return first.trim('"')
        }
        val parts = mutableListOf<String>()
        var idx = start
        while (idx < tokens.size) {
            parts.add(tokens[idx].trim('"'))
            if (tokens[idx].endsWith("\"")) break
            idx++
        }
        return parts.joinToString(" ")
    }

    private fun poisonTokenSkip(tokens: List<String>, start: Int): Int {
        val first = tokens.getOrNull(start) ?: return 1
        if (!first.startsWith("\"")) return 1
        if (first.endsWith("\"") && first.length > 1) return 1
        var end = start
        while (end < tokens.size && !tokens[end].endsWith("\"")) end++
        return end - start + 1
    }

    private fun parseDrop(raw: String): MonsterDrop? {
        val parenIdx = raw.lastIndexOf('(')
        if (parenIdx < 0) return null
        val itemName = raw.substring(0, parenIdx).trim()
        if (itemName.isEmpty()) return null
        val rateStr = raw.substring(parenIdx + 1).trimEnd(')')
        val prefix = if (rateStr.firstOrNull()?.isLetter() == true) rateStr[0] else null
        val rate = (if (prefix != null) rateStr.drop(1) else rateStr).toIntOrNull() ?: 0
        return MonsterDrop(itemName, rate, prefix)
    }

    private fun parse(text: String) {
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            // Skip version-only lines (a bare integer with no tabs)
            if (!line.contains('\t') && line.toIntOrNull() != null) continue

            val parts = line.split('\t')
            if (parts.size < 4) continue

            val name = parts[0]
            val id = parts[1].toIntOrNull() ?: continue
            val images = parts[2].split(',').map { it.trim() }.filter { it.isNotEmpty() }
            val image = images.firstOrNull() ?: ""
            val paramStr = parts[3]

            val p = parseParams(paramStr)

            val drops = (4 until parts.size).mapNotNull { idx ->
                val dropRaw = parts[idx].trim()
                if (dropRaw.isEmpty()) null else parseDrop(dropRaw)
            }

            val monster = MonsterDefinition(
                name = name,
                id = id,
                image = image,
                images = images,
                attack = p.attack,
                attackExpression = p.attackExpression,
                hasAttack = p.hasAttack,
                defense = p.defense,
                defenseExpression = p.defenseExpression,
                hasDefense = p.hasDefense,
                hp = p.hp,
                hpExpression = p.hpExpression,
                hasHp = p.hasHp,
                initiative = p.initiative,
                hasInitiative = p.hasInitiative,
                initiativeExpression = p.initiativeExpression,
                meatDrop = p.meatDrop,
                phylum = p.phylum,
                isBoss = p.isBoss,
                isGhost = p.isGhost,
                subTypes = p.subTypes,
                isLucky = p.isLucky,
                isScaling = p.isScaling,
                scale = p.scale,
                scaleExpression = p.scaleExpression,
                cap = p.cap,
                capExpression = p.capExpression,
                floor = p.floor,
                floorExpression = p.floorExpression,
                experience = p.experience,
                experienceExpression = p.experienceExpression,
                hasExperience = p.hasExperience,
                mlMult = p.mlMult,
                mlMultExpression = p.mlMultExpression,
                hasMlMult = p.hasMlMult,
                article = p.article,
                isCopyable = p.isCopyable,
                isWishable = p.isWishable,
                poison = p.poison,
                group = p.group,
                manuelName = p.manuelName,
                wikiName = p.wikiName,
                attackElements = p.attackElements,
                attackElement = p.attackElement,
                defenseElement = p.defenseElement,
                physicalResistance = p.physicalResistance,
                physicalResistanceExpression = p.physicalResistanceExpression,
                elementalResistance = p.elementalResistance,
                elementalResistanceExpression = p.elementalResistanceExpression,
                hotResistance = p.hotResistance,
                hotResistanceExpression = p.hotResistanceExpression,
                coldResistance = p.coldResistance,
                coldResistanceExpression = p.coldResistanceExpression,
                stenchResistance = p.stenchResistance,
                stenchResistanceExpression = p.stenchResistanceExpression,
                spookyResistance = p.spookyResistance,
                spookyResistanceExpression = p.spookyResistanceExpression,
                sleazeResistance = p.sleazeResistance,
                sleazeResistanceExpression = p.sleazeResistanceExpression,
                minSprinkles = p.minSprinkles,
                minSprinklesExpression = p.minSprinklesExpression,
                maxSprinkles = p.maxSprinkles,
                maxSprinklesExpression = p.maxSprinklesExpression,
                blueVsRedTeam = p.blueVsRedTeam,
                beeCount = computeBeeCount(name, id),
                attributes = paramStr,
                randomModifiers = emptyList(),
                drops = drops,
            )
            _byId[id] = monster
            _byName[name.lowercase()] = monster
            _byLeetName[leetify(name)] = monster
        }
    }
}
