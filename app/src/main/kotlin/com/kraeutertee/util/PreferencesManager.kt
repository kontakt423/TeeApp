package com.kraeutertee.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "kraeutertee_prefs")

class PreferencesManager(private val context: Context) {

    companion object {
        val KEY_API_KEY       = stringPreferencesKey("gemini_api_key")
        val KEY_LAST_LAT      = floatPreferencesKey("last_latitude")
        val KEY_LAST_LNG      = floatPreferencesKey("last_longitude")
        val KEY_THEME_DARK    = booleanPreferencesKey("theme_dark")
        val KEY_NOTIFY_HARVEST = booleanPreferencesKey("notify_harvest")
        val KEY_NOTIFY_DRYING  = booleanPreferencesKey("notify_drying")
        val KEY_NOTIFY_DAYS_BEFORE = intPreferencesKey("notify_days_before")
        val KEY_USER_NAME     = stringPreferencesKey("user_name")
    }

    val apiKey: Flow<String> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it[KEY_API_KEY] ?: "" }

    val lastLocation: Flow<Pair<Double, Double>> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            val lat = prefs[KEY_LAST_LAT]?.toDouble() ?: 48.1374
            val lng = prefs[KEY_LAST_LNG]?.toDouble() ?: 11.5755
            Pair(lat, lng)
        }

    val notifyHarvest: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_NOTIFY_HARVEST] ?: true }

    val notifyDrying: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_NOTIFY_DRYING] ?: true }

    val notifyDaysBefore: Flow<Int> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_NOTIFY_DAYS_BEFORE] ?: 3 }

    val isDarkTheme: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_THEME_DARK] ?: false }

    suspend fun saveApiKey(key: String) = context.dataStore.edit { it[KEY_API_KEY] = key }
    suspend fun saveLocation(lat: Double, lng: Double) = context.dataStore.edit {
        it[KEY_LAST_LAT] = lat.toFloat()
        it[KEY_LAST_LNG] = lng.toFloat()
    }
    suspend fun setDarkTheme(dark: Boolean) = context.dataStore.edit { it[KEY_THEME_DARK] = dark }
    suspend fun setNotifyHarvest(v: Boolean) = context.dataStore.edit { it[KEY_NOTIFY_HARVEST] = v }
    suspend fun setNotifyDrying(v: Boolean) = context.dataStore.edit { it[KEY_NOTIFY_DRYING] = v }
    suspend fun setNotifyDaysBefore(days: Int) = context.dataStore.edit { it[KEY_NOTIFY_DAYS_BEFORE] = days }
    suspend fun setUserName(name: String) = context.dataStore.edit { it[KEY_USER_NAME] = name }
}
