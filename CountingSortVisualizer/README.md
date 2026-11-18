# Counting Sort Visualizer (Java, GUI)

A graphical, step-by-step demonstration of **counting sort**. Enter any whole numbers, press
*Visualize*, and watch the algorithm count, accumulate and place every element.

## Run it

Requires **Java 17 or newer**.

```bash
cd src
javac *.java
java CountingSortVisualizer
```

## How to use

1. Type whole numbers (commas, spaces, semicolons or new lines), **load a text file**, pick an example
   chip, or use **Generate...** to create *n* random numbers in a range you choose.
2. Press **Visualize**.
3. Step with the arrow buttons / keys, press **Play**, drag the timeline, or click a stage in the
   progress bar. Speeds: 0.5x - 4x (animated), **Fast** (no animation) and **Turbo** (finishes any
   input in a few seconds).
4. **Results** shows the complete original input and sorted output (copy / save to a file).
5. **Test cases** runs the built-in normal, boundary and invalid tests (see `TEST_CASES.md`).

Keyboard: `Left`/`Right` step, `Space` play/pause, `Home` restart, `End` last step, `T` theme.

## What you see

* **Three lanes** - INPUT, COUNT, OUTPUT. The element being processed travels between them.
* **Progress bar** - Range, Setup, Count, Accumulate, Place.
* **Pseudocode** with the executing line highlighted, and **live variables** (`i`, `x`, `j`, `count[j]`).
* A plain-English **explanation** of every step (the exact numbers involved).
* For arrays too large for the screen, each lane shows a **window that follows the active cell**
  ("240-255 of 500" in the gutter).

## The algorithm in one paragraph

1. Find `min` and `max`; the range is `k = max - min + 1`.
2. Create `count[k]`, all zeros.
3. For every input `x` (left to right): `count[x - min]++`.
4. Prefix sums: `count[j] += count[j-1]` - now `count[j]` is the position just *after* the last slot
   for value `j + min`.
5. For every input `x` from **right to left**: `count[x - min]--`, then `output[count[x - min]] = x`.
   Going right to left keeps equal values in their original order, so the sort is **stable**.

Time `O(n + k)`, extra space `O(n + k)`.

## Implementation restrictions (stated as required)

* **Whole numbers only** (32-bit `int`: -2,147,483,648 .. 2,147,483,647). **Negative numbers are
  supported** (values are offset by `min`). Decimals and text are rejected with a clear message.
* **No hard-coded limit on how many numbers you can enter.** Capacity depends only on available memory.
* Counting sort needs one counter per possible value, so the **value range** `k = max - min + 1`
  must fit in memory. If it does not, the program says so instead of crashing (for example
  `-2147483648 2147483647` has `k = 4,294,967,296`, more than a Java array can hold). Starting Java
  with a larger heap (`java -Xmx8g CountingSortVisualizer`) raises this limit.
* Very large inputs (over 50,000 numbers) are not echoed into the text box; open **Results** to see them.

## Source files (`src/`)

| File | Role |
|------|------|
| `CountingSortVisualizer.java` | `main` - starts the window |
| `CountingSortEngine.java` | The algorithm, steppable forwards **and backwards** in O(1) per step |
| `Phase.java`, `Step.java` | Phase names and the description of one step |
| `InputParser.java` | Parsing, validation, random generation, file loading |
| `SelfTest.java`, `SelfTestDialog.java` | Built-in test cases and their results window |
| `ResultsDialog.java` | Full input / sorted output, copy and save |
| `AppPanel.java`, `MainWindow.java` | Layout, user actions, playback clock |
| `StagePanel.java` | The animated three-lane stage |
| `CodeView.java`, `VarsView.java`, `PhaseStepper.java`, `Scrubber.java` | Pseudocode, variables, progress bar, timeline |
| `Theme.java`, `Gfx.java`, `Glyph.java`, `Widget.java`, `FlatButton.java`, `Segmented.java`, `RoundPanel.java`, `RoundField.java` | Look and feel (light/dark themes, custom controls) |

### Design note: why the engine steps instead of storing snapshots

Remembering the whole state after every step would need `O(steps x n)` memory and would cap the input
size. Instead the engine keeps one set of arrays and a cursor; every step is exactly reversible
(increment/decrement, add/subtract), so moving forward or back costs O(1) per step and memory stays
`O(n + k)`.
