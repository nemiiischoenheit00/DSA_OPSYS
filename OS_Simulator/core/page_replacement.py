"""Page replacement: FIFO, LRU and Optimal.

One shared simulation loop (`_simulate`) is used by all three algorithms; each algorithm
only differs in HOW it picks a victim page, so there is no duplicated logic.

Frame display: the frames are numbered slots. A new page goes into the first empty slot; when
a victim is evicted the new page takes the victim's slot (so columns in the step table stay stable).

Tie rule: if several pages qualify as a victim (e.g. Optimal with pages that are never used
again) the page that has been in memory the longest (oldest load time) is replaced.
"""
from bisect import bisect_right
from dataclasses import dataclass

from .validation import ValidationError

ALGORITHMS = ["FIFO", "LRU", "Optimal"]
INF = float("inf")


@dataclass
class Step:
    index: int            # 1-based position in the reference string
    page: int
    frames: list          # snapshot of frame slots after this reference (None = empty)
    hit: bool
    replaced: object      # evicted page, or None
    reason: str


@dataclass
class ReplacementResult:
    algorithm: str
    num_frames: int
    steps: list
    hits: int
    faults: int

    @property
    def total(self):
        return self.hits + self.faults

    @property
    def hit_ratio(self):
        return self.hits / self.total if self.total else 0.0

    @property
    def fault_ratio(self):
        return self.faults / self.total if self.total else 0.0


def validate(refs, num_frames):
    if not refs:
        raise ValidationError("The page reference string is empty.")
    if any(r < 0 for r in refs):
        raise ValidationError("Page numbers cannot be negative.")
    if num_frames <= 0:
        raise ValidationError("The number of frames must be at least 1.")


def _simulate(refs, num_frames, name, choose_victim):
    """choose_victim(resident_pages, i, loaded_at, last_used) -> (victim_page, reason)."""
    slots = [None] * num_frames
    loaded_at, last_used = {}, {}
    steps, hits, faults = [], 0, 0
    for i, page in enumerate(refs):
        replaced, reason = None, ""
        if page in loaded_at:                       # page hit
            hits += 1
            hit = True
            reason = "Page is already in a frame (hit)."
        else:                                       # page fault
            faults += 1
            hit = False
            if None in slots:
                slots[slots.index(None)] = page
                reason = "Page fault: a free frame was available, nothing replaced."
            else:
                resident = [p for p in slots]
                replaced, why = choose_victim(resident, i, loaded_at, last_used)
                slots[slots.index(replaced)] = page
                del loaded_at[replaced]
                reason = f"Page fault: frames full, replaced page {replaced} ({why})."
            loaded_at[page] = i
        last_used[page] = i
        steps.append(Step(i + 1, page, list(slots), hit, replaced, reason))
    return ReplacementResult(name, num_frames, steps, hits, faults)


def fifo(refs, num_frames):
    def victim(resident, i, loaded_at, last_used):
        v = min(resident, key=lambda p: loaded_at[p])
        return v, f"FIFO: it entered memory first, at step {loaded_at[v] + 1}"
    validate(refs, num_frames)
    return _simulate(refs, num_frames, "FIFO", victim)


def lru(refs, num_frames):
    def victim(resident, i, loaded_at, last_used):
        v = min(resident, key=lambda p: last_used[p])
        return v, f"LRU: least recently used, last used at step {last_used[v] + 1}"
    validate(refs, num_frames)
    return _simulate(refs, num_frames, "LRU", victim)


def optimal(refs, num_frames):
    validate(refs, num_frames)
    positions = {}                                  # page -> sorted list of reference indices
    for idx, p in enumerate(refs):
        positions.setdefault(p, []).append(idx)

    def next_use(page, i):
        lst = positions[page]
        k = bisect_right(lst, i)
        return lst[k] if k < len(lst) else INF

    def victim(resident, i, loaded_at, last_used):
        # farthest next use wins; ties (e.g. never used again) -> oldest loaded page
        v = max(resident, key=lambda p: (next_use(p, i), -loaded_at[p]))
        nu = next_use(v, i)
        why = ("Optimal: it is never used again" if nu == INF
               else f"Optimal: its next use is farthest away, at step {nu + 1}")
        return v, why

    return _simulate(refs, num_frames, "Optimal", victim)


RUNNERS = {"FIFO": fifo, "LRU": lru, "Optimal": optimal}


def run(algorithm, refs, num_frames):
    if algorithm not in RUNNERS:
        raise ValidationError(f"Unknown algorithm '{algorithm}'.")
    return RUNNERS[algorithm](refs, num_frames)


def compare_all(refs, num_frames):
    return [RUNNERS[name](refs, num_frames) for name in ALGORITHMS]
