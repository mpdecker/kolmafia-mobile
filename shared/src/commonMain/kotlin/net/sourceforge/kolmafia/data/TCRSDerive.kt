package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.character.CharacterClass
import net.sourceforge.kolmafia.character.ZodiacSign
import net.sourceforge.kolmafia.modifiers.DoubleModifier
import net.sourceforge.kolmafia.utilities.PHPMTRandom
import net.sourceforge.kolmafia.utilities.PHPRandom
import net.sourceforge.kolmafia.utilities.PHPRandomSelection

/**
 * Desktop [TCRSDatabase] PRNG derive: class/sign item + cafe maps.
 * HTML introspect of unknown/live items stays in [TCRSDeriver].
 */
object TCRSDerive {

    private const val FISHY = 549
    private const val TIKI_TEMERITY = 2468

    private val effectPool = ArrayList<Int>()

    private val tcrsGeneric = setOf(1599, 1961, 8462, 8899)
    val notReRolled = setOf(
        10847, 10848, 10849, 9745, 10336, 10800,
        10252, 9574, 9910, 10749, 11186, 11306, 11325, 11339,
        11391, 11392, 11393, 11542, 11609, 11975, 11919, 10899,
        10647, 12067, 12181, 12216, 12259,
    )
    private val unalteredConsumables = setOf(5672, 5673)
    private val zeroSizeConsumables = setOf(4412, 4413, 5071)
    private val zeroAdventureConsumables = setOf(9126)
    private val hardcodedEffect = setOf(
        1406, 1407, 1408, 2198, 2199, 2200, 3169, 3489, 3490, 3491,
        3639, 4535, 4606, 4607, 4821, 4839, 4840, 5068, 5240, 5248,
    )
    private val hardcodedEffectDynamicDuration = setOf(4535, 3169)
    private val hardcodedEffectOverride = mapOf(
        2199 to 256,
        3169 to 598,
        4535 to 755,
        4606 to 775,
    )

    private val collapsibleFamilies = listOf(
        setOf(
            DoubleModifier.HOT_RESISTANCE,
            DoubleModifier.COLD_RESISTANCE,
            DoubleModifier.SPOOKY_RESISTANCE,
            DoubleModifier.STENCH_RESISTANCE,
            DoubleModifier.SLEAZE_RESISTANCE,
        ),
        setOf(
            DoubleModifier.HOT_DAMAGE,
            DoubleModifier.COLD_DAMAGE,
            DoubleModifier.SPOOKY_DAMAGE,
            DoubleModifier.STENCH_DAMAGE,
            DoubleModifier.SLEAZE_DAMAGE,
        ),
    )
    private val regenMods = setOf(
        DoubleModifier.HP_REGEN_MIN,
        DoubleModifier.HP_REGEN_MAX,
        DoubleModifier.MP_REGEN_MIN,
        DoubleModifier.MP_REGEN_MAX,
    )
    private val unsupportedFunctions = listOf("pref(", "env(", "zone(", "effect(", "class(", "path(")

    fun seedFor(itemId: Int, ascensionClass: CharacterClass, sign: ZodiacSign): Int =
        (50 * itemId) + (12345 * sign.id) + (100000 * ascensionClass.id)

    fun qualityToTurnsPerFullness(quality: ConsumableQuality): Int = when (quality) {
        ConsumableQuality.EPIC,
        ConsumableQuality.SUPER_EPIC,
        ConsumableQuality.SUPER_ULTRA_EPIC,
        ConsumableQuality.SUPER_ULTRA_MEGA_EPIC,
        ConsumableQuality.SUPER_ULTRA_MEGA_TURBO_EPIC,
        ConsumableQuality.SUPER_MEGA_EPIC,
        -> 5
        ConsumableQuality.AWESOME -> 4
        ConsumableQuality.GOOD -> 3
        ConsumableQuality.DECENT -> 2
        ConsumableQuality.CRAPPY -> 1
        else -> 0
    }

