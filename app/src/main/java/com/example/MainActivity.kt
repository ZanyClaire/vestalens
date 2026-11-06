package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraEnhance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.CompleteLandmarkScan
import com.example.model.TourLanguage
import com.example.ui.ArNarratedClipStage
import com.example.ui.CameraViewfinderSection
import com.example.ui.GroundedHistoryDossierSection
import com.example.ui.PipelineErrorCard
import com.example.ui.PipelineLoadingOverlayCard
import com.example.ui.PipelineStage
import com.example.ui.TourismViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure Firebase App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                VistaLensApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VistaLensApp(
    viewModel: TourismViewModel = viewModel()
) {
    val context = LocalContext.current
    LaunchedEffect(context) {
        viewModel.ensureAudioPlayer(context)
    }

    val pipelineStage by viewModel.pipelineStage.collectAsStateWithLifecycle()
    val playbackState by viewModel.audioPlaybackState.collectAsStateWithLifecycle()
    val selectedHotspotId by viewModel.selectedHotspotId.collectAsStateWithLifecycle()
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val selectedVoice by viewModel.selectedVoice.collectAsStateWithLifecycle()
    val isSynthesizingVoice by viewModel.isSynthesizingVoiceOnly.collectAsStateWithLifecycle()
    val followUpState by viewModel.followUpState.collectAsStateWithLifecycle()
    val savedScans by viewModel.savedScans.collectAsStateWithLifecycle()

    var showLanguageSheet by rememberSaveable { mutableStateOf(false) }

    if (pipelineStage !is PipelineStage.Idle) {
        BackHandler {
            viewModel.resetToViewfinder()
        }
    }

    if (showLanguageSheet) {
        LanguagePreferenceBottomSheet(
            selectedLanguage = selectedLanguage,
            onSelectLanguage = { language ->
                viewModel.selectPreferredLanguage(context, language)
                showLanguageSheet = false
            },
            onDismiss = { showLanguageSheet = false }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            VistaLensTopBar(
                selectedLanguage = selectedLanguage,
                savedCount = savedScans.size,
                isShowingResult = pipelineStage !is PipelineStage.Idle,
                onHomeClick = { viewModel.resetToViewfinder() },
                onLanguageClick = { showLanguageSheet = true }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp)
                    .testTag("main_scroll_container"),
                contentPadding = PaddingValues(bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Quick Persisted Language Selector Bar
                item {
                    PreferredLanguageBar(
                        selectedLanguage = selectedLanguage,
                        onSelectLanguage = { lang ->
                            viewModel.selectPreferredLanguage(context, lang)
                        }
                    )
                }

                when (val stage = pipelineStage) {
                    PipelineStage.Idle -> {
                        item {
                            CameraViewfinderSection(
                                onBitmapCaptured = { bitmap ->
                                    viewModel.analyzeCapturedBitmap(context, bitmap)
                                },
                                onUriSelected = { uri ->
                                    viewModel.analyzeUriPhoto(context, uri)
                                },
                                onPresetSelected = { preset ->
                                    viewModel.analyzePresetLandmark(context, preset)
                                }
                            )
                        }

                        if (savedScans.isNotEmpty()) {
                            item {
                                SavedScansPassportSection(
                                    savedScans = savedScans,
                                    onSelectScan = { scan ->
                                        viewModel.loadSavedScan(context, scan)
                                    }
                                )
                            }
                        }
                    }

                    is PipelineStage.AnalyzingImage -> {
                        item {
                            PipelineLoadingOverlayCard(
                                bitmap = stage.bitmap,
                                currentStepIndex = 1,
                                partialRecognition = null
                            )
                        }
                    }

                    is PipelineStage.SearchingHistory -> {
                        item {
                            PipelineLoadingOverlayCard(
                                bitmap = stage.bitmap,
                                currentStepIndex = 2,
                                partialRecognition = stage.recognition
                            )
                        }
                    }

                    is PipelineStage.SynthesizingAudio -> {
                        item {
                            PipelineLoadingOverlayCard(
                                bitmap = stage.bitmap,
                                currentStepIndex = 3,
                                partialRecognition = stage.recognition
                            )
                        }
                    }

                    is PipelineStage.Ready -> {
                        item {
                            ArNarratedClipStage(
                                scan = stage.scan,
                                playbackState = playbackState,
                                selectedHotspotId = selectedHotspotId,
                                selectedVoice = selectedVoice,
                                isSynthesizingVoice = isSynthesizingVoice,
                                onSelectHotspot = { viewModel.selectHotspot(it) },
                                onTogglePlayPause = { viewModel.toggleAudioPlayPause() },
                                onSeekAudio = { viewModel.seekAudio(it) },
                                onSwitchVoice = { voice ->
                                    viewModel.switchNarratorVoice(context, voice)
                                },
                                onNewScanClick = { viewModel.resetToViewfinder() }
                            )
                        }

                        item {
                            GroundedHistoryDossierSection(
                                scan = stage.scan,
                                followUpState = followUpState,
                                onAskFollowUp = { question ->
                                    viewModel.askFollowUpQuestion(question)
                                }
                            )
                        }
                    }

                    is PipelineStage.Error -> {
                        item {
                            PipelineErrorCard(
                                message = stage.message,
                                onRetry = { viewModel.retryCurrentPhoto(context) },
                                onBackToCamera = { viewModel.resetToViewfinder() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VistaLensTopBar(
    selectedLanguage: TourLanguage,
    savedCount: Int,
    isShowingResult: Boolean,
    onHomeClick: () -> Unit,
    onLanguageClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .clickable(enabled = isShowingResult, onClick = onHomeClick)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraEnhance,
                    contentDescription = "VistaLens Logo",
                    tint = Color(0xFF09101A),
                    modifier = Modifier.size(22.dp)
                )
            }
            Column {
                Text(
                    text = "VistaLens",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "AR Landmark Recognition & Narrated History",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Persisted Language Selector Trigger
            Surface(
                onClick = onLanguageClick,
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.secondaryContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)),
                modifier = Modifier.testTag("language_selector_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = "Preferred Language",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "${selectedLanguage.shortBadge} • ${selectedLanguage.nativeName}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            if (savedCount > 0) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Explore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "$savedCount",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PreferredLanguageBar(
    selectedLanguage: TourLanguage,
    onSelectLanguage: (TourLanguage) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TOUR & NARRATION LANGUAGE (SAVED IN DATASTORE)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = selectedLanguage.bcp47Code,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(end = 8.dp),
            modifier = Modifier.testTag("language_chips_row")
        ) {
            items(TourLanguage.entries, key = { it.bcp47Code }) { language ->
                val isSelected = language == selectedLanguage
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectLanguage(language) },
                    label = {
                        Text(
                            text = "${language.shortBadge} • ${language.nativeName}",
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("language_chip_${language.shortBadge.lowercase()}")
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguagePreferenceBottomSheet(
    selectedLanguage: TourLanguage,
    onSelectLanguage: (TourLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Preferred History & Narration Language",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Persisted automatically on-device using Jetpack DataStore. Applied to Google Search historical facts and Gemini TTS voice synthesis.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))

            TourLanguage.entries.forEach { language ->
                val isSelected = language == selectedLanguage
                Surface(
                    onClick = { onSelectLanguage(language) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    },
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sheet_language_${language.shortBadge.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = language.shortBadge,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = language.nativeName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${language.englishName} (${language.bcp47Code})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedScansPassportSection(
    savedScans: List<CompleteLandmarkScan>,
    onSelectScan: (CompleteLandmarkScan) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "RECENT LANDMARK DISCOVERIES",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(end = 8.dp)
        ) {
            items(savedScans, key = { it.id }) { scan ->
                Card(
                    modifier = Modifier
                        .width(230.dp)
                        .height(110.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onSelectScan(scan) }
                        .testTag("saved_scan_${scan.id}"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        Image(
                            bitmap = remember(scan.bitmap) { scan.bitmap.asImageBitmap() },
                            contentDescription = scan.recognition.landmarkName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .width(84.dp)
                                .fillMaxSize()
                        )
                        Column(
                            modifier = Modifier
                                .padding(10.dp)
                                .weight(1f),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = scan.recognition.landmarkName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = scan.recognition.cityAndCountry,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "${scan.language.shortBadge} • ${scan.narratorVoice.displayName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
