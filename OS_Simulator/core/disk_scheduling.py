"""Disk scheduling: FCFS, SSTF, SCAN, C-SCAN, LOOK, C-LOOK.

Modular design: every algorithm is a function  f(requests, head, low, high, direction)
returning a list of Step objects (the path of the head). The registry ALGORITHMS maps a
display name to its function, so an algorithm can be added or removed by editing one line.

Conventions (also shown in the GUI):
  * Cylinders run from `low` to `high` (inclusive). direction is UP (towards `high`) or DOWN.
  * SCAN / C-SCAN always travel all the way to the disk edge (`high` or `low`) before turning.
    LOOK / C-LOOK only travel as far as the last request in that direction.
  * SCAN reverses direction at the edge; if nothing is left to serve it stops there.
  * C-SCAN / C-LOOK jump back to the opposite side without servicing on the way. The jump is
    counted as head movement by default (the option `count_jump=False` ignores it).
  * A request located exactly at the head position is served first at 0 cost.
  * SSTF ties (equal distance) are broken by the order requests were entered.
"""
from dataclasses import dataclass

from .validation import ValidationError

UP = "Towards higher cylinders"
DOWN = "Towards lower cylinders"
DIRECTIONS = [UP, DOWN]


@dataclass
class Step:
    position: int
    kind: str            # "start", "request", "edge" or "jump"
    jump: bool = False   # True when this move is a C-SCAN/C-LOOK return jump
    served: bool = False  # True when a request is serviced at this position


@dataclass
class DiskResult:
    algorithm: str
    path: list
    total_movement: int
    moves: list          # movement of each step (first entry 0 for the start)

    @property
    def service_order(self):
        return [s.position for s in self.path if s.served]


def validate(requests, head, low, high):
    if low < 0:
        raise ValidationError("The lowest cylinder cannot be negative.")
    if high <= low:
        raise ValidationError("The highest cylinder must be greater than the lowest cylinder.")
    if not requests:
        raise ValidationError("Enter at least one disk request.")
    if not low <= head <= high:
        raise ValidationError(f"The head position {head} must be between {low} and {high}.")
    bad = [r for r in requests if not low <= r <= high]
    if bad:
        shown = ", ".join(str(b) for b in bad[:8]) + (" ..." if len(bad) > 8 else "")
        raise ValidationError(f"Request(s) outside the cylinder range {low}-{high}: {shown}")


# ---------------------------------------------------------------- algorithms
def fcfs(requests, head, low, high, direction):
    path = [Step(head, "start")]
    path += [Step(r, "request", served=True) for r in requests]
    return path


def sstf(requests, head, low, high, direction):
    path = [Step(head, "start")]
    pending = list(requests)
    pos = head
    while pending:
        k = min(range(len(pending)), key=lambda i: (abs(pending[i] - pos), i))
        pos = pending.pop(k)
        path.append(Step(pos, "request", served=True))
    return path


def _split(requests, head, direction):
    """(ahead, behind): requests in the current direction (nearest first) and the rest."""
    if direction == UP:
        ahead = sorted(r for r in requests if r >= head)
        behind = sorted((r for r in requests if r < head), reverse=True)
    else:
        ahead = sorted((r for r in requests if r <= head), reverse=True)
        behind = sorted(r for r in requests if r > head)
    return ahead, behind


def _serve(path, positions):
    path.extend(Step(r, "request", served=True) for r in positions)


def scan(requests, head, low, high, direction):
    path = [Step(head, "start")]
    ahead, behind = _split(requests, head, direction)
    edge = high if direction == UP else low
    _serve(path, ahead)
    if path[-1].position != edge:
        path.append(Step(edge, "edge"))
    _serve(path, behind)                 # moving back the other way, nearest first
    return path


def cscan(requests, head, low, high, direction):
    path = [Step(head, "start")]
    ahead, behind = _split(requests, head, direction)
    edge, far = (high, low) if direction == UP else (low, high)
    _serve(path, ahead)
    if path[-1].position != edge:
        path.append(Step(edge, "edge"))
    if behind:
        path.append(Step(far, "jump", jump=True))
        # after the jump we sweep in the SAME direction starting from the far edge
        rest = sorted(behind) if direction == UP else sorted(behind, reverse=True)
        if rest[0] == far:                # first remaining request is exactly at the far edge
            path[-1].served = True
            rest = rest[1:]
        _serve(path, rest)
    return path


def look(requests, head, low, high, direction):
    path = [Step(head, "start")]
    ahead, behind = _split(requests, head, direction)
    _serve(path, ahead)
    _serve(path, behind)
    return path


def clook(requests, head, low, high, direction):
    path = [Step(head, "start")]
    ahead, behind = _split(requests, head, direction)
    _serve(path, ahead)
    if behind:
        rest = sorted(behind) if direction == UP else sorted(behind, reverse=True)
        path.append(Step(rest[0], "jump", jump=True, served=True))   # jump lands on a request
        _serve(path, rest[1:])
    return path


ALGORITHMS = {
    "FCFS": fcfs,
    "SSTF": sstf,
    "SCAN": scan,
    "C-SCAN": cscan,
    "LOOK": look,
    "C-LOOK": clook,
}


def run(algorithm, requests, head, low, high, direction=UP, count_jump=True):
    validate(requests, head, low, high)
    if algorithm not in ALGORITHMS:
        raise ValidationError(f"Unknown algorithm '{algorithm}'.")
    if direction not in DIRECTIONS:
        raise ValidationError("Choose a valid scan direction.")
    path = ALGORITHMS[algorithm](list(requests), head, low, high, direction)
    moves = [0]
    for prev, cur in zip(path, path[1:]):
        moves.append(0 if (cur.jump and not count_jump) else abs(cur.position - prev.position))
    return DiskResult(algorithm, path, sum(moves), moves)


def compare_all(requests, head, low, high, direction=UP, count_jump=True):
    return [run(name, requests, head, low, high, direction, count_jump) for name in ALGORITHMS]
