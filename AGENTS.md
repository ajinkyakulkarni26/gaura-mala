# Gaura Mala Agent Notes

Last reviewed: 2026-10-01

## Project

Gaura Mala is a native Wear OS japa counter for the Hare Krishna maha-mantra. It is a watch-only app with a circular 108-bead progress ring, daily round goal (16 by default), haptics, a Tile, and a watch-face complication.

- Application ID: `com.gauramala.wear`
- Current local version: `versionCode 10`, `versionName 1.0.0`
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
- Keep Awake is optional and defaults off. The double-pinch path supports ambient mode; do not force the display to remain fully awake to make counting work.
- Preserve the current centered round bead ring. Earlier circle redesigns looked disoriented to the user; the requested adjustment was text placement, not a different ring.
- Wear OS screens must adapt to round displays with different usable diameters and system font scales. Let labels wrap where needed, keep important controls within the curved safe area, and preserve at least 48dp touch targets for primary actions.
- The Settings list uses Material 3 `TransformingLazyColumn` with non-snapping touch and rotary scrolling. Avoid restoring snap behavior without checking with the user; they have reported that snap scrolling felt rough before.
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

Last known Play Console status (2026-10-01): the user had published a Wear OS closed-test release and passed the initial 12 opted-in tester gate. The remaining production-access requirement shown in Console was to run the closed test with at least 12 opted-in testers for at least 14 days, then apply for production access. Confirm the live Console status and dates before advising on progress; the 14-day window must not be assumed complete.

Version 9 was built and installed on the user's Pixel Watch 5; the user confirmed its latest gesture and privacy-screen fixes work. Version 10 increases Daily Goal +/- controls to 48dp, raises small helper text to 10sp and the round label to 12sp, and permits settings copy to wrap. The version 10 debug build passed emulator layout checks at simulated 192dp and 227dp round sizes with reduced (0.85x) and enlarged (1.3x) system font scales. In response to a follow-up report of uneven Settings scrolling, the list was migrated to Material 3 `TransformingLazyColumn`; the debug APK compiles and is installed on the watch, but the user's physical scroll assessment is pending. A signed version 10 release bundle is still pending. The version 9 AAB upload to Google Play was not confirmed as of this note.

Next steps:

1. Have the user assess the updated Settings scrolling with the crown and touch on the Pixel Watch 5; refine it if needed.
2. Manually check a smaller and larger system font size on the watch; inspect the counter, settings, and privacy policy screens.
3. After UI review, build and upload the next unused version code to the correct Wear OS closed-testing track and publish it to testers.
4. Verify the Contact Developer handoff with the watch paired to the user's phone; the emulator only verified the no-phone fallback.
5. Keep at least 12 testers opted in for the full 14-day closed-test period, gather feedback, then complete the Play Console production-access application.
6. Continue checking the Play listing, privacy/data declarations, screenshots, and required store assets before production review.

## Workspace hygiene

At the time this file was added, `play-store/`, `test-artifacts/`, `testers.csv`, and generated Gemini image drafts were untracked. Review them individually before staging anything. In particular, `testers.csv` contains tester email addresses; do not commit it. Avoid broad `git add .` when these user-owned files are present.
