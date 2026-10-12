package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ArticleEntity
import com.example.data.repository.NewsRepository
import com.example.data.repository.ResearchRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class NavTab {
    FEED,
    BOOKMARKS,
    EXPLORE
}

data class FilterParams(
    val query: String = "",
    val isSearchActive: Boolean = false,
    val selectedCategory: String = "All",
    val currentTab: NavTab = NavTab.FEED,
    val selectedArticle: ArticleEntity? = null,
    val isRefreshing: Boolean = false,
    val textScale: Float = 1.0f,
    val isSwarmEnabled: Boolean = false
)

data class NewsUiState(
    val articles: List<ArticleEntity> = emptyList(),
    val filteredArticles: List<ArticleEntity> = emptyList(),
    val bookmarkedArticles: List<ArticleEntity> = emptyList(),
    val featuredArticle: ArticleEntity? = null,
    val selectedCategory: String = "All",
    val availableCategories: List<String> = listOf(
        "All",
        "Gemini",
        "Pixel",
        "Android",
        "Google Research",
        "Google Workspace",
        "Google Health",
        "Sustainability",
        "Chrome"
    ),
    val categoryCounts: Map<String, Int> = emptyMap(),
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val currentTab: NavTab = NavTab.FEED,
    val selectedArticle: ArticleEntity? = null,
    val isRefreshing: Boolean = false,
    val textScale: Float = 1.0f,
    val insights: Map<String, String> = emptyMap(),
    val clusters: Map<String, List<String>> = emptyMap(),
    val isSwarmEnabled: Boolean = false,
    val errorMessage: String? = null
)

class NewsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = NewsRepository(application)
    private val researchRepository = ResearchRepository()

    private val _filterParams = MutableStateFlow(FilterParams())
    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()
    
    // Cache for insights and clusters
    private val _insights = MutableStateFlow<Map<String, String>>(emptyMap())
    private val _clusters = MutableStateFlow<Map<String, List<String>>>(emptyMap())

    val uiState: StateFlow<NewsUiState> = combine(
        repository.allArticles,
        repository.bookmarkedArticles,
        _filterParams,
        _insights,
        _clusters
    ) { allArticles: List<ArticleEntity>, bookmarks: List<ArticleEntity>, params: FilterParams, insights: Map<String, String>, clusters: Map<String, List<String>> ->
        
        // Calculate category counts
        val counts = mutableMapOf<String, Int>()
        counts["All"] = allArticles.size
        allArticles.forEach { article ->
            article.categories.forEach { cat ->
                counts[cat] = (counts[cat] ?: 0) + 1
            }
        }

        // Filter articles
        val filtered = allArticles.filter { article ->
            val matchesCategory = if (params.selectedCategory == "All") {
                true
            } else {
                article.categories.any { it.contains(params.selectedCategory, ignoreCase = true) } ||
                        article.title.contains(params.selectedCategory, ignoreCase = true) ||
                        article.description.contains(params.selectedCategory, ignoreCase = true)
            }

            val matchesQuery = if (params.query.isBlank()) {
                true
            } else {
                article.title.contains(params.query, ignoreCase = true) ||
                        article.description.contains(params.query, ignoreCase = true) ||
                        (article.authorName?.contains(params.query, ignoreCase = true) == true) ||
                        article.categories.any { it.contains(params.query, ignoreCase = true) }
            }

            matchesCategory && matchesQuery
        }

        val featured = if (params.query.isBlank() && params.selectedCategory == "All" && filtered.isNotEmpty()) {
            filtered.first()
        } else null

        val regularArticles = if (featured != null) {
            filtered.drop(1)
        } else {
            filtered
        }

        NewsUiState(
            articles = allArticles,
            filteredArticles = regularArticles,
            bookmarkedArticles = bookmarks,
            featuredArticle = featured,
            selectedCategory = params.selectedCategory,
            categoryCounts = counts,
            searchQuery = params.query,
            isSearchActive = params.isSearchActive,
            currentTab = params.currentTab,
            selectedArticle = params.selectedArticle?.let { sel ->
                allArticles.find { it.guid == sel.guid } ?: sel
            },
            isRefreshing = params.isRefreshing,
            textScale = params.textScale,
            isSwarmEnabled = params.isSwarmEnabled,
            insights = insights,
            clusters = clusters
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NewsUiState()
    )

    init {
        refreshFeed(forceNetwork = false)
    }

    fun refreshFeed(forceNetwork: Boolean = true) {
        viewModelScope.launch {
            _filterParams.update { it.copy(isRefreshing = true) }
            val result = repository.refreshFeed(forceNetwork)
            _filterParams.update { it.copy(isRefreshing = false) }
            if (!result.isSuccess) {
                _toastEvent.emit("Displaying latest offline Google stories")
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _filterParams.update { it.copy(query = query) }
    }

    fun setSearchActive(active: Boolean) {
        _filterParams.update {
            it.copy(
                isSearchActive = active,
                query = if (!active) "" else it.query
            )
        }
    }

    fun selectCategory(category: String) {
        _filterParams.update { it.copy(selectedCategory = category) }
    }

    fun selectTab(tab: NavTab) {
        _filterParams.update { it.copy(currentTab = tab) }
    }

    fun openArticle(article: ArticleEntity) {
        _filterParams.update { it.copy(selectedArticle = article) }
    }

    fun closeArticle() {
        _filterParams.update { it.copy(selectedArticle = null) }
    }

    fun fetchInsight(article: ArticleEntity) {
        if (_insights.value.containsKey(article.guid)) return
        viewModelScope.launch {
            val insight = researchRepository.generateInsight(article.title, article.description)
            _insights.update { it + (article.guid to insight) }
        }
    }

    fun clusterArticles(articles: List<ArticleEntity>) {
        if (_clusters.value.isNotEmpty()) return
        viewModelScope.launch {
            val clusters = researchRepository.generateClusters(articles)
            _clusters.update { clusters }
        }
    }
    
    fun toggleSwarmIntelligence() {
        _filterParams.update { it.copy(isSwarmEnabled = !it.isSwarmEnabled) }
    }
    
    fun filterByCluster(clusterName: String) {
        // Find guid of first article in cluster as a representative filter
        val clusterGuids = _clusters.value[clusterName] ?: return
        val representativeGuid = clusterGuids.firstOrNull() ?: return
        // Simplistic: Set search query to guid to simulate filtering by theme
        _filterParams.update { it.copy(query = representativeGuid, isSearchActive = true) }
        selectTab(NavTab.FEED)
    }

    fun toggleBookmark(article: ArticleEntity) {
        viewModelScope.launch {
            repository.toggleBookmark(article)
            val msg = if (article.isBookmarked) "Removed from Saved" else "Saved for offline reading"
            _toastEvent.emit(msg)
        }
    }

    fun cycleTextScale() {
        _filterParams.update {
            val newScale = when (it.textScale) {
                1.0f -> 1.15f
                1.3f -> 0.9f
                1.15f -> 1.3f
                else -> 1.0f
            }
            it.copy(textScale = newScale)
        }
    }
}
