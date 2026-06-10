package com.example

import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel,
    webViewMap: MutableMap<String, WebView>,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val activeTab = state.tabs.find { it.id == state.activeTabId } ?: state.tabs.firstOrNull()
    val focusManager = LocalFocusManager.current
    var isSearchFocused by remember { mutableStateOf(false) }

    // Intercept hardware back button to navigate WebView back first if possible
    BackHandler {
        if (state.showWeatherDetails) {
            viewModel.toggleWeatherDetails(false)
        } else if (state.showProfileScreen) {
            viewModel.toggleProfileScreen(false)
        } else if (state.showDownloadsScreen) {
            viewModel.toggleDownloadsScreen(false)
        } else if (state.showTabSwitcher) {
            viewModel.closeTabSwitcher()
        } else if (activeTab?.canGoBack == true) {
            viewModel.goBackActiveTab()
        } else if (activeTab?.currentUrl?.isNotEmpty() == true) {
            // If on a page but cannot go back, return to homepage
            viewModel.loadUrlInTab(state.activeTabId, "")
        } else {
            // Close the browser / minimize
        }
    }

    val isIncognito = state.isIncognitoMode
    val isDownloadsOpen = state.showDownloadsScreen

    val topBlobColor = remember(state.themeAccentIndex, isIncognito) {
        if (isIncognito) {
            Color(0x4C3B0764)
        } else {
            when (state.themeAccentIndex) {
                1 -> Color(0x552563EB)
                2 -> Color(0x55059669)
                3 -> Color(0x55D97706)
                4 -> Color(0x55DB2777)
                else -> Color(0x557C3AED)
            }
        }
    }

    val bottomBlobColor = remember(state.themeAccentIndex, isIncognito) {
        if (isIncognito) {
            Color(0x3B581C87)
        } else {
            when (state.themeAccentIndex) {
                1 -> Color(0x453B82F6)
                2 -> Color(0x4510B981)
                3 -> Color(0x45F59E0B)
                4 -> Color(0x45EC4899)
                else -> Color(0x458B5CF6)
            }
        }
    }

    val centerBlobColor = remember(state.themeAccentIndex, isIncognito) {
        if (isIncognito) {
            Color(0x2E4A044E)
        } else {
            when (state.themeAccentIndex) {
                1 -> Color(0x4060A5FA)
                2 -> Color(0x4034D399)
                3 -> Color(0x40FBBF24)
                4 -> Color(0x40F472B6)
                else -> Color(0x40A78BFA)
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "fluid_background")

    val driftX1 by infiniteTransition.animateFloat(
        initialValue = -60f,
        targetValue = 60f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift_x1"
    )

    val driftY1 by infiniteTransition.animateFloat(
        initialValue = -40f,
        targetValue = 40f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift_y1"
    )

    val driftX2 by infiniteTransition.animateFloat(
        initialValue = 50f,
        targetValue = -50f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift_x2"
    )

    val driftY2 by infiniteTransition.animateFloat(
        initialValue = -50f,
        targetValue = 50f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift_y2"
    )

    val scale1 by infiniteTransition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale_1"
    )

    val scale2 by infiniteTransition.animateFloat(
        initialValue = 1.18f,
        targetValue = 0.82f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 11000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale_2"
    )

    val driftX3 by infiniteTransition.animateFloat(
        initialValue = -35f,
        targetValue = 35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift_x3"
    )

    val driftY3 by infiniteTransition.animateFloat(
        initialValue = 55f,
        targetValue = -55f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift_y3"
    )

    val scale3 by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale_3"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isIncognito) Color(0xFF06040A) else Color(0xFF0C0B12)) // Dark midnight purple-black base canvas
    ) {
        // Floating Top-Right Violet/Purple Fluid Blob
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (80 + driftX1).dp, y = ((-80) + driftY1).dp)
                .size((320 * scale1).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(topBlobColor, Color.Transparent),
                        radius = 450f
                    )
                )
        )

        // Floating Bottom-Left Grape/Indigo Fluid Blob
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = ((-80) + driftX2).dp, y = (120 + driftY2).dp)
                .size((320 * scale2).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(bottomBlobColor, Color.Transparent),
                        radius = 450f
                    )
                )
        )

        // Floating Center-Right Orchid Fluid Blob
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = (50 + driftX3).dp, y = (0 + driftY3).dp)
                .size((250 * scale3).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(centerBlobColor, Color.Transparent),
                        radius = 350f
                    )
                )
        )

        // Main Content Layer
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            if (activeTab != null) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (activeTab.currentUrl.isEmpty()) {
                        // HOMEPAGE
                        BrowserHomepage(
                            state = state,
                            viewModel = viewModel,
                            onSearchSubmit = { query ->
                                viewModel.submitSearch(query)
                            },
                            onQuickLinkClick = { url ->
                                viewModel.loadUrlInTab(state.activeTabId, url)
                            },
                            onQuickLinkDelete = { link ->
                                viewModel.deleteQuickLink(link)
                            },
                            onAddShortcutClick = {
                                viewModel.toggleAddShortcutDialog(true)
                            },
                            onProfileClick = {
                                viewModel.toggleProfileScreen(true)
                            },
                            onWeatherClick = {
                                viewModel.toggleWeatherDetails(true)
                            }
                        )
                    } else {
                        // WEB VIEW OR CUSTOM ERROR PAGE
                        if (activeTab.error != null) {
                            CustomErrorOverlay(
                                tab = activeTab,
                                onRetry = { viewModel.reloadTab(activeTab.id) },
                                onHome = { viewModel.loadUrlInTab(activeTab.id, "") }
                            )
                        } else {
                            BrowserWebViewContainer(
                                tabId = activeTab.id,
                                viewModel = viewModel,
                                webViewMap = webViewMap,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }

        // Floating Loading Progress Bar (right above bottom bar)
        if (activeTab?.isLoading == true && activeTab.currentUrl.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 96.dp)
                    .height(2.dp)
                    .background(Color.White.copy(alpha = 0.05f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = (activeTab.progress ?: 0) / 100f)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF3B82F6), Color(0xFF8B5CF6))
                            )
                        )
                        .shadow(4.dp)
                )
            }
        }

        // Glassmorphic Floating Bottom Navigation/Control Bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            GlassmorphicBottomBar(
                state = state,
                activeTab = activeTab,
                isSearchFocused = isSearchFocused,
                onSearchFocusChanged = { focused -> isSearchFocused = focused },
                viewModel = viewModel,
                onSubmitSearch = { query ->
                    viewModel.submitSearch(query)
                    focusManager.clearFocus()
                }
            )
        }

        // Tab Swapper Sheet overlay
        if (state.showTabSwitcher) {
            TabSwitcherOverlay(
                state = state,
                onTabSelect = { tabId -> viewModel.selectTab(tabId) },
                onTabClose = { tabId -> viewModel.closeTab(tabId) { webViewMap[it]?.destroy(); webViewMap.remove(it) } },
                onNewTab = { viewModel.addNewTab("") },
                onCloseSwitcher = { viewModel.toggleTabSwitcher() }
            )
        }

        // Add Shortcut Dialog
        if (state.showAddShortcutDialog) {
            AddShortcutDialog(
                onDismiss = { viewModel.toggleAddShortcutDialog(false) },
                onAdd = { title, url ->
                    viewModel.addQuickLink(title, url)
                    viewModel.toggleAddShortcutDialog(false)
                }
            )
        }

        // Downloads Screen Overlay
        if (isDownloadsOpen) {
            DownloadsScreenOverlay(
                state = state,
                onPause = { viewModel.pauseDownload(it) },
                onResume = { viewModel.resumeDownload(it) },
                onCancel = { viewModel.cancelDownload(it) },
                onOpen = { viewModel.openDownloadedFile(it) },
                onClose = { viewModel.toggleDownloadsScreen(false) }
            )
        }

        // Profile Screen Overlay
        if (state.showProfileScreen) {
            ProfileScreenOverlay(
                state = state,
                onSave = { name, email, bio, avatarIndex, defaultSearch, themeIndex ->
                    viewModel.updateProfile(name, email, bio, avatarIndex, defaultSearch, themeIndex)
                },
                onClose = { viewModel.toggleProfileScreen(false) }
            )
        }

        // Floating suggestions card for active browsing page/bottom address bar
        if (isSearchFocused && state.searchSuggestions.isNotEmpty() && activeTab?.currentUrl?.isNotEmpty() == true) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .shadow(24.dp, RoundedCornerShape(18.dp))
                    .border(
                        1.2.dp,
                        Color.White.copy(alpha = 0.12f),
                        RoundedCornerShape(18.dp)
                    )
                    .background(Color(0xF20D0C11))
                    .clickable(enabled = false) {}
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SUGGESTIONS",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.Gray
                        )
                        if (state.isSearchingSuggestions) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 1.5.dp,
                                color = Color(0xFF8B5CF6)
                            )
                        }
                    }
                    
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                    
                    state.searchSuggestions.forEach { suggestion ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.submitSearch(suggestion)
                                    isSearchFocused = false
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = suggestion,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }

        // Weather Forecast details Overlay
        if (state.showWeatherDetails) {
            WeatherForecastOverlay(
                state = state,
                onClose = { viewModel.toggleWeatherDetails(false) },
                onSelectCity = { cityName -> viewModel.fetchWeatherForCity(cityName) },
                onToggleUnit = { viewModel.toggleWeatherUnit() },
                onDetectLocation = { viewModel.detectAndFetchLiveLocationWeather() }
            )
        }
    }
}