    fun deriveItem(
        ascensionClass: CharacterClass,
        sign: ZodiacSign,
        itemId: Int,
    ): TCRSDatabase.TcrsEntry? {
        ensureReady()
        val item = ItemDatabase.getById(itemId)
        if (item == null || ItemDatabase.isRegisteredLive(itemId)) {
            return introspectItem(itemId)
        }
        var type = item.primaryUse
        val displayName = ModifierDatabase.getStringModifier(item.name, net.sourceforge.kolmafia.modifiers.StringModifier.DISPLAY_NAME)
        if (itemId in notReRolled || displayName.isNotEmpty()) {
            val name = displayName.ifEmpty { item.name }
            val size = sizeFor(itemId)
            val quality = ConsumableDatabase.getQuality(itemId)
            return TCRSDatabase.TcrsEntry(name, size, quality.tcrsDumpName(), unalteredModifiers(itemId).toString())
        }
        if (itemId == 10207) type = ItemPrimaryUse.NONE
        return when (type) {
            ItemPrimaryUse.POTION, ItemPrimaryUse.AVATAR -> derivePotion(ascensionClass, sign, itemId)
            ItemPrimaryUse.FOOD, ItemPrimaryUse.DRINK ->
                deriveFoodBooze(ascensionClass, sign, itemId, type == ItemPrimaryUse.FOOD)
            ItemPrimaryUse.SPLEEN -> deriveSpleen(ascensionClass, sign, itemId)
            ItemPrimaryUse.HAT, ItemPrimaryUse.SHIRT, ItemPrimaryUse.CONTAINER,
            ItemPrimaryUse.WEAPON, ItemPrimaryUse.OFFHAND, ItemPrimaryUse.PANTS,
            ItemPrimaryUse.ACCESSORY,
            -> deriveEquipment(ascensionClass, sign, itemId)
            else -> deriveGeneric(ascensionClass, sign, itemId)
        }
    }

    fun deriveCafe(
        ascensionClass: CharacterClass,
        sign: ZodiacSign,
        id: Int,
        isFood: Boolean,
    ): TCRSDatabase.TcrsEntry? {
        ensureReady()
        val baseName = (if (isFood) CafeDatabase.getCafeFoodName(id) else CafeDatabase.getCafeBoozeName(id))
            ?: return null
        val seed = seedFor(id, ascensionClass, sign)
        val mtRng = PHPMTRandom(seed.toLong())
        val rng = PHPRandom(seed.toLong())
        val cosmeticsString = rollCosmetics(mtRng, rng, 10)
        val qualityRoll = mtRng.nextInt(1, 7)
        val quality = if (isFood) determineFoodQuality(qualityRoll, false) else determineBoozeQuality(qualityRoll)
        val size = rollConsumableSize(mtRng)
        val adjectives = ArrayList<String>()
        addSizeAndQualityAdjectives(adjectives, mtRng, isFood, size, quality)
        if (qualityToTurnsPerFullness(quality) * size >= 8) {
            mtRng.nextDouble()
        }
        if (mtRng.nextInt(1, 10) == 1) {
            adjectives.add(mtRng.pickOne(TCRSStringTables.list("Food Enchantment")))
        }
        rng.shuffle(adjectives)
        adjectives.reverse()
        adjectives.add(cosmeticsString)
        adjectives.add(baseName)
        val name = adjectives.filter { it.isNotBlank() }.joinToString(" ")
        val baseAdventures = ConsumableDatabase.getBaseAverageAdventuresByName(baseName)
        return TCRSDatabase.TcrsEntry(name, size, promoteEpic(quality, size, baseAdventures).tcrsDumpName(), "")
    }

    fun deriveAll(ascensionClass: CharacterClass, sign: ZodiacSign): Int {
        TCRSDatabase.derive(ascensionClass.displayName, sign.signName)
        return TCRSDatabase.entryCount()
    }

    fun introspectItem(itemId: Int): TCRSDatabase.TcrsEntry? = TCRSDeriver.deriveFromCache(itemId)

    fun updateMissing(onItem: (Int) -> TCRSDatabase.TcrsEntry?): Int {
        var count = 0
        for (id in ItemDatabase.allIds().sorted()) {
            if (id == 10254) {
                val existing = TCRSDatabase.getEntry(id)
                if (existing != null && existing.name == "hewn moon-rune spoon") {
                    TCRSDatabase.removeDerivedEntry(id)
                }
            }
            if (TCRSDatabase.getEntry(id) != null) continue
            val entry = onItem(id) ?: continue
            TCRSDatabase.putDerivedEntry(id, entry)
            count++
        }
        return count
    }

