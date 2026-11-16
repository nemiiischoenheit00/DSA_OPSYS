# OS Simulator

A GUI-based Operating Systems simulator written in **Python 3 + Tkinter**. It implements the eight modules
required by the *Operating System Simulator* final-project rubric. Every module runs real algorithms on
**your own input** - there are no hard-coded results and no fixed limit on the number of entries.

## Features

| # | Module | Algorithms / functions |
|---|--------|------------------------|
| 1 | CPU Scheduling | FCFS, SJF (non-preemptive + preemptive/SRTF), Priority (non-preemptive + preemptive), Round Robin; Gantt chart, waiting / turnaround / response time, comparison of all algorithms |
| 2 | Banker's Algorithm | Need matrix, safety algorithm with step-by-step trace, safe sequence, resource-request check, validation (Allocation > Maximum etc.) |
| 3 | Paging | User-defined page table, page/offset split, logical -> physical translation, page-fault and invalid-address handling |
| 4 | Page Replacement | FIFO, LRU, Optimal; frame state after every reference, hit/fault, replaced page + reason, ratios, comparison chart |
| 5 | Segmentation | Segment table (base, limit), (segment, offset) validation, Base + Offset translation, overlap warnings |
| 6 | Fragmentation | First / Best / Worst Fit in **fixed** partitions (internal fragmentation) or **variable** partitions (external fragmentation), memory map, strategy comparison |
| 7 | Disk Scheduling | FCFS, SSTF, SCAN, C-SCAN, LOOK, C-LOOK; direction, cylinder limits, head-movement chart, total movement |
| 8 | Multiprogramming | Memory-sharing simulation: loading, waiting, rejection, round-robin CPU, removal; CPU/memory utilisation, memory map per event |

Other highlights
* **Dynamic input everywhere**: editable tables with *Add row*, *Add several*, *Remove*, *Clear*, *Paste / bulk entry* and *Random data*; matrices are generated for any number of processes / resources.
* **Validation**: blank, non-numeric, negative, duplicate, out-of-range and inconsistent input all produce a clear message box - never a crash.
* **Algorithms are independent of the GUI** (`core/`), so they are unit-tested without any window.
* Dark dashboard UI with sidebar navigation; charts are drawn on Tkinter canvases (no matplotlib needed).

## Technologies and dependencies

* Python 3.9 or newer (developed on 3.12), Windows 10 / 11
* Tkinter / ttk (included with the standard Windows Python installer)
* **No third-party packages are needed to run the app.** `pyinstaller` is only needed to build an `.exe`.

## Installation and running

1. Install Python from python.org (keep **"tcl/tk and IDLE"** ticked, and tick *Add Python to PATH*).
2. Open a terminal in the `OS_Simulator` folder and run:

```
python main.py
```
(or double-click `run.bat`).

## Project structure

```
OS_Simulator/
  main.py                 entry point
  core/                   algorithm logic (no GUI code)
    validation.py         shared parsing + ValidationError
    cpu_scheduling.py  bankers_algorithm.py  paging.py  page_replacement.py
    segmentation.py    fragmentation.py     disk_scheduling.py  multiprogramming.py
  gui/                    Tkinter code
    styles.py widgets.py charts.py base_view.py dashboard.py
    <module>_view.py      one page per module
  tests/                  unit tests + GUI smoke test
  docs/                   SAMPLE_IO.md, TEST_REPORT.md, PRESENTATION_GUIDE.md
```

## Testing

```
python -m unittest discover -s tests -t . -v
```
`tests/test_gui_smoke.py` uses a **fake tkinter** (`tests/fake_tk.py`) so it also runs on machines without a display;
it checks the GUI *logic* only. See `docs/TEST_REPORT.md` for what was actually run and what still must be checked
by hand on a real Windows desktop.

## Building a standalone Windows executable

```
pip install pyinstaller
pyinstaller --noconfirm --onefile --windowed --name OS_Simulator main.py
```
The program is created at `dist\OS_Simulator.exe` (or just double-click `build_exe.bat`). Build on Windows - PyInstaller
creates executables for the operating system it runs on.

## Module descriptions and algorithm notes

### 1. CPU Scheduling
* **FCFS** - run in order of arrival. **SJF** - pick the ready process with the smallest burst (preemptive version = SRTF: re-decide whenever a process arrives). **Priority** - lowest number = highest priority by default (checkbox to reverse). **Round Robin** - each process runs at most one *quantum*; new arrivals join the queue *before* a preempted process is re-queued.
* Turnaround = Completion - Arrival; Waiting = Turnaround - Burst; Response = first CPU time - Arrival.
* Idle CPU time appears as `IDLE`. Ties: earlier arrival, then entry order.

### 2. Banker's Algorithm
Need = Max - Allocation. Safety algorithm: Work = Available; repeatedly choose the lowest-index unfinished process with Need <= Work, add its Allocation to Work. All finished => SAFE (the order is the safe sequence). The *request check* tests Request <= Need, Request <= Available, then pretends to grant it and re-runs the safety algorithm.

### 3. Paging
Physical memory = frames x page size; logical space = pages x page size. page = address // size, offset = address % size, physical = frame x size + offset. A blank frame = page not in memory = page fault. Addresses outside the logical space are *invalid*.

### 4. Page Replacement
One shared simulation loop; algorithms only differ in choosing the victim: FIFO = oldest loaded, LRU = least recently used, Optimal = used farthest in the future (never-used-again pages go first; ties = oldest loaded).

### 5. Segmentation
Valid when the segment exists and 0 <= offset < limit; physical = base + offset. An optional physical-memory size checks that segments fit; overlapping segments give a warning.

### 6. Fragmentation
* **Fixed partitions**: a block holds one process; `block size - process size` is **internal** fragmentation. Empty blocks are just free partitions.
* **Variable partitions**: a block is a hole that is split; leftovers stay free; **no internal** fragmentation.
* **External** fragmentation is reported when a process cannot be placed although *total* usable free memory >= its size (no single piece is big enough).

### 7. Disk Scheduling
SCAN / C-SCAN go to the disk edge, LOOK / C-LOOK only to the last request. C-SCAN / C-LOOK jump back to the other side; the jump is counted as movement by default (checkbox to exclude it). New algorithms can be added by adding one function and one line to `ALGORITHMS` in `core/disk_scheduling.py`.

### 8. Multiprogramming
Jobs arrive over time, are loaded into the first big-enough contiguous hole (otherwise they wait; larger than usable memory = rejected), share one CPU round-robin, and are removed on completion, which frees memory for waiting jobs. Reports CPU utilisation, time-weighted memory utilisation, degree of multiprogramming and throughput.

## Sample input / output
See `docs/SAMPLE_IO.md` (generated from real runs). Every page also has a **Load example** button.

## Assumptions made where the rubric is open
* Disk return jumps counted by default; SCAN always travels to the edge.
* Priority: lower number = higher priority (configurable).
* Page-replacement pages are non-negative integers.
* Multiprogramming uses contiguous first-fit memory and loads at time-slice boundaries.
