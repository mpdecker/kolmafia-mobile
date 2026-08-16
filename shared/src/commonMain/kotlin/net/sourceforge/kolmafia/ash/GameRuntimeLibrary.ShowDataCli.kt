package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.character.ConsumptionEligibility
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.EffectDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.KolGameHolidayCalendar
import net.sourceforge.kolmafia.data.SkillDefinitionDatabase
import net.sourceforge.kolmafia.data.SkillDefinitionProxy
import net.sourceforge.kolmafia.effect.EffectData
import net.sourceforge.kolmafia.familiar.FamiliarData
import net.sourceforge.kolmafia.inventory.CollectionCacheSync
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.modifiers.BooleanModifier
import net.sourceforge.kolmafia.modifiers.DoubleModifier
import net.sourceforge.kolmafia.request.UneffectSkillEffectMap

/**
 * Desktop ShowDataCommand `status`, `modifiers [filter]`, `effects [filter]`,
 * `inv`/`inventory [filter]`, `closet`/`closet list [filter]`, `storage [filter]`,
 * `display [filter]`, `familiars [filter]`, `outfit` / `outfit list [filter]`,
 * `moon`/`moons`, and PlayerSnapshotCommand `log snapshot` / `log a,b,c`
 * (plus FamiliarCommand `familiar` / `familiar list [filter]`).
 * Type-less leftover is a case-insensitive substring of the formatted line.
 */
internal fun GameRuntimeLibrary.cliStatus(rt: AshRuntimeContext) {
    val cs = character?.state?.value ?: return
    val skills = skillManager?.state?.value?.skills.orEmpty()
    val mods = buildCurrentModifiers()
    val stomach = ConsumptionEligibility.stomachCapacity(cs, skills, mods)
    val liver = ConsumptionEligibility.liverCapacity(cs, skills, mods)
    val spleen = ConsumptionEligibility.spleenCapacity(cs)
    rt.print("Name: ${cs.name}")
    rt.print("Class: ${cs.className}")
    rt.print("")
    rt.print("Lv: ${cs.level}")
    rt.print("HP: ${cs.currentHp} / ${formatGroupedInt(cs.maxHp)}")
    rt.print("MP: ${cs.currentMp} / ${formatGroupedInt(cs.maxMp)}")
    rt.print("")
    rt.print("Mus: ${formatStatString(cs.baseMusc, cs.buffedMusc, cs.muscleTNP)}")
    rt.print("Mys: ${formatStatString(cs.baseMyst, cs.buffedMyst, cs.mystTNP)}")
    rt.print("Mox: ${formatStatString(cs.baseMoxie, cs.buffedMoxie, cs.moxieTNP)}")
    rt.print("")
    rt.print("Advs: ${cs.adventuresLeft}")
    rt.print("Meat: ${formatGroupedInt(cs.meat)}")
    rt.print("")
    rt.print("Full: ${cs.fullness} / $stomach")
    rt.print("Drunk: ${cs.inebriety} / $liver")
    rt.print("Spleen: ${cs.spleenUsed} / $spleen")
    rt.print("")
}

internal fun GameRuntimeLibrary.cliModifiers(filter: String, rt: AshRuntimeContext) {
    val leftover = filter.trim()
    val mods = buildCurrentModifiers()
    val state = character?.state?.value
    val loc = lastLocationName()
    fun emit(line: String) {
        if (leftover.isNotEmpty() && !line.contains(leftover, ignoreCase = true)) return
        rt.print(line)
    }
    emit("ML: ${CombatAdjustment.monsterLevelAdjustment(mods, state, loc)}")
    emit("Enc: ${formatRoundedModifier(CombatAdjustment.combatRateModifier(mods, loc))}%")
    emit("Init: ${formatRoundedModifier(CombatAdjustment.initiativeModifier(mods))}%")
    emit("")
    emit("Exp: ${formatRoundedModifier(CombatAdjustment.experienceBonus(mods, state))}")
    emit("Meat: ${formatRoundedModifier(CombatAdjustment.meatDropModifier(mods))}%")
    emit("Item: ${formatRoundedModifier(CombatAdjustment.itemDropModifier(mods))}%")
    emit("")
}

