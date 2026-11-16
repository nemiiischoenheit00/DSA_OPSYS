"""CPU scheduling: FCFS, SJF, Priority (each non-preemptive or preemptive) and Round Robin.

All algorithms return the same ScheduleResult so the GUI and tests treat them uniformly.

Conventions (also shown in the GUI):
  * Time starts at 0. If no process has arrived yet the CPU is shown as IDLE.
  * Ties are broken by earlier arrival time, then by the order the processes were entered.
  * Priority: by default a LOWER number means HIGHER priority (configurable).
  * Round Robin: processes that arrive while another is running are queued BEFORE the
    preempted process is put back at the tail of the queue.
  * Turnaround = Completion - Arrival;  Waiting = Turnaround - Burst;
    Response = First CPU time - Arrival.
"""
import heapq
from collections import deque
from dataclasses import dataclass

from .validation import ValidationError

IDLE = "IDLE"
ALGORITHMS = ["FCFS", "SJF", "Priority", "Round Robin"]


@dataclass
class Process:
    pid: str
    arrival: int
    burst: int
    priority: int = 0


@dataclass
class GanttSegment:
    pid: str
    start: int
    end: int


@dataclass
class ProcessResult:
    pid: str
    arrival: int
    burst: int
    priority: int
    completion: int
    turnaround: int
    waiting: int
    response: int


@dataclass
class ScheduleResult:
    algorithm: str
    gantt: list
    processes: list
    avg_waiting: float
    avg_turnaround: float
    avg_response: float
    cpu_utilization: float
    total_time: int

    @property
    def execution_order(self):
        """PIDs in the order they received the CPU (idle time excluded)."""
        return [s.pid for s in self.gantt if s.pid != IDLE]


def validate_processes(processes):
    if not processes:
        raise ValidationError("Add at least one process.")
    seen = set()
    for p in processes:
        if not str(p.pid).strip():
            raise ValidationError("Every process needs a Process ID.")
        if p.pid in seen:
            raise ValidationError(f"Duplicate Process ID '{p.pid}'. IDs must be unique.")
        seen.add(p.pid)
        if p.arrival < 0:
            raise ValidationError(f"{p.pid}: arrival time cannot be negative.")
        if p.burst <= 0:
            raise ValidationError(f"{p.pid}: burst time must be greater than 0.")
        if p.priority < 0:
            raise ValidationError(f"{p.pid}: priority cannot be negative.")


class _GanttBuilder:
    """Collects segments and merges neighbouring segments of the same process."""

    def __init__(self):
        self.segments = []

    def add(self, pid, start, end):
        if end <= start:
            return
        if self.segments and self.segments[-1].pid == pid and self.segments[-1].end == start:
            self.segments[-1].end = end
        else:
            self.segments.append(GanttSegment(pid, start, end))


def _finish(label, procs, gantt, completion, first_start):
    results = []
    for i, p in enumerate(procs):
        turnaround = completion[i] - p.arrival
        results.append(ProcessResult(p.pid, p.arrival, p.burst, p.priority, completion[i],
                                     turnaround, turnaround - p.burst, first_start[i] - p.arrival))
    n = len(results)
    total_time = max(completion.values())
    busy = sum(s.end - s.start for s in gantt if s.pid != IDLE)
    return ScheduleResult(
        algorithm=label, gantt=gantt, processes=results,
        avg_waiting=sum(r.waiting for r in results) / n,
        avg_turnaround=sum(r.turnaround for r in results) / n,
        avg_response=sum(r.response for r in results) / n,
        cpu_utilization=busy / total_time if total_time else 0.0,
        total_time=total_time)


