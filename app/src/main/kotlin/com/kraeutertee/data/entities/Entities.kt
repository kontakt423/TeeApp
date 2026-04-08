package com.kraeutertee.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

// ──────────────────────────────────────────────────────────────────────────────
// Saved herb notes (user's personal annotations on a herb)
// ──────────────────────────────────────────────────────────────────────────────
@Entity(tableName = "herb_notes")
data class HerbNote(
    @PrimaryKey val herbId: String,
    val personalNotes: String = "",
    val isInGarden: Boolean = false,
    val isFavorite: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)

// ──────────────────────────────────────────────────────────────────────────────
// A pinned location on the map where a herb was found
// ──────────────────────────────────────────────────────────────────────────────
@Entity(tableName = "herb_locations")
data class HerbLocation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val herbId: String,
    val herbName: String,
    val latitude: Double,
    val longitude: Double,
    val notes: String = "",
    val dateFound: Long = System.currentTimeMillis(),
    val quantity: String = "",            // e.g. "viel", "wenig", "mittel"
    val photoUri: String = ""
)

// ──────────────────────────────────────────────────────────────────────────────
// Harvest reminder
// ──────────────────────────────────────────────────────────────────────────────
@Entity(tableName = "harvest_reminders")
data class HarvestReminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val herbId: String,
    val herbName: String,
    val reminderDate: Long,               // epoch millis
    val isActive: Boolean = true,
    val notes: String = "",
    val workerId: String = ""             // WorkManager ID for cancellation
)

// ──────────────────────────────────────────────────────────────────────────────
// Active drying entry
// ──────────────────────────────────────────────────────────────────────────────
@Entity(tableName = "drying_entries")
data class DryingEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val herbId: String,
    val herbName: String,
    val startDate: Long = System.currentTimeMillis(),
    val expectedDays: Int,
    val dryingMethod: String = "Lufttrocknung",  // e.g. "Lufttrocknung", "Dörrgerät", "Ofen"
    val quantity: String = "",
    val notes: String = "",
    val isCompleted: Boolean = false,
    val completedDate: Long? = null
)

// ──────────────────────────────────────────────────────────────────────────────
// User-created custom herb (AI-generated or manually entered)
// ──────────────────────────────────────────────────────────────────────────────
@Entity(tableName = "custom_herbs")
data class CustomHerbEntity(
    @PrimaryKey val id: String,           // "custom_<timestamp>"
    val name: String,
    val latinName: String = "",
    val emoji: String = "🌿",
    val shortDescription: String = "",
    val harvestPart: String = "Blätter",
    val harvestMonthsJson: String = "[6,7,8]",   // JSON int array
    val harvestTips: String = "",
    val dryingDays: Int = 7,
    val dryingTempMax: Int = 40,
    val dryingMethod: String = "Lufttrocknung",
    val storageMonths: Int = 12,
    val teaInfo: String = "",
    val brewingTempC: Int = 90,
    val brewingMinutes: Int = 7,
    val effectsJson: String = "[]",          // JSON string array
    val compatibleWithJson: String = "[]",   // JSON string array
    val gardenTips: String = "",
    val warningsDE: String = "",
    val category: String = "Sonstige",
    val createdAt: Long = System.currentTimeMillis()
)
@Entity(tableName = "recipes")
data class Recipe(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String = "",
    val ingredientsJson: String = "[]",  // JSON array of RecipeIngredient
    val instructions: String = "",
    val tagsJson: String = "[]",         // JSON array of String tags
    val brewingTempC: Int = 95,
    val brewingMinutes: Int = 5,
    val servings: Int = 2,
    val rating: Float = 0f,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val imageUri: String = ""
)
