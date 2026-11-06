package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.ArAudioPlayer
import com.example.audio.AudioPlaybackState
import com.example.data.UserPreferencesRepository
import com.example.model.ArHotspot
import com.example.model.CompleteLandmarkScan
import com.example.model.FollowUpAnswer
import com.example.model.GroundedHistoryDossier
import com.example.model.LandmarkRecognition
import com.example.model.NarratorVoice
import com.example.model.PresetLandmark
import com.example.model.SearchSource
import com.example.model.TourLanguage
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.SpeechConfig
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.Voice
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "TourismViewModel"

sealed interface PipelineStage {
    data object Idle : PipelineStage
    data class AnalyzingImage(val bitmap: Bitmap) : PipelineStage
    data class SearchingHistory(
        val bitmap: Bitmap,
        val recognition: LandmarkRecognition
    ) : PipelineStage
    data class SynthesizingAudio(
        val bitmap: Bitmap,
        val recognition: LandmarkRecognition,
        val dossier: GroundedHistoryDossier
    ) : PipelineStage
    data class Ready(val scan: CompleteLandmarkScan) : PipelineStage
    data class Error(
        val message: String,
        val bitmap: Bitmap? = null,
        val partialRecognition: LandmarkRecognition? = null,
        val partialDossier: GroundedHistoryDossier? = null
    ) : PipelineStage
}

sealed interface FollowUpUiState {
    data object Idle : FollowUpUiState
    data class Loading(val question: String) : FollowUpUiState
    data class Success(val result: FollowUpAnswer) : FollowUpUiState
    data class Error(val message: String) : FollowUpUiState
}

class TourismViewModel : ViewModel() {

