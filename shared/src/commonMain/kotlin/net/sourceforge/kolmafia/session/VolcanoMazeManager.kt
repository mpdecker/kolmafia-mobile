package net.sourceforge.kolmafia.session

import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.sourceforge.kolmafia.data.VolcanoMazeDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.VolcanoMazeRequest
import net.sourceforge.kolmafia.volcano.Neighbors
import net.sourceforge.kolmafia.volcano.VolcanoMap
import net.sourceforge.kolmafia.volcano.VolcanoMazeConstants
import net.sourceforge.kolmafia.volcano.VolcanoMazeConstants.CELLS
import net.sourceforge.kolmafia.volcano.VolcanoMazeConstants.GOAL
import net.sourceforge.kolmafia.volcano.VolcanoMazeConstants.MAPS
import net.sourceforge.kolmafia.volcano.VolcanoMazeConstants.NCOLS
import net.sourceforge.kolmafia.volcano.VolcanoMazeConstants.NROWS
import net.sourceforge.kolmafia.volcano.VolcanoMazeConstants.START
import net.sourceforge.kolmafia.volcano.VolcanoMazeConstants.pos
import net.sourceforge.kolmafia.volcano.VolcanoPath

object VolcanoMazeManager {

    private val maps = arrayOfNulls<VolcanoMap>(MAPS)
    private val squares = IntArray(CELLS)
    private val neighbors = arrayOfNulls<Neighbors>(CELLS)
    private var currentMap = 0
    private var currentLocation = -1
    private var found = 1
    private var loaded = false
    private var pathsMade = 0
    private var pathsExamined = 0

    private val json = Json { ignoreUnknownKeys = true }
    private val squarePattern = Regex(
        """<div id="sq(\d+)" class="sq (no|yes)\s+(you|goal|)\s*lv(\d+)" rel="(\d+),(\d+)">""",
    )
    private val posPattern = Regex("""(\d+),(\d+)""")

    fun reset() {
        loaded = false
        currentMap = 0
        currentLocation = -1
        maps.fill(null)
        squares.fill(0)
        neighbors.fill(null)
        found = 1
        pathsMade = 0
        pathsExamined = 0
    }

    fun clear(preferences: Preferences) {
        reset()
        for (map in 1..MAPS) {
            preferences.setString("volcanoMaze$map", "")
        }
    }

    fun atGoal(): Boolean = VolcanoMazeConstants.GOAL_NEIGHBORS.contains(currentLocation)

    fun currentCoordinates(): String = VolcanoMazeConstants.coordinateString(currentLocation)

    fun loadCurrentMaps(preferences: Preferences) {
        if (loaded) return
        for (map in 0 until MAPS) {
            loadCurrentMap(preferences, map)
        }
        currentMap = 0
        currentLocation = -1
        loaded = true
    }

    internal fun loadCurrentMaps(preferences: Preferences, current: Int, level: Int) {
        reloadMapsFromPrefs(preferences)
        currentMap = level
        currentLocation = current
    }

    private fun reloadMapsFromPrefs(preferences: Preferences) {
        maps.fill(null)
        squares.fill(0)
        neighbors.fill(null)
        found = 1
        loaded = false
        for (map in 0 until MAPS) {
            loadCurrentMap(preferences, map)
        }
        loaded = true
    }

    private fun loadCurrentMap(preferences: Preferences, map: Int) {
        val setting = "volcanoMaze${map + 1}"
        val coords = preferences.getString(setting, "")
        if (!validMap(coords)) {
            preferences.setString(setting, "")
            maps[map] = null
        } else {
            maps[map] = VolcanoMap(coords)
            addSquares(map, preferences)
        }
    }

    internal fun validMap(coordinates: String): Boolean {
        if (coordinates.isBlank()) return false
        for (token in coordinates.split(',')) {
            val trimmed = token.trim()
            if (trimmed.isEmpty()) continue
            val value = trimmed.toIntOrNull() ?: return false
            if (value !in VolcanoMazeConstants.MIN_SQUARE..VolcanoMazeConstants.MAX_SQUARE) {
                return false
            }
        }
        return true
    }

