package net.sourceforge.kolmafia.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import net.sourceforge.kolmafia.ui.scripts.ScriptConsoleScreen
import net.sourceforge.kolmafia.ui.scripts.ScriptEditorScreen
import net.sourceforge.kolmafia.ui.scripts.ScriptsScreen
import net.sourceforge.kolmafia.ui.shop.ShopScreen
import net.sourceforge.kolmafia.ui.skills.SkillsScreen
import org.koin.compose.koinInject

/** In-app navigation state for the Scripts sub-screens. */
private sealed class ScriptsNav {
    object List : ScriptsNav()
    data class Editor(val script: ScriptEntry?) : ScriptsNav()
    data class Console(val name: String) : ScriptsNav()
}

@Composable
fun App() {
    MaterialTheme {
        var isLoggedIn by remember { mutableStateOf(false) }
        val sessionManager: SessionManager = koinInject()
        val character: KoLCharacter = koinInject()

        if (!isLoggedIn) {
            val viewModel = remember { LoginViewModel(sessionManager) }
            LoginScreen(viewModel = viewModel, onLoginSuccess = { isLoggedIn = true })
            return@MaterialTheme
        }

        var selectedTab by remember { mutableIntStateOf(0) }
        var scriptsNav by remember { mutableStateOf<ScriptsNav>(ScriptsNav.List) }

        Scaffold(
            bottomBar = {
                NavigationBar {
                    // 0 Character
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.AccountCircle, "Character") },
                        label = { Text("Character") }
                    )
                    // 1 Adventure
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.Place, "Adventure") },
                        label = { Text("Adventure") }
                    )
                    // 2 Inventory
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.AutoMirrored.Filled.List, "Inventory") },
                        label = { Text("Inventory") }
                    )
                    // 3 Skills
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = { Icon(Icons.Default.AutoFixHigh, "Skills") },
                        label = { Text("Skills") }
                    )
                    // 4 Scripts (new)
                    NavigationBarItem(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4; scriptsNav = ScriptsNav.List },
                        icon = { Icon(Icons.Default.Code, "Scripts") },
                        label = { Text("Scripts") }
                    )
                    // 5 Familiars
                    NavigationBarItem(
                        selected = selectedTab == 5,
                        onClick = { selectedTab = 5 },
                        icon = { Icon(Icons.Default.Favorite, "Familiars") },
                        label = { Text("Familiars") }
                    )
                    // 6 Chat
                    NavigationBarItem(
                        selected = selectedTab == 6,
                        onClick = { selectedTab = 6 },
                        icon = { Icon(Icons.Default.Forum, "Chat") },
                        label = { Text("Chat") }
                    )
                    // 7 Shop
                    NavigationBarItem(
                        selected = selectedTab == 7,
                        onClick = { selectedTab = 7 },
                        icon = { Icon(Icons.Default.Store, "Shop") },
                        label = { Text("Shop") }
                    )
                    // 8 Mall
                    NavigationBarItem(
                        selected = selectedTab == 8,
                        onClick = { selectedTab = 8 },
                        icon = { Icon(Icons.Default.ShoppingCart, "Mall") },
                        label = { Text("Mall") }
                    )
                }
            }
        ) { _ ->
            when (selectedTab) {
                0 -> CharacterScreen(character = character)
                1 -> AdventureScreen()
                2 -> InventoryScreen()
                3 -> SkillsScreen()
                4 -> when (val nav = scriptsNav) {
                    is ScriptsNav.List -> ScriptsScreen(
                        onEditScript = { scriptsNav = ScriptsNav.Editor(it) },
                        onShowConsole = { name -> scriptsNav = ScriptsNav.Console(name) }
                    )
                    is ScriptsNav.Editor -> ScriptEditorScreen(
                        existingScript = nav.script,
                        onSaved = { scriptsNav = ScriptsNav.List },
                        onCancel = { scriptsNav = ScriptsNav.List }
                    )
                    is ScriptsNav.Console -> ScriptConsoleScreen(
                        scriptName = nav.name,
                        onBack = { scriptsNav = ScriptsNav.List }
                    )
                }
                5 -> FamiliarScreen()
                6 -> ChatScreen()
                7 -> ShopScreen()
                8 -> MallScreen()
            }
        }
    }
}
