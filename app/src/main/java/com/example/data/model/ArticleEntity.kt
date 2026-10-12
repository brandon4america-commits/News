package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(tableName = "articles")
data class ArticleEntity(
    @PrimaryKey
    val guid: String,
    val title: String,
    val link: String,
    val imageUrl: String?,
    val description: String,
    val pubDate: String,
    val categoriesCsv: String,
    val authorName: String?,
    val authorTitle: String?,
    val authorDepartment: String?,
    val isBookmarked: Boolean = false,
    val bookmarkedAt: Long? = null,
    val cachedAt: Long = System.currentTimeMillis()
) {
    val categories: List<String>
        get() = if (categoriesCsv.isBlank()) emptyList() else categoriesCsv.split("|")

    val primaryCategory: String
        get() = categories.firstOrNull() ?: "Google News"

    val authorDisplay: String
        get() = when {
            !authorName.isNullOrBlank() && !authorTitle.isNullOrBlank() -> "$authorName • $authorTitle"
            !authorName.isNullOrBlank() -> authorName
            else -> "Google Team"
        }

    val readingTimeMinutes: Int
        get() {
            val words = (title + " " + description).split("\\s+".toRegex()).size
            return (words / 50).coerceAtLeast(1)
        }

    val formattedDate: String
        get() {
            return try {
                // Example pubDate: Tue, 18 Aug 2026 19:00:00 +0000
                val inputFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US)
                val outputFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)
                val date = inputFormat.parse(pubDate)
                if (date != null) outputFormat.format(date) else pubDate
            } catch (e: Exception) {
                pubDate
            }
        }
}
