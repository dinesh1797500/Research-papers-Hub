package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.ConferenceEntity
import com.example.data.model.PaperEntity
import com.example.data.model.UserProfileEntity
import com.example.data.repository.ResearchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

sealed interface ScanUiState {
    object Idle : ScanUiState
    object Checking : ScanUiState
    data class Completed(val paper: PaperEntity) : ScanUiState
    data class Error(val errorMessage: String) : ScanUiState
}

enum class AppTab {
    DASHBOARD,
    CHECKER,
    CONFERENCES,
    HISTORY,
    PROFILE
}

class ResearchViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = ResearchRepository(
        db.paperDao(),
        db.conferenceDao(),
        db.userProfileDao()
    )

    // Observables from Database
    val papers: StateFlow<List<PaperEntity>> = repository.allPapers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val conferences: StateFlow<List<ConferenceEntity>> = repository.allConferences
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val userProfile: StateFlow<UserProfileEntity?> = repository.userProfile
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Navigation and UI state
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _loggedInUser = MutableStateFlow<String?>(null)
    val loggedInUser: StateFlow<String?> = _loggedInUser.asStateFlow()

    private val _showSignUpScreen = MutableStateFlow(false)
    val showSignUpScreen: StateFlow<Boolean> = _showSignUpScreen.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _authSuccessMessage = MutableStateFlow<String?>(null)
    val authSuccessMessage: StateFlow<String?> = _authSuccessMessage.asStateFlow()

    private val credentialsStore = mutableMapOf<String, String>(
        "admin" to "admin123",
        "scholar" to "password"
    )

    fun login(username: String, pass: String): Boolean {
        _authError.value = null
        if (username.isBlank() || pass.isBlank()) {
            _authError.value = "Username and password cannot be empty."
            return false
        }
        val userKey = username.trim().lowercase()
        val storedPass = credentialsStore[userKey]
        if (storedPass == pass) {
            _isLoggedIn.value = true
            _loggedInUser.value = userKey
            _authError.value = null
            _authSuccessMessage.value = null
            return true
        } else {
            _authError.value = "Invalid username or password."
            return false
        }
    }

    fun signup(username: String, pass: String, passConfirm: String): Boolean {
        _authError.value = null
        _authSuccessMessage.value = null
        if (username.isBlank() || pass.isBlank() || passConfirm.isBlank()) {
            _authError.value = "All fields are required."
            return false
        }
        if (pass != passConfirm) {
            _authError.value = "Passwords do not match."
            return false
        }
        if (pass.length < 4) {
            _authError.value = "Password must be at least 4 characters."
            return false
        }
        val userKey = username.trim().lowercase()
        if (credentialsStore.containsKey(userKey)) {
            _authError.value = "Username already exists."
            return false
        }
        credentialsStore[userKey] = pass
        _authSuccessMessage.value = "Account created successfully! Please log in."
        _showSignUpScreen.value = false
        return true
    }

    fun changePassword(oldPass: String, newPass: String, confirmNewPass: String): Boolean {
        _authError.value = null
        _authSuccessMessage.value = null
        val user = _loggedInUser.value
        if (user == null) {
            _authError.value = "No user logged in."
            return false
        }
        val storedPass = credentialsStore[user]
        if (storedPass != oldPass) {
            _authError.value = "Incorrect current password."
            return false
        }
        if (newPass.isBlank() || confirmNewPass.isBlank()) {
            _authError.value = "New passwords cannot be empty."
            return false
        }
        if (newPass != confirmNewPass) {
            _authError.value = "New passwords do not match."
            return false
        }
        if (newPass.length < 4) {
            _authError.value = "New password must be at least 4 characters."
            return false
        }
        credentialsStore[user] = newPass
        _authSuccessMessage.value = "Password updated successfully!"
        return true
    }

    fun clearAuthMessages() {
        _authError.value = null
        _authSuccessMessage.value = null
    }

    fun setNavigateToSignUp(signUp: Boolean) {
        _showSignUpScreen.value = signUp
        _authError.value = null
        _authSuccessMessage.value = null
    }

    fun logout() {
        _isLoggedIn.value = false
        _loggedInUser.value = null
        _currentTab.value = AppTab.DASHBOARD
        _selectedPaperId.value = null
    }

    private val _currentTab = MutableStateFlow(AppTab.DASHBOARD)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    private val _scanState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val scanState: StateFlow<ScanUiState> = _scanState.asStateFlow()

    private val _selectedPaperId = MutableStateFlow<Int?>(null)
    val selectedPaperId: StateFlow<Int?> = _selectedPaperId.asStateFlow()

    // Filtering/Search for Conferences
    private val _conferenceSearchQuery = MutableStateFlow("")
    val conferenceSearchQuery: StateFlow<String> = _conferenceSearchQuery.asStateFlow()

    private val _conferenceFilterOnlyFavorites = MutableStateFlow(false)
    val conferenceFilterOnlyFavorites: StateFlow<Boolean> = _conferenceFilterOnlyFavorites.asStateFlow()

    init {
        viewModelScope.launch {
            // Seed conferences if database of conferences over Chennai is empty
            repository.checkAndPrepopulateConferences()
            // Seed a default profile if it is empty
            checkAndSeedDefaultProfile()
        }
    }

    fun selectTab(tab: AppTab) {
        _currentTab.value = tab
        // Clear scan state when switching tab, unless scanning
        if (_scanState.value !is ScanUiState.Checking) {
            _scanState.value = ScanUiState.Idle
        }
    }

    fun selectPaperToView(paperId: Int?) {
        _selectedPaperId.value = paperId
        if (paperId != null) {
            _currentTab.value = AppTab.HISTORY
        }
    }

    fun deletePaper(paper: PaperEntity) {
        viewModelScope.launch {
            repository.deletePaper(paper)
            if (_selectedPaperId.value == paper.id) {
                _selectedPaperId.value = null
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearPaperHistory()
            _selectedPaperId.value = null
        }
    }

    fun toggleConferenceFavorite(conference: ConferenceEntity) {
        viewModelScope.launch {
            repository.toggleConferenceFavorite(conference)
        }
    }

    fun createConference(
        name: String,
        acronym: String,
        location: String,
        dates: String,
        topics: String,
        description: String,
        websiteUrl: String,
        submissionDeadline: String
    ) {
        viewModelScope.launch {
            val newConf = ConferenceEntity(
                name = name,
                acronym = acronym.uppercase(),
                location = location,
                dates = dates,
                topics = topics,
                description = description,
                websiteUrl = websiteUrl.ifEmpty { "https://chennai-conferences.org/${acronym.lowercase()}" },
                submissionDeadline = submissionDeadline,
                isUserAdded = true
            )
            repository.insertConference(newConf)
        }
    }

    fun saveProfile(
        name: String,
        email: String,
        institution: String,
        department: String,
        researchDomain: String,
        bio: String,
        avatarId: String
    ) {
        viewModelScope.launch {
            val profile = UserProfileEntity(
                id = 1,
                name = name,
                email = email,
                institution = institution,
                department = department,
                researchDomain = researchDomain,
                bio = bio,
                avatarId = avatarId
            )
            repository.saveUserProfile(profile)
        }
    }

    fun setConferenceSearchQuery(query: String) {
        _conferenceSearchQuery.value = query
    }

    fun setConferenceFilterOnlyFavorites(filter: Boolean) {
        _conferenceFilterOnlyFavorites.value = filter
    }

    fun triggerPlagiarismScan(title: String, authors: String, abstractContent: String, fullContent: String) {
        if (title.isBlank() || fullContent.isBlank()) {
            _scanState.value = ScanUiState.Error("Title and full content can't be empty.")
            return
        }

        viewModelScope.launch {
            _scanState.value = ScanUiState.Checking
            // Save & Check with repository
            val result = repository.analyzePlagiarismAndSave(
                title = title,
                authors = authors.ifBlank { "Unspecified Author" },
                abstractContent = abstractContent.ifBlank { "No abstract provided." },
                content = fullContent
            )

            if (result.status == "Scanned") {
                _scanState.value = ScanUiState.Completed(result)
                _selectedPaperId.value = result.id
                // Optionally jump to history directly to see the report
                _currentTab.value = AppTab.HISTORY
            } else {
                _scanState.value = ScanUiState.Error(
                    result.aiExplanation.replace("Scan failed: ", "")
                )
            }
        }
    }

    private suspend fun checkAndSeedDefaultProfile() {
        // We evaluate profile and seed if null
        // Utilizing direct DB check
        val currentProfile = repository.userProfile.firstOrNull()
        if (currentProfile == null) {
            val defaultProfile = UserProfileEntity(
                id = 1,
                name = "Dr. Srimathi Raman",
                email = "srimathi.raman@annauniv.edu",
                institution = "Anna University, Guindy",
                department = "Department of Computer Science & Information Technology",
                researchDomain = "Natural Language Processing (Tamil NLP) & AI Ethics",
                bio = "Associate Professor teaching AI principles and ethics. Lead investigator of Chennai Academic Dravidian NLP Alliance (CADNA).",
                avatarId = "avatar_1"
            )
            repository.saveUserProfile(defaultProfile)
        }
    }
}
