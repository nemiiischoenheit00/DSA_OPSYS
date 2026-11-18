import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.*;

/**
 * The main stage: three lanes (input / count / output) with animated transitions.
 *
 * Arrays of any size are supported. When they do not fit on screen, each lane shows a
 * WINDOW of cells that follows the active cell, and the gutter says which part is visible.
 */
final class StagePanel extends Widget {

    private enum CellState { NORMAL, ACTIVE, SOLID, DONE, TARGET, EMPTY }

    /** Moment (0..1 of a step's animation) at which the count cell changes. */
    static double changeAt(Phase p) { return p == Phase.PLACE ? 0.35 : 0.7; }

    /** Moment at which a placed element lands in the output array. */
    static final double LAND = 0.8;

    private static final int LANES = 3;

    private CountingSortEngine engine;
    private Step step;
    private boolean animating;
    private double t = 1;

    // geometry, recomputed on every paint
    private int cell;
    private int laneH;
    private int areaW;
    private final int gap = 6;
    private final int padX = 22;
    private final int gutter = 64;
    private final int[] laneTop = new int[LANES];
    private final int[] cellY = new int[LANES];
    private final int[] size = new int[LANES];      // number of cells in each lane
    private final int[] visible = new int[LANES];   // number of cells that fit on screen
    private final int[] first = new int[LANES];     // index of the first visible cell