    internal fun derivePotion(
        ascensionClass: CharacterClass,
        sign: ZodiacSign,
        itemId: Int,
    ): TCRSDatabase.TcrsEntry {
        val seed = seedFor(itemId, ascensionClass, sign)
        val mtRng = PHPMTRandom(seed.toLong())
        val rng = PHPRandom(seed.toLong())
        val cosmeticsString = rollCosmetics(mtRng, rng, 6)
        if (itemId in tcrsGeneric) {
            val name = joinName(cosmeticsString, removeAdjectives(ItemDatabase.getItemName(itemId)))
            return TCRSDatabase.TcrsEntry(name, 0, "", unalteredModifiers(itemId).toString())
        }
        val mods = getRetainedModifiers(itemId)
        val potionMods = ArrayList<String>()
        var numPotionMods = 1
        if (mtRng.nextInt(1, 3) == 1) numPotionMods++
        if (mtRng.nextInt(1, 3) == 1) numPotionMods++
        repeat(numPotionMods) {
            potionMods.add(mtRng.pickOne(TCRSStringTables.list("Potion Mod")))
        }
        val effectName = effectNameAt(mtRng.nextInt(0, effectPool.size))
        val duration = mtRng.nextInt(11, 69)
        val potionPrefixes = TCRSStringTables.list("Potion Prefix")
        val prefixedPotionMods = ArrayList<String>()
        for (mod in potionMods) {
            var next = mod
            val prefixRoll = mtRng.nextInt(1, 40)
            if (prefixRoll <= potionPrefixes.size) {
                next = potionPrefixes[prefixRoll - 1] + "-" + next
            }
            prefixedPotionMods.add(0, next)
        }
        val potionString = prefixedPotionMods.joinToString(" ")
        if (effectName.isNotBlank()) {
            mods.addModifier("Effect", effectName)
            mods.addModifier("Effect Duration", duration.toString())
        }
        val name = joinName(
            potionString,
            cosmeticsString,
            removeAdjectives(ItemDatabase.getItemName(itemId)),
        )
        return TCRSDatabase.TcrsEntry(name, 0, "", mods.toString())
    }

