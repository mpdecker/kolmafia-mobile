package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.ash.currentDateString
import net.sourceforge.kolmafia.shared.generated.resources.Res
import kotlin.math.pow
import org.jetbrains.compose.resources.ExperimentalResourceApi

// Parses fullness.txt, inebriety.txt, spleenhit.txt, and nonfilling.txt from bundled compose resources.
// Food/drink/spleen format (tab-separated): name  amount  levelReq  quality  adv  musc  myst  moxie  [notes]
// Nonfilling format: name  levelReq  [notes]
// Call load() once at app startup (or lazily on first access).
@OptIn(ExperimentalResourceApi::class)
object ConsumableDatabase {

    private val byNameFood = mutableMapOf<String, ConsumableData>()
    private val byNameDrink = mutableMapOf<String, ConsumableData>()
    private val byNameSpleen = mutableMapOf<String, ConsumableData>()
    private val byNameNonFilling = mutableMapOf<String, ConsumableData>()
    private val bundledByNameFood = mutableMapOf<String, ConsumableData>()
    private val bundledByNameDrink = mutableMapOf<String, ConsumableData>()
    private val bundledByNameSpleen = mutableMapOf<String, ConsumableData>()
    private var loaded = false

    suspend fun load() {
        if (loaded) return
        loadFile("files/data/fullness.txt", ConsumableType.FOOD)
        loadFile("files/data/inebriety.txt", ConsumableType.DRINK)
        loadFile("files/data/spleenhit.txt", ConsumableType.SPLEEN)
        loadNonFilling()
        snapshotBundled()
        loaded = true
    }

    fun getFood(name: String): ConsumableData? = byNameFood[name.lowercase()]
    fun getDrink(name: String): ConsumableData? = byNameDrink[name.lowercase()]
    fun getSpleen(name: String): ConsumableData? = byNameSpleen[name.lowercase()]
    fun getNonFilling(name: String): ConsumableData? = byNameNonFilling[name.lowercase()]

    fun get(name: String, type: ConsumableType): ConsumableData? = when (type) {
        ConsumableType.FOOD -> getFood(name)
        ConsumableType.DRINK -> getDrink(name)
        ConsumableType.SPLEEN -> getSpleen(name)
        ConsumableType.NONFILLING -> getNonFilling(name)
    }

    fun getConsumableByName(name: String): ConsumableData? {
        val key = name.lowercase()
        return byNameFood[key]
            ?: byNameDrink[key]
            ?: byNameSpleen[key]
            ?: byNameNonFilling[key]
    }

    fun getLevelReqByName(name: String): Int? = getConsumableByName(name)?.levelReq

    fun getFullnessByName(name: String): Int = getFood(name)?.amount ?: 0

    fun getInebrietyByName(name: String): Int = getDrink(name)?.amount ?: 0

    fun getSpleenByName(name: String): Int = getSpleen(name)?.amount ?: 0

    fun getQualityName(name: String): String = getConsumableByName(name)?.quality?.displayName() ?: ""

    fun getAdventureRange(name: String): String =
        getConsumableByName(name)?.let { formatRange(it.advMin, it.advMax) } ?: "0"

    fun getMuscleRange(name: String): String =
        getConsumableByName(name)?.let { formatRange(it.muscMin, it.muscMax) } ?: "0"

    fun getMysticalityRange(name: String): String =
        getConsumableByName(name)?.let { formatRange(it.mystMin, it.mystMax) } ?: "0"

    fun getMoxieRange(name: String): String =
        getConsumableByName(name)?.let { formatRange(it.moxieMin, it.moxieMax) } ?: "0"

    fun getNotesByName(name: String): String = getConsumableByName(name)?.notes ?: ""

    fun allFood(): Collection<ConsumableData> = byNameFood.values
    fun allDrinks(): Collection<ConsumableData> = byNameDrink.values
    fun allSpleen(): Collection<ConsumableData> = byNameSpleen.values
    fun allNonFilling(): Collection<ConsumableData> = byNameNonFilling.values

