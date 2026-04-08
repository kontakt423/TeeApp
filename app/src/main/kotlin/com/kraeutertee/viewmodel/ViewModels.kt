package com.kraeutertee.viewmodel

import android.content.Context
import androidx.lifecycle.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kraeutertee.api.ApiResult
import com.kraeutertee.api.GeminiService
import com.kraeutertee.data.AppDatabase
import com.kraeutertee.data.entities.*
import com.kraeutertee.ui.screens.ChatMessage
import com.kraeutertee.util.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private val gson = Gson()

/** Converts a CustomHerbEntity to the shared HerbInfo model used by all screens. */
fun CustomHerbEntity.toHerbInfo(): HerbInfo {
    fun parseStrList(json: String) = try {
        gson.fromJson<List<String>>(json, object : TypeToken<List<String>>() {}.type)
    } catch (_: Exception) { emptyList() }

    fun parseIntList(json: String) = try {
        gson.fromJson<List<Int>>(json, object : TypeToken<List<Int>>() {}.type)
    } catch (_: Exception) { emptyList() }

    return HerbInfo(
        id = id,
        name = name,
        latinName = latinName,
        emoji = emoji,
        shortDescription = shortDescription,
        harvestPart = harvestPart,
        harvestMonths = parseIntList(harvestMonthsJson),
        harvestTips = harvestTips,
        dryingDays = dryingDays,
        dryingTempMax = dryingTempMax,
        dryingMethod = dryingMethod,
        storageMonths = storageMonths,
        teaInfo = teaInfo,
        brewingTempC = brewingTempC,
        brewingMinutes = brewingMinutes,
        effects = parseStrList(effectsJson),
        compatibleWith = parseStrList(compatibleWithJson),
        gardenTips = gardenTips,
        warningsDE = warningsDE,
        category = category
    )
}

/** Top-level alias so HerbDetailScreen can call toHerbInfo(entity) */
fun toHerbInfo(entity: CustomHerbEntity): HerbInfo = entity.toHerbInfo()

// ══════════════════════════════════════════════════════════════════════════════
// HerbsViewModel – now merges built-in + custom herbs
// ══════════════════════════════════════════════════════════════════════════════
class HerbsViewModel(app: android.app.Application) : AndroidViewModel(app) {

    private val db    = AppDatabase.getInstance(app)
    private val prefs = PreferencesManager(app)

    val searchQuery      = MutableStateFlow("")
    val selectedCategory = MutableStateFlow<String?>(null)
    val gardenFilter     = MutableStateFlow(false)
    val aiExplanation    = MutableStateFlow("")
    val isLoading        = MutableStateFlow(false)

    // All herbs = built-in + custom (as HerbInfo)
    private val allHerbs: Flow<List<HerbInfo>> = db.customHerbDao().getAll()
        .map { customList ->
            HerbDatabase.all + customList.map { it.toHerbInfo() }
        }

