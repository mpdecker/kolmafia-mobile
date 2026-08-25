package net.sourceforge.kolmafia.combat

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences

class DiscoCombatHelperTest {
    private lateinit var prefs: Preferences

    @BeforeTest
    fun setUp() {
        prefs = Preferences(MapSettings())
        DiscoCombatHelper.resetForTest()
        prefs.setInt("skillLevel50", 1)
        prefs.setInt("skillLevel51", 1)
        prefs.setInt("skillLevel52", 1)
    }

    @AfterTest
    fun tearDown() {
        DiscoCombatHelper.resetForTest()
    }

    @Test
    fun initializeLoadsPrefsAndDisambiguates() {
        prefs.setString("raveCombo5", "Break It On Down,Pop and Lock It,Run Like the Wind")
        DiscoCombatHelper.initialize(
            isDiscoBandit = true,
            preferences = prefs,
            hasSkill = { true },
        )
        assertTrue(DiscoCombatHelper.canCombo)
        assertEquals("Rave Steal", DiscoCombatHelper.disambiguateCombo("steal"))
        val combo = DiscoCombatHelper.getCombo("Rave Steal")
        assertNotNull(combo)
        assertEquals(listOf(50, 51, 52), combo.toList())
    }

    @Test
    fun invalidComboDisambiguateNull() {
        DiscoCombatHelper.initialize(true, prefs, hasSkill = { true })
        assertNull(DiscoCombatHelper.disambiguateCombo("not-a-combo"))
    }

    @Test
    fun canRaveStealRespectsCapAndVolcano() {
        DiscoCombatHelper.initialize(true, prefs, hasSkill = { true }) {
            "Breakdancing Raver"
        }
        prefs.setInt("_raveStealCount", 30)
        assertTrue(DiscoCombatHelper.canRaveSteal())
        DiscoCombatHelper.initialize(true, prefs, hasSkill = { true }) {
            "some goblin"
        }
        assertFalse(DiscoCombatHelper.canRaveSteal("some goblin"))
        prefs.setInt("_raveStealCount", 5)
        assertTrue(DiscoCombatHelper.canRaveSteal("some goblin"))
    }

    @Test
    fun learnRaveComboFromFightHtml() {
        DiscoCombatHelper.initialize(true, prefs, hasSkill = { true })
        val html = "Your savage beatdown seems to have knocked loose some treasure. Sweet!"
        DiscoCombatHelper.parseFightRound("skill50", html)
        DiscoCombatHelper.parseFightRound("skill51", html)
        DiscoCombatHelper.parseFightRound("skill52", html)
        assertEquals(
            "Break It On Down,Pop and Lock It,Run Like the Wind",
            prefs.getString("raveCombo5", ""),
        )
        assertTrue(DiscoCombatHelper.knownCombo[DiscoCombatHelper.RAVE_STEAL])
    }

    @Test
    fun randomRaveDeduceWhenOneUnknown() {
        DiscoCombatHelper.initialize(true, prefs, hasSkill = { true })
        // Seed five known combos; last permutation is deduced as Rave Substats
        val perms = listOf(
            Triple(0, 1, 2),
            Triple(0, 2, 1),
            Triple(1, 0, 2),
            Triple(1, 2, 0),
            Triple(2, 0, 1),
        )
        for ((i, p) in perms.withIndex()) {
            DiscoCombatHelper.learnRaveComboForTest(i, p.first, p.second, p.third)
        }
        assertTrue(DiscoCombatHelper.knownCombo[DiscoCombatHelper.RAVE_SUBSTATS])
        assertEquals(
            "Run Like the Wind,Pop and Lock It,Break It On Down",
            prefs.getString("raveCombo6", ""),
        )
    }

    @Test
    fun nemesisResetClearsCombos() {
        prefs.setString("raveCombo1", "Break It On Down,Pop and Lock It,Run Like the Wind")
        prefs.setInt("lastNemesisReset", 0)
        DiscoCombatHelper.initialize(true, prefs, hasSkill = { true })
        assertTrue(DiscoCombatHelper.ensureUpdatedNemesisStatus(prefs, 1))
        assertEquals("", prefs.getString("raveCombo1", "x"))
        assertEquals(1, prefs.getInt("lastNemesisReset", -1))
    }

    @Test
    fun macrofierExpandsCombo() {
        prefs.setString("raveCombo5", "Break It On Down,Pop and Lock It,Run Like the Wind")
        DiscoCombatHelper.initialize(true, prefs, hasSkill = { true })
        val text = Macrofier.expandAction("combo Rave Steal", prefs)
        assertTrue(text.contains("skill 50"), text)
        assertTrue(text.contains("skill 51"), text)
        assertTrue(text.contains("skill 52"), text)
        assertTrue(text.contains("call mafiaround"), text)
    }

    @Test
    fun combatActionManagerDisambiguatesCombo() {
        DiscoCombatHelper.initialize(true, prefs, hasSkill = { true })
        assertEquals(
            "combo Rave Steal",
            CombatActionManager.getLongCombatOptionName("combo steal"),
        )
        assertEquals("skip", CombatActionManager.getShortCombatOptionName("combo nope"))
    }
}
