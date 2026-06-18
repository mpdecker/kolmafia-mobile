package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.character.CharacterClass
import net.sourceforge.kolmafia.data.JourneymanDatabase

private val JOURNEY_USAGE =
    "Usage: journey zones [SC | TT | PM | S | AT | DB]| find [all | SC | TT | PM | S | AT | DB] <skill>"

internal fun GameRuntimeLibrary.cliJourney(params: String, print: (String) -> Unit) {
    val tokens = params.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (tokens.isEmpty()) {
        print(JOURNEY_USAGE)
        return
    }
    if (!JourneymanDatabase.isLoaded) {
        print("Journeyman data is not loaded.")
        return
    }
    when (tokens[0].lowercase()) {
        "zones" -> journeyZones(tokens, print)
        "find" -> journeyFind(tokens, print)
        else -> print(JOURNEY_USAGE)
    }
}

private fun journeyZones(tokens: List<String>, print: (String) -> Unit) {
    if (tokens.size < 2) {
        print("Specify a class: SC, TT, PM, S, DB, AT.")
        return
    }
    val characterClass = parseJourneyClass(tokens[1]) ?: run {
        print("I don't know what '${tokens[1]}' is.")
        return
    }
    print("Zone | Skill 1 (4 turns) | Skill 2 (8) | Skill 3 (12) | Skill 4 (16) | Skill 5 (20) | Skill 6 (24)")
    for (zone in JourneymanDatabase.zoneNames) {
        val skills = JourneymanDatabase.skillsForZone(zone, characterClass) ?: continue
        print(
            buildString {
                append(zone)
                append(" | ")
                append(skills.joinToString(" | ") { it.orEmpty() })
            },
        )
    }
}

private fun GameRuntimeLibrary.journeyFind(tokens: List<String>, print: (String) -> Unit) {
    val all = tokens.size >= 2 && tokens[1].equals("all", ignoreCase = true)
    val classes: List<CharacterClass>
    val skillWords: List<String>

    when {
        all -> {
            classes = CharacterClass.entries.filter { it.isStandardClass }
            skillWords = tokens.drop(2)
        }
        tokens.size >= 3 -> {
            val characterClass = parseJourneyClass(tokens[1]) ?: run {
                print("I don't know what '${tokens[1]}' is.")
                return
            }
            classes = listOf(characterClass)
            skillWords = tokens.drop(2)
        }
        else -> {
            print("Specify a class: SC, TT, PM, S, DB, AT.")
            return
        }
    }

    if (skillWords.isEmpty()) {
        print(JOURNEY_USAGE)
        return
    }

    val skillQuery = skillWords.joinToString(" ")
    val skill = gameDatabase?.skill(skillQuery)
        ?: run {
            print("I don't know a skill named \"$skillQuery\"")
            return
        }

    val locations = JourneymanDatabase.locationsForSkill(skill.name)
    if (locations.isEmpty()) {
        print("The \"${skill.name}\" skill is not available to Journeymen.")
        return
    }

    for (characterClass in classes) {
        val encoded = locations[characterClass] ?: continue
        val zone = JourneymanDatabase.zoneNameForEncoded(encoded)
        if (zone == null) {
            print("A Journeyman ${characterClass.displayName} can learn \"${skill.name}\" in an unknown zone.")
            continue
        }
        val turns = JourneymanDatabase.turnsForEncoded(encoded)
        print("A Journeyman ${characterClass.displayName} can learn \"${skill.name}\" after $turns turns in $zone.")
    }
}

private fun parseJourneyClass(abbrev: String): CharacterClass? = when (abbrev.lowercase()) {
    "sc" -> CharacterClass.SEAL_CLUBBER
    "tt" -> CharacterClass.TURTLE_TAMER
    "pm" -> CharacterClass.PASTAMANCER
    "s" -> CharacterClass.SAUCEROR
    "db" -> CharacterClass.DISCO_BANDIT
    "at" -> CharacterClass.ACCORDION_THIEF
    else -> null
}
