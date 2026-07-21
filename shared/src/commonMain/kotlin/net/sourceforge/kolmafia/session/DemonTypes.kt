package net.sourceforge.kolmafia.session

/** Desktop [KoLAdventure.DEMON_TYPES] — location and buff effect labels per demon slot. */
object DemonTypes {
    const val BLACK_CANDLE = 620
    const val EVIL_SCROLL = 1960
    const val SUMMONING_CHOICE = 922

    /** Pair of optional location hint and effect/buff name (index + 1 = demon number). */
    val ENTRIES: List<Pair<String?, String>> = listOf(
        "Summoning Chamber" to "Pies",
        "Spooky Forest" to "Preternatural Greed",
        "Sonofa Beach" to "Fit To Be Tide",
        "Deep Fat Friars' Gate" to "Big Flaming Whip",
        "Haunted Bathroom" to "Demonic Taint",
        null to "pile of smoking rags",
        null to "Drinks",
        "Nemesis' Lair" to "Existential Torment",
        "Sinister Ancient Tablet" to "Burning, Man",
        "Strange Cube" to "The Pleasures of the Flesh",
        "Battlefield" to "Infernal Thirst",
        null to "Jacked In",
        null to "Yeg's Power",
        null to "Demon in Combat",
    )

    fun demonNameKey(number: Int): String = "demonName$number"

    const val DEMON_COUNT = 14
}
