import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Turns user input (typed text, a file, or generator settings) into an int[].
 *
 * There is deliberately NO limit on how many numbers can be entered. Every
 * problem is reported as an IllegalArgumentException whose message can be
 * shown to the user as-is.
 */
final class InputParser {

    private InputParser() { }

    /**
     * Parses whole numbers separated by commas, semicolons, spaces, tabs or new lines.
     *
     * @throws IllegalArgumentException if the text is empty or contains anything that is not
     *                                  a whole number that fits in 32 bits
     */
    static int[] parse(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Please enter at least one whole number.");
        }
        String[] tokens = text.trim().split("[,;\\s]+");
        int[] values = new int[tokens.length];
        List<String> problems = new ArrayList<>();
        int badCount = 0;
        for (int i = 0; i < tokens.length; i++) {
            try {
                values[i] = Integer.parseInt(tokens[i]);
            } catch (NumberFormatException e) {
                badCount++;
                if (problems.isEmpty()) problems.add(describeBadToken(tokens[i]));
            }
        }
        if (badCount > 0) {
            String more = badCount > 1 ? "  (+" + (badCount - 1) + " more invalid)" : "";
            throw new IllegalArgumentException(problems.get(0) + more);
        }
        return values;
    }

    private static String describeBadToken(String t) {
        String shown = t.length() > 14 ? t.substring(0, 14) + "\u2026" : t;
        if (t.matches("[+-]?\\d+")) {
            return "'" + shown + "' is outside the 32-bit range (-2,147,483,648 to 2,147,483,647)";
        }
        if (t.matches("[+-]?\\d*\\.\\d+")) {
            return "'" + shown + "' has a decimal point; only whole numbers are supported";
        }
        return "'" + shown + "' is not a whole number";
    }

    /** Reads a text file and parses it like typed input. */
    static int[] readFile(File file) {
        try {
            return parse(Files.readString(file.toPath(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read the file: " + e.getMessage());
        } catch (OutOfMemoryError e) {
            throw new IllegalArgumentException("The file is too large for the available memory.");
        }
    }

    /** Generates n random whole numbers between min and max (inclusive). */
    static int[] generate(int n, int min, int max) {
        if (n < 1) throw new IllegalArgumentException("How many numbers must be at least 1.");
        if (min > max) throw new IllegalArgumentException("Minimum must not be larger than maximum.");
        Runtime rt = Runtime.getRuntime();
        long free = rt.maxMemory() - (rt.totalMemory() - rt.freeMemory());
        if ((long) n * 12 > free) {
            throw new IllegalArgumentException("Not enough memory for that many numbers.");
        }
        Random random = new Random();
        long span = (long) max - min + 1;
        int[] values = new int[n];
        for (int i = 0; i < n; i++) values[i] = (int) (min + (long) (random.nextDouble() * span));
        return values;
    }

    /** All numbers, comma separated. */
    static String join(int[] values) {
        StringBuilder sb = new StringBuilder(values.length * 4);
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(values[i]);
        }
        return sb.toString();
    }

    /** At most 'limit' numbers, followed by "..." when there are more. */
    static String preview(int[] values, int limit) {
        StringBuilder sb = new StringBuilder();
        int shown = Math.min(values.length, limit);
        for (int i = 0; i < shown; i++) {
            if (i > 0) sb.append(", ");
            sb.append(values[i]);
        }
        if (values.length > shown) sb.append(", \u2026 (").append(String.format("%,d", values.length)).append(" numbers)");
        return sb.toString();
    }
}
