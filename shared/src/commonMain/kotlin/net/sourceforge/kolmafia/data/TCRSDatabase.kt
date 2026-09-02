package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.character.CharacterClass
import net.sourceforge.kolmafia.modifiers.DoubleModifier
import net.sourceforge.kolmafia.modifiers.ModifierParser
import net.sourceforge.kolmafia.modifiers.StringModifier
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Per-run Two Crazy Random Summer item name mappings and runtime modifier overrides.
 * Mirrors desktop TCRSDatabase applyModifiers/resetModifiers (item + consumable + effect slices).
 */
object TCRSDatabase {

    data class TcrsEntry(
        val name: String,
        val size: Int = 0,
        val quality: String = "",
        val modifiers: String = "",
    )

    private const val CAFE_BOOZE_SUFFIX = "_cafe_booze"
    private const val CAFE_FOOD_SUFFIX = "_cafe_food"

    private val standardSignNames = setOf(
        "Mongoose", "Wallaby", "Vole",
        "Platypus", "Opossum", "Marmot",
        "Wombat", "Blender", "Packrat",
    )

    private var currentClassSign = ""
    private val tcrsMap = mutableMapOf<Int, TcrsEntry>()
    private val tcrsBoozeMap = mutableMapOf<Int, TcrsEntry>()
    private val tcrsFoodMap = mutableMapOf<Int, TcrsEntry>()

    fun getTCRSName(itemId: Int): String {
        val entry = tcrsMap[itemId]
        return entry?.name ?: ItemDatabase.getById(itemId)?.name ?: ""
    }

    fun getEntry(itemId: Int): TcrsEntry? = tcrsMap[itemId]

    fun isLoaded(): Boolean = currentClassSign.isNotEmpty() && tcrsMap.isNotEmpty()

    fun entryCount(): Int = tcrsMap.size

    fun applyModifiers(characterLevel: Int): Int {
        EffectDatabase.stripConsumableActions()
        var applied = 0
        for ((itemId, entry) in tcrsMap) {
            if (shouldSkipItem(itemId)) continue
            val item = ItemDatabase.getById(itemId) ?: continue
            val itemName = item.name
            var changed = false
            if (ModifierDatabase.updateItem(itemId, entry.modifiers)) changed = true
            if (applyConsumableModifiers(itemId, itemName, entry)) changed = true
            val effectName = ModifierDatabase.getStringModifier(itemName, StringModifier.EFFECT)
            if (effectName.isNotBlank()) {
                EffectDatabase.addEffectSource(itemName, item.primaryUse, effectName)
                changed = true
            }
            if (ConcoctionDatabase.getByResult(itemName) != null) {
                ConcoctionDatabase.setEffectName(itemId, itemName)
            }
            if (changed) applied++
        }
        for ((cafeId, entry) in tcrsBoozeMap) {
            val name = CafeDatabase.getCafeBoozeName(cafeId) ?: continue
            if (applyConsumableModifiersByName(ConsumableType.DRINK, name, entry)) applied++
        }
        for ((cafeId, entry) in tcrsFoodMap) {
            val name = CafeDatabase.getCafeFoodName(cafeId) ?: continue
            if (applyConsumableModifiersByName(ConsumableType.FOOD, name, entry)) applied++
        }
        ConsumableDatabase.setLevelVariableConsumables(characterLevel)
        ConcoctionDatabase.refreshConcoctions()
        return applied
    }

    fun resetModifiers(preferences: Preferences, characterLevel: Int) {
        if (currentClassSign.isEmpty()) return
        ModifierDatabase.resetOverrides()
        ConsumableDatabase.resetOverrides()
        EffectDatabase.resetOverrides()
        ConcoctionDatabase.resetEffectNames()
        ConcoctionDatabase.refreshConcoctions()
        ConsumableDatabase.setVariableConsumables(preferences, characterLevel)
        ConsumableDatabase.calculateAllAverageAdventures()
    }

    private fun shouldSkipItem(itemId: Int): Boolean {
        if (itemId in TCRSSkipItemIds.CAMPGROUND_ITEMS) return true
        if (itemId in TCRSSkipItemIds.CHATEAU_ITEMS) return true
        val item = ItemDatabase.getById(itemId) ?: return true
        return item.primaryUse == ItemPrimaryUse.FAMILIAR
    }

    private fun qualityMultiplier(quality: String): Int = when (ConsumableQuality.fromString(quality)) {
        ConsumableQuality.EPIC -> 5
        ConsumableQuality.AWESOME -> 4
        ConsumableQuality.GOOD -> 3
        ConsumableQuality.DECENT -> 2
        ConsumableQuality.CRAPPY -> 1
        else -> 0
    }

