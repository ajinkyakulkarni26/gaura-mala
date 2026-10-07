import unittest

from integration_test_inventory import compare_case_inventory


class IntegrationTestInventoryTest(unittest.TestCase):
    def test_exact_registered_cases_pass_regardless_of_total_count(self) -> None:
        expected = {"example.CounterTest#tapAdvances", "example.CounterTest#roundCompletes"}
        missing, unregistered = compare_case_inventory(expected, expected)

        self.assertEqual(set(), missing)
        self.assertEqual(set(), unregistered)

    def test_removed_registered_case_is_reported(self) -> None:
        expected = {"example.CounterTest#tapAdvances", "example.CounterTest#roundCompletes"}
        actual = {"example.CounterTest#tapAdvances"}

        missing, unregistered = compare_case_inventory(expected, actual)

        self.assertEqual({"example.CounterTest#roundCompletes"}, missing)
        self.assertEqual(set(), unregistered)

    def test_new_case_must_be_added_to_inventory(self) -> None:
        expected = {"example.CounterTest#tapAdvances"}
        actual = {"example.CounterTest#tapAdvances", "example.CounterTest#undoRestoresCount"}

        missing, unregistered = compare_case_inventory(expected, actual)

        self.assertEqual(set(), missing)
        self.assertEqual({"example.CounterTest#undoRestoresCount"}, unregistered)


if __name__ == "__main__":
    unittest.main()
