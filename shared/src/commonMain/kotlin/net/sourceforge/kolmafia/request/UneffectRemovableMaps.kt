package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.data.SkillDefinitionDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.skill.SkillManager

/** Desktop [net.sourceforge.kolmafia.request.UneffectRequest.reset] removable-effect maps. */
object UneffectRemovableMaps {
    const val REMEDY = 588
    const val ANCIENT_CURE_ALL = 7982
    const val HOT_DREADSYLVANIAN_COCOA = 6594

    private val removeWithSkillMap = linkedMapOf<String, MutableSet<Int>>()
    private val removeWithItemMap = linkedMapOf<Int, MutableSet<Int>>()

    init {
        reset { false }
    }

    fun reset(hasSkill: (String) -> Boolean) {
        removeWithItemMap.clear()
        removeWithSkillMap.clear()

        putItemRemovables(ItemIds.ANTIDOTE,
            EffectIds.HARDLY_POISONED,
            EffectIds.MAJORLY_POISONED,
            EffectIds.A_LITTLE_BIT_POISONED,
            EffectIds.SOMEWHAT_POISONED,
            EffectIds.REALLY_QUITE_POISONED,
        )
        putItemRemovables(ItemIds.TINY_HOUSE,
            EffectIds.BEATEN_UP,
            EffectIds.CONFUSED,
            EffectIds.EMBARRASSED,
            EffectIds.SUNBURNED,
            EffectIds.WUSSINESS,
        )
        putItemRemovables(ItemIds.TEARS, EffectIds.BEATEN_UP)
        putItemRemovables(ItemIds.TRIPPLES, EffectIds.BEATEN_UP)
        putItemRemovables(ItemIds.HOT_DREADSYLVANIAN_COCOA,
            EffectIds.TOUCHED_BY_A_GHOST,
            EffectIds.CHILLED_TO_THE_BONE,
            EffectIds.NAUSEATED,
            EffectIds.CURSE_OF_HOLLOWNESS,
            EffectIds.CURSE_OF_VULNERABILITY,
            EffectIds.CURSE_OF_EXPOSURE,
            EffectIds.CURSE_OF_IMPOTENCE,
            EffectIds.CURSE_OF_DULLNESS,
            EffectIds.CURSE_OF_WEAKNESS,
            EffectIds.CURSE_OF_SLUGGISHNESS,
            EffectIds.CURSE_OF_FORGETFULNESS,
            EffectIds.CURSE_OF_MISFORTUNE,
            EffectIds.CURSE_OF_CLUMSINESS,
            EffectIds.CURSE_OF_LONELINESS,
        )

        putSkillRemovables("Tongue of the Walrus",
            EffectIds.AXE_WOUND,
            EffectIds.BEATEN_UP,
            EffectIds.GRILLED,
            EffectIds.HALF_EATEN_BRAIN,
            EffectIds.MISSING_FINGERS,
            EffectIds.SUNBURNED,
        )

        val discoNap = mutableSetOf(
            EffectIds.CONFUSED,
            EffectIds.EMBARRASSED,
            EffectIds.SLEEPY,
            EffectIds.SUNBURNED,
            EffectIds.WUSSINESS,
            EffectIds.DISAVOWED,
        )
        if (hasSkill(SkillNames.ADVENTURER_OF_LEISURE)) {
            discoNap.addAll(
                listOf(
                    EffectIds.AFFRONTED_DECENCY,
                    EffectIds.APATHY,
                    EffectIds.CONSUMED_BY_FEAR,
                    EffectIds.CUNCTATITIS,
                    EffectIds.EASILY_EMBARRASSED,
                    EffectIds.EXISTENTIAL_TORMENT,
                    EffectIds.LIGHT_HEADED,
                    EffectIds.N_SPATIAL_VISION,
                    EffectIds.PRESTIDIGYSFUNCTION,
                    EffectIds.RAINY_SOUL_MIASMA,
                    EffectIds.SOCIALISMYDIA,
                    EffectIds.TENUOUS_GRIP_ON_REALITY,
                    EffectIds.TETANUS,
                    EffectIds.THE_COLORS,
                    EffectIds.THE_DISEASE,
                ),
            )
        }
        removeWithSkillMap["Disco Nap"] = discoNap

        putSkillRemovables("Shake It Off",
            EffectIds.A_REVOLUTION_IN_YOUR_MOUTH,
            EffectIds.AFFRONTED_DECENCY,
            EffectIds.ALL_COVERED_IN_WHATSIT,
            EffectIds.APATHY,
            EffectIds.AXE_WOUND,
            EffectIds.BARKING_DOGS,
            EffectIds.BEATEN_UP,
            EffectIds.BEER_IN_YOUR_SHOES,
            EffectIds.BLOODY_HAND,
            EffectIds.CONFUSED,
            EffectIds.CONSUMED_BY_FEAR,
            EffectIds.CORRODED_WEAPON,
            EffectIds.CUNCTATITIS,
            EffectIds.DEADENED_PALATE,
            EffectIds.EASILY_EMBARRASSED,
            EffectIds.EMBARRASSED,
            EffectIds.EXISTENTIAL_TORMENT,
            EffectIds.FLARED_NOSTRILS,
            EffectIds.GRILLED,
            EffectIds.HALF_EATEN_BRAIN,
            EffectIds.HERNIA,
            EffectIds.LIGHT_HEADED,
            EffectIds.MISSING_FINGERS,
            EffectIds.N_SPATIAL_VISION,
            EffectIds.NATURAL_1,
            EffectIds.ONCE_CURSED,
            EffectIds.PRESTIDIGYSFUNCTION,
            EffectIds.RAINY_SOUL_MIASMA,
            EffectIds.SLEEPY,
            EffectIds.SOCIALISMYDIA,
            EffectIds.STRANGULATED,
            EffectIds.SUNBURNED,
            EffectIds.TANGLED_UP,
            EffectIds.TEMPORARY_BLINDNESS,
            EffectIds.TENUOUS_GRIP_ON_REALITY,
            EffectIds.TETANUS,
            EffectIds.TOAD_IN_THE_HOLE,
            EffectIds.TURNED_INTO_A_SKELETON,
            EffectIds.THE_COLORS,
            EffectIds.THE_DISEASE,
            EffectIds.THRICE_CURSED,
            EffectIds.TWICE_CURSED,
            EffectIds.WUSSINESS,
        )
        putSkillRemovables("Pep Talk", EffectIds.OVERCONFIDENT)
        putSkillRemovables("Blood Sugar Sauce Magic",
            EffectIds.BLOOD_SUGAR_SAUCE_MAGIC_LITE,
            EffectIds.BLOOD_SUGAR_SAUCE_MAGIC,
        )
        putSkillRemovables("Spirit of Nothing",
            EffectIds.SPIRIT_OF_CAYENNE,
            EffectIds.SPIRIT_OF_PEPPERMINT,
            EffectIds.SPIRIT_OF_GARLIC,
            EffectIds.SPIRIT_OF_WORMWOOD,
            EffectIds.SPIRIT_OF_BACON_GREASE,
        )
        putSkillRemovables("Iron Palm Technique", EffectIds.IRON_PALMS)
        putSkillRemovables("Wolf Form", EffectIds.WOLF_FORM)
        putSkillRemovables("Mist Form", EffectIds.MIST_FORM)
        putSkillRemovables("Flock of Bats Form", EffectIds.BATS_FORM)
        putSkillRemovables("Absorb Cowrruption", EffectIds.COWRRUPTION)
        putSkillRemovables("Gelatinous Reconstruction", EffectIds.BEATEN_UP)
    }

