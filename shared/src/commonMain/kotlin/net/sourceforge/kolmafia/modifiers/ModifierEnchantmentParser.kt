package net.sourceforge.kolmafia.modifiers

import net.sourceforge.kolmafia.character.CharacterClass

/**
 * Parses KoL item/effect enchantment description lines into modifiers.txt tokens.
 * Ported from desktop [ModifierDatabase.parseModifier] and modifier enum desc patterns.
 */
object ModifierEnchantmentParser {

    private class DescPattern<T>(val modifier: T, vararg val patterns: Regex)

    private val DOUBLE_DESC: List<DescPattern<DoubleModifier>> = listOf(
        DescPattern(DoubleModifier.FAMILIAR_WEIGHT, Regex("([+-]\\d+) (to )?Familiar Weight")),
        DescPattern(DoubleModifier.MONSTER_LEVEL, Regex("([+-]\\d+) to Monster Level"), Regex("Monster Level ([+-]\\d+)")),
        DescPattern(DoubleModifier.INITIATIVE, Regex("Combat Initiative ([+-]\\d+)%"), Regex("([+-]\\d+)% Combat Initiative")),
        DescPattern(DoubleModifier.EXPERIENCE, Regex("([+-]\\d+) Stat.*Per Fight")),
        DescPattern(DoubleModifier.ITEMDROP, Regex("([+-]\\d+)% Item Drops? [Ff]rom Monsters$")),
        DescPattern(DoubleModifier.MEATDROP, Regex("([+-]\\d+)% Meat from Monsters")),
        DescPattern(DoubleModifier.DAMAGE_ABSORPTION, Regex("Damage Absorption ([+-]\\d+)")),
        DescPattern(DoubleModifier.DAMAGE_REDUCTION, Regex("Damage Reduction: ([+-]?\\d+)")),
        DescPattern(DoubleModifier.MANA_COST, Regex("([+-]\\d+) MP to use Skills$")),
        DescPattern(DoubleModifier.MOX, Regex("Moxie ([+-]\\d+)$"), Regex("([+-]\\d+) Moxie$")),
        DescPattern(DoubleModifier.MOX_PCT, Regex("Moxie ([+-]\\d+)%"), Regex("([+-]\\d+)% Moxie")),
        DescPattern(DoubleModifier.MUS, Regex("Muscle ([+-]\\d+)$"), Regex("([+-]\\d+) Muscle$")),
        DescPattern(DoubleModifier.MUS_PCT, Regex("Muscle ([+-]\\d+)%"), Regex("([+-]\\d+)% Muscle")),
        DescPattern(DoubleModifier.MYS, Regex("Mysticality ([+-]\\d+)$"), Regex("([+-]\\d+) Mysticality$")),
        DescPattern(DoubleModifier.MYS_PCT, Regex("Mysticality ([+-]\\d+)%"), Regex("([+-]\\d+)% Mysticality")),
        DescPattern(DoubleModifier.HP, Regex("Maximum HP ([+-]\\d+)$")),
        DescPattern(DoubleModifier.HP_PCT, Regex("Maximum HP ([+-]\\d+)%")),
        DescPattern(DoubleModifier.MP, Regex("Maximum MP ([+-]\\d+)$")),
        DescPattern(DoubleModifier.MP_PCT, Regex("Maximum MP ([+-]\\d+)%")),
        DescPattern(DoubleModifier.WEAPON_DAMAGE, Regex("Weapon Damage ([+-]\\d+)$"), Regex("([+-]\\d+) Weapon Damage")),
        DescPattern(DoubleModifier.RANGED_DAMAGE, Regex("Ranged Damage ([+-]\\d+)$"), Regex("([+-]\\d+) Ranged Damage")),
        DescPattern(DoubleModifier.SPELL_DAMAGE, Regex("Spell Damage ([+-]\\d+)$"), Regex("([+-]\\d+) Spell Damage")),
        DescPattern(DoubleModifier.SPELL_DAMAGE_PCT, Regex("Spell Damage ([+-][\\d.]+)%"), Regex("([+-][\\d.]+)% Spell Damage")),
        DescPattern(DoubleModifier.COLD_DAMAGE, Regex("^([+-]\\d+) <font color=blue>Cold Damage<")),
        DescPattern(DoubleModifier.HOT_DAMAGE, Regex("^([+-]\\d+) <font color=red>Hot Damage<")),
        DescPattern(DoubleModifier.SLEAZE_DAMAGE, Regex("^([+-]\\d+) <font color=blueviolet>Sleaze Damage<")),
        DescPattern(DoubleModifier.SPOOKY_DAMAGE, Regex("^([+-]\\d+) <font color=gray>Spooky Damage<")),
        DescPattern(DoubleModifier.STENCH_DAMAGE, Regex("^([+-]\\d+) <font color=green>Stench Damage<")),
        DescPattern(DoubleModifier.COLD_SPELL_DAMAGE, Regex("^([+-]\\d+) (Damage )?to <font color=blue>Cold Spells</font>")),
        DescPattern(DoubleModifier.HOT_SPELL_DAMAGE, Regex("^([+-]\\d+) (Damage )?to (<font color=red>)?Hot Spells(</font>)?")),
        DescPattern(DoubleModifier.SLEAZE_SPELL_DAMAGE, Regex("^([+-]\\d+) (Damage )?to <font color=blueviolet>Sleaze Spells</font>")),
        DescPattern(DoubleModifier.SPOOKY_SPELL_DAMAGE, Regex("^([+-]\\d+) (Damage )?to <font color=gray>Spooky Spells</font>")),
        DescPattern(DoubleModifier.STENCH_SPELL_DAMAGE, Regex("^([+-]\\d+) (Damage )?to <font color=green>Stench Spells</font>")),
        DescPattern(DoubleModifier.FUMBLE, Regex("(\\d+)x chance of Fumble")),
        DescPattern(DoubleModifier.ADVENTURES, Regex("([+-]\\d+) Adventure\\(s\\) per day( when equipped)?")),
        DescPattern(DoubleModifier.FAMILIAR_WEIGHT_PCT, Regex("([+-]\\d+)% Familiar Weight")),
        DescPattern(DoubleModifier.WEAPON_DAMAGE_PCT, Regex("Weapon Damage ([+-]\\d+)%")),
        DescPattern(DoubleModifier.RANGED_DAMAGE_PCT, Regex("Ranged Damage ([+-]\\d+)%")),
        DescPattern(DoubleModifier.STACKABLE_MANA_COST, Regex("([+-]\\d+) MP to use Skills$")),
        DescPattern(DoubleModifier.HOBO_POWER, Regex("([+-]\\d+) Hobo Power")),
        DescPattern(DoubleModifier.CRITICAL_PCT, Regex("([+-]\\d+)% [Cc]hance of Critical Hit")),
        DescPattern(DoubleModifier.PVP_FIGHTS, Regex("([+-]\\d+) PvP [Ff]ight\\(s\\) per day( when equipped)?")),
        DescPattern(DoubleModifier.FOODDROP, Regex("([+-]\\d+)% Food Drops? [Ff]rom Monsters$")),
        DescPattern(DoubleModifier.BOOZEDROP, Regex("([+-]\\d+)% Booze Drops? [Ff]rom Monsters$")),
        DescPattern(DoubleModifier.HATDROP, Regex("([+-]\\d+)% Hat(?:/Pants)? Drops? [Ff]rom Monsters$")),
        DescPattern(DoubleModifier.WEAPONDROP, Regex("([+-]\\d+)% Weapon Drops? [Ff]rom Monsters$")),
        DescPattern(DoubleModifier.OFFHANDDROP, Regex("([+-]\\d+)% Off-[Hh]and Drops? [Ff]rom Monsters$")),
        DescPattern(DoubleModifier.SHIRTDROP, Regex("([+-]\\d+)% Shirt Drops? [Ff]rom Monsters$")),
        DescPattern(DoubleModifier.PANTSDROP, Regex("([+-]\\d+)% (?:Hat/)?Pants Drops? [Ff]rom Monsters$")),
        DescPattern(DoubleModifier.ACCESSORYDROP, Regex("([+-]\\d+)% Accessory Drops? [Ff]rom Monsters$")),
        DescPattern(DoubleModifier.SLIME_HATES_IT, Regex("Slime( Really)? Hates (It|You)")),
        DescPattern(DoubleModifier.SPELL_CRITICAL_PCT, Regex("([+-]\\d+)% [cC]hance of Spell Critical Hit")),
        DescPattern(DoubleModifier.MUS_EXPERIENCE, Regex("([+-]\\d+) Muscle Stat.*Per Fight")),
        DescPattern(DoubleModifier.MYS_EXPERIENCE, Regex("([+-]\\d+) Mysticality Stat.*Per Fight")),
        DescPattern(DoubleModifier.MOX_EXPERIENCE, Regex("([+-]\\d+) Moxie Stat.*Per Fight")),
        DescPattern(DoubleModifier.CANDYDROP, Regex("([+-]\\d+)% Candy Drops? [Ff]rom Monsters$")),
        DescPattern(DoubleModifier.DB_COMBAT_DAMAGE, Regex("([+-]\\d+) damage to Disco Bandit Combat Skills"), Regex("([+-]\\d+) Disco Bandit Skill Damage")),
        DescPattern(DoubleModifier.SOMBRERO_BONUS, Regex("([+-]\\d+) lbs?\\. of Sombrero")),
        DescPattern(DoubleModifier.FAMILIAR_EXP, Regex("([+-]\\d+) Familiar Experience")),
        DescPattern(DoubleModifier.PICKPOCKET_CHANCE, Regex("([+-]\\d+)% Pickpocket Chance")),
        DescPattern(DoubleModifier.COMBAT_MANA_COST, Regex("([+-]\\d+) MP to use Skills \\(in-combat only\\)")),
        DescPattern(DoubleModifier.MUS_EXPERIENCE_PCT, Regex("([+-]\\d+)% to all Muscle Gains")),
        DescPattern(DoubleModifier.MYS_EXPERIENCE_PCT, Regex("([+-]\\d+)% to all Mysticality Gains")),
        DescPattern(DoubleModifier.MOX_EXPERIENCE_PCT, Regex("([+-]\\d+)% to all Moxie Gains")),
        DescPattern(DoubleModifier.MINSTREL_LEVEL, Regex("([+-]\\d+) to Minstrel Level"), Regex("Minstrel Level ([+-]\\d+)")),
        DescPattern(DoubleModifier.MUS_LIMIT, Regex("Base Muscle Limited to (\\d+)")),
        DescPattern(DoubleModifier.MYS_LIMIT, Regex("Base Mysticality Limited to (\\d+)")),
        DescPattern(DoubleModifier.MOX_LIMIT, Regex("Base Moxie Limited to (\\d+)")),
        DescPattern(DoubleModifier.SONG_DURATION, Regex("Song Duration: ([+-]\\d+) Adventures")),
        DescPattern(DoubleModifier.SMITHSNESS, Regex("([+-]\\d+) Smithsness")),
        DescPattern(DoubleModifier.REDUCE_ENEMY_DEFENSE, Regex("Reduce enemy defense by (\\d+)%")),
        DescPattern(DoubleModifier.POOL_SKILL, Regex("([+-]\\d+) Pool Skill")),
        DescPattern(DoubleModifier.FAMILIAR_DAMAGE, Regex("([+-]\\d+) to Familiar Damage"), Regex("Familiar Damage ([+-]\\d+)")),
        DescPattern(DoubleModifier.GEARDROP, Regex("([+-]\\d+)% Gear Drops? [Ff]rom Monsters$")),
        DescPattern(DoubleModifier.MAXIMUM_HOOCH, Regex("([+-]\\d+) Maximum Hooch")),
        DescPattern(DoubleModifier.CRIMBOT_POWER, Regex("([+-]\\d+) Crimbot Outfit Power")),
        DescPattern(DoubleModifier.RANDOM_MONSTER_MODIFIERS, Regex("([+-]\\d+) Random Monster Modifier")),
        DescPattern(DoubleModifier.LUCK, Regex("([+-]\\d+) Luck")),
        DescPattern(DoubleModifier.OTHELLO_SKILL, Regex("([+-]\\d+) Othello Skill")),
        DescPattern(DoubleModifier.DISCO_STYLE, Regex("([+-]\\d+) Disco Style")),
        DescPattern(DoubleModifier.ROLLOVER_EFFECT_DURATION, Regex("Grants (\\d+) Adventures of <b>.*?</b> at Rollover")),
        DescPattern(DoubleModifier.FISHING_SKILL, Regex("([+-]\\d+) Fishing Skill")),
        DescPattern(DoubleModifier.ADDITIONAL_SONG, Regex("Keep (\\d+) additional song in your head")),
        DescPattern(DoubleModifier.SPRINKLES, Regex("([+-]\\d+)% Sprinkles from Monsters")),
        DescPattern(DoubleModifier.ABSORB_ADV, Regex("([+-]\\d+) Adventures when you absorb an item")),
        DescPattern(DoubleModifier.ABSORB_STAT, Regex("([+-]\\d+) Stats when you absorb an item")),
        DescPattern(DoubleModifier.RUBEE_DROP, Regex("FantasyRealm enemies will drop (\\d+) extra Rubee")),
        DescPattern(DoubleModifier.KRUEGERAND_DROP, Regex("Lets you find (\\d+)% more Kruegerands")),
        DescPattern(DoubleModifier.WARBEAR_ARMOR_PENETRATION, Regex("([+-]\\d+) WarBear Armor Penetration")),
        DescPattern(DoubleModifier.PP, Regex("([+-]\\d+) Max(imum)? Power Point")),
        DescPattern(DoubleModifier.DRIPPY_DAMAGE, Regex("([+-]\\d+) Damage vs. creatures of The Drip"), Regex("([+-]\\d+) Damage against Drip creatures")),
        DescPattern(DoubleModifier.WATER, Regex("Collect (\\d+) water per adventure")),
        DescPattern(DoubleModifier.SPLEEN_DROP, Regex("([+-]\\d+)% Spleen Item Drops? [Ff]rom Monsters$")),
        DescPattern(DoubleModifier.POTION_DROP, Regex("([+-]\\d+)% Potion Drops? [Ff]rom Monsters$")),
        DescPattern(DoubleModifier.SAUCE_SPELL_DAMAGE, Regex("Sauce Spell Damage ([+-]\\d+)$"), Regex("([+-]\\d+) Sauce Spell Damage")),
        DescPattern(DoubleModifier.MONSTER_LEVEL_PERCENT, Regex("([+-]\\d+)% Monster Level")),
        DescPattern(DoubleModifier.STOMACH_CAPACITY, Regex("(.*) Stomach Capacity")),
        DescPattern(DoubleModifier.LIVER_CAPACITY, Regex("(.*) Liver Capacity")),
        DescPattern(DoubleModifier.SPLEEN_CAPACITY, Regex("(.*) Spleen Capacity")),
        DescPattern(DoubleModifier.FREE_RESTS, Regex("Rest Without Spending an Adventure \\((\\d+)x / day\\)")),
        DescPattern(DoubleModifier.ELF_WARFARE_EFFECTIVENESS, Regex("([+-]\\d+) Elf Warfare Effectiveness")),
        DescPattern(DoubleModifier.PIRATE_WARFARE_EFFECTIVENESS, Regex("([+-]\\d+) Pirate Warfare Effectiveness")),
        DescPattern(DoubleModifier.COMBAT_ITEM_DAMAGE_PCT, Regex("Combat items deal ([+-]\\d+)% more damage")),
        DescPattern(DoubleModifier.AVOID_ATTACK, Regex("The first attack against you will always miss")),
        DescPattern(DoubleModifier.BUGBEAR_DAMAGE, Regex("([+-]\\d+)% Damage vs. Bugbears")),
        DescPattern(DoubleModifier.WEREWOLF_DAMAGE, Regex("([+-]\\d+)% Damage vs. Werewolves")),
        DescPattern(DoubleModifier.ZOMBIE_DAMAGE, Regex("([+-]\\d+)% Damage vs. Zombies")),
        DescPattern(DoubleModifier.GHOST_DAMAGE, Regex("([+-]\\d+) Damage vs. Ghosts")),
        DescPattern(DoubleModifier.VAMPIRE_DAMAGE, Regex("([+-]\\d+)% Damage vs. Vampires")),
        DescPattern(DoubleModifier.SKELETON_DAMAGE, Regex("([+-]\\d+)% Damage vs. Skeletons")),
        DescPattern(DoubleModifier.UNDEAD_DAMAGE, Regex("([+-]\\d+) Damage vs. Undead")),
        DescPattern(DoubleModifier.MERKIN_DAMAGE, Regex("([+-]\\d+)% Damage vs. Mer-kin")),
        DescPattern(DoubleModifier.ORC_DAMAGE, Regex("([+-]\\d+) Damage vs. Orcs")),
        DescPattern(DoubleModifier.SEAL_DAMAGE, Regex("([+-]\\d+)% Damage vs. Seals")),
        DescPattern(DoubleModifier.RAM, Regex("([+-]\\d+) RAM")),
    )

