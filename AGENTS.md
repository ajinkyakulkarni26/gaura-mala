# Gaura Mala Agent Notes

Last reviewed: 2026-10-30

## Project

Gaura Mala is a native Wear OS japa counter for the Hare Krishna maha-mantra. It is a watch-only app with a circular 108-bead progress ring, daily round goal (16 by default), haptics, a Tile, and a watch-face complication.

- Application ID: `com.gauramala.wear`
- Current Android source version: `versionCode 15`, `versionName 1.1.0` for the next feature release; it has not been uploaded to Play. Play release `versionCode 14` (`versionName 1.0.0`) was submitted to Production on 2026-10-16 and the user confirmed it was published on 2026-10-30.
- Main stack: Kotlin, Jetpack Compose for Wear OS Material 3, DataStore, Wearable Services
- Minimum SDK 30; target and compile SDK 37; JDK 17
- The app is declared standalone in `AndroidManifest.xml`; core counting does not require a phone app.
- A separate standalone Apple Watch implementation is being added under `watchos/`; it does not require an iPhone companion app.

## Source map

- `MainActivity.kt`: activity, screen-on preference, and Compose setup.
- `AutoCountOngoingActivity.kt`: low-priority notification that keeps running Auto Count reachable through ambient timeout and remains as “Paused · tap to return” while a session is paused; it is removed when counting is stopped.
- `presentation/MantraCounterViewModel.kt`: counter behavior, day rollover, debouncing, persistence coordination, and haptic dispatch.
- `presentation/MantraCounterStateMachine.kt`: pure counter transitions and undo history.
- `presentation/AutoChantPacer.kt`: validated user pace calibration and one-at-a-time timer scheduling.
- `presentation/CounterHapticPolicy.kt`: input-specific haptic selection.
- `presentation/RotaryBeadInput.kt`: crown delta accumulation and idle reset.
- `presentation/ui/MantraCounterScreen.kt`: counter screen, tap/crown/double-pinch input, and the one-time pinch cue.
- `presentation/ui/RoundProgressDialog.kt`: manual completed-round adjustment for rounds chanted with physical beads.
- `presentation/ui/BeadProgressRing.kt`: 108-bead circular progress UI.
- `presentation/ui/SettingsDialog.kt`: settings, resets, and privacy policy entry.
- `presentation/ui/AutoChantSetupScreen.kt`: pace calibration, optional recording disclosure, playback, and hands-free controls.
- `presentation/ui/PrivacyPolicyScreen.kt`: on-watch policy and Contact Developer action.
- `media/VoiceChantAudio.kt`, `media/VoiceChantFiles.kt`: optional microphone capture and playback, stored only in app-private no-backup files.
- `data/MantraPreferences.kt`: local DataStore preferences and daily progress.
- `haptics/HapticHelper.kt`: vibration patterns.
- `tile/GauraMalaTileService.kt`, `complication/GauraMalaComplicationService.kt`: Wear OS surfaces.
- `tile/GauraMalaTileContent.kt`: pure mapping from saved progress to Tile text.
- `watchos/GauraMala.xcodeproj/`: native standalone watchOS SwiftUI app project.
- `watchos/GauraMalaWatch/`: SwiftUI counter, round adjustment, settings, privacy view, local persistence, crown handling, and haptics.
- `watchos/GauraMalaWidget/`: WidgetKit accessory complication families and Smart Stack surface.
- `watchos/GauraMalaCore/`: Swift counter state machine and unit tests runnable on macOS.
- `app/src/test/`: unit tests for counter transitions, haptic policy, preference state, and rotary input.
- `app/src/androidTest/`: Wear OS UI and TileService integration tests for the counter, tap/rotary input, undo, round completion and adjustment, settings, reset flows, Tile progress, and daily rollover.
- `scripts/test-local.sh`: local pre-push checks with optional Wear OS emulator integration tests and screenshot collection; `scripts/print-android-test-results.py` prints individual case names, descriptions, and outcomes.

## Product decisions to preserve

