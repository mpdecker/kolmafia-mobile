package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.adventure.choice.ItemPool
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.EffectDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.effect.EffectManager
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.shop.DesertBeachUnlockSync

/**
 * Desktop [ResultProcessor] — parse fight/NC HTML into inventory/meat/effects
 * and [gainItem] ItemPool side effects (Phases 1221–1250 + 1791–1850).
 */
object ResultProcessor {
    private val ITEM_GAINED = Regex(
        """You acquire (?:and equip )?an item:\s*<b>(.*?)</b>(?:\s*\((\d+)\))?(?:\s*\((stored in Hagnk's Ancestral Mini-Storage|automatically equipped)\))?""",
        RegexOption.IGNORE_CASE,
    )
    private val MEAT_GAINED = Regex("""You gain ([\d,]+) Meat""")
    private val EFFECT_GAINED = Regex(
        """You acquire an (?:effect|intrinsic):\s*<b>(.*?)</b>(?:.*?\(duration:\s*(\d+)\s*Adventures?\))?""",
        RegexOption.IGNORE_CASE,
    )
    private val EFFECT_LOST = Regex(
        """You lose (?:an|some of an) (?:effect|intrinsic):\s*<b>(.*?)</b>""",
        RegexOption.IGNORE_CASE,
    )
    private val FAMILIAR_LB = Regex(
        """gains a pound|gained a pound|puts on weight|gaining weight|just got heavier|put on some weight""",
        RegexOption.IGNORE_CASE,
    )

    private val OYSTER_EGGS = setOf(
        ItemPool.MAGNIFICENT_OYSTER_EGG,
        ItemPool.BRILLIANT_OYSTER_EGG,
        ItemPool.GLISTENING_OYSTER_EGG,
        ItemPool.SCINTILLATING_OYSTER_EGG,
        ItemPool.PEARLESCENT_OYSTER_EGG,
        ItemPool.LUSTROUS_OYSTER_EGG,
        ItemPool.GLEAMING_OYSTER_EGG,
    )

    private val SHADOW_ITEMS = setOf(
        ItemPool.SHADOW_SAUSAGE,
        ItemPool.SHADOW_SKIN,
        ItemPool.SHADOW_FLAME,
        ItemPool.SHADOW_BREAD,
        ItemPool.SHADOW_ICE,
        ItemPool.SHADOW_FLUID,
        ItemPool.SHADOW_GLASS,
        ItemPool.SHADOW_BRICK,
        ItemPool.SHADOW_SINEW,
        ItemPool.SHADOW_VENOM,
        ItemPool.SHADOW_NECTAR,
        ItemPool.SHADOW_STICK,
    )

    private val SHEN_ITEMS = setOf(
        ItemPool.FIRST_PIZZA,
        ItemPool.LACROSSE_STICK,
        ItemPool.EYE_OF_THE_STARS,
        ItemPool.STANKARA_STONE,
        ItemPool.MURPHYS_FLAG,
        ItemPool.SHIELD_OF_BROOK,
    )

    /** Optional DI for inventory mutations during processItem/removeItem/autoCreate. */
    var inventoryProvider: (() -> InventoryManager?)? = null
    var questDatabaseProvider: (() -> QuestDatabase?)? = null
    var ascensionNumberProvider: (() -> Int)? = null
    var equipmentManagerProvider: (() -> EquipmentManager?)? = null
    var holidayContainsOysterEggDay: () -> Boolean = { false }
    var hasEquipped: (Int) -> Boolean = { false }

    data class ParsedResults(
        val items: List<Pair<String, Int>> = emptyList(),
        val autoEquipped: List<String> = emptyList(),
        val meat: Int = 0,
        val effectsGained: List<Pair<String, Int>> = emptyList(),
        val effectsLost: List<String> = emptyList(),
        val familiarGainedPound: Boolean = false,
    )

    fun parseItems(html: String): List<Pair<String, Int>> {
        val counts = linkedMapOf<String, Int>()
        ITEM_GAINED.findAll(html).forEach { m ->
            val name = m.groupValues[1].trim()
            val quantity = m.groupValues.getOrNull(2)?.toIntOrNull() ?: 1
            val comment = m.groupValues.getOrNull(3).orEmpty()
            if (name.isNotEmpty() && !comment.contains("Hagnk", ignoreCase = true)) {
                counts[name] = (counts[name] ?: 0) + quantity
            }
        }
        return counts.map { it.key to it.value }
    }

    fun parseAutoEquipped(html: String): List<String> =
        ITEM_GAINED.findAll(html).mapNotNull { m ->
            val name = m.groupValues[1].trim()
            val comment = m.groupValues.getOrNull(3).orEmpty()
            if (name.isNotEmpty() && comment.contains("automatically equipped", ignoreCase = true)) {
                name
            } else {
                null
            }
        }.toList()

    fun parseMeat(html: String): Int =
        MEAT_GAINED.find(html)?.groupValues?.get(1)?.replace(",", "")?.toIntOrNull() ?: 0

    fun parseEffects(html: String): List<Pair<String, Int>> =
        EFFECT_GAINED.findAll(html).map { m ->
            val name = m.groupValues[1].trim()
            val duration = m.groupValues.getOrNull(2)?.toIntOrNull() ?: 0
            name to duration
        }.toList()

    fun parseEffectLosses(html: String): List<String> =
        EFFECT_LOST.findAll(html).map { it.groupValues[1].trim() }.toList()

