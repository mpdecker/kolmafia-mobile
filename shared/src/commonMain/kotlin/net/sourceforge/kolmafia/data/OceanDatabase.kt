package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * Pirate-ship ocean destination map from [ocean.txt].
 * Mirrors desktop [OceanManager] static destination data.
 */
@OptIn(ExperimentalResourceApi::class)
object OceanDatabase {

    data class OceanPoint(val x: Int, val y: Int) {
        init {
            require(valid(x, y)) { "Invalid ocean point: $x,$y" }
        }

        override fun toString(): String = "$x,$y"

        companion object {
            const val X_MIN = 1
            const val X_MAX = 242
            const val Y_MIN = 1
            const val Y_MAX = 100

            private val pointPattern = Regex("""(\d+),(\d+)""")

            fun valid(x: Int, y: Int): Boolean =
                x in X_MIN..X_MAX && y in Y_MIN..Y_MAX

            fun parse(input: String): OceanPoint? {
                val match = pointPattern.matchEntire(input.trim()) ?: return null
                val x = match.groupValues[1].toIntOrNull() ?: return null
                val y = match.groupValues[2].toIntOrNull() ?: return null
                if (!valid(x, y)) return null
                return OceanPoint(x, y)
            }
        }
    }

    enum class OceanDestination(val desc: String) {
        GILLIGAN("Gilligan's Island"),
        MONKEY("Monkey Island"),
        OYSTER("Oyster Island"),
        DINOSAUR("Dinosaur Comics"),
        LAND_OF_LOST("Land of the Lost"),
        MYST("Myst Island"),
        CAST_AWAY("Cast Away"),
        LORD_OF_FLIES("Lord of the Flies"),
        LOST("LOST"),
        RAINBOW_SAND("rainbow sand"),
        ALTAR("sinister altar fragment"),
        SPHERE("El Vibrato power sphere"),
        PLINTH("Plinth"),
        MAINLAND("mainland"),
        ;

        companion object {
            private val byDesc = entries.associateBy { it.desc }

            fun fromDesc(desc: String): OceanDestination? = byDesc[desc]
        }
    }

    private val pointToDestination = mutableMapOf<OceanPoint, OceanDestination>()
    private val destinationPoints = mutableMapOf<OceanDestination, MutableSet<OceanPoint>>()
    private var loadedPointCountInternal = 0
    private var loaded = false

    val isLoaded: Boolean get() = loaded
    val loadedPointCount: Int get() = loadedPointCountInternal

    suspend fun load() {
        if (loaded) return
        val text = Res.readBytes("files/data/ocean.txt").decodeToString()
        applyParse(parse(text))
        loaded = true
    }

    fun destinationAt(point: OceanPoint): OceanDestination? = pointToDestination[point]

    fun pointsFor(destination: OceanDestination): Set<OceanPoint> =
        destinationPoints[destination]?.toSet() ?: emptySet()

    fun pointsForKeyword(keyword: String): List<OceanPoint>? =
        when (keyword.lowercase()) {
            "muscle" -> listFor(
                OceanDestination.GILLIGAN,
                OceanDestination.MONKEY,
                OceanDestination.OYSTER,
            )
            "mysticality" -> listFor(
                OceanDestination.DINOSAUR,
                OceanDestination.LAND_OF_LOST,
                OceanDestination.MYST,
            )
            "moxie" -> listFor(
                OceanDestination.CAST_AWAY,
                OceanDestination.LORD_OF_FLIES,
                OceanDestination.LOST,
            )
            "sand" -> listFor(OceanDestination.RAINBOW_SAND)
            "altar" -> listFor(OceanDestination.ALTAR)
            "sphere" -> listFor(OceanDestination.SPHERE)
            "plinth" -> listFor(OceanDestination.PLINTH)
            else -> null
        }

    fun isMainland(point: OceanPoint): Boolean =
        destinationAt(point) == OceanDestination.MAINLAND

    internal fun parseForTest(text: String): ParseSnapshot = parse(text)

    internal fun injectForTest(snapshot: ParseSnapshot) {
        applyParse(snapshot)
        loaded = true
    }

    internal fun resetForTest() {
        pointToDestination.clear()
        destinationPoints.clear()
        loadedPointCountInternal = 0
        loaded = false
    }

    data class ParseSnapshot(
        val pointToDestination: Map<OceanPoint, OceanDestination>,
        val destinationPoints: Map<OceanDestination, Set<OceanPoint>>,
        val loadedPointCount: Int,
    )

    private fun listFor(vararg destinations: OceanDestination): List<OceanPoint> =
        destinations.flatMap { destinationPoints[it].orEmpty() }

    private fun applyParse(snapshot: ParseSnapshot) {
        pointToDestination.clear()
        pointToDestination.putAll(snapshot.pointToDestination)
        destinationPoints.clear()
        snapshot.destinationPoints.forEach { (dest, points) ->
            destinationPoints[dest] = points.toMutableSet()
        }
        loadedPointCountInternal = snapshot.loadedPointCount
    }

    private fun parse(text: String): ParseSnapshot {
        val mutablePointToDestination = mutableMapOf<OceanPoint, OceanDestination>()
        val mutableDestinationPoints = mutableMapOf<OceanDestination, MutableSet<OceanPoint>>()
        var count = 0

        for (rawLine in text.lines()) {
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#")) continue

            val fields = line.split('\t')
            if (fields.size != 3) continue

            val lon = fields[0].trim().toIntOrNull() ?: continue
            val lat = fields[1].trim().toIntOrNull() ?: continue
            if (!OceanPoint.valid(lon, lat)) continue

            val destination = OceanDestination.fromDesc(fields[2].trim()) ?: continue
            val point = OceanPoint(lon, lat)
            mutablePointToDestination[point] = destination
            mutableDestinationPoints.getOrPut(destination) { mutableSetOf() }.add(point)
            count++
        }

        return ParseSnapshot(
            pointToDestination = mutablePointToDestination,
            destinationPoints = mutableDestinationPoints.mapValues { it.value.toSet() },
            loadedPointCount = count,
        )
    }
}
