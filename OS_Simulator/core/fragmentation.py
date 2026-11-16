"""Memory allocation and fragmentation: First Fit, Best Fit, Worst Fit.

Two partitioning modes are supported so internal and external fragmentation stay distinct:

  FIXED     - every memory block is a fixed-size partition that holds AT MOST ONE process.
              Unused space inside an allocated block is INTERNAL fragmentation.
              (Unused space inside an empty block is simply a free partition.)
  VARIABLE  - a block is a free hole. A process takes exactly what it needs and the rest of
              the block stays free as a smaller hole. There is NO internal fragmentation.
              Free space scattered over several holes can cause EXTERNAL fragmentation.

External fragmentation is reported for a process that could not be placed even though the
TOTAL free memory is large enough for it (no single hole is big enough).
Ties: the block with the lowest index wins. Processes are placed in the order entered.
"""
from dataclasses import dataclass, field

from .validation import ValidationError

FIXED = "Fixed partitions"
VARIABLE = "Variable partitions"
MODES = [FIXED, VARIABLE]
STRATEGIES = ["First Fit", "Best Fit", "Worst Fit"]


@dataclass
class AllocationRecord:
    pid: str
    size: int
    block: int
    internal_fragmentation: int


@dataclass
class BlockState:
    index: int
    size: int
    allocations: list = field(default_factory=list)   # [(pid, size)]

    @property
    def used(self):
        return sum(s for _, s in self.allocations)

    @property
    def free(self):
        return self.size - self.used


@dataclass
class Unallocated:
    pid: str
    size: int
    external: bool
    reason: str


@dataclass
class FragmentationResult:
    strategy: str
    mode: str
    blocks: list
    allocated: list
    unallocated: list
    total_memory: int
    total_internal: int
    total_free: int            # free memory other processes could still use
    largest_hole: int

    @property
    def has_external(self):
        return any(u.external for u in self.unallocated)

    @property
    def utilization(self):
        used = sum(b.used for b in self.blocks)
        return used / self.total_memory if self.total_memory else 0.0


def validate(block_sizes, processes):
    if not block_sizes:
        raise ValidationError("Define at least one memory block.")
    if not processes:
        raise ValidationError("Define at least one process.")
    if any(b <= 0 for b in block_sizes):
        raise ValidationError("Every memory block size must be greater than 0.")
    seen = set()
    for pid, size in processes:
        if not str(pid).strip():
            raise ValidationError("Every process needs an ID.")
        if pid in seen:
            raise ValidationError(f"Duplicate process ID '{pid}'. IDs must be unique.")
        seen.add(pid)
        if size <= 0:
            raise ValidationError(f"Process {pid}: memory requirement must be greater than 0.")


def _pick_block(candidates, strategy, free_of, need):
    """candidates: list of block indices that can hold `need`."""
    if strategy == "First Fit":
        return candidates[0]
    if strategy == "Best Fit":      # smallest leftover
        return min(candidates, key=lambda b: (free_of(b) - need, b))
    if strategy == "Worst Fit":     # largest leftover
        return max(candidates, key=lambda b: (free_of(b), -b))
    raise ValidationError(f"Unknown strategy '{strategy}'.")


def allocate(block_sizes, processes, strategy, mode=FIXED):
    """processes: list of (pid, size). Returns a FragmentationResult."""
    validate(block_sizes, processes)
    if mode not in MODES:
        raise ValidationError(f"Unknown mode '{mode}'.")
    blocks = [BlockState(i, s) for i, s in enumerate(block_sizes)]
    allocated, pending_unallocated = [], []
    fixed = mode == FIXED

    def can_hold(b, need):
        if fixed:
            return not b.allocations and b.size >= need
        return b.free >= need

    for pid, size in processes:
        candidates = [b.index for b in blocks if can_hold(b, size)]
        if candidates:
            idx = _pick_block(candidates, strategy, lambda i: blocks[i].free, size)
            blocks[idx].allocations.append((pid, size))
            internal = blocks[idx].size - size if fixed else 0
            allocated.append(AllocationRecord(pid, size, idx, internal))
        else:
            pending_unallocated.append((pid, size))

    # Free memory that a waiting process could still use (final state).
    if fixed:
        usable = [b.size for b in blocks if not b.allocations]
    else:
        usable = [b.free for b in blocks if b.free > 0]
    total_free = sum(usable)
    largest = max(usable) if usable else 0

    unallocated = []
    for pid, size in pending_unallocated:
        if total_free >= size:
            unallocated.append(Unallocated(
                pid, size, True,
                f"External fragmentation: {total_free} units are free in total, but the largest "
                f"single free {'partition' if fixed else 'hole'} is only {largest} (< {size})."))
        else:
            unallocated.append(Unallocated(
                pid, size, False,
                f"Not enough free memory: needs {size}, only {total_free} free in total."))
    total_internal = sum(a.internal_fragmentation for a in allocated)
    return FragmentationResult(strategy, mode, blocks, allocated, unallocated,
                               sum(block_sizes), total_internal, total_free, largest)


def explain(result):
    """Plain-English summary that separates internal from external fragmentation."""
    lines = [f"{result.strategy} with {result.mode.lower()}: "
             f"{len(result.allocated)} process(es) allocated, {len(result.unallocated)} not allocated."]
    if result.mode == FIXED:
        lines.append(f"INTERNAL fragmentation = unused space inside blocks that hold a process "
                     f"= {result.total_internal} units.")
        for a in result.allocated:
            if a.internal_fragmentation:
                lines.append(f"  {a.pid} ({a.size}) in block {a.block} "
                             f"(size {result.blocks[a.block].size}): wastes {a.internal_fragmentation}.")
    else:
        lines.append("Variable partitions split a block exactly, so INTERNAL fragmentation = 0.")
    lines.append(f"Free memory still usable: {result.total_free} units; "
                 f"largest single free {'partition' if result.mode == FIXED else 'hole'}: {result.largest_hole}.")
    ext = [u for u in result.unallocated if u.external]
    if ext:
        lines.append("EXTERNAL fragmentation occurred: enough total free memory exists, but not in one "
                     "contiguous piece, for: " + ", ".join(u.pid for u in ext) + ".")
    for u in result.unallocated:
        if not u.external:
            lines.append(f"  {u.pid} ({u.size}) could not be placed: not enough free memory overall.")
    if not ext and result.mode == VARIABLE and not result.unallocated:
        lines.append("No external fragmentation problem: every process found a hole.")
    return "\n".join(lines)
