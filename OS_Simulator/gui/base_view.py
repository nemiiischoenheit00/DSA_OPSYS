"""Common page layout and error handling shared by all eight module views.

Every page has:  header  ->  1. Input card  ->  Run / Reset bar  ->  Results cards.
Subclasses implement build_inputs(), run() and reset().
Anything raised inside run() is caught here, so bad input can never crash the application.
"""
import traceback
import tkinter as tk
from tkinter import messagebox, ttk

from core.validation import ValidationError, parse_int
from .styles import COLORS
from .widgets import Card, ScrollableFrame


class ModuleView(ttk.Frame):
    title = "Module"
    description = ""
    run_label = "Run simulation"

    def __init__(self, parent):
        super().__init__(parent)
        head = ttk.Frame(self)
        head.pack(fill="x", padx=30, pady=(22, 7))
        ttk.Label(head, text="SIMULATION MODULE", style="Eyebrow.TLabel").pack(anchor="w", pady=(0, 4))
        ttk.Label(head, text=self.title, style="Title.TLabel").pack(anchor="w")
        ttk.Label(head, text=self.description, style="Muted.TLabel", wraplength=1000,
                  justify="left").pack(anchor="w", pady=(2, 0))

        self.page = ScrollableFrame(self)
        self.page.pack(fill="both", expand=True, padx=(30, 14), pady=(8, 14))
        content = self.page.inner

        self.input_card = Card(content, "Configure your simulation", "Enter values below, or load an example to explore.")
        self.input_card.pack(fill="x", pady=(0, 10))
        self.build_inputs(self.input_card.body)

        bar = ttk.Frame(content)
        bar.pack(fill="x", pady=(0, 10))
        ttk.Button(bar, text=self.run_label, style="Accent.TButton", command=self._run_clicked).pack(side="left")
        ttk.Button(bar, text="Reset inputs", command=self._reset_clicked).pack(side="left", padx=8)
        self.status_var = tk.StringVar(value="")
        self.status_label = ttk.Label(bar, textvariable=self.status_var, style="Muted.TLabel")
        self.status_label.pack(side="left", padx=12)

        self.results = ttk.Frame(content)
        self.results.pack(fill="x")

    # ---- to be provided by each module view
    def build_inputs(self, parent):
        raise NotImplementedError

    def run(self):
        raise NotImplementedError

    def reset(self):
        raise NotImplementedError

    # ---- helpers for subclasses
    def result_card(self, title, subtitle=None):
        """Create a new results card and return its body frame."""
        card = Card(self.results, title, subtitle)
        card.pack(fill="x", pady=(0, 10))
        return card.body

    def clear_results(self):
        for child in self.results.winfo_children():
            child.destroy()

    def set_status(self, text, kind="info"):
        self.status_var.set(text)
        style = {"ok": "StatusOk.TLabel", "bad": "StatusBad.TLabel"}.get(kind, "Muted.TLabel")
        self.status_label.configure(style=style)

    def int_cell(self, row_no, column, text, minimum=None, maximum=None):
        """Parse a table cell; error messages name the row and column."""
        try:
            return parse_int(text, f"Row {row_no}, {column}", minimum, maximum)
        except ValidationError:
            raise

    def require_rows(self, rows, what):
        if not rows:
            raise ValidationError(f"Add at least one {what} (the table is empty).")

    def check_blank_cells(self, rows, columns, required):
        """Rows that are partly filled must have every `required` column filled in."""
        for row_no, values in rows:
            for idx in required:
                if values[idx] == "":
                    raise ValidationError(f"Row {row_no}: '{columns[idx]}' is empty.")

    # ---- button handlers (errors never propagate)
    def _run_clicked(self):
        self.clear_results()
        try:
            self.run()
            self.after(60, self._scroll_to_results)
        except ValidationError as err:
            self.clear_results()
            self.set_status("Invalid input - see message", "bad")
            messagebox.showwarning("Invalid input", str(err), parent=self)
        except Exception as err:                                  # last line of defence
            traceback.print_exc()
            self.clear_results()
            self.set_status("Unexpected error", "bad")
            messagebox.showerror("Unexpected error",
                                 f"Something went wrong while running the simulation:\n\n{err}", parent=self)

    def _reset_clicked(self):
        self.clear_results()
        try:
            self.reset()
        except Exception as err:
            traceback.print_exc()
            messagebox.showerror("Reset failed", str(err), parent=self)
        self.set_status("")

    def safe_call(self, func):
        """Run a helper button action (e.g. 'Load example') with the same error protection."""
        try:
            func()
        except ValidationError as err:
            messagebox.showwarning("Invalid input", str(err), parent=self)
        except Exception as err:
            traceback.print_exc()
            messagebox.showerror("Unexpected error", str(err), parent=self)

    def _scroll_to_results(self):
        try:
            canvas = self.page.canvas
            canvas.update_idletasks()
            total = max(self.page.inner.winfo_height(), 1)
            canvas.yview_moveto(min(1.0, self.results.winfo_y() / total))
        except Exception:
            pass
