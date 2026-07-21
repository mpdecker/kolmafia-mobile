package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [DemonNamesCommand] — list known demon names and solve demon 14. */
class DemonNamesManager(
    private val preferences: Preferences,
    private val segmentSync: DemonInCombatNameSync,
) {

    fun listDemons(print: (String) -> Unit) {
        for (i in DemonTypes.ENTRIES.indices) {
            val number = i + 1
            val name = preferences.getString(DemonTypes.demonNameKey(number), "")
            print("$number: $name")
            val (location, effect) = DemonTypes.ENTRIES[i]
            if (location != null) {
                print(" => Found in the $location")
            }
            print(" => Gives $effect")
        }
    }

    fun solve14(print: (String) -> Unit) {
        val prefValue = preferences.getString(Preferences.DEMON_NAME_14_SEGMENTS, "")
        if (prefValue.isEmpty()) {
            print("You need to make bad requests with your Allied Radio Backpack to find segments of your demon name")
            return
        }

        val segments = segmentSync.parseSegmentsPref(prefValue).keys
        print("Attempting to solve your demon name with ${segments.size} segment(s). This may take a while...")

        val solutions = DemonName14Manager.solve(segments).sorted()
        if (solutions.isEmpty()) {
            print("Sorry, you do not have enough segments to solve your demon name.")
            return
        }

        if (segments.size < 10) {
            print("Unless you have a really unfortunate demon name, you might want to try to find more segments before solving it. The solution set may be artificially small.")
            print("")
        }

        print("${solutions.size} solution(s) found:")
        solutions.forEachIndexed { index, solution ->
            print("${index + 1}: $solution")
        }
        print("")
        print("Done! If none of these are correct, try finding more segments.")
    }
}
