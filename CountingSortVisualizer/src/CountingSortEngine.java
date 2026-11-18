/**
 * Counting sort that can be watched one step at a time, forwards AND backwards.
 *
 * <h3>How the steps are laid out</h3>
 * With n input items and a value range of k = max - min + 1:
 * <pre>
 *   0             INTRO      (nothing changes)
 *   1             RANGE      find min / max
 *   2             INIT       create count[k] filled with zeros
 *   3 .. n+2      COUNT      count[x - min]++ for every x, left to right
 *   n+3           PREFIX     (explanation only)
 *   n+4 .. n+k+3  PREFIX     count[j] += count[j-1]
 *   n+k+4         ALLOCATE   create the output array
 *   n+k+5 ..      PLACE      right to left: count[x-min]--, output[count[x-min]] = x
 *   last          DONE
 * </pre>
 * The total is 2n + k + 6 steps.
 *
 * <h3>Why not store a snapshot per step?</h3>
 * That would need O(steps x (n + k)) memory, which is hopeless for large
 * inputs. Instead the engine keeps ONE set of arrays plus a cursor, and every
 * step's effect is exactly reversible (increment/decrement, add/subtract), so
 * moving the cursor in either direction costs O(1) per step.
 *
 * <h3>Restrictions</h3>
 * Only 32-bit integers are supported (negatives are fine). There is no fixed
 * limit on the number of items; the only limits are the size of a Java array
 * and the available memory (see {@link #validate(int[])}).
 */
final class CountingSortEngine {

    private final int[] input;
    private final int n;
    private final int k;
    private final int min;
    private final int max;
    private final int total;

    private final int[] count;
    private final int[] output;
    private final boolean[] placed;

    /** Number of steps applied so far; the arrays reflect the state AT this step. */
    private int pos;

    // ------------------------------------------------------------------
    // Validation and construction
    // ------------------------------------------------------------------

    /**
     * Checks whether the data can be visualised.
     *
     * @return an error message, or null when the data is fine
     */
    static String validate(int[] data) {
        if (data == null || data.length == 0) {
            return "Please enter at least one whole number.";
        }
        int lo = data[0], hi = data[0];
        for (int v : data) {
            if (v < lo) lo = v;
            if (v > hi) hi = v;
        }
        long range = (long) hi - lo + 1;
        if (range > Integer.MAX_VALUE - 8) {
            return String.format("Range too large: max - min + 1 = %,d is more than a Java array can hold.", range);
        }
        // count array (4 bytes per slot) + output array and flags (5 bytes per item)
        long needed = 4L * range + 5L * data.length;
        Runtime rt = Runtime.getRuntime();
        long free = rt.maxMemory() - (rt.totalMemory() - rt.freeMemory());
        if (needed > free * 0.8) {
            return String.format("Not enough memory: a range of %,d values needs about %s. "
                    + "Use a smaller range or start Java with a larger -Xmx.", range, humanBytes(needed));
        }
        return null;
    }

    private static String humanBytes(long bytes) {
        if (bytes >= 1L << 30) return String.format("%.1f GB", bytes / (double) (1L << 30));
        return String.format("%.0f MB", bytes / (double) (1L << 20));
    }

    /** @throws IllegalArgumentException with a user-friendly message if the data is not acceptable */
    CountingSortEngine(int[] data) {
        String problem = validate(data);
        if (problem != null) throw new IllegalArgumentException(problem);

        input = data.clone();
        n = input.length;
        int lo = input[0], hi = input[0];
        for (int v : input) {
            if (v < lo) lo = v;
            if (v > hi) hi = v;
        }
        min = lo;
        max = hi;
        k = (int) ((long) hi - lo + 1);
        total = 2 * n + k + 6;

        int[] c;
        int[] o;
        boolean[] p;
        try {
            c = new int[k];
            o = new int[n];
            p = new boolean[n];
        } catch (OutOfMemoryError e) {
            throw new IllegalArgumentException("Not enough memory for this input. "
                    + "Use a smaller range or start Java with a larger -Xmx.");
        }
        count = c;
        output = o;
        placed = p;
    }

    // ------------------------------------------------------------------
    // Read-only access for the UI (callers must not modify the arrays)
    // ------------------------------------------------------------------

    int n() { return n; }
    int k() { return k; }
    int min() { return min; }
    int max() { return max; }
    int totalSteps() { return total; }
    int[] input() { return input; }
    int[] count() { return count; }
    boolean isPlaced(int i) { return placed[i]; }
    int outputAt(int i) { return output[i]; }

    /** The phase that step s belongs to. */
    Phase phaseAt(int s) {
        if (s == 0) return Phase.INTRO;
        if (s == 1) return Phase.RANGE;
        if (s == 2) return Phase.INIT;
        if (s < 3 + n) return Phase.COUNT;
        if (s < 4 + n + k) return Phase.PREFIX;
        if (s == 4 + n + k) return Phase.ALLOCATE;
        if (s < 5 + 2 * n + k) return Phase.PLACE;
        return Phase.DONE;
    }

    /** First step of stage g in the five-stage progress bar (0=Range .. 4=Place). */
    int firstStepOfGroup(int g) {
        return switch (g) {
            case 0 -> 1;
            case 1 -> 2;
            case 2 -> 3;
            case 3 -> 3 + n;
            default -> 4 + n + k;
        };
    }