    fun parseResults(html: String): ParsedResults = ParsedResults(
        items = parseItems(html),
        autoEquipped = parseAutoEquipped(html),
        meat = parseMeat(html),
        effectsGained = parseEffects(html),
        effectsLost = parseEffectLosses(html),
        familiarGainedPound = FAMILIAR_LB.containsMatchIn(html),
    )

    fun processResults(
        adventureResults: Boolean,
        html: String,
        inventory: InventoryManager? = null,
        character: KoLCharacter? = null,
        preferences: Preferences? = null,
        effectManager: EffectManager? = null,
        questDatabase: QuestDatabase? = null,
        equipmentManager: EquipmentManager? = equipmentManagerProvider?.invoke(),
    ): Boolean {
        val prevInv = inventoryProvider
        val prevQuest = questDatabaseProvider
        val prevAsc = ascensionNumberProvider
        val prevEquip = equipmentManagerProvider
        if (inventory != null) inventoryProvider = { inventory }
        if (questDatabase != null) questDatabaseProvider = { questDatabase }
        if (equipmentManager != null) equipmentManagerProvider = { equipmentManager }
        if (character != null) {
            ascensionNumberProvider = { character.state.value.ascensionNumber }
        }
        try {
            val parsed = parseResults(html)
            var refresh = false
            val mgr = equipmentManager ?: equipmentManagerProvider?.invoke()

            for ((name, count) in parsed.items) {
                val itemId = ItemDatabase.getByName(name)?.id ?: -1
                if (itemId > 0 && inventory != null) {
                    inventory.gainItemLocally(itemId, count)
                    gainItem(adventureResults, itemId, count, name, preferences, questDatabase, inventory)
                    refresh = true
                } else if (itemId > 0) {
                    gainItem(adventureResults, itemId, count, name, preferences, questDatabase, inventory)
                }
            }

            for (name in parsed.autoEquipped) {
                val itemId = ItemDatabase.getByName(name)?.id ?: continue
                mgr?.autoequipItem(itemId, swapInventory = true)
                refresh = true
            }

            if (parsed.meat != 0 && character != null) {
                processMeat(parsed.meat.toLong(), character)
                refresh = true
            }

            if (effectManager != null && (parsed.effectsGained.isNotEmpty() || parsed.effectsLost.isNotEmpty())) {
                applyEffectsLocally(effectManager, parsed.effectsGained, parsed.effectsLost)
                refresh = true
            }

            if (parsed.familiarGainedPound) {
                preferences?.setBoolean("_familiarGainedPoundThisFight", true)
                processFamiliarWeightGain(html, character)
            }

            processStatGain(html, character)
            processAdventuresFromHtml(html, character)
            processGainLossFromHtml(html, character, inventory, preferences, questDatabase)

            return refresh
        } finally {
            inventoryProvider = prevInv
            questDatabaseProvider = prevQuest
            ascensionNumberProvider = prevAsc
            equipmentManagerProvider = prevEquip
        }
    }

    private fun applyEffectsLocally(
        effectManager: EffectManager,
        gained: List<Pair<String, Int>>,
        lost: List<String>,
    ) {
        val current = effectManager.state.value.effects.toMutableList()
        for (name in lost) {
            current.removeAll { it.name.equals(name, ignoreCase = true) }
        }
        for ((name, duration) in gained) {
            val existing = current.indexOfFirst { it.name.equals(name, ignoreCase = true) }
            val id = EffectDatabase.getByName(name)?.id ?: 0
            val data = net.sourceforge.kolmafia.effect.EffectData(id = id, name = name, duration = duration)
            if (existing >= 0) {
                current[existing] = data
            } else {
                current.add(data)
            }
        }
        effectManager.replaceEffectsForTest(current)
    }

    private val STAT_GAIN = Regex(
        """You gain ([\d,]+) (Muscle|Mysticality|Moxie)""",
        RegexOption.IGNORE_CASE,
    )
    private val ADV_GAIN = Regex("""You gain ([\d,]+) Adventure""", RegexOption.IGNORE_CASE)
    private val ADV_LOSE = Regex("""You lose ([\d,]+) Adventure""", RegexOption.IGNORE_CASE)

    /** Desktop [ResultProcessor.processMeat]. */
    fun processMeat(amount: Long, character: KoLCharacter?) {
        if (amount == 0L || character == null) return
        val next = (character.state.value.meat + amount).coerceAtLeast(0L).toInt()
        character.updateMeat(next)
        if (amount > 0) character.addSessionMeat(amount)
    }

    fun processMeatFromHtml(html: String, character: KoLCharacter?): Int {
        val meat = parseMeat(html)
        if (meat != 0) processMeat(meat.toLong(), character)
        return meat
    }

    /** Desktop [ResultProcessor.processStatGain] subset — Muscle/Mysticality/Moxie gain lines. */
    fun processStatGain(html: String, character: KoLCharacter?): Boolean {
        if (character == null) return false
        var changed = false
        STAT_GAIN.findAll(html).forEach { m ->
            val amount = m.groupValues[1].replace(",", "").toLongOrNull() ?: return@forEach
            when (m.groupValues[2].lowercase()) {
                "muscle" -> character.adjustSubstats(musDelta = amount)
                "mysticality" -> character.adjustSubstats(mysDelta = amount)
                "moxie" -> character.adjustSubstats(moxDelta = amount)
            }
            changed = true
        }
        return changed
    }

