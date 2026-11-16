"""GUI page for Module 3: Paging."""
import random
import tkinter as tk
from tkinter import ttk

from core import paging
from core.validation import ValidationError, parse_int
from .base_view import ModuleView
from .widgets import DynamicTable, OutputText, ResultTable, labeled_entry

ASSUMPTIONS = (
    "Assumptions: page size = frame size.  Physical memory = frames x page size.  Logical address space = "
    "pages x page size (addresses 0 .. size-1).  Page number = address // page size, offset = address % page "
    "size, physical address = frame x page size + offset.  A page with a blank Frame is not in memory, so using "
    "it causes a PAGE FAULT.  Two pages cannot use the same frame.")


class PagingView(ModuleView):
    title = "Paging"
    description = ("Define the memory configuration and the page table (page -> frame), then enter logical "
                   "addresses. Each one is translated to a physical address, or reported as a page fault / "
                   "invalid address.")

    def build_inputs(self, parent):
        cfg = ttk.Frame(parent, style="Card.TFrame")
        cfg.pack(fill="x")
        self.page_size = tk.StringVar(value="100")
        self.frames = tk.StringVar(value="5")
        self.pages = tk.StringVar(value="8")
        labeled_entry(cfg, "Page size", self.page_size, 8).pack(side="left", padx=(0, 14))
        labeled_entry(cfg, "Number of frames", self.frames, 8).pack(side="left", padx=(0, 14))
        labeled_entry(cfg, "Number of logical pages", self.pages, 8).pack(side="left", padx=(0, 14))
        ttk.Button(cfg, text="Load example", style="Small.TButton",
                   command=lambda: self.safe_call(self.load_example)).pack(side="right")
        ttk.Label(parent, text=ASSUMPTIONS, style="CardMuted.TLabel", wraplength=1000,
                  justify="left").pack(anchor="w", pady=(8, 8))

        both = ttk.Frame(parent, style="Card.TFrame")
        both.pack(fill="x")
        left = ttk.Frame(both, style="Card.TFrame")
        left.pack(side="left", fill="x", expand=True, padx=(0, 16))
        ttk.Label(left, text="Page table  (leave Frame blank = page not in memory)",
                  style="Card.TLabel").pack(anchor="w", pady=(0, 4))
        self.table = DynamicTable(left, ["Page", "Frame"], widths=[10, 10], auto_fill=lambda n: str(n),
                                  height=200, item_name="entry")
        self.table.pack(fill="x")
        ttk.Button(left, text="Auto-map pages 0..n to frames 0..n", style="Small.TButton",
                   command=lambda: self.safe_call(self.auto_map)).pack(anchor="w", pady=(6, 0))

        right = ttk.Frame(both, style="Card.TFrame")
        right.pack(side="left", fill="x", expand=True)
        ttk.Label(right, text="Logical addresses to translate", style="Card.TLabel").pack(anchor="w", pady=(0, 4))
        self.addresses = DynamicTable(right, ["Logical address"], widths=[18], height=200,
                                      generator=self.random_addresses, item_name="address")
        self.addresses.pack(fill="x")
        self.reset()

    def config(self):
        return paging.PagingConfig(parse_int(self.page_size.get(), "Page size", 1),
                                   parse_int(self.frames.get(), "Number of frames", 1),
                                   parse_int(self.pages.get(), "Number of logical pages", 1))

    def random_addresses(self, n):
        cfg = self.config()
        return [[str(random.randint(0, cfg.logical_size - 1))] for _ in range(n)]

    def auto_map(self):
        cfg = self.config()
        self.table.set_rows([[str(p), str(f)] for p, f in paging.auto_map(cfg)])

    def load_example(self):
        self.page_size.set("100")
        self.frames.set("5")
        self.pages.set("8")
        self.table.set_rows([["0", "3"], ["1", "1"], ["2", ""], ["3", "0"]])
        self.addresses.set_rows([["117"], ["5"], ["345"], ["250"], ["799"], ["800"]])

    def reset(self):
        self.page_size.set("100")
        self.frames.set("5")
        self.pages.set("8")
        self.table.clear()
        self.addresses.clear()
        for _ in range(2):
            self.table.add_row()
            self.addresses.add_row()

    def run(self):
        cfg = self.config()
        entries = []
        for row_no, (page, frame) in self.table.rows():
            if page == "":
                raise ValidationError(f"Page table row {row_no}: the page number is empty.")
            entries.append((self.int_cell(row_no, "Page", page, 0),
                            self.int_cell(row_no, "Frame", frame, 0) if frame != "" else None))
        table = paging.build_page_table(cfg, entries)
        addr_rows = self.addresses.rows()
        self.require_rows(addr_rows, "logical address")
        addresses = [self.int_cell(r, "Logical address", v[0]) for r, v in addr_rows]
        results = [paging.translate(cfg, table, a) for a in addresses]

        body = self.result_card("Memory configuration")
        text = OutputText(body)
        text.pack(fill="x")
        ok = sum(r.status == paging.OK for r in results)
        faults = sum(r.status == paging.PAGE_FAULT for r in results)
        bad = sum(r.status == paging.INVALID for r in results)
        text.set_lines([
            (f"Page size / frame size : {cfg.page_size}", None),
            (f"Physical memory        : {cfg.num_frames} frames x {cfg.page_size} = {cfg.physical_size} "
             f"(addresses 0..{cfg.physical_size - 1})", None),
            (f"Logical address space  : {cfg.num_pages} pages x {cfg.page_size} = {cfg.logical_size} "
             f"(addresses 0..{cfg.logical_size - 1})", None),
            (f"Translated OK: {ok}    Page faults: {faults}    Invalid addresses: {bad}",
             "ok" if ok == len(results) else "warn")])

        body = self.result_card("Address translation")
        tag = {paging.OK: "ok", paging.PAGE_FAULT: "warn", paging.INVALID: "bad"}
        rows = []
        for t in results:
            dash = lambda v: "-" if v is None else v
            rows.append(([t.address, dash(t.page), dash(t.offset), dash(t.frame), dash(t.physical), t.status,
                          t.message], tag[t.status]))
        ResultTable(body, ["Logical address", "Page #", "Offset", "Frame #", "Physical address", "Status",
                           "Calculation / explanation"], rows, max_height=14).pack(fill="x")

        body = self.result_card("Page table")
        rows = [[p, "-" if f is None else f, "in memory" if f is not None else "not in memory"]
                for p, f in sorted(table.items())]
        ResultTable(body, ["Page", "Frame", "Valid"], rows, max_height=10).pack(fill="x")

        body = self.result_card("Physical memory (frame table)")
        rows = []
        for frame, page in paging.frame_table(cfg, table):
            lo = frame * cfg.page_size
            rows.append([frame, f"{lo} - {lo + cfg.page_size - 1}", "free" if page is None else f"page {page}"])
        ResultTable(body, ["Frame", "Physical addresses", "Holds"], rows, max_height=10).pack(fill="x")
        self.set_status(f"{len(results)} address(es) translated", "ok")
