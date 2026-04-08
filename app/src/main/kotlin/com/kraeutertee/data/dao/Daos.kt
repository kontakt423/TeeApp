package com.kraeutertee.data.dao

import androidx.room.*
import com.kraeutertee.data.entities.*
import kotlinx.coroutines.flow.Flow

// ── HerbNote DAO ─────────────────────────────────────────────────────────────
@Dao
interface HerbNoteDao {
    @Query("SELECT * FROM herb_notes") fun getAll(): Flow<List<HerbNote>>
    @Query("SELECT * FROM herb_notes WHERE herbId = :id") suspend fun getById(id: String): HerbNote?
    @Query("SELECT * FROM herb_notes WHERE isFavorite = 1") fun getFavorites(): Flow<List<HerbNote>>
    @Query("SELECT * FROM herb_notes WHERE isInGarden = 1") fun getGardenHerbs(): Flow<List<HerbNote>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(note: HerbNote)
    @Delete suspend fun delete(note: HerbNote)
}

// ── HerbLocation DAO ──────────────────────────────────────────────────────────
@Dao
interface HerbLocationDao {
    @Query("SELECT * FROM herb_locations ORDER BY dateFound DESC")
    fun getAll(): Flow<List<HerbLocation>>

    @Query("SELECT * FROM herb_locations WHERE herbId = :herbId")
    fun getByHerb(herbId: String): Flow<List<HerbLocation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(loc: HerbLocation): Long
    @Update suspend fun update(loc: HerbLocation)
    @Delete suspend fun delete(loc: HerbLocation)
    @Query("DELETE FROM herb_locations WHERE id = :id") suspend fun deleteById(id: Int)
}

// ── HarvestReminder DAO ────────────────────────────────────────────────────────
@Dao
interface HarvestReminderDao {
    @Query("SELECT * FROM harvest_reminders ORDER BY reminderDate ASC")
    fun getAll(): Flow<List<HarvestReminder>>

    @Query("SELECT * FROM harvest_reminders WHERE isActive = 1 ORDER BY reminderDate ASC")
    fun getActive(): Flow<List<HarvestReminder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(r: HarvestReminder): Long
    @Update suspend fun update(r: HarvestReminder)
    @Delete suspend fun delete(r: HarvestReminder)
    @Query("DELETE FROM harvest_reminders WHERE id = :id") suspend fun deleteById(id: Int)
}

// ── DryingEntry DAO ────────────────────────────────────────────────────────────
@Dao
interface DryingEntryDao {
    @Query("SELECT * FROM drying_entries ORDER BY startDate DESC")
    fun getAll(): Flow<List<DryingEntry>>

    @Query("SELECT * FROM drying_entries WHERE isCompleted = 0 ORDER BY startDate DESC")
    fun getActive(): Flow<List<DryingEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(e: DryingEntry): Long
    @Update suspend fun update(e: DryingEntry)
    @Delete suspend fun delete(e: DryingEntry)
    @Query("DELETE FROM drying_entries WHERE id = :id") suspend fun deleteById(id: Int)
}

// ── CustomHerb DAO ────────────────────────────────────────────────────────────
@Dao
interface CustomHerbDao {
    @Query("SELECT * FROM custom_herbs ORDER BY createdAt DESC")
    fun getAll(): Flow<List<CustomHerbEntity>>

    @Query("SELECT * FROM custom_herbs WHERE id = :id")
    suspend fun getById(id: String): CustomHerbEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(h: CustomHerbEntity)
    @Update suspend fun update(h: CustomHerbEntity)
    @Delete suspend fun delete(h: CustomHerbEntity)
    @Query("DELETE FROM custom_herbs WHERE id = :id") suspend fun deleteById(id: String)
}

// ── Recipe DAO ────────────────────────────────────────────────────────────────
@Dao
interface RecipeDao {
    @Query("SELECT * FROM recipes ORDER BY updatedAt DESC") fun getAll(): Flow<List<Recipe>>
    @Query("SELECT * FROM recipes WHERE id = :id") suspend fun getById(id: Int): Recipe?
    @Query("SELECT * FROM recipes WHERE name LIKE '%' || :q || '%' OR description LIKE '%' || :q || '%'")
    fun search(q: String): Flow<List<Recipe>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(r: Recipe): Long
    @Update suspend fun update(r: Recipe)
    @Delete suspend fun delete(r: Recipe)
    @Query("DELETE FROM recipes WHERE id = :id") suspend fun deleteById(id: Int)
}
