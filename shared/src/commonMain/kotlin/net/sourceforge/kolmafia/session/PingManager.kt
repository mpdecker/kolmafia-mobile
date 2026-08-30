package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.PingRequest
import kotlin.math.roundToLong

/** Headless ping statistics, history and login-abort trigger model. */
object PingManager {
    class PingTest(val page: String = "api") {
        val pings = mutableListOf<Long>()
        var count: Long = 0
            internal set
        var total: Long = 0
            internal set
        var low: Long = 0
            internal set
        var high: Long = 0
            internal set
        var bytes: Long = 0
            internal set
        var trigger: PingAbortTrigger? = null

        val average: Double get() = if (count == 0L) 0.0 else total.toDouble() / count
        val bps: Double get() = if (total == 0L) 0.0 else bytes * 1000.0 / total

        fun addPing(elapsed: Long, responseBytes: Long) {
            pings += elapsed
            count++
            total += elapsed
            if (low == 0L || elapsed < low) low = elapsed
            if (elapsed > high) high = elapsed
            bytes += responseBytes
        }

        override fun toString(): String =
            "$page:$count:$low:$high:$total:$bytes:${"%.2f".format(average)}"

        fun isSaveable(preferences: Preferences): Boolean =
            page == PingRequest.normalizePage(preferences.getString("pingDefaultTestPage", "api"))

        fun save(preferences: Preferences) {
            preferences.setString("pingLatest", toString())
            if (!isSaveable(preferences)) return
            val shortest = parseProperty(preferences, "pingShortest")
            val longest = parseProperty(preferences, "pingLongest")
            if (shortest.page != page || shortest.count == 0L || average < shortest.average) {
                preferences.setString("pingShortest", toString())
            }
            if (longest.page != page || longest.count == 0L || average > longest.average) {
                preferences.setString("pingLongest", toString())
            }
        }
    }

    data class PingAbortTrigger(var count: Int, var factor: Int) : Comparable<PingAbortTrigger> {
        override fun compareTo(other: PingAbortTrigger): Int =
            compareValuesBy(this, other, { it.factor }, { it.count })
    }

    fun parseProperty(preferences: Preferences, property: String): PingTest {
        return parse(preferences.getString(property, ""))
    }

    fun parse(serialized: String?): PingTest {
        val values = serialized.orEmpty().split(":")
        if (values.size < 6) return PingTest("api")
        val page = PingRequest.normalizePage(values[0]).ifBlank { "api" }
        val result = PingTest(page)
        result.count = values[1].toLongOrNull() ?: 0
        result.low = values[2].toLongOrNull() ?: 0
        result.high = values[3].toLongOrNull() ?: 0
        result.total = values[4].toLongOrNull() ?: 0
        result.bytes = values[5].toLongOrNull() ?: 0
        return result
    }

    fun loadAbortTriggers(preferences: Preferences): Set<PingAbortTrigger> =
        preferences.getString("pingLoginAbort", "").split(Regex("\\s*\\|\\s*"))
            .mapNotNull { token ->
                val parts = token.split(":", limit = 2)
                if (parts.size != 2) return@mapNotNull null
                PingAbortTrigger(parts[0].toIntOrNull() ?: 0, parts[1].toIntOrNull() ?: 0)
                    .takeIf { it.count > 0 && it.factor > 0 }
            }.toSortedSet()

    fun saveAbortTriggers(preferences: Preferences, triggers: Set<PingAbortTrigger>) {
        preferences.setString(
            "pingLoginAbort",
            triggers.filter { it.count > 0 && it.factor > 0 }
                .sorted().joinToString("|") { "${it.count}:${it.factor}" },
        )
    }

    suspend fun runPingTest(
        client: io.ktor.client.HttpClient,
        count: Int,
        page: String,
        preferences: Preferences? = null,
        checkTriggers: Boolean = false,
    ): PingTest {
        val result = PingTest(PingRequest.normalizePage(page).ifBlank { "api" })
        val request = PingRequest(client, result.page)
        val historical = preferences?.let { parseProperty(it, "pingShortest") }
        val triggers = preferences?.let { loadAbortTriggers(it).associateWith { 0 }.toMutableMap() }
            ?: mutableMapOf()
        val check = checkTriggers && historical?.page == result.page
        if (request.run().isFailure) return result
        for (index in 0 until count.coerceAtLeast(0)) {
            if (request.run().isFailure) break
            val elapsed = request.getElapsedTime()
            result.addPing(elapsed, request.responseText?.length?.toLong() ?: 0)
            if (check && historical != null) {
                for ((trigger, seen) in triggers) {
                    if (elapsed >= historical.average * trigger.factor) {
                        val next = seen + 1
                        triggers[trigger] = next
                        if (next >= trigger.count) {
                            result.trigger = trigger
                            break
                        }
                    }
                }
                if (result.trigger != null) break
            }
        }
        preferences?.let { result.save(it) }
        return result
    }
}