internal fun GameRuntimeLibrary.cliEquipment(filter: String, rt: AshRuntimeContext) {
    val cs = character?.state?.value ?: return
    val leftover = filter.trim()
    fun emit(line: String) {
        if (leftover.isNotEmpty() && !line.contains(leftover, ignoreCase = true)) return
        rt.print(line)
    }
    fun equipped(slot: EquipmentSlot): String =
        cs.equipment[slot]?.takeIf { it.isNotBlank() } ?: "none"

    if (cs.inHatTrick && cs.hatTrickHatIds.isNotEmpty()) {
        for (hatId in cs.hatTrickHatIds) {
            val hatName = ItemDatabase.getById(hatId)?.name ?: "Item #$hatId"
            emit("Hat: $hatName")
            if (hatName.equals("Crown of Thrones", ignoreCase = true)) {
                emit("Carrying: ${cs.enthronedFamiliarName.ifBlank { "none" }}")
            }
        }
    } else {
        val hat = equipped(EquipmentSlot.HAT)
        emit("Hat: $hat")
        if (hat.equals("Crown of Thrones", ignoreCase = true)) {
            emit("Carrying: ${cs.enthronedFamiliarName.ifBlank { "none" }}")
        }
    }
    emit("Weapon: ${equipped(EquipmentSlot.WEAPON)}")
    emit("Off-hand: ${equipped(EquipmentSlot.OFFHAND)}")
    emit("Shirt: ${equipped(EquipmentSlot.SHIRT)}")
    emit("Pants: ${equipped(EquipmentSlot.PANTS)}")
    val container = equipped(EquipmentSlot.CONTAINER)
    if (container != "none") {
        emit("Back: $container")
        if (container.equals("Buddy Bjorn", ignoreCase = true)) {
            emit("Carrying: ${cs.bjornedFamiliarName.ifBlank { "none" }}")
        }
    }
    emit("")
    emit("Acc. 1: ${equipped(EquipmentSlot.ACC1)}")
    emit("Acc. 2: ${equipped(EquipmentSlot.ACC2)}")
    emit("Acc. 3: ${equipped(EquipmentSlot.ACC3)}")
    emit("")
    val pet = if (cs.familiarName.isBlank()) {
        "none"
    } else {
        "${cs.familiarName} (${cs.familiarWeight} lbs)"
    }
    emit("Pet: $pet")
    emit("Item: ${equipped(EquipmentSlot.FAMILIAR)}")
    val stickers = EquipmentSlot.STICKER_SLOTS.map { equipped(it) }
    if (stickers.any { it != "none" }) {
        emit("")
        emit("Sticker 1: ${stickers[0]}")
        emit("Sticker 2: ${stickers[1]}")
        emit("Sticker 3: ${stickers[2]}")
    }
}

internal fun GameRuntimeLibrary.cliLogSnapshot(parameters: String, rt: AshRuntimeContext) {
    val trimmed = parameters.trim()
    val options = if (trimmed.equals("snapshot", ignoreCase = true)) {
        listOf("moon", "status", "equipment", "skills", "effects", "modifiers")
    } else {
        trimmed.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    }
    val banner = "=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-="
    val title = "Player Snapshot"
    val paddedTitle = " ".repeat(((46 - title.length) / 2).coerceAtLeast(0)) + title
    val wrapped = SnapshotPrintContext(rt, sessionLogger)
    wrapped.print(banner)
    wrapped.print(paddedTitle)
    wrapped.print(banner)
    for (option in options) {
        when (option.lowercase()) {
            "moon", "moons" -> cliMoon(wrapped)
            "status" -> cliStatus(wrapped)
            "equip", "equipment" -> cliEquipment("", wrapped)
            "skill", "skills" -> cliSkills("", wrapped)
            "effect", "effects" -> cliEffects("", wrapped)
            "modifier", "modifiers" -> cliModifiers("", wrapped)
            "outfit", "outfits" -> cliOutfits("", wrapped)
            else -> { /* unknown options no-op */ }
        }
    }
    wrapped.print(banner)
}

