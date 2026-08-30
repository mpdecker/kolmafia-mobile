package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.session.GreyYouManager.AbsorptionType
import net.sourceforge.kolmafia.session.GreyYouManager.PassiveEffect

/** Static Grey You absorption/skill tables ported from desktop GreyYouManager. */
internal object GreyYouCatalog {

    data class GooAbsorptionDef(val type: AbsorptionType, val monster: String, val value: Int = 0)

    data class GooSkillDef(
        val skillName: String,
        val monsterName: String,
        val effects: String = "",
        val passiveEffect: PassiveEffect? = null,
        val level: Int = 0,
    )

    /** Desktop monsterZone overrides for multi-zone monsters. */
    val MONSTER_ZONE_OVERRIDES: Map<String, String> = mapOf(
        "G imp" to "The Dark Elbow of the Woods",
        "L imp" to "The Dark Elbow of the Woods",
        "P imp" to "The Dark Heart of the Woods",
        "Fallen Archfiend" to "The Dark Heart of the Woods",
        "Hellion" to "The Dark Neck of the Woods",
        "W imp" to "The Dark Neck of the Woods",
        "gaunt ghuol" to "The Defiled Cranny",
        "gluttonous ghuol" to "The Defiled Cranny",
        "corpulent zobmie" to "The Defiled Alcove",
        "grave rober zmobie" to "The Defiled Alcove",
        "toothy sklelton" to "The Defiled Nook",
        "senile lihc" to "The Defiled Niche",
        "pygmy orderlies" to "The Hidden Hospital",
        "pygmy janitor" to "The Hidden Park",
        "pygmy witch lawyer" to "The Hidden Office Building",
        "tomb servant" to "The Upper Chamber",
        "Knob Goblin Alchemist" to "Cobb's Knob Laboratory",
    )

