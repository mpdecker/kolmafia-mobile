package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.modifiers.BitmapModifier
import net.sourceforge.kolmafia.modifiers.BooleanModifier
import net.sourceforge.kolmafia.modifiers.DoubleModifier
import net.sourceforge.kolmafia.modifiers.ModifierParser
import net.sourceforge.kolmafia.modifiers.StringModifier
import net.sourceforge.kolmafia.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class)
object ModifierDatabase {
    private val _byTypeAndName = mutableMapOf<String, MutableMap<String, ModifierEntry>>()
    private val _bundledByTypeAndName = mutableMapOf<String, MutableMap<String, ModifierEntry>>()
    private val _allByName = mutableMapOf<String, MutableList<ModifierEntry>>()
    private val _synergies = mutableListOf<ModifierEntry>()
    private val _synergyMaskByName = mutableMapOf<String, Int>()
    private val _inventorySkillProviderNames = mutableSetOf<String>()
    private val _replaceableMutexByEffectId = mutableMapOf<Int, Set<Int>>()
    private val bitmapMasks = mutableMapOf<BitmapModifier, Int>()
    private val bitmapMasksBySource = mutableMapOf<BitmapModifier, MutableMap<String, Int>>()
    private val tooManyBitmapSources = mutableSetOf<BitmapModifier>()
    private var loaded = false

    /** Desktop [ModifierDatabase.CARRIED_OVER] tag names preserved on override. */
    private val CARRIED_OVER_TAGS: Set<String> = buildSet {
        add(StringModifier.CLASS.tag)
        add(StringModifier.WIKI_NAME.tag)
        add(StringModifier.STAT_TUNING.tag)
        add(StringModifier.EQUIPS_ON.tag)
        add(StringModifier.FAMILIAR_EFFECT.tag)
        add(StringModifier.SKILL.tag)
        add(StringModifier.RECIPE.tag)
        add(StringModifier.LAST_AVAILABLE_DATE.tag)
        add(StringModifier.CONDITIONAL_SKILL_EQUIPPED.tag)
        add(StringModifier.CONDITIONAL_SKILL_INVENTORY.tag)
        add(StringModifier.LANTERN_ELEMENT.tag)
        add(BitmapModifier.BRIMSTONE.tag)
        add(BitmapModifier.CLOATHING.tag)
        add(BitmapModifier.SYNERGETIC.tag)
        add(BitmapModifier.RAVEOSITY.tag)
        add(BitmapModifier.MCHUGELARGE.tag)
        add(BitmapModifier.STINKYCHEESE.tag)
        add(BooleanModifier.NONSTACKABLE_WATCH.tag)
        add(BooleanModifier.NOPULL.tag)
        add(BooleanModifier.ALTERS_PAGE_TEXT.tag)
        add(BooleanModifier.BLIND.tag)
        add(BooleanModifier.BREAKABLE.tag)
        add(BooleanModifier.DROPS_ITEMS.tag)
        add(BooleanModifier.DROPS_MEAT.tag)
        add(DoubleModifier.THORNS.tag)
        add(DoubleModifier.SPORADIC_THORNS.tag)
        add(DoubleModifier.DAMAGE_AURA.tag)
        add(DoubleModifier.SPORADIC_DAMAGE_AURA.tag)
    }

    val byTypeAndName: Map<String, Map<String, ModifierEntry>> get() = _byTypeAndName
    val allByName: Map<String, List<ModifierEntry>> get() = _allByName
    fun synergies(): List<ModifierEntry> = _synergies

    /** Desktop [ModifierDatabase.getSynergies] bitmask keyed by synergy pair name. */
    fun synergyMaskByName(): Map<String, Int> = _synergyMaskByName

