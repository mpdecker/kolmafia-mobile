package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.adventure.choice.EffectPool
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.WereProfessorDatabase
import net.sourceforge.kolmafia.effect.EffectManager
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.ResearchBenchRequest

class WereProfessorManagerTest {

    private lateinit var preferences: Preferences

    @BeforeTest
    fun setUp() {
        WereProfessorDatabase.resetForTest()
        WereProfessorDatabase.injectForTest(
            WereProfessorDatabase.parseForTest(
                """
                1	mus1	10	none	Osteocalcin injection	Mus +20%
                2	mus2	20	mus1	Somatostatin catalyst	Mus +30%
                """.trimIndent(),
            ),
        )
        preferences = Preferences(MapSettings())
    }

    @AfterTest
    fun tearDown() {
        WereProfessorDatabase.resetForTest()
    }

    @Test
    fun dumpSkills_nonAnnotated_includesFieldAndCost() = runBlocking {
        WereProfessorDatabase.resetForTest()
        WereProfessorDatabase.load()
        val lines = mutableListOf<String>()
        WereProfessorManager.dumpSkills(verbose = false, print = lines::add)
        val html = lines.single()
        assertTrue(html.contains("<table border=2 cols=6>"))
        assertTrue(html.contains("mus1 (10 rp)"))
    }

    @Test
    fun dumpSkills_annotated_marksKnownAvailableAndLocked() = runBlocking {
        WereProfessorDatabase.resetForTest()
        WereProfessorDatabase.load()
        val mus1 = WereProfessorDatabase.findResearch("mus1")!!
        val mus2 = WereProfessorDatabase.findResearch("mus2")!!
        val lines = mutableListOf<String>()
        WereProfessorManager.dumpSkills(
            known = setOf(mus1),
            available = setOf(mus2),
            verbose = false,
            print = lines::add,
        )
        val html = lines.single()
        assertTrue(html.contains("""color:black font-weight:bold"""))
        assertTrue(html.contains("""color:red"""))
        assertTrue(html.contains("mus1"))
        assertTrue(html.contains("mus2 (20 rp)"))
    }

    @Test
    fun researchSkill_rejectsUnknownField() = kotlinx.coroutines.test.runTest {
        val lines = mutableListOf<String>()
        val ok = WereProfessorManager.researchSkill(
            field = "unknown",
            charState = CharacterState(challengePath = "WereProfessor"),
            effectNames = emptyList(),
            preferences = preferences,
            request = null,
            print = lines::add,
        )
        assertFalse(ok)
        assertTrue(lines.single().contains("is not known research"))
    }

    @Test
    fun researchSkill_rejectsNonWereProfessor() = kotlinx.coroutines.test.runTest {
        val lines = mutableListOf<String>()
        val ok = WereProfessorManager.researchSkill(
            field = "mus1",
            charState = CharacterState(challengePath = "Standard"),
            effectNames = emptyList(),
            preferences = preferences,
            request = null,
            print = lines::add,
        )
        assertFalse(ok)
        assertTrue(lines.single().contains("Only WereProfessors can use their Research Bench."))
    }

    @Test
    fun researchSkill_rejectsSavageBeast() = kotlinx.coroutines.test.runTest {
        val lines = mutableListOf<String>()
        val ok = WereProfessorManager.researchSkill(
            field = "mus1",
            charState = CharacterState(challengePath = "WereProfessor"),
            effectNames = listOf(EffectPool.SAVAGE_BEAST),
            preferences = preferences,
            request = null,
            print = lines::add,
        )
        assertFalse(ok)
        assertTrue(lines.single().contains("locked out of your Humble Cottage"))
    }

    @Test
    fun researchSkill_rejectsAlreadyKnown() = kotlinx.coroutines.test.runTest {
        val mus1 = WereProfessorDatabase.findResearch("mus1")!!
        WereProfessorDatabase.saveResearch(preferences, WereProfessorDatabase.KNOWN_RESEARCH, setOf(mus1))
        val lines = mutableListOf<String>()
        val ok = WereProfessorManager.researchSkill(
            field = "mus1",
            charState = CharacterState(challengePath = "WereProfessor"),
            effectNames = emptyList(),
            preferences = preferences,
            request = null,
            print = lines::add,
        )
        assertFalse(ok)
        assertTrue(lines.single().contains("already researched 'mus1'"))
    }

    @Test
    fun researchSkill_delegatesToRequestWhenValid() = kotlinx.coroutines.test.runTest {
        val mus1 = WereProfessorDatabase.findResearch("mus1")!!
        WereProfessorDatabase.saveResearch(
            preferences,
            WereProfessorDatabase.AVAILABLE_RESEARCH,
            setOf(mus1),
        )
        preferences.setInt("wereProfessorResearchPoints", 100)
        var researched: String? = null
        val request = object : ResearchBenchRequest(
            HttpClient(MockEngine { respond("", HttpStatusCode.OK) }),
            EffectManager(
                HttpClient(MockEngine { respond("", HttpStatusCode.OK) }),
                GameEventBus(),
            ),
            preferences,
        ) {
            override suspend fun visitBench() = Result.success("" to "")
            override suspend fun research(field: String): Result<Pair<String, String>> {
                researched = field
                preferences.setInt("wereProfessorResearchPoints", 90)
                return Result.success("" to "")
            }
        }
        val lines = mutableListOf<String>()
        val ok = WereProfessorManager.researchSkill(
            field = "mus1",
            charState = CharacterState(challengePath = "WereProfessor"),
            effectNames = emptyList(),
            preferences = preferences,
            request = request,
            print = lines::add,
        )
        assertTrue(ok)
        assertEquals("mus1", researched)
        assertTrue(lines.single().contains("You have 90 Research Points available."))
    }
}
