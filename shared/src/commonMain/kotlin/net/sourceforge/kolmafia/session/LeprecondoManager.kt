package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [LeprecondoManager] — furniture/need catalog, discovery prefs,
 * rearrangement visit parse, familiar combat messages (Phases 3366–3380).
 */
object LeprecondoManager {

    enum class Need(val label: String) {
        MENTAL_STIMULATION("mental stimulation"),
        EXERCISE("exercise"),
        DUMB_ENTERTAINMENT("dumb entertainment"),
        FOOD("food"),
        BOOZE("booze"),
        SLEEP("sleep"),
    }

    enum class Furniture(
        val displayName: String,
        val id: Int,
        val needs: Map<Need, String>,
        val location: String,
    ) {
        CONCRETE("buckets of concrete", 1, mapOf(Need.EXERCISE to "spends a few minutes swinging around his buckets of concrete"), ""),
        PAINTING("thrift store oil painting", 2, mapOf(Need.MENTAL_STIMULATION to "wanders over and spends some time studying his painting"), ""),
        COMICS("boxes of old comic books", 3, mapOf(Need.DUMB_ENTERTAINMENT to "plops down and reads some old comics"), ""),
        HOT_PLATE("second-hand hot plate", 4, mapOf(Need.FOOD to "fires up his second-hand hot plate"), ""),
        BEER_COOLER("beer cooler", 5, mapOf(Need.BOOZE to "grabs a beer from the beer cooler"), ""),
        MATTRESS("free mattress", 6, mapOf(Need.SLEEP to "heads to his filthy mattress for a nap"), ""),
        CHESS_SET(
            "gigantic chess set", 7,
            mapOf(
                Need.EXERCISE to "spends a few minutes laboriously resetting his big heavy chessboard",
                Need.MENTAL_STIMULATION to "plays a game of chess against himself on his giant board",
            ),
            "An Octopus's Garden",
        ),
        KARAOKE(
            "UltraDance karaoke machine", 8,
            mapOf(
                Need.DUMB_ENTERTAINMENT to "wanders over to the karaoke machines and sings a few popular songs",
                Need.EXERCISE to "sets his karaoke machine to dance mode and careens around the room",
            ),
            "Infernal Rackets Backstage",
        ),
        TREADMILL(
            "cupcake treadmill", 9,
            mapOf(
                Need.EXERCISE to "spends a while running on his cupcake treadmill",
                Need.FOOD to "grabs a cupcake from his treadmill, and tosses you one as well",
            ),
            "Madness Bakery",
        ),
        BEER_PONG(
            "beer pong table", 10,
            mapOf(
                Need.BOOZE to "plays beer pong for a while, but stops drinking before he finishes the whole table's worth",
                Need.EXERCISE to "plays beer pong until he gets tired",
            ),
            "The Orcish Frat House",
        ),
        WEIGHT_BENCH(
            "padded weight bench", 11,
            mapOf(
                Need.EXERCISE to "does some benchpresses on his nice weight bench",
                Need.SLEEP to "takes a nap on his padded weight bench",
            ),
            "The Degrassi Knoll Garage",
        ),
        LAPTOP(
            "internet-connected laptop", 12,
            mapOf(
                Need.DUMB_ENTERTAINMENT to "spends some time arguing with people about politics on social media",
                Need.MENTAL_STIMULATION to "watches some home improvement videos and takes some notes",
            ),
            "The Hidden Office Building",
        ),
        SOUS_VIDE(
            "sous vide laboratory", 13,
            mapOf(
                Need.FOOD to "whips up a giant meal in the sous vide machine and shares it with you",
                Need.MENTAL_STIMULATION to "reads the manual for his sous vide machine",
            ),
            "The Haunted Kitchen",
        ),
        BLENDER(
            "programmable blender", 14,
            mapOf(
                Need.MENTAL_STIMULATION to "spends a few minutes adjusting the settings on his fancy blender",
                Need.BOOZE to "makes a huge blended cocktail for himself, and one for you",
            ),
            "Cobb's Knob Kitchens",
        ),
        DEPRIVATION_TANK(
            "sensory deprivation tank", 15,
            mapOf(
                Need.MENTAL_STIMULATION to "meditates in his tank for a while",
                Need.SLEEP to "sleeps in his sensory deprivation tank for a long time",
            ),
            "The Marinara Trench",
        ),
        ROBOT(
            "fruit-smashing robot", 16,
            mapOf(
                Need.DUMB_ENTERTAINMENT to "watches his fruit-smashing robot for a while, giggling",
                Need.FOOD to "grabs some of the fruit from his fruit-smashing robot's storage cache",
            ),
            "Wartime Hippy Camp (Frat Disguise)",
        ),
        SPORTS_BAR(
            "ManCave&trade; sports bar set", 17,
            mapOf(
                Need.DUMB_ENTERTAINMENT to "plays darts for a while in his sports bar",
                Need.BOOZE to "has a few beers at the sports bar",
            ),
            "A Barroom Brawl",
        ),
        COUCH_AND_FLATSCREEN(
            "couch and flatscreen", 18,
            mapOf(
                Need.DUMB_ENTERTAINMENT to "sits on his couch and watches some reruns of old reality shows",
                Need.SLEEP to "falls asleep on his comfy couch",
            ),
            "The Orcish Frat House",
        ),
        KEGERATOR(
            "kegerator", 19,
            mapOf(
                Need.FOOD to "grabs some leftovers from the fridge",
                Need.BOOZE to "grabs a homebrew from his kegerator",
            ),
            "The Orcish Frat House (Bombed Back to the Stone Age)",
        ),
        DINING_SET(
            "fine upholstered dining table set", 20,
            mapOf(
                Need.FOOD to "heads to his dinette set for a nice meal, and tosses you a little something for yourself",
                Need.SLEEP to "stretches out on his upholstered dining table and takes a little snooze",
            ),
            "The Hidden Apartment Building",
        ),
        WHISKEYBED(
            "whiskeybed", 21,
            mapOf(
                Need.BOOZE to "extracts some of the whiskey from his bed",
                Need.SLEEP to "takes a long nap in his whiskeybed",
            ),
            "The Castle in the Clouds in the Sky (Ground Floor)",
        ),
        WORKOUT_SYSTEM(
            "high-end home workout system", 22,
            mapOf(Need.EXERCISE to "avails himself of his fancy home gym"),
            "The Degrassi Knoll Gym",
        ),
        CLASSICS_LIBRARY(
            "complete classics library", 23,
            mapOf(Need.MENTAL_STIMULATION to "reads some books about military history"),
            "The Haunted Library",
        ),
        RETRO_CONSOLE(
            "ultimate retro game console", 24,
            mapOf(Need.DUMB_ENTERTAINMENT to "play old video games for a few hours"),
            "Megalo-City",
        ),
        OMNIPOT(
            "Omnipot", 25,
            mapOf(Need.FOOD to "makes a small but delicious meal in his Omnipot"),
            "Cobb's Knob Laboratory",
        ),
        WET_BAR(
            "fully-stocked wet bar", 26,
            mapOf(Need.BOOZE to "whips up a cocktail in his nice bar"),
            "The Purple Light District",
        ),
        POSTER_BED(
            "four-poster bed", 27,
            mapOf(Need.SLEEP to "takes a long nap in his nice bed"),
            "Dreadsylvanian Castle",
        );

        companion object {
            private val DISCOVERY = Regex(
                """spots (?:an?|some) (.*?) and runs out of his condo\.""",
                RegexOption.IGNORE_CASE,
            )

            fun byDiscovery(text: String): Furniture? {
                val match = DISCOVERY.find(text) ?: return null
                val fragment = match.groupValues[1]
                return entries.firstOrNull { fragment.startsWith(it.displayName, ignoreCase = true) }
            }

            fun byId(id: Int): Furniture? = entries.firstOrNull { it.id == id }

            fun byName(name: String): Furniture? =
                entries.firstOrNull { it.displayName.equals(name, ignoreCase = true) }

            fun byLocation(location: String): List<Furniture> =
                entries.filter { it.location.equals(location, ignoreCase = true) }
        }
    }

