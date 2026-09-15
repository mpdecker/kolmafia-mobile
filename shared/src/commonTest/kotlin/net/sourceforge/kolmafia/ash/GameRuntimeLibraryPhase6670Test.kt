package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.adventure.AdventurePrep
import net.sourceforge.kolmafia.adventure.choice.ChoiceWalkAway
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.combat.CombatActionManager
import net.sourceforge.kolmafia.combat.Macrofier
import net.sourceforge.kolmafia.data.AdventureZone
import net.sourceforge.kolmafia.data.ZoneParentDatabase
import net.sourceforge.kolmafia.maximizer.MaximizerContinuation
import net.sourceforge.kolmafia.platform.UserDataFilePaths
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.session.ChoiceCombatAshState

/**
 * Focused XLVI Track C coverage (phases 6651–6670).
 * Revision bump deferred to parent (phase6850); stays phase6850.
 */
class GameRuntimeLibraryPhase6670Test {

    private lateinit var tempDir: File

    @BeforeTest
    fun setUp() {
        ChoiceCombatAshState.reset()
        CombatActionManager.resetForTest()
        Macrofier.resetForTest()
        AdventurePrep.resetForTest()
        MaximizerContinuation.forceContinue()
        tempDir = File(System.getProperty("java.io.tmpdir"), "kolmafia-xlvic-${System.nanoTime()}")
        tempDir.mkdirs()
        UserDataFilePaths.testBasePath = tempDir.absolutePath
        runBlocking { ZoneParentDatabase.load() }
    }

    @AfterTest
    fun tearDown() {
        ChoiceCombatAshState.reset()
        CombatActionManager.resetForTest()
        Macrofier.resetForTest()
        AdventurePrep.resetForTest()
        MaximizerContinuation.forceContinue()
        UserDataFilePaths.testBasePath = null
        tempDir.deleteRecursively()
    }

    @Test
    fun revisionStaysPhase6610() {
        assertEquals("phase7210", GameRuntimeLibrary.REVISION)
        assertEquals("7210", outputLib(GameRuntimeLibrary(), "print(get_revision());").trim())
    }

    @Test
    fun canAdventure_barroomBrawlNeedsRatQuest() {
        val cs = CharacterState(adventuresLeft = 10, level = 10)
        val prefs = Preferences(MapSettings())
        val barroom = AdventureZone(
            zoneName = "Town",
            urlParams = "adventure=23",
            locationName = "The Barroom Brawl",
            environment = "indoor",
            diffLevel = "low",
            statRequirement = 0,
            goals = emptyList(),
            isOverdrunk = false,
            noWander = false,
        )
        assertFalse(
            AdventurePrep.canAdventureAt("The Barroom Brawl", cs, barroom, prefs),
        )
        prefs.setString(Quest.RAT.prefKey, QuestDatabase.STARTED)
        assertTrue(
            AdventurePrep.canAdventureAt("The Barroom Brawl", cs, barroom, prefs),
        )
    }

    @Test
    fun preValidate_noneIsFalse() {
        assertEquals(
            "false",
            outputLib(
                GameRuntimeLibrary(),
                """print(to_string(pre_validate_adventure(to_location("none"))));""",
            ).trim(),
        )
    }

    @Test
    fun ccs_directoryRescanOnSetAndWriteReload() {
        val p = prefs()
        val body = "[ default ]\nskill saucegeyser\n"
        // Write without going through availableLookups cache first
        assertTrue(CombatActionManager.writeCcs("DiskScript", body, p))
        CombatActionManager.resetForTest()
        // Disk-only script should be discoverable after getAvailableLookups rescan
        assertTrue(
            outputLib(GameRuntimeLibrary(preferences = p), """print(set_ccs("diskscript"));""")
                .trim().equals("true", ignoreCase = true),
        )
        assertEquals("DiskScript", p.getString("customCombatScript", ""))
        val updated = "[ default ]\nattack with weapon\n"
        assertTrue(CombatActionManager.writeCcs("DiskScript", updated, p))
        assertEquals(
            "attack with weapon",
            CombatActionManager.getCombatAction("default", 0, allowMacro = true, p),
        )
        assertTrue(CombatActionManager.getAvailableLookups().any { it.equals("DiskScript", true) })
    }

