"""GUI page for Module 7: Disk Scheduling. The algorithm list comes from core.disk_scheduling.ALGORITHMS,
so adding an algorithm there makes it appear here automatically."""
import random
import tkinter as tk
from tkinter import ttk

from core import disk_scheduling as disk
from core.validation import parse_int, parse_int_list
from .base_view import ModuleView
from .charts import BarChart, DiskChart
from .widgets import OutputText, ResultTable, labeled_combo, labeled_entry

EXAMPLE = "98 183 37 122 14 124 65 67"


class DiskSchedulingView(ModuleView):
    title = "Disk Scheduling"
    description = ("Enter a queue of cylinder requests, the starting head position and the disk limits. See the "
                   "service order, every head movement, the total head movement and a picture of the head path.")

    def build_inputs(self, parent):
        self.requests = tk.StringVar(value=EXAMPLE)
        self.head = tk.StringVar(value="53")
        self.low = tk.StringVar(value="0")
        self.high = tk.StringVar(value="199")
        self.algo = tk.StringVar(value=list(disk.ALGORITHMS)[0])
        self.direction = tk.StringVar(value=disk.UP)
        self.count_jump = tk.BooleanVar(value=True)
        self.rand_n = tk.StringVar(value="30")

        ttk.Label(parent, text="Request queue (cylinder numbers separated by spaces or commas)",
                  style="Card.TLabel").pack(anchor="w")
        ttk.Entry(parent, textvariable=self.requests).pack(fill="x", pady=(2, 8))
        row1 = ttk.Frame(parent, style="Card.TFrame")
        row1.pack(fill="x")
        labeled_entry(row1, "Initial head position", self.head, 7).pack(side="left", padx=(0, 14))
        labeled_entry(row1, "Lowest cylinder", self.low, 7).pack(side="left", padx=(0, 14))
        labeled_entry(row1, "Highest cylinder", self.high, 7).pack(side="left", padx=(0, 14))
        ttk.Button(row1, text="Load example", style="Small.TButton",
                   command=lambda: self.safe_call(self.load_example)).pack(side="right")
        row2 = ttk.Frame(parent, style="Card.TFrame")
        row2.pack(fill="x", pady=(8, 0))
        labeled_combo(row2, "Algorithm", self.algo, list(disk.ALGORITHMS), 10).pack(side="left", padx=(0, 14))
        labeled_combo(row2, "Initial direction", self.direction, disk.DIRECTIONS, 22).pack(side="left", padx=(0, 14))
        ttk.Checkbutton(row2, text="Count C-SCAN / C-LOOK return jump as movement", variable=self.count_jump,
                        style="Card.TCheckbutton").pack(side="left")
        row3 = ttk.Frame(parent, style="Card.TFrame")
        row3.pack(fill="x", pady=(8, 0))
        ttk.Label(row3, text="Random queue:", style="CardMuted.TLabel").pack(side="left", padx=(0, 8))
        labeled_entry(row3, "number of requests", self.rand_n, 7).pack(side="left", padx=(0, 10))
        ttk.Button(row3, text="Generate", style="Small.TButton",
                   command=lambda: self.safe_call(self.generate)).pack(side="left")
        ttk.Label(parent, style="CardMuted.TLabel", wraplength=1000, justify="left",
                  text="Rules: SCAN / C-SCAN travel to the disk edge before turning; LOOK / C-LOOK only go as far "
                       "as the last request. SCAN reverses at the edge; C-SCAN / C-LOOK jump back to the other "
                       "side. SSTF ties are broken by queue order.").pack(anchor="w", pady=(8, 0))

    def limits(self):
        low = parse_int(self.low.get(), "Lowest cylinder", 0)
        high = parse_int(self.high.get(), "Highest cylinder", 0)
        return low, high

    def generate(self):
        n = parse_int(self.rand_n.get(), "Number of requests", 1)
        low, high = self.limits()
        if high <= low:
            from core.validation import ValidationError
            raise ValidationError("The highest cylinder must be greater than the lowest cylinder.")
        self.requests.set(" ".join(str(random.randint(low, high)) for _ in range(n)))
        self.head.set(str(random.randint(low, high)))

    def load_example(self):
        self.requests.set(EXAMPLE)
        self.head.set("53")
        self.low.set("0")
        self.high.set("199")

    def reset(self):
        self.requests.set("")
        self.head.set("")
        self.low.set("0")
        self.high.set("199")
        self.algo.set(list(disk.ALGORITHMS)[0])
        self.direction.set(disk.UP)
        self.count_jump.set(True)

    def run(self):
        reqs = parse_int_list(self.requests.get(), "Request queue", 0)
        head = parse_int(self.head.get(), "Initial head position", 0)
        low, high = self.limits()
        jump = self.count_jump.get()
        res = disk.run(self.algo.get(), reqs, head, low, high, self.direction.get(), jump)

        body = self.result_card(f"Result - {res.algorithm}")
        text = OutputText(body)
        text.pack(fill="x")
        order = ", ".join(str(p) for p in res.service_order)
        text.set_lines([(f"Service order: {order}", "head"),
                        (f"Total head movement: {res.total_movement} cylinders", "ok"),
                        (f"Average per request: {res.total_movement / len(reqs):.2f}", None)])

        body = self.result_card("Head movement chart")
        chart = DiskChart(body)
        chart.pack(fill="x")
        chart.draw(res.path, low, high)

        body = self.result_card("Step-by-step movement")
        kind = {"start": "start", "request": "serve request", "edge": "go to disk edge", "jump": "return jump"}
        rows = []
        for i, (s, move) in enumerate(zip(res.path, res.moves)):
            prev = "-" if i == 0 else res.path[i - 1].position
            label = kind[s.kind] + (" (and serve)" if s.kind == "jump" and s.served else "")
            rows.append(([i, prev, s.position, move, label], "warn" if s.kind in ("edge", "jump") else None))
        ResultTable(body, ["Step", "From", "To", "Movement", "Action"], rows, max_height=14).pack(fill="x")

        runs = disk.compare_all(reqs, head, low, high, self.direction.get(), jump)
        body = self.result_card("Algorithm comparison (same queue)")
        ResultTable(body, ["Algorithm", "Total head movement", "Average per request"],
                    [[r.algorithm, r.total_movement, f"{r.total_movement / len(reqs):.2f}"] for r in runs]
                    ).pack(fill="x")
        bars = BarChart(body, height=210)
        bars.pack(fill="x", pady=(8, 0))
        bars.draw([r.algorithm for r in runs], [r.total_movement for r in runs], "Total head movement", "{:g}")
        self.set_status(f"Total head movement: {res.total_movement}", "ok")
