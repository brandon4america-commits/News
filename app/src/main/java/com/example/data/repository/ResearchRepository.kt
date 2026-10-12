package com.example.data.repository

import com.example.BuildConfig
import com.example.data.model.ArticleEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// --- Gemini API Models ---
@Serializable
data class GenerateContentRequest(
    val contents: List<Content>
)

@Serializable
data class Content(val parts: List<Part>)

@Serializable
data class Part(val text: String)

@Serializable
data class GenerateContentResponse(val candidates: List<Candidate>)

@Serializable
data class Candidate(val content: Content)

// --- Retrofit Interface ---
interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

// --- Repository Implementation ---
class ResearchRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val service = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/")
        .client(client)
        .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(GeminiApiService::class.java)

    suspend fun generateInsight(title: String, description: String): String = withContext(Dispatchers.IO) {
        try {
            // val prompt = "Provide a 2-sentence 'Smart Insight' about the relationship between this news and broader tech trends: $title - $description"
            // val request = GenerateContentRequest(listOf(Content(listOf(Part(prompt)))))
            // val response = service.generateContent(BuildConfig.GEMINI_API_KEY, request)
            // response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No insight available."
            "Insight feature requires API configuration."
        } catch (e: Exception) {
            "Unable to generate smart insight at this time."
        }
    }

    suspend fun generateClusters(articles: List<ArticleEntity>): Map<String, List<String>> = withContext(Dispatchers.IO) {
        try {
            // val articleList = articles.joinToString("\n") { "${it.guid}: ${it.title}" }
            // val prompt = "Group these articles into thematic clusters. Return JSON: { \"clusterName\": [\"guid1\", \"guid2\"] }. Articles: $articleList"
            // val request = GenerateContentRequest(listOf(Content(listOf(Part(prompt)))))
            // val response = service.generateContent(BuildConfig.GEMINI_API_KEY, request)
            
            // val jsonText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "{}"
            
            // Basic JSON parsing - real impl should use a more robust parser
            // Json { ignoreUnknownKeys = true }.decodeFromString<Map<String, List<String>>>(jsonText.replace("```json", "").replace("```", "").trim())
            emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