    private val BOOLEAN_DESC: List<DescPattern<BooleanModifier>> = listOf(
        DescPattern(BooleanModifier.SOFTCORE, Regex("This item cannot be equipped while in Hardcore")),
        DescPattern(BooleanModifier.NEVER_FUMBLE, Regex("Never Fumble")),
        DescPattern(BooleanModifier.WEAKENS, Regex("Successful hit weakens opponent")),
        DescPattern(BooleanModifier.FOUR_SONGS, Regex("Allows you to keep 4 songs in your head instead of 3")),
        DescPattern(BooleanModifier.ADVENTURE_UNDERWATER, Regex("Lets you [bB]reathe [uU]nderwater")),
        DescPattern(BooleanModifier.UNDERWATER_FAMILIAR, Regex("Lets your Familiar Breathe Underwater")),
        DescPattern(BooleanModifier.LASTS_ONE_DAY, Regex("This item will disappear at the end of the day")),
        DescPattern(BooleanModifier.ATTACKS_CANT_MISS, Regex("Regular Attacks Can't Miss"), Regex("Cannot miss")),
        DescPattern(BooleanModifier.EXTRA_PICKPOCKET, Regex("Gives you an additional Pickpocketing attempt"), Regex("1 Additional Pickpocket Attempt")),
        DescPattern(BooleanModifier.NEGATIVE_STATUS_RESIST, Regex("75% Chance of Preventing Negative Status Attacks")),
        DescPattern(BooleanModifier.WEAKENS_MONSTER_ON_CRITICAL_HIT, Regex("Weakens Monster on Critical Hit")),
    )

