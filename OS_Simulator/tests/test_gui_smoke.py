"""Smoke test of the GUI *logic* using a fake tkinter (see fake_tk.py).

It builds every page, clicks 'Load example' and 'Run', feeds invalid input, and checks that no
unexpected-error dialog appears. It does NOT verify real rendering - run the app on a PC for that.
"""
import unittest

from tests import fake_tk

fake_tk.install()                                    # must happen before importing gui.*

from gui.dashboard import MODULES, MainWindow        # noqa: E402
from gui import cpu_scheduling_view, disk_scheduling_view  # noqa: E402


def make_window():
    fake_tk.DIALOGS.clear()
    return MainWindow(fake_tk.Tk())


def errors():
    return [d for d in fake_tk.DIALOGS if d[0] == "showerror"]


def warnings():
    return [d for d in fake_tk.DIALOGS if d[0] == "showwarning"]


class TestGuiSmoke(unittest.TestCase):
    def test_every_module_example_runs(self):
        win = make_window()
        for key, _n, name, _s, _c in MODULES:
            win.show(key)
            view = win.views[key]
            view.safe_call(view.load_example)
            view._run_clicked()
            self.assertEqual(errors(), [], f"{name}: unexpected error {errors()}")
            self.assertEqual(warnings(), [], f"{name}: example input rejected {warnings()}")
            self.assertGreater(len(view.results.winfo_children()), 0, f"{name}: no results drawn")

    def test_empty_default_inputs_are_handled(self):
        win = make_window()
        for key, _n, name, _s, _c in MODULES:
            win.show(key)
            view = win.views[key]
            view._reset_clicked()
            view._run_clicked()                       # blank tables must give a message, not a crash
            self.assertEqual(errors(), [], f"{name}: crashed on empty input: {errors()}")

    def test_cpu_all_algorithms_and_bad_values(self):
        win = make_window()
        win.show("cpu")
        v = win.views["cpu"]
        v.safe_call(v.load_example)
        for algo, pre in (("FCFS", False), ("SJF", False), ("SJF", True), ("Priority", False),
                          ("Priority", True), ("Round Robin", False)):
            v.algo.set(algo)
            v.preemptive.set(pre)
            v._run_clicked()
        self.assertEqual((errors(), warnings()), ([], []))
        v.algo.set("Round Robin")
        v.quantum.set("abc")
        v._run_clicked()
        self.assertEqual(len(warnings()), 1)
        v.quantum.set("2")
        v.table.set_rows([["P1", "x", "3", "1"]])
        v._run_clicked()
        self.assertEqual(len(warnings()), 2)
        v.table.set_rows([["P1", "0", "-3", "1"]])
        v._run_clicked()
        self.assertEqual(len(warnings()), 3)
        v.table.set_rows([["P1", "0", "3", "1"], ["P1", "1", "3", "1"]])        # duplicate ID
        v._run_clicked()
        self.assertEqual(len(warnings()), 4)
        v.table.set_rows([["P1", "", "3", "1"]])                                # blank cell
        v._run_clicked()
        self.assertEqual(len(warnings()), 5)
        self.assertEqual(errors(), [])

    def test_cpu_large_dataset_via_random_generator(self):
        win = make_window()
        win.show("cpu")
        v = win.views["cpu"]
        v.table.set_rows(cpu_scheduling_view.random_processes(400))
        for algo in ("FCFS", "SJF", "Priority", "Round Robin"):
            v.algo.set(algo)
            v._run_clicked()
        self.assertEqual((errors(), warnings()), ([], []))

    def test_bankers_dynamic_size_and_validation(self):
        win = make_window()
        win.show("bankers")
        v = win.views["bankers"]
        v.n_var.set("40")
        v.m_var.set("6")
        v.safe_call(v.random_data)
        v._run_clicked()
        self.assertEqual((errors(), warnings()), ([], []))
        v.n_var.set("2")
        v.m_var.set("2")
        v.safe_call(v.generate)
        v.alloc.set_values([["5", "0"], ["0", "0"]])
        v.maxi.set_values([["3", "3"], ["3", "3"]])          # allocation exceeds maximum
        v._run_clicked()
        self.assertEqual(len(warnings()), 1)
        self.assertIn("exceeds", warnings()[0][2])
        v.maxi.set_values([["9", "x"], ["3", "3"]])
        v._run_clicked()
        self.assertEqual(len(warnings()), 2)
        v.n_var.set("0")
        v.safe_call(v.generate)
        self.assertEqual(len(warnings()), 3)

    def test_paging_invalid_values(self):
        win = make_window()
        win.show("paging")
        v = win.views["paging"]
        v.safe_call(v.load_example)
        v.page_size.set("0")
        v._run_clicked()
        v.page_size.set("100")
        v.table.set_rows([["0", "9"]])                       # frame beyond physical memory
        v._run_clicked()
        v.table.set_rows([["0", "1"]])
        v.addresses.set_rows([["abc"]])
        v._run_clicked()
        self.assertEqual(len(warnings()), 3)
        self.assertEqual(errors(), [])

    def test_page_replacement_variants_and_invalid(self):
        win = make_window()
        win.show("replace")
        v = win.views["replace"]
        for algo in ("FIFO", "LRU", "Optimal"):
            v.algo.set(algo)
            v._run_clicked()
        v.rand_len.set("3000")
        v.safe_call(v.generate)
        v._run_clicked()
        self.assertEqual((errors(), warnings()), ([], []))
        v.frames.set("0")
        v._run_clicked()
        v.frames.set("3")
        v.refs.set("1 2 x")
        v._run_clicked()
        self.assertEqual(len(warnings()), 2)

    def test_disk_all_algorithms_and_invalid(self):
        win = make_window()
        win.show("disk")
        v = win.views["disk"]
        v.safe_call(v.load_example)
        for algo in disk_scheduling_view.disk.ALGORITHMS:
            v.algo.set(algo)
            v._run_clicked()
        v.rand_n.set("2000")
        v.high.set("4999")
        v.safe_call(v.generate)
        v._run_clicked()
        self.assertEqual((errors(), warnings()), ([], []))
        v.head.set("99999")
        v._run_clicked()
        v.head.set("")
        v._run_clicked()
        self.assertEqual(len(warnings()), 2)

    def test_fragmentation_modes_and_multiprogramming(self):
        win = make_window()
        win.show("frag")
        v = win.views["frag"]
        v.safe_call(v.load_example)
        for strat in ("First Fit", "Best Fit", "Worst Fit"):
            for mode in ("Fixed partitions", "Variable partitions"):
                v.strategy.set(strat)
                v.mode.set(mode)
                v._run_clicked()
        win.show("multi")
        m = win.views["multi"]
        m.safe_call(m.load_example)
        m.bypass.set(True)
        m._run_clicked()
        m.table.set_rows(m.table.generator(300))
        m.total.set("300")
        m._run_clicked()
        self.assertEqual((errors(), warnings()), ([], []))
        m.total.set("-5")
        m._run_clicked()
        self.assertEqual(len(warnings()), 1)

    def test_segmentation_errors(self):
        win = make_window()
        win.show("segment")
        v = win.views["segment"]
        v.safe_call(v.load_example)
        v._run_clicked()
        v.segments.set_rows([["0", "100", "0"]])             # limit 0 is invalid
        v._run_clicked()
        v.segments.set_rows([["0", "100", "50"], ["0", "200", "50"]])   # duplicate segment
        v._run_clicked()
        self.assertEqual(len(warnings()), 2)
        self.assertEqual(errors(), [])


if __name__ == "__main__":
    unittest.main()
