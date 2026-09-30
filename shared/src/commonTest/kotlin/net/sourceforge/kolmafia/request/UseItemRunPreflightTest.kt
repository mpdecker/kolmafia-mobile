package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.campground.CampgroundItemSync
import net.sourceforge.kolmafia.campground.DwellingSync
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.item.RetrieveItemService
import net.sourceforge.kolmafia.preferences.Preferences

class UseItemRunPreflightTest {

    private fun client(handler: MockRequestHandler): HttpClient = HttpClient(MockEngine(handler))

    @Test
    fun sealFigurine_needsAClub() {
        val refused = UseItemRunPreflight.route(3902, wieldingClub = false, ownedCount = 1)
        assertIs<UseItemRunPreflight.Route.Refuse>(refused)
        assertEquals(UseItemRunPreflight.CLUB_MESSAGE, refused.message)
        assertIs<UseItemRunPreflight.Route.Proceed>(
            UseItemRunPreflight.route(3902, wieldingClub = true, ownedCount = 1),
        )
        assertTrue(UseItemRunPreflight.isSealFigurine(4296))
        assertFalse(UseItemRunPreflight.isSealFigurine(2))
    }

    @Test
    fun earlyReturns_deckBrickoStickerDiaryVolcano() {
        assertIs<UseItemRunPreflight.Route.PlayRandomDeck>(
            UseItemRunPreflight.route(UseItemRunPreflight.DECK_OF_EVERY_CARD, true, 1),
        )
        assertIs<UseItemRunPreflight.Route.PlayRandomDeck>(
            UseItemRunPreflight.route(UseItemRunPreflight.REPLICA_DECK_OF_EVERY_CARD, true, 1),
        )
        val missing = UseItemRunPreflight.route(UseItemRunPreflight.BRICKO_SWORD, true, 0)
        assertIs<UseItemRunPreflight.Route.Refuse>(missing)
        assertEquals(UseItemRunPreflight.MISSING_MESSAGE, missing.message)
        assertIs<UseItemRunPreflight.Route.BreakBricko>(
            UseItemRunPreflight.route(UseItemRunPreflight.BRICKO_HAT, true, 1),
        )
        assertIs<UseItemRunPreflight.Route.FoldSticker>(
            UseItemRunPreflight.route(UseItemRunPreflight.STICKER_CROSSBOW, true, 1),
        )
        assertIs<UseItemRunPreflight.Route.ReadDiary>(
            UseItemRunPreflight.route(UseItemRunPreflight.ED_DIARY, true, 0),
        )
        assertIs<UseItemRunPreflight.Route.ReadVolcanoMap>(
            UseItemRunPreflight.route(UseItemRunPreflight.VOLCANO_MAP, true, 0),
        )
    }

    @Test
    fun mementoBinge_refusesListedFood() {
        val prefs = Preferences(MapSettings())
        assertNull(UseItemRunPreflight.mementoRefusal(true, prefs, "tiny bottle of absinthe"))
        prefs.setBoolean("mementoListActive", true)
        prefs.setString("mementoList", "tiny bottle of absinthe")
        assertEquals(
            UseItemRunPreflight.MEMENTO_MESSAGE,
            UseItemRunPreflight.mementoRefusal(true, prefs, "tiny bottle of absinthe"),
        )
        assertNull(UseItemRunPreflight.mementoRefusal(false, prefs, "tiny bottle of absinthe"))
    }

    @Test
    fun replacementConfirm_headlessAllowsDowngrade() {
        assertTrue(UseItemRunPreflight.confirmReplacement())
        assertFalse(UseItemRunPreflight.needsBeddingReplacementConfirm(3345, 0))
        assertTrue(UseItemRunPreflight.needsBeddingReplacementConfirm(3345, 429))
        assertFalse(UseItemRunPreflight.needsDwellingReplacementConfirm(69, UseItemRunPreflight.BIG_ROCK))
        assertTrue(UseItemRunPreflight.needsDwellingReplacementConfirm(69, 526))
        assertTrue(UseItemRunPreflight.needsDwellingReplacementConfirm(10497, 4771))
        assertEquals(1, UseItemRunPreflight.dwellingLevel(69))
        assertEquals(7, UseItemRunPreflight.dwellingLevel(4771))
        assertEquals(0, UseItemRunPreflight.dwellingLevel(UseItemRunPreflight.BIG_ROCK))
    }

