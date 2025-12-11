package com.supertone.supertonic

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.supertone.supertonic.ui.theme.ThemePalette
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * User preferences manager using DataStore
 */
class PreferencesManager(private val context: Context) {
    
    companion object {
        private val THEME_PALETTE_KEY = stringPreferencesKey("theme_palette")
        private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
        private val DEFAULT_VOICE_KEY = stringPreferencesKey("default_voice")
        private val DEFAULT_SPEED_KEY = stringPreferencesKey("default_speed")
        private val DEFAULT_STEPS_KEY = stringPreferencesKey("default_steps")
    }
    
    val themePalette: Flow<ThemePalette> = context.dataStore.data.map { preferences ->
        val paletteName = preferences[THEME_PALETTE_KEY] ?: ThemePalette.PURPLE.name
        try {
            ThemePalette.valueOf(paletteName)
        } catch (e: IllegalArgumentException) {
            ThemePalette.PURPLE
        }
    }
    
    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DARK_MODE_KEY] ?: false
    }
    
    val defaultVoice: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[DEFAULT_VOICE_KEY] ?: "M1"
    }
    
    val defaultSpeed: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[DEFAULT_SPEED_KEY]?.toFloatOrNull() ?: 1.05f
    }
    
    val defaultSteps: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[DEFAULT_STEPS_KEY]?.toIntOrNull() ?: 5
    }
    
    suspend fun setThemePalette(palette: ThemePalette) {
        context.dataStore.edit { preferences ->
            preferences[THEME_PALETTE_KEY] = palette.name
        }
    }
    
    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE_KEY] = enabled
        }
    }
    
    suspend fun setDefaultVoice(voice: String) {
        context.dataStore.edit { preferences ->
            preferences[DEFAULT_VOICE_KEY] = voice
        }
    }
    
    suspend fun setDefaultSpeed(speed: Float) {
        context.dataStore.edit { preferences ->
            preferences[DEFAULT_SPEED_KEY] = speed.toString()
        }
    }
    
    suspend fun setDefaultSteps(steps: Int) {
        context.dataStore.edit { preferences ->
            preferences[DEFAULT_STEPS_KEY] = steps.toString()
        }
    }
}
