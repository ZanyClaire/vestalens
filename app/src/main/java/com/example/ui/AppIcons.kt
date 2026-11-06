package com.example.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object AppIcons {
    val CameraAlt: ImageVector by lazy {
        singlePathIcon("CameraAlt") {
            moveTo(12f, 15.2f)
            arcToRelative(3.2f, 3.2f, 0f, true, false, 0f, -6.4f)
            arcToRelative(3.2f, 3.2f, 0f, false, false, 0f, 6.4f)
            close()
            moveTo(9f, 2f)
            lineTo(7.17f, 4f)
            horizontalLineTo(4f)
            curveTo(2.9f, 4f, 2f, 4.9f, 2f, 6f)
            verticalLineToRelative(12f)
            curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
            horizontalLineToRelative(16f)
            curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
            verticalLineTo(6f)
            curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
            horizontalLineToRelative(-3.17f)
            lineTo(15f, 2f)
            horizontalLineTo(9f)
            close()
            moveTo(12f, 17f)
            curveToRelative(-2.76f, 0f, -5f, -2.24f, -5f, -5f)
            reflectiveCurveToRelative(2.24f, -5f, 5f, -5f)
            reflectiveCurveToRelative(5f, 2.24f, 5f, 5f)
            reflectiveCurveToRelative(-2.24f, 5f, -5f, 5f)
            close()
        }
    }

    val PhotoLibrary: ImageVector by lazy {
        singlePathIcon("PhotoLibrary") {
            moveTo(22f, 16f)
            verticalLineTo(4f)
            curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
            horizontalLineTo(8f)
            curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
            verticalLineToRelative(12f)
            curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
            horizontalLineToRelative(12f)
            curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
            close()
            moveTo(11f, 12f)
            lineToRelative(2.03f, 2.71f)
            lineTo(16f, 11f)
            lineToRelative(4f, 5f)
            horizontalLineTo(8f)
            lineToRelative(3f, -4f)
            close()
            moveTo(2f, 6f)
            verticalLineToRelative(14f)
            curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
            horizontalLineToRelative(14f)
            verticalLineToRelative(-2f)
            horizontalLineTo(4f)
            verticalLineTo(6f)
            horizontalLineTo(2f)
            close()
        }
    }

    val Public: ImageVector by lazy {
        singlePathIcon("Public") {
            moveTo(12f, 2f)
            curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
            reflectiveCurveToRelative(4.48f, 10f, 10f, 10f)
            reflectiveCurveToRelative(10f, -4.48f, 10f, -10f)
            reflectiveCurveTo(17.52f, 2f, 12f, 2f)
            close()
            moveTo(11f, 19.93f)
            curveToRelative(-3.95f, -0.49f, -7f, -3.85f, -7f, -7.93f)
            curveToRelative(0f, -0.62f, 0.08f, -1.21f, 0.21f, -1.79f)
            lineTo(9f, 15f)
            verticalLineToRelative(1f)
            curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
            verticalLineToRelative(1.93f)
            close()
            moveTo(17.9f, 17.39f)
            curveToRelative(-0.26f, -0.81f, -1f, -1.39f, -1.9f, -1.39f)
            horizontalLineToRelative(-1f)
            verticalLineToRelative(-3f)
            curveToRelative(0f, -0.55f, -0.45f, -1f, -1f, -1f)
            horizontalLineTo(8f)
            verticalLineToRelative(-2f)
            horizontalLineToRelative(2f)
            curveToRelative(0.55f, 0f, 1f, -0.45f, 1f, -1f)
            verticalLineTo(7f)
            horizontalLineToRelative(2f)
            curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
            verticalLineToRelative(-0.41f)
            curveToRelative(2.93f, 1.19f, 5f, 4.06f, 5f, 7.41f)
            curveToRelative(0f, 2.08f, -0.8f, 3.97f, -2.1f, 5.39f)
            close()
        }
    }

    val RecordVoiceOver: ImageVector by lazy {
        singlePathIcon("RecordVoiceOver") {
            moveTo(9f, 9f)
            moveToRelative(-4f, 0f)
            arcToRelative(4f, 4f, 0f, true, true, 8f, 0f)
            arcToRelative(4f, 4f, 0f, true, true, -8f, 0f)
            close()
            moveTo(9f, 15f)
            curveToRelative(-2.67f, 0f, -8f, 1.34f, -8f, 4f)
            verticalLineToRelative(2f)
            horizontalLineToRelative(16f)
            verticalLineToRelative(-2f)
            curveToRelative(0f, -2.66f, -5.33f, -4f, -8f, -4f)
            close()
            moveTo(16.76f, 5.36f)
            lineToRelative(-1.68f, 1.69f)
            curveToRelative(0.84f, 1.18f, 0.84f, 2.71f, 0f, 3.89f)
            lineToRelative(1.68f, 1.69f)
            curveToRelative(2.02f, -2.02f, 2.02f, -5.07f, 0f, -7.27f)
            close()
        }
    }

    val CenterFocusStrong: ImageVector by lazy {
        singlePathIcon("CenterFocusStrong") {
            moveTo(12f, 8f)
            curveToRelative(-2.21f, 0f, -4f, 1.79f, -4f, 4f)
            reflectiveCurveToRelative(1.79f, 4f, 4f, 4f)
            reflectiveCurveToRelative(4f, -1.79f, 4f, -4f)
            reflectiveCurveToRelative(-1.79f, -4f, -4f, -4f)
            close()
            moveTo(5f, 15f)
            horizontalLineTo(3f)
            verticalLineToRelative(4f)
            curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
            horizontalLineToRelative(4f)
            verticalLineToRelative(-2f)
            horizontalLineTo(5f)
            verticalLineToRelative(-4f)
            close()
            moveTo(5f, 5f)
            horizontalLineToRelative(4f)
            verticalLineTo(3f)
            horizontalLineTo(5f)
            curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
            verticalLineToRelative(4f)
            horizontalLineToRelative(2f)
            verticalLineTo(5f)
            close()
            moveTo(19f, 3f)
            horizontalLineToRelative(-4f)
            verticalLineToRelative(2f)
            horizontalLineToRelative(4f)
            verticalLineToRelative(4f)
            horizontalLineToRelative(2f)
            verticalLineTo(5f)
            curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
            close()
            moveTo(19f, 19f)
            horizontalLineToRelative(-4f)
            verticalLineToRelative(2f)
            horizontalLineToRelative(4f)
            curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
            verticalLineToRelative(-4f)
            horizontalLineToRelative(-2f)
            verticalLineToRelative(4f)
            close()
        }
    }

    val AutoAwesome: ImageVector by lazy {
        singlePathIcon("AutoAwesome") {
            moveTo(19f, 9f)
            lineToRelative(1.25f, -2.75f)
            lineTo(23f, 5f)
            lineToRelative(-2.75f, -1.25f)
            lineTo(19f, 1f)
            lineToRelative(-1.25f, 2.75f)
            lineTo(15f, 5f)
            lineToRelative(2.75f, 1.25f)
            lineTo(19f, 9f)
            close()
            moveTo(11.5f, 9.5f)
            lineTo(9f, 4f)
            lineTo(6.5f, 9.5f)
            lineTo(1f, 12f)
            lineToRelative(5.5f, 2.5f)
            lineTo(9f, 20f)
            lineToRelative(2.5f, -5.5f)
            lineTo(17f, 12f)
            lineToRelative(-5.5f, -2.5f)
            close()
        }
    }

    val GraphicEq: ImageVector by lazy {
        singlePathIcon("GraphicEq") {
            moveTo(7f, 18f)
            horizontalLineToRelative(2f)
            verticalLineTo(6f)
            horizontalLineTo(7f)
            verticalLineToRelative(12f)
            close()
            moveTo(11f, 22f)
            horizontalLineToRelative(2f)
            verticalLineTo(2f)
            horizontalLineToRelative(-2f)
            verticalLineToRelative(20f)
            close()
            moveTo(3f, 14f)
            horizontalLineToRelative(2f)
            verticalLineToRelative(-4f)
            horizontalLineTo(3f)
            verticalLineToRelative(4f)
            close()
            moveTo(15f, 18f)
            horizontalLineToRelative(2f)
            verticalLineTo(6f)
            horizontalLineToRelative(-2f)
            verticalLineToRelative(12f)
            close()
            moveTo(19f, 10f)
            verticalLineToRelative(4f)
            horizontalLineToRelative(2f)
            verticalLineToRelative(-4f)
            horizontalLineToRelative(-2f)
            close()
        }
    }

    val Pause: ImageVector by lazy {
        singlePathIcon("Pause") {
            moveTo(6f, 19f)
            horizontalLineToRelative(4f)
            verticalLineTo(5f)
            horizontalLineTo(6f)
            verticalLineToRelative(14f)
            close()
            moveTo(14f, 5f)
            verticalLineToRelative(14f)
            horizontalLineToRelative(4f)
            verticalLineTo(5f)
            horizontalLineToRelative(-4f)
            close()
        }
    }

    val Replay: ImageVector by lazy {
        singlePathIcon("Replay") {
            moveTo(12f, 5f)
            verticalLineTo(1f)
            lineTo(7f, 6f)
            lineToRelative(5f, 5f)
            verticalLineTo(7f)
            curveToRelative(3.31f, 0f, 6f, 2.69f, 6f, 6f)
            reflectiveCurveToRelative(-2.69f, 6f, -6f, 6f)
            reflectiveCurveToRelative(-6f, -2.69f, -6f, -6f)
            horizontalLineTo(4f)
            curveToRelative(0f, 4.42f, 3.58f, 8f, 8f, 8f)
            reflectiveCurveToRelative(8f, -3.58f, 8f, -8f)
            reflectiveCurveToRelative(-3.58f, -8f, -8f, -8f)
            close()
        }
    }

    val Verified: ImageVector by lazy {
        singlePathIcon("Verified") {
            moveTo(23f, 12f)
            lineToRelative(-2.44f, -2.79f)
            lineToRelative(0.34f, -3.69f)
            lineToRelative(-3.61f, -0.82f)
            lineToRelative(-1.89f, -3.2f)
            lineTo(12f, 2.96f)
            lineTo(8.6f, 1.5f)
            lineTo(6.71f, 4.69f)
            lineTo(3.1f, 5.5f)
            lineTo(3.44f, 9.2f)
            lineTo(1f, 12f)
            lineToRelative(2.44f, 2.79f)
            lineToRelative(-0.34f, 3.7f)
            lineToRelative(3.61f, 0.82f)
            lineToRelative(1.89f, 3.2f)
            lineTo(12f, 21.04f)
            lineToRelative(3.4f, 1.46f)
            lineToRelative(1.89f, -3.2f)
            lineToRelative(3.61f, -0.82f)
            lineToRelative(-0.34f, -3.69f)
            lineTo(23f, 12f)
            close()
            moveTo(10.09f, 16.72f)
            lineToRelative(-3.8f, -3.81f)
            lineToRelative(1.48f, -1.48f)
            lineToRelative(2.32f, 2.33f)
            lineToRelative(5.85f, -5.87f)
            lineToRelative(1.48f, 1.48f)
            lineToRelative(-7.33f, 7.35f)
            close()
        }
    }

    val HistoryEdu: ImageVector by lazy {
        singlePathIcon("HistoryEdu") {
            moveTo(9f, 4f)
            verticalLineToRelative(1.38f)
            curveToRelative(-0.83f, -0.33f, -1.72f, -0.5f, -2.61f, -0.5f)
            curveToRelative(-1.79f, 0f, -3.58f, 0.68f, -4.95f, 2.05f)
            lineToRelative(3.33f, 3.33f)
            horizontalLineToRelative(1.11f)
            verticalLineToRelative(1.11f)
            curveToRelative(0.86f, 0.86f, 1.98f, 1.31f, 3.11f, 1.36f)
            verticalLineTo(15f)
            horizontalLineTo(6f)
            verticalLineToRelative(3f)
            curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
            horizontalLineToRelative(10f)
            curveToRelative(1.66f, 0f, 3f, -1.34f, 3f, -3f)
            verticalLineTo(4f)
            horizontalLineTo(9f)
            close()
        }
    }

    val Lightbulb: ImageVector by lazy {
        singlePathIcon("Lightbulb") {
            moveTo(9f, 21f)
            curveToRelative(0f, 0.55f, 0.45f, 1f, 1f, 1f)
            horizontalLineToRelative(4f)
            curveToRelative(0.55f, 0f, 1f, -0.45f, 1f, -1f)
            verticalLineToRelative(-1f)
            horizontalLineTo(9f)
            verticalLineToRelative(1f)
            close()
            moveTo(12f, 2f)
            curveTo(8.14f, 2f, 5f, 5.14f, 5f, 9f)
            curveToRelative(0f, 2.38f, 1.19f, 4.47f, 3f, 5.74f)
            verticalLineTo(17f)
            curveToRelative(0f, 0.55f, 0.45f, 1f, 1f, 1f)
            horizontalLineToRelative(6f)
            curveToRelative(0.55f, 0f, 1f, -0.45f, 1f, -1f)
            verticalLineToRelative(-2.26f)
            curveToRelative(1.81f, -1.27f, 3f, -3.36f, 3f, -5.74f)
            curveToRelative(0f, -3.86f, -3.14f, -7f, -7f, -7f)
            close()
        }
    }

    val Timeline: ImageVector by lazy {
        singlePathIcon("Timeline") {
            moveTo(23f, 8f)
            curveToRelative(0f, 1.1f, -0.9f, 2f, -2f, 2f)
            curveToRelative(-0.18f, 0f, -0.35f, -0.02f, -0.51f, -0.07f)
            lineToRelative(-3.56f, 3.55f)
            curveToRelative(0.05f, 0.16f, 0.07f, 0.34f, 0.07f, 0.52f)
            curveToRelative(0f, 1.1f, -0.9f, 2f, -2f, 2f)
            reflectiveCurveToRelative(-2f, -0.9f, -2f, -2f)
            curveToRelative(0f, -0.18f, 0.02f, -0.36f, 0.07f, -0.52f)
            lineToRelative(-2.55f, -2.55f)
            curveToRelative(-0.16f, 0.05f, -0.34f, 0.07f, -0.52f, 0.07f)
            reflectiveCurveToRelative(-0.36f, -0.02f, -0.52f, -0.07f)
            lineToRelative(-4.55f, 4.56f)
            curveToRelative(0.05f, 0.16f, 0.07f, 0.33f, 0.07f, 0.51f)
            curveToRelative(0f, 1.1f, -0.9f, 2f, -2f, 2f)
            reflectiveCurveToRelative(-2f, -0.9f, -2f, -2f)
            reflectiveCurveToRelative(0.9f, -2f, 2f, -2f)
            curveToRelative(0.18f, 0f, 0.35f, 0.02f, 0.51f, 0.07f)
            lineToRelative(4.56f, -4.55f)
            curveTo(8.02f, 9.36f, 8f, 9.18f, 8f, 9f)
            curveToRelative(0f, -1.1f, 0.9f, -2f, 2f, -2f)
            reflectiveCurveToRelative(2f, 0.9f, 2f, 2f)
            curveToRelative(0f, 0.18f, -0.02f, 0.36f, -0.07f, 0.52f)
            lineToRelative(2.55f, 2.55f)
            curveToRelative(0.16f, -0.05f, 0.34f, -0.07f, 0.52f, -0.07f)
            reflectiveCurveToRelative(0.36f, 0.02f, 0.52f, 0.07f)
            lineToRelative(3.55f, -3.56f)
            curveTo(19.02f, 8.35f, 19f, 8.18f, 19f, 8f)
            curveToRelative(0f, -1.1f, 0.9f, -2f, 2f, -2f)
            reflectiveCurveToRelative(2f, 0.9f, 2f, 2f)
            close()
        }
    }

    val OpenInNew: ImageVector by lazy {
        singlePathIcon("OpenInNew") {
            moveTo(19f, 19f)
            horizontalLineTo(5f)
            verticalLineTo(5f)
            horizontalLineToRelative(7f)
            verticalLineTo(3f)
            horizontalLineTo(5f)
            curveToRelative(-1.11f, 0f, -2f, 0.9f, -2f, 2f)
            verticalLineToRelative(14f)
            curveToRelative(0f, 1.1f, 0.89f, 2f, 2f, 2f)
            horizontalLineToRelative(14f)
            curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
            verticalLineToRelative(-7f)
            horizontalLineToRelative(-2f)
            verticalLineToRelative(7f)
            close()
            moveTo(14f, 3f)
            verticalLineToRelative(2f)
            horizontalLineToRelative(3.59f)
            lineToRelative(-9.83f, 9.83f)
            lineToRelative(1.41f, 1.41f)
            lineTo(19f, 6.41f)
            verticalLineTo(10f)
            horizontalLineToRelative(2f)
            verticalLineTo(3f)
            horizontalLineToRelative(-7f)
            close()
        }
    }

    val Explore: ImageVector by lazy {
        singlePathIcon("Explore") {
            moveTo(12f, 10.9f)
            curveToRelative(-0.61f, 0f, -1.1f, 0.49f, -1.1f, 1.1f)
            reflectiveCurveToRelative(0.49f, 1.1f, 1.1f, 1.1f)
            curveToRelative(0.61f, 0f, 1.1f, -0.49f, 1.1f, -1.1f)
            reflectiveCurveToRelative(-0.49f, -1.1f, -1.1f, -1.1f)
            close()
            moveTo(12f, 2f)
            curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
            reflectiveCurveToRelative(4.48f, 10f, 10f, 10f)
            reflectiveCurveToRelative(10f, -4.48f, 10f, -10f)
            reflectiveCurveTo(17.52f, 2f, 12f, 2f)
            close()
            moveTo(14.19f, 14.19f)
            lineTo(6f, 18f)
            lineToRelative(3.81f, -8.19f)
            lineTo(18f, 6f)
            lineToRelative(-3.81f, 8.19f)
            close()
        }
    }

    val Translate: ImageVector by lazy {
        singlePathIcon("Translate") {
            moveTo(12.87f, 15.07f)
            lineToRelative(-2.54f, -2.51f)
            lineToRelative(0.03f, -0.03f)
            curveToRelative(1.74f, -1.94f, 2.98f, -4.17f, 3.71f, -6.53f)
            horizontalLineTo(17f)
            verticalLineTo(4f)
            horizontalLineToRelative(-7f)
            verticalLineTo(2f)
            horizontalLineTo(8f)
            verticalLineToRelative(2f)
            horizontalLineTo(1f)
            verticalLineToRelative(1.99f)
            horizontalLineToRelative(11.17f)
            curveTo(11.5f, 7.92f, 10.44f, 9.75f, 9f, 11.35f)
            curveTo(8.07f, 10.32f, 7.3f, 9.19f, 6.69f, 8f)
            horizontalLineToRelative(-2f)
            curveToRelative(0.73f, 1.63f, 1.73f, 3.17f, 2.98f, 4.56f)
            lineToRelative(-5.09f, 5.02f)
            lineTo(4f, 19f)
            lineToRelative(5f, -5f)
            lineToRelative(3.11f, 3.11f)
            lineToRelative(0.76f, -2.04f)
            close()
            moveTo(18.5f, 10f)
            horizontalLineToRelative(-2f)
            lineTo(12f, 22f)
            horizontalLineToRelative(2f)
            lineToRelative(1.12f, -3f)
            horizontalLineToRelative(4.75f)
            lineTo(21f, 22f)
            horizontalLineToRelative(2f)
            lineToRelative(-4.5f, -12f)
            close()
            moveTo(15.88f, 17f)
            lineToRelative(1.62f, -4.33f)
            lineTo(19.12f, 17f)
            horizontalLineToRelative(-3.24f)
            close()
        }
    }

    private fun singlePathIcon(
        name: String,
        block: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit
    ): ImageVector {
        return ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            fill = SolidColor(Color.Black),
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            pathBuilder = block
        ).build()
    }
}
