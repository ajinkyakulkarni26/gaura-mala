# Wear OS release checklist

Use this checklist for each Google Play upload. Play publishing stays a deliberate maintainer action; CI does not upload or roll out an app automatically.

## Before building

- [ ] Confirm the app changes are merged to `main` and all required GitHub checks passed.
- [ ] Increment `versionCode` in `app/build.gradle.kts`; Play will reject a reused code.
- [ ] Run unit tests, the pure-logic coverage gate, and Wear OS emulator tests. Check the latest emulator reports for both large and small round profiles.
- [ ] Confirm the CI integration suite's 48dp target check passes on both round sizes and font scales.
- [ ] Check behavior on a physical watch when a release changes crown input, double pinch, vibration, Tile, complication, ambient behavior, or watch-to-phone handoff.

## Physical-watch checklist

Run these checks on a compatible watch before a release that changes input, display, haptics, or power behavior. Record the watch model, Wear OS version, app version, and any failures with the release notes.

- [ ] **Crown:** Count several beads, stop and resume turning, complete a round, and verify crown input still works after closing Settings and the round picker.
- [ ] **Haptics:** Verify ordinary bead feedback, milestone feedback, round completion, and picker selection ticks. Turn haptics off and confirm the watch stays quiet for ordinary counting.
- [ ] **Double pinch:** On a watch that supports it, confirm one pinch counts once in active and dim/ambient modes. Confirm tap and crown still work and the help cue does not repeat after counting.
- [ ] **Ambient display:** Let the watch enter ambient mode. Confirm the displayed round and bead progress match the active screen, then count once by pinch if supported and confirm progress remains correct after waking.
- [ ] **Battery and inactivity:** With Keep Awake off, confirm the screen dims and the app follows normal Wear OS inactivity dismissal; reopen it and verify progress was saved. If Keep Awake changed, verify it holds the screen only while enabled and compare battery use during a short, recorded session.
- [ ] **Touch targets:** On the physical watch, confirm the counter, undo, settings, daily-goal controls, reset actions, and round-picker actions are easy to hit without touching the curved screen edge.

## Build and inspect

- [ ] Build the signed release AAB locally using the upload key stored outside the repository.
- [ ] Confirm CI builds the release AAB with an isolated temporary signing key and verifies its `arm64-v8a` native libraries and matching 64-bit variants.
- [ ] Confirm CI finds native debug symbol metadata embedded in the AAB under `BUNDLE-METADATA/com.android.tools.build.debugsymbols/`. The uploadable AAB should carry these symbols into Play Console.
- [ ] Check the final AAB's package name, version code, Wear OS targeting, and signing certificate before upload.

## Google Play Console

- [ ] Upload to the dedicated Wear OS testing track; this app bundle is not for phone-only tracks.
- [ ] Review Play Console's validation messages and confirm whether the native-symbol warning clears for the uploaded AAB.
- [ ] Review release notes, screenshots, privacy policy, Data safety answers, tester list, and country availability.
- [ ] Start rollout only after the track and artifact are correct. For production access, confirm the account's current opted-in tester count and continuous testing period in Play Console before applying.

## After rollout

- [ ] Confirm testers can install the release from the Play opt-in link.
- [ ] Check crash and ANR reports after testers begin using the build.
- [ ] Record the uploaded version, track, date, artifact status, and outstanding Play warnings in `AGENTS.md`.
