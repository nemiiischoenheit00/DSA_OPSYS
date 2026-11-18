import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

/**
 * Built-in test cases: normal, boundary and invalid inputs.
 *
 * Every accepted input is run through the SAME engine the GUI uses, all the way to the
 * last step, and compared with java.util.Arrays.sort. The engine is then stepped back to
 * the beginning to prove that the "previous step" button restores the exact initial
 * state, and forward again to prove the result is identical.
 */
final class SelfTest {

    enum Kind {
        NORMAL("Normal"), BOUNDARY("Boundary"), INVALID("Invalid");

        final String label;

        Kind(String label) {
            this.label = label;
        }
    }

    /** One test case. inputLabel is what the report shows; input supplies the real text. */
    record Case(Kind kind, String name, String inputLabel, Supplier<String> input, boolean shouldBeAccepted) { }

    record Result(Case testCase, String expected, String actual, boolean passed) { }

    private SelfTest() { }

    // ------------------------------------------------------------------
    // The test cases
    // ------------------------------------------------------------------

    static List<Case> cases() {
        List<Case> list = new ArrayList<>();
        // --- normal
        add(list, Kind.NORMAL, "Classic example", "4, 2, 2, 8, 3, 3, 1", true);
        add(list, Kind.NORMAL, "Duplicate values", "3 3 3 1 1 2 2 3", true);
        add(list, Kind.NORMAL, "Already sorted", "1 2 3 4 5 6", true);
        add(list, Kind.NORMAL, "Reverse order", "9 8 7 6 5 4 3 2 1", true);
        add(list, Kind.NORMAL, "Negative and positive", "5 -3 0 -3 2 -1", true);
        add(list, Kind.NORMAL, "Mixed separators", "7; 3,  5\t1\n4", true);
        // --- boundary
        add(list, Kind.BOUNDARY, "Single element", "42", true);
        add(list, Kind.BOUNDARY, "Two elements, reversed", "2 1", true);
        add(list, Kind.BOUNDARY, "All elements equal (range = 1)", "5 5 5 5 5", true);
        add(list, Kind.BOUNDARY, "All negative", "-5 -9 -1 -7", true);
        add(list, Kind.BOUNDARY, "Zeros only", "0 0 0", true);
        add(list, Kind.BOUNDARY, "Near Integer.MAX_VALUE", "2147483647 2147483645 2147483646", true);
        add(list, Kind.BOUNDARY, "Near Integer.MIN_VALUE", "-2147483648 -2147483646 -2147483647", true);
        add(list, Kind.BOUNDARY, "Wide range (0 .. 100,000)", "0 100000 50000 1", true);
        list.add(new Case(Kind.BOUNDARY, "Large input: 200,000 numbers", "200,000 random numbers in 0..999",
                () -> randomText(200_000, 999), true));
        // --- invalid
        add(list, Kind.INVALID, "Empty input", "", false);
        add(list, Kind.INVALID, "Letters instead of numbers", "3, abc, 5", false);
        add(list, Kind.INVALID, "Decimal number", "1.5 2", false);
        add(list, Kind.INVALID, "Number too large for 32 bits", "99999999999", false);
        add(list, Kind.INVALID, "Lone minus sign", "- 3", false);
        add(list, Kind.INVALID, "Range larger than a Java array", "-2147483648 2147483647", false);
        return list;
    }

    private static void add(List<Case> list, Kind kind, String name, String text, boolean accepted) {
        String label = text.isEmpty() ? "(nothing)" : text.replace("\n", "\\n").replace("\t", "\\t");
        list.add(new Case(kind, name, label, () -> text, accepted));
    }

    private static String randomText(int n, int maxValue) {
        Random random = new Random(12345);       // fixed seed: the report is reproducible
        StringBuilder sb = new StringBuilder(n * 4);
        for (int i = 0; i < n; i++) sb.append(random.nextInt(maxValue + 1)).append(' ');
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // Running them
    // ------------------------------------------------------------------

    static List<Result> runAll() {
        List<Result> results = new ArrayList<>();
        for (Case c : cases()) results.add(run(c));
        return results;
    }

    private static Result run(Case c) {
        try {
            int[] data = InputParser.parse(c.input().get());
            CountingSortEngine engine = new CountingSortEngine(data);     // validates the range / memory
            if (!c.shouldBeAccepted()) {
                return new Result(c, "Rejected with a clear message", "ACCEPTED (unexpected)", false);
            }
            int[] expected = data.clone();
            Arrays.sort(expected);

            int last = engine.totalSteps() - 1;
            engine.goTo(last);
            int[] firstRun = readOutput(engine);

            engine.goTo(0);                                              // step all the way back
            boolean cleanStart = isPristine(engine);
            engine.goTo(last);                                           // and forward again
            int[] secondRun = readOutput(engine);

            boolean ok = Arrays.equals(expected, firstRun)
                    && Arrays.equals(expected, secondRun)
                    && Arrays.equals(expected, CountingSortEngine.sort(data))
                    && cleanStart;
            String actual = InputParser.preview(firstRun, 10) + (cleanStart ? "" : "  [step-back left residue]");
            return new Result(c, InputParser.preview(expected, 10), actual, ok);
        } catch (IllegalArgumentException e) {
            if (c.shouldBeAccepted()) return new Result(c, "Sorted correctly", "Rejected: " + e.getMessage(), false);
            return new Result(c, "Rejected with a clear message", "Rejected: " + e.getMessage(), true);
        }
    }

    private static int[] readOutput(CountingSortEngine engine) {
        int[] out = new int[engine.n()];
        for (int i = 0; i < out.length; i++) out[i] = engine.isPlaced(i) ? engine.outputAt(i) : Integer.MIN_VALUE;
        return out;
    }

    /** After stepping back to step 0 nothing may be counted or placed. */
    private static boolean isPristine(CountingSortEngine engine) {
        for (int v : engine.count()) if (v != 0) return false;
        for (int i = 0; i < engine.n(); i++) if (engine.isPlaced(i)) return false;
        return true;
    }

    // ------------------------------------------------------------------
    // Report
    // ------------------------------------------------------------------

    static String summary(List<Result> results) {
        long passed = results.stream().filter(Result::passed).count();
        return passed + " of " + results.size() + " tests passed";
    }

    /** Markdown table, handy for submitting sample test cases and their outputs. */
    static String toMarkdown(List<Result> results) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Counting Sort - test cases and results\n\n");
        sb.append("**").append(summary(results)).append("**\n\n");
        sb.append("| # | Category | Test case | Input | Expected | Actual | Result |\n");
        sb.append("|---|----------|-----------|-------|----------|--------|--------|\n");
        int i = 1;
        for (Result r : results) {
            sb.append("| ").append(i++)
                    .append(" | ").append(r.testCase().kind().label)
                    .append(" | ").append(cell(r.testCase().name()))
                    .append(" | ").append(cell(r.testCase().inputLabel()))
                    .append(" | ").append(cell(r.expected()))
                    .append(" | ").append(cell(r.actual()))
                    .append(" | ").append(r.passed() ? "PASS" : "FAIL")
                    .append(" |\n");
        }
        return sb.toString();
    }

    private static String cell(String s) {
        return s.replace("|", "\\|");
    }
}
