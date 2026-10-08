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
while read -r serial; do
  [[ -n "$serial" ]] || continue
  if "$adb_path" -s "$serial" shell pm list features 2>/dev/null | grep -q 'android.hardware.type.watch'; then
    wear_emulator="$serial"
    break
  fi
done < <("$adb_path" devices | awk 'NR > 1 && $2 == "device" && $1 ~ /^emulator-/ { print $1 }')

if [[ -z "$wear_emulator" ]]; then
  echo "No running Wear OS emulator detected. Unit tests passed; skipping device tests."
  echo "Start a round Wear OS AVD in Android Studio, then run this script again to include tap and rotary tests."
  exit 0
fi

echo "Found Wear OS emulator $wear_emulator; running Wear OS UI integration tests..."
# Avoid the Wear OS charging activity taking focus from the app during tests.
trap '
  "$adb_path" -s "$wear_emulator" shell dumpsys battery reset >/dev/null 2>&1 || true
' EXIT
"$adb_path" -s "$wear_emulator" shell dumpsys battery unplug
test_exit_code=0
ANDROID_SERIAL="$wear_emulator" ./gradlew :app:connectedDebugAndroidTest || test_exit_code=$?
python3 scripts/print-android-test-results.py
exit "$test_exit_code"
