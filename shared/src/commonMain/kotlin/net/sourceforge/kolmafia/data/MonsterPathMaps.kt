package net.sourceforge.kolmafia.data

/**
 * Desktop [MonsterDatabase] path boss remaps for ASH [get_monster_mapping].
 * Keys are desktop path display names (plus common mobile aliases).
 * Values: target monster name, `#id` for id remaps, or null for none.
 */
object MonsterPathMaps {
    private data class Entry(val from: String, val to: String?)

    private val maps: Map<String, List<Entry>> = buildMaps()

    fun getMonsterPathMap(pathName: String): Map<String, String?> {
        val entries = maps[pathName] ?: maps.entries.firstOrNull {
            it.key.equals(pathName, ignoreCase = true)
        }?.value ?: return emptyMap()
        return entries.associate { it.from to it.to }
    }

    fun resolveMappedMonster(pathName: String, fromName: String): MonsterDefinition? {
        val to = getMonsterPathMap(pathName)[fromName] ?: return null
        if (to.startsWith("#")) {
            return MonsterDatabase.getById(to.removePrefix("#").toIntOrNull() ?: return null)
        }
        return MonsterDatabase.getByName(to)
    }

    private fun buildMaps(): Map<String, List<Entry>> {
        val result = linkedMapOf<String, List<Entry>>()
        fun put(name: String, vararg entries: Pair<String, String?>) {
            result[name] = entries.map { Entry(it.first, it.second) }
        }
        fun alias(canonical: String, vararg aliases: String) {
            val body = result[canonical] ?: return
            for (a in aliases) result[a] = body
        }

        put(
            "You, Robot",
            "Boss Bat" to "Boss Bot",
            "Knob Goblin King" to "Gobot King",
            "Bonerdagon" to "Robonerdagon",
            "Groar" to "Groarbot",
            "Dr. Awkward" to "Tobias J. Saibot",
            "Lord Spookyraven" to "Lord Cyberraven",
            "Protector Spectre" to "Protector S. P. E. C. T. R. E.",
            "The Big Wisniewski" to "The Artificial Wisniewski",
            "The Man" to "The Android",
            "Naughty Sorceress" to "Nautomatic Sorceress",
            "Naughty Sorceress (2)" to null,
            "Naughty Sorceress (3)" to null,
        )
        put(
            "Path of the Plumber",
            "Boss Bat" to "Koopa Paratroopa",
            "Knob Goblin King" to "Hammer Brother",
            "Bonerdagon" to "Very Dry Bones",
            "Groar" to "Angry Sun",
            "Dr. Awkward" to "Birdo",
            "Lord Spookyraven" to "King Boo",
            "Protector Spectre" to "Kamek",
            "The Big Wisniewski" to "#2172",
            "The Man" to "#2173",
            "Naughty Sorceress" to "Wa%playername/lowercase%",
            "Naughty Sorceress (2)" to null,
            "Naughty Sorceress (3)" to null,
        )
        alias("Path of the Plumber", "A Pocket Guide to Loathing")
        put(
            "Dark Gyffte",
            "Boss Bat" to "Steve Belmont",
            "Knob Goblin King" to "Ricardo Belmont",
            "Bonerdagon" to "Jayden Belmont",
            "Groar" to "Sharona",
            "Dr. Awkward" to "Travis Belmont",
            "Lord Spookyraven" to "Greg Dagreasy",
            "Protector Spectre" to "Sylvia Belgrande",
            "The Big Wisniewski" to "Jake Norris",
            "The Man" to "Chad Alacarte",
            "Your Shadow" to "Your Lack of Reflection",
            "Naughty Sorceress" to "%alucard%",
            "Naughty Sorceress (2)" to null,
            "Naughty Sorceress (3)" to null,
        )
        put(
            "Pocket Familiars",
            "Boss Bat" to "#2050",
            "Knob Goblin King" to "#2051",
            "Bonerdagon" to "#2052",
            "Groar" to "#2053",
            "Dr. Awkward" to "#2054",
            "Lord Spookyraven" to "#2055",
            "Protector Spectre" to "#2056",
            "The Big Wisniewski" to "#2057",
            "The Man" to "#2058",
            "Naughty Sorceress" to "#2059",
            "Naughty Sorceress (2)" to null,
            "Naughty Sorceress (3)" to null,
        )
        put(
            "Heavy Rains",
            "Boss Bat" to "Aquabat",
            "Knob Goblin King" to "Aquagoblin",
            "Bonerdagon" to "Auqadargon",
            "Groar" to "Gurgle",
            "Dr. Awkward" to "Dr. Aquard",
            "Lord Spookyraven" to "Lord Soggyraven",
            "Protector Spectre" to "Protector Spurt",
            "The Big Wisniewski" to "Big Wisnaqua",
            "The Man" to "The Aquaman",
            "Naughty Sorceress" to "The Rain King",
            "Naughty Sorceress (2)" to null,
            "Naughty Sorceress (3)" to null,
        )
        put(
            "Actually Ed the Undying",
            "Boss Bat" to "Boss Bat?",
            "Knob Goblin King" to "new Knob Goblin King",
            "Bonerdagon" to "Donerbagon",
            "Groar" to "Your winged yeti",
            "Naughty Sorceress" to "You the Adventurer",
            "Naughty Sorceress (2)" to null,
            "Naughty Sorceress (3)" to null,
        )
        put(
            "Wildfire",
            "Boss Bat" to "Blaze Bat",
            "Knob Goblin King" to "fired-up Knob Goblin King",
            "Bonerdagon" to "Burnerdagon",
            "Groar" to "Groar, Except Hot",
            "Dr. Awkward" to "Dr. Awkward, who is on fire",
            "Lord Spookyraven" to "Lord Sootyraven",
            "Protector Spectre" to "Protector Spectre (Wildfire)",
            "The Big Wisniewski" to "The Big Ignatowicz",
            "The Man" to "The Man on Fire",
            "Naughty Sorceress" to "The Naughty Scorcheress",
            "Naughty Sorceress (2)" to null,
            "Naughty Sorceress (3)" to null,
        )
        put(
            "Fall of the Dinosaurs",
            "Boss Bat" to "two-headed pteranodon with a two-headed bat inside it",
            "Knob Goblin King" to "goblodocus",
            "Bonerdagon" to "T-Rex who ate the Bonerdagon",
            "Groar" to "refrigeradon",
            "Dr. Awkward" to "suruasaurus",
            "Lord Spookyraven" to "herd of well-fed microraptors",
            "Protector Spectre" to "protoceratops spectre",
            "The Big Wisniewski" to "Slackiosaurus",
            "The Man" to "Oligarcheopteryx",
            "Naughty Sorceress" to "Naughty Saursaurus",
            "Naughty Sorceress (2)" to null,
            "Naughty Sorceress (3)" to null,
        )
        put(
            "Avatar of Shadows Over Loathing",
            "Boss Bat" to "two-headed shadow bat",
            "Knob Goblin King" to "goblin king's shadow",
            "Bonerdagon" to "shadowboner shadowdagon",
            "Groar" to "shadow of groar",
            "Dr. Awkward" to "W. Odah's Shadow",
            "Lord Spookyraven" to "shadow Lord Spookyraven",
            "Protector Spectre" to "corruptor shadow",
            "The Big Wisniewski" to "shadow of the 1960s",
            "The Man" to "shadow of the 1980s",
        )
        alias("Avatar of Shadows Over Loathing", "Shadows Over Loathing")
        put(
            "Legacy of Loathing",
            "Boss Bat" to "Classic Boss Bat",
            "Knob Goblin King" to "Weirdly Scrawny Knob Goblin King",
            "Bonerdagon" to "Orignial Bonerdagon",
            "Groar" to "Flock of Groars?",
            "Dr. Awkward" to "Jr. Awkwarj",
            "Lord Spookyraven" to "Little Lord Spookyraven",
            "Protector Spectre" to "Protector Spectre Candidate",
            "The Big Wisniewski" to "The Little Wisniewski",
            "The Man" to "The Boy",
        )
        put(
            "WereProfessor",
            "Boss Bat" to "Boss Beast",
            "Knob Goblin King" to "Knob Goblin Beast",
            "Bonerdagon" to "Curséd Bonerdagon",
            "Groar" to "Just Groar",
            "Dr. Awkward" to "Were-Dr. Awkwarder, ew",
            "Lord Spookyraven" to "Lord Beastlyraven",
            "Protector Spectre" to "Protector Beast",
            "The Big Wisniewski" to "The Beast Wisniewski",
            "The Man" to "The Beastman",
            "Naughty Sorceress" to "The Naughty Wolferess",
            "Naughty Sorceress (2)" to null,
            "Naughty Sorceress (3)" to null,
        )
        put(
            "11,037 Leagues Under the Sea",
            "Naughty Sorceress" to "Nautical Seaceress",
            "Naughty Sorceress (2)" to null,
            "Naughty Sorceress (3)" to null,
        )
        alias("11,037 Leagues Under the Sea", "Under the Sea")
        put(
            "Adventurer Meats World",
            "beefy bodyguard bat" to "beef bodyguard bat",
            "Boss Bat" to "Basted Boss Bat",
            "Knob Goblin King" to "Gabogooblin King",
            "Bonerdagon" to "The Maety Bonerdagon",
            "Groar" to "Groarst",
            "Dr. Awkward" to "Feeble Dr. Awkward, El Beef",
            "Lord Spookyraven" to "Lard Spookyraven",
            "Protector Spectre" to "Protector Speck-ter",
            "The Big Wisniewski" to "The Big Mac Wisniewski",
            "The Man" to "The Manwich",
            "Naughty Sorceress" to "Naughty Sorceress, all sausage",
            "Naughty Sorceress (2)" to null,
            "Naughty Sorceress (3)" to null,
        )
        return result
    }
}
