package net.sourceforge.kolmafia.data

/** Mirror desktop [MonsterDatabase.Element] enum declaration order. */
private val ELEMENT_ENUM_ORDER = listOf(
    "none",
    "cold",
    "hot",
    "sleaze",
    "spooky",
    "stench",
    "slime",
    "supercold",
    "bad spelling",
    "shadow",
)

val ELEMENT_VALUES: Set<String> = ELEMENT_ENUM_ORDER.toSet()

fun canonicalElementOrder(elements: List<String>): List<String> =
    elements.filter { it in ELEMENT_VALUES }.sortedBy { ELEMENT_ENUM_ORDER.indexOf(it) }

/** Desktop getAttackElement(): last element in EnumSet iteration order. */
fun primaryAttackElement(elements: List<String>): String =
    canonicalElementOrder(elements).lastOrNull() ?: ""