    suspend fun load() {
        if (loaded) return
        val text = Res.readBytes("files/data/modifiers.txt").decodeToString()
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            if (!line.contains('\t') && line.toIntOrNull() != null) continue
            val parts = line.split('\t')
            if (parts.size < 3) continue
            val entityType = parts[0].trim()
            val name = parts[1].trim()
            val modifiers = parts[2].trim()
            if (entityType.isEmpty() || name.isEmpty()) continue
            val entry = ModifierEntry(entityType, name, modifiers)
            if (entityType == "Synergy") {
                _synergies += entry
            } else {
                _byTypeAndName.getOrPut(entityType) { mutableMapOf() }[name] = entry
                _bundledByTypeAndName.getOrPut(entityType) { mutableMapOf() }[name] = entry
                _allByName.getOrPut(name.lowercase()) { mutableListOf() } += entry
            }
        }
        rebuildInventorySkillProviders()
        rebuildSynergyMasks()
        rebuildMutexBits()
        rebuildReplaceableMutexEffects()
        loaded = true
    }

    /** Desktop [ModifierDatabase.computeMutexes] — assign MUTEX bitmap bits from MutexI/MutexE rows. */
    private fun rebuildMutexBits() {
        listOf("Item", "Effect").forEach { entityType ->
            val bundled = _bundledByTypeAndName[entityType] ?: return@forEach
            val live = _byTypeAndName.getOrPut(entityType) { mutableMapOf() }
            for ((name, entry) in bundled) {
                live[name] = entry
            }
        }
        var groupIndex = 0
        for (mutexType in listOf("MutexI", "MutexE")) {
            val targetType = if (mutexType == "MutexI") "Item" else "Effect"
            val groups = _bundledByTypeAndName[mutexType] ?: continue
            for ((groupName, _) in groups) {
                val bit = 1 shl groupIndex
                for (piece in groupName.split('/')) {
                    val pieceName = piece.trim()
                    if (pieceName.isEmpty()) continue
                    val entry = resolveMutexPiece(targetType, pieceName) ?: continue
                    val updated = withMutexBit(entry.modifiers, bit)
                    _byTypeAndName.getOrPut(targetType) { mutableMapOf() }[entry.name] =
                        entry.copy(modifiers = updated)
                }
                groupIndex++
            }
        }
    }

    private fun resolveMutexPiece(entityType: String, pieceName: String): ModifierEntry? {
        val map = _byTypeAndName[entityType] ?: return null
        return map[pieceName]
            ?: map.entries.firstOrNull { it.key.equals(pieceName, ignoreCase = true) }?.value
    }

    private fun withMutexBit(modifiers: String, bit: Int): String {
        if (bit == 0) return modifiers
        val parsed = ModifierParser.parse(modifiers)
        val newMutex = parsed.get(BitmapModifier.MUTEX) or bit
        val tokens = modifierTokens(modifiers).filter { token ->
            !modifierTag(token).equals(BitmapModifier.MUTEX.tag, ignoreCase = true)
        }
        val base = tokens.joinToString(", ")
        val mutexToken = "${BitmapModifier.MUTEX.tag}: $newMutex"
        return if (base.isBlank()) mutexToken else "$base, $mutexToken"
    }

    internal fun rebuildMutexBitsForTest() = rebuildMutexBits()

    /** Desktop [ModifierDatabase.computeReplaceableEffectMutexes] — MutexER peer groups by effect id. */
    private fun rebuildReplaceableMutexEffects() {
        _replaceableMutexByEffectId.clear()
        val groups = _bundledByTypeAndName["MutexER"] ?: return
        for ((groupName, _) in groups) {
            val pieces = groupName.split('/').map { it.trim() }.filter { it.isNotEmpty() }
            if (pieces.size < 2) continue
            val effectIds = pieces.mapNotNull { piece ->
                val id = EffectDefinitionProxy.resolveEffectId(piece)
                if (id <= 0) null else id
            }.toSet()
            if (effectIds.size < 2) continue
            for (id in effectIds) {
                _replaceableMutexByEffectId[id] = effectIds
            }
        }
    }

    /** Desktop [ModifierDatabase.getReplaceableMutexFor] peer effect ids for replace-before-add. */
    fun getReplaceableMutexFor(effectId: Int): Set<Int> =
        _replaceableMutexByEffectId[effectId].orEmpty()

    internal fun rebuildReplaceableMutexEffectsForTest() = rebuildReplaceableMutexEffects()

    private fun rebuildSynergyMasks() {
        _synergyMaskByName.clear()
        for (entry in _synergies) {
            var mask = 0
            for (piece in entry.name.split('/')) {
                val pieceName = piece.trim()
                if (pieceName.isEmpty()) continue
                val itemEntry = getItem(pieceName)
                    ?: _byTypeAndName["Item"]?.entries
                        ?.firstOrNull { it.key.equals(pieceName, ignoreCase = true) }
                        ?.value
                if (itemEntry != null) {
                    mask = mask or ModifierParser.parse(itemEntry.modifiers)
                        .get(BitmapModifier.SYNERGETIC)
                }
            }
            if (mask != 0) {
                _synergyMaskByName[entry.name] = mask
            }
        }
    }

    /** Desktop [ModifierDatabase.getInventorySkillProviders] item names. */
    fun inventorySkillProviderNames(): Set<String> = _inventorySkillProviderNames

    private fun rebuildInventorySkillProviders() {
        _inventorySkillProviderNames.clear()
        val items = _byTypeAndName["Item"] ?: return
        for ((name, entry) in items) {
            if (isInventorySkillProvider(entry.modifiers)) {
                _inventorySkillProviderNames += name
            }
        }
    }

    private fun isInventorySkillProvider(modifiers: String): Boolean {
        val parsed = ModifierParser.parse(modifiers)
        if (parsed.getAll(StringModifier.CONDITIONAL_SKILL_INVENTORY).isNotEmpty()) {
            return true
        }
        return parsed.getAll(StringModifier.CONDITIONAL_SKILL_EQUIPPED).any { skillName ->
            SkillDefinitionDatabase.getByName(skillName)?.isNonCombat == true
        }
    }

    /** Desktop [ModifierDatabase.overrideModifier] for GENERATED — exact override, no CARRIED_OVER merge. */
    fun overrideGenerated(name: String, modifierString: String) {
        _byTypeAndName.getOrPut("Generated") { mutableMapOf() }[name] =
            ModifierEntry("Generated", name, modifierString)
    }

    /** Desktop [ModifierDatabase.overrideModifier] — replace runtime modifier string, preserving CARRIED_OVER. */
    fun overrideModifier(entityType: String, name: String, modifierString: String) {
        val base = _bundledByTypeAndName[entityType]?.get(name)?.modifiers.orEmpty()
        val merged = mergeCarriedOver(base, modifierString)
        _byTypeAndName.getOrPut(entityType) { mutableMapOf() }[name] =
            ModifierEntry(entityType, name, merged)
    }

    internal fun mergeCarriedOver(base: String, override: String): String {
        if (base.isBlank()) return override
        val overrideTags = modifierTags(override)
        val carried = modifierTokens(base).filter { token ->
            modifierTag(token) in CARRIED_OVER_TAGS && modifierTag(token) !in overrideTags
        }
        if (carried.isEmpty()) return override
        return (listOf(override.trim()) + carried).filter { it.isNotBlank() }.joinToString(", ")
    }

    /** Restore runtime overrides from bundled modifiers.txt snapshot (desktop resetModifiers Item slice). */
    fun resetOverrides() {
        resetBitmapMasks()
        for ((type, entries) in _bundledByTypeAndName) {
            val live = _byTypeAndName.getOrPut(type) { mutableMapOf() }
            for ((name, entry) in entries) {
                live[name] = entry
            }
        }
        rebuildMutexBits()
        rebuildReplaceableMutexEffects()
    }

    /**
     * Desktop [ModifierDatabase.getBitmapMask] — assign unique bits per
     * ([BitmapModifier], lookup) and reuse them on reparse so Clowniness/Raveosity
     * cannot exhaust the 32-bit mask during TCRS override.
     */
    internal fun getBitmapMask(mod: BitmapModifier, lookup: String, bitcount: Int): Int {
        ensureBitmapMasks()
        val assigned = bitmapMasksBySource.getOrPut(mod) { mutableMapOf() }
        assigned[lookup]?.let { return it }
        val bits = bitcount.coerceAtLeast(1)
        var mask = bitmapMasks[mod] ?: 1
        bitmapMasks[mod] = mask shl bits
        for (i in 0 until bits - 1) {
            mask = mask or (mask shl 1)
        }
        if ((bitmapMasks[mod] ?: 0) == 0) {
            tooManyBitmapSources += mod
        }
        assigned[lookup] = mask
        return mask
    }

    internal fun hasTooManyBitmapSources(mod: BitmapModifier): Boolean =
        mod in tooManyBitmapSources

    private fun ensureBitmapMasks() {
        if (bitmapMasks.isNotEmpty()) return
        resetBitmapMasks()
    }

    private fun resetBitmapMasks() {
        bitmapMasksBySource.clear()
        tooManyBitmapSources.clear()
        bitmapMasks.clear()
        for (mod in BitmapModifier.entries) {
            bitmapMasks[mod] = 1
        }
    }

    fun updateItem(itemId: Int, modifierString: String): Boolean {
        val name = ItemDatabase.getById(itemId)?.name ?: return false
        overrideModifier("Item", name, modifierString)
        return true
    }

    internal fun resetOverridesForTest() = resetOverrides()

    internal fun injectForTest(entityType: String, name: String, modifiers: String) {
        val entry = ModifierEntry(entityType, name, modifiers)
        _byTypeAndName.getOrPut(entityType) { mutableMapOf() }[name] = entry
        _bundledByTypeAndName.getOrPut(entityType) { mutableMapOf() }[name] = entry
        if (entityType == "Synergy") {
            _synergies += entry
            rebuildSynergyMasks()
        } else if (entityType == "Item") {
            if (isInventorySkillProvider(modifiers)) {
                _inventorySkillProviderNames += name
            }
            if (_synergies.isNotEmpty()) {
                rebuildSynergyMasks()
            }
        }
        if (entityType == "MutexI" || entityType == "MutexE") {
            rebuildMutexBits()
        }
        if (entityType == "MutexER") {
            rebuildReplaceableMutexEffects()
        }
    }

    internal fun resetForTest() {
        _byTypeAndName.clear()
        _bundledByTypeAndName.clear()
        _allByName.clear()
        _synergies.clear()
        _synergyMaskByName.clear()
        _inventorySkillProviderNames.clear()
        _replaceableMutexByEffectId.clear()
        resetBitmapMasks()
        loaded = false
    }

    private fun modifierTokens(modifiers: String): List<String> {
        val result = mutableListOf<String>()
        var depth = 0
        var inQuote = false
        var start = 0
        for (i in modifiers.indices) {
            when (modifiers[i]) {
                '"' -> inQuote = !inQuote
                '[' -> if (!inQuote) depth++
                ']' -> if (!inQuote) depth--
                ',' -> if (!inQuote && depth == 0) {
                    result += modifiers.substring(start, i).trim()
                    start = i + 1
                }
            }
        }
        result += modifiers.substring(start).trim()
        return result.filter { it.isNotEmpty() }
    }

    private fun modifierTag(token: String): String {
        val colon = token.indexOf(": ")
        return if (colon < 0) token.trim() else token.substring(0, colon).trim()
    }

    private fun modifierTags(modifiers: String): Set<String> =
        modifierTokens(modifiers).map { modifierTag(it) }.toSet()

    fun modifierTagsForEntry(entry: ModifierEntry?): Set<String> =
        entry?.modifiers?.let { modifierTags(it) }.orEmpty()

    fun hasBooleanModifier(itemName: String, modifier: BooleanModifier): Boolean {
        val entry = getItem(itemName)
            ?: _byTypeAndName["Item"]?.entries
                ?.firstOrNull { it.key.equals(itemName, ignoreCase = true) }
                ?.value
            ?: return false
        return modifier.tag in modifierTagsForEntry(entry)
    }

    fun getStringModifier(itemName: String, modifier: StringModifier): String {
        val entry = getItem(itemName)
            ?: _byTypeAndName["Item"]?.entries
                ?.firstOrNull { it.key.equals(itemName, ignoreCase = true) }
                ?.value
            ?: return ""
        return ModifierParser.parse(entry.modifiers).strings[modifier]?.firstOrNull().orEmpty()
    }

    /** Desktop [TCRSDatabase.carriedOverModifiers] — preserve static item tags on derive. */
    fun carriedOverModifiersForItem(itemId: Int): String {
        val name = ItemDatabase.getById(itemId)?.name ?: return ""
        val modifiers = getItem(name)?.modifiers.orEmpty()
        if (modifiers.isBlank()) return ""
        return modifierTokens(modifiers).filter { token ->
            modifierTag(token) in CARRIED_OVER_TAGS
        }.joinToString(", ")
    }

    fun getItem(name: String): ModifierEntry?     = get("Item",    name)
    fun getEffect(name: String): ModifierEntry?   = get("Effect",  name)
    fun getSkill(name: String): ModifierEntry?    = get("Skill",   name)
    fun getSign(name: String): ModifierEntry?     = get("Sign",    name)
    fun getPath(name: String): ModifierEntry?     = get("Path",    name)
    fun getFamiliar(name: String): ModifierEntry? = get("Familiar",name)
    fun getThrall(name: String): ModifierEntry? {
        val map = _byTypeAndName["Thrall"] ?: return null
        return map[name] ?: map.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value
    }
    fun getThrone(race: String): ModifierEntry? {
        val map = _byTypeAndName["Throne"] ?: return null
        return map[race] ?: map.entries.firstOrNull { it.key.equals(race, ignoreCase = true) }?.value
    }
    fun getBjorn(race: String): ModifierEntry? = getThrone(race)
    fun getMaxCat(name: String): ModifierEntry? = get("MaxCat", name)
    fun getOutfit(name: String): ModifierEntry?   = get("Outfit",  name)
    fun getZone(name: String): ModifierEntry?     = get("Zone",    name)
    fun getLocation(name: String): ModifierEntry? = get("Loc",     name)
    fun getEternityCodpiece(name: String): ModifierEntry? = get("EternityCodpiece", name)

    fun isCodpieceGem(itemId: Int): Boolean {
        val name = ItemDatabase.getById(itemId)?.name ?: return false
        return getEternityCodpiece(name) != null
    }

    fun get(type: String, name: String): ModifierEntry? {
        val map = _byTypeAndName[type] ?: return null
        return map[name] ?: map.entries.firstOrNull {
            it.key.equals(name.trim(), ignoreCase = true)
        }?.value
    }

    /** Mode-specific equipment modifiers (UnbreakableUmbrella, JurassicParka, etc.). */
    fun getModeable(type: String, mode: String): ModifierEntry? = get(type, mode)

    /** All known entity types present in modifiers.txt (e.g. "Item", "Effect", "Sign", "Path"). */
    fun types(): Set<String> = _byTypeAndName.keys

    fun all(): List<ModifierEntry> = _allByName.values.flatten()
}
