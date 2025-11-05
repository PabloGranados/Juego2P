package com.example.buscaminas.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.buscaminas.model.AppTheme
import com.example.buscaminas.model.FileFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Gestor de preferencias de la aplicación usando DataStore
 */
class GamePreferences(private val context: Context) {
    
    companion object {
        private val THEME_KEY = stringPreferencesKey("app_theme")
        private val FILE_FORMAT_KEY = stringPreferencesKey("file_format")
        private val SOUND_ENABLED_KEY = booleanPreferencesKey("sound_enabled")
    }
    
    /**
     * Obtiene el tema seleccionado
     */
    val appTheme: Flow<AppTheme> = context.dataStore.data.map { preferences ->
        val themeName = preferences[THEME_KEY] ?: AppTheme.GUINDA_IPN.name
        AppTheme.fromString(themeName)
    }
    
    /**
     * Obtiene el formato de archivo preferido
     */
    val fileFormat: Flow<FileFormat> = context.dataStore.data.map { preferences ->
        val formatName = preferences[FILE_FORMAT_KEY] ?: FileFormat.JSON.name
        FileFormat.values().find { it.name == formatName } ?: FileFormat.JSON
    }
    
    /**
     * Obtiene si el sonido está habilitado
     */
    val soundEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SOUND_ENABLED_KEY] ?: true
    }
    
    /**
     * Guarda el tema seleccionado
     */
    suspend fun setAppTheme(theme: AppTheme) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme.name
        }
    }
    
    /**
     * Guarda el formato de archivo preferido
     */
    suspend fun setFileFormat(format: FileFormat) {
        context.dataStore.edit { preferences ->
            preferences[FILE_FORMAT_KEY] = format.name
        }
    }
    
    /**
     * Guarda si el sonido está habilitado
     */
    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SOUND_ENABLED_KEY] = enabled
        }
    }
}
