package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.familiar.FamiliarData
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.familiar.FamiliarState
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.GreyYouManager

/** Phases 1023–1032 Familiar / path CLI Track B corpus. */
class GameRuntimeLibraryFamiliarPathCliTest {

    @AfterTest
    fun resetGreyYou() = GreyYouManager.resetAbsorptions()

    private fun prefs(block: MapSettings.() -> Unit = {}): Preferences {
        val s = MapSettings()
        s.block()
        return Preferences(s)
    }

    private fun invWith(items: Map<Int, InventoryItem>): InventoryManager =
        object : InventoryManager(
            HttpClient(MockEngine { respond("") }),
            GameEventBus(),
        ) {
            private val flow = MutableStateFlow(InventoryState(items = items))
            override val state = flow.asStateFlow()
        }

    private fun fakeFamiliarManager(
        familiars: List<FamiliarData> = emptyList(),
        active: FamiliarData? = null,
    ): FamiliarManager {
        val client = HttpClient(MockEngine { respond("ok") })
        val fm = object : FamiliarManager(client, GameEventBus()) {}
        fm.testSetState(FamiliarState(ownedFamiliars = familiars, activeFamiliar = active))
        return fm
    }

    @Test
    fun familiarLock_setsPreference() {
        val p = prefs()
        val lib = GameRuntimeLibrary(preferences = p)
        val out = outputLib(lib, """cli_execute("familiar lock");""")
        assertTrue(out.contains("locked"), out)
        assertEquals(true, p.getBoolean("familiarEquipmentLocked", false))
    }

    @Test
    fun familiarUnlock_whenAlreadyUnlocked_reports() {
        val p = prefs()
        val lib = GameRuntimeLibrary(preferences = p)
        val out = outputLib(lib, """cli_execute("familiar unlock");""")
        assertTrue(out.contains("already unlocked"), out)
    }

    @Test
    fun familiarLock_whenAlreadyLocked_reports() {
        val p = prefs { putBoolean("familiarEquipmentLocked", true) }
        val lib = GameRuntimeLibrary(preferences = p)
        val out = outputLib(lib, """cli_execute("familiar lock");""")
        assertTrue(out.contains("already locked"), out)
    }

    @Test
    fun absorptions_aliasDispatches() {
        val lib = GameRuntimeLibrary(preferences = prefs())
        // May print registry rows or the empty-match line depending on MonsterDatabase load.
        val out = outputLib(lib, """cli_execute("absorptions");""")
        assertTrue(
            out.contains("Grey You") || out.contains("[") || out.contains("No Grey You"),
            out,
        )
    }

    @Test
    fun gooskills_listsOrEmpty() {
        val lib = GameRuntimeLibrary(preferences = prefs())
        val out = outputLib(lib, """cli_execute("gooskills");""")
        assertTrue(
            out.contains("[") || out.contains("No Grey You goo skills"),
            out,
        )
    }

    @Test
    fun bugbears_statusDump() {
        val p = prefs {
            putString("statusMedbay", "2")
            putString("statusSonar", "cleared")
        }
        val lib = GameRuntimeLibrary(preferences = p)
        val out = outputLib(lib, """cli_execute("bugbears");""")
        assertTrue(out.contains("Medbay"), out)
        assertTrue(out.contains("2/3"), out)
        assertTrue(out.contains("Sonar"), out)
        assertTrue(out.contains("cleared"), out)
        assertTrue(out.contains("hypodermic bugbear"), out)
    }

    @Test
    fun chibi_usageWhenNoPrefs() {
        val lib = GameRuntimeLibrary(preferences = prefs())
        val out = outputLib(lib, """cli_execute("chibi");""")
        assertTrue(out.contains("Usage: chibi"), out)
    }

    @Test
    fun chibi_statusFromPrefs() {
        val p = prefs {
            putString("chibiName", "Li'l Test")
            putInt("chibiFitness", 3)
            putInt("chibiIntelligence", 4)
        }
        val lib = GameRuntimeLibrary(preferences = p)
        val out = outputLib(lib, """cli_execute("chibi");""")
        assertTrue(out.contains("Li'l Test"), out)
        assertTrue(out.contains("Fitness: 3/10"), out)
        assertTrue(out.contains("Intelligence: 4/10"), out)
    }

    @Test
    fun panda_usageWhenEmpty() {
        val lib = GameRuntimeLibrary(preferences = prefs())
        val out = outputLib(lib, """cli_execute("panda");""")
        assertTrue(out.contains("Usage: panda"), out)
    }

    @Test
    fun panda_rejectsUnknownComedy() {
        val lib = GameRuntimeLibrary(preferences = prefs())
        val out = outputLib(lib, """cli_execute("panda comedy slapstick");""")
        assertTrue(out.contains("comedy"), out)
    }

    @Test
    fun devilcandyegg_requiresDeviler() {
        val lib = GameRuntimeLibrary(preferences = prefs())
        val out = outputLib(lib, """cli_execute("devilcandyegg");""")
        assertTrue(out.contains("candy egg devil"), out)
    }

    @Test
    fun devilcandyegg_usageWhenOwnedNoItem() {
        val inv = invWith(
            mapOf(
                11774 to InventoryItem(11774, "candy egg deviler", 1, ItemType.OTHER),
            ),
        )
        val lib = GameRuntimeLibrary(preferences = prefs(), inventoryManager = inv)
        val out = outputLib(lib, """cli_execute("devilcandyegg");""")
        assertTrue(out.contains("Usage: devilcandyegg"), out)
    }

    @Test
    fun train_statusStub() {
        val goat = FamiliarData(
            id = 7, name = "Billy", race = "Angry Goat",
            weight = 12, experience = 0, kills = 0,
        )
        val fm = fakeFamiliarManager(familiars = listOf(goat), active = goat)
        val lib = GameRuntimeLibrary(
            preferences = prefs(),
            familiarManager = fm,
            character = KoLCharacter(),
        )
        val out = outputLib(lib, """cli_execute("train");""")
        assertTrue(out.contains("not available"), out)
        assertTrue(out.contains("Angry Goat"), out)
        assertTrue(out.contains("weight 12"), out)
    }

    @Test
    fun help_listsTrackBVerbs() {
        val lib = GameRuntimeLibrary()
        for (verb in listOf("absorptions", "bugbears", "chibi", "devilcandyegg", "gooskills", "panda", "train")) {
            val out = outputLib(lib, """cli_execute("help $verb");""")
            assertTrue(out.contains(verb), "help $verb → $out")
        }
    }
}