    StagePanel() {
        setOpaque(false);
        setFocusable(true);
        setPreferredSize(new Dimension(700, 520));
        getAccessibleContext().setAccessibleName("Counting sort stage");
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { requestFocusInWindow(); }
        });
    }

    /** Shows a step. 'animate' = play the transition from the previous step (t runs 0..1). */
    void show(CountingSortEngine eng, Step cur, boolean animate, double t0) {
        engine = eng;
        step = cur;
        animating = animate;
        t = t0;
        getAccessibleContext().setAccessibleDescription(cur.phase().title + ". " + cur.text());
        repaint();
    }

    void clear() {
        engine = null;
        step = null;
        repaint();
    }

    void setT(double v) {
        t = v;
        repaint();
    }

    /** Does this step have something to animate? Text-only steps skip the animation. */
    static boolean hasMotion(Step s) {
        return switch (s.phase()) {
            case COUNT, PLACE, INIT, ALLOCATE, DONE -> true;
            case PREFIX -> s.countIdx() > 0;
            default -> false;
        };
    }

    // ------------------------------------------------------------------
    // Geometry
    // ------------------------------------------------------------------

    private void computeGeometry(int w, int h) {
        areaW = w - 2 * padX - gutter;
        int chrome = 30 + 32;
        int byHeight = (h - 4 * 8) / 3 - chrome;
        int maxCols = Math.max(1, Math.max(size[0], size[1]));
        int fitAll = (areaW - (maxCols - 1) * gap) / maxCols;
        if (fitAll >= 30) {                                   // everything fits: no windowing
            cell = (int) Gfx.clamp(Math.min(fitAll, Math.min(byHeight, 64)), 18, 64);
            for (int l = 0; l < LANES; l++) visible[l] = size[l];
        } else {                                              // too many cells: show a window
            cell = (int) Gfx.clamp(Math.min(40, byHeight), 18, 40);
            int maxVisible = Math.max(1, (areaW + gap) / (cell + gap));
            for (int l = 0; l < LANES; l++) visible[l] = Math.min(size[l], maxVisible);
        }
        laneH = cell + chrome;
        int gapY = Math.max(8, (h - 3 * laneH) / 4);
        for (int l = 0; l < LANES; l++) {
            laneTop[l] = gapY + l * (laneH + gapY);
            cellY[l] = laneTop[l] + 30;
        }
    }

    /** Scrolls a lane's window so that the active cell stays visible (with a little margin). */
    private void followActive(int lane, int active) {
        int total = size[lane], v = visible[lane];
        if (v >= total) {
            first[lane] = 0;
            return;
        }
        int s = first[lane];
        if (active >= 0 && (active < s + 1 || active > s + v - 2)) s = active - v / 2;
        first[lane] = Math.max(0, Math.min(total - v, s));
    }

    /** x of the centre of the cell shown at display position d of a lane. */
    private double cxDisplay(int lane, int d) {
        int c = Math.max(1, visible[lane]);
        double rowW = c * cell + (c - 1) * gap;
        double x0 = padX + gutter + (areaW - rowW) / 2.0;
        return x0 + d * (cell + gap) + cell / 2.0;
    }

    /** x of the centre of cell number idx of a lane (it must currently be visible). */
    private double cx(int lane, int idx) {
        return cxDisplay(lane, idx - first[lane]);
    }

    private static double pop(double t, double start, double dur) {
        if (t <= start || t >= start + dur) return 1;
        return 1 + 0.22 * Math.sin(Math.PI * (t - start) / dur);
    }

    // ------------------------------------------------------------------
    // Painting
    // ------------------------------------------------------------------

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
        if (engine == null || step == null) {
            paintEmptyState(g, W, H);
            g.dispose();
            return;
        }
        if (W < 240 || H < 150) {
            g.dispose();
            return;
        }

        Step s = step;
        Phase ph = s.phase();
        int n = engine.n(), k = engine.k();
        boolean countExists = ph.ordinal() >= Phase.INIT.ordinal();
        boolean outExists = ph.ordinal() >= Phase.ALLOCATE.ordinal();
        size[0] = n;
        size[1] = countExists ? k : n;
        size[2] = n;
        computeGeometry(W, H);
        followActive(0, s.inIdx());
        followActive(1, countExists ? s.countIdx() : -1);
        followActive(2, outExists ? s.outIdx() : -1);

        double chg = changeAt(ph);
        boolean countAppears = animating && ph == Phase.INIT;
        boolean outAppears = animating && ph == Phase.ALLOCATE;

        // lane backgrounds, titles, gutter labels
        String[] titles = {"INPUT", "COUNT", "OUTPUT"};
        String[] subs = {
            "the array to sort  \u00B7  n = " + String.format("%,d", n),
            (ph.ordinal() >= Phase.PREFIX.ordinal() ? "running total = end position of each value"
                    : "how many times each value occurs") + "  \u00B7  k = " + String.format("%,d", k),
            "the sorted result"
        };
        Color[] laneColor = {T.input, T.count, T.out};
        boolean[] exists = {true, countExists, outExists};
        for (int l = 0; l < LANES; l++) {
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
            if (exists[l]) {
                g.setFont(Gfx.ui(Font.PLAIN, 11.5f));
                g.drawString("index", padX, cellY[l] + cell + 16);
                if (visible[l] < size[l]) {                       // tell the user which part is shown
                    g.drawString(String.format("%,d\u2013%,d", first[l] + 1, first[l] + visible[l]), padX, cellY[l] + cell / 2 - 2);
                    g.drawString(String.format("of %,d", size[l]), padX, cellY[l] + cell / 2 + 12);
                }
            }
        }
        if (countExists) {
            g.setFont(Gfx.ui(Font.PLAIN, 11.5f));
            g.setColor(T.muted);
            g.drawString("value", padX, cellY[1] + cell + 32);
        }

        // dashed connectors between the cells involved in this step
        g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f,
                new float[]{5f, 5f}, 0f));
        g.setColor(Gfx.alpha(T.muted, 150));
        if ((ph == Phase.COUNT || ph == Phase.PLACE) && s.inIdx() >= 0 && s.countIdx() >= 0) {
            g.draw(curve(cx(0, s.inIdx()), cellY[0] + cell, cx(1, s.countIdx()), cellY[1]));
        }
        if (ph == Phase.PLACE && s.outIdx() >= 0) {
            g.draw(curve(cx(1, s.countIdx()), cellY[1] + cell, cx(2, s.outIdx()), cellY[2]));
        }
        if (ph == Phase.PREFIX && s.countIdx() > 0) {
            Path2D arc = new Path2D.Double();
            double xa = cx(1, s.countIdx() - 1), xb = cx(1, s.countIdx());
            arc.moveTo(xa, cellY[1]);
            arc.quadTo((xa + xb) / 2, cellY[1] - cell * 0.9, xb, cellY[1]);
            g.draw(arc);
        }

        // ---- INPUT lane
        {
            int v = visible[0];
            String[] txt = new String[v], l1 = new String[v];
            CellState[] st = new CellState[v];
            double[] sc = new double[v];
            for (int d = 0; d < v; d++) {
                int i = first[0] + d;
                txt[d] = String.valueOf(engine.input()[i]);
                l1[d] = String.format("%d", i);
                boolean processed = (ph == Phase.COUNT && i < s.inIdx())
                        || (ph == Phase.PLACE && i > s.inIdx()) || ph == Phase.DONE;
                st[d] = i == s.inIdx() ? CellState.ACTIVE : processed ? CellState.DONE : CellState.NORMAL;
                sc[d] = 1;
            }
            drawLane(g, 0, txt, st, sc, l1, null, T.input, 1);
        }

        // ---- COUNT lane
        if (!countExists) {
            drawPlaceholder(g, 1, "Not created yet");
        } else {
            int v = visible[1];
            String[] txt = new String[v], l1 = new String[v], l2 = new String[v];
            CellState[] st = new CellState[v];
            double[] sc = new double[v];
            int[] counts = engine.count();
            for (int d = 0; d < v; d++) {
                int j = first[1] + d;
                int shown = counts[j];
                boolean changing = animating && j == s.countIdx() && t < chg
                        && (ph == Phase.COUNT || ph == Phase.PREFIX || ph == Phase.PLACE);
                if (changing) shown = s.countBefore();       // show the old value until the chip arrives
                txt[d] = String.valueOf(shown);
                l1[d] = String.valueOf(j);
                l2[d] = String.valueOf((long) j + s.min());
                st[d] = j == s.countIdx() ? CellState.ACTIVE : CellState.NORMAL;
                sc[d] = 1;
                if (j == s.countIdx() && animating && ph != Phase.INIT) {
                    sc[d] = ph == Phase.PLACE ? pop(t, 0.35, 0.25) : pop(t, 0.7, 0.3);
                }
                if (countAppears) sc[d] = Gfx.ease(t * 1.6 - 0.6 * d / Math.max(1, v));
            }
            drawLane(g, 1, txt, st, sc, l1, l2, T.count, countAppears ? Gfx.ease(t * 1.4) : 1);
        }

        // ---- OUTPUT lane
        if (!outExists) {
            drawPlaceholder(g, 2, "Not created yet");
        } else {
            int v = visible[2];
            String[] txt = new String[v], l1 = new String[v];
            CellState[] st = new CellState[v];
            double[] sc = new double[v];
            for (int d = 0; d < v; d++) {
                int i = first[2] + d;
                boolean filled = engine.isPlaced(i);
                if (animating && ph == Phase.PLACE && i == s.outIdx() && t < LAND) filled = false;   // chip still travelling
                txt[d] = filled ? String.valueOf(engine.outputAt(i)) : "";
                l1[d] = String.valueOf(i);
                sc[d] = 1;
                if (ph == Phase.DONE) {
                    st[d] = CellState.SOLID;
                    if (animating) sc[d] = pop(t, 0.6 * d / Math.max(1, v), 0.4);
                } else if (i == s.outIdx() && ph == Phase.PLACE) {
                    st[d] = filled ? CellState.ACTIVE : CellState.TARGET;
                    if (animating) sc[d] = pop(t, LAND, 1 - LAND);
                } else {
                    st[d] = filled ? CellState.DONE : CellState.EMPTY;
                }
                if (outAppears) sc[d] = Gfx.ease(t * 1.6 - 0.6 * d / Math.max(1, v));
            }
            drawLane(g, 2, txt, st, sc, l1, null, T.out, outAppears ? Gfx.ease(t * 1.4) : 1);
        }

        // ---- the travelling chip
        if (animating && t < 1) {
            if (ph == Phase.COUNT && s.inIdx() >= 0 && t < 0.7) {
                double p = Gfx.ease(t / 0.7);
                Point2D pt = onCurve(cx(0, s.inIdx()), cellY[0] + cell, cx(1, s.countIdx()), cellY[1], p);
                drawChip(g, pt.getX(), pt.getY(), String.valueOf(s.value()), T.input);
            } else if (ph == Phase.PLACE && s.inIdx() >= 0 && t < LAND) {
                String val = String.valueOf(s.value());
                if (t < 0.35) {
                    double p = Gfx.ease(t / 0.35);
                    Point2D pt = onCurve(cx(0, s.inIdx()), cellY[0] + cell, cx(1, s.countIdx()), cellY[1], p);
                    drawChip(g, pt.getX(), pt.getY(), val, T.input);
                } else {
                    double p = Gfx.ease((t - 0.35) / (LAND - 0.35));
                    Point2D pt = onCurve(cx(1, s.countIdx()), cellY[1] + cell, cx(2, s.outIdx()), cellY[2], p);
                    drawChip(g, pt.getX(), pt.getY(), val, Gfx.mix(T.input, T.out, p));
                }
            } else if (ph == Phase.PREFIX && s.countIdx() > 0 && t < 0.7) {
                double p = Gfx.ease(t / 0.7);
                double x1 = cx(1, s.countIdx() - 1), x2 = cx(1, s.countIdx());
                double x = x1 + (x2 - x1) * p;
                double y = cellY[1] + cell / 2.0 - cell * 0.75 * Math.sin(Math.PI * p);
                drawChip(g, x, y, "+" + engine.count()[s.countIdx() - 1], T.count);
            }
        }
        drawLegend(g, W - padX, laneTop[0] + 15);
        g.dispose();
    }

    private void paintEmptyState(Graphics2D g, int w, int h) {
        Theme T = Theme.cur;
        g.setFont(Gfx.ui(Font.BOLD, 22f));
        g.setColor(T.text);
        Gfx.centerText(g, "Nothing to visualize yet", w / 2.0, h / 2.0 - 34);
        g.setFont(Gfx.ui(Font.PLAIN, 15f));
        g.setColor(T.muted);
        Gfx.centerText(g, "Type whole numbers above (any amount, negatives allowed),", w / 2.0, h / 2.0 + 2);
        Gfx.centerText(g, "load them from a file, or pick an example \u2013 then press Visualize.", w / 2.0, h / 2.0 + 26);
        // three little dashed boxes echo the three lanes
        g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{4f, 4f}, 0f));
        Color[] cs = {T.input, T.count, T.out};
        for (int i = 0; i < 3; i++) {
            g.setColor(Gfx.alpha(cs[i], 160));
            g.draw(new RoundRectangle2D.Double(w / 2.0 - 75 + i * 52, h / 2.0 - 100, 46, 46, 14, 14));
        }
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
        int c = Math.max(1, visible[lane]);
        double rowW = c * cell + (c - 1) * gap;
        double x0 = padX + gutter + (areaW - rowW) / 2.0;
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{6f, 6f}, 0f));
        g.setColor(Gfx.alpha(T.cellBorder, 170));
        g.draw(new RoundRectangle2D.Double(x0, cellY[lane], rowW, cell, 16, 16));
        g.setFont(Gfx.ui(Font.ITALIC, 13.5f));
        g.setColor(T.muted);
        Gfx.centerText(g, msg, x0 + rowW / 2, cellY[lane] + cell / 2.0);
    }

    /** Draws the visible cells of one lane plus their labels and the caret over the active cell. */
    private void drawLane(Graphics2D g, int lane, String[] txt, CellState[] st, double[] sc,
                          String[] l1, String[] l2, Color color, double alpha) {
        Theme T = Theme.cur;
        Composite old = g.getComposite();
        for (int d = 0; d < txt.length; d++) {
            double cxv = cxDisplay(lane, d), x = cxv - cell / 2.0, y = cellY[lane];
            drawCell(g, x, y, cell, txt[d], st[d], sc[d], color, alpha);
            if (alpha < 1) g.setComposite(AlphaComposite.SrcOver.derive((float) Gfx.clamp(alpha, 0, 1)));
            boolean act = st[d] == CellState.ACTIVE;
            Gfx.fitFont(g, l1[d], act ? Font.BOLD : Font.PLAIN, 12f, cell + gap - 2);
            g.setColor(act ? T.text : T.muted);
            Gfx.centerText(g, l1[d], cxv, y + cell + 12);
            if (l2 != null) {
                Gfx.fitFont(g, l2[d], Font.BOLD, 12f, cell + gap - 2);
                g.setFont(Gfx.mono(Font.BOLD, g.getFont().getSize2D()));
                g.setColor(act ? color : T.text);
                Gfx.centerText(g, l2[d], cxv, y + cell + 28);
            }
            if (act) {
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
        // "more to the left / right" markers
        g.setFont(Gfx.ui(Font.BOLD, 16f));
        g.setColor(T.muted);
        if (first[lane] > 0) g.drawString("\u2026", (float) (cxDisplay(lane, 0) - cell / 2.0 - 14), (float) (cellY[lane] + cell / 2.0 + 6));
        if (first[lane] + visible[lane] < size[lane]) {
            g.drawString("\u2026", (float) (cxDisplay(lane, visible[lane] - 1) + cell / 2.0 + 3), (float) (cellY[lane] + cell / 2.0 + 6));
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
        Stroke dash = new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{4f, 4f}, 0f);
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
            Gfx.fitFont(g, txt, Font.BOLD, (float) Gfx.clamp(s * 0.4, 11, 24), s - 8);
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
        Gfx.fitFont(g, txt, Font.BOLD, (float) Math.max(12, r * 0.9), 2 * r - 6);
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
                g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{3f, 3f}, 0f));
                g.draw(sw);
            }
            g.setColor(T.muted);
            g.setFont(Gfx.ui(Font.PLAIN, 12f));
            g.drawString(labels[kind], (float) (x + 21), (float) (cy + 4));
            x += 21 + fm.stringWidth(labels[kind]) + 18;
        }
    }
}
