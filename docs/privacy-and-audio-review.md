# Microphone and child-privacy review

**Reviewed:** October 26, 2026

**Scope:** Android/Wear OS source and the Google Play and U.S. COPPA guidance linked below. This is a technical policy review, not a legal opinion or Play Console verification.

## Assessment

The optional recording path has good local privacy controls: it is started by the user, requires Android microphone permission, uses app-private storage excluded from Android backup, limits the capture to 20 seconds, replaces an earlier saved take only after a valid new take is ready, and provides an in-app delete action. The Android source manifest does not declare `INTERNET`, and the recording code has no upload path. The selected audio remains on the watch until the user deletes it or uninstalls the app.

The simplified UI records one voice take. It discloses the purpose as a chant guide, local storage, non-transmission, and deletion before the microphone permission request. The user must tap “Agree & begin”; the bottom “Not now” action cancels. The user can listen, adjust watch media volume, re-record, or proceed. Pace calibration uses a timer icon and explicitly says that it times the chant without recording audio.

**Current status: technically privacy-conscious, but not fully cleared for a child-directed release.** Play Console screenshots dated October 16, 2026 show target ages 6–8, 9–12, 13–15, 16–17, and 18+, a commitment to the Families policy, no data collection/sharing, and the statement “Data isn’t encrypted.” Families requirements therefore apply; verify the chosen ages and full policy answers. The final release artifact's merged manifest and SDK behavior, the deployed privacy-policy URL, and the legal treatment of child voice recordings have not been verified here. Do not treat the code review alone as proof of compliance.

The policy copy in this branch is a draft for Android version 15 (1.1.0). Release 14's public policy should remain in place while that release is under review. Publish the version 15 policy with the next release and verify the public URL after deployment.

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
- Google Play Data Safety treats on-device-only access and processing as not collected for that section. That is separate from Families requirements: Google explicitly lists children's microphone sensor data as sensitive and requires accurate disclosures and target-audience/Data Safety answers when children are in the target audience.
- If children are among the declared target audience, the Families policy applies. Do not choose an adults-only audience simply to avoid the policy; accurately declare who the app is for and review all APIs/SDKs used in the final bundle.
- The FTC says a child's voice in an audio file is personal information when collected. Its narrow voice-recording enforcement statement is for audio used only as a short-lived substitute for text to fulfill a request, then deleted immediately. Gaura Mala retains a selected recording for later chant playback, so that exception is not a fit for the feature's intended use.
- FTC guidance also says merely interacting with information that stays on the device and is never transmitted is not COPPA collection. That local-only distinction is relevant here, but it does not settle every wearable recording scenario or laws outside the U.S. The app's intended audience, service context, and actual network/SDK behavior should be assessed with counsel before relying on it.

## Open issues before a child-directed release

1. **Target audience and Families declaration:** Play Console screenshots show ages 6–8 and 9–12 selected alongside older groups, plus the Families commitment. Confirm these age groups reflect the product's intended use and recheck the content-rating answers. Record whether the listing is child-directed, mixed-audience, or general-audience based on the actual product and marketing.
2. **Child voice and consent assessment:** obtain a jurisdiction-specific answer on whether local capture/storage is covered by COPPA or other child privacy laws for this product. If prior parent consent or a neutral age screen is required, add a compliant flow before enabling recording for children. A parent-supervision sentence is not itself verifiable parental consent.
3. **SDK and artifact audit:** inspect the final release AAB's merged permissions and dependency/SDK inventory; confirm no component transmits audio or other child data. Revisit this after any dependency or manifest change.
4. **Retention choice:** today the selected recording stays until the user deletes it or uninstalls the app. Decide whether that user-controlled retention is acceptable for the intended child audience and applicable law; document the policy and keep deletion easy to find.
5. **Policy deployment:** after Release 14's review is complete and version 15 is ready, publish the updated `docs/privacy-policy.html` to the hosted policy URL and verify the Play Console privacy-policy URL points to it. The in-app policy draft is updated for version 15; keep both copies aligned. Google Play requires accurate Data safety answers and a valid policy, while the app's in-flow disclosure and affirmative consent remain necessary before microphone access.
6. **Device verification:** on a watch, verify disclosure before the Android prompt, “Not now” cancellation, permission denial/retry, recording indicator, stop action, 20-second limit, listen/volume controls, re-record and delete behavior, and that pace-only timing never requests the microphone.

## References

- [Google Play User Data policy](https://support.google.com/googleplay/android-developer/answer/10144311?hl=en)
- [Google Play disclosure and consent best practices](https://support.google.com/googleplay/android-developer/answer/11150561?hl=en)
- [Google Play Families policy](https://support.google.com/googleplay/android-developer/answer/9893335?hl=en)
- [Google Play Data Safety guidance](https://support.google.com/googleplay/android-developer/answer/10787469?hl=en)
- [FTC COPPA FAQs, including local-only and voice recording guidance](https://www.ftc.gov/business-guidance/resources/complying-coppa-frequently-asked-questions)
- [FTC voice-recording enforcement policy statement](https://www.ftc.gov/system/files/documents/public_statements/1266473/coppa_policy_statement_audiorecordings.pdf)
- [The supplied Google Play country/region requirements page](https://support.google.com/googleplay/android-developer/answer/6223646)
