"""Helpers for validating the Wear OS instrumentation case inventory."""

from collections import Counter
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
INVENTORY_FILE = REPO_ROOT / "config" / "wear-os-integration-tests.txt"


def compare_case_inventory(expected: set[str], actual: set[str]) -> tuple[set[str], set[str]]:
    """Return inventory IDs without a result and result IDs missing from the inventory."""
    return expected - actual, actual - expected


def load_inventory(path: Path = INVENTORY_FILE) -> set[str]:
    """Load expected fully qualified JUnit IDs, ignoring blank lines and comments."""
    entries = [
        line.strip()
        for line in path.read_text(encoding="utf-8").splitlines()
        if line.strip() and not line.lstrip().startswith("#")
    ]
    duplicates = sorted(entry for entry, count in Counter(entries).items() if count > 1)
    if duplicates:
        raise ValueError("Duplicate integration test IDs in inventory: " + ", ".join(duplicates))
    if not entries:
        raise ValueError(f"Integration test inventory is empty: {path}")
    malformed = [entry for entry in entries if "#" not in entry]
    if malformed:
        raise ValueError("Integration test IDs must use package.Class#method format: " + ", ".join(malformed))
    return set(entries)
