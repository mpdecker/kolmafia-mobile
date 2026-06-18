package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.VolcanoMazeRequest
import net.sourceforge.kolmafia.session.VolcanoMazeManager

internal fun GameRuntimeLibrary.cliVolcano(params: String, print: (String) -> Unit) {
    val tokens = params.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    val command = tokens.firstOrNull().orEmpty().lowercase()

    when (command) {
        "visit" -> {
            val client = httpClient ?: run {
                print("HTTP client is not available.")
                return
            }
            runBlocking {
                try {
                    val response = client.get("$KOL_BASE_URL/bigisland.php")
                    if (response.status.value in 200..299) {
                        preferences?.setBoolean(Preferences.VOLCANO_ISLAND_VISITED, true)
                    }
                } catch (_: Exception) {
                    // best-effort visit
                }
            }
        }
        "clear" -> {
            val prefs = preferences ?: run {
                print("Preferences are not available.")
                return
            }
            VolcanoMazeManager.clear(prefs)
        }
        "map" -> {
            val prefs = preferences ?: run {
                print("Preferences are not available.")
                return
            }
            if (tokens.size >= 2) {
                val mapNum = tokens[1].toIntOrNull()
                if (mapNum == null || mapNum !in 1..5) {
                    print("Choose map # from 1 - 5")
                    return
                }
                VolcanoMazeManager.displayMap(prefs, mapNum, print)
            } else {
                VolcanoMazeManager.displayMap(prefs, print)
            }
        }
        "platforms" -> {
            val prefs = preferences ?: run {
                print("Preferences are not available.")
                return
            }
            VolcanoMazeManager.platforms(prefs, print)
        }
        "step", "solve", "jump" -> {
            val prefs = preferences ?: run {
                print("Preferences are not available.")
                return
            }
            val client = httpClient ?: run {
                print("HTTP client is not available.")
                return
            }
            val request = VolcanoMazeRequest(client)
            runBlocking {
                when (command) {
                    "step" -> VolcanoMazeManager.step(request, prefs, print)
                    "solve" -> VolcanoMazeManager.solve(request, prefs, print)
                    "jump" -> VolcanoMazeManager.jump(request, prefs, print)
                    else -> Unit
                }
            }
        }
        "move", "movep" -> {
            val prefs = preferences ?: run {
                print("Preferences are not available.")
                return
            }
            val client = httpClient ?: run {
                print("HTTP client is not available.")
                return
            }
            if (tokens.size != 3) {
                print("Specify x y coordinate to jump to")
                return
            }
            val x = tokens[1].toIntOrNull()
            val y = tokens[2].toIntOrNull()
            if (x == null || x !in 0..12) {
                print("Coordinate 'x' must be between 0 and 12")
                return
            }
            if (y == null || y !in 0..12) {
                print("Coordinate 'y' must be between 0 and 12")
                return
            }
            val request = VolcanoMazeRequest(client)
            runBlocking {
                VolcanoMazeManager.moveTo(x, y, request, prefs, print, showMap = command == "movep")
            }
        }
        "test" -> {
            val prefs = preferences ?: run {
                print("Preferences are not available.")
                return
            }
            val mapNum: Int
            val x: Int
            val y: Int
            when (tokens.size) {
                1 -> {
                    mapNum = 1
                    x = 6
                    y = 12
                }
                4 -> {
                    mapNum = tokens[1].toIntOrNull() ?: run {
                        print("Map # must be between 1 and 5")
                        return
                    }
                    x = tokens[2].toIntOrNull() ?: run {
                        print("Coordinate 'x' must be between 0 and 12")
                        return
                    }
                    y = tokens[3].toIntOrNull() ?: run {
                        print("Coordinate 'y' must be between 0 and 12")
                        return
                    }
                }
                else -> {
                    print("Specify map x y")
                    return
                }
            }
            if (mapNum !in 1..5) {
                print("Map # must be between 1 and 5")
                return
            }
            if (x !in 0..12) {
                print("Coordinate 'x' must be between 0 and 12")
                return
            }
            if (y !in 0..12) {
                print("Coordinate 'y' must be between 0 and 12")
                return
            }
            VolcanoMazeManager.test(mapNum, x, y, prefs, print)
        }
        "" -> print("What do you want to do in the volcano?")
        else -> print("What do you want to do in the volcano?")
    }
}
