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
        val TOKEN = stringPreferencesKey("jwt_token")
        val ANTHROPIC_KEY = stringPreferencesKey("anthropic_api_key")
        val ENVIRONMENT = stringPreferencesKey("environment")
    }

    val tokenFlow: Flow<String?> = context.dataStore.data
        .map { it[Keys.TOKEN] }

    val anthropicKeyFlow: Flow<String?> = context.dataStore.data
        .map { it[Keys.ANTHROPIC_KEY] }

    val environmentFlow: Flow<Environment> = context.dataStore.data
        .map { Environment.fromName(it[Keys.ENVIRONMENT]) }

    suspend fun getToken(): String? = tokenFlow.first()

    suspend fun getAnthropicKey(): String? = anthropicKeyFlow.first()

    suspend fun getEnvironment(): Environment = environmentFlow.first()

    suspend fun saveToken(token: String) {
        context.dataStore.edit { it[Keys.TOKEN] = token }
    }

    suspend fun clearToken() {
        context.dataStore.edit { it.remove(Keys.TOKEN) }
    }

    suspend fun saveAnthropicKey(key: String) {
        context.dataStore.edit { it[Keys.ANTHROPIC_KEY] = key }
    }

    suspend fun saveEnvironment(env: Environment) {
        context.dataStore.edit { it[Keys.ENVIRONMENT] = env.name }
    }
}