    val GOO_ABSORPTIONS: List<GooAbsorptionDef> = buildList {
        fun adv5(vararg names: String) = names.forEach { add(GooAbsorptionDef(AbsorptionType.ADVENTURES, it, 5)) }
        fun adv7(vararg names: String) = names.forEach { add(GooAbsorptionDef(AbsorptionType.ADVENTURES, it, 7)) }
        fun adv10(vararg names: String) = names.forEach { add(GooAbsorptionDef(AbsorptionType.ADVENTURES, it, 10)) }

        adv5(
            "albino bat", "batrat", "dire pigeon", "G imp", "gingerbread murderer", "grave rober",
            "irate mariachi", "Knob Goblin Bean Counter", "Knob Goblin Madam", "Knob Goblin Master Chef",
            "L imp", "magical fruit bat", "P imp", "plastered frat orc", "swarm of Knob lice",
            "swarm of skulls", "W imp", "warwelf",
        )
        adv7(
            "animated rustic nightstand", "basic lihc", "Battlie Knight Ghost", "Booze Giant",
            "Bubblemint Twins", "CH Imp", "chalkdust wraith", "cloud of disembodied whiskers",
            "eXtreme Orcish snowboarder", "gluttonous ghuol", "Grass Elemental", "grave rober zmobie",
            "guy with a pitchfork, and his wife", "junksprite sharpener", "Knob Goblin Very Mad Scientist",
            "model skeleton", "Ninja Snowman Janitor", "oil baron", "party skelteon",
            "possessed silverware drawer", "possessed toy chest", "revolving bugbear", "sabre-toothed goat",
            "serialbus", "sheet ghost", "skeletal hamster", "smut orc pipelayer", "swarm of killer bees",
            "tapdancing skeleton", "toilet papergeist", "upgraded ram", "vicious gnauga", "whitesnake",
        )
        adv10(
            "1335 HaXx0r", "Alphabet Giant", "black magic woman", "blur", "Bob Racecar", "coaltergeist",
            "fleet woodsman", "Iiti Kitty", "Irritating Series of Random Encounters", "Little Man in the Canoe",
            "mad wino", "Mob Penguin Capo", "One-Eyed Willie", "pygmy blowgunner", "pygmy headhunter",
            "pygmy orderlies", "pygmy shaman", "Racecar Bob", "Raver Giant", "Renaissance Giant",
            "swarm of fire ants", "tomb asp",
        )
        add(GooAbsorptionDef(AbsorptionType.MUSCLE, "stone temple pirate", 3))
        add(GooAbsorptionDef(AbsorptionType.MUSCLE, "Burly Sidekick", 5))
        add(GooAbsorptionDef(AbsorptionType.MUSCLE, "Knob Goblin Mutant", 5))
        add(GooAbsorptionDef(AbsorptionType.MUSCLE, "sleeping Knob Goblin Guard", 5))
        add(GooAbsorptionDef(AbsorptionType.MUSCLE, "angry bugbear", 10))
        add(GooAbsorptionDef(AbsorptionType.MUSCLE, "Fallen Archfiend", 10))
        add(GooAbsorptionDef(AbsorptionType.MUSCLE, "Fitness Giant", 10))
        add(GooAbsorptionDef(AbsorptionType.MUSCLE, "toothy sklelton", 10))
        add(GooAbsorptionDef(AbsorptionType.MYSTICALITY, "baa-relief sheep", 3))
        add(GooAbsorptionDef(AbsorptionType.MYSTICALITY, "fiendish can of asparagus", 5))
        add(GooAbsorptionDef(AbsorptionType.MYSTICALITY, "Quiet Healer", 5))
        add(GooAbsorptionDef(AbsorptionType.MYSTICALITY, "Blue Oyster cultist", 10))
        add(GooAbsorptionDef(AbsorptionType.MYSTICALITY, "bookbat", 10))
        add(GooAbsorptionDef(AbsorptionType.MYSTICALITY, "forest spirit", 10))
        add(GooAbsorptionDef(AbsorptionType.MYSTICALITY, "Hellion", 10))
        add(GooAbsorptionDef(AbsorptionType.MYSTICALITY, "Possibility Giant", 10))
        add(GooAbsorptionDef(AbsorptionType.MYSTICALITY, "senile lihc", 10))
        add(GooAbsorptionDef(AbsorptionType.MYSTICALITY, "tomb servant", 10))
        add(GooAbsorptionDef(AbsorptionType.MOXIE, "craven carven raven", 3))
        add(GooAbsorptionDef(AbsorptionType.MOXIE, "drunken half-orc hobo", 5))
        add(GooAbsorptionDef(AbsorptionType.MOXIE, "hung-over half-orc hobo", 5))
        add(GooAbsorptionDef(AbsorptionType.MOXIE, "sassy pirate", 5))
        add(GooAbsorptionDef(AbsorptionType.MOXIE, "Spunky Princess", 5))
        add(GooAbsorptionDef(AbsorptionType.MOXIE, "Demoninja", 10))
        add(GooAbsorptionDef(AbsorptionType.MOXIE, "gaunt ghuol", 10))
        add(GooAbsorptionDef(AbsorptionType.MOXIE, "gnefarious gnome", 10))
        add(GooAbsorptionDef(AbsorptionType.MOXIE, "Punk Rock Giant", 10))
        add(GooAbsorptionDef(AbsorptionType.MOXIE, "swarm of scarab beatles", 10))
        add(GooAbsorptionDef(AbsorptionType.MAX_HP, "fluffy bunny", 5))
        add(GooAbsorptionDef(AbsorptionType.MAX_HP, "beefy bodyguard bat", 10))
        add(GooAbsorptionDef(AbsorptionType.MAX_HP, "vampire bat", 10))
        add(GooAbsorptionDef(AbsorptionType.MAX_HP, "corpulent zobmie", 20))
        add(GooAbsorptionDef(AbsorptionType.MAX_MP, "Zol", 5))
        add(GooAbsorptionDef(AbsorptionType.MAX_MP, "grumpy 7-Foot Dwarf", 10))
        add(GooAbsorptionDef(AbsorptionType.MAX_MP, "plaque of locusts", 10))
    }

