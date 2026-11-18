"""Visual system for the simulator: calm ink surfaces, clear hierarchy and a single accent."""
from tkinter import ttk

THEMES = {
    "dark": {
    "bg": "#0D1220",
    "panel": "#111827",
    "card": "#171F30",
    "card_alt": "#1C2639",
    "border": "#29354A",
    "entry": "#101827",
    "text": "#F2F5FB",
    "muted": "#9AA8BD",
    "accent": "#69D2C1",
    "accent_hover": "#8BE2D4",
    "ok": "#6DDBA6",
    "bad": "#FF8585",
    "warn": "#F4C86A",
    "select": "#264B54",
    },
    "light": {
        "bg": "#F3F6FA", "panel": "#E8EEF5", "card": "#FFFFFF", "card_alt": "#E6F0F2",
        "border": "#D4DEE8", "entry": "#FFFFFF", "text": "#1B2738", "muted": "#64748B",
        "accent": "#087F75", "accent_hover": "#06675F", "ok": "#13845B", "bad": "#C44545",
        "warn": "#9A6700", "select": "#CDEAE5",
    },
}
THEME_PALETTES = {
    "dark": ["#69D2C1", "#F4C86A", "#8EA8FF", "#F08FB3", "#68C7E8", "#BEA2FF",
             "#F39B70", "#75D7A1", "#FF8585", "#72A7F8", "#D28BE8", "#CADB78"],
    "light": ["#087F75", "#B87900", "#496BC2", "#C14E78", "#1882A5", "#7953B6",
              "#C05D2A", "#27834D", "#C44545", "#3769B2", "#9A4DAA", "#71851A"],
}
CURRENT_THEME = "dark"
COLORS = dict(THEMES[CURRENT_THEME])

FONT = ("Segoe UI", 10)
FONT_BOLD = ("Segoe UI", 10, "bold")
FONT_SMALL = ("Segoe UI", 9)
FONT_TITLE = ("Segoe UI", 24, "bold")
FONT_HEADING = ("Segoe UI", 12, "bold")
FONT_MONO = ("Cascadia Code", 10)
PALETTE = list(THEME_PALETTES[CURRENT_THEME])


def pid_color(index):
    return PALETTE[index % len(PALETTE)]


def set_theme(name):
    """Switch the shared color tokens in place so existing imports see the change."""
    global CURRENT_THEME
    if name not in THEMES:
        raise ValueError(f"Unknown theme: {name}")
    CURRENT_THEME = name
    COLORS.clear()
    COLORS.update(THEMES[name])
    PALETTE[:] = THEME_PALETTES[name]


