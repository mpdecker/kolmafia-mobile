package net.sourceforge.kolmafia.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import net.sourceforge.kolmafia.ash.ScriptEntry
import net.sourceforge.kolmafia.ash.ScriptManager
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.di.platformModule
import net.sourceforge.kolmafia.di.sharedModule
import net.sourceforge.kolmafia.session.SessionState
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

/**
 * Offline Compose user journeys for the logged-out login gate and logged-in drawer shell.
 * Login never calls Kingdom of Loathing — [App] is given a [loginOverride].
 */
@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class UserJourneyTest {
    private val mainDispatcher = UnconfinedTestDispatcher()
    private var loginBehavior: suspend (String, String) -> SessionState =
        { _, _ -> SessionState.LoggedIn }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
        stopKoinQuietly()
        startKoin {
            allowOverride(true)
            modules(
                sharedModule,
                platformModule,
                module {
                    single<HttpClient> {
                        HttpClient(
                            MockEngine { _ ->
                                respond(
                                    content = "{}",
                                    status = HttpStatusCode.OK,
                                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                                )
                            },
                        )
                    }
                },
            )
        }
        GlobalContext.get().get<KoLCharacter>().updateFromApiResponse(
            net.sourceforge.kolmafia.character.CharacterApiResponse(
                name = "JourneyTester",
                level = "5",
            ),
        )
    }

    @AfterTest
    fun tearDown() {
        stopKoinQuietly()
        Dispatchers.resetMain()
    }

    @Test
    fun loggedOut_showsUsernamePasswordAndBlankSubmitError() = runComposeUiTest {
        setContent { App(loginOverride = loginBehavior) }
        onNodeWithText("KoLmafia Mobile").assertIsDisplayed()
        onNodeWithText("Username").assertIsDisplayed()
        onNodeWithText("Password").assertIsDisplayed()
        onNodeWithText("Log In").performClick()
        waitForIdle()
        onNodeWithText("Username and password are required").assertIsDisplayed()
    }

    @Test
    fun loggedOut_errorLoginStaysOnLogin() = runComposeUiTest {
        loginBehavior = { _, _ -> SessionState.Error("Invalid password") }
        setContent { App(loginOverride = loginBehavior) }
        onNodeWithText("Username").performTextInput("player")
        onNodeWithText("Password").performTextInput("wrong")
        onNodeWithText("Log In").performClick()
        waitForIdle()
        onNodeWithText("Invalid password").assertIsDisplayed()
        onNodeWithText("KoLmafia Mobile").assertIsDisplayed()
        onNodeWithText("Log In").assertIsDisplayed()
    }

    @Test
    fun successfulLogin_showsDrawerAndCharacter() = runComposeUiTest {
        setContent { App(loginOverride = loginBehavior) }
        performLogin()
        onNodeWithContentDescription("Open navigation").assertIsDisplayed()
        onAllNodesWithText("Character")[0].assertIsDisplayed()
        onNodeWithText("JourneyTester").assertIsDisplayed()
        onNodeWithText("Level 5").assertIsDisplayed()
        onNodeWithText("Stats").assertIsDisplayed()
    }

    @Test
    fun destination_inventory() = runComposeUiTest {
        setContent { App(loginOverride = loginBehavior) }
        performLogin()
        openDestination("Inventory")
        onAllNodesWithText("Inventory")[0].assertIsDisplayed()
    }

    @Test
    fun destination_adventure() = runComposeUiTest {
        setContent { App(loginOverride = loginBehavior) }
        performLogin()
        openDestination("Adventure")
        onAllNodesWithText("Adventure")[0].assertIsDisplayed()
        onNodeWithText("Browse").assertIsDisplayed()
    }

    @Test
    fun destination_skills() = runComposeUiTest {
        setContent { App(loginOverride = loginBehavior) }
        performLogin()
        openDestination("Skills")
        onAllNodesWithText("Skills")[0].assertIsDisplayed()
    }

    @Test
    fun destination_scripts() = runComposeUiTest {
        setContent { App(loginOverride = loginBehavior) }
        performLogin()
        openDestination("Scripts")
        onNodeWithText("New Script").assertIsDisplayed()
    }

    @Test
    fun destination_familiars() = runComposeUiTest {
        setContent { App(loginOverride = loginBehavior) }
        performLogin()
        openDestination("Familiars")
        onNodeWithText("No familiar active").assertIsDisplayed()
    }

    @Test
    fun destination_chat() = runComposeUiTest {
        setContent { App(loginOverride = loginBehavior) }
        performLogin()
        openDestination("Chat")
        onNodeWithText("Send").assertIsDisplayed()
    }

    @Test
    fun destination_shop() = runComposeUiTest {
        setContent { App(loginOverride = loginBehavior) }
        performLogin()
        openDestination("Shop")
        onNodeWithText("Select a coinmaster").assertIsDisplayed()
    }

    @Test
    fun destination_mall() = runComposeUiTest {
        setContent { App(loginOverride = loginBehavior) }
        performLogin()
        openDestination("Mall")
        onNodeWithText("Search").assertIsDisplayed()
    }

    @Test
    fun destination_relay() = runComposeUiTest {
        setContent { App(loginOverride = loginBehavior) }
        performLogin()
        openDestination("Relay")
        onNodeWithText("Stop Relay").assertIsDisplayed()
    }

    @Test
    fun scripts_listToEditorAndBack() = runComposeUiTest {
        setContent { App(loginOverride = loginBehavior) }
        performLogin()
        openDestination("Scripts")
        onNodeWithText("New Script").performClick()
        waitForIdle()
        onNodeWithText("New Script").assertIsDisplayed()
        onNodeWithText("Script name").assertIsDisplayed()
        onNodeWithText("Cancel").performClick()
        waitForIdle()
        onNodeWithText("No scripts yet. Tap 'New Script' to create one.").assertIsDisplayed()
    }

    @Test
    fun scripts_savedScriptShowsRunControl() = runComposeUiTest {
        GlobalContext.get().get<ScriptManager>().saveScript(
            ScriptEntry(name = "journey_probe_list", source = "print(\"hi\");"),
        )
        setContent { App(loginOverride = loginBehavior) }
        performLogin()
        openDestination("Scripts")
        onNodeWithText("journey_probe_list").assertIsDisplayed()
        onNodeWithText("Run").assertIsDisplayed()
        onNodeWithText("Edit").assertIsDisplayed()
    }

    @Test
    fun scripts_listToConsoleAndBack() = runComposeUiTest {
        GlobalContext.get().get<ScriptManager>().saveScript(
            ScriptEntry(name = "journey_probe_run", source = "print(\"hi\");"),
        )
        setContent { App(loginOverride = loginBehavior) }
        performLogin()
        openDestination("Scripts")
        onNodeWithText("Run").performClick()
        waitForIdle()
        onNodeWithText("← Back").assertIsDisplayed()
        onNodeWithText("journey_probe_run").assertIsDisplayed()
        onNodeWithText("← Back").performClick()
        waitForIdle()
        onNodeWithText("Run").assertIsDisplayed()
        onNodeWithText("Edit").assertIsDisplayed()
    }

    @Test
    fun character_opensAndDismissesEffectsSheet() = runComposeUiTest {
        setContent { App(loginOverride = loginBehavior) }
        performLogin()
        onNodeWithText("Active Effects").performClick()
        waitForIdle()
        onNodeWithText("No active effects.").assertIsDisplayed()
        onNodeWithText("0 effects").assertIsDisplayed()
        onNodeWithText("Close").performClick()
        waitForIdle()
        onAllNodesWithText("No active effects.").assertCountEquals(0)
        onNodeWithText("JourneyTester").assertIsDisplayed()
        onNodeWithText("Stats").assertIsDisplayed()
    }

    @Test
    fun character_opensAndDismissesAboutSheet() = runComposeUiTest {
        setContent { App(loginOverride = loginBehavior) }
        performLogin()
        onNodeWithText("About").performClick()
        waitForIdle()
        onNodeWithText("KoLmafia Mobile").assertIsDisplayed()
        onNodeWithText("Revision phase10150", substring = true).assertIsDisplayed()
        onNodeWithText("mpdecker.github.io/kolmafia-mobile/privacy-policy.html", substring = true)
            .assertIsDisplayed()
        onNodeWithText("github.com/mpdecker/kolmafia-mobile", substring = true).assertIsDisplayed()
        onNodeWithText("Close").performClick()
        waitForIdle()
        onNodeWithText("JourneyTester").assertIsDisplayed()
        onNodeWithText("Stats").assertIsDisplayed()
    }

    @Test
    fun drawer_listsAllTenDestinations() = runComposeUiTest {
        setContent { App(loginOverride = loginBehavior) }
        performLogin()
        onNodeWithContentDescription("Open navigation").performClick()
        waitForIdle()
        listOf(
            "Character",
            "Adventure",
            "Inventory",
            "Skills",
            "Scripts",
            "Familiars",
            "Chat",
            "Shop",
            "Mall",
            "Relay",
        ).forEach { label ->
            // Drawer items expose contentDescription; top bar also shows the selected label as text.
            onNodeWithContentDescription(label).assertIsDisplayed()
        }
    }

    @Test
    fun adventure_opensZoneBrowserAndCloses() = runComposeUiTest {
        setContent { App(loginOverride = loginBehavior) }
        performLogin()
        openDestination("Adventure")
        onNodeWithText("Browse").performClick()
        waitForIdle()
        onNodeWithText("Browse Zones").assertIsDisplayed()
        onNodeWithText("Close").performClick()
        waitForIdle()
        onNodeWithText("Browse").assertIsDisplayed()
    }

    @Test
    fun mall_typesSearchQuery() = runComposeUiTest {
        setContent { App(loginOverride = loginBehavior) }
        performLogin()
        openDestination("Mall")
        onNodeWithText("Search the mall…").performTextInput("tent")
        waitForIdle()
        onNodeWithText("tent").assertIsDisplayed()
        onNodeWithText("Search").assertIsDisplayed()
    }

    @Test
    fun scripts_editSavedScriptAndCancel() = runComposeUiTest {
        GlobalContext.get().get<ScriptManager>().saveScript(
            ScriptEntry(name = "journey_probe_edit", source = "print(\"edit\");"),
        )
        setContent { App(loginOverride = loginBehavior) }
        performLogin()
        openDestination("Scripts")
        onNodeWithText("Edit").performClick()
        waitForIdle()
        onNodeWithText("Edit Script").assertIsDisplayed()
        onNodeWithText("journey_probe_edit").assertIsDisplayed()
        onNodeWithText("Cancel").performClick()
        waitForIdle()
        onNodeWithText("Run").assertIsDisplayed()
        onNodeWithText("Edit").assertIsDisplayed()
    }

    private fun androidx.compose.ui.test.ComposeUiTest.performLogin() {
        onNodeWithText("Username").performTextInput("player")
        onNodeWithText("Password").performTextInput("secret")
        onNodeWithText("Log In").performClick()
        waitForIdle()
    }

    private fun androidx.compose.ui.test.ComposeUiTest.openDestination(label: String) {
        onNodeWithContentDescription("Open navigation").performClick()
        waitForIdle()
        val nodes = onAllNodesWithText(label)
        nodes[nodes.fetchSemanticsNodes().lastIndex].performClick()
        waitForIdle()
    }

    private fun stopKoinQuietly() {
        try {
            stopKoin()
        } catch (_: Exception) {
            // Already stopped.
        }
    }
}
