"""Dark theme: one place that defines colours, fonts and ttk styles for the whole app."""
from tkinter import ttk

COLORS = {
    "bg": "#14161c",        # window background
    "panel": "#1b1e27",     # sidebar / headers
    "card": "#232733",      # content cards
    "border": "#363c4d",
    "entry": "#10131a",     # input fields
    "text": "#e8eaf0",
    "muted": "#9aa3b5",
    "accent": "#5b8cff",
    "accent_hover": "#7aa2ff",
    "ok": "#4ade80",
    "bad": "#f87171",
    "warn": "#fbbf24",
    "select": "#34407a",
}

FONT = ("Segoe UI", 10)
FONT_BOLD = ("Segoe UI", 10, "bold")
FONT_SMALL = ("Segoe UI", 9)
FONT_TITLE = ("Segoe UI", 20, "bold")
FONT_HEADING = ("Segoe UI", 12, "bold")
FONT_MONO = ("Consolas", 10)

# Colours cycled through for processes / segments in charts
PALETTE = ["#5b8cff", "#4ade80", "#fbbf24", "#f472b6", "#22d3ee", "#a78bfa",
           "#fb923c", "#34d399", "#f87171", "#60a5fa", "#c084fc", "#facc15"]


def pid_color(index):
    return PALETTE[index % len(PALETTE)]


def apply_theme(root):
    """Configure ttk widgets (called once at start-up)."""
    c = COLORS
    root.configure(background=c["bg"])
    style = ttk.Style(root)
    try:
        style.theme_use("clam")          # 'clam' is the most colour-customisable built-in theme
    except Exception:
        pass
    style.configure(".", background=c["bg"], foreground=c["text"], fieldbackground=c["entry"],
                    bordercolor=c["border"], lightcolor=c["border"], darkcolor=c["border"],
                    troughcolor=c["panel"], font=FONT, focuscolor=c["accent"])

    style.configure("TFrame", background=c["bg"])
    style.configure("Panel.TFrame", background=c["panel"])
    style.configure("Card.TFrame", background=c["card"])
    style.configure("TLabel", background=c["bg"], foreground=c["text"])
    style.configure("Card.TLabel", background=c["card"], foreground=c["text"])
    style.configure("Muted.TLabel", background=c["bg"], foreground=c["muted"])
    style.configure("CardMuted.TLabel", background=c["card"], foreground=c["muted"], font=FONT_SMALL)
    style.configure("Title.TLabel", font=FONT_TITLE)
    style.configure("Heading.TLabel", background=c["card"], foreground=c["text"], font=FONT_HEADING)
    style.configure("Brand.TLabel", background=c["panel"], foreground=c["text"], font=("Segoe UI", 15, "bold"))
    style.configure("BrandSub.TLabel", background=c["panel"], foreground=c["muted"], font=FONT_SMALL)
    style.configure("Ok.TLabel", background=c["card"], foreground=c["ok"], font=("Segoe UI", 13, "bold"))
    style.configure("Bad.TLabel", background=c["card"], foreground=c["bad"], font=("Segoe UI", 13, "bold"))
    style.configure("Warn.TLabel", background=c["card"], foreground=c["warn"], font=FONT_BOLD)
    style.configure("StatusOk.TLabel", foreground=c["ok"])
    style.configure("StatusBad.TLabel", foreground=c["bad"])

    style.configure("TButton", background=c["border"], foreground=c["text"], borderwidth=0,
                    padding=(12, 6), relief="flat")
    style.map("TButton", background=[("active", "#464e66"), ("disabled", c["card"])],
              foreground=[("disabled", c["muted"])])
    style.configure("Accent.TButton", background=c["accent"], foreground="#ffffff",
                    font=FONT_BOLD, padding=(16, 8))
    style.map("Accent.TButton", background=[("active", c["accent_hover"])])
    style.configure("Nav.TButton", background=c["panel"], foreground=c["muted"], anchor="w",
                    padding=(18, 10), borderwidth=0)
    style.map("Nav.TButton", background=[("active", c["card"])], foreground=[("active", c["text"])])
    style.configure("NavActive.TButton", background=c["card"], foreground=c["accent_hover"], anchor="w",
                    padding=(18, 10), borderwidth=0, font=FONT_BOLD)
    style.configure("Small.TButton", padding=(8, 3), font=FONT_SMALL)
    style.configure("Remove.TButton", padding=(4, 0), font=FONT_SMALL)

    style.configure("TEntry", fieldbackground=c["entry"], foreground=c["text"], insertcolor=c["text"],
                    padding=4, bordercolor=c["border"])
    style.map("TEntry", bordercolor=[("focus", c["accent"])])
    style.configure("TCombobox", fieldbackground=c["entry"], background=c["border"], foreground=c["text"],
                    arrowcolor=c["text"], padding=4)
    style.map("TCombobox", fieldbackground=[("readonly", c["entry"])],
              foreground=[("readonly", c["text"])], selectbackground=[("readonly", c["entry"])],
              selectforeground=[("readonly", c["text"])])
    root.option_add("*TCombobox*Listbox.background", c["entry"])
    root.option_add("*TCombobox*Listbox.foreground", c["text"])
    root.option_add("*TCombobox*Listbox.selectBackground", c["accent"])
    root.option_add("*TCombobox*Listbox.selectForeground", "#ffffff")

    for name in ("TCheckbutton", "Card.TCheckbutton"):
        bg = c["card"] if name.startswith("Card") else c["bg"]
        style.configure(name, background=bg, foreground=c["text"])
        style.map(name, background=[("active", bg)],
                  indicatorcolor=[("selected", c["accent"]), ("!selected", c["entry"])])

    style.configure("Treeview", background=c["card"], fieldbackground=c["card"], foreground=c["text"],
                    rowheight=24, borderwidth=0)
    style.map("Treeview", background=[("selected", c["select"])], foreground=[("selected", "#ffffff")])
    style.configure("Treeview.Heading", background=c["border"], foreground=c["text"], font=FONT_BOLD,
                    relief="flat", padding=4)
    style.map("Treeview.Heading", background=[("active", "#464e66")])

    style.configure("Vertical.TScrollbar", background=c["border"], troughcolor=c["panel"],
                    arrowcolor=c["text"], bordercolor=c["panel"])
    style.configure("Horizontal.TScrollbar", background=c["border"], troughcolor=c["panel"],
                    arrowcolor=c["text"], bordercolor=c["panel"])
    return style
