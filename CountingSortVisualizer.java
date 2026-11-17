import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.function.IntConsumer;

/**
 * Counting Sort Visualizer
 * ------------------------
 * Shows every phase of counting sort, either as ASCII art in the terminal
 * or in a Swing GUI.
 *
 * Design: CountingSortEngine runs the algorithm ONCE and records an immutable
 * snapshot (Step) after every meaningful action. Both views just render
 * those snapshots, so they can never disagree with each other, and stepping
 * backwards is free.
 *
 * Requires Java 17+.
 *   javac CountingSortVisualizer.java
 *   java CountingSortVisualizer            (menu)
 *   java CountingSortVisualizer gui        (straight to GUI)
 *   java CountingSortVisualizer terminal [--no-color]
 */
public class CountingSortVisualizer {

    // Limits keep the visuals readable (about 100 columns in the terminal).
    static final int MAX_N = 20;
    static final int MAX_RANGE = 25;

    // ------------------------------------------------------------------
    // Model
    // ------------------------------------------------------------------

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
        Phase(String title) { this.title = title; }
    }

    /**
     * Immutable snapshot of the algorithm.
     * inIdx / countIdx / outIdx are the highlighted cells (-1 = none).
     * output cells that are still empty are null.
     */
    record Step(Phase phase, String text, int[] input, int[] count, Integer[] output,
                int inIdx, int countIdx, int outIdx, int min, int max) { }

    // ------------------------------------------------------------------
    // Engine: runs the algorithm and records every step
    // ------------------------------------------------------------------

    static final class CountingSortEngine {

        static String validate(int[] a) {
            if (a == null || a.length == 0) return "Please enter at least one number.";
            if (a.length > MAX_N) return "Please use at most " + MAX_N + " numbers.";
            int min = Arrays.stream(a).min().getAsInt();
            int max = Arrays.stream(a).max().getAsInt();
            if ((long) max - min + 1 > MAX_RANGE) {
                return "Range too large: max - min + 1 must be at most " + MAX_RANGE + ".";
            }
            return null;
        }

        static List<Step> build(int[] a) {
            List<Step> steps = new ArrayList<>();
            int n = a.length;
            int min = Arrays.stream(a).min().getAsInt();
            int max = Arrays.stream(a).max().getAsInt();
            int k = max - min + 1;

            int[] count = new int[0];
            Integer[] out = new Integer[0];

            add(steps, Phase.INTRO,
                "Counting sort never compares two elements. It counts how often each value "
                + "occurs, converts those counts into final positions, and drops every element "
                + "straight into its slot. Time: O(n + k), where k is the size of the value range.",
                a, count, out, -1, -1, -1, min, max);

            add(steps, Phase.RANGE,
                String.format("min = %d, max = %d  ->  k = max - min + 1 = %d. "
                    + "The count array needs %d slots; value v lives at index (v - min).",
                    min, max, k, k),
                a, count, out, -1, -1, -1, min, max);

            count = new int[k];
            add(steps, Phase.INIT,
                "Create count[0.." + (k - 1) + "] filled with zeros. count[j] will store how many "
                + "times the value (j + " + min + ") appears in the input.",
                a, count, out, -1, -1, -1, min, max);

            // Phase 3: count
            for (int i = 0; i < n; i++) {
                int v = a[i];
                int j = v - min;
                int before = count[j];
                count[j]++;
                add(steps, Phase.COUNT,
                    String.format("input[%d] = %d  ->  index j = %d - (%d) = %d.  "
                        + "count[%d]: %d -> %d.", i, v, v, min, j, j, before, count[j]),
                    a, count, out, i, j, -1, min, max);
            }

            // Phase 4: prefix sums
            add(steps, Phase.PREFIX,
                "Now turn counts into cumulative sums: count[j] += count[j-1]. Afterwards count[j] "
                + "= how many elements are <= (j + " + min + "), which is the position just AFTER "
                + "the last slot reserved for that value.",
                a, count, out, -1, -1, -1, min, max);
            for (int j = 0; j < k; j++) {
                if (j == 0) {
                    add(steps, Phase.PREFIX,
                        "count[0] stays " + count[0] + " (nothing before it).",
                        a, count, out, -1, 0, -1, min, max);
                } else {
                    int before = count[j];
                    count[j] += count[j - 1];
                    add(steps, Phase.PREFIX,
                        String.format("count[%d] = count[%d] + count[%d] = %d + %d = %d.",
                            j, j, j - 1, before, count[j - 1], count[j]),
                        a, count, out, -1, j, -1, min, max);
                }
            }

            // Phase 5: place
            out = new Integer[n];
            add(steps, Phase.ALLOCATE,
                "Create an empty output array of size " + n + ". We walk the input from RIGHT to "
                + "LEFT: that keeps equal values in their original order (the sort is STABLE).",
                a, count, out, -1, -1, -1, min, max);

            for (int i = n - 1; i >= 0; i--) {
                int v = a[i];
                int j = v - min;
                int before = count[j];
                count[j]--;
                int pos = count[j];
                out[pos] = v;
                add(steps, Phase.PLACE,
                    String.format("input[%d] = %d, j = %d. count[%d] was %d -> decrement to %d, "
                        + "so write %d into output[%d].", i, v, j, j, before, count[j], v, pos),
                    a, count, out, i, j, pos, min, max);
            }

            add(steps, Phase.DONE,
                "Sorted! The output array holds the input in ascending order. Total work: "
                + "one pass over the input (" + n + "), one over the range (" + k
                + "), one more over the input (" + n + ") = O(n + k).",
                a, count, out, -1, -1, -1, min, max);
            return steps;
        }

        private static void add(List<Step> steps, Phase p, String text, int[] in, int[] c,
                                Integer[] o, int ii, int ci, int oi, int min, int max) {
            steps.add(new Step(p, text, in.clone(), c.clone(), o.clone(), ii, ci, oi, min, max));
        }
    }

    // ------------------------------------------------------------------
    // Terminal view (ASCII)
    // ------------------------------------------------------------------

    static final class TerminalView {
        private static final String ESC = "\u001B[";
        private final boolean color;
        private final Scanner in;

        TerminalView(Scanner in, boolean color) {
            this.in = in;
            this.color = color;
        }

        void run(List<Step> steps) {
            int i = 0;
            while (true) {
                show(steps, i);
                System.out.print("\n[ENTER] next   [b] back   [a] auto-play   [r] restart   [q] quit > ");
                if (!in.hasNextLine()) return;
                String cmd = in.nextLine().trim().toLowerCase();
                switch (cmd) {
                    case "q" -> { return; }
                    case "b" -> i = Math.max(0, i - 1);
                    case "r" -> i = 0;
                    case "a" -> {
                        for (; i < steps.size(); i++) {
                            show(steps, i);
                            sleep(1300);
                        }
                        i = steps.size() - 1;
                    }
                    default -> {
                        if (i == steps.size() - 1) return;
                        i++;
                    }
                }
            }
        }

        private void show(List<Step> steps, int idx) {
            if (color) System.out.print(ESC + "H" + ESC + "2J");
            else System.out.println("\n" + "=".repeat(70));
            System.out.println(render(steps.get(idx), idx + 1, steps.size()));
            System.out.flush();
        }

        private static void sleep(long ms) {
            try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }

        String render(Step s, int number, int total) {
            // Uniform cell width so all three arrays line up the same way.
            int maxLen = 1;
            for (int v : s.input()) maxLen = Math.max(maxLen, String.valueOf(v).length());
            for (int v : s.count()) maxLen = Math.max(maxLen, String.valueOf(v).length());
            for (Integer v : s.output()) if (v != null) maxLen = Math.max(maxLen, v.toString().length());
            maxLen = Math.max(maxLen, String.valueOf(s.min()).length());
            maxLen = Math.max(maxLen, String.valueOf(s.max()).length());
            int w = Math.max(3, maxLen + 2);

            StringBuilder sb = new StringBuilder();
            String bar = "#".repeat(70);
            sb.append(bar).append('\n');
            sb.append(String.format("  COUNTING SORT   |   %s   |   step %d/%d%n", s.phase().title, number, total));
            sb.append(bar).append("\n\n");

            // INPUT
            String[] inCells = new String[s.input().length];
            String[] inIdx = new String[inCells.length];
            for (int i = 0; i < inCells.length; i++) {
                inCells[i] = String.valueOf(s.input()[i]);
                inIdx[i] = String.valueOf(i);
            }
            String inTag = s.phase() == Phase.COUNT ? "reading input[i]"
                    : s.phase() == Phase.PLACE ? "placing input[i]" : "";
            sb.append(row("INPUT  (index i)", inCells, inIdx, s.inIdx(), inTag, w)).append('\n');

            // COUNT
            if (s.count().length == 0) {
                sb.append("COUNT  (not created yet)\n\n");
            } else {
                String[] cCells = new String[s.count().length];
                String[] cIdx = new String[cCells.length];
                for (int j = 0; j < cCells.length; j++) {
                    cCells[j] = String.valueOf(s.count()[j]);
                    cIdx[j] = String.valueOf(j);
                }
                String[] vals = new String[cCells.length];
                for (int j = 0; j < vals.length; j++) vals[j] = String.valueOf(j + s.min());
                String cTag = s.phase() == Phase.COUNT ? "count[v - min]++"
                        : s.phase() == Phase.PREFIX ? "adding previous"
                        : s.phase() == Phase.PLACE ? "count[v - min]--" : "";
                sb.append(row("COUNT  (index j, value = j + min)", cCells, cIdx, s.countIdx(), cTag, w));
                sb.append(plain("  value:", vals, w)).append("\n\n");
            }

            // OUTPUT
            if (s.output().length == 0) {
                sb.append("OUTPUT (not created yet)\n");
            } else {
                String[] oCells = new String[s.output().length];
                String[] oIdx = new String[oCells.length];
                for (int i = 0; i < oCells.length; i++) {
                    oCells[i] = s.output()[i] == null ? "" : s.output()[i].toString();
                    oIdx[i] = String.valueOf(i);
                }
                String oTag = s.phase() == Phase.PLACE ? "written here" : "";
                sb.append(row("OUTPUT (index)", oCells, oIdx, s.outIdx(), oTag, w));
            }

            sb.append("\n").append("-".repeat(70)).append('\n');
            sb.append(wrap(s.text(), 70)).append('\n');
            sb.append("-".repeat(70));
            return sb.toString();
        }

        /** Draws one boxed array: title, borders, values, indices and a pointer line. */
        private String row(String title, String[] cells, String[] idx, int hl, String tag, int w) {
            StringBuilder top = new StringBuilder();
            StringBuilder mid = new StringBuilder();
            StringBuilder ids = new StringBuilder(" ");
            StringBuilder ptr = new StringBuilder(" ");
            for (int i = 0; i < cells.length; i++) {
                boolean h = i == hl;
                top.append('+').append((h ? "=" : "-").repeat(w));
                String cell = center(cells[i], w);
                mid.append('|').append(h ? paint(cell) : cell);
                ids.append(center(idx[i], w)).append(' ');
                ptr.append(center(h ? "^" : "", w)).append(' ');
            }
            top.append('+');
            mid.append('|');
            String pointer = hl >= 0 && !tag.isEmpty() ? ptr.toString().stripTrailing() + "  " + tag : "";

            StringBuilder sb = new StringBuilder();
            sb.append(title).append('\n');
            sb.append(top).append('\n').append(mid).append('\n').append(top).append('\n');
            sb.append(ids.toString().stripTrailing()).append('\n');
            sb.append(pointer.isEmpty() ? "" : pointer + "\n");
            return sb.append('\n').toString();
        }

        private String plain(String label, String[] cells, int w) {
            StringBuilder sb = new StringBuilder(" ");
            for (String c : cells) sb.append(center(c, w)).append(' ');
            return label + "\n" + sb.toString().stripTrailing();
        }

        private String paint(String s) { return color ? ESC + "1;33m" + s + ESC + "0m" : s; }

        private static String center(String s, int w) {
            int left = Math.max(0, (w - s.length()) / 2);
            int right = Math.max(0, w - s.length() - left);
            return " ".repeat(left) + s + " ".repeat(right);
        }

        private static String wrap(String text, int width) {
            StringBuilder out = new StringBuilder();
            int col = 0;
            for (String word : text.split(" ")) {
                if (col + word.length() > width) { out.append('\n'); col = 0; }
                out.append(word).append(' ');
                col += word.length() + 1;
            }
            return out.toString().stripTrailing();
        }
    }

    // ------------------------------------------------------------------
    // GUI view (Swing) - custom painted, themeable and animated
    // ------------------------------------------------------------------
    //
    // UX principles behind this redesign:
    //  1. One thing in focus: the stage shows INPUT -> COUNT -> OUTPUT as three
    //     lanes, and the current element literally travels between them.
    //  2. Always answer "where am I?": phase stepper, pseudocode with the active
    //     line highlighted, live variables, and a scrubbable timeline.
    //  3. No modal dialogs: validation is inline, presets are one click.
    //  4. Colour is never the only signal (carets, dashed borders, text labels),
    //     text colours are chosen by contrast ratio, and every control is
    //     keyboard reachable with a visible focus ring.
    //  5. Light and dark themes, remembered between runs.

    /** Design tokens. Every colour in the GUI comes from here. */
    static final class Theme {
        final boolean dark;
        final Color bg, surface, surface2, border, text, muted, accent, accentSoft,
                input, count, out, danger, cell, cellBorder;

        Theme(boolean dark, int bg, int surface, int surface2, int border, int text, int muted,
              int accent, int accentSoft, int input, int count, int out, int danger,
              int cell, int cellBorder) {
            this.dark = dark;
            this.bg = new Color(bg);
            this.surface = new Color(surface);
            this.surface2 = new Color(surface2);
            this.border = new Color(border);
            this.text = new Color(text);
            this.muted = new Color(muted);
            this.accent = new Color(accent);
            this.accentSoft = new Color(accentSoft);
            this.input = new Color(input);
            this.count = new Color(count);
            this.out = new Color(out);
            this.danger = new Color(danger);
            this.cell = new Color(cell);
            this.cellBorder = new Color(cellBorder);
        }

        static final Theme LIGHT = new Theme(false, 0xF3F5FA, 0xFFFFFF, 0xEDF0F7, 0xD8DEEA,
                0x1B2333, 0x5A6678, 0x2F5BEA, 0xE2EAFE, 0xF2A20E, 0x7C4DFF, 0x0E9F6E, 0xD63A3A,
                0xFFFFFF, 0xBCC5D6);
        static final Theme DARK = new Theme(true, 0x0E1320, 0x171E2E, 0x1F2839, 0x2B3550,
                0xE7EBF3, 0x9AA7BC, 0x6C92FF, 0x1F2F5C, 0xF7B84B, 0xA78BFF, 0x3DD598, 0xFF6B6B,
                0x1D2638, 0x3B4765);
        static Theme cur = LIGHT;
    }

    /** Small drawing helpers shared by all custom components. */
    static final class Gfx {
        private static final String UI = pick("Inter", "Segoe UI", "SF Pro Text", "Helvetica Neue",
                "Roboto", "Noto Sans", "DejaVu Sans", Font.SANS_SERIF);
        private static final String MONO = pick("JetBrains Mono", "Cascadia Mono", "Consolas", "Menlo",
                "DejaVu Sans Mono", Font.MONOSPACED);

        private static String pick(String... names) {
            Set<String> have = new HashSet<>(Arrays.asList(
                    GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
            for (String n : names) if (have.contains(n)) return n;
            return names[names.length - 1];
        }

        static Font ui(int style, float size) { return new Font(UI, style, 1).deriveFont(size); }
        static Font mono(int style, float size) { return new Font(MONO, style, 1).deriveFont(size); }

        static Graphics2D aa(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            return g2;
        }

        static Color alpha(Color c, int a) { return new Color(c.getRed(), c.getGreen(), c.getBlue(), a); }

        static Color mix(Color a, Color b, double t) {
            t = clamp(t, 0, 1);
            return new Color((int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                    (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                    (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
        }

        static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }

        static double ease(double t) {
            t = clamp(t, 0, 1);
            return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
        }

        private static double lin(double v) { return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4); }

        static double lum(Color c) {
            return 0.2126 * lin(c.getRed() / 255.0) + 0.7152 * lin(c.getGreen() / 255.0)
                    + 0.0722 * lin(c.getBlue() / 255.0);
        }

        /** Dark or white text, whichever has the better WCAG contrast on the given background. */
        static Color onColor(Color bg) {
            Color dark = new Color(0x111827);
            double l = lum(bg);
            double cw = 1.05 / (l + 0.05);
            double cd = (l + 0.05) / (lum(dark) + 0.05);
            return cw >= cd ? Color.WHITE : dark;
        }

        static void centerText(Graphics2D g, String s, double cx, double cy) {
            FontMetrics fm = g.getFontMetrics();
            g.drawString(s, (float) (cx - fm.stringWidth(s) / 2.0),
                    (float) (cy + (fm.getAscent() - fm.getDescent()) / 2.0));
        }
    }

    /** Vector icons, so we never depend on emoji or symbol fonts. */
    enum Glyph { PLAY, PAUSE, NEXT, PREV, RESTART, SUN, MOON, CHECK }

    static void drawGlyph(Graphics2D g, Glyph ic, double cx, double cy, double s, Color c) {
        g.setColor(c);
        g.setStroke(new BasicStroke((float) (s * 0.13), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        double h = s / 2;
        switch (ic) {
            case PLAY -> {
                Path2D p = new Path2D.Double();
                p.moveTo(cx - h * 0.5, cy - h * 0.9);
                p.lineTo(cx + h * 0.85, cy);
                p.lineTo(cx - h * 0.5, cy + h * 0.9);
                p.closePath();
                g.fill(p);
            }
            case PAUSE -> {
                g.fill(new RoundRectangle2D.Double(cx - h * 0.65, cy - h * 0.85, h * 0.5, h * 1.7, 3, 3));
                g.fill(new RoundRectangle2D.Double(cx + h * 0.15, cy - h * 0.85, h * 0.5, h * 1.7, 3, 3));
            }
            case NEXT -> {
                Path2D p = new Path2D.Double();
                p.moveTo(cx - h * 0.35, cy - h * 0.8);
                p.lineTo(cx + h * 0.45, cy);
                p.lineTo(cx - h * 0.35, cy + h * 0.8);
                g.draw(p);
            }
            case PREV -> {
                Path2D p = new Path2D.Double();
                p.moveTo(cx + h * 0.35, cy - h * 0.8);
                p.lineTo(cx - h * 0.45, cy);
                p.lineTo(cx + h * 0.35, cy + h * 0.8);
                g.draw(p);
            }
            case RESTART -> {
                g.fill(new RoundRectangle2D.Double(cx - h * 0.85, cy - h * 0.8, h * 0.28, h * 1.6, 2, 2));
                Path2D p = new Path2D.Double();
                p.moveTo(cx + h * 0.85, cy - h * 0.8);
                p.lineTo(cx - h * 0.35, cy);
                p.lineTo(cx + h * 0.85, cy + h * 0.8);
                p.closePath();
                g.fill(p);
            }
            case SUN -> {
                g.fill(new Ellipse2D.Double(cx - h * 0.4, cy - h * 0.4, h * 0.8, h * 0.8));
                for (int i = 0; i < 8; i++) {
                    double a = i * Math.PI / 4;
                    g.draw(new Line2D.Double(cx + Math.cos(a) * h * 0.68, cy + Math.sin(a) * h * 0.68,
                            cx + Math.cos(a) * h * 0.98, cy + Math.sin(a) * h * 0.98));
                }
            }
            case MOON -> {
                Area a = new Area(new Ellipse2D.Double(cx - h * 0.8, cy - h * 0.8, h * 1.6, h * 1.6));
                a.subtract(new Area(new Ellipse2D.Double(cx - h * 0.15, cy - h * 1.0, h * 1.5, h * 1.5)));
                g.fill(a);
            }
            case CHECK -> {
                Path2D p = new Path2D.Double();
                p.moveTo(cx - h * 0.7, cy + h * 0.05);
                p.lineTo(cx - h * 0.2, cy + h * 0.55);
                p.lineTo(cx + h * 0.75, cy - h * 0.5);
                g.draw(p);
            }
        }
    }

    /** Base for custom-painted widgets: plain JComponent has no accessible context, so provide one. */
    static class Widget extends JComponent {
        private final AccessibleRole role;
        private AccessibleContext ac;

        Widget() { this(AccessibleRole.PANEL); }
        Widget(AccessibleRole role) { this.role = role; }

        @Override
        public AccessibleContext getAccessibleContext() {
            if (ac == null) {
                ac = new AccessibleJComponent() {
                    @Override public AccessibleRole getAccessibleRole() { return role; }
                };
            }
            return ac;
        }
    }

    /** Rounded surface used for cards. */
    static final class RoundPanel extends JPanel {
        private final int radius;

        RoundPanel(LayoutManager lm, int radius) {
            super(lm);
            this.radius = radius;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = Gfx.aa(g0);
            Theme t = Theme.cur;
            g.setColor(t.surface);
            g.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), radius * 2, radius * 2));
            g.setColor(t.border);
            g.setStroke(new BasicStroke(1f));
            g.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1, getHeight() - 1, radius * 2, radius * 2));
            g.dispose();
        }
    }

    /** One button class for the whole app: primary / secondary / ghost / chip. */
    static final class FlatButton extends Widget {
        enum Style { PRIMARY, SECONDARY, GHOST, CHIP }

        private final Style style;
        private final Runnable action;
        private String text;
        private Glyph glyph;
        private boolean hover, pressed, selected, clickFocus;
        private int fixedWidth = -1;

        FlatButton(String text, Glyph glyph, Style style, String tooltip, Runnable action) {
            super(AccessibleRole.PUSH_BUTTON);
            this.text = text;
            this.glyph = glyph;
            this.style = style;
            this.action = action;
            setFocusable(true);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            if (tooltip != null) {
                setToolTipText(tooltip);
                getAccessibleContext().setAccessibleName(tooltip);
            } else if (text != null) {
                getAccessibleContext().setAccessibleName(text);
            }
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; pressed = false; repaint(); }
                @Override public void mousePressed(MouseEvent e) {
                    if (!isEnabled()) return;
                    pressed = true;
                    clickFocus = true;
                    requestFocusInWindow();
                    repaint();
                }
                @Override public void mouseReleased(MouseEvent e) {
                    boolean fire = pressed && hover && isEnabled();
                    pressed = false;
                    repaint();
                    if (fire) FlatButton.this.action.run();
                }
            });
            addKeyListener(new KeyAdapter() {
                @Override public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_SPACE || e.getKeyCode() == KeyEvent.VK_ENTER) {
                        if (isEnabled()) FlatButton.this.action.run();
                        e.consume();
                    }
                }
            });
            addFocusListener(new FocusAdapter() {
                @Override public void focusLost(FocusEvent e) { clickFocus = false; repaint(); }
                @Override public void focusGained(FocusEvent e) { repaint(); }
            });
        }

        void setText(String t) { text = t; repaint(); }
        void setGlyph(Glyph gl) { glyph = gl; repaint(); }
        void setSelected(boolean s) { selected = s; repaint(); }
        FlatButton fixedWidth(int w) { fixedWidth = w; return this; }

        @Override public void setEnabled(boolean b) {
            super.setEnabled(b);
            setCursor(Cursor.getPredefinedCursor(b ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            repaint();
        }

        @Override public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(Gfx.ui(Font.BOLD, 14f));
            int h = style == Style.CHIP ? 30 : 38;
            int w = 0;
            if (glyph != null) w += 16;
            if (text != null) w += (glyph != null ? 8 : 0) + fm.stringWidth(text);
            w += text == null ? 22 : style == Style.CHIP ? 28 : 32;
            if (fixedWidth > 0) w = fixedWidth;
            return new Dimension(w, h);
        }

        @Override public Dimension getMaximumSize() { return getPreferredSize(); }
        @Override public Dimension getMinimumSize() { return getPreferredSize(); }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = Gfx.aa(g0);
            Theme t = Theme.cur;
            int w = getWidth(), h = getHeight();
            Color bg = null, fg, bd = null;
            switch (style) {
                case PRIMARY -> {
                    bg = pressed ? Gfx.mix(t.accent, Color.BLACK, 0.18)
                            : hover ? Gfx.mix(t.accent, Color.WHITE, 0.14) : t.accent;
                    fg = Gfx.onColor(t.accent);
                }
                case SECONDARY -> {
                    bg = pressed || hover ? Gfx.mix(t.surface2, t.text, 0.08) : t.surface2;
                    bd = t.border;
                    fg = t.text;
                }
                case GHOST -> {
                    if (pressed || hover) bg = t.surface2;
                    fg = t.text;
                }
                default -> {
                    if (selected) { bg = t.accentSoft; bd = t.accent; fg = t.accent; }
                    else { if (hover || pressed) bg = t.surface2; bd = t.border; fg = t.text; }
                }
            }
            if (!isEnabled()) fg = Gfx.mix(fg, t.surface, 0.6);
            double arc = style == Style.CHIP ? h : 12;
            RoundRectangle2D shape = new RoundRectangle2D.Double(0.5, 0.5, w - 1, h - 1, arc, arc);
            if (bg != null) {
                g.setColor(isEnabled() || style != Style.PRIMARY ? bg : Gfx.mix(bg, t.surface, 0.55));
                g.fill(shape);
            }
            if (bd != null) { g.setColor(bd); g.setStroke(new BasicStroke(1f)); g.draw(shape); }

            // content, centred
            g.setFont(Gfx.ui(Font.BOLD, 14f));
            FontMetrics fm = g.getFontMetrics();
            int tw = text == null ? 0 : fm.stringWidth(text);
            int gw = glyph == null ? 0 : 16;
            int gap = (text != null && glyph != null) ? 8 : 0;
            double x = (w - (gw + gap + tw)) / 2.0;
            if (glyph != null) {
                drawGlyph(g, glyph, x + gw / 2.0, h / 2.0, 15, fg);
                x += gw + gap;
            }
            if (text != null) {
                g.setColor(fg);
                g.setFont(Gfx.ui(Font.BOLD, 14f));
                g.drawString(text, (float) x, (float) (h / 2.0 + (fm.getAscent() - fm.getDescent()) / 2.0));
            }
            // keyboard focus ring (hidden for mouse clicks)
            if (hasFocus() && !clickFocus) {
                g.setColor(Gfx.alpha(t.accent, 200));
                g.setStroke(new BasicStroke(2f));
                g.draw(new RoundRectangle2D.Double(-1 + 1.5, -1 + 1.5, w - 3, h - 3, arc, arc));
            }
            g.dispose();
        }
    }

    /** Segmented control (used for playback speed). */
    static final class Segmented extends Widget {
        private final String[] labels;
        private final IntConsumer onChange;
        private int sel;

        Segmented(String[] labels, int sel, String tooltip, IntConsumer onChange) {
            this.labels = labels;
            this.sel = sel;
            this.onChange = onChange;
            setToolTipText(tooltip);
            getAccessibleContext().setAccessibleName(tooltip);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    double segW = (getWidth() - 8) / (double) Segmented.this.labels.length;
                    int i = (int) Gfx.clamp((e.getX() - 4) / segW, 0, Segmented.this.labels.length - 1);
                    setSelected(i);
                    Segmented.this.onChange.accept(i);
                }
            });
        }

        void setSelected(int i) { sel = i; repaint(); }

        @Override public Dimension getPreferredSize() { return new Dimension(labels.length * 46 + 8, 34); }
        @Override public Dimension getMaximumSize() { return getPreferredSize(); }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = Gfx.aa(g0);
            Theme t = Theme.cur;
            int w = getWidth(), h = getHeight();
            g.setColor(t.surface2);
            g.fill(new RoundRectangle2D.Double(0, 0, w, h, h, h));
            g.setColor(t.border);
            g.draw(new RoundRectangle2D.Double(0.5, 0.5, w - 1, h - 1, h, h));
            double segW = (w - 8) / (double) labels.length;
            g.setFont(Gfx.ui(Font.BOLD, 13f));
            for (int i = 0; i < labels.length; i++) {
                double x = 4 + i * segW;
                if (i == sel) {
                    g.setColor(t.accent);
                    g.fill(new RoundRectangle2D.Double(x, 4, segW, h - 8, h - 8, h - 8));
                    g.setColor(Gfx.onColor(t.accent));
                } else {
                    g.setColor(t.muted);
                }
                Gfx.centerText(g, labels[i], x + segW / 2, h / 2.0);
            }
            g.dispose();
        }
    }

    /** Timeline with one tick per step; click or drag to jump anywhere. */
    static final class Scrubber extends Widget {
        private int max = 1, value;
        private List<Step> steps = List.of();
        private IntConsumer onChange = v -> { };

        Scrubber() {
            super(AccessibleRole.SLIDER);
            setPreferredSize(new Dimension(200, 34));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("Timeline");
            getAccessibleContext().setAccessibleName("Step timeline");
            MouseAdapter m = new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) { fromX(e.getX()); }
                @Override public void mouseDragged(MouseEvent e) { fromX(e.getX()); }
                @Override public void mouseMoved(MouseEvent e) {
                    int i = indexAt(e.getX());
                    if (i >= 0 && i < steps.size()) {
                        setToolTipText("Step " + (i + 1) + ": " + steps.get(i).phase().title);
                    }
                }
            };
            addMouseListener(m);
            addMouseMotionListener(m);
        }

        void configure(List<Step> s, IntConsumer cb) { steps = s; max = Math.max(1, s.size() - 1); onChange = cb; repaint(); }
        void setValue(int v) { value = v; repaint(); }

        private int indexAt(int x) {
            double x0 = 14, x1 = getWidth() - 14;
            return (int) Math.round(Gfx.clamp((x - x0) / (x1 - x0), 0, 1) * max);
        }

        private void fromX(int x) { onChange.accept(indexAt(x)); }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = Gfx.aa(g0);
            Theme t = Theme.cur;
            double x0 = 14, x1 = getWidth() - 14, cy = getHeight() / 2.0;
            g.setColor(t.border);
            g.fill(new RoundRectangle2D.Double(x0, cy - 3, x1 - x0, 6, 6, 6));
            double xv = x0 + (x1 - x0) * value / max;
            g.setColor(t.accent);
            g.fill(new RoundRectangle2D.Double(x0, cy - 3, xv - x0, 6, 6, 6));
            if ((x1 - x0) / max >= 8) {
                for (int i = 0; i <= max; i++) {
                    double x = x0 + (x1 - x0) * i / max;
                    g.setColor(i <= value ? Gfx.mix(t.accent, Color.WHITE, 0.55) : Gfx.mix(t.border, t.muted, 0.5));
                    g.fill(new Ellipse2D.Double(x - 1.5, cy - 1.5, 3, 3));
                }
            }
            g.setColor(new Color(0, 0, 0, t.dark ? 90 : 45));
            g.fill(new Ellipse2D.Double(xv - 9, cy - 9 + 2, 18, 18));
            g.setColor(t.surface);
            g.fill(new Ellipse2D.Double(xv - 9, cy - 9, 18, 18));
            g.setColor(t.accent);
            g.setStroke(new BasicStroke(3f));
            g.draw(new Ellipse2D.Double(xv - 7.5, cy - 7.5, 15, 15));
            g.dispose();
        }
    }

    /** Five-stage progress indicator; click a stage to jump to it. */
    static final class PhaseStepper extends Widget {
        private static final String[] LABELS = {"Range", "Setup", "Count", "Accumulate", "Place"};
        private int active = -1;
        private IntConsumer onPick = i -> { };

        PhaseStepper() {
            setPreferredSize(new Dimension(300, 66));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            getAccessibleContext().setAccessibleName("Algorithm phases");
            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    double seg = (getWidth() - 40) / 5.0;
                    onPick.accept((int) Gfx.clamp((e.getX() - 20) / seg, 0, 4));
                }
            });
        }

        void setActive(int a) { active = a; repaint(); }
        void onPick(IntConsumer c) { onPick = c; }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = Gfx.aa(g0);
            Theme t = Theme.cur;
            int n = LABELS.length;
            double padX = 20, seg = (getWidth() - 2 * padX) / n, cy = 22, r = 12;
            for (int i = 0; i < n - 1; i++) {
                double xa = padX + seg * (i + 0.5) + r + 4, xb = padX + seg * (i + 1.5) - r - 4;
                g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(i < active ? t.accent : t.border);
                g.draw(new Line2D.Double(xa, cy, xb, cy));
            }
            for (int i = 0; i < n; i++) {
                double cx = padX + seg * (i + 0.5);
                boolean done = i < active, now = i == active;
                Ellipse2D c = new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r);
                if (now) {
                    g.setColor(Gfx.alpha(t.accent, 40));
                    g.fill(new Ellipse2D.Double(cx - r - 4, cy - r - 4, 2 * r + 8, 2 * r + 8));
                }
                g.setColor(done ? t.accent : now ? t.surface : t.surface2);
                g.fill(c);
                g.setStroke(new BasicStroke(now ? 2.5f : 1.5f));
                g.setColor(done || now ? t.accent : t.border);
                g.draw(c);
                if (done) {
                    drawGlyph(g, Glyph.CHECK, cx, cy, 14, Gfx.onColor(t.accent));
                } else {
                    g.setFont(Gfx.ui(Font.BOLD, 13f));
                    g.setColor(now ? t.accent : t.muted);
                    Gfx.centerText(g, String.valueOf(i + 1), cx, cy);
                }
                g.setFont(Gfx.ui(now ? Font.BOLD : Font.PLAIN, 12.5f));
                g.setColor(now ? t.text : t.muted);
                Gfx.centerText(g, LABELS[i], cx, cy + 28);
            }
            g.dispose();
        }
    }

    /** Pseudocode with the currently executing lines highlighted. */
    static final class CodeView extends Widget {
        private static final String[] CODE = {
            "k \u2190 max \u2212 min + 1",
            "count \u2190 new array[k] of 0",
            "for x in input:",
            "    count[x \u2212 min] += 1",
            "for j \u2190 1 \u2026 k\u22121:",
            "    count[j] += count[j\u22121]",
            "output \u2190 new array[n]",
            "for x in input (right \u2192 left):",
            "    count[x \u2212 min] \u2212= 1",
            "    output[count[x \u2212 min]] \u2190 x",
            "return output"
        };
        private static final Set<String> KEYWORDS = Set.of("for", "in", "return", "new", "of");
        private Set<Integer> main = Set.of(), ctx = Set.of();

        CodeView() {
            setPreferredSize(new Dimension(320, 380));
            getAccessibleContext().setAccessibleName("Algorithm pseudocode");
        }

        void setStep(Step s) {
            switch (s.phase()) {
                case RANGE -> { main = Set.of(1); ctx = Set.of(); }
                case INIT -> { main = Set.of(2); ctx = Set.of(); }
                case COUNT -> { main = Set.of(4); ctx = Set.of(3); }
                case PREFIX -> { main = s.countIdx() > 0 ? Set.of(6) : Set.of(); ctx = Set.of(5); }
                case ALLOCATE -> { main = Set.of(7); ctx = Set.of(); }
                case PLACE -> { main = Set.of(9, 10); ctx = Set.of(8); }
                case DONE -> { main = Set.of(11); ctx = Set.of(); }
                default -> { main = Set.of(); ctx = Set.of(); }
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = Gfx.aa(g0);
            Theme t = Theme.cur;
            int w = getWidth(), h = getHeight();
            g.setColor(t.surface);
            g.fill(new RoundRectangle2D.Double(0, 0, w, h, 28, 28));
            g.setColor(t.border);
            g.draw(new RoundRectangle2D.Double(0.5, 0.5, w - 1, h - 1, 28, 28));

            g.setFont(Gfx.ui(Font.BOLD, 11.5f));
            g.setColor(t.muted);
            g.drawString("ALGORITHM", 18, 28);

            int top = 42, footer = 40;
            boolean showFooter = h - top - footer >= CODE.length * 20;
            if (!showFooter) footer = 8;
            double rowH = Gfx.clamp((h - top - footer) / (double) CODE.length, 17, 32);
            g.setFont(Gfx.mono(Font.PLAIN, 13f));
            FontMetrics fm = g.getFontMetrics();
            for (int i = 0; i < CODE.length; i++) {
                int ln = i + 1;
                double y = top + i * rowH;
                boolean isMain = main.contains(ln), isCtx = ctx.contains(ln);
                if (isMain || isCtx) {
                    g.setColor(isMain ? t.accentSoft : Gfx.alpha(t.accentSoft, 110));
                    g.fill(new RoundRectangle2D.Double(8, y, w - 16, rowH - 2, 10, 10));
                }
                if (isMain) {
                    g.setColor(t.accent);
                    g.fill(new RoundRectangle2D.Double(8, y + 4, 3.5, rowH - 10, 3, 3));
                }
                double base = y + (rowH - 2) / 2.0 + (fm.getAscent() - fm.getDescent()) / 2.0;
                g.setColor(isMain ? t.accent : t.muted);
                g.drawString(String.format("%2d", ln), 20, (float) base);
                // tiny tokenizer: keywords get the accent colour
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\s+|[A-Za-z]+|.").matcher(CODE[i]);
                double x = 50;
                while (m.find()) {
                    String tok = m.group();
                    boolean kw = KEYWORDS.contains(tok);
                    g.setColor(kw ? t.accent : isMain || isCtx ? t.text : Gfx.mix(t.text, t.muted, 0.45));
                    g.setFont(Gfx.mono(kw ? Font.BOLD : Font.PLAIN, 13f));
                    g.drawString(tok, (float) x, (float) base);
                    x += g.getFontMetrics().stringWidth(tok);
                }
                g.setFont(Gfx.mono(Font.PLAIN, 13f));
            }
            if (showFooter) {
                g.setFont(Gfx.ui(Font.PLAIN, 12f));
                g.setColor(t.muted);
                g.drawString("Time O(n + k)   \u00B7   Space O(n + k)   \u00B7   Stable", 18, h - 16);
            }
            g.dispose();
        }
    }

    /** Live values of the variables used by the highlighted pseudocode. */
    static final class VarsView extends Widget {
        private Step s;

        VarsView() {
            setPreferredSize(new Dimension(320, 162));
            getAccessibleContext().setAccessibleName("Live variables");
        }

        void setStep(Step st) { s = st; repaint(); }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = Gfx.aa(g0);
            Theme t = Theme.cur;
            int w = getWidth(), h = getHeight();
            g.setColor(t.surface);
            g.fill(new RoundRectangle2D.Double(0, 0, w, h, 28, 28));
            g.setColor(t.border);
            g.draw(new RoundRectangle2D.Double(0.5, 0.5, w - 1, h - 1, 28, 28));
            g.setFont(Gfx.ui(Font.BOLD, 11.5f));
            g.setColor(t.muted);
            g.drawString("LIVE VARIABLES", 18, 28);
            if (s != null) {
                boolean started = s.phase() != Phase.INTRO;
                String dash = "\u2013";
                String[][] items = {
                    {"min .. max", started ? s.min() + " .. " + s.max() : dash},
                    {"k", started ? String.valueOf(s.max() - s.min() + 1) : dash},
                    {"i", s.inIdx() >= 0 ? String.valueOf(s.inIdx()) : dash},
                    {"x = input[i]", s.inIdx() >= 0 ? String.valueOf(s.input()[s.inIdx()]) : dash},
                    {"j = x \u2212 min", s.countIdx() >= 0 ? String.valueOf(s.countIdx()) : dash},
                    {"count[j]", s.countIdx() >= 0 && s.countIdx() < s.count().length
                            ? String.valueOf(s.count()[s.countIdx()]) : dash}
                };
                double colW = (w - 36) / 2.0;
                for (int i = 0; i < items.length; i++) {
                    double x = 18 + (i % 2) * colW, y = 42 + (i / 2) * 38;
                    g.setFont(Gfx.ui(Font.PLAIN, 11.5f));
                    g.setColor(t.muted);
                    g.drawString(items[i][0], (float) x, (float) (y + 8));
                    g.setFont(Gfx.mono(Font.BOLD, 15f));
                    g.setColor(items[i][1].equals(dash) ? t.muted : t.text);
                    g.drawString(items[i][1], (float) x, (float) (y + 26));
                }
            }
            g.dispose();
        }
    }

    /** The main stage: three lanes (input / count / output) with animated transitions. */
    static final class StagePanel extends Widget {
        private enum CellState { NORMAL, ACTIVE, SOLID, DONE, TARGET, EMPTY }

        /** Moment (0..1 of a step's animation) when the count cell changes. */
        static double changeAt(Phase p) { return p == Phase.PLACE ? 0.35 : 0.7; }
        /** Moment when a placed element lands in the output array. */
        static final double LAND = 0.8;

        private Step step, prev;
        private double t = 1;
        private int cell, laneH;
        private final int gap = 6, padX = 22, gutter = 60;
        private int areaW;
        private final int[] laneTop = new int[3], cellY = new int[3], cols = new int[3];

        StagePanel() {
            setOpaque(false);
            setFocusable(true);
            setPreferredSize(new Dimension(700, 520));
            getAccessibleContext().setAccessibleName("Counting sort stage");
            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) { requestFocusInWindow(); }
            });
        }

        void show(Step cur, Step previous, double t0) {
            step = cur;
            prev = previous;
            t = t0;
            getAccessibleContext().setAccessibleDescription(cur.phase().title + ". " + cur.text());
            repaint();
        }

        void setT(double v) { t = v; repaint(); }

        /** Does this step have something to animate? Text-only steps skip the animation. */
        static boolean hasMotion(Step s) {
            return switch (s.phase()) {
                case COUNT, PLACE, INIT, ALLOCATE, DONE -> true;
                case PREFIX -> s.countIdx() > 0;
                default -> false;
            };
        }

        private void computeGeometry(int W, int H) {
            int maxCols = Math.max(cols[0], Math.max(cols[1], 1));
            areaW = W - 2 * padX - gutter;
            int byW = (areaW - (maxCols - 1) * gap) / maxCols;
            int chrome = 30 + 32;
            int byH = (H - 4 * 8) / 3 - chrome;
            cell = (int) Gfx.clamp(Math.min(byW, byH), 18, 64);
            laneH = cell + chrome;
            int gapY = Math.max(8, (H - 3 * laneH) / 4);
            for (int l = 0; l < 3; l++) {
                laneTop[l] = gapY + l * (laneH + gapY);
                cellY[l] = laneTop[l] + 30;
            }
        }

        private double cx(int lane, int idx) {
            int c = Math.max(1, cols[lane]);
            double rowW = c * cell + (c - 1) * gap;
            double x0 = padX + gutter + (areaW - rowW) / 2.0;
            return x0 + idx * (cell + gap) + cell / 2.0;
        }

        private static double pop(double t, double start, double dur) {
            if (t <= start || t >= start + dur) return 1;
            return 1 + 0.22 * Math.sin(Math.PI * (t - start) / dur);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = Gfx.aa(g0);
            Theme T = Theme.cur;
            int W = getWidth(), H = getHeight();
            g.setColor(T.surface);
            g.fill(new RoundRectangle2D.Double(0, 0, W, H, 36, 36));
            g.setColor(T.border);
            g.setStroke(new BasicStroke(1f));
            g.draw(new RoundRectangle2D.Double(0.5, 0.5, W - 1, H - 1, 36, 36));
            if (step == null || W < 240 || H < 150) { g.dispose(); return; }

            Step s = step;
            int n = s.input().length, k = s.count().length, m = s.output().length;
            cols[0] = n;
            cols[1] = k == 0 ? n : k;
            cols[2] = m == 0 ? n : m;
            computeGeometry(W, H);

            Phase ph = s.phase();
            double chg = changeAt(ph);

            // Values as currently displayed (an animation may be mid-way).
            int[] cv = s.count().clone();
            Integer[] ov = s.output().clone();
            if (prev != null && s.countIdx() >= 0 && cv.length == prev.count().length && t < chg
                    && (ph == Phase.COUNT || ph == Phase.PREFIX || ph == Phase.PLACE)) {
                cv[s.countIdx()] = prev.count()[s.countIdx()];
            }
            if (prev != null && ph == Phase.PLACE && s.outIdx() >= 0 && t < LAND) ov[s.outIdx()] = null;
            boolean countAppears = prev != null && prev.count().length == 0 && k > 0;
            boolean outAppears = prev != null && prev.output().length == 0 && m > 0;

            // Lane backgrounds, titles and row labels
            String[] titles = {"INPUT", "COUNT", "OUTPUT"};
            String[] subs = {"the array to sort",
                    ph.ordinal() >= Phase.PREFIX.ordinal() ? "running total = end position of each value"
                            : "how many times each value occurs",
                    "the sorted result"};
            Color[] laneColor = {T.input, T.count, T.out};
            for (int l = 0; l < 3; l++) {
                g.setColor(Gfx.alpha(T.surface2, 150));
                g.fill(new RoundRectangle2D.Double(padX - 12, laneTop[l], W - 2 * padX + 24, laneH, 24, 24));
                g.setColor(laneColor[l]);
                g.fill(new Ellipse2D.Double(padX, laneTop[l] + 10, 10, 10));
                g.setFont(Gfx.ui(Font.BOLD, 13f));
                g.setColor(T.text);
                g.drawString(titles[l], padX + 18, laneTop[l] + 19);
                int tw = g.getFontMetrics().stringWidth(titles[l]);
                g.setFont(Gfx.ui(Font.PLAIN, 12.5f));
                g.setColor(T.muted);
                g.drawString(subs[l], padX + 18 + tw + 10, laneTop[l] + 19);
                boolean exists = l == 0 || (l == 1 ? k > 0 : m > 0);
                if (exists) {
                    g.setFont(Gfx.ui(Font.PLAIN, 11.5f));
                    g.drawString("index", padX, cellY[l] + cell + 16);
                }
            }
            if (k > 0) g.drawString("value", padX, cellY[1] + cell + 32);

            // Connectors (dashed) between the cells involved in the current step
            Stroke dashed = new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f,
                    new float[]{5f, 5f}, 0f);
            g.setStroke(dashed);
            g.setColor(Gfx.alpha(T.muted, 150));
            if ((ph == Phase.COUNT || ph == Phase.PLACE) && s.inIdx() >= 0 && s.countIdx() >= 0 && k > 0) {
                g.draw(curve(cx(0, s.inIdx()), cellY[0] + cell, cx(1, s.countIdx()), cellY[1]));
            }
            if (ph == Phase.PLACE && s.outIdx() >= 0 && m > 0) {
                g.draw(curve(cx(1, s.countIdx()), cellY[1] + cell, cx(2, s.outIdx()), cellY[2]));
            }
            if (ph == Phase.PREFIX && s.countIdx() > 0) {
                Path2D arc = new Path2D.Double();
                arc.moveTo(cx(1, s.countIdx() - 1), cellY[1]);
                arc.quadTo((cx(1, s.countIdx() - 1) + cx(1, s.countIdx())) / 2, cellY[1] - cell * 0.9,
                        cx(1, s.countIdx()), cellY[1]);
                g.draw(arc);
            }

            // INPUT lane
            {
                String[] txt = new String[n], l1 = new String[n];
                CellState[] st = new CellState[n];
                double[] sc = new double[n];
                for (int i = 0; i < n; i++) {
                    txt[i] = String.valueOf(s.input()[i]);
                    l1[i] = String.valueOf(i);
                    boolean processed = (ph == Phase.COUNT && i < s.inIdx())
                            || (ph == Phase.PLACE && i > s.inIdx()) || ph == Phase.DONE;
                    st[i] = i == s.inIdx() ? CellState.ACTIVE : processed ? CellState.DONE : CellState.NORMAL;
                    sc[i] = 1;
                }
                drawLane(g, 0, txt, st, sc, l1, null, T.input, 1);
            }
            // COUNT lane
            if (k == 0) {
                drawPlaceholder(g, 1, "Not created yet");
            } else {
                String[] txt = new String[k], l1 = new String[k], l2 = new String[k];
                CellState[] st = new CellState[k];
                double[] sc = new double[k];
                for (int j = 0; j < k; j++) {
                    txt[j] = String.valueOf(cv[j]);
                    l1[j] = String.valueOf(j);
                    l2[j] = String.valueOf(j + s.min());
                    st[j] = j == s.countIdx() ? CellState.ACTIVE : CellState.NORMAL;
                    sc[j] = 1;
                    if (j == s.countIdx() && prev != null && ph != Phase.INIT) {
                        sc[j] = ph == Phase.PLACE ? pop(t, 0.35, 0.25) : pop(t, 0.7, 0.3);
                    }
                    if (countAppears) sc[j] = Gfx.ease(t * 1.6 - 0.6 * j / Math.max(1, k));
                }
                drawLane(g, 1, txt, st, sc, l1, l2, T.count, countAppears ? Gfx.ease(t * 1.4) : 1);
            }
            // OUTPUT lane
            if (m == 0) {
                drawPlaceholder(g, 2, "Not created yet");
            } else {
                String[] txt = new String[m], l1 = new String[m];
                CellState[] st = new CellState[m];
                double[] sc = new double[m];
                for (int i = 0; i < m; i++) {
                    txt[i] = ov[i] == null ? "" : String.valueOf(ov[i]);
                    l1[i] = String.valueOf(i);
                    sc[i] = 1;
                    if (ph == Phase.DONE) {
                        st[i] = CellState.SOLID;
                        if (prev != null) sc[i] = pop(t, 0.6 * i / Math.max(1, m), 0.4);
                    } else if (i == s.outIdx() && ph == Phase.PLACE) {
                        st[i] = ov[i] == null ? CellState.TARGET : CellState.ACTIVE;
                        if (prev != null) sc[i] = pop(t, LAND, 1 - LAND);
                    } else {
                        st[i] = ov[i] == null ? CellState.EMPTY : CellState.DONE;
                    }
                    if (outAppears) sc[i] = Gfx.ease(t * 1.6 - 0.6 * i / Math.max(1, m));
                }
                drawLane(g, 2, txt, st, sc, l1, null, T.out, outAppears ? Gfx.ease(t * 1.4) : 1);
            }

            // The travelling chip
            if (prev != null && t < 1) {
                if (ph == Phase.COUNT && s.inIdx() >= 0 && t < 0.7) {
                    double p = Gfx.ease(t / 0.7);
                    Point2D pt = onCurve(cx(0, s.inIdx()), cellY[0] + cell, cx(1, s.countIdx()), cellY[1], p);
                    drawChip(g, pt.getX(), pt.getY(), String.valueOf(s.input()[s.inIdx()]), T.input);
                } else if (ph == Phase.PLACE && s.inIdx() >= 0 && t < LAND) {
                    String v = String.valueOf(s.input()[s.inIdx()]);
                    if (t < 0.35) {
                        double p = Gfx.ease(t / 0.35);
                        Point2D pt = onCurve(cx(0, s.inIdx()), cellY[0] + cell, cx(1, s.countIdx()), cellY[1], p);
                        drawChip(g, pt.getX(), pt.getY(), v, T.input);
                    } else {
                        double p = Gfx.ease((t - 0.35) / (LAND - 0.35));
                        Point2D pt = onCurve(cx(1, s.countIdx()), cellY[1] + cell, cx(2, s.outIdx()), cellY[2], p);
                        drawChip(g, pt.getX(), pt.getY(), v, Gfx.mix(T.input, T.out, p));
                    }
                } else if (ph == Phase.PREFIX && s.countIdx() > 0 && t < 0.7) {
                    double p = Gfx.ease(t / 0.7);
                    double x1 = cx(1, s.countIdx() - 1), x2 = cx(1, s.countIdx());
                    double x = x1 + (x2 - x1) * p;
                    double y = cellY[1] + cell / 2.0 - cell * 0.75 * Math.sin(Math.PI * p);
                    drawChip(g, x, y, "+" + s.count()[s.countIdx() - 1], T.count);
                }
            }
            drawLegend(g, W - padX, laneTop[0] + 15);
            g.dispose();
        }

        private static Path2D curve(double x1, double y1, double x2, double y2) {
            double dy = (y2 - y1) * 0.5;
            Path2D p = new Path2D.Double();
            p.moveTo(x1, y1);
            p.curveTo(x1, y1 + dy, x2, y2 - dy, x2, y2);
            return p;
        }

        private static Point2D onCurve(double x1, double y1, double x2, double y2, double p) {
            double dy = (y2 - y1) * 0.5, u = 1 - p;
            double c1y = y1 + dy, c2y = y2 - dy;
            double x = u * u * u * x1 + 3 * u * u * p * x1 + 3 * u * p * p * x2 + p * p * p * x2;
            double y = u * u * u * y1 + 3 * u * u * p * c1y + 3 * u * p * p * c2y + p * p * p * y2;
            return new Point2D.Double(x, y);
        }

        private void drawPlaceholder(Graphics2D g, int lane, String msg) {
            Theme T = Theme.cur;
            int c = Math.max(1, cols[lane]);
            double rowW = c * cell + (c - 1) * gap;
            double x0 = padX + gutter + (areaW - rowW) / 2.0;
            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f,
                    new float[]{6f, 6f}, 0f));
            g.setColor(Gfx.alpha(T.cellBorder, 170));
            g.draw(new RoundRectangle2D.Double(x0, cellY[lane], rowW, cell, 16, 16));
            g.setFont(Gfx.ui(Font.ITALIC, 13.5f));
            g.setColor(T.muted);
            Gfx.centerText(g, msg, x0 + rowW / 2, cellY[lane] + cell / 2.0);
        }

        private void drawLane(Graphics2D g, int lane, String[] txt, CellState[] st, double[] sc,
                              String[] l1, String[] l2, Color color, double alpha) {
            Theme T = Theme.cur;
            Composite old = g.getComposite();
            for (int i = 0; i < txt.length; i++) {
                double cxv = cx(lane, i), x = cxv - cell / 2.0, y = cellY[lane];
                drawCell(g, x, y, cell, txt[i], st[i], sc[i], color, alpha);
                if (alpha < 1) g.setComposite(AlphaComposite.SrcOver.derive((float) Gfx.clamp(alpha, 0, 1)));
                boolean act = st[i] == CellState.ACTIVE;
                g.setFont(Gfx.ui(act ? Font.BOLD : Font.PLAIN, 12f));
                g.setColor(act ? T.text : T.muted);
                Gfx.centerText(g, l1[i], cxv, y + cell + 12);
                if (l2 != null) {
                    g.setFont(Gfx.mono(Font.BOLD, 12f));
                    g.setColor(act ? color : T.text);
                    Gfx.centerText(g, l2[i], cxv, y + cell + 28);
                }
                if (act) { // caret above the active cell
                    Path2D tri = new Path2D.Double();
                    tri.moveTo(cxv - 6, y - 10);
                    tri.lineTo(cxv + 6, y - 10);
                    tri.lineTo(cxv, y - 3);
                    tri.closePath();
                    g.setColor(color);
                    g.fill(tri);
                }
                g.setComposite(old);
            }
        }

        private void drawCell(Graphics2D g, double x, double y, double size, String txt, CellState st,
                              double scale, Color lane, double alpha) {
            Theme T = Theme.cur;
            double s = size * Math.max(0.01, scale), cxv = x + size / 2, cyv = y + size / 2;
            double rx = cxv - s / 2, ry = cyv - s / 2, r = Math.max(7, size * 0.17);
            RoundRectangle2D rr = new RoundRectangle2D.Double(rx, ry, s, s, r * 2, r * 2);
            Composite old = g.getComposite();
            if (alpha < 1) g.setComposite(AlphaComposite.SrcOver.derive((float) Gfx.clamp(alpha, 0, 1)));
            Color fg;
            Stroke solid = new BasicStroke(1.5f);
            Stroke dash = new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f,
                    new float[]{4f, 4f}, 0f);
            switch (st) {
                case ACTIVE -> {
                    for (int i = 3; i >= 1; i--) {
                        double e = i * 3;
                        g.setColor(Gfx.alpha(lane, 30));
                        g.fill(new RoundRectangle2D.Double(rx - e, ry - e, s + 2 * e, s + 2 * e, r * 2 + e, r * 2 + e));
                    }
                    g.setColor(lane);
                    g.fill(rr);
                    g.setColor(Gfx.mix(lane, Color.BLACK, 0.22));
                    g.setStroke(solid);
                    g.draw(rr);
                    fg = Gfx.onColor(lane);
                }
                case SOLID -> {
                    g.setColor(lane);
                    g.fill(rr);
                    g.setColor(Gfx.mix(lane, Color.BLACK, 0.22));
                    g.setStroke(solid);
                    g.draw(rr);
                    fg = Gfx.onColor(lane);
                }
                case DONE -> {
                    g.setColor(Gfx.mix(T.cell, lane, 0.22));
                    g.fill(rr);
                    g.setColor(Gfx.mix(T.cellBorder, lane, 0.55));
                    g.setStroke(solid);
                    g.draw(rr);
                    fg = Gfx.mix(T.text, T.muted, 0.35);
                }
                case TARGET -> {
                    g.setColor(Gfx.alpha(lane, 38));
                    g.fill(rr);
                    g.setColor(lane);
                    g.setStroke(dash);
                    g.draw(rr);
                    fg = T.muted;
                }
                case EMPTY -> {
                    g.setColor(T.cellBorder);
                    g.setStroke(dash);
                    g.draw(rr);
                    fg = T.muted;
                }
                default -> {
                    g.setColor(T.cell);
                    g.fill(rr);
                    g.setColor(T.cellBorder);
                    g.setStroke(solid);
                    g.draw(rr);
                    fg = T.text;
                }
            }
            if (!txt.isEmpty()) {
                g.setColor(fg);
                g.setFont(Gfx.ui(Font.BOLD, (float) Gfx.clamp(s * 0.4, 11, 24)));
                Gfx.centerText(g, txt, cxv, cyv);
            }
            g.setComposite(old);
        }

        private void drawChip(Graphics2D g, double x, double y, String txt, Color c) {
            Theme T = Theme.cur;
            double r = Math.max(13, cell * 0.36);
            g.setColor(new Color(0, 0, 0, T.dark ? 100 : 55));
            g.fill(new Ellipse2D.Double(x - r, y - r + 4, 2 * r, 2 * r));
            g.setColor(c);
            g.fill(new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r));
            g.setColor(Gfx.alpha(Color.WHITE, 210));
            g.setStroke(new BasicStroke(2f));
            g.draw(new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r));
            g.setColor(Gfx.onColor(c));
            g.setFont(Gfx.ui(Font.BOLD, (float) Math.max(12, r * 0.9)));
            Gfx.centerText(g, txt, x, y);
        }

        private void drawLegend(Graphics2D g, int right, int cy) {
            Theme T = Theme.cur;
            String[] labels = {"Active", "Processed", "Empty / target"};
            g.setFont(Gfx.ui(Font.PLAIN, 12f));
            FontMetrics fm = g.getFontMetrics();
            int total = 0;
            for (String l : labels) total += 21 + fm.stringWidth(l) + 18;
            double x = right - total + 18;
            for (int kind = 0; kind < 3; kind++) {
                RoundRectangle2D sw = new RoundRectangle2D.Double(x, cy - 7, 14, 14, 5, 5);
                if (kind == 0) {
                    Shape oldClip = g.getClip();
                    g.clip(sw);
                    Color[] lc = {T.input, T.count, T.out};
                    for (int q = 0; q < 3; q++) {
                        g.setColor(lc[q]);
                        g.fill(new Rectangle2D.Double(x + q * 14 / 3.0, cy - 7, 14 / 3.0 + 0.5, 14));
                    }
                    g.setClip(oldClip);
                } else if (kind == 1) {
                    g.setColor(Gfx.mix(T.cell, T.accent, 0.25));
                    g.fill(sw);
                    g.setColor(Gfx.mix(T.cellBorder, T.accent, 0.55));
                    g.setStroke(new BasicStroke(1.2f));
                    g.draw(sw);
                } else {
                    g.setColor(T.cellBorder);
                    g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f,
                            new float[]{3f, 3f}, 0f));
                    g.draw(sw);
                }
                g.setColor(T.muted);
                g.drawString(labels[kind], (float) (x + 21), (float) (cy + 4));
                x += 21 + fm.stringWidth(labels[kind]) + 18;
            }
        }
    }

    /** Text field with rounded border, placeholder and inline error state. */
    static final class RoundField extends JTextField {
        private boolean error;
        private final String placeholder;

        RoundField(String placeholder) {
            this.placeholder = placeholder;
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(9, 14, 9, 14));
            addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) { repaint(); }
                @Override public void focusLost(FocusEvent e) { repaint(); }
            });
            getAccessibleContext().setAccessibleName("Numbers to sort");
        }

        void setError(boolean e) { error = e; repaint(); }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = Gfx.aa(g0);
            Theme t = Theme.cur;
            int w = getWidth(), h = getHeight();
            RoundRectangle2D r = new RoundRectangle2D.Double(0.5, 0.5, w - 1, h - 1, 14, 14);
            g.setColor(t.cell);
            g.fill(r);
            if (hasFocus() && !error) {
                g.setColor(Gfx.alpha(t.accent, 55));
                g.setStroke(new BasicStroke(5f));
                g.draw(r);
            }
            g.setColor(error ? t.danger : hasFocus() ? t.accent : t.border);
            g.setStroke(new BasicStroke(hasFocus() || error ? 2f : 1f));
            g.draw(r);
            g.dispose();
            super.paintComponent(g0);
            if (getText().isEmpty()) {
                Graphics2D p = Gfx.aa(g0);
                p.setFont(getFont());
                p.setColor(Gfx.alpha(t.muted, 190));
                FontMetrics fm = p.getFontMetrics();
                p.drawString(placeholder, getInsets().left, (h + fm.getAscent() - fm.getDescent()) / 2);
                p.dispose();
            }
        }
    }

    /** The whole GUI as a panel (kept separate from the window so it can be tested headlessly). */
    static final class AppPanel extends JPanel {
        private static final double[] SPEEDS = {0.5, 1, 2, 4};
        private static final java.util.prefs.Preferences PREFS =
                java.util.prefs.Preferences.userNodeForPackage(CountingSortVisualizer.class);

        final StagePanel stage = new StagePanel();
        final PhaseStepper stepper = new PhaseStepper();
        final CodeView code = new CodeView();
        final VarsView vars = new VarsView();
        final Scrubber scrubber = new Scrubber();
        final RoundField field = new RoundField("e.g. 4, 2, 2, 8, 3, 3, 1   (up to " + MAX_N
                + " numbers, range up to " + MAX_RANGE + ")");
        private final JLabel title = new JLabel("Counting Sort");
        private final JLabel subtitle = new JLabel("See how it counts, accumulates and places every element.");
        private final JLabel errorLabel = new JLabel(" ");
        private final JLabel stepTitle = new JLabel();
        private final JLabel stepBadge = new JLabel();
        private final JLabel hint = new JLabel("\u2190 \u2192  step     Space  play / pause     Home  restart     T  theme", SwingConstants.CENTER);
        private final JTextArea explanation = new JTextArea(3, 40);
        private final FlatButton restart, prevBtn, playBtn, nextBtn, themeBtn;
        private final List<FlatButton> presetChips = new ArrayList<>();
        private final List<Runnable> themers = new ArrayList<>();
        private final javax.swing.Timer clock;

        List<Step> steps = List.of();
        int index;
        private boolean playing;
        private double animT = 1, dwellLeft;
        private int speedIdx = 1;
        private long lastTick;

        AppPanel(int[] initial) {
            super(new BorderLayout(0, 10));
            setBorder(BorderFactory.createEmptyBorder(14, 20, 10, 20));
            Theme.cur = PREFS.getBoolean("dark", false) ? Theme.DARK : Theme.LIGHT;

            clock = new javax.swing.Timer(16, e -> tick());

            // ---- header
            JPanel titles = new JPanel();
            titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
            titles.setOpaque(false);
            title.setFont(Gfx.ui(Font.BOLD, 24f));
            subtitle.setFont(Gfx.ui(Font.PLAIN, 14f));
            titles.add(title);
            titles.add(Box.createVerticalStrut(2));
            titles.add(subtitle);
            themeBtn = new FlatButton(null, Glyph.MOON, FlatButton.Style.GHOST, "Switch theme (T)", this::toggleTheme);
            JPanel header = new JPanel(new BorderLayout());
            header.setOpaque(false);
            header.add(titles, BorderLayout.WEST);
            header.add(themeBtn, BorderLayout.EAST);

            // ---- input card
            RoundPanel inputCard = new RoundPanel(new BorderLayout(0, 8), 14);
            inputCard.setBorder(BorderFactory.createEmptyBorder(12, 14, 8, 14));
            JLabel arrayLabel = new JLabel("Array");
            arrayLabel.setFont(Gfx.ui(Font.BOLD, 14f));
            field.setFont(Gfx.ui(Font.PLAIN, 15f));
            FlatButton go = new FlatButton("Visualize", null, FlatButton.Style.PRIMARY, "Run counting sort on this array", this::applyField);
            go.fixedWidth(120);
            field.addActionListener(e -> applyField());
            field.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                void clear() { if (field.error) { field.setError(false); errorLabel.setText(" "); } }
                @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { clear(); }
                @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { clear(); }
                @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { clear(); }
            });
            JPanel row1 = new JPanel(new BorderLayout(12, 0));
            row1.setOpaque(false);
            row1.add(arrayLabel, BorderLayout.WEST);
            row1.add(field, BorderLayout.CENTER);
            row1.add(go, BorderLayout.EAST);

            JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            row2.setOpaque(false);
            JLabel tryLabel = new JLabel("Try");
            tryLabel.setFont(Gfx.ui(Font.BOLD, 13f));
            row2.add(tryLabel);
            row2.add(Box.createHorizontalStrut(2));
            addPreset(row2, "Classic", () -> new int[]{4, 2, 2, 8, 3, 3, 1});
            addPreset(row2, "Duplicates", () -> new int[]{3, 3, 3, 1, 1, 2, 2, 3});
            addPreset(row2, "Negatives", () -> new int[]{5, -3, 0, -3, 2, -1});
            addPreset(row2, "Reversed", () -> new int[]{9, 8, 7, 6, 5, 4, 3, 2, 1});
            addPreset(row2, "Random", () -> randomArray(new Random()));
            errorLabel.setFont(Gfx.ui(Font.BOLD, 12.5f));
            JPanel south = new JPanel(new BorderLayout());
            south.setOpaque(false);
            south.add(row2, BorderLayout.WEST);
            south.add(errorLabel, BorderLayout.CENTER);
            errorLabel.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));
            inputCard.add(row1, BorderLayout.NORTH);
            inputCard.add(south, BorderLayout.CENTER);

            JPanel north = new JPanel(new BorderLayout(0, 12));
            north.setOpaque(false);
            north.add(header, BorderLayout.NORTH);
            north.add(inputCard, BorderLayout.CENTER);
            add(north, BorderLayout.NORTH);

            // ---- centre: stepper, stage, explanation | code, variables
            RoundPanel stepperCard = new RoundPanel(new BorderLayout(), 14);
            stepperCard.add(stepper, BorderLayout.CENTER);
            stepper.onPick(this::jumpToGroup);

            stepTitle.setFont(Gfx.ui(Font.BOLD, 16f));
            stepBadge.setFont(Gfx.ui(Font.PLAIN, 13f));
            explanation.setEditable(false);
            explanation.setFocusable(false);
            explanation.setLineWrap(true);
            explanation.setWrapStyleWord(true);
            explanation.setOpaque(false);
            explanation.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
            explanation.setFont(Gfx.ui(Font.PLAIN, 15f));
            RoundPanel explainCard = new RoundPanel(new BorderLayout(), 14);
            explainCard.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
            JPanel eh = new JPanel(new BorderLayout());
            eh.setOpaque(false);
            eh.add(stepTitle, BorderLayout.WEST);
            eh.add(stepBadge, BorderLayout.EAST);
            explainCard.add(eh, BorderLayout.NORTH);
            explainCard.add(explanation, BorderLayout.CENTER);
            explainCard.setPreferredSize(new Dimension(300, 108));

            JPanel left = new JPanel(new BorderLayout(0, 12));
            left.setOpaque(false);
            left.add(stepperCard, BorderLayout.NORTH);
            left.add(stage, BorderLayout.CENTER);
            left.add(explainCard, BorderLayout.SOUTH);

            JPanel right = new JPanel(new BorderLayout(0, 12));
            right.setOpaque(false);
            right.add(code, BorderLayout.CENTER);
            right.add(vars, BorderLayout.SOUTH);
            right.setPreferredSize(new Dimension(320, 300));

            JPanel center = new JPanel(new BorderLayout(14, 0));
            center.setOpaque(false);
            center.add(left, BorderLayout.CENTER);
            center.add(right, BorderLayout.EAST);
            add(center, BorderLayout.CENTER);

            // ---- transport bar
            restart = new FlatButton(null, Glyph.RESTART, FlatButton.Style.GHOST, "Restart (Home)", () -> { pause(); goTo(0, false); });
            prevBtn = new FlatButton(null, Glyph.PREV, FlatButton.Style.SECONDARY, "Previous step (\u2190)", () -> { pause(); goTo(index - 1, false); });
            playBtn = new FlatButton("Play", Glyph.PLAY, FlatButton.Style.PRIMARY, "Play / pause (Space)", this::togglePlay).fixedWidth(110);
            nextBtn = new FlatButton(null, Glyph.NEXT, FlatButton.Style.SECONDARY, "Next step (\u2192)", () -> { pause(); goTo(index + 1, true); });
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            buttons.setOpaque(false);
            buttons.add(restart);
            buttons.add(prevBtn);
            buttons.add(playBtn);
            buttons.add(nextBtn);

            Segmented speed = new Segmented(new String[]{"0.5\u00D7", "1\u00D7", "2\u00D7", "4\u00D7"}, speedIdx,
                    "Playback speed", i -> speedIdx = i);
            JLabel speedLabel = new JLabel("Speed");
            speedLabel.setFont(Gfx.ui(Font.BOLD, 13f));
            JPanel speedBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            speedBox.setOpaque(false);
            speedBox.add(speedLabel);
            speedBox.add(speed);

            RoundPanel transport = new RoundPanel(new BorderLayout(16, 0), 14);
            transport.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
            transport.add(buttons, BorderLayout.WEST);
            transport.add(scrubber, BorderLayout.CENTER);
            transport.add(speedBox, BorderLayout.EAST);

            hint.setFont(Gfx.ui(Font.PLAIN, 12f));
            hint.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
            JPanel bottom = new JPanel(new BorderLayout());
            bottom.setOpaque(false);
            bottom.add(transport, BorderLayout.CENTER);
            bottom.add(hint, BorderLayout.SOUTH);
            add(bottom, BorderLayout.SOUTH);

            // ---- theming + keyboard
            themers.add(() -> {
                Theme t = Theme.cur;
                setBackground(t.bg);
                title.setForeground(t.text);
                subtitle.setForeground(t.muted);
                arrayLabel.setForeground(t.text);
                tryLabel.setForeground(t.muted);
                speedLabel.setForeground(t.muted);
                hint.setForeground(t.muted);
                stepBadge.setForeground(t.muted);
                explanation.setForeground(t.text);
                field.setForeground(t.text);
                field.setCaretColor(t.accent);
                field.setSelectionColor(t.accentSoft);
                field.setSelectedTextColor(t.text);
                errorLabel.setForeground(t.danger);
                themeBtn.setGlyph(t.dark ? Glyph.SUN : Glyph.MOON);
            });
            bind("LEFT", () -> { pause(); goTo(index - 1, false); }, true);
            bind("RIGHT", () -> { pause(); goTo(index + 1, true); }, true);
            bind("SPACE", this::togglePlay, true);
            bind("HOME", () -> { pause(); goTo(0, false); }, true);
            bind("END", () -> { pause(); goTo(steps.size() - 1, false); }, true);
            bind("T", this::toggleTheme, true);

            setOpaque(true);
            field.setText(join(initial));
            load(initial);
            applyTheme();
        }

        private void addPreset(JPanel row, String name, java.util.function.Supplier<int[]> data) {
            FlatButton chip = new FlatButton(name, null, FlatButton.Style.CHIP, "Use the \"" + name + "\" example", () -> {
                int[] a = data.get();
                field.setText(join(a));
                applyField();
            });
            presetChips.add(chip);
            row.add(chip);
        }

        private void bind(String key, Runnable r, boolean guardField) {
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), key);
            getActionMap().put(key, new AbstractAction() {
                @Override public void actionPerformed(ActionEvent e) {
                    if (guardField && field.hasFocus()) return;
                    r.run();
                }
            });
        }

        // ---- theme
        private void toggleTheme() {
            Theme.cur = Theme.cur.dark ? Theme.LIGHT : Theme.DARK;
            try { PREFS.putBoolean("dark", Theme.cur.dark); } catch (RuntimeException ignored) { }
            applyTheme();
        }

        void applyTheme() {
            for (Runnable r : themers) r.run();
            if (!steps.isEmpty()) refresh(steps.get(index));
            repaint();
        }

        // ---- input
        private void applyField() {
            try {
                int[] a = parse(field.getText());
                String err = CountingSortEngine.validate(a);
                if (err != null) throw new IllegalArgumentException(err);
                field.setError(false);
                errorLabel.setText(" ");
                pause();
                load(a);
                stage.requestFocusInWindow();
            } catch (IllegalArgumentException ex) {
                field.setError(true);
                errorLabel.setText(ex.getMessage());
                field.requestFocusInWindow();
            }
        }

        private void load(int[] a) {
            steps = CountingSortEngine.build(a);
            scrubber.configure(steps, i -> { pause(); goTo(i, false); });
            goTo(0, false);
        }

        // ---- navigation
        private void jumpToGroup(int group) {
            pause();
            for (int i = 0; i < steps.size(); i++) {
                if (group(steps.get(i).phase()) == group) { goTo(i, false); return; }
            }
        }

        static int group(Phase p) {
            return switch (p) {
                case INTRO -> -1;
                case RANGE -> 0;
                case INIT -> 1;
                case COUNT -> 2;
                case PREFIX -> 3;
                case ALLOCATE, PLACE -> 4;
                case DONE -> 5;
            };
        }

        void goTo(int i, boolean animate) {
            int old = index;
            index = Math.max(0, Math.min(steps.size() - 1, i));
            Step cur = steps.get(index);
            Step pv = animate && index == old + 1 && StagePanel.hasMotion(cur) ? steps.get(old) : null;
            animT = pv == null ? 1 : 0;
            stage.show(cur, pv, animT);
            dwellLeft = pv == null ? (StagePanel.hasMotion(cur) ? 700 : 1700) / SPEEDS[speedIdx] : 0;
            refresh(cur);
            if (playing || pv != null) startClock();
        }

        private void refresh(Step cur) {
            Theme t = Theme.cur;
            stepper.setActive(group(cur.phase()));
            code.setStep(cur);
            vars.setStep(cur);
            scrubber.setValue(index);
            stepTitle.setText(cur.phase().title);
            stepTitle.setForeground(cur.phase() == Phase.DONE ? t.out : t.text);
            stepBadge.setText("Step " + (index + 1) + " of " + steps.size());
            explanation.setText(cur.text());
            restart.setEnabled(index > 0);
            prevBtn.setEnabled(index > 0);
            nextBtn.setEnabled(index < steps.size() - 1);
            updatePlayButton();
        }

        // ---- playback
        private void startClock() {
            lastTick = System.nanoTime();
            if (!clock.isRunning()) clock.start();
        }

        private void tick() {
            long now = System.nanoTime();
            double dt = (now - lastTick) / 1e6;
            lastTick = now;
            double speed = SPEEDS[speedIdx];
            if (animT < 1) {
                animT = Math.min(1, animT + dt * speed / 1000.0);
                stage.setT(animT);
                if (animT >= 1) dwellLeft = 450 / speed;
            } else if (playing) {
                dwellLeft -= dt;
                if (dwellLeft <= 0) {
                    if (index < steps.size() - 1) goTo(index + 1, true);
                    else pause();
                }
            } else {
                clock.stop();
            }
        }

        private void togglePlay() {
            if (playing) { pause(); return; }
            if (index >= steps.size() - 1) goTo(0, false);
            playing = true;
            updatePlayButton();
            dwellLeft = 500 / SPEEDS[speedIdx];
            startClock();
        }

        private void pause() {
            playing = false;
            updatePlayButton();
        }

        private void updatePlayButton() {
            boolean atEnd = !steps.isEmpty() && index >= steps.size() - 1;
            playBtn.setText(playing ? "Pause" : atEnd ? "Replay" : "Play");
            playBtn.setGlyph(playing ? Glyph.PAUSE : atEnd ? Glyph.RESTART : Glyph.PLAY);
        }
    }

    /** Top-level window. */
    static final class GuiView extends JFrame {
        static {
            System.setProperty("awt.useSystemAAFontSettings", "on");
            System.setProperty("swing.aatext", "true");
        }

        GuiView(int[] initial) {
            super("Counting Sort Visualizer");
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            setContentPane(new AppPanel(initial));
            setSize(1220, 840);
            setMinimumSize(new Dimension(1020, 780));
            setLocationRelativeTo(null);
        }
    }

    // ------------------------------------------------------------------
    // Input helpers
    // ------------------------------------------------------------------

    static int[] parse(String text) {
        String t = text == null ? "" : text.trim();
        if (t.isEmpty()) throw new IllegalArgumentException("Please enter some numbers.");
        String[] parts = t.split("[,\\s]+");
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                out[i] = Integer.parseInt(parts[i]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("'" + parts[i] + "' is not a valid integer.");
            }
        }
        return out;
    }

    static int[] randomArray(Random r) {
        int[] a = new int[8 + r.nextInt(5)];
        for (int i = 0; i < a.length; i++) a[i] = r.nextInt(10);
        return a;
    }

    static String join(int[] a) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < a.length; i++) sb.append(i == 0 ? "" : ", ").append(a[i]);
        return sb.toString();
    }

    static int[] askForArray(Scanner in) {
        while (true) {
            System.out.println("\nChoose the input array:");
            System.out.println("  1) Classic example  [4, 2, 2, 8, 3, 3, 1]");
            System.out.println("  2) Random");
            System.out.println("  3) Type my own (max " + MAX_N + " numbers, range <= " + MAX_RANGE + ")");
            System.out.print("> ");
            if (!in.hasNextLine()) return new int[]{4, 2, 2, 8, 3, 3, 1};
            switch (in.nextLine().trim()) {
                case "1" -> { return new int[]{4, 2, 2, 8, 3, 3, 1}; }
                case "2" -> {
                    int[] a = randomArray(new Random());
                    System.out.println("Using: " + join(a));
                    return a;
                }
                case "3" -> {
                    System.out.print("Numbers (e.g. 5 3 -1 3 0): ");
                    if (!in.hasNextLine()) continue;
                    try {
                        int[] a = parse(in.nextLine());
                        String err = CountingSortEngine.validate(a);
                        if (err == null) return a;
                        System.out.println("  ! " + err);
                    } catch (IllegalArgumentException e) {
                        System.out.println("  ! " + e.getMessage());
                    }
                }
                default -> System.out.println("  ! Please enter 1, 2 or 3.");
            }
        }
    }

    // ------------------------------------------------------------------
    // Entry point
    // ------------------------------------------------------------------

    public static void main(String[] args) {
        boolean color = Arrays.stream(args).noneMatch("--no-color"::equals);
        String mode = args.length > 0 ? args[0].toLowerCase() : "";
        Scanner in = new Scanner(System.in);

        if (!mode.equals("gui") && !mode.equals("terminal")) {
            System.out.println("+----------------------------------+");
            System.out.println("|   COUNTING SORT VISUALIZER       |");
            System.out.println("+----------------------------------+");
            System.out.println("  1) Terminal (ASCII)");
            System.out.println("  2) GUI (window)");
            System.out.print("> ");
            String choice = in.hasNextLine() ? in.nextLine().trim() : "1";
            mode = choice.equals("2") ? "gui" : "terminal";
        }

        int[] data = askForArray(in);

        if (mode.equals("gui")) {
            SwingUtilities.invokeLater(() -> new GuiView(data).setVisible(true));
        } else {
            new TerminalView(in, color).run(CountingSortEngine.build(data));
        }
    }
}