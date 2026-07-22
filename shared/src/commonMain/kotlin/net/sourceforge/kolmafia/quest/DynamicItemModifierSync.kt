package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.data.EffectDatabase
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [InventoryManager.checkItem] / effect mod prefs → [ModifierDatabase.overrideModifier]. */
object DynamicItemModifierSync {

    const val COAT_OF_PAINT_ITEM = "fresh coat of paint"
    private const val COAT_OF_PAINT_PREF = "_coatOfPaintModifier"

    private val PREF_TO_ITEM = linkedMapOf(
        "_noHatModifier" to "no hat",
        "jickSwordModifier" to "Sword of Procedural Generation",
        "_pantogramModifier" to "pantogram pants",
        "_futuristicShirtModifier" to "futuristic shirt",
        "_futuristicHatModifier" to "futuristic hat",
        "_futuristicCollarModifier" to "futuristic collar",
        "latteModifier" to "latte lovers member's mug",
    )

    private val PREF_TO_EFFECT = linkedMapOf(
        "currentDistillateMods" to "Buzzed on Distillate",
        "_birdOfTheDayMods" to "Blessing of the Bird",
        "yourFavoriteBirdMods" to "Blessing of your favorite Bird",
        "_citizenZoneMods" to "Citizen of a Zone",
        "zootGraftedMods" to "Grafted",
        "heartstoneAttunementMods" to "Heartstone Attunement",
        "zootMilkCrueltyMods" to "Milk of Familiar Cruelty",
        "zootMilkKindnessMods" to "Milk of Familiar Kindness",
        "_savageBeastMods" to "Savage Beast",
    )

    /** Handled outside v1 active/pref gating with desktop login/extended semantics. */
    private val EXTENDED_EFFECT_PREFS = setOf(
        "zootGraftedMods",
        "zootMilkCrueltyMods",
        "zootMilkKindnessMods",
        "heartstoneAttunementMods",
        "_savageBeastMods",
    )

    private val ZOOT_EFFECT_NAMES = listOf(
        "Grafted",
        "Milk of Familiar Kindness",
        "Milk of Familiar Cruelty",
    )

    /** Desktop login extras: owned-item desc visits without pref gating. */
    val OWNED_DESC_ITEMS = listOf(
        "Kremlin's Greatest Briefcase",
        "Baseball Diamond",
        "Everfull Dart Holster",
        "mimic egg",
    )

    data class CheckContext(
        val inventoryItemIds: Set<Int>,
        val equippedItemNames: Set<String>,
        val activeEffectNames: Set<String>,
        val closetItemIds: Set<Int> = emptySet(),
        val ascensionPath: AscensionPath = AscensionPath.NONE,
    )

    sealed class DescVisit(val path: String) {
        data class Item(val descId: String) : DescVisit("desc_item.php?whichitem=$descId")
        data class Effect(val descId: String) : DescVisit("desc_effect.php?whicheffect=$descId")
        data class Skill(val skillId: Int) : DescVisit("desc_skill.php?whichskill=$skillId&self=true")
    }

    fun applyCachedOverrides(preferences: Preferences) {
        for ((pref, itemName) in PREF_TO_ITEM) {
            val mods = preferences.getString(pref, "")
            if (mods.isNotEmpty()) {
                ModifierDatabase.overrideModifier("Item", itemName, mods)
            }
        }
        val coatMods = preferences.getString(COAT_OF_PAINT_PREF, "")
        if (coatMods.isNotEmpty()) {
            ModifierDatabase.overrideModifier("Item", COAT_OF_PAINT_ITEM, coatMods)
        }
        for ((pref, effectName) in PREF_TO_EFFECT) {
            val mods = preferences.getString(pref, "")
            if (mods.isNotEmpty()) {
                ModifierDatabase.overrideModifier("Effect", effectName, mods)
            }
        }
        applyG9CachedOverride(preferences)
    }

