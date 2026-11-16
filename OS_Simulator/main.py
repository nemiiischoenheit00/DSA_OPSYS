"""OS Simulator - entry point.  Run with:  python main.py"""
import sys
import traceback
import tkinter as tk
from tkinter import messagebox

from gui.dashboard import MainWindow
from gui.styles import apply_theme
from gui.widgets import install_mousewheel


def main():
    if sys.platform == "win32":                    # sharper text on high-DPI Windows screens
        try:
            import ctypes
            ctypes.windll.shcore.SetProcessDpiAwareness(1)
        except Exception:
            pass
    root = tk.Tk()
    root.title("OS Simulator")
    root.geometry("1280x820")
    root.minsize(1000, 650)
    apply_theme(root)
    install_mousewheel(root)

    def report_error(exc_type, exc, tb):           # last safety net: show a dialog instead of dying
        traceback.print_exception(exc_type, exc, tb)
        messagebox.showerror("Unexpected error", f"{exc_type.__name__}: {exc}")

    root.report_callback_exception = report_error
    MainWindow(root)
    root.mainloop()


if __name__ == "__main__":
    main()