    private val STRING_DESC: List<DescPattern<StringModifier>> = listOf(
        DescPattern(StringModifier.CLASS, Regex("Only (.*?) may use this item"), Regex("Bonus for (.*?) only"), Regex("Bonus&nbsp;for&nbsp;(.*?)&nbsp;only")),
        DescPattern(StringModifier.INTRINSIC_EFFECT, Regex("Intrinsic Effect: <a.*?><font color=blue>(.*)</font></a>")),
        DescPattern(StringModifier.JIGGLE, Regex("Jiggle: *(.*?)$")),
        DescPattern(StringModifier.AVATAR, Regex("Makes you look like (?:a |an |the )?(.++)(?<!doctor|gross doctor)"), Regex("Te hace ver como un (.++)")),
        DescPattern(StringModifier.ROLLOVER_EFFECT, Regex("Adventures of <b><a.*?>(.*)</a></b> at Rollover")),
        DescPattern(StringModifier.SKILL, Regex("Grants Skill:.*?<b>(.*?)</b>")),
        DescPattern(StringModifier.CONDITIONAL_SKILL_EQUIPPED, Regex("Grants \"(.*?)\" Combat Skill")),
    )

    private val BITMAP_DESC: List<DescPattern<BitmapModifier>> = listOf(
        DescPattern(BitmapModifier.SURGEONOSITY, Regex("Makes you look like a doctor"), Regex("Makes you look like a gross doctor")),
        DescPattern(BitmapModifier.CLOWNINESS, Regex("Makes you look (\\d+)% clowny")),
    )

