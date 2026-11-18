/** The phases of counting sort, in the order in which they happen. */
enum Phase {
    INTRO("Counting Sort"),
    RANGE("Step 1 - Find the range"),
    INIT("Step 2 - Create the count array"),
    COUNT("Step 3 - Count occurrences"),
    PREFIX("Step 4 - Cumulative sums (prefix sums)"),
    ALLOCATE("Step 5 - Prepare the output array"),
    PLACE("Step 5 - Place elements (right to left)"),
    DONE("Finished");

    final String title;

    Phase(String title) {
        this.title = title;
    }
}
