package net.sourceforge.kolmafia.data

data class RestoreData(
    val name: String,
    val type: RestoreType,
    val hpMinExpr: String,     // may be a number "50" or expression "[HP]"
    val hpMaxExpr: String,
    val mpMinExpr: String,
    val mpMaxExpr: String,
    val advCost: Int,
    val usesLeftExpr: String,  // "" if unlimited
    val notes: String
) {
    val restoresHp get() = hpMinExpr != "0" || hpMaxExpr != "0"
    val restoresMp get() = mpMinExpr != "0" || mpMaxExpr != "0"
    // Simple numeric accessors (return 0 for expressions)
    val hpMin get() = hpMinExpr.toIntOrNull() ?: 0
    val hpMax get() = hpMaxExpr.toIntOrNull() ?: 0
    val mpMin get() = mpMinExpr.toIntOrNull() ?: 0
    val mpMax get() = mpMaxExpr.toIntOrNull() ?: 0
}
