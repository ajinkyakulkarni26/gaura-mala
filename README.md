# 📿 Gaura Mala (`gaura-mala`)
### A quiet Wear OS companion for Hare Krishna japa

> *"harer nāma harer nāma harer nāmaiva kevalam*  
> *kalau nāsty eva nāsty eva nāsty eva gatir anyathā"*  
> — *Śrī Caitanya-caritāmṛta, Ādi 17.21* ([read the verse](https://vedabase.io/en/library/cc/adi/17/21/))

---

## Why Gaura Mala?

Gaura Mala is a simple counter for Hare Krishna maha-mantra rounds on Wear OS (with a standalone Apple Watch counterpart in `watchos/`). It helps you keep your place with a centered 108-bead ring and quiet haptic cues, whether you are sitting for japa or doing a simple, familiar task with your hands. The watch helps you keep count; the chanting and attention remain yours.

Japa with real beads remains the heart of the practice. The app is meant to support remembrance through ordinary parts of the day and help build a steadier chanting routine—not to replace a physical mala, attentive japa, the guidance of your spiritual community, or the aspiration many devotees hold to complete their rounds during Brahma-muhūrta.

The thought behind the app draws on verses such as Bhagavad-gītā 8.7, which teaches remembrance of Krishna while carrying out one’s duty, and Śrīmad-Bhāgavatam 11.5.32, read in the Gaudiya Vaishnava tradition as describing the worship of the Lord through chanting in Kali-yuga. Śrī Caitanya Mahāprabhu’s Śikṣāṣṭakam also describes the holy name as cleansing the mirror of the heart. These teachings are the spiritual context for the app, not a claim that a watch can create that transformation. [Read the full story and verse references](https://ajinkyakulkarni26.github.io/gaura-mala/why-gaura-mala.html) ([HTML source](./docs/why-gaura-mala.html)).

## At a glance

- **Count your rounds:** Follow the centered 108-bead ring with tap, Digital Crown, or the system double-pinch gesture on supported watches.
- **Keep a gentle rhythm:** Subtle haptics mark beads and round milestones, so you can look less often or close your eyes during quiet seated japa.
- **Use Auto Count when it helps:** Set a natural pace, or optionally record one short chant as a local guide. Pause or stop at any time.
- **Gentle chanting reminders:** Optionally enable daytime reminders (off by default) that nudge you after 2.5 hours of inactivity once you have started chanting for the day, with quick notification actions to log physical-mala rounds or mark all rounds complete.
- **Keep physical-mala progress:** Adjust completed rounds at any time after chanting with real beads.
- **See progress at a glance:** Use the Wear OS Tile or a watch-face complication.
- **Keep your data on the watch:** Counts, settings, pace, and an optional voice guide stay local. The guide is user-recorded, excluded from Android backup, and can be deleted in the app. See the [privacy policy](https://ajinkyakulkarni26.github.io/gaura-mala/privacy-policy.html).

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
│   │       ├── ChantReminderScheduler.kt          # Optional 2.5-hour daytime reminder alarms & notifications
│   │       ├── ChantReminderReceiver.kt           # Reminder alarm & "All Rounds Complete" broadcast handler
│   │       ├── WearSurfaceUpdater.kt              # Tile and complication refresh requests
│   │       ├── data/
│   │       │   └── MantraPreferences.kt           # DataStore persistence
│   │       ├── haptics/
│   │       │   └── HapticHelper.kt                # Vibrator waveform controller
│   │       ├── media/
│   │       │   ├── VoiceChantAudio.kt             # Optional on-device chant recording and playback
│   │       │   └── VoiceChantFiles.kt             # App-private non-backed-up audio file management
│   │       ├── presentation/
│   │       │   ├── AutoChantPacer.kt              # Pace calibration and timed count scheduling
│   │       │   ├── ChantReminderPolicy.kt         # Reminder eligibility, quiet hours, and rotating copy
│   │       │   ├── CounterHapticPolicy.kt         # Input-specific haptic selection
│   │       │   ├── MantraCounterStateMachine.kt   # Pure counter transitions and undo history
│   │       │   ├── MantraCounterViewModel.kt      # State, day rollover & debouncing coordination
│   │       │   ├── MantraUiState.kt               # Immutable state container
│   │       │   ├── RotaryBeadInput.kt             # Crown delta accumulation and idle reset
│   │       │   ├── theme/                         # Wear Material3 typography & colors
│   │       │   └── ui/
│   │       │       ├── AutoChantSetupScreen.kt    # Pace calibration, recording consent, and controls
│   │       │       ├── BeadProgressRing.kt        # Canvas 108-bead circular track
│   │       │       ├── MantraCounterScreen.kt     # Main circular counter UI
│   │       │       ├── PrivacyPolicyScreen.kt     # On-watch policy and Contact Developer action
│   │       │       ├── RoundProgressDialog.kt     # Manual completed-round adjustment
│   │       │       ├── SettingsDialog.kt          # Counter settings, reminders, and Auto Count entry
│   │       │       └── SummaryDialog.kt           # Daily goal completion celebration
│   │       ├── tile/
│   │       │   ├── GauraMalaTileContent.kt        # Pure mapping from saved progress to Tile labels
│   │       │   └── GauraMalaTileService.kt        # Wear OS Tile provider
│   │       └── complication/
│   │           └── GauraMalaComplicationService.kt# Watch face complication provider
│   ├── src/test/                                  # JVM unit tests (100% line coverage on pure core logic)
│   ├── src/androidTest/                           # Wear OS Compose UI and TileService integration tests
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── watchos/                                       # Standalone Apple Watch SwiftUI app, WidgetKit extension & core package
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## Design and Store Assets

- `assets/branding/` contains the app icon and logo source files.
- `assets/design-references/gemini-generated/` keeps generated visual explorations outside the app source tree.
- `play-store/` contains the Play Store feature graphic and current Wear OS listing screenshots. The Auto Count set shows pace setup, running and paused states, plus the optional voice guide; review these files before uploading them in Play Console.
- `test-artifacts/` contains watch recordings and captures used during testing; review files individually before adding them to Git. Emulator UI screenshots from local test runs are saved under the ignored `test-artifacts/emulator-screenshots/` directory.

---

## 🚀 Getting Started

### Requirements
- Android Studio Quail 4 (2026.1.4) or newer, JDK 17, and the Android SDK Platform 37.
- The Gradle wrapper uses Gradle 9.8.2 and Android Gradle Plugin 9.5.0.
- To run the app in an emulator, install a round Wear OS system image for your computer's architecture in Android Studio's SDK Manager, then create a round Wear OS device in Device Manager.

Build the debug app from the repository root with `./gradlew :app:assembleDebug`. Before pushing, run `./scripts/test-local.sh` to execute the local pre-push suite (policy tests, JVM unit tests, 100% core-logic coverage gate, debug APK build, 64-bit native ABI check, `actionlint`/`shellcheck` when installed, and the Wear OS emulator UI suite when an emulator is running). It targets only an emulator, even when a physical watch is also connected. Set `WEAR_TEST_SERIAL` to choose a specific emulator when more than one is running, `WEAR_TEST_RUN_LABEL` to name the screenshot folder, and `WEAR_TEST_FONT_SCALE=1.3` to inspect a larger text scale. To run device tests directly, start a Wear OS emulator and use `./gradlew :app:connectedDebugAndroidTest`, then print detailed results with `python3 scripts/print-android-test-results.py`.

### Continuous Integration

GitHub Actions runs the same unit, coverage, workflow-lint, and 64-bit ABI checks, plus the Wear OS instrumentation suite on both a large round API 35 emulator at default font scale and a small round emulator at 1.3x font scale. The emulator suite covers the counter, tap/rotary input, undo, round completion and adjustment, settings, resets, three-trial pace calibration, Auto Count pause/resume and notification behavior, chanting reminders, voice-recording disclosure and playback setup, actual `GauraMalaTileService` responses, and >= 48dp semantic touch targets. CI uploads review screenshots with each emulator profile (saved locally under `test-artifacts/emulator-screenshots/`), and compares executed case IDs with `config/wear-os-integration-tests.txt` so every registered case must run and new tests must be registered. Microphone capture, audio playback quality, real crown feel, double-pinch hardware detection, and real vibration still require a physical watch.

CI also builds a release AAB using an isolated temporary signing key, checks required 64-bit native ABIs, and validates embedded native debug symbols when present (the Play upload key is never exposed in CI). A separate Apple Watch workflow (`watchos-ci.yml`) runs Swift CodeQL, `GauraMalaCore` unit tests, and a watchOS simulator build. Gradle Enhanced Caching and diagnostic Build Scans are enabled. CodeQL (`java-kotlin`, `security-extended`) and Dependency Review run in the required Android CI check; high-or-higher CodeQL security alerts, CodeQL error alerts, and moderate-or-higher new PR dependency vulnerabilities block merges. Read-only PR workflows and trusted `workflow_run` / `main` workflows submit Gradle dependency snapshots for Dependabot, while `.github/dependabot.yml` groups minor/patch updates weekly and keeps major/security updates separate. Workflow actions are pinned to full commit SHAs. You can trigger **Android CI** manually via **Actions → Run workflow**.

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
