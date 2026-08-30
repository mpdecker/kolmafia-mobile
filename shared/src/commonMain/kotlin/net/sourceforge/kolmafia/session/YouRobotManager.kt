package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.data.SkillDefinitionDatabase
import net.sourceforge.kolmafia.modifiers.ExpressionContext
import net.sourceforge.kolmafia.modifiers.ModifierParser
import net.sourceforge.kolmafia.modifiers.ModifierValues
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.skill.SkillData
import net.sourceforge.kolmafia.skill.SkillManager
import net.sourceforge.kolmafia.skill.SkillType

/**
 * Desktop [YouRobotManager] — You, Robot part/CPU catalog, install accounting,
 * robocore equip/familiar/potion gates, and choice 1445/1447 sync (Phases 3171–3200).
 */
object YouRobotManager {

    const val REASSEMBLY_CHOICE = 1445
    const val STATBOT_CHOICE = 1447
    private const val TORSO_SKILL_ID = 12

    enum class Part(
        val keyword: String,
        val section: String,
        val displayName: String,
        val usable: Usable = Usable.NONE,
    ) {
        TOP("top", "Top", "Top Attachment", Usable.HAT),
        LEFT("left", "Left", "Left Arm", Usable.WEAPON),
        RIGHT("right", "Right", "Right Arm", Usable.OFFHAND),
        BOTTOM("bottom", "Bottom", "Propulsion System", Usable.PANTS),
        CPU("cpus", "Cpus", "CPU Upgrade"),
        ;

        companion object {
            fun fromKeyword(keyword: String?): Part? =
                entries.firstOrNull { it.keyword.equals(keyword, ignoreCase = true) }
        }
    }

    enum class Effect { PASSIVE, COMBAT, EQUIP }

    enum class Usable(val description: String, val slot: EquipmentSlot? = null) {
        NONE("no special effect"),
        HAT("can equip hats", EquipmentSlot.HAT),
        WEAPON("can equip weapons", EquipmentSlot.WEAPON),
        OFFHAND("can equip offhands", EquipmentSlot.OFFHAND),
        SHIRT("can equip shirts", EquipmentSlot.SHIRT),
        PANTS("can equip pants", EquipmentSlot.PANTS),
        FAMILIAR("can use familiars"),
        POTIONS("can use potions"),
    }

