package net.sourceforge.kolmafia.familiar

data class FamiliarState(
    val activeFamiliar: FamiliarData? = null,
    val ownedFamiliars: List<FamiliarData> = emptyList(),
    val isStale: Boolean = false
)
