package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.character.CharacterClass
import net.sourceforge.kolmafia.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * Journeyman path skill/zone map from [journeyman.txt]. Mirrors desktop [JourneyManager].
 */
@OptIn(ExperimentalResourceApi::class)
object JourneymanDatabase {

    private val zoneSkills = mutableMapOf<String, Map<CharacterClass, Array<String?>>>()
    private val skillLocations = mutableMapOf<String, Map<CharacterClass, Int>>()
    private var zoneNamesList = listOf<String>()
    private var entryCount = 0
    private var loaded = false

    val zoneNames: List<String> get() = zoneNamesList
    val isLoaded: Boolean get() = loaded
    val loadedEntryCount: Int get() = entryCount
    val skillCount: Int get() = skillLocations.size

    suspend fun load() {
        if (loaded) return
        val text = Res.readBytes("files/data/journeyman.txt").decodeToString()
        applyParse(parse(text, validateReferences = true))
        loaded = true
    }

    fun skillsForZone(locationName: String, characterClass: CharacterClass): Array<String?>? =
        zoneSkills[locationName]?.get(characterClass)

    fun encodedLocation(skillName: String, characterClass: CharacterClass): Int? =
        skillLocations[skillName]?.get(characterClass)

    fun locationsForSkill(skillName: String): Map<CharacterClass, Int> =
        skillLocations[skillName] ?: emptyMap()

    fun zoneNameForEncoded(encoded: Int): String? = zoneNamesList.getOrNull(encoded / 6)

    fun turnsForEncoded(encoded: Int): Int = (encoded % 6 + 1) * 4

    internal fun parseForTest(text: String, validateReferences: Boolean = false): ParseSnapshot =
        parse(text, validateReferences)

    internal fun injectForTest(snapshot: ParseSnapshot) {
        applyParse(snapshot)
        loaded = true
    }

    internal fun resetForTest() {
        zoneSkills.clear()
        skillLocations.clear()
        zoneNamesList = emptyList()
        entryCount = 0
        loaded = false
    }

    data class ParseSnapshot(
        val zoneSkills: Map<String, Map<CharacterClass, Array<String?>>>,
        val skillLocations: Map<String, Map<CharacterClass, Int>>,
        val zoneNames: List<String>,
        val entryCount: Int,
    )

    private fun applyParse(snapshot: ParseSnapshot) {
        zoneSkills.clear()
        zoneSkills.putAll(snapshot.zoneSkills)
        skillLocations.clear()
        skillLocations.putAll(snapshot.skillLocations)
        zoneNamesList = snapshot.zoneNames
        entryCount = snapshot.entryCount
    }

    private val skillBracketPattern = Regex("""^\[(\d+)\](.+)$""")

    private fun parse(text: String, validateReferences: Boolean): ParseSnapshot {
        val mutableZoneSkills = mutableMapOf<String, MutableMap<CharacterClass, Array<String?>>>()
        var count = 0

        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            val parts = line.split('\t')
            if (parts.size < 4) continue

            val characterClass = classFromName(parts[0].trim()) ?: continue
            val locationName = parts[1].trim()
            if (validateReferences && AdventureDatabase.getByName(locationName) == null) continue

            val index = parts[2].trim().toIntOrNull() ?: continue
            if (index !in 1..6) continue

            val skillName = parseSkillName(parts[3].trim(), validateReferences) ?: continue

            val byClass = mutableZoneSkills.getOrPut(locationName) { mutableMapOf() }
            val skills = byClass.getOrPut(characterClass) { arrayOfNulls(6) }
            if (skills[index - 1] != null) continue
            skills[index - 1] = skillName
            count++
        }

        val zoneNames = mutableZoneSkills.keys.sorted()
        val zoneIndex = zoneNames.withIndex().associate { it.value to it.index }
        val mutableSkillLocations = mutableMapOf<String, MutableMap<CharacterClass, Int>>()

        for ((location, byClass) in mutableZoneSkills) {
            val zIdx = zoneIndex[location] ?: continue
            for ((characterClass, skills) in byClass) {
                skills.forEachIndexed { i, skill ->
                    if (skill == null) return@forEachIndexed
                    val encoded = zIdx * 6 + i
                    mutableSkillLocations.getOrPut(skill) { mutableMapOf() }[characterClass] = encoded
                }
            }
        }

        val frozenZoneSkills = mutableZoneSkills.mapValues { (_, byClass) ->
            byClass.mapValues { (_, skills) -> skills.copyOf() }
        }
        val frozenSkillLocations = mutableSkillLocations.mapValues { (_, byClass) ->
            byClass.toMap()
        }

        return ParseSnapshot(frozenZoneSkills, frozenSkillLocations, zoneNames, count)
    }

    private fun classFromName(name: String): CharacterClass? =
        CharacterClass.entries.firstOrNull {
            it.isStandardClass && it.displayName.equals(name, ignoreCase = true)
        }

    private fun parseSkillName(raw: String, validateReferences: Boolean): String? {
        val bracket = skillBracketPattern.find(raw.trim())
        if (bracket != null) {
            val id = bracket.groupValues[1].toIntOrNull()
            val namePart = bracket.groupValues[2].trim()
            if (id != null) {
                SkillDefinitionDatabase.getById(id)?.name?.let { return it }
            }
            if (validateReferences) {
                SkillDefinitionDatabase.getByName(namePart)?.name?.let { return it }
            } else if (namePart.isNotEmpty()) {
                return namePart
            }
            return null
        }
        return if (validateReferences) {
            SkillDefinitionDatabase.getByName(raw)?.name
        } else {
            raw.trim().takeIf { it.isNotEmpty() }
        }
    }
}