- Inputs are full-screen tap, rotary crown, and system double-pinch where the watch advertises gesture detection. Keep tap/crown usable when gesture detection is unavailable.
- The system gesture indicator is deliberately shown at most once per counter-screen visit. Do not show the help cue after every tap, crown movement, or pinch, and do not let it shift the counter text layout.
- Crown movement is converted from accumulated rotary pixels to discrete bead steps. The accumulator resets after idle time to prevent counts continuing after crown rotation stops.
- Tap and crown use app haptics for bead progress and special milestone/round cues. Double-pinch relies on the system's ordinary gesture feedback while retaining the app's special milestone, round-completion, and daily-goal cues.
- Hands-free Auto Count uses a guided three-trial pace calibration and saves the middle duration to reduce the effect of one unusually fast or slow chant. Each trial has a visible 3-2-1 and a haptic start cue. It waits one interval before the first bead, then emits one ordinary bead haptic per chant plus the same milestone, round, and daily-goal cues as manual counting. Timer delays must never cause catch-up bursts. Pause/stop are always available, and ordinary manual inputs are disabled while auto count is running. Keep the display dim in ambient mode; do not use `FLAG_KEEP_SCREEN_ON` for Auto Count. Keep an Ongoing Activity while the session is running and retain its notification in a paused state after an in-app pause or when leaving the app. Pausing stops counting and voice playback and releases the Auto Count wake lock; tapping the paused notification returns to the app without resuming. Stopping cancels the notification. Ask for notification permission in context before starting.
- The optional voice guide is one user-recorded chant kept local-only. The user can play it, change watch media volume, record it again, or delete it. The completed recording increments the counter once per playback. Request microphone permission only after the on-screen disclosure and explicit user action. Keep saved audio under `noBackupFilesDir`; never import, upload, share, or back up user recordings. Stop playback and discard unfinished recordings when the app leaves the foreground.
- The recording disclosure must tell users to record only a voice they have permission to record and use. Do not market the feature as a way to capture or replay famous people's recordings; do not bundle third-party chant audio.
- Milestones are beads 27, 54, and 81; one round completes at 108. The daily-goal completion cue takes priority when the last bead also reaches the daily goal.
- The counter's round label opens a manual adjustment for completed rounds today, so users can reconcile progress after chanting with physical beads. The picker supports plus/minus buttons and crown rotation, with a subtle selection tick when haptics are enabled; use a short title and check/cross actions to fit the round display. Saving starts the next watch round at bead 0, clears the previous bead's undo history, and does not play chanting haptics or show the goal celebration.
- Reset confirmation actions use accessible check/cross icon buttons to avoid clipped labels on round displays.
- When a modal screen takes focus for crown input, restore focus to the counter after it closes so crown counting continues without requiring a tap.
- The ambient counter header shows the current round (`completedRounds + 1`) to match the active display, and shows `Goal` when the daily goal is complete.
- Keep Awake is optional and defaults off. The double-pinch path and Auto Count haptics work in ambient mode; do not force the display to remain fully awake to make counting work. A running Auto Count uses an Ongoing Activity while preserving the dim display. A paused session removes that notification and follows the watch's normal inactivity timeout when Keep Awake is off; it can be resumed after reopening the app.
- Preserve the current centered round bead ring. Earlier circle redesigns looked disoriented to the user; the requested adjustment was text placement, not a different ring.
- Wear OS screens must adapt to round displays with different usable diameters and system font scales. Let labels wrap where needed, keep important controls within the curved safe area, and preserve at least 48dp touch targets for primary actions.
- The Settings list is a short, regular `Column` with continuous `verticalScroll` and `rotaryScrollable` attached to the same scroll state. The user confirmed this feels smooth on the Pixel Watch 5. Keep this implementation; `TransformingLazyColumn` free scrolling felt rough, and its snap-and-fling experiment felt slow and stepped like an elevator.
- Wrist-flick dismissal is standard Wear OS behavior. Do not try to intercept system navigation to keep the app open.
- Progress, preferences, and an optional chant recording are stored locally on the watch. Counts roll over on the local calendar date; preferences and any recording remain until cleared or uninstalled. The app does not send this data to a server, and Android backup is disabled. Do not describe the app as storing no data at all.
- The launcher uses a black adaptive-icon background and a bead-mala foreground with no text. Keep the artwork centered and sized to avoid clipping in the circular mask.
- Contact Developer opens the GitHub Issues page on the paired phone through `RemoteActivityHelper`. If the phone is unavailable, the policy screen displays the URL. There is no email sent automatically and no phone companion app is required.
- The Apple Watch port is standalone. It uses the primary hand-gesture shortcut for Double Tap where watchOS and hardware support it; tap and Digital Crown remain available. The Digital Crown and haptics use native SwiftUI/WatchKit APIs.
- Keep the Apple Watch implementation in `watchos/` and its build/test workflow separate from Android. Core behavior is ported to Swift and is not source-shared with Kotlin; keep transition behavior aligned with the Android app and add tests for every changed rule.

