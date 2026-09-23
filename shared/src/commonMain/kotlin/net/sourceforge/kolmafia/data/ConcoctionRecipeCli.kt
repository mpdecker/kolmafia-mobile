package net.sourceforge.kolmafia.data

/**
 * Desktop [RecipeCommand] headless recipe / ingredients formatting.
 * HTML is omitted; yield and flatten match desktop counts.
 */
object ConcoctionRecipeCli {
    fun formatYieldName(concoction: ConcoctionData, tripleReagent: Boolean): String {
        val yield = ConcoctionYield.getYield(concoction, tripleReagent)
        return if (yield > 1) "$yield ${concoction.result}" else concoction.result
    }

    fun formatRecipe(
        concoction: ConcoctionData,
        tripleReagent: Boolean,
        depth: Int = 0,
        visiting: MutableSet<String> = mutableSetOf(),
    ): String {
        val indent = if (depth > 0) "\n" + "   ".repeat(depth) else ""
        val name = formatYieldName(concoction, tripleReagent)
        val type = concoction.craftTypeDescription()
        val parts = concoction.ingredients.joinToString(" + ") { "${it.quantity} ${it.name}" }
        val line = if (concoction.ingredients.isEmpty()) {
            "$indent$name ($type)"
        } else {
            "$indent$name ($type): $parts"
        }
        val key = concoction.result.lowercase()
        if (key in visiting) return line
        visiting.add(key)
        val nested = concoction.ingredients.mapNotNull { ingredient ->
            if (isRecursing(concoction, ingredient)) return@mapNotNull null
            val child = ConcoctionDatabase.getByResult(ingredient.name) ?: return@mapNotNull null
            formatRecipe(child, tripleReagent, depth + 1, visiting)
        }
        visiting.remove(key)
        return line + nested.joinToString("")
    }

    fun formatIngredients(
        concoction: ConcoctionData,
        haveCount: (String) -> Int,
        tripleReagent: Boolean,
    ): String {
        val header = formatYieldName(concoction, tripleReagent)
        val flat = flattenIngredients(concoction, haveCount)
        if (flat.isEmpty()) return "$header:"
        val parts = flat.joinToString(", ") { ingredient ->
            val have = haveCount(ingredient.name)
            val need = ingredient.quantity
            val missing = (need - have).coerceAtLeast(0)
            if (missing > 0) {
                "${ingredient.name} ($have/$need)"
            } else {
                "$need ${ingredient.name}"
            }
        }
        return "$header: $parts"
    }

    fun flattenIngredients(
        concoction: ConcoctionData,
        haveCount: (String) -> Int,
        deep: Boolean = false,
        visiting: MutableSet<String> = mutableSetOf(),
    ): List<ConcoctionIngredient> {
        val out = mutableListOf<ConcoctionIngredient>()
        flattenInto(concoction, haveCount, deep, visiting, out)
        return out
    }

    private fun flattenInto(
        concoction: ConcoctionData,
        haveCount: (String) -> Int,
        deep: Boolean,
        visiting: MutableSet<String>,
        out: MutableList<ConcoctionIngredient>,
    ) {
        val key = concoction.result.lowercase()
        if (key in visiting) return
        visiting.add(key)
        for (ingredient in concoction.ingredients) {
            val child = ConcoctionDatabase.getByResult(ingredient.name)
            val have = haveCount(ingredient.name)
            if (child != null &&
                !isRecursing(concoction, ingredient) &&
                (deep || have == 0)
            ) {
                flattenInto(child, haveCount, deep, visiting, out)
            } else {
                val existing = out.indexOfFirst { it.name.equals(ingredient.name, ignoreCase = true) }
                if (existing >= 0) {
                    out[existing] = out[existing].copy(quantity = out[existing].quantity + ingredient.quantity)
                } else {
                    out += ingredient
                }
            }
        }
        visiting.remove(key)
    }

    private fun isRecursing(parent: ConcoctionData, child: ConcoctionIngredient): Boolean {
        if (parent.result.equals(child.name, ignoreCase = true)) return true
        if ("ROLL" in parent.methods || "ROLLING_PIN" in parent.methods) return true
        val childRecipe = ConcoctionDatabase.getByResult(child.name) ?: return false
        return childRecipe.ingredients.any { it.name.equals(parent.result, ignoreCase = true) }
    }
}
