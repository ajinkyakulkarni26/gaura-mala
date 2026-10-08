#!/usr/bin/env bash

# Create the AGP alias after android-emulator-runner finishes SDK manager setup.
sdk_platforms="${ANDROID_HOME}/platforms"
if [[ -d "${sdk_platforms}/android-37.0" && ! -e "${sdk_platforms}/android-37" ]]; then
  ln -s android-37.0 "${sdk_platforms}/android-37"
fi

font_scale="${WEAR_TEST_FONT_SCALE:-1.0}"
run_label="${WEAR_TEST_RUN_LABEL:-wear-os}"
echo "Wear OS test profile: ${WEAR_TEST_DEVICE_PROFILE:-unspecified}; font scale: ${font_scale}"
adb shell settings put system font_scale "${font_scale}"

# android-emulator-runner executes each script line in a separate shell, so call
# this file as one command to keep the test and reporting exit codes together.
set +e
./gradlew :app:connectedDebugAndroidTest
test_status=$?
python3 scripts/print-android-test-results.py
report_status=$?

if [[ -n "${RUNNER_TEMP:-}" ]]; then
  results_destination="${RUNNER_TEMP}/wear-os-test-results/${run_label}"
  mkdir -p "${results_destination}"
  if [[ -d app/build/reports/androidTests/connected ]]; then
    cp -R app/build/reports/androidTests/connected "${results_destination}/reports"
  fi
  if [[ -d app/build/outputs/androidTest-results/connected ]]; then
    cp -R app/build/outputs/androidTest-results/connected "${results_destination}/results"
  fi
fi

if [[ "${test_status}" -ne 0 ]]; then
  exit "${test_status}"
fi
exit "${report_status}"