    fun bestFoods(minQuality: ConsumableQuality): List<ConsumableData> =
        byNameFood.values.filter { it.quality >= minQuality }

    fun bestDrinks(minQuality: ConsumableQuality): List<ConsumableData> =
        byNameDrink.values.filter { it.quality >= minQuality }

    private const val AVERAGE_ADVENTURE_CACHE_SIZE = 32

    private val currentAverageAdventures = mutableListOf<MutableMap<String, Double>>()
    private var adventuresNeededContext: ConcoctionAdventuresContext = ConcoctionAdventuresContext.EMPTY

    /** Live session context for craft-turn deduction in average-adventure cache build. */
    fun setAdventuresNeededContextForLive(context: ConcoctionAdventuresContext) {
        adventuresNeededContext = context
    }

    /** Desktop ConsumablesDatabase.calculateAllAverageAdventures — rebuild 32-map average cache. */
    fun calculateAllAverageAdventures() {
        ensureAverageAdventureCache()
        for (map in currentAverageAdventures) {
            map.clear()
        }
        for (consumable in allFood() + allDrinks() + allSpleen()) {
            calculateAverageAdventures(consumable)
        }
    }

    /** Desktop ConsumablesDatabase.getAverageAdventures — v2 active-effect/pref/skill lookup. */
    fun getAverageAdventures(
        name: String,
        context: AverageAdventureContext = AverageAdventureContext.EMPTY,
        extraContext: ConditionalExtraAdventureContext = ConditionalExtraAdventureContext.EMPTY,
    ): Double {
        val consumable = getConsumableByName(name) ?: return 0.0
        if (context.inSlowcore) {
            return 0.0
        }

        val adventuresBoosted = ConcoctionDatabase.getByResult(name)?.areAdventuresBoosted() ?: true

        val range = when (consumable.type) {
            ConsumableType.FOOD -> {
                var value = getAdventureMap(
                    context.perUnit,
                    gainEffect1 = false,
                    gainEffect2 = adventuresBoosted && context.lunchActive,
                    gainEffect3 = adventuresBoosted && context.gourmandActive,
                    gainEffect4 = adventuresBoosted && context.munchiesActive,
                )[name.lowercase()] ?: return 0.0
                if (adventuresBoosted && context.milkActive) {
                    value += 5
                }
                value
            }
            ConsumableType.DRINK -> {
                getAdventureMap(
                    context.perUnit,
                    gainEffect1 = adventuresBoosted && context.odeActive,
                    gainEffect2 = adventuresBoosted && context.rowdyActive,
                    gainEffect3 = false,
                    gainEffect4 = false,
                )[name.lowercase()] ?: return 0.0
            }
            ConsumableType.SPLEEN -> {
                getAdventureMap(
                    context.perUnit,
                    gainEffect1 = false,
                    gainEffect2 = false,
                    gainEffect3 = false,
                    gainEffect4 = false,
                )[name.lowercase()] ?: return 0.0
            }
            ConsumableType.NONFILLING -> return 0.0
        }
        return range + conditionalExtraAdventures(consumable, context.perUnit, extraContext)
    }

    /** Live session lookup — builds contexts from prefs/effects/skills/path/equipment. */
    fun getAverageAdventuresLive(
        name: String,
        preferences: Preferences?,
        activeEffectNames: List<String>,
        skillNames: Set<String>,
        ascensionPath: AscensionPath?,
        isMysticalityClass: Boolean = false,
        equippedItemNames: Set<String> = emptySet(),
        equippedItemIds: Set<Int> = emptySet(),
        itemAvailable: (Int) -> Boolean = { false },
        canEquip: (Int) -> Boolean = { false },
        dateYmd: String = currentDateString(),
    ): Double = getAverageAdventures(
        name,
        buildAverageAdventureContext(
            preferences = preferences,
            activeEffectNames = activeEffectNames,
            skillNames = skillNames,
            ascensionPath = ascensionPath,
        ),
        buildConditionalExtraAdventureContext(
            preferences = preferences,
            activeEffectNames = activeEffectNames,
            skillNames = skillNames,
            ascensionPath = ascensionPath,
            isMysticalityClass = isMysticalityClass,
            equippedItemNames = equippedItemNames,
            equippedItemIds = equippedItemIds,
            dateYmd = dateYmd,
            itemAvailable = itemAvailable,
            canEquip = canEquip,
        ),
    )