    enum class RobotUpgrade(
        val upgradeName: String,
        val part: Part,
        val cost: Int,
        val effect: Effect = Effect.PASSIVE,
        val index: Int = 0,
        val keyword: String = "",
        val skillOrDesc: String = "",
        val usable: Usable = Usable.NONE,
    ) {
        PEA_SHOOTER("Pea Shooter", Part.TOP, 5, Effect.COMBAT, 1, skillOrDesc = "Shoot Pea"),
        BIRD_CAGE("Bird Cage", Part.TOP, 5, Effect.EQUIP, 2, usable = Usable.FAMILIAR),
        SOLAR_PANEL("Solar Panel", Part.TOP, 5, index = 3),
        MANNEQUIN_HEAD("Mannequin Head", Part.TOP, 15, Effect.EQUIP, 4, usable = Usable.HAT),
        MEAT_RADAR("Meat Radar", Part.TOP, 30, index = 5),
        JUNK_CANNON("Junk Cannon", Part.TOP, 30, Effect.COMBAT, 6, skillOrDesc = "Junk Blast"),
        TESLA_BLASTER("Tesla Blaster", Part.TOP, 30, Effect.COMBAT, 7, skillOrDesc = "Tesla Blast"),
        SNOW_BLOWER("Snow Blower", Part.TOP, 40, Effect.COMBAT, 8, skillOrDesc = "Blow Snow"),

        POUND_O_TRON("Pound-O-Tron", Part.LEFT, 5, Effect.COMBAT, 1, skillOrDesc = "Swing Pound-O-Tron"),
        REFLECTIVE_SHARD("Reflective Shard", Part.LEFT, 5, index = 2, skillOrDesc = "Resist All: +3"),
        METAL_DETECTOR("Metal Detector", Part.LEFT, 5, index = 3),
        VICE_GRIPS("Vice Grips", Part.LEFT, 15, Effect.EQUIP, 4, usable = Usable.WEAPON),
        SNIPER_RIFLE("Sniper Rifle", Part.LEFT, 30, Effect.COMBAT, 5, skillOrDesc = "Snipe"),
        JUNK_MACE("Junk Mace", Part.LEFT, 30, Effect.COMBAT, 6, skillOrDesc = "Junk Mace Smash"),
        CAMOUFLAGE_CURTAIN("Camouflage Curtain", Part.LEFT, 30, index = 7),
        GREASE_GUN("Grease Gun", Part.LEFT, 40, Effect.COMBAT, 8, skillOrDesc = "Shoot Grease"),

        SLAB_O_MATIC("Slab-O-Matic", Part.RIGHT, 5, index = 1),
        JUNK_SHIELD("Junk Shield", Part.RIGHT, 5, index = 2),
        HORSESHOE_MAGNET("Horseshoe Magnet", Part.RIGHT, 5, index = 3),
        OMNI_CLAW("Omni-Claw", Part.RIGHT, 15, Effect.EQUIP, 4, usable = Usable.OFFHAND),
        MAMMAL_PROD("Mammal Prod", Part.RIGHT, 30, Effect.COMBAT, 5, skillOrDesc = "Prod"),
        SOLENOID_PISTON("Solenoid Piston", Part.RIGHT, 30, Effect.COMBAT, 6, skillOrDesc = "Solenoid Slam"),
        BLARING_SPEAKER("Blaring Speaker", Part.RIGHT, 30, index = 7),
        SURPLUS_FLAMETHROWER("Surplus Flamethrower", Part.RIGHT, 40, Effect.COMBAT, 8, skillOrDesc = "Throw Flame"),

        BALD_TIRES("Bald Tires", Part.BOTTOM, 5, index = 1),
        ROCKET_CROTCH("Rocket Crotch", Part.BOTTOM, 5, Effect.COMBAT, 2, skillOrDesc = "Crotch Burn"),
        MOTORCYCLE_WHEEL("Motorcycle Wheel", Part.BOTTOM, 5, index = 3),
        ROBO_LEGS("Robo-Legs", Part.BOTTOM, 15, Effect.EQUIP, 4, usable = Usable.PANTS),
        MAGNO_LEV("Magno-Lev", Part.BOTTOM, 30, index = 5),
        TANK_TREADS("Tank Treads", Part.BOTTOM, 30, index = 6),
        SNOWPLOW("Snowplow", Part.BOTTOM, 30, index = 7),

        LEVERAGE_COPROCESSING("Leverage Coprocessing", Part.CPU, 30, keyword = "robot_muscle"),
        DYNAMIC_ARCANE_FLUX_MODELING("Dynamic Arcane Flux Modeling", Part.CPU, 30, keyword = "robot_mysticality"),
        UPGRADED_FASHION_SENSE("Upgraded Fashion Sensor", Part.CPU, 30, keyword = "robot_moxie"),
        FINANCE_NEURAL_NET("Finance Neural Net", Part.CPU, 30, keyword = "robot_meat"),
        SPATIAL_COMPRESSION_FUNCTION("Spatial Compression Functions", Part.CPU, 40, keyword = "robot_hp1"),
        SELF_REPAIR_ROUTINES("Self-Repair Routines", Part.CPU, 40, keyword = "robot_regen"),
        WEATHER_CONTROL_ALGORITHMS(
            "Weather Control Algorithms",
            Part.CPU,
            40,
            keyword = "robot_resist",
            skillOrDesc = "Resist All: +2",
        ),
        IMPROVED_OPTICAL_PROCESSING("Improved Optical Processing", Part.CPU, 40, keyword = "robot_items"),
        TOPOLOGY_GRID("Topology Grid", Part.CPU, 50, Effect.EQUIP, keyword = "robot_shirt", usable = Usable.SHIRT),
        OVERCLOCKING("Overclocking", Part.CPU, 50, keyword = "robot_energy"),
        BIOMASS_PROCESSING_FUNCTION(
            "Biomass Processing Function",
            Part.CPU,
            50,
            Effect.EQUIP,
            keyword = "robot_potions",
            usable = Usable.POTIONS,
        ),
        HOLOGRAPHIC_DEFLECTOR_PROJECTION("Holographic Deflector Projection", Part.CPU, 50, keyword = "robot_hp2"),
        ;

        override fun toString(): String = upgradeName
    }

