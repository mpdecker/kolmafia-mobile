package net.sourceforge.kolmafia.quest

/** Desktop [IslandManager] battlefield victory message tables. */
object IslandWarBattlefieldMessages {

    val HIPPY_MESSAGES: List<List<String>> = listOf(
        listOf("M.C. Escher", "protesting the war", "out of commission", "pacifist beliefs", "garotting", "soapy water", "his own nasty breath", "smoke and dust", "sugar water", "seems to be enjoying it", "astral projection", "knock another hippy into the void", "blasting a nearby hippy"),
        listOf("three hippies", "vodka and pain", "cheap aftershave", "lob a sake bomb"),
        listOf("skunky beer", "brushing burning coals", "subsequent exhale", "hot kabobs"),
        listOf("naked human body", "personal airship", "slightly envious", "guess the outcome"),
        listOf("Absolutely nothing", "glass sculpture", "one more six-pack"),
        listOf("planks of wood", "SWAT vans", "wooden barrel"),
    )

    val FRAT_MESSAGES: List<List<String>> = listOf(
        listOf("karmic retribution", "herbal brownies", "homeopathic healing", "much lighter fluid", "meaty goodness", "Ultimate Frisbee", "alcohol poisoning", "entranced by the sun", "Marxist spell", "smash the can", "bulb of garlic", "knock another frat boy into the void", "blasting a nearby frat boy"),
        listOf("three attacking", "three frat boys", "three-way", "stock market crash"),
        listOf("gopher hole", "runaway Mobile Sweat Lodge", "red-hot coals.", "purple squirrels"),
        listOf("platoon of", "funk of hippies"),
        listOf("regiment"),
        listOf("scream like schoolgirls", "en masse", "mobile homes"),
    )

    fun battlefieldDelta(responseText: String, messages: List<List<String>>): Int {
        var test = 2
        for (tier in messages) {
            if (tier.any { responseText.contains(it) }) {
                return test
            }
            test *= 2
        }
        return 1
    }
}
