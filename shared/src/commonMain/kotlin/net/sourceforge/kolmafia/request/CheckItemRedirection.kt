package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.adventure.AdventureSession
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.EncounterManager
import net.sourceforge.kolmafia.session.ResultProcessor

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
    const val SHAKING_CRAPPY_CAMERA = 7176
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
    const val DEPLETED_URANIUM_SEAL = 4296
    const val SEAL_BLUBBER_CANDLE = 3901
    const val IMBUED_SEAL_BLUBBER_CANDLE = 3912
    const val WRETCHED_SEAL = 3902
    const val CUTE_BABY_SEAL = 3903
    const val ARMORED_SEAL = 3904
    const val ANCIENT_SEAL = 3905
    const val FOSSILIZED_BAT_SKULL = 4687
    const val FOSSILIZED_SERPENT_SKULL = 4688
    const val FOSSILIZED_BABOON_SKULL = 4689
    const val FOSSILIZED_WYRM_SKULL = 4690
    const val FOSSILIZED_WING = 4691
    const val FOSSILIZED_LIMB = 4692
    const val FOSSILIZED_TORSO = 4693
    const val FOSSILIZED_SPINE = 4694
    const val FOSSILIZED_SPIKE = 4700
    const val FOSSILIZED_DEMON_SKULL = 4704
    const val FOSSILIZED_SPIDER_SKULL = 4705
    const val GENIE_BOTTLE = 9529
    const val POCKET_WISH = 9537
    const val REPLICA_GENIE_BOTTLE = 11234
    const val CLARIFIED_BUTTER = 9908
    const val AMORPHOUS_BLOB = 9558
    const val GIANT_AMORPHOUS_BLOB = 9561
    const val MOLEHILL_MOUNTAIN = 11106
    const val TIED_UP_LEAFLET = 11352
    const val TIED_UP_MONSTERA = 11353
    const val TIED_UP_LEAVIATHAN = 11354
    const val MAP_TO_A_CANDY_RICH_BLOCK = 11337
    const val MINIATURE_EMBERING_HULK = 11644
    const val CARONCH_MAP = 2950
    const val FRATHOUSE_BLUEPRINTS = 2951
    const val CURSED_PIECE_OF_THIRTEEN = 3034
    const val RONALD_SHELTER_MAP = 5171
    const val GRIMACE_SHELTER_MAP = 5172
    const val D10 = 5288
    const val SHAKING_SKULL = 6412
    const val TONIC_DJINN = 6421
    const val ABYSSAL_BATTLE_PLANS = 6782
    const val SUSPICIOUS_ADDRESS = 6855
    const val CHEF_BOY_BUSINESS_CARD = 6898
    const val XIBLAXIAN_HOLOTRAINING_SIMCODE = 7739
    const val XIBLAXIAN_POLITICAL_PRISONER = 7742
    const val CHATEAU_WATERCOLOR = 8033
    const val DECK_OF_EVERY_CARD = 8382
    const val GIFT_CARD = 8392
    const val BARREL_MAP = 8599
    const val VYKEA_INSTRUCTIONS = 8730
    const val TIME_SPINNER = 9104
    const val TIME_RESIDUE = 9116
    const val BASTILLE_LOANER_VOUCHER = 9938
    const val GLITCH_ITEM = 10207
    const val SIZZLING_DESK_BELL = 10617
    const val FROST_RIMED_DESK_BELL = 10618
    const val UNCANNY_DESK_BELL = 10619
    const val NASTY_DESK_BELL = 10620
    const val GREASY_DESK_BELL = 10621

    private val BRICKO_FIGHTERS = setOf(
        4474, 4475, 4476, 4477, 4478, 4479, 4480, 4481, 4482, 4483, 4484,
    )

    private val SEAL_FIGURINES = setOf(
        3902, 3903, 3904, 3905, 3906, 3907, 3908, 3909, 3910, 3911,
    )

    private val GENIE_ITEMS = setOf(GENIE_BOTTLE, POCKET_WISH, REPLICA_GENIE_BOTTLE)

    private val DESK_BELLS = setOf(
        SIZZLING_DESK_BELL,
        FROST_RIMED_DESK_BELL,
        UNCANNY_DESK_BELL,
        NASTY_DESK_BELL,
        GREASY_DESK_BELL,
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

    /** Used by choice/skill redirection fight labels. */
    fun setItemMonster(name: String?) {
        itemMonster = name
    }

    /** Desktop [GenericRequest.sealRitualCandles] candle itemId → count to consume. */
    fun sealRitualCandles(itemId: Int): Pair<Int, Int>? = when (itemId) {
        WRETCHED_SEAL -> SEAL_BLUBBER_CANDLE to 1
        CUTE_BABY_SEAL -> SEAL_BLUBBER_CANDLE to 5
        ARMORED_SEAL -> SEAL_BLUBBER_CANDLE to 10
        ANCIENT_SEAL -> SEAL_BLUBBER_CANDLE to 3
        in SEAL_FIGURINES, DEPLETED_URANIUM_SEAL -> IMBUED_SEAL_BLUBBER_CANDLE to 1
        else -> null
    }

    fun apply(
        itemId: Int,
        preferences: Preferences?,
        inventory: InventoryManager?,
        count: Int = 1,
        urlString: String = "",
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
            DEPLETED_URANIUM_SEAL -> {
                name = "Infernal Seal Ritual"
                preferences?.increment("_sealsSummoned")
                preferences?.increment("_sealFigurineUses")
                consumeSealCandles(itemId, preferences, inventory)
                // Desktop does not mark the depleted uranium seal itself consumed here.
                consumed = false
            }
            in SEAL_FIGURINES -> {
                name = "Infernal Seal Ritual"
                preferences?.increment("_sealsSummoned")
                consumeSealCandles(itemId, preferences, inventory)
                consumed = true
            }
            FOSSILIZED_BAT_SKULL -> {
                name = "Fossilized Bat Skull"
                consumed = true
                ResultProcessor.processItem(FOSSILIZED_WING, -2, preferences, inventory = inventory)
            }
            FOSSILIZED_BABOON_SKULL -> {
                name = "Fossilized Baboon Skull"
                consumed = true
                ResultProcessor.processItem(FOSSILIZED_TORSO, -1, preferences, inventory = inventory)
                ResultProcessor.processItem(FOSSILIZED_LIMB, -4, preferences, inventory = inventory)
            }
            FOSSILIZED_SERPENT_SKULL -> {
                name = "Fossilized Serpent Skull"
                consumed = true
                ResultProcessor.processItem(FOSSILIZED_SPINE, -3, preferences, inventory = inventory)
            }
            FOSSILIZED_WYRM_SKULL -> {
                name = "Fossilized Wyrm Skull"
                consumed = true
                ResultProcessor.processItem(FOSSILIZED_TORSO, -1, preferences, inventory = inventory)
                ResultProcessor.processItem(FOSSILIZED_LIMB, -2, preferences, inventory = inventory)
                ResultProcessor.processItem(FOSSILIZED_WING, -2, preferences, inventory = inventory)
                ResultProcessor.processItem(FOSSILIZED_SPINE, -3, preferences, inventory = inventory)
            }
            FOSSILIZED_DEMON_SKULL -> {
                name = "Fossilized Demon Skull"
                consumed = true
                ResultProcessor.processItem(FOSSILIZED_TORSO, -1, preferences, inventory = inventory)
                ResultProcessor.processItem(FOSSILIZED_SPIKE, -1, preferences, inventory = inventory)
                ResultProcessor.processItem(FOSSILIZED_LIMB, -4, preferences, inventory = inventory)
                ResultProcessor.processItem(FOSSILIZED_WING, -2, preferences, inventory = inventory)
                ResultProcessor.processItem(FOSSILIZED_SPINE, -1, preferences, inventory = inventory)
            }
            FOSSILIZED_SPIDER_SKULL -> {
                name = "Fossilized Spider Skull"
                consumed = true
                ResultProcessor.processItem(FOSSILIZED_TORSO, -1, preferences, inventory = inventory)
                ResultProcessor.processItem(FOSSILIZED_LIMB, -8, preferences, inventory = inventory)
                ResultProcessor.processItem(FOSSILIZED_SPIKE, -8, preferences, inventory = inventory)
            }
            in GENIE_ITEMS -> {
                // Desktop early-return: do not consume, ignore-special, or log turns here.
                return Result(handled = true, itemMonster = null, consumed = false)
            }
            CARONCH_MAP -> {
                name = "Cap'm Caronch's Map"
            }
            FRATHOUSE_BLUEPRINTS -> {
                name = "Orcish Frat House blueprints"
            }
            CURSED_PIECE_OF_THIRTEEN -> {
                name = "Cursed Piece of Thirteen"
            }
            RONALD_SHELTER_MAP, GRIMACE_SHELTER_MAP -> {
                name = ItemDatabase.getItemName(itemId).ifEmpty {
                    if (itemId == RONALD_SHELTER_MAP) "map to Ronaldus" else "map to Grimacetown"
                }
                consumed = true
            }
            XIBLAXIAN_HOLOTRAINING_SIMCODE -> {
                name = "Xiblaxian holo-training simcode"
                consumed = true
            }
            XIBLAXIAN_POLITICAL_PRISONER -> {
                name = "Xiblaxian encrypted political prisoner"
                consumed = true
            }
            D10 -> {
                // Desktop: only a single D10 generates a monster; inv_use does not
                // redirect, so the item is consumed elsewhere via Javascript.
                if (count != 1) return Result(handled = false)
                name = "d10"
                consumed = false
            }
            SHAKING_SKULL -> {
                name = "shaking skull"
                consumed = true
            }
            ABYSSAL_BATTLE_PLANS -> {
                name = "abyssal battle plans"
            }
            SUSPICIOUS_ADDRESS -> {
                name = "a suspicious address"
            }
            CHEF_BOY_BUSINESS_CARD -> {
                name = "Chef Boy, R&D's business card"
            }
            CHATEAU_WATERCOLOR -> {
                name = "Chateau Painting"
                consumed = false
                preferences?.setBoolean("_chateauMonsterFought", true)
                ignoreSpecial = true
            }
            DECK_OF_EVERY_CARD -> {
                name = "Deck of Every Card"
            }
            GIFT_CARD -> {
                name = "gift card"
                consumed = true
            }
            BARREL_MAP -> {
                name = "map to the Biggest Barrel"
                consumed = true
            }
            VYKEA_INSTRUCTIONS -> {
                name = "VYKEA instructions"
            }
            TONIC_DJINN -> {
                name = "tonic djinn"
            }
            TIME_RESIDUE -> {
                name = "time residue"
                consumed = true
            }
            TIME_SPINNER -> {
                name = "Time-Spinner"
            }
            GLITCH_ITEM -> {
                // Desktop only fight-labels the glitch when eaten via inv_eat.php.
                if (!urlString.contains("inv_eat.php", ignoreCase = true)) {
                    return Result(handled = false)
                }
                name = "[glitch season reward name]"
            }
            in DESK_BELLS, BASTILLE_LOANER_VOUCHER -> {
                name = ItemDatabase.getItemName(itemId).ifEmpty {
                    when (itemId) {
                        BASTILLE_LOANER_VOUCHER -> "Bastille Battalion loaner voucher"
                        else -> "desk bell"
                    }
                }
                consumed = true
            }
            CLARIFIED_BUTTER -> {
                name = "Dish of Clarified Butter"
                preferences?.increment("_godLobsterFights", 1)
                consumed = true
            }
            AMORPHOUS_BLOB -> {
                name = "amorphous blob"
                consumed = true
            }
            GIANT_AMORPHOUS_BLOB -> {
                name = "giant amorphous blob"
                consumed = true
            }
            MOLEHILL_MOUNTAIN -> {
                name = ItemDatabase.getItemName(itemId).ifEmpty { "molehill mountain" }
                preferences?.setBoolean("_molehillMountainUsed", true)
            }
            TIED_UP_LEAFLET -> {
                name = ItemDatabase.getItemName(itemId).ifEmpty { "tied-up flaming leaflet" }
                preferences?.setBoolean("_tiedUpFlamingLeafletFought", true)
                consumed = true
                ignoreSpecial = true
            }
            TIED_UP_MONSTERA -> {
                name = ItemDatabase.getItemName(itemId).ifEmpty { "tied-up flaming monstera" }
                preferences?.setBoolean("_tiedUpFlamingMonsteraFought", true)
                consumed = true
                ignoreSpecial = true
            }
            TIED_UP_LEAVIATHAN -> {
                name = ItemDatabase.getItemName(itemId).ifEmpty { "tied-up leaviathan" }
                preferences?.setBoolean("_tiedUpLeaviathanFought", true)
                consumed = true
                ignoreSpecial = true
            }
            MAP_TO_A_CANDY_RICH_BLOCK -> {
                name = ItemDatabase.getItemName(itemId).ifEmpty { "map to a candy-rich block" }
                preferences?.setBoolean("_mapToACandyRichBlockUsed", true)
                consumed = true
            }
            MINIATURE_EMBERING_HULK -> {
                name = ItemDatabase.getItemName(itemId).ifEmpty { "miniature embering hulk" }
                preferences?.setBoolean("_emberingHulkFought", true)
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

    private fun consumeSealCandles(
        itemId: Int,
        preferences: Preferences?,
        inventory: InventoryManager?,
    ) {
        val (candleId, qty) = sealRitualCandles(itemId) ?: return
        ResultProcessor.processItem(candleId, -qty, preferences, inventory = inventory)
    }
}