    /** Desktop [UneffectRequest.getUneffectSkill] — returns skill name or empty string. */
    fun getUneffectSkill(effectId: Int, hasSkill: (String) -> Boolean): String {
        for ((skillName, removables) in removeWithSkillMap) {
            if (!removables.contains(effectId)) continue
            if (hasSkill(skillName)) return skillName
        }
        return ""
    }

    /** Desktop [UneffectRequest.getAction] item branch — first mapped item id or null. */
    fun getUneffectItemId(effectId: Int): Int? {
        for ((itemId, removables) in removeWithItemMap) {
            if (removables.contains(effectId)) return itemId
        }
        return null
    }

    /** True when desktop Hot Tub (Shake It Off removables) can remove this effect. */
    fun removableByShakeItOff(effectId: Int): Boolean =
        removeWithSkillMap["Shake It Off"]?.contains(effectId) == true

    /** Desktop [UneffectRequest.isRemovable] — blacklist with default removable. */
    fun isRemovable(effectId: Int): Boolean {
        if (effectId <= 0) return false
        if (effectId in UNREMOVABLE_EFFECT_IDS) return false
        return true
    }

    fun isRemovable(effectName: String): Boolean {
        val effectId = net.sourceforge.kolmafia.data.EffectDatabase.getByName(effectName)?.id ?: -1
        return isRemovable(effectId)
    }

