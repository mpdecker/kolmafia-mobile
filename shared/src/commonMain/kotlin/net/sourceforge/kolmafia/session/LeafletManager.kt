package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.preferences.Preferences

/** State parser and resumable automation engine for the Strange Leaflet. */
object LeafletManager {
    enum class Location(val marker: String, val description: String) {
        HOUSE(">In the House</b>", "in the house"),
        FIELD(">West of House</b>", "west of the house"),
        PATH(">North of the Field</b>", "north of the field"),
        CLEARING(">Forest Clearing</b>", "in the forest clearing"),
        CAVE(">Cave</b>", "in the cave"),
        BANK(">South Bank</b>", "on the south bank"),
        FOREST(">Forest</b>", "in the forest"),
        BOTTOM(">On the other side of the forest maze...</b>", "past maze"),
        TREE(">Halfway Up The Tree</b>", "halfway up the tree"),
        TABLE(">Tabletop</b>", "on the tabletop"),
        UNKNOWN("", "Unknown"),
    }

    data class State(
        var location: Location = Location.UNKNOWN,
        var leaflet: Boolean = false,
        var sword: Boolean = false,
        var stick: Boolean = false,
        var boots: Boolean = false,
        var parchment: Boolean = false,
        var egg: Boolean = false,
        var ruby: Boolean = false,
        var scroll: Boolean = false,
        var ring: Boolean = false,
        var trophy: Boolean = false,
        var wornBoots: Boolean = false,
        var door: Boolean = false,
        var hedge: Boolean = false,
        var torch: Boolean = false,
        var serpent: Boolean = false,
        var chest: Boolean = false,
        var fireplace: Boolean = false,
        var magic: String? = null,
        var mazeExit: String? = null,
        var roadrunner: Boolean = false,
        var petunias: Boolean = false,
        var giant: Boolean = false,
    )

    private val forestPattern = Regex("""Gaps in the dense, forbidding foliage lead (.*?),""")
    var state = State()
        private set

    fun reset() { state = State() }

    fun getLocation(response: String): Location =
        Location.entries.firstOrNull { it != Location.UNKNOWN && response.contains(it.marker) }
            ?: Location.UNKNOWN

    fun locationName(response: String): String = getLocation(response).description

    fun parseLocation(response: String): Location {
        val location = getLocation(response)
        state.location = location
        state.mazeExit = null
        when (location) {
            Location.HOUSE -> {
                state.fireplace = response.contains("fireplace is lit")
                state.door = true
            }
            Location.FIELD -> state.door = !response.contains("front door is closed")
            Location.PATH -> state.hedge = !response.contains("thick hedge")
            Location.CLEARING -> state.hedge = true
            Location.CAVE -> {
                state.hedge = true
                state.chest = response.contains("empty treasure chest")
                state.serpent = !response.contains("dangerous-looking serpent")
            }
            Location.BANK -> state.fireplace = true
            Location.FOREST -> state.mazeExit = forestPattern.find(response)?.groupValues?.get(1)
            Location.TREE -> {
                state.roadrunner = !response.contains("large ruby in its beak")
                state.petunias = !response.contains("scroll entangled in the flowers")
            }
            Location.TABLE -> state.giant = !response.contains("The Giant himself")
            Location.BOTTOM, Location.UNKNOWN -> Unit
        }
        return location
    }

    fun initialize(response: String): State {
        state = State()
        state.leaflet = response.contains("A junk mail leaflet")
        state.sword = response.contains("An ornate sword") && !response.contains("hangs above the mantel")
        state.torch = response.contains("A burning torch")
        state.stick = state.torch ||
            response.contains("A hefty stick") && !response.contains("lies on the ground")
        state.boots = response.contains("A pair of large rubber wading boots")
        state.wornBoots = state.boots && response.contains("boots (equipped)")
        state.parchment = response.contains("A piece of parchment")
        state.egg = response.contains("A jewel-encrusted egg")
        state.ruby = response.contains("A fiery ruby")
        state.scroll = response.contains("A rolled-up scroll")
        state.ring = response.contains("A giant's pinky ring")
        state.trophy = response.contains("A shiny bowling trophy")
        parseLocation(response)
        return state
    }

