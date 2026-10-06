package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.EquipmentManager
import net.sourceforge.kolmafia.session.ResultProcessor

/**
 * Desktop [GenericRequest.prepareForURL] visit preflight hub for visit_url /
 * adventure entry: hermit autoworthless/autopermit, casino pass, orc-chasm bridge,
 * pyramid Staff of Ed, pandamonium comedy equip-then-mourn.
 */
object PrepareForURL {
    const val HERMIT_PERMIT = 42
    const val WORTHLESS_ITEM = 13
    /** Desktop [ItemPool.HERMIT_SCRIPT] / HermitRequest.HACK_SCROLL. */
    const val HACK_SCROLL = 567
    const val CASINO_PASS = 40
    const val BRIDGE = 535
    const val STAFF_OF_ED = 2325
    const val INSULT_PUPPET = 4667
    const val OBSERVATIONAL_GLASSES = 4668
    const val COMEDY_PROP = 4669

    /** Optional DI (wired from GameRuntimeLibrary). */
    var retrieveItem: (suspend (Int, Int) -> Int)? = null
    var useItem: (suspend (Int) -> Boolean)? = null
    var equipItem: (suspend (Int, EquipmentSlot) -> Boolean)? = null
    var unequipSlot: (suspend (EquipmentSlot) -> Boolean)? = null
    var mournComedy: (suspend (String) -> String?)? = null
    var hasEquipped: ((Int) -> Boolean)? = null
    var weaponHands: ((Int) -> Int)? = null
    var getEquipmentId: ((EquipmentSlot) -> Int)? = null
    var inZombiecore: (() -> Boolean)? = null

    fun resetForTest() {
        retrieveItem = null
        useItem = null
        equipItem = null
        unequipSlot = null
        mournComedy = null
        hasEquipped = null
        weaponHands = null
        getEquipmentId = null
        inZombiecore = null
    }

    data class Result(
        /** When false, caller should not submit the original URL (response already captured). */
        val proceed: Boolean,
        val responseText: String? = null,
        val abortMessage: String? = null,
    )

    /**
     * Run preflight for [location]. Returns [Result.proceed]=false when pandamonium
     * mourn already produced a response (desktop short-circuit).
     */
    suspend fun prepare(
        location: String,
        preferences: Preferences? = null,
        inventory: InventoryManager? = null,
        @Suppress("UNUSED_PARAMETER") character: KoLCharacter? = null,
        @Suppress("UNUSED_PARAMETER") equipmentManager: EquipmentManager? = null,
    ): Result {
        val loc = location.removePrefix("https://www.kingdomofloathing.com/")
            .removePrefix("http://www.kingdomofloathing.com/")
            .removePrefix("/")

        if (loc.startsWith("hermit.php?auto", ignoreCase = true) ||
            loc.startsWith("hermit.php", ignoreCase = true) && loc.contains("auto")
        ) {
            val old = preferences?.getBoolean("autoSatisfyWithNPCs", false) ?: false
            try {
                if (!old) preferences?.setBoolean("autoSatisfyWithNPCs", true)
                if (loc.contains("autoworthless=on", ignoreCase = true)) {
                    retrieveItem?.invoke(WORTHLESS_ITEM, 1)
                }
                if (loc.contains("autopermit=on", ignoreCase = true)) {
                    if ((inventory?.getCount(HACK_SCROLL) ?: 0) > 0) {
                        useItem?.invoke(HACK_SCROLL)
                    }
                    retrieveItem?.invoke(HERMIT_PERMIT, 1)
                }
            } finally {
                if (!old) preferences?.setBoolean("autoSatisfyWithNPCs", false)
            }
            return Result(proceed = true)
        }

        if (loc.startsWith("casino.php", ignoreCase = true)) {
            if (inZombiecore?.invoke() != true) {
                retrieveItem?.invoke(CASINO_PASS, 1)
            }
            return Result(proceed = true)
        }

        if (loc.equals("place.php?whichplace=orc_chasm&action=bridge0", ignoreCase = true) ||
            loc.equals("place.php?whichplace=orc_chasm&action=label1", ignoreCase = true)
        ) {
            retrieveItem?.invoke(BRIDGE, 1)
            return Result(proceed = true)
        }

        if (loc.startsWith("place.php?whichplace=desertbeach&action=db_pyramid1", ignoreCase = true) ||
            loc.startsWith(
                "place.php?whichplace=exploathing_beach&action=expl_pyramidpre",
                ignoreCase = true,
            )
        ) {
            // Prefer retrieve (create-if-needed); fall back to local autoCreate stub.
            val retrieved = retrieveItem?.invoke(STAFF_OF_ED, 1) ?: 0
            if (retrieved <= 0) {
                ResultProcessor.autoCreate(STAFF_OF_ED, preferences, inventory)
            }
            return Result(proceed = true)
        }

        if (loc.startsWith("pandamonium.php?action=mourn&whichitem=", ignoreCase = true)) {
            return preparePandamoniumMourn(loc, inventory)
        }

        return Result(proceed = true)
    }

    private suspend fun preparePandamoniumMourn(
        location: String,
        inventory: InventoryManager?,
    ): Result {
        val comedyItemId = Regex("""whichitem=(\d+)""", RegexOption.IGNORE_CASE)
            .find(location)?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: return Result(proceed = false, abortMessage = "Missing comedy item")
        val comedy = when (comedyItemId) {
            INSULT_PUPPET -> "insult" to true
            OBSERVATIONAL_GLASSES -> "observe" to false
            COMEDY_PROP -> "prop" to false
            else -> return Result(
                proceed = false,
                abortMessage = "\"$comedyItemId\" is not a comedy item number that Mafia recognizes.",
            )
        }
        val (comedyAction, offhand) = comedy
        val have = (inventory?.getCount(comedyItemId) ?: 0) > 0
        if (have) {
            if (offhand) {
                val weaponId = getEquipmentId?.invoke(EquipmentSlot.WEAPON) ?: -1
                if (weaponId > 0 && (weaponHands?.invoke(weaponId) ?: 1) > 1) {
                    unequipSlot?.invoke(EquipmentSlot.WEAPON)
                }
            }
            equipItem?.invoke(comedyItemId, if (offhand) EquipmentSlot.OFFHAND else EquipmentSlot.ACC1)
        }
        if (hasEquipped?.invoke(comedyItemId) == true) {
            val text = mournComedy?.invoke(comedyAction)
            if (text != null) {
                return Result(proceed = false, responseText = text)
            }
        }
        return Result(proceed = true)
    }
}
