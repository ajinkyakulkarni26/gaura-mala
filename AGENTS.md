# Gaura Mala Agent Notes

Last reviewed: 2026-10-06

## Project

Gaura Mala is a native Wear OS japa counter for the Hare Krishna maha-mantra. It is a watch-only app with a circular 108-bead progress ring, daily round goal (16 by default), haptics, a Tile, and a watch-face complication.

- Application ID: `com.gauramala.wear`
- Current local version: `versionCode 11`, `versionName 1.0.0`
- Main stack: Kotlin, Jetpack Compose for Wear OS Material 3, DataStore, Wearable Services
- Minimum SDK 30; target and compile SDK 37; JDK 17
- The app is declared standalone in `AndroidManifest.xml`; core counting does not require a phone app.

## Source map

- `MainActivity.kt`: activity, screen-on preference, and Compose setup.
- `presentation/MantraCounterViewModel.kt`: counter behavior, day rollover, debouncing, persistence coordination, and haptic dispatch.
- `presentation/MantraCounterStateMachine.kt`: pure counter transitions and undo history.
- `presentation/CounterHapticPolicy.kt`: input-specific haptic selection.
- `presentation/RotaryBeadInput.kt`: crown delta accumulation and idle reset.
- `presentation/ui/MantraCounterScreen.kt`: counter screen, tap/crown/double-pinch input, and the one-time pinch cue.
- `presentation/ui/RoundProgressDialog.kt`: manual completed-round adjustment for rounds chanted with physical beads.
- `presentation/ui/BeadProgressRing.kt`: 108-bead circular progress UI.
- `presentation/ui/SettingsDialog.kt`: settings, resets, and privacy policy entry.
- `presentation/ui/PrivacyPolicyScreen.kt`: on-watch policy and Contact Developer action.
- `data/MantraPreferences.kt`: local DataStore preferences and daily progress.
- `haptics/HapticHelper.kt`: vibration patterns.
- `tile/GauraMalaTileService.kt`, `complication/GauraMalaComplicationService.kt`: Wear OS surfaces.
- `app/src/test/`: unit tests for counter transitions, haptic policy, preference state, and rotary input.

## Product decisions to preserve

- Inputs are full-screen tap, rotary crown, and system double-pinch where the watch advertises gesture detection. Keep tap/crown usable when gesture detection is unavailable.
- The system gesture indicator is deliberately shown at most once per counter-screen visit. Do not show the help cue after every tap, crown movement, or pinch, and do not let it shift the counter text layout.
- Crown movement is converted from accumulated rotary pixels to discrete bead steps. The accumulator resets after idle time to prevent counts continuing after crown rotation stops.
- Tap and crown use app haptics for bead progress and special milestone/round cues. Double-pinch relies on the system's ordinary gesture feedback while retaining the app's special milestone, round-completion, and daily-goal cues.
- Milestones are beads 27, 54, and 81; one round completes at 108. The daily-goal completion cue takes priority when the last bead also reaches the daily goal.
- The counter's round label opens a manual adjustment for completed rounds today, so users can reconcile progress after chanting with physical beads. Saving starts the next watch round at bead 0, clears the previous bead's undo history, and does not play chanting haptics or show the goal celebration.
- Keep Awake is optional and defaults off. The double-pinch path supports ambient mode; do not force the display to remain fully awake to make counting work.
- Preserve the current centered round bead ring. Earlier circle redesigns looked disoriented to the user; the requested adjustment was text placement, not a different ring.
- Wear OS screens must adapt to round displays with different usable diameters and system font scales. Let labels wrap where needed, keep important controls within the curved safe area, and preserve at least 48dp touch targets for primary actions.
- The Settings list is a short, regular `Column` with continuous `verticalScroll` and `rotaryScrollable` attached to the same scroll state. The user confirmed this feels smooth on the Pixel Watch 5. Keep this implementation; `TransformingLazyColumn` free scrolling felt rough, and its snap-and-fling experiment felt slow and stepped like an elevator.
- Wrist-flick dismissal is standard Wear OS behavior. Do not try to intercept system navigation to keep the app open.
- Progress and preferences are stored locally on the watch. Counts roll over on the local calendar date; preferences remain. The app does not send this data to a server, and Android backup is disabled. Do not describe the app as storing no data at all.
- The launcher uses a black adaptive-icon background and a bead-mala foreground with no text. Keep the artwork centered and sized to avoid clipping in the circular mask.
- Contact Developer opens the GitHub Issues page on the paired phone through `RemoteActivityHelper`. If the phone is unavailable, the policy screen displays the URL. There is no email sent automatically and no phone companion app is required.

## Privacy and store listing

