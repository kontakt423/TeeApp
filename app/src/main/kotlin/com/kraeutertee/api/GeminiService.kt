package com.kraeutertee.api

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

// ══════════════════════════════════════════════════════════════════════════════
// Request / Response models
// ══════════════════════════════════════════════════════════════════════════════

data class GeminiRequest(
    @SerializedName("system_instruction")
    val systemInstruction: GeminiSystemInstruction? = null,
    val contents: List<GeminiContent>,
    @SerializedName("generationConfig")
    val generationConfig: GeminiGenerationConfig = GeminiGenerationConfig()
)

data class GeminiSystemInstruction(val parts: List<GeminiPart>)
data class GeminiContent(val role: String, val parts: List<GeminiPart>)
data class GeminiPart(val text: String)

data class GeminiGenerationConfig(
    @SerializedName("maxOutputTokens") val maxOutputTokens: Int = 1024,
    val temperature: Float = 0.7f
)

data class GeminiResponse(
    val candidates: List<GeminiCandidate>?,
    val error: GeminiError?
)

data class GeminiCandidate(
    val content: GeminiContent?,
    @SerializedName("finishReason") val finishReason: String?
)

data class GeminiError(val code: Int, val message: String, val status: String)

// ══════════════════════════════════════════════════════════════════════════════
// Shared result type
// ══════════════════════════════════════════════════════════════════════════════

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String) : ApiResult<Nothing>()
}

// ══════════════════════════════════════════════════════════════════════════════
// GeminiService
//
// Model:  gemini-1.5-flash
//   • Free tier: 15 RPM, 1 million TPM  (vs. 2.0 Flash: 10 RPM, stricter burst)
//   • Very capable for herb descriptions / JSON generation
//   • Built-in retry with exponential backoff for 429 errors
// ══════════════════════════════════════════════════════════════════════════════

