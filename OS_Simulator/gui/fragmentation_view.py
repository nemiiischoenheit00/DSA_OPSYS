"""GUI page for Module 6: Fragmentation (First / Best / Worst Fit)."""
import random
import tkinter as tk
from tkinter import ttk

from core import fragmentation as frag
from core.validation import ValidationError
from .base_view import ModuleView
from .charts import MemoryMap
from .widgets import DynamicTable, OutputText, ResultTable, labeled_combo

BLOCKS_EXAMPLE = [["B1", "100"], ["B2", "500"], ["B3", "200"], ["B4", "300"], ["B5", "600"]]
PROCS_EXAMPLE = [["P1", "212"], ["P2", "417"], ["P3", "112"], ["P4", "426"]]


class FragmentationView(ModuleView):
    title = "Fragmentation"
    description = ("Enter memory blocks and processes, pick an allocation strategy and a partition type. "
                   "Fixed partitions show INTERNAL fragmentation (waste inside an allocated block); variable "
                   "partitions show EXTERNAL fragmentation (free memory split into holes too small to use).")

    def build_inputs(self, parent):
        both = ttk.Frame(parent, style="Card.TFrame")
        both.pack(fill="x")
        left = ttk.Frame(both, style="Card.TFrame")
        left.pack(side="left", fill="x", expand=True, padx=(0, 16))
        ttk.Label(left, text="Memory blocks", style="Card.TLabel").pack(anchor="w", pady=(0, 4))
        self.blocks = DynamicTable(left, ["Block", "Size"], widths=[10, 10], auto_fill=lambda n: f"B{n + 1}",
                                   height=200, item_name="block",
                                   generator=lambda n: [[f"B{i + 1}", str(random.randint(20, 600))] for i in range(n)])
        self.blocks.pack(fill="x")
        right = ttk.Frame(both, style="Card.TFrame")
        right.pack(side="left", fill="x", expand=True)
        ttk.Label(right, text="Processes", style="Card.TLabel").pack(anchor="w", pady=(0, 4))
        self.procs = DynamicTable(right, ["Process", "Size"], widths=[10, 10], auto_fill=lambda n: f"P{n + 1}",
                                  height=200, item_name="process",
                                  generator=lambda n: [[f"P{i + 1}", str(random.randint(10, 400))] for i in range(n)])
        self.procs.pack(fill="x")

        opts = ttk.Frame(parent, style="Card.TFrame")
        opts.pack(fill="x", pady=(10, 0))
        self.strategy = tk.StringVar(value="First Fit")
        self.mode = tk.StringVar(value=frag.FIXED)
        labeled_combo(opts, "Strategy", self.strategy, frag.STRATEGIES, 12).pack(side="left", padx=(0, 14))
        labeled_combo(opts, "Partition type", self.mode, frag.MODES, 20).pack(side="left", padx=(0, 14))
        ttk.Button(opts, text="Load example", style="Small.TButton",
                   command=lambda: self.safe_call(self.load_example)).pack(side="right")
        ttk.Label(parent, style="CardMuted.TLabel", wraplength=1000, justify="left",
                  text="Fixed: each block holds at most one process; unused space inside it is internal "
                       "fragmentation. Variable: a block is a hole that is split; leftovers stay free. "
                       "Processes are placed in the order entered; ties go to the lowest block number."
                  ).pack(anchor="w", pady=(8, 0))
        self.reset()

    def load_example(self):
        self.blocks.set_rows(BLOCKS_EXAMPLE)
        self.procs.set_rows(PROCS_EXAMPLE)

    def reset(self):
        self.blocks.clear()
        self.procs.clear()
        for _ in range(2):
            self.blocks.add_row()
            self.procs.add_row()
        self.strategy.set("First Fit")
        self.mode.set(frag.FIXED)

    def read(self):
        brows, prows = self.blocks.rows(), self.procs.rows()
        self.require_rows(brows, "memory block")
        self.require_rows(prows, "process")
        names, sizes = [], []
        for row_no, (name, size) in brows:
            if size == "":
                raise ValidationError(f"Block row {row_no}: size is empty.")
            names.append(name or f"B{row_no}")
            sizes.append(self.int_cell(row_no, "Block size", size, 1))
        procs = []
        for row_no, (pid, size) in prows:
            if pid == "" or size == "":
                raise ValidationError(f"Process row {row_no}: both ID and size are required.")
            procs.append((pid, self.int_cell(row_no, "Process size", size, 1)))
        return names, sizes, procs

    def run(self):
        names, sizes, procs = self.read()
        res = frag.allocate(sizes, procs, self.strategy.get(), self.mode.get())
        fixed = res.mode == frag.FIXED

        body = self.result_card(f"Result - {res.strategy}, {res.mode.lower()}")
        text = OutputText(body)
        text.pack(fill="x")
        lines = [(ln, "bad" if "EXTERNAL" in ln else "head" if "INTERNAL" in ln else None)
                 for ln in frag.explain(res).split("\n")]
        lines.append((f"Memory utilisation (allocated / total): {res.utilization * 100:.1f}%", "ok"))
        text.set_lines(lines)

        body = self.result_card("Memory map")
        mem = MemoryMap(body)
        mem.pack(fill="x")
        rows = []
        for b in res.blocks:
            segs = [(size, f"{pid} ({size})", pid) for pid, size in b.allocations]
            if b.free:
                segs.append((b.free, f"{'internal waste' if fixed and b.allocations else 'free'} {b.free}", "free"))
            rows.append((f"{names[b.index]} [{b.size}]", b.size, segs))
        mem.draw(rows)

        body = self.result_card("Blocks")
        ResultTable(body, ["Block", "Size", "Processes", "Used", "Free", "Internal fragmentation"],
                    [[names[b.index], b.size, ", ".join(p for p, _ in b.allocations) or "-", b.used, b.free,
                      b.free if fixed and b.allocations else 0] for b in res.blocks], max_height=12).pack(fill="x")

        body = self.result_card("Allocated processes")
        ResultTable(body, ["Process", "Size", "Block", "Internal fragmentation"],
                    [[a.pid, a.size, names[a.block], a.internal_fragmentation] for a in res.allocated],
                    max_height=12).pack(fill="x")

        body = self.result_card("Processes NOT allocated")
        if res.unallocated:
            ResultTable(body, ["Process", "Size", "Cause", "Explanation"],
                        [([u.pid, u.size, "EXTERNAL fragmentation" if u.external else "Not enough memory",
                           u.reason], "bad" if u.external else "warn") for u in res.unallocated],
                        max_height=12).pack(fill="x")
        else:
            ttk.Label(body, text="Every process was allocated.", style="Ok.TLabel").pack(anchor="w")

        body = self.result_card("Strategy comparison (same input, same partition type)")
        comp = []
        for strategy in frag.STRATEGIES:
            r = frag.allocate(sizes, procs, strategy, self.mode.get())
            comp.append([strategy, len(r.allocated), len(r.unallocated), r.total_internal, r.total_free,
                         r.largest_hole, "Yes" if r.has_external else "No", f"{r.utilization * 100:.1f}%"])
        ResultTable(body, ["Strategy", "Allocated", "Not allocated", "Internal frag.", "Free usable memory",
                           "Largest free piece", "External frag.?", "Utilisation"], comp).pack(fill="x")
        self.set_status(f"{len(res.allocated)} allocated, {len(res.unallocated)} not allocated", "ok")
