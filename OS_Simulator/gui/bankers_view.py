"""GUI page for Module 2: Banker's Algorithm."""
import random
import tkinter as tk
from tkinter import messagebox, ttk

from core import bankers_algorithm as bank
from core.validation import ValidationError, parse_int, parse_int_list
from .base_view import ModuleView
from .widgets import MatrixGrid, OutputText, ResultTable, labeled_entry

EXAMPLE_ALLOC = [[0, 1, 0], [2, 0, 0], [3, 0, 2], [2, 1, 1], [0, 0, 2]]
EXAMPLE_MAX = [[7, 5, 3], [3, 2, 2], [9, 0, 2], [2, 2, 2], [4, 3, 3]]
EXAMPLE_AVAIL = [3, 3, 2]


class BankersView(ModuleView):
    title = "Banker's Algorithm"
    description = ("Choose the number of processes and resource types, press 'Generate tables', then fill in "
                   "Allocation, Maximum and Available. The simulator computes Need, runs the safety algorithm "
                   "and shows the safe sequence step by step.")

    def build_inputs(self, parent):
        self.n_var = tk.StringVar(value="5")
        self.m_var = tk.StringVar(value="3")
        top = ttk.Frame(parent, style="Card.TFrame")
        top.pack(fill="x")
        labeled_entry(top, "Processes", self.n_var, 6).pack(side="left", padx=(0, 14))
        labeled_entry(top, "Resource types", self.m_var, 6).pack(side="left", padx=(0, 14))
        ttk.Button(top, text="Generate tables", command=lambda: self.safe_call(self.generate)).pack(side="left")
        ttk.Button(top, text="Load example", style="Small.TButton",
                   command=lambda: self.safe_call(self.load_example)).pack(side="right")
        ttk.Button(top, text="Random safe-ish data", style="Small.TButton",
                   command=lambda: self.safe_call(self.random_data)).pack(side="right", padx=6)
        self.grids_frame = ttk.Frame(parent, style="Card.TFrame")
        self.grids_frame.pack(fill="x", pady=(10, 0))
        self.alloc = self.maxi = self.avail = None

        # optional: test a resource request
        req = ttk.Frame(parent, style="Card.TFrame")
        req.pack(fill="x", pady=(12, 0))
        ttk.Label(req, text="Optional - can a request be granted?", style="Card.TLabel").pack(side="left", padx=(0, 10))
        self.req_pid = tk.StringVar(value="1")
        self.req_vals = tk.StringVar(value="1, 0, 2")
        labeled_entry(req, "Process index", self.req_pid, 5).pack(side="left", padx=(0, 10))
        labeled_entry(req, "Request (one value per resource)", self.req_vals, 14).pack(side="left", padx=(0, 10))
        ttk.Button(req, text="Check request", style="Small.TButton",
                   command=self._check_request_clicked).pack(side="left")
        self.generate()

    # ------------------------------------------------------------------ tables
    def generate(self, silent=False):
        n = parse_int(self.n_var.get(), "Number of processes", 1)
        m = parse_int(self.m_var.get(), "Number of resource types", 1)
        if n * m > 20000 and not messagebox.askyesno(
                "Large tables", f"This creates {2 * n * m + m} input cells and may be slow. Continue?",
                parent=self):
            return
        for child in self.grids_frame.winfo_children():
            child.destroy()
        rows = [f"P{i}" for i in range(n)]
        cols = [f"R{j}" for j in range(m)]
        self.alloc = self._grid("Allocation (resources currently held)", rows, cols)
        self.maxi = self._grid("Maximum (most each process may ever request)", rows, cols)
        self.avail = self._grid("Available (free resources)", ["Avail"], cols, height=60)

    def _grid(self, caption, rows, cols, height=190):
        ttk.Label(self.grids_frame, text=caption, style="Card.TLabel").pack(anchor="w", pady=(6, 2))
        grid = MatrixGrid(self.grids_frame, len(rows), len(cols), rows, cols, height=height)
        grid.pack(fill="x")
        return grid

    def load_example(self):
        self.n_var.set("5")
        self.m_var.set("3")
        self.generate()
        self.alloc.set_values(EXAMPLE_ALLOC)
        self.maxi.set_values(EXAMPLE_MAX)
        self.avail.set_values([EXAMPLE_AVAIL])
        self.req_pid.set("1")
        self.req_vals.set("1, 0, 2")

    def random_data(self):
        n = parse_int(self.n_var.get(), "Number of processes", 1)
        m = parse_int(self.m_var.get(), "Number of resource types", 1)
        if self.alloc is None or self.alloc.rows != n or self.alloc.cols != m:
            self.generate()
        maxi = [[random.randint(1, 9) for _ in range(m)] for _ in range(n)]
        alloc = [[random.randint(0, mx // 2) for mx in row] for row in maxi]
        avail = [max(1, sum(col) // 3) for col in zip(*maxi)]
        self.alloc.set_values(alloc)
        self.maxi.set_values(maxi)
        self.avail.set_values([avail])

    def reset(self):
        self.n_var.set("5")
        self.m_var.set("3")
        self.generate()

    # ------------------------------------------------------------------ run
    def read_state(self):
        def matrix(grid, name):
            out = []
            for i, row in enumerate(grid.values()):
                out.append([parse_int(v, f"{name}[P{i}][R{j}]") for j, v in enumerate(row)])
            return out

        return (matrix(self.alloc, "Allocation"), matrix(self.maxi, "Maximum"),
                matrix(self.avail, "Available")[0])

    def run(self):
        alloc, maxi, avail = self.read_state()
        result = bank.check_safety(alloc, maxi, avail)
        m = len(avail)
        res_cols = [f"R{j}" for j in range(m)]

        body = self.result_card("Verdict")
        if result.safe:
            seq = " -> ".join(f"P{i}" for i in result.sequence)
            ttk.Label(body, text=f"SAFE   |   Safe sequence: < {seq} >", style="Ok.TLabel").pack(anchor="w")
        else:
            done = " -> ".join(f"P{i}" for i in result.sequence) or "none"
            ttk.Label(body, text="UNSAFE   |   no safe sequence exists", style="Bad.TLabel").pack(anchor="w")
            ttk.Label(body, text=f"Processes that could finish before getting stuck: {done}",
                      style="Card.TLabel").pack(anchor="w", pady=(4, 0))

        body = self.result_card("Need matrix  (Need = Maximum - Allocation)")
        rows = [[f"P{i}"] + need for i, need in enumerate(result.need)]
        ResultTable(body, ["Process"] + res_cols, rows, max_height=12).pack(fill="x")

        body = self.result_card("Safety algorithm - step by step")
        steps = [[k, f"P{s.process}", s.need, s.work_before, s.allocation, s.work_after]
                 for k, s in enumerate(result.steps, start=1)]
        if steps:
            ResultTable(body, ["Step", "Process run", "Need", "Work before", "Releases (Allocation)",
                               "Work after"], steps, max_height=12).pack(fill="x", pady=(0, 8))
        text = OutputText(body)
        text.pack(fill="x")
        lines = bank.explain(result, avail).split("\n")
        text.set_lines([(ln, "ok" if "SAFE" in ln and "UNSAFE" not in ln else "bad" if "UNSAFE" in ln else None)
                        for ln in lines])
        self.set_status("System is SAFE" if result.safe else "System is UNSAFE",
                        "ok" if result.safe else "bad")

    def _check_request_clicked(self):
        self.clear_results()
        self.safe_call(self.check_request)

    def check_request(self):
        alloc, maxi, avail = self.read_state()
        pid = parse_int(self.req_pid.get(), "Process index", 0)
        request = parse_int_list(self.req_vals.get(), "Request", 0)
        outcome = bank.request_resources(alloc, maxi, avail, pid, request)
        body = self.result_card(f"Resource request by P{pid}: {request}")
        ttk.Label(body, text=("GRANTED - " if outcome.granted else "NOT GRANTED - ") + outcome.reason,
                  style="Ok.TLabel" if outcome.granted else "Bad.TLabel", wraplength=900,
                  justify="left").pack(anchor="w")
        if outcome.safety is not None:
            text = OutputText(body)
            text.pack(fill="x", pady=(8, 0))
            new_avail = [a - r for a, r in zip(avail, request)]
            text.set_text("State AFTER pretending to grant the request:\n"
                          + bank.explain(outcome.safety, new_avail))
        self.set_status("Request granted" if outcome.granted else "Request not granted",
                        "ok" if outcome.granted else "bad")
