package net.sourceforge.kolmafia.data

data class FoldGroup(
    val hpDamagePct: Int,       // % of max HP taken as damage when folding
    val items: List<String>     // ordered: each item folds into the next
)
