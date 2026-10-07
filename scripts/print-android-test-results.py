#!/usr/bin/env python3
"""Print each connected Android instrumentation test case from Gradle XML reports."""

from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET


REPO_ROOT = Path(__file__).resolve().parents[1]
RESULTS_ROOT = REPO_ROOT / "app" / "build" / "outputs" / "androidTest-results" / "connected"
MIN_EXPECTED_CASES = 17


def readable_name(name: str) -> str:
    words = re.sub(r"([a-z0-9])([A-Z])", r"\1 \2", name)
    words = re.sub(r"([a-zA-Z])([0-9])", r"\1 \2", words)
    return words[:1].upper() + words[1:]


def main() -> int:
    reports = sorted(RESULTS_ROOT.rglob("TEST-*.xml"))
    if not reports:
        print(f"No connected Android test XML reports found under {RESULTS_ROOT}", file=sys.stderr)
        return 1

    cases = []
    for report in reports:
        root = ET.parse(report).getroot()
        for case in root.iter("testcase"):
            problem = next(
                (child for child in case if child.tag in {"failure", "error"}),
                None,
            )
            skipped = next((child for child in case if child.tag == "skipped"), None)
            outcome = "FAIL" if problem is not None else "SKIP" if skipped is not None else "PASS"
            cases.append((outcome, case.get("classname", "unknown"), case.get("name", "unnamed"), problem))

    print(f"Wear OS instrumentation cases: {len(cases)}")
    if len(cases) < MIN_EXPECTED_CASES:
        print(
            f"Expected at least {MIN_EXPECTED_CASES} integration cases; found {len(cases)}. "
            "Do not remove existing coverage without replacing it with equivalent tests.",
            file=sys.stderr,
        )
    for outcome, classname, method, problem in cases:
        description = readable_name(method)
        duration = ""
        if problem is not None:
            message = problem.get("message") or problem.text or ""
            details = message.strip().splitlines()[0:1]
            duration = f" — {details[0]}" if details else ""
        print(f"{outcome}: {classname}#{method} — {description}{duration}")

    failed = sum(outcome == "FAIL" for outcome, *_ in cases)
    skipped = sum(outcome == "SKIP" for outcome, *_ in cases)
    print(f"Summary: {len(cases) - failed - skipped} passed, {failed} failed, {skipped} skipped")
    return 1 if failed or len(cases) < MIN_EXPECTED_CASES else 0


if __name__ == "__main__":
    sys.exit(main())