## UI conventions

- Treat peer choices equally: use the same size, visual weight, color treatment, and spacing unless one action is clearly primary or destructive. Do not preselect or visually favor one setup option without a product reason.
- Keep a consistent gap of about 10dp between a page heading and its first action. Center labels inside pill buttons; center sparse page content on round screens.
- Keep primary touch targets at least 48dp and provide clear TalkBack labels for icon-only controls. Use the centered bottom arrow consistently for back/cancel; make the accessible and visible meaning clear when it is also the opt-out action.
- Prefer one obvious action per step and short, child-readable instructions. When an action changes state, show the next cue or the matching stop control in place rather than adding a redundant confirmation screen.
- Pace calibration uses a timer control, guides the user through three timed chants, and records no audio. Voice recording is a separate optional flow for one short chant. Explain its purpose, local storage, non-transmission, and deletion before requesting microphone permission, and require an affirmative user action.
- See `docs/ui-guidelines.md` for the fuller checklist and `docs/privacy-and-audio-review.md` for the current microphone/child-privacy assessment.

## Privacy and store listing

- The on-watch policy is implemented in `PrivacyPolicyScreen.kt`; its sections are stacked to fit a round display.
- The hosted policy is `https://ajinkyakulkarni26.github.io/gaura-mala/privacy-policy.html`.
- The app requests vibration access and requests microphone permission only when the user opts into recording a chant. Update the in-app policy, hosted policy, and Play Console Data safety form together when audio behavior changes. The voice recording is local-only, non-backed-up, and user-deletable. The app has no account, ads, analytics, or server-side user data collection.
- The hosted and in-app policies must describe local voice storage accurately; do not say the app stores no personal information. Recheck Google Play Target Audience, Families, Data safety, and SDK disclosures before release. See `docs/privacy-and-audio-review.md`; this code review is not a legal compliance determination.
- Google Play requires a dedicated Wear OS track for this watch bundle. Do not upload the Wear OS-only AAB to a phone-oriented track.

## Build, test, and install

From the repository root:

```bash
./gradlew :app:assembleDebug
./gradlew :app:test
./gradlew :app:verifyCoreLogicCoverage
./gradlew :app:connectedDebugAndroidTest
./scripts/test-local.sh
./scripts/lint-workflows.sh
./gradlew :app:bundleRelease :app:assembleRelease
swift test --package-path watchos/GauraMalaCore
xcodebuild -project watchos/GauraMala.xcodeproj -scheme GauraMalaWatch -sdk watchsimulator -destination 'generic/platform=watchOS Simulator' CODE_SIGNING_ALLOWED=NO build
```

Maintain 100% JaCoCo **line coverage** across the Android-independent core files: `MantraCounterStateMachine.kt`, `AutoChantPacer.kt`, `CounterHapticPolicy.kt`, `RotaryBeadInput.kt`, `MantraUiState.kt`, and `GauraMalaTileContent.kt`. `:app:verifyCoreLogicCoverage` is the CI gate; add or update unit tests whenever these behaviors change, and do not lower the threshold or exclude core files just to make the build pass. The complete unit-test coverage report is generated at `app/build/reports/coverage/test/debug/index.html`. Compose screens and Android services are tested through Wear OS integration/device tests but are not included in this JVM coverage percentage. CI compares executed Wear OS integration test IDs with `config/wear-os-integration-tests.txt`; every registered case must run, and every new case must be added to that inventory. There is no fixed test-count threshold.

The counter screen renders before its asynchronous DataStore preferences load finishes. UI tests must wait for the loaded state before injecting input; `CounterInputIntegrationTest` seeds a visible round marker, waits until it appears, then clears it and waits for the clean baseline. Do not use the counter surface becoming visible as proof that preferences are ready, or increase timeouts to hide this startup race.

