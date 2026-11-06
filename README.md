# VistaLens — AI Photo Tourism & AR Narrated Audio Guide

**VistaLens** is an Android application built with Kotlin, Jetpack Compose, CameraX, Jetpack DataStore, and Firebase AI Logic. Take or upload a photo of any city landmark to recognize its architecture, fetch search-grounded history, and play an interactive AR-style narrated clip.

---

## 📱 Download & Install on Android (`ZanyClaire/vestalens`)

Tap either link below on your Android phone to download and install **`VistaLens.apk`**:

- **[⬇️ Direct APK Download from Repository (`VistaLens.apk`)](https://github.com/ZanyClaire/vestalens/raw/main/release/VistaLens.apk)**
- **[⬇️ Direct APK Download from Latest Release (`VistaLens.apk`)](https://github.com/ZanyClaire/vestalens/releases/latest/download/VistaLens.apk)**
- **[📦 View Latest GitHub Release Page](https://github.com/ZanyClaire/vestalens/releases/latest)**

---

## ✨ Features

1. **AR Viewfinder & Photo Input (`CameraX` + `PickVisualMedia`)**
   - Live CameraX AR viewfinder with animated LIDAR scanline reticle and corner HUD brackets.
   - Zero-permission Android Photo Picker and 3 pre-bundled high-resolution world landmarks (**The Colosseum**, **Eiffel Tower**, and **Senso-ji Pagoda**).
2. **Landmark & Spatial AR Hotspot Recognition (`gemini-4-argon`)**
   - Identifies the monument, city/country, architectural style, era, and normalized `(normX, normY)` coordinates for interactive **AR Spatial Pins** overlaid on the photo.
3. **Real-Time Google Search Grounding (`gemini-3.8-flash` + `Tool.googleSearch()`)**
   - Fetches verified historical overviews, chronological milestones, spoken narration scripts, and clickable web citations, plus interactive follow-up Q&A.
4. **AR-Style Narrated Audio Clip (`gemini-3.8-flash-tts`)**
   - Synthesizes neural speech audio with 5 selectable narrator personas (**Kore**, **Aoede**, **Puck**, **Charon**, and **Fenrir**), a 36-bar waveform scrubber, and synchronized teleprompter subtitles.
5. **Multilingual Persistence (`Jetpack DataStore`)**
   - Persists your preferred tour and narration language (`en-US`, `es-ES`, `fr-FR`, `it-IT`, `ja-JP`, `de-DE`, `pt-BR`, `ko-KR`) on-device via `UserPreferencesRepository`.
