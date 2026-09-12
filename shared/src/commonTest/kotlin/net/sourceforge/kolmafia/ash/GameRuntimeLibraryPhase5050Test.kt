package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.AdventureQueueDatabase
import net.sourceforge.kolmafia.request.ChefStaffRequestHub
import net.sourceforge.kolmafia.request.Crimbo11Request
import net.sourceforge.kolmafia.request.Crimbo14Request
import net.sourceforge.kolmafia.request.SpacegateEquipmentRequestHub
import net.sourceforge.kolmafia.request.StillRequestHub
import net.sourceforge.kolmafia.request.SugarSheetRequestHub

class GameRuntimeLibraryPhase5050Test {

    @Test
    fun revision_phase5050() {
        assertEquals("phase6910", GameRuntimeLibrary.REVISION)
        assertEquals("6850", outputLib(GameRuntimeLibrary(), "print(get_revision());"))
    }

    @Test
    fun have_effect_none_isZero() {
        assertEquals(
            "0",
            outputLib(GameRuntimeLibrary(), """print(have_effect(to_effect("none")));"""),
        )
    }

    @Test
    fun appearance_rates_includeQueue_divergesWhenQueued() = kotlinx.coroutines.runBlocking {
        AdventureQueueDatabase.resetQueue()
        val db = net.sourceforge.kolmafia.data.GameDatabase()
        db.load()
        val loc = "The Spooky Forest"
        val monster = "spooky vampire"
        val lib = GameRuntimeLibrary(gameDatabase = db)
        val before = outputLib(
            lib,
            """print(to_string(appearance_rates(to_location("$loc"), true)[to_monster("$monster")]));""",
        ).trim()
        AdventureQueueDatabase.enqueue(loc, monster)
        val after = outputLib(
            lib,
            """print(to_string(appearance_rates(to_location("$loc"), true)[to_monster("$monster")]));""",
        ).trim()
        val stateless = outputLib(
            lib,
            """print(to_string(appearance_rates(to_location("$loc"), false)[to_monster("$monster")]));""",
        ).trim()
        assertTrue(before.isNotBlank())
        assertEquals(stateless, before)
        assertNotEquals(after, before)
        AdventureQueueDatabase.resetQueue()
    }

    @Test
    fun adventureQueue_applyQueueEffects_inVsOut() {
        AdventureQueueDatabase.resetQueue()
        val loc = "Test Zone"
        AdventureQueueDatabase.enqueue(loc, "A")
        val weights = mapOf("a" to 1, "b" to 1)
        val inQ = AdventureQueueDatabase.applyQueueEffects(
            numerator = 50.0,
            monsterName = "A",
            locationName = loc,
            totalWeighting = 2,
            weightOf = { weights[it.lowercase()] ?: 0 },
        )
        val outQ = AdventureQueueDatabase.applyQueueEffects(
            numerator = 50.0,
            monsterName = "B",
            locationName = loc,
            totalWeighting = 2,
            weightOf = { weights[it.lowercase()] ?: 0 },
        )
        // in-queue 1/(4*2-3*1)=1/5 → 10; out-queue 4/5 → 40
        assertEquals(10.0, inQ, 0.001)
        assertEquals(40.0, outQ, 0.001)
        AdventureQueueDatabase.resetQueue()
    }

    @Test
    fun http_hubs_phase5050() {
        assertTrue(Crimbo11Request.registerRequest("crimbo11.php?place=town"))
        assertFalse(Crimbo11Request.registerRequest("adventure.php"))
        assertTrue(Crimbo14Request.registerRequest("shop.php?whichshop=crimbo14"))
        assertTrue(StillRequestHub.registerRequest("shop.php?whichshop=still"))
        assertFalse(StillRequestHub.registerRequest("shop.php?whichshop=other"))
        assertTrue(SugarSheetRequestHub.registerRequest("shop.php?whichshop=sugarsheets"))
        assertTrue(SpacegateEquipmentRequestHub.registerRequest("choice.php?whichchoice=1233&option=1"))
        assertTrue(ChefStaffRequestHub.registerRequest("guild.php?action=makestaff&whichitem=1"))
        assertFalse(ChefStaffRequestHub.registerRequest("guild.php?action=buyskill"))
    }

    @Test
    fun monster_factoids_available_oneArg() {
        val out = outputLib(
            GameRuntimeLibrary(),
            """print(monster_factoids_available(to_monster("none")));""",
        )
        assertEquals("0", out.trim())
    }
}
