package com.timalo.mobileevent.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.timalo.mobileevent.model.Environment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_prefs")

/**
 * Stockage persistant via Jetpack DataStore Preferences :
 * - JWT token
 * - clé API Anthropic
 * - environnement sélectionné
 */
class AppPreferences(private val context: Context) {

    private object Keys {
        val ANTHROPIC_KEY = stringPreferencesKey("anthropic_api_key")
        val ENVIRONMENT = stringPreferencesKey("environment")
        fun token(env: Environment) = stringPreferencesKey("jwt_token_${env.name}")
    }

    val anthropicKeyFlow: Flow<String?> = context.dataStore.data
        .map { it[Keys.ANTHROPIC_KEY] }

    val environmentFlow: Flow<Environment> = context.dataStore.data
        .map { Environment.fromName(it[Keys.ENVIRONMENT]) }

    /** Flow du token pour un environnement donné. */
    fun tokenFlow(env: Environment): Flow<String?> = context.dataStore.data
        .map { it[Keys.token(env)] }

    suspend fun getToken(env: Environment): String? =
        context.dataStore.data.first()[Keys.token(env)]

    suspend fun getAnthropicKey(): String? = anthropicKeyFlow.first()

    suspend fun getEnvironment(): Environment = environmentFlow.first()

    suspend fun saveToken(env: Environment, token: String) {
        context.dataStore.edit { it[Keys.token(env)] = token }
    }

    suspend fun clearToken(env: Environment) {
        context.dataStore.edit { it.remove(Keys.token(env)) }
    }

    suspend fun saveAnthropicKey(key: String) {
        context.dataStore.edit { it[Keys.ANTHROPIC_KEY] = key }
    }

    suspend fun saveEnvironment(env: Environment) {
        context.dataStore.edit { it[Keys.ENVIRONMENT] = env.name }
    }
}