    private fun addSquares(index: Int, preferences: Preferences) {
        val map = maps[index] ?: return
        val seq = index + 1
        for (square in map.platforms) {
            val old = squares[square]
            if (old == 0) {
                squares[square] = seq
                found++
            } else if (old != seq) {
                // duplicate platform across maps — desktop logs only
            }
        }
    }

    fun parseResult(preferences: Preferences, responseText: String) {
        loadCurrentMaps(preferences)
        val coords = parseCoords(responseText) ?: return
        if (!validMap(coords)) return

        if (found == CELLS) {
            for (i in maps.indices) {
                if (coords == maps[i]?.coordinates) {
                    currentMap = i
                    return
                }
            }
        }

        if (preferences.getBoolean("useCachedVolcanoMaps", true) && VolcanoMazeDatabase.isLoaded) {
            val key = VolcanoMazeDatabase.keyForCoordinates(coords)
            if (key != null) {
                val mapSequence = VolcanoMazeDatabase.getMapSequence(key) ?: return
                for (i in mapSequence.indices) {
                    val map = mapSequence[i] ?: continue
                    maps[i] = map
                    addSquares(i, preferences)
                    if (coords == map.coordinates) {
                        currentMap = i
                    }
                    preferences.setString("volcanoMaze${i + 1}", map.coordinates)
                }
                found = CELLS
                return
            }
        }

        var index = currentMap
        do {
            val current = maps[index]
            if (current == null) {
                currentMap = index
                maps[index] = VolcanoMap(coords)
                break
            }
            if (coords == current.coordinates) {
                currentMap = index
                return
            }
            index = (index + 1) % MAPS
        } while (index != currentMap)

        val sequence = index + 1
        preferences.setString("volcanoMaze$sequence", coords)
        addSquares(currentMap, preferences)
    }

    internal fun parseCoords(responseText: String): String? = when {
        responseText == "false" -> null
        responseText.startsWith("<html>") -> parseHtmlCoords(responseText)
        else -> parseJsonCoords(responseText)
    }

    private fun parseHtmlCoords(responseText: String): String {
        val buffer = StringBuilder()
        var first = true
        for (match in squarePattern.findAll(responseText)) {
            val square = match.groupValues[1].toIntOrNull() ?: continue
            val special = match.groupValues[3]
            when (special) {
                "you" -> currentLocation = square
                "goal" -> if (square != GOAL) {
                    // goal mismatch — desktop logs only
                }
            }
            if (match.groupValues[2] != "yes") continue
            if (first) {
                first = false
            } else {
                buffer.append(',')
            }
            buffer.append(square)
        }
        return buffer.toString()
    }

    internal fun parseJsonCoords(responseText: String): String {
        val buffer = StringBuilder()
        val root = try {
            json.parseToJsonElement(responseText).jsonObject
        } catch (_: Exception) {
            return ""
        }

        try {
            val pos = root["pos"]?.jsonPrimitive?.content
            if (pos != null) {
                val matcher = posPattern.find(pos)
                if (matcher != null) {
                    val col = matcher.groupValues[1].toInt()
                    val row = matcher.groupValues[2].toInt()
                    currentLocation = row * NCOLS + col
                }
            }
        } catch (_: Exception) {
            currentLocation = -1
        }

        val show = try {
            root["show"]?.jsonArray
        } catch (_: Exception) {
            return ""
        } ?: return ""

        var first = true
        for (element in show) {
            val square = element.jsonPrimitive.content
            if (square == GOAL.toString()) continue
            if (first) {
                first = false
            } else {
                buffer.append(',')
            }
            buffer.append(square)
        }
        return buffer.toString()
    }

    fun generateNeighbors() {
        for (square in 0 until CELLS) {
            if (neighbors[square] != null) continue
            neighbors[square] = if (square == GOAL) {
                Neighbors(square, null)
            } else {
                val index = squares[square]
                val pmap = maps[index % MAPS]
                pmap?.neighbors(square) ?: Neighbors(square, null)
            }
        }
    }

