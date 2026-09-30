package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.adventure.AdventureSession
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.EncounterManager

/**
 * Desktop [GenericRequest.checkItemRedirection] for fight-starting item uses.
 * Called when `inv_use` redirects into combat so the item is consumed and prefs
 * cleared before the fight loop runs.
 */
object CheckItemRedirection {
    // Desktop ItemPool IDs (high-traffic fight starters)
    const val BLACK_PUDDING = 2338
    const val DRUM_MACHINE = 2328
    const val SPOOKY_PUTTY_SHEET = 3665
    const val SPOOKY_PUTTY_MONSTER = 3667
    const val DOLPHIN_WHISTLE = 3997
    const val DURABLE_DOLPHIN_WHISTLE = 11972
    const val SHAKING_CAMERA = 4170
    const val SHAKING_CRAPPY_CAMERA = 6581
    const val PHOTOCOPIED_MONSTER = 4873
    const val RAIN_DOH_BOX = 5563
    const val RAIN_DOH_MONSTER = 5564
    const val WAX_BUGBEAR = 5704
    const val ENVYFISH_EGG = 6388
    const val CRUDE_SCULPTURE = 6677
    const val ICE_SCULPTURE = 7080
    const val LYNYRD_SNARE = 7204
    const val WHITE_PAGE = 7555
    const val RUSTY_HEDGE_TRIMMERS = 5115
    const val SCREENCAPPED_MONSTER = 9023
    const val MEGACOPIA = 9184

    private val BRICKO_FIGHTERS = setOf(
        4474, 4475, 4476, 4477, 4478, 4479, 4480, 4481, 4482, 4483, 4484,
    )

    private val SEAL_FIGURINES = setOf(
        3902, 3903, 3904, 3905, 3906, 3907, 3908, 3909, 3910, 3911,
    )

    data class Result(
        val handled: Boolean,
        val itemMonster: String? = null,
        val consumed: Boolean = false,
    )

    var itemMonster: String? = null
        private set

    fun clearItemMonster() {
        itemMonster = null
    }

    fun apply(
        itemId: Int,
        preferences: Preferences?,
        inventory: InventoryManager?,
        count: Int = 1,
    ): Result {
        itemMonster = null
        if (itemId <= 0) return Result(handled = false)

        var name: String? = null
        var consumed = false
        var nextAdventure: String? = null
        var ignoreSpecial = false

        when (itemId) {
            BLACK_PUDDING -> {
                name = "Black Pudding"
                consumed = true
            }
            DRUM_MACHINE -> {
                name = "Drum Machine"
                consumed = true
            }
            DOLPHIN_WHISTLE, DURABLE_DOLPHIN_WHISTLE -> {
                name = ItemDatabase.getItemName(itemId).ifEmpty { "dolphin whistle" }
                consumed = itemId == DOLPHIN_WHISTLE
                preferences?.setString("dolphinItem", "")
                if (itemId == DURABLE_DOLPHIN_WHISTLE) {
                    preferences?.increment("_durableDolphinWhistleUsed")
                }
            }
            SPOOKY_PUTTY_MONSTER -> {
                name = "Spooky Putty Monster"
                preferences?.setString("spookyPuttyMonster", "")
                inventory?.gainItemLocally(SPOOKY_PUTTY_SHEET, 1)
                consumed = true
                ignoreSpecial = true
            }
            RAIN_DOH_MONSTER -> {
                name = "Rain-Doh box full of monster"
                preferences?.setString("rainDohMonster", "")
                inventory?.gainItemLocally(RAIN_DOH_BOX, 1)
                consumed = true
                ignoreSpecial = true
            }
            SHAKING_CAMERA -> {
                name = "shaking 4-D camera"
                preferences?.setString("cameraMonster", "")
                preferences?.setBoolean("_cameraUsed", true)
                consumed = true
                ignoreSpecial = true
            }
            SHAKING_CRAPPY_CAMERA -> {
                name = "Shaking crappy camera"
                preferences?.setString("crappyCameraMonster", "")
                preferences?.setBoolean("_crappyCameraUsed", true)
                consumed = true
                ignoreSpecial = true
            }
            ICE_SCULPTURE -> {
                name = "ice sculpture"
                preferences?.setString("iceSculptureMonster", "")
                preferences?.setBoolean("_iceSculptureUsed", true)
                consumed = true
                ignoreSpecial = true
            }
            PHOTOCOPIED_MONSTER -> {
                name = "photocopied monster"
                preferences?.setString("photocopyMonster", "")
                preferences?.setBoolean("_photocopyUsed", true)
                consumed = true
                ignoreSpecial = true
            }
            WAX_BUGBEAR -> {
                name = "wax bugbear"
                preferences?.setString("waxMonster", "")
                consumed = true
                ignoreSpecial = true
            }
            ENVYFISH_EGG -> {
                name = "envyfish egg"
                preferences?.setString("envyfishMonster", "")
                preferences?.setBoolean("_envyfishEggUsed", true)
                consumed = true
                ignoreSpecial = true
            }
            CRUDE_SCULPTURE -> {
                name = "crude monster sculpture"
                preferences?.setString("crudeMonster", "")
                consumed = true
                ignoreSpecial = true
            }
            SCREENCAPPED_MONSTER -> {
                name = "screencapped monster"
                preferences?.setString("screencappedMonster", "")
                consumed = true
                ignoreSpecial = true
            }
            MEGACOPIA -> {
                name = "megacopia"
                consumed = true
            }
            WHITE_PAGE -> {
                name = "white page"
                consumed = true
                nextAdventure = "Whitey's Grove"
            }
            RUSTY_HEDGE_TRIMMERS -> {
                name = "rusty hedge trimmers"
                consumed = true
                nextAdventure = "Twin Peak"
            }
            LYNYRD_SNARE -> {
                name = "lynyrd snare"
                consumed = true
                nextAdventure = "A Mob of Zeppelin Protesters"
                preferences?.increment("_lynyrdSnareUses")
            }
            in BRICKO_FIGHTERS -> {
                name = ItemDatabase.getItemName(itemId).ifEmpty { "BRICKO monster" }
                preferences?.increment("_brickoFights")
                consumed = true
            }
            in SEAL_FIGURINES -> {
                name = "Infernal Seal Ritual"
                preferences?.increment("_sealsSummoned")
                preferences?.increment("_sealFigurineUses")
                consumed = true
            }
            else -> return Result(handled = false)
        }

        if (ignoreSpecial) EncounterManager.ignoreSpecialMonsters()
        if (consumed) inventory?.consumeItemLocally(itemId, count.coerceAtLeast(1))

        if (nextAdventure == null) {
            AdventureSession.setLastAdventure("None", preferences)
            AdventureSession.setNextAdventure("None", preferences)
        } else {
            AdventureSession.setLastAdventure(nextAdventure, preferences)
            AdventureSession.setNextAdventure(nextAdventure, preferences)
        }

        itemMonster = name
        return Result(handled = true, itemMonster = name, consumed = consumed)
    }
}
