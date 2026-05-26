package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.example.data.model.ConferenceEntity
import com.example.data.model.PaperEntity
import com.example.data.model.UserProfileEntity
import com.example.network.PlagiarismSource
import com.example.network.RetrofitClient
import com.example.network.RephrasingSuggestion
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.ResearchViewModel
import com.example.ui.viewmodel.ScanUiState
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainLayout(viewModel: ResearchViewModel) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val showSignUpScreen by viewModel.showSignUpScreen.collectAsState()

    if (!isLoggedIn) {
        if (showSignUpScreen) {
            SignUpScreen(viewModel = viewModel)
        } else {
            LoginScreen(viewModel = viewModel)
        }
    } else {
        val currentTab by viewModel.currentTab.collectAsState()
        val papers by viewModel.papers.collectAsState()
        val conferences by viewModel.conferences.collectAsState()
        val userProfile by viewModel.userProfile.collectAsState()
        val selectedPaperId by viewModel.selectedPaperId.collectAsState()

        val currentPaper = selectedPaperId?.let { id -> papers.find { it.id == id } }

        Scaffold(
            bottomBar = {
                NavigationBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars),
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = currentTab == AppTab.DASHBOARD && selectedPaperId == null,
                        onClick = {
                            viewModel.selectPaperToView(null)
                            viewModel.selectTab(AppTab.DASHBOARD)
                        },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                        label = { Text("Dashboard") },
                        modifier = Modifier.testTag("nav_dashboard")
                    )
                    NavigationBarItem(
                        selected = currentTab == AppTab.CHECKER,
                        onClick = {
                            viewModel.selectPaperToView(null)
                            viewModel.selectTab(AppTab.CHECKER)
                        },
                        icon = { Icon(Icons.Default.Add, contentDescription = "Scan Paper") },
                        label = { Text("AI Scan") },
                        modifier = Modifier.testTag("nav_scan")
                    )
                    NavigationBarItem(
                        selected = currentTab == AppTab.CONFERENCES,
                        onClick = {
                            viewModel.selectPaperToView(null)
                            viewModel.selectTab(AppTab.CONFERENCES)
                        },
                        icon = { Icon(Icons.Default.Search, contentDescription = "Conferences") },
                        label = { Text("Conferences") },
                        modifier = Modifier.testTag("nav_conferences")
                    )
                    NavigationBarItem(
                        selected = currentTab == AppTab.HISTORY || selectedPaperId != null,
                        onClick = {
                            viewModel.selectTab(AppTab.HISTORY)
                        },
                        icon = { Icon(Icons.Default.List, contentDescription = "History") },
                        label = { Text("History") },
                        modifier = Modifier.testTag("nav_history")
                    )
                    NavigationBarItem(
                        selected = currentTab == AppTab.PROFILE,
                        onClick = {
                            viewModel.selectPaperToView(null)
                            viewModel.selectTab(AppTab.PROFILE)
                        },
                        icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                        label = { Text("Profile") },
                        modifier = Modifier.testTag("nav_profile")
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when {
                    selectedPaperId != null && currentPaper != null -> {
                        PaperDetailScreen(
                            paper = currentPaper,
                            onBack = { viewModel.selectPaperToView(null) },
                            onDelete = { viewModel.deletePaper(currentPaper) }
                        )
                    }
                    else -> {
                        when (currentTab) {
                            AppTab.DASHBOARD -> DashboardScreen(
                                papers = papers,
                                conferences = conferences,
                                userProfile = userProfile,
                                onNavigateToScan = { viewModel.selectTab(AppTab.CHECKER) },
                                onNavigateToConferences = { viewModel.selectTab(AppTab.CONFERENCES) },
                                onViewPaper = { viewModel.selectPaperToView(it) }
                            )
                            AppTab.CHECKER -> PlagiarismCheckerScreen(
                                viewModel = viewModel
                            )
                            AppTab.CONFERENCES -> ConferencesScreen(
                                viewModel = viewModel,
                                conferences = conferences
                            )
                            AppTab.HISTORY -> HistoryScreen(
                                papers = papers,
                                onViewPaper = { viewModel.selectPaperToView(it) },
                                onDeletePaper = { viewModel.deletePaper(it) },
                                onClearAll = { viewModel.clearHistory() }
                            )
                            AppTab.PROFILE -> {
                                val authError by viewModel.authError.collectAsState()
                                val authSuccess by viewModel.authSuccessMessage.collectAsState()
                                ProfileScreen(
                                    userProfile = userProfile,
                                    onSave = { name, email, inst, dept, domain, bio, avatar ->
                                        viewModel.saveProfile(name, email, inst, dept, domain, bio, avatar)
                                    },
                                    onLogout = {
                                        viewModel.logout()
                                    },
                                    onChangePassword = { current, new, confirm ->
                                        viewModel.changePassword(current, new, confirm)
                                    },
                                    authError = authError,
                                    authSuccess = authSuccess,
                                    onClearAuthMessages = {
                                        viewModel.clearAuthMessages()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================
// REGION: DASHBOARD SCREEN
// ============================================

@Composable
fun DashboardScreen(
    papers: List<PaperEntity>,
    conferences: List<ConferenceEntity>,
    userProfile: UserProfileEntity?,
    onNavigateToScan: () -> Unit,
    onNavigateToConferences: () -> Unit,
    onViewPaper: (Int) -> Unit
) {
    val scrollState = rememberScrollState()
    val averagePlagiarism = if (papers.isNotEmpty()) {
        val scannedPapers = papers.filter { it.status == "Scanned" }
        if (scannedPapers.isNotEmpty()) {
            scannedPapers.map { it.plagiarismPercentage }.average().toInt()
        } else 0
    } else 0

    val upcomingConferences = conferences.take(3)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Header with beautiful gradient
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.12f),
                            radius = size.maxDimension * 0.4f,
                            center = Offset(size.width * 0.9f, size.height * 0.1f)
                        )
                    }
                    .padding(24.dp)
            ) {
                Column {
                    Text(
                        text = "Chennai Research Hub",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                    Text(
                        text = "Scholar: ${userProfile?.name ?: "Dr. Srimathi Raman"}",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        ),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        text = "Ensure theoretical integrity & explore premier local publishing assemblies.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onNavigateToScan,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            contentColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("quick_scan_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Analyze New Paper")
                    }
                }
            }
        }

        // Statistical Panel Grid
        Text(
            text = "Academic Metrics",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Total Checked", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = papers.size.toString(),
                        style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Avg Similarity", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "$averagePlagiarism%",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = when {
                                averagePlagiarism > 30 -> Color(0xFFDC2626)
                                averagePlagiarism > 15 -> Color(0xFFD97706)
                                else -> Color(0xFF0D9488)
                            }
                        )
                    )
                }
            }
        }

        // Upcoming conferences over Chennai section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Chennai Conferences",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            TextButton(onClick = onNavigateToConferences) {
                Text("See All (${conferences.size})")
            }
        }

        if (upcomingConferences.isEmpty()) {
            Text("No conferences seeded. Check back soon!", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                upcomingConferences.forEach { conf ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onNavigateToConferences,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    conf.acronym.take(4),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = conf.name,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Venue: ${conf.location}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }
        }

        // Recent scanning history preview
        Text(
            text = "Recent Uploads",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(top = 8.dp)
        )

        if (papers.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "No uploaded papers yet.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Upload and evaluate your manuscript on our AI checker.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                papers.take(3).forEach { paper ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onViewPaper(paper.id) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        ListItem(
                            headlineContent = { Text(paper.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            supportingContent = { Text("Authors: ${paper.authors} • Conf: ${paper.matchedConference}", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            trailingContent = {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            when {
                                                paper.status != "Scanned" -> Color.LightGray.copy(alpha = 0.3f)
                                                paper.plagiarismPercentage > 30 -> Color(0xFFFCA5A5).copy(alpha = 0.4f)
                                                paper.plagiarismPercentage > 15 -> Color(0xFFFDE68A).copy(alpha = 0.4f)
                                                else -> Color(0xFF99F6E4).copy(alpha = 0.4f)
                                            }
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (paper.status == "Scanned") "${paper.plagiarismPercentage}%" else paper.status,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = when {
                                                paper.status != "Scanned" -> Color.DarkGray
                                                paper.plagiarismPercentage > 30 -> Color(0xFF991B1B)
                                                paper.plagiarismPercentage > 15 -> Color(0xFF92400E)
                                                else -> Color(0xFF0F766E)
                                            }
                                        )
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ============================================
// REGION: PLAGIARISM CHECKER
// ============================================

@Composable
fun PlagiarismCheckerScreen(
    viewModel: ResearchViewModel
) {
    val scanState by viewModel.scanState.collectAsState()

    var title by remember { mutableStateOf("") }
    var authors by remember { mutableStateOf("") }
    var abstractContent by remember { mutableStateOf("") }
    var fullContent by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    if (scanState is ScanUiState.Checking) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .drawBehind {
                        drawArc(
                            color = TealPrim.copy(alpha = 0.1f),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(80.dp),
                    strokeWidth = 6.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Comparing with Repositories...",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Generating dynamic report utilizing Gemini AI model.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "AI Plagiarism Scanner",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
            )
            Text(
                text = "Submit your academic draft. Our AI will analyze syntactic compositions, reference overlap, and highlight rephrasing suggestions.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            if (scanState is ScanUiState.Error) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = "Error", tint = MaterialTheme.colorScheme.onErrorContainer)
                        Text(
                            text = (scanState as ScanUiState.Error).errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Input Fields
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Research Paper Title *") },
                placeholder = { Text("e.g., Deep Residual Networks for Automated Tamil Text Summarization") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_title"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = authors,
                onValueChange = { authors = it },
                label = { Text("Authors") },
                placeholder = { Text("e.g., Dr. Srimathi Raman, Kumar S") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_authors"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = abstractContent,
                onValueChange = { abstractContent = it },
                label = { Text("Abstract") },
                placeholder = { Text("Provide brief overview summarizing research scope...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .testTag("input_abstract"),
                maxLines = 4,
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = fullContent,
                onValueChange = { fullContent = it },
                label = { Text("Manuscript / Body Content *") },
                placeholder = { Text("Paste thesis body context or relevant code algorithms to evaluate...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .testTag("input_content"),
                maxLines = 20,
                shape = RoundedCornerShape(12.dp)
            )

            Button(
                onClick = {
                    viewModel.triggerPlagiarismScan(title, authors, abstractContent, fullContent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("submit_scan_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Intelligent Repository Check", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ============================================
// REGION: CHENNAI CONFERENCES SCREEN
// ============================================

@Composable
fun ConferencesScreen(
    viewModel: ResearchViewModel,
    conferences: List<ConferenceEntity>
) {
    val searchQuery by viewModel.conferenceSearchQuery.collectAsState()
    val filterOnlyFavorites by viewModel.conferenceFilterOnlyFavorites.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    val filteredConferences = conferences.filter {
        val matchesSearch = it.name.lowercase().contains(searchQuery.lowercase()) ||
                it.acronym.lowercase().contains(searchQuery.lowercase()) ||
                it.location.lowercase().contains(searchQuery.lowercase()) ||
                it.topics.lowercase().contains(searchQuery.lowercase())

        val matchesFav = if (filterOnlyFavorites) it.isFavorite else true

        matchesSearch && matchesFav
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_conference_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Conference")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Chennai Research Assemblies",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
            )
            Text(
                text = "Explore and track international and national research conventions occurring over Tamil Nadu capital Chennai.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            // Search Bar & Filter Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setConferenceSearchQuery(it) },
                    placeholder = { Text("Search conferences/topics...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("conference_search_bar"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                IconButton(
                    onClick = { viewModel.setConferenceFilterOnlyFavorites(!filterOnlyFavorites) }
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Filter Favorites",
                        tint = if (filterOnlyFavorites) Color.Red else Color.Gray
                    )
                }
            }

            if (filteredConferences.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.LightGray)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No matching assemblies found over Chennai.", color = Color.Gray, textAlign = TextAlign.Center)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredConferences) { conf ->
                        ConferenceItemCard(
                            conf = conf,
                            onToggleFav = { viewModel.toggleConferenceFavorite(conf) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddConferenceDialog(
            onDismiss = { showAddDialog = false },
            onSubmit = { name, acronym, location, dates, topics, desc, web, deadline ->
                viewModel.createConference(name, acronym, location, dates, topics, desc, web, deadline)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun ConferenceItemCard(
    conf: ConferenceEntity,
    onToggleFav: () -> Unit
) {
    val urlHandler = LocalUriHandler.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = conf.acronym,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        if (conf.isUserAdded) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text("Custom", style = MaterialTheme.typography.bodySmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = conf.name,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                IconButton(onClick = onToggleFav) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Favorite",
                        tint = if (conf.isFavorite) Color.Red else Color.Gray
                    )
                }
            }

            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "• Venue: ${conf.location}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )

                Text(
                    text = "• Event Dates: ${conf.dates}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )

                Text(
                    text = "• Paper Deadline: ${conf.submissionDeadline}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(12.dp)
            ) {
                Column {
                    Text("Focus Domains:", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                    Text(conf.topics, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }

            Text(
                conf.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { urlHandler.openUri(conf.websiteUrl) }) {
                    Text("Visit Official Website →")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddConferenceDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var acronym by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var dates by remember { mutableStateOf("") }
    var topics by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var websiteUrl by remember { mutableStateOf("") }
    var submissionDeadline by remember { mutableStateOf("2026-09-30") }

    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Manage Assembly",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Text(
                    "Announce another academic research conference occurring in and around Chennai.",
                    style = MaterialTheme.typography.bodySmall
                )

                OutlinedTextField(
                    value = acronym,
                    onValueChange = { acronym = it },
                    label = { Text("Conference Short Name *") },
                    placeholder = { Text("e.g. ICCS 2026") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Conference Title *") },
                    placeholder = { Text("e.g. International Conference on Computational Science") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Venue Location (Chennai) *") },
                    placeholder = { Text("e.g. IIT Madras Campus, Chennai") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = dates,
                    onValueChange = { dates = it },
                    label = { Text("Event Schedule Dates *") },
                    placeholder = { Text("e.g. November 10-12, 2026") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = submissionDeadline,
                    onValueChange = { submissionDeadline = it },
                    label = { Text("Submission Deadline *") },
                    placeholder = { Text("YYYY-MM-DD") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = topics,
                    onValueChange = { topics = it },
                    label = { Text("Technical Themes (Comma separated) *") },
                    placeholder = { Text("e.g. Big Data, Cloud, Tamil NLP, Internet of Things") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Abstract Synopsis *") },
                    placeholder = { Text("Brief description of target assembly academic scope") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    maxLines = 3
                )

                OutlinedTextField(
                    value = websiteUrl,
                    onValueChange = { websiteUrl = it },
                    label = { Text("Official Conference URL") },
                    placeholder = { Text("e.g. https://domain.edu/iccs2026") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Button(
                    onClick = {
                        if (name.isNotEmpty() && acronym.isNotEmpty() && location.isNotEmpty() && dates.isNotEmpty()) {
                            onSubmit(name, acronym, location, dates, topics, description, websiteUrl, submissionDeadline)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Add Academy Assembly", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ============================================
// REGION: PROFILE SCREEN
// ============================================

@Composable
fun ProfileScreen(
    userProfile: UserProfileEntity?,
    onSave: (String, String, String, String, String, String, String) -> Unit,
    onLogout: () -> Unit,
    onChangePassword: (String, String, String) -> Boolean,
    authError: String?,
    authSuccess: String?,
    onClearAuthMessages: () -> Unit
) {
    var name by remember { mutableStateOf(userProfile?.name ?: "Dr. Srimathi Raman") }
    var email by remember { mutableStateOf(userProfile?.email ?: "srimathi.raman@annauniv.edu") }
    var institution by remember { mutableStateOf(userProfile?.institution ?: "Anna University, Guindy") }
    var department by remember { mutableStateOf(userProfile?.department ?: "Dept of Computer Science & IT") }
    var researchDomain by remember { mutableStateOf(userProfile?.researchDomain ?: "NLP & AI Ethics") }
    var bio by remember { mutableStateOf(userProfile?.bio ?: "") }
    var avatarId by remember { mutableStateOf(userProfile?.avatarId ?: "avatar_1") }

    val scrollState = rememberScrollState()
    var successNotice by remember { mutableStateOf(false) }

    // Synchronize states if entities from db change
    LaunchedEffect(userProfile) {
        userProfile?.let {
            name = it.name
            email = it.email
            institution = it.institution
            department = it.department
            researchDomain = it.researchDomain
            bio = it.bio
            avatarId = it.avatarId
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Scholar Profile",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
        )

        // Custom Avatar Selection
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Large Drawn Avatar Logo
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(
                                listOf(
                                    TealPrim,
                                    IndigoBlue,
                                    AmberAccent,
                                    TealPrim
                                )
                            )
                        )
                        .padding(3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            name.split(" ").filter { it.isNotEmpty() }.take(2).joinToString("") { it.take(1).uppercase() },
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 2.sp
                            )
                        )
                    }
                }

                Text(
                    name.ifEmpty { "Scholarly Researcher" },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Text(
                    institution.ifEmpty { "Affiliation Pending" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val avatars = listOf("avatar_1", "avatar_2", "avatar_3", "avatar_4")
                    avatars.forEach { avId ->
                        val isSelected = avatarId == avId
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    else Color.Transparent
                                )
                                .border(
                                    width = 1.5.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { avatarId = avId }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Profile " + avId.takeLast(1),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        if (successNotice) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFD1FAE5)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Success", tint = Color(0xFF0F766E))
                    Text(
                        "Scholar profile details saved securely.",
                        color = Color(0xFF0F766E),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            LaunchedEffect(Unit) {
                delay(3000)
                successNotice = false
            }
        }

        // Edit Profile Inputs
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Display Name *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Official Contact Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = institution,
            onValueChange = { institution = it },
            label = { Text("Academic Institution *") },
            placeholder = { Text("e.g. Anna University, IIT Madras") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = department,
            onValueChange = { department = it },
            label = { Text("Department") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = researchDomain,
            onValueChange = { researchDomain = it },
            label = { Text("Major Research Field") },
            placeholder = { Text("e.g. Computer Science, Biotechnology, Power Systems") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = bio,
            onValueChange = { bio = it },
            label = { Text("Research Statement / Biography") },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            maxLines = 4
        )

        Button(
            onClick = {
                if (name.isNotEmpty() && institution.isNotEmpty()) {
                    onSave(name, email, institution, department, researchDomain, bio, avatarId)
                    successNotice = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("submit_profile_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Update Scholar Integrity Profile", fontWeight = FontWeight.Bold)
        }

        // Password Change Card
        var currentPassword by remember { mutableStateOf("") }
        var newPassword by remember { mutableStateOf("") }
        var confirmNewPassword by remember { mutableStateOf("") }
        var isCurrentVisible by remember { mutableStateOf(false) }
        var isNewVisible by remember { mutableStateOf(false) }
        var isConfirmNewVisible by remember { mutableStateOf(false) }

        // Clear VM auth messages on launch and on field change
        LaunchedEffect(Unit) {
            onClearAuthMessages()
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Change Password",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Change / Update Password",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                Text(
                    text = "Ensure your academic publisher profile credentials are updated.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                if (authError != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = authError,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                if (authSuccess != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFC0FBEF)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Success",
                                tint = Color(0xFF0F766E),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = authSuccess,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF0F766E)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { 
                        currentPassword = it 
                        onClearAuthMessages()
                    },
                    label = { Text("Current Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    trailingIcon = {
                        Text(
                            text = if (isCurrentVisible) "Hide" else "Show",
                            modifier = Modifier
                                .clickable { isCurrentVisible = !isCurrentVisible }
                                .padding(end = 12.dp),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    },
                    visualTransformation = if (isCurrentVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().testTag("profile_current_password_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { 
                        newPassword = it 
                        onClearAuthMessages()
                    },
                    label = { Text("New Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    trailingIcon = {
                        Text(
                            text = if (isNewVisible) "Hide" else "Show",
                            modifier = Modifier
                                .clickable { isNewVisible = !isNewVisible }
                                .padding(end = 12.dp),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    },
                    visualTransformation = if (isNewVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().testTag("profile_new_password_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = confirmNewPassword,
                    onValueChange = { 
                        confirmNewPassword = it 
                        onClearAuthMessages()
                    },
                    label = { Text("Confirm New Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    trailingIcon = {
                        Text(
                            text = if (isConfirmNewVisible) "Hide" else "Show",
                            modifier = Modifier
                                .clickable { isConfirmNewVisible = !isConfirmNewVisible }
                                .padding(end = 12.dp),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    },
                    visualTransformation = if (isConfirmNewVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().testTag("profile_confirm_password_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Button(
                    onClick = {
                        val ok = onChangePassword(currentPassword, newPassword, confirmNewPassword)
                        if (ok) {
                            currentPassword = ""
                            newPassword = ""
                            confirmNewPassword = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("profile_change_password_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Update Password", fontWeight = FontWeight.Bold)
                }
            }
        }

        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("logout_button"),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.ExitToApp, contentDescription = "Log Out")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Log Out", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

// ============================================
// REGION: HISTORY SCREEN
// ============================================

@Composable
fun HistoryScreen(
    papers: List<PaperEntity>,
    onViewPaper: (Int) -> Unit,
    onDeletePaper: (PaperEntity) -> Unit,
    onClearAll: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Upload History",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                )
                Text(
                    text = "Records of reviewed manuscripts.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            if (papers.isNotEmpty()) {
                IconButton(onClick = onClearAll) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear History",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        if (papers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "History Empty",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "No research papers have been uploaded or checked yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(papers) { paper ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onViewPaper(paper.id) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = formatTimestamp(paper.checkedDate),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            when {
                                                paper.status != "Scanned" -> Color.LightGray.copy(alpha = 0.3f)
                                                paper.plagiarismPercentage > 30 -> Color(0xFFFCA5A5).copy(alpha = 0.4f)
                                                paper.plagiarismPercentage > 15 -> Color(0xFFFDE68A).copy(alpha = 0.4f)
                                                else -> Color(0xFF99F6E4).copy(alpha = 0.4f)
                                            }
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (paper.status == "Scanned") "${paper.plagiarismPercentage}% Similarity" else paper.status,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = when {
                                            paper.status != "Scanned" -> Color.DarkGray
                                            paper.plagiarismPercentage > 30 -> Color(0xFF991B1B)
                                            paper.plagiarismPercentage > 15 -> Color(0xFF92400E)
                                            else -> Color(0xFF0F766E)
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                paper.title,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                "Authors: ${paper.authors}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Target: ${paper.matchedConference}",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                )

                                TextButton(
                                    onClick = { onDeletePaper(paper) },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Remove")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================
// REGION: PAPER REPORTS DETAILS
// ============================================

@Composable
fun PaperDetailScreen(
    paper: PaperEntity,
    onBack: () -> Unit,
    onDelete: () -> Unit
) {
    val scrollState = rememberScrollState()

    // Safely parse JSON sources and suggestions utilizing mock helpers if parsing fails
    val matchedSources: List<PlagiarismSource> = remember(paper.matchSourcesJson) {
        try {
            val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, PlagiarismSource::class.java)
            val adapter = RetrofitClient.moshi.adapter<List<PlagiarismSource>>(type)
            adapter.fromJson(paper.matchSourcesJson) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    val suggestions: List<RephrasingSuggestion> = remember(paper.rephrasingSuggestionsJson) {
        try {
            val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, RephrasingSuggestion::class.java)
            val adapter = RetrofitClient.moshi.adapter<List<RephrasingSuggestion>>(type)
            adapter.fromJson(paper.rephrasingSuggestionsJson) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Toolbar Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("← Back to List", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }

            Text(
                "Diagnostic Report",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
            )

            IconButton(onClick = {
                onDelete()
                onBack()
            }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
            }
        }

        // Header Title Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Checked: ${formatTimestamp(paper.checkedDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = paper.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Authors: ${paper.authors}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Diagnostic Plagiarism Circular Gauge
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Intellectual Similarity Rating",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier.size(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val severityColor = when {
                        paper.status != "Scanned" -> Color.LightGray
                        paper.plagiarismPercentage > 30 -> Color(0xFFDC2626) // Strong Red
                        paper.plagiarismPercentage > 15 -> Color(0xFFD97706) // Intense Amber
                        else -> Color(0xFF0D9488) // Safe Teal
                    }

                    // Native Draw Gauge
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidthPx = 14.dp.toPx()
                        // Draw Background Arc
                        drawArc(
                            color = Color.LightGray.copy(alpha = 0.3f),
                            startAngle = -220f,
                            sweepAngle = 260f,
                            useCenter = false,
                            style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                        )

                        // Draw Filled Value Arc
                        val filledSweepAngle = (paper.plagiarismPercentage / 100f) * 260f
                        drawArc(
                            color = severityColor,
                            startAngle = -220f,
                            sweepAngle = filledSweepAngle,
                            useCenter = false,
                            style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${paper.plagiarismPercentage}%",
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = severityColor
                            )
                        )
                        Text(
                            text = when {
                                paper.status != "Scanned" -> "Error"
                                paper.plagiarismPercentage > 30 -> "HIGH RISK"
                                paper.plagiarismPercentage > 15 -> "MEDIUM RISK"
                                else -> "SAFE / ORIGINAL"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = paper.aiExplanation,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }

        // Matched Sources Segment
        Text(
            text = "Semantic Match Repositories",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )

        if (matchedSources.isEmpty()) {
            Text(
                "Intellectual review indicates complete uniqueness. No duplicate sources discovered in major indexes.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                matchedSources.forEach { source ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(source.name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(source.description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "${source.percentage}% match",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.secondary)
                            )
                        }
                    }
                }
            }
        }

        // Paraphrase segment
        Text(
            text = "AI Paraphrasing & Phrasing Corrections",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(top = 12.dp)
        )

        if (suggestions.isEmpty()) {
            Text(
                "No custom suggestions requested. Standard syntaxes look optimal.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                suggestions.forEach { suggestion ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Original Phrasing:",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                suggestion.originalText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                "AI Scholar Reword:",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF0F766E))
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                suggestion.rewrittenText,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                "Scholarly Benefit: ${suggestion.benefit}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

// ============================================
// REGION: CONVENIENCE FUNCTIONS
// ============================================

fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp)
    val formatter = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
    return formatter.format(date)
}