class GeminiService(private val apiKey: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val JSON = "application/json; charset=utf-8".toMediaType()

    /**
     * Gemini 1.5 Flash – best free-tier model for this app:
     *   · More generous rate limits than 2.0 Flash
     *   · Fast response times
     *   · Excellent multilingual (German) quality
     */
    private val MODEL = "gemini-1.5-flash"
    private val BASE_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent"

    // ── Core request with automatic retry on 429 ───────────────────────────────

    private suspend fun generate(
        systemPrompt: String?,
        contents: List<GeminiContent>,
        maxTokens: Int = 1024,
        maxRetries: Int = 3
    ): ApiResult<String> = withContext(Dispatchers.IO) {

        if (apiKey.isBlank()) {
            return@withContext ApiResult.Error(
                "Kein API-Schlüssel konfiguriert. Bitte in den Einstellungen (⚙️) eingeben."
            )
        }

        val requestBody = GeminiRequest(
            systemInstruction = systemPrompt?.let {
                GeminiSystemInstruction(listOf(GeminiPart(it)))
            },
            contents = contents,
            generationConfig = GeminiGenerationConfig(maxOutputTokens = maxTokens)
        )

        val body = gson.toJson(requestBody).toRequestBody(JSON)

        var lastError = ""
        var delayMs   = 2_000L   // start with 2 s, doubles on each retry

        repeat(maxRetries) { attempt ->
            if (attempt > 0) {
                delay(delayMs)
                delayMs *= 2   // exponential back-off: 2 s → 4 s → 8 s
            }

            try {
                val request = Request.Builder()
                    .url("$BASE_URL?key=$apiKey")
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                val responseText = response.body?.string() ?: ""

                when {
                    response.isSuccessful -> {
                        val parsed = gson.fromJson(responseText, GeminiResponse::class.java)
                        val text   = parsed.candidates
                            ?.firstOrNull()
                            ?.content
                            ?.parts
                            ?.firstOrNull()
                            ?.text

                        return@withContext if (text != null) {
                            ApiResult.Success(text)
                        } else {
                            ApiResult.Error("Leere Antwort vom Server.")
                        }
                    }

                    response.code == 429 -> {
                        // Rate-limited – retry after back-off
                        val parsed = runCatching {
                            gson.fromJson(responseText, GeminiResponse::class.java)
                        }.getOrNull()
                        lastError = parsed?.error?.message
                            ?: "Rate-Limit erreicht (429). Bitte kurz warten."
                        // Continue to next retry
                    }

                    else -> {
                        val parsed = runCatching {
                            gson.fromJson(responseText, GeminiResponse::class.java)
                        }.getOrNull()
                        lastError = parsed?.error?.message
                            ?: "Fehler ${response.code}: $responseText"
                        return@withContext ApiResult.Error(lastError)
                    }
                }

            } catch (e: IOException) {
                lastError = "Netzwerkfehler: ${e.localizedMessage ?: e.message}"
                // Network errors also get retried
            }
        }

        ApiResult.Error("$lastError (nach $maxRetries Versuchen)")
    }

    // ── Convenience wrappers ───────────────────────────────────────────────────

    private suspend fun chat(
        systemPrompt: String,
        userMessage: String,
        maxTokens: Int = 1024
    ): ApiResult<String> = generate(
        systemPrompt = systemPrompt,
        contents     = listOf(GeminiContent("user", listOf(GeminiPart(userMessage)))),
        maxTokens    = maxTokens
    )

    // ── Domain methods ─────────────────────────────────────────────────────────

    suspend fun explainHerb(herbName: String, latinName: String): ApiResult<String> = chat(
        systemPrompt = """Du bist ein erfahrener Kräuterkundler und Naturheilkundler.
Du erklärst Kräuter auf Deutsch, klar, verständlich und praxisnah.
Struktur: 1) Wirkung & Inhaltsstoffe, 2) Verwendung als Tee, 3) Besonderheiten & Tipps.
Maximal 300 Wörter. Keine Markdown-Überschriften.""",
        userMessage  = "Erkläre mir das Kraut '$herbName' ($latinName) für die Teezubereitung.",
        maxTokens    = 512
    )

    suspend fun suggestTeaBlends(availableHerbs: List<String>): ApiResult<String> = chat(
        systemPrompt = """Du bist ein Teemeister der Kräuterküche.
Antworte auf Deutsch. Schlage konkrete Teemischungen vor mit Verhältnis, Wirkung, Zubereitungstipp.
Maximal 400 Wörter.""",
        userMessage  = """Mir stehen folgende Kräuter zur Verfügung: ${availableHerbs.joinToString(", ")}.
Empfiehl 3–5 harmonische Teemischungen. Gib jeweils Name, Zutaten mit Verhältnis, Wirkung und Tipp.""",
        maxTokens    = 700
    )

    /**
     * Generates a complete herb profile as JSON.
     * Returns a valid JSON object string with all HerbInfo fields.
     */
    suspend fun generateHerbInfo(herbName: String): ApiResult<String> = chat(
        systemPrompt = """Du bist ein Kräuterexperte. Antworte NUR mit einem gültigen JSON-Objekt, kein Text davor oder danach, keine Markdown-Backticks.
Das JSON muss folgende Felder enthalten:
{
  "latinName": "...",
  "emoji": "ein passendes Emoji",
  "shortDescription": "1-2 Sätze auf Deutsch",
  "harvestPart": "Blätter|Blüten|Wurzel|Samen|Früchte",
  "harvestMonths": [6,7,8],
  "harvestTips": "Ernte-Tipps auf Deutsch",
  "dryingDays": 7,
  "dryingTempMax": 40,
  "dryingMethod": "Methode auf Deutsch",
  "storageMonths": 12,
  "teaInfo": "Zubereitungshinweis auf Deutsch",
  "brewingTempC": 90,
  "brewingMinutes": 7,
  "effects": ["Wirkung1", "Wirkung2"],
  "gardenTips": "Gartentipps auf Deutsch",
  "warningsDE": "Hinweise/Warnungen oder leerer String",
  "category": "Beruhigung|Erkältung|Verdauung|Schlaf|Immunsystem|Entgiftung|Kreislauf|Stimmung|Sonstige"
}""",
        userMessage  = "Erstelle ein vollständiges Kräuterprofil für: $herbName",
        maxTokens    = 800
    )

    /**
     * Multi-turn chat. Handles "user"/"assistant" → "user"/"model" mapping for Gemini.
     */
    suspend fun getChatResponse(
        messages: List<Pair<String, String>>,
        herbContext: String = ""
    ): ApiResult<String> {

        if (apiKey.isBlank()) {
            return ApiResult.Error(
                "Kein API-Schlüssel konfiguriert. Bitte in den Einstellungen (⚙️) eingeben."
            )
        }

        val systemPrompt = buildString {
            append("""Du bist ein freundlicher und sachkundiger Kräuterexperte und Teemeister.
Du hilfst beim Sammeln, Trocknen, Zubereiten und Kombinieren von Kräutertees.
Antworte stets auf Deutsch, freundlich, praxisnah und klar.
Bei medizinischen Fragen weise auf einen Arzt hin.""")
            if (herbContext.isNotBlank()) append("\nVerfügbare Kräuter des Nutzers: $herbContext")
        }

        // Gemini uses "model" instead of "assistant"
        val contents = messages
            .dropWhile { it.first == "assistant" }   // must start with "user"
            .map { (role, content) ->
                GeminiContent(
                    role  = if (role == "assistant") "model" else "user",
                    parts = listOf(GeminiPart(content))
                )
            }

        return generate(systemPrompt, contents, maxTokens = 1024)
    }
}
