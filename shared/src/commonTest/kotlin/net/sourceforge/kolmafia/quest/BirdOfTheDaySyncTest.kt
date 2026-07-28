package net.sourceforge.kolmafia.quest

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.EffectData
import net.sourceforge.kolmafia.data.EffectDatabase
import net.sourceforge.kolmafia.data.EffectQuality
import net.sourceforge.kolmafia.preferences.Preferences

class BirdOfTheDaySyncTest {

    @AfterTest
    fun tearDown() {
        EffectDatabase.resetForTest()
    }

    @Test
    fun parseSkillName_extractsBoldTitle() {
        val html = """<div><b>Seek out a Turkey</b><br><b>MP Cost:</b> 20</div>"""
        assertEquals("Seek out a Turkey", BirdOfTheDaySync.parseSkillName(html))
    }

    @Test
    fun parseSkillMpCost_extractsCost() {
        val html = """<b>Seek out a Bird</b><br><b>MP Cost:</b> 40"""
        assertEquals(40, BirdOfTheDaySync.parseSkillMpCost(html))
    }

    @Test
    fun applySeekBirdSkillDescription_setsBirdPrefsAndCastCount() {
        val prefs = Preferences(MapSettings())
        val html = """<b>Seek out a Turkey</b><br><b>MP Cost:</b> 20"""
        assertTrue(BirdOfTheDaySync.applySeekBirdSkillDescription(html, prefs))
        assertEquals("Turkey", prefs.getString("_birdOfTheDay", ""))
        assertTrue(prefs.getBoolean("_canSeekBirds", false))
        assertEquals(2, prefs.getInt("_birdsSoughtToday", 0))
    }

    @Test
    fun applySeekBirdSkillDescription_repeatUnlock_returnsFalse() {
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("_canSeekBirds", true)
        val html = """<b>Seek out a Turkey</b><br><b>MP Cost:</b> 20"""
        assertFalse(BirdOfTheDaySync.applySeekBirdSkillDescription(html, prefs))
    }

    @Test
    fun applySeekBirdSkillDescription_leavesBirdPrefWhenSkillLocked() {
        val prefs = Preferences(MapSettings())
        prefs.setString("_birdOfTheDay", "Yesterday's Bird")
        val html = """<b>Seek out Birds</b><br><b>MP Cost:</b> 5"""
        assertFalse(BirdOfTheDaySync.applySeekBirdSkillDescription(html, prefs))
        assertEquals("Yesterday's Bird", prefs.getString("_birdOfTheDay", ""))
    }

    @Test
    fun descVisitSkill_usesSelfTruePath() {
        assertEquals(
            "desc_skill.php?whichskill=7323&self=true",
            DynamicItemModifierSync.DescVisit.Skill(BirdOfTheDaySync.SEEK_OUT_A_BIRD_SKILL_ID).path,
        )
    }

    @Test
    fun checkBirdOfTheDay_schedulesSkillAndTwoEffectVisitsFromCloset() {
        EffectDatabase.registerForTest(
            EffectData(1, "Blessing of the Bird", "eff.gif", "bird-desc", EffectQuality.GOOD, emptySet()),
        )
        EffectDatabase.registerForTest(
            EffectData(
                2,
                "Blessing of your favorite Bird",
                "eff.gif",
                "fav-desc",
                EffectQuality.GOOD,
                emptySet(),
            ),
        )
        val context = DynamicItemModifierSync.CheckContext(
            inventoryItemIds = emptySet(),
            equippedItemNames = emptySet(),
            activeEffectNames = emptySet(),
            closetItemIds = setOf(10434),
        )
        val db = object : net.sourceforge.kolmafia.data.GameDatabase() {
            override fun item(name: String) =
                if (name.equals("Bird-a-Day calendar", ignoreCase = true)) {
                    net.sourceforge.kolmafia.data.ItemData(
                        id = 10434,
                        name = "Bird-a-Day calendar",
                        descId = "cal-desc",
                        image = "birdcal.gif",
                        primaryUse = net.sourceforge.kolmafia.data.ItemPrimaryUse.USABLE,
                        secondaryUses = emptySet(),
                        access = emptySet(),
                        autosellPrice = 0,
                        plural = null,
                    )
                } else {
                    null
                }
        }
        val visits = BirdOfTheDaySync.checkBirdOfTheDay(context, db)
        assertEquals(3, visits.size)
        assertEquals(
            DynamicItemModifierSync.DescVisit.Skill(7323),
            visits.filterIsInstance<DynamicItemModifierSync.DescVisit.Skill>().single(),
        )
        assertEquals(
            setOf("bird-desc", "fav-desc"),
            visits.filterIsInstance<DynamicItemModifierSync.DescVisit.Effect>().map { it.descId }.toSet(),
        )
    }
}
