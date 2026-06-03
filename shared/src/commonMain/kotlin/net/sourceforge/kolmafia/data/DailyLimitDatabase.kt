package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class)
object DailyLimitDatabase {
    private val _allLimits = mutableListOf<DailyLimitData>()
    private val _byName = mutableMapOf<String, MutableList<DailyLimitData>>()
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
            val maxValue = if (parts.size >= 4) parts[3].trim().toIntOrNull() ?: -1 else -1
            val entry = DailyLimitData(type, name, trackingProperty, maxValue)
            _allLimits += entry
            _byName.getOrPut(name.lowercase()) { mutableListOf() } += entry
        }
        loaded = true
    }

    fun getByName(name: String): List<DailyLimitData> = _byName[name.lowercase()] ?: emptyList()

    fun all(): List<DailyLimitData> = _allLimits

    fun casts(): List<DailyLimitData> = _allLimits.filter { it.type.equals("Cast", ignoreCase = true) }

    fun uses(): List<DailyLimitData> = _allLimits.filter { it.type.equals("Use", ignoreCase = true) }

    fun byType(type: String): List<DailyLimitData> = _allLimits.filter { it.type.equals(type, ignoreCase = true) }
}