    fun parseMantelpiece(response: String): String? {
        state.magic = when {
            response.contains("carved driftwood bird") -> "plover"
            response.contains("small white house") -> "xyzzy"
            response.contains("brick building") -> "plugh"
            response.contains("model ship") -> "yoho"
            else -> null
        }
        return state.magic
    }

    suspend fun locationName(execute: suspend (String) -> Result<String>): Result<String> =
        execute("inv").map { locationName(it) }

    suspend fun robStrangeLeaflet(
        invokeMagic: Boolean,
        preferences: Preferences?,
        execute: suspend (String) -> Result<String>,
    ): Result<List<String>> {
        val commands = mutableListOf<String>()
        suspend fun command(value: String): Result<String> {
            commands += value
            return execute(value).onSuccess(::parseLocation)
        }

        val inventory = command("inv").getOrElse { return Result.failure(it) }
        initialize(inventory)

        suspend fun go(destination: Location): Result<Unit> {
            var guard = 100
            while (state.location != destination && guard-- > 0) {
                val cmd = nextMovement(destination)
                    ?: return Result.failure(IllegalStateException(
                        "Cannot reach ${destination.description} from ${state.location.description}.",
                    ))
                command(cmd).getOrElse { return Result.failure(it) }
                if (state.location == Location.FOREST && destination == Location.BOTTOM) {
                    while (state.mazeExit != null && guard-- > 0) {
                        command(state.mazeExit!!).getOrElse { return Result.failure(it) }
                    }
                }
            }
            return if (state.location == destination) Result.success(Unit)
            else Result.failure(IllegalStateException("Leaflet navigation did not converge."))
        }

        // First half: collect the mail, sword/stick, clear hedge, light torch, kill serpent/chest.
        if (state.location.ordinal <= Location.BANK.ordinal) {
            if (!state.leaflet) {
                go(Location.FIELD).getOrElse { return Result.failure(it) }
                command("open mailbox").getOrElse { return Result.failure(it) }
                command("take leaflet").getOrElse { return Result.failure(it) }
                state.leaflet = true
            }
            if (!state.sword) {
                if (!state.door) {
                    go(Location.FIELD).getOrElse { return Result.failure(it) }
                    command("open door").getOrElse { return Result.failure(it) }
                    state.door = true
                }
                go(Location.HOUSE).getOrElse { return Result.failure(it) }
                command("take sword").getOrElse { return Result.failure(it) }
                state.sword = true
            }
            if (!state.hedge) {
                go(Location.PATH).getOrElse { return Result.failure(it) }
                command("cut hedge").getOrElse { return Result.failure(it) }
                state.hedge = true
            }
            if (!state.stick && !state.torch) {
                go(Location.PATH).getOrElse { return Result.failure(it) }
                command("take stick").getOrElse { return Result.failure(it) }
                state.stick = true
            }
            if (!state.torch) {
                go(Location.CLEARING).getOrElse { return Result.failure(it) }
                command("light stick").getOrElse { return Result.failure(it) }
                state.torch = true
            }
            go(Location.CAVE).getOrElse { return Result.failure(it) }
            if (!state.serpent) command("kill serpent").getOrElse { return Result.failure(it) }
            if (!state.chest) command("open chest").getOrElse { return Result.failure(it) }
            command("look behind chest").getOrElse { return Result.failure(it) }
            command("look in hole").getOrElse { return Result.failure(it) }

            // Discover the per-player magic word before lighting the fireplace.
            go(Location.HOUSE).getOrElse { return Result.failure(it) }
            parseMantelpiece(command("examine fireplace").getOrElse { return Result.failure(it) })
            if (state.magic == null) {
                command("take trophy").getOrElse { return Result.failure(it) }
                state.trophy = true
            } else if (!invokeMagic) {
                return Result.success(commands)
            } else {
                val word = state.magic!!
                val magicResponse = command(word).getOrElse { return Result.failure(it) }
                if (magicResponse.contains("That only works once.") ||
                    magicResponse.contains("send the plover over") ||
                    magicResponse.contains("nothing happens")
                ) state.magic = null
            }

            if (!state.parchment) {
                command("examine fireplace").getOrElse { return Result.failure(it) }
                command("examine tinder").getOrElse { return Result.failure(it) }
                state.parchment = true
            }
            command("light fireplace").getOrElse { return Result.failure(it) }
            state.fireplace = true
            command("take boots").getOrElse { return Result.failure(it) }
            state.boots = true
            command("wear boots").getOrElse { return Result.failure(it) }
            state.wornBoots = true
        }

        // Second half: maze/tree puzzle, CLEESH the giant, and collect the ring.
        if (!state.ring) {
            go(Location.BOTTOM).getOrElse { return Result.failure(it) }
            go(Location.TREE).getOrElse { return Result.failure(it) }
            if (!state.roadrunner) {
                if (!state.egg) command("take egg").getOrElse { return Result.failure(it) }
                command("throw egg at roadrunner").getOrElse { return Result.failure(it) }
            }
            go(Location.BOTTOM).getOrElse { return Result.failure(it) }
            command("move leaves").getOrElse { return Result.failure(it) }
            state.ruby = true
            go(Location.TREE).getOrElse { return Result.failure(it) }
            if (!state.petunias) {
                command("throw ruby at petunias").getOrElse { return Result.failure(it) }
                command("read scroll").getOrElse { return Result.failure(it) }
            }
            if (state.parchment) command("GNUSTO CLEESH").getOrElse { return Result.failure(it) }
            go(Location.TABLE).getOrElse { return Result.failure(it) }
            if (!state.giant) command("CLEESH giant").getOrElse { return Result.failure(it) }
            command("take ring").getOrElse { return Result.failure(it) }
            state.ring = true
        }
        preferences?.setBoolean("leafletCompleted", true)
        return Result.success(commands)
    }

