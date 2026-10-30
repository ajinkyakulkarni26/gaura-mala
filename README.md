# 📿 Gaura Mala (`gaura-mala`)
### A quiet Wear OS companion for Hare Krishna japa

> *"harer nāma harer nāma harer nāmaiva kevalam*  
> *kalau nāsty eva nāsty eva nāsty eva gatir anyathā"*  
> — *Śrī Caitanya-caritāmṛta, Ādi 17.21* ([read the verse](https://vedabase.io/en/library/cc/adi/17/21/))

---

## A little room for remembrance

Gaura Mala is a simple counter for Hare Krishna maha-mantra rounds on Wear OS. It helps you keep your place with a 108-bead ring and quiet haptic cues, whether you are sitting for japa or doing a simple task with your hands.

Real japa beads remain the ideal companion. Gaura Mala is a small aid for moments when holding beads is impractical—not a replacement for beads, attentive chanting, or the guidance of your spiritual community. [Read the story and the thinking behind the app](./docs/why-gaura-mala.html) or [open the published page](https://ajinkyakulkarni26.github.io/gaura-mala/why-gaura-mala.html).

## At a glance

- **Count your rounds:** Follow the centered 108-bead ring with tap, Digital Crown, or the system double-pinch gesture on supported watches.
- **Keep a gentle rhythm:** Subtle haptics mark beads and round milestones, so you can look less often or close your eyes during quiet seated japa.
- **Use Auto Count when it helps:** Set a natural pace, or optionally record one short chant as a local guide. Pause or stop at any time.
- **Keep physical-mala progress:** Adjust completed rounds after chanting with real beads.
- **See progress at a glance:** Use the Wear OS Tile or a watch-face complication.
- **Keep your data on the watch:** Counts, settings, pace, and an optional voice guide stay local. The guide is user-recorded, excluded from Android backup, and can be deleted in the app. See the [privacy policy](https://ajinkyakulkarni26.github.io/gaura-mala/privacy-policy.html).

For gesture support, audio recording, local storage, and other data practices, see the sections below and the linked privacy policy.

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
│   │       ├── AutoCountOngoingActivity.kt        # Low-power active-session notification
│   │       ├── WearSurfaceUpdater.kt              # Tile and complication refresh requests
│   │       ├── data/
│   │       │   └── MantraPreferences.kt          # DataStore persistence
│   │       ├── haptics/
│   │       │   └── HapticHelper.kt               # Vibrator waveform controller
│   │       ├── media/
│   │       │   └── VoiceChantAudio.kt             # Optional on-device chant recording and playback
│   │       ├── presentation/
│   │       │   ├── AutoChantPacer.kt              # Pace calibration and timed count scheduling
│   │       │   ├── MantraUiState.kt              # Immutable state container
│   │       │   ├── MantraCounterViewModel.kt     # State & debouncing business logic
│   │       │   ├── theme/                        # Wear Material3 typography & colors
│   │       │   └── ui/
│   │       │       ├── MantraCounterScreen.kt    # Main circular counter UI
│   │       │       ├── BeadProgressRing.kt       # Canvas 108-bead circular track
│   │       │       ├── RoundProgressDialog.kt    # Manual completed-round adjustment
│   │       │       ├── SettingsDialog.kt         # Counter settings and Auto Count entry point
│   │       │       ├── AutoChantSetupScreen.kt   # Pace calibration, recording consent, and controls
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

Additional source files: `presentation/CounterHapticPolicy.kt` selects input-specific feedback, `presentation/RotaryBeadInput.kt` turns crown motion into bead steps, `tile/GauraMalaTileContent.kt` maps saved progress to Tile labels, and `presentation/ui/PrivacyPolicyScreen.kt` contains the on-watch policy and Contact Developer action. JVM tests for the counter, haptics, saved state, crown input, and Tile label mapping are under `app/src/test/`.

## Design and Store Assets

- `assets/branding/` contains the app icon and logo source files.
- `assets/design-references/gemini-generated/` keeps generated visual explorations outside the app source tree.
- `play-store/` contains the Play Store feature graphic and current Wear OS listing screenshots. The Auto Count set shows pace setup, running and paused states, plus the optional voice guide; review these files before uploading them in Play Console.
- `test-artifacts/` contains watch recordings and captures used during testing; review files individually before adding them to Git. Emulator UI screenshots from local test runs are saved under the ignored `test-artifacts/emulator-screenshots/` directory.

## Why Gaura Mala?

Japa with real beads is the heart of the practice. Gaura Mala began as a small companion for the moments when the hands are busy with a simple, familiar task, or when a quiet seated practice benefits from a gentle haptic rhythm. The watch helps you keep count; the chanting and attention remain yours.

The app is meant to support remembrance through ordinary parts of the day and, for those who use it, help build a steadier chanting routine. It does not replace a physical mala, attentive japa, or the aspiration many devotees hold to complete their rounds during Brahma-muhūrta.

The thought behind the app draws on verses such as Bhagavad-gītā 8.7, which teaches remembrance of Krishna while carrying out one’s duty, and Śrīmad-Bhāgavatam 11.5.32, read in the Gaudiya Vaishnava tradition as describing the worship of the Lord through chanting in Kali-yuga. Śrī Caitanya Mahāprabhu’s Śikṣāṣṭakam also describes the holy name as cleansing the mirror of the heart. These teachings are the spiritual context for the app, not a claim that a watch can create that transformation.

[Read the longer story behind Gaura Mala](./docs/why-gaura-mala.html), with verse references and notes on how the app is intended to be used.

---

## 🚀 Getting Started

### Requirements
- Android Studio Quail 4 (2026.1.4) or newer, JDK 17, and the Android SDK Platform 37.
- The Gradle wrapper uses Gradle 9.8.0 and Android Gradle Plugin 9.4.0.
- To run the app in an emulator, install the latest stable Wear OS 7 (API 37) system image for your computer's architecture in Android Studio's SDK Manager, then create a round Wear OS device in Device Manager.

Build the debug app from the repository root with `./gradlew :app:assembleDebug`. Before pushing, run `./scripts/test-local.sh`: it tests the contributor-approval policy, runs the JVM unit tests, 100% pure-logic coverage gate, debug build, and 64-bit native ABI check, then detects a connected Wear OS emulator and runs the UI integration suite when one is online. It targets only an emulator, even when a physical watch is also connected. Set `WEAR_TEST_SERIAL` to choose a specific emulator when more than one is running. Set `WEAR_TEST_RUN_LABEL` to name the screenshot folder, and optionally set `WEAR_TEST_FONT_SCALE=1.3` to inspect a larger text scale. Local screenshots are collected under `test-artifacts/emulator-screenshots/`. If `actionlint` and `shellcheck` are installed, it also lints all workflows and shell scripts. The script prints each integration test case and its result. To run device tests directly, start a Wear OS emulator and use `./gradlew :app:connectedDebugAndroidTest`, then print the detailed results with `python3 scripts/print-android-test-results.py`. Android Studio can also build and install the app with the `app` run configuration.

### Continuous Integration

GitHub Actions runs JVM tests, enforces 100% JaCoCo line coverage for the pure counter logic and Tile label mapping, builds a debug APK, and checks that 64-bit native libraries are packaged. It validates workflows with actionlint and shellcheck. The Wear OS instrumentation suite runs on both a large round API 35 emulator at default font scale and a small round emulator at 1.3x font scale. It covers the counter, tap/rotary input, undo, round completion and adjustment, settings, resets, three-trial pace calibration, Auto Count pause/resume and notification behavior, voice-recording disclosure and playback setup, and actual TileService responses. UI tests save screenshots of the counter, Auto Count setup/calibration/running/paused states, and voice-guide screens; CI uploads them with each emulator profile so layout can be reviewed at both sizes. Local runs save screenshots under `test-artifacts/emulator-screenshots/`. CI compares executed case IDs with `config/wear-os-integration-tests.txt`: every registered case must run, and newly added tests must be registered. Microphone capture, audio playback quality, real crown feel, double-pinch hardware detection, and real vibration still need a compatible physical watch. Gradle Enhanced Caching is enabled, and each Gradle invocation publishes an optional Build Scan for build diagnostics. CodeQL scans Kotlin/Java code with the `security-extended` query set, and Dependency Review fails PRs that introduce moderate-or-higher vulnerable dependencies; both run in the existing required Android CI check. A separate read-only workflow generates dependency snapshots for PRs, which a trusted follow-up workflow submits to GitHub; pushes to `main` also submit the Gradle dependency graph. Dependabot checks Gradle and GitHub Actions weekly, grouping minor and patch version updates by ecosystem/directory while leaving major and security updates separately reviewable. Workflow actions are pinned to full commit SHAs with their release tags in comments. Build Scans remain build diagnostics, not security scans. As of 2026-10-07, GitHub's dependency graph, Dependabot alerts, and Dependabot security updates are enabled. The active `main` ruleset requires CodeQL results and blocks high-or-higher security alerts (and CodeQL error alerts); secret scanning and push protection were already enabled. Automatic dependency submission is disabled because the repository submits Gradle snapshots through its workflows. You can start a run manually from the repository's **Actions** tab by selecting **Android CI** and choosing **Run workflow**. Compose tests inject rotary events through the app's input handler.

The emulator suite also asserts that the counter, round adjustment, settings, and primary actions keep at least 48dp semantic touch targets. CI builds a release AAB using a temporary signing key and checks required 64-bit native ABIs. It also reports whether Gradle embedded native debug symbols and validates them when present; symbol availability is confirmed after upload in Play Console. CI never exposes or uses the Play upload key.

The `main` branch requires pull requests and passing CI checks. Contributors should push to a branch and open a pull request; direct pushes to `main` are blocked. PRs from other authors also require approval from repository owner `ajinkyakulkarni26` for the current commit. Owner-authored PRs are exempt from this approval check and still require all CI checks to pass. CI workflows, coverage/build configuration, and policy scripts are protected with `CODEOWNERS`; changing those files requires a second reviewer. Automated coverage and test counts detect regressions, while a maintainer must still judge that new tests meaningfully exercise the change.

GitHub private vulnerability reporting is enabled, and `SECURITY.md` plus the issue forms point contributors to private disclosure for security reports.

The draft release notes for version 15 in English, Hindi, and Marathi are in [RELEASE_NOTES_v15.txt](./RELEASE_NOTES_v15.txt). Earlier release notes remain in [RELEASE_NOTES_v13.txt](./RELEASE_NOTES_v13.txt).

Use the [Wear OS release checklist](./docs/release-checklist.md) for each Google Play upload. Release publishing remains a maintainer action in Play Console. Release builds request `SYMBOL_TABLE`; Gradle packages symbols it can extract, while already-stripped prebuilt libraries may not contribute symbol data.

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
  5. Follow the [physical-watch checklist](./docs/release-checklist.md#physical-watch-checklist) for crown, gesture, haptic, ambient, and inactivity checks.

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