    val GOO_SKILLS: List<GooSkillDef> = listOf(
        GooSkillDef("Pseudopod Slap", "", "Deals 10 damage"),
        GooSkillDef("Hardslab", "remaindered skeleton", "Deals Mus in physical damage"),
        GooSkillDef("Telekinetic Murder", "crêep", "Deals Mys in physical damage"),
        GooSkillDef("Snakesmack", "sewer snake with a sewer snake in it", "Deals Mox in physical damage"),
        GooSkillDef("Ire Proof", "raging bull", passiveEffect = PassiveEffect.HOT_RESISTANCE, level = 3),
        GooSkillDef("Nanofur", "ratbat", passiveEffect = PassiveEffect.COLD_RESISTANCE, level = 3),
        GooSkillDef("Autovampirism Routines", "spooky vampire", passiveEffect = PassiveEffect.SPOOKY_RESISTANCE, level = 3),
        GooSkillDef("Conifer Polymers", "pine bat", passiveEffect = PassiveEffect.STENCH_RESISTANCE, level = 3),
        GooSkillDef("Anti-Sleaze Recursion", "werecougar", passiveEffect = PassiveEffect.SLEAZE_RESISTANCE, level = 3),
        GooSkillDef("Microburner", "Cobb's Knob oven", passiveEffect = PassiveEffect.HOT_DAMAGE, level = 5),
        GooSkillDef("Cryocurrency", "Knob Goblin MBA", passiveEffect = PassiveEffect.COLD_DAMAGE, level = 5),
        GooSkillDef("Curses Library", "lihc", passiveEffect = PassiveEffect.SPOOKY_DAMAGE, level = 5),
        GooSkillDef("Exhaust Tubules", "beanbat", passiveEffect = PassiveEffect.STENCH_DAMAGE, level = 5),
        GooSkillDef("Camp Subroutines", "Knob Goblin Harem Girl", passiveEffect = PassiveEffect.SLEAZE_DAMAGE, level = 5),
        GooSkillDef("Grey Noise", "Boss Bat", "Deals 5 damage + bonus elemental damage"),
        GooSkillDef("Advanced Exo-Alloy", "Knob Goblin Elite Guard", passiveEffect = PassiveEffect.DAMAGE_ABSORPTION, level = 100),
        GooSkillDef("Localized Vacuum", "cubist bull", passiveEffect = PassiveEffect.HOT_RESISTANCE, level = 2),
        GooSkillDef("Microweave", "eXtreme cross-country hippy", passiveEffect = PassiveEffect.COLD_RESISTANCE, level = 2),
        GooSkillDef("Ectogenesis", "Claybender Sorcerer Ghost", passiveEffect = PassiveEffect.SPOOKY_RESISTANCE, level = 2),
        GooSkillDef("Clammy Microcilia", "malevolent hair clog", passiveEffect = PassiveEffect.STENCH_RESISTANCE, level = 2),
        GooSkillDef("Lubricant Layer", "oil slick", passiveEffect = PassiveEffect.SLEAZE_RESISTANCE, level = 2),
        GooSkillDef("Infernal Automata", "demonic icebox", passiveEffect = PassiveEffect.HOT_DAMAGE, level = 10),
        GooSkillDef("Cooling Tubules", "Ninja Snowman Weaponmaster", passiveEffect = PassiveEffect.COLD_DAMAGE, level = 10),
        GooSkillDef("Ominous Substrate", "animated ornate nightstand", passiveEffect = PassiveEffect.SPOOKY_DAMAGE, level = 10),
        GooSkillDef("Secondary Fermentation", "drunk goat", passiveEffect = PassiveEffect.STENCH_DAMAGE, level = 10),
        GooSkillDef("Procgen Ribaldry", "smut orc screwer", passiveEffect = PassiveEffect.SLEAZE_DAMAGE, level = 10),
        GooSkillDef("Solid Fuel", "Knob Goblin Alchemist", passiveEffect = PassiveEffect.ADVENTURES, level = 10),
        GooSkillDef("Autochrony", "zombie waltzers", passiveEffect = PassiveEffect.ADVENTURES, level = 10),
        GooSkillDef("Temporal Hyperextension", "Pr Imp", passiveEffect = PassiveEffect.ADVENTURES, level = 10),
        GooSkillDef("Propagation Drive", "junksprite bender", passiveEffect = PassiveEffect.ITEM_DROP, level = 20),
        GooSkillDef("Financial Spreadsheets", "me4t begZ0r", passiveEffect = PassiveEffect.MEAT_DROP, level = 40),
        GooSkillDef("Phase Shift", "Spectral Jellyfish", "10 turns of Shifted Phase (Combat Rate -10)"),
        GooSkillDef("Piezoelectric Honk", "white lion", "10 turns of Hooooooooonk! (Combat Rate +10)"),
        GooSkillDef("Overclocking", "Big Wheelin' Twins", passiveEffect = PassiveEffect.INITIATIVE, level = 300),
        GooSkillDef("Subatomic Hardening", "pooltergeist", passiveEffect = PassiveEffect.DAMAGE_REDUCTION, level = 30),
        GooSkillDef("Gravitational Compression", "suckubus", passiveEffect = PassiveEffect.ITEM_DROP, level = 100),
        GooSkillDef("Hivemindedness", "mind flayer", passiveEffect = PassiveEffect.MP_REGEN, level = 100),
        GooSkillDef("Ponzi Apparatus", "anglerbush", passiveEffect = PassiveEffect.MEAT_DROP, level = 200),
        GooSkillDef("Fluid Dynamics Simulation", "Carnivorous Moxie Weed", passiveEffect = PassiveEffect.HP_REGEN, level = 100),
        GooSkillDef("Nantlers", "stuffed moose head", "Deals Mus in damage + bonus damage"),
        GooSkillDef("Nanoshock", "Jacob's adder", "Deals Mys in damage + bonus damage"),
        GooSkillDef("Audioclasm", "spooky music box", "Deals Mox in damage + bonus damage"),
        GooSkillDef("System Sweep", "pygmy janitor", "Deals Mus in physical damage & banish on win"),
        GooSkillDef("Double Nanovision", "drunk pygmy", "Deals Mys in physical damage & +100% Item Drop on win"),
        GooSkillDef("Infinite Loop", "pygmy witch lawyer", "Deals Mus in physical damage & +3 exp on win"),
        GooSkillDef("Photonic Shroud", "black panther", "10 turns of Darkened Photons (Combat Rate -10)"),
        GooSkillDef("Steam Mycelia", "steam elemental", passiveEffect = PassiveEffect.HOT_DAMAGE, level = 15),
        GooSkillDef("Snow-Cooling System", "Snow Queen", passiveEffect = PassiveEffect.COLD_DAMAGE, level = 15),
        GooSkillDef("Legacy Code", "possessed wine rack", passiveEffect = PassiveEffect.SPOOKY_DAMAGE, level = 15),
        GooSkillDef("AUTOEXEC.BAT", "Flock of Stab-bats", passiveEffect = PassiveEffect.STENCH_DAMAGE, level = 15),
        GooSkillDef("Innuendo Circuitry", "Astronomer", passiveEffect = PassiveEffect.SLEAZE_DAMAGE, level = 15),
        GooSkillDef("Subatomic Tango", "fan dancer", passiveEffect = PassiveEffect.ADVENTURES, level = 15),
        GooSkillDef("Extra Innings", "baseball bat", passiveEffect = PassiveEffect.ADVENTURES, level = 5),
        GooSkillDef("Reloading", "Bullet Bill", passiveEffect = PassiveEffect.ADVENTURES, level = 5),
        GooSkillDef("Harried", "rushing bum", passiveEffect = PassiveEffect.ADVENTURES, level = 5),
        GooSkillDef("Temporal Bent", "undead elbow macaroni", passiveEffect = PassiveEffect.ADVENTURES, level = 5),
        GooSkillDef("Provably Efficient", "Sub-Assistant Knob Mad Scientist", passiveEffect = PassiveEffect.ADVENTURES, level = 5),
        GooSkillDef("Basic Improvements", "BASIC Elemental", passiveEffect = PassiveEffect.ADVENTURES, level = 5),
        GooSkillDef("Shifted About", "shifty pirate", passiveEffect = PassiveEffect.ADVENTURES, level = 5),
        GooSkillDef("Spooky Veins", "ghost miner", passiveEffect = PassiveEffect.ADVENTURES, level = 10),
        GooSkillDef("Seven Foot Feelings", "dopey 7-Foot Dwarf", passiveEffect = PassiveEffect.ADVENTURES, level = 5),
        GooSkillDef("Self-Actualized", "banshee librarian", passiveEffect = PassiveEffect.ADVENTURES, level = 5),
    )
}
