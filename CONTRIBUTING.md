# Contributing to Gaura Mala

Thanks for helping improve Gaura Mala. Changes should preserve the watch-first experience and include tests for changed behavior.

## Before opening a pull request

Before pushing, run the local check script:

```bash
./scripts/test-local.sh
```

The script tests the contributor-approval policy, then runs the JVM tests, core-logic coverage gate, and debug build. If it detects a running Wear OS emulator, it runs the Wear OS UI integration suite and prints each test case's result and description. To run those tests directly, start an emulator and use `./gradlew :app:connectedDebugAndroidTest`; print the detailed report with `python3 scripts/print-android-test-results.py`.

The JaCoCo gate requires 100% line coverage across the pure counter logic in `MantraCounterStateMachine`, `CounterHapticPolicy`, `RotaryBeadInput`, and `MantraUiState`. This is not 100% coverage of the whole app: Compose UI and Android services are exercised by Wear OS integration tests but are not included in the JVM coverage metric. The integration report compares every executed test ID with `config/wear-os-integration-tests.txt`: removing a case without updating the reviewed inventory fails CI, and adding a case requires registering it there. There is no fixed test-count threshold. The unit-test coverage report is available at `app/build/reports/coverage/test/debug/index.html`.

The Wear OS instrumentation suite has named cases covering the counter's initial state, tap/rotary input, undo, 108-step round rollover, manual round adjustment, settings controls, and reset flows. GitHub Actions prints each case and its outcome and uploads the XML/HTML reports. Keep the inventory synchronized as the suite grows. Gesture hardware and physical haptics still require testing on a real watch.

The connected tests verify that a tap advances the bead count and that rotary input reaches the counter. A compatible physical watch is still needed to verify double-pinch detection and actual haptic feedback.

GitHub Actions runs these checks on every branch push and pull request. Push your work to a branch and open a pull request targeting `main`; direct pushes to `main` are blocked. Both Android CI jobs must pass and the branch must be up to date before merging. Pull requests from contributors also need approval from the repository owner, `ajinkyakulkarni26`; the owner can merge their own PRs after CI passes. Owner approval must apply to the current commit, so new commits require a fresh approval. Changes to CI workflows, coverage/build configuration, or policy scripts require owner approval through `CODEOWNERS` as well. These governance files need a second reviewer even on owner-authored PRs. Test results and coverage metrics help detect regressions, but they cannot prove a test is meaningful; review the changed behavior and its tests. Keep pull requests focused, describe how you verified the change, and do not commit signing keys, tester email lists, or private recordings.