    val filteredHerbs: StateFlow<List<HerbInfo>> = combine(
        searchQuery, selectedCategory, gardenFilter,
        db.herbNoteDao().getAll(), allHerbs
    ) { q, cat, gardenOnly, notes, herbs ->
        val gardenIds = notes.filter { it.isInGarden }.map { it.herbId }.toSet()
        herbs.filter { herb ->
            (q.isBlank()
                    || herb.name.contains(q, ignoreCase = true)
                    || herb.latinName.contains(q, ignoreCase = true)
                    || herb.effects.any { it.contains(q, ignoreCase = true) })
                    && (cat == null || herb.category == cat)
                    && (!gardenOnly || herb.id in gardenIds)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HerbDatabase.all)

    /** All categories from both built-in and custom herbs, sorted. */
    val allCategories: StateFlow<List<String>> = db.customHerbDao().getAll()
        .map { customList ->
            val customCategories = customList.map { it.category }
            (HerbDatabase.categories + customCategories).distinct().sorted()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HerbDatabase.categories)

    fun setSearch(q: String)        { searchQuery.value = q }
    fun setCategory(cat: String?)   { selectedCategory.value = cat }
    fun setGardenFilter(v: Boolean) { gardenFilter.value = v }

    fun getNoteFor(herbId: String): Flow<HerbNote?> =
        db.herbNoteDao().getAll().map { list -> list.find { it.herbId == herbId } }

    fun toggleFavorite(herbId: String, current: HerbNote?) = viewModelScope.launch {
        val note = current ?: HerbNote(herbId = herbId)
        db.herbNoteDao().upsert(note.copy(isFavorite = !note.isFavorite))
    }

    fun toggleGarden(herbId: String, current: HerbNote?) = viewModelScope.launch {
        val note = current ?: HerbNote(herbId = herbId)
        db.herbNoteDao().upsert(note.copy(isInGarden = !note.isInGarden))
    }

    fun saveNotes(herbId: String, notes: String, current: HerbNote?) = viewModelScope.launch {
        val note = current ?: HerbNote(herbId = herbId)
        db.herbNoteDao().upsert(note.copy(personalNotes = notes, lastUpdated = System.currentTimeMillis()))
    }

    fun isCustomHerb(herbId: String) = herbId.startsWith("custom_")

    fun deleteCustomHerb(herbId: String) = viewModelScope.launch {
        db.customHerbDao().deleteById(herbId)
    }

    fun loadAiExplanation(herb: HerbInfo) = viewModelScope.launch {
        isLoading.value = true
        aiExplanation.value = ""
        val key    = prefs.apiKey.first()
        val result = GeminiService(key).explainHerb(herb.name, herb.latinName)
        aiExplanation.value = when (result) {
            is ApiResult.Success -> result.data
            is ApiResult.Error   -> "Fehler: ${result.message}"
        }
        isLoading.value = false
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// CustomHerbsViewModel – handles adding/editing custom herbs with Gemini AI
// ══════════════════════════════════════════════════════════════════════════════
class CustomHerbsViewModel(app: android.app.Application) : AndroidViewModel(app) {

    private val db    = AppDatabase.getInstance(app)
    private val prefs = PreferencesManager(app)

    val isGenerating = MutableStateFlow(false)
    val generationError = MutableStateFlow("")

    /**
     * Ask Gemini to generate herb info, parse the JSON, and save to the DB.
     * Returns the new herb's ID on success, null on failure.
     */
    fun generateAndSave(
        herbName: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) = viewModelScope.launch {
        isGenerating.value = true
        generationError.value = ""

        val key = prefs.apiKey.first()
        val result = GeminiService(key).generateHerbInfo(herbName)

        when (result) {
            is ApiResult.Success -> {
                val entity = parseGeminiHerbJson(herbName, result.data)
                if (entity != null) {
                    db.customHerbDao().insert(entity)
                    onSuccess(entity.id)
                } else {
                    val msg = "Konnte die KI-Antwort nicht verarbeiten. Bitte erneut versuchen."
                    generationError.value = msg
                    onError(msg)
                }
            }
            is ApiResult.Error -> {
                generationError.value = result.message
                onError(result.message)
            }
        }
        isGenerating.value = false
    }

    /** Save a manually filled-in custom herb directly. */
    fun saveCustomHerb(entity: CustomHerbEntity) = viewModelScope.launch {
        db.customHerbDao().insert(entity)
    }

    fun updateCustomHerb(entity: CustomHerbEntity) = viewModelScope.launch {
        db.customHerbDao().update(entity)
    }

    fun deleteCustomHerb(id: String) = viewModelScope.launch {
        db.customHerbDao().deleteById(id)
    }

    suspend fun getCustomHerbById(id: String): CustomHerbEntity? =
        db.customHerbDao().getById(id)

    private fun parseGeminiHerbJson(name: String, raw: String): CustomHerbEntity? {
        return try {
            // Strip markdown code fences if Gemini adds them
            val clean = raw.trim()
                .removePrefix("```json").removePrefix("```")
                .removeSuffix("```").trim()

            val map: Map<String, Any> = gson.fromJson(
                clean, object : TypeToken<Map<String, Any>>() {}.type
            )

            fun str(key: String, default: String = "") =
                (map[key] as? String)?.trim() ?: default

            fun int(key: String, default: Int) =
                (map[key] as? Double)?.toInt() ?: (map[key] as? Int) ?: default

            fun strList(key: String): String {
                val raw2 = map[key]
                val list = when (raw2) {
                    is List<*> -> raw2.filterIsInstance<String>()
                    else       -> emptyList()
                }
                return gson.toJson(list)
            }

            fun intList(key: String): String {
                val raw2 = map[key]
                val list = when (raw2) {
                    is List<*> -> raw2.filterIsInstance<Double>().map { it.toInt() }
                    else       -> listOf(6, 7, 8)
                }
                return gson.toJson(list)
            }

            val id = "custom_${System.currentTimeMillis()}"
            CustomHerbEntity(
                id               = id,
                name             = name,
                latinName        = str("latinName"),
                emoji            = str("emoji", "🌿"),
                shortDescription = str("shortDescription"),
                harvestPart      = str("harvestPart", "Blätter"),
                harvestMonthsJson = intList("harvestMonths"),
                harvestTips      = str("harvestTips"),
                dryingDays       = int("dryingDays", 7),
                dryingTempMax    = int("dryingTempMax", 40),
                dryingMethod     = str("dryingMethod", "Lufttrocknung"),
                storageMonths    = int("storageMonths", 12),
                teaInfo          = str("teaInfo"),
                brewingTempC     = int("brewingTempC", 90),
                brewingMinutes   = int("brewingMinutes", 7),
                effectsJson      = strList("effects"),
                compatibleWithJson = strList("compatibleWith"),
                gardenTips       = str("gardenTips"),
                warningsDE       = str("warningsDE"),
                category         = str("category", "Sonstige")
            )
        } catch (_: Exception) {
            null
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// MapViewModel
// ══════════════════════════════════════════════════════════════════════════════
class MapViewModel(app: android.app.Application) : AndroidViewModel(app) {

    private val db    = AppDatabase.getInstance(app)
    private val prefs = PreferencesManager(app)

    val allLocations = db.herbLocationDao().getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val latitude         = MutableStateFlow(48.1374)
    val longitude        = MutableStateFlow(11.5755)
    val selectedLocation = MutableStateFlow<HerbLocation?>(null)

    init {
        viewModelScope.launch {
            prefs.lastLocation.collect { (lat, lng) ->
                latitude.value  = lat
                longitude.value = lng
            }
        }
    }

    fun refreshLocation(context: Context) = viewModelScope.launch {
        LocationHelper.getLastLocation(context)?.let { loc ->
            latitude.value  = loc.latitude
            longitude.value = loc.longitude
            prefs.saveLocation(loc.latitude, loc.longitude)
        }
    }

    fun addLocation(loc: HerbLocation) = viewModelScope.launch {
        db.herbLocationDao().insert(loc)
    }

    fun deleteLocation(loc: HerbLocation) = viewModelScope.launch {
        db.herbLocationDao().delete(loc)
        if (selectedLocation.value?.id == loc.id) selectedLocation.value = null
    }

    fun selectLocation(loc: HerbLocation) { selectedLocation.value = loc }
}

// ══════════════════════════════════════════════════════════════════════════════
// CalendarViewModel
// ══════════════════════════════════════════════════════════════════════════════
class CalendarViewModel(app: android.app.Application) : AndroidViewModel(app) {

    private val db    = AppDatabase.getInstance(app)
    private val prefs = PreferencesManager(app)

    val activeReminders = db.harvestReminderDao().getActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeDrying = db.dryingEntryDao().getActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allDrying = db.dryingEntryDao().getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val latitude = MutableStateFlow(48.1374)

    init {
        viewModelScope.launch {
            prefs.lastLocation.collect { (lat, _) -> latitude.value = lat }
        }
    }

    fun refreshLocation(context: Context) = viewModelScope.launch {
        LocationHelper.getLastLocation(context)?.let { loc ->
            latitude.value = loc.latitude
            prefs.saveLocation(loc.latitude, loc.longitude)
        }
    }

    fun climateZoneName(lat: Double)   = LocationHelper.climateZoneName(lat)
    fun offsetDescription(lat: Double) = LocationHelper.harvestOffsetDescription(lat)

    fun addReminder(reminder: HarvestReminder) = viewModelScope.launch {
        db.harvestReminderDao().insert(reminder)
    }

    fun deleteReminder(reminder: HarvestReminder) = viewModelScope.launch {
        db.harvestReminderDao().delete(reminder)
    }

    fun addDrying(entry: DryingEntry) = viewModelScope.launch {
        db.dryingEntryDao().insert(entry)
    }

    fun completeDrying(entry: DryingEntry) = viewModelScope.launch {
        db.dryingEntryDao().update(
            entry.copy(isCompleted = true, completedDate = System.currentTimeMillis())
        )
    }

    fun deleteDrying(entry: DryingEntry) = viewModelScope.launch {
        db.dryingEntryDao().delete(entry)
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// RecipesViewModel  – flatMapLatest requires @OptIn(ExperimentalCoroutinesApi)
// ══════════════════════════════════════════════════════════════════════════════
class RecipesViewModel(app: android.app.Application) : AndroidViewModel(app) {

    private val db = AppDatabase.getInstance(app)

    val searchQuery = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    val recipes: StateFlow<List<Recipe>> = searchQuery
        .flatMapLatest { q ->
            if (q.isBlank()) db.recipeDao().getAll()
            else             db.recipeDao().search(q)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setSearch(q: String) { searchQuery.value = q }

    fun addRecipe(recipe: Recipe) = viewModelScope.launch {
        db.recipeDao().insert(recipe)
    }

    fun updateRecipe(recipe: Recipe) = viewModelScope.launch {
        db.recipeDao().update(recipe.copy(updatedAt = System.currentTimeMillis()))
    }

    fun deleteRecipe(recipe: Recipe) = viewModelScope.launch {
        db.recipeDao().delete(recipe)
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// KIViewModel
// ══════════════════════════════════════════════════════════════════════════════
class KIViewModel(app: android.app.Application) : AndroidViewModel(app) {

    private val db    = AppDatabase.getInstance(app)
    private val prefs = PreferencesManager(app)

    val messages         = MutableStateFlow<List<ChatMessage>>(emptyList())
    val isLoading        = MutableStateFlow(false)
    val blendSuggestions = MutableStateFlow("")

    val hasApiKey: StateFlow<Boolean> = prefs.apiKey
        .map { it.isNotBlank() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val gardenHerbNames: StateFlow<List<String>> = combine(
        db.herbNoteDao().getGardenHerbs(),
        db.customHerbDao().getAll()
    ) { notes, customHerbs ->
        val customById = customHerbs.associateBy { it.id }
        notes.mapNotNull { note ->
            HerbDatabase.getById(note.herbId)?.name
                ?: customById[note.herbId]?.name
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun sendMessage(text: String) = viewModelScope.launch {
        val userMsg = ChatMessage("user", text)
        messages.value = messages.value + userMsg
        isLoading.value = true

        val key     = prefs.apiKey.first()
        val herbCtx = gardenHerbNames.value.joinToString(", ")
        // Pass full conversation history (including the just-added user message)
        val history = messages.value.map { Pair(it.role, it.content) }

        val result = GeminiService(key).getChatResponse(history, herbCtx)
        val response = when (result) {
            is ApiResult.Success -> ChatMessage("assistant", result.data)
            is ApiResult.Error   -> ChatMessage("assistant", "⚠️ ${result.message}", isError = true)
        }
        messages.value = messages.value + response
        isLoading.value = false
    }

    fun loadBlendSuggestions() = viewModelScope.launch {
        if (gardenHerbNames.value.isEmpty()) return@launch
        isLoading.value = true
        blendSuggestions.value = ""
        val key    = prefs.apiKey.first()
        val result = GeminiService(key).suggestTeaBlends(gardenHerbNames.value)
        blendSuggestions.value = when (result) {
            is ApiResult.Success -> result.data
            is ApiResult.Error   -> "⚠️ ${result.message}"
        }
        isLoading.value = false
    }

    fun clearChat() { messages.value = emptyList() }
}

// ══════════════════════════════════════════════════════════════════════════════
// SettingsViewModel
// ══════════════════════════════════════════════════════════════════════════════
class SettingsViewModel(app: android.app.Application) : AndroidViewModel(app) {

    private val prefs = PreferencesManager(app)

    val apiKey = prefs.apiKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
    val notifyHarvest = prefs.notifyHarvest
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
    val notifyDrying  = prefs.notifyDrying
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
    val daysBefore    = prefs.notifyDaysBefore
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 3)

    fun saveApiKey(key: String)      = viewModelScope.launch { prefs.saveApiKey(key) }
    fun setNotifyHarvest(v: Boolean) = viewModelScope.launch { prefs.setNotifyHarvest(v) }
    fun setNotifyDrying(v: Boolean)  = viewModelScope.launch { prefs.setNotifyDrying(v) }
    fun setDaysBefore(days: Int)     = viewModelScope.launch { prefs.setNotifyDaysBefore(days) }
}

// ══════════════════════════════════════════════════════════════════════════════
// ViewModelFactory
// ══════════════════════════════════════════════════════════════════════════════
class AppViewModelFactory(
    private val app: android.app.Application
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(HerbsViewModel::class.java)       -> HerbsViewModel(app)       as T
        modelClass.isAssignableFrom(CustomHerbsViewModel::class.java) -> CustomHerbsViewModel(app) as T
        modelClass.isAssignableFrom(MapViewModel::class.java)         -> MapViewModel(app)         as T
        modelClass.isAssignableFrom(CalendarViewModel::class.java)    -> CalendarViewModel(app)    as T
        modelClass.isAssignableFrom(RecipesViewModel::class.java)     -> RecipesViewModel(app)     as T
        modelClass.isAssignableFrom(KIViewModel::class.java)          -> KIViewModel(app)          as T
        modelClass.isAssignableFrom(SettingsViewModel::class.java)    -> SettingsViewModel(app)    as T
        else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
