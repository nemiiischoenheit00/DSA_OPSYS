"""Reusable GUI building blocks shared by all eight module pages.

ScrollableFrame - a frame that scrolls when its content is bigger than the window
Card            - titled panel used for every section (Input / Results ...)
DynamicTable    - editable table with Add / Remove / Clear / Bulk-paste / Random (no row limit)
MatrixGrid      - editable N x M grid of numbers (Banker's matrices)
ResultTable     - read-only table (Treeview) with scrollbars and coloured rows
OutputText      - read-only multi-line text box
"""
import tkinter as tk
from tkinter import messagebox, simpledialog, ttk

from core.validation import ValidationError
from .styles import COLORS, FONT_MONO


# ----------------------------------------------------------------------------- mouse wheel
def install_mousewheel(root):
    """One global handler: scroll the nearest scrollable canvas under the mouse pointer."""

    def on_wheel(event):
        widget = event.widget
        try:
            cls = widget.winfo_class()
            if cls == "TCombobox":
                return None
            if cls in ("Treeview", "Text", "Listbox"):
                # these scroll themselves when they have hidden content; otherwise the
                # wheel should scroll the whole page
                if widget.yview() != (0.0, 1.0):
                    return None
        except Exception:
            return None
        if getattr(event, "num", None) == 4:
            step = -1
        elif getattr(event, "num", None) == 5:
            step = 1
        else:
            step = -1 if event.delta > 0 else 1
        w = widget
        while w is not None:
            canvas = getattr(w, "scroll_canvas", None)
            if canvas is not None:
                first, last = canvas.yview()
                if (first, last) != (0.0, 1.0):
                    canvas.yview_scroll(step * 2, "units")
                    return "break"
            w = getattr(w, "master", None)
        return None

    root.bind_all("<MouseWheel>", on_wheel)
    root.bind_all("<Button-4>", on_wheel)       # Linux
    root.bind_all("<Button-5>", on_wheel)


# ----------------------------------------------------------------------------- scrolling
class ScrollableFrame(ttk.Frame):
    """Put child widgets in `.inner`. Vertical scrollbar always; horizontal on request."""

    def __init__(self, parent, height=None, horizontal=False, card=False):
        super().__init__(parent, style="Card.TFrame" if card else "TFrame")
        bg = COLORS["card"] if card else COLORS["bg"]
        self.horizontal = horizontal
        kw = {"height": height} if height else {}
        self.canvas = tk.Canvas(self, highlightthickness=0, bd=0, bg=bg, **kw)
        self.canvas._surface_key = "card" if card else "bg"
        self.vbar = ttk.Scrollbar(self, orient="vertical", command=self.canvas.yview)
        self.canvas.configure(yscrollcommand=self.vbar.set)
        self.canvas.grid(row=0, column=0, sticky="nsew")
        self.vbar.grid(row=0, column=1, sticky="ns")
        if horizontal:
            self.hbar = ttk.Scrollbar(self, orient="horizontal", command=self.canvas.xview)
            self.canvas.configure(xscrollcommand=self.hbar.set)
            self.hbar.grid(row=1, column=0, sticky="ew")
        self.columnconfigure(0, weight=1)
        self.rowconfigure(0, weight=1)
        self.inner = ttk.Frame(self.canvas, style="Card.TFrame" if card else "TFrame")
        self._window = self.canvas.create_window((0, 0), window=self.inner, anchor="nw")
        self.inner.bind("<Configure>", self._on_inner_configure)
        self.canvas.bind("<Configure>", self._on_canvas_configure)
        self.canvas.scroll_canvas = self.canvas          # marker used by install_mousewheel

    def _on_inner_configure(self, _event=None):
        self.canvas.configure(scrollregion=self.canvas.bbox("all"))

    def _on_canvas_configure(self, event):
        if not self.horizontal:                          # stretch content to the visible width
            self.canvas.itemconfigure(self._window, width=event.width)


class Card(ttk.Frame):
    """A titled panel. Add content to `card.body`."""

    def __init__(self, parent, title, subtitle=None):
        super().__init__(parent, style="Card.TFrame")
        ttk.Label(self, text=title, style="Heading.TLabel").pack(anchor="w", padx=16, pady=(12, 0))
        if subtitle:
            ttk.Label(self, text=subtitle, style="CardMuted.TLabel", wraplength=1000,
                      justify="left").pack(anchor="w", padx=16, pady=(2, 0))
        self.body = ttk.Frame(self, style="Card.TFrame")
        self.body.pack(fill="both", expand=True, padx=16, pady=12)


# ----------------------------------------------------------------------------- form helpers
def labeled_entry(parent, label, var, width=8):
    frame = ttk.Frame(parent, style="Card.TFrame")
    ttk.Label(frame, text=label, style="Card.TLabel").pack(side="left", padx=(0, 6))
    entry = ttk.Entry(frame, textvariable=var, width=width)
    entry.pack(side="left")
    frame.entry = entry
    return frame