    /** Desktop [InventoryManager.checkMods] — visit desc pages when mod pref empty and item/effect accessible. */
    fun checkMods(
        preferences: Preferences,
        context: CheckContext,
        gameDatabase: GameDatabase,
        playerClassChanged: Boolean = false,
    ): List<DescVisit> {
        val visits = mutableListOf<DescVisit>()
        visits.addAll(checkCoatOfPaint(preferences, context, gameDatabase, playerClassChanged))
        for ((pref, itemName) in PREF_TO_ITEM) {
            val item = gameDatabase.item(itemName) ?: continue
            if (!isEquippedOrInInventory(item.id, item.name, context)) continue
            val mods = preferences.getString(pref, "")
            if (mods.isNotEmpty()) {
                ModifierDatabase.overrideModifier("Item", itemName, mods)
                continue
            }
            if (item.descId.isNotEmpty()) {
                visits.add(DescVisit.Item(item.descId))
            }
        }
        for ((pref, effectName) in PREF_TO_EFFECT) {
            if (pref in EXTENDED_EFFECT_PREFS) continue
            if (!context.activeEffectNames.any { it.equals(effectName, ignoreCase = true) }) continue
            val mods = preferences.getString(pref, "")
            if (mods.isNotEmpty()) {
                ModifierDatabase.overrideModifier("Effect", effectName, mods)
                continue
            }
            val effect = EffectDatabase.getByName(effectName) ?: continue
            if (effect.descId.isNotEmpty()) {
                visits.add(DescVisit.Effect(effect.descId))
            }
        }
        return visits
    }

    /** Desktop [InventoryManager.checkCoatOfPaint]. */
    fun checkCoatOfPaint(
        preferences: Preferences,
        context: CheckContext,
        gameDatabase: GameDatabase,
        playerClassChanged: Boolean,
    ): List<DescVisit> {
        val coat = gameDatabase.item(COAT_OF_PAINT_ITEM) ?: return emptyList()
        if (!isAccessible(coat.id, coat.name, context)) return emptyList()
        val mods = preferences.getString(COAT_OF_PAINT_PREF, "")
        if (!playerClassChanged && mods.isNotEmpty()) {
            ModifierDatabase.overrideModifier("Item", COAT_OF_PAINT_ITEM, mods)
            return emptyList()
        }
        return if (coat.descId.isNotEmpty()) {
            listOf(DescVisit.Item(coat.descId))
        } else {
            emptyList()
        }
    }

    /** Desktop extended [InventoryManager.checkMods] special cases + closet-inclusive accessibility. */
    fun checkExtendedMods(
        preferences: Preferences,
        context: CheckContext,
        gameDatabase: GameDatabase,
    ): List<DescVisit> {
        val visits = mutableListOf<DescVisit>()
        visits.addAll(checkSaber(preferences, context, gameDatabase))
        visits.addAll(checkUmbrella(context, gameDatabase))
        visits.addAll(checkVampireVintnerWine(context, gameDatabase))
        visits.addAll(checkCrimboTrainingManual(preferences, context, gameDatabase))
        visits.addAll(checkRing(context, gameDatabase))
        visits.addAll(checkExperimentalEffectG9(preferences))
        visits.addAll(checkZootomistMods(preferences, context))
        visits.addAll(checkHeartstoneAttunement(preferences, context))
        return visits
    }

    /** Desktop login desc checks from [KoLmafia] post-login block + [ResultProcessor]. */
    fun checkLoginDescChecks(
        preferences: Preferences,
        context: CheckContext,
        gameDatabase: GameDatabase,
    ): List<DescVisit> {
        val visits = mutableListOf<DescVisit>()
        visits.addAll(checkCrownOfThrones(context, gameDatabase))
        visits.addAll(checkBuddyBjorn(context, gameDatabase))
        visits.addAll(BirdOfTheDaySync.checkBirdOfTheDay(context, gameDatabase))
        visits.addAll(checkEntauntauned())
        visits.addAll(checkSavageBeast(preferences, context))
        return visits
    }

    /** Desktop [InventoryManager.checkIfOwned] — visit desc when item accessible. */
    fun checkOwnedItemDescriptions(
        context: CheckContext,
        gameDatabase: GameDatabase,
        itemNames: List<String>,
    ): List<DescVisit> {
        val visits = mutableListOf<DescVisit>()
        for (itemName in itemNames) {
            val item = gameDatabase.item(itemName) ?: continue
            if (!isAccessible(item.id, item.name, context)) continue
            if (item.descId.isNotEmpty()) {
                visits.add(DescVisit.Item(item.descId))
            }
        }
        return visits
    }

