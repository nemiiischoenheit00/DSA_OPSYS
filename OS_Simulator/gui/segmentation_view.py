"""GUI page for Module 5: Segmentation."""
import random
import tkinter as tk
from tkinter import ttk

from core import segmentation as seg
from core.validation import ValidationError, parse_int
from .base_view import ModuleView
from .widgets import DynamicTable, OutputText, ResultTable, labeled_entry

SEG_EXAMPLE = [["0", "1400", "1000"], ["1", "6300", "400"], ["2", "4300", "400"], ["3", "3200", "1100"],
               ["4", "4700", "1000"]]
ADDR_EXAMPLE = [["2", "53"], ["3", "852"], ["0", "1222"], ["1", "399"], ["1", "400"], ["7", "10"]]


class SegmentationView(ModuleView):
    title = "Segmentation"
    description = ("Define segments (number, base address, limit), then enter logical addresses as "
                   "(segment, offset). Valid addresses give Base + Offset; offsets >= limit or unknown "
                   "segments are reported as errors.")

    def build_inputs(self, parent):
        both = ttk.Frame(parent, style="Card.TFrame")
        both.pack(fill="x")
        left = ttk.Frame(both, style="Card.TFrame")
        left.pack(side="left", fill="x", expand=True, padx=(0, 16))
        ttk.Label(left, text="Segment table", style="Card.TLabel").pack(anchor="w", pady=(0, 4))
        self.segments = DynamicTable(left, ["Segment", "Base", "Limit"], widths=[9, 10, 10],
                                     auto_fill=lambda n: str(n), height=200, item_name="segment",
                                     generator=self.random_segments)
        self.segments.pack(fill="x")
        right = ttk.Frame(both, style="Card.TFrame")
        right.pack(side="left", fill="x", expand=True)
        ttk.Label(right, text="Logical addresses (segment, offset)", style="Card.TLabel").pack(anchor="w", pady=(0, 4))
        self.addresses = DynamicTable(right, ["Segment", "Offset"], widths=[10, 10], height=200,
                                      item_name="address", generator=self.random_addresses)
        self.addresses.pack(fill="x")

        extra = ttk.Frame(parent, style="Card.TFrame")
        extra.pack(fill="x", pady=(10, 0))
        self.mem = tk.StringVar(value="")
        labeled_entry(extra, "Physical memory size (optional)", self.mem, 10).pack(side="left")
        ttk.Label(extra, text="  If given, every segment must fit inside it.",
                  style="CardMuted.TLabel").pack(side="left")
        ttk.Button(extra, text="Load example", style="Small.TButton",
                   command=lambda: self.safe_call(self.load_example)).pack(side="right")
        self.reset()

    def random_segments(self, n):
        rows, base = [], random.randint(0, 500)
        for i in range(n):
            limit = random.randint(50, 1000)
            rows.append([str(i), str(base), str(limit)])
            base += limit + random.randint(0, 100)         # keep segments from overlapping
        return rows

    def random_addresses(self, n):
        segs = self.read_segment_rows()
        if not segs:
            raise ValidationError("Define the segments first; random addresses are based on them.")
        out = []
        for _ in range(n):
            s = random.choice(segs)
            out.append([str(s[0]), str(random.randint(0, int(s[2] * 1.3)))])   # ~25% will be out of range
        return out

    def read_segment_rows(self):
        out = []
        for row_no, (sid, base, limit) in self.segments.rows():
            if "" in (sid, base, limit):
                raise ValidationError(f"Segment row {row_no}: segment, base and limit are all required.")
            out.append((self.int_cell(row_no, "Segment", sid, 0), self.int_cell(row_no, "Base", base, 0),
                        self.int_cell(row_no, "Limit", limit, 1)))
        return out

    def load_example(self):
        self.segments.set_rows(SEG_EXAMPLE)
        self.addresses.set_rows(ADDR_EXAMPLE)
        self.mem.set("")

    def reset(self):
        self.segments.clear()
        self.addresses.clear()
        self.mem.set("")
        for _ in range(2):
            self.segments.add_row()
            self.addresses.add_row()

    def run(self):
        rows = self.read_segment_rows()
        self.require_rows(rows, "segment")
        mem = parse_int(self.mem.get(), "Physical memory size", 1) if self.mem.get().strip() else None
        table, warnings = seg.build_table([seg.SegmentEntry(*r) for r in rows], mem)
        addr_rows = self.addresses.rows()
        self.require_rows(addr_rows, "logical address")
        results = []
        for row_no, (s, o) in addr_rows:
            if "" in (s, o):
                raise ValidationError(f"Address row {row_no}: both segment and offset are required.")
            results.append(seg.translate(table, self.int_cell(row_no, "Segment", s),
                                         self.int_cell(row_no, "Offset", o)))

        body = self.result_card("Segment table")
        ResultTable(body, ["Segment", "Base", "Limit", "Valid offsets", "Physical range"],
                    [[s.seg_id, s.base, s.limit, f"0 - {s.limit - 1}", f"{s.base} - {s.base + s.limit - 1}"]
                     for s in sorted(table.values(), key=lambda s: s.seg_id)], max_height=10).pack(fill="x")
        if warnings:
            ttk.Label(body, text="Warning: " + " ".join(warnings), style="Warn.TLabel", wraplength=950,
                      justify="left").pack(anchor="w", pady=(6, 0))

        body = self.result_card("Address translation  (physical = base + offset)")
        tag = {seg.OK: "ok", seg.SEGMENT_FAULT: "bad", seg.OUT_OF_RANGE: "bad"}
        rows_out = [([t.segment, t.offset, "-" if t.limit is None else t.limit,
                      "-" if t.physical is None else t.physical, t.status, t.message], tag[t.status])
                    for t in results]
        ResultTable(body, ["Segment", "Offset", "Limit", "Physical address", "Status", "Explanation"],
                    rows_out, max_height=14).pack(fill="x")
        ok = sum(t.status == seg.OK for t in results)
        text = OutputText(body)
        text.pack(fill="x", pady=(8, 0))
        text.set_lines([(f"{ok} valid translation(s), {len(results) - ok} error(s).",
                         "ok" if ok == len(results) else "warn")])
        self.set_status(f"{ok}/{len(results)} addresses valid", "ok")
