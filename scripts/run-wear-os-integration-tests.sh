#!/usr/bin/env bash

# Create the AGP alias after android-emulator-runner finishes SDK manager setup.
sdk_platforms="${ANDROID_HOME}/platforms"
if [[ -d "${sdk_platforms}/android-37.0" && ! -e "${sdk_platforms}/android-37" ]]; then
  ln -s android-37.0 "${sdk_platforms}/android-37"
fi

# android-emulator-runner executes each script line in a separate shell, so call
# this file as one command to keep the test and reporting exit codes together.
set +e
./gradlew :app:connectedDebugAndroidTest
test_status=$?
python3 scripts/print-android-test-results.py
report_status=$?

if [[ "${test_status}" -ne 0 ]]; then
  exit "${test_status}"
fi
exit "${report_status}"
