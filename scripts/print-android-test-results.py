#!/usr/bin/env python3
"""Print Wear OS instrumentation results and verify the checked-in case inventory."""

from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET
from integration_test_inventory import compare_case_inventory, load_inventory


REPO_ROOT = Path(__file__).resolve().parents[1]
RESULTS_ROOT = REPO_ROOT / "app" / "build" / "outputs" / "androidTest-results" / "connected"
def readable_name(name: str) -> str:
    words = re.sub(r"([a-z0-9])([A-Z])", r"\1 \2", name)
    words = re.sub(r"([a-zA-Z])([0-9])", r"\1 \2", words)
    return words[:1].upper() + words[1:]


def main() -> int:
    reports = sorted(RESULTS_ROOT.rglob("TEST-*.xml"))
    if not reports:
        print(f"No connected Android test XML reports found under {RESULTS_ROOT}", file=sys.stderr)
        return 1

    try:
        expected_ids = load_inventory()
    except (OSError, ValueError) as error:
        print(f"Unable to verify Wear OS integration test inventory: {error}", file=sys.stderr)
        return 1

    cases = []
    actual_ids: set[str] = set()
    for report in reports:
        root = ET.parse(report).getroot()
        for case in root.iter("testcase"):
            classname = case.get("classname", "unknown")
            method = case.get("name", "unnamed")
            case_id = f"{classname}#{method}"
            actual_ids.add(case_id)
            problem = next(
                (child for child in case if child.tag in {"failure", "error"}),
                None,
            )
            skipped = next((child for child in case if child.tag == "skipped"), None)
            outcome = "FAIL" if problem is not None else "SKIP" if skipped is not None else "PASS"
            cases.append((outcome, case_id, problem))

    missing, unregistered = compare_case_inventory(expected_ids, actual_ids)
    print(f"Wear OS instrumentation cases: {len(cases)} (registered: {len(expected_ids)})")
    if missing:
        print("Registered cases without an execution result:", file=sys.stderr)
        for case_id in sorted(missing):
            print(f"  - {case_id}", file=sys.stderr)
    if unregistered:
        print("Executed cases missing from the checked-in inventory:", file=sys.stderr)
        for case_id in sorted(unregistered):
            print(f"  - {case_id}", file=sys.stderr)

    for outcome, case_id, problem in cases:
        description = readable_name(case_id.rsplit("#", 1)[-1])
        details = ""
        if problem is not None:
            message = problem.get("message") or problem.text or ""
            first_line = message.strip().splitlines()[:1]
            details = f" — {first_line[0]}" if first_line else ""
        print(f"{outcome}: {case_id} — {description}{details}")

    failed = sum(outcome == "FAIL" for outcome, *_ in cases)
    skipped = sum(outcome == "SKIP" for outcome, *_ in cases)
    print(f"Summary: {len(cases) - failed - skipped} passed, {failed} failed, {skipped} skipped")
    if missing or unregistered:
        print(
            "Update the checked-in test inventory when adding, replacing, or intentionally removing a case; "
            "review the test and inventory changes together.",
            file=sys.stderr,
        )
    return 1 if failed or skipped or missing or unregistered else 0


if __name__ == "__main__":
    sys.exit(main())