    /** Desktop ConsumablesDatabase.getAverageAdventures(effect flags) — direct map lookup. */
    fun getAverageAdventures(
        name: String,
        perUnit: Boolean,
        gainEffect1: Boolean,
        gainEffect2: Boolean,
        gainEffect3: Boolean,
        gainEffect4: Boolean,
    ): Double = getAdventureMap(perUnit, gainEffect1, gainEffect2, gainEffect3, gainEffect4)[name.lowercase()] ?: 0.0

    internal fun resetAverageAdventuresForTest() {
        currentAverageAdventures.clear()
        adventuresNeededContext = ConcoctionAdventuresContext.EMPTY
    }

    private fun ensureAverageAdventureCache() {
        if (currentAverageAdventures.size == AVERAGE_ADVENTURE_CACHE_SIZE) return
        currentAverageAdventures.clear()
        repeat(AVERAGE_ADVENTURE_CACHE_SIZE) {
            currentAverageAdventures.add(mutableMapOf())
        }
    }

    private fun calculateAverageAdventures(consumable: ConsumableData) {
        val name = consumable.name
        var start = consumable.advMin
        var end = consumable.advMax
        val size = consumable.amount
        val conc = ConcoctionDatabase.getByResult(name)
        val advs = conc?.let {
            getAdventuresNeeded(it, 1, considerFree = true, adventuresNeededContext)
        } ?: 0

        val average = (start + end) / 2.0 - advs
        val benefit = average != 0.0

        val gain0 = if (benefit) average else 0.0
        val gain1 = if (benefit) average + size else 0.0
        val gain2 = if (benefit) average + size * 2.0 else 0.0

        addCurrentAdventures(name, size, gainEffect1 = false, gainEffect2 = false, gainEffect3 = false, gainEffect4 = false, result = gain0)
        addCurrentAdventures(name, size, gainEffect1 = true, gainEffect2 = false, gainEffect3 = false, gainEffect4 = false, result = gain1)
        addCurrentAdventures(name, size, gainEffect1 = false, gainEffect2 = true, gainEffect3 = false, gainEffect4 = false, result = gain1)
        addCurrentAdventures(name, size, gainEffect1 = true, gainEffect2 = true, gainEffect3 = false, gainEffect4 = false, result = gain2)

        if (consumable.type != ConsumableType.FOOD) {
            return
        }

        val munchieBonus = when {
            end <= 3 -> 3.0
            start >= 7 -> 1.0
            else -> {
                var munchieTotal = 0
                for (i in start..end) {
                    munchieTotal += maxOf((12 - i) / 3, 1)
                }
                munchieTotal.toDouble() / (end - start + 1)
            }
        }

        val gain3 = if (benefit) average + size * 3.0 else 0.0
        val gain0a = if (benefit) average + munchieBonus else 0.0
        val gain1a = if (benefit) average + size + munchieBonus else 0.0
        val gain2a = if (benefit) average + size * 2.0 + munchieBonus else 0.0
        val gain3a = if (benefit) average + size * 3.0 + munchieBonus else 0.0

        addCurrentAdventures(name, size, gainEffect1 = false, gainEffect2 = true, gainEffect3 = false, gainEffect4 = false, result = gain1)
        addCurrentAdventures(name, size, gainEffect1 = false, gainEffect2 = false, gainEffect3 = true, gainEffect4 = false, result = gain1)
        addCurrentAdventures(name, size, gainEffect1 = true, gainEffect2 = false, gainEffect3 = true, gainEffect4 = false, result = gain2)
        addCurrentAdventures(name, size, gainEffect1 = false, gainEffect2 = true, gainEffect3 = true, gainEffect4 = false, result = gain2)
        addCurrentAdventures(name, size, gainEffect1 = true, gainEffect2 = true, gainEffect3 = true, gainEffect4 = false, result = gain3)
        addCurrentAdventures(name, size, gainEffect1 = false, gainEffect2 = false, gainEffect3 = false, gainEffect4 = true, result = gain0a)
        addCurrentAdventures(name, size, gainEffect1 = true, gainEffect2 = false, gainEffect3 = false, gainEffect4 = true, result = gain1a)
        addCurrentAdventures(name, size, gainEffect1 = false, gainEffect2 = true, gainEffect3 = false, gainEffect4 = true, result = gain1a)
        addCurrentAdventures(name, size, gainEffect1 = false, gainEffect2 = false, gainEffect3 = true, gainEffect4 = true, result = gain1a)
        addCurrentAdventures(name, size, gainEffect1 = true, gainEffect2 = true, gainEffect3 = false, gainEffect4 = true, result = gain2a)
        addCurrentAdventures(name, size, gainEffect1 = true, gainEffect2 = false, gainEffect3 = true, gainEffect4 = true, result = gain2a)
        addCurrentAdventures(name, size, gainEffect1 = false, gainEffect2 = true, gainEffect3 = true, gainEffect4 = true, result = gain2a)
        addCurrentAdventures(name, size, gainEffect1 = true, gainEffect2 = true, gainEffect3 = true, gainEffect4 = true, result = gain3a)
    }

