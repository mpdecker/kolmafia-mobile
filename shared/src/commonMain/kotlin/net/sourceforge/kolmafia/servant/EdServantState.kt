package net.sourceforge.kolmafia.servant

import net.sourceforge.kolmafia.modifiers.ServantData
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Preference-backed per-servant name/level/XP. Mirrors desktop [EdServantData.edServants].
 */
object EdServantState {

    const val RECORDS_PREF = "_edServantRecords"

    fun getRecord(preferences: Preferences, type: String): EdServantRecord? {
        val resolved = ServantData.resolve(type)?.type ?: return null
        return getAllRecords(preferences).firstOrNull { it.type.equals(resolved, ignoreCase = true) }
    }

    fun getAllRecords(preferences: Preferences): List<EdServantRecord> =
        preferences.getString(RECORDS_PREF, "")
            .split(';')
            .mapNotNull { decodeEntry(it) }

    fun upsert(preferences: Preferences, record: EdServantRecord) {
        val resolved = ServantData.resolve(record.type)?.type ?: return
        val normalized = record.copy(type = resolved)
        val records = getAllRecords(preferences)
            .filterNot { it.type.equals(resolved, ignoreCase = true) }
            .toMutableList()
        records += normalized
        preferences.setString(RECORDS_PREF, records.joinToString(";") { encodeEntry(it) })
    }

    fun addCombatExperience(
        preferences: Preferences,
        activeType: String,
        crownOfEdEquipped: Boolean,
    ): EdServantRecord? {
        val record = getRecord(preferences, activeType) ?: return null
        if (record.experience >= MAX_EXPERIENCE) return record

        val delta = if (crownOfEdEquipped) 2 else 1
        var experience = (record.experience + delta).coerceAtMost(MAX_EXPERIENCE)
        var level = record.level
        val nextLevel = level + 1
        if (experience >= nextLevel * nextLevel) {
            level++
        }
        val updated = record.copy(level = level, experience = experience)
        upsert(preferences, updated)
        return updated
    }

    private fun encodeEntry(record: EdServantRecord): String =
        listOf(record.type, record.name, record.level.toString(), record.experience.toString())
            .joinToString(":") { it.replace(':', ' ') }

    private fun decodeEntry(entry: String): EdServantRecord? {
        if (entry.isBlank()) return null
        val parts = entry.split(':')
        if (parts.size < 4) return null
        val type = parts[0]
        val experience = parts.last().toIntOrNull() ?: return null
        val level = parts[parts.size - 2].toIntOrNull() ?: return null
        val name = parts.subList(1, parts.size - 2).joinToString(":")
        if (type.isBlank()) return null
        return EdServantRecord(type = type, name = name, level = level, experience = experience)
    }

    private const val MAX_EXPERIENCE = 441
}
