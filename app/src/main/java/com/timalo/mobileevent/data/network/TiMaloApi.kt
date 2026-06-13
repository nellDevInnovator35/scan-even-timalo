package com.timalo.mobileevent.data.network

import android.content.ContentResolver
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.timalo.mobileevent.model.EventForm
import com.timalo.mobileevent.model.LoginResponse
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Client HTTP pour l'API Ti-Malo (OkHttp + Gson).
 * baseUrl est fourni à l'appel pour suivre l'environnement courant.
 */
class TiMaloApi(
    private val contentResolver: ContentResolver,
    private val gson: Gson = Gson()
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    /** POST /api/auth/login → { token, user } ou { token }. */
    fun login(baseUrl: String, email: String, password: String): Result<LoginResponse> {
        return try {
            val body = JsonObject().apply {
                addProperty("email", email)
                addProperty("password", password)
            }.toString().toRequestBody(jsonMedia)

            val request = Request.Builder()
                .url("$baseUrl/api/auth/login")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return Result.failure(IOException("Login échoué (${response.code}): $raw"))
                }
                val parsed = gson.fromJson(raw, LoginResponse::class.java)
                if (parsed?.token.isNullOrBlank()) {
                    Result.failure(IOException("Réponse sans token: $raw"))
                } else {
                    Result.success(parsed)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * POST /api/events en multipart/form-data.
     * Chaque champ texte est une part séparée ; types[] = une part par valeur.
     */
    fun createEvent(
        baseUrl: String,
        token: String,
        form: EventForm
    ): Result<String> {
        return try {
            val builder = MultipartBody.Builder().setType(MultipartBody.FORM)

            builder.addFormDataPart("title", form.title)
            builder.addFormDataPart("description", form.description)
            builder.addFormDataPart("location", form.location)
            builder.addFormDataPart("start_date", form.startDate)
            builder.addFormDataPart("end_date", form.endDate)
            builder.addFormDataPart("organizer_name", form.organizerName)
            builder.addFormDataPart("organizer_email", form.organizerEmail)
            builder.addFormDataPart("price", form.price)
            builder.addFormDataPart("source_url", form.sourceUrl)
            builder.addFormDataPart("color", form.color)

            if (form.maxAttendees.isNotBlank()) {
                builder.addFormDataPart("max_attendees", form.maxAttendees.trim())
            }

            form.source?.let { builder.addFormDataPart("source", it) }

            // types[] : une part par valeur
            form.types.forEach { type ->
                builder.addFormDataPart("types[]", type)
            }

            // image en part "image" avec son mime type
            form.imageUri?.let { uri ->
                val imagePart = buildImagePart(uri)
                if (imagePart != null) {
                    builder.addFormDataPart("image", imagePart.first, imagePart.second)
                }
            }

            val request = Request.Builder()
                .url("$baseUrl/api/events")
                .addHeader("Authorization", "Bearer $token")
                .post(builder.build())
                .build()

            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    Result.success(raw)
                } else {
                    Result.failure(IOException("Création échouée (${response.code}): $raw"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildImagePart(uri: Uri): Pair<String, RequestBody>? {
        val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
        val mime = contentResolver.getType(uri) ?: "image/jpeg"
        val extension = when (mime) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
        val fileName = "upload_${System.currentTimeMillis()}.$extension"
        val requestBody = bytes.toRequestBody(mime.toMediaTypeOrNull())
        return fileName to requestBody
    }
}