    private fun addCurrentAdventures(
        name: String,
        unitCost: Int,
        gainEffect1: Boolean,
        gainEffect2: Boolean,
        gainEffect3: Boolean,
        gainEffect4: Boolean,
        result: Double,
    ) {
        val key = name.lowercase()
        getAdventureMap(false, gainEffect1, gainEffect2, gainEffect3, gainEffect4)[key] = result
        getAdventureMap(true, gainEffect1, gainEffect2, gainEffect3, gainEffect4)[key] =
            result / (if (unitCost == 0) 1 else unitCost)
    }

    private fun getAdventureMap(
        perUnit: Boolean,
        gainEffect1: Boolean,
        gainEffect2: Boolean,
        gainEffect3: Boolean,
        gainEffect4: Boolean,
    ): MutableMap<String, Double> {
        ensureAverageAdventureCache()
        return currentAverageAdventures[
            adventureFlagsToKey(perUnit, gainEffect1, gainEffect2, gainEffect3, gainEffect4),
        ]
    }

    private fun adventureFlagsToKey(
        perUnit: Boolean,
        gainEffect1: Boolean,
        gainEffect2: Boolean,
        gainEffect3: Boolean,
        gainEffect4: Boolean,
    ): Int =
        ((if (perUnit) 1 else 0) shl 4) or
            ((if (gainEffect1) 1 else 0) shl 3) or
            ((if (gainEffect2) 1 else 0) shl 2) or
            ((if (gainEffect3) 1 else 0) shl 1) or
            (if (gainEffect4) 1 else 0)

    /** Desktop ConsumablesDatabase.updateConsumable — runtime TCRS override for food/drink/spleen. */
    fun updateConsumable(
        itemName: String,
        size: Int,
        level: Int,
        quality: ConsumableQuality,
        adv: String,
        mus: String,
        myst: String,
        mox: String,
        notes: String,
    ): Boolean {
        val key = itemName.lowercase()
        val existing = getFood(itemName) ?: getDrink(itemName) ?: getSpleen(itemName) ?: return false
        val (advMin, advMax) = parseRange(adv)
        val (muscMin, muscMax) = parseRange(mus)
        val (mystMin, mystMax) = parseRange(myst)
        val (moxieMin, moxieMax) = parseRange(mox)
        val updated = existing.copy(
            amount = size,
            levelReq = level,
            quality = quality,
            advMin = advMin,
            advMax = advMax,
            muscMin = muscMin,
            muscMax = muscMax,
            mystMin = mystMin,
            mystMax = mystMax,
            moxieMin = moxieMin,
            moxieMax = moxieMax,
            notes = notes,
        )
        when (existing.type) {
            ConsumableType.FOOD -> byNameFood[key] = updated
            ConsumableType.DRINK -> byNameDrink[key] = updated
            ConsumableType.SPLEEN -> byNameSpleen[key] = updated
            ConsumableType.NONFILLING -> return false
        }
        return true
    }