    /** Desktop [UneffectRequest.needsCocoa] — Dreadsylvanian curse/ghost effects. */
    fun needsCocoa(effectId: Int): Boolean = effectId in NEEDS_COCOA_EFFECT_IDS

    fun resetFromSession(preferences: Preferences?, skillManager: SkillManager?) {
        reset(hasSkillResolver(preferences, skillManager))
    }

    fun hasSkillResolver(preferences: Preferences?, skillManager: SkillManager?): (String) -> Boolean =
        { name ->
            val skillId = SkillDefinitionDatabase.getByName(name)?.id
            if (skillId != null && (preferences?.getInt("skillLevel$skillId", 0) ?: 0) > 0) {
                true
            } else {
                skillManager?.state?.value?.skills?.any { it.name.equals(name, ignoreCase = true) } == true
            }
        }

    internal fun removableEffectCountForSkill(skillName: String): Int =
        removeWithSkillMap[skillName]?.size ?: 0

    internal fun removableEffectIdsForItem(itemId: Int): Set<Int> =
        removeWithItemMap[itemId]?.toSet() ?: emptySet()

    private fun putItemRemovables(itemId: Int, vararg effectIds: Int) {
        removeWithItemMap[itemId] = effectIds.toMutableSet()
    }

    private fun putSkillRemovables(skillName: String, vararg effectIds: Int) {
        removeWithSkillMap[skillName] = effectIds.toMutableSet()
    }

    private object SkillNames {
        const val ADVENTURER_OF_LEISURE = "Adventurer of Leisure"
    }

    private object ItemIds {
        const val ANTIDOTE = 829
        const val TINY_HOUSE = 592
        const val TEARS = 869
        const val TRIPPLES = 6027
        const val HOT_DREADSYLVANIAN_COCOA = 6594
    }

    private object EffectIds {
        const val SLEEPY = 2
        const val CONFUSED = 3
        const val EMBARRASSED = 4
        const val BEATEN_UP = 7
        const val HARDLY_POISONED = 8
        const val BLOODY_HAND = 15
        const val HERNIA = 39
        const val SUNBURNED = 42
        const val WUSSINESS = 43
        const val RAINY_SOUL_MIASMA = 57
        const val MISSING_FINGERS = 80
        const val CORRODED_WEAPON = 105
        const val APATHY = 115
        const val SPIRIT_OF_CAYENNE = 167
        const val SPIRIT_OF_PEPPERMINT = 168
        const val SPIRIT_OF_GARLIC = 169
        const val SPIRIT_OF_WORMWOOD = 170
        const val SPIRIT_OF_BACON_GREASE = 171
        const val TEMPORARY_BLINDNESS = 180
        const val MAJORLY_POISONED = 264
        const val TENUOUS_GRIP_ON_REALITY = 265
        const val TURNED_INTO_A_SKELETON = 266
        const val BARKING_DOGS = 267
        const val PRESTIDIGYSFUNCTION = 268
        const val TANGLED_UP = 281
        const val A_LITTLE_BIT_POISONED = 282
        const val SOMEWHAT_POISONED = 283
        const val REALLY_QUITE_POISONED = 284
        const val LIGHT_HEADED = 288
        const val TETANUS = 292
        const val HALF_EATEN_BRAIN = 293
        const val SOCIALISMYDIA = 295
        const val AXE_WOUND = 296
        const val GRILLED = 298
        const val THE_DISEASE = 299
        const val CUNCTATITIS = 301
        const val AFFRONTED_DECENCY = 388
        const val FLARED_NOSTRILS = 432
        const val EASILY_EMBARRASSED = 433
        const val ALL_COVERED_IN_WHATSIT = 434
        const val BEER_IN_YOUR_SHOES = 435
        const val TOAD_IN_THE_HOLE = 436
        const val STRANGULATED = 437
        const val A_REVOLUTION_IN_YOUR_MOUTH = 453
        const val THE_COLORS = 584
        const val EXISTENTIAL_TORMENT = 675
        const val IRON_PALMS = 709
        const val DEADENED_PALATE = 774
        const val NATURAL_1 = 930
        const val OVERCONFIDENT = 1011
        const val N_SPATIAL_VISION = 1035
        const val CONSUMED_BY_FEAR = 1146
        const val TOUCHED_BY_A_GHOST = 1276
        const val CHILLED_TO_THE_BONE = 1277
        const val NAUSEATED = 1278
        const val CURSE_OF_HOLLOWNESS = 1304
        const val CURSE_OF_VULNERABILITY = 1305
        const val CURSE_OF_EXPOSURE = 1306
        const val CURSE_OF_IMPOTENCE = 1307
        const val CURSE_OF_DULLNESS = 1308
        const val CURSE_OF_WEAKNESS = 1309
        const val CURSE_OF_SLUGGISHNESS = 1310
        const val CURSE_OF_FORGETFULNESS = 1311
        const val CURSE_OF_MISFORTUNE = 1312
        const val CURSE_OF_CLUMSINESS = 1313
        const val CURSE_OF_LONELINESS = 1314
        const val ONCE_CURSED = 1348
        const val TWICE_CURSED = 1349
        const val THRICE_CURSED = 1350
        const val BLOOD_SUGAR_SAUCE_MAGIC_LITE = 1457
        const val BLOOD_SUGAR_SAUCE_MAGIC = 1458
        const val COWRRUPTION = 2064
        const val DISAVOWED = 2294
        const val WOLF_FORM = 2449
        const val MIST_FORM = 2450
        const val BATS_FORM = 2451
        const val GOOFBALL_WITHDRAWAL = 111
        const val CURSED_BY_RNG = 217
        const val SOUL_CRUSHING_HEADACHE = 465
        const val FORM_OF_ROACH = 509
        const val SHAPE_OF_MOLE = 510
        const val FORM_OF_BIRD = 511
        const val COATED_IN_SLIME = 633
        const val EVERYTHING_LOOKS_YELLOW = 790
        const val EVERYTHING_LOOKS_BLUE = 791
        const val EVERYTHING_LOOKS_RED = 792
        const val DEEP_TAINTED_MIND = 1217
        const val SPIRIT_PARIAH = 1431
        const val BORED_WITH_EXPLOSIONS = 1557
        const val FEELING_QUEASY = 2099
        const val EVERYTHING_LOOKS_GREEN = 2881
        const val MILD_MANNERED_PROFESSOR = 2897
        const val SAVAGE_BEAST = 2898
        const val EVERYTHING_LOOKS_PURPLE = 2922
    }