    internal fun isEquippedOrInInventory(
        itemId: Int,
        itemName: String,
        context: CheckContext,
    ): Boolean {
        if (context.inventoryItemIds.contains(itemId)) return true
        return context.equippedItemNames.any { it.equals(itemName, ignoreCase = true) }
    }

    internal fun isAccessible(
        itemId: Int,
        itemName: String,
        context: CheckContext,
    ): Boolean {
        if (isEquippedOrInInventory(itemId, itemName, context)) return true
        return context.closetItemIds.contains(itemId)
    }

    internal fun isInInventory(itemId: Int, context: CheckContext): Boolean =
        context.inventoryItemIds.contains(itemId)

    private fun checkSaber(
        preferences: Preferences,
        context: CheckContext,
        gameDatabase: GameDatabase,
    ): List<DescVisit> {
        if (preferences.getString("_saberMod", "0") != "0") return emptyList()
        val saber = gameDatabase.item("Fourth of May Cosplay Saber") ?: return emptyList()
        val replica = gameDatabase.item("replica Fourth of May Cosplay Saber")
        val hasSaber = isAccessible(saber.id, saber.name, context)
        val hasReplica = context.ascensionPath == AscensionPath.LEGACY_OF_LOATHING &&
            replica != null &&
            isAccessible(replica.id, replica.name, context)
        if (!hasSaber && !hasReplica) return emptyList()
        return if (saber.descId.isNotEmpty()) {
            listOf(DescVisit.Item(saber.descId))
        } else {
            emptyList()
        }
    }

    private fun checkUmbrella(
        context: CheckContext,
        gameDatabase: GameDatabase,
    ): List<DescVisit> {
        val umbrella = gameDatabase.item("unbreakable umbrella") ?: return emptyList()
        if (!isAccessible(umbrella.id, umbrella.name, context)) return emptyList()
        return if (umbrella.descId.isNotEmpty()) {
            listOf(DescVisit.Item(umbrella.descId))
        } else {
            emptyList()
        }
    }

    private fun checkVampireVintnerWine(
        context: CheckContext,
        gameDatabase: GameDatabase,
    ): List<DescVisit> {
        val wine = gameDatabase.item("1950 Vampire Vintner wine") ?: return emptyList()
        if (!isInInventory(wine.id, context)) return emptyList()
        return if (wine.descId.isNotEmpty()) {
            listOf(DescVisit.Item(wine.descId))
        } else {
            emptyList()
        }
    }

    private fun checkCrimboTrainingManual(
        preferences: Preferences,
        context: CheckContext,
        gameDatabase: GameDatabase,
    ): List<DescVisit> {
        val skill = preferences.getInt("crimboTrainingSkill", 0)
        if (skill in 1..11) return emptyList()
        val manual = gameDatabase.item("Crimbo training manual") ?: return emptyList()
        if (!isAccessible(manual.id, manual.name, context)) return emptyList()
        return if (manual.descId.isNotEmpty()) {
            listOf(DescVisit.Item(manual.descId))
        } else {
            emptyList()
        }
    }

    private fun checkRing(
        context: CheckContext,
        gameDatabase: GameDatabase,
    ): List<DescVisit> {
        val ring = gameDatabase.item("ring") ?: return emptyList()
        if (!isAccessible(ring.id, ring.name, context)) return emptyList()
        return if (ring.descId.isNotEmpty()) {
            listOf(DescVisit.Item(ring.descId))
        } else {
            emptyList()
        }
    }

    private fun checkExperimentalEffectG9(preferences: Preferences): List<DescVisit> {
        applyG9CachedOverride(preferences)
        val effect = EffectDatabase.getByName("Experimental Effect G-9") ?: return emptyList()
        return if (effect.descId.isNotEmpty()) {
            listOf(DescVisit.Effect(effect.descId))
        } else {
            emptyList()
        }
    }

