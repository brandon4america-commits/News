package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ArticleEntity
import com.example.ui.NewsUiState
import com.example.ui.components.ArticleCard
import com.example.ui.components.CategoryChips
import com.example.ui.components.EmptyState
import com.example.ui.components.FeaturedHeroCard
import com.example.ui.theme.GoogleBlue

@Composable
fun FeedScreen(
    uiState: NewsUiState,
    onCategorySelected: (String) -> Unit,
    onArticleClicked: (ArticleEntity) -> Unit,
    onBookmarkToggle: (ArticleEntity) -> Unit,
    onShareArticle: (ArticleEntity) -> Unit,
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Refresh indicator bar
        if (uiState.isRefreshing) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = GoogleBlue
            )
        }

        // Category Filter Chips
        CategoryChips(
            categories = uiState.availableCategories,
            selectedCategory = uiState.selectedCategory,
            categoryCounts = uiState.categoryCounts,
            onCategorySelected = onCategorySelected
        )

        if (uiState.filteredArticles.isEmpty() && uiState.featuredArticle == null) {
            // Empty state for search/filters
            EmptyState(
                icon = Icons.Default.SearchOff,
                title = if (uiState.searchQuery.isNotEmpty()) "No matches found" else "No articles available",
                message = if (uiState.searchQuery.isNotEmpty()) "No announcements matching \"${uiState.searchQuery}\" were found. Try another search term." else "Check back soon for latest Google announcements.",
                actionLabel = if (uiState.searchQuery.isNotEmpty()) "Clear Search" else null,
                onAction = onClearSearch,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("feed_articles_list"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top Featured Hero Story (if category is All and not searching)
                if (uiState.featuredArticle != null) {
                    item(key = "featured_${uiState.featuredArticle.guid}") {
                        FeaturedHeroCard(
                            article = uiState.featuredArticle,
                            onClick = { onArticleClicked(uiState.featuredArticle) },
                            onBookmarkToggle = { onBookmarkToggle(uiState.featuredArticle) },
                            onShare = { onShareArticle(uiState.featuredArticle) }
                        )
                    }

                    item(key = "section_header_latest") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Latest Announcements",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${uiState.filteredArticles.size} articles",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (uiState.selectedCategory != "All" || uiState.searchQuery.isNotEmpty()) {
                    item(key = "section_header_filtered") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (uiState.searchQuery.isNotEmpty()) "Results for \"${uiState.searchQuery}\"" else "${uiState.selectedCategory} Updates",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${uiState.filteredArticles.size} found",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Regular Article Cards
                items(
                    items = uiState.filteredArticles,
                    key = { it.guid }
                ) { article ->
                    ArticleCard(
                        article = article,
                        onClick = { onArticleClicked(article) },
                        onBookmarkToggle = { onBookmarkToggle(article) },
                        onShare = { onShareArticle(article) }
                    )
                }
            }
        }
    }
}
