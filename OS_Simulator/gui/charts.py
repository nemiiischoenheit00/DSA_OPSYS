"""Charts drawn directly on tkinter Canvas widgets (no matplotlib needed, so nothing extra to install).

GanttChart  - CPU schedule timeline
DiskChart   - disk-head movement (cylinder on x axis, time going down)
MemoryMap   - horizontal memory bars (fragmentation blocks, multiprogramming memory)
BarChart    - simple vertical bars for comparing algorithms
StepChart   - step line (e.g. memory utilisation over time)
"""
import tkinter as tk
from tkinter import ttk

from .styles import COLORS, FONT_SMALL, PALETTE, pid_color

MAX_DRAWN = 5000          # drawing limit for very long charts (tables always show everything)


class _ChartBase(ttk.Frame):
    def __init__(self, parent, width, height, scroll_x=False, scroll_y=False):
        super().__init__(parent, style="Card.TFrame")
        self.canvas = tk.Canvas(self, width=width, height=height, bg=COLORS["card"],
                                highlightthickness=0, bd=0)
        self.canvas.grid(row=0, column=0, sticky="nsew")
        if scroll_x:
            hbar = ttk.Scrollbar(self, orient="horizontal", command=self.canvas.xview)
            self.canvas.configure(xscrollcommand=hbar.set)
            hbar.grid(row=1, column=0, sticky="ew")
        if scroll_y:
            vbar = ttk.Scrollbar(self, orient="vertical", command=self.canvas.yview)
            self.canvas.configure(yscrollcommand=vbar.set)
            vbar.grid(row=0, column=1, sticky="ns")
            self.canvas.scroll_canvas = self.canvas
        self.columnconfigure(0, weight=1)

    def clear(self):
        self.canvas.delete("all")