    private val ALL_ATTR = Regex("^All Attributes ([+-]\\d+)$")
    private val ALL_ATTR_PCT = Regex("^All Attributes ([+-]\\d+)%$")
    private val CLASS_NBSP = Regex("Bonus&nbsp;for&nbsp;(.*)&nbsp;only")
    private val COMBAT = Regex("Monsters (?:are|will be) (.*) attracted to you")
    private val HP_MP = Regex("^Maximum HP/MP ([+-]\\d+)$")
    private val REGEN = Regex("Regenerate (\\d*)-?(\\d*)? ([HM]P)( and .*)? per [aA]dventure$")
    private val RESISTANCE_LEVEL = Regex("Resistance \\(([+-]\\d+)\\)")

    private val SKILL = Regex("Grants Skill:.*?<b>(.*?)</b>")
    private val DR = Regex("Damage Reduction: (<b>)?([+-]?\\d+)(</b>)?")
    private val SINGLE = Regex("You may not equip more than one of these at a time")
    private val SOFTCORE = Regex("This item cannot be equipped while in Hardcore")
    private val ITEM_DROPPER = Regex("Occasional Hilarity")
    private val LASTS_ONE_DAY = Regex("This item will disappear at the end of the day")
    private val FREE_PULL = Regex("Free pull from Hagnk's")
    private val NO_PULL = Regex("Cannot be pulled from Hagnk's")
    private val EFFECT = Regex("Effect: <b><a([^>]*)>([^<]*)</a></b>")
    private val EFFECT_DURATION = Regex("</a></b> \\(([\\d]*) Adventures?\\)")
    private val SONG_DURATION = Regex("Song Duration: <b>([\\d]*) Adventures</b>")
    private val LAST_AVAILABLE = Regex("<![-—]+ Last Available Date: (\\d{4}-\\d{2}) [-—]+>")