@Composable
fun AuraBrandHeader(
    state: BrowserState,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "aura_brand")
    
    // Smooth breathing glow cycle
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowScale"
    )

    // Fluid neon intensity pulse
    val neonIntensity by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "neonIntensity"
    )

    // Theme color mappings
    val (primaryColor, secondaryColor, neonGlowColor) = remember(state.themeAccentIndex) {
        when (state.themeAccentIndex) {
            1 -> Triple(Color(0xFF3B82F6), Color(0xFF60A5FA), Color(0xFF2563EB))
            2 -> Triple(Color(0xFF10B981), Color(0xFF34D399), Color(0xFF059669))
            3 -> Triple(Color(0xFFF59E0B), Color(0xFFFBBF24), Color(0xFFD97706))
            4 -> Triple(Color(0xFFEC4899), Color(0xFFF472B6), Color(0xFFDB2777))
            else -> Triple(Color(0xFF8B5CF6), Color(0xFFA78BFA), Color(0xFF7C3AED))
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            // Background ambient blur layer to create the "glow" texture
            Text(
                text = "AURA",
                style = androidx.compose.ui.text.TextStyle(
                    fontSize = 58.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 16.sp,
                    color = neonGlowColor.copy(alpha = 0.25f * neonIntensity),
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = neonGlowColor,
                        offset = androidx.compose.ui.geometry.Offset(0f, 0f),
                        blurRadius = 45f * glowScale
                    )
                ),
                textAlign = TextAlign.Center
            )

            // Dynamic Stroke styling (glowing border of the letters in high-contrast)
            Text(
                text = "AURA",
                style = androidx.compose.ui.text.TextStyle(
                    fontSize = 58.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 16.sp,
                    color = Color.Transparent,
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = secondaryColor.copy(alpha = 0.8f),
                        offset = androidx.compose.ui.geometry.Offset(0f, 0f),
                        blurRadius = 15f * glowScale
                    )
                ),
                textAlign = TextAlign.Center
            )

            // Inner Fluid fill with rich animated multi-color linear gradient brush
            Text(
                text = "AURA",
                style = androidx.compose.ui.text.TextStyle(
                    fontSize = 58.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 16.sp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            primaryColor,
                            secondaryColor,
                            primaryColor.copy(alpha = 0.7f),
                            secondaryColor
                        )
                    )
                ),
                textAlign = TextAlign.Center
            )
        }

        // Elegant minimal subtitle with tracking and small neon spacer
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(width = 20.dp, height = 2.dp)
                    .background(
                        Brush.linearGradient(listOf(primaryColor, Color.Transparent)),
                        RoundedCornerShape(1.dp)
                    )
            )
            Text(
                text = "NEXT-GEN BROWSER",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
                    color = Color.White.copy(alpha = 0.5f)
                ),
                modifier = Modifier.padding(horizontal = 10.dp)
            )
            Box(
                modifier = Modifier
                    .size(width = 20.dp, height = 2.dp)
                    .background(
                        Brush.linearGradient(listOf(Color.Transparent, primaryColor)),
                        RoundedCornerShape(1.dp)
                    )
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BrowserHomepage(
    state: BrowserState,
    viewModel: BrowserViewModel,
    onSearchSubmit: (String) -> Unit,
    onQuickLinkClick: (String) -> Unit,
    onQuickLinkDelete: (QuickLink) -> Unit,
    onAddShortcutClick: () -> Unit,
    onProfileClick: () -> Unit,
    onWeatherClick: () -> Unit
) {
    var searchInput by remember { mutableStateOf("") }
    var currentTimeMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTimeMillis = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000)
        }
    }

    val dateAndTimeString = remember(currentTimeMillis) {
        val sdf = SimpleDateFormat("EEEE, MMM d • h:mm:ss a", Locale.getDefault())
        sdf.format(Date(currentTimeMillis))
    }

    val greeting = remember(currentTimeMillis) {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentTimeMillis
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else -> "Good night"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 20.dp, bottom = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Bar: Greeting & Weather Display (Inspired by Frosted Glass mockup)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (state.isIncognitoMode) "INCOGNITO ACTIVE • $dateAndTimeString" else dateAndTimeString,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = if (state.isIncognitoMode) Color(0xFFC084FC) else Color(0xFFA78BFA),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (state.isIncognitoMode) {
                        Icon(
                            imageVector = Icons.Default.VisibilityOff,
                            contentDescription = "Incognito active",
                            tint = Color(0xFFC084FC),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = if (state.isIncognitoMode) "Private Space" else greeting,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = Color.White
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Interactive Real Weather Forecast Indicator
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onWeatherClick() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("homepage_weather_card")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when (state.weatherIconIndex) {
                                1 -> Icons.Default.Cloud
                                2 -> Icons.Default.Umbrella
                                3 -> Icons.Default.AcUnit
                                4 -> Icons.Default.FlashOn
                                else -> Icons.Default.WbSunny
                            },
                            contentDescription = "Weather Icon",
                            tint = when (state.weatherIconIndex) {
                                1 -> Color(0xFFCBD5E1)
                                2 -> Color(0xFF60A5FA)
                                3 -> Color(0xFF93C5FD)
                                4 -> Color(0xFFFBBF24)
                                else -> Color(0xFFFBBF24)
                            },
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = state.weatherTemp,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                    Text(
                        text = state.weatherCondition.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        ),
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }

                // Interactive Dynamic Profile Avatar Button!
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(
                                when (state.themeAccentIndex) {
                                    1 -> listOf(Color(0xFF2563EB), Color(0xFF60A5FA), Color(0xFF2563EB))
                                    2 -> listOf(Color(0xFF059669), Color(0xFF34D399), Color(0xFF059669))
                                    3 -> listOf(Color(0xFFD97706), Color(0xFFFBBF24), Color(0xFFD97706))
                                    4 -> listOf(Color(0xFFDB2777), Color(0xFFF472B6), Color(0xFFDB2777))
                                    else -> listOf(Color(0xFF7C3AED), Color(0xFFA78BFA), Color(0xFF7C3AED))
                                }
                            )
                        )
                        .border(
                            1.5.dp,
                            Color.White.copy(alpha = 0.4f),
                            CircleShape
                        )
                        .clickable { onProfileClick() }
                        .testTag("homepage_profile_avatar"),
                    contentAlignment = Alignment.Center
                ) {
                    val initials = if (state.profileName.isNotEmpty()) {
                        state.profileName.take(1).uppercase()
                    } else {
                        "B"
                    }
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    )
                }
            }
        }

        // Center Content Area: Search and Speed Dial grid
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            AuraBrandHeader(state = state)

            // Neon Glow Search Bar
            val infiniteTransition = rememberInfiniteTransition(label = "glow")
            val glowAlpha by infiniteTransition.animateFloat(
                initialValue = 0.25f,
                targetValue = 0.6f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2500, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "glowAlpha"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(16.dp, RoundedCornerShape(16.dp), spotColor = Color(0xFF7C3AED))
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF3B82F6).copy(alpha = glowAlpha),
                                Color(0xFF7C3AED).copy(alpha = glowAlpha + 0.2f)
                            )
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .background(Color(0xFF16161A), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Icon",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    TextField(
                        value = searchInput,
                        onValueChange = { 
                            searchInput = it
                            viewModel.onSearchInputChange(it)
                        },
                        placeholder = {
                            Text(
                                "Search or type URL",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_search_input"),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            if (searchInput.isNotEmpty()) {
                                onSearchSubmit(searchInput)
                            }
                        }),
                        singleLine = true
                    )
                    if (searchInput.isNotEmpty()) {
                        IconButton(onClick = { 
                            searchInput = ""
                            viewModel.onSearchInputChange("")
                        }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear text",
                                tint = Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            if (searchInput.isNotEmpty() && state.searchSuggestions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(16.dp, RoundedCornerShape(16.dp))
                        .border(
                            1.dp,
                            Color.White.copy(alpha = 0.08f),
                            RoundedCornerShape(16.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = Color(0xF2141416)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        state.searchSuggestions.forEach { suggestion ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        searchInput = suggestion
                                        onSearchSubmit(suggestion)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = suggestion,
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Quick Links Title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Speed Dial",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.8f),
                        letterSpacing = 0.5.sp
                    )
                )
                IconButton(
                    onClick = onAddShortcutClick,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("add_shortcut_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add shortcut",
                        tint = Color(0xFFA78BFA),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Frosted Grid of Quick Links
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.quickLinks) { link ->
                    var showDeleteMenu by remember { mutableStateOf(false) }

                    Box(
                        contentAlignment = Alignment.TopEnd
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .combinedClickable(
                                    onClick = { onQuickLinkClick(link.url) },
                                    onLongClick = { showDeleteMenu = true }
                                )
                                .padding(vertical = 4.dp)
                        ) {
                            // Frosted glass wrapper: bg-white/5 order border-white/10
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                // Inner color solid icon container with shadow-lg
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .shadow(8.dp, RoundedCornerShape(10.dp), spotColor = Color(link.colorHex))
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(link.colorHex)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = link.iconLetter,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = link.title,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = Color.White.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Delete link dropdown option when long pressed
                        DropdownMenu(
                            expanded = showDeleteMenu,
                            onDismissRequest = { showDeleteMenu = false },
                            modifier = Modifier.background(Color(0xFF1E1E24))
                        ) {
                            DropdownMenuItem(
                                text = { Text("Delete Shortcut", color = Color(0xFFEF4444)) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444)) },
                                onClick = {
                                    onQuickLinkDelete(link)
                                    showDeleteMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Ambient Indicator placeholder line or spacing at the base
        Spacer(modifier = Modifier.height(1.dp))
    }
}

@Composable
fun GlassmorphicBottomBar(
    state: BrowserState,
    activeTab: BrowserTab?,
    isSearchFocused: Boolean,
    onSearchFocusChanged: (Boolean) -> Unit,
    viewModel: BrowserViewModel,
    onSubmitSearch: (String) -> Unit
) {
    var searchInput by remember(state.searchInput) { mutableStateOf(state.searchInput) }
    var showMenu by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .shadow(16.dp, RoundedCornerShape(24.dp))
            .background(
                color = Color(0xDC0C0C0E), // High transparency elegant glass dark gray
                shape = RoundedCornerShape(24.dp)
            )
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.16f),
                        Color.White.copy(alpha = 0.04f),
                        Color(0xFF8B5CF6).copy(alpha = 0.22f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Expand Animation for search field
        AnimatedVisibility(
            visible = !isSearchFocused,
            enter = fadeIn() + expandHorizontally(),
            exit = fadeOut() + shrinkHorizontally()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { viewModel.goBackActiveTab() },
                    enabled = activeTab?.canGoBack == true,
                    modifier = Modifier.testTag("back_button"),
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = Color.White,
                        disabledContentColor = Color.DarkGray
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }

                IconButton(
                    onClick = { viewModel.goForwardActiveTab() },
                    enabled = activeTab?.canGoForward == true,
                    modifier = Modifier.testTag("forward_button"),
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = Color.White,
                        disabledContentColor = Color.DarkGray
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Forward"
                    )
                }
            }
        }

        // Center Omnibox / Input Field
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(23.dp))
                    .border(
                        1.dp,
                        if (isSearchFocused) Color(0xFF8B5CF6) else Color.White.copy(alpha = 0.08f),
                        RoundedCornerShape(23.dp)
                    )
                    .background(Color(0xFF141416))
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = if (activeTab?.currentUrl?.startsWith("https://") == true) Icons.Default.Lock else Icons.Default.Search,
                        contentDescription = "Status",
                        tint = if (activeTab?.currentUrl?.startsWith("https://") == true) Color(0xFF10B981) else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))

                    TextField(
                        value = searchInput,
                        onValueChange = {
                            searchInput = it
                            viewModel.onSearchInputChange(it)
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            onSubmitSearch(searchInput)
                        }),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.LightGray
                        ),
                        placeholder = {
                            Text(
                                "Navigate or Search",
                                color = Color.Gray,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester)
                            .onFocusChanged { state ->
                                onSearchFocusChanged(state.isFocused)
                            }
                            .testTag("omnibox_input")
                    )

                    if (isSearchFocused) {
                        IconButton(onClick = {
                            searchInput = ""
                            viewModel.onSearchInputChange("")
                        }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Action Buttons
        AnimatedVisibility(
            visible = !isSearchFocused,
            enter = fadeIn() + expandHorizontally(),
            exit = fadeOut() + shrinkHorizontally()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Tabs Count Button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { viewModel.toggleTabSwitcher() }
                        .border(1.2.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.tabs.size.toString(),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Options/More Dialog Menu Button
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More",
                            tint = Color.White
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(Color(0xFF1E1E24))
                    ) {
                        DropdownMenuItem(
                            text = { Text("New Tab", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, tint = Color.LightGray) },
                            onClick = {
                                viewModel.addNewTab("")
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (state.isIncognitoMode) "Exit Private Mode" else "New Private Tab", color = if (state.isIncognitoMode) Color(0xFFC084FC) else Color.White) },
                            leadingIcon = { Icon(imageVector = if (state.isIncognitoMode) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null, tint = if (state.isIncognitoMode) Color(0xFFC084FC) else Color.LightGray) },
                            onClick = {
                                viewModel.toggleIncognitoMode()
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Downloads", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.Download, contentDescription = null, tint = Color.LightGray) },
                            onClick = {
                                viewModel.toggleDownloadsScreen(true)
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Reload", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.LightGray) },
                            onClick = {
                                viewModel.reloadActiveTab()
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Go Home", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.Home, contentDescription = null, tint = Color.LightGray) },
                            onClick = {
                                viewModel.loadUrlInTab(state.activeTabId, "")
                                showMenu = false
                            }
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        DropdownMenuItem(
                            text = { Text("User Profile", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color.LightGray) },
                            modifier = Modifier.testTag("menu_profile"),
                            onClick = {
                                viewModel.toggleProfileScreen(true)
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Tabs Switcher", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.Layers, contentDescription = null, tint = Color.LightGray) },
                            onClick = {
                                viewModel.toggleTabSwitcher()
                                showMenu = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TabSwitcherOverlay(
    state: BrowserState,
    onTabSelect: (String) -> Unit,
    onTabClose: (String) -> Unit,
    onNewTab: () -> Unit,
    onCloseSwitcher: () -> Unit
) {
    val visibleTabs = remember(state.tabs, state.isIncognitoMode) {
        state.tabs.filter { it.isIncognito == state.isIncognitoMode }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (state.isIncognitoMode) Color(0xF2060608) else Color(0xE60A0A0C)) // Frosted dark/purple back layer
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (state.isIncognitoMode) "${visibleTabs.size} Private Tabs" else "${visibleTabs.size} Active Tabs",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (state.isIncognitoMode) Color(0xFFC084FC) else Color.White
                )

                IconButton(
                    onClick = onCloseSwitcher,
                    modifier = Modifier.background(Color.White.copy(alpha = 0.06f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close switcher",
                        tint = Color.White
                    )
                }
            }

            // Grid of open tab cards
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(visibleTabs) { tab ->
                    val isActive = tab.id == state.activeTabId
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .shadow(8.dp, RoundedCornerShape(16.dp))
                            .border(
                                width = if (isActive) 2.dp else 1.2.dp,
                                brush = if (isActive) {
                                    Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFF3B82F6)))
                                } else {
                                    SolidColor(Color.White.copy(alpha = 0.08f))
                                },
                                shape = RoundedCornerShape(16.dp)
                            )
                            .background(Color(0xFF16161A), RoundedCornerShape(16.dp))
                            .clickable { onTabSelect(tab.id) }
                            .padding(12.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                            // Tab Card Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(6.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = tab.title.take(1).uppercase(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.LightGray
                                    )
                                }
                                
                                IconButton(
                                    onClick = { onTabClose(tab.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Cancel,
                                        contentDescription = "Close tab",
                                        tint = Color.White.copy(alpha = 0.4f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Dynamic Preview placeholder (futuristic dashboard theme lines)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = null,
                                        tint = Color.DarkGray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (tab.currentUrl.isEmpty()) "Homepage" else "Web Page",
                                        fontSize = 9.sp,
                                        color = Color.DarkGray
                                    )
                                }
                            }

                            // Tab Card Footer
                            Column {
                                Text(
                                    text = tab.title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (tab.currentUrl.isEmpty()) "aero://home" else tab.currentUrl,
                                    fontSize = 9.sp,
                                    color = Color.Gray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Floating New Tab button at the bottom center of Tab Switcher
            Button(
                onClick = onNewTab,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF8B5CF6),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .height(52.dp)
                    .testTag("tab_switcher_new_tab_btn")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open New Tab", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CustomErrorOverlay(
    tab: BrowserTab,
    onRetry: () -> Unit,
    onHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0C0E))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(Color(0x1AEF4444), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.WifiOff,
                contentDescription = "Connection lost",
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Page Loading Failed",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = tab.error ?: "An unknown network discovery error occurred.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onHome,
                border = BorderStroke(1.2.dp, Color.White.copy(alpha = 0.15f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Home, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Go Home")
            }

            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF8B5CF6),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Retry Page")
            }
        }
    }
}

@Composable
fun AddShortcutDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Add Dynamic Quick Link",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        containerColor = Color(0xFF16161A),
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Shortcut Name", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF8B5CF6),
                        unfocusedBorderColor = Color.DarkGray
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_link_title_input")
                )

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Web Address URL", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF8B5CF6),
                        unfocusedBorderColor = Color.DarkGray
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_link_url_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotEmpty() && url.isNotEmpty()) {
                        onAdd(title, url)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF8B5CF6)
                ),
                enabled = title.isNotEmpty() && url.isNotEmpty()
            ) {
                Text("Confirm", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.LightGray)
            }
        }
    )
}

