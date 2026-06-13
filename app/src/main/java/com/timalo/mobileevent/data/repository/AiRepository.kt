package com.timalo.mobileevent.data.repository

import com.timalo.mobileevent.data.network.AnthropicApi
import com.timalo.mobileevent.data.preferences.AppPreferences
import com.timalo.mobileevent.model.AiEnrichment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Enrichissement IA via l'API Anthropic, clé lue dans DataStore. */
class AiRepository(
    private val api: AnthropicApi,
    private val prefs: AppPreferences
) {
    suspend fun enrich(title: String, location: String, startDate: String): Result<AiEnrichment> =
        withContext(Dispatchers.IO) {
            val key = prefs.getAnthropicKey().orEmpty()
            api.enrich(key, title, location, startDate)
        }
}