    /** Immediate movement only; object-manipulation preconditions are handled by the solver. */
    fun nextMovement(destination: Location): String? = when (destination) {
        Location.HOUSE -> when (state.location) {
            Location.FIELD -> "east"
            Location.PATH -> "south"
            Location.CLEARING -> "east"
            Location.CAVE -> "south"
            Location.BANK -> "north"
            else -> null
        }
        Location.FIELD -> when (state.location) {
            Location.HOUSE -> "west"
            Location.PATH -> "south"
            Location.BANK -> "north"
            Location.CLEARING -> "east"
            Location.CAVE -> "south"
            else -> null
        }
        Location.PATH -> when (state.location) {
            Location.FIELD -> "north"
            Location.CLEARING -> "east"
            Location.CAVE -> "south"
            Location.HOUSE -> "west"
            Location.BANK -> "north"
            else -> null
        }
        Location.CLEARING -> if (state.location == Location.PATH) "west" else nextMovement(Location.PATH)
        Location.CAVE -> if (state.location == Location.PATH) "north" else nextMovement(Location.PATH)
        Location.BANK -> if (state.location == Location.FIELD) "south" else nextMovement(Location.FIELD)
        Location.FOREST -> if (state.location == Location.BANK) "south" else nextMovement(Location.BANK)
        Location.BOTTOM -> when (state.location) {
            Location.BANK -> "south"
            Location.FOREST -> state.mazeExit
            Location.TREE -> "down"
            Location.TABLE -> "down"
            else -> nextMovement(Location.FOREST)
        }
        Location.TREE -> when (state.location) {
            Location.BOTTOM -> "up"
            Location.TABLE -> "down"
            else -> nextMovement(Location.BOTTOM)
        }
        Location.TABLE -> if (state.location == Location.TREE) "up" else nextMovement(Location.TREE)
        Location.UNKNOWN -> null
    }
}
