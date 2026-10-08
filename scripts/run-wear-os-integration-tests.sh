#!/usr/bin/env bash
set -euo pipefail

# Create the AGP alias after android-emulator-runner finishes SDK manager setup.
sdk_platforms="${ANDROID_HOME}/platforms"
if [[ -d "${sdk_platforms}/android-37.0" && ! -e "${sdk_platforms}/android-37" ]]; then
  ln -s android-37.0 "${sdk_platforms}/android-37"
fi

font_scale="${WEAR_TEST_FONT_SCALE:-1.0}"
run_label="${WEAR_TEST_RUN_LABEL:-wear-os}"
device_serial="${ANDROID_SERIAL:-emulator-5554}"
if [[ -n "${ANDROID_HOME:-}" && -x "${ANDROID_HOME}/platform-tools/adb" ]]; then
  adb_path="${ANDROID_HOME}/platform-tools/adb"
else
  adb_path="$(command -v adb)"
fi
echo "Wear OS test profile: ${WEAR_TEST_DEVICE_PROFILE:-unspecified}; font scale: ${font_scale}"

wait_for_emulator() {
  echo "Waiting for ${device_serial} to reconnect and finish booting..."
  "${adb_path}" reconnect offline >/dev/null 2>&1 || true
  for ((attempt = 1; attempt <= 45; attempt++)); do
    local device_state
    local boot_completed
    device_state="$("${adb_path}" -s "${device_serial}" get-state 2>/dev/null || true)"
    if [[ "${device_state}" == "device" ]]; then
      boot_completed="$("${adb_path}" -s "${device_serial}" shell getprop sys.boot_completed 2>/dev/null || true)"
      if [[ "${boot_completed}" == "1" ]]; then
        return 0
      fi
    fi
    sleep 2
  done
  echo "${device_serial} did not return to a booted state." >&2
  return 1
}

if ! "${adb_path}" -s "${device_serial}" shell settings put system font_scale "${font_scale}"; then
  echo "ADB disconnected while configuring the emulator; attempting recovery..." >&2
  wait_for_emulator
  "${adb_path}" -s "${device_serial}" shell settings put system font_scale "${font_scale}"
fi

# android-emulator-runner executes each script line in a separate shell, so call
# this file as one command to keep the test and reporting exit codes together.
test_log="$(mktemp "${RUNNER_TEMP:-${TMPDIR:-/tmp}}/wear-os-tests.XXXXXX")"
trap 'rm -f "${test_log}"' EXIT
test_results_dir="app/build/outputs/androidTest-results/connected"
test_reports_dir="app/build/reports/androidTests/connected"

run_instrumentation() {
  local gradle_status
  set +e
  ./gradlew :app:connectedDebugAndroidTest 2>&1 | tee -a "${test_log}"
  gradle_status=${PIPESTATUS[0]}
  set -e
  return "${gradle_status}"
}

# Avoid mistaking XML from an earlier profile or attempt for this run's results.
rm -rf "${test_results_dir}" "${test_reports_dir}"
if run_instrumentation; then
  test_status=0
else
  test_status=$?
fi

has_test_cases() {
  [[ -d "${test_results_dir}" ]] && grep -Rqs --include='*.xml' '<testcase' "${test_results_dir}"
}

if grep -Eiq 'adb: device offline|adb: device not found' "${test_log}" && ! has_test_cases; then
  echo "ADB lost the emulator before instrumentation began; reconnecting and retrying once..." >&2
  if wait_for_emulator; then
    rm -rf "${test_results_dir}" "${test_reports_dir}"
    if run_instrumentation; then
      test_status=0
    else
      test_status=$?
    fi
  else
    test_status=1
  fi
fi

if python3 scripts/print-android-test-results.py; then
  report_status=0
else
  report_status=$?
fi

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
