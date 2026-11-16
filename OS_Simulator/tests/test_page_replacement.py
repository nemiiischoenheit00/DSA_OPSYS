import random
import unittest

from core.page_replacement import compare_all, fifo, lru, optimal, run
from core.validation import ValidationError

TEXTBOOK = [7, 0, 1, 2, 0, 3, 0, 4, 2, 3, 0, 3, 2, 1, 2, 0, 1, 7, 0, 1]
BELADY = [1, 2, 3, 4, 1, 2, 5, 1, 2, 3, 4, 5]


class TestPageReplacement(unittest.TestCase):
    def test_textbook_fault_counts(self):
        # Well-known results for 3 frames: FIFO 15, LRU 12, Optimal 9
        self.assertEqual(fifo(TEXTBOOK, 3).faults, 15)
        self.assertEqual(lru(TEXTBOOK, 3).faults, 12)
        self.assertEqual(optimal(TEXTBOOK, 3).faults, 9)

    def test_hits_ratios(self):
        r = optimal(TEXTBOOK, 3)
        self.assertEqual(r.hits, 11)
        self.assertEqual(r.total, 20)
        self.assertAlmostEqual(r.hit_ratio, 0.55)
        self.assertAlmostEqual(r.fault_ratio, 0.45)

    def test_fifo_frame_states(self):
        r = fifo([1, 2, 3, 1, 4], 3)
        self.assertEqual([s.frames for s in r.steps],
                         [[1, None, None], [1, 2, None], [1, 2, 3], [1, 2, 3], [4, 2, 3]])
        self.assertEqual([s.hit for s in r.steps], [False, False, False, True, False])
        self.assertEqual(r.steps[4].replaced, 1)                   # FIFO evicts the oldest (1)

    def test_lru_evicts_least_recently_used(self):
        r = lru([1, 2, 3, 1, 4], 3)                                # 1 was just used -> evict 2
        self.assertEqual(r.steps[4].replaced, 2)
        self.assertEqual(r.steps[4].frames, [1, 4, 3])
        self.assertIn("least recently used", r.steps[4].reason)

    def test_optimal_evicts_farthest_next_use(self):
        r = optimal([1, 2, 3, 4, 1, 2], 3)     # at '4': next uses 1@5, 2@6, 3 never -> evict 3
        self.assertEqual(r.steps[3].replaced, 3)
        self.assertIn("never used again", r.steps[3].reason)
        r = optimal([1, 2, 3, 4, 2, 3, 1], 3)  # at '4': 1@7 farthest -> evict 1
        self.assertEqual(r.steps[3].replaced, 1)
        self.assertIn("farthest", r.steps[3].reason)

    def test_belady_anomaly_for_fifo(self):
        self.assertEqual(fifo(BELADY, 3).faults, 9)
        self.assertEqual(fifo(BELADY, 4).faults, 10)       # more frames, more faults
        self.assertEqual(lru(BELADY, 3).faults, 10)
        self.assertEqual(lru(BELADY, 4).faults, 8)
        self.assertEqual(optimal(BELADY, 3).faults, 7)
        self.assertEqual(optimal(BELADY, 4).faults, 6)

    def test_boundaries(self):
        r = fifo([5, 5, 5, 5], 1)
        self.assertEqual((r.faults, r.hits), (1, 3))
        r = fifo([1, 2, 3, 1, 2, 3], 1)                    # one frame -> every change faults
        self.assertEqual(r.faults, 6)
        r = lru([1, 2, 3], 10)                             # more frames than pages
        self.assertEqual((r.faults, r.hits), (3, 0))
        self.assertTrue(all(s.replaced is None for s in r.steps))
        r = optimal([4], 2)
        self.assertEqual(r.faults, 1)

    def test_invalid_input(self):
        with self.assertRaises(ValidationError):
            fifo([], 3)
        with self.assertRaises(ValidationError):
            fifo([1, 2], 0)
        with self.assertRaises(ValidationError):
            lru([1, -2], 3)
        with self.assertRaises(ValidationError):
            run("MRU", [1], 1)

    def test_large_reference_string_invariants(self):
        rng = random.Random(5)
        refs = [rng.randint(0, 49) for _ in range(5000)]
        results = compare_all(refs, 8)
        by = {r.algorithm: r for r in results}
        for r in results:
            self.assertEqual(r.hits + r.faults, 5000)
            for s in r.steps:
                resident = [p for p in s.frames if p is not None]
                self.assertEqual(len(resident), len(set(resident)))     # no page twice
                self.assertIn(s.page, resident)                         # requested page is resident
        self.assertLessEqual(by["Optimal"].faults, by["FIFO"].faults)   # Optimal is a lower bound
        self.assertLessEqual(by["Optimal"].faults, by["LRU"].faults)


if __name__ == "__main__":
    unittest.main()