    private val COMBAT_RATE_DESCRIPTIONS = mapOf(
        "incredibly very much more" to "+25",
        "<i>way</i> more" to "+20",
        "significantly more" to "+15",
        "much more" to "+10",
        "more" to "+5",
        "slightly less" to "-3",
        "less" to "-5",
        "more than a little less" to "-7",
        "quite a bit less" to "-9",
        "much less" to "-10",
        "very much less" to "-11",
        "significantly less" to "-15",
        "very very very much less" to "-20",
        "<i>way</i> less" to "-20",
        "incredibly very much less" to "-25",
    )

    private val CLASS_PLURALS = listOf(
        listOf("Seal Clubber", "Seal Clubbers", "Seal&nbsp;Clubbers"),
        listOf("Turtle Tamer", "Turtle Tamers", "Turtle&nbsp;Tamers"),
        listOf("Pastamancer", "Pastamancers"),
        listOf("Sauceror", "Saucerors"),
        listOf("Disco Bandit", "Disco Bandits", "Disco&nbsp;Bandits"),
        listOf("Accordion Thief", "Accordion Thieves", "Accordion&nbsp;Thieves"),
    )

    fun parseModifier(enchantment: String): String? {
        parseDoubleModifierFromDesc(enchantment)?.let { return it }
        parseBooleanModifierFromDesc(enchantment)?.let { return it }
        parseStringModifierFromDesc(enchantment)?.let { return it }
        parseBitmapModifierFromDesc(enchantment)?.let { return it }

        ALL_ATTR.find(enchantment)?.let { m ->
            val mod = m.groupValues[1]
            return listOf(
                "${DoubleModifier.MUS.tag}: $mod",
                "${DoubleModifier.MYS.tag}: $mod",
                "${DoubleModifier.MOX.tag}: $mod",
            ).joinToString(", ")
        }

        ALL_ATTR_PCT.find(enchantment)?.let { m ->
            val mod = m.groupValues[1]
            return listOf(
                "${DoubleModifier.MUS_PCT.tag}: $mod",
                "${DoubleModifier.MYS_PCT.tag}: $mod",
                "${DoubleModifier.MOX_PCT.tag}: $mod",
            ).joinToString(", ")
        }

        CLASS_NBSP.find(enchantment)?.let { m ->
            val className = depluralizeClassName(m.groupValues[1].replace("&nbsp;", " "))
            resolveClassName(className)?.let {
                return "${StringModifier.CLASS.tag}: \"$it\""
            }
        }

        COMBAT.find(enchantment)?.let { m ->
            val tag = if (!enchantment.contains("Underwater only")) {
                DoubleModifier.COMBAT_RATE.tag
            } else {
                "Combat Rate (Underwater)"
            }
            val rate = COMBAT_RATE_DESCRIPTIONS[m.groupValues[1]] ?: "+0"
            return "$tag: $rate"
        }

        HP_MP.find(enchantment)?.let { m ->
            val mod = m.groupValues[1]
            return "${DoubleModifier.HP.tag}: $mod, ${DoubleModifier.MP.tag}: $mod"
        }

        if (enchantment.contains("Regenerate")) {
            parseRegeneration(enchantment)?.let { return it }
        }

        if (enchantment.contains("Resistance")) {
            parseResistance(enchantment)?.let { return it }
        }

        if (enchantment.contains("Your familiar will always act in combat")) {
            return "${DoubleModifier.FAMILIAR_ACTION_BONUS.tag}: +100"
        }

        return null
    }

