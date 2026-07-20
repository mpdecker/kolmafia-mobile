package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

// Parses monsters.txt from the bundled compose resources.
// Format (tab-separated): name  id  image  parameters  [drop1  drop2  ...]
// Parameters is a space-separated list of Key: value pairs and flags.
// Call load() once at app startup (or lazily on first access).
@OptIn(ExperimentalResourceApi::class)
object MonsterDatabase {

    private val _byId = mutableMapOf<Int, MonsterDefinition>()
    private val _byName = mutableMapOf<String, MonsterDefinition>()
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
        val isLucky: Boolean = false,
        val isScaling: Boolean = false,
        val scale: Int = 0,
        val cap: Int = 0,
        val floor: Int = 0,
        val article: String = "",
        val isCopyable: Boolean = true,
        val isWishable: Boolean = true,
        val poison: Int = Int.MAX_VALUE,
        val attackElement: String = "",
        val defenseElement: String = "",
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
        var isLucky = false
        var isScaling = false
        var scale = 0
        var cap = 0
        var floor = 0
        var sawCap = false
        var sawFloor = false
        var article = ""
        var isCopyable = true
        var isWishable = true
        var poison = Int.MAX_VALUE
        var attackElement = ""
        var defenseElement = ""

        var i = 0
        while (i < tokens.size) {
            val token = tokens[i]
            when {
                token == "BOSS" -> isBoss = true
                token == "GHOST" -> isGhost = true
                token == "LUCKY" -> isLucky = true
                token == "NOCOPY" -> isCopyable = false
                token == "NOWISH" -> isWishable = false
                token == "ULTRARARE" -> { /* ignored */ }
                token.endsWith(':') -> {
                    val value = tokens.getOrNull(i + 1) ?: ""
                    when (token) {
                        "Atk:" -> {
                            hasAttack = true
                            if (value.startsWith("[")) {
                                attackExpression = value.removePrefix("[").removeSuffix("]")
                                attack = 0
                            } else {
                                attackExpression = null
                                attack = value.toIntOrNull() ?: 0
                            }
                            i++
                        }
                        "Def:" -> {
                            hasDefense = true
                            if (value.startsWith("[")) {
                                defenseExpression = value.removePrefix("[").removeSuffix("]")
                                defense = 0
                            } else {
                                defenseExpression = null
                                defense = value.toIntOrNull() ?: 0
                            }
                            i++
                        }
                        "HP:" -> {
                            hasHp = true
                            if (value.startsWith("[")) {
                                hpExpression = value.removePrefix("[").removeSuffix("]")
                                hp = 0
                            } else {
                                hpExpression = null
                                hp = value.toIntOrNull() ?: 0
                            }
                            i++
                        }
                        "Init:" -> {
                            hasInitiative = true
                            if (value.startsWith("[")) {
                                initiativeExpression = value.removePrefix("[").removeSuffix("]")
                                initiative = 0
                            } else {
                                initiativeExpression = null
                                initiative = value.toIntOrNull() ?: 0
                            }
                            i++
                        }
                        "Meat:" -> { meatDrop = value.toIntOrNull() ?: 0; i++ }
                        "P:" -> { phylum = value; i++ }
                        "Scale:" -> {
                            isScaling = true
                            // Expression Scale deferred — leave numeric 0
                            scale = if (value.startsWith("[")) 0 else (value.toIntOrNull() ?: 0)
                            i++
                        }
                        "Cap:" -> {
                            sawCap = true
                            cap = when {
                                value == "?" -> MonsterDefinition.DEFAULT_CAP
                                else -> value.toIntOrNull() ?: MonsterDefinition.DEFAULT_CAP
                            }
                            i++
                        }
                        "Floor:" -> {
                            sawFloor = true
                            floor = when {
                                value == "?" -> MonsterDefinition.DEFAULT_FLOOR
                                else -> value.toIntOrNull() ?: MonsterDefinition.DEFAULT_FLOOR
                            }
                            i++
                        }
                        "Article:" -> { article = value; i++ }
                        "Poison:" -> {
                            val poisonName = readQuotedOrSingleValue(tokens, i + 1)
                            poison = PoisonLevels.levelForEffectName(poisonName)
                            i += poisonTokenSkip(tokens, i + 1)
                        }
                        "EA:" -> {
                            if (value.startsWith("\"")) {
                                i += poisonTokenSkip(tokens, i + 1)
                            } else {
                                val elem = value.lowercase()
                                if (attackElement.isEmpty() && elem in ELEMENT_VALUES) {
                                    attackElement = elem
                                }
                                i++
                            }
                        }
                        "ED:" -> {
                            if (value.startsWith("\"")) {
                                i += poisonTokenSkip(tokens, i + 1)
                            } else {
                                val elem = value.lowercase()
                                if (defenseElement.isEmpty() && elem in ELEMENT_VALUES) {
                                    defenseElement = elem
                                }
                                i++
                            }
                        }
                        // Manuel: and other unknown key: value pairs — skip the value
                        else -> {
                            if (value.startsWith("\"")) {
                                i += poisonTokenSkip(tokens, i + 1)
                            } else {
                                i++
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
            isLucky = isLucky,
            isScaling = isScaling,
            scale = scale,
            cap = effectiveCap,
            floor = effectiveFloor,
            article = article,
            isCopyable = isCopyable,
            isWishable = isWishable,
            poison = poison,
            attackElement = attackElement,
            defenseElement = defenseElement,
        )
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
            val image = parts[2]
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
                isLucky = p.isLucky,
                isScaling = p.isScaling,
                scale = p.scale,
                cap = p.cap,
                floor = p.floor,
                article = p.article,
                isCopyable = p.isCopyable,
                isWishable = p.isWishable,
                poison = p.poison,
                attackElement = p.attackElement,
                defenseElement = p.defenseElement,
                drops = drops,
            )
            _byId[id] = monster
            _byName[name.lowercase()] = monster
        }
    }

    private val ELEMENT_VALUES = setOf(
        "hot", "cold", "spooky", "stench", "sleaze", "slime", "supercold",
    )
}