Build outputs:

- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release APK: `app/build/outputs/apk/release/app-release.apk`
- Play upload bundle: `app/build/outputs/bundle/release/app-release.aab`

The debug application ID is `com.gauramala.wear.debug`. The release application ID is `com.gauramala.wear`. Follow the Android versioning policy in `docs/versioning.md`: use Semantic Versioning for `versionName` and strictly increase `versionCode` for every Play upload; Play does not allow reusing an uploaded version code. Release signing reads Gradle properties `GAURA_UPLOAD_STORE_FILE`, `GAURA_UPLOAD_STORE_PASSWORD`, `GAURA_UPLOAD_KEY_ALIAS`, and `GAURA_UPLOAD_KEY_PASSWORD`. Never commit the keystore or signing credentials.

For a connected watch, find its ADB serial with `adb devices -l`, then install and launch the release APK:

```bash
adb -s <watch-serial> install -r app/build/outputs/apk/release/app-release.apk
adb -s <watch-serial> shell am start -n com.gauramala.wear/.MainActivity
```

Use a compatible physical Pixel Watch to verify double-pinch and real haptics. Generic Wear OS emulators may not expose gesture hardware or realistic vibration. Emulator testing can still check layout, navigation, and the no-phone Contact Developer fallback.

Run `./scripts/test-local.sh` before pushing. It tests repository policy scripts, runs unit tests, the 100% core-logic coverage gate, builds the debug app, and checks that `arm64-v8a` native libraries are packaged. If actionlint and shellcheck are installed, it also lints workflows and shell scripts. When a Wear OS emulator is running, it executes the UI integration suite and prints each test case's outcome; it explicitly targets the emulator so connected physical watches are not run as instrumentation devices. It saves UI review screenshots under `test-artifacts/emulator-screenshots/`. GitHub Actions runs the same tests on a large round API 35 emulator at default font scale and a small round emulator at 1.3x font scale, and uploads screenshots with reports from each profile. Tile integration cases request the real `GauraMalaTileService` response and check saved progress, its activity action, and local-day reset; visual Tile carousel rendering still needs a physical watch check. `.github/workflows/android-ci.yml` also uses Gradle Enhanced Caching and publishes Gradle Build Scans for build diagnostics; scans are not vulnerability scans. CodeQL and Dependency Review run in the required Android CI check. The report inventory compares executed test IDs with `config/wear-os-integration-tests.txt`; every case must be registered. Compose rotary injection checks the app's rotary event path, but it does not verify physical crown hardware, double-pinch detection, or real haptics. Keep those checks in the physical-watch release checklist.

Both local and CI Wear OS emulator test setup temporarily calls `adb shell dumpsys battery unplug` before instrumentation, then restores the battery state after the run. The CI runner restarts ADB, reconnects, and retries instrumentation once if ADB reports that the device went offline or disappeared, clearing partial reports first. The Gradle task may still exit successfully after the Android test runner loses its device, so the recovery also checks the captured ADB log. The AVD may start connected to AC and show the system charging activity after boot; that activity pauses the app and can invalidate Compose UI tests.

The emulator suite checks that the counter, round adjustment, Settings, and primary actions have at least 48dp semantic touch targets on both round profiles. Compose UI tests save review screenshots for the counter and the Auto Count pace and voice flows; CI uploads them with each profile's report artifact, and local runs save them under the ignored `test-artifacts/emulator-screenshots/` directory. Refresh the Play Store screenshots in `play-store/` from the large-round images after reviewing the feature screens. CI builds a release AAB with an isolated temporary signing key and validates required 64-bit native ABIs. `scripts/check_release_aab.py` validates embedded native debug symbols when present and reports their absence as a warning; the Play Console upload check remains authoritative. CI never uses the Play upload key or publishes the resulting artifact. The bug-report issue form requires the app version, watch model, Wear OS version, and input method or affected surface. See `docs/release-checklist.md#physical-watch-checklist` for the manual crown, gesture, haptic, ambient, touch, and inactivity checks.

