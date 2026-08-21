package net.sourceforge.kolmafia.data

/**
 * Port of desktop `SkillDatabase` cost switch tables.
 * Skill IDs from desktop `SkillPool.java`.
 */
object SkillCosts {

    fun getAdventureCost(skillId: Int): Int = when (skillId) {
        1027, 2027, 3026, 4025, 12031, 15017, 16011, 168 -> 1
        else -> 0
    }

    fun getSoulsauceCost(skillId: Int): Int = when (skillId) {
        7182, 7185 -> 5   // SOUL_BUBBLE, SOUL_FOOD
        7186 -> 25        // SOUL_ROTATION
        7183 -> 40        // SOUL_FINGER
        7184 -> 100       // SOUL_BLAZE
        else -> 0
    }

    fun getThunderCost(skillId: Int): Int = when (skillId) {
        16003 -> 1   // THUNDER_BIRD
        16005 -> 5   // THUNDERSTRIKE
        16002, 16004 -> 20  // THUNDERCLOUD, THUNDERHEART
        16001 -> 40  // THUNDER_CLAP
        else -> 0
    }

    fun getRainCost(skillId: Int): Int = when (skillId) {
        16015 -> 3   // RAINBOW
        16013, 16014 -> 10  // MAKE_IT_RAIN, RAIN_DANCE
        16012 -> 20  // RAINY_DAY
        16011 -> 50  // RAIN_MAN
        else -> 0
    }

    fun getLightningCost(skillId: Int): Int = when (skillId) {
        16025 -> 1   // LIGHTNING_BOLT_RAIN
        16023 -> 5   // BALL_LIGHTNING
        16022, 16024 -> 10  // CLEAN_HAIR_LIGHTNING, SHEET_LIGHTNING
        16021, 16026 -> 20  // LIGHTNING_STRIKE, LIGHTNING_ROD
        else -> 0
    }

    fun getFuelCost(skillId: Int): Int = when (skillId) {
        7287 -> 10   // AM_BEAN_BAG_CANNON
        7288 -> 50   // AM_FRONT_BUMPER
        7286 -> 100  // AM_MISSILE_LAUNCHER
        else -> 0
    }

    fun getHPCost(skillId: Int): Int = when (skillId) {
        24020, 24030, 24010 -> 3  // BLOOD_SPIKE, PIERCING_GAZE, SAVAGE_BITE
        24021 -> 5                // BLOOD_CHAINS
        24022 -> 7                // CHILL_OF_THE_TOMB
        24023, 24013, 24011, 24034, 24024, 24033, 24014, 3044 -> 10
        24031 -> 15               // PERCEIVE_SOUL
        24012, 24032 -> 30        // BALEFUL_HOWL, ENSORCEL
        1043, 2044, 4042 -> 30    // BLOOD_FRENZY, BLOOD_BOND, BLOOD_BUBBLE
        5042, 6046 -> 50          // BLOOD_BLADE, BRAMS_BLOODY_BAGATELLE
        else -> 0
    }

    fun getMeatCost(skillId: Int): Int = when (skillId) {
        33023 -> 1           // BACON_RAY
        33001, 33012 -> 2    // BEEF_SHANK, SPICY_MEATBALL
        33005 -> 3           // STEW
        33002, 33004, 33014 -> 5  // MEAT_CLEAVER, ACT_JERKY, CHEW_THE_FAT
        33024, 33025 -> 8    // MEAT_LOCKER, WET_RUB
        33003, 33006, 33015, 33017, 33027, 33028 -> 10
        33013, 33016, 33026 -> 20  // MEAT_CUTE, MEAT_LOAF, DARK_MEAT
        else -> 0
    }
}