    internal fun deriveFoodBooze(
        ascensionClass: CharacterClass,
        sign: ZodiacSign,
        itemId: Int,
        isFood: Boolean,
    ): TCRSDatabase.TcrsEntry {
        val seed = seedFor(itemId, ascensionClass, sign)
        val mtRng = PHPMTRandom(seed.toLong())
        val rng = PHPRandom(seed.toLong())
        val beverage = ConsumableDatabase.isBeverage(itemId)
        val cosmeticsString = rollCosmetics(mtRng, rng, if (beverage) 8 else 10)
        if (itemId in unalteredConsumables) {
            val name = joinName(cosmeticsString, removeAdjectives(ItemDatabase.getItemName(itemId)))
            val mods = getRetainedModifiers(itemId)
            return TCRSDatabase.TcrsEntry(
                name,
                sizeFor(itemId),
                ConsumableDatabase.getQuality(itemId).tcrsDumpName(),
                mods.toString(),
            )
        }
        val qualityRoll = mtRng.nextInt(1, 7)
        var quality = if (isFood) determineFoodQuality(qualityRoll, beverage) else determineBoozeQuality(qualityRoll)
        var size = if (beverage) 1 else rollConsumableSize(mtRng)
        val adjectives = ArrayList<String>()
        if (!beverage) {
            addSizeAndQualityAdjectives(adjectives, mtRng, isFood, size, quality)
        }
        if (qualityToTurnsPerFullness(quality) * size >= 8) {
            mtRng.nextDouble()
        }
        val mods = getRetainedModifiers(itemId)
        val rolledEnchantment = mtRng.nextInt(1, 10) == 1
        if (rolledEnchantment) {
            adjectives.add(mtRng.pickOne(TCRSStringTables.list("Food Enchantment")))
        }
        var enchantment = rollConsumableEnchantment(mtRng)
        val hardcoded = itemId in hardcodedEffect
        val dynamicDuration = itemId in hardcodedEffectDynamicDuration
        if (hardcoded) {
            val overrideId = hardcodedEffectOverride[itemId]
            enchantment = Enchantment(
                effect = if (overrideId != null) {
                    disambiguatedEffectName(overrideId)
                } else {
                    ModifierDatabase.getStringModifier(
                        ItemDatabase.getItemName(itemId),
                        net.sourceforge.kolmafia.modifiers.StringModifier.EFFECT,
                    )
                },
                duration = if (dynamicDuration) {
                    enchantment.duration
                } else {
                    ModifierDatabase.getItem(ItemDatabase.getItemName(itemId))
                        ?.let {
                            net.sourceforge.kolmafia.modifiers.ModifierParser.parse(it.modifiers)
                                .getInt(DoubleModifier.EFFECT_DURATION)
                        } ?: 0
                },
            )
        }
        val enchanted = hardcoded || rolledEnchantment
        if (enchanted && enchantment.effect.isNotBlank()) {
            mods.addModifier("Effect", enchantment.effect)
            if (rolledEnchantment || !dynamicDuration) {
                mods.addModifier("Effect Duration", enchantment.duration.toString())
            }
        }
        if (itemId in zeroSizeConsumables) size = 0
        rng.shuffle(adjectives)
        adjectives.reverse()
        adjectives.add(cosmeticsString)
        adjectives.add(removeAdjectives(ItemDatabase.getItemName(itemId)))
        val name = adjectives.filter { it.isNotBlank() }.joinToString(" ")
        val baseAdventures = if (itemId in zeroAdventureConsumables) {
            0.0
        } else {
            ConsumableDatabase.getBaseAverageAdventures(itemId)
        }
        quality = promoteEpic(quality, size, baseAdventures)
        return TCRSDatabase.TcrsEntry(name, size, quality.tcrsDumpName(), mods.toString())
    }

    internal fun deriveSpleen(
        ascensionClass: CharacterClass,
        sign: ZodiacSign,
        itemId: Int,
    ): TCRSDatabase.TcrsEntry {
        val seed = seedFor(itemId, ascensionClass, sign)
        val mtRng = PHPMTRandom(seed.toLong())
        val rng = PHPRandom(seed.toLong())
        val cosmeticsString = rollCosmetics(mtRng, rng, 4)
        val quality = determineSpleenQuality(mtRng.nextInt(1, 7))
        val adjective = mtRng.pickOne(TCRSStringTables.list("Spleen Mod"))
        if (quality == ConsumableQuality.CRAPPY) {
            if (mtRng.nextInt(1, 6) == 6) mtRng.nextDouble()
        } else {
            mtRng.nextDouble()
            mtRng.nextDouble()
        }
        mtRng.nextDouble()
        val mods = getRetainedModifiers(itemId)
        if (mtRng.nextInt(1, 3) == 1) {
            val enchantment = rollConsumableEnchantment(mtRng)
            if (enchantment.effect.isNotBlank()) {
                mods.addModifier("Effect", enchantment.effect)
                mods.addModifier("Effect Duration", enchantment.duration.toString())
            }
        }
        val name = joinName(adjective, cosmeticsString, removeAdjectives(ItemDatabase.getItemName(itemId)))
        return TCRSDatabase.TcrsEntry(name, 1, quality.tcrsDumpName(), mods.toString())
    }