    fun parseStringModifier(enchantment: String): String? = parseStringModifierFromDesc(enchantment)

    internal fun parseDoubleModifierFromDesc(enchantment: String): String? {
        for (entry in DOUBLE_DESC) {
            for (pattern in entry.patterns) {
                val match = pattern.find(enchantment) ?: continue
                val tag = entry.modifier.tag
                if (match.groupValues.size <= 1) {
                    return "$tag: 1"
                }
                if (entry.modifier == DoubleModifier.SLIME_HATES_IT) {
                    return if (match.groups[1]?.value.isNullOrEmpty()) {
                        "Slime Hates It: +1"
                    } else {
                        "Slime Hates It: +2"
                    }
                }
                return "$tag: ${match.groupValues[1].trim()}"
            }
        }
        return null
    }

    internal fun parseBooleanModifierFromDesc(enchantment: String): String? {
        for (entry in BOOLEAN_DESC) {
            for (pattern in entry.patterns) {
                val match = pattern.find(enchantment) ?: continue
                val tag = entry.modifier.tag
                if (match.groupValues.size <= 1) {
                    return tag
                }
                return "$tag: ${match.groupValues[1].trim()}"
            }
        }
        return null
    }

    internal fun parseStringModifierFromDesc(enchantment: String): String? {
        for (entry in STRING_DESC) {
            for (pattern in entry.patterns) {
                val match = pattern.find(enchantment) ?: continue
                val tag = entry.modifier.tag
                if (match.groupValues.size <= 1) {
                    return tag
                }
                var value = match.groupValues[1].trim()
                if (entry.modifier == StringModifier.CLASS) {
                    value = depluralizeClassName(value)
                }
                return "$tag: \"$value\""
            }
        }
        return null
    }

