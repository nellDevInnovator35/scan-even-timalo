package com.timalo.mobileevent.data.repository

import com.timalo.mobileevent.data.network.TiMaloApi
import com.timalo.mobileevent.data.preferences.AppPreferences
import com.timalo.mobileevent.model.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Gère le login et la persistance du token JWT. */
class AuthRepository(
    private val api: TiMaloApi,
    private val prefs: AppPreferences
) {
    /** Tente une connexion sur l'env donné ; stocke le token en cas de succès. */
    suspend fun login(env: Environment, email: String, password: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            api.login(env.baseUrl, email, password).fold(
                onSuccess = { response ->
                    val token = response.token
                    if (token.isNullOrBlank()) {
                        Result.failure(IllegalStateException("Token absent de la réponse"))
                    } else {
                        prefs.saveToken(token)
                        Result.success(Unit)
                    }
                },
                onFailure = { Result.failure(it) }
            )
        }

    suspend fun logout() = prefs.clearToken()

    suspend fun currentToken(): String? = prefs.getToken()
}
