package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.data.CombatDatabase
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.data.MonsterDatabase
import net.sourceforge.kolmafia.data.SkillDefinitionDatabase
import net.sourceforge.kolmafia.preferences.Preferences

/** Shared Grey You absorption registry and session state, ported from desktop GreyYouManager. */
object GreyYouManager {
    enum class AbsorptionType(val label: String) {
        SKILL("Skill"),
        ADVENTURES("Adventures"),
        MUSCLE("Muscle"),
        MYSTICALITY("Mysticality"),
        MOXIE("Moxie"),
        MAX_HP("Max HP"),
        MAX_MP("Max MP"),
    }

    enum class PassiveEffect(val displayName: String, val sortKey: Int) {
        HOT_DAMAGE("Hot Damage", 1),
        COLD_DAMAGE("Cold Damage", 2),
        SPOOKY_DAMAGE("Spooky Damage", 3),
        STENCH_DAMAGE("Stench Damage", 4),
        SLEAZE_DAMAGE("Sleaze Damage", 5),
        HOT_RESISTANCE("Hot Resistance", 6),
        COLD_RESISTANCE("Cold Resistance", 7),
        SPOOKY_RESISTANCE("Spooky Resistance", 8),
        STENCH_RESISTANCE("Stench Resistance", 9),
        SLEAZE_RESISTANCE("Sleaze Resistance", 10),
        DAMAGE_ABSORPTION("Damage Absorption", 11),
        DAMAGE_REDUCTION("Damage Absorption", 12),
        ITEM_DROP("Item Drop", 13),
        MEAT_DROP("Meat Drop", 14),
        INITIATIVE("Initiative", 15),
        HP_REGEN("HP Regen", 16),
        MP_REGEN("MP Regen", 17),
        ADVENTURES("Rollover Adventures", 18),
    }

    enum class SkillTag { COMBAT, NONCOMBAT, PASSIVE }

    data class Absorption(
        val monsterId: Int,
        val monsterName: String,
        val zone: String,
        val type: AbsorptionType,
        val value: Int = 0,
        val skillId: Int? = null,
        val skillName: String? = null,
        val enchantments: String = "",
        val skillTag: SkillTag? = null,
        val skillTypeName: String = "",
        val mpCost: Int = 0,
        val passiveEffect: PassiveEffect? = null,
        val passiveLevel: Int = 0,
    ) {
        fun haveAbsorbed(): Boolean = when (type) {
            AbsorptionType.SKILL -> skillId?.let(GreyYouManager::haveLearned) == true
            else -> GreyYouManager.haveAbsorbed(monsterId)
        }

        fun rewardLabel(): String = when {
            skillName != null -> skillName
            type == AbsorptionType.ADVENTURES -> "+$value Adventures"
            type in setOf(
                AbsorptionType.MUSCLE,
                AbsorptionType.MYSTICALITY,
                AbsorptionType.MOXIE,
                AbsorptionType.MAX_HP,
                AbsorptionType.MAX_MP,
            ) -> "${type.label} +$value"
            else -> type.label
        }

        fun evaluatedEnchantments(): String {
            if (type != AbsorptionType.SKILL || skillName.isNullOrBlank()) return enchantments
            if (skillTag == SkillTag.PASSIVE) {
                return ModifierDatabase.getSkill(skillName)?.modifiers?.takeIf { it.isNotBlank() }
                    ?: enchantments
            }
            return enchantments
        }
    }

    private val registry = linkedMapOf<Int, Absorption>()
    private val skills = linkedMapOf<Int, Absorption>()
    val absorbedMonsters: MutableSet<Int> = linkedSetOf()
    val learnedSkills: MutableSet<Int> = linkedSetOf()
    val unknownAbsorptions: MutableMap<Int, String> = linkedMapOf()
    val zoneAbsorptions: MutableMap<String, MutableSet<Absorption>> = sortedMapOf(String.CASE_INSENSITIVE_ORDER)

    val allAbsorptions: Map<Int, Absorption> get() = registry
    val allGooSkills: Map<Int, Absorption> get() = skills

    fun loadRegistry() {
        if (registry.isNotEmpty()) return
        GreyYouCatalog.GOO_ABSORPTIONS.forEach { def ->
            val monster = MonsterDatabase.getByName(def.monster)
            val zone = monsterZone(monster?.name ?: def.monster)
            val absorption = Absorption(
                monsterId = monster?.id ?: -1,
                monsterName = monster?.name ?: def.monster,
                zone = zone,
                type = def.type,
                value = def.value,
            )
            if (monster != null) registry[monster.id] = absorption
            addZoneAbsorption(absorption)
        }
        GreyYouCatalog.GOO_SKILLS.forEach { def ->
            val skill = SkillDefinitionDatabase.getByName(def.skillName) ?: return@forEach
            val monster = MonsterDatabase.getByName(def.monsterName)
            val zone = monsterZone(monster?.name ?: def.monsterName)
            val tag = when {
                def.passiveEffect != null || skill.isPassive -> SkillTag.PASSIVE
                skill.isCombat -> SkillTag.COMBAT
                else -> SkillTag.NONCOMBAT
            }
            val enchantments = when (tag) {
                SkillTag.PASSIVE ->
                    ModifierDatabase.getSkill(skill.name)?.modifiers.orEmpty()
                else -> def.effects
            }
            val absorption = Absorption(
                monsterId = monster?.id ?: -1,
                monsterName = monster?.name ?: def.monsterName,
                zone = zone,
                type = AbsorptionType.SKILL,
                skillId = skill.id,
                skillName = skill.name,
                enchantments = enchantments.ifBlank { def.effects },
                skillTag = tag,
                skillTypeName = skillTypeLabel(tag),
                mpCost = if (tag == SkillTag.PASSIVE) 0 else skill.mpCost,
                passiveEffect = def.passiveEffect,
                passiveLevel = def.level,
            )
            skills[skill.id] = absorption
            if (monster != null) registry[monster.id] = absorption
            addZoneAbsorption(absorption)
        }
    }