    /** Restore runtime overrides from bundled fullness/inebriety/spleenhit snapshots. */
    fun resetOverrides() {
        byNameFood.clear()
        byNameFood.putAll(bundledByNameFood)
        byNameDrink.clear()
        byNameDrink.putAll(bundledByNameDrink)
        byNameSpleen.clear()
        byNameSpleen.putAll(bundledByNameSpleen)
    }

    /**
     * Desktop ConsumablesDatabase.setLevelVariableConsumables — adventure/stat ranges that scale
     * with character level (capped 3–11). Called after TCRS apply/reset.
     */
    fun setLevelVariableConsumables(characterLevel: Int) {
        val level = characterLevel.coerceIn(3, 11)

        updateLevelVariableDrink(
            name = "astral pilsner",
            adventures = level.toString(),
            statGain = "0-${2 * level}",
            notes = getNotesByName("astral pilsner"),
        )

        val hotDogAdvMin = kotlin.math.ceil(1.8 * level).toInt()
        val hotDogAdvMax = kotlin.math.floor(2.2 * level).toInt()
        updateLevelVariableFood(
            name = "astral hot dog",
            adventures = "$hotDogAdvMin-$hotDogAdvMax",
            statGain = "${16 * level}-${20 * level}",
            notes = getNotesByName("astral hot dog"),
        )

        val energyAdvBase = 10 + level * 2
        updateLevelVariableSpleen(
            name = "[5140]astral energy drink",
            adventures = "${energyAdvBase - 3}-${energyAdvBase + 3}",
            notes = getNotesByName("[5140]astral energy drink"),
        )

        val breakfastAdv = floatToRange((level + 1) / 2.0)
        updateLevelVariableFood(
            name = "spaghetti breakfast",
            adventures = breakfastAdv,
            statGain = "0",
            notes = getNotesByName("spaghetti breakfast"),
        )

        val coldOneAdv = floatToRange((level + 1) / 2.0)
        updateLevelVariableDrink(
            name = "Cold One",
            adventures = coldOneAdv,
            statGain = "0",
            notes = getNotesByName("Cold One"),
        )
    }

    /** Desktop ConsumablesDatabase.setVariableConsumables — level + pref-driven consumables. */
    fun setVariableConsumables(preferences: Preferences, characterLevel: Int) {
        setLevelVariableConsumables(characterLevel)
        setSmoresData(preferences)
        setAffirmationCookieData(preferences)
        setDistillateData(preferences)
        ConcoctionDatabase.markRecalculateAdventureRange()
    }

    /** Desktop ConsumablesDatabase.setSmoresData — fullness/adventures scale with smoresEaten pref. */
    fun setSmoresData(preferences: Preferences) {
        val existing = getFood("s'more") ?: return
        val size = preferences.getInt("smoresEaten", 0) + 1
        val adventures = size.toDouble().pow(1.75).let { kotlin.math.ceil(it).toInt() }.toString()
        updateConsumable(
            itemName = "s'more",
            size = size,
            level = existing.levelReq,
            quality = ConsumableQuality.CRAPPY,
            adv = adventures,
            mus = "0",
            myst = "0",
            mox = "0",
            notes = "",
        )
    }

    /** Desktop ConsumablesDatabase.setAffirmationCookieData — stats/adv scale with cookies eaten. */
    fun setAffirmationCookieData(preferences: Preferences) {
        val existing = getFood("Affirmation Cookie") ?: return
        val count = minOf(4, preferences.getInt("affirmationCookiesEaten", 0) + 1)
        val statGain = (30 * count).toString()
        updateConsumable(
            itemName = "Affirmation Cookie",
            size = existing.amount,
            level = existing.levelReq,
            quality = ConsumableQuality.GOOD,
            adv = (2 * count + 1).toString(),
            mus = statGain,
            myst = statGain,
            mox = statGain,
            notes = "",
        )
    }

