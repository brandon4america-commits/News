package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.ArticleEntity
import com.example.data.parser.RssParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class NewsRepository(context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val dao = db.articleDao()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    val allArticles: Flow<List<ArticleEntity>> = dao.getAllArticles()
    val bookmarkedArticles: Flow<List<ArticleEntity>> = dao.getBookmarkedArticles()

    fun getArticle(guid: String): Flow<ArticleEntity?> = dao.getArticleByGuid(guid)

    suspend fun toggleBookmark(article: ArticleEntity) = withContext(Dispatchers.IO) {
        val newBookmarkState = !article.isBookmarked
        val bookmarkedAt = if (newBookmarkState) System.currentTimeMillis() else null
        dao.updateBookmarkStatus(article.guid, newBookmarkState, bookmarkedAt)
    }

    suspend fun refreshFeed(forceNetwork: Boolean = false): Result<Int> = withContext(Dispatchers.IO) {
        try {
            var parsedArticles: List<ArticleEntity> = emptyList()

            // Try network first
            try {
                val request = Request.Builder()
                    .url("https://blog.google/rss/")
                    .header("User-Agent", "GoogleNewsAndroidApp/1.0")
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    response.body?.byteStream()?.use { stream ->
                        parsedArticles = RssParser.parse(stream)
                    }
                }
            } catch (networkError: Exception) {
                Log.w("NewsRepository", "Network fetch failed, will check seed fallback", networkError)
            }

            // If network returned empty or failed, fallback to seed articles
            if (parsedArticles.isEmpty()) {
                parsedArticles = RssParser.parseString(RssParser.SEED_RSS_XML)
            }

            if (parsedArticles.isNotEmpty()) {
                // Preserve existing bookmarks
                val mergedArticles = parsedArticles.map { parsed ->
                    val existing = dao.getArticleSync(parsed.guid)
                    if (existing != null) {
                        parsed.copy(
                            isBookmarked = existing.isBookmarked,
                            bookmarkedAt = existing.bookmarkedAt
                        )
                    } else {
                        parsed
                    }
                }
                dao.insertOrUpdateArticles(mergedArticles)
                Result.success(mergedArticles.size)
            } else {
                Result.failure(Exception("No articles found"))
            }
        } catch (e: Exception) {
            Log.e("NewsRepository", "Failed to refresh feed", e)
            Result.failure(e)
        }
    }
}