    private val INSTALLED_FURNITURE = Regex(
        """<img id="i(\d)" alt="(.*?) in (?:top|bottom) (?:left|right)"""",
    )
    private val REARRANGEMENTS = Regex(
        """You can rearrange the furnishings (\d) more""",
        RegexOption.IGNORE_CASE,
    )
    private val DISCOVERY_SELECT = Regex(
        """<select id="r1" name="r1">(.*?)</select>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val DISCOVERY_OPTION = Regex("""<option(?: selected)? value='(\d+)'""", RegexOption.IGNORE_CASE)
    val UNMET_NEED = Regex("""is upset that his (.*?) need wasn't met""", RegexOption.IGNORE_CASE)

    fun handlePostCombatMessage(
        text: String,
        image: String,
        preferences: Preferences?,
        currentRun: Int = 0,
        goalManager: GoalManager? = null,
    ): Boolean {
        if (!image.equals("familiar2.gif", ignoreCase = true) || preferences == null) return false

        Furniture.byDiscovery(text)?.let { discovered ->
            preferences.setInt("_leprecondoFurniture", preferences.getInt("_leprecondoFurniture", 0) + 1)
            val existing = preferences.getString("leprecondoDiscovered", "")
                .split(",")
                .filter { it.isNotBlank() }
                .mapNotNull { it.toIntOrNull() }
                .toMutableSet()
            existing += discovered.id
            preferences.setString(
                "leprecondoDiscovered",
                existing.sorted().joinToString(","),
            )
            goalManager?.noteLeprecondoProgress()
            return true
        }

        UNMET_NEED.find(text)?.groupValues?.getOrNull(1)?.let { need ->
            processNeedChange(need, preferences, currentRun)
            return true
        }

        val metNeed = Furniture.entries
            .flatMap { it.needs.entries }
            .firstOrNull { text.contains(it.value, ignoreCase = true) }
            ?.key
            ?.label
        if (metNeed != null) {
            processNeedChange(metNeed, preferences, currentRun)
            return true
        }
        return false
    }

    fun visit(text: String, preferences: Preferences?) {
        if (preferences == null) return
        val installed = INSTALLED_FURNITURE.findAll(text)
            .map { match ->
                val slot = match.groupValues[1].toIntOrNull() ?: 0
                val furniture = Furniture.byName(match.groupValues[2])
                slot to (furniture?.id ?: 0)
            }
            .sortedBy { it.first }
            .joinToString(",") { it.second.toString() }
        preferences.setString("leprecondoInstalled", installed)

        val rearrangementsLeft = REARRANGEMENTS.find(text)?.groupValues?.getOrNull(1)?.toIntOrNull()
        if (rearrangementsLeft != null) {
            preferences.setInt("_leprecondoRearrangements", 3 - rearrangementsLeft)
            if (rearrangementsLeft > 0) {
                DISCOVERY_SELECT.find(text)?.groupValues?.getOrNull(1)?.let { optionsHtml ->
                    val discoveries = DISCOVERY_OPTION.findAll(optionsHtml)
                        .map { it.groupValues[1] }
                        .distinct()
                        .joinToString(",")
                    preferences.setString("leprecondoDiscovered", discoveries)
                }
            }
        }
    }

    fun processNeedChange(need: String, preferences: Preferences, currentRun: Int) {
        if (preferences.getString("leprecondoCurrentNeed", "") == need) return
        preferences.setString("leprecondoCurrentNeed", need)
        preferences.setInt("leprecondoLastNeedChange", currentRun)
        val history = preferences.getString("leprecondoNeedOrder", "")
        if (history.contains(need)) {
            if (history.split(",").filter { it.isNotBlank() }.size != 6) {
                preferences.setString("leprecondoNeedOrder", need)
            }
            return
        }
        preferences.setString(
            "leprecondoNeedOrder",
            if (history.isEmpty()) need else "$history,$need",
        )
    }

    fun getUndiscoveredFurnitureForLocation(zone: String, preferences: Preferences?): String {
        if (zone.isBlank() || preferences == null) return ""
        val furniture = Furniture.byLocation(zone)
        if (furniture.isEmpty()) return ""
        val discovered = preferences.getString("leprecondoDiscovered", "")
            .split(",")
            .mapNotNull { it.toIntOrNull() }
            .toSet()
        return furniture.filterNot { discovered.contains(it.id) }
            .joinToString(", ") { it.displayName }
    }
}