    /** Desktop [ResultProcessor.processFamiliarWeightGain]. */
    fun processFamiliarWeightGain(html: String, character: KoLCharacter?): Boolean {
        if (character == null || !FAMILIAR_LB.containsMatchIn(html)) return false
        val state = character.state.value
        if (state.familiarId <= 0) return false
        character.updateFamiliar(
            id = state.familiarId,
            name = state.familiarName,
            weight = state.familiarWeight + 1,
            exp = state.familiarExp,
        )
        return true
    }

    fun processAdventuresLeft(amount: Int, character: KoLCharacter?) {
        if (character == null) return
        character.updateAdventuresLeft((character.state.value.adventuresLeft + amount).coerceAtLeast(0))
    }

    fun processAdventuresUsed(amount: Int, character: KoLCharacter?) {
        if (amount == 0 || character == null) return
        processAdventuresLeft(-amount, character)
    }

    fun processAdventuresFromHtml(html: String, character: KoLCharacter?): Boolean {
        if (character == null) return false
        var changed = false
        ADV_GAIN.findAll(html).forEach { m ->
            val n = m.groupValues[1].replace(",", "").toIntOrNull() ?: return@forEach
            processAdventuresLeft(n, character)
            changed = true
        }
        ADV_LOSE.findAll(html).forEach { m ->
            val n = m.groupValues[1].replace(",", "").toIntOrNull() ?: return@forEach
            processAdventuresUsed(n, character)
            changed = true
        }
        return changed
    }

    private val HP_MP_GAIN = Regex(
        """You (gain|lose) ([\d,]+) (hit points?|mana points?|Muscularity Points?|Mojo Points?|Psychic Energy)""",
        RegexOption.IGNORE_CASE,
    )
    private val DISCARD = Regex("""You discard your (.*?)\.""", RegexOption.IGNORE_CASE)
    private val DONATION_PHRASES = listOf(
        "moist orphans",
        "Cola Wars Veterans",
        "Thanks for the larva",
        "next to the library",
        "we donated",
    )
    private val HIPPY_DONATE = Regex("""we donated ([\d,]+) meat""", RegexOption.IGNORE_CASE)

    /**
     * Desktop [ResultProcessor.processGainLoss] — single gain/lose token.
     * Returns true when the token was recognized.
     */
    fun processGainLoss(
        token: String,
        character: KoLCharacter?,
        inventory: InventoryManager? = inventoryProvider?.invoke(),
    ): Boolean {
        var lastToken = token.trim()
        val dot = lastToken.indexOf('.')
        if (dot > 0) lastToken = lastToken.substring(0, dot)
        val paren = lastToken.indexOf('(')
        if (paren > 0) lastToken = lastToken.substring(0, paren).trim()

        if (lastToken.contains("Meat", ignoreCase = true)) {
            val amount = Regex("""([\d,]+)""")
                .find(lastToken)?.groupValues?.get(1)?.replace(",", "")?.toLongOrNull()
                ?: return false
            val signed = if (lastToken.contains("lose", ignoreCase = true) ||
                lastToken.contains("spent", ignoreCase = true)
            ) {
                -amount
            } else {
                amount
            }
            processMeat(signed, character)
            return true
        }

        if (lastToken.startsWith("You gain a", ignoreCase = true) ||
            lastToken.startsWith("You gain some", ignoreCase = true)
        ) {
            return true
        }

        HP_MP_GAIN.find(lastToken)?.let { m ->
            val lose = m.groupValues[1].equals("lose", ignoreCase = true)
            val amount = m.groupValues[2].replace(",", "").toIntOrNull() ?: return@let
            val kind = m.groupValues[3].lowercase()
            val delta = if (lose) -amount else amount
            character ?: return true
            val st = character.state.value
            when {
                kind.startsWith("hit") -> {
                    val hp = (st.currentHp + delta).coerceIn(0, st.maxHp.coerceAtLeast(0))
                    character.updateHpMp(hp, st.maxHp, st.currentMp, st.maxMp)
                }
                else -> {
                    val mp = (st.currentMp + delta).coerceIn(0, st.maxMp.coerceAtLeast(0))
                    character.updateHpMp(st.currentHp, st.maxHp, mp, st.maxMp)
                }
            }
            return true
        }

        if (ADV_GAIN.containsMatchIn(lastToken) || ADV_LOSE.containsMatchIn(lastToken)) {
            processAdventuresFromHtml(lastToken, character)
            return true
        }

        if (STAT_GAIN.containsMatchIn(lastToken)) {
            processStatGain(lastToken, character)
            return true
        }
        return false
    }

    /** Desktop [ResultProcessor.processDiscard]. */
    fun processDiscard(
        token: String,
        inventory: InventoryManager? = inventoryProvider?.invoke(),
        preferences: Preferences? = null,
        questDatabase: QuestDatabase? = null,
    ) {
        val name = DISCARD.find(token)?.groupValues?.get(1)?.trim() ?: return
        val itemId = ItemDatabase.getByName(name)?.id ?: return
        processItem(itemId, -1, preferences, questDatabase, inventory)
    }

