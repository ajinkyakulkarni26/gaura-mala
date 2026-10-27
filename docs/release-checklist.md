# Wear OS release checklist

Use this checklist for each Google Play upload. Play publishing stays a deliberate maintainer action; CI does not upload or roll out an app automatically.

## Before building

- [ ] Confirm the app changes are merged to `main` and all required GitHub checks passed.
- [ ] Increment `versionCode` in `app/build.gradle.kts` and update `versionName` using the [Android versioning policy](versioning.md): minor for backward-compatible features, patch for backward-compatible bug/security fixes, and major for incompatible changes. Play will reject a reused code.
- [ ] Run unit tests, the pure-logic coverage gate, and Wear OS emulator tests. Check the latest emulator reports for both large and small round profiles.
- [ ] Confirm the CI integration suite's 48dp target check passes on both round sizes and font scales.
- [ ] Review Target Audience and Content for every age group the app is genuinely intended for. If children are included, complete the Google Play Families, Data safety, and SDK/API review before release; do not select adults-only solely to avoid child-safety requirements.
- [ ] Check behavior on a physical watch when a release changes crown input, double pinch, vibration, Tile, complication, ambient behavior, or watch-to-phone handoff.

## Physical-watch checklist

Run these checks on a compatible watch before a release that changes input, display, haptics, or power behavior. Record the watch model, Wear OS version, app version, and any failures with the release notes.

- [ ] **Crown:** Count several beads, stop and resume turning, complete a round, and verify crown input still works after closing Settings and the round picker.
- [ ] **Haptics:** Verify ordinary bead feedback, milestone feedback, round completion, and picker selection ticks. Turn haptics off and confirm the watch stays quiet for ordinary counting.
- [ ] **Auto Count:** Complete all three pace measurements; verify each countdown ends with a haptic cue, the middle duration is saved, and “Next chant” starts its cue directly. Confirm pace setup times only and never requests microphone permission. Confirm counting starts only after the user taps Start counting; check one bead/haptic per interval, pause/resume/stop, quarter and round boundaries, and no catch-up counts after a delayed resume. With Keep Awake off, let the watch dim past its normal timeout; confirm count and haptics continue and the ongoing activity offers a return path. Pause counting and confirm the ongoing activity is removed, the watch returns to its face after the normal timeout, and reopening Gaura Mala offers Resume. Keep Awake continues to honor the user's preference. Stop clears the session.
- [ ] **Voice guide:** Confirm the disclosure states what is recorded, purpose, local storage, non-transmission, and deletion before the microphone permission prompt; verify both Agree and Not now paths and permission denial. Record one chant, listen at multiple watch media volume levels, then re-record or continue. Verify the saved take remains until deleted, a valid replacement overwrites it, count advances at playback completion, and delete removes it. Confirm the dim display stays on the active session until stopped. If the user leaves the app, playback pauses. Do not record another person's voice unless you have permission to record and use it.
- [ ] **Double pinch:** On a watch that supports it, confirm one pinch counts once in active and dim/ambient modes. Confirm tap and crown still work and the help cue does not repeat after counting.
- [ ] **Ambient display:** Let the watch enter ambient mode. Confirm the displayed round and bead progress match the active screen, then count once by pinch if supported and confirm progress remains correct after waking.
- [ ] **Battery and inactivity:** With Keep Awake off, confirm the screen dims and the app follows normal Wear OS inactivity dismissal; reopen it and verify progress was saved. If Keep Awake changed, verify it holds the screen only while enabled and compare battery use during a short, recorded session.
- [ ] **Touch targets:** On the physical watch, confirm the counter, undo, settings, daily-goal controls, reset actions, and round-picker actions are easy to hit without touching the curved screen edge.

## Build and inspect

- [ ] Build the signed release AAB locally using the upload key stored outside the repository.
- [ ] Confirm CI builds the release AAB with an isolated temporary signing key and verifies its `arm64-v8a` native libraries and matching 64-bit variants.
- [ ] Check CI's native-symbol report. The release build requests `SYMBOL_TABLE`; CI validates symbol files if Gradle packages them and warns if they are absent. Some prebuilt native libraries may not provide symbols to package.
- [ ] Check the final AAB's package name, version code, Wear OS targeting, and signing certificate before upload.
- [ ] Review emulator screenshots from both round profiles. Update `play-store/` with current screenshots that show Auto Count pace setup, pause/resume, and optional voice-guide controls; verify text is legible and no user audio or personal data is present.

## Google Play Console

- [ ] Upload to the dedicated Wear OS testing track; this app bundle is not for phone-only tracks.
- [ ] Review Play Console's validation messages and confirm whether the native-symbol warning clears for the uploaded AAB.
- [ ] If Play Console still reports missing native debug symbols, record the warning and check whether the affected libraries provide symbol files; do not treat the CI warning as resolved until Play Console confirms it.
- [ ] Review release notes, screenshots, privacy policy, Data safety answers, tester list, and country availability.
- [ ] If the microphone feature is included, verify the in-app disclosure and hosted privacy policy are current and aligned; review microphone access, local-only audio storage, and Families disclosures in Play Console. Confirm the final release AAB's merged permissions and dependencies.
- [ ] If children are in the intended or declared audience, resolve the child voice recording and parental-consent assessment for applicable jurisdictions before rollout; a parent-supervision sentence alone does not implement consent.
- [ ] Start rollout only after the track and artifact are correct. For production access, confirm the account's current opted-in tester count and continuous testing period in Play Console before applying.

## After rollout

- [ ] Confirm testers can install the release from the Play opt-in link.
- [ ] Check crash and ANR reports after testers begin using the build.
- [ ] Record the uploaded version, track, date, artifact status, and outstanding Play warnings in `AGENTS.md`.
