package com.timalo.mobileevent.data.network

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.timalo.mobileevent.model.AiEnrichment
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Client pour l'API Anthropic (enrichissement IA d'un événement).
 * Appelle POST https://api.anthropic.com/v1/messages avec l'outil web_search.
 */
class AnthropicApi(private val gson: Gson = Gson()) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    private companion object {
        const val URL = "https://api.anthropic.com/v1/messages"
        const val MODEL = "claude-sonnet-4-6"
        const val ANTHROPIC_VERSION = "2023-06-01"
        const val WEB_SEARCH_BETA = "web-search-2025-03-05"
        val VALID_TYPES = listOf(
            "touristique", "culturel", "sportif",
            "kid_friendly", "pluvieux", "apprendre", "ecologie"
        )
    }

    /**
     * Demande à Claude une description, un organisateur et des types pour l'événement.
     * @return AiEnrichment ou erreur.
     */
    fun enrich(
        apiKey: String,
        title: String,
        location: String,
        startDate: String
    ): Result<AiEnrichment> {
        if (apiKey.isBlank()) {
            return Result.failure(IOException("Clé API Anthropic manquante. Configurez-la dans Réglages."))
        }
        return try {
            val body = buildRequestBody(title, location, startDate)

            val request = Request.Builder()
                .url(URL)
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", ANTHROPIC_VERSION)
                .addHeader("anthropic-beta", WEB_SEARCH_BETA)
                .addHeader("content-type", "application/json")
                .post(body.toString().toRequestBody(jsonMedia))
                .build()

            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return Result.failure(IOException("Anthropic (${response.code}): $raw"))
                }
                parseEnrichment(raw)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildRequestBody(title: String, location: String, startDate: String): JsonObject {
        val prompt = buildString {
            append("Tu enrichis une fiche d'événement local à Saint-Malo (France).\n")
            if (title.isNotBlank()) append("Indice titre : $title\n")
            if (location.isNotBlank()) append("Lieu : $location\n")
            if (startDate.isNotBlank()) append("Date approximative : $startDate\n")
            append("\nRecherche sur le web des informations fiables sur cet événement.\n\n")
            append("Réponds UNIQUEMENT avec un objet JSON valide, sans texte autour, ")
            append("avec exactement ces clés (null si information introuvable) :\n")
            append("{\n")
            append("  \"title\": \"titre officiel exact de l'événement\",\n")
            append("  \"description\": \"un paragraphe descriptif clair en français\",\n")
            append("  \"organizer_name\": \"le nom de l'organisateur ou de l'association\",\n")
            append("  \"organizer_email\": \"email de contact si trouvé, sinon null\",\n")
            append("  \"start_date\": \"date et heure de début au format yyyy-MM-dd'T'HH:mm:ss, heure locale française, null si inconnue\",\n")
            append("  \"end_date\": \"date et heure de fin au format yyyy-MM-dd'T'HH:mm:ss, null si inconnue\",\n")
            append("  \"types\": [\"...\"]\n")
            append("}\n")
            append("Les valeurs autorisées pour types sont exactement : ")
            append(VALID_TYPES.joinToString(", "))
            append(". Choisis un à trois types pertinents.")
        }

        // Outil web_search
        val webSearchTool = JsonObject().apply {
            addProperty("type", "web_search_20250305")
            addProperty("name", "web_search")
            addProperty("max_uses", 3)
        }
        val tools = JsonArray().apply { add(webSearchTool) }

        val userMessage = JsonObject().apply {
            addProperty("role", "user")
            addProperty("content", prompt)
        }
        val messages = JsonArray().apply { add(userMessage) }

        return JsonObject().apply {
            addProperty("model", MODEL)
            addProperty("max_tokens", 1024)
            add("tools", tools)
            add("messages", messages)
        }
    }

    /** Extrait le dernier bloc text de content[] et parse le JSON qu'il contient. */
    private fun parseEnrichment(raw: String): Result<AiEnrichment> {
        return try {
            val root = JsonParser.parseString(raw).asJsonObject
            val content = root.getAsJsonArray("content")
                ?: return Result.failure(IOException("Réponse Anthropic sans content"))

            var lastText: String? = null
            for (element in content) {
                val obj = element.asJsonObject
                if (obj.get("type")?.asString == "text") {
                    lastText = obj.get("text")?.asString
                }
            }

            if (lastText.isNullOrBlank()) {
                return Result.failure(IOException("Aucun bloc text dans la réponse"))
            }

            val jsonText = extractJsonObject(lastText)
                ?: return Result.failure(IOException("Pas de JSON exploitable dans la réponse IA"))

            val parsed = JsonParser.parseString(jsonText).asJsonObject
            val title = parsed.get("title")?.takeIf { !it.isJsonNull }?.asString
            val description = parsed.get("description")?.takeIf { !it.isJsonNull }?.asString
            val organizerName = parsed.get("organizer_name")?.takeIf { !it.isJsonNull }?.asString
            val organizerEmail = parsed.get("organizer_email")?.takeIf { !it.isJsonNull }?.asString
            val startDate = parsed.get("start_date")?.takeIf { !it.isJsonNull }?.asString
            val endDate = parsed.get("end_date")?.takeIf { !it.isJsonNull }?.asString
            val types = parsed.getAsJsonArray("types")?.mapNotNull { it.asString }
                ?.filter { it in VALID_TYPES }

            Result.success(AiEnrichment(title, description, organizerName, organizerEmail, startDate, endDate, types))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Isole le premier objet JSON {...} d'une chaîne (gère le texte parasite éventuel). */
    private fun extractJsonObject(text: String): String? {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        return if (start >= 0 && end > start) text.substring(start, end + 1) else null
    }
}
