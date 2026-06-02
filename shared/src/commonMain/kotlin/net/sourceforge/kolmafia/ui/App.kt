package net.sourceforge.kolmafia.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Place
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
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.session.SessionManager
import net.sourceforge.kolmafia.ui.adventure.AdventureScreen
import net.sourceforge.kolmafia.ui.character.CharacterScreen
import net.sourceforge.kolmafia.ui.familiar.FamiliarScreen
import net.sourceforge.kolmafia.ui.inventory.InventoryScreen
import net.sourceforge.kolmafia.ui.login.LoginScreen
import net.sourceforge.kolmafia.ui.login.LoginViewModel
import net.sourceforge.kolmafia.ui.skills.SkillsScreen
import org.koin.compose.koinInject

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

        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.AccountCircle, "Character") },
                        label = { Text("Character") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.Place, "Adventure") },
                        label = { Text("Adventure") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.AutoMirrored.Filled.List, "Inventory") },
                        label = { Text("Inventory") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = { Icon(Icons.Default.AutoFixHigh, "Skills") },
                        label = { Text("Skills") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 },
                        icon = { Icon(Icons.Default.Favorite, "Familiars") },
                        label = { Text("Familiars") }
                    )
                }
            }
        ) { _ ->
            when (selectedTab) {
                0 -> CharacterScreen(character = character)
                1 -> AdventureScreen()
                2 -> InventoryScreen()
                3 -> SkillsScreen()
                4 -> FamiliarScreen()
            }
        }
    }
}