    /** Desktop ConsumablesDatabase.setDistillateData — adv/notes scale with familiarSweat pref. */
    fun setDistillateData(preferences: Preferences) {
        val existing = getDrink("stillsuit distillate") ?: return
        var drams = preferences.getInt("familiarSweat", 0)
        if (drams < 10) drams = 0
        val adventures = drams.toDouble().pow(0.4).let { kotlin.math.round(it).toInt() }.toString()
        val effectTurns = minOf(100, drams / 5)
        updateConsumable(
            itemName = "stillsuit distillate",
            size = existing.amount,
            level = existing.levelReq,
            quality = existing.quality,
            adv = adventures,
            mus = "0",
            myst = "0",
            mox = "0",
            notes = "$effectTurns Buzzed on Distillate",
        )
    }

    private fun updateLevelVariableFood(
        name: String,
        adventures: String,
        statGain: String,
        notes: String,
    ) {
        val existing = getFood(name) ?: return
        updateConsumable(
            itemName = name,
            size = existing.amount,
            level = existing.levelReq,
            quality = ConsumableQuality.UNKNOWN,
            adv = adventures,
            mus = statGain,
            myst = statGain,
            mox = statGain,
            notes = notes,
        )
    }

    private fun updateLevelVariableDrink(
        name: String,
        adventures: String,
        statGain: String,
        notes: String,
    ) {
        val existing = getDrink(name) ?: return
        updateConsumable(
            itemName = name,
            size = existing.amount,
            level = existing.levelReq,
            quality = ConsumableQuality.UNKNOWN,
            adv = adventures,
            mus = statGain,
            myst = statGain,
            mox = statGain,
            notes = notes,
        )
    }

    private fun updateLevelVariableSpleen(
        name: String,
        adventures: String,
        notes: String,
    ) {
        val existing = getSpleen(name) ?: return
        updateConsumable(
            itemName = name,
            size = existing.amount,
            level = existing.levelReq,
            quality = ConsumableQuality.UNKNOWN,
            adv = adventures,
            mus = "0",
            myst = "0",
            mox = "0",
            notes = notes,
        )
    }

    /** Desktop ConsumablesDatabase.floatToRange — round half-up friendly range for fractional adv. */
    private fun floatToRange(average: Double): String {
        val floor = kotlin.math.floor(average + 0.0001).toLong()
        val ceiling = kotlin.math.ceil(average - 0.0001).toLong()
        return if (floor < ceiling) {
            "${if (floor < 0) "-" else ""}$floor-$ceiling"
        } else {
            floor.toString()
        }
    }

    internal fun resetForTest() {
        byNameFood.clear()
        byNameDrink.clear()
        byNameSpleen.clear()
        byNameNonFilling.clear()
        bundledByNameFood.clear()
        bundledByNameDrink.clear()
        bundledByNameSpleen.clear()
        loaded = false
        resetAverageAdventuresForTest()
    }

    internal fun injectForTest(entry: ConsumableData) {
        val key = entry.name.lowercase()
        val map = when (entry.type) {
            ConsumableType.FOOD -> byNameFood
            ConsumableType.DRINK -> byNameDrink
            ConsumableType.SPLEEN -> byNameSpleen
            ConsumableType.NONFILLING -> byNameNonFilling
        }
        val bundled = when (entry.type) {
            ConsumableType.FOOD -> bundledByNameFood
            ConsumableType.DRINK -> bundledByNameDrink
            ConsumableType.SPLEEN -> bundledByNameSpleen
            ConsumableType.NONFILLING -> return
        }
        map[key] = entry
        bundled[key] = entry
        loaded = true
    }

    private fun snapshotBundled() {
        bundledByNameFood.clear()
        bundledByNameFood.putAll(byNameFood)
        bundledByNameDrink.clear()
        bundledByNameDrink.putAll(byNameDrink)
        bundledByNameSpleen.clear()
        bundledByNameSpleen.putAll(byNameSpleen)
    }