def labeled_combo(parent, label, var, values, width=16):
    frame = ttk.Frame(parent, style="Card.TFrame")
    ttk.Label(frame, text=label, style="Card.TLabel").pack(side="left", padx=(0, 6))
    combo = ttk.Combobox(frame, textvariable=var, values=list(values), width=width, state="readonly")
    combo.pack(side="left")
    frame.combo = combo
    return frame


def set_enabled(widget, enabled):
    """Enable/disable a ttk widget."""
    widget.state(["!disabled"] if enabled else ["disabled"])


# ----------------------------------------------------------------------------- DynamicTable
class DynamicTable(ttk.Frame):
    """Editable table that grows and shrinks at run time - there is NO limit on the row count.

    columns   : header names
    widths    : entry widths in characters (default 12)
    auto_fill : optional function(n) -> text used to pre-fill column 0 of new rows (n starts at 0)
    defaults  : optional list of default texts for the other columns of new rows
    generator : optional function(count) -> list of rows, enables the 'Random...' button
    """

    def __init__(self, parent, columns, widths=None, auto_fill=None, defaults=None,
                 generator=None, height=170, item_name="row"):
        super().__init__(parent, style="Card.TFrame")
        self.columns = list(columns)
        self.widths = widths or [12] * len(columns)
        self.auto_fill = auto_fill
        self.defaults = defaults or [""] * len(columns)
        self.generator = generator
        self.item_name = item_name
        self._rows = []                  # each: {"frame", "vars", "entries", "number"}
        self._counter = 0

        header = ttk.Frame(self, style="Card.TFrame")
        header.pack(fill="x")
        ttk.Label(header, text="#", style="CardMuted.TLabel", width=4).pack(side="left")
        for name, w in zip(self.columns, self.widths):
            ttk.Label(header, text=name, style="CardMuted.TLabel", width=w + 2).pack(side="left", padx=2)

        self.scroll = ScrollableFrame(self, height=height, card=True)
        self.scroll.pack(fill="both", expand=True, pady=(2, 6))
        self.body = self.scroll.inner

        bar = ttk.Frame(self, style="Card.TFrame")
        bar.pack(fill="x")
        ttk.Button(bar, text=f"+ Add {item_name}", style="Small.TButton",
                   command=lambda: self.add_row(focus=True)).pack(side="left", padx=(0, 6))
        ttk.Button(bar, text="+ Add several...", style="Small.TButton",
                   command=self._ask_add_several).pack(side="left", padx=(0, 6))
        ttk.Button(bar, text="Paste / bulk entry...", style="Small.TButton",
                   command=self._bulk_dialog).pack(side="left", padx=(0, 6))
        if generator:
            ttk.Button(bar, text="Random data...", style="Small.TButton",
                       command=self._random_dialog).pack(side="left", padx=(0, 6))
        ttk.Button(bar, text=f"Remove last", style="Small.TButton",
                   command=self.remove_last).pack(side="left", padx=(0, 6))
        ttk.Button(bar, text="Clear all", style="Small.TButton", command=self.clear).pack(side="left")
        self.count_var = tk.StringVar(value="0 rows")
        ttk.Label(bar, textvariable=self.count_var, style="CardMuted.TLabel").pack(side="right")

    # ---- row management
    def add_row(self, values=None, focus=False):
        if values is None:
            values = list(self.defaults)
            if self.auto_fill:
                values[0] = self.auto_fill(self._counter)
        values = [str(v) for v in values] + [""] * (len(self.columns) - len(values))
        self._counter += 1
        frame = ttk.Frame(self.body, style="Card.TFrame")
        frame.pack(fill="x", pady=1)
        number = ttk.Label(frame, style="CardMuted.TLabel", width=4)
        number.pack(side="left")
        row = {"frame": frame, "vars": [], "entries": [], "number": number}
        for c, (w, text) in enumerate(zip(self.widths, values)):
            var = tk.StringVar(value=text)
            entry = ttk.Entry(frame, textvariable=var, width=w)
            entry.pack(side="left", padx=2)
            entry.bind("<Return>", lambda e, r=row, col=c: self._move(r, col, +1))
            entry.bind("<Down>", lambda e, r=row, col=c: self._move(r, col, +1))
            entry.bind("<Up>", lambda e, r=row, col=c: self._move(r, col, -1))
            row["vars"].append(var)
            row["entries"].append(entry)
        ttk.Button(frame, text="x", style="Remove.TButton", width=2,
                   command=lambda r=row: self._remove(r)).pack(side="left", padx=4)
        self._rows.append(row)
        self._renumber()
        if focus:
            row["entries"][min(1, len(self.columns) - 1)].focus_set()
        return row

    def _move(self, row, col, direction):
        i = self._rows.index(row) + direction
        if i < 0:
            return "break"
        if i >= len(self._rows):
            new = self.add_row(focus=False)
            new["entries"][col].focus_set()
        else:
            self._rows[i]["entries"][col].focus_set()
        return "break"

    def _remove(self, row):
        if row in self._rows:
            self._rows.remove(row)
            row["frame"].destroy()
            self._renumber()

    def remove_last(self):
        if self._rows:
            self._remove(self._rows[-1])

    def clear(self):
        for row in self._rows:
            row["frame"].destroy()
        self._rows = []
        self._counter = 0
        self._renumber()

    def _renumber(self):
        for i, row in enumerate(self._rows, start=1):
            row["number"].configure(text=str(i))
        n = len(self._rows)
        self.count_var.set(f"{n} {'row' if n == 1 else 'rows'}")

    def set_rows(self, rows):
        self.clear()
        for r in rows:
            self.add_row(list(r))

    def rows(self):
        """Return [(row_number, [text, ...]), ...] skipping rows where every cell is blank."""
        out = []
        for i, row in enumerate(self._rows, start=1):
            values = [v.get().strip() for v in row["vars"]]
            if any(values):
                out.append((i, values))
        return out

    # ---- dialogs
    def _ask_add_several(self):
        n = simpledialog.askinteger("Add rows", f"How many {self.item_name}s do you want to add?",
                                    parent=self, minvalue=1, initialvalue=5)
        if n:
            for _ in range(n):
                self.add_row()

    def _random_dialog(self):
        n = simpledialog.askinteger("Random data", f"How many {self.item_name}s should be generated?",
                                    parent=self, minvalue=1, initialvalue=20)
        if not n:
            return
        try:
            self.set_rows(self.generator(n))
        except ValidationError as err:
            messagebox.showwarning("Cannot generate data", str(err), parent=self)

    def _bulk_dialog(self):
        top = tk.Toplevel(self)
        top.title("Paste / bulk entry")
        top.configure(background=COLORS["card"])
        top.geometry("520x380")
        cols = ", ".join(self.columns)
        msg = (f"One {self.item_name} per line. Separate columns with spaces, commas or tabs.\n"
               f"Columns: {cols}")
        if self.auto_fill:
            msg += f"\n(You may leave out the first column '{self.columns[0]}' - it will be generated.)"
        ttk.Label(top, text=msg, style="Card.TLabel", justify="left").pack(anchor="w", padx=12, pady=(12, 6))
        text = tk.Text(top, height=12, bg=COLORS["entry"], fg=COLORS["text"],
                       insertbackground=COLORS["text"], relief="flat", font=FONT_MONO)
        text.pack(fill="both", expand=True, padx=12)
        replace = tk.BooleanVar(value=True)
        ttk.Checkbutton(top, text="Replace existing rows", variable=replace,
                        style="Card.TCheckbutton").pack(anchor="w", padx=12, pady=6)

        def apply():
            try:
                rows = self._parse_bulk(text.get("1.0", "end"))
            except ValidationError as err:
                messagebox.showwarning("Cannot read the pasted data", str(err), parent=top)
                return
            if replace.get():
                self.clear()
            for r in rows:
                self.add_row(r)
            top.destroy()

        buttons = ttk.Frame(top, style="Card.TFrame")
        buttons.pack(fill="x", padx=12, pady=(0, 12))
        ttk.Button(buttons, text="Add rows", style="Accent.TButton", command=apply).pack(side="right")
        ttk.Button(buttons, text="Cancel", command=top.destroy).pack(side="right", padx=6)
        top.transient(self.winfo_toplevel())

    def _parse_bulk(self, raw):
        import re
        ncols = len(self.columns)
        rows = []
        for line_no, line in enumerate(raw.splitlines(), start=1):
            tokens = [t for t in re.split(r"[,\s;]+", line.strip()) if t]
            if not tokens:
                continue
            if len(tokens) > ncols:
                raise ValidationError(f"Line {line_no} has {len(tokens)} values but there are only "
                                      f"{ncols} columns ({', '.join(self.columns)}).")
            if self.auto_fill and len(tokens) == ncols - 1:
                tokens = [self.auto_fill(self._counter + len(rows))] + tokens
            rows.append(tokens + [""] * (ncols - len(tokens)))
        if not rows:
            raise ValidationError("Nothing to add: the text box is empty.")
        return rows


