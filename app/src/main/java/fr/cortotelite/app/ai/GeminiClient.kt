package fr.cortotelite.app.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicInteger

/**
 * Client Gemini avec multi-clés et rotation automatique.
 * Les clés sont fournies sous forme de liste (séparées par | ou \n).
 * En cas d'erreur 429 / 403 / 500, la clé suivante est essayée.
 */
class GeminiClient(
    private val keysRaw: String,
    private val model: String = "gemini-2.0-flash"
) {
    private val keys: List<String> = keysRaw
        .split('|', '\n', ';')
        .map { it.trim() }
        .filter { it.isNotBlank() && it.startsWith("AIza") }

    private val index = AtomicInteger(0)

    val hasKeys: Boolean get() = keys.isNotEmpty()
    val keyCount: Int get() = keys.size

    private fun nextKey(): String? {
        if (keys.isEmpty()) return null
        val i = index.getAndIncrement() % keys.size
        return keys[i]
    }

    /**
     * Génère du texte via Gemini.
     * @return texte généré ou message d'erreur préfixé par "ERREUR:"
     */
    suspend fun generate(
        prompt: String,
        systemInstruction: String? = null,
        temperature: Double = 0.7,
        maxTokens: Int = 2048
    ): String = withContext(Dispatchers.IO) {
        if (keys.isEmpty()) return@withContext "ERREUR: Aucune clé API Gemini configurée (Société → Clés Gemini)."

        var lastError = "ERREUR: Toutes les clés ont échoué."
        val attempts = keys.size.coerceAtMost(5)

        repeat(attempts) {
            val key = nextKey() ?: return@withContext lastError
            try {
                val result = callApi(key, prompt, systemInstruction, temperature, maxTokens)
                if (!result.startsWith("ERREUR:")) return@withContext result
                lastError = result
                // 429 / quota → on tourne
                if (result.contains("429") || result.contains("RESOURCE_EXHAUSTED") ||
                    result.contains("403") || result.contains("quota")
                ) {
                    // continue rotation
                } else {
                    // autre erreur : on s'arrête
                    return@withContext result
                }
            } catch (e: Exception) {
                lastError = "ERREUR: ${e.message}"
            }
        }
        lastError
    }

    private fun callApi(
        apiKey: String,
        prompt: String,
        systemInstruction: String?,
        temperature: Double,
        maxTokens: Int
    ): String {
        val url = URL(
            "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
        )
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            doOutput = true
            connectTimeout = 30_000
            readTimeout = 60_000
        }

        val body = JSONObject().apply {
            if (!systemInstruction.isNullOrBlank()) {
                put("system_instruction", JSONObject().put(
                    "parts", JSONArray().put(JSONObject().put("text", systemInstruction))
                ))
            }
            put("contents", JSONArray().put(
                JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))
            ))
            put("generationConfig", JSONObject().apply {
                put("temperature", temperature)
                put("maxOutputTokens", maxTokens)
            })
        }

        OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val response = BufferedReader(InputStreamReader(stream)).use { it.readText() }

        if (code !in 200..299) {
            return "ERREUR: HTTP $code — ${response.take(300)}"
        }

        return try {
            val json = JSONObject(response)
            val candidates = json.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                "ERREUR: Réponse vide de Gemini."
            } else {
                candidates.getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
                    .trim()
            }
        } catch (e: Exception) {
            "ERREUR: Parsing — ${e.message}"
        }
    }

    // ── Helpers métier Cortot Élite ──────────────────────────

    suspend fun generateQuoteDescription(
        clientName: String,
        powerKwc: Double,
        notes: String = ""
    ): String {
        val prompt = """
            Rédige une description professionnelle courte (2-4 phrases) pour un devis d'entretien
            et contrôle électrique d'installation photovoltaïque.
            Client : $clientName
            Puissance : ${if (powerKwc > 0) "$powerKwc kWc" else "non renseignée"}
            Notes : ${notes.ifBlank { "aucune" }}
            Style : professionnel, clair, en français. Pas de formules marketing excessives.
            Uniquement le texte de description, sans titre ni signature.
        """.trimIndent()
        return generate(prompt, temperature = 0.5)
    }

    suspend fun generateEmailBody(
        kind: String, // "devis" ou "facture"
        number: String,
        clientName: String,
        totalTtc: String,
        companyName: String
    ): String {
        val prompt = """
            Rédige un e-mail professionnel court en français pour envoyer un $kind.
            Numéro : $number
            Client : $clientName
            Montant TTC : $totalTtc
            Société : $companyName
            Ton poli et professionnel. Inclure une formule de politesse.
            Uniquement le corps de l'e-mail (sans objet).
        """.trimIndent()
        return generate(prompt, temperature = 0.4)
    }

    suspend fun analyzeLateInvoices(summary: String): String {
        val prompt = """
            Tu es un assistant de gestion pour une société de maintenance photovoltaïque.
            Analyse le résumé suivant des factures en retard / en attente et propose
            3 actions concrètes prioritaires (relance, priorité, etc.).
            
            $summary
            
            Réponds en français, de façon concise (liste à puces).
        """.trimIndent()
        return generate(prompt, temperature = 0.3)
    }

    suspend fun suggestMaintenance(powerKwc: Double, inverter: String, ageYears: Int = 0): String {
        val prompt = """
            Conseille les points de contrôle prioritaires pour l'entretien électrique
            d'une installation photovoltaïque de ${powerKwc} kWc
            (onduleur : ${inverter.ifBlank { "non précisé" }},
            âge approximatif : ${if (ageYears > 0) "$ageYears ans" else "inconnu"}).
            Liste 5 à 8 points techniques concrets. Français, style checklist.
        """.trimIndent()
        return generate(prompt, temperature = 0.4)
    }
}