    internal fun deriveEquipment(
        ascensionClass: CharacterClass,
        sign: ZodiacSign,
        itemId: Int,
    ): TCRSDatabase.TcrsEntry {
        val seed = seedFor(itemId, ascensionClass, sign)
        val mtRng = PHPMTRandom(seed.toLong())
        val cosmeticList = buildCosmeticList(mtRng, 8)
        val root = removeAdjectives(ItemDatabase.getItemName(itemId))
        val mods = getRetainedModifiers(itemId)
        val count = enchantCount(itemId)
        val enchantRng = PHPRandom((seed + 10).toLong())
        val prefixes = ArrayList<String>()
        val suffixes = ArrayList<String>()
        for ((descriptor, value) in getMods(itemId, ascensionClass, sign, count, enchantRng)) {
            if (descriptor.startsWith("of ")) suffixes.add(descriptor)
            else prefixes.add(0, descriptor)
            TCRSModifierList.appendModifier(mods, value)
        }
        val shuffleRng = if (count == 0) PHPRandom(seed.toLong()) else enchantRng
        val cosmeticsString = shuffleCosmetics(cosmeticList, shuffleRng)
        val adjectives = TCRSStringTables.adjectives()
        val name = listOf(
            cosmeticsString,
            *prefixes.filter { it !in adjectives }.toTypedArray(),
            root,
            *suffixes.filter { it !in adjectives }.toTypedArray(),
        ).filter { it.isNotBlank() }.joinToString(" ")
        return TCRSDatabase.TcrsEntry(name, 0, "", mods.toString())
    }

    internal fun deriveGeneric(
        ascensionClass: CharacterClass,
        sign: ZodiacSign,
        itemId: Int,
    ): TCRSDatabase.TcrsEntry {
        val seed = seedFor(itemId, ascensionClass, sign)
        val mtRng = PHPMTRandom(seed.toLong())
        val rng = PHPRandom(seed.toLong())
        val cosmeticsString = rollCosmetics(mtRng, rng, 8)
        val name = joinName(cosmeticsString, removeAdjectives(ItemDatabase.getItemName(itemId)))
        val mods = unalteredModifiers(itemId)
        return TCRSDatabase.TcrsEntry(
            name,
            sizeFor(itemId),
            ConsumableDatabase.getQuality(itemId).tcrsDumpName(),
            mods.toString(),
        )
    }

    fun enchantCount(itemId: Int): Int {
        val raw = ModifierDatabase.getItem(ItemDatabase.getItemName(itemId))?.modifiers.orEmpty()
        val modifiers = TCRSModifierList.split(raw)
        if (modifiers.containsModifier("Enchantment Count")) {
            return modifiers.getModifierValue("Enchantment Count")?.toDoubleOrNull()?.toInt() ?: 0
        }
        val present = linkedMapOf<DoubleModifier, MutableSet<String>>()
        for (mv in modifiers.iterator().asSequence()) {
            val modifier = DoubleModifier.byTag(mv.name) ?: continue
            if (modifier.isEnchantment() && isEnchantableValue(mv.value)) {
                present.getOrPut(modifier) { linkedSetOf() }.add(mv.value.orEmpty())
            }
        }
        var count = 0
        val consumed = HashSet<DoubleModifier>()
        for (family in collapsibleFamilies) {
            var complete = true
            val values = HashSet<String>()
            for (modifier in family) {
                val found = present[modifier]
                if (found == null) {
                    complete = false
                    break
                }
                values.addAll(found)
            }
            if (complete && values.size == 1) {
                count += 1
                consumed.addAll(family)
            }
        }
        val regen = regenCount(present)
        if (regen > 0) {
            count += regen
            consumed.addAll(regenMods)
        }
        val isFamiliarEquipment = ItemDatabase.getById(itemId)?.primaryUse == ItemPrimaryUse.FAMILIAR
        for ((modifier, values) in present) {
            if (modifier in consumed) continue
            if (isFamiliarEquipment && modifier == DoubleModifier.FAMILIAR_WEIGHT) continue
            count += values.size
        }
        return count
    }

    private fun getMods(
        itemId: Int,
        ascensionClass: CharacterClass,
        sign: ZodiacSign,
        count: Int,
        enchantRng: PHPRandom,
    ): List<Pair<String, String>> {
        val seed = seedFor(itemId, ascensionClass, sign) + 10
        val table = TCRSStringTables.equipmentModifiers()
        val indices = PHPRandomSelection(enchantRng, PHPMTRandom(seed.toLong())).pick(table.size, count)
        return indices.map { table[it] }
    }