# ----------------------------------------------------------------------------- MatrixGrid
class MatrixGrid(ttk.Frame):
    """Editable rows x cols grid of entries (used for Allocation / Maximum / Available)."""

    def __init__(self, parent, rows, cols, row_labels, col_labels, height=190, cell_width=5):
        super().__init__(parent, style="Card.TFrame")
        self.rows, self.cols = rows, cols
        self.scroll = ScrollableFrame(self, height=height, horizontal=True, card=True)
        self.scroll.pack(fill="both", expand=True)
        grid = self.scroll.inner
        self.vars = []
        for j, name in enumerate(col_labels):
            ttk.Label(grid, text=name, style="CardMuted.TLabel", anchor="center").grid(
                row=0, column=j + 1, padx=2, pady=(0, 2))
        for i in range(rows):
            ttk.Label(grid, text=row_labels[i], style="CardMuted.TLabel").grid(row=i + 1, column=0, padx=(0, 6))
            line = []
            for j in range(cols):
                var = tk.StringVar(value="0")
                entry = ttk.Entry(grid, textvariable=var, width=cell_width, justify="center")
                entry.grid(row=i + 1, column=j + 1, padx=2, pady=1)
                line.append(var)
            self.vars.append(line)

    def values(self):
        return [[v.get().strip() for v in line] for line in self.vars]

    def set_values(self, matrix):
        for i, line in enumerate(matrix):
            for j, val in enumerate(line):
                if i < self.rows and j < self.cols:
                    self.vars[i][j].set(str(val))