    private fun applyConsumableModifiers(itemId: Int, itemName: String, entry: TcrsEntry): Boolean {
        val item = ItemDatabase.getById(itemId) ?: return false
        val usage = when (item.primaryUse) {
            ItemPrimaryUse.FOOD -> ConsumableType.FOOD
            ItemPrimaryUse.DRINK -> ConsumableType.DRINK
            ItemPrimaryUse.SPLEEN -> ConsumableType.SPLEEN
            else -> return false
        }
        return applyConsumableModifiersByName(usage, itemName, entry)
    }

    private fun applyConsumableModifiersByName(
        usage: ConsumableType,
        itemName: String,
        entry: TcrsEntry,
    ): Boolean {
        if (ConsumableDatabase.getConsumableByName(itemName) == null) return false
        val level = ConsumableDatabase.getLevelReqByName(itemName) ?: 0
        val adv = if (usage == ConsumableType.SPLEEN) 0 else entry.size * qualityMultiplier(entry.quality)
        return ConsumableDatabase.updateConsumable(
            itemName = itemName,
            size = entry.size,
            level = level,
            quality = ConsumableQuality.fromString(entry.quality),
            adv = adv.toString(),
            mus = "0",
            myst = "0",
            mox = "0",
            notes = buildConsumableNotes(itemName),
        )
    }

    /** Desktop Attribute leftover tokens preserved across TCRS consumable override. */
    private val CONSUMABLE_NOTE_ATTRIBUTES = listOf(
        "MARTINI", "LASAGNA", "SAUCY", "PIZZA", "BEANS",
        "WINE", "SALAD", "BEER", "CANNED", "BEVERAGE", "TACO",
    )

    /**
     * Desktop PR #3675: leftover attributes/effect only. Do not prefix Unspaded;
     * empty notes when there is nothing else to say.
     */
    private fun buildConsumableNotes(itemName: String): String {
        val parts = leftoverAttributeNotes(itemName).toMutableList()
        val effectName = ModifierDatabase.getStringModifier(itemName, StringModifier.EFFECT)
        if (effectName.isNotBlank()) {
            val duration = effectDuration(itemName)
            val effectModifiers = ModifierDatabase.getEffect(effectName)?.modifiers.orEmpty()
            parts += "$duration $effectName ($effectModifiers)"
        }
        return parts.joinToString(", ")
    }

    private fun leftoverAttributeNotes(itemName: String): List<String> {
        val notes = ConsumableDatabase.getNotesByName(itemName)
        if (notes.isBlank()) return emptyList()
        return CONSUMABLE_NOTE_ATTRIBUTES.filter { notes.contains(it) }
    }

    private fun effectDuration(itemName: String): Int {
        val modifiers = ModifierDatabase.getItem(itemName)?.modifiers.orEmpty()
        if (modifiers.isBlank()) return 0
        return ModifierParser.parse(modifiers).getInt(DoubleModifier.EFFECT_DURATION)
    }

    fun filename(className: String, signName: String, suffix: String = ""): String {
        if (!validate(className, signName)) return ""
        val classPart = className.replace(' ', '_')
        return "TCRS_${classPart}_${signName}${suffix}.txt"
    }

    fun prefKey(className: String, signName: String, suffix: String = ""): String {
        if (!validate(className, signName)) return ""
        val classPart = className.replace(' ', '_')
        return "tcrs_${classPart}_${signName}$suffix"
    }

    fun validate(className: String, signName: String): Boolean {
        val cls = CharacterClass.entries.firstOrNull {
            it.displayName.equals(className, ignoreCase = true)
        } ?: return false
        if (!cls.isStandardClass) return false
        return standardSignNames.any { it.equals(signName, ignoreCase = true) }
    }

    fun parseFromText(text: String): Map<Int, TcrsEntry> {
        val map = linkedMapOf<Int, TcrsEntry>()
        for (line in text.lineSequence()) {
            val trimmed = line.trimEnd('\r', '\n').trimStart()
            if (trimmed.isEmpty() || trimmed.startsWith('#')) continue
            val cols = trimmed.split('\t')
            if (cols.size < 5) continue
            val itemId = cols[0].toIntOrNull() ?: continue
            val name = cols[1]
            val size = cols[2].toIntOrNull() ?: 0
            val quality = cols[3]
            val modifiers = cols[4]
            map[itemId] = TcrsEntry(name, size, quality, modifiers)
        }
        return map
    }