    private fun regenCount(present: Map<DoubleModifier, Set<String>>): Int {
        val hp = present.containsKey(DoubleModifier.HP_REGEN_MIN) || present.containsKey(DoubleModifier.HP_REGEN_MAX)
        val mp = present.containsKey(DoubleModifier.MP_REGEN_MIN) || present.containsKey(DoubleModifier.MP_REGEN_MAX)
        if (!hp || !mp) return if (hp || mp) 1 else 0
        val sameAmounts =
            present[DoubleModifier.HP_REGEN_MIN] == present[DoubleModifier.MP_REGEN_MIN] &&
                present[DoubleModifier.HP_REGEN_MAX] == present[DoubleModifier.MP_REGEN_MAX]
        return if (sameAmounts) 1 else 2
    }

    private fun isEnchantableValue(value: String?): Boolean {
        if (value == null || !value.startsWith("[")) return true
        return unsupportedFunctions.none { value.contains(it) }
    }

    private fun unalteredModifiers(itemId: Int): TCRSModifierList {
        val mods = TCRSModifierList()
        for (mv in rawModifiers(itemId).iterator().asSequence()) {
            if (mv.name != "Enchantment Count") mods.addModifier(mv)
        }
        return mods
    }

    private fun getRetainedModifiers(itemId: Int): TCRSModifierList {
        val retained = TCRSModifierList()
        for (mv in rawModifiers(itemId).iterator().asSequence()) {
            val name = mv.name
            if (name == "Effect" || name == "Effect Duration") continue
            if (name == "Enchantment Count") continue
            if (TCRSModifierList.isEnchantment(name) && !isExpression(mv.value)) continue
            retained.addModifier(mv)
        }
        return retained
    }

    private fun rawModifiers(itemId: Int): TCRSModifierList {
        val raw = ModifierDatabase.getItem(ItemDatabase.getItemName(itemId))?.modifiers.orEmpty()
        return TCRSModifierList.split(raw)
    }

    private fun isExpression(value: String?): Boolean = value != null && value.contains("[")

    private fun sizeFor(itemId: Int): Int {
        val item = ItemDatabase.getById(itemId) ?: return 0
        val name = item.name
        return when (item.primaryUse) {
            ItemPrimaryUse.FOOD -> ConsumableDatabase.getFullnessByName(name)
            ItemPrimaryUse.DRINK -> ConsumableDatabase.getInebrietyByName(name)
            ItemPrimaryUse.SPLEEN -> ConsumableDatabase.getSpleenByName(name)
            else -> 0
        }
    }

    private fun removeAdjectives(name: String): String =
        name.split(' ').filter { it !in TCRSStringTables.adjectives() }.joinToString(" ")

    private fun joinName(vararg parts: String): String =
        parts.filter { it.isNotBlank() }.joinToString(" ")

    private fun buildCosmeticList(mtRng: PHPMTRandom, max: Int): ArrayList<String> {
        val cosmeticMods = ArrayList<String>()
        if (mtRng.nextInt(1, max) == 1) {
            cosmeticMods.add(mtRng.pickOne(TCRSStringTables.list("Color")))
        }
        var numCosmeticMods = 0
        if (mtRng.nextInt(1, max) == 1) numCosmeticMods++
        if (mtRng.nextInt(1, max) == 1) numCosmeticMods++
        if (mtRng.nextInt(1, max) == 1) numCosmeticMods++
        repeat(numCosmeticMods) {
            cosmeticMods.add(mtRng.pickOne(TCRSStringTables.list("Cosmetic")))
        }
        return cosmeticMods
    }

    private fun shuffleCosmetics(cosmeticMods: ArrayList<String>, rng: PHPRandom): String {
        if (cosmeticMods.isNotEmpty()) rng.shuffle(cosmeticMods)
        cosmeticMods.reverse()
        return cosmeticMods.joinToString(" ")
    }

    private fun rollCosmetics(mtRng: PHPMTRandom, rng: PHPRandom, max: Int): String =
        shuffleCosmetics(buildCosmeticList(mtRng, max), rng)

    private data class Enchantment(val effect: String, val duration: Int)

    private fun effectNameAt(roll: Int): String {
        if (effectPool.isEmpty()) return ""
        val index = minOf(roll, effectPool.size - 1)
        return disambiguatedEffectName(effectPool[index])
    }

    private fun rollConsumableEnchantment(mtRng: PHPMTRandom): Enchantment {
        val effectName = effectNameAt(mtRng.nextInt(0, effectPool.size))
        val duration = 5 * mtRng.nextInt(1, 10)
        return Enchantment(effectName, duration)
    }

