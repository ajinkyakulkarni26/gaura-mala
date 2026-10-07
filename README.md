# 📿 Gaura Mala (`gaura-mala`)
### Native Wear OS Maha-Mantra Counter & Digital Japa Mala

> *"harer nāma harer nāma harer nāmaiva kevalam*  
> *kalau nāsty eva nāsty eva nāsty eva gatir anyathā"*  
> — *Sri Chaitanya Mahaprabhu (Chaitanya Charitamrita Adi 17.21)*

---

## 🌟 Overview

**Gaura Mala** is a native **Wear OS** application designed specifically for chanting the **Hare Krishna Maha-mantra**. Named in honor of **Sri Chaitanya Mahaprabhu (Gaura)**, who inaugurated the congregational chanting of the Holy Names, the app transforms your smartwatch into an ergonomic, eyes-closed digital *japa mala*.

It addresses the fundamental limitation of traditional smartwatch counters: **requiring two hands** (one wearing the watch, the other tapping the screen). On compatible Wear OS devices, chanters can advance the counter with the system's **double-pinch gesture**. Screen tap and crown input remain available on other watches.

---

## ⚡ Key Highlights & Features

### 1. 🤏 Single-Handed Double-Pinch Gesture
- Uses Wear Compose's one-handed gesture API for the system primary action (double pinch on Pixel Watch).
- Enable or disable the gesture independently from full-screen tap in Settings.
- This requires the Wear OS one-handed gesture API and hardware that advertises gesture detection. The app hides the pinch hint when the system feature is unavailable; tap and crown input continue to work. See [Android's one-handed gesture guide](https://developer.android.com/training/wearables/compose/one-handed-gestures).
- A **280ms debounce window** filters accidental repeat taps and gestures.
- The pinch cue appears at most once each time the counter screen is opened; counting with taps, crown turns, or pinches does not repeatedly show it.

### 2. 📳 Eyes-Closed Tactile Feedback (HapticHelper)
- **Single Bead (1–107):** Subtle, crisp click (`VibrationEffect.EFFECT_CLICK`) providing quiet confirmation without looking.
- **Milestones (27, 54, 81):** Subtle double-tick marking quarter, half, and three-quarter mala progress.
- **Round Completed (108 Beads = 1 Mala):** Distinctive, deep double-pulse waveform (`[0, 140ms, 100ms, 260ms]`) signaling the completion of a round.
- **Daily Goal Complete:** Triumphant 3-pulse crescendo vibration when the configurable daily round goal is reached (16 rounds by default).
- **Haptic Undo Alert:** Soft tick confirmation when undoing an accidental chant.
- **Round Picker Tick:** A subtle haptic tick confirms each completed-round adjustment when app haptics are enabled.

### 3. 🛡️ Input Fallbacks & Ergonomics
- **Full-Screen Tap:** Tap anywhere on the watch display to advance.
- **Rotary Crown Support:** Rotate the physical watch crown downwards to advance beads.
- **Physical Mala Progress:** Tap the round label to set completed rounds after chanting with a physical mala. Adjust the value with the crown or +/- buttons; saving starts the next watch round at bead 0 and clears any partial bead progress.
- **Fatigue Mitigation:** Enable or disable gesture and screen-tap input independently in Settings.
- **Accidental Tap Protection:** Quick Undo button and confirmation dialogs before resetting current-round or daily progress; reset confirmations use check/cross icons that fit round displays.
- **Daily Goal:** Adjust the target from 1 to 64 rounds in Settings.
- **Daily Rollover:** Progress resets for a new local calendar day when you return to or use the app; preferences and daily progress persist across app restarts.
- **Local data:** Counts and preferences stay on the watch, roll over by local calendar day, and are excluded from Android backup. The app has no account, ads, analytics, or server-side user-data collection; see the [privacy policy](https://ajinkyakulkarni26.github.io/gaura-mala/privacy-policy.html).
- **Intermittent use:** Wear OS may dim the display and return to the watch face after its configured idle timeout. This is expected system behavior, not a counter reset; saved progress remains available when you reopen Gaura Mala.
- **Keep Awake:** An optional setting prevents dimming and uses more battery. It remains off by default. There is no persistent activity notification pinning Gaura Mala to the watch face, so the app can leave the foreground during a chanting break.

### 4. 🎨 Sacred Aesthetic & Battery Efficiency
- **Golden Gaura Theme:** Inspired by Lord Chaitanya’s golden complexion (deep saffron `#FFB300`, amber `#FF8F00`, and sacred gold `#FFE082`).
- **OLED Pure Black:** Built on true `#000000` AMOLED canvas for maximum battery longevity during multi-hour chanting sessions.
- **Ambient Mode Support:** Shows the time, counter, and 108-bead progress ring while removing controls when the watch enters low-power ambient mode. The pinch handler opts into ambient mode on supported devices, so Keep Awake can remain off by default.
- **Consistent Round Display:** Active and ambient screens show the current round; ambient mode shows “Goal” after the daily target is complete.
- **Privacy Policy:** Read the policy on the watch. Contact Developer opens the project's GitHub Issues page on a paired phone, with the URL shown on the watch if no phone is available.

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
│   │       │       ├── RoundProgressDialog.kt    # Manual completed-round adjustment
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

Additional source files: `presentation/CounterHapticPolicy.kt` selects input-specific feedback, `presentation/RotaryBeadInput.kt` turns crown motion into bead steps, and `presentation/ui/PrivacyPolicyScreen.kt` contains the on-watch policy and Contact Developer action. JVM tests for the counter, haptics, saved state, and crown input are under `app/src/test/`.

## Design and Store Assets

- `assets/branding/` contains the app icon and logo source files.
- `assets/design-references/gemini-generated/` keeps generated visual explorations outside the app source tree.
- `play-store/` contains the Play Store feature graphic and watch listing screenshots. Refresh the screenshots before production after the UI is finalized.
- `test-artifacts/` contains watch recordings and captures used during testing; review files individually before adding them to Git.

---

## 🚀 Getting Started

### Requirements
- Android Studio Quail 4 (2026.1.4) or newer, JDK 17, and the Android SDK Platform 37.
- The Gradle wrapper uses Gradle 9.8.0 and Android Gradle Plugin 9.4.0.
- To run the app in an emulator, install the latest stable Wear OS 7 (API 37) system image for your computer's architecture in Android Studio's SDK Manager, then create a round Wear OS device in Device Manager.

Build the debug app from the repository root with `./gradlew :app:assembleDebug`. Before pushing, run `./scripts/test-local.sh`: it tests the contributor-approval policy, runs the JVM unit tests, 100% core-logic coverage gate, and debug build, then detects a connected Wear OS emulator and runs the UI integration suite when one is online. The script prints each integration test case and its result. To run device tests directly, start a Wear OS emulator and use `./gradlew :app:connectedDebugAndroidTest`, then print the detailed results with `python3 scripts/print-android-test-results.py`. Android Studio can also build and install the app with the `app` run configuration.

### Continuous Integration

GitHub Actions runs the JVM unit tests, enforces 100% JaCoCo line coverage for four pure counter-logic files, builds a debug APK, and runs the Wear OS UI integration suite on a round Wear OS 5.1 (API 35) emulator on every branch push and pull request. The core coverage metric does not include Compose UI or Android service code; those behaviors are checked through named integration cases. CI compares executed case IDs with `config/wear-os-integration-tests.txt`: every registered case must run, and newly added tests must be registered. This inventory-based check grows with the suite instead of relying on a fixed minimum count. CI prints each case and uploads coverage and Android test reports. You can start a run manually from the repository's **Actions** tab by selecting **Android CI** and choosing **Run workflow**. The integration suite exercises the counter screen, tap/rotary input, undo, round completion, round adjustment, settings, and reset flows. Compose tests inject rotary events through the app's input handler; double-pinch hardware detection and real vibration still need a compatible physical watch.

The `main` branch requires pull requests and passing CI checks. Contributors should push to a branch and open a pull request; direct pushes to `main` are blocked. PRs from other authors also require approval from repository owner `ajinkyakulkarni26` for the current commit. Owner-authored PRs are exempt from this approval check and still require all CI checks to pass. CI workflows, coverage/build configuration, and policy scripts are protected with `CODEOWNERS`; changing those files requires a second reviewer. Automated coverage and test counts detect regressions, while a maintainer must still judge that new tests meaningfully exercise the change.

The current Play testing release notes in English, Hindi, and Marathi are in [RELEASE_NOTES_v13.txt](./RELEASE_NOTES_v13.txt).

### 1. Open in Android Studio
1. Launch **Android Studio**.
2. Select **Open** and choose this repository's `gaura-mala` folder.
3. Allow Gradle to sync dependencies automatically.

### 2. Run on Wear OS Emulator or Physical Watch
- **Using an Emulator:**
  1. In Android Studio, go to **Tools > Device Manager**.
  2. Create a round **Wear OS** virtual device and install its system image if prompted.
  3. Generic Wear OS AVD images may not include gesture-detection hardware. The app checks for that system feature and shows tap/crown input when it is missing. Test physical double pinch on a compatible Pixel Watch; tap and crown input work in the generic emulator.
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

# Launch Gaura Mala on the watch
~/Library/Android/sdk/platform-tools/adb shell am start -n com.gauramala.wear.debug/com.gauramala.wear.MainActivity
```

The Android Studio `app` run configuration installs the debug build automatically. Rotary crown events should be tested using the emulator's rotary control or a physical watch; a regular keyboard key event does not reliably simulate crown input.
