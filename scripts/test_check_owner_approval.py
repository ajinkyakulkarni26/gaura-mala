import unittest

from check_owner_approval import approval_decision


OWNER = "ajinkyakulkarni26"
CURRENT_HEAD = "abc123"


def pull_request(author: str = "contributor") -> dict:
    return {"user": {"login": author}, "head": {"sha": CURRENT_HEAD}}


def owner_review(state: str = "APPROVED", commit_id: str = CURRENT_HEAD) -> dict:
    return {
        "user": {"login": OWNER},
        "state": state,
        "commit_id": commit_id,
        "submitted_at": "2026-10-07T12:00:00Z",
    }


class OwnerApprovalPolicyTest(unittest.TestCase):
    def test_owner_authored_pull_request_does_not_need_another_reviewer(self) -> None:
        passed, message = approval_decision(pull_request(OWNER), [], OWNER)
        self.assertTrue(passed)
        self.assertIn("exempt", message)

    def test_contributor_pull_request_waits_for_owner_approval(self) -> None:
        passed, message = approval_decision(pull_request(), [], OWNER)
        self.assertFalse(passed)
        self.assertIn("Waiting", message)

    def test_current_owner_approval_passes(self) -> None:
        passed, message = approval_decision(pull_request(), [owner_review()], OWNER)
        self.assertTrue(passed)
        self.assertIn("current commit", message)

    def test_approval_for_an_older_commit_does_not_pass(self) -> None:
        passed, message = approval_decision(
            pull_request(), [owner_review(commit_id="old456")], OWNER
        )
        self.assertFalse(passed)
        self.assertIn("older commit", message)

    def test_latest_non_approval_requires_a_new_approval(self) -> None:
        earlier_approval = owner_review()
        later_comment = {
            **owner_review(state="COMMENTED"),
            "submitted_at": "2026-10-07T13:00:00Z",
        }
        passed, message = approval_decision(
            pull_request(), [earlier_approval, later_comment], OWNER
        )
        self.assertFalse(passed)
        self.assertIn("new approval", message)


if __name__ == "__main__":
    unittest.main()
