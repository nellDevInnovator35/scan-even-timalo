package com.timalo.mobileevent.data.repository

import android.content.Context
import android.net.Uri
import com.timalo.mobileevent.data.network.AnthropicApi
import com.timalo.mobileevent.data.preferences.AppPreferences
import com.timalo.mobileevent.model.AiEnrichment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Enrichissement IA via l'API Anthropic, clé lue dans DataStore. */
class AiRepository(
    private val api: AnthropicApi,
    private val prefs: AppPreferences,
    private val context: Context
) {
    suspend fun enrich(
        imageUri: Uri?,
        title: String,
        location: String,
        startDate: String
    ): Result<AiEnrichment> = withContext(Dispatchers.IO) {
        val key = prefs.getAnthropicKey().orEmpty()
        val imageBytes = imageUri?.let {
            context.contentResolver.openInputStream(it)?.use { s -> s.readBytes() }
        }
        val imageMime = imageUri?.let {
            context.contentResolver.getType(it) ?: "image/jpeg"
        }
        api.enrich(key, imageBytes, imageMime, title, location, startDate)
    }
}