# ----------------------------------------------------------------------------- ResultTable
class ResultTable(ttk.Frame):
    """Read-only table. rows: list of values, or (values, tag) pairs. Tags: ok, bad, warn, accent."""

    def __init__(self, parent, columns, rows, max_height=12, on_select=None):
        super().__init__(parent, style="Card.TFrame")
        self.columns = list(columns)
        height = max(1, min(len(rows), max_height))
        self.tree = ttk.Treeview(self, columns=self.columns, show="headings", height=height,
                                 selectmode="browse")
        vbar = ttk.Scrollbar(self, orient="vertical", command=self.tree.yview)
        hbar = ttk.Scrollbar(self, orient="horizontal", command=self.tree.xview)
        self.tree.configure(yscrollcommand=vbar.set, xscrollcommand=hbar.set)
        self.tree.grid(row=0, column=0, sticky="nsew")
        vbar.grid(row=0, column=1, sticky="ns")
        hbar.grid(row=1, column=0, sticky="ew")
        self.columnconfigure(0, weight=1)
        self.rowconfigure(0, weight=1)
        for tag, color in (("ok", COLORS["ok"]), ("bad", COLORS["bad"]),
                           ("warn", COLORS["warn"]), ("accent", COLORS["accent_hover"])):
            self.tree.tag_configure(tag, foreground=color)

        normalised = [r if isinstance(r, tuple) else (r, None) for r in rows]
        for c in self.columns:
            longest = max([len(str(c))] + [len(str(vals[self.columns.index(c)]))
                                           for vals, _ in normalised[:200]])
            width = min(max(60, 9 * longest + 18), 420)
            self.tree.heading(c, text=c)
            self.tree.column(c, width=width, minwidth=width, anchor="center", stretch=True)
        for i, (vals, tag) in enumerate(normalised):
            self.tree.insert("", "end", iid=str(i), values=[str(v) for v in vals],
                             tags=(tag,) if tag else ())
        if on_select:
            self.tree.bind("<<TreeviewSelect>>", lambda e: self._selected(on_select))

    def refresh_theme(self):
        for tag, color in (("ok", COLORS["ok"]), ("bad", COLORS["bad"]),
                           ("warn", COLORS["warn"]), ("accent", COLORS["accent_hover"])):
            self.tree.tag_configure(tag, foreground=color)

    def _selected(self, callback):
        sel = self.tree.selection()
        if sel:
            callback(int(sel[0]))


class OutputText(tk.Text):
    """Read-only text area. Use set_lines([(text, tag), ...]). Tags: ok, bad, warn, head."""

    def __init__(self, parent, max_lines=60):
        super().__init__(parent, bg=COLORS["entry"], fg=COLORS["text"], relief="flat",
                         wrap="word", font=FONT_MONO, padx=10, pady=8, highlightthickness=0, height=4)
        self.max_lines = max_lines
        self.tag_configure("ok", foreground=COLORS["ok"])
        self.tag_configure("bad", foreground=COLORS["bad"])
        self.tag_configure("warn", foreground=COLORS["warn"])
        self.tag_configure("head", foreground=COLORS["accent_hover"])

    def set_lines(self, lines):
        self.configure(state="normal")
        self.delete("1.0", "end")
        for text, tag in lines:
            self.insert("end", text + "\n", (tag,) if tag else ())
        visual = sum(1 + len(text) // 105 for text, _ in lines)     # allow for wrapped lines
        self.configure(state="disabled", height=max(2, min(visual + 1, self.max_lines)))

    def set_text(self, text, tag=None):
        self.set_lines([(line, tag) for line in text.split("\n")])