@Composable
fun DownloadsScreenOverlay(
    state: BrowserState,
    onPause: (Long) -> Unit,
    onResume: (Long) -> Unit,
    onCancel: (Long) -> Unit,
    onOpen: (Long) -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0C0E))
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        tint = Color(0xFFA78BFA),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Download Manager",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
                
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.background(Color.White.copy(alpha = 0.06f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Downloads",
                        tint = Color.White
                    )
                }
            }
            
            if (state.downloads.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = Color.DarkGray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Downloads Found",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Files enqueued from web pages will list here",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.DarkGray
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.downloads) { item ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                                .background(Color(0xFF16161A))
                                .padding(16.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.filename,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${item.sizeString} • ${item.status.name}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray
                                        )
                                    }
                                    
                                    val statusIconColor = when (item.status) {
                                        DownloadStatus.DOWNLOADING -> Color(0xFF3B82F6)
                                        DownloadStatus.COMPLETED -> Color(0xFF10B981)
                                        DownloadStatus.FAILED -> Color(0xFFEF4444)
                                        DownloadStatus.PAUSED -> Color(0xFFFBBF24)
                                        DownloadStatus.CANCELLED -> Color.Gray
                                    }
                                    
                                    val statusIcon = when (item.status) {
                                        DownloadStatus.DOWNLOADING -> Icons.Default.Refresh
                                        DownloadStatus.COMPLETED -> Icons.Default.CheckCircle
                                        DownloadStatus.FAILED -> Icons.Default.Error
                                        DownloadStatus.PAUSED -> Icons.Default.Pause
                                        DownloadStatus.CANCELLED -> Icons.Default.Cancel
                                    }
                                    
                                    Icon(
                                        imageVector = statusIcon,
                                        contentDescription = "Status",
                                        tint = statusIconColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                
                                if (item.status == DownloadStatus.DOWNLOADING || item.status == DownloadStatus.PAUSED) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        LinearProgressIndicator(
                                            progress = { item.progress / 100f },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(6.dp)
                                                .clip(CircleShape),
                                            color = Color(0xFF8B5CF6),
                                            trackColor = Color.White.copy(alpha = 0.08f)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "${item.progress}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (item.status == DownloadStatus.DOWNLOADING) {
                                        TextButton(onClick = { onPause(item.id) }) {
                                            Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFFBBF24))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Pause", color = Color(0xFFFBBF24))
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                    } else if (item.status == DownloadStatus.PAUSED) {
                                        TextButton(onClick = { onResume(item.id) }) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF10B981))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Resume", color = Color(0xFF10B981))
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    
                                    if (item.status == DownloadStatus.DOWNLOADING || item.status == DownloadStatus.PAUSED) {
                                        TextButton(onClick = { onCancel(item.id) }) {
                                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFEF4444))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Cancel", color = Color(0xFFEF4444))
                                        }
                                    }
                                    
                                    if (item.status == DownloadStatus.COMPLETED) {
                                        Button(
                                            onClick = { onOpen(item.id) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF8B5CF6)
                                            ),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Launch,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Open File")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileScreenOverlay(
    state: BrowserState,
    onSave: (String, String, String, Int, String, Int) -> Unit,
    onClose: () -> Unit
) {
    var name by remember { mutableStateOf(state.profileName) }
    var email by remember { mutableStateOf(state.profileEmail) }
    var bio by remember { mutableStateOf(state.profileBio) }
    var avatarIndex by remember { mutableStateOf(state.profileAvatarIndex) }
    var searchEngine by remember { mutableStateOf(state.defaultSearchEngine) }
    var themeIndex by remember { mutableStateOf(state.themeAccentIndex) }
    val isIncognito = state.isIncognitoMode

    val scrollState = rememberScrollState()

    // Base deep dark canvas
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isIncognito) Color(0xF206040A) else Color(0xE60C0B12))
            .padding(16.dp)
            .clickable(enabled = false) {} // block clicks falling behind
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .statusBarsPadding()
        ) {
            // Header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = if (isIncognito) Color(0xFFC084FC) else {
                            when (themeIndex) {
                                1 -> Color(0xFF3B82F6)
                                2 -> Color(0xFF10B981)
                                3 -> Color(0xFFF59E0B)
                                4 -> Color(0xFFEC4899)
                                else -> Color(0xFF8B5CF6)
                            }
                        },
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "User Profile",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = Color.White
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.background(Color.White.copy(alpha = 0.06f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Profile",
                        tint = Color.White
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Interactive Card Header with dynamic theme brush
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                when (themeIndex) {
                                    1 -> listOf(Color(0xFF1E3A8A), Color(0xFF3B82F6))
                                    2 -> listOf(Color(0xFF064E3B), Color(0xFF10B981))
                                    3 -> listOf(Color(0xFF78350F), Color(0xFFF59E0B))
                                    4 -> listOf(Color(0xFF831843), Color(0xFFEC4899))
                                    else -> listOf(Color(0xFF4C1D95), Color(0xFF8B5CF6))
                                }
                            )
                        )
                        .border(
                            1.dp,
                            Color.White.copy(alpha = 0.15f),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Monogram Avatar
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.18f))
                                .border(2.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (name.isNotEmpty()) name.take(1).uppercase() else "B",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = if (name.isNotEmpty()) name else "Assign Name",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (email.isNotEmpty()) email else "your.email@domain.com",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (bio.isNotEmpty()) bio else "No bio assigned yet.",
                                style = MaterialTheme.typography.bodySmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                                color = Color.White.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Stats Dashboard Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(
                        "Tabs" to state.tabs.size.toString(),
                        "Downloads" to state.downloads.size.toString(),
                        "Engine" to searchEngine
                    ).forEach { (label, value) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.04f))
                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = value,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isIncognito) Color(0xFFC084FC) else {
                                            when (themeIndex) {
                                                1 -> Color(0xFF3B82F6)
                                                2 -> Color(0xFF10B981)
                                                3 -> Color(0xFFF59E0B)
                                                4 -> Color(0xFFEC4899)
                                                else -> Color(0xFF8B5CF6)
                                            }
                                        }
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }

                // Fields Section Title
                Text(
                    text = "Edit Credentials",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = Color.Gray
                )

                // Name Input Field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name", color = Color.Gray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = if (isIncognito) Color(0xFFC084FC) else {
                            when (themeIndex) {
                                1 -> Color(0xFF3B82F6)
                                2 -> Color(0xFF10B981)
                                3 -> Color(0xFFF59E0B)
                                4 -> Color(0xFFEC4899)
                                else -> Color(0xFF8B5CF6)
                            }
                        },
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_name_input")
                )

                // Email Input Field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address", color = Color.Gray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = if (isIncognito) Color(0xFFC084FC) else {
                            when (themeIndex) {
                                1 -> Color(0xFF3B82F6)
                                2 -> Color(0xFF10B981)
                                3 -> Color(0xFFF59E0B)
                                4 -> Color(0xFFEC4899)
                                else -> Color(0xFF8B5CF6)
                            }
                        },
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Bio Input Field
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Short Bio", color = Color.Gray) },
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = if (isIncognito) Color(0xFFC084FC) else {
                            when (themeIndex) {
                                1 -> Color(0xFF3B82F6)
                                2 -> Color(0xFF10B981)
                                3 -> Color(0xFFF59E0B)
                                4 -> Color(0xFFEC4899)
                                else -> Color(0xFF8B5CF6)
                            }
                        },
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Preference Section Title
                Text(
                    text = "Search & Theme Settings",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = Color.Gray
                )

                // Search engine selector
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Primary Search Engine",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Choose search default in active tab queries",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Google", "Bing", "DuckDuckGo", "Yahoo").forEach { engine ->
                                val isSelected = searchEngine == engine
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) {
                                                if (isIncognito) Color(0xFFC084FC).copy(alpha = 0.2f) else {
                                                    when (themeIndex) {
                                                        1 -> Color(0xFF3B82F6).copy(alpha = 0.2f)
                                                        2 -> Color(0xFF10B981).copy(alpha = 0.2f)
                                                        3 -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                                                        4 -> Color(0xFFEC4899).copy(alpha = 0.2f)
                                                        else -> Color(0xFF8B5CF6).copy(alpha = 0.2f)
                                                    }
                                                }
                                            } else Color.White.copy(alpha = 0.04f)
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) {
                                                if (isIncognito) Color(0xFFC084FC) else {
                                                    when (themeIndex) {
                                                        1 -> Color(0xFF3B82F6)
                                                        2 -> Color(0xFF10B981)
                                                        3 -> Color(0xFFF59E0B)
                                                        4 -> Color(0xFFEC4899)
                                                        else -> Color(0xFF8B5CF6)
                                                    }
                                                }
                                            } else Color.White.copy(alpha = 0.05f),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { searchEngine = engine }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = engine,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSelected) Color.White else Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }

                // Theme Accent Color Dot Selector
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Theme Palette Accent",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Shift environment background and buttons mood",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(
                                0 to Color(0xFF8B5CF6), // Purple
                                1 to Color(0xFF3B82F6), // Ocean Blue
                                2 to Color(0xFF10B981), // Mint Green
                                3 to Color(0xFFF59E0B), // Sunset Gold
                                4 to Color(0xFFEC4899)  // Cyber Pink
                            ).forEach { (idx, color) ->
                                val isSelected = themeIndex == idx
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            if (isSelected) 3.dp else 0.dp,
                                            Color.White,
                                            CircleShape
                                        )
                                        .clickable {
                                            themeIndex = idx
                                            avatarIndex = idx
                                        }
                                )
                            }
                        }
                    }
                }

                // Privacy Actions selector
                var cacheClearedMsg by remember { mutableStateOf(false) }
                if (cacheClearedMsg) {
                    androidx.compose.runtime.LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(2000)
                        cacheClearedMsg = false
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Emergency Clear Cache",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Clear temporary session tabs & data instantly",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }

                        Button(
                            onClick = { cacheClearedMsg = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (cacheClearedMsg) Color(0xFF10B981) else Color(0xFFEF4444).copy(alpha = 0.3f),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (cacheClearedMsg) "Done!" else "Clear",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Save & Cancel Footer Buttons
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onClose,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Text("Cancel", color = Color.White)
                }

                Button(
                    onClick = {
                        onSave(name, email, bio, avatarIndex, searchEngine, themeIndex)
                        onClose()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("profile_save_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isIncognito) Color(0xFF8B5CF6) else {
                            when (themeIndex) {
                                1 -> Color(0xFF3B82F6)
                                2 -> Color(0xFF10B981)
                                3 -> Color(0xFFF59E0B)
                                4 -> Color(0xFFEC4899)
                                else -> Color(0xFF8B5CF6)
                            }
                        }
                    )
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Changes")
                }
            }
        }
    }
}

