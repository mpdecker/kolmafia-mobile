package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.data.MonsterDatabase
import net.sourceforge.kolmafia.data.SkillDefinitionDatabase
import net.sourceforge.kolmafia.preferences.Preferences

/** Shared Grey You absorption registry and session state, ported from desktop GreyYouManager. */
object GreyYouManager {
    enum class AbsorptionType { SKILL, ADVENTURES, MUSCLE, MYSTICALITY, MOXIE, MAX_HP, MAX_MP }

    data class Absorption(
        val monsterId: Int,
        val monsterName: String,
        val type: AbsorptionType,
        val value: Int = 0,
        val skillId: Int? = null,
        val skillName: String? = null,
        val enchantments: String = "",
    )

    private val registry = linkedMapOf<Int, Absorption>()
    private val skills = linkedMapOf<Int, Absorption>()
    val absorbedMonsters: MutableSet<Int> = linkedSetOf()
    val learnedSkills: MutableSet<Int> = linkedSetOf()
    val unknownAbsorptions: MutableMap<Int, String> = linkedMapOf()

    val allAbsorptions: Map<Int, Absorption> get() = registry
    val allGooSkills: Map<Int, Absorption> get() = skills

    fun loadRegistry() {
        if (registry.isNotEmpty()) return
        GOO_ABSORPTIONS.forEach { (type, name, value) ->
            MonsterDatabase.getByName(name)?.let {
                registry[it.id] = Absorption(it.id, it.name, type, value)
            }
        }
        GOO_SKILLS.forEach { (skillName, monsterName, enchantments) ->
            val skill = SkillDefinitionDatabase.getByName(skillName) ?: return@forEach
            val monster = MonsterDatabase.getByName(monsterName)
            val absorption = Absorption(
                monsterId = monster?.id ?: -1,
                monsterName = monster?.name ?: monsterName,
                type = AbsorptionType.SKILL,
                skillId = skill.id,
                skillName = skill.name,
                enchantments = enchantments,
            )
            skills[skill.id] = absorption
            if (monster != null) registry[monster.id] = absorption
        }
    }

    fun resetAbsorptions() {
        absorbedMonsters.clear()
        learnedSkills.clear()
        unknownAbsorptions.clear()
    }

    fun parseAbsorptions(responseText: String, inGreyYou: Boolean, unknownLog: (String) -> Unit = {}) {
        if (!inGreyYou || !responseText.contains("Absorptions:")) {
            resetAbsorptions()
            return
        }
        loadRegistry()
        ABSORPTION.findAll(responseText).forEach {
            val description = it.groupValues[1].replace(Regex("<.*?>"), "").trim()
            val monsterName = it.groupValues[2].replace(Regex("<.*?>"), "").trim()
            val monsterId = it.groupValues[3].toInt()
            if (registry.containsKey(monsterId)) {
                absorbedMonsters += monsterId
            } else {
                unknownAbsorptions[monsterId] = description
                unknownLog("*** Unknown Grey You absorption: '$description' from '$monsterName' (id = $monsterId)")
            }
        }
        SKILL.findAll(responseText).forEach { learnSkill(it.groupValues[1].toInt(), inGreyYou, unknownLog) }
    }

    fun learnSkill(skillId: Int, inGreyYou: Boolean, unknownLog: (String) -> Unit = {}): Absorption? {
        if (!inGreyYou || skillId / 1000 != 27) return null
        loadRegistry()
        learnedSkills += skillId
        val skill = skills[skillId]
        if (skill == null) unknownLog("*** Unknown Grey You skill with id = $skillId")
        skill?.takeIf { it.monsterId > 0 }?.let { absorbedMonsters += it.monsterId }
        return skill
    }

    fun absorbMonster(
        monsterId: Int,
        absorbText: String,
        inGreyYou: Boolean,
        preferences: Preferences?,
    ): Boolean {
        if (!inGreyYou) return false
        loadRegistry()
        val absorption = registry[monsterId] ?: return false
        if (absorption.type == AbsorptionType.ADVENTURES) {
            if (!absorbText.contains("a lot of potential energy!") &&
                !absorbText.contains("incorporate this energetic creature.")
            ) return false
            preferences?.setInt(
                "_greyYouAdventures",
                preferences.getInt("_greyYouAdventures", 0) + absorption.value,
            )
        }
        absorbedMonsters += monsterId
        absorption.skillId?.let { learnedSkills += it }
        return true
    }

    fun reprocessMonster(monsterId: Int, preferences: Preferences?) {
        if (monsterId <= 0 || preferences == null) return
        val ids = preferences.getString("gooseReprocessed").split(',')
            .mapNotNull(String::toIntOrNull).toMutableSet()
        ids += monsterId
        preferences.setString("gooseReprocessed", ids.sorted().joinToString(","))
    }

    fun haveAbsorbed(monsterId: Int): Boolean = monsterId in absorbedMonsters
    fun haveLearned(skillId: Int): Boolean = skillId in learnedSkills
    fun unknownDescription(monsterId: Int): String? = unknownAbsorptions[monsterId]

