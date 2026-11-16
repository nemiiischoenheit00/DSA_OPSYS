"""Main window: sidebar navigation + the home dashboard. Module pages are created on first use."""
import tkinter as tk
from tkinter import ttk

from .bankers_view import BankersView
from .cpu_scheduling_view import CPUSchedulingView
from .disk_scheduling_view import DiskSchedulingView
from .fragmentation_view import FragmentationView
from .multiprogramming_view import MultiprogrammingView
from .page_replacement_view import PageReplacementView
from .paging_view import PagingView
from .segmentation_view import SegmentationView
from .styles import COLORS

# (key, number, sidebar title, summary for the dashboard card, view class)
MODULES = [
    ("cpu", 1, "CPU Scheduling", "FCFS, SJF, Priority and Round Robin with Gantt chart, waiting and "
                                 "turnaround times.", CPUSchedulingView),
    ("bankers", 2, "Banker's Algorithm", "Need matrix, safety check and safe sequence for any number of "
                                         "processes and resources.", BankersView),
    ("paging", 3, "Paging", "Page table and logical-to-physical address translation with page faults.",
     PagingView),
    ("replace", 4, "Page Replacement", "FIFO, LRU and Optimal with frame states, hits and faults.",
     PageReplacementView),
    ("segment", 5, "Segmentation", "Segment table, base/limit checking and out-of-range detection.",
     SegmentationView),
    ("frag", 6, "Fragmentation", "First/Best/Worst Fit; internal vs external fragmentation.",
     FragmentationView),
    ("disk", 7, "Disk Scheduling", "FCFS, SSTF, SCAN, C-SCAN, LOOK and C-LOOK with head-movement chart.",
     DiskSchedulingView),
    ("multi", 8, "Multiprogramming", "Programs sharing memory and CPU: loading, waiting, removal and "
                                     "utilisation.", MultiprogrammingView),
]


class Dashboard(ttk.Frame):
    def __init__(self, parent, open_module):
        super().__init__(parent)
        ttk.Label(self, text="OS Simulator", style="Title.TLabel").pack(anchor="w", padx=28, pady=(24, 0))
        ttk.Label(self, style="Muted.TLabel", wraplength=900, justify="left",
                  text="An interactive Operating Systems laboratory. Eight modules, every one driven by your own "
                       "input - add as many processes, requests, blocks or pages as you like and watch the "
                       "algorithms work.").pack(anchor="w", padx=28, pady=(4, 16))
        grid = ttk.Frame(self)
        grid.pack(fill="both", expand=True, padx=22)
        for i, (key, num, name, summary, _cls) in enumerate(MODULES):
            card = ttk.Frame(grid, style="Card.TFrame")
            card.grid(row=i // 2, column=i % 2, sticky="nsew", padx=6, pady=6)
            ttk.Label(card, text=f"{num:02d}  {name}", style="Heading.TLabel").pack(anchor="w", padx=16, pady=(14, 2))
            ttk.Label(card, text=summary, style="CardMuted.TLabel", wraplength=420, justify="left").pack(
                anchor="w", padx=16)
            ttk.Button(card, text="Open module  >", style="Small.TButton",
                       command=lambda k=key: open_module(k)).pack(anchor="w", padx=16, pady=(10, 14))
        grid.columnconfigure(0, weight=1)
        grid.columnconfigure(1, weight=1)
        ttk.Label(self, style="Muted.TLabel", text="Tip: every table has 'Load example', 'Paste / bulk entry' and "
                                                   "'Random data' buttons for quick demos and large datasets."
                  ).pack(anchor="w", padx=28, pady=(8, 16))


class MainWindow:
    def __init__(self, root):
        self.root = root
        self.views = {}
        self.nav = {}
        self.current = None

        sidebar = ttk.Frame(root, style="Panel.TFrame", width=240)
        sidebar.pack(side="left", fill="y")
        sidebar.pack_propagate(False)
        ttk.Label(sidebar, text="OS Simulator", style="Brand.TLabel").pack(anchor="w", padx=18, pady=(22, 0))
        ttk.Label(sidebar, text="Operating Systems project", style="BrandSub.TLabel").pack(
            anchor="w", padx=18, pady=(0, 18))
        self._nav_button(sidebar, "home", "Dashboard")
        for key, num, name, _s, _c in MODULES:
            self._nav_button(sidebar, key, f"{num}.  {name}")

        self.content = ttk.Frame(root)
        self.content.pack(side="left", fill="both", expand=True)
        self.classes = {key: cls for key, _n, _t, _s, cls in MODULES}
        self.views["home"] = Dashboard(self.content, self.show)
        self.show("home")

    def _nav_button(self, parent, key, text):
        btn = ttk.Button(parent, text=text, style="Nav.TButton", command=lambda: self.show(key))
        btn.pack(fill="x")
        self.nav[key] = btn

    def show(self, key):
        if key not in self.views:                      # create module pages lazily (fast start-up)
            self.views[key] = self.classes[key](self.content)
        if self.current is not None:
            self.views[self.current].pack_forget()
            self.nav[self.current].configure(style="Nav.TButton")
        self.views[key].pack(fill="both", expand=True)
        self.nav[key].configure(style="NavActive.TButton")
        self.current = key
