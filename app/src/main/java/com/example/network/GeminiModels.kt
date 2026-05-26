package com.example.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @Json(name = "text") val text: String
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @Json(name = "parts") val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiConfig(
    @Json(name = "responseMimeType") val responseMimeType: String? = "application/json",
    @Json(name = "temperature") val temperature: Float? = 0.2f
)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    @Json(name = "contents") val contents: List<GeminiContent>,
    @Json(name = "generationConfig") val generationConfig: GeminiConfig? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @Json(name = "content") val content: GeminiContent
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidate>?
)

// --- Moshi models for the parsed Plagiarism result ---

@JsonClass(generateAdapter = true)
data class PlagiarismSource(
    val name: String,
    val percentage: Int,
    val description: String
)

@JsonClass(generateAdapter = true)
data class RephrasingSuggestion(
    val originalText: String,
    val rewrittenText: String,
    val benefit: String
)

@JsonClass(generateAdapter = true)
data class PlagiarismAnalysisResponse(
    val plagiarismPercentage: Int,
    val explanation: String,
    val sources: List<PlagiarismSource>,
    val suggestions: List<RephrasingSuggestion>
)