    private fun checkZootomistMods(
        preferences: Preferences,
        context: CheckContext,
    ): List<DescVisit> {
        if (context.ascensionPath != AscensionPath.Z_IS_FOR_ZOOTOMIST) return emptyList()
        val visits = mutableListOf<DescVisit>()
        for (effectName in ZOOT_EFFECT_NAMES) {
            val pref = PREF_TO_EFFECT.entries.firstOrNull { it.value == effectName }?.key
            if (pref != null) {
                val mods = preferences.getString(pref, "")
                if (mods.isNotEmpty()) {
                    ModifierDatabase.overrideModifier("Effect", effectName, mods)
                }
            }
            val effect = EffectDatabase.getByName(effectName) ?: continue
            if (effect.descId.isNotEmpty()) {
                visits.add(DescVisit.Effect(effect.descId))
            }
        }
        return visits
    }

    private fun checkHeartstoneAttunement(
        preferences: Preferences,
        context: CheckContext,
    ): List<DescVisit> {
        val effectName = "Heartstone Attunement"
        if (!context.activeEffectNames.any { it.equals(effectName, ignoreCase = true) }) {
            return emptyList()
        }
        val mods = preferences.getString("heartstoneAttunementMods", "")
        if (mods.isNotEmpty()) {
            ModifierDatabase.overrideModifier("Effect", effectName, mods)
        }
        val effect = EffectDatabase.getByName(effectName) ?: return emptyList()
        return if (effect.descId.isNotEmpty()) {
            listOf(DescVisit.Effect(effect.descId))
        } else {
            emptyList()
        }
    }

    private fun applyG9CachedOverride(preferences: Preferences) {
        val value = preferences.getString("_g9Effect", "0")
        if (value.isEmpty() || value == "0") return
        val mods = "Muscle Percent: +$value, Mysticality Percent: +$value, Moxie Percent: +$value"
        ModifierDatabase.overrideModifier("Effect", "Experimental Effect G-9", mods)
    }

    private fun checkCrownOfThrones(
        context: CheckContext,
        gameDatabase: GameDatabase,
    ): List<DescVisit> {
        val crownName = "Crown of Thrones"
        if (context.equippedItemNames.any { it.equals(crownName, ignoreCase = true) }) {
            return emptyList()
        }
        val crown = gameDatabase.item(crownName) ?: return emptyList()
        if (!isAccessible(crown.id, crown.name, context)) return emptyList()
        return if (crown.descId.isNotEmpty()) {
            listOf(DescVisit.Item(crown.descId))
        } else {
            emptyList()
        }
    }

    private fun checkBuddyBjorn(
        context: CheckContext,
        gameDatabase: GameDatabase,
    ): List<DescVisit> {
        val bjornName = "Buddy Bjorn"
        if (context.equippedItemNames.any { it.equals(bjornName, ignoreCase = true) }) {
            return emptyList()
        }
        val bjorn = gameDatabase.item(bjornName) ?: return emptyList()
        if (!isAccessible(bjorn.id, bjorn.name, context)) return emptyList()
        return if (bjorn.descId.isNotEmpty()) {
            listOf(DescVisit.Item(bjorn.descId))
        } else {
            emptyList()
        }
    }

    private fun checkEntauntauned(): List<DescVisit> {
        val effect = EffectDatabase.getByName("Entauntauned") ?: return emptyList()
        return if (effect.descId.isNotEmpty()) {
            listOf(DescVisit.Effect(effect.descId))
        } else {
            emptyList()
        }
    }

    private fun checkSavageBeast(
        preferences: Preferences,
        context: CheckContext,
    ): List<DescVisit> {
        val effectName = "Savage Beast"
        if (!context.activeEffectNames.any { it.equals(effectName, ignoreCase = true) }) {
            return emptyList()
        }
        val mods = preferences.getString("_savageBeastMods", "")
        if (mods.isNotEmpty()) {
            ModifierDatabase.overrideModifier("Effect", effectName, mods)
        }
        val effect = EffectDatabase.getByName(effectName) ?: return emptyList()
        return if (effect.descId.isNotEmpty()) {
            listOf(DescVisit.Effect(effect.descId))
        } else {
            emptyList()
        }
    }
}
