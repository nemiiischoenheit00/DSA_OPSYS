"""GUI page for Module 4: Page Replacement (FIFO, LRU, Optimal)."""
import random
import tkinter as tk
from tkinter import ttk

from core import page_replacement as pr
from core.validation import parse_int, parse_int_list
from .base_view import ModuleView
from .charts import BarChart
from .widgets import OutputText, ResultTable, labeled_combo, labeled_entry

EXAMPLE = "7 0 1 2 0 3 0 4 2 3 0 3 2 1 2 0 1 7 0 1"


class PageReplacementView(ModuleView):
    title = "Page Replacement"
    description = ("Enter a page reference string of any length and the number of frames. The table shows the "
                   "frame contents after every reference, which pages hit or fault, and which page was replaced "
                   "and why.")

    def build_inputs(self, parent):
        self.refs = tk.StringVar(value=EXAMPLE)
        self.frames = tk.StringVar(value="3")
        self.algo = tk.StringVar(value="FIFO")
        self.rand_len = tk.StringVar(value="30")
        self.rand_max = tk.StringVar(value="9")

        row = ttk.Frame(parent, style="Card.TFrame")
        row.pack(fill="x")
        ttk.Label(row, text="Reference string (numbers separated by spaces or commas)",
                  style="Card.TLabel").pack(anchor="w")
        ttk.Entry(parent, textvariable=self.refs).pack(fill="x", pady=(2, 8))

        opts = ttk.Frame(parent, style="Card.TFrame")
        opts.pack(fill="x")
        labeled_entry(opts, "Number of frames", self.frames, 6).pack(side="left", padx=(0, 14))
        labeled_combo(opts, "Algorithm", self.algo, pr.ALGORITHMS, 10).pack(side="left", padx=(0, 14))
        ttk.Button(opts, text="Load example", style="Small.TButton",
                   command=lambda: self.safe_call(self.load_example)).pack(side="right")

        rnd = ttk.Frame(parent, style="Card.TFrame")
        rnd.pack(fill="x", pady=(8, 0))
        ttk.Label(rnd, text="Random string:", style="CardMuted.TLabel").pack(side="left", padx=(0, 8))
        labeled_entry(rnd, "length", self.rand_len, 7).pack(side="left", padx=(0, 10))
        labeled_entry(rnd, "highest page number", self.rand_max, 5).pack(side="left", padx=(0, 10))
        ttk.Button(rnd, text="Generate", style="Small.TButton",
                   command=lambda: self.safe_call(self.generate)).pack(side="left")

    def generate(self):
        n = parse_int(self.rand_len.get(), "Random length", 1)
        hi = parse_int(self.rand_max.get(), "Highest page number", 0)
        self.refs.set(" ".join(str(random.randint(0, hi)) for _ in range(n)))

    def load_example(self):
        self.refs.set(EXAMPLE)
        self.frames.set("3")

    def reset(self):
        self.refs.set("")
        self.frames.set("3")
        self.algo.set("FIFO")

    def run(self):
        refs = parse_int_list(self.refs.get(), "Reference string", 0)
        k = parse_int(self.frames.get(), "Number of frames", 1)
        result = pr.run(self.algo.get(), refs, k)

        body = self.result_card(f"Result - {result.algorithm} with {k} frame(s)")
        text = OutputText(body)
        text.pack(fill="x")
        text.set_lines([
            (f"References: {result.total}", None),
            (f"Page hits  : {result.hits}   (hit ratio  {result.hit_ratio * 100:.2f}%)", "ok"),
            (f"Page faults: {result.faults}   (fault ratio {result.fault_ratio * 100:.2f}%)", "bad")])

        body = self.result_card("Step-by-step frame states",
                                "A new page uses the first empty frame; a replaced page's frame is reused by "
                                "the new page. '-' means an empty frame.")
        cols = ["Step", "Page"] + [f"Frame {i + 1}" for i in range(k)] + ["Result", "Replaced", "Reason"]
        rows = []
        for s in result.steps:
            frames = ["-" if f is None else f for f in s.frames]
            rows.append(([s.index, s.page] + frames + ["HIT" if s.hit else "FAULT",
                                                       "-" if s.replaced is None else s.replaced, s.reason],
                         "ok" if s.hit else "bad"))
        ResultTable(body, cols, rows, max_height=16).pack(fill="x")

        runs = pr.compare_all(refs, k)
        body = self.result_card("Algorithm comparison",
                                "Optimal looks into the future, so it is the lower bound for page faults.")
        ResultTable(body, ["Algorithm", "Hits", "Faults", "Hit ratio", "Fault ratio"],
                    [[r.algorithm, r.hits, r.faults, f"{r.hit_ratio * 100:.2f}%", f"{r.fault_ratio * 100:.2f}%"]
                     for r in runs]).pack(fill="x")
        chart = BarChart(body, height=200)
        chart.pack(fill="x", pady=(8, 0))
        chart.draw([r.algorithm for r in runs], [r.faults for r in runs], "Page faults", "{:g}")
        self.set_status(f"{result.faults} faults, {result.hits} hits", "ok")