def _priority_queue_schedule(procs, key_fn, preemptive, label):
    """Shared engine for FCFS, SJF and Priority. `key_fn(i, remaining)` ranks ready processes
    (smaller key runs first). A heap keeps this fast for thousands of processes."""
    n = len(procs)
    remaining = [p.burst for p in procs]
    arrivals = sorted(range(n), key=lambda i: (procs[i].arrival, i))
    a = 0
    t = 0
    heap = []
    completion, first_start = {}, {}
    gantt = _GanttBuilder()
    while len(completion) < n:
        while a < n and procs[arrivals[a]].arrival <= t:
            i = arrivals[a]
            heapq.heappush(heap, (key_fn(i, remaining), i))
            a += 1
        if not heap:                      # nothing ready -> CPU idles until next arrival
            nxt = procs[arrivals[a]].arrival
            gantt.add(IDLE, t, nxt)
            t = nxt
            continue
        _, i = heapq.heappop(heap)
        first_start.setdefault(i, t)
        run = remaining[i]
        if preemptive and a < n:          # a newly arriving process may preempt us
            run = min(run, procs[arrivals[a]].arrival - t)
        gantt.add(procs[i].pid, t, t + run)
        t += run
        remaining[i] -= run
        if remaining[i] == 0:
            completion[i] = t
        else:
            heapq.heappush(heap, (key_fn(i, remaining), i))
    return _finish(label, procs, gantt.segments, completion, first_start)


def _round_robin(procs, quantum):
    n = len(procs)
    remaining = [p.burst for p in procs]
    arrivals = sorted(range(n), key=lambda i: (procs[i].arrival, i))
    a = 0
    t = 0
    queue = deque()
    completion, first_start = {}, {}
    gantt = _GanttBuilder()
    while len(completion) < n:
        while a < n and procs[arrivals[a]].arrival <= t:
            queue.append(arrivals[a])
            a += 1
        if not queue:
            nxt = procs[arrivals[a]].arrival
            gantt.add(IDLE, t, nxt)
            t = nxt
            continue
        i = queue.popleft()
        first_start.setdefault(i, t)
        run = min(quantum, remaining[i])
        gantt.add(procs[i].pid, t, t + run)
        t += run
        remaining[i] -= run
        while a < n and procs[arrivals[a]].arrival <= t:   # arrivals during the slice go first
            queue.append(arrivals[a])
            a += 1
        if remaining[i] > 0:
            queue.append(i)
        else:
            completion[i] = t
    return _finish(f"Round Robin (q={quantum})", procs, gantt.segments, completion, first_start)


def schedule(processes, algorithm, preemptive=False, quantum=None, lower_is_higher=True):
    """Run one scheduling algorithm. `algorithm` is one of ALGORITHMS."""
    validate_processes(processes)
    procs = list(processes)
    if algorithm == "FCFS":
        return _priority_queue_schedule(
            procs, lambda i, rem: (procs[i].arrival, i), False, "FCFS")
    if algorithm == "SJF":
        label = "SJF (preemptive / SRTF)" if preemptive else "SJF (non-preemptive)"
        return _priority_queue_schedule(
            procs, lambda i, rem: (rem[i], procs[i].arrival, i), preemptive, label)
    if algorithm == "Priority":
        sign = 1 if lower_is_higher else -1
        label = "Priority (preemptive)" if preemptive else "Priority (non-preemptive)"
        return _priority_queue_schedule(
            procs, lambda i, rem: (sign * procs[i].priority, procs[i].arrival, i), preemptive, label)
    if algorithm == "Round Robin":
        if quantum is None or int(quantum) <= 0:
            raise ValidationError("Round Robin needs a time quantum greater than 0.")
        return _round_robin(procs, int(quantum))
    raise ValidationError(f"Unknown algorithm '{algorithm}'.")


def compare_all(processes, quantum=2, lower_is_higher=True):
    """Run every variant on the same workload (used for the comparison table)."""
    runs = [("FCFS", False), ("SJF", False), ("SJF", True),
            ("Priority", False), ("Priority", True), ("Round Robin", False)]
    return [schedule(processes, alg, pre, quantum, lower_is_higher) for alg, pre in runs]