    fun solve(location: Int, map: Int): VolcanoPath? {
        if (found < CELLS) return null
        generateNeighbors()

        val queue = ArrayDeque<VolcanoPath>()
        val visited = BooleanArray(CELLS)
        pathsMade = 0
        pathsExamined = 0

        val current = maps[map] ?: return null
        val roots = current.neighbors(location)
        visited[location] = true

        for (square in roots.platforms) {
            pathsMade++
            queue.addLast(VolcanoPath(square))
            visited[square] = true
        }

        while (queue.isNotEmpty()) {
            val path = queue.removeFirst()
            pathsExamined++
            val last = path.getLast()
            val neighbor = neighbors[last] ?: continue
            for (platform in neighbor.platforms) {
                if (platform == GOAL) {
                    pathsMade++
                    return VolcanoPath(path, platform)
                }
                if (!visited[platform]) {
                    pathsMade++
                    queue.addLast(VolcanoPath(path, platform))
                    visited[platform] = true
                }
            }
        }
        return null
    }

    fun printStatistics(solution: VolcanoPath?, print: (String) -> Unit) {
        val suffix = if (solution != null) {
            " solution with ${solution.size()} hops."
        } else {
            " no solution found."
        }
        print("Paths examined/made $pathsExamined/$pathsMade ->$suffix")
    }

    suspend fun visitMaze(request: VolcanoMazeRequest, preferences: Preferences, print: (String) -> Unit) {
        request.runFetch("volcanomaze.php?start=1", preferences).fold(
            onSuccess = { printCurrentCoordinates(print) },
            onFailure = { print(it.message ?: "Failed to visit lava maze.") },
        )
    }

    suspend fun jump(request: VolcanoMazeRequest, preferences: Preferences, print: (String) -> Unit) {
        request.runFetch("volcanomaze.php?jump=1", preferences).fold(
            onSuccess = { printCurrentCoordinates(print) },
            onFailure = { print(it.message ?: "Failed to jump.") },
        )
    }

    suspend fun moveTo(
        col: Int,
        row: Int,
        request: VolcanoMazeRequest,
        preferences: Preferences,
        print: (String) -> Unit,
        showMap: Boolean = false,
    ) {
        request.runFetch(VolcanoMazeRequest.getMoveUrl(col, row), preferences).fold(
            onSuccess = {
                if (showMap) displayMap(preferences, print)
                printCurrentCoordinates(print)
            },
            onFailure = { print(it.message ?: "Failed to move.") },
        )
    }

    suspend fun discoverMaps(
        request: VolcanoMazeRequest,
        preferences: Preferences,
        print: (String) -> Unit,
    ): Boolean {
        loadCurrentMaps(preferences)
        if (found == CELLS) return true

        if (currentLocation < 0) {
            val error = request.runFetch("volcanomaze.php?start=1", preferences).exceptionOrNull()?.message
            if (error != null) {
                print(error)
                return false
            }
        }

        if (currentLocation < 0) {
            print("You couldn't find the lava cave")
            return false
        }

        printCurrentCoordinates(print)

        while (found < CELLS) {
            val map = maps[currentMap] ?: return false
            val next = map.pickNeighbor(currentLocation)
            if (next < 0) {
                print("You seem to be stuck")
                return false
            }
            print("Move to: ${VolcanoMazeConstants.coordinateString(next, currentMap)}")
            val ofound = found
            request.runFetch(VolcanoMazeRequest.getMoveUrl(next), preferences).onFailure {
                print(it.message ?: "Failed to move.")
                return false
            }
            if (ofound >= found) {
                print("Moving did not discover new platforms")
                return false
            }
        }
        return true
    }

    suspend fun step(
        request: VolcanoMazeRequest,
        preferences: Preferences,
        print: (String) -> Unit,
    ) {
        loadCurrentMaps(preferences)
        if (atGoal()) return
        val path = nextStep(preferences, print)
        request.runFetch(path, preferences).fold(
            onSuccess = { printCurrentCoordinates(print) },
            onFailure = { print(it.message ?: "Failed to step.") },
        )
    }

