package com.example.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.model.PRESET_CITY_LANDMARKS
import com.example.model.PresetLandmark

private const val TAG = "CameraViewfinder"

@Composable
fun CameraViewfinderSection(
    onBitmapCaptured: (Bitmap) -> Unit,
    onUriSelected: (android.net.Uri) -> Unit,
    onPresetSelected: (PresetLandmark) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onUriSelected(uri)
        }
    }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // AR Viewfinder Viewport Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .testTag("ar_viewfinder_card"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (hasCameraPermission) {
                    LiveCameraPreview(
                        imageCapture = imageCapture,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Rich editorial backdrop when camera permission isn't active yet
                    Image(
                        painter = painterResource(id = R.drawable.img_landmark_colosseum),
                        contentDescription = "City landmark preview backdrop",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xCC09101A),
                                        Color(0x8809101A),
                                        Color(0xEB09101A)
                                    )
                                )
                            )
                    )
                }

                // Animated AR Reticle & Telemetry HUD Overlay
                ArHudReticleOverlay(
                    modifier = Modifier.fillMaxSize()
                )

                // Top AI Pipeline Telemetry Pill
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color(0xCC09101A),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = AppIcons.CenterFocusStrong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = if (hasCameraPermission) "AR OPTICS LIVE • READY TO SCAN" else "AR LANDMARK SCANNER",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }

                // Center callout if Camera permission not granted yet
                if (!hasCameraPermission) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Point at any City Monument",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Enable live AR camera, upload a travel photo, or tap a city landmark below to launch AI recognition, Google Search grounding & TTS narration.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.82f),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        FilledTonalButton(
                            onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                            modifier = Modifier.testTag("enable_camera_button"),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = AppIcons.CameraAlt,
                                contentDescription = "Enable Camera",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Enable Live AR Camera")
                        }
                    }
                }

                // Bottom Capture & Upload Action Bar inside the AR Viewport
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xE609101A))
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (hasCameraPermission) {
                                capturePhotoFromCameraX(
                                    context = context,
                                    imageCapture = imageCapture,
                                    onBitmapReady = onBitmapCaptured
                                )
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("snap_landmark_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = AppIcons.CameraAlt,
                            contentDescription = "Snap Landmark",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (hasCameraPermission) "Snap Landmark" else "Camera Scan",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    FilledTonalButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("upload_photo_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = AppIcons.PhotoLibrary,
                            contentDescription = "Upload Photo",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Upload Photo",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        // 3-Stage AI Architecture Status Strip
        ModelCapabilitiesStrip()

        // Instant City Field Scans (Pre-bundled high-res travel photos for 1-tap testing)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "INSTANT CITY FIELD SCANS",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "Tap any photo to analyze",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(end = 8.dp),
                modifier = Modifier.testTag("preset_landmarks_row")
            ) {
                items(PRESET_CITY_LANDMARKS, key = { it.id }) { preset ->
                    PresetLandmarkCard(
                        preset = preset,
                        onClick = { onPresetSelected(preset) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveCameraPreview(
    imageCapture: ImageCapture,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    DisposableEffect(lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val executor = ContextCompat.getMainExecutor(context)
        val listener = Runnable {
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val selector = CameraSelector.DEFAULT_BACK_CAMERA
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    selector,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                Log.e(TAG, "CameraX bind failed", e)
            }
        }
        cameraProviderFuture.addListener(listener, executor)
        onDispose {
            try {
                cameraProviderFuture.get().unbindAll()
            } catch (_: Exception) {
            }
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    )
}

@Composable
private fun ArHudReticleOverlay(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "ar_reticle")
    val scanLineProgress by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.82f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scan_line"
    )

    val cyanColor = MaterialTheme.colorScheme.secondary
    val amberColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val padX = w * 0.12f
        val padY = h * 0.16f
        val cornerLen = 28.dp.toPx()
        val strokeW = 2.5.dp.toPx()

        // Top-Left Bracket
        drawLine(cyanColor, Offset(padX, padY), Offset(padX + cornerLen, padY), strokeW, StrokeCap.Round)
        drawLine(cyanColor, Offset(padX, padY), Offset(padX, padY + cornerLen), strokeW, StrokeCap.Round)

        // Top-Right Bracket
        drawLine(cyanColor, Offset(w - padX, padY), Offset(w - padX - cornerLen, padY), strokeW, StrokeCap.Round)
        drawLine(cyanColor, Offset(w - padX, padY), Offset(w - padX, padY + cornerLen), strokeW, StrokeCap.Round)

        // Bottom-Left Bracket
        drawLine(cyanColor, Offset(padX, h - padY), Offset(padX + cornerLen, h - padY), strokeW, StrokeCap.Round)
        drawLine(cyanColor, Offset(padX, h - padY), Offset(padX, h - padY - cornerLen), strokeW, StrokeCap.Round)

        // Bottom-Right Bracket
        drawLine(cyanColor, Offset(w - padX, h - padY), Offset(w - padX - cornerLen, h - padY), strokeW, StrokeCap.Round)
        drawLine(cyanColor, Offset(w - padX, h - padY), Offset(w - padX, h - padY - cornerLen), strokeW, StrokeCap.Round)

        // Subtle Center Reticle Ring
        drawCircle(
            color = amberColor.copy(alpha = 0.45f),
            radius = 22.dp.toPx(),
            center = Offset(w / 2f, h / 2f),
            style = Stroke(width = 1.5.dp.toPx())
        )
        drawCircle(
            color = amberColor,
            radius = 3.dp.toPx(),
            center = Offset(w / 2f, h / 2f)
        )

        // Animated Horizontal LIDAR Scanline
        val scanY = h * scanLineProgress
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    cyanColor.copy(alpha = 0.75f),
                    amberColor.copy(alpha = 0.9f),
                    cyanColor.copy(alpha = 0.75f),
                    Color.Transparent
                ),
                startX = padX,
                endX = w - padX
            ),
            start = Offset(padX, scanY),
            end = Offset(w - padX, scanY),
            strokeWidth = 2.dp.toPx()
        )
    }
}