    /** Desktop UneffectRequest.isRemovable blacklist. */
    private val UNREMOVABLE_EFFECT_IDS = setOf(
        EffectIds.CURSED_BY_RNG,
        EffectIds.FORM_OF_ROACH,
        EffectIds.SHAPE_OF_MOLE,
        EffectIds.FORM_OF_BIRD,
        EffectIds.MILD_MANNERED_PROFESSOR,
        EffectIds.SAVAGE_BEAST,
        EffectIds.GOOFBALL_WITHDRAWAL,
        EffectIds.SOUL_CRUSHING_HEADACHE,
        EffectIds.COATED_IN_SLIME,
        EffectIds.EVERYTHING_LOOKS_YELLOW,
        EffectIds.EVERYTHING_LOOKS_BLUE,
        EffectIds.EVERYTHING_LOOKS_RED,
        EffectIds.EVERYTHING_LOOKS_GREEN,
        EffectIds.EVERYTHING_LOOKS_PURPLE,
        EffectIds.DEEP_TAINTED_MIND,
        EffectIds.SPIRIT_PARIAH,
        EffectIds.BORED_WITH_EXPLOSIONS,
        EffectIds.FEELING_QUEASY,
    )

    private val NEEDS_COCOA_EFFECT_IDS = setOf(
        EffectIds.TOUCHED_BY_A_GHOST,
        EffectIds.CHILLED_TO_THE_BONE,
        EffectIds.NAUSEATED,
        EffectIds.CURSE_OF_HOLLOWNESS,
        EffectIds.CURSE_OF_VULNERABILITY,
        EffectIds.CURSE_OF_EXPOSURE,
        EffectIds.CURSE_OF_IMPOTENCE,
        EffectIds.CURSE_OF_DULLNESS,
        EffectIds.CURSE_OF_WEAKNESS,
        EffectIds.CURSE_OF_SLUGGISHNESS,
        EffectIds.CURSE_OF_FORGETFULNESS,
        EffectIds.CURSE_OF_MISFORTUNE,
        EffectIds.CURSE_OF_CLUMSINESS,
        EffectIds.CURSE_OF_LONELINESS,
    )
}
