import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.util.function.IntConsumer;

/** Pseudocode with the currently executing lines highlighted. */
final class CodeView extends Widget {
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
        if (s == null) {
            main = Set.of();
            ctx = Set.of();
            repaint();
            return;
        }
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