    /** Desktop [ResultProcessor.handleDonations] selective. */
    fun handleDonations(
        url: String,
        html: String,
        character: KoLCharacter?,
        preferences: Preferences? = null,
    ) {
        if (preferences?.getBoolean("onlyAutosellDonationsCount", true) == true) {
            return
        }
        val hippy = HIPPY_DONATE.find(html)?.groupValues?.get(1)?.replace(",", "")?.toLongOrNull()
        if (hippy != null && hippy > 0) {
            processMeat(-hippy, character)
            preferences?.setLong("charitableDonations", 
                (preferences.getLong("charitableDonations", 0L) + hippy))
            return
        }
        if (DONATION_PHRASES.any { html.contains(it, ignoreCase = true) }) {
            val amount = Regex("""([\d,]+) Meat""", RegexOption.IGNORE_CASE)
                .find(html)?.groupValues?.get(1)?.replace(",", "")?.toLongOrNull() ?: return
            processMeat(-amount, character)
            preferences?.setLong(
                "charitableDonations",
                preferences.getLong("charitableDonations", 0L) + amount,
            )
        }
    }

    /** Scan HTML for gain/lose/discard lines and apply. */
    fun processGainLossFromHtml(
        html: String,
        character: KoLCharacter?,
        inventory: InventoryManager? = inventoryProvider?.invoke(),
        preferences: Preferences? = null,
        questDatabase: QuestDatabase? = null,
    ): Boolean {
        var changed = false
        val lines = html.replace(Regex("<[^>]+>"), "\n").lines()
        for (line in lines) {
            val t = line.trim()
            if (t.startsWith("You discard", ignoreCase = true)) {
                processDiscard(t, inventory, preferences, questDatabase)
                changed = true
            } else if (
                t.startsWith("You gain", ignoreCase = true) ||
                (t.startsWith("You lose ", ignoreCase = true) &&
                    !t.startsWith("You lose an item", ignoreCase = true)) ||
                t.startsWith("You spent", ignoreCase = true)
            ) {
                if (processGainLoss(t, character, inventory)) changed = true
            }
        }
        return changed
    }

    /** Desktop tallyResult subset — HP/MP from processResults already; tick effects duration. */
    fun tallyEffectDurationTicks(effectManager: EffectManager?, adventuresUsed: Int) {
        if (effectManager == null || adventuresUsed <= 0) return
        val current = effectManager.state.value.effects.map { e ->
            if (e.duration > 0) e.copy(duration = (e.duration - adventuresUsed).coerceAtLeast(0))
            else e
        }.filter { it.duration != 0 || it.name.isNotBlank() }
        effectManager.replaceEffectsForTest(current)
    }

    /** Desktop [ResultProcessor.processItem] — inventory delta + gainItem side effects. */
    fun processItem(
        itemId: Int,
        count: Int,
        preferences: Preferences? = null,
        questDatabase: QuestDatabase? = null,
        inventory: InventoryManager? = inventoryProvider?.invoke(),
        adventureResults: Boolean = false,
    ) {
        if (count == 0) return
        val inv = inventory
        if (inv != null) {
            if (count > 0) inv.gainItemLocally(itemId, count)
            else inv.consumeItemLocally(itemId, -count)
        }
        gainItem(adventureResults, itemId, count, "", preferences, questDatabase, inv)
    }

    fun removeItem(
        itemId: Int,
        preferences: Preferences? = null,
        questDatabase: QuestDatabase? = null,
        inventory: InventoryManager? = inventoryProvider?.invoke(),
    ) {
        val inv = inventory ?: return
        if ((inv.state.value.items[itemId]?.quantity ?: 0) > 0) {
            processItem(itemId, -1, preferences, questDatabase, inv)
        }
    }

    /** Consume one or more item IDs, coalescing duplicates, after a successful craft/use. */
    fun consumeItems(
        itemIds: List<Int>,
        preferences: Preferences? = null,
        inventory: InventoryManager? = inventoryProvider?.invoke(),
        questDatabase: QuestDatabase? = null,
    ) {
        if (itemIds.isEmpty()) return
        val counts = itemIds.filter { it > 0 }.groupingBy { it }.eachCount()
        for ((itemId, qty) in counts) {
            processItem(itemId, -qty, preferences, questDatabase, inventory)
        }
    }

    /**
     * Desktop [ResultProcessor.autoCreate] subset — crafts blackbird/crow when both
     * ingredients are present and `autoCraft` is enabled.
     */
    fun autoCreate(
        itemId: Int,
        preferences: Preferences?,
        inventory: InventoryManager? = inventoryProvider?.invoke(),
        questDatabase: QuestDatabase? = null,
    ) {
        val prefs = preferences ?: return
        if (!prefs.getBoolean("autoCraft", true)) return
        val inv = inventory ?: return
        val recipe = when (itemId) {
            ItemPool.REASSEMBLED_BLACKBIRD ->
                listOf(ItemPool.BROKEN_WINGS, ItemPool.SUNKEN_EYES)
            ItemPool.RECONSTITUTED_CROW ->
                listOf(ItemPool.BUSTED_WINGS, ItemPool.BIRD_BRAIN)
            ItemPool.MCCLUSKY_FILE ->
                listOf(ItemPool.MCCLUSKY_FILE_PAGE5)
            else -> return
        }
        if (recipe.any { (inv.state.value.items[it]?.quantity ?: 0) < 1 }) return
        if ((inv.state.value.items[itemId]?.quantity ?: 0) > 0 && itemId != ItemPool.MCCLUSKY_FILE) return
        for (ing in recipe) {
            processItem(ing, -1, prefs, questDatabase, inv)
        }
        processItem(itemId, 1, prefs, questDatabase, inv)
    }

