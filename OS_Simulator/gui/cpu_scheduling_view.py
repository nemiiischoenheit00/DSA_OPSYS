"""GUI page for Module 1: CPU Scheduling."""
import random
import tkinter as tk
from tkinter import ttk

from core import cpu_scheduling as cpu
from core.validation import ValidationError, parse_int
from .base_view import ModuleView
from .charts import BarChart, GanttChart
from .widgets import (DynamicTable, OutputText, ResultTable, labeled_combo, labeled_entry, set_enabled)

COLUMNS = ["Process ID", "Arrival", "Burst", "Priority"]
EXAMPLE = [["P1", "0", "7", "3"], ["P2", "2", "4", "1"], ["P3", "4", "1", "4"], ["P4", "5", "4", "2"]]


def random_processes(n):
    return [[f"P{i + 1}", str(random.randint(0, max(5, n))), str(random.randint(1, 12)),
             str(random.randint(1, 5))] for i in range(n)]


class CPUSchedulingView(ModuleView):
    title = "CPU Scheduling"
    description = ("Add processes, choose an algorithm and run it. You get the Gantt chart, the waiting and "
                   "turnaround time of every process, and a comparison of all algorithms on the same workload.")

    def build_inputs(self, parent):
        self.table = DynamicTable(parent, COLUMNS, widths=[12, 9, 9, 9], auto_fill=lambda n: f"P{n + 1}",
                                  defaults=["", "0", "1", "1"], generator=random_processes,
                                  item_name="process")
        self.table.pack(fill="x")
        for _ in range(3):
            self.table.add_row()

        opts = ttk.Frame(parent, style="Card.TFrame")
        opts.pack(fill="x", pady=(10, 0))
        self.algo = tk.StringVar(value="FCFS")
        self.preemptive = tk.BooleanVar(value=False)
        self.quantum = tk.StringVar(value="2")
        self.low_first = tk.BooleanVar(value=True)
        combo = labeled_combo(opts, "Algorithm", self.algo, cpu.ALGORITHMS, width=14)
        combo.pack(side="left", padx=(0, 16))
        combo.combo.bind("<<ComboboxSelected>>", lambda e: self._update_options())
        self.pre_check = ttk.Checkbutton(opts, text="Preemptive", variable=self.preemptive,
                                         style="Card.TCheckbutton")
        self.pre_check.pack(side="left", padx=(0, 16))
        self.q_frame = labeled_entry(opts, "Time quantum", self.quantum, width=6)
        self.q_frame.pack(side="left", padx=(0, 16))
        self.prio_check = ttk.Checkbutton(opts, text="Lower number = higher priority",
                                          variable=self.low_first, style="Card.TCheckbutton")
        self.prio_check.pack(side="left")
        ttk.Button(opts, text="Load example", style="Small.TButton",
                   command=lambda: self.safe_call(self.load_example)).pack(side="right")
        ttk.Label(parent, style="CardMuted.TLabel", justify="left", wraplength=1000,
                  text="Rules: ties are broken by earlier arrival, then by entry order. Idle CPU time is shown "
                       "as IDLE. Priority is only needed for the Priority algorithm. SJF + Preemptive = SRTF."
                  ).pack(anchor="w", pady=(8, 0))
        self._update_options()

    def _update_options(self):
        algo = self.algo.get()
        set_enabled(self.pre_check, algo in ("SJF", "Priority"))
        set_enabled(self.q_frame.entry, algo == "Round Robin")
        set_enabled(self.prio_check, algo == "Priority")
        if algo not in ("SJF", "Priority"):
            self.preemptive.set(False)

    def load_example(self):
        self.table.set_rows(EXAMPLE)
        self.algo.set("SJF")
        self.preemptive.set(True)
        self._update_options()

    def reset(self):
        self.table.clear()
        for _ in range(3):
            self.table.add_row()
        self.algo.set("FCFS")
        self.quantum.set("2")
        self.preemptive.set(False)
        self.low_first.set(True)
        self._update_options()

    # ------------------------------------------------------------------ run
    def read_processes(self, need_priority):
        rows = self.table.rows()
        self.require_rows(rows, "process")
        self.check_blank_cells(rows, COLUMNS, required=[0, 1, 2])
        procs = []
        for row_no, (pid, arr, burst, prio) in rows:
            if need_priority and prio == "":
                raise ValidationError(f"Row {row_no}: Priority is required for the Priority algorithm.")
            procs.append(cpu.Process(
                pid,
                self.int_cell(row_no, "Arrival", arr, 0),
                self.int_cell(row_no, "Burst", burst, 1),
                self.int_cell(row_no, "Priority", prio, 0) if prio != "" else 0))
        return procs

    def run(self):
        algo = self.algo.get()
        procs = self.read_processes(need_priority=(algo == "Priority"))
        quantum = None
        if algo == "Round Robin":
            quantum = parse_int(self.quantum.get(), "Time quantum", 1)
        low_first = self.low_first.get()
        result = cpu.schedule(procs, algo, self.preemptive.get(), quantum, low_first)

        # --- summary
        body = self.result_card(f"Result - {result.algorithm}")
        text = OutputText(body)
        text.pack(fill="x")
        order = " -> ".join(result.execution_order)
        text.set_lines([
            (f"Execution order: {order}", "head"),
            (f"Average waiting time    : {result.avg_waiting:.2f}", "ok"),
            (f"Average turnaround time : {result.avg_turnaround:.2f}", "ok"),
            (f"Average response time   : {result.avg_response:.2f}", None),
            (f"CPU utilisation         : {result.cpu_utilization * 100:.1f}%   (total time {result.total_time})", None),
            ("Formulas: Turnaround = Completion - Arrival;  Waiting = Turnaround - Burst;  "
             "Response = First run - Arrival.", None)])

        # --- Gantt chart
        body = self.result_card("Gantt chart")
        gantt = GanttChart(body)
        gantt.pack(fill="x")
        gantt.draw([(s.pid, s.start, s.end) for s in result.gantt], cpu.IDLE)

        # --- per-process table
        body = self.result_card("Per-process results")
        rows = [[p.pid, p.arrival, p.burst, p.priority if algo == "Priority" else "-", p.completion,
                 p.turnaround, p.waiting, p.response] for p in result.processes]
        rows.append((["AVERAGE", "", "", "", "", f"{result.avg_turnaround:.2f}", f"{result.avg_waiting:.2f}",
                      f"{result.avg_response:.2f}"], "accent"))
        ResultTable(body, ["Process", "Arrival", "Burst", "Priority", "Completion", "Turnaround", "Waiting",
                           "Response"], rows, max_height=14).pack(fill="x")

        # --- comparison of every algorithm on this workload
        try:
            q = parse_int(self.quantum.get(), "Time quantum", 1)
        except ValidationError:
            q = 2
        runs = cpu.compare_all(procs, q, low_first)
        body = self.result_card("Algorithm comparison (same processes)",
                                "Lower waiting/turnaround time is better. Round Robin uses the quantum above "
                                "(2 if it is blank).")
        ResultTable(body, ["Algorithm", "Avg waiting", "Avg turnaround", "Avg response", "CPU util %"],
                    [[r.algorithm, f"{r.avg_waiting:.2f}", f"{r.avg_turnaround:.2f}", f"{r.avg_response:.2f}",
                      f"{r.cpu_utilization * 100:.1f}"] for r in runs]).pack(fill="x")
        chart = BarChart(body)
        chart.pack(fill="x", pady=(8, 0))
        labels = ["FCFS", "SJF", "SRTF", "Priority", "Priority-P", "RR"]    # same order as compare_all()
        chart.draw(labels, [r.avg_waiting for r in runs], "Average waiting time", "{:.2f}")
        self.set_status(f"{len(procs)} process(es) scheduled", "ok")
