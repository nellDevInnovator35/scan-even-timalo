package com.timalo.mobileevent.model

import android.net.Uri

/** Environnement cible de l'API. */
enum class Environment(val displayName: String, val baseUrl: String) {
    PREPROD("PRÉPROD", "https://preprod.ti-malo.fr"),
    PROD("PROD", "https://ti-malo.fr");

    companion object {
        fun fromName(name: String?): Environment =
            entries.firstOrNull { it.name == name } ?: PREPROD
    }
}

/** Credentials par défaut proposés selon l'environnement (jamais hardcodés en logique métier). */
data class DefaultCredentials(val email: String, val password: String)

fun Environment.defaultCredentials(): DefaultCredentials = when (this) {
    Environment.PREPROD -> DefaultCredentials(
        com.timalo.mobileevent.BuildConfig.PREPROD_EMAIL,
        com.timalo.mobileevent.BuildConfig.PREPROD_PASSWORD
    )
    Environment.PROD -> DefaultCredentials(
        com.timalo.mobileevent.BuildConfig.PROD_EMAIL,
        com.timalo.mobileevent.BuildConfig.PROD_PASSWORD
    )
}

/** Types d'événements supportés par la plateforme. */
object EventTypes {
    val ALL = listOf(
        "touristique",
        "culturel",
        "sportif",
        "kid_friendly",
        "pluvieux",
        "apprendre",
        "ecologie"
    )

    fun label(type: String): String = when (type) {
        "touristique" -> "Touristique"
        "culturel" -> "Culturel"
        "sportif" -> "Sportif"
        "kid_friendly" -> "Enfants"
        "pluvieux" -> "Pluvieux"
        "apprendre" -> "Apprendre"
        "ecologie" -> "Écologie"
        else -> type
    }
}

/** Réponse de login : token obligatoire, user optionnel. */
data class LoginResponse(
    val token: String?,
    val user: ApiUser?
)

data class ApiUser(
    val id: String?,
    val name: String?,
    val email: String?,
    val role: String?
)

/** Modèle d'un événement à créer / éditer. */
data class EventForm(
    val title: String = "",
    val description: String = "",
    val location: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val organizerName: String = "",
    val organizerEmail: String = "contact@ti-malo.fr",
    val price: String = "",
    val maxAttendees: String = "",
    val sourceUrl: String = "",
    val types: List<String> = emptyList(),
    val imageUri: Uri? = null,
    val color: String = "#6366f1"
) {
    /** "flyer" si une image est présente, sinon null (champ source non envoyé). */
    val source: String?
        get() = if (imageUri != null) "flyer" else null
}

/** Une occurrence individuelle pour un événement multi-jours. */
data class EventOccurrence(
    val id: Long,
    val startDate: String,
    val endDate: String
)

/** Résultat de l'enrichissement IA. */
data class AiEnrichment(
    val description: String?,
    val organizerName: String?,
    val types: List<String>?
)