    internal fun parseBitmapModifierFromDesc(enchantment: String): String? {
        for (entry in BITMAP_DESC) {
            for (pattern in entry.patterns) {
                val match = pattern.find(enchantment) ?: continue
                val tag = entry.modifier.tag
                if (match.groupValues.size <= 1) {
                    if (entry.modifier == BitmapModifier.SURGEONOSITY) {
                        return "$tag: +1"
                    }
                    return tag
                }
                return "$tag: ${match.groupValues[1].trim()}"
            }
        }
        return null
    }

    fun parseDamageReduction(text: String): String? {
        if (!text.contains("Damage Reduction:")) return null
        var dr = 0
        for (match in DR.findAll(text)) {
            dr += match.groupValues[2].toIntOrNull() ?: 0
        }
        return "${DoubleModifier.DAMAGE_REDUCTION.tag}: $dr"
    }

    fun parseSkill(text: String): String? =
        SKILL.find(text)?.let { "${StringModifier.SKILL.tag}: \"${it.groupValues[1]}\"" }

    fun parseSingleEquip(text: String): String? =
        SINGLE.find(text)?.let { BooleanModifier.SINGLE.tag }

    fun parseSoftcoreOnly(text: String): String? =
        SOFTCORE.find(text)?.let { BooleanModifier.SOFTCORE.tag }

    fun parseDropsItems(text: String): String? =
        ITEM_DROPPER.find(text)?.let { BooleanModifier.DROPS_ITEMS.tag }

    fun parseLastsOneDay(text: String): String? =
        LASTS_ONE_DAY.find(text)?.let { BooleanModifier.LASTS_ONE_DAY.tag }

    fun parseFreePull(text: String): String? =
        FREE_PULL.find(text)?.let { BooleanModifier.FREE_PULL.tag }

    fun parseNoPull(text: String): String? =
        NO_PULL.find(text)?.let { BooleanModifier.NOPULL.tag }

