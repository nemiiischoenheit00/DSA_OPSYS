"""GUI page for Module 8: Multiprogramming."""
import random
import tkinter as tk
from tkinter import ttk

from core import multiprogramming as mp
from core.validation import parse_int
from .base_view import ModuleView
from .charts import MemoryMap, StepChart
from .widgets import DynamicTable, OutputText, ResultTable, labeled_entry

COLUMNS = ["Program ID", "Memory", "Arrival", "CPU burst"]
EXAMPLE = [["A", "40", "0", "4"], ["B", "40", "0", "4"], ["C", "40", "0", "2"], ["D", "30", "3", "5"],
           ["E", "150", "4", "3"]]


def random_jobs(n):
    return [[f"J{i + 1}", str(random.randint(5, 60)), str(random.randint(0, 2 * n)),
             str(random.randint(1, 12))] for i in range(n)]


class MultiprogrammingView(ModuleView):
    title = "Multiprogramming"
    description = ("Several programs share main memory and one CPU. Programs are loaded when a big enough hole "
                   "exists, wait in the job queue otherwise, run round-robin, and are removed when finished. "
                   "Click any event in the timeline to see the memory at that moment.")

    def build_inputs(self, parent):
        self.total = tk.StringVar(value="100")
        self.os_mem = tk.StringVar(value="0")
        self.quantum = tk.StringVar(value="2")
        self.bypass = tk.BooleanVar(value=False)
        cfg = ttk.Frame(parent, style="Card.TFrame")
        cfg.pack(fill="x")
        labeled_entry(cfg, "Total memory", self.total, 8).pack(side="left", padx=(0, 14))
        labeled_entry(cfg, "OS-reserved memory", self.os_mem, 8).pack(side="left", padx=(0, 14))
        labeled_entry(cfg, "CPU time quantum", self.quantum, 6).pack(side="left", padx=(0, 14))
        ttk.Checkbutton(cfg, text="Let smaller programs skip past a blocked one", variable=self.bypass,
                        style="Card.TCheckbutton").pack(side="left")
        ttk.Button(cfg, text="Load example", style="Small.TButton",
                   command=lambda: self.safe_call(self.load_example)).pack(side="right")
        self.table = DynamicTable(parent, COLUMNS, widths=[12, 10, 10, 10], auto_fill=lambda n: f"J{n + 1}",
                                  defaults=["", "10", "0", "1"], generator=random_jobs, item_name="program")
        self.table.pack(fill="x", pady=(10, 0))
        ttk.Label(parent, style="CardMuted.TLabel", wraplength=1000, justify="left",
                  text="Model: programs get a contiguous memory area (first fit). A program bigger than the "
                       "usable memory can never run (rejected). Loading decisions are made when a program "
                       "arrives or finishes, at CPU time-slice boundaries. Units are arbitrary (e.g. KB / ms)."
                  ).pack(anchor="w", pady=(8, 0))
        self.reset()

    def load_example(self):
        self.total.set("100")
        self.os_mem.set("10")
        self.quantum.set("2")
        self.table.set_rows(EXAMPLE)

    def reset(self):
        self.total.set("100")
        self.os_mem.set("0")
        self.quantum.set("2")
        self.bypass.set(False)
        self.table.clear()
        for _ in range(3):
            self.table.add_row()

    def run(self):
        total = parse_int(self.total.get(), "Total memory", 1)
        osm = parse_int(self.os_mem.get(), "OS-reserved memory", 0)
        quantum = parse_int(self.quantum.get(), "CPU time quantum", 1)
        rows = self.table.rows()
        self.require_rows(rows, "program")
        self.check_blank_cells(rows, COLUMNS, required=[0, 1, 2, 3])
        jobs = [mp.Job(pid, self.int_cell(r, "Memory", m, 1), self.int_cell(r, "Arrival", a, 0),
                       self.int_cell(r, "CPU burst", b, 1)) for r, (pid, m, a, b) in rows]
        res = mp.simulate(jobs, total, osm, quantum, self.bypass.get())
        usable = total - osm

        body = self.result_card("System utilisation")
        text = OutputText(body)
        text.pack(fill="x")
        done = sum(j.completion is not None for j in res.jobs)
        text.set_lines([
            (f"Total time              : {res.total_time}", None),
            (f"CPU utilisation         : {res.cpu_utilization * 100:.1f}%", "ok"),
            (f"Memory utilisation      : {res.memory_utilization * 100:.1f}%  (time-weighted, of {usable} usable)",
             "ok"),
            (f"Peak memory in use      : {res.peak_memory_used} of {usable}", None),
            (f"Avg programs in memory  : {res.avg_degree:.2f}   (degree of multiprogramming)", None),
            (f"Programs completed     : {done} of {len(res.jobs)}   throughput {res.throughput:.3f} per time unit",
             "ok" if done == len(res.jobs) else "warn")])
        self.set_status(f"{done}/{len(res.jobs)} programs completed", "ok")

        body = self.result_card("Programs")
        dash = lambda v: "-" if v is None else v
        ResultTable(body, ["Program", "Memory", "Arrival", "CPU burst", "Status", "Loaded at",
                           "Waited for memory", "Finished at", "Turnaround"],
                    [([j.pid, j.memory, j.arrival, j.burst, j.status, dash(j.load_time), dash(j.memory_wait),
                       dash(j.completion), dash(j.turnaround)], None if j.completion is not None else "bad")
                     for j in res.jobs], max_height=12).pack(fill="x")

        body = self.result_card("Memory at the selected event",
                                "Select a row in the timeline below to redraw this map.")
        self.memory_map = MemoryMap(body)
        self.memory_map.pack(fill="x")
        self.detail = ttk.Label(body, text="", style="Card.TLabel", wraplength=950, justify="left")
        self.detail.pack(anchor="w", pady=(6, 0))
        self.snapshots, self.total_memory, self.os_reserved = res.snapshots, total, osm

        body = self.result_card("Event timeline (loading, waiting, removal)")
        rows_t = [[s.time, s.event, ", ".join(p.pid for p in s.loaded) or "-", ", ".join(s.waiting) or "-",
                   s.used, s.free, s.largest_hole] for s in res.snapshots]
        ResultTable(body, ["Time", "Event", "In memory", "Waiting", "Used", "Free", "Largest hole"], rows_t,
                    max_height=12, on_select=self.show_snapshot).pack(fill="x")
        if self.snapshots:
            self.show_snapshot(0)

        body = self.result_card("Memory in use over time")
        points = [(s.time, 100.0 * s.used / usable) for s in res.snapshots]
        if points and points[0][0] != 0:
            points.insert(0, (0, 0.0))
        points.append((res.total_time, 0.0))
        chart = StepChart(body)
        chart.pack(fill="x")
        chart.draw(points, 100.0, "Memory in use (% of usable memory)")

    def show_snapshot(self, index):
        snap = self.snapshots[index]
        segments, cursor = [], 0
        if self.os_reserved:
            segments.append((self.os_reserved, "OS", "os"))
            cursor = self.os_reserved
        for p in sorted(snap.loaded, key=lambda p: p.start):
            if p.start > cursor:
                segments.append((p.start - cursor, f"free {p.start - cursor}", "free"))
            segments.append((p.size, f"{p.pid} ({p.size})", p.pid))
            cursor = p.start + p.size
        if cursor < self.total_memory:
            segments.append((self.total_memory - cursor, f"free {self.total_memory - cursor}", "free"))
        self.memory_map.draw([(f"t = {snap.time}", self.total_memory, segments)], height=70)
        self.detail.configure(
            text=f"t = {snap.time}: {snap.event}\nIn memory: {', '.join(p.pid for p in snap.loaded) or 'none'}   |   "
                 f"Waiting: {', '.join(snap.waiting) or 'none'}   |   "
                 f"Rejected (cannot fit): {', '.join(snap.rejected) or 'none'}   |   "
                 f"Finished: {len(snap.finished)}")
