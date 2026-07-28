package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.character.CharacterClass
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.ZodiacSign
import net.sourceforge.kolmafia.data.NpcStoreData
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase.Companion.UNSTARTED
import net.sourceforge.kolmafia.quest.QuestDatabase.Companion.stepOrdinal

/** Desktop NPCStoreDatabase.canPurchase shop/item gates (meat affordability ignored). */
object NpcPurchaseAccessibility {

    private const val LAB_KEY = 339
    private const val SPARE_KIDNEY = 2718
    private const val FORGED_ID_DOCUMENTS = 2064

    fun canPurchaseIgnoringMeat(
        itemId: Int,
        store: NpcStoreData,
        state: CharacterState,
        prefs: Preferences? = null,
        accessibleCount: (Int) -> Int = { 0 },
    ): Boolean {
        if (isSavageBeast(prefs)) return false
        return shopItemAvailable(store.storeKey, store.storeName, itemId, state, prefs, accessibleCount)
    }

    private fun isSavageBeast(prefs: Preferences?): Boolean =
        prefs?.getString("_savageBeastMods", "")?.isNotEmpty() == true

    private fun inBadMoon(state: CharacterState): Boolean =
        ZodiacSign.find(state.zodiacSign)?.isBadMoon == true

    private fun guildStoreOpen(state: CharacterState, prefs: Preferences?): Boolean {
        if (state.inNuclearAutumn || state.inPokefam) return false
        return prefs?.getInt("lastGuildStoreOpen", -1) == state.ascensionNumber
    }

    private fun dispensaryOpen(state: CharacterState, prefs: Preferences?, accessibleCount: (Int) -> Int): Boolean {
        if (prefs?.getInt("lastDispensaryOpen", -1) != state.ascensionNumber) return false
        return accessibleCount(LAB_KEY) > 0
    }

    private fun shopItemAvailable(
        storeKey: String,
        storeName: String,
        itemId: Int,
        state: CharacterState,
        prefs: Preferences?,
        accessibleCount: (Int) -> Int,
    ): Boolean {
        return when (storeKey.lowercase()) {
            "guildstore1" -> {
                state.characterClassEnum.isMoxieBased && guildStoreOpen(state, prefs)
            }
            "guildstore2" -> {
                (state.characterClassEnum.isMysticality ||
                    (state.characterClassEnum == CharacterClass.ACCORDION_THIEF && state.level >= 9)) &&
                    guildStoreOpen(state, prefs)
            }
            "guildstore3" -> {
                ((state.characterClassEnum.isMuscleBased && !state.isAxecore) ||
                    (state.characterClassEnum == CharacterClass.ACCORDION_THIEF && state.level >= 9)) &&
                    guildStoreOpen(state, prefs)
            }
            "hiddentavern" ->
                prefs?.getInt("hiddenTavernUnlock", -1) == state.ascensionNumber
            "jewelers" ->
                !state.inZombiecore && canadiaAvailable(state)
            "knobdisp" ->
                dispensaryOpen(state, prefs, accessibleCount)
            "doc" -> {
                if (state.inZombiecore || state.inNuclearAutumn || state.isKingdomOfExploathing) {
                    return false
                }
                true
            }
            "bugbear" -> !state.inNuclearAutumn
            "chateau" ->
                prefs?.getBoolean("chateauAvailable", false) == true
            "blackmarket" -> {
                if (!blackMarketAvailable(state, prefs)) return false
                when (itemId) {
                    SPARE_KIDNEY -> inBadMoon(state) && accessibleCount(SPARE_KIDNEY) <= 0
                    FORGED_ID_DOCUMENTS -> forgedIdDocumentsAvailable(prefs)
                    else -> true
                }
            }
            "wildfire" -> {
                if (!state.isFirecore) return false
                when (itemId) {
                    else -> true
                }
            }
            else -> true
        }
    }

    private fun canadiaAvailable(state: CharacterState): Boolean {
        if (state.isKingdomOfExploathing) return false
        val sign = ZodiacSign.find(state.zodiacSign) ?: return false
        return sign == ZodiacSign.BLENDER || sign == ZodiacSign.PACKRAT || sign == ZodiacSign.VOLE
    }

    private fun blackMarketAvailable(state: CharacterState, prefs: Preferences?): Boolean {
        if (prefs?.getInt("lastWuTangDefeated", -1) == state.ascensionNumber) return false
        val progress = prefs?.getString(Quest.MACGUFFIN.prefKey, UNSTARTED) ?: UNSTARTED
        return progress == "finished" || progress.contains("step")
    }

    private fun forgedIdDocumentsAvailable(prefs: Preferences?): Boolean {
        val progress = prefs?.getString(Quest.MACGUFFIN.prefKey, UNSTARTED) ?: UNSTARTED
        return stepOrdinal(progress) <= stepOrdinal("step1")
    }
}