    fun parseEffect(text: String): String? =
        EFFECT.find(text)?.let { match ->
            val name = match.groupValues[2].trim()
            "${StringModifier.EFFECT.tag}: \"$name\""
        }

    fun parseEffectDuration(text: String): String? =
        EFFECT_DURATION.find(text)?.let {
            "${DoubleModifier.EFFECT_DURATION.tag}: ${it.groupValues[1]}"
        }

    fun parseSongDuration(text: String): String? =
        SONG_DURATION.find(text)?.let {
            "${DoubleModifier.SONG_DURATION.tag}: ${it.groupValues[1]}"
        }

    fun parseLastAvailable(text: String): String? =
        LAST_AVAILABLE.find(text)?.let {
            "${StringModifier.LAST_AVAILABLE_DATE.tag}: \"${it.groupValues[1]}\""
        }

    private fun parseRegeneration(enchantment: String): String? {
        val match = REGEN.find(enchantment) ?: return null
        var min = match.groupValues[1]
        var max = match.groupValues[2].ifEmpty { min }
        val hp = match.groupValues[3] == "HP"
        val both = match.groupValues[4].isNotEmpty()
        if (max.isEmpty()) max = min

        return if (both) {
            listOf(
                "${DoubleModifier.HP_REGEN_MIN.tag}: $min",
                "${DoubleModifier.HP_REGEN_MAX.tag}: $max",
                "${DoubleModifier.MP_REGEN_MIN.tag}: $min",
                "${DoubleModifier.MP_REGEN_MAX.tag}: $max",
            ).joinToString(", ")
        } else if (hp) {
            "${DoubleModifier.HP_REGEN_MIN.tag}: $min, ${DoubleModifier.HP_REGEN_MAX.tag}: $max"
        } else {
            "${DoubleModifier.MP_REGEN_MIN.tag}: $min, ${DoubleModifier.MP_REGEN_MAX.tag}: $max"
        }
    }

    private fun parseResistanceLevel(enchantment: String): String {
        RESISTANCE_LEVEL.find(enchantment)?.let { return it.groupValues[1] }
        return when {
            enchantment.contains("Slight") -> "+1"
            enchantment.contains("So-So") -> "+2"
            enchantment.contains("Serious") -> "+3"
            enchantment.contains("Stupendous") -> "+4"
            enchantment.contains("Superhuman") -> "+5"
            enchantment.contains("Stunning") -> "+7"
            enchantment.contains("Sublime") -> "+9"
            else -> ""
        }
    }

    private fun parseResistance(enchantment: String): String? {
        val level = parseResistanceLevel(enchantment)
        val all = enchantment.contains("All Elements")
        val mods = buildList {
            if (enchantment.contains("Spooky") || all) add(DoubleModifier.SPOOKY_RESISTANCE.tag + ": ")
            if (enchantment.contains("Stench") || all) add(DoubleModifier.STENCH_RESISTANCE.tag + ": ")
            if (enchantment.contains("Hot") || all) add(DoubleModifier.HOT_RESISTANCE.tag + ": ")
            if (enchantment.contains("Cold") || all) add(DoubleModifier.COLD_RESISTANCE.tag + ": ")
            if (enchantment.contains("Sleaze") || all) add(DoubleModifier.SLEAZE_RESISTANCE.tag + ": ")
            if (enchantment.contains("Slime")) add(DoubleModifier.SLIME_RESISTANCE.tag + ": ")
            if (enchantment.contains("Supercold")) add(DoubleModifier.SUPERCOLD_RESISTANCE.tag + ": ")
        }.map { it + level }
        return mods.joinToString(", ").ifEmpty { null }
    }

    internal fun depluralizeClassName(string: String): String {
        for (results in CLASS_PLURALS) {
            val canonical = results[0]
            if (results.any { it == string }) return canonical
        }
        return string
    }

    private fun resolveClassName(plural: String): String? {
        val depluralized = depluralizeClassName(plural)
        ClassNames.resolve(depluralized)?.let { return it }
        CharacterClass.entries.firstOrNull {
            it != CharacterClass.UNKNOWN &&
                it.displayName.equals(depluralized, ignoreCase = true)
        }?.let { return it.displayName }
        return null
    }
}