private class SnapshotPrintContext(
    private val delegate: AshRuntimeContext,
    private val logger: net.sourceforge.kolmafia.session.SessionLogger?,
) : AshRuntimeContext {
    override fun print(msg: String) {
        delegate.print(msg)
        logger?.appendRawLine(msg)
    }

    override fun lastCombatAction(): String = delegate.lastCombatAction()

    override fun setCombatAction(action: String) = delegate.setCombatAction(action)
}

internal fun GameRuntimeLibrary.cliMoon(rt: AshRuntimeContext) {
    rt.print(KolGameHolidayCalendar.getCalendarDayAsString())
    rt.print("")
    rt.print("Ronald: ${KolGameHolidayCalendar.getRonaldPhaseAsString()}")
    rt.print("Grimace: ${KolGameHolidayCalendar.getGrimacePhaseAsString()}")
    rt.print("Mini-moon: ${KolGameHolidayCalendar.getMiniMoonAsString()}")
    rt.print("")
    for (prediction in KolGameHolidayCalendar.getHolidayPredictions()) {
        rt.print(prediction)
    }
    rt.print("")
    rt.print(KolGameHolidayCalendar.getHoliday())
    rt.print(KolGameHolidayCalendar.getMoonEffect())
    rt.print("")
}

internal fun GameRuntimeLibrary.cliOutfits(filter: String, rt: AshRuntimeContext) {
    val leftover = filter.trim()
    kotlinx.coroutines.runBlocking {
        val names = outfitManager?.getOutfitsWithPieces()?.map { it.name } ?: emptyList()
        for (name in names) {
            if (leftover.isNotEmpty() && !name.contains(leftover, ignoreCase = true)) continue
            rt.print(name)
        }
    }
}

internal fun GameRuntimeLibrary.cliEffects(filter: String, rt: AshRuntimeContext) {
    runBlocking { effectManager?.fetchEffects() }
    val effects = effectManager?.state?.value?.effects.orEmpty()
    var nBuffs = 0
    for (effect in effects) {
        val skillName = UneffectSkillEffectMap.effectToSkill(effect.name) ?: continue
        val skillId = SkillDefinitionDatabase.getByName(skillName)?.id ?: continue
        if (SkillDefinitionProxy.isAccordionThiefSong(skillId)) nBuffs++
    }
    rt.print("$nBuffs of ${songLimit()} AT buffs active.")
    val leftover = filter.trim()
    for (effect in effects) {
        val line = formatEffectLine(effect)
        if (leftover.isNotEmpty() && !line.contains(leftover, ignoreCase = true)) continue
        rt.print(line)
    }
}

internal fun GameRuntimeLibrary.cliInventory(filter: String, rt: AshRuntimeContext) {
    runBlocking {
        inventoryManager?.fetchInventory()
        inventoryManager?.syncCharacterEquipment()
    }
    val leftover = filter.trim()
    val items = inventoryManager?.state?.value?.items?.values.orEmpty()
    for (item in items) {
        val line = formatInventoryLine(item)
        if (leftover.isNotEmpty() && !line.contains(leftover, ignoreCase = true)) continue
        rt.print(line)
    }
}

internal fun GameRuntimeLibrary.songLimit(): Int {
    val values = buildCurrentModifiers().values
    var limit = 3
    if (values.get(BooleanModifier.FOUR_SONGS)) limit++
    limit += values.getInt(DoubleModifier.ADDITIONAL_SONG)
    return limit
}

