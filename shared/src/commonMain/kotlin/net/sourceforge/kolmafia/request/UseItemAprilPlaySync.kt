package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.adventure.AdventureSession
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.DesertVisitSync
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.session.ChoiceCombatAshState
import net.sourceforge.kolmafia.session.EquipmentManager
import net.sourceforge.kolmafia.session.RequestLogger
import net.sourceforge.kolmafia.session.SessionLogger

/**
 * Desktop [UseItemRequest.parseAprilPlay]. Each April Band instrument can be
 * played three times a day. A successful play logs `Playing …` and then applies
 * the tuba, tom, or piccolo side effect.
 */
object UseItemAprilPlaySync {
    const val SAXOPHONE = 11566
    const val TOM = 11567
    const val TUBA = 11568
    const val STAFF = 11569
    const val PICCOLO = 11570
    const val WORM_RIDING_HOOKS = 2302

    private val IID = Regex("""iid=(\d+)""", RegexOption.IGNORE_CASE)
    private val FIGHT_LINK = Regex("""href=['"]?/?fight\.php""")

    fun parse(
        url: String,
        responseText: String,
        preferences: Preferences? = null,
        inventory: InventoryManager? = null,
        equipment: EquipmentManager? = null,
        character: KoLCharacter? = null,
        familiarManager: FamiliarManager? = null,
        sessionLogger: SessionLogger? = null,
        questDatabase: QuestDatabase? = null,
        itemName: (Int) -> String = { id ->
            ItemDatabase.getItemName(id).ifBlank { "item #$id" }
        },
        markMultiFight: (Boolean) -> Unit = { ChoiceCombatAshState.inMultiFight = it },
    ) {
        val itemId = IID.find(pagePath(url))?.groupValues?.getOrNull(1)?.toIntOrNull() ?: return
        val preference = preferenceFor(itemId) ?: return
        val prefs = preferences
        if (responseText.contains("enough for one day")) {
            prefs?.setInt(preference, 3)
            return
        }
        if (itemId == SAXOPHONE && responseText.contains("already seem lucky")) return
        if (itemId == PICCOLO && responseText.contains("doesn't seem interested")) return

        RequestLogger.updateSessionLog("Playing ${itemName(itemId)}", sessionLogger)
        prefs?.increment(preference, 1, 3)

        when (itemId) {
            TUBA -> prefs?.setBoolean("noncombatForcerActive", true)
            TOM -> applyTom(responseText, prefs, inventory, equipment, character, sessionLogger, questDatabase, markMultiFight)
            PICCOLO -> {
                if (responseText.contains("You hand the piccolo")) {
                    addFamiliarExperience(40, character, familiarManager)
                }
            }
        }
    }

    private fun applyTom(
        responseText: String,
        preferences: Preferences?,
        inventory: InventoryManager?,
        equipment: EquipmentManager?,
        character: KoLCharacter?,
        sessionLogger: SessionLogger?,
        questDatabase: QuestDatabase?,
        markMultiFight: (Boolean) -> Unit,
    ) {
        if (responseText.contains("hooks were still on")) {
            val equipped = equipment?.getEquipmentId(EquipmentSlot.WEAPON) == WORM_RIDING_HOOKS
            if (equipped) {
                equipment?.discardEquipment(WORM_RIDING_HOOKS)
            } else {
                inventory?.consumeItemLocally(WORM_RIDING_HOOKS, 1)
            }
            if (preferences != null) {
                val progress = preferences.getInt("gnasirProgress", 0) or 16
                preferences.setInt("gnasirProgress", progress)
                DesertVisitSync.incrementExploration(preferences, questDatabase, 30)
            }
            return
        }
        if (responseText.contains("Something moves under your feet")) {
            AdventureSession.setLastAdventure("None", preferences)
            AdventureSession.setNextAdventure("None", preferences)
            AdventureSession.locationLogged = true
            val turns = character?.state?.value?.turnsPlayed
                ?: preferences?.getInt("turnsPlayed", 0)
                ?: 0
            RequestLogger.updateSessionLog("[$turns] Apriling Band Quad Tom", sessionLogger)
            markMultiFight(FIGHT_LINK.containsMatchIn(responseText))
        }
    }

    private fun addFamiliarExperience(
        amount: Int,
        character: KoLCharacter?,
        familiarManager: FamiliarManager?,
    ) {
        val active = familiarManager?.state?.value?.activeFamiliar
        if (active != null) {
            familiarManager.applyActiveFamiliarLocally(
                active.copy(experience = active.experience + amount),
            )
        }
        if (character != null) {
            val state = character.state.value
            character.updateFamiliar(
                state.familiarId,
                state.familiarName,
                state.familiarWeight,
                state.familiarExp + amount,
            )
        }
    }

    private fun preferenceFor(itemId: Int): String? = when (itemId) {
        SAXOPHONE -> "_aprilBandSaxophoneUses"
        TOM -> "_aprilBandTomUses"
        TUBA -> "_aprilBandTubaUses"
        STAFF -> "_aprilBandStaffUses"
        PICCOLO -> "_aprilBandPiccoloUses"
        else -> null
    }

    private fun pagePath(url: String): String {
        val noHash = url.substringBefore('#')
        return if (noHash.contains("://")) {
            noHash.substringAfter("://").substringAfter('/')
        } else {
            noHash.trimStart('/')
        }
    }
}