    fun load(className: String, signName: String, text: String?) {
        if (text.isNullOrBlank()) return
        tcrsMap.clear()
        tcrsMap.putAll(parseFromText(text))
        currentClassSign = "$className/$signName"
    }

    fun loadCafeFromPreferences(className: String, signName: String, preferences: Preferences) {
        if (!validate(className, signName)) return
        tcrsBoozeMap.clear()
        tcrsFoodMap.clear()
        val boozeText = preferences.getString(prefKey(className, signName, CAFE_BOOZE_SUFFIX), "")
        if (boozeText.isNotBlank()) {
            tcrsBoozeMap.putAll(parseFromText(boozeText))
        }
        val foodText = preferences.getString(prefKey(className, signName, CAFE_FOOD_SUFFIX), "")
        if (foodText.isNotBlank()) {
            tcrsFoodMap.putAll(parseFromText(foodText))
        }
    }

    fun loadFromPreferences(className: String, signName: String, preferences: Preferences): Boolean {
        if (!validate(className, signName)) {
            reset()
            return false
        }
        val key = prefKey(className, signName)
        val text = preferences.getString(key, "")
        if (text.isBlank()) {
            reset()
            currentClassSign = "$className/$signName"
            return false
        }
        load(className, signName, text)
        loadCafeFromPreferences(className, signName, preferences)
        return true
    }

    fun saveToPreferences(className: String, signName: String, preferences: Preferences): Boolean {
        if (!validate(className, signName)) return false
        saveMapToPreferences(className, signName, preferences, "", tcrsMap)
        saveMapToPreferences(className, signName, preferences, CAFE_BOOZE_SUFFIX, tcrsBoozeMap)
        saveMapToPreferences(className, signName, preferences, CAFE_FOOD_SUFFIX, tcrsFoodMap)
        return true
    }

    private fun saveMapToPreferences(
        className: String,
        signName: String,
        preferences: Preferences,
        suffix: String,
        map: Map<Int, TcrsEntry>,
    ) {
        val key = prefKey(className, signName, suffix)
        if (map.isEmpty()) {
            preferences.setString(key, "")
            return
        }
        val lines = map.entries.sortedBy { it.key }.joinToString("\n") { (itemId, entry) ->
            listOf(
                itemId.toString(),
                entry.name,
                entry.size.toString(),
                entry.quality,
                entry.modifiers,
            ).joinToString("\t")
        }
        preferences.setString(key, lines)
    }

    fun reset() {
        currentClassSign = ""
        tcrsMap.clear()
        tcrsBoozeMap.clear()
        tcrsFoodMap.clear()
    }

    internal fun registerForTest(itemId: Int, name: String) {
        tcrsMap[itemId] = TcrsEntry(name)
    }

    internal fun injectMapForTest(entries: Map<Int, TcrsEntry>, classSign: String = "Seal Clubber/Mongoose") {
        tcrsMap.clear()
        tcrsMap.putAll(entries)
        currentClassSign = classSign
    }

    internal fun injectCafeMapsForTest(
        booze: Map<Int, TcrsEntry> = emptyMap(),
        food: Map<Int, TcrsEntry> = emptyMap(),
    ) {
        tcrsBoozeMap.clear()
        tcrsBoozeMap.putAll(booze)
        tcrsFoodMap.clear()
        tcrsFoodMap.putAll(food)
    }

    internal fun currentClassSignForTest(): String = currentClassSign

    internal fun mapSizeForTest(): Int = tcrsMap.size

    fun importFetchedText(
        className: String,
        signName: String,
        suffix: String,
        text: String,
    ): Int {
        if (!validate(className, signName)) return 0
        val entries = parseFromText(text)
        when (suffix) {
            "" -> {
                tcrsMap.clear()
                tcrsMap.putAll(entries)
                currentClassSign = "$className/$signName"
            }
            CAFE_BOOZE_SUFFIX -> {
                tcrsBoozeMap.clear()
                tcrsBoozeMap.putAll(entries)
            }
            CAFE_FOOD_SUFFIX -> {
                tcrsFoodMap.clear()
                tcrsFoodMap.putAll(entries)
            }
            else -> return 0
        }
        return entries.size
    }

    fun putDerivedEntry(itemId: Int, entry: TcrsEntry) {
        tcrsMap[itemId] = entry
    }

    fun deriveEntry(itemId: Int, html: String): TcrsEntry =
        TCRSDeriver.deriveFromHtml(itemId, html)
}
