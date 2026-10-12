package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.ArticleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {

    @Query("SELECT * FROM articles ORDER BY cachedAt DESC")
    fun getAllArticles(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE isBookmarked = 1 ORDER BY bookmarkedAt DESC, cachedAt DESC")
    fun getBookmarkedArticles(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE guid = :guid LIMIT 1")
    fun getArticleByGuid(guid: String): Flow<ArticleEntity?>

    @Query("SELECT * FROM articles WHERE guid = :guid LIMIT 1")
    suspend fun getArticleSync(guid: String): ArticleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateArticles(articles: List<ArticleEntity>)

    @Query("UPDATE articles SET isBookmarked = :isBookmarked, bookmarkedAt = :bookmarkedAt WHERE guid = :guid")
    suspend fun updateBookmarkStatus(guid: String, isBookmarked: Boolean, bookmarkedAt: Long?)

    @Query("SELECT * FROM articles WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR categoriesCsv LIKE '%' || :query || '%' OR authorName LIKE '%' || :query || '%'")
    fun searchArticles(query: String): Flow<List<ArticleEntity>>
}
