# Wear OS release checklist

Use this checklist for each Google Play upload. Play publishing stays a deliberate maintainer action; CI does not upload or roll out an app automatically.

## Before building

- [ ] Confirm the app changes are merged to `main` and all required GitHub checks passed.
- [ ] Increment `versionCode` in `app/build.gradle.kts`; Play will reject a reused code.
- [ ] Run unit tests, the pure-logic coverage gate, and Wear OS emulator tests. Check the latest emulator reports for both large and small round profiles.
- [ ] Check behavior on a physical watch when a release changes crown input, double pinch, vibration, Tile, complication, ambient behavior, or watch-to-phone handoff.

## Build and inspect

- [ ] Build the signed release AAB locally using the upload key stored outside the repository.
- [ ] Confirm the AAB contains a 64-bit native ABI. CI checks for `arm64-v8a` native libraries in the Android build output.
- [ ] Confirm the build creates `app/build/outputs/native-debug-symbols/release/native-debug-symbols.zip` and upload it with the AAB when Play Console requests native symbols.
- [ ] Check the final AAB's package name, version code, Wear OS targeting, and signing certificate before upload.

## Google Play Console

- [ ] Upload to the dedicated Wear OS testing track; this app bundle is not for phone-only tracks.
- [ ] Review Play Console's validation messages. Warnings about symbols can be cleared by attaching the symbol ZIP; confirm rather than assuming they cleared.
- [ ] Review release notes, screenshots, privacy policy, Data safety answers, tester list, and country availability.
- [ ] Start rollout only after the track and artifact are correct. For production access, confirm the account's current opted-in tester count and continuous testing period in Play Console before applying.

## After rollout

- [ ] Confirm testers can install the release from the Play opt-in link.
- [ ] Check crash and ANR reports after testers begin using the build.
- [ ] Record the uploaded version, track, date, artifact status, and outstanding Play warnings in `AGENTS.md`.
