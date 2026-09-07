"""Guard against silently mixing payloads in benchmark charts."""

import copy
import unittest
from pathlib import Path

from plot_json_benchmark import collect_results, load_results


class ResultSelectionTest(unittest.TestCase):
    def setUp(self):
        self.media = load_results(Path(__file__).parent / "results/benchmark_results.json")
        self.large = []
        for payload in ("users", "clients"):
            for row in copy.deepcopy(self.media):
                row["params"] = {"payload": payload, "sizeKb": "1000"}
                if payload == "clients":
                    row["primaryMetric"]["score"] *= 2
                self.large.append(row)

    def test_payload_selection(self):
        historical = collect_results(self.media)
        users = collect_results(self.large, "users")
        clients = collect_results(self.large, "clients")
        self.assertEqual(historical, users)
        for case, scores in users.items():
            for library, (score, _) in scores.items():
                self.assertEqual(score * 2, clients[case][library][0])

    def test_mixed_input_requires_selection(self):
        with self.assertRaisesRegex(ValueError, "--payload"):
            collect_results(self.large)

    def test_missing_cases_are_rejected(self):
        with self.assertRaisesRegex(ValueError, "Missing"):
            collect_results(self.large, "users", 10)
        with self.assertRaisesRegex(ValueError, "Missing"):
            collect_results(self.media[1:])

    def test_duplicates_are_rejected(self):
        with self.assertRaisesRegex(ValueError, "Duplicate"):
            collect_results(self.media + self.media[:1])

    def test_invalid_scores_are_rejected(self):
        for score in (float("nan"), float("inf"), 0, -1):
            with self.subTest(score=score):
                rows = copy.deepcopy(self.media)
                rows[0]["primaryMetric"]["score"] = score
                with self.assertRaisesRegex(ValueError, "Invalid throughput"):
                    collect_results(rows)


if __name__ == "__main__":
    unittest.main()