    // 1. JSON Schema for gemini-4-argon Landmark & AR Spatial Hotspot Recognition
    private val landmarkRecognitionSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "landmarkName" to Schema.string(
                    description = "Name of the recognized landmark, monument, building, or urban plaza"
                ),
                "cityAndCountry" to Schema.string(
                    description = "City and country where the landmark is located"
                ),
                "architecturalStyle" to Schema.string(
                    description = "Primary architectural style or engineering classification"
                ),
                "eraOrYearBuilt" to Schema.string(
                    description = "Construction year, century, or historical era"
                ),
                "confidencePercent" to Schema.integer(
                    description = "Recognition confidence score from 60 to 99"
                ),
                "visualSummary" to Schema.string(
                    description = "Concise 2-sentence visual description of the landmark features visible in the photo"
                ),
                "arHotspots" to Schema.array(
                    description = "3 to 4 spatial AR callout points placed on distinct architectural features visible in the image",
                    items = Schema.obj(
                        properties = mapOf(
                            "label" to Schema.string(description = "Short 2-4 word feature title"),
                            "detail" to Schema.string(description = "1-2 sentence architectural or historical explanation of this specific feature"),
                            "normX" to Schema.double(description = "Normalized horizontal coordinate from 0.15 (left) to 0.85 (right)"),
                            "normY" to Schema.double(description = "Normalized vertical coordinate from 0.15 (top) to 0.80 (bottom)"),
                            "tag" to Schema.string(description = "Category tag such as STRUCTURE, HISTORY, ARTISTRY, or ENGINEERING")
                        )
                    )
                )
            )
        )
    }

    // MANDATED MODEL 1: Image Understanding using gemini-4-argon (lazily initialized)
    private val visionModel: GenerativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-4-argon",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = landmarkRecognitionSchema
            }
        )
    }

    // MANDATED MODEL 2: Google Search Grounding using gemini-3.8-flash with Tool.googleSearch() (lazily initialized)
    private val searchGroundingModel: GenerativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            tools = listOf(Tool.googleSearch())
        )
    }

    // MANDATED MODEL 3: Text-to-Speech using gemini-3.8-flash-tts (lazily initialized per voice + language)
    private val ttsModels = mutableMapOf<Pair<NarratorVoice, TourLanguage>, GenerativeModel>()
    private val ttsVoiceOnlyModels = mutableMapOf<NarratorVoice, GenerativeModel>()
    private val ttsPreviewFallbackModels = mutableMapOf<NarratorVoice, GenerativeModel>()

    @OptIn(PublicPreviewAPI::class)
    private fun getTtsModel(voice: NarratorVoice, language: TourLanguage): GenerativeModel {
        return ttsModels.getOrPut(voice to language) {
            Firebase.ai.generativeModel(
                modelName = "gemini-3.8-flash-tts",
                generationConfig = generationConfig {
                    responseModalities = listOf(ResponseModality.AUDIO)
                    speechConfig = SpeechConfig(Voice(voice.voiceId), language.bcp47Code)
                }
            )
        }
    }

    @OptIn(PublicPreviewAPI::class)
    private fun getTtsVoiceOnlyModel(voice: NarratorVoice): GenerativeModel {
        return ttsVoiceOnlyModels.getOrPut(voice) {
            Firebase.ai.generativeModel(
                modelName = "gemini-3.8-flash-tts",
                generationConfig = generationConfig {
                    responseModalities = listOf(ResponseModality.AUDIO)
                    speechConfig = SpeechConfig(Voice(voice.voiceId))
                }
            )
        }
    }

    @OptIn(PublicPreviewAPI::class)
    private fun getTtsPreviewFallbackModel(voice: NarratorVoice): GenerativeModel {
        return ttsPreviewFallbackModels.getOrPut(voice) {
            Firebase.ai.generativeModel(
                modelName = "gemini-3.1-flash-tts-preview",
                generationConfig = generationConfig {
                    responseModalities = listOf(ResponseModality.AUDIO)
                    speechConfig = SpeechConfig(Voice(voice.voiceId))
                }
            )
        }
    }

    private val _pipelineStage = MutableStateFlow<PipelineStage>(PipelineStage.Idle)
    val pipelineStage: StateFlow<PipelineStage> = _pipelineStage.asStateFlow()

    private val _selectedLanguage = MutableStateFlow(TourLanguage.ENGLISH)
    val selectedLanguage: StateFlow<TourLanguage> = _selectedLanguage.asStateFlow()

    private val _selectedVoice = MutableStateFlow(NarratorVoice.KORE)
    val selectedVoice: StateFlow<NarratorVoice> = _selectedVoice.asStateFlow()

    private val _isSynthesizingVoiceOnly = MutableStateFlow(false)
    val isSynthesizingVoiceOnly: StateFlow<Boolean> = _isSynthesizingVoiceOnly.asStateFlow()

    private val _savedScans = MutableStateFlow<List<CompleteLandmarkScan>>(emptyList())
    val savedScans: StateFlow<List<CompleteLandmarkScan>> = _savedScans.asStateFlow()

    private val _followUpState = MutableStateFlow<FollowUpUiState>(FollowUpUiState.Idle)
    val followUpState: StateFlow<FollowUpUiState> = _followUpState.asStateFlow()

    private val _selectedHotspotId = MutableStateFlow<String?>(null)
    val selectedHotspotId: StateFlow<String?> = _selectedHotspotId.asStateFlow()

    private var arAudioPlayer: ArAudioPlayer? = null
    private var preferencesRepository: UserPreferencesRepository? = null

    private val _audioPlaybackState = MutableStateFlow(AudioPlaybackState())
    val audioPlaybackState: StateFlow<AudioPlaybackState> = _audioPlaybackState.asStateFlow()

    fun ensureAudioPlayer(context: Context) {
        if (arAudioPlayer == null) {
            val player = ArAudioPlayer(context.applicationContext, viewModelScope)
            arAudioPlayer = player
            viewModelScope.launch {
                player.playbackState.collect { state ->
                    _audioPlaybackState.value = state
                }
            }
        }
        if (preferencesRepository == null) {
            val repo = UserPreferencesRepository(context.applicationContext)
            preferencesRepository = repo
            viewModelScope.launch {
                repo.userPreferencesFlow.collect { prefs ->
                    _selectedLanguage.value = prefs.preferredLanguage
                    _selectedVoice.value = prefs.preferredVoice
                }
            }
        }
    }

    /**
     * Persists the user's preferred language in DataStore via UserPreferencesRepository
     * and, if a landmark scan is currently open, refreshes the historical facts & narration in that language.
     */
    fun selectPreferredLanguage(context: Context, language: TourLanguage) {
        ensureAudioPlayer(context)
        val previous = _selectedLanguage.value
        _selectedLanguage.value = language
        viewModelScope.launch {
            preferencesRepository?.setPreferredLanguage(language)
        }

        val currentStage = _pipelineStage.value
        if (currentStage is PipelineStage.Ready && previous != language) {
            refreshLanguageForScan(context, currentStage.scan, language)
        }
    }

    fun selectHotspot(hotspotId: String?) {
        _selectedHotspotId.value = if (_selectedHotspotId.value == hotspotId) null else hotspotId
    }

    fun analyzePresetLandmark(context: Context, preset: PresetLandmark) {
        ensureAudioPlayer(context)
        viewModelScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    val opts = BitmapFactory.Options().apply {
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }
                    val decoded = BitmapFactory.decodeResource(context.resources, preset.drawableRes, opts)
                        ?: throw IllegalStateException("Could not decode preset landmark image.")
                    scaleBitmapUnderLimit(decoded, 1280)
                }
                runFullTourismPipeline(
                    context = context,
                    bitmap = bitmap,
                    hintContext = "City photo taken in ${preset.city} showing ${preset.title}"
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load preset landmark", e)
                _pipelineStage.value = PipelineStage.Error(
                    message = e.localizedMessage ?: "Failed to load preset landmark photo."
                )
            }
        }
    }

    fun analyzeUriPhoto(context: Context, uri: Uri) {
        ensureAudioPlayer(context)
        viewModelScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    val decoded = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val source = ImageDecoder.createSource(context.contentResolver, uri)
                        ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                            decoder.isMutableRequired = true
                            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                    }
                    scaleBitmapUnderLimit(decoded, 1280)
                }
                runFullTourismPipeline(context = context, bitmap = bitmap)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to decode photo from URI", e)
                _pipelineStage.value = PipelineStage.Error(
                    message = e.localizedMessage ?: "Unable to read the selected photo."
                )
            }
        }
    }

    fun analyzeCapturedBitmap(context: Context, rawBitmap: Bitmap) {
        ensureAudioPlayer(context)
        viewModelScope.launch {
            val scaled = withContext(Dispatchers.Default) {
                scaleBitmapUnderLimit(rawBitmap, 1280)
            }
            runFullTourismPipeline(context = context, bitmap = scaled)
        }
    }

    fun retryCurrentPhoto(context: Context) {
        val current = _pipelineStage.value
        val bitmap = when (current) {
            is PipelineStage.Error -> current.bitmap
            is PipelineStage.Ready -> current.scan.bitmap
            is PipelineStage.AnalyzingImage -> current.bitmap
            is PipelineStage.SearchingHistory -> current.bitmap
            is PipelineStage.SynthesizingAudio -> current.bitmap
            PipelineStage.Idle -> null
        }
        if (bitmap != null) {
            runFullTourismPipeline(context = context, bitmap = bitmap)
        }
    }

    private fun runFullTourismPipeline(
        context: Context,
        bitmap: Bitmap,
        hintContext: String? = null
    ) {
        ensureAudioPlayer(context)
        arAudioPlayer?.stopAndRelease()
        _followUpState.value = FollowUpUiState.Idle

        val targetLanguage = _selectedLanguage.value
        val currentVoice = _selectedVoice.value

        viewModelScope.launch {
            _pipelineStage.value = PipelineStage.AnalyzingImage(bitmap)
            var recognitionResult: LandmarkRecognition? = null
            var dossierResult: GroundedHistoryDossier? = null

            try {
                // STEP 1: Analyze Landmark Image using gemini-4-argon
                val visionPrompt = buildString {
                    append(
                        "You are an expert architectural historian and computer-vision city guide. " +
                            "Analyze this city photograph, identify the landmark or architectural structure, " +
                            "its city and country, style, era built, and pinpoint 3 to 4 distinct visual features " +
                            "with normalized (normX, normY) coordinates on the image for an AR HUD overlay. " +
                            "Write all descriptive text values (landmarkName, architecturalStyle, visualSummary, and arHotspots label/detail) " +
                            "in ${targetLanguage.englishName} (${targetLanguage.nativeName})."
                    )
                    if (!hintContext.isNullOrBlank()) {
                        append(" Visual context hint: $hintContext.")
                    }
                }

                val visionResponse = withTimeout(60_000L) {
                    visionModel.generateContent(
                        content {
                            image(bitmap)
                            text(visionPrompt)
                        }
                    )
                }

                val visionJsonText = visionResponse.text
                    ?: throw IllegalStateException("gemini-4-argon returned an empty landmark analysis.")
                recognitionResult = parseLandmarkRecognitionJson(visionJsonText)
                _selectedHotspotId.value = recognitionResult.arHotspots.firstOrNull()?.id

                // STEP 2: Fetch Real-Time Grounded History via Google Search using gemini-3.8-flash
                _pipelineStage.value = PipelineStage.SearchingHistory(bitmap, recognitionResult)

                dossierResult = fetchGroundedHistoryDossier(
                    recognition = recognitionResult,
                    language = targetLanguage
                )

                // STEP 3: Synthesize AR Narrated Clip using gemini-3.8-flash-tts
                _pipelineStage.value = PipelineStage.SynthesizingAudio(
                    bitmap = bitmap,
                    recognition = recognitionResult,
                    dossier = dossierResult
                )

                val (audioBytes, audioMimeType) = synthesizeSpeechAudio(
                    script = dossierResult.narrationScript,
                    voice = currentVoice,
                    language = targetLanguage
                )

                val waveform = if (audioBytes != null) {
                    ArAudioPlayer.extractWaveformBars(audioBytes, 36)
                } else {
                    emptyList()
                }

                val completeScan = CompleteLandmarkScan(
                    id = UUID.randomUUID().toString(),
                    timestampMillis = System.currentTimeMillis(),
                    bitmap = bitmap,
                    recognition = recognitionResult,
                    dossier = dossierResult,
                    audioBytes = audioBytes,
                    audioMimeType = audioMimeType,
                    waveform = waveform,
                    narratorVoice = currentVoice,
                    language = targetLanguage
                )

                _pipelineStage.value = PipelineStage.Ready(completeScan)
                saveScanToHistory(completeScan)

                if (audioBytes != null && audioBytes.isNotEmpty()) {
                    arAudioPlayer?.loadAndPlayAudio(
                        rawBytes = audioBytes,
                        mimeType = audioMimeType,
                        autoPlay = true
                    )
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Tourism AI pipeline timed out", e)
                _pipelineStage.value = PipelineStage.Error(
                    message = "AI request timed out after 60 seconds. Please check your connection and tap Retry.",
                    bitmap = bitmap,
                    partialRecognition = recognitionResult,
                    partialDossier = dossierResult
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Tourism AI pipeline failed", e)
                _pipelineStage.value = PipelineStage.Error(
                    message = e.localizedMessage ?: "Landmark recognition or grounding failed.",
                    bitmap = bitmap,
                    partialRecognition = recognitionResult,
                    partialDossier = dossierResult
                )
            }
        }
    }

    /**
     * Re-generates grounded historical facts & TTS narration in the newly selected language
     * when the user switches languages on an active landmark scan.
     */
    private fun refreshLanguageForScan(
        context: Context,
        existingScan: CompleteLandmarkScan,
        targetLanguage: TourLanguage
    ) {
        ensureAudioPlayer(context)
        arAudioPlayer?.stopAndRelease()
        val currentVoice = _selectedVoice.value

        viewModelScope.launch {
            _pipelineStage.value = PipelineStage.SearchingHistory(
                bitmap = existingScan.bitmap,
                recognition = existingScan.recognition
            )
            try {
                val updatedDossier = fetchGroundedHistoryDossier(
                    recognition = existingScan.recognition,
                    language = targetLanguage
                )

                _pipelineStage.value = PipelineStage.SynthesizingAudio(
                    bitmap = existingScan.bitmap,
                    recognition = existingScan.recognition,
                    dossier = updatedDossier
                )

                val (audioBytes, audioMimeType) = synthesizeSpeechAudio(
                    script = updatedDossier.narrationScript,
                    voice = currentVoice,
                    language = targetLanguage
                )

                val waveform = if (audioBytes != null) {
                    ArAudioPlayer.extractWaveformBars(audioBytes, 36)
                } else {
                    existingScan.waveform
                }

                val updatedScan = existingScan.copy(
                    dossier = updatedDossier,
                    audioBytes = audioBytes,
                    audioMimeType = audioMimeType,
                    waveform = waveform,
                    narratorVoice = currentVoice,
                    language = targetLanguage
                )

                _pipelineStage.value = PipelineStage.Ready(updatedScan)
                saveScanToHistory(updatedScan)

                if (audioBytes != null && audioBytes.isNotEmpty()) {
                    arAudioPlayer?.loadAndPlayAudio(
                        rawBytes = audioBytes,
                        mimeType = audioMimeType,
                        autoPlay = true
                    )
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Language refresh timed out", e)
                _pipelineStage.value = PipelineStage.Error(
                    message = "Translating grounded history & narration timed out after 60 seconds.",
                    bitmap = existingScan.bitmap,
                    partialRecognition = existingScan.recognition,
                    partialDossier = existingScan.dossier
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Language refresh failed", e)
                _pipelineStage.value = PipelineStage.Error(
                    message = e.localizedMessage ?: "Failed to update language for historical facts.",
                    bitmap = existingScan.bitmap,
                    partialRecognition = existingScan.recognition,
                    partialDossier = existingScan.dossier
                )
            }
        }
    }

    private suspend fun fetchGroundedHistoryDossier(
        recognition: LandmarkRecognition,
        language: TourLanguage
    ): GroundedHistoryDossier {
        val searchPrompt = """
            Use Google Search to retrieve accurate, up-to-date historical and architectural facts about:
            Landmark: ${recognition.landmarkName}
            Location: ${recognition.cityAndCountry}
            Style / Era: ${recognition.architecturalStyle} (${recognition.eraOrYearBuilt})

            IMPORTANT LANGUAGE REQUIREMENT:
            Write the content of all four sections entirely in ${language.englishName} (${language.nativeName}, BCP-47: ${language.bcp47Code}), while keeping the exact bracketed section headers in English so they can be parsed:

            [NARRATION_SCRIPT]
            Write a vivid, documentary-style spoken AR audio tour script in ${language.englishName} (${language.nativeName}) in 3 to 4 engaging sentences (around 55-75 words) as if standing right in front of ${recognition.landmarkName}. Do not include stage directions or markdown formatting inside this script.

            [HISTORICAL_OVERVIEW]
            Provide a rich 2-paragraph historical and cultural deep-dive in ${language.englishName} (${language.nativeName}) grounded in verified facts, including who built or designed it, why it was constructed, and how it shaped ${recognition.cityAndCountry}.

            [MILESTONES]
            List 4 chronological historical milestones in ${language.englishName} (${language.nativeName}), one per line, each starting with "- " and a year/date.

            [VISITOR_TIP]
            Provide 1-2 sentences of practical, current visitor context or architectural viewing tip in ${language.englishName} (${language.nativeName}).
        """.trimIndent()

        val searchResponse = withTimeout(60_000L) {
            searchGroundingModel.generateContent(searchPrompt)
        }

        val searchText = searchResponse.text
            ?: throw IllegalStateException("gemini-3.8-flash returned an empty search grounding response.")

        val candidate = searchResponse.candidates.firstOrNull()
        val groundingMeta = candidate?.groundingMetadata
        val webQueries = groundingMeta?.webSearchQueries?.filter { it.isNotBlank() }.orEmpty()
        val webSources = groundingMeta?.groundingChunks
            ?.mapNotNull { chunk ->
                val web = chunk.web
                val uriVal = web?.uri
                if (web != null && !uriVal.isNullOrBlank()) {
                    SearchSource(
                        title = web.title?.takeIf { it.isNotBlank() }
                            ?: web.domain?.takeIf { it.isNotBlank() }
                            ?: "Google Search Source",
                        uri = uriVal,
                        domain = web.domain?.takeIf { it.isNotBlank() } ?: extractDomain(uriVal)
                    )
                } else {
                    null
                }
            }
            ?.distinctBy { it.uri }
            .orEmpty()

        return parseGroundedDossier(
            rawText = searchText,
            landmarkName = recognition.landmarkName,
            searchQueries = webQueries,
            sources = webSources
        )
    }

    /**
     * Synthesizes narration audio using mandated model `gemini-3.8-flash-tts` in the user's preferred language.
     */
    private suspend fun synthesizeSpeechAudio(
        script: String,
        voice: NarratorVoice,
        language: TourLanguage
    ): Pair<ByteArray?, String?> {
        val ttsPrompt = "Narrate this architectural tour script clearly in ${language.englishName} (${language.nativeName}): $script"
        val response = try {
            withTimeout(60_000L) {
                getTtsModel(voice, language).generateContent(ttsPrompt)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (primaryError: Exception) {
            Log.w(TAG, "Primary TTS call with languageCode failed, retrying voice-only gemini-3.8-flash-tts", primaryError)
            try {
                withTimeout(60_000L) {
                    getTtsVoiceOnlyModel(voice).generateContent(ttsPrompt)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (secondaryError: Exception) {
                Log.w(TAG, "Retrying with gemini-3.1-flash-tts-preview", secondaryError)
                withTimeout(60_000L) {
                    getTtsPreviewFallbackModel(voice).generateContent(ttsPrompt)
                }
            }
        }
        val audioPart = response.inlineDataParts.firstOrNull()
            ?: throw IllegalStateException("TTS model did not return an inline audio stream.")
        return audioPart.inlineData to audioPart.mimeType
    }

    /**
     * Switches the narrator voice, persists it in DataStore, and re-synthesizes the AR audio clip using `gemini-3.8-flash-tts`.
     */
    fun switchNarratorVoice(context: Context, newVoice: NarratorVoice) {
        ensureAudioPlayer(context)
        _selectedVoice.value = newVoice
        viewModelScope.launch {
            preferencesRepository?.setPreferredVoice(newVoice)
        }

        val currentStage = _pipelineStage.value
        if (currentStage !is PipelineStage.Ready) return

        val currentScan = currentStage.scan
        val currentLanguage = _selectedLanguage.value
        viewModelScope.launch {
            _isSynthesizingVoiceOnly.value = true
            try {
                val (audioBytes, audioMimeType) = synthesizeSpeechAudio(
                    script = currentScan.dossier.narrationScript,
                    voice = newVoice,
                    language = currentLanguage
                )
                val waveform = if (audioBytes != null) {
                    ArAudioPlayer.extractWaveformBars(audioBytes, 36)
                } else {
                    currentScan.waveform
                }
                val updatedScan = currentScan.copy(
                    audioBytes = audioBytes,
                    audioMimeType = audioMimeType,
                    waveform = waveform,
                    narratorVoice = newVoice,
                    language = currentLanguage
                )
                _pipelineStage.value = PipelineStage.Ready(updatedScan)
                saveScanToHistory(updatedScan)

                if (audioBytes != null && audioBytes.isNotEmpty()) {
                    arAudioPlayer?.loadAndPlayAudio(
                        rawBytes = audioBytes,
                        mimeType = audioMimeType,
                        autoPlay = true
                    )
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "TTS voice synthesis timed out", e)
                _pipelineStage.value = PipelineStage.Error(
                    message = "Voice synthesis timed out after 60 seconds.",
                    bitmap = currentScan.bitmap,
                    partialRecognition = currentScan.recognition,
                    partialDossier = currentScan.dossier
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "TTS voice synthesis failed", e)
                _pipelineStage.value = PipelineStage.Error(
                    message = e.localizedMessage ?: "Failed to synthesize voice narration.",
                    bitmap = currentScan.bitmap,
                    partialRecognition = currentScan.recognition,
                    partialDossier = currentScan.dossier
                )
            } finally {
                _isSynthesizingVoiceOnly.value = false
            }
        }
    }

    /**
     * Interactive Search-Grounded Q&A using `gemini-3.8-flash` + `Tool.googleSearch()` in the user's preferred language.
     */
    fun askFollowUpQuestion(question: String) {
        val currentStage = _pipelineStage.value
        if (currentStage !is PipelineStage.Ready || question.isBlank()) return
        val recognition = currentStage.scan.recognition
        val language = _selectedLanguage.value

        viewModelScope.launch {
            _followUpState.value = FollowUpUiState.Loading(question)
            try {
                val prompt = """
                    Using Google Search, answer this visitor question about ${recognition.landmarkName} (${recognition.cityAndCountry}):
                    Question: $question
                    Respond in ${language.englishName} (${language.nativeName}). Keep your answer accurate, concise (2-4 sentences), and grounded in up-to-date facts.
                """.trimIndent()

                val response = withTimeout(60_000L) {
                    searchGroundingModel.generateContent(prompt)
                }
                val answerText = response.text?.trim()
                if (answerText.isNullOrEmpty()) {
                    _followUpState.value = FollowUpUiState.Error("Model returned an empty answer.")
                    return@launch
                }

                val groundingMeta = response.candidates.firstOrNull()?.groundingMetadata
                val queries = groundingMeta?.webSearchQueries?.filter { it.isNotBlank() }.orEmpty()
                val sources = groundingMeta?.groundingChunks
                    ?.mapNotNull { chunk ->
                        val web = chunk.web
                        val uriVal = web?.uri
                        if (web != null && !uriVal.isNullOrBlank()) {
                            SearchSource(
                                title = web.title?.takeIf { it.isNotBlank() } ?: "Search Result",
                                uri = uriVal,
                                domain = web.domain?.takeIf { it.isNotBlank() } ?: extractDomain(uriVal)
                            )
                        } else {
                            null
                        }
                    }
                    ?.distinctBy { it.uri }
                    .orEmpty()

                _followUpState.value = FollowUpUiState.Success(
                    FollowUpAnswer(
                        question = question,
                        answer = answerText,
                        searchQueries = queries,
                        sources = sources
                    )
                )
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Follow-up search timed out", e)
                _followUpState.value = FollowUpUiState.Error("Search query timed out after 60 seconds.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Follow-up search failed", e)
                _followUpState.value = FollowUpUiState.Error(
                    e.localizedMessage ?: "Failed to fetch grounded answer."
                )
            }
        }
    }

    fun loadSavedScan(context: Context, scan: CompleteLandmarkScan) {
        ensureAudioPlayer(context)
        _selectedVoice.value = scan.narratorVoice
        _selectedHotspotId.value = scan.recognition.arHotspots.firstOrNull()?.id
        _followUpState.value = FollowUpUiState.Idle
        _pipelineStage.value = PipelineStage.Ready(scan)
        if (scan.audioBytes != null && scan.audioBytes.isNotEmpty()) {
            arAudioPlayer?.loadAndPlayAudio(
                rawBytes = scan.audioBytes,
                mimeType = scan.audioMimeType,
                autoPlay = false
            )
        }
    }

    fun toggleAudioPlayPause() {
        arAudioPlayer?.togglePlayPause()
    }

    fun seekAudio(fraction: Float) {
        arAudioPlayer?.seekToFraction(fraction)
    }

    fun resetToViewfinder() {
        arAudioPlayer?.pause()
        _pipelineStage.value = PipelineStage.Idle
        _followUpState.value = FollowUpUiState.Idle
    }

    private fun saveScanToHistory(scan: CompleteLandmarkScan) {
        val existing = _savedScans.value.toMutableList()
        val index = existing.indexOfFirst {
            it.recognition.landmarkName.equals(scan.recognition.landmarkName, ignoreCase = true)
        }
        if (index >= 0) {
            existing[index] = scan
        } else {
            existing.add(0, scan)
        }
        _savedScans.value = existing.take(12)
    }

    override fun onCleared() {
        super.onCleared()
        arAudioPlayer?.stopAndRelease()
    }

    companion object {
        fun parseLandmarkRecognitionJson(jsonString: String): LandmarkRecognition {
            val cleanedJson = jsonString
                .trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
            val json = JSONObject(cleanedJson)
            val landmarkName = json.optString("landmarkName", "Historic City Landmark")
            val cityAndCountry = json.optString("cityAndCountry", "Urban Cultural District")
            val architecturalStyle = json.optString("architecturalStyle", "Monumental Architecture")
            val eraOrYearBuilt = json.optString("eraOrYearBuilt", "Historic Era")
            val confidencePercent = json.optInt("confidencePercent", 94).coerceIn(50, 99)
            val visualSummary = json.optString(
                "visualSummary",
                "Architectural landmark recognized from structural proportions and facade detailing."
            )

            val hotspotsArray = json.optJSONArray("arHotspots")
            val hotspots = mutableListOf<ArHotspot>()
            if (hotspotsArray != null) {
                for (i in 0 until hotspotsArray.length()) {
                    val item = hotspotsArray.optJSONObject(i) ?: continue
                    hotspots.add(
                        ArHotspot(
                            id = "hotspot_$i",
                            label = item.optString("label", "Architectural Detail"),
                            detail = item.optString("detail", "Notable structural element in the facade."),
                            normX = item.optDouble("normX", 0.3 + i * 0.2).toFloat().coerceIn(0.12f, 0.88f),
                            normY = item.optDouble("normY", 0.35 + (i % 2) * 0.22).toFloat().coerceIn(0.15f, 0.82f),
                            tag = item.optString("tag", "ARCHITECTURE")
                        )
                    )
                }
            }
            if (hotspots.isEmpty()) {
                hotspots.add(
                    ArHotspot(
                        id = "hotspot_0",
                        label = landmarkName,
                        detail = visualSummary,
                        normX = 0.5f,
                        normY = 0.45f,
                        tag = "PRIMARY STRUCTURE"
                    )
                )
            }

            return LandmarkRecognition(
                landmarkName = landmarkName,
                cityAndCountry = cityAndCountry,
                architecturalStyle = architecturalStyle,
                eraOrYearBuilt = eraOrYearBuilt,
                confidencePercent = confidencePercent,
                visualSummary = visualSummary,
                arHotspots = hotspots
            )
        }

        fun parseGroundedDossier(
            rawText: String,
            landmarkName: String,
            searchQueries: List<String>,
            sources: List<SearchSource>
        ): GroundedHistoryDossier {
            val cleaned = rawText.replace("**", "")
            val narration = extractSection(
                cleaned,
                "[NARRATION_SCRIPT]",
                listOf("[HISTORICAL_OVERVIEW]", "[MILESTONES]", "[VISITOR_TIP]")
            ).ifBlank {
                cleaned.lineSequence().firstOrNull { it.length > 40 }
                    ?: "Welcome to $landmarkName, an enduring masterpiece of urban history and architectural craft."
            }

            val overview = extractSection(
                cleaned,
                "[HISTORICAL_OVERVIEW]",
                listOf("[MILESTONES]", "[VISITOR_TIP]")
            ).ifBlank {
                cleaned
            }

            val milestonesBlock = extractSection(
                cleaned,
                "[MILESTONES]",
                listOf("[VISITOR_TIP]")
            )
            val milestones = milestonesBlock
                .lines()
                .map { it.trim().removePrefix("-").removePrefix("•").removePrefix("*").trim() }
                .filter { it.length > 6 }
                .ifEmpty {
                    listOf(
                        "Recognized as a defining cultural and architectural icon of the city.",
                        "Preserved as a historic monument visited by travelers from around the world."
                    )
                }

            val visitorTip = extractSection(
                cleaned,
                "[VISITOR_TIP]",
                emptyList()
            ).ifBlank {
                "Best photographed during golden hour when warm light accentuates the exterior relief and structural depth."
            }

            val sentences = narration
                .split(Regex("(?<=[.!?。！？])\\s*"))
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .ifEmpty { listOf(narration) }

            return GroundedHistoryDossier(
                narrationScript = narration,
                narrationSentences = sentences,
                historicalOverview = overview,
                keyMilestones = milestones,
                visitorTip = visitorTip,
                searchQueries = searchQueries.ifEmpty { listOf("$landmarkName history architecture") },
                sources = sources
            )
        }

        private fun extractSection(
            text: String,
            header: String,
            nextHeaders: List<String>
        ): String {
            val startIdx = text.indexOf(header, ignoreCase = true)
            if (startIdx == -1) return ""
            val contentStart = startIdx + header.length
            var endIdx = text.length
            for (next in nextHeaders) {
                val idx = text.indexOf(next, startIndex = contentStart, ignoreCase = true)
                if (idx != -1 && idx < endIdx) {
                    endIdx = idx
                }
            }
            return text.substring(contentStart, endIdx).trim()
        }

        private fun extractDomain(uri: String): String {
            return try {
                Uri.parse(uri).host?.removePrefix("www.") ?: "google.com"
            } catch (_: Exception) {
                "google.com"
            }
        }

        fun scaleBitmapUnderLimit(source: Bitmap, maxDimension: Int): Bitmap {
            val width = source.width
            val height = source.height
            if (width <= maxDimension && height <= maxDimension) {
                return source
            }
            val ratio = width.toFloat() / height.toFloat()
            val (targetW, targetH) = if (width >= height) {
                maxDimension to (maxDimension / ratio).toInt().coerceAtLeast(1)
            } else {
                (maxDimension * ratio).toInt().coerceAtLeast(1) to maxDimension
            }
            return Bitmap.createScaledBitmap(source, targetW, targetH, true)
        }
    }
}