    private val nameToUpgrade = RobotUpgrade.entries.associateBy { it.upgradeName.lowercase() }
    private val indexMaps: Map<Part, Map<Int, RobotUpgrade>> = Part.entries
        .filter { it != Part.CPU }
        .associateWith { part ->
            RobotUpgrade.entries.filter { it.part == part && it.index > 0 }.associateBy { it.index }
        }
    private val keywordToCpu = RobotUpgrade.entries
        .filter { it.part == Part.CPU }
        .associateBy { it.keyword }

    private val currentParts = mutableMapOf<Part, RobotUpgrade>()
    private val currentCpu = mutableSetOf<RobotUpgrade>()

    private val AVATAR = Regex("""otherimages/robot/(left|right|top|bottom|body)(\d+)\.png""")
    private val CPU_INSTALLED = Regex(
        """<button.*?value="([a-z0-9_]+)"[^\(]+\(already installed\)""",
        RegexOption.IGNORE_CASE,
    )
    private val STATBOT_COST = Regex("""Current upgrade cost: <b>(\d+) energy</b>""")
    private val optionToStat = mapOf(1 to "Muscle", 2 to "Mysticality", 3 to "Moxie")

    fun reset() {
        currentParts.clear()
        currentCpu.clear()
    }

    /** Restore installed parts/CPU from prefs (login / path restore). */
    fun restoreFromPreferences(preferences: Preferences?, skillManager: SkillManager? = null) {
        reset()
        preferences ?: return
        for (part in listOf(Part.TOP, Part.LEFT, Part.RIGHT, Part.BOTTOM)) {
            val index = preferences.getInt("youRobot${part.section}", 0)
            if (index > 0) {
                indexMaps[part]?.get(index)?.let { installUpgrade(part, it, preferences, skillManager, accountCost = false) }
            }
        }
        preferences.getString("youRobotCPUUpgrades", "")
            .split(',')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .forEach { kw ->
                keywordToCpu[kw]?.let { installCpuUpgrade(it, preferences, skillManager, accountCost = false) }
            }
    }

    fun hasEquipped(name: String): Boolean {
        val upgrade = nameToUpgrade[name.lowercase()] ?: return false
        return hasEquipped(upgrade)
    }

    fun hasEquipped(upgrade: RobotUpgrade): Boolean = when (upgrade.part) {
        Part.CPU -> upgrade in currentCpu
        else -> currentParts[upgrade.part] == upgrade
    }

    fun currentPart(part: Part): RobotUpgrade? = currentParts[part]
    fun currentCpuUpgrades(): Set<RobotUpgrade> = currentCpu.toSet()

    fun canUseFamiliars(): Boolean = hasEquipped(RobotUpgrade.BIRD_CAGE)

    fun canUsePotions(): Boolean = hasEquipped(RobotUpgrade.BIOMASS_PROCESSING_FUNCTION)

    fun canEquip(primaryUse: ItemPrimaryUse, hasTorso: Boolean = false): Boolean = when (primaryUse) {
        ItemPrimaryUse.HAT -> hasEquipped(RobotUpgrade.MANNEQUIN_HEAD)
        ItemPrimaryUse.WEAPON -> hasEquipped(RobotUpgrade.VICE_GRIPS)
        ItemPrimaryUse.OFFHAND -> hasEquipped(RobotUpgrade.OMNI_CLAW)
        ItemPrimaryUse.SHIRT -> hasTorso || hasEquipped(RobotUpgrade.TOPOLOGY_GRID)
        ItemPrimaryUse.PANTS -> hasEquipped(RobotUpgrade.ROBO_LEGS)
        else -> true
    }