    fun gainItem(
        adventureResults: Boolean,
        itemId: Int,
        count: Int,
        itemName: String = "",
        preferences: Preferences? = null,
        questDatabase: QuestDatabase? = questDatabaseProvider?.invoke(),
        inventory: InventoryManager? = inventoryProvider?.invoke(),
    ) {
        if (count == 0) return
        ConcoctionDatabase.markRefreshNeeded()

        val prefs = preferences ?: return
        val quests = questDatabase ?: QuestDatabase(prefs)
        val inv = inventory
        val ascensions = ascensionNumberProvider?.invoke() ?: 0

        // Both positive and negative results
        applyAlwaysEffects(itemId, count, adventureResults, prefs, quests, inv)

        if (count < 0) return

        applyPositiveEffects(itemId, count, adventureResults, prefs, quests, inv, ascensions, itemName)

        // Legacy name fallbacks for callers without resolved ids
        if (itemId <= 0) applyNameFallbacks(itemName, count, adventureResults, prefs)
    }

    private fun applyAlwaysEffects(
        itemId: Int,
        count: Int,
        adventureResults: Boolean,
        prefs: Preferences,
        quests: QuestDatabase,
        inv: InventoryManager?,
    ) {
        when (itemId) {
            ItemPool.BLACK_BARTS_BOOTY -> prefs.setBoolean("blackBartsBootyAvailable", false)
            ItemPool.HOLIDAY_FUN_BOOK -> prefs.setBoolean("holidayHalsBookAvailable", false)
            ItemPool.ANTAGONISTIC_SNOWMAN_KIT -> prefs.setBoolean("antagonisticSnowmanKitAvailable", false)
            ItemPool.MAP_TO_KOKOMO -> prefs.setBoolean("mapToKokomoAvailable", false)
            ItemPool.ESSENCE_OF_BEAR -> prefs.setBoolean("essenceOfBearAvailable", false)
            ItemPool.MANUAL_OF_NUMBEROLOGY -> prefs.setBoolean("manualOfNumberologyAvailable", false)
            ItemPool.ROM_OF_OPTIMALITY -> prefs.setBoolean("ROMOfOptimalityAvailable", false)
            ItemPool.GUIDE_TO_SAFARI -> prefs.setBoolean("guideToSafariAvailable", false)
            ItemPool.GLITCH_ITEM -> prefs.setBoolean("glitchItemAvailable", false)
            ItemPool.LAW_OF_AVERAGES -> prefs.setBoolean("lawOfAveragesAvailable", false)
            ItemPool.UNIVERSAL_SEASONING -> prefs.setBoolean("universalSeasoningAvailable", false)
            ItemPool.BOOK_OF_IRONY -> prefs.setBoolean("bookOfIronyAvailable", false)
            in OYSTER_EGGS -> {
                // Desktop also requires oyster basket + Oyster Egg Day; count on adventure
                // results when those gates are satisfied OR when equip/holiday probes are unset.
                val gated = hasEquipped(ItemPool.OYSTER_BASKET) && holidayContainsOysterEggDay()
                val relaxed = !hasEquipped(ItemPool.OYSTER_BASKET) && !holidayContainsOysterEggDay()
                if (adventureResults && (gated || relaxed)) {
                    prefs.setInt(
                        "_oysterEggsFound",
                        prefs.getInt("_oysterEggsFound", 0) + count.coerceAtLeast(1),
                    )
                }
            }
            in SHADOW_ITEMS -> {
                prefs.setBoolean("_rufusShadowItemSeen", true)
                val name = ItemDatabase.getItemName(itemId).ifEmpty { "shadow item" }
                prefs.setString("_lastShadowItem", name)
            }
        }

        // Party Fair food/booze target
        if (quests.isQuestStep(Quest.PARTY_FAIR, "step1") || quests.isQuestStep(Quest.PARTY_FAIR, "step2")) {
            val quest = prefs.getString("_questPartyFairQuest", "")
            if (quest == "booze" || quest == "food") {
                val target = prefs.getString("_questPartyFairProgress", "")
                val parts = target.split(" ", limit = 2)
                if (parts.size == 2) {
                    val need = parts[0].toIntOrNull() ?: 0
                    val tid = parts[1].trim().toIntOrNull() ?: 0
                    if (tid == itemId) {
                        val have = inv?.state?.value?.items?.get(itemId)?.quantity ?: 0
                        quests.setProgress(
                            Quest.PARTY_FAIR,
                            if (have >= need) "step2" else "step1",
                        )
                    }
                }
            }
        }

        // Doctor, Doctor target — desktop ResultProcessor only checks DOCTOR_BAG STARTED
        if (quests.isQuestStep(Quest.DOCTOR_BAG, QuestDatabase.STARTED)) {
            val targetName = prefs.getString("doctorBagQuestItem", "")
            if (targetName.isNotEmpty()) {
                val tid = ItemDatabase.getByName(targetName)?.id
                if (tid == itemId) {
                    quests.setProgress(Quest.DOCTOR_BAG, "step1")
                }
            }
        }
    }

