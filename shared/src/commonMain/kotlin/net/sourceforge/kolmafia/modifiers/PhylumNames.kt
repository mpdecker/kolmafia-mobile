package net.sourceforge.kolmafia.modifiers

/**
 * Monster phylum catalog. Mirrors desktop [MonsterDatabase.Phylum].
 */
object PhylumNames {

    data class Phylum(val name: String, val plural: String)

    private val PHYLA = listOf(
        Phylum("beast", "beasts"),
        Phylum("bug", "bugs"),
        Phylum("constellation", "constellations"),
        Phylum("construct", "constructs"),
        Phylum("demon", "demons"),
        Phylum("dude", "dudes"),
        Phylum("elemental", "elementals"),
        Phylum("elf", "elves"),
        Phylum("fish", "fishies"),
        Phylum("goblin", "goblins"),
        Phylum("hippy", "hippys"),
        Phylum("hobo", "hobos"),
        Phylum("horror", "horrors"),
        Phylum("humanoid", "humanoids"),
        Phylum("mer-kin", "merkins"),
        Phylum("orc", "orcs"),
        Phylum("penguin", "penguins"),
        Phylum("pirate", "pirates"),
        Phylum("plant", "plants"),
        Phylum("slime", "slimes"),
        Phylum("undead", "the undead"),
        Phylum("weird", "weirds"),
    )

    private val BY_NAME = PHYLA.associateBy { normalize(it.name) }

    private fun normalize(name: String): String = name.trim().lowercase().replace("-", "")

    fun resolve(name: String): String? {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || trimmed.equals("none", ignoreCase = true)) return null

        val normalized = normalize(trimmed)
        BY_NAME[normalized]?.let { return it.name }

        PHYLA.firstOrNull {
            trimmed.equals(it.name, ignoreCase = true) ||
                trimmed.equals(it.plural, ignoreCase = true)
        }?.let { return it.name }

        return null
    }

    fun isValid(name: String): Boolean = resolve(name) != null
}
