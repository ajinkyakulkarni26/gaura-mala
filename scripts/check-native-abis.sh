#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
native_lib_root="${repo_root}/app/build/intermediates/merged_native_libs/debug/mergeDebugNativeLibs/out/lib"

if [[ ! -d "${native_lib_root}/arm64-v8a" ]]; then
  echo "Missing arm64-v8a native libraries under ${native_lib_root}" >&2
  exit 1
fi

if ! find "${native_lib_root}/arm64-v8a" -type f -name '*.so' -print -quit | grep -q .; then
  echo "No arm64-v8a shared libraries were packaged" >&2
  exit 1
fi

echo "Verified arm64-v8a native libraries are packaged for Wear OS:"
find "${native_lib_root}/arm64-v8a" -type f -name '*.so' -print | sed "s#${repo_root}/##"
