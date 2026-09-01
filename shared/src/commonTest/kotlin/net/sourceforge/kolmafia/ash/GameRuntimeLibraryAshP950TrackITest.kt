package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.session.ChoiceCombatAshState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameRuntimeLibraryAshP950TrackITest {

    @BeforeTest
    fun setUp() {
        ChoiceCombatAshState.reset()
    }

    @AfterTest
    fun tearDown() {
        ChoiceCombatAshState.reset()
    }

    @Test
    fun phase950_availableChoiceTextInputs() {
        ChoiceCombatAshState.lastChoiceResponseText = """
            <form>
            <input type="hidden" name="whichchoice" value="100">
            <input type="submit" name="option" value="1">Go
            Coordinates: <input name="word" type="text" size="15">
            </form>
        """.trimIndent()
        val lib = GameRuntimeLibrary(preferences = prefs())
        assertEquals("1", outputLib(lib, "print(count(available_choice_text_inputs(1)));"))
        assertEquals("true", outputLib(lib, """print(contains_key(available_choice_text_inputs(1), "word"));"""))
    }

    @Test
    fun phase951_availableChoiceSelectInputs() {
        ChoiceCombatAshState.lastChoiceResponseText = """
            <form>
            <input type="hidden" name="whichchoice" value="100">
            <input type="submit" name="option" value="2">Toss
            <select name="tossid"><option value="7375">actual tapas</option></select>
            </form>
        """.trimIndent()
        val lib = GameRuntimeLibrary(preferences = prefs())
        assertEquals("1", outputLib(lib, "print(count(available_choice_select_inputs(2)));"))
        assertEquals(
            "actual tapas",
            outputLib(lib, """print(available_choice_select_inputs(2)["tossid"]["7375"]);"""),
        )
    }

    @Test
    fun phase952_formFields() {
        ChoiceCombatAshState.setFormFieldsFromPostData("whichchoice=100&option=1&word=abc")
        val lib = GameRuntimeLibrary(preferences = prefs())
        assertEquals("3", outputLib(lib, "print(count(form_fields()));"))
        assertEquals("abc", outputLib(lib, """print(form_fields()["word"]);"""))
    }

    @Test
    fun phase953_choiceFollowsFight() {
        ChoiceCombatAshState.choiceFollowsFight = true
        val lib = GameRuntimeLibrary(preferences = prefs())
        assertEquals("true", outputLib(lib, "print(choice_follows_fight());"))
    }

    @Test
    fun phase954_spoilersAppendTitle() {
        ChoiceCombatAshState.lastChoiceResponseText = """
            <form>
            <input type="submit" name="option" value="1" title="cool item">Take left
            </form>
        """.trimIndent()
        val lib = GameRuntimeLibrary(preferences = prefs())
        val withSpoilers = outputLib(lib, "print(available_choice_options(true)[1]);")
        assertTrue(withSpoilers.contains("cool item"), withSpoilers)
        val plain = outputLib(lib, "print(available_choice_options(false)[1]);")
        assertTrue(plain.contains("left", ignoreCase = true), plain)
    }

    @Test
    fun phase955_runCombatFilterAccepted() {
        ChoiceCombatAshState.currentRound = 1
        ChoiceCombatAshState.lastFightResponseText = "fight-html"
        val lib = GameRuntimeLibrary(preferences = prefs())
        assertEquals("fight-html", outputLib(lib, """print(run_combat("abort"));"""))
        assertEquals("phase4370", GameRuntimeLibrary.REVISION)
    }
}
