package net.sourceforge.kolmafia.ui.login

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.sourceforge.kolmafia.session.SessionManager
import net.sourceforge.kolmafia.session.SessionState

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false
)

class LoginViewModel(
    private val sessionManager: SessionManager? = null,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main),
    private val loginOverride: (suspend (String, String) -> SessionState)? = null,
) {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onUsernameChange(value: String) {
        _uiState.value = _uiState.value.copy(username = value, error = null)
    }

    fun onPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(password = value, error = null)
    }

    fun onLoginClick() {
        val state = _uiState.value
        if (state.username.isBlank() || state.password.isBlank()) {
            _uiState.value = state.copy(error = "Username and password are required")
            return
        }
        _uiState.value = state.copy(isLoading = true, error = null)
        scope.launch {
            val result = when {
                loginOverride != null -> loginOverride.invoke(state.username, state.password)
                sessionManager != null -> sessionManager.login(state.username, state.password)
                else -> SessionState.Error("Login is not configured")
            }
            _uiState.value = when (result) {
                SessionState.LoggedIn ->
                    _uiState.value.copy(isLoading = false, isLoggedIn = true)
                is SessionState.Error ->
                    _uiState.value.copy(isLoading = false, error = result.message)
                SessionState.LoggedOut ->
                    _uiState.value.copy(isLoading = false)
            }
        }
    }
}