    @Test
    fun get_ccs_action_and_run_choice_minusOneOnly() {
        val p = prefs()
        CombatActionManager.loadFromText(
            """
            [ default ]
            skill saucegeyser
            attack with weapon
            """.trimIndent(),
            name = "xlvic",
            preferences = p,
        )
        p.setString("battleAction", "custom combat script")
        val lib = GameRuntimeLibrary(preferences = p)
        assertEquals("skill saucegeyser", outputLib(lib, "print(get_ccs_action(0));").trim())

        ChoiceCombatAshState.reset()
        val html =
            """<form><input type=hidden name=whichchoice value=1076>
               <input type=submit name=option value=1>Keep it</form>"""
        ChoiceCombatAshState.noteChoiceVisit(1076, html)
        // option < -1 must not trigger goal pick — returns last response text
        val out = outputLib(lib, "print(run_choice(-5));")
        assertTrue(out.contains("Keep it") || out.contains(html.trim()), "run_choice(-5): $out")
        assertTrue(ChoiceCombatAshState.handlingChoice)
        assertEquals(1076, ChoiceCombatAshState.lastChoice)
    }

    @Test
    fun can_walk_from_choice_syncsOnVisit() {
        ChoiceCombatAshState.reset()
        assertEquals(
            "true",
            outputLib(GameRuntimeLibrary(), "print(to_string(can_walk_from_choice()));").trim(),
        )
        // Non-walkable choice id
        ChoiceCombatAshState.noteChoiceVisit(1, "locked")
        assertFalse(ChoiceCombatAshState.canWalkAway)
        assertEquals(
            "false",
            outputLib(GameRuntimeLibrary(), "print(to_string(can_walk_from_choice()));").trim(),
        )
        ChoiceCombatAshState.noteChoiceVisit(1601, "walkable")
        assertTrue(ChoiceWalkAway.canWalkFromChoice(1601))
        assertTrue(ChoiceCombatAshState.canWalkAway)
        assertEquals(
            "true",
            outputLib(GameRuntimeLibrary(), "print(to_string(can_walk_from_choice()));").trim(),
        )
    }

    @Test
    fun available_choice_options_spoilersAndEmpty() {
        ChoiceCombatAshState.reset()
        assertEquals(
            "0",
            outputLib(GameRuntimeLibrary(), "print(count(available_choice_options()));").trim(),
        )
        ChoiceCombatAshState.noteChoiceVisit(
            1076,
            """<form><input type=hidden name=whichchoice value=1076>
               <input class=button type=submit name=option value=1 title="Mayo">Keep it</form>""",
        )
        val plain = outputLib(GameRuntimeLibrary(), "print(available_choice_options()[1]);").trim()
        assertTrue(plain.contains("Keep it"), plain)
        val spoiled = outputLib(
            GameRuntimeLibrary(),
            "print(available_choice_options(true)[1]);",
        ).trim()
        assertTrue(spoiled.contains("Keep it"), spoiled)
    }

    @Test
    fun auto_attack_syncsCharacterAndPref() {
        val p = prefs()
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(adventures = "10"))
        }
        val lib = GameRuntimeLibrary(preferences = p, character = char)
        outputLib(lib, """set_auto_attack(1);""")
        assertEquals(1, char.state.value.autoAttackAction)
        assertEquals(1, p.getInt("defaultAutoAttack", 0))
        assertEquals("1", outputLib(lib, "print(get_auto_attack());").trim())
        outputLib(lib, """set_auto_attack("none");""")
        assertEquals(0, char.state.value.autoAttackAction)
        assertEquals(0, p.getInt("defaultAutoAttack", -1))
        assertEquals("0", outputLib(lib, "print(get_auto_attack());").trim())
    }

    @Test
    fun adventure_zeroTurnsContinueValue() {
        val lib = GameRuntimeLibrary()
        assertEquals(
            "true",
            outputLib(lib, """print(to_string(adventure(0, to_location("none"))));""")
                .trim(),
        )
        MaximizerContinuation.abort()
        assertEquals(
            "false",
            outputLib(lib, """print(to_string(adv1(to_location("none"))));""")
                .trim(),
        )
        MaximizerContinuation.forceContinue()
    }

    private fun prefs(): Preferences = Preferences(MapSettings())
}
