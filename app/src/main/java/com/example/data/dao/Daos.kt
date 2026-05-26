package com.example.data.dao

import androidx.room.*
import com.example.data.model.ConferenceEntity
import com.example.data.model.PaperEntity
import com.example.data.model.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaperDao {
    @Query("SELECT * FROM papers ORDER BY checkedDate DESC")
    fun getAllPapers(): Flow<List<PaperEntity>>

    @Query("SELECT * FROM papers WHERE id = :id")
    suspend fun getPaperById(id: Int): PaperEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaper(paper: PaperEntity): Long

    @Delete
    suspend fun deletePaper(paper: PaperEntity)

    @Query("DELETE FROM papers")
    suspend fun clearHistory()
}

@Dao
interface ConferenceDao {
    @Query("SELECT * FROM conferences ORDER BY acronym ASC")
    fun getAllConferences(): Flow<List<ConferenceEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertConferences(conferences: List<ConferenceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConference(conference: ConferenceEntity)

    @Update
    suspend fun updateConference(conference: ConferenceEntity)

    @Delete
    suspend fun deleteConference(conference: ConferenceEntity)
}

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getUserProfile(): Flow<UserProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfileEntity)
}
