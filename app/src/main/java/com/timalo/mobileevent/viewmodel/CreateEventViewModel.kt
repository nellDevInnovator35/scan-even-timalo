package com.timalo.mobileevent.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timalo.mobileevent.data.preferences.AppPreferences
import com.timalo.mobileevent.data.repository.AiRepository
import com.timalo.mobileevent.data.repository.EventRepository
import com.timalo.mobileevent.model.Environment
import com.timalo.mobileevent.model.EventForm
import com.timalo.mobileevent.model.EventOccurrence
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class CreateEventUiState(
    val form: EventForm = EventForm(),
    val env: Environment = Environment.PREPROD,
    val submitting: Boolean = false,
    val submittingEnv: Environment? = null, // env en cours d'envoi
    val aiLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val needsLoginForEnv: Environment? = null, // demande de login pour cet env
    // Multi-jours
    val occurrences: List<EventOccurrence> = emptyList(),
    val showMultiDayReview: Boolean = false,
    val multiDayResult: String? = null
)

class CreateEventViewModel(
    private val eventRepository: EventRepository,
    private val aiRepository: AiRepository,
    private val prefs: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateEventUiState())
    val uiState: StateFlow<CreateEventUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(env = prefs.getEnvironment())
        }
    }

    fun refreshEnv() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(env = prefs.getEnvironment())
        }
    }

    private fun updateForm(transform: (EventForm) -> EventForm) {
        _uiState.value = _uiState.value.copy(form = transform(_uiState.value.form))
    }

    fun onTitleChange(v: String) = updateForm { it.copy(title = v) }
    fun onDescriptionChange(v: String) = updateForm { it.copy(description = v) }
    fun onLocationChange(v: String) = updateForm { it.copy(location = v) }
    fun onOrganizerNameChange(v: String) = updateForm { it.copy(organizerName = v) }
    fun onOrganizerEmailChange(v: String) = updateForm { it.copy(organizerEmail = v) }
    fun onPriceChange(v: String) = updateForm { it.copy(price = v) }
    fun onMaxAttendeesChange(v: String) = updateForm { it.copy(maxAttendees = v.filter { c -> c.isDigit() }) }
    fun onSourceUrlChange(v: String) = updateForm { it.copy(sourceUrl = v) }
    fun onStartDateChange(iso: String) = updateForm { it.copy(startDate = iso) }
    fun onEndDateChange(iso: String) = updateForm { it.copy(endDate = iso) }
    fun onImageChange(uri: Uri?) = updateForm { it.copy(imageUri = uri) }

    fun toggleType(type: String) {
        updateForm { form ->
            val types = if (type in form.types) form.types - type else form.types + type
            form.copy(types = types)
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, successMessage = null, multiDayResult = null)
    }

    fun clearNeedsLogin() {
        _uiState.value = _uiState.value.copy(needsLoginForEnv = null)
    }

    /** Validation minimale des champs obligatoires. */
    private fun validate(form: EventForm): String? {
        return when {
            form.title.isBlank() -> "Le titre est obligatoire"
            form.startDate.isBlank() -> "La date de début est obligatoire"
            form.endDate.isBlank() -> "La date de fin est obligatoire"
            form.organizerName.isBlank() -> "L'organisateur est obligatoire"
            form.organizerEmail.isBlank() -> "L'email de l'organisateur est obligatoire"
            else -> null
        }
    }

    /**
     * Soumet l'événement vers l'environnement cible.
     * Vérifie le token ; si absent, demande un login via needsLoginForEnv.
     * Si multi-jours, bascule vers l'écran de revue.
     */
    fun submitToEnv(targetEnv: Environment) {
        val form = _uiState.value.form
        validate(form)?.let {
            _uiState.value = _uiState.value.copy(error = it)
            return
        }
        viewModelScope.launch {
            val token = prefs.getToken(targetEnv)
            if (token.isNullOrBlank()) {
                _uiState.value = _uiState.value.copy(needsLoginForEnv = targetEnv)
                return@launch
            }
            if (isMultiDay(form.startDate, form.endDate)) {
                val occurrences = generateOccurrences(form.startDate, form.endDate)
                _uiState.value = _uiState.value.copy(
                    occurrences = occurrences,
                    showMultiDayReview = true,
                    submittingEnv = targetEnv,
                    error = null
                )
                return@launch
            }
            createSingle(form, targetEnv)
        }
    }

    private fun createSingle(form: EventForm, targetEnv: Environment) {
        _uiState.value = _uiState.value.copy(submitting = true, submittingEnv = targetEnv, error = null, successMessage = null)
        viewModelScope.launch {
            val result = eventRepository.createEvent(targetEnv, form)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        submitting = false,
                        submittingEnv = null,
                        successMessage = "Envoyé sur ${targetEnv.displayName} ✅"
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        submitting = false,
                        submittingEnv = null,
                        error = e.message ?: "Échec de la création"
                    )
                }
            )
        }
    }

    // --- Multi-jours ---

    fun dismissMultiDayReview() {
        _uiState.value = _uiState.value.copy(showMultiDayReview = false)
    }

    fun updateOccurrence(id: Long, startIso: String, endIso: String) {
        val updated = _uiState.value.occurrences.map {
            if (it.id == id) it.copy(startDate = startIso, endDate = endIso) else it
        }
        _uiState.value = _uiState.value.copy(occurrences = updated)
    }

    /** Crée séquentiellement toutes les occurrences ; rapporte X/N. */
    fun confirmMultiDay() {
        val base = _uiState.value.form
        val occurrences = _uiState.value.occurrences
        val targetEnv = _uiState.value.submittingEnv ?: _uiState.value.env
        if (occurrences.isEmpty()) return

        _uiState.value = _uiState.value.copy(submitting = true, error = null, multiDayResult = null)
        viewModelScope.launch {
            var created = 0
            for (occ in occurrences) {
                val form = base.copy(startDate = occ.startDate, endDate = occ.endDate)
                val result = eventRepository.createEvent(targetEnv, form)
                if (result.isSuccess) created++
            }
            _uiState.value = _uiState.value.copy(
                submitting = false,
                showMultiDayReview = false,
                multiDayResult = "$created/${occurrences.size} événements créés",
                successMessage = if (created == occurrences.size) "Tous les événements créés ✅" else null,
                error = if (created < occurrences.size) "${occurrences.size - created} échec(s)" else null
            )
        }
    }

    // --- Enrichissement IA ---

    fun enrichWithAi() {
        val form = _uiState.value.form
        if (form.imageUri == null && form.title.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Charge une image ou saisis un titre pour l'IA")
            return
        }
        _uiState.value = _uiState.value.copy(aiLoading = true, error = null)
        viewModelScope.launch {
            val result = aiRepository.enrich(form.imageUri, form.title, form.location, form.startDate)
            result.fold(
                onSuccess = { enrichment ->
                    updateForm { current ->
                        current.copy(
                            title = enrichment.title?.takeIf { it.isNotBlank() } ?: current.title,
                            description = enrichment.description?.takeIf { it.isNotBlank() } ?: current.description,
                            organizerName = enrichment.organizerName?.takeIf { it.isNotBlank() } ?: current.organizerName,
                            organizerEmail = enrichment.organizerEmail?.takeIf { it.isNotBlank() } ?: current.organizerEmail,
                            startDate = enrichment.startDate?.takeIf { it.isNotBlank() } ?: current.startDate,
                            endDate = enrichment.endDate?.takeIf { it.isNotBlank() } ?: current.endDate,
                            types = enrichment.types?.takeIf { it.isNotEmpty() } ?: current.types
                        )
                    }
                    _uiState.value = _uiState.value.copy(aiLoading = false)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        aiLoading = false,
                        error = e.message ?: "Échec de l'enrichissement IA"
                    )
                }
            )
        }
    }

    companion object {
        private val ISO_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.FRANCE)

        /** Formate un Calendar SANS conversion UTC : heure locale telle quelle. */
        fun toLocalISO(cal: Calendar): String = ISO_FORMAT.format(cal.time)

        fun parseLocalISO(iso: String): Calendar? = try {
            Calendar.getInstance().apply { time = ISO_FORMAT.parse(iso)!! }
        } catch (e: Exception) {
            null
        }

        /** True si start et end tombent sur des jours calendaires différents. */
        fun isMultiDay(startIso: String, endIso: String): Boolean {
            val start = parseLocalISO(startIso) ?: return false
            val end = parseLocalISO(endIso) ?: return false
            return start.get(Calendar.YEAR) != end.get(Calendar.YEAR) ||
                start.get(Calendar.DAY_OF_YEAR) != end.get(Calendar.DAY_OF_YEAR)
        }

        /**
         * Génère une occurrence par jour entre start et end (inclus),
         * en conservant l'heure de start et l'heure de end chaque jour.
         */
        fun generateOccurrences(startIso: String, endIso: String): List<EventOccurrence> {
            val start = parseLocalISO(startIso) ?: return emptyList()
            val end = parseLocalISO(endIso) ?: return emptyList()

            val startHour = start.get(Calendar.HOUR_OF_DAY)
            val startMin = start.get(Calendar.MINUTE)
            val endHour = end.get(Calendar.HOUR_OF_DAY)
            val endMin = end.get(Calendar.MINUTE)

            val occurrences = mutableListOf<EventOccurrence>()
            val cursor = Calendar.getInstance().apply {
                time = start.time
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val lastDay = Calendar.getInstance().apply {
                time = end.time
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            var id = 0L
            while (!cursor.after(lastDay)) {
                val dayStart = (cursor.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, startHour)
                    set(Calendar.MINUTE, startMin)
                }
                val dayEnd = (cursor.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, endHour)
                    set(Calendar.MINUTE, endMin)
                }
                occurrences.add(
                    EventOccurrence(
                        id = id++,
                        startDate = toLocalISO(dayStart),
                        endDate = toLocalISO(dayEnd)
                    )
                )
                cursor.add(Calendar.DAY_OF_YEAR, 1)
            }
            return occurrences
        }
    }
}
