package com.example.ui

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.audio.AudioPlaybackState
import com.example.model.ArHotspot
import com.example.model.CompleteLandmarkScan
import com.example.model.LandmarkRecognition
import com.example.model.NarratorVoice
import kotlin.math.roundToInt

@Composable
fun PipelineLoadingOverlayCard(
    bitmap: Bitmap,
    currentStepIndex: Int, // 1 = Vision, 2 = Search Grounding, 3 = TTS Narration
    partialRecognition: LandmarkRecognition?,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pipeline_scan")
    val scanY by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scan_y"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .testTag("pipeline_loading_card"),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.65f))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    bitmap = remember(bitmap) { bitmap.asImageBitmap() },
                    contentDescription = "Captured landmark photo being analyzed",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x9909101A))
                )

                val cyan = MaterialTheme.colorScheme.secondary
                val amber = MaterialTheme.colorScheme.primary

                // LIDAR Grid + Scanning Beam
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stepX = size.width / 8f
                    val stepY = size.height / 6f
                    for (i in 1..7) {
                        drawLine(
                            color = cyan.copy(alpha = 0.14f),
                            start = Offset(i * stepX, 0f),
                            end = Offset(i * stepX, size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    for (j in 1..5) {
                        drawLine(
                            color = cyan.copy(alpha = 0.14f),
                            start = Offset(0f, j * stepY),
                            end = Offset(size.width, j * stepY),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    val beamY = size.height * scanY
                    drawLine(
                        brush = Brush.horizontalGradient(
                            listOf(Color.Transparent, cyan, amber, cyan, Color.Transparent)
                        ),
                        start = Offset(0f, beamY),
                        end = Offset(size.width, beamY),
                        strokeWidth = 3.dp.toPx()
                    )

                    // If recognition hotspots already arrived in step 2/3, plot them live!
                    partialRecognition?.arHotspots?.forEach { spot ->
                        val px = size.width * spot.normX
                        val py = size.height * spot.normY
                        drawCircle(
                            color = amber.copy(alpha = 0.35f),
                            radius = 18.dp.toPx(),
                            center = Offset(px, py)
                        )
                        drawCircle(
                            color = amber,
                            radius = 6.dp.toPx(),
                            center = Offset(px, py)
                        )
                    }
                }

                // Recognized Landmark Banner if available during Step 2 & 3
                if (partialRecognition != null) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xD909101A),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                            Text(
                                text = "RECOGNIZED • ${partialRecognition.confidencePercent}% MATCH",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = partialRecognition.landmarkName,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                            Text(
                                text = "${partialRecognition.cityAndCountry} • ${partialRecognition.eraOrYearBuilt}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                // Bottom Step Progress Telemetry Card
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(14.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xE6101B2B),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PipelineStepRow(
                            stepNumber = 1,
                            currentStep = currentStepIndex,
                            title = "Analyzing Architecture & Spatial Pins",
                            modelBadge = "gemini-4-argon"
                        )
                        PipelineStepRow(
                            stepNumber = 2,
                            currentStep = currentStepIndex,
                            title = "Grounding History via Google Search",
                            modelBadge = "gemini-3.8-flash + googleSearch"
                        )
                        PipelineStepRow(
                            stepNumber = 3,
                            currentStep = currentStepIndex,
                            title = "Synthesizing AR Narrated Audio Clip",
                            modelBadge = "gemini-3.8-flash-tts"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PipelineStepRow(
    stepNumber: Int,
    currentStep: Int,
    title: String,
    modelBadge: String
) {
    val isDone = currentStep > stepNumber
    val isActive = currentStep == stepNumber

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            when {
                isDone -> Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Completed",
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(18.dp)
                )
                isActive -> CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                else -> Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = if (isActive || isDone) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Surface(
            color = if (isActive) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = modelBadge,
                style = MaterialTheme.typography.labelSmall,
                color = if (isActive) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ArNarratedClipStage(
    scan: CompleteLandmarkScan,
    playbackState: AudioPlaybackState,
    selectedHotspotId: String?,
    selectedVoice: NarratorVoice,
    isSynthesizingVoice: Boolean,
    onSelectHotspot: (String?) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekAudio: (Float) -> Unit,
    onSwitchVoice: (NarratorVoice) -> Unit,
    onNewScanClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ar_clip_motion")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_radius"
    )

    val kenBurnsScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (playbackState.isPlaying) 1.09f else 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ken_burns_scale"
    )

    val hotspots = scan.recognition.arHotspots
    val activeHotspot = remember(selectedHotspotId, hotspots, playbackState.progress, playbackState.isPlaying) {
        val explicit = hotspots.find { it.id == selectedHotspotId }
        if (explicit != null) {
            explicit
        } else if (hotspots.isNotEmpty()) {
            val idx = ((playbackState.progress * hotspots.size).toInt()).coerceIn(0, hotspots.lastIndex)
            hotspots[idx]
        } else {
            null
        }
    }

    val sentences = scan.dossier.narrationSentences
    val activeSentenceIndex = remember(playbackState.progress, sentences.size) {
        if (sentences.isEmpty()) 0
        else ((playbackState.progress * sentences.size).toInt()).coerceIn(0, sentences.lastIndex)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. AR-STYLE NARRATED CLIP VIEWPORT
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(390.dp)
                .testTag("ar_narrated_clip_viewport"),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.65f))
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val widthPx = constraints.maxWidth.toFloat()
                val heightPx = constraints.maxHeight.toFloat()

                // Landmark Photo with subtle Ken Burns motion during narration
                Image(
                    bitmap = remember(scan.bitmap) { scan.bitmap.asImageBitmap() },
                    contentDescription = scan.recognition.landmarkName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = kenBurnsScale
                            scaleY = kenBurnsScale
                        }
                )

                // Cinematic Top & Bottom Vignettes for AR HUD legibility
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color(0xCC09101A),
                                    0.28f to Color(0x2209101A),
                                    0.65f to Color(0x4409101A),
                                    1.0f to Color(0xEE09101A)
                                )
                            )
                        )
                )

                val amberColor = MaterialTheme.colorScheme.primary
                val cyanColor = MaterialTheme.colorScheme.secondary

                // AR Spatial Connector Lines & Corner HUD Reticle
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val corner = 24.dp.toPx()
                    val pad = 14.dp.toPx()
                    val stroke = 2.dp.toPx()

                    // Corner HUD brackets
                    drawLine(cyanColor.copy(alpha = 0.7f), Offset(pad, pad), Offset(pad + corner, pad), stroke)
                    drawLine(cyanColor.copy(alpha = 0.7f), Offset(pad, pad), Offset(pad, pad + corner), stroke)
                    drawLine(cyanColor.copy(alpha = 0.7f), Offset(size.width - pad, pad), Offset(size.width - pad - corner, pad), stroke)
                    drawLine(cyanColor.copy(alpha = 0.7f), Offset(size.width - pad, pad), Offset(size.width - pad, pad + corner), stroke)

                    // Dashed vector line from active hotspot to its callout card
                    if (activeHotspot != null) {
                        val spotX = size.width * activeHotspot.normX
                        val spotY = size.height * activeHotspot.normY
                        drawCircle(
                            color = amberColor.copy(alpha = 0.28f),
                            radius = 18.dp.toPx() * pulseRadius,
                            center = Offset(spotX, spotY),
                            style = Stroke(width = 2.dp.toPx())
                        )
                        drawLine(
                            color = amberColor.copy(alpha = 0.85f),
                            start = Offset(spotX, spotY),
                            end = Offset(size.width * 0.5f, size.height * 0.64f),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                        )
                    }
                }

                // Interactive AR Spatial Pins placed on normalized landmark coordinates
                val halfPinPx = with(LocalDensity.current) { 24.dp.roundToPx() }
                hotspots.forEachIndexed { idx, spot ->
                    val isSelected = activeHotspot?.id == spot.id
                    val offsetX = (widthPx * spot.normX).roundToInt() - halfPinPx
                    val offsetY = (heightPx * spot.normY).roundToInt() - halfPinPx

                    Box(
                        modifier = Modifier
                            .offset { IntOffset(offsetX, offsetY) }
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable { onSelectHotspot(spot.id) }
                            .testTag("ar_hotspot_pin_$idx"),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                Color(0xCC09101A)
                            },
                            border = BorderStroke(
                                width = 2.dp,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.secondary
                            ),
                            modifier = Modifier.size(if (isSelected) 34.dp else 28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${idx + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.secondary
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Top Bar inside AR Viewport: Landmark Header + Confidence + New Scan button
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = AppIcons.Verified,
                                contentDescription = "Verified Recognition",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${scan.recognition.confidencePercent}% AI MATCH • ${scan.recognition.eraOrYearBuilt.uppercase()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = scan.recognition.landmarkName,
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "${scan.recognition.cityAndCountry} • ${scan.recognition.architecturalStyle}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Surface(
                        onClick = onNewScanClick,
                        shape = RoundedCornerShape(50),
                        color = Color(0xCC101B2B),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.testTag("new_scan_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "New Scan",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "New Scan",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White
                            )
                        }
                    }
                }

                // Bottom Overlay inside AR Viewport: Active Hotspot Callout + Synchronized Teleprompter Subtitles
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Floating Glassmorphic AR Feature Callout Card
                    AnimatedVisibility(
                        visible = activeHotspot != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        if (activeHotspot != null) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xD9101B2B),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "AR PIN • ${activeHotspot.tag.uppercase()}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Tap pins on photo to inspect",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = activeHotspot.label,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = activeHotspot.detail,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.88f),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // Synchronized AR Teleprompter Caption Box
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xE609101A),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ar_teleprompter_box")
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = AppIcons.GraphicEq,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (playbackState.isPlaying) {
                                        "LIVE AR NARRATION • ${scan.narratorVoice.displayName.uppercase()} (${scan.language.shortBadge})"
                                    } else {
                                        "AR NARRATION SCRIPT • ${scan.narratorVoice.displayName.uppercase()} (${scan.language.nativeName})"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            val annotatedScript = buildAnnotatedString {
                                sentences.forEachIndexed { index, sentence ->
                                    val isCurrent = index == activeSentenceIndex
                                    withStyle(
                                        SpanStyle(
                                            color = if (isCurrent) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                Color.White.copy(alpha = 0.72f)
                                            },
                                            fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    ) {
                                        append(sentence)
                                        if (index < sentences.lastIndex) append(" ")
                                    }
                                }
                            }
                            Text(
                                text = annotatedScript,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // 2. AUDIO WAVEFORM & GEMINI-3.8-FLASH-TTS PLAYBACK CONTROLLER
        AudioNarrationDeck(
            scan = scan,
            playbackState = playbackState,
            selectedVoice = selectedVoice,
            isSynthesizingVoice = isSynthesizingVoice,
            onTogglePlayPause = onTogglePlayPause,
            onSeekAudio = onSeekAudio,
            onSwitchVoice = onSwitchVoice
        )

        // 3. INTERACTIVE AR HOTSPOT CHIPS ROW
        if (hotspots.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                hotspots.forEachIndexed { idx, spot ->
                    val selected = activeHotspot?.id == spot.id
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectHotspot(spot.id) },
                        label = {
                            Text(
                                text = "${idx + 1}. ${spot.label}",
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioNarrationDeck(
    scan: CompleteLandmarkScan,
    playbackState: AudioPlaybackState,
    selectedVoice: NarratorVoice,
    isSynthesizingVoice: Boolean,
    onTogglePlayPause: () -> Unit,
    onSeekAudio: (Float) -> Unit,
    onSwitchVoice: (NarratorVoice) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("audio_narration_deck"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Play / Pause Circular Button (min 48dp touch target)
                Surface(
                    onClick = onTogglePlayPause,
                    enabled = !isSynthesizingVoice && scan.audioBytes != null,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("play_pause_narration_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isSynthesizingVoice) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.5.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(
                                imageVector = if (playbackState.isPlaying) {
                                    AppIcons.Pause
                                } else {
                                    Icons.Default.PlayArrow
                                },
                                contentDescription = if (playbackState.isPlaying) {
                                    "Pause Narration"
                                } else {
                                    "Play Narration"
                                },
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                // Interactive 36-Bar Waveform Scrubber
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val bars = scan.waveform.ifEmpty { List(36) { 0.35f } }
                    val activeColor = MaterialTheme.colorScheme.primary
                    val inactiveColor = MaterialTheme.colorScheme.outline
                    val progress = playbackState.progress

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    if (size.width > 0) {
                                        val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                                        onSeekAudio(fraction)
                                    }
                                }
                            }
                    ) {
                        val barCount = bars.size
                        val spacing = 3.dp.toPx()
                        val totalSpacing = spacing * (barCount - 1)
                        val barWidth = ((size.width - totalSpacing) / barCount).coerceAtLeast(2f)

                        bars.forEachIndexed { index, amplitude ->
                            val barFraction = index.toFloat() / barCount.toFloat()
                            val barHeight = (size.height * amplitude.coerceIn(0.18f, 1f))
                            val topY = (size.height - barHeight) / 2f
                            val startX = index * (barWidth + spacing)

                            drawRoundRect(
                                color = if (barFraction <= progress) activeColor else inactiveColor,
                                topLeft = Offset(startX, topY),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatMillis(playbackState.currentPositionMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "gemini-3.8-flash-tts • ${selectedVoice.personaSubtitle}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = formatMillis(playbackState.durationMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Replay from start button
                IconButton(
                    onClick = { onSeekAudio(0f) },
                    enabled = !isSynthesizingVoice,
                    modifier = Modifier.testTag("replay_narration_button")
                ) {
                    Icon(
                        imageVector = AppIcons.Replay,
                        contentDescription = "Replay Narration",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // Narrator Voice Selector Pills (synthesizes with gemini-3.8-flash-tts)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 4.dp)
            ) {
                items(NarratorVoice.entries) { voice ->
                    val isSelected = voice == selectedVoice
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (!isSynthesizingVoice && voice != selectedVoice) {
                                onSwitchVoice(voice)
                            }
                        },
                        label = {
                            Text(
                                text = "${voice.displayName} (${voice.personaSubtitle.substringBefore(" ")})",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.secondary
                        ),
                        modifier = Modifier.testTag("voice_chip_${voice.voiceId.lowercase()}")
                    )
                }
            }
        }
    }
}

@Composable
fun PipelineErrorCard(
    message: String,
    onRetry: () -> Unit,
    onBackToCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("pipeline_error_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "AI Landmark Pipeline Notice",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onRetry,
                    modifier = Modifier.testTag("retry_pipeline_button")
                ) {
                    Text("Retry Analysis")
                }
                OutlinedButton(
                    onClick = onBackToCamera,
                    modifier = Modifier.testTag("back_to_camera_button")
                ) {
                    Text("Back to Scanner")
                }
            }
        }
    }
}

private fun formatMillis(ms: Int): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%02d:%02d".format(min, sec)
}
