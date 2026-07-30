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

    private val IMAGE_BY_NAME = mapOf(
        "beast" to "beastflavor.gif",
        "bug" to "stinkbug.gif",
        "constellation" to "star.gif",
        "construct" to "sprocket.gif",
        "demon" to "demonflavor.gif",
        "dude" to "happy.gif",
        "elemental" to "rrainbow.gif",
        "elf" to "elfflavor.gif",
        "fish" to "fish.gif",
        "goblin" to "goblinflavor.gif",
        "hippy" to "hippyflavor.gif",
        "hobo" to "hoboflavor.gif",
        "horror" to "skull.gif",
        "humanoid" to "statue.gif",
        "merkin" to "merkinflavor.gif",
        "orc" to "frattyflavor.gif",
        "penguin" to "bowtie.gif",
        "pirate" to "pirateflavor.gif",
        "plant" to "leafflavor.gif",
        "slime" to "sebashield.gif",
        "undead" to "spookyflavor.gif",
        "weird" to "weirdflavor.gif",
    )

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

    fun getImage(name: String): String {
        val resolved = resolve(name) ?: return ""
        return IMAGE_BY_NAME[normalize(resolved)] ?: ""
    }
}
