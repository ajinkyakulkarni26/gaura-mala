# Contributing to Gaura Mala

Thanks for helping improve Gaura Mala. Changes should preserve the watch-first experience and include tests for changed behavior.

## Before opening a pull request

Before pushing, run the local check script:

```bash
./scripts/test-local.sh
```

The script always runs the JVM tests, core-logic coverage gate, and debug build. If it detects a running Wear OS emulator, it runs the Wear OS UI integration suite and prints each test case's result and description. To run those tests directly, start an emulator and use `./gradlew :app:connectedDebugAndroidTest`; print the detailed report with `python3 scripts/print-android-test-results.py`.

The coverage gate requires at least 90% JaCoCo line coverage across `MantraCounterStateMachine`, `CounterHapticPolicy`, `RotaryBeadInput`, and `MantraUiState`. The overall unit-test coverage report is available at `app/build/reports/coverage/test/debug/index.html`.

The Wear OS instrumentation suite currently has 17 named cases covering the counter's initial state, tap/rotary input, undo, 108-step round rollover, manual round adjustment, settings controls, and reset flows. GitHub Actions prints each case and its outcome and uploads the XML/HTML reports. Gesture hardware and physical haptics still require testing on a real watch.

The connected tests verify that a tap advances the bead count and that rotary input reaches the counter. A compatible physical watch is still needed to verify double-pinch detection and actual haptic feedback.

GitHub Actions runs these checks on every branch push and pull request. Push your work to a branch, open a pull request targeting `main`, and wait for both required CI jobs to pass before merging. `main` is protected against direct pushes. Keep pull requests focused, describe how you verified the change, and do not commit signing keys, tester email lists, or private recordings.