internal fun GameRuntimeLibrary.formatEffectLine(effect: EffectData): String {
    var name = effect.name
    if (name.equals("On the Trail", ignoreCase = true)) {
        val monster = preferences?.getString("olfactedMonster", "").orEmpty()
        if (monster.isNotEmpty() && !monster.equals("unknown", ignoreCase = true)) {
            name = "$name [$monster]"
        }
    } else {
        if (EffectDatabase.getByName(effect.name)?.isSong() == true) {
            name = "♫ $name"
        }
        val skillName = UneffectSkillEffectMap.effectToSkill(effect.name)
        val skillId = skillName?.let { SkillDefinitionDatabase.getByName(it)?.id }
        if (skillId != null && SkillDefinitionProxy.isExpression(skillId)) {
            name = "☺ $name"
        }
    }
    return when {
        effect.duration == 1 -> name
        effect.duration < 0 -> "$name (∞)"
        else -> "$name (${effect.duration})"
    }
}

internal fun GameRuntimeLibrary.cliCloset(filter: String, rt: AshRuntimeContext) {
    val contents = runBlocking {
        val req = closetRequest ?: return@runBlocking emptyMap()
        val fetched = req.fetchContents()
        preferences?.let { CollectionCacheSync.saveCloset(it, fetched) }
        fetched
    }
    printItemMap(contents, filter, rt)
}

internal fun GameRuntimeLibrary.cliStorage(filter: String, rt: AshRuntimeContext) {
    val contents = runBlocking {
        val req = storageRequest ?: return@runBlocking emptyMap()
        val classified = req.fetchClassifiedContents(character?.state?.value, preferences)
        preferences?.let { CollectionCacheSync.saveStorage(it, classified.storage, classified.freepulls) }
        classified.storage
    }
    printItemMap(contents, filter, rt)
}

internal fun GameRuntimeLibrary.cliDisplay(filter: String, rt: AshRuntimeContext) {
    val contents = runBlocking {
        val req = displayCaseRequest ?: return@runBlocking emptyMap()
        val fetched = req.fetchContents()
        preferences?.let { CollectionCacheSync.saveDisplay(it, fetched) }
        fetched
    }
    printItemMap(contents, filter, rt)
}

internal fun GameRuntimeLibrary.cliFamiliars(filter: String, rt: AshRuntimeContext) {
    runBlocking { familiarManager?.fetchFamiliars() }
    val leftover = filter.trim()
    for (fam in familiarManager?.state?.value?.ownedFamiliars.orEmpty()) {
        val line = formatFamiliarLine(fam)
        if (leftover.isNotEmpty() && !line.contains(leftover, ignoreCase = true)) continue
        rt.print(line)
    }
}

internal fun GameRuntimeLibrary.printItemMap(
    contents: Map<Int, Int>,
    filter: String,
    rt: AshRuntimeContext,
) {
    val leftover = filter.trim()
    for ((id, qty) in contents) {
        val name = gameDatabase?.item(id)?.name
            ?: ItemDatabase.getById(id)?.name
            ?: "Item #$id"
        val line = formatInventoryLine(InventoryItem(id, name, qty, ItemType.OTHER))
        if (leftover.isNotEmpty() && !line.contains(leftover, ignoreCase = true)) continue
        rt.print(line)
    }
}

internal fun formatInventoryLine(item: InventoryItem): String =
    if (item.quantity == 1) item.name else "${item.name} (${item.quantity})"

/** Desktop `KoLConstants.COMMA_FORMAT` grouping for ShowData numeric fields. */
internal fun formatGroupedInt(value: Int): String {
    val negative = value < 0
    val digits = kotlin.math.abs(value).toString()
    val grouped = digits.reversed().chunked(3).joinToString(",").reversed()
    return if (negative) "-$grouped" else grouped
}

