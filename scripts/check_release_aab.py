#!/usr/bin/env python3
"""Check required Wear OS ABIs and report embedded native debug symbols."""

from __future__ import annotations

import argparse
from pathlib import Path
import sys
from zipfile import BadZipFile, ZipFile


def inspect_release_artifacts(aab_path: Path) -> tuple[list[str], list[str]]:
    """Return AAB validation errors and symbol-availability warnings."""
    errors: list[str] = []
    warnings: list[str] = []

    try:
        with ZipFile(aab_path) as aab:
            members = aab.infolist()
    except (BadZipFile, OSError) as error:
        return [f"Cannot read app bundle {aab_path}: {error}"], warnings

    native_abis = {
        parts[2]
        for member in members
        if len(parts := member.filename.split("/")) >= 4
        and parts[0] != "BUNDLE-METADATA"
        and parts[1] == "lib"
        and parts[-1].endswith(".so")
    }
    if not native_abis:
        errors.append("The release AAB contains no native shared libraries to validate.")
    if "arm64-v8a" not in native_abis:
        errors.append("The release AAB is missing required Wear OS ABI arm64-v8a.")
    if "x86" in native_abis and "x86_64" not in native_abis:
        errors.append("The release AAB contains x86 libraries without matching x86_64 libraries.")
    if "armeabi-v7a" in native_abis and "arm64-v8a" not in native_abis:
        errors.append("The release AAB contains armeabi-v7a libraries without matching arm64-v8a libraries.")

    symbols_prefix = "BUNDLE-METADATA/com.android.tools.build.debugsymbols/"
    symbol_members = [
        member for member in members
        if member.filename.startswith(symbols_prefix)
        and member.filename.endswith((".sym", ".dbg"))
        and member.file_size > 0
    ]
    if not symbol_members:
        warnings.append(
            "The release AAB contains no embedded native debug symbols. "
            "Confirm the native-symbol status in Play Console after upload."
        )
    elif not any(
        f"{symbols_prefix}arm64-v8a/" in member.filename
        for member in symbol_members
    ):
        warnings.append(
            "The release AAB has no embedded native debug symbols for arm64-v8a. "
            "Confirm the native-symbol status in Play Console after upload."
        )

    return errors, warnings


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "aab",
        nargs="?",
        type=Path,
        default=Path("app/build/outputs/bundle/release/app-release.aab"),
        help="release App Bundle path",
    )
    args = parser.parse_args()
    errors, warnings = inspect_release_artifacts(args.aab)
    for warning in warnings:
        print(f"WARNING: {warning}", file=sys.stderr)
    if errors:
        for error in errors:
            print(f"FAIL: {error}", file=sys.stderr)
        return 1

    print("Release AAB includes arm64-v8a native libraries and compatible 64-bit variants.")
    if not warnings:
        print("Native debug symbols are embedded in the AAB, including arm64-v8a.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
