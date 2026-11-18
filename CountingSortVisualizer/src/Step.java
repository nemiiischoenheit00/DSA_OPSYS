/**
 * Describes ONE step of the algorithm for the UI.
 *
 * A step never stores the arrays themselves (that would need O(steps x n)
 * memory for large inputs). The arrays live in {@link CountingSortEngine},
 * which is always positioned at exactly this step.
 *
 * @param index       0-based position of this step
 * @param total       total number of steps
 * @param phase       which phase of the algorithm this step belongs to
 * @param text        plain-English explanation of what just happened
 * @param inIdx       index in the input array being processed (-1 = none)
 * @param countIdx    index in the count array being touched (-1 = none)
 * @param outIdx      index in the output array being written (-1 = none)
 * @param countBefore value of count[countIdx] before this step changed it
 * @param countAfter  value of count[countIdx] after this step
 * @param value       the input element being processed (valid if inIdx >= 0)
 * @param min         smallest input value
 * @param max         largest input value
 */
record Step(int index, int total, Phase phase, String text,
            int inIdx, int countIdx, int outIdx,
            int countBefore, int countAfter, int value,
            int min, int max) { }