Security gates are part of the repository CI. The existing required Android CI job runs CodeQL (`java-kotlin`, `security-extended`) and reviews pull-request dependency changes, failing for newly introduced moderate-or-higher vulnerabilities. A read-only pull-request workflow generates Gradle dependency snapshots; a trusted `workflow_run` workflow submits those snapshots, and pushes to `main` submit the current Gradle graph for Dependabot alerts. `.github/dependabot.yml` schedules updates for root Gradle files, app dependencies, and GitHub Actions. Minor/patch updates are grouped per ecosystem and directory; major and security updates remain separately reviewable. Workflow actions are pinned to verified full commit SHAs with release tags in comments. CI validates workflows with actionlint and shellcheck. Keep workflow permissions narrow; never give write permissions to a workflow that executes untrusted pull-request code.

`SECURITY.md` directs vulnerability reports to GitHub's private advisory flow, and `.github/ISSUE_TEMPLATE/` contains structured bug and feature forms. GitHub private vulnerability reporting is enabled in repository settings as of 2026-10-07.

As of 2026-10-07, GitHub's dependency graph, Dependabot alerts, and Dependabot security updates are enabled under **Settings → Security and quality → Code security and analysis**. The active `main` ruleset requires CodeQL results at **High or higher** for security alerts and blocks CodeQL error alerts; it also requires CI status checks and pull requests. Secret scanning and push protection were already enabled and remain on. Automatic dependency submission is disabled because repository workflows submit Gradle dependency snapshots. Dependency Review runs inside the existing Android CI required check. CodeQL analysis reports findings; the ruleset is what blocks merges at the configured severity. Scans reduce risk but do not prove the app is entirely secure.

The repository's `main` branch is protected: changes go through pull requests, and both Android CI jobs plus the contributor-approval job must pass before merge. Contributor PRs require approval from `ajinkyakulkarni26` on the current commit; owner-authored PRs are exempt from that approval check. New commits after approval require a fresh owner approval. `.github/CODEOWNERS` assigns the owner to CI workflows, coverage/build configuration, Gradle policy, and scripts; those files require an additional code-owner review even on owner-authored PRs. The main ruleset also requires CodeQL results at High or higher and blocks CodeQL error alerts. Update the GitHub ruleset's required checks and code-owner review setting when changing this policy. Do not push directly to `main`.

The Apple Watch project targets watchOS 10 or later and uses app bundle ID `com.gauramala.watchos.watchkitapp` and widget extension ID `com.gauramala.watchos.watchkitapp.widget`. The WidgetKit extension provides accessory complication families and reads the app's progress from the `group.com.gauramala.watchos` App Group; register this App Group with the Apple Developer account before distribution signing. The Swift core test suite runs with `swift test --package-path watchos/GauraMalaCore`; a generic simulator build uses the `xcodebuild` command above. Xcode and the watchOS SDK are required; a watchOS simulator runtime is needed to boot the simulator. Apple Watch UI automation, App Store assets, TestFlight setup, and release signing are not yet configured. Verify real Digital Crown feel, Taptic feedback, Double Tap, and screen-size/font behavior on a physical watch before release. The new watchOS workflow runs Swift CodeQL, counter tests, and a simulator build on pushes and pull requests; add its status check to the `main` ruleset after its first successful GitHub run. Keep Apple App Store versioning independent from Android Play version codes.

Before starting any development or edits, fetch and inspect the latest `origin/main` so work is based on current repository code. This repository's default branch is `main` (not `master`). Create new work branches from the updated `origin/main`; when continuing an existing topic branch, incorporate the latest `main` first when it can be done safely. Preserve uncommitted user changes and never use a destructive reset just to synchronize.

The contributor-approval workflow checks out the approval script from the target branch, so a PR cannot change the checker it is being evaluated by. During the initial policy bootstrap only, when that script is not on `main` yet, the workflow permits the repository owner to land the setup PR and rejects other authors.

## Release status and next work

The user confirmed on 2026-10-30 that Play release `versionCode 14` (`versionName 1.0.0`) was published. The source is set to `versionCode 15`, `versionName 1.1.0` for the next feature release; this build has not been uploaded to Play. Confirm the live Play Console state before release advice.

Version 9 was built and installed on the user's Pixel Watch 5; the user confirmed its gesture and privacy-screen fixes work. Version 10 increases Daily Goal +/- controls to 48dp, raises small helper text to 10sp and the round label to 12sp, and permits settings copy to wrap. The version 10 debug build passed emulator layout checks at simulated 192dp and 227dp round sizes with reduced (0.85x) and enlarged (1.3x) system font scales. The current Settings implementation uses a regular `Column` with continuous touch scrolling and direct rotary scrolling; the user confirmed it feels smooth on the Pixel Watch 5.

