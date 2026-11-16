"""Banker's Algorithm: Need matrix, safety algorithm and resource-request check.

Need[i][j] = Max[i][j] - Allocation[i][j]
Safety algorithm: repeatedly find an unfinished process whose Need <= Work; pretend it runs
to completion and returns its Allocation (Work += Allocation). If every process can finish
this way, the state is SAFE and the order found is a safe sequence.
When several processes qualify, the one with the LOWEST index is chosen (deterministic).
"""
from dataclasses import dataclass, field

from .validation import ValidationError


@dataclass
class SafetyStep:
    process: int
    work_before: list
    need: list
    allocation: list
    work_after: list


@dataclass
class SafetyResult:
    safe: bool
    need: list
    sequence: list                      # process indices in safe order (partial when unsafe)
    steps: list = field(default_factory=list)
    blocked: list = field(default_factory=list)   # (process, need) pairs that could not run
    final_work: list = field(default_factory=list)


def calculate_need(allocation, maximum):
    return [[m - a for m, a in zip(max_row, alloc_row)]
            for max_row, alloc_row in zip(maximum, allocation)]


def validate_state(allocation, maximum, available):
    """Raise ValidationError describing every inconsistency found (shape, sign, Alloc > Max)."""
    errors = []
    if not allocation or not maximum:
        raise ValidationError("Allocation and Maximum matrices cannot be empty.")
    n, m = len(allocation), len(allocation[0])
    if m == 0:
        raise ValidationError("There must be at least one resource type.")
    if len(maximum) != n or any(len(r) != m for r in allocation) or any(len(r) != m for r in maximum):
        raise ValidationError("Allocation and Maximum must have the same dimensions.")
    if len(available) != m:
        raise ValidationError("Available must have one value per resource type.")
    for i in range(n):
        for j in range(m):
            if allocation[i][j] < 0:
                errors.append(f"Allocation[P{i}][R{j}] is negative.")
            if maximum[i][j] < 0:
                errors.append(f"Maximum[P{i}][R{j}] is negative.")
            if allocation[i][j] > maximum[i][j]:
                errors.append(f"Allocation[P{i}][R{j}] = {allocation[i][j]} exceeds "
                              f"Maximum[P{i}][R{j}] = {maximum[i][j]}.")
    for j in range(m):
        if available[j] < 0:
            errors.append(f"Available[R{j}] is negative.")
    if errors:
        more = f"\n...and {len(errors) - 10} more problem(s)." if len(errors) > 10 else ""
        raise ValidationError("Inconsistent input:\n" + "\n".join(errors[:10]) + more)


def check_safety(allocation, maximum, available):
    """Run the safety algorithm and return a SafetyResult (with step-by-step trace)."""
    validate_state(allocation, maximum, available)
    n = len(allocation)
    need = calculate_need(allocation, maximum)
    work = list(available)
    finished = [False] * n
    sequence, steps = [], []
    progress = True
    while progress and len(sequence) < n:
        progress = False
        for i in range(n):
            if not finished[i] and all(need[i][j] <= work[j] for j in range(len(work))):
                before = list(work)
                work = [w + a for w, a in zip(work, allocation[i])]
                steps.append(SafetyStep(i, before, list(need[i]), list(allocation[i]), list(work)))
                finished[i] = True
                sequence.append(i)
                progress = True
                break                       # restart scan from P0 -> lowest index first
    blocked = [(i, need[i]) for i in range(n) if not finished[i]]
    return SafetyResult(len(sequence) == n, need, sequence, steps, blocked, work)


@dataclass
class RequestResult:
    granted: bool
    reason: str
    safety: SafetyResult = None


def request_resources(allocation, maximum, available, pid, request):
    """Resource-request algorithm: can process `pid` safely be granted `request` now?"""
    validate_state(allocation, maximum, available)
    n, m = len(allocation), len(available)
    if not 0 <= pid < n:
        raise ValidationError(f"Process index must be between 0 and {n - 1}.")
    if len(request) != m or any(r < 0 for r in request):
        raise ValidationError("Request needs one non-negative value per resource type.")
    need = calculate_need(allocation, maximum)
    if any(request[j] > need[pid][j] for j in range(m)):
        return RequestResult(False, f"P{pid} asked for more than its remaining Need (error).")
    if any(request[j] > available[j] for j in range(m)):
        return RequestResult(False, f"Resources not available; P{pid} must wait.")
    new_alloc = [list(r) for r in allocation]
    new_alloc[pid] = [a + r for a, r in zip(new_alloc[pid], request)]
    new_avail = [a - r for a, r in zip(available, request)]
    safety = check_safety(new_alloc, maximum, new_avail)
    if safety.safe:
        return RequestResult(True, "Request can be granted: the resulting state is safe.", safety)
    return RequestResult(False, "Request denied: granting it would leave the system unsafe.", safety)


def explain(result, available):
    """Plain-English explanation of a SafetyResult."""
    lines = []
    lines.append("Need = Maximum - Allocation for every process/resource.")
    lines.append(f"Starting Work = Available = {available}.")
    for k, s in enumerate(result.steps, start=1):
        lines.append(f"Step {k}: P{s.process} has Need {s.need} <= Work {s.work_before}, so it can "
                     f"finish and release {s.allocation}; Work becomes {s.work_after}.")
    if result.safe:
        seq = " -> ".join(f"P{i}" for i in result.sequence)
        lines.append(f"All processes can finish: the system is SAFE. Safe sequence: <{seq}>.")
    else:
        lines.append(f"Work is now {result.final_work}, and no remaining process can have its Need met:")
        for i, need in result.blocked:
            lines.append(f"  P{i} needs {need} > Work {result.final_work}")
        lines.append("The system is UNSAFE (a deadlock is possible).")
    return "\n".join(lines)
