# 10-Minute Presentation Guide (follows the rubric's schedule)

Rehearse once with a timer. Every module page has a **Load example** button, so the demo needs almost no typing.

| Time | Activity | What to do / say |
|------|----------|------------------|
| 0:00-1:00 | Introduction | Title, language (Python + Tkinter), purpose: simulate 8 OS concepts with real calculations from user input. Mention: no fixed limit on entries, input is validated. |
| 1:00-3:00 | GUI and structure | Show dashboard + sidebar. Show one input table: **Add row / Paste / Random data**. Explain Input -> Run -> Results -> Reset layout. Mention `core/` (algorithms) vs `gui/` (screens) vs `tests/`. |
| 3:00-7:00 | Live demo (~30 s per module) | See script below. |
| 7:00-9:00 | Algorithm explanation | Pick 2: Round Robin and Banker's (or LRU / SCAN). Explain input -> processing -> output. Show comparison tables. State one challenge (below). |
| 9:00-10:00 | Q&A + conclusion | Tests: 100+ automated tests incl. large datasets; limits; future work. |

## Demo script (4 minutes)
1. **CPU** - Load example -> Round Robin, quantum 2 -> Run: point at Gantt chart, waiting/turnaround, comparison table.
2. **Banker's** - Load example -> Run: Need matrix, SAFE, safe sequence, step trace. Change Available to `0 0 0` -> UNSAFE.
3. **Paging** - Load example -> Run: translation of 117, a page fault (250) and an invalid address (800).
4. **Page replacement** - Load example (3 frames) -> Run FIFO; show the comparison: FIFO 15, LRU 12, Optimal 9.
5. **Segmentation** - Load example -> (1,400) out of range, (2,53) = 4353.
6. **Fragmentation** - Load example -> Fixed + First Fit (internal waste, P4 not placed) -> switch to Variable (external fragmentation message).
7. **Disk** - Load example -> SCAN, show chart; comparison: FCFS 640, SSTF 236, SCAN 331 ...
8. **Multiprogramming** - Load example -> Run, click timeline rows to show memory changing; mention rejected job E.
Finish with a **large dataset**: *Random data...* -> 300 rows -> Run (shows no limit).

## Key explanations (1-2 sentences each)
* **Waiting time** = time spent in the ready queue = Turnaround - Burst. **Turnaround** = Completion - Arrival.
* **Round Robin**: fair, good response time, more context switches; huge quantum behaves like FCFS.
* **Banker's**: *safe* means at least one order exists where every process can finish; unsafe does not mean deadlock yet, only that it is possible.
* **LRU vs FIFO vs Optimal**: Optimal needs the future, so it is only a benchmark; FIFO can show Belady's anomaly (see test).
* **Internal vs external fragmentation**: waste *inside* an allocated fixed block vs. free space split across holes that are individually too small.
* **SCAN vs LOOK**: LOOK does not travel to the disk edge, so it moves less. C-SCAN gives more uniform waiting time.
* **Multiprogramming**: keeping several programs in memory keeps the CPU busy while others wait; limited by memory.

## Challenge to mention
Keeping the algorithms independent from the GUI and handling edge cases correctly (idle CPU time, ties, requests exactly at the disk edge, Round Robin arrival order), which we verified with hand-calculated tests.

## Possible questions and suggested answers
1. **How does your Round Robin handle new arrivals?** A process arriving during a slice is queued before the preempted process goes back to the tail.
2. **Why does SJF have starvation?** Long jobs can wait forever if shorter jobs keep arriving; aging fixes it.
3. **How do you know the Banker's result is right?** Need = Max - Alloc; we replay the sequence with Work and a unit test re-checks the sequence independently; textbook example gives a valid sequence.
4. **Why does Optimal give the fewest faults?** It evicts the page whose next use is farthest away, which can never cause an earlier fault than another choice.
5. **What is Belady's anomaly?** More frames giving more faults (FIFO on 1 2 3 4 1 2 5 1 2 3 4 5: 9 faults with 3 frames, 10 with 4). LRU/Optimal never do this.
6. **How is a logical address translated?** page = addr // size, offset = addr % size, physical = frame x size + offset.
7. **Why are page faults not crashes?** A blank frame means "not in memory"; the OS would load it - we report it.
8. **What is the difference between SCAN and C-SCAN?** SCAN reverses direction at the edge; C-SCAN jumps back and keeps going one way. Jump counted as movement by default (option available).
9. **How do you prevent crashes on bad input?** Every field is parsed with validation functions that raise a `ValidationError`; the page catches it and shows a message box. A last-resort handler catches anything else.
10. **Is there a limit on items?** No hard limit - tables create rows on demand; only memory/speed limit it. Large tests (2000 processes, 5000 page references, 3000 disk requests) are in the unit tests.
11. **Why can a job wait even if total memory is free in multiprogramming?** Memory must be contiguous; free space split into small holes is external fragmentation.
12. **Where would you add a new algorithm?** Disk: one function + one line in `ALGORITHMS`; the GUI list updates automatically.
