package com.timalo.mobileevent.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timalo.mobileevent.data.preferences.AppPreferences
import com.timalo.mobileevent.data.repository.AuthRepository
import com.timalo.mobileevent.model.Environment
import com.timalo.mobileevent.model.defaultCredentials
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val env: Environment = Environment.PREPROD,
    val email: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val loggedIn: Boolean = false
)

class LoginViewModel(
    private val authRepository: AuthRepository,
    private val prefs: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val env = prefs.getEnvironment()
            applyEnvDefaults(env)
        }
    }

    private fun applyEnvDefaults(env: Environment) {
        val creds = env.defaultCredentials()
        _uiState.value = _uiState.value.copy(
            env = env,
            email = creds.email,
            password = creds.password,
            error = null
        )
    }

    fun onEnvChange(env: Environment) {
        viewModelScope.launch { prefs.saveEnvironment(env) }
        applyEnvDefaults(env)
    }

    fun onEmailChange(value: String) {
        _uiState.value = _uiState.value.copy(email = value)
    }

    fun onPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(password = value)
    }

    fun login() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.value = state.copy(error = "Email et mot de passe requis")
            return
        }
        _uiState.value = state.copy(loading = true, error = null)
        viewModelScope.launch {
            val result = authRepository.login(state.env, state.email.trim(), state.password)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(loading = false, loggedIn = true)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        error = e.message ?: "Échec de la connexion"
                    )
                }
            )
        }
    }
}
