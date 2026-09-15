package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.banish.Banisher
import net.sourceforge.kolmafia.banish.BanishManager
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.combat.CombatActionManager
import net.sourceforge.kolmafia.combat.Macrofier
import net.sourceforge.kolmafia.combat.MonsterStatusTracker
import net.sourceforge.kolmafia.data.MonsterDefinition
import net.sourceforge.kolmafia.platform.UserDataFilePaths
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.StandardRequest
import net.sourceforge.kolmafia.session.ChoiceCombatAshState
import net.sourceforge.kolmafia.track.TrackManager

/**
 * Focused XLIV Track A coverage (phases 6491–6510).
 * Revision bump deferred to parent (phase6850).
 */
class GameRuntimeLibraryPhase6550Test {

    private lateinit var tempDir: File

    @BeforeTest
    fun setUp() {
        ChoiceCombatAshState.reset()
        CombatActionManager.resetForTest()
        Macrofier.resetForTest()
        MonsterStatusTracker.resetLastMonster()
        StandardRequest.resetForTest()
        tempDir = File(System.getProperty("java.io.tmpdir"), "kolmafia-xliva-${System.nanoTime()}")
        tempDir.mkdirs()
        UserDataFilePaths.testBasePath = tempDir.absolutePath
    }

    @AfterTest
    fun tearDown() {
        ChoiceCombatAshState.reset()
        CombatActionManager.resetForTest()
        Macrofier.resetForTest()
        MonsterStatusTracker.resetLastMonster()
        StandardRequest.resetForTest()
        UserDataFilePaths.testBasePath = null
        tempDir.deleteRecursively()
    }

    @Test
    fun get_ccs_action_usesCurrentKeyAndAllowMacro() {
        val p = prefs()
        CombatActionManager.loadFromText(
            """
            [ default ]
            skill saucegeyser
            attack with weapon
            """.trimIndent(),
            name = "xliva",
            preferences = p,
        )
        p.setString("battleAction", "custom combat script")
        val lib = GameRuntimeLibrary(preferences = p)
        assertEquals("skill saucegeyser", outputLib(lib, "print(get_ccs_action(0));").trim())
        assertEquals("attack with weapon", outputLib(lib, "print(get_ccs_action(1));").trim())
        assertEquals("default", CombatActionManager.getCurrentKey())
    }

    @Test
    fun set_ccs_read_write_lookupAndActiveReload() {
        val p = prefs()
        val body = "[ default ]\nskill saucegeyser\n"
        assertTrue(CombatActionManager.writeCcs("MyScript", body, p))
        assertTrue(
            outputLib(GameRuntimeLibrary(preferences = p), """print(set_ccs("myscript"));""")
                .trim().equals("true", ignoreCase = true),
        )
        assertEquals("MyScript", p.getString("customCombatScript", ""))
        assertTrue(
            outputLib(GameRuntimeLibrary(preferences = p), """print(read_ccs("MyScript"));""")
                .contains("saucegeyser"),
        )
        assertFalse(CombatActionManager.writeCcs("../evil", body, p))
        assertEquals("", CombatActionManager.readCcs("../evil"))
        val updated = "[ default ]\nattack with weapon\n"
        assertTrue(CombatActionManager.writeCcs("MyScript", updated, p))
        assertEquals(
            "attack with weapon",
            CombatActionManager.getCombatAction("default", 0, allowMacro = true, p),
        )
    }

    @Test
    fun run_combat_filter_executesAshCallback() {
        ChoiceCombatAshState.currentRound = 2
        ChoiceCombatAshState.lastFightResponseText = "fight-page"
        MonsterStatusTracker.setNextMonster(
            MonsterDefinition(
                name = "xliva slug",
                id = 65501,
                image = "x.gif",
                attack = 10,
                defense = 10,
                hp = 10,
                initiative = 0,
                meatDrop = 0,
                phylum = "beast",
                isBoss = false,
                isGhost = false,
                isLucky = false,
                isScaling = false,
                scale = 0,
                cap = 0,
                floor = 0,
                drops = emptyList(),
            ),
            emptyList(),
        )
        val lib = GameRuntimeLibrary(preferences = prefs())
        val src = """
            string myfilter(int round, monster m, string page) {
              return "abort \"from-filter\"";
            }
            print(run_combat("myfilter"));
        """.trimIndent()
        val out = outputLib(lib, src)
        assertTrue(out.isNotEmpty(), out)
        ChoiceCombatAshState.currentRound = 0
        ChoiceCombatAshState.inMultiFight = false
        assertEquals("", outputLib(lib, """print(run_combat("abort"));""").trim())
    }

    @Test
    fun is_banished_and_tracked_by_honorIsEffective() {
        val p = prefs()
        StandardRequest.parseResponse(
            "<b>Items</b><p><span class=\"i\">ice house</span><p>",
        )
        val restricted = CharacterState(isHardcore = true, roninLeft = 0)
        val banishes = BanishManager(p).also {
            it.banishMonster("Ninja Snowman", Banisher.ICE_HOUSE, currentTurn = 1)
            it.banishMonster("spooky vampire", Banisher.BANISHING_SHOUT, currentTurn = 1)
        }
        assertFalse(banishes.isBanished("Ninja Snowman", 1, restricted))
        assertTrue(banishes.isBanished("spooky vampire", 1, restricted))
        assertEquals(
            listOf(Banisher.BANISHING_SHOUT),
            banishes.banishedBy("spooky vampire", 1, restricted),
        )

        TrackManager.trackMonster(p, "spooky vampire", TrackManager.Tracker.NOSY_NOSE, 1)
        assertEquals(0, TrackManager.countCopies(p, "spooky vampire", 1, currentFamiliarId = -1))
        assertEquals(1, TrackManager.countCopies(p, "spooky vampire", 1, currentFamiliarId = 173))
        assertTrue(TrackManager.trackedBy(p, "spooky vampire", 1, 173).isNotEmpty())
        assertTrue(TrackManager.trackedBy(p, "spooky vampire", 1, -1).isEmpty())

        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(familiar = "173", turnsplayed = "1"))
        }
        val lib = GameRuntimeLibrary(preferences = p, banishManager = banishes, character = char)
        assertEquals(
            "true",
            outputLib(lib, """print(to_string(is_banished("spooky vampire")));""").trim(),
        )
        assertTrue(
            outputLib(lib, """print(count(banished_by("spooky vampire")));""").trim().toInt() >= 1,
        )
        assertEquals(
            "1",
            outputLib(lib, """print(track_copy_count("spooky vampire"));""").trim(),
        )
    }

    @Test
    fun choice_follows_fight_syncsFromFightHtml() {
        ChoiceCombatAshState.reset()
        assertEquals(
            "false",
            outputLib(GameRuntimeLibrary(), "print(choice_follows_fight());").trim(),
        )
        ChoiceCombatAshState.noteFightRound(
            """You win the fight!<br><a href="choice.php">Continue</a>""",
        )
        assertTrue(ChoiceCombatAshState.choiceFollowsFight)
        assertEquals(
            "true",
            outputLib(GameRuntimeLibrary(), "print(choice_follows_fight());").trim(),
        )
        assertEquals("phase7210", GameRuntimeLibrary.REVISION)
    }

    private fun prefs(): Preferences = Preferences(MapSettings())
}
