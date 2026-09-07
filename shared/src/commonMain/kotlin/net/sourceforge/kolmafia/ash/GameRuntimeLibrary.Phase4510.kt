package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.CharacterClass
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.CandyDatabase
import net.sourceforge.kolmafia.data.FactDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.MonsterDatabase
import net.sourceforge.kolmafia.data.MonsterPathMaps
import net.sourceforge.kolmafia.data.ShrunkenHeadDatabase
import net.sourceforge.kolmafia.data.SkillDefinitionDatabase
import net.sourceforge.kolmafia.data.WardrobeOMaticDatabase
import net.sourceforge.kolmafia.request.EquipmentRequest

/**
 * Phases 4501–4510 — ASH behavioral deepen X.
 *
 * 4501 equip_all_familiars · 4502 get_permed_skills · 4503 update_candy_prices ·
 * 4504 candy_for_tier(tier, flags) · 4505 futuristic_wardrobe ·
 * 4506 shrunken_head_zombie · 4507 outfit_name_with_codpiece_gems ·
 * 4508 get_monster_mapping · 4509 fact_* class/path overloads ·
 * 4510 (wardrobe 3-arg day overload already in 4505)
 */
internal fun GameRuntimeLibrary.registerPhase4510(scope: AshScope) {
    // ── 4501: equip_all_familiars ─────────────────────────────────
    regFn(scope, "equip_all_familiars", AshType.BOOLEAN, emptyList()) { _, _ ->
        val fam = familiarManager ?: return@regFn AshValue.FALSE
        val inv: (Int) -> Int = { id -> inventoryManager?.getCount(id) ?: 0 }
        AshValue.of(runBlocking { fam.equipAllFamiliars(inv) })
    }

    // ── 4502: get_permed_skills ───────────────────────────────────
    val skillToBool = AggregateType(AshType.SKILL, AshType.BOOLEAN)
    regFn(scope, "get_permed_skills", skillToBool, emptyList()) { _, _ ->
        val result = AggregateValue(skillToBool)
        val permed = skillManager?.state?.value?.permedSkills.orEmpty()
        for ((id, hardcore) in permed) {
            val name = SkillDefinitionDatabase.getById(id)?.name
                ?: skillManager?.state?.value?.skills?.firstOrNull { it.id == id }?.name
                ?: "Skill $id"
            result[AshValue.skill(name)] = AshValue.of(hardcore)
        }
        result
    }

    // ── 4503: update_candy_prices ─────────────────────────────────
    regFn(scope, "update_candy_prices", AshType.VOID, emptyList()) { _, _ ->
        val ids = CandyDatabase.candyIdsForPriceUpdate()
        if (ids.isNotEmpty()) {
            runBlocking { mallManager?.refreshMallPrices(ids, 0.0) }
        }
        AshValue.VOID
    }

    // ── 4504: candy_for_tier(tier, flags) ──────────────────────────
    val itemArray = AggregateType(AshType.INT, AshType.ITEM)
    regFn(
        scope,
        "candy_for_tier",
        itemArray,
        listOf("tier" to AshType.INT, "flags" to AshType.INT),
    ) { _, args ->
        val tier = args[0].toLong().toInt()
        val flags = args[1].toLong().toInt()
        if ((flags and CandyDatabase.ASH_FLAG_NO_BLACKLIST) == 0) {
            CandyDatabase.loadBlacklist(preferences)
        }
        val inv: (Int) -> Int = { id -> inventoryManager?.getCount(id) ?: 0 }
        val candies = CandyDatabase.candyForTier(tier, flags, inv)
        val result = AggregateValue(itemArray)
        candies.forEachIndexed { i, itemId ->
            val name = ItemDatabase.getById(itemId)?.name ?: return@forEachIndexed
            result[AshValue.of(i)] = AshValue.item(name)
        }
        result
    }

    // ── 4505: futuristic_wardrobe ─────────────────────────────────
    val modToInt = AggregateType(AshType.MODIFIER, AshType.INT)
    fun wardrobeMap(day: Int, slotOrdinal: Int, tier: Int): AggregateValue {
        val result = AggregateValue(modToInt)
        val slot = EquipmentSlot.entries.getOrNull(slotOrdinal)
        val clothing = when (slot) {
            EquipmentSlot.SHIRT -> WardrobeOMaticDatabase.shirt(day, tier)
            EquipmentSlot.HAT -> WardrobeOMaticDatabase.hat(day, tier)
            EquipmentSlot.FAMILIAR -> WardrobeOMaticDatabase.collar(day, tier)
            else -> null
        } ?: return result
        for ((mod, value) in clothing.modifiers) {
            result[AshValue(AshType.MODIFIER, mod.tag)] = AshValue.of(value.toLong())
        }
        return result
    }
    regFn(
        scope,
        "futuristic_wardrobe",
        modToInt,
        listOf("slot" to AshType.INT, "tier" to AshType.INT),
    ) { _, args ->
        val day = character?.state?.value?.globalDaycount ?: 0
        wardrobeMap(day, args[0].toLong().toInt(), args[1].toLong().toInt())
    }
    regFn(
        scope,
        "futuristic_wardrobe",
        modToInt,
        listOf("day" to AshType.INT, "slot" to AshType.INT, "tier" to AshType.INT),
    ) { _, args ->
        wardrobeMap(args[0].toLong().toInt(), args[1].toLong().toInt(), args[2].toLong().toInt())
    }

    // ── 4506: shrunken_head_zombie ────────────────────────────────
    val stringArray = AggregateType(AshType.INT, AshType.STRING)
    fun shrunkenArray(monsterId: Int, pathId: Int): AggregateValue {
        val result = AggregateValue(stringArray)
        ShrunkenHeadDatabase.shrunkenHeadZombie(monsterId, pathId).forEachIndexed { i, ability ->
            result[AshValue.of(i)] = AshValue.of(ability)
        }
        return result
    }
    regFn(scope, "shrunken_head_zombie", stringArray, listOf("monster" to AshType.MONSTER)) { _, args ->
        val mon = MonsterDatabase.getByName(args[0].toString()) ?: return@regFn AggregateValue(stringArray)
        val pathId = character?.state?.value?.ascensionPath?.pathId ?: 0
        shrunkenArray(mon.id, pathId)
    }
    regFn(
        scope,
        "shrunken_head_zombie",
        stringArray,
        listOf("monster" to AshType.MONSTER, "path" to AshType.PATH),
    ) { _, args ->
        val mon = MonsterDatabase.getByName(args[0].toString()) ?: return@regFn AggregateValue(stringArray)
        val path = AscensionPath.fromApiString(args[1].toString())
        shrunkenArray(mon.id, path.pathId)
    }

    // ── 4507: outfit_name_with_codpiece_gems ───────────────────────
    regFn(
        scope,
        "outfit_name_with_codpiece_gems",
        AshType.STRING,
        listOf("name" to AshType.STRING),
    ) { _, args ->
        val equipment = character?.state?.value?.equipment.orEmpty()
        val gemIds = EquipmentSlot.CODPIECE_SLOTS.map { slot ->
            val name = equipment[slot].orEmpty()
            if (name.isBlank()) 0 else ItemDatabase.getByName(name)?.id ?: 0
        }
        AshValue.of(EquipmentRequest.outfitNameWithCodpieceGems(args[0].toString(), gemIds))
    }

    // ── 4508: get_monster_mapping ─────────────────────────────────
    val monsterToMonster = AggregateType(AshType.MONSTER, AshType.MONSTER)
    fun mappingForPath(pathName: String): AggregateValue {
        val result = AggregateValue(monsterToMonster)
        for ((from, to) in MonsterPathMaps.getMonsterPathMap(pathName)) {
            val toValue = when {
                to == null -> AshValue(AshType.MONSTER, "")
                to.startsWith("#") -> {
                    val id = to.removePrefix("#").toIntOrNull()
                    val name = id?.let { MonsterDatabase.getById(it)?.name }.orEmpty()
                    AshValue(AshType.MONSTER, name)
                }
                else -> AshValue(AshType.MONSTER, to)
            }
            result[AshValue(AshType.MONSTER, from)] = toValue
        }
        return result
    }
    regFn(scope, "get_monster_mapping", monsterToMonster, emptyList()) { _, _ ->
        val path = character?.state?.value?.ascensionPath?.apiName.orEmpty()
        mappingForPath(path)
    }
    regFn(scope, "get_monster_mapping", monsterToMonster, listOf("path" to AshType.STRING)) { _, args ->
        mappingForPath(args[0].toString())
    }
    regFn(scope, "get_monster_mapping", monsterToMonster, listOf("path" to AshType.PATH)) { _, args ->
        mappingForPath(args[0].toString())
    }

    // ── 4509: fact_* (class, path, monster) ───────────────────────
    fun resolveClass(arg: AshValue): CharacterClass {
        val name = arg.toString()
        return CharacterClass.entries.firstOrNull {
            it.displayName.equals(name, ignoreCase = true)
        } ?: CharacterClass.UNKNOWN
    }
    fun resolvePath(arg: AshValue): AscensionPath = AscensionPath.fromApiString(arg.toString())

    regFn(
        scope,
        "fact_type",
        AshType.STRING,
        listOf("cls" to AshType.CLASS, "path" to AshType.PATH, "monster" to AshType.MONSTER),
    ) { _, args ->
        val monster = MonsterDatabase.getByName(args[2].toString())
            ?: return@regFn AshValue.EMPTY_STRING
        AshValue.of(FactDatabase.factTypeString(monster, resolveClass(args[0]), resolvePath(args[1]), null))
    }
    regFn(
        scope,
        "item_fact",
        AshType.ITEM,
        listOf("cls" to AshType.CLASS, "path" to AshType.PATH, "monster" to AshType.MONSTER),
    ) { _, args ->
        val monster = MonsterDatabase.getByName(args[2].toString()) ?: return@regFn AshValue.item("")
        val fact = FactDatabase.getFact(monster, resolveClass(args[0]), resolvePath(args[1]), true, null)
        if (fact.type.toString() == "item") AshValue.item(fact.display) else AshValue.item("")
    }
    regFn(
        scope,
        "effect_fact",
        AshType.EFFECT,
        listOf("cls" to AshType.CLASS, "path" to AshType.PATH, "monster" to AshType.MONSTER),
    ) { _, args ->
        val monster = MonsterDatabase.getByName(args[2].toString()) ?: return@regFn AshValue.effect("")
        val fact = FactDatabase.getFact(monster, resolveClass(args[0]), resolvePath(args[1]), true, null)
        if (fact.type.toString() == "effect") AshValue.effect(fact.display) else AshValue.effect("")
    }
    regFn(
        scope,
        "numeric_fact",
        AshType.INT,
        listOf("cls" to AshType.CLASS, "path" to AshType.PATH, "monster" to AshType.MONSTER),
    ) { _, args ->
        val monster = MonsterDatabase.getByName(args[2].toString()) ?: return@regFn AshValue.ZERO
        val fact = FactDatabase.getFact(monster, resolveClass(args[0]), resolvePath(args[1]), true, null)
        val typeStr = fact.type.toString()
        if (typeStr == "meat" || typeStr == "stat" || typeStr == "hp") {
            AshValue.of(fact.display.filter { it.isDigit() || it == '-' }.toLongOrNull() ?: 0L)
        } else AshValue.ZERO
    }
    regFn(
        scope,
        "string_fact",
        AshType.STRING,
        listOf("cls" to AshType.CLASS, "path" to AshType.PATH, "monster" to AshType.MONSTER),
    ) { _, args ->
        val monster = MonsterDatabase.getByName(args[2].toString())
            ?: return@regFn AshValue.EMPTY_STRING
        AshValue.of(FactDatabase.factString(monster, resolveClass(args[0]), resolvePath(args[1]), null))
    }
}