@Composable
fun WeatherForecastOverlay(
    state: BrowserState,
    onClose: () -> Unit,
    onSelectCity: (String) -> Unit,
    onToggleUnit: () -> Unit,
    onDetectLocation: () -> Unit
) {
    val scrollState = rememberScrollState()
    val isIncognito = state.isIncognitoMode
    val currentCity = state.weatherLocationName
    
    val popularCities = listOf(
        "Kolkata", "New York", "London", "Tokyo", "Paris", 
        "San Francisco", "Sydney", "Mumbai", "Delhi", "Bangalore"
    )

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        onDetectLocation()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isIncognito) Color(0xF606040A) else Color(0xF20C0B12))
            .padding(16.dp)
            .clickable(enabled = false) {}
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .statusBarsPadding()
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = null,
                        tint = Color(0xFFA78BFA),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Global Forecasts",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = Color.White
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.background(Color.White.copy(alpha = 0.06f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Forecast",
                        tint = Color.White
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Interactive Selection: City Horizontal List
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "SELECT STATION",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = Color.Gray
                    )
                    
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            val isGPSActive = currentCity.contains("GPS") || currentCity.contains("IP") || currentCity.contains("Location")
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(32.dp))
                                    .background(
                                        if (isGPSActive) Color(0xFF10B981).copy(alpha = 0.25f)
                                        else Color.White.copy(alpha = 0.05f)
                                    )
                                    .border(
                                        1.dp,
                                        if (isGPSActive) Color(0xFF10B981) else Color.White.copy(alpha = 0.08f),
                                        RoundedCornerShape(32.dp)
                                    )
                                    .clickable {
                                        permissionLauncher.launch(
                                            arrayOf(
                                                android.Manifest.permission.ACCESS_FINE_LOCATION,
                                                android.Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = if (isGPSActive) Color(0xFF34D399) else Color(0xFFA78BFA),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Live Location",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isGPSActive) Color.White else Color.LightGray
                                    )
                                }
                            }
                        }

                        items(popularCities) { city ->
                            val isSelected = currentCity.lowercase() == city.lowercase() && !currentCity.contains("GPS") && !currentCity.contains("IP") && !currentCity.contains("Location")
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(32.dp))
                                    .background(
                                        if (isSelected) Color(0xFF8B5CF6).copy(alpha = 0.25f)
                                        else Color.White.copy(alpha = 0.05f)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) Color(0xFF8B5CF6) else Color.White.copy(alpha = 0.08f),
                                        RoundedCornerShape(32.dp)
                                    )
                                    .clickable { onSelectCity(city) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = city,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) Color.White else Color.LightGray
                                )
                            }
                        }
                    }
                }

                // Main Weather Focal Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF3B82F6).copy(alpha = 0.15f),
                                    Color(0xFF8B5CF6).copy(alpha = 0.15f)
                                )
                            )
                        )
                        .border(
                            1.dp,
                            Color.White.copy(alpha = 0.12f),
                            RoundedCornerShape(24.dp)
                        )
                        .padding(24.dp)
                ) {
                    if (state.isWeatherLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFFA78BFA),
                                strokeWidth = 2.dp
                            )
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = currentCity.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp
                                ),
                                color = Color(0xFFA78BFA)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = when (state.weatherIconIndex) {
                                        1 -> Icons.Default.Cloud
                                        2 -> Icons.Default.Umbrella
                                        3 -> Icons.Default.AcUnit
                                        4 -> Icons.Default.FlashOn
                                        else -> Icons.Default.WbSunny
                                    },
                                    contentDescription = null,
                                    tint = when (state.weatherIconIndex) {
                                        1 -> Color(0xFFCBD5E1)
                                        2 -> Color(0xFF60A5FA)
                                        3 -> Color(0xFF93C5FD)
                                        4 -> Color(0xFFFBBF24)
                                        else -> Color(0xFFFBBF24)
                                    },
                                    modifier = Modifier.size(54.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = state.weatherTemp,
                                    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = Color.White
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = state.weatherCondition,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Unit Switcher Button
                            Button(
                                onClick = onToggleUnit,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White.copy(alpha = 0.06f),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = if (state.isCelsius) "Switch to Fahrenheit (°F)" else "Switch to Celsius (°C)",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }

                // Forecast List Title
                Text(
                    text = "7-DAY WEEKLY OUTLOOK",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = Color.Gray
                )

                // Outlook Column
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        state.weatherForecasts.forEachIndexed { idx, forecast ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Day Name Column
                                Text(
                                    text = forecast.dayName,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    modifier = Modifier.width((70).dp)
                                )

                                // Icon and text
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = when (forecast.iconIndex) {
                                            1 -> Icons.Default.Cloud
                                            2 -> Icons.Default.Umbrella
                                            3 -> Icons.Default.AcUnit
                                            4 -> Icons.Default.FlashOn
                                            else -> Icons.Default.WbSunny
                                        },
                                        contentDescription = null,
                                        tint = when (forecast.iconIndex) {
                                            1 -> Color(0xFFCBD5E1)
                                            2 -> Color(0xFF60A5FA)
                                            3 -> Color(0xFF93C5FD)
                                            4 -> Color(0xFFFBBF24)
                                            else -> Color(0xFFFBBF24)
                                        },
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = forecast.condition,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.LightGray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Min - Max temps
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = forecast.tempMax,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                    Text(
                                        text = forecast.tempMin,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.Gray
                                    )
                                }
                            }

                            if (idx < state.weatherForecasts.size - 1) {
                                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

