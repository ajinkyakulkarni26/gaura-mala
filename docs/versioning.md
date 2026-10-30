# Android app versioning

Use [Semantic Versioning 2.0.0](https://semver.org/) for Android `versionName` and a separate, monotonically increasing integer for Android `versionCode`.

## `versionName`

Use the `MAJOR.MINOR.PATCH` format, for example `1.1.0`.

- **MAJOR**: an incompatible change to the app's user experience, saved-data format, or behavior that requires users to adapt or data to be migrated.
- **MINOR**: one or more backward-compatible user-facing features. Example: move from `1.0.0` to `1.1.0` for the Auto Count feature release.
- **PATCH**: backward-compatible bug fixes, security fixes, or maintenance updates without a new user-facing feature. Example: move from `1.1.0` to `1.1.1` for a fix-only release.

When a release contains changes of more than one type, use the highest applicable level: a feature release with fixes gets a MINOR bump; an incompatible change gets a MAJOR bump. Treat security fixes as PATCH changes when they do not also include a feature or incompatible change.

## `versionCode`

- Increase `versionCode` for every new Android App Bundle uploaded to any Play track. Never reuse a code already uploaded for this application ID.
- Keep it independent from `versionName`; it is an Android/Play ordering value and is not the version label users normally see.
- Check both values in `app/build.gradle.kts` and in the final AAB before upload.

## Current release sequence

- Play release 14: `versionCode 14`, `versionName 1.0.0`.
- Next feature release: `versionCode 15`, `versionName 1.1.0`.

Update this sequence when a release is actually uploaded, and keep the current Play review/rollout status in `AGENTS.md` based on the latest confirmed Play Console information.
