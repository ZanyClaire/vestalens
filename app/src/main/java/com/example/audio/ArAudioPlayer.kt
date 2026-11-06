package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val TAG = "ArAudioPlayer"

data class AudioPlaybackState(
    val isReady: Boolean = false,
    val isPlaying: Boolean = false,
    val progress: Float = 0f,
    val currentPositionMs: Int = 0,
    val durationMs: Int = 0
)

class ArAudioPlayer(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null
    private var currentTempFile: File? = null

    private val _playbackState = MutableStateFlow(AudioPlaybackState())
    val playbackState: StateFlow<AudioPlaybackState> = _playbackState.asStateFlow()

    fun loadAndPlayAudio(
        rawBytes: ByteArray,
        mimeType: String?,
        autoPlay: Boolean = true
    ) {
        stopAndRelease()
        if (rawBytes.isEmpty()) return

        try {
            val isAlreadyContainer = isEncodedContainer(rawBytes, mimeType)
            val playableBytes = if (isAlreadyContainer) {
                rawBytes
            } else {
                val sampleRate = parseSampleRate(mimeType)
                wrapPcm16MonoInWav(rawBytes, sampleRate)
            }

            val ext = when {
                mimeType?.contains("mp3", ignoreCase = true) == true ||
                    mimeType?.contains("mpeg", ignoreCase = true) == true -> ".mp3"
                mimeType?.contains("ogg", ignoreCase = true) == true -> ".ogg"
                else -> ".wav"
            }

            val tempFile = File(context.cacheDir, "vistalens_tts_${System.currentTimeMillis()}$ext")
            FileOutputStream(tempFile).use { fos ->
                fos.write(playableBytes)
                fos.flush()
            }
            currentTempFile = tempFile

            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(tempFile.absolutePath)
                setOnCompletionListener { mp ->
                    stopProgressTicker()
                    val total = max(mp.duration, 1)
                    _playbackState.value = _playbackState.value.copy(
                        isPlaying = false,
                        progress = 1f,
                        currentPositionMs = total,
                        durationMs = total
                    )
                }
                prepare()
            }

            mediaPlayer = player
            val duration = max(player.duration, 1)
            _playbackState.value = AudioPlaybackState(
                isReady = true,
                isPlaying = false,
                progress = 0f,
                currentPositionMs = 0,
                durationMs = duration
            )

            if (autoPlay) {
                play()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load or play synthesized TTS audio", e)
            _playbackState.value = AudioPlaybackState()
        }
    }

    fun togglePlayPause() {
        val player = mediaPlayer ?: return
        if (player.isPlaying) {
            pause()
        } else {
            if (_playbackState.value.progress >= 0.98f) {
                seekToFraction(0f)
            }
            play()
        }
    }

    fun play() {
        val player = mediaPlayer ?: return
        try {
            if (!player.isPlaying) {
                player.start()
                _playbackState.value = _playbackState.value.copy(isPlaying = true)
                startProgressTicker()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting playback", e)
        }
    }

    fun pause() {
        val player = mediaPlayer ?: return
        try {
            if (player.isPlaying) {
                player.pause()
            }
            stopProgressTicker()
            _playbackState.value = _playbackState.value.copy(
                isPlaying = false,
                currentPositionMs = player.currentPosition
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing playback", e)
        }
    }

    fun seekToFraction(fraction: Float) {
        val player = mediaPlayer ?: return
        try {
            val clamped = fraction.coerceIn(0f, 1f)
            val targetMs = (player.duration * clamped).toInt()
            player.seekTo(targetMs)
            _playbackState.value = _playbackState.value.copy(
                progress = clamped,
                currentPositionMs = targetMs
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error seeking audio", e)
        }
    }

    private fun startProgressTicker() {
        stopProgressTicker()
        progressJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                val player = mediaPlayer
                if (player != null && player.isPlaying) {
                    val dur = max(player.duration, 1)
                    val pos = player.currentPosition.coerceIn(0, dur)
                    _playbackState.value = _playbackState.value.copy(
                        isPlaying = true,
                        progress = (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f),
                        currentPositionMs = pos,
                        durationMs = dur
                    )
                }
                delay(50L)
            }
        }
    }

    private fun stopProgressTicker() {
        progressJob?.cancel()
        progressJob = null
    }

    fun stopAndRelease() {
        stopProgressTicker()
        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {
        }
        try {
            mediaPlayer?.release()
        } catch (_: Exception) {
        }
        mediaPlayer = null
        currentTempFile?.delete()
        currentTempFile = null
        _playbackState.value = AudioPlaybackState()
    }

    companion object {
        private fun isEncodedContainer(bytes: ByteArray, mimeType: String?): Boolean {
            if (bytes.size >= 4) {
                val header = String(bytes, 0, 4, Charsets.US_ASCII)
                if (header == "RIFF" || header == "OggS" || header.startsWith("ID3")) {
                    return true
                }
            }
            if (mimeType != null) {
                val lower = mimeType.lowercase()
                if (lower.contains("mp3") || lower.contains("mpeg") || lower.contains("ogg")) {
                    return true
                }
            }
            return false
        }

        private fun parseSampleRate(mimeType: String?): Int {
            if (mimeType.isNullOrBlank()) return 24000
            val regex = Regex("rate=(\\d+)", RegexOption.IGNORE_CASE)
            val match = regex.find(mimeType)
            return match?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 24000
        }

        fun wrapPcm16MonoInWav(pcmData: ByteArray, sampleRate: Int = 24000): ByteArray {
            val channels = 1
            val bitsPerSample = 16
            val byteRate = sampleRate * channels * bitsPerSample / 8
            val blockAlign = (channels * bitsPerSample / 8).toShort()
            val dataSize = pcmData.size
            val chunkSize = 36 + dataSize

            val buffer = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)
            // RIFF header
            buffer.put("RIFF".toByteArray(Charsets.US_ASCII))
            buffer.putInt(chunkSize)
            buffer.put("WAVE".toByteArray(Charsets.US_ASCII))
            // fmt subchunk
            buffer.put("fmt ".toByteArray(Charsets.US_ASCII))
            buffer.putInt(16) // Subchunk1Size for PCM
            buffer.putShort(1.toShort()) // AudioFormat = 1 (PCM)
            buffer.putShort(channels.toShort())
            buffer.putInt(sampleRate)
            buffer.putInt(byteRate)
            buffer.putShort(blockAlign)
            buffer.putShort(bitsPerSample.toShort())
            // data subchunk
            buffer.put("data".toByteArray(Charsets.US_ASCII))
            buffer.putInt(dataSize)
            buffer.put(pcmData)
            return buffer.array()
        }

        fun extractWaveformBars(rawBytes: ByteArray, barCount: Int = 36): List<Float> {
            if (rawBytes.size < 128) {
                return List(barCount) { idx -> 0.25f + 0.5f * ((idx * 7) % 10) / 10f }
            }
            val startOffset = if (
                rawBytes.size > 44 &&
                String(rawBytes, 0, 4, Charsets.US_ASCII) == "RIFF"
            ) {
                44
            } else {
                0
            }
            val usableBytes = rawBytes.size - startOffset
            val totalSamples = usableBytes / 2
            if (totalSamples < barCount) {
                return List(barCount) { 0.4f }
            }

            val samplesPerBar = max(1, totalSamples / barCount)
            val rawRms = FloatArray(barCount)
            var maxRms = 0.01f

            for (bar in 0 until barCount) {
                val sampleStart = bar * samplesPerBar
                val sampleEnd = minOf(totalSamples, sampleStart + samplesPerBar)
                var sumSquares = 0.0
                var count = 0
                val step = max(1, (sampleEnd - sampleStart) / 60)
                var s = sampleStart
                while (s < sampleEnd) {
                    val byteIdx = startOffset + s * 2
                    if (byteIdx + 1 < rawBytes.size) {
                        val low = rawBytes[byteIdx].toInt() and 0xFF
                        val high = rawBytes[byteIdx + 1].toInt()
                        val sample = (high shl 8) or low
                        val norm = sample / 32768.0
                        sumSquares += norm * norm
                        count++
                    }
                    s += step
                }
                val rms = if (count > 0) sqrt(sumSquares / count).toFloat() else 0.1f
                rawRms[bar] = rms
                if (rms > maxRms) maxRms = rms
            }

            return rawRms.map { (it / maxRms).coerceIn(0.16f, 1.0f) }
        }
    }
}