    fun addRobotModifiers(total: ModifierValues, ctx: ExpressionContext): ModifierValues {
        var acc = total
        for (upgrade in currentParts.values) {
            val raw = ModifierDatabase.get("Robot", upgrade.upgradeName)?.modifiers ?: continue
            if (raw.isNotBlank() && !raw.equals("none", ignoreCase = true)) {
                acc = acc + ModifierParser.parse(raw, ctx)
            }
        }
        for (upgrade in currentCpu) {
            val raw = ModifierDatabase.get("Robot", upgrade.upgradeName)?.modifiers
                ?: ModifierDatabase.get("RobotCPU", upgrade.keyword)?.modifiers
                ?: continue
            if (raw.isNotBlank() && !raw.equals("none", ignoreCase = true)) {
                acc = acc + ModifierParser.parse(raw, ctx)
            }
        }
        return acc
    }

    fun parseAvatar(
        text: String,
        preferences: Preferences?,
        skillManager: SkillManager? = null,
    ): Boolean {
        var changed = false
        AVATAR.findAll(text).forEach { match ->
            val section = match.groupValues[1]
            val index = match.groupValues[2].toIntOrNull() ?: return@forEach
            if (section == "body") {
                preferences?.setInt("youRobotBody", index)
            } else {
                val part = Part.fromKeyword(section) ?: return@forEach
                val upgrade = indexMaps[part]?.get(index)
                if (upgrade != null) {
                    changed = installUpgrade(part, upgrade, preferences, skillManager, accountCost = false) || changed
                } else {
                    preferences?.setInt("youRobot${part.section}", index)
                }
            }
        }
        return changed
    }

    fun parseCpuUpgrades(
        text: String,
        preferences: Preferences?,
        skillManager: SkillManager? = null,
    ): Boolean {
        var changed = false
        CPU_INSTALLED.findAll(text).forEach { match ->
            val keyword = match.groupValues[1]
            val upgrade = keywordToCpu[keyword] ?: return@forEach
            changed = installCpuUpgrade(upgrade, preferences, skillManager, accountCost = false) || changed
        }
        return changed
    }

    fun parseStatbotCost(text: String, preferences: Preferences?): Boolean {
        preferences ?: return false
        val cost = STATBOT_COST.find(text)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: return false
        preferences.setInt("statbotUses", cost - 10)
        return true
    }

    fun visitChoice(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
        choiceUrl: String = "",
        skillManager: SkillManager? = null,
    ): Boolean {
        return when (choiceId) {
            REASSEMBLY_CHOICE -> {
                parseAvatar(html, preferences, skillManager)
                if (choiceUrl.contains("show=cpus", ignoreCase = true) ||
                    html.contains("(already installed)")
                ) {
                    parseCpuUpgrades(html, preferences, skillManager)
                }
                true
            }
            STATBOT_CHOICE -> parseStatbotCost(html, preferences)
            else -> false
        }
    }

    fun postChoice(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
        choiceUrl: String = "",
        character: KoLCharacter? = null,
        skillManager: SkillManager? = null,
        decision: Int = 0,
    ): Boolean {
        return when (choiceId) {
            REASSEMBLY_CHOICE -> {
                val show = extractFieldValue(choiceUrl, "show")?.lowercase().orEmpty()
                val part = Part.fromKeyword(show)
                val chosen = extractFieldValue(choiceUrl, "p").orEmpty()
                val upgrade = urlFieldsToUpgrade(part, chosen)
                if (upgrade == null || part == null) {
                    visitChoice(choiceId, html, preferences, choiceUrl, skillManager)
                    return true
                }
                if (part == Part.CPU) {
                    if (installCpuUpgrade(upgrade, preferences, skillManager, accountCost = true)) {
                        character?.let { adjustEnergy(it, -upgrade.cost) }
                    }
                } else {
                    if (installUpgrade(part, upgrade, preferences, skillManager, accountCost = true)) {
                        character?.let { adjustScraps(it, -upgrade.cost) }
                    }
                }
                parseAvatar(html, preferences, skillManager)
                true
            }
            STATBOT_CHOICE -> {
                parseStatbotCost(html, preferences)
                if (!html.contains("You don't have enough Energy to do that.") && decision in 1..3) {
                    val cost = (preferences?.getInt("statbotUses", 0) ?: 0) + 10 - 1
                    character?.let { adjustEnergy(it, -cost) }
                }
                true
            }
            else -> false
        }
    }

