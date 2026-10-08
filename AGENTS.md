# Gaura Mala Agent Notes

Last reviewed: 2026-10-07

## Project

Gaura Mala is a native Wear OS japa counter for the Hare Krishna maha-mantra. It is a watch-only app with a circular 108-bead progress ring, daily round goal (16 by default), haptics, a Tile, and a watch-face complication.

- Application ID: `com.gauramala.wear`
- Current local version: `versionCode 13`, `versionName 1.0.0`
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
- `app/src/androidTest/`: Wear OS UI integration tests for the counter, tap/rotary input, undo, round completion and adjustment, settings, and reset flows.
- `scripts/test-local.sh`: local pre-push checks with optional Wear OS emulator integration tests; `scripts/print-android-test-results.py` prints individual case names, descriptions, and outcomes.

## Product decisions to preserve

- Inputs are full-screen tap, rotary crown, and system double-pinch where the watch advertises gesture detection. Keep tap/crown usable when gesture detection is unavailable.
- The system gesture indicator is deliberately shown at most once per counter-screen visit. Do not show the help cue after every tap, crown movement, or pinch, and do not let it shift the counter text layout.
- Crown movement is converted from accumulated rotary pixels to discrete bead steps. The accumulator resets after idle time to prevent counts continuing after crown rotation stops.
- Tap and crown use app haptics for bead progress and special milestone/round cues. Double-pinch relies on the system's ordinary gesture feedback while retaining the app's special milestone, round-completion, and daily-goal cues.
- Milestones are beads 27, 54, and 81; one round completes at 108. The daily-goal completion cue takes priority when the last bead also reaches the daily goal.
- The counter's round label opens a manual adjustment for completed rounds today, so users can reconcile progress after chanting with physical beads. The picker supports plus/minus buttons and crown rotation, with a subtle selection tick when haptics are enabled; use a short title and check/cross actions to fit the round display. Saving starts the next watch round at bead 0, clears the previous bead's undo history, and does not play chanting haptics or show the goal celebration.
- Reset confirmation actions use accessible check/cross icon buttons to avoid clipped labels on round displays.
- When a modal screen takes focus for crown input, restore focus to the counter after it closes so crown counting continues without requiring a tap.
- The ambient counter header shows the current round (`completedRounds + 1`) to match the active display, and shows `Goal` when the daily goal is complete.
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
./gradlew :app:verifyCoreLogicCoverage
./gradlew :app:connectedDebugAndroidTest
./scripts/test-local.sh
./gradlew :app:bundleRelease :app:assembleRelease
```

Maintain 100% JaCoCo **line coverage** across the pure counter logic in `MantraCounterStateMachine.kt`, `CounterHapticPolicy.kt`, `RotaryBeadInput.kt`, and `MantraUiState.kt`. Current main covers all 127 lines. `:app:verifyCoreLogicCoverage` is the CI gate; add or update unit tests whenever these behaviors change, and do not lower the threshold or exclude core files just to make the build pass. The complete unit-test coverage report is generated at `app/build/reports/coverage/test/debug/index.html`. This metric is scoped to Android-independent counter logic; Compose screens and Android services are tested through Wear OS integration/device tests but are not included in the JVM coverage percentage. CI compares executed Wear OS integration test IDs with `config/wear-os-integration-tests.txt`; every registered case must run, and every new case must be added to that inventory. There is no fixed test-count threshold.

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

Run `./scripts/test-local.sh` before pushing. It tests the repository policy scripts, runs unit tests, the 100% core-logic coverage gate, and a debug build; when it finds a running Wear OS emulator, it runs the UI integration suite and prints each test case's outcome. `.github/workflows/android-ci.yml` runs the same Android test layers in GitHub Actions on every branch push and pull request, with a Wear OS 5.1 (API 35) emulator. The workflow uses Gradle Enhanced Caching and publishes a Gradle Build Scan for each Gradle invocation; scans are build diagnostics, not vulnerability scans. CodeQL and Dependency Review run in the required Android CI check. The Wear OS emulator step prints each instrumentation case and outcome after execution, while also preserving test failures as a failed CI step. The current Android instrumentation suite runs on one emulator without sharding, so its cases execute sequentially. The integration report check compares test IDs against `config/wear-os-integration-tests.txt`, so suite growth does not require changing an arbitrary minimum count. The XML and HTML reports are uploaded as artifacts. Compose rotary injection checks the app's rotary event path, but it does not verify physical crown hardware, double-pinch detection, or real haptics. Keep those checks in the physical-watch release checklist.

Security gates are part of the repository CI. The existing required Android CI job runs CodeQL (`java-kotlin`, `security-extended`) and reviews pull-request dependency changes, failing for newly introduced moderate-or-higher vulnerabilities. A read-only pull-request workflow generates Gradle dependency snapshots; a trusted `workflow_run` workflow submits those snapshots, and pushes to `main` submit the current Gradle graph for Dependabot alerts. `.github/dependabot.yml` schedules updates for root Gradle files, app dependencies, and GitHub Actions. Keep workflow permissions narrow; never give write permissions to a workflow that executes untrusted pull-request code.

As of 2026-10-07, GitHub's dependency graph, Dependabot alerts, and Dependabot security updates are enabled under **Settings → Security and quality → Code security and analysis**. The active `main` ruleset requires CodeQL results at **High or higher** for security alerts and blocks CodeQL error alerts; it also requires CI status checks and pull requests. Secret scanning and push protection were already enabled and remain on. Automatic dependency submission is disabled because repository workflows submit Gradle dependency snapshots. Dependency Review runs inside the existing Android CI required check. CodeQL analysis reports findings; the ruleset is what blocks merges at the configured severity. Scans reduce risk but do not prove the app is entirely secure.

The repository's `main` branch is protected: changes go through pull requests, and both Android CI jobs plus the contributor-approval job must pass before merge. Contributor PRs require approval from `ajinkyakulkarni26` on the current commit; owner-authored PRs are exempt from that approval check. New commits after approval require a fresh owner approval. `.github/CODEOWNERS` assigns the owner to CI workflows, coverage/build configuration, Gradle policy, and scripts; those files require an additional code-owner review even on owner-authored PRs. The main ruleset also requires CodeQL results at High or higher and blocks CodeQL error alerts. Update the GitHub ruleset's required checks and code-owner review setting when changing this policy. Do not push directly to `main`.

The contributor-approval workflow checks out the approval script from the target branch, so a PR cannot change the checker it is being evaluated by. During the initial policy bootstrap only, when that script is not on `main` yet, the workflow permits the repository owner to land the setup PR and rejects other authors.

## Release status and next work

Last known Play Console status (2026-10-06): the user has two active Wear OS closed-testing tracks. The “Closed testing Round 2” track shows release 10 (1.0.0), last updated Oct 2, 2026. The earlier “Closed Testing Gaura Mala” track shows release 4 (1.0.0), last updated Sep 30, 2026. The user had passed the initial 12 opted-in tester gate; production access still requires at least 12 testers opted in continuously for 14 days, then an application for production access. Confirm live tester counts and dates before advising; do not assume the 14-day period is complete.

Version 9 was built and installed on the user's Pixel Watch 5; the user confirmed its gesture and privacy-screen fixes work. Version 10 increases Daily Goal +/- controls to 48dp, raises small helper text to 10sp and the round label to 12sp, and permits settings copy to wrap. The version 10 debug build passed emulator layout checks at simulated 192dp and 227dp round sizes with reduced (0.85x) and enlarged (1.3x) system font scales. The current Settings implementation uses a regular `Column` with continuous touch scrolling and direct rotary scrolling; the user confirmed it feels smooth on the Pixel Watch 5.

Version 11 is a dependency maintenance release. `app/build.gradle.kts` now uses Wear Tiles 1.5.0 and Wear ProtoLayout 1.3.0. Gradle resolves `tiles-proto` 1.5.0 and ProtoLayout artifacts 1.3.0; these include the fixes for the Wear OS 5/API 34 Tile update `SecurityException` and protobuf CVE-2024-7254 warnings seen on release 4. The signed version 11 AAB built successfully on 2026-10-06. It has not been uploaded to Play Console or manually validated on a physical watch. The old Fragment 1.1.0 dependency warning was traced to wearable/Play Services transitive dependencies; the build now constrains Fragment to 1.9.1, uses Gradle 9.8.0, and uses Android Gradle Plugin 9.4.0. Verify dependency resolution and whether Play Console clears its notice on the next uploaded bundle.

Version 12's signed AAB was built on 2026-10-06; Play Console upload is not confirmed. The user confirmed its crown counting and picker haptics work on the Pixel Watch 5. Version 13 shortens on-watch confirmation, settings, summary, and privacy text. Its signed AAB and debug APK built successfully on 2026-10-06. An earlier v13 debug build was installed on the Pixel Watch 5 and the round-reset message was checked; reinstalling the final copy update is pending because the watch disconnected from ADB. Play Console upload is not confirmed. Its English, Hindi, and Marathi release notes are in `RELEASE_NOTES_v13.txt`.

Next steps:

1. Manually check the new round adjustment on the watch, including crown increments/decrements with selection haptics, crown counting after dismissing the picker, and a save with partial bead progress; then check smaller and larger system font sizes on the counter, settings, and privacy policy screens.
2. Upload `app/build/outputs/bundle/release/app-release.aab` (version code 13) to a Wear OS closed-testing track, then check whether Play Console clears the Tile and protobuf notices for that artifact.
3. Verify Tile updates on a Wear OS 5/API 34 device if available; also check the Tile and complication after the dependency update on the Pixel Watch 5.
4. Verify the Contact Developer handoff with the watch paired to the user's phone; the emulator only verified the no-phone fallback.
5. Keep at least 12 testers opted in for the full 14-day closed-test period, gather feedback, then complete the Play Console production-access application.
6. Continue checking the Play listing, privacy/data declarations, screenshots, and required store assets before production review.

## Workspace hygiene

Branding files are grouped under `assets/branding/`; generated visual references are under `assets/design-references/gemini-generated/`; Play Store artwork and listing screenshots are under `play-store/`; watch recordings and captures are under `test-artifacts/`. Review screenshots and test artifacts individually before staging. `testers.csv` contains tester email addresses; do not commit it. Avoid broad `git add .` while these user-owned files are present.
