package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

// Parses equipment.txt from bundled compose resources.
// The file is divided into sections (Hats, Weapons, etc.).
// Each data line is tab-separated: name  power  stat_requirement
@OptIn(ExperimentalResourceApi::class)
object EquipmentDatabase {

    private val byName = mutableMapOf<String, EquipmentData>()
    private var loaded = false

    suspend fun load() {
        if (loaded) return
        val text = Res.readBytes("files/data/equipment.txt").decodeToString()
        parse(text)
        loaded = true
    }

    fun getByName(name: String): EquipmentData? = byName[name.lowercase()]
    fun all(): Collection<EquipmentData> = byName.values

    private fun parse(text: String) {
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            // Skip version line and section headers
            if (!line.contains('\t')) continue

            val parts = line.split('\t')
            if (parts.size < 2) continue

            val name = parts[0].trim()
            val power = parts[1].trim().toIntOrNull() ?: continue
            val statReq = parts.getOrNull(2)?.trim()?.let {
                if (it == "none" || it.isBlank()) null else it
            }

            val equip = EquipmentData(name, power, statReq)
            byName[name.lowercase()] = equip
        }
    }
}