    fun absorptionModifiers(): Map<String, Int> {
        val result = linkedMapOf<String, Int>()
        absorbedMonsters.mapNotNull(registry::get).filter { it.type != AbsorptionType.SKILL }.forEach {
            val key = when (it.type) {
                AbsorptionType.MUSCLE -> "Muscle"
                AbsorptionType.MYSTICALITY -> "Mysticality"
                AbsorptionType.MOXIE -> "Moxie"
                AbsorptionType.MAX_HP -> "Maximum HP"
                AbsorptionType.MAX_MP -> "Maximum MP"
                else -> return@forEach
            }
            result[key] = (result[key] ?: 0) + it.value
        }
        return result
    }

    private val ABSORPTION =
        Regex("""Absorbed (.*?) from (.*?)\.<!--\s*(\d+)\s*-->""", setOf(RegexOption.DOT_MATCHES_ALL))
    private val SKILL =
        Regex("""desc_skill\.php\?whichskill=(\d+)&(?:amp;)?self=true""", RegexOption.DOT_MATCHES_ALL)

    private data class Goo(val type: AbsorptionType, val monster: String, val value: Int)
    private data class GooSkill(val skill: String, val monster: String, val effects: String = "")

    private val GOO_ABSORPTIONS = listOf(
        "albino bat", "batrat", "dire pigeon", "G imp", "gingerbread murderer", "grave rober",
        "irate mariachi", "Knob Goblin Bean Counter", "Knob Goblin Madam", "Knob Goblin Master Chef",
        "L imp", "magical fruit bat", "P imp", "plastered frat orc", "swarm of Knob lice",
        "swarm of skulls", "W imp", "warwelf",
    ).map { Goo(AbsorptionType.ADVENTURES, it, 5) } + listOf(
        "animated rustic nightstand", "basic lihc", "Battlie Knight Ghost", "Booze Giant",
        "chalkdust wraith", "gluttonous ghuol", "grave rober zmobie", "model skeleton",
        "Ninja Snowman Janitor", "oil baron", "party skelteon", "sheet ghost",
        "skeletal hamster", "smut orc pipelayer", "tapdancing skeleton", "vicious gnauga",
    ).map { Goo(AbsorptionType.ADVENTURES, it, 7) } + listOf(
        "1335 HaXx0r", "Alphabet Giant", "black magic woman", "blur", "Bob Racecar",
        "coaltergeist", "fleet woodsman", "Iiti Kitty", "mad wino", "Mob Penguin Capo",
        "pygmy blowgunner", "pygmy headhunter", "pygmy orderlies", "Raver Giant",
        "Renaissance Giant", "tomb asp",
    ).map { Goo(AbsorptionType.ADVENTURES, it, 10) } + listOf(
        Goo(AbsorptionType.MUSCLE, "stone temple pirate", 3),
        Goo(AbsorptionType.MUSCLE, "Burly Sidekick", 5),
        Goo(AbsorptionType.MUSCLE, "angry bugbear", 10),
        Goo(AbsorptionType.MYSTICALITY, "baa-relief sheep", 3),
        Goo(AbsorptionType.MYSTICALITY, "Quiet Healer", 5),
        Goo(AbsorptionType.MYSTICALITY, "bookbat", 10),
        Goo(AbsorptionType.MOXIE, "craven carven raven", 3),
        Goo(AbsorptionType.MOXIE, "sassy pirate", 5),
        Goo(AbsorptionType.MOXIE, "Demoninja", 10),
        Goo(AbsorptionType.MAX_HP, "fluffy bunny", 5),
        Goo(AbsorptionType.MAX_HP, "vampire bat", 10),
        Goo(AbsorptionType.MAX_HP, "corpulent zobmie", 20),
        Goo(AbsorptionType.MAX_MP, "Zol", 5),
        Goo(AbsorptionType.MAX_MP, "grumpy 7-Foot Dwarf", 10),
        Goo(AbsorptionType.MAX_MP, "plaque of locusts", 10),
    )

    private val GOO_SKILLS = listOf(
        GooSkill("Pseudopod Slap", "", "Deals 10 damage"),
        GooSkill("Hardslab", "remaindered skeleton", "Deals Mus in physical damage"),
        GooSkill("Telekinetic Murder", "crêep", "Deals Mys in physical damage"),
        GooSkill("Snakesmack", "sewer snake with a sewer snake in it", "Deals Mox in physical damage"),
        GooSkill("Grey Noise", "Boss Bat", "Deals 5 damage + bonus elemental damage"),
        GooSkill("Phase Shift", "Spectral Jellyfish", "10 turns of Shifted Phase"),
        GooSkill("Piezoelectric Honk", "white lion", "10 turns of Hooooooooonk!"),
        GooSkill("Nantlers", "stuffed moose head", "Deals Mus in damage + bonus damage"),
        GooSkill("Nanoshock", "Jacob's adder", "Deals Mys in damage + bonus damage"),
        GooSkill("Audioclasm", "spooky music box", "Deals Mox in damage + bonus damage"),
        GooSkill("System Sweep", "pygmy janitor", "Deals Mus in physical damage & banish on win"),
        GooSkill("Double Nanovision", "drunk pygmy", "+100% Item Drop on win"),
        GooSkill("Infinite Loop", "pygmy witch lawyer", "+3 exp on win"),
        GooSkill("Photonic Shroud", "black panther", "10 turns of Darkened Photons"),
    )
}
