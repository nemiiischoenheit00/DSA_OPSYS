import random
import unittest

from core.cpu_scheduling import IDLE, Process, compare_all, schedule
from core.validation import ValidationError


def P(pid, arrival, burst, priority=0):
    return Process(pid, arrival, burst, priority)


class TestCPUScheduling(unittest.TestCase):
    # ---- classic textbook workloads (hand-calculated) -------------------------------
    def test_fcfs_textbook(self):
        # P1(0,24) P2(0,3) P3(0,3): waits 0,24,27 -> avg 17 ; turnaround 24,27,30 -> avg 27
        r = schedule([P("P1", 0, 24), P("P2", 0, 3), P("P3", 0, 3)], "FCFS")
        self.assertEqual(r.execution_order, ["P1", "P2", "P3"])
        self.assertEqual([p.waiting for p in r.processes], [0, 24, 27])
        self.assertEqual([p.turnaround for p in r.processes], [24, 27, 30])
        self.assertAlmostEqual(r.avg_waiting, 17)
        self.assertAlmostEqual(r.avg_turnaround, 27)

    def test_sjf_non_preemptive(self):
        # P1(0,7) P2(2,4) P3(4,1) P4(5,4): order P1,P3,P2,P4; waits 0,6,3,7 -> 4.0
        r = schedule([P("P1", 0, 7), P("P2", 2, 4), P("P3", 4, 1), P("P4", 5, 4)], "SJF")
        self.assertEqual(r.execution_order, ["P1", "P3", "P2", "P4"])
        self.assertEqual([p.waiting for p in r.processes], [0, 6, 3, 7])
        self.assertAlmostEqual(r.avg_waiting, 4.0)

    def test_srtf_preemptive(self):
        # Same workload: SRTF gantt P1 0-2, P2 2-4, P3 4-5, P2 5-7, P4 7-11, P1 11-16
        r = schedule([P("P1", 0, 7), P("P2", 2, 4), P("P3", 4, 1), P("P4", 5, 4)], "SJF", preemptive=True)
        self.assertEqual([(s.pid, s.start, s.end) for s in r.gantt],
                         [("P1", 0, 2), ("P2", 2, 4), ("P3", 4, 5), ("P2", 5, 7), ("P4", 7, 11), ("P1", 11, 16)])
        self.assertEqual([p.waiting for p in r.processes], [9, 1, 0, 2])
        self.assertAlmostEqual(r.avg_waiting, 3.0)

    def test_round_robin_textbook(self):
        # P1=24,P2=3,P3=3, q=4 -> P1 P2 P3 P1(x5). waits 6,4,7 -> avg 5.666..
        r = schedule([P("P1", 0, 24), P("P2", 0, 3), P("P3", 0, 3)], "Round Robin", quantum=4)
        self.assertEqual(r.execution_order, ["P1", "P2", "P3", "P1"])  # last P1 slices merge
        self.assertEqual([(s.pid, s.start, s.end) for s in r.gantt],
                         [("P1", 0, 4), ("P2", 4, 7), ("P3", 7, 10), ("P1", 10, 30)])
        self.assertEqual([p.waiting for p in r.processes], [6, 4, 7])
        self.assertAlmostEqual(r.avg_waiting, 17 / 3)

    def test_round_robin_arrival_before_requeue(self):
        # q=2: P1(0,5) P2(1,3) P3(3,1).
        # t=0 P1 runs 0-2; P2 (arrived t=1) is queued BEFORE P1 -> queue [P2,P1]
        # t=2 P2 runs 2-4; P3 (arrived t=3) queued before P2 -> queue [P1,P3,P2]
        # then P1 4-6, P3 6-7, P2 7-8, P1 8-9
        r = schedule([P("P1", 0, 5), P("P2", 1, 3), P("P3", 3, 1)], "Round Robin", quantum=2)
        self.assertEqual([(s.pid, s.start, s.end) for s in r.gantt],
                         [("P1", 0, 2), ("P2", 2, 4), ("P1", 4, 6), ("P3", 6, 7), ("P2", 7, 8), ("P1", 8, 9)])
        done = {p.pid: p.completion for p in r.processes}
        self.assertEqual(done, {"P1": 9, "P2": 8, "P3": 7})

    def test_priority_non_preemptive(self):
        # lower number = higher priority. All arrive at 0: order by priority.
        r = schedule([P("P1", 0, 10, 3), P("P2", 0, 1, 1), P("P3", 0, 2, 4), P("P4", 0, 1, 5), P("P5", 0, 5, 2)],
                     "Priority")
        self.assertEqual(r.execution_order, ["P2", "P5", "P1", "P3", "P4"])
        self.assertAlmostEqual(r.avg_waiting, 8.2)

    def test_priority_preemptive(self):
        # P1(0,4,prio3) is preempted at t=1 by P2(1,2,prio1)
        r = schedule([P("P1", 0, 4, 3), P("P2", 1, 2, 1)], "Priority", preemptive=True)
        self.assertEqual([(s.pid, s.start, s.end) for s in r.gantt],
                         [("P1", 0, 1), ("P2", 1, 3), ("P1", 3, 6)])

    def test_priority_higher_number_is_higher(self):
        r = schedule([P("A", 0, 2, 1), P("B", 0, 2, 9)], "Priority", lower_is_higher=False)
        self.assertEqual(r.execution_order, ["B", "A"])

    # ---- boundary / exceptional cases -------------------------------------------------
    def test_idle_time_at_start_and_middle(self):
        r = schedule([P("A", 3, 2), P("B", 10, 1)], "FCFS")
        self.assertEqual([(s.pid, s.start, s.end) for s in r.gantt],
                         [(IDLE, 0, 3), ("A", 3, 5), (IDLE, 5, 10), ("B", 10, 11)])
        self.assertAlmostEqual(r.cpu_utilization, 3 / 11)
        self.assertEqual([p.waiting for p in r.processes], [0, 0])

    def test_idle_in_round_robin(self):
        r = schedule([P("A", 5, 2)], "Round Robin", quantum=1)
        self.assertEqual([(s.pid, s.start, s.end) for s in r.gantt], [(IDLE, 0, 5), ("A", 5, 7)])

    def test_ties_broken_by_arrival_then_entry_order(self):
        r = schedule([P("A", 0, 3), P("B", 0, 3), P("C", 0, 3)], "SJF")
        self.assertEqual(r.execution_order, ["A", "B", "C"])

    def test_single_process(self):
        r = schedule([P("X", 0, 5)], "Round Robin", quantum=100)
        self.assertEqual(r.avg_waiting, 0)
        self.assertEqual(r.avg_turnaround, 5)

    def test_quantum_larger_than_bursts_equals_fcfs(self):
        procs = [P("A", 0, 3), P("B", 1, 4), P("C", 2, 2)]
        rr = schedule(procs, "Round Robin", quantum=50)
        fc = schedule(procs, "FCFS")
        self.assertEqual(rr.execution_order, fc.execution_order)
        self.assertEqual(rr.avg_waiting, fc.avg_waiting)

    def test_response_time(self):
        r = schedule([P("P1", 0, 24), P("P2", 0, 3), P("P3", 0, 3)], "Round Robin", quantum=4)
        self.assertEqual([p.response for p in r.processes], [0, 4, 7])

    # ---- invalid input ----------------------------------------------------------------
    def test_invalid_inputs(self):
        with self.assertRaises(ValidationError):
            schedule([], "FCFS")
        with self.assertRaises(ValidationError):
            schedule([P("A", 0, 0)], "FCFS")                    # zero burst
        with self.assertRaises(ValidationError):
            schedule([P("A", -1, 3)], "FCFS")                   # negative arrival
        with self.assertRaises(ValidationError):
            schedule([P("A", 0, 3, -2)], "Priority")            # negative priority
        with self.assertRaises(ValidationError):
            schedule([P("A", 0, 3), P("A", 1, 2)], "FCFS")      # duplicate id
        with self.assertRaises(ValidationError):
            schedule([P("", 0, 3)], "FCFS")                     # blank id
        with self.assertRaises(ValidationError):
            schedule([P("A", 0, 3)], "Round Robin", quantum=0)
        with self.assertRaises(ValidationError):
            schedule([P("A", 0, 3)], "Round Robin")             # missing quantum
        with self.assertRaises(ValidationError):
            schedule([P("A", 0, 3)], "Lottery")

    # ---- larger dataset ---------------------------------------------------------------
    def test_large_dataset_invariants(self):
        rng = random.Random(7)
        procs = [P(f"P{i}", rng.randint(0, 3000), rng.randint(1, 30), rng.randint(0, 9)) for i in range(2000)]
        for r in compare_all(procs, quantum=4):
            self.assertEqual(len(r.processes), 2000)
            busy = sum(s.end - s.start for s in r.gantt if s.pid != IDLE)
            self.assertEqual(busy, sum(p.burst for p in procs))          # CPU work conserved
            for pr in r.processes:
                self.assertEqual(pr.turnaround, pr.completion - pr.arrival)
                self.assertEqual(pr.waiting, pr.turnaround - pr.burst)
                self.assertGreaterEqual(pr.waiting, 0)
            for a, b in zip(r.gantt, r.gantt[1:]):                        # gantt is gap-free
                self.assertEqual(a.end, b.start)

    def test_srtf_never_worse_than_fcfs_average_waiting(self):
        rng = random.Random(3)
        procs = [P(f"P{i}", rng.randint(0, 50), rng.randint(1, 20)) for i in range(60)]
        self.assertLessEqual(schedule(procs, "SJF", preemptive=True).avg_waiting,
                             schedule(procs, "FCFS").avg_waiting)


if __name__ == "__main__":
    unittest.main()