    private fun nextStep(preferences: Preferences, print: (String) -> Unit): String {
        if (currentLocation < 0) return "volcanomaze.php?start=1"

        if (found < CELLS) {
            val map = maps[currentMap] ?: return "volcanomaze.php?start=1"
            val next = map.pickNeighbor(currentLocation)
            return if (next < 0) {
                "volcanomaze.php?jump=1"
            } else {
                VolcanoMazeRequest.getMoveUrl(next)
            }
        }

        if (atGoal()) return "volcanomaze.php?start=1"

        val solution = solve(currentLocation, currentMap)
        printStatistics(solution, print)
        if (solution == null) return "volcanomaze.php?jump=1"
        return VolcanoMazeRequest.getMoveUrl(solution[0])
    }

    suspend fun solve(
        request: VolcanoMazeRequest,
        preferences: Preferences,
        print: (String) -> Unit,
    ) {
        if (!discoverMaps(request, preferences, print)) return

        if (found < CELLS) {
            print("We couldn't discover all the maps")
            return
        }

        val solution = solve(currentLocation, currentMap)
        printStatistics(solution, print)

        if (solution == null) {
            print("You can't get there from here. Swim to shore and try again.")
            return
        }

        var moveIndex = 0
        for (sq in solution) {
            if (sq == GOAL) break
            moveIndex++
            print("Move $moveIndex of ${solution.size()}")
            request.runFetch(VolcanoMazeRequest.getMoveUrl(sq), preferences).onFailure {
                print(it.message ?: "Failed to move.")
                return
            }
        }
    }

    fun test(
        mapNum: Int,
        x: Int,
        y: Int,
        preferences: Preferences,
        print: (String) -> Unit,
    ) {
        reloadMapsFromPrefs(preferences)
        if (found < CELLS) {
            print("You don't know all the maps")
            return
        }
        val location = pos(y, x)
        val solution = solve(location, mapNum - 1)
        printStatistics(solution, print)
        if (solution == null) {
            print("You can't get there from here. Swim to shore and try again.")
            return
        }
        for (next in solution) {
            print("Hop to ${VolcanoMazeConstants.coordinateString(next)}")
        }
    }

    private fun printCurrentCoordinates(print: (String) -> Unit) {
        val msg = if (currentLocation < 0) {
            "I don't know where you are"
        } else {
            "Current position: ${VolcanoMazeConstants.coordinateString(currentLocation, currentMap)}"
        }
        print(msg)
    }

    fun displayMap(preferences: Preferences, print: (String) -> Unit) {
        loadCurrentMaps(preferences)
        val map = maps[currentMap]
        if (map == null) {
            print("We haven't seen the volcanic cave yet")
            return
        }
        map.printMap(currentLocation, print)
    }

    fun displayMap(preferences: Preferences, mapNum: Int, print: (String) -> Unit) {
        if (mapNum !in 1..MAPS) {
            print("Choose map # from 1 - $MAPS")
            return
        }
        loadCurrentMaps(preferences)
        val map = maps[mapNum - 1]
        if (map == null) {
            print("We haven't seen map #$mapNum")
            return
        }
        map.printMap(-1, print)
    }

    fun platforms(preferences: Preferences, print: (String) -> Unit) {
        loadCurrentMaps(preferences)
        if (maps.all { it == null }) {
            print("We haven't seen the volcanic cave yet")
            return
        }
        print("\t" + (0 until NCOLS).joinToString("\t") { it.toString() })
        for (row in 0 until NROWS) {
            val line = buildString {
                append(row)
                for (col in 0 until NCOLS) {
                    append('\t')
                    append(squares[pos(row, col)])
                }
            }
            print(line)
        }
        print("$found total platforms seen.")
    }

    internal fun resetForTest() = reset()

    internal fun currentLocationForTest(): Int = currentLocation

    internal fun foundForTest(): Int = found
}