    internal fun applyNonFillingParse(text: String) {
        byNameNonFilling.clear()
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            if (!line.contains('\t') && line.toIntOrNull() != null) continue

            val parts = line.split('\t')
            if (parts.size < 2) continue

            val entryName = parts[0].trim()
            val levelReq = parts[1].trim().toIntOrNull() ?: continue
            val notes = parts.getOrNull(2)?.trim() ?: ""

            byNameNonFilling[entryName.lowercase()] = ConsumableData(
                name = entryName,
                type = ConsumableType.NONFILLING,
                amount = 0,
                levelReq = levelReq,
                quality = ConsumableQuality.NONE,
                advMin = 0,
                advMax = 0,
                muscMin = 0,
                muscMax = 0,
                mystMin = 0,
                mystMax = 0,
                moxieMin = 0,
                moxieMax = 0,
                notes = notes,
            )
        }
    }

    private suspend fun loadNonFilling() {
        val text = Res.readBytes("files/data/nonfilling.txt").decodeToString()
        applyNonFillingParse(text)
    }

    private suspend fun loadFile(filename: String, type: ConsumableType) {
        val text = Res.readBytes(filename).decodeToString()
        val map = when (type) {
            ConsumableType.FOOD -> byNameFood
            ConsumableType.DRINK -> byNameDrink
            ConsumableType.SPLEEN -> byNameSpleen
            ConsumableType.NONFILLING -> byNameNonFilling
        }
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            // Skip version-only lines (entire content is a bare integer with no tabs)
            if (!line.contains('\t') && line.toIntOrNull() != null) continue

            val parts = line.split('\t')
            if (parts.size < 8) continue

            val entryName = parts[0]
            val amount = parts[1].toIntOrNull() ?: continue
            val levelReq = parts[2].toIntOrNull() ?: continue
            val quality = ConsumableQuality.fromString(parts[3])
            val (advMin, advMax) = parseRange(parts[4])
            val (muscMin, muscMax) = parseRange(parts[5])
            val (mystMin, mystMax) = parseRange(parts[6])
            val (moxieMin, moxieMax) = parseRange(parts[7])
            val notes = parts.getOrNull(8)?.trim() ?: ""

            val entry = ConsumableData(
                name = entryName,
                type = type,
                amount = amount,
                levelReq = levelReq,
                quality = quality,
                advMin = advMin,
                advMax = advMax,
                muscMin = muscMin,
                muscMax = muscMax,
                mystMin = mystMin,
                mystMax = mystMax,
                moxieMin = moxieMin,
                moxieMax = moxieMax,
                notes = notes,
            )
            map[entryName.lowercase()] = entry
        }
    }

    /** Parses "10-14" into Pair(10,14) and "8" into Pair(8,8). */
    private fun parseRange(s: String): Pair<Int, Int> {
        val trimmed = s.trim()
        val dashIdx = trimmed.indexOf('-', startIndex = 1) // skip possible leading minus
        return if (dashIdx > 0) {
            val min = trimmed.substring(0, dashIdx).toIntOrNull() ?: 0
            val max = trimmed.substring(dashIdx + 1).toIntOrNull() ?: 0
            Pair(min, max)
        } else {
            val v = trimmed.toIntOrNull() ?: 0
            Pair(v, v)
        }
    }

    internal fun formatRange(min: Int, max: Int): String = when {
        min == 0 && max == 0 -> "0"
        min == max -> min.toString()
        else -> "$min-$max"
    }

    internal fun injectConsumableForTest(consumable: ConsumableData) {
        val key = consumable.name.lowercase()
        when (consumable.type) {
            ConsumableType.FOOD -> byNameFood[key] = consumable
            ConsumableType.DRINK -> byNameDrink[key] = consumable
            ConsumableType.SPLEEN -> byNameSpleen[key] = consumable
            ConsumableType.NONFILLING -> byNameNonFilling[key] = consumable
        }
        loaded = true
    }

    internal fun resetConsumablesForTest() {
        byNameFood.clear()
        byNameDrink.clear()
        byNameSpleen.clear()
        byNameNonFilling.clear()
        bundledByNameFood.clear()
        bundledByNameDrink.clear()
        bundledByNameSpleen.clear()
        loaded = false
        resetAverageAdventuresForTest()
    }
}
