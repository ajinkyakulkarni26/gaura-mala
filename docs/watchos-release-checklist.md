# Apple Watch release checklist

The native watchOS port is in early development. Complete these checks before inviting TestFlight testers or submitting it to the App Store.

## Apple Developer setup

- [ ] Register the watch-only container bundle ID `com.gauramala.watchos`, watch app bundle ID `com.gauramala.watchos.watchkitapp`, and widget ID `com.gauramala.watchos.watchkitapp.widget` under the chosen team.
- [ ] Create and enable the `group.com.gauramala.watchos` App Group for both targets so the complication can read the app's saved progress.
- [ ] Set the correct development team and verify automatic signing for a physical Apple Watch.
- [ ] Create the App Store Connect app record using the watch-only container bundle ID `com.gauramala.watchos`; confirm the record is configured as a standalone watch app.

## Device and behavior checks

- [ ] Test on a physical watch: tap counting, Digital Crown counting, system Double Tap on supported hardware, undo, milestone and round haptics, and the daily-goal alert.
- [ ] Check that crown counting resumes after closing the round picker and Settings.
- [ ] Verify saved counts and preferences after force quit, restart, and local midnight rollover.
- [ ] Review the counter, round picker, Settings, and privacy policy at the smallest and largest supported case sizes and with the largest practical system text setting.
- [ ] Add the Gaura Mala complication and Smart Stack widget; confirm their progress refreshes after a count and after the local day changes.
- [ ] Confirm the Contact Developer link opens a usable GitHub issue route from the watch.

## Store readiness

- [ ] Review the hosted privacy policy and complete App Store privacy disclosures based on the actual data handling.
- [ ] Prepare watch screenshots, localized store text, support URL, category, and age rating.
- [ ] Archive the `GauraMala` scheme (the watch-only distribution container) with the production team; install and test the embedded watch app from that exact signed archive before uploading.
- [ ] Upload to TestFlight, test installation and update behavior, then submit the reviewed build for App Store review.
- [ ] Add the `Apple Watch CI / Swift counter tests and watchOS build` status check to the `main` ruleset after GitHub has recorded a successful run.

The simulator currently verifies compilation, installation, launch, and initial counter layout on 40 mm, 42 mm, and 49 mm simulator devices. It does not verify physical gestures, crown feel, Taptic behavior, or store signing.

The `GauraMalaWatch` scheme builds and runs the watch app for simulator checks. The separate `GauraMala` scheme builds the Xcode watch-only distribution container required for App Store packaging. CI archives this scheme without production signing and checks that the watch app and widget are embedded under the expected bundle identifiers; a production-team signed archive and App Store Connect validation are still required before TestFlight.
