"""Multiprogramming simulation: several programs share main memory and one CPU.

Model (documented in the GUI):
  * Memory is `total_memory` units; `os_reserved` units at address 0 belong to the OS.
  * Jobs arrive over time. A job is LOADED into the first free contiguous hole that is big
    enough (first fit). If no hole fits, it WAITS in the job queue.
  * A job larger than the usable memory (total - OS) can never run -> REJECTED ("cannot fit").
  * Loaded jobs share the single CPU using Round Robin with `quantum` time units.
  * When a job finishes it is REMOVED (unloaded) and its memory is freed; waiting jobs are
    then considered for loading.
  * Admission policy: strict FCFS (a blocked job at the head of the queue blocks the ones
    behind it) or, if `allow_bypass` is True, smaller jobs may jump ahead of a blocked one.
  * Scheduling decisions (and therefore loading) happen at slice boundaries.
  * CPU utilisation = busy time / total time.
    Memory utilisation = time-weighted average of (memory in use / usable memory).
"""
from collections import deque
from dataclasses import dataclass, field

from .validation import ValidationError


@dataclass
class Job:
    pid: str
    memory: int
    arrival: int
    burst: int


@dataclass
class Placement:
    pid: str
    start: int
    size: int
    remaining: int


@dataclass
class Snapshot:
    time: int
    event: str
    loaded: list          # [Placement]
    waiting: list         # [pid]
    rejected: list        # [pid]
    finished: list        # [pid]
    used: int
    free: int
    largest_hole: int


@dataclass
class JobResult:
    pid: str
    memory: int
    arrival: int
    burst: int
    status: str                    # "Completed" or "Rejected (cannot fit)"
    load_time: object = None
    completion: object = None

    @property
    def memory_wait(self):
        return None if self.load_time is None else self.load_time - self.arrival

    @property
    def turnaround(self):
        return None if self.completion is None else self.completion - self.arrival


@dataclass
class MultiResult:
    total_memory: int
    os_reserved: int
    jobs: list
    snapshots: list
    total_time: int
    cpu_utilization: float
    memory_utilization: float
    peak_memory_used: int
    avg_degree: float              # average number of programs in memory over time
    throughput: float              # completed jobs per time unit


def validate(jobs, total_memory, os_reserved, quantum):
    if total_memory <= 0:
        raise ValidationError("Total memory must be greater than 0.")
    if os_reserved < 0 or os_reserved >= total_memory:
        raise ValidationError("OS-reserved memory must be 0 or more and smaller than total memory.")
    if quantum <= 0:
        raise ValidationError("The time quantum must be greater than 0.")
    if not jobs:
        raise ValidationError("Add at least one program.")
    seen = set()
    for j in jobs:
        if not str(j.pid).strip():
            raise ValidationError("Every program needs an ID.")
        if j.pid in seen:
            raise ValidationError(f"Duplicate program ID '{j.pid}'. IDs must be unique.")
        seen.add(j.pid)
        if j.memory <= 0:
            raise ValidationError(f"{j.pid}: memory requirement must be greater than 0.")
        if j.arrival < 0:
            raise ValidationError(f"{j.pid}: arrival time cannot be negative.")
        if j.burst <= 0:
            raise ValidationError(f"{j.pid}: CPU burst must be greater than 0.")


def _holes(loaded, os_reserved, total):
    """Free contiguous holes as (start, size), in address order."""
    holes, cursor = [], os_reserved
    for p in sorted(loaded, key=lambda p: p.start):
        if p.start > cursor:
            holes.append((cursor, p.start - cursor))
        cursor = p.start + p.size
    if cursor < total:
        holes.append((cursor, total - cursor))
    return holes


def simulate(jobs, total_memory, os_reserved=0, quantum=2, allow_bypass=False):
    validate(jobs, total_memory, os_reserved, quantum)
    usable = total_memory - os_reserved
    results = {j.pid: JobResult(j.pid, j.memory, j.arrival, j.burst, "Completed") for j in jobs}

    incoming = deque(j for _, j in sorted(enumerate(jobs), key=lambda x: (x[1].arrival, x[0])))
    waiting = deque()                  # jobs that arrived but are not in memory
    loaded = []                        # [Placement]
    ready = deque()                    # pids in loaded, in round-robin order
    rejected, finished = [], []
    snapshots = []
    t = 0
    busy = 0
    mem_time = 0                       # sum(memory in use * duration)
    degree_time = 0                    # sum(programs in memory * duration)
    peak = 0

    def used_now():
        return sum(p.size for p in loaded)

    def snap(event):
        holes = _holes(loaded, os_reserved, total_memory)
        snapshots.append(Snapshot(
            t, event, [Placement(p.pid, p.start, p.size, p.remaining) for p in loaded],
            [j.pid for j in waiting], list(rejected), list(finished),
            used_now(), usable - used_now(), max((h[1] for h in holes), default=0)))

    def admit_arrivals():
        while incoming and incoming[0].arrival <= t:
            j = incoming.popleft()
            if j.memory > usable:
                rejected.append(j.pid)
                results[j.pid].status = "Rejected (cannot fit)"
                snap(f"{j.pid} arrived: needs {j.memory} > usable memory {usable} -> REJECTED")
            else:
                waiting.append(j)
                snap(f"{j.pid} arrived and joined the job queue")

    def try_load():
        nonlocal peak
        for j in list(waiting):
            hole = next((h for h in _holes(loaded, os_reserved, total_memory) if h[1] >= j.memory), None)
            if hole is None:
                if allow_bypass:
                    continue
                break                                   # strict FCFS: blocked job blocks the rest
            waiting.remove(j)
            loaded.append(Placement(j.pid, hole[0], j.memory, j.burst))
            ready.append(j.pid)
            results[j.pid].load_time = t
            peak = max(peak, used_now())
            snap(f"{j.pid} LOADED at address {hole[0]} ({j.memory} units)")

    admit_arrivals()
    try_load()
    while incoming or waiting or loaded:
        if not ready:
            if not incoming:                            # nothing can ever run again
                break
            nxt = incoming[0].arrival
            mem_time += used_now() * (nxt - t)
            t = nxt
            admit_arrivals()
            try_load()
            continue
        pid = ready.popleft()
        place = next(p for p in loaded if p.pid == pid)
        run = min(quantum, place.remaining)
        mem_time += used_now() * run
        degree_time += len(loaded) * run
        busy += run
        t += run
        place.remaining -= run
        admit_arrivals()
        if place.remaining > 0:
            ready.append(pid)
        else:
            loaded.remove(place)
            finished.append(pid)
            results[pid].completion = t
            snap(f"{pid} FINISHED and was REMOVED; {place.size} units freed")
        try_load()

    total_time = t
    completed = sum(1 for r in results.values() if r.completion is not None)
    return MultiResult(
        total_memory, os_reserved, [results[j.pid] for j in jobs], snapshots, total_time,
        busy / total_time if total_time else 0.0,
        mem_time / (usable * total_time) if total_time else 0.0,
        peak, degree_time / total_time if total_time else 0.0,
        completed / total_time if total_time else 0.0)