    @Test
    fun use_sealWithoutClub_doesNotRequest() = runTest {
        var requested = false
        val http = client {
            requested = true
            respond("ok", HttpStatusCode.OK)
        }
        val result = UseItemRequest(http).use(3902)
        assertTrue(result.isFailure)
        assertEquals(UseItemRunPreflight.CLUB_MESSAGE, result.exceptionOrNull()?.message)
        assertFalse(requested)
    }

    @Test
    fun use_brickoSword_breaksBricksWhenOwned() = runTest {
        val paths = mutableListOf<String>()
        val http = client { request ->
            paths += request.url.fullPath
            respond(
                "You break apart your BRICKO sword.",
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "text/html"),
            )
        }
        val inventory = InventoryManager(http, GameEventBus())
        inventory.gainItemLocally(UseItemRunPreflight.BRICKO_SWORD, 1)
        val result = UseItemRequest(http, inventoryManager = inventory)
            .use(UseItemRunPreflight.BRICKO_SWORD)
        assertTrue(result.isSuccess)
        assertEquals(UseItemRunPreflight.SPLITTING_BRICKS, UseItemRunPreflight.lastUpdate)
        assertTrue(paths.any { it.contains("action=breakbricko") && it.contains("whichitem=4473") })
    }

    @Test
    fun use_diaryAndVolcanoAndDeck_takeEarlyPaths() = runTest {
        GameDatabase().load()
        val paths = mutableListOf<String>()
        val http = client { request ->
            val body = request.body.toByteArray().decodeToString()
            paths += request.url.fullPath + " " + body
            respond(
                "<html>Diary</html>",
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "text/html"),
            )
        }
        val inventory = InventoryManager(http, GameEventBus())
        inventory.gainItemLocally(UseItemRunPreflight.DECK_OF_EVERY_CARD, 1)
        val prefs = Preferences(MapSettings())
        val request = UseItemRequest(http, preferences = prefs, inventoryManager = inventory)

        assertTrue(request.use(UseItemRunPreflight.MACGUFFIN_DIARY).isSuccess)
        assertEquals(UseItemRunPreflight.DIARY_READ, UseItemRunPreflight.lastUpdate)
        assertTrue(
            paths.any { it.contains("diary.php") && it.contains("textversion") },
            paths.toString(),
        )

        paths.clear()
        assertTrue(request.use(UseItemRunPreflight.VOLCANO_MAP, quantity = 5).isSuccess)
        assertEquals(UseItemRunPreflight.VOLCANO_READ, UseItemRunPreflight.lastUpdate)
        assertTrue(paths.any { it.contains("inv_use.php") && it.contains("whichitem=3291") })
        assertFalse(paths.any { it.contains("quantity=5") })

        paths.clear()
        val deck = request.use(UseItemRunPreflight.DECK_OF_EVERY_CARD)
        assertTrue(deck.isSuccess, deck.exceptionOrNull()?.message)
        assertTrue(paths.any { it.contains("inv_use.php") && it.contains("whichitem=8382") })
        assertTrue(paths.any { it.contains("choice.php") && it.contains("whichchoice=1085") })
    }

    @Test
    fun binge_memento_skipsTheRequest() = runTest {
        GameDatabase().load()
        var requested = false
        val http = client {
            requested = true
            respond("fed", HttpStatusCode.OK)
        }
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("mementoListActive", true)
        prefs.setString("mementoList", "tiny bottle of absinthe")
        val result = UseItemRequest(http, preferences = prefs).binge(2655, 1)
        assertTrue(result.isFailure)
        assertEquals(UseItemRunPreflight.MEMENTO_MESSAGE, result.exceptionOrNull()?.message)
        assertFalse(requested)
    }

    @Test
    fun organWarnings_matchDesktopConfirms() {
        assertEquals(
            "A spice melangeclears 3 stomach and liver and you have not filled that yet.  Are you sure you want to use it?",
            UseItemRunPreflight.organWarning(3433, "spice melange", true, true, 0, 0, 0),
        )
        assertEquals(
            "A spice melangeclears 3 stomach and you have not filled that yet.  Are you sure you want to use it?",
            UseItemRunPreflight.organWarning(3433, "spice melange", true, true, 0, 4, 0),
        )
        assertNull(UseItemRunPreflight.organWarning(3433, "spice melange", true, true, 3, 3, 0))
        assertNull(UseItemRunPreflight.organWarning(3433, "spice melange", false, false, 0, 0, 0))
        assertNull(UseItemRunPreflight.organWarning(9269, "fancy chocolate sculpture", true, true, 0, 0, 2))
        assertTrue(
            UseItemRunPreflight.organWarning(9269, "fancy chocolate sculpture", true, true, 0, 0, 3)
                ?.contains("wasted after using 3") == true,
        )
        assertTrue(UseItemRunPreflight.organWarning(9429, "milk", true, true, 2, 0, 0)?.contains("stomach") == true)
        assertNull(UseItemRunPreflight.organWarning(9429, "milk", true, true, 3, 0, 0))
        assertTrue(UseItemRunPreflight.organWarning(9421, "pod", true, true, 0, 1, 0)?.contains("liver") == true)
        assertNull(UseItemRunPreflight.organWarning(9421, "pod", true, true, 0, 3, 0))
    }

    @Test
    fun use_unfilledSpice_stopsWithoutRequest() = runTest {
        var requested = false
        val http = client {
            requested = true
            respond("ok", HttpStatusCode.OK)
        }
        val result = UseItemRequest(http).use(UseItemRunPreflight.SPICE_MELANGE)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("stomach and liver") == true)
        assertFalse(requested)
    }

    @Test
    fun use_filledSpice_weapon_sphere_curse_andUnusable() = runTest {
        GameDatabase().load()
        val paths = mutableListOf<String>()
        val http = client { request ->
            val body = request.body.toByteArray().decodeToString()
            paths += request.url.fullPath + " " + body
            respond(
                "<html>The pieces of the device rise</html>",
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "text/html"),
            )
        }
        val character = KoLCharacter()
        character.setLevel(10)
        character.updateConsumables(fullness = 5, inebriety = 5, spleenUsed = 0)
        val request = UseItemRequest(http, character = character, preferences = Preferences(MapSettings()))

        paths.clear()
        assertTrue(request.use(UseItemRunPreflight.SPICE_MELANGE).isSuccess)
        assertTrue(paths.any { it.contains("inv_use.php") && it.contains("whichitem=3433") }, paths.toString())

        paths.clear()
        assertTrue(request.use(1).isSuccess, "weapon")
        assertTrue(paths.any { it.contains("inv_equip.php") && it.contains("whichitem=1") }, paths.toString())
        assertFalse(paths.any { it.contains("inv_use.php") })

        paths.clear()
        assertTrue(request.use(UseItemRunPreflight.POWER_SPHERE).isSuccess)
        assertTrue(paths.any { it.contains("campground.php") && it.contains("powerelvibratoportal") }, paths.toString())

        paths.clear()
        assertTrue(request.use(625).isSuccess, "curse")
        assertTrue(paths.any { it.contains("curse.php") && it.contains("whichitem=625") }, paths.toString())

        paths.clear()
        val paste = request.use(25)
        assertTrue(paste.isFailure)
        assertEquals("meat paste is unusable.", paste.exceptionOrNull()?.message)
        assertTrue(paths.isEmpty())
    }

    @Test
    fun use_sealWithClub_reachesInvUse() = runTest {
        GameDatabase().load()
        val paths = mutableListOf<String>()
        val http = client { request ->
            paths += request.url.fullPath
            respond(
                "<html>You use the item.</html>",
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "text/html"),
            )
        }
        val character = KoLCharacter()
        character.updateEquipment(EquipmentSlot.WEAPON, "seal-clubbing club")
        val result = UseItemRequest(http, character = character).use(3902)
        assertTrue(result.isSuccess, result.exceptionOrNull()?.message)
        assertTrue(paths.any { it.contains("inv_use.php") && it.contains("whichitem=3902") })
        assertTrue(paths.any { it.contains("checked=1") }, paths.toString())
        assertEquals(2, paths.count { it.contains("inv_use.php") })
    }

    @Test
    fun consumeBatches_beansPogsAndSingleUse() {
        assertEquals(listOf(5, 20), UseItemRunPreflight.consumeBatches(905, 25))
        assertEquals(listOf(1, 11), UseItemRunPreflight.consumeBatches(5505, 12))
        assertEquals(listOf(1), UseItemRunPreflight.consumeBatches(2, 1))
        assertEquals(listOf(1, 1, 1), UseItemRunPreflight.consumeBatches(2, 3))
    }

    @Test
    fun planConsume_levelAndInteractRefuse() = runTest {
        GameDatabase().load()
        val low = UseItemRunPreflight.planConsume(
            itemId = 4532,
            itemName = "bishop cookie",
            quantity = 1,
            character = net.sourceforge.kolmafia.character.CharacterState(level = 1),
            preferences = null,
        )
        assertIs<UseItemRunPreflight.ConsumePlan.Refuse>(low)
        assertEquals("Insufficient level to consume bishop cookie", low.message)

        val interact = UseItemRunPreflight.planConsume(
            itemId = 4532,
            itemName = "bishop cookie",
            quantity = 1,
            character = net.sourceforge.kolmafia.character.CharacterState(level = 13, canInteract = false),
            preferences = null,
        )
        assertIs<UseItemRunPreflight.ConsumePlan.Refuse>(interact)
    }

    @Test
    fun use_levelFail_sendsNoRequest() = runTest {
        GameDatabase().load()
        var requested = false
        val http = client {
            requested = true
            respond("ok", HttpStatusCode.OK)
        }
        val character = KoLCharacter()
        character.setLevel(1)
        val result = UseItemRequest(http, character = character).use(4532)
        assertTrue(result.isFailure)
        assertEquals("Insufficient level to consume bishop cookie", result.exceptionOrNull()?.message)
        assertFalse(requested)
    }

    @Test
    fun use_level13WithoutInteract_sendsNoRequest() = runTest {
        GameDatabase().load()
        var requested = false
        val http = client {
            requested = true
            respond("ok", HttpStatusCode.OK)
        }
        val character = KoLCharacter()
        character.setLevel(13)
        character.setCanInteract(false)
        val result = UseItemRequest(http, character = character).use(4532)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Insufficient level") == true)
        assertFalse(requested)
    }

    @Test
    fun use_dailyCap_clampsQuantity() = runTest {
        GameDatabase().load()
        val paths = mutableListOf<String>()
        val http = client { request ->
            paths += request.url.fullPath
            respond(
                "<html>You use the item.</html>",
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "text/html"),
            )
        }
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("_photocopyUsed", false)
        val result = UseItemRequest(http, preferences = prefs, character = KoLCharacter()).use(4864, 5)
        assertTrue(result.isSuccess, result.exceptionOrNull()?.message)
        assertEquals(1, paths.count { it.contains("inv_use.php") })
        assertFalse(paths.any { it.contains("quantity=") }, paths.toString())
        assertEquals("Finished using 1 portable photocopier.", UseItemRunPreflight.lastUpdate)
    }

    @Test
    fun use_beans_batchesFiveThenTwenty() = runTest {
        GameDatabase().load()
        val quantities = mutableListOf<Int?>()
        val http = client { request ->
            quantities += request.url.parameters["quantity"]?.toIntOrNull()
            respond(
                "<html>You use the item.</html>",
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "text/html"),
            )
        }
        val result = UseItemRequest(http, character = KoLCharacter()).use(905, 25)
        assertTrue(result.isSuccess, result.exceptionOrNull()?.message)
        assertEquals(listOf<Int?>(5, 20), quantities)
        assertTrue(UseItemRunPreflight.lastUpdate.startsWith("Finished using 25"))
    }

    @Test
    fun use_pogs_batchesOneThenEleven() = runTest {
        GameDatabase().load()
        val quantities = mutableListOf<Int?>()
        val http = client { request ->
            quantities += request.url.parameters["quantity"]?.toIntOrNull()
            respond(
                "<html>You use the item.</html>",
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "text/html"),
            )
        }
        val result = UseItemRequest(http, character = KoLCharacter()).use(5505, 12)
        assertTrue(result.isSuccess, result.exceptionOrNull()?.message)
        assertEquals(listOf<Int?>(null, 11), quantities)
        assertTrue(UseItemRunPreflight.lastUpdate.startsWith("Finished using 12"))
    }

    @Test
    fun use_phial_removesConflictingFormBeforeUse() = runTest {
        GameDatabase().load()
        val paths = mutableListOf<String>()
        val http = client { request ->
            paths += request.url.fullPath
            respond(
                "<html>Effect removed. You use the item.</html>",
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "text/html"),
            )
        }
        val effects = net.sourceforge.kolmafia.effect.EffectManager(http, GameEventBus())
        effects.applyEffectsFromCharpane(
            """
            <br>Lvl. 10
            <img alt="Coldform" onClick='eff("a3c8719b97cd5b55d8004e16f83995e8",1);'><td>(10)
            """.trimIndent(),
        )
        assertTrue(effects.state.value.effects.any { it.name.equals("Coldform", ignoreCase = true) })
        val result = UseItemRequest(http, character = KoLCharacter(), effectManager = effects)
            .use(UseItemRunPreflight.PHIAL_OF_HOTNESS)
        assertTrue(result.isSuccess, result.exceptionOrNull()?.message)
        val uneffectIdx = paths.indexOfFirst { it.contains("uneffect.php") || it.contains("charsheet.php") }
        val useIdx = paths.indexOfFirst { it.contains("inv_use.php") && it.contains("whichitem=6556") }
        assertTrue(uneffectIdx >= 0, paths.toString())
        assertTrue(useIdx > uneffectIdx, paths.toString())
    }

    @Test
    fun luciferAndCardAndCheckedHelpers() {
        assertEquals(10L, UseItemRunPreflight.luciferMinimumMp(maxMp = 100, currentHp = 11))
        assertEquals(100L, UseItemRunPreflight.luciferMinimumMp(maxMp = 100, currentHp = 1))
        assertTrue(UseItemRunPreflight.needsAnswerPlz(UseItemRunPreflight.WHAT_CARD))
        assertTrue(UseItemRunPreflight.needsAnswerPlz(UseItemRunPreflight.WHERE_CARD))
        assertFalse(UseItemRunPreflight.needsAnswerPlz(2))
        assertTrue(UseItemRunPreflight.needsCheckedFollowUp(3902))
        assertTrue(UseItemRunPreflight.needsCheckedFollowUp(4474))
        assertTrue(UseItemRunPreflight.isBrickoMonster(4484))
        assertFalse(UseItemRunPreflight.needsCheckedFollowUp(2))
        assertTrue(
            UseItemRunPreflight.hasEquipped(
                mapOf(EquipmentSlot.ACC1 to "support cummerbund"),
                "support cummerbund",
            ),
        )
        assertFalse(
            UseItemRunPreflight.hasEquipped(
                mapOf(EquipmentSlot.ACC1 to "other"),
                "support cummerbund",
            ),
        )
    }

    @Test
    fun use_triviaCard_sendsAnswerPlz() = runTest {
        GameDatabase().load()
        val paths = mutableListOf<String>()
        val http = client { request ->
            paths += request.url.fullPath
            respond(
                "<html>You use the item.</html>",
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "text/html"),
            )
        }
        val result = UseItemRequest(http, character = KoLCharacter())
            .use(UseItemRunPreflight.WHAT_CARD)
        assertTrue(result.isSuccess, result.exceptionOrNull()?.message)
        assertTrue(
            paths.any { it.contains("inv_use.php") && it.contains("whichitem=5511") && it.contains("answerplz=1") },
            paths.toString(),
        )
    }

    @Test
    fun use_brickoMonster_sendsCheckedFollowUp() = runTest {
        GameDatabase().load()
        val paths = mutableListOf<String>()
        val http = client { request ->
            paths += request.url.fullPath
            respond(
                "<html>You use the item.</html>",
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "text/html"),
            )
        }
        val result = UseItemRequest(http, character = KoLCharacter()).use(4474)
        assertTrue(result.isSuccess, result.exceptionOrNull()?.message)
        assertEquals(2, paths.count { it.contains("inv_use.php") && it.contains("whichitem=4474") })
        assertTrue(paths.any { it.contains("checked=1") }, paths.toString())
        assertTrue(paths.indexOfFirst { it.contains("checked=1") } > 0, paths.toString())
    }

    @Test
    fun use_mafiaAria_equipsCummerbundBeforeUse() = runTest {
        GameDatabase().load()
        val paths = mutableListOf<String>()
        val http = client { request ->
            val body = request.body.toByteArray().decodeToString()
            paths += request.url.fullPath + " " + body
            respond(
                "<html>You use the item.</html>",
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "text/html"),
            )
        }
        val character = KoLCharacter()
        val result = UseItemRequest(http, character = character).use(UseItemRunPreflight.MAFIA_ARIA)
        assertTrue(result.isSuccess, result.exceptionOrNull()?.message)
        val equipIdx = paths.indexOfFirst {
            it.contains("inv_equip.php") && it.contains("whichitem=778")
        }
        val useIdx = paths.indexOfFirst {
            it.contains("inv_use.php") && it.contains("whichitem=781")
        }
        assertTrue(equipIdx >= 0, paths.toString())
        assertTrue(useIdx > equipIdx, paths.toString())
    }

    @Test
    fun use_lucifer_reachesInvUseAfterBurnAttempt() = runTest {
        GameDatabase().load()
        val paths = mutableListOf<String>()
        val http = client { request ->
            paths += request.url.fullPath
            respond(
                "<html>You use the item.</html>",
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "text/html"),
            )
        }
        val character = KoLCharacter()
        character.setLevel(5)
        character.updateHpMp(currentHp = 11, maxHp = 100, currentMp = 200, maxMp = 200)
        val prefs = Preferences(MapSettings())
        val skills = net.sourceforge.kolmafia.skill.SkillManager(
            http,
            net.sourceforge.kolmafia.skill.SkillCastRequest(http),
            GameEventBus(),
            prefs,
        )
        val burner = net.sourceforge.kolmafia.mood.ManaBurnManager(skills, prefs)
        val result = UseItemRequest(
            http,
            character = character,
            preferences = prefs,
            manaBurnManager = burner,
            skillManager = skills,
        ).use(UseItemRunPreflight.JUMBO_DR_LUCIFER)
        assertTrue(result.isSuccess, result.exceptionOrNull()?.message)
        assertTrue(
            paths.any { it.contains("inv_use.php") && it.contains("whichitem=571") },
            paths.toString(),
        )
        assertEquals(110L, UseItemRunPreflight.luciferMinimumMp(200, 11))
    }

    @Test
    fun retrieveAndConfirmHelpers() {
        assertEquals("Insufficient items to use.", UseItemRunPreflight.INSUFFICIENT_ITEMS)
        assertTrue(UseItemRunPreflight.shouldRetrieveBeforeUse(UseItemRunPreflight.WHAT_CARD))
        assertFalse(
            UseItemRunPreflight.needsConfirmFormField(
                itemId = 3345,
                currentBedId = 0,
                currentDwellingId = UseItemRunPreflight.BIG_ROCK,
            ),
        )
        assertTrue(
            UseItemRunPreflight.needsConfirmFormField(
                itemId = 3345,
                currentBedId = 429,
                currentDwellingId = UseItemRunPreflight.BIG_ROCK,
            ),
        )
        assertFalse(
            UseItemRunPreflight.needsConfirmFormField(
                itemId = 69,
                currentBedId = 0,
                currentDwellingId = UseItemRunPreflight.BIG_ROCK,
            ),
        )
        assertTrue(
            UseItemRunPreflight.needsConfirmFormField(
                itemId = 69,
                currentBedId = 0,
                currentDwellingId = 526,
            ),
        )
    }

    @Test
    fun use_retrieveFailure_refusesWithInsufficientItems() = runTest {
        GameDatabase().load()
        var requested = false
        val http = client {
            requested = true
            respond("ok", HttpStatusCode.OK)
        }
        val retrieve = object : RetrieveItemService(
            null, null, null, null, null, null, null, null, null, null, null,
        ) {
            override suspend fun retrieve(itemId: Int, qty: Int): Int = 0
        }
        val result = UseItemRequest(
            http,
            character = KoLCharacter(),
            retrieveItemServiceProvider = { retrieve },
        ).use(UseItemRunPreflight.WHAT_CARD)
        assertTrue(result.isFailure)
        assertEquals(UseItemRunPreflight.INSUFFICIENT_ITEMS, result.exceptionOrNull()?.message)
        assertEquals(UseItemRunPreflight.INSUFFICIENT_ITEMS, UseItemRunPreflight.lastUpdate)
        assertFalse(requested)
    }

    @Test
    fun use_retrieveSuccess_reachesInvUse() = runTest {
        GameDatabase().load()
        val retrieved = mutableListOf<Pair<Int, Int>>()
        val paths = mutableListOf<String>()
        val http = client { request ->
            paths += request.url.fullPath
            respond(
                "<html>You use the item.</html>",
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "text/html"),
            )
        }
        val retrieve = object : RetrieveItemService(
            null, null, null, null, null, null, null, null, null, null, null,
        ) {
            override suspend fun retrieve(itemId: Int, qty: Int): Int {
                retrieved += itemId to qty
                return qty
            }
        }
        val result = UseItemRequest(
            http,
            character = KoLCharacter(),
            retrieveItemServiceProvider = { retrieve },
        ).use(UseItemRunPreflight.WHAT_CARD)
        assertTrue(result.isSuccess, result.exceptionOrNull()?.message)
        assertEquals(listOf(UseItemRunPreflight.WHAT_CARD to 1), retrieved)
        assertTrue(
            paths.any {
                it.contains("inv_use.php") &&
                    it.contains("whichitem=${UseItemRunPreflight.WHAT_CARD}")
            },
        )
    }

    @Test
    fun use_beddingReplacement_sendsConfirmTrue() = runTest {
        GameDatabase().load()
        val paths = mutableListOf<String>()
        val http = client { request ->
            paths += request.url.fullPath
            respond(
                "<html>You use the item.</html>",
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "text/html"),
            )
        }
        val previousBed = CampgroundItemSync.currentBedItemId
        try {
            CampgroundItemSync.currentBedItemId = 429
            val result = UseItemRequest(http, character = KoLCharacter()).use(3345)
            assertTrue(result.isSuccess, result.exceptionOrNull()?.message)
            assertTrue(
                paths.any {
                    it.contains("inv_use.php") &&
                        it.contains("whichitem=3345") &&
                        it.contains("confirm=true")
                },
                paths.toString(),
            )
        } finally {
            CampgroundItemSync.currentBedItemId = previousBed
        }
    }

    @Test
    fun use_dwellingReplacement_sendsConfirmTrue() = runTest {
        GameDatabase().load()
        val paths = mutableListOf<String>()
        val http = client { request ->
            paths += request.url.fullPath
            respond(
                "<html>You use the item.</html>",
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "text/html"),
            )
        }
        val prefs = Preferences(MapSettings())
        prefs.setInt(DwellingSync.CURRENT_DWELLING_ITEM_ID_PREF, 526)
        val result = UseItemRequest(http, preferences = prefs, character = KoLCharacter()).use(69)
        assertTrue(result.isSuccess, result.exceptionOrNull()?.message)
        assertTrue(
            paths.any {
                it.contains("inv_use.php") &&
                    it.contains("whichitem=69") &&
                    it.contains("confirm=true")
            },
            paths.toString(),
        )
    }
}