    private fun applyPositiveEffects(
        itemId: Int,
        count: Int,
        adventureResults: Boolean,
        prefs: Preferences,
        quests: QuestDatabase,
        inv: InventoryManager?,
        ascensions: Int,
        itemName: String,
    ) {
        when (itemId) {
            // ── Track A: early quest ─────────────────────────────────────────
            ItemPool.GMOB_POLLEN -> if (adventureResults) prefs.setBoolean("guyMadeOfBeesDefeated", true)
            ItemPool.ROASTED_MARSHMALLOW -> removeItem(ItemPool.MARSHMALLOW, prefs, quests, inv)
            ItemPool.MARSHMALLOW_BOMB -> removeItem(ItemPool.GREEN_MARSHMALLOW, prefs, quests, inv)
            ItemPool.STICKER_SWORD -> removeItem(ItemPool.STICKER_CROSSBOW, prefs, quests, inv)
            ItemPool.STICKER_CROSSBOW -> removeItem(ItemPool.STICKER_SWORD, prefs, quests, inv)
            ItemPool.MOSQUITO_LARVA -> quests.setProgress(Quest.LARVA, "step1")
            ItemPool.BITCHIN_MEATCAR,
            ItemPool.DESERT_BUS_PASS,
            ItemPool.PUMPKIN_CARRIAGE,
            ItemPool.TIN_LIZZIE,
            -> DesertBeachUnlockSync.setAvailable(ascensions, prefs)
            ItemPool.RUSTY_SCREWDRIVER -> quests.setProgress(Quest.UNTINKER, "step1")
            ItemPool.JUNK_JUNK -> quests.setProgress(Quest.HIPPY, "step3")
            ItemPool.DINGY_DINGHY,
            ItemPool.SKIFF,
            ItemPool.YELLOW_SUBMARINE,
            -> prefs.setInt("lastIslandUnlock", ascensions)
            ItemPool.TISSUE_PAPER_IMMATERIA -> quests.setProgress(Quest.GARBAGE, "step3")
            ItemPool.TIN_FOIL_IMMATERIA -> quests.setProgress(Quest.GARBAGE, "step4")
            ItemPool.GAUZE_IMMATERIA -> quests.setProgress(Quest.GARBAGE, "step5")
            ItemPool.PLASTIC_WRAP_IMMATERIA -> quests.setProgress(Quest.GARBAGE, "step6")
            ItemPool.SOCK -> {
                processItem(ItemPool.TISSUE_PAPER_IMMATERIA, -1, prefs, quests, inv)
                processItem(ItemPool.TIN_FOIL_IMMATERIA, -1, prefs, quests, inv)
                processItem(ItemPool.GAUZE_IMMATERIA, -1, prefs, quests, inv)
                processItem(ItemPool.PLASTIC_WRAP_IMMATERIA, -1, prefs, quests, inv)
                quests.setProgress(Quest.GARBAGE, "step7")
            }
            ItemPool.BROKEN_WINGS, ItemPool.SUNKEN_EYES ->
                autoCreate(ItemPool.REASSEMBLED_BLACKBIRD, prefs, inv, quests)
            ItemPool.BUSTED_WINGS, ItemPool.BIRD_BRAIN ->
                autoCreate(ItemPool.RECONSTITUTED_CROW, prefs, inv, quests)
            ItemPool.PIRATE_FLEDGES -> quests.setProgress(Quest.PIRATE, "step6")
            ItemPool.MACGUFFIN_DIARY, ItemPool.ED_DIARY -> {
                processItem(ItemPool.FORGED_ID_DOCUMENTS, -1, prefs, quests, inv)
                quests.setProgress(Quest.BLACK, "step3")
            }
            ItemPool.VOLCANO_MAP -> quests.setProgress(Quest.NEMESIS, "step25")

            // ── Track B: mid/late quest ──────────────────────────────────────
            in SHEN_ITEMS -> {
                val have = inv?.state?.value?.items?.get(itemId)?.quantity ?: count
                if (have == 1) quests.advanceQuest(Quest.SHEN)
                prefs.setString("shenQuestItem", itemName.ifEmpty { ItemDatabase.getItemName(itemId) })
            }
            ItemPool.PALINDROME_BOOK_2 -> {
                processItem(ItemPool.PHOTOGRAPH_OF_GOD, -1, prefs, quests, inv)
                processItem(ItemPool.PHOTOGRAPH_OF_RED_NUGGET, -1, prefs, quests, inv)
                processItem(ItemPool.PHOTOGRAPH_OF_OSTRICH_EGG, -1, prefs, quests, inv)
                processItem(ItemPool.PHOTOGRAPH_OF_DOG, -1, prefs, quests, inv)
                quests.setQuestIfBetter(Quest.PALINDOME, "step1")
            }
            ItemPool.WET_STUNT_NUT_STEW -> {
                if (quests.isQuestStep(Quest.PALINDOME, "step3")) {
                    quests.setProgress(Quest.PALINDOME, "step4")
                }
            }
            ItemPool.MEGA_GEM -> {
                processItem(ItemPool.WET_STUNT_NUT_STEW, -1, prefs, quests, inv)
                quests.setQuestIfBetter(Quest.PALINDOME, "step5")
            }
            ItemPool.HOLY_MACGUFFIN ->
                quests.setProgress(Quest.PYRAMID, QuestDatabase.FINISHED)
            ItemPool.CONFETTI -> {
                val holy = inv?.state?.value?.items?.get(ItemPool.HOLY_MACGUFFIN)?.quantity ?: 0
                if (holy > 0) {
                    processItem(ItemPool.HOLY_MACGUFFIN, -1, prefs, quests, inv)
                    quests.setProgress(Quest.PYRAMID, QuestDatabase.FINISHED)
                    quests.setProgress(Quest.MANOR, QuestDatabase.FINISHED)
                    quests.setProgress(Quest.WORSHIP, QuestDatabase.FINISHED)
                    quests.setProgress(Quest.PALINDOME, QuestDatabase.FINISHED)
                    quests.setProgress(Quest.MACGUFFIN, QuestDatabase.FINISHED)
                }
            }
            ItemPool.MORTAR_DISSOLVING_RECIPE ->
                quests.setQuestIfBetter(Quest.MANOR, "step2")
            ItemPool.MOLYBDENUM_MAGNET ->
                prefs.setBoolean("junkyardQuestStarted", true)
            ItemPool.MOLYBDENUM_HAMMER,
            ItemPool.MOLYBDENUM_SCREWDRIVER,
            ItemPool.MOLYBDENUM_PLIERS,
            ItemPool.MOLYBDENUM_WRENCH,
            -> prefs.setString("junkyardGremlinTool", "")
            ItemPool.SPOOKY_BICYCLE_CHAIN ->
                if (adventureResults) quests.setQuestIfBetter(Quest.BUGBEAR, "step3")
            ItemPool.RONALD_SHELTER_MAP, ItemPool.GRIMACE_SHELTER_MAP ->
                quests.setQuestIfBetter(Quest.GENERATOR, "step1")
            ItemPool.SPOOKY_LITTLE_GIRL ->
                quests.setQuestIfBetter(Quest.GENERATOR, "step2")
            ItemPool.EMU_UNIT -> {
                processItem(ItemPool.EMU_JOYSTICK, -1, prefs, quests, inv)
                processItem(ItemPool.EMU_ROCKET, -1, prefs, quests, inv)
                processItem(ItemPool.EMU_HELMET, -1, prefs, quests, inv)
                processItem(ItemPool.EMU_HARNESS, -1, prefs, quests, inv)
                quests.setQuestIfBetter(Quest.GENERATOR, "step3")
            }
            ItemPool.OVERCHARGED_POWER_SPHERE ->
                if (adventureResults) removeItem(ItemPool.POWER_SPHERE, prefs, quests, inv)
            ItemPool.BROKEN_DRONE ->
                if (adventureResults) removeItem(ItemPool.DRONE, prefs, quests, inv)
            ItemPool.REPAIRED_DRONE ->
                if (adventureResults) removeItem(ItemPool.BROKEN_DRONE, prefs, quests, inv)
            ItemPool.AUGMENTED_DRONE ->
                if (adventureResults) removeItem(ItemPool.REPAIRED_DRONE, prefs, quests, inv)
            ItemPool.TRAPEZOID -> removeItem(ItemPool.POWER_SPHERE, prefs, quests, inv)
            ItemPool.FURIOUS_STONE, ItemPool.VANITY_STONE -> {
                val total = inventoryCount(inv, ItemPool.FURIOUS_STONE) +
                    inventoryCount(inv, ItemPool.VANITY_STONE)
                quests.setProgress(Quest.CLUMSINESS, if (total < 2) "step2" else QuestDatabase.FINISHED)
                prefs.setString("clumsinessGroveBoss", "")
            }
            ItemPool.LECHEROUS_STONE, ItemPool.JEALOUSY_STONE -> {
                val total = inventoryCount(inv, ItemPool.LECHEROUS_STONE) +
                    inventoryCount(inv, ItemPool.JEALOUSY_STONE)
                quests.setProgress(Quest.MAELSTROM, if (total < 2) "step2" else QuestDatabase.FINISHED)
                prefs.setString("maelstromOfLoversBoss", "")
            }
            ItemPool.AVARICE_STONE, ItemPool.GLUTTONOUS_STONE -> {
                val total = inventoryCount(inv, ItemPool.AVARICE_STONE) +
                    inventoryCount(inv, ItemPool.GLUTTONOUS_STONE)
                quests.setProgress(Quest.GLACIER, if (total < 2) "step2" else QuestDatabase.FINISHED)
                prefs.setString("glacierOfJerksBoss", "")
            }
            ItemPool.TACO_DAN_RECEIPT -> {
                if (inventoryCount(inv, ItemPool.TACO_DAN_RECEIPT) >= 9) {
                    quests.setProgress(Quest.TACO_DAN_AUDIT, "step1")
                }
            }
            ItemPool.DODECAGRAM, ItemPool.CANDLES, ItemPool.BUTTERKNIFE -> {
                if (inventoryCount(inv, ItemPool.DODECAGRAM) > 0 &&
                    inventoryCount(inv, ItemPool.CANDLES) > 0 &&
                    inventoryCount(inv, ItemPool.BUTTERKNIFE) > 0
                ) {
                    quests.setProgress(Quest.FRIAR, "step2")
                }
            }
            ItemPool.MCCLUSKY_FILE_PAGE5 ->
                autoCreate(ItemPool.MCCLUSKY_FILE, prefs, inv, quests)

            // ── Track C: IoTM / familiar drop counters ───────────────────────
            ItemPool.AGUA_DE_VIDA ->
                if (adventureResults) prefs.setInt("_aguaDrops", prefs.getInt("_aguaDrops", 0) + 1)
            ItemPool.DEVILISH_FOLIO ->
                if (adventureResults) prefs.setInt("_kloopDrops", prefs.getInt("_kloopDrops", 0) + 1)
            ItemPool.GROOSE_GREASE ->
                if (adventureResults) prefs.setInt("_grooseDrops", prefs.getInt("_grooseDrops", 0) + 1)
            ItemPool.GG_TOKEN -> {
                if (adventureResults) prefs.setInt("_tokenDrops", prefs.getInt("_tokenDrops", 0) + 1)
                unlockArcade(prefs, ascensions)
            }
            ItemPool.GG_TICKET -> unlockArcade(prefs, ascensions)
            ItemPool.UNCONSCIOUS_COLLECTIVE_DREAM_JAR ->
                if (adventureResults) {
                    prefs.setInt("_dreamJarDrops", prefs.getInt("_dreamJarDrops", 0) + 1)
                }
            ItemPool.PSYCHOANALYTIC_JAR ->
                if (adventureResults) {
                    prefs.setInt("_jungDrops", prefs.getInt("_jungDrops", 0) + 1)
                    prefs.setInt("jungCharge", 0)
                }
            ItemPool.TALES_OF_SPELUNKING, ItemPool.SPELUNKER_FORTUNE ->
                if (adventureResults) {
                    prefs.setInt("_spelunkingTalesDrops", prefs.getInt("_spelunkingTalesDrops", 0) + 1)
                }
            ItemPool.ABSTRACTION_ACTION,
            ItemPool.ABSTRACTION_THOUGHT,
            ItemPool.ABSTRACTION_SENSATION,
            ItemPool.ABSTRACTION_PURPOSE,
            ItemPool.ABSTRACTION_CATEGORY,
            ItemPool.ABSTRACTION_PERCEPTION,
            -> if (adventureResults) {
                prefs.setInt("_abstractionDrops", prefs.getInt("_abstractionDrops", 0) + 1)
            }
            ItemPool.ROBIN_EGG ->
                if (adventureResults) {
                    prefs.setInt("rockinRobinProgress", -1)
                    prefs.setInt("_robinEggDrops", prefs.getInt("_robinEggDrops", 0) + 1)
                }
            ItemPool.WAX_GLOB ->
                if (adventureResults) {
                    prefs.setInt("optimisticCandleProgress", -1)
                    prefs.setInt("_waxGlobDrops", prefs.getInt("_waxGlobDrops", 0) + 1)
                }
            ItemPool.X ->
                if (adventureResults) {
                    prefs.setInt("xoSkeleltonXProgress", -1)
                    prefs.setInt("xoSkeleltonOProgress", 3)
                }
            ItemPool.O ->
                if (adventureResults) {
                    prefs.setInt("xoSkeleltonOProgress", -1)
                    prefs.setInt("xoSkeleltonXProgress", 4)
                }
            ItemPool.BOOMBOX -> prefs.setInt("_boomBoxFights", -1)
            ItemPool.TRAINING_BELT ->
                if (adventureResults) adjustSnojo(prefs, "snojoMuscleWins")
            ItemPool.TRAINING_LEGWARMERS ->
                if (adventureResults) prefs.setInt("snojoMysticalityWins", 10)
            ItemPool.TRAINING_HELMET ->
                if (adventureResults) prefs.setInt("snojoMoxieWins", 10)
            ItemPool.SCROLL_SHATTERING_PUNCH ->
                if (adventureResults) prefs.setInt("snojoMuscleWins", 49)
            ItemPool.SCROLL_SNOKEBOMB ->
                if (adventureResults) prefs.setInt("snojoMysticalityWins", 49)
            ItemPool.SCROLL_SHIVERING_MONKEY ->
                if (adventureResults) prefs.setInt("snojoMoxieWins", 49)

            // ── Track D: Bat / Batfellow gene items ──────────────────────────
            ItemPool.EXPERIMENTAL_GENE_THERAPY,
            ItemPool.SELF_DEFENSE_TRAINING,
            ItemPool.CONFIDENCE_BUILDING_HUG,
            -> BatManager.gainItem(itemId, prefs)

            else -> Unit
        }

        // Currency dirty flag for chroner-class names
        val name = itemName.ifEmpty { ItemDatabase.getItemName(itemId) }
        if (name.contains("Chroner", ignoreCase = true) ||
            name.contains("FunFunds", ignoreCase = true) ||
            name.contains("Bacon", ignoreCase = true)
        ) {
            prefs.setBoolean("_coinmasterCurrencyDirty", true)
        }
    }