    private fun determineFoodQuality(qualityRoll: Int, beverage: Boolean): ConsumableQuality = when (qualityRoll) {
        1 -> ConsumableQuality.CRAPPY
        2 -> if (beverage) ConsumableQuality.DECENT else ConsumableQuality.CRAPPY
        3 -> ConsumableQuality.DECENT
        4 -> if (beverage) ConsumableQuality.GOOD else ConsumableQuality.DECENT
        5 -> ConsumableQuality.GOOD
        6 -> if (beverage) ConsumableQuality.AWESOME else ConsumableQuality.GOOD
        7 -> if (beverage) ConsumableQuality.EPIC else ConsumableQuality.AWESOME
        else -> ConsumableQuality.NONE
    }

    private fun determineBoozeQuality(qualityRoll: Int): ConsumableQuality = when (qualityRoll) {
        1, 2 -> ConsumableQuality.DECENT
        3, 4 -> ConsumableQuality.GOOD
        5 -> ConsumableQuality.AWESOME
        6, 7 -> ConsumableQuality.EPIC
        else -> ConsumableQuality.NONE
    }

    private fun determineSpleenQuality(qualityRoll: Int): ConsumableQuality = when (qualityRoll) {
        1 -> ConsumableQuality.CRAPPY
        2, 3 -> ConsumableQuality.DECENT
        4, 5 -> ConsumableQuality.GOOD
        6 -> ConsumableQuality.AWESOME
        7 -> ConsumableQuality.EPIC
        else -> ConsumableQuality.NONE
    }

    private fun rollConsumableSize(mtRng: PHPMTRandom): Int = when (mtRng.nextInt(1, 10)) {
        1 -> 1
        2, 3 -> 2
        4, 5, 6 -> 3
        7, 8 -> 4
        9 -> 5
        10 -> 5 + mtRng.nextInt(1, 5)
        else -> 0
    }

    private fun addSizeAndQualityAdjectives(
        adjectives: MutableList<String>,
        mtRng: PHPMTRandom,
        isFood: Boolean,
        size: Int,
        quality: ConsumableQuality,
    ) {
        val sizeDescriptors = if (isFood) {
            TCRSStringTables.foodSize(minOf(size, 6))
        } else {
            TCRSStringTables.boozeSize(minOf(size, 6))
        }
        if (sizeDescriptors.isNotEmpty()) {
            adjectives.add(mtRng.pickOne(sizeDescriptors))
        }
        val qualityDescriptors = if (isFood) {
            TCRSStringTables.foodQuality(quality)
        } else {
            TCRSStringTables.boozeQuality(quality)
        }
        if (qualityDescriptors.isEmpty()) return
        adjectives.add(
            if (qualityDescriptors.size > 1) mtRng.pickOne(qualityDescriptors) else qualityDescriptors[0],
        )
    }

    private fun promoteEpic(quality: ConsumableQuality, size: Int, baseAdventures: Double): ConsumableQuality {
        if (quality != ConsumableQuality.EPIC || size <= 0) return quality
        return ConsumableDatabase.superEpicQuality(baseAdventures / size)
    }

    private fun disambiguatedEffectName(effectId: Int): String {
        val effect = EffectDatabase.getById(effectId) ?: return ""
        val dupes = EffectDatabase.all().count { it.name == effect.name }
        return if (dupes > 1) "[${effect.id}]${effect.name}" else effect.name
    }

    private fun buildEffectPool() {
        effectPool.clear()
        EffectDatabase.all()
            .filter { it.quality == EffectQuality.GOOD }
            .filter { "nohookah" !in it.attributes || it.id == FISHY }
            .filter { "notcrs" !in it.attributes }
            .filter { it.id <= TIKI_TEMERITY }
            .sortedBy { it.id }
            .forEach { effectPool += it.id }
    }

    private fun ensureReady() {
        if (effectPool.isEmpty() && EffectDatabase.all().isNotEmpty()) buildEffectPool()
    }

    internal fun rebuildEffectPoolForTest() {
        buildEffectPool()
    }

    internal fun effectPoolSizeForTest(): Int = effectPool.size
}
