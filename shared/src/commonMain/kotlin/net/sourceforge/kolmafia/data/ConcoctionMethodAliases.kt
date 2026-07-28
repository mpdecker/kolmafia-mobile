package net.sourceforge.kolmafia.data

/** Desktop ConcoctionDatabase.addCraftingData compound token expansion. */
object ConcoctionMethodAliases {

    val LEGACY_BLOCKED = setOf(
        "CRIMBO05", "CRIMBO06", "CRIMBO07", "CRIMBO12",
    )

    fun normalize(methods: Set<String>): Set<String> {
        val result = methods.toMutableSet()
        for (token in methods) {
            when (token) {
                "TINKER" -> {
                    result.remove("TINKER")
                    result.add("GNOME_TINKER")
                }
                "WSMITH" -> {
                    result.remove("WSMITH")
                    result.add("SSMITH")
                    result.add("WEAPON")
                }
                "ASMITH" -> {
                    result.remove("ASMITH")
                    result.add("SSMITH")
                    result.add("ARMOR")
                }
                "SAUCE" -> {
                    result.remove("SAUCE")
                    result.add("COOK_FANCY")
                    result.add("REAGENT")
                }
                "SSAUCE" -> {
                    result.remove("SSAUCE")
                    result.add("COOK_FANCY")
                    result.add("WAY")
                }
                "DSAUCE" -> {
                    result.remove("DSAUCE")
                    result.add("COOK_FANCY")
                    result.add("DEEP")
                }
                "ACOCK" -> {
                    result.remove("ACOCK")
                    result.add("MIX_FANCY")
                    result.add("AC")
                }
                "SCOCK" -> {
                    result.remove("SCOCK")
                    result.add("MIX_FANCY")
                    result.add("SHC")
                }
                "SACOCK" -> {
                    result.remove("SACOCK")
                    result.add("MIX_FANCY")
                    result.add("SALACIOUS")
                }
                "PASTA" -> {
                    result.remove("PASTA")
                    result.add("COOK_FANCY")
                    result.add("PASTAMASTERY")
                }
                "PASTAMASTERY" -> {
                    result.remove("PASTAMASTERY")
                    result.add("COOK")
                    result.add("PASTAMASTERY")
                }
                "TNOODLE", "TRANSNOODLE" -> {
                    result.remove(token)
                    result.add("COOK_FANCY")
                    result.add("TRANSNOODLE")
                }
                "TEMPURA" -> {
                    result.remove("TEMPURA")
                    result.add("COOK_FANCY")
                    result.add("TEMPURAMANCY")
                }
                "EJEWEL" -> {
                    result.remove("EJEWEL")
                    result.add("JEWELRY")
                    result.add("EXPENSIVE")
                }
            }
        }
        return result
    }
}