class GanttChart(_ChartBase):
    def __init__(self, parent, width=860):
        super().__init__(parent, width, 115, scroll_x=True)

    def draw(self, segments, idle_label="IDLE"):
        """segments: list of (label, start, end)."""
        self.clear()
        if not segments:
            return
        shown = segments[:MAX_DRAWN]
        total = max(end for _, _, end in shown)
        unit = max(6.0, min(60.0, 820.0 / max(total, 1)))
        left, top, h = 20, 25, 44
        colors = {}
        last_label_x = -100
        for label, start, end in shown:
            x0, x1 = left + start * unit, left + end * unit
            if label == idle_label:
                fill = "#3a3f4f"
            else:
                fill = colors.setdefault(label, pid_color(len(colors)))
            self.canvas.create_rectangle(x0, top, x1, top + h, fill=fill, outline=COLORS["card"], width=1)
            if x1 - x0 >= 18:
                text = label if (x1 - x0) >= 8 * len(label) else label[: max(1, int((x1 - x0) // 8))]
                self.canvas.create_text((x0 + x1) / 2, top + h / 2, text=text, fill="#0b0d12",
                                        font=("Segoe UI", 9, "bold"))
            if x0 - last_label_x >= 26:                       # avoid overlapping time labels
                self.canvas.create_text(x0, top + h + 14, text=str(start), fill=COLORS["muted"], font=FONT_SMALL)
                last_label_x = x0
        end_x = left + total * unit
        self.canvas.create_text(end_x, top + h + 14, text=str(total), fill=COLORS["muted"], font=FONT_SMALL)
        if len(segments) > MAX_DRAWN:
            self.canvas.create_text(left, 8, anchor="w", fill=COLORS["warn"], font=FONT_SMALL,
                                    text=f"Chart shows the first {MAX_DRAWN} of {len(segments)} segments "
                                         f"(the table lists everything).")
        self.canvas.configure(scrollregion=(0, 0, end_x + 40, 110))


class DiskChart(_ChartBase):
    def __init__(self, parent, width=860):
        super().__init__(parent, width, 330, scroll_y=True)
        self.width = width

    def draw(self, path, low, high):
        """path: list of Step objects from core.disk_scheduling."""
        self.clear()
        left, right, top, row_h = 40, self.width - 40, 36, 24
        span = max(high - low, 1)

        def x_of(pos):
            return left + (pos - low) * (right - left) / span

        steps = path[:MAX_DRAWN]
        bottom = top + row_h * len(steps) + 20
        for frac in (0, 0.25, 0.5, 0.75, 1):                     # cylinder axis with grid lines
            pos = low + span * frac
            x = x_of(pos)
            self.canvas.create_line(x, top - 6, x, bottom, fill=COLORS["border"], dash=(2, 4))
            self.canvas.create_text(x, 14, text=str(int(round(pos))), fill=COLORS["muted"], font=FONT_SMALL)
        points = [(x_of(s.position), top + i * row_h) for i, s in enumerate(steps)]
        for (x0, y0), (x1, y1), s in zip(points, points[1:], steps[1:]):
            colour = COLORS["bad"] if s.jump else COLORS["accent"]
            self.canvas.create_line(x0, y0, x1, y1, fill=colour, width=2, dash=(5, 3) if s.jump else None)
        label_points = len(steps) <= 400
        for (x, y), s in zip(points, steps):
            colour = {"start": COLORS["ok"], "edge": COLORS["warn"]}.get(s.kind, COLORS["accent_hover"])
            self.canvas.create_oval(x - 4, y - 4, x + 4, y + 4, fill=colour, outline="")
            if label_points:
                self.canvas.create_text(x + 8, y - 8, text=str(s.position), anchor="w",
                                        fill=COLORS["text"], font=FONT_SMALL)
        self.canvas.create_text(left, bottom + 4, anchor="w", font=FONT_SMALL, fill=COLORS["muted"],
                                text="green = start   blue = request served   amber = disk edge   "
                                     "red dashed = return jump")
        self.canvas.configure(scrollregion=(0, 0, self.width, bottom + 24))


class MemoryMap(_ChartBase):
    """Draws one horizontal bar per row. rows: [(title, total, segments)],
    segments: [(size, label, kind)] laid out left to right; kind is 'os', 'free' or a process id."""

    def __init__(self, parent, width=860, height=120):
        super().__init__(parent, width, height, scroll_y=True)
        self.width = width
        self.canvas.configure(height=height)
        self._colors = {}

    def _color(self, key):
        return self._colors.setdefault(key, pid_color(len(self._colors)))

    def draw(self, rows, height=None):
        self.clear()
        label_w, bar_h, gap = 110, 26, 14
        biggest = max((total for _, total, _ in rows), default=1)
        scale = (self.width - label_w - 30) / max(biggest, 1)
        shown = rows[:MAX_DRAWN]
        for r, (title, total, segments) in enumerate(shown):
            y = 12 + r * (bar_h + gap)
            self.canvas.create_text(8, y + bar_h / 2, text=title, anchor="w", fill=COLORS["text"], font=FONT_SMALL)
            x = label_w
            for size, text, kind in segments:
                w = size * scale
                if kind == "free":
                    fill, txt_color = "#2d3242", COLORS["muted"]
                elif kind == "os":
                    fill, txt_color = "#6b7280", "#ffffff"
                else:
                    fill, txt_color = self._color(kind), "#0b0d12"
                self.canvas.create_rectangle(x, y, x + w, y + bar_h, fill=fill, outline=COLORS["card"])
                if w >= 8 * len(text) + 6:
                    self.canvas.create_text(x + w / 2, y + bar_h / 2, text=text, fill=txt_color, font=FONT_SMALL)
                x += w
            self.canvas.create_rectangle(label_w, y, label_w + total * scale, y + bar_h,
                                         outline=COLORS["border"])
        end = 12 + len(shown) * (bar_h + gap) + 18
        self.canvas.create_text(label_w, end - 6, anchor="w", fill=COLORS["muted"], font=FONT_SMALL,
                                text="coloured = in use     dark grey = free     light grey = OS area")
        self.canvas.configure(scrollregion=(0, 0, self.width, end + 8),
                              height=height or min(max(end + 8, 80), 420))


class BarChart(_ChartBase):
    def __init__(self, parent, width=860, height=220):
        super().__init__(parent, width, height)
        self.width, self.height = width, height

    def draw(self, labels, values, title="", fmt="{:g}", best_is_min=True):
        self.clear()
        if not values:
            return
        left, bottom, top = 50, self.height - 40, 34
        vmax = max(values) or 1
        n = len(values)
        slot = (self.width - left - 20) / n
        bar_w = min(70, slot * 0.6)
        best = min(values) if best_is_min else max(values)
        self.canvas.create_text(left, 12, text=title, anchor="w", fill=COLORS["text"], font=("Segoe UI", 10, "bold"))
        self.canvas.create_line(left, bottom, self.width - 10, bottom, fill=COLORS["border"])
        for i, (label, value) in enumerate(zip(labels, values)):
            x = left + slot * i + (slot - bar_w) / 2
            h = (bottom - top) * value / vmax
            colour = COLORS["ok"] if value == best else PALETTE[i % len(PALETTE)]
            self.canvas.create_rectangle(x, bottom - h, x + bar_w, bottom, fill=colour, outline="")
            self.canvas.create_text(x + bar_w / 2, bottom - h - 9, text=fmt.format(value),
                                    fill=COLORS["text"], font=FONT_SMALL)
            self.canvas.create_text(x + bar_w / 2, bottom + 14, text=label, fill=COLORS["muted"], font=FONT_SMALL)
        self.canvas.create_text(self.width - 10, 12, anchor="e", fill=COLORS["ok"], font=FONT_SMALL,
                                text="green = best")


class StepChart(_ChartBase):
    def __init__(self, parent, width=860, height=170):
        super().__init__(parent, width, height)
        self.width, self.height = width, height

    def draw(self, points, ymax=100.0, title="", y_unit="%"):
        """points: [(time, value)] - the value holds until the next point."""
        self.clear()
        if len(points) < 2:
            return
        left, right, top, bottom = 50, self.width - 20, 30, self.height - 30
        tmax = max(t for t, _ in points) or 1
        self.canvas.create_text(left, 12, text=title, anchor="w", fill=COLORS["text"], font=("Segoe UI", 10, "bold"))
        for frac in (0, 0.5, 1):
            y = bottom - (bottom - top) * frac
            self.canvas.create_line(left, y, right, y, fill=COLORS["border"], dash=(2, 4))
            self.canvas.create_text(left - 6, y, anchor="e", fill=COLORS["muted"], font=FONT_SMALL,
                                    text=f"{ymax * frac:g}{y_unit}")
        coords = []
        for (t, v), nxt in zip(points, points[1:] + [(tmax, points[-1][1])]):
            x0 = left + (right - left) * t / tmax
            x1 = left + (right - left) * nxt[0] / tmax
            y = bottom - (bottom - top) * min(v, ymax) / ymax
            coords += [x0, y, x1, y]
        self.canvas.create_line(*coords, fill=COLORS["accent"], width=2)
        self.canvas.create_text(left, bottom + 14, text="0", fill=COLORS["muted"], font=FONT_SMALL)
        self.canvas.create_text(right, bottom + 14, text=f"time {tmax}", anchor="e", fill=COLORS["muted"],
                                font=FONT_SMALL)
