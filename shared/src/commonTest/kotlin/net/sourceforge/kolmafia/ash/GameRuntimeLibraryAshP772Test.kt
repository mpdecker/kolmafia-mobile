package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.YouRobotChoiceSync

class GameRuntimeLibraryAshP772Test {

    @Test
    fun revision_phase826() {
        assertEquals("phase826", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun visit_parsesAvatarParts() {
        val prefs = Preferences(MapSettings())
        val html = """
            <img src="otherimages/robot/body3.png">
            <img src="otherimages/robot/left2.png">
            <img src="otherimages/robot/right5.png">
            <img src="otherimages/robot/top1.png">
            <img src="otherimages/robot/bottom4.png">
        """.trimIndent()
        assertTrue(YouRobotChoiceSync.applyVisit(1445, html, prefs))
        assertEquals(3, prefs.getInt("youRobotBody", 0))
        assertEquals(2, prefs.getInt("youRobotLeft", 0))
        assertEquals(5, prefs.getInt("youRobotRight", 0))
        assertEquals(1, prefs.getInt("youRobotTop", 0))
        assertEquals(4, prefs.getInt("youRobotBottom", 0))
    }

    @Test
    fun visit_parsesCpuAndStatbot() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            YouRobotChoiceSync.applyVisit(
                1445,
                """<button value="topology_grid" foo (already installed)
                   <button value="biomass_processing" bar (already installed)""",
                prefs,
                "choice.php?whichchoice=1445&show=cpus",
            ),
        )
        assertEquals("biomass_processing,topology_grid", prefs.getString("youRobotCPUUpgrades", ""))

        assertTrue(
            YouRobotChoiceSync.applyVisit(
                1447,
                "Current upgrade cost: <b>25 energy</b>",
                prefs,
            ),
        )
        assertEquals(15, prefs.getInt("statbotUses", 0))
    }

    @Test
    fun post_installsPartFromUrl() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            YouRobotChoiceSync.apply(
                choiceId = 1445,
                html = """<img src="otherimages/robot/left7.png">""",
                preferences = prefs,
                choiceUrl = "show=left&p=7",
            ),
        )
        assertEquals(7, prefs.getInt("youRobotLeft", 0))
    }

    @Test
    fun questChoiceRules_wires1445() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1445,
                responseText = """<img src="otherimages/robot/body1.png">""",
                questDatabase = QuestDatabase(prefs),
                decision = 1,
                preferences = prefs,
                choiceUrl = "show=top&p=2",
            ),
        )
        assertEquals(2, prefs.getInt("youRobotTop", 0))
        assertEquals(1, prefs.getInt("youRobotBody", 0))
    }
}