- The on-watch policy is implemented in `PrivacyPolicyScreen.kt`; its sections are stacked to fit a round display.
- The hosted policy is `https://ajinkyakulkarni26.github.io/gaura-mala/privacy-policy.html`.
- The app requests vibration access. It has no account, ads, analytics, or server-side user data collection.
- Google Play requires a dedicated Wear OS track for this watch bundle. Do not upload the Wear OS-only AAB to a phone-oriented track.

## Build, test, and install

From the repository root:

```bash
./gradlew :app:assembleDebug
./gradlew :app:test
./gradlew :app:bundleRelease :app:assembleRelease
```

Build outputs:

- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release APK: `app/build/outputs/apk/release/app-release.apk`
- Play upload bundle: `app/build/outputs/bundle/release/app-release.aab`

The debug application ID is `com.gauramala.wear.debug`. The release application ID is `com.gauramala.wear`. Increment `versionCode` in `app/build.gradle.kts` for every new Play upload; Play does not allow reusing an uploaded version code. Release signing reads Gradle properties `GAURA_UPLOAD_STORE_FILE`, `GAURA_UPLOAD_STORE_PASSWORD`, `GAURA_UPLOAD_KEY_ALIAS`, and `GAURA_UPLOAD_KEY_PASSWORD`. Never commit the keystore or signing credentials.

For a connected watch, find its ADB serial with `adb devices -l`, then install and launch the release APK:

```bash
adb -s <watch-serial> install -r app/build/outputs/apk/release/app-release.apk
adb -s <watch-serial> shell am start -n com.gauramala.wear/.MainActivity
```

Use a compatible physical Pixel Watch to verify double-pinch and real haptics. Generic Wear OS emulators may not expose gesture hardware or realistic vibration. Emulator testing can still check layout, navigation, and the no-phone Contact Developer fallback.

## Release status and next work

Last known Play Console status (2026-10-06): the user has two active Wear OS closed-testing tracks. The “Closed testing Round 2” track shows release 10 (1.0.0), last updated Oct 2, 2026. The earlier “Closed Testing Gaura Mala” track shows release 4 (1.0.0), last updated Sep 30, 2026. The user had passed the initial 12 opted-in tester gate; production access still requires at least 12 testers opted in continuously for 14 days, then an application for production access. Confirm live tester counts and dates before advising; do not assume the 14-day period is complete.

Version 9 was built and installed on the user's Pixel Watch 5; the user confirmed its gesture and privacy-screen fixes work. Version 10 increases Daily Goal +/- controls to 48dp, raises small helper text to 10sp and the round label to 12sp, and permits settings copy to wrap. The version 10 debug build passed emulator layout checks at simulated 192dp and 227dp round sizes with reduced (0.85x) and enlarged (1.3x) system font scales. The current Settings implementation uses a regular `Column` with continuous touch scrolling and direct rotary scrolling; the user confirmed it feels smooth on the Pixel Watch 5.

Version 11 is a dependency maintenance release. `app/build.gradle.kts` now uses Wear Tiles 1.5.0 and Wear ProtoLayout 1.3.0. Gradle resolves `tiles-proto` 1.5.0 and ProtoLayout artifacts 1.3.0; these include the fixes for the Wear OS 5/API 34 Tile update `SecurityException` and protobuf CVE-2024-7254 warnings seen on release 4. The signed version 11 AAB built successfully on 2026-10-06. It has not been uploaded to Play Console or manually validated on a physical watch. The Fragment 1.1.0 “outdated SDK” notice remains a lower-priority transitive dependency warning and was not changed.

Next steps:

1. Manually check the new round adjustment on the watch, including a save with partial bead progress, then check smaller and larger system font sizes on the counter, settings, and privacy policy screens.
2. Before preparing the next Play bundle, confirm whether version 11 was uploaded; use a new version code if it was. Build and upload the updated source to a Wear OS closed-testing track, then check whether Play Console clears the Tile and protobuf notices for that artifact.
3. Verify Tile updates on a Wear OS 5/API 34 device if available; also check the Tile and complication after the dependency update on the Pixel Watch 5.
4. Verify the Contact Developer handoff with the watch paired to the user's phone; the emulator only verified the no-phone fallback.
5. Keep at least 12 testers opted in for the full 14-day closed-test period, gather feedback, then complete the Play Console production-access application.
6. Continue checking the Play listing, privacy/data declarations, screenshots, and required store assets before production review.

## Workspace hygiene

At the time this file was added, `play-store/`, `test-artifacts/`, `testers.csv`, and generated Gemini image drafts were untracked. Review them individually before staging anything. In particular, `testers.csv` contains tester email addresses; do not commit it. Avoid broad `git add .` when these user-owned files are present.