    private fun addZoneAbsorption(absorption: Absorption) {
        if (absorption.zone.isBlank()) return
        zoneAbsorptions.getOrPut(absorption.zone) { linkedSetOf() }.add(absorption)
    }

    private fun monsterZone(monsterName: String): String {
        GreyYouCatalog.MONSTER_ZONE_OVERRIDES[monsterName]?.let { return it }
        return CombatDatabase.all().firstOrNull { zone ->
            zone.monsters.any { it.name.equals(monsterName, ignoreCase = true) }
        }?.locationName.orEmpty()
    }

    private fun skillTypeLabel(tag: SkillTag): String = when (tag) {
        SkillTag.COMBAT -> "combat"
        SkillTag.NONCOMBAT -> "noncombat"
        SkillTag.PASSIVE -> "passive"
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
        parseMonsterAbsorptions(responseText, unknownLog)
        parseSkillAbsorptions(responseText, unknownLog)
    }

    fun parseMonsterAbsorptions(responseText: String, unknownLog: (String) -> Unit = {}) {
        ABSORPTION.findAll(responseText).forEach {
            val description = it.groupValues[1].replace(Regex("<.*?>"), "").trim()
            val monsterName = it.groupValues[2].replace(Regex("<.*?>"), "").trim()
            val monsterId = it.groupValues[3].toInt()
            if (registry.containsKey(monsterId)) {
                absorbedMonsters += monsterId
            } else {
                unknownAbsorptions[monsterId] = description
                unknownLog(
                    "*** Unknown Grey You absorption: '$description' from '$monsterName' (id = $monsterId)",
                )
            }
        }
    }

    fun parseSkillAbsorptions(responseText: String, unknownLog: (String) -> Unit = {}) {
        SKILL.findAll(responseText).forEach {
            val skillId = it.groupValues[1].toInt()
            if (skillId / 1000 != 27) return@forEach
            val skill = learnSkill(skillId, true, unknownLog)
            skill?.takeIf { it.monsterId > 0 }?.let { absorbedMonsters += it.monsterId }
        }
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

    /** Modifier overlay for CurrentModifiers / Maximizer when in Grey You. */
    fun modifierOverlay(): String {
        val parts = mutableListOf<String>()
        absorptionModifiers().forEach { (name, value) ->
            if (value != 0) parts.add("$name: +$value")
        }
        learnedSkills.forEach { skillId ->
            skills[skillId]?.takeIf { it.skillTag == SkillTag.PASSIVE }?.skillName?.let { name ->
                ModifierDatabase.getSkill(name)?.modifiers?.takeIf { it.isNotBlank() }?.let(parts::add)
            }
        }
        return parts.joinToString(", ")
    }

    fun sortedGooSkills(order: String): List<Absorption> {
        loadRegistry()
        val list = skills.values.toList()
        val comparator = when (order.lowercase()) {
            "id" -> compareBy<Absorption> { it.skillId ?: 0 }
            "name" -> compareBy { it.skillName.orEmpty().lowercase() }
            "monster" -> compareBy { it.monsterName.lowercase() }
            "zone" -> compareBy { it.zone.lowercase() }
            "type" -> compareBy<Absorption>(
                { skillSortRank(it) },
                { it.passiveEffect?.sortKey ?: 0 },
                { it.passiveLevel },
                { it.skillName.orEmpty().lowercase() },
            )
            else -> compareBy(
                { skillSortRank(it) },
                { it.passiveEffect?.sortKey ?: 0 },
                { it.passiveLevel },
                { it.skillName.orEmpty().lowercase() },
            )
        }
        return list.sortedWith(comparator)
    }

    private fun skillSortRank(skill: Absorption): Int = when (skill.skillTag) {
        SkillTag.COMBAT -> 0
        SkillTag.NONCOMBAT -> 1
        SkillTag.PASSIVE -> 2
        null -> 3
    }

    fun resetForTest() {
        registry.clear()
        skills.clear()
        zoneAbsorptions.clear()
        resetAbsorptions()
    }

    private val ABSORPTION =
        Regex("""Absorbed (.*?) from (.*?)\.<!--\s*(\d+)\s*-->""", setOf(RegexOption.DOT_MATCHES_ALL))
    private val SKILL =
        Regex("""desc_skill\.php\?whichskill=(\d+)&(?:amp;)?self=true""", RegexOption.DOT_MATCHES_ALL)
}