@Composable
private fun ModelCapabilitiesStrip() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CapabilityBadge(
                icon = AppIcons.AutoAwesome,
                title = "VISION",
                subtitle = "gemini-4-argon",
                accent = MaterialTheme.colorScheme.primary
            )
            CapabilityBadge(
                icon = AppIcons.Public,
                title = "SEARCH",
                subtitle = "gemini-3.8-flash",
                accent = MaterialTheme.colorScheme.secondary
            )
            CapabilityBadge(
                icon = AppIcons.RecordVoiceOver,
                title = "NARRATOR",
                subtitle = "gemini-3.8-flash-tts",
                accent = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@Composable
private fun CapabilityBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    accent: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(15.dp)
            )
        }
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PresetLandmarkCard(
    preset: PresetLandmark,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(205.dp)
            .height(148.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("preset_card_${preset.id}"),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = preset.drawableRes),
                contentDescription = "${preset.title} in ${preset.city}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0x3309101A),
                                Color(0xCC09101A)
                            )
                        )
                    )
            )
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp),
                color = Color(0xBF09101A),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
            ) {
                Text(
                    text = preset.badge,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Text(
                    text = preset.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = preset.city,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1
                )
            }
        }
    }
}

private fun capturePhotoFromCameraX(
    context: Context,
    imageCapture: ImageCapture,
    onBitmapReady: (Bitmap) -> Unit
) {
    val executor = ContextCompat.getMainExecutor(context)
    imageCapture.takePicture(
        executor,
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                try {
                    val rawBitmap = image.toBitmap()
                    val rotation = image.imageInfo.rotationDegrees
                    val rotatedBitmap = if (rotation != 0) {
                        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                        Bitmap.createBitmap(
                            rawBitmap,
                            0,
                            0,
                            rawBitmap.width,
                            rawBitmap.height,
                            matrix,
                            true
                        )
                    } else {
                        rawBitmap
                    }
                    onBitmapReady(rotatedBitmap)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed converting captured ImageProxy to Bitmap", e)
                } finally {
                    image.close()
                }
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e(TAG, "CameraX capture error", exception)
            }
        }
    )
}
