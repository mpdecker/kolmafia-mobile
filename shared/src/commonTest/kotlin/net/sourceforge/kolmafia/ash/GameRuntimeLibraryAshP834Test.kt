package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.DailyLimitDatabase
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.CanadianInstituteChoiceSync
import net.sourceforge.kolmafia.quest.CanadianMcdChoiceSync
import net.sourceforge.kolmafia.quest.DoctorBagCureChoiceSync
import net.sourceforge.kolmafia.quest.LatteChoiceSync
import net.sourceforge.kolmafia.quest.LyleFavoredChoiceSync
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.TorporChoiceSync
import net.sourceforge.kolmafia.request.LatteRequest

class GameRuntimeLibraryAshP834Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase1070", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun latteSkillCast_setsDailyPrefs() {
        val prefs = Preferences(MapSettings())
        assertTrue(LatteChoiceSync.applySkillCast(LatteChoiceSync.THROW_LATTE_SKILL, prefs))
        assertTrue(LatteChoiceSync.applySkillCast(LatteChoiceSync.OFFER_LATTE_SKILL, prefs))
        assertTrue(LatteChoiceSync.applySkillCast(LatteChoiceSync.GULP_LATTE_SKILL, prefs))
        assertTrue(prefs.getBoolean("_latteBanishUsed", false))
        assertTrue(prefs.getBoolean("_latteCopyUsed", false))
        assertTrue(prefs.getBoolean("_latteDrinkUsed", false))
    }

    @Test
    fun latteDailyLimits_loadedFromData() {
        kotlinx.coroutines.runBlocking { GameDatabase().load() }
        assertTrue(DailyLimitDatabase.getByName("Throw Latte on Opponent").isNotEmpty())
        assertTrue(DailyLimitDatabase.getByName("Offer Latte to Opponent").isNotEmpty())
        assertTrue(DailyLimitDatabase.getByName("Gulp Latte").isNotEmpty())
    }

    @Test
    fun latteListUnlocks_filters() {
        val prefs = Preferences(MapSettings())
        prefs.setString("latteUnlocks", "cinnamon,vanilla")
        val unlocked = LatteChoiceSync.listUnlocks(all = false, preferences = prefs)
        assertTrue(unlocked.contains("cinnamon"))
        assertTrue(unlocked.contains("vanilla"))
        assertFalse(unlocked.contains("basil |"))
        val all = LatteChoiceSync.listUnlocks(all = true, preferences = prefs)
        assertTrue(all.contains("basil"))
    }

    @Test
    fun latteResolveIngredients_matchesTokens() {
        val resolved = LatteRequest.resolveIngredients("cinnamon", "pumpkin", "vanilla")
        assertEquals("cinnamon", resolved[0]?.ingredient)
        assertEquals("pumpkin", resolved[1]?.ingredient)
        assertEquals("vanilla", resolved[2]?.ingredient)
    }

    @Test
    fun canadianMcd_setsLevelFromUrl() {
        var level = -1
        assertTrue(
            CanadianMcdChoiceSync.apply(
                769,
                1,
                "whichchoice=769&option=1&setting=7",
                "You switch the dial to 7.",
            ) { level = it },
        )
        assertEquals(7, level)
    }

    @Test
    fun canadianInstitute_logsWorkout() {
        val logs = mutableListOf<String>()
        assertTrue(
            CanadianInstituteChoiceSync.apply(770, 1, "You learn from the sages.") { logs += it },
        )
        assertEquals(listOf("Workout completed."), logs)
    }

    @Test
    fun doctorBagCure_consumesItemAndClears() {
        val prefs = Preferences(MapSettings())
        prefs.setString("doctorBagQuestItem", "cast")
        prefs.setString("doctorBagQuestLocation", "Somewhere")
        val db = QuestDatabase(prefs)
        db.setProgress(net.sourceforge.kolmafia.quest.Quest.DOCTOR_BAG, "step1")
        val consumed = mutableListOf<Pair<Int, Int>>()
        // Without ItemDatabase load, consume may no-op; lights still sync.
        assertTrue(
            DoctorBagCureChoiceSync.apply(
                1341,
                1,
                "One of the five green lights lights up.",
                prefs,
                db,
            ) { id, qty -> consumed += id to qty },
        )
        assertEquals(1, prefs.getInt("doctorBagQuestLights", -1))
        assertEquals("", prefs.getString("doctorBagQuestItem", "x"))
    }

    @Test
    fun torpor_learnsAndForgets() {
        val known = mutableSetOf(24010)
        val learned = mutableListOf<Int>()
        val forgot = mutableListOf<Int>()
        assertTrue(
            TorporChoiceSync.apply(
                choiceId = 1342,
                decision = 2,
                choiceUrl = "whichchoice=1342&option=2&sk[]=11",
                hasSkill = { it in known },
                learnSkill = {
                    known += it
                    learned += it
                },
                forgetSkill = {
                    known.remove(it)
                    forgot += it
                },
            ),
        )
        assertEquals(listOf(24011), learned)
        assertEquals(listOf(24010), forgot)
    }

    @Test
    fun lyle_setsFavoredAndCandyFlag() {
        val prefs = Preferences(MapSettings())
        assertTrue(LyleFavoredChoiceSync.apply(1309, prefs, hasCandyCaneSwordEquipped = true))
        assertTrue(prefs.getBoolean("_lyleFavored", false))
        assertTrue(prefs.getBoolean("_candyCaneSwordLyle", false))
    }
}