Version 11 is a dependency maintenance release. `app/build.gradle.kts` now uses Wear Tiles 1.5.0 and Wear ProtoLayout 1.3.0. Gradle resolves `tiles-proto` 1.5.0 and ProtoLayout artifacts 1.3.0; these include the fixes for the Wear OS 5/API 34 Tile update `SecurityException` and protobuf CVE-2024-7254 warnings seen on release 4. The signed version 11 AAB built successfully on 2026-10-06. It has not been uploaded to Play Console or manually validated on a physical watch. The old Fragment 1.1.0 dependency warning was traced to wearable/Play Services transitive dependencies; the build now constrains Fragment to 1.9.1, uses Gradle 9.8.0, and uses Android Gradle Plugin 9.4.0. Verify dependency resolution and whether Play Console clears its notice on the next uploaded bundle.

Version 12's signed AAB was built on 2026-10-06; Play Console upload is not confirmed. The user confirmed its crown counting and picker haptics work on the Pixel Watch 5. Version 13 shortens on-watch confirmation, settings, summary, and privacy text. Its signed AAB and debug APK built successfully on 2026-10-06. An earlier v13 debug build was installed on the Pixel Watch 5 and the round-reset message was checked; reinstalling the final copy update is pending because the watch disconnected from ADB. Play Console upload is not confirmed. Its English, Hindi, and Marathi release notes are in `RELEASE_NOTES_v13.txt`.

The user confirmed that version code 14 was submitted to Production on 2026-10-16 and published on 2026-10-30. The next source version is code 15 / name 1.1.0, reserved for the new feature set and not yet uploaded. Keep the version 14 public privacy policy in `docs/privacy-policy.html` in place while version 14 is the latest production release. The version 15 policy draft is `release-materials/privacy-policy-v15-draft.html`; publish it to the hosted policy URL with the version 15 rollout. Verify the Play Console state and complete the release checklist before upload.

Release readiness checked on 2026-10-30:

- Play Console dashboard shows version 14 as the latest Production Wear OS release at 100% rollout, with no unpublished changes.
- `./scripts/test-local.sh` passed on the large and small round API 35 ARM emulators: all 27 registered integration cases passed on each profile, JVM tests passed, required core line coverage remained 100%, and the debug build packaged `arm64-v8a` libraries. The refreshed local screenshots are under ignored `test-artifacts/emulator-screenshots/local-20261030-*`; the existing version 15 Play screenshot set was reviewed.
- The user confirmed hands-on Wear OS testing of the merged Auto Count notification and pause behavior was complete on 2026-10-30. Keep the detailed physical-watch checklist for any scenarios not covered during that session.
- The live Play listing has an Everyone content rating and its Data safety section says “Committed to follow the Play Families Policy.” The last supplied Play Console review notes show child age groups selected; recheck the exact target-age selection before v15. The optional voice guide remains local-only, but the child-audience and retained-child-voice assessment remains open; confirm that the declared audience matches the intended product and resolve this issue before rollout.
- Keep the version 14 hosted privacy policy in place until version 15 is ready. Before rollout, publish `release-materials/privacy-policy-v15-draft.html` and reconcile it with the in-app disclosure, Play Console Data safety, Target Audience and Content, Families, and SDK declarations. Add the story-page URL to the store description and Website contact field.
- Release notes for version 15 are drafted in `RELEASE_NOTES_v15.txt`. A signed version 15 AAB was rebuilt locally on 2026-10-30 from merged commit `464b4ee`; it is at `app/build/outputs/bundle/release/app-release.aab` and has not been uploaded. Rebuild if source changes, then archive the uploaded artifacts after release.

## Workspace hygiene

Branding files are grouped under `assets/branding/`; generated visual references are under `assets/design-references/gemini-generated/`; Play Store artwork and listing screenshots are under `play-store/`; watch recordings and captures are under `test-artifacts/`. Review screenshots and test artifacts individually before staging. `testers.csv` contains tester email addresses; do not commit it. Avoid broad `git add .` while these user-owned files are present.
