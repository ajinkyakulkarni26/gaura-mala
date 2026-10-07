#!/usr/bin/env python3
"""Require the repository owner to approve contributor pull requests."""

from __future__ import annotations

import json
import os
import sys
from typing import Any
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen


def approval_decision(
    pull_request: dict[str, Any], reviews: list[dict[str, Any]], owner: str
) -> tuple[bool, str]:
    """Return whether this PR satisfies the owner-review policy and why."""
    author = pull_request["user"]["login"]
    if author.casefold() == owner.casefold():
        return True, f"PR author is @{owner}; owner-authored PRs are exempt from owner review."

    owner_reviews = [
        review
        for review in reviews
        if review.get("user", {}).get("login", "").casefold() == owner.casefold()
    ]
    if not owner_reviews:
        return False, f"Waiting for @{owner} to approve this contributor PR."

    latest_review = max(
        owner_reviews,
        key=lambda review: review.get("submitted_at") or review.get("created_at") or "",
    )
    if latest_review.get("state", "").upper() != "APPROVED":
        state = latest_review.get("state", "unknown").replace("_", " ").lower()
        return False, f"The latest review from @{owner} is {state}; a new approval is required."

    current_head = pull_request["head"]["sha"]
    if latest_review.get("commit_id") != current_head:
        return False, f"@{owner}'s approval is for an older commit; please review the latest changes."

    return True, f"Contributor PR approved by @{owner} for the current commit."


def github_get_json(url: str, token: str) -> Any:
    request = Request(
        url,
        headers={
            "Accept": "application/vnd.github+json",
            "Authorization": f"Bearer {token}",
            "X-GitHub-Api-Version": "2022-11-28",
        },
    )
    with urlopen(request, timeout=30) as response:
        return json.loads(response.read())


def fetch_reviews(api_base: str, repository: str, pull_number: str, token: str) -> list[dict[str, Any]]:
    reviews: list[dict[str, Any]] = []
    page = 1
    while True:
        url = (
            f"{api_base}/repos/{repository}/pulls/{pull_number}/reviews"
            f"?per_page=100&page={page}"
        )
        batch = github_get_json(url, token)
        reviews.extend(batch)
        if len(batch) < 100:
            return reviews
        page += 1


def main() -> int:
    repository = os.environ["GITHUB_REPOSITORY"]
    pull_number = os.environ["PR_NUMBER"]
    token = os.environ["GITHUB_TOKEN"]
    owner = os.environ.get("REQUIRED_APPROVER", repository.split("/", 1)[0])
    api_base = os.environ.get("GITHUB_API_URL", "https://api.github.com").rstrip("/")

    try:
        pull_request = github_get_json(
            f"{api_base}/repos/{repository}/pulls/{pull_number}", token
        )
        reviews = fetch_reviews(api_base, repository, pull_number, token)
    except (HTTPError, URLError, TimeoutError, json.JSONDecodeError) as error:
        print(f"Unable to check pull request approval: {error}", file=sys.stderr)
        return 2

    passed, message = approval_decision(pull_request, reviews, owner)
    print(message)
    return 0 if passed else 1


if __name__ == "__main__":
    raise SystemExit(main())
