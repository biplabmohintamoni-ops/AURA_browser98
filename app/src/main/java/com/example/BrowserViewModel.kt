package com.example

import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.URLUtil
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.util.Calendar
import java.util.Date
import java.util.UUID

enum class DownloadStatus {
    DOWNLOADING,
    COMPLETED,
    FAILED,
    PAUSED,
    CANCELLED
}

data class DownloadItem(
    val id: Long,
    val filename: String,
    val url: String,
    val progress: Int, // 0 to 100
    val sizeString: String = "",
    val status: DownloadStatus = DownloadStatus.DOWNLOADING,
    val localUri: String? = null
)

data class QuickLink(
    val title: String,
    val url: String,
    val iconLetter: String,
    val colorHex: Long
)

data class BrowserTab(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Blank Tab",
    val url: String = "",
    val currentUrl: String = "",
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val error: String? = null,
    val isIncognito: Boolean = false
)

data class WeatherForecastItem(
    val dayName: String,
    val tempMax: String,
    val tempMin: String,
    val condition: String,
    val iconIndex: Int
)

data class BrowserState(
    val tabs: List<BrowserTab> = listOf(BrowserTab()),
    val activeTabId: String = "",
    val showTabSwitcher: Boolean = false,
    val searchInput: String = "",
    val quickLinks: List<QuickLink> = listOf(
        QuickLink("Google", "https://www.google.com", "G", 0xFF4285F4),
        QuickLink("YouTube", "https://www.youtube.com", "Y", 0xFFFF0000),
        QuickLink("GitHub", "https://www.github.com", "G", 0xFF24292E),
        QuickLink("Wikipedia", "https://www.wikipedia.org", "W", 0xFF4A4A4A),
        QuickLink("Reddit", "https://www.reddit.com", "R", 0xFFFF4500),
        QuickLink("Figma", "https://www.figma.com", "F", 0xFFF24E1E)
    ),
    val showAddShortcutDialog: Boolean = false,
    val isIncognitoMode: Boolean = false,
    val downloads: List<DownloadItem> = emptyList(),
    val showDownloadsScreen: Boolean = false,
    val activeRegularTabId: String = "",
    val activeIncognitoTabId: String = "",
    val profileName: String = "Biplab Mohinta",
    val profileEmail: String = "biplabmohintamoni@gmail.com",
    val profileBio: String = "Web Explorer & Fluid Browser Enthusiast",
    val profileAvatarIndex: Int = 0,
    val showProfileScreen: Boolean = false,
    val defaultSearchEngine: String = "Google",
    val themeAccentIndex: Int = 0,
    val searchSuggestions: List<String> = emptyList(),
    val isSearchingSuggestions: Boolean = false,
    val weatherTemp: String = "84°F",
    val weatherCondition: String = "Partly Cloudy",
    val weatherIconIndex: Int = 1, // 0=Clear, 1=Cloudy, 2=Rain, 3=Snow, 4=Storm
    val weatherLocationName: String = "Kolkata",
    val weatherForecasts: List<WeatherForecastItem> = listOf(
        WeatherForecastItem("Today", "86°F", "78°F", "Sunny", 0),
        WeatherForecastItem("Thu", "88°F", "77°F", "Humid / Sunny", 0),
        WeatherForecastItem("Fri", "89°F", "79°F", "Thundershowers", 4),
        WeatherForecastItem("Sat", "84°F", "75°F", "Heavy Rain", 2),
        WeatherForecastItem("Sun", "83°F", "74°F", "Scattered Showers", 2),
        WeatherForecastItem("Mon", "87°F", "77°F", "Partly Cloudy", 1),
        WeatherForecastItem("Tue", "88°F", "78°F", "Sunny Intervals", 1)
    ),
    val isWeatherLoading: Boolean = false,
    val isCelsius: Boolean = false,
    val showWeatherDetails: Boolean = false,
    val weatherLatitude: Double = 22.5726,
    val weatherLongitude: Double = 88.3639
)

