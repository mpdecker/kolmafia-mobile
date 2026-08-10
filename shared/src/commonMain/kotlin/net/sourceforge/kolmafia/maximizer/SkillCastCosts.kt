package net.sourceforge.kolmafia.maximizer

/** Desktop [net.sourceforge.kolmafia.persistence.SkillDatabase] non-MP cast costs. */
object SkillCastCosts {

    fun adventureCost(skillId: Int): Int = when (skillId) {
        168, // Evoke Eldritich Horror
        1027, // Hibernate
        2027, // Spirit Vacation
        3026, // Transcendental Dente
        4025, // Simmer
        12031, // Recruit Zombie
        15017, // Check Mirror
        16011, // Rain Man
        -> 1
        else -> 0
    }

    fun soulsauceCost(skillId: Int): Int = when (skillId) {
        7182, 7185 -> 5 // Soul Bubble, Soul Food
        7183 -> 40 // Soul Finger
        7184 -> 100 // Soul Blaze
        7186 -> 25 // Soul Rotation
        7187 -> 50 // Soul Funk
        else -> 0
    }

    fun thunderCost(skillId: Int): Int = when (skillId) {
        16001 -> 40 // Thunder Clap
        16002, 16004 -> 20 // Thundercloud, Thunderheart
        16003 -> 1 // Thunder Bird
        16005 -> 5 // Thunderstrike
        16006 -> 60 // Thunder Down Underwear
        else -> 0
    }

    fun rainCost(skillId: Int): Int = when (skillId) {
        16011 -> 50 // Rain Man
        16012 -> 20 // Rainy Day
        16013, 16014 -> 10 // Make It Rain, Rain Dance
        16015 -> 3 // Rainbow
        16016 -> 40 // Raincoat
        else -> 0
    }

    fun lightningCost(skillId: Int): Int = when (skillId) {
        16021, 16026 -> 20 // Lightning Strike, Lightning Rod
        16022, 16024 -> 10 // Clean Hair Lightning, Sheet Lightning
        16023 -> 5 // Ball Lightning
        16025 -> 1 // Lightning Bolt Rain
        else -> 0
    }

    fun hpCost(skillId: Int): Int = when (skillId) {
        24020, 24021, 24022 -> 3 // Blood Spike, Piercing Gaze, Savage Bite
        24023 -> 5 // Blood Chains
        24024 -> 7 // Chill of the Tomb
        24025, 24026, 24027, 24028, 24029, 24030, 24031, 24032 -> 10
        24033 -> 15 // Perceive Soul
        24034, 24035 -> 30 // Baleful Howl, Ensorcel
        24036, 24037, 24038 -> 30 // Blood Frenzy, Blood Bond, Blood Bubble
        24039, 24040 -> 50 // Blood Blade, Brams Bloody Bagatelle
        else -> 0
    }
}
