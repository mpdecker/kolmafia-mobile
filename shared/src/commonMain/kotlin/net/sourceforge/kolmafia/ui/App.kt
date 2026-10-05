package net.sourceforge.kolmafia.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.sourceforge.kolmafia.ash.ScriptEntry
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.session.SessionManager
import net.sourceforge.kolmafia.ui.adventure.AdventureScreen
import net.sourceforge.kolmafia.ui.character.CharacterScreen
import net.sourceforge.kolmafia.ui.chat.ChatScreen
import net.sourceforge.kolmafia.ui.familiar.FamiliarScreen
import net.sourceforge.kolmafia.ui.inventory.InventoryScreen
import net.sourceforge.kolmafia.ui.login.LoginScreen
import net.sourceforge.kolmafia.ui.login.LoginViewModel
import net.sourceforge.kolmafia.ui.mall.MallScreen
import net.sourceforge.kolmafia.ui.relay.RelayBrowserScreen
import net.sourceforge.kolmafia.ui.scripts.ScriptConsoleScreen
import net.sourceforge.kolmafia.ui.scripts.ScriptEditorScreen
import net.sourceforge.kolmafia.ui.scripts.ScriptsScreen
import net.sourceforge.kolmafia.ui.shop.ShopScreen
import net.sourceforge.kolmafia.ui.skills.SkillsScreen
import net.sourceforge.kolmafia.session.SessionState
import org.koin.compose.koinInject

/** In-app navigation state for the Scripts sub-screens. */
private sealed class ScriptsNav {
    object List : ScriptsNav()
    data class Editor(val script: ScriptEntry?) : ScriptsNav()
    data class Console(val name: String) : ScriptsNav()
}

private data class AppDestination(
    val label: String,
    val icon: ImageVector,
)

private val APP_DESTINATIONS = listOf(
    AppDestination("Character", Icons.Default.AccountCircle),
    AppDestination("Adventure", Icons.Default.Place),
    AppDestination("Inventory", Icons.AutoMirrored.Filled.List),
    AppDestination("Skills", Icons.Default.AutoFixHigh),
    AppDestination("Scripts", Icons.Default.Code),
    AppDestination("Familiars", Icons.Default.Favorite),
    AppDestination("Chat", Icons.Default.Forum),
    AppDestination("Shop", Icons.Default.Store),
    AppDestination("Mall", Icons.Default.ShoppingCart),
    AppDestination("Relay", Icons.Default.Language),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(
    loginOverride: (suspend (String, String) -> SessionState)? = null,
) {
    MaterialTheme {
        var isLoggedIn by remember { mutableStateOf(false) }
        val character: KoLCharacter = koinInject()

        if (!isLoggedIn) {
            val viewModel = if (loginOverride != null) {
                remember(loginOverride) { LoginViewModel(loginOverride = loginOverride) }
            } else {
                val sessionManager: SessionManager = koinInject()
                remember { LoginViewModel(sessionManager = sessionManager) }
            }
            LoginScreen(viewModel = viewModel, onLoginSuccess = { isLoggedIn = true })
            return@MaterialTheme
        }

        var selectedTab by remember { mutableIntStateOf(0) }
        var scriptsNav by remember { mutableStateOf<ScriptsNav>(ScriptsNav.List) }
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    Text(
                        text = "KoLmafia",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(16.dp),
                    )
                    APP_DESTINATIONS.forEachIndexed { index, destination ->
                        NavigationDrawerItem(
                            label = { Text(destination.label) },
                            selected = selectedTab == index,
                            onClick = {
                                selectedTab = index
                                if (index == 4) scriptsNav = ScriptsNav.List
                                scope.launch { drawerState.close() }
                            },
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            modifier = Modifier.padding(horizontal = 12.dp),
                        )
                    }
                }
            },
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(APP_DESTINATIONS[selectedTab].label) },
                        navigationIcon = {
                            IconButton(
                                onClick = { scope.launch { drawerState.open() } },
                            ) {
                                Icon(Icons.Default.Menu, contentDescription = "Open navigation")
                            }
                        },
                    )
                },
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    when (selectedTab) {
                        0 -> CharacterScreen(character = character)
                        1 -> AdventureScreen()
                        2 -> InventoryScreen()
                        3 -> SkillsScreen()
                        4 -> when (val nav = scriptsNav) {
                            is ScriptsNav.List -> ScriptsScreen(
                                onEditScript = { scriptsNav = ScriptsNav.Editor(it) },
                                onShowConsole = { name -> scriptsNav = ScriptsNav.Console(name) },
                            )
                            is ScriptsNav.Editor -> ScriptEditorScreen(
                                existingScript = nav.script,
                                onSaved = { scriptsNav = ScriptsNav.List },
                                onCancel = { scriptsNav = ScriptsNav.List },
                            )
                            is ScriptsNav.Console -> ScriptConsoleScreen(
                                scriptName = nav.name,
                                onBack = { scriptsNav = ScriptsNav.List },
                            )
                        }
                        5 -> FamiliarScreen()
                        6 -> ChatScreen()
                        7 -> ShopScreen()
                        8 -> MallScreen()
                        9 -> RelayBrowserScreen()
                    }
                }
            }
        }
    }
}
