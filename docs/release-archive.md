# Android release archive

Keep a reproducible record of every Android build uploaded to Google Play. Play Console remains the source of truth for rollout status and the APKs Google generated for delivery; keep an independent, access-controlled archive so the release artifact and its source provenance are easy to recover later.

## Archive for each Play upload

Save these items together under a folder named for both the user-facing version and Play version code, such as `1.0.0+14`:

- The exact uploaded AAB, if it can be downloaded from Play Console, or the original AAB retained at upload time.
- The Play-signed universal APK from App Bundle Explorer. Save device-specific APKs only when needed for support or testing.
- The source commit hash used to build the release, plus an annotated Git tag pointing to that exact commit.
- The final release notes and the Play track, upload date, rollout date, and rollout status.
- The R8/ProGuard mapping file and native debug symbols when the release uses them.
- A SHA-256 checksum manifest for each archived binary and symbol file.

Store the binary archive in private, backed-up storage. Do not add AABs or APKs to the source repository or include upload keys, signing keys, or credentials in the archive. A public GitHub release is appropriate only if the project intentionally wants to distribute those binaries outside Google Play.

## Recovering the published v14 build

The current local release output is for the newer source version, not the published v14 release. Do not rename or rebuild that output as v14. In Play Console, open **Test and release → Latest releases and bundles → All app versions**, select version code 14, and use its **Downloads** tab to retrieve the uploaded bundle/APK and Play-generated signed APKs that are available. Google documents that the App Bundle Explorer retains uploaded APKs/bundles and generated APKs in the Downloads tab: [Play Console FAQ](https://support.google.com/googleplay/android-developer/answer/10246389?hl=en-EN), [Inspect app versions](https://support.google.com/googleplay/android-developer/answer/9844279).

Save the recovered files in private storage, inspect their package name and version code, and record the v14 source commit if it can be confirmed from the build/release record. Only create a `android-v1.0.0-14` tag after identifying that exact source commit; do not tag the current branch as v14.

## Release handoff

After rollout, update the release record in `AGENTS.md` and the version sequence in `versioning.md`. Record where the archive is stored, then verify that the files can be retrieved from the backup location.
