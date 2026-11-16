"""A minimal FAKE tkinter used only for smoke-testing the GUI code on machines without Tk/display.

It records widgets in a tree and keeps StringVar values, but draws nothing. It exercises our own
Python logic (parsing, validation, building result tables) - it does NOT test real rendering.
"""
import sys
import types

DIALOGS = []        # every messagebox call is recorded here as (kind, title, message)


class _Var:
    def __init__(self, master=None, value=None, **kw):
        self._v = value if value is not None else self._default

    def get(self):
        return self._v

    def set(self, value):
        self._v = value

    def trace_add(self, *a, **k):
        pass


class StringVar(_Var):
    _default = ""


class BooleanVar(_Var):
    _default = False


class IntVar(_Var):
    _default = 0


class Widget:
    def __init__(self, master=None, *args, **kw):
        self.master = master
        self.kw = dict(kw)
        self.children = []
        self.items = []
        self.text = ""
        if master is not None and hasattr(master, "children"):
            master.children.append(self)

    def __getattr__(self, name):                       # any other Tk method is a harmless no-op
        if name.startswith("__"):
            raise AttributeError(name)
        return lambda *a, **k: None

    def configure(self, **kw):
        self.kw.update(kw)

    config = configure

    def cget(self, key):
        return self.kw.get(key)

    def winfo_children(self):
        return list(self.children)

    def destroy(self):
        for c in list(self.children):
            c.destroy()
        if self.master is not None and self in getattr(self.master, "children", []):
            self.master.children.remove(self)

    def winfo_class(self):
        return type(self).__name__

    def winfo_toplevel(self):
        return self

    def winfo_y(self):
        return 0

    def winfo_height(self):
        return 1

    def yview(self, *a):
        return (0.0, 1.0)

    def bbox(self, *a):
        return (0, 0, 1, 1)

    def create_rectangle(self, *a, **k):
        return 1

    create_text = create_line = create_oval = create_window = create_rectangle

    def insert(self, *args, **kw):                     # Treeview.insert / Text.insert
        self.items.append(args)

    def delete(self, *a):
        self.items = []

    def get_children(self, *a):
        return [str(i) for i in range(len(self.items))]

    def selection(self):
        return ()

    def after(self, *a, **k):
        return None

    def mainloop(self):
        pass

    def title(self, *a):
        pass


class Tk(Widget):
    pass


for _name in ("Frame", "Canvas", "Label", "Entry", "Button", "Text", "Toplevel", "Scrollbar", "Listbox"):
    globals()[_name] = type(_name, (Widget,), {})

_ttk_names = ("Frame", "Label", "Entry", "Button", "Combobox", "Checkbutton", "Scrollbar", "Treeview",
              "Radiobutton", "Spinbox", "Separator")
ttk = types.ModuleType("tkinter.ttk")
for _name in _ttk_names:
    setattr(ttk, _name, type(_name if _name != "Combobox" else "TCombobox", (Widget,), {}))


class _Style(Widget):
    pass


ttk.Style = _Style

messagebox = types.ModuleType("tkinter.messagebox")
for _kind in ("showinfo", "showwarning", "showerror"):
    setattr(messagebox, _kind, (lambda k: lambda title, message, **kw: DIALOGS.append((k, title, message)))(_kind))
messagebox.askyesno = lambda *a, **k: True

simpledialog = types.ModuleType("tkinter.simpledialog")
simpledialog.askinteger = lambda *a, **k: 5


def install():
    """Replace the real tkinter with this fake one (call before importing the gui package)."""
    mod = types.ModuleType("tkinter")
    for name, value in globals().items():
        if not name.startswith("_") and name not in ("install", "sys", "types"):
            setattr(mod, name, value)
    mod.ttk, mod.messagebox, mod.simpledialog = ttk, messagebox, simpledialog
    sys.modules["tkinter"] = mod
    sys.modules["tkinter.ttk"] = ttk
    sys.modules["tkinter.messagebox"] = messagebox
    sys.modules["tkinter.simpledialog"] = simpledialog
    return mod
