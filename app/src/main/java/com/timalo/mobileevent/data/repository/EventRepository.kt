package com.timalo.mobileevent.data.repository

import com.timalo.mobileevent.data.network.TiMaloApi
import com.timalo.mobileevent.data.preferences.AppPreferences
import com.timalo.mobileevent.model.Environment
import com.timalo.mobileevent.model.EventForm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Création d'événements via l'API Ti-Malo (multipart). */
class EventRepository(
    private val api: TiMaloApi,
    private val prefs: AppPreferences
) {
    /** Crée un événement unique. Retourne le corps brut de la réponse en succès. */
    suspend fun createEvent(env: Environment, form: EventForm): Result<String> =
        withContext(Dispatchers.IO) {
            val token = prefs.getToken()
                ?: return@withContext Result.failure(IllegalStateException("Non authentifié"))
            api.createEvent(env.baseUrl, token, form)
        }
}
