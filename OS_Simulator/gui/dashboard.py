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
from . import styles
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
        ttk.Label(self, text="OPERATING SYSTEMS LAB", style="Eyebrow.TLabel").pack(
            anchor="w", padx=30, pady=(25, 5))
        ttk.Label(self, text="Explore how an OS works", style="Title.TLabel").pack(
            anchor="w", padx=30, pady=(0, 4))
        ttk.Label(self, text="A hands-on workspace for the algorithms behind modern operating systems.",
                  style="Muted.TLabel").pack(anchor="w", padx=30, pady=(0, 18))

        hero = ttk.Frame(self, style="AltCard.TFrame")
        hero.pack(fill="x", padx=24, pady=(0, 16))
        ttk.Label(hero, text="LEARN BY RUNNING", style="AltEyebrow.TLabel").pack(
            anchor="w", padx=20, pady=(17, 4))
        ttk.Label(hero, text="Your inputs. Real algorithms. Clear results.", style="Hero.TLabel").pack(
            anchor="w", padx=20)
        ttk.Label(hero, text="Choose a module, adjust the data, and follow each decision step by step.",
                  style="HeroBody.TLabel").pack(anchor="w", padx=20, pady=(4, 17))

        ttk.Label(self, text="SIMULATION MODULES", style="Eyebrow.TLabel").pack(
            anchor="w", padx=30, pady=(0, 7))
        grid = ttk.Frame(self)
        grid.pack(fill="both", expand=True, padx=24)
        for i, (key, num, name, summary, _cls) in enumerate(MODULES):
            card = ttk.Frame(grid, style="Card.TFrame")
            card.grid(row=i // 2, column=i % 2, sticky="nsew", padx=5, pady=5)
            ttk.Label(card, text=f"MODULE  {num:02d}", style="CardNumber.TLabel").pack(
                anchor="w", padx=17, pady=(15, 5))
            ttk.Label(card, text=name, style="Heading.TLabel").pack(anchor="w", padx=17, pady=(0, 3))
            ttk.Label(card, text=summary, style="CardMuted.TLabel", wraplength=440, justify="left").pack(
                anchor="w", padx=17)
            ttk.Button(card, text="Open module  →", style="Small.TButton",
                       command=lambda k=key: open_module(k)).pack(anchor="w", padx=17, pady=(10, 14))
        grid.columnconfigure(0, weight=1)
        grid.columnconfigure(1, weight=1)
        ttk.Label(self, style="Muted.TLabel", text="Tip  ·  Load an example dataset to get started quickly.").pack(
            anchor="w", padx=30, pady=(8, 16))


class MainWindow:
    def __init__(self, root):
        self.root = root
        self.views = {}
        self.nav = {}
        self.current = None
        self.theme_button = None

        sidebar = ttk.Frame(root, style="Panel.TFrame", width=240)
        sidebar.pack(side="left", fill="y")
        sidebar.pack_propagate(False)
        ttk.Label(sidebar, text="OS Simulator", style="Brand.TLabel").pack(anchor="w", padx=18, pady=(22, 0))
        ttk.Label(sidebar, text="Operating Systems project", style="BrandSub.TLabel").pack(
            anchor="w", padx=18, pady=(0, 18))
        ttk.Separator(sidebar).pack(fill="x", padx=16, pady=(0, 14))
        ttk.Label(sidebar, text="WORKSPACE", style="BrandSub.TLabel").pack(anchor="w", padx=18, pady=(0, 7))
        self._nav_button(sidebar, "home", "Dashboard")
        for key, num, name, _s, _c in MODULES:
            self._nav_button(sidebar, key, f"{num}.  {name}")

        self.theme_button = ttk.Button(sidebar, style="Nav.TButton", command=self.toggle_theme)
        self.theme_button.pack(side="bottom", fill="x", padx=8, pady=(0, 4))
        self._update_theme_button()

        footer = ttk.Frame(sidebar, style="Panel.TFrame")
        footer.pack(side="bottom", fill="x", padx=18, pady=18)
        ttk.Label(footer, text="8 interactive modules", style="BrandSub.TLabel").pack(anchor="w")
        ttk.Label(footer, text="Built for learning by doing", style="BrandSub.TLabel").pack(anchor="w", pady=(3, 0))

        self.content = ttk.Frame(root)
        self.content.pack(side="left", fill="both", expand=True, padx=(0, 4), pady=3)
        self.classes = {key: cls for key, _n, _t, _s, cls in MODULES}
        self.views["home"] = Dashboard(self.content, self.show)
        self.show("home")

    def _update_theme_button(self):
        target = "light" if styles.CURRENT_THEME == "dark" else "dark"
        self.theme_button.configure(text=f"Switch to {target} mode")

    def toggle_theme(self):
        from tkinter import Text

        from .styles import apply_theme

        styles.set_theme("light" if styles.CURRENT_THEME == "dark" else "dark")
        apply_theme(self.root)

        def refresh(widget):
            cls = widget.winfo_class()
            if cls == "Canvas":
                try:
                    key = getattr(widget, "_surface_key", "bg")
                    widget.configure(background=COLORS.get(key, COLORS["bg"]))
                except Exception:
                    pass
            elif isinstance(widget, Text):
                widget.configure(bg=COLORS["entry"], fg=COLORS["text"],
                                 insertbackground=COLORS["text"])
                for tag, color in (("ok", COLORS["ok"]), ("bad", COLORS["bad"]),
                                   ("warn", COLORS["warn"]), ("head", COLORS["accent_hover"])):
                    try:
                        widget.tag_configure(tag, foreground=color)
                    except Exception:
                        pass
            redraw = getattr(widget, "refresh_theme", None)
            if redraw:
                redraw()
            for child in widget.winfo_children():
                refresh(child)

        refresh(self.root)
        self._update_theme_button()

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
