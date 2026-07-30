package net.sourceforge.kolmafia.data

import kotlin.math.floor
import kotlin.math.max
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class)
object DailyLimitDatabase {
    private val _allLimits = mutableListOf<DailyLimitData>()
    private val _byName = mutableMapOf<String, MutableList<DailyLimitData>>()
    private val _byItemId = mutableMapOf<DailyLimitKind, MutableMap<Int, DailyLimitEntry>>()
    private var loaded = false

    val allLimits: List<DailyLimitData> get() = _allLimits
    val byName: Map<String, List<DailyLimitData>> get() = _byName

    suspend fun load() {
        if (loaded) return
        val text = Res.readBytes("files/data/dailylimits.txt").decodeToString()
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            if (!line.contains('\t') && line.toIntOrNull() != null) continue
            val parts = line.split('\t')
            if (parts.size < 3) continue
            val type = parts[0].trim()
            val name = parts[1].trim()
            val trackingProperty = parts[2].trim()
            val maxRaw = if (parts.size >= 4) parts[3].trim() else ""
            val maxValue = resolveMaxValue(maxRaw)
            val entry = DailyLimitData(type, name, trackingProperty, maxValue)
            _allLimits += entry
            _byName.getOrPut(name.lowercase()) { mutableListOf() } += entry

            val kind = DailyLimitKind.fromTag(type) ?: continue
            val itemId = ItemDatabase.getByName(name)?.id ?: continue
            _byItemId.getOrPut(kind) { mutableMapOf() }[itemId] =
                DailyLimitEntry(kind, itemId, trackingProperty, maxValue)
        }
        loaded = true
    }

    fun getByName(name: String): List<DailyLimitData> = _byName[name.lowercase()] ?: emptyList()

    fun getEntry(itemId: Int, kind: DailyLimitKind): DailyLimitEntry? =
        _byItemId[kind]?.get(itemId)

    fun getUsesRemaining(entry: DailyLimitEntry, preferences: Preferences?): Int {
        val uses = readUses(entry.trackingProperty, preferences)
        return floor(max(0, entry.maxValue - uses).toDouble()).toInt()
    }

    fun all(): List<DailyLimitData> = _allLimits

    fun casts(): List<DailyLimitData> = _allLimits.filter { it.type.equals("Cast", ignoreCase = true) }

    fun uses(): List<DailyLimitData> = _allLimits.filter { it.type.equals("Use", ignoreCase = true) }

    fun byType(type: String): List<DailyLimitData> = _allLimits.filter { it.type.equals(type, ignoreCase = true) }

    internal fun resetForTest() {
        _allLimits.clear()
        _byName.clear()
        _byItemId.clear()
        loaded = false
    }

    private fun resolveMaxValue(maxRaw: String): Int {
        if (maxRaw.isEmpty()) return 1
        maxRaw.toIntOrNull()?.let { return it }
        if (maxRaw.startsWith("[") && maxRaw.endsWith("]")) return 1
        return 1
    }

    private fun readUses(trackingProperty: String, preferences: Preferences?): Int {
        if (preferences == null) return 0
        return when (preferences.getString(trackingProperty, "")) {
            "true" -> 1
            "false" -> 0
            else -> {
                if (preferences.getBoolean(trackingProperty, false)) {
                    1
                } else {
                    preferences.getInt(trackingProperty, 0)
                }
            }
        }
    }
}
