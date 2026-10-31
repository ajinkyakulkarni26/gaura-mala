# Microphone and child-privacy review

**Reviewed:** October 30, 2026

**Scope:** Android/Wear OS source and the Google Play and U.S. COPPA guidance linked below. This is a technical policy review, not a legal opinion or Play Console verification.

## Assessment

The optional recording path has good local privacy controls: it is started by the user, requires Android microphone permission, uses app-private storage excluded from Android backup, limits the capture to 20 seconds, replaces an earlier saved take only after a valid new take is ready, and provides an in-app delete action. The Android source manifest does not declare `INTERNET`, and the recording code has no upload path. The selected audio remains on the watch until the user deletes it, clears app storage, or uninstalls the app.

The simplified UI records one voice take. Before the microphone permission request, it discloses the purpose, up-to-20-second duration, private on-watch storage, non-upload/non-sharing by Gaura Mala, and deletion. It also tells users that a parent or guardian should decide and help with a child's recording and deletion. The user must tap “Agree & begin”; the bottom “Not now” action cancels. The user can listen, adjust watch media volume, re-record, or proceed. Pace calibration uses a timer icon and explicitly says that it times the chant without recording audio.

**Current status: technically privacy-conscious, but not fully cleared for an all-ages release that includes retained voice recordings.** Play Console screenshots dated October 16, 2026 show target ages 6–8, 9–12, 13–15, 16–17, and 18+, a commitment to the Families policy, no data collection/sharing, and the statement “Data isn’t encrypted.” The user confirmed on October 30 that children remain part of the intended audience. Families requirements therefore apply; recheck the exact age groups and full policy answers before v15. The recording disclosure includes parent/guardian guidance, but it does not verify a parent's identity or establish any legally required parental consent. The final release artifact's merged manifest and SDK behavior, the deployed privacy-policy URL, and the legal treatment of child voice recordings have not been verified here. Do not treat the code review alone as proof of compliance.

The user confirmed that Release 14 was published on 2026-10-30. Version 15 (1.1.0) and related store-listing changes were then submitted for Play review with Managed publishing enabled. The hosted policy had been restored to v14 before submission, so this update makes both `docs/privacy-policy.html` and `release-materials/privacy-policy-v15-draft.html` a versioned policy: it describes v14's current behavior and says the v15 recording practices take effect when v15 is released. The public URL must be checked after the GitHub Pages deployment and before publishing v15. This policy update does not resolve the separate child-audience and retained-child-voice assessment.

## What the source does

- `AndroidManifest.xml` declares `RECORD_AUDIO`; the app requests it only when the user starts “Record a chant.”
- `VoiceChantAudio` captures AAC audio from the microphone at 22.05 kHz and stops each capture at 20 seconds. The visible recording state is labeled “Recording”; Android also provides its system microphone indicator.
- `VoiceChantFiles` writes into `noBackupFilesDir`. One selected recording is kept. A new valid take replaces it; an interrupted or invalid take is discarded, preserving the previous recording.
- `allowBackup=false` is set in the application manifest. The user can delete the selected take from Auto Count settings; uninstalling the app removes app-private files.
- The code does not encrypt the recording separately from Android's app-private storage. Do not describe this as end-to-end encrypted or promise secure forensic erasure.
- No upload code or `INTERNET` permission appears in the source manifest. The build's final merged release manifest and all SDK data practices still need review before release.

## Policy reading

The supplied Google Play link (`answer/6223646`) covers requirements for distributing apps in specific countries or regions; it is not the microphone-recording policy. The more relevant Play requirements are:

- Google treats microphone data as personal and sensitive user data. Where access may not be reasonably expected, its User Data policy calls for clear in-app disclosure and consent before permission/access. Its best-practice guide says to show the disclosure immediately before the permission, explain why/what/how, require affirmative consent, and provide a way to decline. The current revised screen follows that pattern; verify it on the watch after building.
- Google defines Data Safety “collection” as transmitting data off the user's device; on-device processing that is never sent off-device does not need to be declared as collected. On that definition, the current “No data collected” answer is consistent with the local-only voice guide, provided the final release manifest, SDKs, and runtime behavior confirm that no audio or other declared user data is sent off-device. This is separate from Families requirements, which apply when children are in the target audience, and the User Data policy, which treats microphone access as sensitive and still requires the in-flow disclosure and affirmative action already implemented.
- If children are among the declared target audience, the Families policy applies. Do not choose an adults-only audience simply to avoid the policy; accurately declare who the app is for and review all APIs/SDKs used in the final bundle.
- The FTC says a child's voice in an audio file is personal information when collected. Its narrow voice-recording enforcement statement is for audio used only as a short-lived substitute for text to fulfill a request, then deleted immediately. Gaura Mala retains a selected recording for later chant playback, so that exception is not a fit for the feature's intended use.
- FTC guidance also says merely interacting with information that stays on the device and is never transmitted is not COPPA collection. That local-only distinction is relevant here, but it does not settle every wearable recording scenario or laws outside the U.S. The app's intended audience, service context, and actual network/SDK behavior should be assessed with counsel before relying on it.

## Open issues before an all-ages release with child access

1. **Target audience and Families declaration:** Play Console screenshots show ages 6–8 and 9–12 selected alongside older groups, plus the Families commitment. The user confirmed that children remain included. Recheck the exact selected ages, content-rating answers, and all Play Console responses for v15; the declared audience must reflect the actual product and marketing.
2. **Child voice and consent assessment:** obtain a jurisdiction-specific answer on whether local capture/storage is covered by COPPA or other child privacy laws for this product. If prior parent consent or a neutral age screen is required, add a compliant flow before enabling recording for children. A parent-supervision sentence is not itself verifiable parental consent.
3. **SDK and artifact audit:** inspect the final release AAB's merged permissions and dependency/SDK inventory; confirm no component transmits audio or other child data. Revisit this after any dependency or manifest change.
4. **Retention choice:** the selected recording stays until the user deletes it, clears app storage, or uninstalls the app. Decide whether that user-controlled retention is acceptable for the intended child audience and applicable law; document the policy and keep deletion easy to find.
5. **Policy deployment:** publish the versioned policy from `docs/privacy-policy.html` through GitHub Pages and verify the public Play Console privacy-policy URL shows both the live v14 behavior and the v15 recording disclosures with the correct effective timing. Keep it aligned with the v15 in-app policy and Play Console Data safety answers. Google Play requires accurate Data safety answers and a valid policy, while the app's in-flow disclosure and affirmative consent remain necessary before microphone access.
6. **Device verification:** on a watch, verify disclosure before the Android prompt, “Not now” cancellation, permission denial/retry, recording indicator, stop action, 20-second limit, listen/volume controls, re-record and delete behavior, and that pace-only timing never requests the microphone.

## References

- [Google Play User Data policy](https://support.google.com/googleplay/android-developer/answer/10144311?hl=en)
- [Google Play disclosure and consent best practices](https://support.google.com/googleplay/android-developer/answer/11150561?hl=en)
- [Google Play Families policy](https://support.google.com/googleplay/android-developer/answer/9893335?hl=en)
- [Google Play Data Safety guidance](https://support.google.com/googleplay/android-developer/answer/10787469?hl=en)
- [FTC COPPA FAQs, including local-only and voice recording guidance](https://www.ftc.gov/business-guidance/resources/complying-coppa-frequently-asked-questions)
- [FTC voice-recording enforcement policy statement](https://www.ftc.gov/system/files/documents/public_statements/1266473/coppa_policy_statement_audiorecordings.pdf)
- [The supplied Google Play country/region requirements page](https://support.google.com/googleplay/android-developer/answer/6223646)
