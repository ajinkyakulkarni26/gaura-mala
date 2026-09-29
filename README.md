# 📿 GauraMala (`gaura-mala`)
### Native Wear OS Maha-Mantra Counter & Digital Japa Mala

> *"harer nāma harer nāma harer nāmaiva kevalam*  
> *kalau nāsty eva nāsty eva nāsty eva gatir anyathā"*  
> — *Sri Chaitanya Mahaprabhu (Chaitanya Charitamrita Adi 17.21)*

---

## 🌟 Overview

**GauraMala** is a native **Wear OS** application designed specifically for chanting the **Hare Krishna Maha-mantra**. Named in honor of **Sri Chaitanya Mahaprabhu (Gaura)**, who inaugurated the congregational chanting of the Holy Names, the app transforms your smartwatch into an ergonomic, eyes-closed digital *japa mala*.

It addresses the fundamental limitation of traditional smartwatch counters: **requiring two hands** (one wearing the watch, the other tapping the screen). On supported Wear OS 7 devices, chanters can advance the counter with the system's **double-pinch gesture**. Screen tap and crown input remain available on other watches.

---

## ⚡ Key Highlights & Features

### 1. 🤏 Single-Handed Double-Pinch Gesture
- Uses Wear Compose's one-handed gesture API for the system primary action (double pinch on Pixel Watch).
- Enable or disable the gesture independently from full-screen tap in Settings.
- This requires Wear OS 7 (API 37) and compatible hardware. On unsupported devices, tap and crown input continue to work. See [Android's one-handed gesture guide](https://developer.android.com/training/wearables/compose/one-handed-gestures).
- A **280ms debounce window** filters accidental repeat taps and gestures.

### 2. 📳 Eyes-Closed Tactile Feedback (HapticHelper)
- **Single Bead (1–107):** Subtle, crisp click (`VibrationEffect.EFFECT_CLICK`) providing quiet confirmation without looking.
- **Milestones (27, 54, 81):** Subtle double-tick marking quarter, half, and three-quarter mala progress.
- **Round Completed (108 Beads = 1 Mala):** Distinctive, deep double-pulse waveform (`[0, 140ms, 100ms, 260ms]`) signaling the completion of a round.
- **Daily Goal Complete:** Triumphant 3-pulse crescendo vibration when the configurable daily round goal is reached (16 rounds by default).
- **Haptic Undo Alert:** Soft tick confirmation when undoing an accidental chant.

### 3. 🛡️ Input Fallbacks & Ergonomics
- **Full-Screen Tap:** Tap anywhere on the watch display to advance.
- **Rotary Crown Support:** Rotate the physical watch crown downwards to advance beads.
- **Fatigue Mitigation:** Toggle between gesture and screen tap in Settings to avoid finger strain during long japa sessions.
- **Accidental Tap Protection:** Quick Undo button and confirmation dialogs before resetting current-round or daily progress.
- **Daily Goal:** Adjust the target from 1 to 64 rounds in Settings.
- **Daily Rollover:** Progress resets for a new local calendar day when you return to or use the app; preferences and daily progress persist across app restarts.

### 4. 🎨 Sacred Aesthetic & Battery Efficiency
- **Golden Gaura Theme:** Inspired by Lord Chaitanya’s golden complexion (deep saffron `#FFB300`, amber `#FF8F00`, and sacred gold `#FFE082`).
- **OLED Pure Black:** Built on true `#000000` AMOLED canvas for maximum battery longevity during multi-hour chanting sessions.
- **Ambient Mode Support:** Removes controls and progress animation and displays high-contrast essentials when the watch enters ambient mode.

### 5. ⌚ Wear OS Ecosystem Integration
- **Glanceable Wear OS Tile (`GauraMalaTileService`):** View today's round and bead progress, refreshed as you chant.
- **Watch Face Complication (`GauraMalaComplicationService`):** Supports `SHORT_TEXT` and `RANGED_VALUE` complications, refreshed on round completion and daily goal changes.

---

## 📁 Project Architecture

```
gaura-mala/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── res/
│   │   │   ├── values/ (colors.xml, strings.xml, styles.xml)
│   │   │   └── drawable/ (ic_launcher.xml)
│   │   └── java/com/gauramala/wear/
│   │       ├── MainActivity.kt
│   │       ├── WearSurfaceUpdater.kt              # Tile and complication refresh requests
│   │       ├── data/
│   │       │   └── MantraPreferences.kt          # DataStore persistence
│   │       ├── haptics/
│   │       │   └── HapticHelper.kt               # Vibrator waveform controller
│   │       ├── presentation/
│   │       │   ├── MantraUiState.kt              # Immutable state container
│   │       │   ├── MantraCounterViewModel.kt     # State & debouncing business logic
│   │       │   ├── theme/                        # Wear Material3 typography & colors
│   │       │   └── ui/
│   │       │       ├── MantraCounterScreen.kt    # Main circular counter UI
│   │       │       ├── BeadProgressRing.kt       # Canvas 108-bead circular track
│   │       │       ├── SettingsDialog.kt         # Toggles for pinch, tap, haptics
│   │       │       └── SummaryDialog.kt          # 16-round completion celebration
│   │       ├── tile/
│   │       │   └── GauraMalaTileService.kt       # Wear OS Tile provider
│   │       └── complication/
│   │           └── GauraMalaComplicationService.kt# Watch face complication provider
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

---

## 🚀 Getting Started

### 1. Open in Android Studio
1. Launch **Android Studio**.
2. Select **Open** and choose this repository's `gaura-mala` folder.
3. Allow Gradle to sync dependencies automatically.

### 2. Run on Wear OS Emulator or Physical Watch
- **Using an Emulator:**
  1. In Android Studio, go to **Tools > Device Manager**.
  2. Create a round **Wear OS** virtual device and install its system image if prompted.
  3. To try double pinch, use a Wear OS 7 (API 37) image and a device profile that supports the gesture. Tap and crown input work on earlier images too.
  4. Select the `app` run configuration and click **Run (`Shift + F10`)**.
  5. Try tapping to count, undo, rotating the crown, changing the daily goal, and resetting progress. The emulator may not provide realistic vibration feedback.
- **Using a Physical Watch (e.g., Pixel Watch):**
  1. On the watch: Enable **Developer Options** (Settings > System > About > tap *Build number* 7 times).
  2. Enable **ADB Debugging** and **Wireless Debugging**.
  3. Pair your computer with the watch via ADB:
     ```bash
     adb pair <watch-ip>:<pairing-port>
     adb connect <watch-ip>:<connect-port>
     ```
  4. Select your watch in Android Studio and deploy!

---

## 🛠️ Useful ADB Commands

```bash
# Verify connected watch
~/Library/Android/sdk/platform-tools/adb devices

# Install debug APK directly via ADB
~/Library/Android/sdk/platform-tools/adb install app/build/outputs/apk/debug/app-debug.apk

# Launch GauraMala on the watch
~/Library/Android/sdk/platform-tools/adb shell am start -n com.gauramala.wear.debug/.MainActivity
```

The Android Studio `app` run configuration installs the debug build automatically. Rotary crown events should be tested using the emulator's rotary control or a physical watch; a regular keyboard key event does not reliably simulate crown input.