    fun registerRequest(
        urlString: String,
        sessionLogger: SessionLogger?,
        preferences: Preferences? = null,
    ): Boolean {
        val choice = extractFieldValue(urlString, "whichchoice")?.toIntOrNull() ?: return false
        val decision = extractFieldValue(urlString, "option")?.toIntOrNull() ?: 0
        when (choice) {
            REASSEMBLY_CHOICE -> {
                val show = extractFieldValue(urlString, "show")
                val part = Part.fromKeyword(show)
                if (part == null) {
                    RequestLogger.updateSessionLog(urlString, sessionLogger)
                    return true
                }
                if (decision == 0) {
                    val message = "Inspecting $part options at the Reassembly Station."
                    RequestLogger.updateSessionLog(message, sessionLogger)
                    return true
                }
                val chosen = extractFieldValue(urlString, "p")
                val upgrade = urlFieldsToUpgrade(part, chosen)
                if (upgrade != null) {
                    val message = if (part == Part.CPU) {
                        "Upgrading your CPU with $upgrade for ${upgrade.cost} energy."
                    } else {
                        "Installing $upgrade as your $part for ${upgrade.cost} scrap."
                    }
                    RequestLogger.updateSessionLog(message, sessionLogger)
                    return true
                }
                RequestLogger.updateSessionLog(urlString, sessionLogger)
                return true
            }
            STATBOT_CHOICE -> {
                if (decision != 0) {
                    val stat = optionToStat[decision]
                    if (stat != null) {
                        val cost = (preferences?.getInt("statbotUses", 0) ?: 0) + 10
                        RequestLogger.updateSessionLog(
                            "Spending $cost energy to upgrade $stat by 5 points.",
                            sessionLogger,
                        )
                    }
                }
                return true
            }
            else -> return false
        }
    }

    /** Headless status lines for `robot` CLI. */
    fun statusLines(preferences: Preferences?, character: KoLCharacter?): List<String> {
        val energy = character?.state?.value?.youRobotEnergy
            ?: preferences?.getInt("youRobotEnergy", 0)
            ?: 0
        val scraps = character?.state?.value?.youRobotScraps
            ?: preferences?.getInt("youRobotScraps", 0)
            ?: 0
        val lines = mutableListOf(
            "You, Robot energy: $energy",
            "You, Robot scraps: $scraps",
        )
        for (part in listOf(Part.TOP, Part.LEFT, Part.RIGHT, Part.BOTTOM)) {
            val upgrade = currentParts[part]
            lines += if (upgrade != null) {
                "${part.displayName}: ${upgrade.upgradeName}"
            } else {
                val index = preferences?.getInt("youRobot${part.section}", 0) ?: 0
                "${part.displayName}: #$index"
            }
        }
        val cpus = currentCpu.map { it.keyword }.sorted().ifEmpty {
            preferences?.getString("youRobotCPUUpgrades", "")
                ?.split(',')
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?.sorted()
                .orEmpty()
        }
        lines += if (cpus.isEmpty()) "CPU upgrades: (none)" else "CPU upgrades: ${cpus.joinToString(", ")}"
        return lines
    }

    fun installUpgradeForTest(upgrade: RobotUpgrade, preferences: Preferences? = null) {
        if (upgrade.part == Part.CPU) {
            installCpuUpgrade(upgrade, preferences, null, accountCost = false)
        } else {
            installUpgrade(upgrade.part, upgrade, preferences, null, accountCost = false)
        }
    }

    private fun urlFieldsToUpgrade(part: Part?, chosenPart: String?): RobotUpgrade? {
        if (part == null || chosenPart.isNullOrBlank()) return null
        return if (part == Part.CPU) {
            keywordToCpu[chosenPart]
        } else {
            indexMaps[part]?.get(chosenPart.toIntOrNull() ?: return null)
        }
    }

