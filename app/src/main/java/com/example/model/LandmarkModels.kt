package com.example.model

import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import com.example.R

enum class TourLanguage(
    val bcp47Code: String,
    val nativeName: String,
    val shortBadge: String,
    val englishName: String
) {
    ENGLISH("en-US", "English", "EN", "English"),
    SPANISH("es-ES", "Español", "ES", "Spanish"),
    FRENCH("fr-FR", "Français", "FR", "French"),
    ITALIAN("it-IT", "Italiano", "IT", "Italian"),
    JAPANESE("ja-JP", "日本語", "JA", "Japanese"),
    GERMAN("de-DE", "Deutsch", "DE", "German"),
    PORTUGUESE("pt-BR", "Português", "PT", "Portuguese"),
    KOREAN("ko-KR", "한국어", "KO", "Korean");

    companion object {
        fun fromCode(code: String?): TourLanguage {
            if (code.isNullOrBlank()) return ENGLISH
            return entries.find { it.bcp47Code.equals(code, ignoreCase = true) } ?: ENGLISH
        }
    }
}

data class ArHotspot(
    val id: String,
    val label: String,
    val detail: String,
    val normX: Float,
    val normY: Float,
    val tag: String
)

data class LandmarkRecognition(
    val landmarkName: String,
    val cityAndCountry: String,
    val architecturalStyle: String,
    val eraOrYearBuilt: String,
    val confidencePercent: Int,
    val visualSummary: String,
    val arHotspots: List<ArHotspot>
)

data class SearchSource(
    val title: String,
    val uri: String,
    val domain: String
)

data class GroundedHistoryDossier(
    val narrationScript: String,
    val narrationSentences: List<String>,
    val historicalOverview: String,
    val keyMilestones: List<String>,
    val visitorTip: String,
    val searchQueries: List<String>,
    val sources: List<SearchSource>
)

data class FollowUpAnswer(
    val question: String,
    val answer: String,
    val searchQueries: List<String>,
    val sources: List<SearchSource>
)

enum class NarratorVoice(
    val voiceId: String,
    val displayName: String,
    val personaSubtitle: String
) {
    KORE("Kore", "Kore", "Warm Documentary Guide"),
    AOEDE("Aoede", "Aoede", "Expressive Cultural Storyteller"),
    PUCK("Puck", "Puck", "Lively Urban Explorer"),
    CHARON("Charon", "Charon", "Deep Architectural Historian"),
    FENRIR("Fenrir", "Fenrir", "Cinematic Narrator");

    companion object {
        fun fromVoiceId(id: String?): NarratorVoice {
            if (id.isNullOrBlank()) return KORE
            return entries.find { it.voiceId.equals(id, ignoreCase = true) } ?: KORE
        }
    }
}

data class CompleteLandmarkScan(
    val id: String,
    val timestampMillis: Long,
    val bitmap: Bitmap,
    val recognition: LandmarkRecognition,
    val dossier: GroundedHistoryDossier,
    val audioBytes: ByteArray?,
    val audioMimeType: String?,
    val waveform: List<Float>,
    val narratorVoice: NarratorVoice,
    val language: TourLanguage = TourLanguage.ENGLISH
)

data class PresetLandmark(
    val id: String,
    val title: String,
    val city: String,
    @DrawableRes val drawableRes: Int,
    val badge: String
)

val PRESET_CITY_LANDMARKS = listOf(
    PresetLandmark(
        id = "colosseum_rome",
        title = "The Colosseum",
        city = "Rome, Italy",
        drawableRes = R.drawable.img_landmark_colosseum,
        badge = "70–80 AD • Flavian"
    ),
    PresetLandmark(
        id = "eiffel_paris",
        title = "Eiffel Tower",
        city = "Paris, France",
        drawableRes = R.drawable.img_landmark_eiffel,
        badge = "1889 • Iron Lattice"
    ),
    PresetLandmark(
        id = "sensoji_tokyo",
        title = "Senso-ji Pagoda",
        city = "Tokyo, Japan",
        drawableRes = R.drawable.img_landmark_tokyo,
        badge = "645 AD • Asakusa"
    )
)