sealed interface BrowserCommand {
    data class LoadUrl(val tabId: String, val url: String) : BrowserCommand
    data class GoBack(val tabId: String) : BrowserCommand
    data class GoForward(val tabId: String) : BrowserCommand
    data class Reload(val tabId: String) : BrowserCommand
}

class BrowserViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(BrowserState())
    val uiState: StateFlow<BrowserState> = _uiState.asStateFlow()

    private val _commands = MutableSharedFlow<BrowserCommand>(extraBufferCapacity = 64)
    val commands: SharedFlow<BrowserCommand> = _commands.asSharedFlow()

    private var isPolling = false
    private var searchSuggestionsJob: kotlinx.coroutines.Job? = null

    init {
        val firstTab = _uiState.value.tabs.first()
        _uiState.update { state ->
            state.copy(
                activeTabId = firstTab.id,
                activeRegularTabId = firstTab.id
            )
        }
        detectAndFetchLiveLocationWeather()
    }

    fun onSearchInputChange(input: String) {
        _uiState.update { it.copy(searchInput = input) }
        searchSuggestionsJob?.cancel()
        searchSuggestionsJob = viewModelScope.launch {
            kotlinx.coroutines.delay(250)
            fetchSearchSuggestions(input)
        }
    }

    fun submitSearch(input: String) {
        _uiState.update { it.copy(searchSuggestions = emptyList()) }
        val parsedUrl = parseUrlOrSearch(input)
        if (parsedUrl.isNotEmpty()) {
            val activeId = _uiState.value.activeTabId
            loadUrlInTab(activeId, parsedUrl)
        }
    }

    fun selectTab(tabId: String) {
        _uiState.update { state ->
            val selectedTab = state.tabs.find { t -> t.id == tabId }
            val isIncog = selectedTab?.isIncognito ?: false
            state.copy(
                activeTabId = tabId,
                isIncognitoMode = isIncog,
                activeRegularTabId = if (!isIncog) tabId else state.activeRegularTabId,
                activeIncognitoTabId = if (isIncog) tabId else state.activeIncognitoTabId,
                showTabSwitcher = false,
                searchInput = selectedTab?.currentUrl ?: ""
            )
        }
    }

    fun addNewTab(initialUrl: String = "") {
        val isIncog = _uiState.value.isIncognitoMode
        val title = if (isIncog) {
            if (initialUrl.isEmpty()) "Private Tab" else "Private"
        } else {
            if (initialUrl.isEmpty()) "Blank Tab" else "Loading..."
        }
        val newTab = BrowserTab(
            url = initialUrl,
            currentUrl = initialUrl,
            isIncognito = isIncog,
            title = title
        )
        _uiState.update { state ->
            val updatedTabs = state.tabs + newTab
            state.copy(
                tabs = updatedTabs,
                activeTabId = newTab.id,
                activeRegularTabId = if (!isIncog) newTab.id else state.activeRegularTabId,
                activeIncognitoTabId = if (isIncog) newTab.id else state.activeIncognitoTabId,
                showTabSwitcher = false,
                searchInput = initialUrl
            )
        }
        if (initialUrl.isNotEmpty()) {
            loadUrlInTab(newTab.id, initialUrl)
        }
    }

    fun toggleIncognitoMode() {
        _uiState.update { state ->
            val nextIncognito = !state.isIncognitoMode
            val tabsForNextMode = state.tabs.filter { it.isIncognito == nextIncognito }
            
            if (nextIncognito) {
                // Entering Private mode: restore active private tab or first or create if empty
                val targetTabId = if (state.activeIncognitoTabId.isNotEmpty() && state.tabs.any { it.id == state.activeIncognitoTabId && it.isIncognito }) {
                    state.activeIncognitoTabId
                } else if (tabsForNextMode.isNotEmpty()) {
                    tabsForNextMode.first().id
                } else {
                    ""
                }
                
                if (targetTabId.isEmpty()) {
                    val newPrivateTab = BrowserTab(title = "Private Tab", isIncognito = true)
                    state.copy(
                        isIncognitoMode = true,
                        tabs = state.tabs + newPrivateTab,
                        activeIncognitoTabId = newPrivateTab.id,
                        activeTabId = newPrivateTab.id,
                        activeRegularTabId = if (state.activeTabId.isNotEmpty() && !state.isIncognitoMode) state.activeTabId else state.activeRegularTabId,
                        searchInput = ""
                    )
                } else {
                    val targetTab = state.tabs.find { it.id == targetTabId }
                    state.copy(
                        isIncognitoMode = true,
                        activeIncognitoTabId = targetTabId,
                        activeTabId = targetTabId,
                        activeRegularTabId = if (state.activeTabId.isNotEmpty() && !state.isIncognitoMode) state.activeTabId else state.activeRegularTabId,
                        searchInput = targetTab?.currentUrl ?: ""
                    )
                }
            } else {
                // Leaving Private mode: restore active regular tab
                val targetTabId = if (state.activeRegularTabId.isNotEmpty() && state.tabs.any { it.id == state.activeRegularTabId && !it.isIncognito }) {
                    state.activeRegularTabId
                } else if (tabsForNextMode.isNotEmpty()) {
                    tabsForNextMode.first().id
                } else {
                    ""
                }
                
                if (targetTabId.isEmpty()) {
                    val newRegularTab = BrowserTab(isIncognito = false)
                    state.copy(
                        isIncognitoMode = false,
                        tabs = state.tabs + newRegularTab,
                        activeRegularTabId = newRegularTab.id,
                        activeTabId = newRegularTab.id,
                        activeIncognitoTabId = if (state.activeTabId.isNotEmpty() && state.isIncognitoMode) state.activeTabId else state.activeIncognitoTabId,
                        searchInput = ""
                    )
                } else {
                    val targetTab = state.tabs.find { it.id == targetTabId }
                    state.copy(
                        isIncognitoMode = false,
                        activeRegularTabId = targetTabId,
                        activeTabId = targetTabId,
                        activeIncognitoTabId = if (state.activeTabId.isNotEmpty() && state.isIncognitoMode) state.activeTabId else state.activeIncognitoTabId,
                        searchInput = targetTab?.currentUrl ?: ""
                    )
                }
            }
        }
    }

    fun closeTab(tabId: String, onDestroyWebView: (String) -> Unit) {
        val currentState = _uiState.value
        val incognito = currentState.isIncognitoMode
        val modeTabs = currentState.tabs.filter { it.isIncognito == incognito }
        
        if (modeTabs.size <= 1 && modeTabs.any { it.id == tabId }) {
            val newTab = BrowserTab(isIncognito = incognito, title = if (incognito) "Private Tab" else "Blank Tab")
            _uiState.update { state ->
                val nonTargetTabs = state.tabs.filter { it.id != tabId }
                val updatedTabs = nonTargetTabs + newTab
                state.copy(
                    tabs = updatedTabs,
                    activeTabId = newTab.id,
                    activeRegularTabId = if (!incognito) newTab.id else state.activeRegularTabId,
                    activeIncognitoTabId = if (incognito) newTab.id else state.activeIncognitoTabId,
                    showTabSwitcher = false,
                    searchInput = ""
                )
            }
            onDestroyWebView(tabId)
            return
        }

        val tabIndex = modeTabs.indexOfFirst { it.id == tabId }
        val updatedTabs = currentState.tabs.filter { it.id != tabId }
        val remainingModeTabs = updatedTabs.filter { it.isIncognito == incognito }
        
        val newActiveId = if (currentState.activeTabId == tabId) {
            val targetIndex = if (tabIndex >= remainingModeTabs.size) remainingModeTabs.size - 1 else tabIndex
            remainingModeTabs[targetIndex].id
        } else {
            currentState.activeTabId
        }

        _uiState.update { state ->
            val targetTab = updatedTabs.find { t -> t.id == newActiveId }
            state.copy(
                tabs = updatedTabs,
                activeTabId = newActiveId,
                activeRegularTabId = if (!incognito) newActiveId else state.activeRegularTabId,
                activeIncognitoTabId = if (incognito) newActiveId else state.activeIncognitoTabId,
                searchInput = targetTab?.currentUrl ?: ""
            )
        }

        onDestroyWebView(tabId)
    }

    fun toggleTabSwitcher() {
        _uiState.update { it.copy(showTabSwitcher = !it.showTabSwitcher) }
    }

    fun closeTabSwitcher() {
        _uiState.update { it.copy(showTabSwitcher = false) }
    }

    fun goBackActiveTab() {
        val activeId = _uiState.value.activeTabId
        viewModelScope.launch {
            _commands.emit(BrowserCommand.GoBack(activeId))
        }
    }

    fun goForwardActiveTab() {
        val activeId = _uiState.value.activeTabId
        viewModelScope.launch {
            _commands.emit(BrowserCommand.GoForward(activeId))
        }
    }

    fun reloadActiveTab() {
        val activeId = _uiState.value.activeTabId
        viewModelScope.launch {
            _commands.emit(BrowserCommand.Reload(activeId))
        }
    }

    fun reloadTab(tabId: String) {
        viewModelScope.launch {
            _commands.emit(BrowserCommand.Reload(tabId))
        }
    }

    fun loadUrlInTab(tabId: String, url: String) {
        val parsed = parseUrlOrSearch(url)
        _uiState.update { state ->
            state.copy(
                tabs = state.tabs.map { tab ->
                    if (tab.id == tabId) {
                        tab.copy(url = parsed, currentUrl = parsed, error = null)
                    } else tab
                },
                searchInput = if (tabId == state.activeTabId) parsed else state.searchInput
            )
        }
        viewModelScope.launch {
            _commands.emit(BrowserCommand.LoadUrl(tabId, parsed))
        }
    }

    fun updateTabLoadingState(tabId: String, isLoading: Boolean) {
        _uiState.update { state ->
            state.copy(
                tabs = state.tabs.map { tab ->
                    if (tab.id == tabId) {
                        tab.copy(
                            isLoading = isLoading,
                            error = if (isLoading) null else tab.error
                        )
                    } else tab
                }
            )
        }
    }

    fun updateTabProgress(tabId: String, progress: Int) {
        _uiState.update { state ->
            state.copy(
                tabs = state.tabs.map { tab ->
                    if (tab.id == tabId) {
                        tab.copy(progress = progress)
                    } else tab
                }
            )
        }
    }

    fun updateTabUrlAndFlags(tabId: String, url: String, title: String?, canGoBack: Boolean, canGoForward: Boolean) {
        _uiState.update { state ->
            state.copy(
                tabs = state.tabs.map { tab ->
                    if (tab.id == tabId) {
                        tab.copy(
                            currentUrl = url,
                            url = if (tab.url.isEmpty() && url.isNotEmpty()) url else tab.url,
                            title = if (!title.isNullOrBlank()) title else tab.title,
                            canGoBack = canGoBack,
                            canGoForward = canGoForward
                        )
                    } else tab
                },
                searchInput = if (tabId == state.activeTabId) url else state.searchInput
            )
        }
    }

    fun updateTabError(tabId: String, errorDescription: String) {
        _uiState.update { state ->
            state.copy(
                tabs = state.tabs.map { tab ->
                    if (tab.id == tabId) {
                        tab.copy(error = errorDescription, isLoading = false)
                    } else tab
                }
            )
        }
    }

    fun addQuickLink(title: String, url: String) {
        val parsed = parseUrlOrSearch(url)
        val colors = listOf(0xFF4285F4, 0xFF34A853, 0xFFFBBC05, 0xFFEA4335, 0xFF9C27B0, 0xFF673AB7, 0xFF00BCD4, 0xFFE91E63)
        val selectedColor = colors.random()
        _uiState.update { state ->
            state.copy(
                quickLinks = state.quickLinks + QuickLink(title, parsed, title.take(1).uppercase(), selectedColor)
            )
        }
    }

    fun deleteQuickLink(link: QuickLink) {
        _uiState.update { state ->
            state.copy(
                quickLinks = state.quickLinks.filter { it != link }
            )
        }
    }

    fun toggleAddShortcutDialog(show: Boolean) {
        _uiState.update { it.copy(showAddShortcutDialog = show) }
    }

    fun toggleDownloadsScreen(show: Boolean) {
        _uiState.update { it.copy(showDownloadsScreen = show) }
    }

    fun startDownload(url: String, userAgent: String?, contentDisposition: String?, mimetype: String?, contentLength: Long) {
        val context = getApplication<Application>()
        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val rawFilename = URLUtil.guessFileName(url, contentDisposition, mimetype)
            val filename = if (rawFilename.isNullOrBlank()) "download" else rawFilename
            
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                if (!mimetype.isNullOrBlank()) {
                    setMimeType(mimetype)
                }
                if (!userAgent.isNullOrBlank()) {
                    addRequestHeader("User-Agent", userAgent)
                }
                setDescription("Downloading file...")
                setTitle(filename)
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                try {
                    setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
                } catch (e: Exception) {
                    setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, filename)
                }
            }
            
            val downloadId = downloadManager.enqueue(request)
            
            val newItem = DownloadItem(
                id = downloadId,
                filename = filename,
                url = url,
                progress = 0,
                sizeString = formatSize(contentLength),
                status = DownloadStatus.DOWNLOADING
            )
            
            _uiState.update { state ->
                state.copy(
                    downloads = listOf(newItem) + state.downloads,
                    showDownloadsScreen = true
                )
            }
            
            startPollingDownloads()
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun pauseDownload(id: Long) {
        _uiState.update { state ->
            state.copy(
                downloads = state.downloads.map {
                    if (it.id == id) it.copy(status = DownloadStatus.PAUSED) else it
                }
            )
        }
    }

    fun resumeDownload(id: Long) {
        _uiState.update { state ->
            state.copy(
                downloads = state.downloads.map {
                    if (it.id == id) it.copy(status = DownloadStatus.DOWNLOADING) else it
                }
            )
        }
        startPollingDownloads()
    }

    fun cancelDownload(id: Long) {
        val context = getApplication<Application>()
        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.remove(id)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _uiState.update { state ->
            state.copy(
                downloads = state.downloads.map {
                    if (it.id == id) it.copy(status = DownloadStatus.CANCELLED, progress = 0) else it
                }
            )
        }
    }

    fun openDownloadedFile(id: Long) {
        val context = getApplication<Application>()
        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val fileUri = downloadManager.getUriForDownloadedFile(id)
            if (fileUri != null) {
                val mimeType = downloadManager.getMimeTypeForDownloadedFile(id)
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                    setDataAndType(fileUri, mimeType)
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } else {
                val intent = android.content.Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                val intent = android.content.Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    private fun startPollingDownloads() {
        if (isPolling) return
        isPolling = true
        
        viewModelScope.launch {
            val context = getApplication<Application>()
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            
            while (true) {
                var hasActive = false
                val currentDownloads = _uiState.value.downloads
                
                val updatedDownloads = currentDownloads.map { item ->
                    if (item.status == DownloadStatus.DOWNLOADING) {
                        hasActive = true
                        val query = DownloadManager.Query().setFilterById(item.id)
                        val cursor = downloadManager.query(query)
                        if (cursor != null && cursor.moveToFirst()) {
                            val columnIndexStatus = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                            val statusVal = if (columnIndexStatus >= 0) cursor.getInt(columnIndexStatus) else -1
                            
                            val columnIndexDownloaded = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                            val bytesDownloaded = if (columnIndexDownloaded >= 0) cursor.getLong(columnIndexDownloaded) else 0L
                            
                            val columnIndexTotal = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                            val totalBytes = if (columnIndexTotal >= 0) cursor.getLong(columnIndexTotal) else 0L
                            
                            val columnIndexLocalUri = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                            val localUriString = if (columnIndexLocalUri >= 0) cursor.getString(columnIndexLocalUri) else null
                            
                            val status = when (statusVal) {
                                DownloadManager.STATUS_SUCCESSFUL -> DownloadStatus.COMPLETED
                                DownloadManager.STATUS_FAILED -> DownloadStatus.FAILED
                                DownloadManager.STATUS_PAUSED -> DownloadStatus.PAUSED
                                DownloadManager.STATUS_RUNNING -> DownloadStatus.DOWNLOADING
                                else -> item.status
                            }
                            
                            val progress = if (totalBytes > 0) {
                                ((bytesDownloaded * 100) / totalBytes).toInt()
                            } else {
                                item.progress
                            }
                            
                            val sizeStr = if (totalBytes > 0) formatSize(totalBytes) else item.sizeString
                            
                            cursor.close()
                            item.copy(
                                status = status,
                                progress = progress,
                                sizeString = sizeStr,
                                localUri = localUriString
                            )
                        } else {
                            cursor?.close()
                            item
                        }
                    } else {
                        item
                    }
                }
                
                _uiState.update { it.copy(downloads = updatedDownloads) }
                
                if (!hasActive) {
                    isPolling = false
                    break
                }
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    private fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "Unknown size"
        val kb = bytes / 1024f
        if (kb < 1024) return String.format("%.1f KB", kb)
        val mb = kb / 1024f
        return String.format("%.1f MB", mb)
    }

    private fun parseUrlOrSearch(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return ""
        
        val urlPattern = "^(https?://)?([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}(/.*)?$".toRegex()
        val isUrl = trimmed.matches(urlPattern) || 
                    trimmed.startsWith("http://") || 
                    trimmed.startsWith("https://") ||
                    trimmed.startsWith("file://") ||
                    trimmed.contains("localhost")
                    
        return if (isUrl) {
            if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
                "https://$trimmed"
            } else {
                trimmed
            }
        } else {
            val query = URLEncoder.encode(trimmed, "UTF-8")
            when (_uiState.value.defaultSearchEngine) {
                "DuckDuckGo" -> "https://duckduckgo.com/?q=$query"
                "Bing" -> "https://www.bing.com/search?q=$query"
                "Yahoo" -> "https://search.yahoo.com/search?p=$query"
                else -> "https://www.google.com/search?q=$query"
            }
        }
    }

    fun toggleProfileScreen(show: Boolean) {
        _uiState.update { it.copy(showProfileScreen = show) }
    }

    fun updateProfile(
        name: String,
        email: String,
        bio: String,
        avatarIndex: Int,
        searchEngine: String,
        themeIndex: Int
    ) {
        _uiState.update {
            it.copy(
                profileName = name,
                profileEmail = email,
                profileBio = bio,
                profileAvatarIndex = avatarIndex,
                defaultSearchEngine = searchEngine,
                themeAccentIndex = themeIndex
            )
        }
    }

    fun toggleWeatherUnit() {
        val nextIsCelsius = !_uiState.value.isCelsius
        _uiState.update { it.copy(isCelsius = nextIsCelsius) }
        fetchWeatherForCoordsAndName(
            _uiState.value.weatherLatitude,
            _uiState.value.weatherLongitude,
            _uiState.value.weatherLocationName
        )
    }

    fun toggleWeatherDetails(show: Boolean) {
        _uiState.update { it.copy(showWeatherDetails = show) }
    }

    private fun getWeatherConditionText(code: Int): String {
        return when (code) {
            0 -> "Clear Sky"
            1, 2, 3 -> "Partly Cloudy"
            45, 48 -> "Foggy"
            51, 53, 55 -> "Drizzle"
            61, 63, 65 -> "Rainy"
            66, 67 -> "Freezing Rain"
            71, 73, 75 -> "Snowy"
            77 -> "Snow Grains"
            80, 81, 82 -> "Rain Showers"
            85, 86 -> "Snow Showers"
            95 -> "Thunderstorm"
            96, 99 -> "Heavy Thunderstorm"
            else -> "Mostly Clear"
        }
    }

    private fun getWeatherIconIndex(code: Int): Int {
        return when (code) {
            0 -> 0 // Sun
            1, 2, 3 -> 1 // Cloud/Partly Cloudy
            45, 48 -> 1 // Cloud
            51, 53, 55, 61, 63, 65, 80, 81, 82 -> 2 // Rain
            71, 73, 75, 77, 85, 86 -> 3 // Snow
            95, 96, 99 -> 4 // Storm
            else -> 0
        }
    }

    fun reverseGeocode(lat: Double, lon: Double): String? {
        try {
            val urlStr = "https://api.bigdatacloud.net/data/reverse-geocode-client?latitude=$lat&longitude=$lon&localityLanguage=en"
            val url = java.net.URL(urlStr)
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val json = org.json.JSONObject(response)
                var name = json.optString("city")
                if (name.isNullOrEmpty()) {
                    name = json.optString("locality")
                }
                if (name.isNullOrEmpty()) {
                    name = json.optString("principalSubdivision")
                }
                if (!name.isNullOrEmpty()) {
                    return name
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun detectAndFetchLiveLocationWeather() {
        val context = getApplication<Application>().applicationContext
        val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        
        _uiState.update { it.copy(isWeatherLoading = true) }
        
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            var fetchedSuccess = false
            if (hasFine || hasCoarse) {
                try {
                    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
                    if (locationManager != null) {
                        var lastLocation: android.location.Location? = null
                        val providers = locationManager.getProviders(true)
                        for (provider in providers) {
                            try {
                                val loc = locationManager.getLastKnownLocation(provider)
                                if (loc != null) {
                                    if (lastLocation == null || loc.accuracy < lastLocation.accuracy) {
                                        lastLocation = loc
                                    }
                                }
                            } catch (e: Exception) {
                                // ignore
                            }
                        }
                        
                        if (lastLocation != null) {
                            val lat = lastLocation.latitude
                            val lon = lastLocation.longitude
                            val city = reverseGeocode(lat, lon) ?: "Current GPS Location"
                            fetchWeatherForCoordsAndName(lat, lon, city)
                            fetchedSuccess = true
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            if (!fetchedSuccess) {
                try {
                    val url = java.net.URL("https://ipapi.co/json/")
                    val conn = url.openConnection() as java.net.HttpURLConnection
                    conn.connectTimeout = 4000
                    conn.readTimeout = 4000
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                    if (conn.responseCode == 200) {
                        val response = conn.inputStream.bufferedReader().use { it.readText() }
                        val json = org.json.JSONObject(response)
                        val lat = json.optDouble("latitude", 22.5726)
                        val lon = json.optDouble("longitude", 88.3639)
                        val city = json.optString("city", "Kolkata (IP)")
                        fetchWeatherForCoordsAndName(lat, lon, city)
                        fetchedSuccess = true
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            if (!fetchedSuccess) {
                fetchWeatherForCity("Kolkata")
            }
        }
    }

    fun fetchWeatherForCity(cityName: String) {
        val (lat, lon) = when (cityName.trim().lowercase()) {
            "kolkata" -> 22.5726 to 88.3639
            "new york" -> 40.7128 to -74.0060
            "london" -> 51.5074 to -0.1278
            "tokyo" -> 35.6762 to 139.6503
            "paris" -> 48.8566 to 2.3522
            "san francisco" -> 37.7749 to -122.4194
            "sydney" -> -33.8688 to 151.2093
            "mumbai" -> 19.0760 to 72.8777
            "delhi" -> 28.6139 to 77.2090
            "bangalore" -> 12.9716 to 77.5946
            else -> 22.5726 to 88.3639 // default to Kolkata
        }
        fetchWeatherForCoordsAndName(lat, lon, cityName)
    }

    fun fetchWeatherForCoordsAndName(lat: Double, lon: Double, cityName: String) {
        _uiState.update { state ->
            state.copy(
                weatherLocationName = cityName,
                isWeatherLoading = true,
                weatherLatitude = lat,
                weatherLongitude = lon
            )
        }
        
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val urlStr = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,weather_code,relative_humidity_2m,wind_speed_10m&daily=weather_code,temperature_2m_max,temperature_2m_min&timezone=auto"
                val url = java.net.URL(urlStr)
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                
                if (conn.responseCode == 200) {
                    val text = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = org.json.JSONObject(text)
                    
                    val currentObj = json.optJSONObject("current")
                    val tempC = currentObj?.optDouble("temperature_2m", 28.0) ?: 28.0
                    val code = currentObj?.optInt("weather_code", 0) ?: 0
                    
                    val dailyObj = json.optJSONObject("daily")
                    val dailyTime = dailyObj?.optJSONArray("time")
                    val dailyMax = dailyObj?.optJSONArray("temperature_2m_max")
                    val dailyMin = dailyObj?.optJSONArray("temperature_2m_min")
                    val dailyCodes = dailyObj?.optJSONArray("weather_code")
                    
                    val parsedForecasts = mutableListOf<WeatherForecastItem>()
                    if (dailyTime != null && dailyMax != null && dailyMin != null && dailyCodes != null) {
                        for (i in 0 until dailyTime.length()) {
                            val timeStr = dailyTime.optString(i)
                            val dayLabel = try {
                                val sdfSource = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                                val date = sdfSource.parse(timeStr)
                                val sdfDest = java.text.SimpleDateFormat("E", java.util.Locale.US)
                                if (i == 0) "Today" else sdfDest.format(date)
                            } catch (e: Exception) {
                                timeStr
                            }
                            
                            val tMaxC = dailyMax.optDouble(i, 30.0)
                            val tMinC = dailyMin.optDouble(i, 22.0)
                            val dCode = dailyCodes.optInt(i, 0)
                            
                            val (fMax, fMin) = if (_uiState.value.isCelsius) {
                                String.format("%.0f°C", tMaxC) to String.format("%.0f°C", tMinC)
                            } else {
                                String.format("%.0f°F", tMaxC * 9/5 + 32) to String.format("%.0f°F", tMinC * 9/5 + 32)
                            }
                            
                            val cond = getWeatherConditionText(dCode)
                            val iconIdx = getWeatherIconIndex(dCode)
                            
                            parsedForecasts.add(
                                WeatherForecastItem(
                                    dayName = dayLabel,
                                    tempMax = fMax,
                                    tempMin = fMin,
                                    condition = cond,
                                    iconIndex = iconIdx
                                )
                            )
                        }
                    }
                    
                    val (currentTempFormatted, conditionText, iconIdx) = if (_uiState.value.isCelsius) {
                        Triple(String.format("%.0f°C", tempC), getWeatherConditionText(code), getWeatherIconIndex(code))
                    } else {
                        Triple(String.format("%.0f°F", tempC * 9/5 + 32), getWeatherConditionText(code), getWeatherIconIndex(code))
                    }
                    
                    _uiState.update { state ->
                        state.copy(
                            weatherTemp = currentTempFormatted,
                            weatherCondition = conditionText,
                            weatherIconIndex = iconIdx,
                            weatherForecasts = parsedForecasts,
                            isWeatherLoading = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(isWeatherLoading = false) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(isWeatherLoading = false) }
            }
        }
    }

    fun fetchSearchSuggestions(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            _uiState.update { it.copy(searchSuggestions = emptyList()) }
            return
        }
        
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || (trimmed.contains(".") && !trimmed.contains(" "))) {
            _uiState.update { it.copy(searchSuggestions = emptyList()) }
            return
        }
        
        _uiState.update { it.copy(isSearchingSuggestions = true) }
        
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val urlStr = "https://suggestqueries.google.com/complete/search?client=firefox&q=${URLEncoder.encode(trimmed, "UTF-8")}"
                val url = java.net.URL(urlStr)
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 3000
                conn.readTimeout = 3000
                
                if (conn.responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    val jsonArray = org.json.JSONArray(response)
                    val suggestionsArr = jsonArray.optJSONArray(1)
                    val suggestions = mutableListOf<String>()
                    if (suggestionsArr != null) {
                        for (i in 0 until kotlin.math.min(suggestionsArr.length(), 6)) {
                            suggestions.add(suggestionsArr.optString(i))
                        }
                    }
                    _uiState.update { 
                        it.copy(
                            searchSuggestions = suggestions,
                            isSearchingSuggestions = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(isSearchingSuggestions = false) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(isSearchingSuggestions = false) }
            }
        }
    }
}
