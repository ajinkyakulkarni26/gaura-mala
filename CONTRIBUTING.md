# Contributing to Gaura Mala

Thanks for helping improve Gaura Mala. Changes should preserve the watch-first experience and include tests for changed behavior.

## Before opening a pull request

Before pushing, run the local check script:

```bash
./scripts/test-local.sh
```

The script always runs the JVM tests, core-logic coverage gate, and debug build. If it detects a running Wear OS emulator, it also runs the tap and rotary instrumentation tests. To run those tests directly, start an emulator and use `./gradlew :app:connectedDebugAndroidTest`.

The coverage gate requires at least 90% JaCoCo line coverage across `MantraCounterStateMachine`, `CounterHapticPolicy`, `RotaryBeadInput`, and `MantraUiState`. The overall unit-test coverage report is available at `app/build/reports/coverage/test/debug/index.html`.

The connected tests verify that a tap advances the bead count and that rotary input reaches the counter. A compatible physical watch is still needed to verify double-pinch detection and actual haptic feedback.

GitHub Actions runs these checks on pull requests to `main`. Keep pull requests focused, describe how you verified the change, and do not commit signing keys, tester email lists, or private recordings.
