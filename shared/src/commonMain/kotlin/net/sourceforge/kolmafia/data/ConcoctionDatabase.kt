package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

// Parses concoctions.txt from the bundled compose resources.
// Format (tab-separated): result  method  ingredient1  [ingredient2  ...]
// result and ingredients may carry a quantity suffix like "item name (3)".
// method is a comma-separated list of creation method strings.
// Call load() once at app startup (or lazily on first access).
@OptIn(ExperimentalResourceApi::class)
object ConcoctionDatabase {

    private val _byResult = mutableMapOf<String, ConcoctionData>()
    private val _byIngredient = mutableMapOf<String, MutableList<ConcoctionData>>()
    private var loaded = false

    val byResult: Map<String, ConcoctionData> get() = _byResult
    val byIngredient: Map<String, List<ConcoctionData>> get() = _byIngredient

    suspend fun load() {
        if (loaded) return
        val text = Res.readBytes("files/data/concoctions.txt").decodeToString()
        parse(text)
        loaded = true
    }

    fun getByResult(name: String): ConcoctionData? = _byResult[name.lowercase()]
    fun getByIngredient(name: String): List<ConcoctionData> =
        _byIngredient[name.lowercase()] ?: emptyList()
    fun all(): Collection<ConcoctionData> = _byResult.values
    fun cooking(): List<ConcoctionData> = _byResult.values.filter { it.isCooking }
    fun mixing(): List<ConcoctionData> = _byResult.values.filter { it.isMixing }
    fun smithing(): List<ConcoctionData> = _byResult.values.filter { it.isSmithing }

    // Parses a field that may carry an optional quantity suffix: "item name (3)" → Pair("item name", 3)
    // Fields without a parenthesised integer suffix have quantity 1.
    private fun parseNameWithQuantity(raw: String): Pair<String, Int> {
        val parenIdx = raw.lastIndexOf('(')
        if (parenIdx >= 0) {
            val inside = raw.substring(parenIdx + 1).trimEnd(')')
            val qty = inside.toIntOrNull()
            if (qty != null) {
                val name = raw.substring(0, parenIdx).trim()
                return Pair(name, qty)
            }
        }
        return Pair(raw.trim(), 1)
    }

    private fun parse(text: String) {
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            // Skip version-only lines (a bare integer with no tabs)
            if (!line.contains('\t') && line.toIntOrNull() != null) continue

            val parts = line.split('\t')
            if (parts.size < 2) continue

            val (resultName, resultQty) = parseNameWithQuantity(parts[0])
            val methods = parts[1].split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()

            val ingredients = (2 until parts.size).mapNotNull { idx ->
                val raw2 = parts[idx].trim()
                if (raw2.isEmpty()) null
                else {
                    val (ingName, ingQty) = parseNameWithQuantity(raw2)
                    ConcoctionIngredient(ingName, ingQty)
                }
            }

            val concoction = ConcoctionData(
                result = resultName,
                resultQuantity = resultQty,
                methods = methods,
                ingredients = ingredients,
            )

            _byResult[resultName.lowercase()] = concoction
            for (ingredient in ingredients) {
                _byIngredient
                    .getOrPut(ingredient.name.lowercase()) { mutableListOf() }
                    .add(concoction)
            }
        }
    }
}
