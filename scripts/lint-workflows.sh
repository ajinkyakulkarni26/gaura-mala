#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${repo_root}"

if ! command -v actionlint >/dev/null 2>&1; then
  echo "Install actionlint to lint GitHub Actions workflows." >&2
  exit 2
fi
if ! command -v shellcheck >/dev/null 2>&1; then
  echo "Install shellcheck to lint workflow and repository shell scripts." >&2
  exit 2
fi

actionlint -color
shellcheck scripts/*.sh