/** Desktop `ShowDataCommand.getStatString`. */
internal fun formatStatString(base: Int, buffed: Int, tnp: Int): String {
    val core = if (base != buffed) {
        "${formatGroupedInt(buffed)} (${formatGroupedInt(base)})"
    } else {
        formatGroupedInt(buffed)
    }
    return "$core, tnp = ${formatGroupedInt(tnp)}"
}

internal fun formatFamiliarLine(fam: FamiliarData): String =
    "${fam.race} (${fam.weight} lbs)"

internal data class AccordionListing(
    val itemId: Int,
    val fallbackName: String,
    val source: String,
)

/** Desktop [AccordionsCommand] id/source table. */
internal val ACCORDION_LISTINGS = listOf(
    AccordionListing(11, "stolen accordion", "starter item"),
    AccordionListing(50, "Rock and Roll Legend", "Epic Weapon"),
    AccordionListing(2557, "Squeezebox of the Ages", "Legendary Epic Weapon"),
    AccordionListing(4321, "The Trickster's Trikitixa", "Your Nemesis"),
    AccordionListing(6810, "beer-battered accordion", "drunken half-orc hobo"),
    AccordionListing(6811, "baritone accordion", "bar"),
    AccordionListing(6812, "mama's squeezebox", "werecougar"),
    AccordionListing(6813, "guancertina", "perpendicular bat"),
    AccordionListing(6814, "accordion file", "Knob Goblin Accountant"),
    AccordionListing(6815, "accord ion", "Hellion"),
    AccordionListing(6816, "bone bandoneon", "toothy sklelton"),
    AccordionListing(6817, "pentatonic accordion", "Ninja Snowman (Chopsticks)"),
    AccordionListing(6819, "Accordion of Jordion", "1335 HaXx0r"),
    AccordionListing(6818, "non-Euclidean non-accordion", "cubist bull"),
    AccordionListing(6820, "autocalliope", "Steampunk Giant"),
    AccordionListing(6823, "ghost accordion", "skeletal sommelier"),
    AccordionListing(6822, "pygmy concertinette", "drunk pygmy"),
    AccordionListing(6821, "accordionoid rocca", "drab bard"),
    AccordionListing(6824, "peace accordion", "War Hippy (space) Cadet"),
    AccordionListing(6825, "alarm accordion", "alert mariachi"),
    AccordionListing(6856, "Bal-musette accordion", "depressing French accordionist"),
    AccordionListing(6857, "Cajun accordion", "lively Cajun accordionist"),
    AccordionListing(6858, "quirky accordion", "quirky indie-rock accordionist"),
    AccordionListing(7029, "Shakespeare's Sister's Accordion", "smithed using The Smith's Tome"),
)

internal fun GameRuntimeLibrary.cliAccordions(rt: AshRuntimeContext) {
    val stolenToday = preferences?.getString("_stolenAccordions", "").orEmpty()
        .split(',')
        .mapNotNull { it.trim().toIntOrNull() }
        .toSet()
    val inv = inventoryManager?.state?.value?.items ?: emptyMap()
    val equipped = character?.state?.value?.equipment?.values
        ?.map { it.lowercase() }
        ?.toSet()
        ?: emptySet()
    for (acc in ACCORDION_LISTINGS) {
        val name = ItemDatabase.getItemName(acc.itemId).ifBlank { acc.fallbackName }
        val have = (inv[acc.itemId]?.quantity ?: 0) > 0 ||
            equipped.contains(name.lowercase())
        val today = acc.itemId in stolenToday
        val haveToday = "${if (have) "yes" else "no"}/${if (today) "yes" else "no"}"
        rt.print("$name | $haveToday | ${acc.source}")
    }
}

/** Desktop `KoLConstants.ROUNDED_MODIFIER_FORMAT` at one decimal. */
internal fun formatRoundedModifier(value: Double): String {
    val scaled = kotlin.math.round(value * 10.0).toInt()
    val whole = scaled / 10
    val frac = kotlin.math.abs(scaled % 10)
    return "$whole.$frac"
}
