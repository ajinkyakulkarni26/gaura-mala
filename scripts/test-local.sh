#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

echo "Running the repository policy and integration-inventory tests..."
python3 -m unittest discover -s scripts -p 'test_*.py' -v

if command -v actionlint >/dev/null 2>&1 && command -v shellcheck >/dev/null 2>&1; then
  echo "Linting GitHub Actions and shell scripts..."
  ./scripts/lint-workflows.sh
else
  echo "Skipping workflow lint: install actionlint and shellcheck to run this local check."
fi

echo "Running JVM unit tests, the core coverage gate, and a debug build..."
./gradlew :app:verifyCoreLogicCoverage :app:assembleDebug
bash scripts/check-native-abis.sh

sdk_dir="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [[ -z "$sdk_dir" && -f local.properties ]]; then
  sdk_dir="$(sed -n 's/^sdk.dir=//p' local.properties | sed 's/\\ / /g' | tail -n 1)"
fi

adb_path=""
if [[ -n "$sdk_dir" && -x "$sdk_dir/platform-tools/adb" ]]; then
  adb_path="$sdk_dir/platform-tools/adb"
elif command -v adb >/dev/null 2>&1; then
  adb_path="$(command -v adb)"
fi

if [[ -z "$adb_path" ]]; then
  echo "No ADB executable found. Unit tests passed; skipping Wear OS emulator tests."
  exit 0
fi

wear_emulator=""
if [[ -n "${WEAR_TEST_SERIAL:-}" ]]; then
  if ! "$adb_path" devices | awk -v requested="$WEAR_TEST_SERIAL" \
    '$1 == requested && $2 == "device" { found = 1 } END { exit !found }'; then
    echo "Requested Wear OS emulator ${WEAR_TEST_SERIAL} is not connected and ready." >&2
    exit 1
  fi
  if "$adb_path" -s "$WEAR_TEST_SERIAL" shell pm list features 2>/dev/null \
    | grep -q 'android.hardware.type.watch'; then
    wear_emulator="$WEAR_TEST_SERIAL"
  else
    echo "Requested device ${WEAR_TEST_SERIAL} is not a Wear OS emulator." >&2
    exit 1
  fi
else
  while read -r serial; do
    [[ -n "$serial" ]] || continue
    if "$adb_path" -s "$serial" shell pm list features 2>/dev/null | grep -q 'android.hardware.type.watch'; then
      wear_emulator="$serial"
      break
    fi
  done < <("$adb_path" devices | awk 'NR > 1 && $2 == "device" && $1 ~ /^emulator-/ { print $1 }')
fi

if [[ -z "$wear_emulator" ]]; then
  echo "No running Wear OS emulator detected. Unit tests passed; skipping device tests."
  echo "Start a round Wear OS AVD in Android Studio, then run this script again to include tap and rotary tests."
  exit 0
fi

echo "Found Wear OS emulator $wear_emulator; running Wear OS UI integration tests..."
# Optionally set the emulator font scale for a repeatable small-screen review run.
if [[ -n "${WEAR_TEST_FONT_SCALE:-}" ]]; then
  "$adb_path" -s "$wear_emulator" shell settings put system font_scale "$WEAR_TEST_FONT_SCALE"
fi
# Avoid the Wear OS charging activity taking focus from the app during tests.
trap '
  "$adb_path" -s "$wear_emulator" shell dumpsys battery reset >/dev/null 2>&1 || true
' EXIT
"$adb_path" -s "$wear_emulator" shell dumpsys battery unplug
test_exit_code=0
ANDROID_SERIAL="$wear_emulator" ./gradlew :app:connectedDebugAndroidTest || test_exit_code=$?
python3 scripts/print-android-test-results.py || test_exit_code=$?
run_label="${WEAR_TEST_RUN_LABEL:-local}"
run_stamp="$(date +%Y%m%d-%H%M%S)"
screenshot_source="/sdcard/Pictures/GauraMalaEmulatorReview"
screenshot_destination="${repo_root}/test-artifacts/emulator-screenshots/${run_label}-${run_stamp}"
if "$adb_path" -s "$wear_emulator" shell test -d "$screenshot_source" 2>/dev/null; then
  screenshot_names="$("$adb_path" -s "$wear_emulator" shell ls -1 "$screenshot_source" 2>/dev/null | tr -d '\r')"
  latest_screenshot="$(printf '%s\n' "$screenshot_names" | sort -n | tail -n 1)"
  screenshot_run_id="${latest_screenshot%%__*}"
  mkdir -p "$screenshot_destination"
  while IFS= read -r stored_screenshot_name; do
    [[ "$stored_screenshot_name" == "${screenshot_run_id}__"* ]] || continue
    screenshot_name="${stored_screenshot_name#*__}"
    [[ -n "$screenshot_name" ]] || continue
    if ! "$adb_path" -s "$wear_emulator" pull \
      "$screenshot_source/$stored_screenshot_name" "$screenshot_destination/$screenshot_name" \
      >/dev/null </dev/null; then
      echo "Could not pull Wear OS screenshot $screenshot_name." >&2
    else
      "$adb_path" -s "$wear_emulator" shell rm -f \
        "$screenshot_source/$stored_screenshot_name" >/dev/null 2>&1 </dev/null || true
    fi
  done <<< "$screenshot_names"
  echo "Wear OS review screenshots saved under ${screenshot_destination}"
else
  echo "No Wear OS review screenshots were saved by this run."
fi
exit "$test_exit_code"