    private fun installUpgrade(
        part: Part,
        upgrade: RobotUpgrade,
        preferences: Preferences?,
        skillManager: SkillManager?,
        accountCost: Boolean,
    ): Boolean {
        val previous = currentParts[part]
        if (upgrade == previous) return false
        uninstallUpgrade(part, previous, skillManager)
        currentParts[part] = upgrade
        when (upgrade.effect) {
            Effect.COMBAT -> addCombatSkill(upgrade, skillManager)
            Effect.EQUIP, Effect.PASSIVE -> Unit
        }
        preferences?.setInt("youRobot${part.section}", upgrade.index)
        return true
    }

    private fun uninstallUpgrade(part: Part, upgrade: RobotUpgrade?, skillManager: SkillManager?) {
        upgrade ?: return
        when {
            upgrade == RobotUpgrade.BIRD_CAGE -> Unit // familiar drop handled by caller/path
            upgrade.effect == Effect.COMBAT -> removeCombatSkill(upgrade, skillManager)
            else -> Unit
        }
    }

    private fun installCpuUpgrade(
        upgrade: RobotUpgrade,
        preferences: Preferences?,
        skillManager: SkillManager?,
        accountCost: Boolean,
    ): Boolean {
        if (upgrade in currentCpu) return false
        currentCpu += upgrade
        if (upgrade == RobotUpgrade.TOPOLOGY_GRID) {
            skillManager?.learnLocalSkill(
                SkillData(
                    id = TORSO_SKILL_ID,
                    name = "Torso Awaregness",
                    type = SkillType.PASSIVE,
                    mpCost = 0,
                    dailyLimit = 0,
                    timesCast = 0,
                ),
            )
        }
        writeCpuPref(preferences)
        return true
    }

    private fun writeCpuPref(preferences: Preferences?) {
        preferences?.setString(
            "youRobotCPUUpgrades",
            currentCpu.map { it.keyword }.sorted().joinToString(","),
        )
    }

    private fun addCombatSkill(upgrade: RobotUpgrade, skillManager: SkillManager?) {
        skillManager ?: return
        if (upgrade.effect != Effect.COMBAT || upgrade.skillOrDesc.isBlank()) return
        val def = SkillDefinitionDatabase.getByName(upgrade.skillOrDesc)
        skillManager.learnLocalSkill(
            SkillData(
                id = def?.id ?: upgrade.skillOrDesc.hashCode().and(0x7fffffff),
                name = upgrade.skillOrDesc,
                type = SkillType.COMBAT,
                mpCost = 0,
                dailyLimit = 0,
                timesCast = 0,
            ),
        )
    }

    private fun removeCombatSkill(upgrade: RobotUpgrade, skillManager: SkillManager?) {
        skillManager ?: return
        if (upgrade.effect != Effect.COMBAT || upgrade.skillOrDesc.isBlank()) return
        val def = SkillDefinitionDatabase.getByName(upgrade.skillOrDesc)
        if (def != null) {
            skillManager.forgetLocalSkill(def.id)
        }
    }

    private fun adjustEnergy(character: KoLCharacter, delta: Int) {
        val next = (character.state.value.youRobotEnergy + delta).coerceAtLeast(0)
        character.setYouRobotEnergy(next)
    }

    private fun adjustScraps(character: KoLCharacter, delta: Int) {
        val next = (character.state.value.youRobotScraps + delta).coerceAtLeast(0)
        character.setYouRobotScraps(next)
    }

    fun extractFieldValue(urlString: String, field: String): String? {
        // Accept full URLs or bare query fragments (with or without leading '?')
        val query = when {
            urlString.contains('?') -> urlString.substringAfter('?')
            else -> urlString
        }
        for (pair in query.split('&')) {
            val equals = pair.indexOf('=')
            val name = if (equals == -1) pair else pair.substring(0, equals)
            if (name.equals(field, ignoreCase = true)) {
                return if (equals == -1) pair else pair.substring(equals + 1)
            }
        }
        return null
    }
}
