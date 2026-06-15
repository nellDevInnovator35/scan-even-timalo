package com.timalo.mobileevent

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.gson.Gson
import com.timalo.mobileevent.data.network.AnthropicApi
import com.timalo.mobileevent.data.network.TiMaloApi
import com.timalo.mobileevent.data.preferences.AppPreferences
import com.timalo.mobileevent.data.repository.AiRepository
import com.timalo.mobileevent.data.repository.AuthRepository
import com.timalo.mobileevent.data.repository.EventRepository
import com.timalo.mobileevent.viewmodel.CreateEventViewModel
import com.timalo.mobileevent.viewmodel.LoginViewModel

/**
 * Conteneur de dépendances simple (singleton applicatif).
 * Évite une lib DI lourde pour un périmètre restreint.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val gson = Gson()

    val prefs = AppPreferences(appContext)

    private val tiMaloApi = TiMaloApi(appContext.contentResolver, gson)
    private val anthropicApi = AnthropicApi(gson)

    val authRepository = AuthRepository(tiMaloApi, prefs)
    val eventRepository = EventRepository(tiMaloApi, prefs)
    val aiRepository = AiRepository(anthropicApi, prefs, appContext)
}

/** Factory unique pour tous les ViewModels de l'app. */
class AppViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(LoginViewModel::class.java) ->
                LoginViewModel(container.authRepository, container.prefs) as T

            modelClass.isAssignableFrom(CreateEventViewModel::class.java) ->
                CreateEventViewModel(
                    container.eventRepository,
                    container.aiRepository,
                    container.prefs
                ) as T

            else -> throw IllegalArgumentException("ViewModel inconnu : ${modelClass.name}")
        }
    }
}
