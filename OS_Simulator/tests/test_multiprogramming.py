import random
import unittest

from core.multiprogramming import Job, simulate
from core.validation import ValidationError


def result_by_pid(res):
    return {j.pid: j for j in res.jobs}


class TestMultiprogramming(unittest.TestCase):
    def test_hand_calculated_example(self):
        # 100 units, q=2. A,B (40 each, burst 4) load at t=0; C (40, burst 2) must wait (only 20 free).
        # A 0-2, B 2-4, A 4-6 (done, C loaded at 6), B 6-8 (done), C 8-10 (done).
        jobs = [Job("A", 40, 0, 4), Job("B", 40, 0, 4), Job("C", 40, 0, 2)]
        r = simulate(jobs, 100, 0, 2)
        by = result_by_pid(r)
        self.assertEqual((by["A"].completion, by["B"].completion, by["C"].completion), (6, 8, 10))
        self.assertEqual(by["C"].load_time, 6)
        self.assertEqual(by["C"].memory_wait, 6)
        self.assertEqual(r.total_time, 10)
        self.assertAlmostEqual(r.cpu_utilization, 1.0)
        self.assertAlmostEqual(r.memory_utilization, 0.72)      # (80*6 + 80*2 + 40*2) / (100*10)
        self.assertAlmostEqual(r.avg_degree, 1.8)               # (2*6 + 2*2 + 1*2) / 10
        self.assertEqual(r.peak_memory_used, 80)
        self.assertAlmostEqual(r.throughput, 0.3)

    def test_snapshots_show_loaded_and_waiting(self):
        jobs = [Job("A", 40, 0, 4), Job("B", 40, 0, 4), Job("C", 40, 0, 2)]
        r = simulate(jobs, 100, 0, 2)
        after_loads = [s for s in r.snapshots if s.time == 0][-1]
        self.assertEqual([p.pid for p in after_loads.loaded], ["A", "B"])
        self.assertEqual(after_loads.waiting, ["C"])
        self.assertEqual((after_loads.used, after_loads.free), (80, 20))
        removed = [s for s in r.snapshots if "FINISHED" in s.event][0]
        self.assertEqual(removed.time, 6)

    def test_os_reserved_memory_and_addresses(self):
        r = simulate([Job("A", 30, 0, 1), Job("B", 30, 0, 1)], 100, 40, 1)
        first = [s for s in r.snapshots if "LOADED" in s.event]
        self.assertEqual(first[0].loaded[0].start, 40)          # placed after the OS area
        self.assertEqual(first[1].loaded[1].start, 70)
        self.assertEqual(first[1].free, 0)                      # usable memory = 60, all used

    def test_job_too_large_is_rejected(self):
        r = simulate([Job("BIG", 500, 0, 3), Job("OK", 50, 0, 3)], 100, 0, 2)
        by = result_by_pid(r)
        self.assertTrue(by["BIG"].status.startswith("Rejected"))
        self.assertIsNone(by["BIG"].completion)
        self.assertEqual(by["OK"].status, "Completed")
        self.assertIn("BIG", r.snapshots[-1].rejected)

    def test_everything_rejected(self):
        r = simulate([Job("X", 999, 0, 1)], 100)
        self.assertEqual(r.total_time, 0)
        self.assertEqual(r.cpu_utilization, 0)

    def test_idle_cpu_waiting_for_arrival(self):
        r = simulate([Job("A", 10, 5, 3)], 50, 0, 3)
        self.assertEqual(r.total_time, 8)
        self.assertAlmostEqual(r.cpu_utilization, 3 / 8)
        self.assertEqual(result_by_pid(r)["A"].load_time, 5)

    def test_strict_fcfs_versus_bypass(self):
        jobs = [Job("A", 60, 0, 6), Job("B", 60, 0, 2), Job("C", 30, 0, 2)]
        strict = result_by_pid(simulate(jobs, 100, 0, 2, allow_bypass=False))
        bypass = result_by_pid(simulate(jobs, 100, 0, 2, allow_bypass=True))
        self.assertGreater(strict["C"].load_time, 0)            # blocked behind B
        self.assertEqual(bypass["C"].load_time, 0)              # jumped ahead of B
        for res in (strict, bypass):
            self.assertTrue(all(j.status == "Completed" for j in res.values()))

    def test_contiguous_memory_external_fragmentation(self):
        # After A finishes there are 40 free units in total but only 30 contiguous -> D must wait.
        jobs = [Job("A", 30, 0, 1), Job("B", 30, 0, 10), Job("C", 30, 0, 10), Job("D", 40, 0, 1)]
        r = simulate(jobs, 100, 0, 2)
        after_a = [s for s in r.snapshots if "A FINISHED" in s.event][0]
        self.assertEqual((after_a.free, after_a.largest_hole), (40, 30))
        self.assertEqual(after_a.waiting, ["D"])
        self.assertGreater(result_by_pid(r)["D"].load_time, after_a.time)

    def test_invalid_input(self):
        ok = [Job("A", 10, 0, 1)]
        for kwargs in ({"total_memory": 0}, {"total_memory": 100, "os_reserved": 100},
                       {"total_memory": 100, "os_reserved": -1}, {"total_memory": 100, "quantum": 0}):
            args = dict(jobs=ok, total_memory=100, os_reserved=0, quantum=2)
            args.update(kwargs)
            with self.assertRaises(ValidationError):
                simulate(**args)
        for bad in ([], [Job("A", 0, 0, 1)], [Job("A", 5, -1, 1)], [Job("A", 5, 0, 0)],
                    [Job("A", 5, 0, 1), Job("A", 5, 0, 1)], [Job("", 5, 0, 1)]):
            with self.assertRaises(ValidationError):
                simulate(bad, 100)

    def test_large_dataset_invariants(self):
        rng = random.Random(8)
        jobs = [Job(f"J{i}", rng.randint(1, 60), rng.randint(0, 400), rng.randint(1, 15)) for i in range(500)]
        r = simulate(jobs, 200, 20, 3)
        self.assertTrue(all(j.status == "Completed" for j in r.jobs))
        for s in r.snapshots:
            self.assertLessEqual(s.used, 180)
            self.assertEqual(s.used + s.free, 180)
            spans = sorted((p.start, p.start + p.size) for p in s.loaded)
            for (a0, a1), (b0, b1) in zip(spans, spans[1:]):
                self.assertLessEqual(a1, b0)                    # loaded programs never overlap
            if spans:
                self.assertGreaterEqual(spans[0][0], 20)        # nothing inside the OS area
                self.assertLessEqual(spans[-1][1], 200)
        busy = r.cpu_utilization * r.total_time
        self.assertAlmostEqual(busy, sum(j.burst for j in jobs))
        self.assertTrue(0 < r.memory_utilization <= 1)


if __name__ == "__main__":
    unittest.main()
