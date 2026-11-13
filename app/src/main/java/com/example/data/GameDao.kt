package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM campaign_profile WHERE id = 1")
    fun observeProfile(): Flow<CampaignProfileEntity?>

    @Query("SELECT * FROM campaign_profile WHERE id = 1")
    suspend fun getProfileOnce(): CampaignProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: CampaignProfileEntity)

    @Query("SELECT * FROM village_buildings ORDER BY id ASC")
    fun observeBuildings(): Flow<List<VillageBuildingEntity>>

    @Query("SELECT * FROM village_buildings")
    suspend fun getBuildingsOnce(): List<VillageBuildingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBuildings(buildings: List<VillageBuildingEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBuilding(building: VillageBuildingEntity)

    @Query("SELECT * FROM outposts ORDER BY id ASC")
    fun observeOutposts(): Flow<List<OutpostEntity>>

    @Query("SELECT * FROM outposts")
    suspend fun getOutpostsOnce(): List<OutpostEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOutposts(outposts: List<OutpostEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOutpost(outpost: OutpostEntity)

    @Query("SELECT * FROM warriors ORDER BY id ASC")
    fun observeWarriors(): Flow<List<WarriorEntity>>

    @Query("SELECT * FROM warriors")
    suspend fun getWarriorsOnce(): List<WarriorEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWarriors(warriors: List<WarriorEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWarrior(warrior: WarriorEntity)

    @Query("SELECT * FROM history_chronicle ORDER BY sortOrder ASC")
    fun observeChronicle(): Flow<List<HistoryEntryEntity>>

    @Query("SELECT * FROM history_chronicle ORDER BY sortOrder ASC")
    suspend fun getChronicleOnce(): List<HistoryEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChronicle(entries: List<HistoryEntryEntity>)

    @Query("UPDATE history_chronicle SET unlocked = 1 WHERE id = :entryId")
    suspend fun unlockHistoryEntry(entryId: String)
}
