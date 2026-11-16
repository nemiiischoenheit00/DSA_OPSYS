import random
import unittest

from core import disk_scheduling as ds
from core.disk_scheduling import DOWN, UP, run
from core.validation import ValidationError

REQ = [98, 183, 37, 122, 14, 124, 65, 67]      # classic textbook queue
HEAD, LOW, HIGH = 53, 0, 199


def go(alg, req=REQ, head=HEAD, low=LOW, high=HIGH, direction=UP, jump=True):
    return run(alg, req, head, low, high, direction, jump)


class TestDiskScheduling(unittest.TestCase):
    def test_fcfs(self):
        r = go("FCFS")
        self.assertEqual(r.service_order, REQ)
        self.assertEqual(r.total_movement, 640)

    def test_sstf(self):
        r = go("SSTF")
        self.assertEqual(r.service_order, [65, 67, 37, 14, 98, 122, 124, 183])
        self.assertEqual(r.total_movement, 236)

    def test_scan_up_goes_to_edge(self):
        r = go("SCAN")                                    # 53->199 (146) then back to 14 (185)
        self.assertEqual(r.service_order, [65, 67, 98, 122, 124, 183, 37, 14])
        self.assertEqual(r.total_movement, 331)
        self.assertIn(199, [s.position for s in r.path])  # visited the edge

    def test_scan_down(self):
        r = go("SCAN", direction=DOWN)                   # 53->0 (53) then up to 183 (183)
        self.assertEqual(r.service_order, [37, 14, 65, 67, 98, 122, 124, 183])
        self.assertEqual(r.total_movement, 236)

    def test_cscan_up_and_down(self):
        r = go("C-SCAN")                                 # 146 + 199 jump + 37
        self.assertEqual(r.service_order, [65, 67, 98, 122, 124, 183, 14, 37])
        self.assertEqual(r.total_movement, 382)
        r = go("C-SCAN", direction=DOWN)                 # 53 + 199 jump + 134
        self.assertEqual(r.service_order, [37, 14, 183, 124, 122, 98, 67, 65])
        self.assertEqual(r.total_movement, 386)

    def test_look_up_and_down(self):
        self.assertEqual(go("LOOK").total_movement, 299)                  # 130 + 169
        r = go("LOOK", direction=DOWN)                                    # 39 + 169
        self.assertEqual(r.total_movement, 208)
        self.assertEqual(r.service_order, [37, 14, 65, 67, 98, 122, 124, 183])

    def test_clook_up_and_down(self):
        r = go("C-LOOK")                                                  # 130 + 169 + 23
        self.assertEqual(r.service_order, [65, 67, 98, 122, 124, 183, 14, 37])
        self.assertEqual(r.total_movement, 322)
        self.assertEqual(go("C-LOOK", direction=DOWN).total_movement, 326)  # 39 + 169 + 118

    def test_jump_can_be_excluded(self):
        self.assertEqual(go("C-SCAN", jump=False).total_movement, 382 - 199)
        self.assertEqual(go("C-LOOK", jump=False).total_movement, 322 - 169)
        self.assertEqual(go("SCAN", jump=False).total_movement, 331)     # SCAN has no jump

    # ---- boundaries -------------------------------------------------------------------
    def test_head_at_edge(self):
        r = go("SCAN", req=[10, 20], head=0, direction=UP)
        self.assertEqual((r.service_order, r.total_movement), ([10, 20], 199))   # 0 -> 199 edge
        r = go("SCAN", req=[10, 20], head=199, direction=UP)   # already at the edge, turn immediately
        self.assertEqual(r.service_order, [20, 10])
        self.assertEqual(r.total_movement, 189)

    def test_request_equal_to_head_costs_nothing(self):
        r = go("LOOK", req=[53, 60], head=53)
        self.assertEqual(r.service_order, [53, 60])
        self.assertEqual(r.total_movement, 7)

    def test_requests_on_boundaries_not_double_counted(self):
        r = go("SCAN", req=[199, 100], head=50)                 # last request is the edge itself
        self.assertEqual(r.total_movement, 149)      # 50 -> 199
        self.assertEqual([s.position for s in r.path].count(199), 1)
        r = go("C-SCAN", req=[0, 100], head=50)                 # request exactly at the far edge
        self.assertEqual(r.service_order, [100, 0])

    def test_duplicates_and_single_request(self):
        r = go("FCFS", req=[40, 40, 40], head=40)
        self.assertEqual((len(r.service_order), r.total_movement), (3, 0))
        self.assertEqual(go("C-LOOK", req=[7], head=50).total_movement, 43)

    def test_custom_cylinder_limits(self):
        r = run("SCAN", [150, 120], 130, 100, 150, UP)         # disk is cylinders 100-150
        self.assertEqual(r.service_order, [150, 120])
        self.assertEqual(r.total_movement, 20 + 30)            # 130->150, then 150->120

    def test_invalid_input(self):
        with self.assertRaises(ValidationError):
            run("SCAN", [], 5, 0, 10)
        with self.assertRaises(ValidationError):
            run("SCAN", [5], 50, 0, 10)                        # head outside disk
        with self.assertRaises(ValidationError):
            run("SCAN", [500], 5, 0, 10)                       # request outside disk
        with self.assertRaises(ValidationError):
            run("SCAN", [5], 5, 10, 10)                        # empty cylinder range
        with self.assertRaises(ValidationError):
            run("SCAN", [5], 5, -1, 10)
        with self.assertRaises(ValidationError):
            run("ELEVATOR", [5], 5, 0, 10)
        with self.assertRaises(ValidationError):
            run("SCAN", [5], 5, 0, 10, "sideways")

    def test_registry_is_modular(self):
        ds.ALGORITHMS["FCFS-copy"] = ds.fcfs                  # adding an algorithm = one line
        try:
            self.assertEqual(go("FCFS-copy").total_movement, 640)
            self.assertIn("FCFS-copy", [r.algorithm for r in ds.compare_all(REQ, HEAD, LOW, HIGH)])
        finally:
            del ds.ALGORITHMS["FCFS-copy"]

    # ---- larger dataset ---------------------------------------------------------------
    def test_large_dataset_invariants(self):
        rng = random.Random(99)
        req = [rng.randint(0, 4999) for _ in range(3000)]
        head = 2500
        results = {r.algorithm: r for r in ds.compare_all(req, head, 0, 4999)}
        for name, r in results.items():
            self.assertEqual(sorted(r.service_order), sorted(req), name)   # every request served once
            self.assertEqual(r.total_movement, sum(r.moves))
        self.assertLessEqual(results["SSTF"].total_movement, results["FCFS"].total_movement)
        self.assertLessEqual(results["LOOK"].total_movement, results["SCAN"].total_movement)
        self.assertLessEqual(results["C-LOOK"].total_movement, results["C-SCAN"].total_movement)


if __name__ == "__main__":
    unittest.main()
