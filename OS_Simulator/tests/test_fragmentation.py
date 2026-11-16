import random
import unittest

from core.fragmentation import FIXED, STRATEGIES, VARIABLE, allocate, explain
from core.validation import ValidationError

BLOCKS = [100, 500, 200, 300, 600]
PROCS = [("P1", 212), ("P2", 417), ("P3", 112), ("P4", 426)]


def placement(result):
    return {a.pid: a.block for a in result.allocated}


class TestFragmentation(unittest.TestCase):
    # ---- fixed partitions: internal fragmentation -----------------------------------------
    def test_fixed_first_fit(self):
        r = allocate(BLOCKS, PROCS, "First Fit", FIXED)
        self.assertEqual(placement(r), {"P1": 1, "P2": 4, "P3": 2})
        self.assertEqual([u.pid for u in r.unallocated], ["P4"])
        self.assertEqual(r.total_internal, (500 - 212) + (600 - 417) + (200 - 112))   # 288+183+88
        self.assertFalse(r.has_external)       # free partitions 100+300 = 400 < 426 -> just no memory

    def test_fixed_best_fit_places_everything(self):
        r = allocate(BLOCKS, PROCS, "Best Fit", FIXED)
        self.assertEqual(placement(r), {"P1": 3, "P2": 1, "P3": 2, "P4": 4})
        self.assertEqual(r.unallocated, [])
        self.assertEqual(r.total_internal, 88 + 83 + 88 + 174)

    def test_fixed_worst_fit(self):
        r = allocate(BLOCKS, PROCS, "Worst Fit", FIXED)
        self.assertEqual(placement(r), {"P1": 4, "P2": 1, "P3": 3})
        self.assertEqual([u.pid for u in r.unallocated], ["P4"])

    def test_fixed_internal_vs_external_not_confused(self):
        r = allocate([100, 100, 100], [("A", 10), ("B", 10), ("C", 10), ("D", 10)], "First Fit", FIXED)
        self.assertEqual(r.total_internal, 270)         # 3 x 90 wasted INSIDE allocated partitions
        self.assertEqual(r.total_free, 0)
        self.assertFalse(r.unallocated[0].external)     # inside-partition waste is never "external"

    def test_fixed_external_fragmentation_detected(self):
        # Free partitions 60 + 50 = 110 >= 100, but no single partition can hold 100
        r = allocate([60, 60, 50], [("A", 50), ("B", 100)], "First Fit", FIXED)
        self.assertEqual(placement(r), {"A": 0})
        self.assertTrue(r.unallocated[0].external)

    # ---- variable partitions: external fragmentation --------------------------------------
    def test_variable_first_fit_external_fragmentation(self):
        r = allocate(BLOCKS, PROCS, "First Fit", VARIABLE)
        self.assertEqual(placement(r), {"P1": 1, "P2": 4, "P3": 1})
        self.assertEqual([b.free for b in r.blocks], [100, 176, 200, 300, 183])
        self.assertEqual(r.total_internal, 0)                    # no internal fragmentation here
        self.assertEqual(r.total_free, 959)
        self.assertEqual(r.largest_hole, 300)
        self.assertTrue(r.unallocated[0].external)               # 959 free overall but P4 needs 426
        self.assertIn("EXTERNAL", explain(r))

    def test_variable_best_fit(self):
        r = allocate(BLOCKS, PROCS, "Best Fit", VARIABLE)
        self.assertEqual(placement(r), {"P1": 3, "P2": 1, "P3": 2, "P4": 4})
        self.assertEqual([b.free for b in r.blocks], [100, 83, 88, 88, 174])
        self.assertEqual(r.unallocated, [])

    def test_variable_block_holds_several_processes(self):
        r = allocate([100], [("A", 30), ("B", 30), ("C", 30), ("D", 30)], "First Fit", VARIABLE)
        self.assertEqual(len(r.blocks[0].allocations), 3)
        self.assertEqual(r.blocks[0].free, 10)
        self.assertEqual([u.pid for u in r.unallocated], ["D"])

    # ---- boundaries and invalid input -----------------------------------------------------
    def test_exact_fit_and_too_big(self):
        r = allocate([50], [("A", 50)], "Best Fit", FIXED)
        self.assertEqual(r.total_internal, 0)
        r = allocate([50], [("A", 51)], "Best Fit", FIXED)
        self.assertEqual(len(r.unallocated), 1)
        self.assertFalse(r.unallocated[0].external)

    def test_invalid_input(self):
        with self.assertRaises(ValidationError):
            allocate([], PROCS, "First Fit")
        with self.assertRaises(ValidationError):
            allocate(BLOCKS, [], "First Fit")
        with self.assertRaises(ValidationError):
            allocate([0, 10], PROCS, "First Fit")
        with self.assertRaises(ValidationError):
            allocate(BLOCKS, [("A", 0)], "First Fit")
        with self.assertRaises(ValidationError):
            allocate(BLOCKS, [("A", 5), ("A", 6)], "First Fit")
        with self.assertRaises(ValidationError):
            allocate(BLOCKS, PROCS, "Next Fit")
        with self.assertRaises(ValidationError):
            allocate(BLOCKS, PROCS, "First Fit", "Paged")

    def test_large_dataset_invariants(self):
        rng = random.Random(21)
        blocks = [rng.randint(10, 500) for _ in range(400)]
        procs = [(f"P{i}", rng.randint(1, 300)) for i in range(1000)]
        for mode in (FIXED, VARIABLE):
            for strat in STRATEGIES:
                r = allocate(blocks, procs, strat, mode)
                self.assertEqual(len(r.allocated) + len(r.unallocated), 1000)
                for b in r.blocks:
                    self.assertGreaterEqual(b.free, 0)                 # never over-fill a block
                    if mode == FIXED:
                        self.assertLessEqual(len(b.allocations), 1)
                self.assertEqual(sum(b.used for b in r.blocks), sum(a.size for a in r.allocated))


if __name__ == "__main__":
    unittest.main()
