package com.example.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.NarratorVoice
import com.example.model.TourLanguage
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private const val TAG = "UserPreferencesRepo"
private const val DATASTORE_NAME = "vistalens_user_preferences"

val Context.vistaLensDataStore: DataStore<Preferences> by preferencesDataStore(
    name = DATASTORE_NAME
)

data class UserTourPreferences(
    val preferredLanguage: TourLanguage = TourLanguage.ENGLISH,
    val preferredVoice: NarratorVoice = NarratorVoice.KORE
)

class UserPreferencesRepository(
    private val dataStore: DataStore<Preferences>
) {
    constructor(context: Context) : this(context.applicationContext.vistaLensDataStore)

    private object PreferencesKeys {
        val PREFERRED_LANGUAGE_CODE = stringPreferencesKey("preferred_language_code")
        val PREFERRED_VOICE_ID = stringPreferencesKey("preferred_voice_id")
    }

    val userPreferencesFlow: Flow<UserTourPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e(TAG, "Error reading DataStore preferences", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val languageCode = preferences[PreferencesKeys.PREFERRED_LANGUAGE_CODE]
            val voiceId = preferences[PreferencesKeys.PREFERRED_VOICE_ID]
            UserTourPreferences(
                preferredLanguage = TourLanguage.fromCode(languageCode),
                preferredVoice = NarratorVoice.fromVoiceId(voiceId)
            )
        }

    val preferredLanguageFlow: Flow<TourLanguage> = userPreferencesFlow.map { it.preferredLanguage }

    suspend fun setPreferredLanguage(language: TourLanguage) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.PREFERRED_LANGUAGE_CODE] = language.bcp47Code
        }
    }

    suspend fun setPreferredVoice(voice: NarratorVoice) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.PREFERRED_VOICE_ID] = voice.voiceId
        }
    }
}