def apply_theme(root):
    """Apply the shared ttk theme once at application startup."""
    c = COLORS
    root.configure(background=c["bg"])
    style = ttk.Style(root)
    try:
        style.theme_use("clam")
    except Exception:
        pass
    style.configure(".", background=c["bg"], foreground=c["text"], fieldbackground=c["entry"],
                    bordercolor=c["border"], lightcolor=c["border"], darkcolor=c["border"],
                    troughcolor=c["panel"], font=FONT, focuscolor=c["accent"])
    style.configure("TFrame", background=c["bg"])
    style.configure("Panel.TFrame", background=c["panel"])
    style.configure("Card.TFrame", background=c["card"])
    style.configure("AltCard.TFrame", background=c["card_alt"])
    style.configure("TLabel", background=c["bg"], foreground=c["text"])
    style.configure("Card.TLabel", background=c["card"], foreground=c["text"])
    style.configure("AltCard.TLabel", background=c["card_alt"], foreground=c["text"])
    style.configure("Muted.TLabel", background=c["bg"], foreground=c["muted"])
    style.configure("CardMuted.TLabel", background=c["card"], foreground=c["muted"], font=FONT_SMALL)
    style.configure("Title.TLabel", font=FONT_TITLE)
    style.configure("Heading.TLabel", background=c["card"], foreground=c["text"], font=FONT_HEADING)
    style.configure("Brand.TLabel", background=c["panel"], foreground=c["text"], font=("Segoe UI", 16, "bold"))
    style.configure("BrandSub.TLabel", background=c["panel"], foreground=c["muted"], font=FONT_SMALL)
    style.configure("Eyebrow.TLabel", foreground=c["accent"], font=("Segoe UI", 9, "bold"))
    style.configure("AltEyebrow.TLabel", background=c["card_alt"], foreground=c["accent"],
                    font=("Segoe UI", 9, "bold"))
    style.configure("Hero.TLabel", background=c["card_alt"], foreground=c["text"], font=("Segoe UI", 20, "bold"))
    style.configure("HeroBody.TLabel", background=c["card_alt"], foreground=c["muted"], font=FONT)
    style.configure("CardNumber.TLabel", background=c["card"], foreground=c["accent"], font=("Segoe UI", 10, "bold"))
    style.configure("Ok.TLabel", background=c["card"], foreground=c["ok"], font=("Segoe UI", 13, "bold"))
    style.configure("Bad.TLabel", background=c["card"], foreground=c["bad"], font=("Segoe UI", 13, "bold"))
    style.configure("Warn.TLabel", background=c["card"], foreground=c["warn"], font=FONT_BOLD)
    style.configure("StatusOk.TLabel", foreground=c["ok"])
    style.configure("StatusBad.TLabel", foreground=c["bad"])
    style.configure("TButton", background=c["card_alt"], foreground=c["text"], borderwidth=0,
                    padding=(12, 8), relief="flat")
    style.map("TButton", background=[("active", c["border"]), ("disabled", c["card"])],
              foreground=[("disabled", c["muted"])])
    style.configure("Accent.TButton", background=c["accent"], foreground=c["bg"],
                    font=FONT_BOLD, padding=(18, 10))
    style.map("Accent.TButton", background=[("active", c["accent_hover"])], foreground=[("active", c["bg"])])
    style.configure("Nav.TButton", background=c["panel"], foreground=c["muted"], anchor="w",
                    padding=(18, 11), borderwidth=0)
    style.map("Nav.TButton", background=[("active", c["card"])], foreground=[("active", c["text"])])
    style.configure("NavActive.TButton", background=c["card"], foreground=c["accent"], anchor="w",
                    padding=(18, 11), borderwidth=0, font=FONT_BOLD)
    style.configure("Small.TButton", padding=(9, 5), font=FONT_SMALL)
    style.configure("Remove.TButton", padding=(4, 1), font=FONT_SMALL)
    style.configure("TEntry", fieldbackground=c["entry"], foreground=c["text"], insertcolor=c["text"],
                    padding=6, bordercolor=c["border"])
    style.map("TEntry", bordercolor=[("focus", c["accent"])])
    style.configure("TCombobox", fieldbackground=c["entry"], background=c["card_alt"], foreground=c["text"],
                    arrowcolor=c["text"], padding=6)
    style.map("TCombobox", fieldbackground=[("readonly", c["entry"])],
              foreground=[("readonly", c["text"])], selectbackground=[("readonly", c["entry"])],
              selectforeground=[("readonly", c["text"])])
    root.option_add("*TCombobox*Listbox.background", c["entry"])
    root.option_add("*TCombobox*Listbox.foreground", c["text"])
    root.option_add("*TCombobox*Listbox.selectBackground", c["accent"])
    root.option_add("*TCombobox*Listbox.selectForeground", c["bg"])
    for name in ("TCheckbutton", "Card.TCheckbutton"):
        bg = c["card"] if name.startswith("Card") else c["bg"]
        style.configure(name, background=bg, foreground=c["text"])
        style.map(name, background=[("active", bg)],
                  indicatorcolor=[("selected", c["accent"]), ("!selected", c["entry"])])
    style.configure("Treeview", background=c["card"], fieldbackground=c["card"], foreground=c["text"],
                    rowheight=30, borderwidth=0, font=FONT)
    style.map("Treeview", background=[("selected", c["select"])], foreground=[("selected", c["text"])])
    style.configure("Treeview.Heading", background=c["card_alt"], foreground=c["muted"], font=FONT_BOLD,
                    relief="flat", padding=(8, 7))
    style.map("Treeview.Heading", background=[("active", c["border"])])
    style.configure("Vertical.TScrollbar", background=c["border"], troughcolor=c["panel"],
                    arrowcolor=c["muted"], bordercolor=c["panel"])
    style.configure("Horizontal.TScrollbar", background=c["border"], troughcolor=c["panel"],
                    arrowcolor=c["muted"], bordercolor=c["panel"])
    return style
