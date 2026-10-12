package com.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.ArticleEntity
import com.example.ui.NavTab
import com.example.ui.NewsViewModel
import com.example.ui.components.GoogleBrandBar
import com.example.ui.components.SwarmPointerOverlay
import com.example.ui.screens.ArticleDetailScreen
import com.example.ui.screens.BookmarksScreen
import com.example.ui.screens.FeedScreen
import com.example.ui.screens.OntologyMapScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                GoogleNewsApp()
            }
        }
    }
}

@Composable
fun GoogleNewsApp(
    viewModel: NewsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Listen for toast/snackbar events from VM
    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Handle System Back Press
    BackHandler(
        enabled = uiState.selectedArticle != null || uiState.isSearchActive || uiState.currentTab != NavTab.FEED
    ) {
        when {
            uiState.selectedArticle != null -> viewModel.closeArticle()
            uiState.isSearchActive -> viewModel.setSearchActive(false)
            uiState.currentTab != NavTab.FEED -> viewModel.selectTab(NavTab.FEED)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.statusBars,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            if (uiState.selectedArticle == null && uiState.currentTab == NavTab.FEED) {
                GoogleBrandBar(
                    isSearchActive = uiState.isSearchActive,
                    searchQuery = uiState.searchQuery,
                    bookmarkCount = uiState.bookmarkedArticles.size,
                    isBookmarksTab = uiState.currentTab == NavTab.BOOKMARKS,
                    isRefreshing = uiState.isRefreshing,
                    onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
                    onToggleSearch = { viewModel.setSearchActive(it) },
                    onBookmarksClicked = { viewModel.selectTab(NavTab.BOOKMARKS) },
                    onRefreshClicked = { viewModel.refreshFeed(forceNetwork = true) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            AnimatedContent(
                targetState = when {
                    uiState.selectedArticle != null -> "detail"
                    uiState.currentTab == NavTab.BOOKMARKS -> "bookmarks"
                    uiState.currentTab == NavTab.EXPLORE -> "explore"
                    else -> "feed"
                },
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "screen_transition"
            ) { targetScreen ->
                when (targetScreen) {
                    "detail" -> {
                        uiState.selectedArticle?.let { article ->
                            LaunchedEffect(article.guid) {
                                viewModel.fetchInsight(article)
                            }
                            ArticleDetailScreen(
                                article = article,
                                textScale = uiState.textScale,
                                insight = uiState.insights[article.guid],
                                onBack = { viewModel.closeArticle() },
                                onBookmarkToggle = { viewModel.toggleBookmark(article) },
                                onShare = { shareArticle(context, article) },
                                onCycleTextScale = { viewModel.cycleTextScale() }
                            )
                        }
                    }
                    "bookmarks" -> {
                        BookmarksScreen(
                            bookmarkedArticles = uiState.bookmarkedArticles,
                            onArticleClicked = { viewModel.openArticle(it) },
                            onBookmarkToggle = { viewModel.toggleBookmark(it) },
                            onShareArticle = { shareArticle(context, it) },
                            onBackToFeed = { viewModel.selectTab(NavTab.FEED) }
                        )
                    }
                    "explore" -> {
                        OntologyMapScreen(
                            clusters = uiState.clusters,
                            isSwarmEnabled = uiState.isSwarmEnabled,
                            onClusterSelected = { viewModel.filterByCluster(it) },
                            onToggleSwarm = { viewModel.toggleSwarmIntelligence() }
                        )
                    }
                    else -> {
                        FeedScreen(
                            uiState = uiState,
                            onCategorySelected = { viewModel.selectCategory(it) },
                            onArticleClicked = { viewModel.openArticle(it) },
                            onBookmarkToggle = { viewModel.toggleBookmark(it) },
                            onShareArticle = { shareArticle(context, it) },
                            onClearSearch = { viewModel.setSearchActive(false) }
                        )
                    }
                }
            }
            if (uiState.isSwarmEnabled) {
                SwarmPointerOverlay()
            }
        }
    }
}

private fun shareArticle(context: Context, article: ArticleEntity) {
    try {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, article.title)
            putExtra(
                Intent.EXTRA_TEXT,
                "${article.title}\n\n${article.description}\n\nRead more on Google Blog: ${article.link}"
            )
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Google Announcement"))
    } catch (e: Exception) {
        // Fallback
    }
}
