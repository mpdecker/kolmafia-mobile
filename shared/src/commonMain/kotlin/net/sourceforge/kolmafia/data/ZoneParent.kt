package net.sourceforge.kolmafia.data

data class ZoneParent(
    val name: String,
    val parent: String,
    val description: String,
    val requirement: String
) {
    val isTopLevel get() = name == parent
}
