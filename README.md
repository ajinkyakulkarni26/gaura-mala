# 📿 GauraMala (`gaura-mala`)
### Native Wear OS Maha-Mantra Counter & Digital Japa Mala

> *"harer nāma harer nāma harer nāmaiva kevalam*  
> *kalau nāsty eva nāsty eva nāsty eva gatir anyathā"*  
> — *Sri Chaitanya Mahaprabhu (Chaitanya Charitamrita Adi 17.21)*

---

## 🌟 Overview

**GauraMala** is a native **Wear OS** application designed specifically for chanting the **Hare Krishna Maha-mantra**. Named in honor of **Sri Chaitanya Mahaprabhu (Gaura)**, who inaugurated the congregational chanting of the Holy Names, the app transforms your smartwatch into an ergonomic, eyes-closed digital *japa mala*.

It addresses the fundamental limitation of traditional smartwatch counters: **requiring two hands** (one wearing the watch, the other tapping the screen). With GauraMala, chanters on modern devices like the **Pixel Watch** can advance the bead counter using a **single-handed double-pinch gesture**, allowing completely hands-free meditation while walking, traveling, or in motion.

---

## ⚡ Key Highlights & Features

### 1. 🤏 Single-Handed Double-Pinch Gesture
- Integrates Google Wear OS Material3 gesture APIs (`Modifier.oneHandedGesture` with `GestureAction.PRIMARY`).
- Pinches thumb and index finger together twice on the watch arm to advance the mantra count.
- Built-in **280ms debounce window** to eliminate jitter and accidental double-counts.

### 2. 📳 Eyes-Closed Tactile Feedback (HapticHelper)
- **Single Bead (1–107):** Subtle, crisp click (`VibrationEffect.EFFECT_CLICK`) providing quiet confirmation without looking.
- **Milestones (27, 54, 81):** Subtle double-tick marking quarter, half, and three-quarter mala progress.
- **Round Completed (108 Beads = 1 Mala):** Distinctive, deep double-pulse waveform (`[0, 140ms, 100ms, 260ms]`) signaling the completion of a round.
- **Daily Vow Complete (16 Rounds):** Triumphant 3-pulse crescendo vibration celebrating the completion of Srila Prabhupada's prescribed 16-round vow.
- **Haptic Undo Alert:** Soft tick confirmation when undoing an accidental chant.

### 3. 🛡️ Input Fallbacks & Ergonomics
- **Full-Screen Tap:** Tap anywhere on the watch display to advance.
- **Rotary Crown Support:** Rotate the physical watch crown downwards to cycle beads.
- **Fatigue Mitigation:** Toggle between gesture and screen tap in Settings to avoid finger strain during long japa sessions.
- **Accidental Tap Protection:** Quick Undo button and safe confirmation dialogs before resetting rounds.

### 4. 🎨 Sacred Aesthetic & Battery Efficiency
- **Golden Gaura Theme:** Inspired by Lord Chaitanya’s golden complexion (deep saffron `#FFB300`, amber `#FF8F00`, and sacred gold `#FFE082`).
- **OLED Pure Black:** Built on true `#000000` AMOLED canvas for maximum battery longevity during multi-hour chanting sessions.
- **Ambient Mode Support:** Strips animations, dims brightness, and displays high-contrast essential numbers when wrist rests.

### 5. ⌚ Wear OS Ecosystem Integration
- **Glanceable Wear OS Tile (`GauraMalaTileService`):** Swipe left from your watch face to instantly view today's round and bead progress (`Round 8 of 16 — 45 beads`).
- **Watch Face Complication (`GauraMalaComplicationService`):** Supports `SHORT_TEXT` and `RANGED_VALUE` complications on any compatible watch face.

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
2. Select **Open** and choose `/Users/ajinkyak/.gemini/antigravity-ide/scratch/gaura-mala`.
3. Allow Gradle to sync dependencies automatically.

### 2. Run on Wear OS Emulator or Physical Watch
- **Using an Emulator:**
  1. In Android Studio, go to **Tools > Device Manager**.
  2. Create a **Wear OS** Virtual Device (e.g. *Wear OS Round, Android 14/15*).
  3. Click **Run (`Shift + F10`)** to launch GauraMala.
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
~/Library/Android/sdk/platform-tools/adb shell am start -n com.gauramala.wear/.MainActivity

# Simulate watch crown downwards rotation (advance bead)
~/Library/Android/sdk/platform-tools/adb shell input keyevent 261
```
