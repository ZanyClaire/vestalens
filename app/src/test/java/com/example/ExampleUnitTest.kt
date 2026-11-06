package com.example

import com.example.audio.ArAudioPlayer
import com.example.model.NarratorVoice
import com.example.model.SearchSource
import com.example.model.TourLanguage
import com.example.ui.TourismViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun wavWrapper_prependsValidRiffHeader() {
        val dummyPcm = ByteArray(4800) { (it % 120).toByte() }
        val wavBytes = ArAudioPlayer.wrapPcm16MonoInWav(dummyPcm, 24000)
        assertEquals(44 + dummyPcm.size, wavBytes.size)
        assertEquals("RIFF", String(wavBytes, 0, 4, Charsets.US_ASCII))
        assertEquals("WAVE", String(wavBytes, 8, 4, Charsets.US_ASCII))
        val bars = ArAudioPlayer.extractWaveformBars(wavBytes, 36)
        assertEquals(36, bars.size)
        assertTrue(bars.all { it in 0.15f..1.0f })
    }

    @Test
    fun tourLanguageAndVoice_resolveFromPersistedCodes() {
        assertEquals(TourLanguage.JAPANESE, TourLanguage.fromCode("ja-JP"))
        assertEquals(TourLanguage.SPANISH, TourLanguage.fromCode("es-ES"))
        assertEquals(TourLanguage.ENGLISH, TourLanguage.fromCode(null))
        assertEquals(NarratorVoice.AOEDE, NarratorVoice.fromVoiceId("Aoede"))
        assertEquals(NarratorVoice.KORE, NarratorVoice.fromVoiceId("unknown"))
    }

    @Test
    fun parseGroundedDossier_extractsAllSectionsAndSentences() {
        val sampleSearchResponse = """
            [NARRATION_SCRIPT]
            Standing before the Colosseum, you are looking at the largest ancient amphitheatre ever built. Commissioned by Emperor Vespasian in 72 AD, its stone arches once echoed with fifty thousand spectators.

            [HISTORICAL_OVERVIEW]
            Constructed from travertine limestone, tuff, and brick-faced concrete, the Colosseum anchored Imperial Rome.

            [MILESTONES]
            - 72 AD: Construction begins under Emperor Vespasian
            - 80 AD: Inaugurated by Emperor Titus

            [VISITOR_TIP]
            Visit early in the morning or at golden hour for optimal lighting across the outer facade.
        """.trimIndent()

        val sources = listOf(
            SearchSource("Colosseum Official History", "https://parcocolosseo.it", "parcocolosseo.it")
        )
        val dossier = TourismViewModel.parseGroundedDossier(
            rawText = sampleSearchResponse,
            landmarkName = "The Colosseum",
            searchQueries = listOf("Colosseum Rome history"),
            sources = sources
        )

        assertEquals(2, dossier.narrationSentences.size)
        assertEquals(2, dossier.keyMilestones.size)
        assertTrue(dossier.historicalOverview.contains("travertine limestone"))
        assertEquals(1, dossier.sources.size)
    }
}
