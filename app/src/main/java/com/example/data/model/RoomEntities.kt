package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "papers")
data class PaperEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val authors: String,
    val abstractContent: String,
    val fullContent: String,
    val plagiarismPercentage: Int,
    val matchedConference: String,
    val matchSourcesJson: String, // JSON array of matched sources
    val checkedDate: Long, // timestamp
    val status: String, // "Scanned", "Failed", "Analyzing"
    val aiExplanation: String,
    val rephrasingSuggestionsJson: String // JSON array of rephrasing suggestions
)

@Entity(tableName = "conferences")
data class ConferenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val acronym: String,
    val location: String, // Venue in Chennai
    val dates: String,
    val topics: String,
    val description: String,
    val websiteUrl: String,
    val submissionDeadline: String,
    val isFavorite: Boolean = false,
    val isUserAdded: Boolean = false
)

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1, // Only 1 profile active
    val name: String,
    val email: String,
    val institution: String,
    val department: String,
    val researchDomain: String,
    val bio: String,
    val avatarId: String // ID for profile avatar
)
