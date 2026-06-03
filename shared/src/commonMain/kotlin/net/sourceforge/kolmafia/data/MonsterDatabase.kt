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
        val defense: Int = 0,
        val hp: Int = 0,
        val initiative: Int = 0,
        val meatDrop: Int = 0,
        val phylum: String = "",
        val isBoss: Boolean = false,
        val isGhost: Boolean = false,
        val isLucky: Boolean = false,
        val isScaling: Boolean = false,
        val scale: Int = 0,
        val cap: Int = 0,
        val floor: Int = 0,
    )

    private fun parseParams(params: String): ParsedParams {
        val tokens = params.split(' ').filter { it.isNotEmpty() }
        var attack = 0
        var defense = 0
        var hp = 0
        var initiative = 0
        var meatDrop = 0
        var phylum = ""
        var isBoss = false
        var isGhost = false
        var isLucky = false
        var isScaling = false
        var scale = 0
        var cap = 0
        var floor = 0

        var i = 0
        while (i < tokens.size) {
            val token = tokens[i]
            when {
                token == "BOSS" -> isBoss = true
                token == "GHOST" -> isGhost = true
                token == "LUCKY" -> isLucky = true
                // Other flags: NOWISH, NOCOPY, ULTRARARE — parsed but not stored in MonsterDefinition
                token == "NOWISH" || token == "NOCOPY" || token == "ULTRARARE" -> { /* ignored */ }
                token.endsWith(':') -> {
                    val value = tokens.getOrNull(i + 1) ?: ""
                    when (token) {
                        "Atk:" -> { attack = value.toIntOrNull() ?: 0; i++ }
                        "Def:" -> { defense = value.toIntOrNull() ?: 0; i++ }
                        "HP:" -> { hp = value.toIntOrNull() ?: 0; i++ }
                        "Init:" -> { initiative = value.toIntOrNull() ?: 0; i++ }
                        "Meat:" -> { meatDrop = value.toIntOrNull() ?: 0; i++ }
                        "P:" -> { phylum = value; i++ }
                        "Scale:" -> { scale = value.toIntOrNull() ?: 0; isScaling = true; i++ }
                        "Cap:" -> { cap = value.toIntOrNull() ?: 0; i++ }
                        "Floor:" -> { floor = value.toIntOrNull() ?: 0; i++ }
                        // EA:, Article:, and other unknown key: value pairs — skip the value
                        else -> { i++ }
                    }
                }
                // Unknown bare tokens — ignore
            }
            i++
        }

        return ParsedParams(
            attack = attack,
            defense = defense,
            hp = hp,
            initiative = initiative,
            meatDrop = meatDrop,
            phylum = phylum,
            isBoss = isBoss,
            isGhost = isGhost,
            isLucky = isLucky,
            isScaling = isScaling,
            scale = scale,
            cap = cap,
            floor = floor,
        )
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
                defense = p.defense,
                hp = p.hp,
                initiative = p.initiative,
                meatDrop = p.meatDrop,
                phylum = p.phylum,
                isBoss = p.isBoss,
                isGhost = p.isGhost,
                isLucky = p.isLucky,
                isScaling = p.isScaling,
                scale = p.scale,
                cap = p.cap,
                floor = p.floor,
                drops = drops,
            )
            _byId[id] = monster
            _byName[name.lowercase()] = monster
        }
    }
}