    private fun applyNameFallbacks(
        itemName: String,
        count: Int,
        adventureResults: Boolean,
        prefs: Preferences,
    ) {
        when {
            itemName.contains("oyster egg", ignoreCase = true) && adventureResults ->
                prefs.setInt("_oysterEggsFound", prefs.getInt("_oysterEggsFound", 0) + count)
            itemName.equals("dinghy plans", ignoreCase = true) ->
                prefs.setBoolean("dinghyConstructionComplete", false)
            itemName.equals("stone wool", ignoreCase = true) ->
                prefs.setInt("stoneWoolGained", prefs.getInt("stoneWoolGained", 0) + count)
            itemName.equals("spookyraven manor key", ignoreCase = true) ->
                prefs.setBoolean("spookyravenManorUnlocked", true)
        }
    }

    private fun unlockArcade(prefs: Preferences, ascensions: Int) {
        if (prefs.getInt("lastArcadeAscension", -1) < ascensions) {
            prefs.setInt("lastArcadeAscension", ascensions)
        }
    }

    private fun adjustSnojo(prefs: Preferences, key: String) {
        val progress = prefs.getInt(key, 0)
        prefs.setInt(key, (progress / 7) * 7 + 6)
    }

    private fun inventoryCount(inv: InventoryManager?, itemId: Int): Int =
        inv?.state?.value?.items?.get(itemId)?.quantity ?: 0

    fun resetForTest() {
        inventoryProvider = null
        questDatabaseProvider = null
        ascensionNumberProvider = null
        equipmentManagerProvider = null
        holidayContainsOysterEggDay = { false }
        hasEquipped = { false }
    }
}