    // ------------------------------------------------------------------
    // Moving through the algorithm
    // ------------------------------------------------------------------

    /** Moves the cursor to the given step (forwards or backwards) and describes it. */
    Step goTo(int target) {
        target = Math.max(0, Math.min(total - 1, target));
        while (pos < target) {
            pos++;
            applyForward(pos);
        }
        while (pos > target) {
            undo(pos);
            pos--;
        }
        return describe();
    }

    private int bucket(int value) {
        return (int) ((long) value - min);
    }

    private void applyForward(int s) {
        switch (phaseAt(s)) {
            case COUNT -> count[bucket(input[s - 3])]++;
            case PREFIX -> {
                int j = s - 4 - n;
                if (j > 0) count[j] += count[j - 1];
            }
            case PLACE -> {
                int i = n - 1 - (s - 5 - n - k);
                int j = bucket(input[i]);
                count[j]--;
                output[count[j]] = input[i];
                placed[count[j]] = true;
            }
            default -> { }
        }
    }

    /** Exact inverse of {@link #applyForward(int)}. */
    private void undo(int s) {
        switch (phaseAt(s)) {
            case COUNT -> count[bucket(input[s - 3])]--;
            case PREFIX -> {
                int j = s - 4 - n;
                if (j > 0) count[j] -= count[j - 1];
            }
            case PLACE -> {
                int i = n - 1 - (s - 5 - n - k);
                int j = bucket(input[i]);
                placed[count[j]] = false;
                count[j]++;
            }
            default -> { }
        }
    }

    // ------------------------------------------------------------------
    // Step descriptions (derived from the current state)
    // ------------------------------------------------------------------

    private Step describe() {
        int s = pos;
        Phase ph = phaseAt(s);
        int inIdx = -1, ci = -1, oi = -1, before = 0, after = 0, value = 0;
        String text;
        switch (ph) {
            case INTRO -> text = "Counting sort never compares two elements. It counts how often each value "
                    + "occurs, converts those counts into final positions, and drops every element straight "
                    + "into its slot. Time: O(n + k). Supports whole numbers (negatives too); the value range "
                    + "k = max - min + 1 only has to fit in memory.";
            case RANGE -> text = String.format("min = %d, max = %d  ->  k = max - min + 1 = %d. "
                    + "The count array needs %d slots; value v lives at index (v - min).", min, max, k, k);
            case INIT -> text = String.format("Create count[0..%d] filled with zeros. count[j] will store how many "
                    + "times the value (j + %d) appears in the input.", k - 1, min);
            case COUNT -> {
                inIdx = s - 3;
                value = input[inIdx];
                ci = bucket(value);
                after = count[ci];
                before = after - 1;
                text = String.format("input[%d] = %d  ->  index j = %d - (%d) = %d.  count[%d]: %d -> %d.",
                        inIdx, value, value, min, ci, ci, before, after);
            }
            case PREFIX -> {
                if (s == 3 + n) {
                    text = "Now turn counts into cumulative sums: count[j] += count[j-1]. Afterwards count[j] = "
                            + "how many elements are <= (j + " + min + "), which is the position just AFTER the "
                            + "last slot reserved for that value.";
                } else {
                    ci = s - 4 - n;
                    after = count[ci];
                    if (ci == 0) {
                        before = after;
                        text = "count[0] stays " + after + " (nothing before it).";
                    } else {
                        before = after - count[ci - 1];
                        text = String.format("count[%d] = count[%d] + count[%d] = %d + %d = %d.",
                                ci, ci, ci - 1, before, count[ci - 1], after);
                    }
                }
            }
            case ALLOCATE -> text = "Create an empty output array of size " + n + ". We walk the input from RIGHT "
                    + "to LEFT: that keeps equal values in their original order (the sort is STABLE).";
            case PLACE -> {
                inIdx = n - 1 - (s - 5 - n - k);
                value = input[inIdx];
                ci = bucket(value);
                after = count[ci];
                before = after + 1;
                oi = after;
                text = String.format("input[%d] = %d, j = %d. count[%d] was %d -> decrement to %d, so write %d "
                        + "into output[%d].", inIdx, value, ci, ci, before, after, value, oi);
            }
            default -> text = String.format("Sorted! Output: %s. Total work: one pass over the input (%d), one "
                    + "over the range (%d), one more over the input (%d) = O(n + k).",
                    InputParser.preview(output, 16), n, k, n);
        }
        return new Step(s, total, ph, text, inIdx, ci, oi, before, after, value, min, max);
    }

    // ------------------------------------------------------------------
    // Plain (non-animated) counting sort, used for the Results window
    // ------------------------------------------------------------------

    /** Reference implementation: returns a new, sorted copy of the data. */
    static int[] sort(int[] data) {
        int lo = data[0], hi = data[0];
        for (int v : data) {
            if (v < lo) lo = v;
            if (v > hi) hi = v;
        }
        int[] counts = new int[(int) ((long) hi - lo + 1)];
        for (int v : data) counts[(int) ((long) v - lo)]++;          // 1. count
        for (int j = 1; j < counts.length; j++) counts[j] += counts[j - 1];   // 2. prefix sums
        int[] sorted = new int[data.length];
        for (int i = data.length - 1; i >= 0; i--) {                 // 3. place (right to left = stable)
            sorted[--counts[(int) ((long) data[i] - lo)]] = data[i];
        }
        return sorted;
    }
}
