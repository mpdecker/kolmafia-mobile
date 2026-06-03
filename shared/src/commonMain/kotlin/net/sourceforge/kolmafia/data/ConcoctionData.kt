package net.sourceforge.kolmafia.data

data class ConcoctionData(
    val result: String,
    val resultQuantity: Int,       // usually 1; SUSE items may produce multiple
    val methods: Set<String>,      // COMBINE, COOK, MIX, SMITH, etc.
    val ingredients: List<ConcoctionIngredient>
) {
    val isCooking get() = methods.any { it.contains("COOK") || it.contains("PASTA") || it.contains("SAUCE") }
    val isMixing get() = methods.any { it.contains("MIX") || it.contains("COCK") || it.contains("STILL") }
    val isSmithing get() = methods.any { it.contains("SMITH") }
    val isCombining get() = "COMBINE" in methods
    val isSingleUse get() = "SUSE" in methods   // use one item to get another
}
