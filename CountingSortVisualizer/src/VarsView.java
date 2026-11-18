import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/** Live values of the variables used by the highlighted pseudocode. */
final class VarsView extends Widget {
    private Step step;

    VarsView() {
        setPreferredSize(new Dimension(320, 162));
        getAccessibleContext().setAccessibleName("Live variables");
    }

    void setStep(Step s) {
        step = s;
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
        g.drawString("LIVE VARIABLES", 18, 28);
        if (step != null) {
            Step s = step;
            boolean started = s.phase() != Phase.INTRO;
            String dash = "\u2013";
            long k = (long) s.max() - s.min() + 1;
            String[][] items = {
                {"min .. max", started ? s.min() + " .. " + s.max() : dash},
                {"k", started ? String.format("%,d", k) : dash},
                {"i", s.inIdx() >= 0 ? String.format("%,d", s.inIdx()) : dash},
                {"x = input[i]", s.inIdx() >= 0 ? String.valueOf(s.value()) : dash},
                {"j = x \u2212 min", s.countIdx() >= 0 ? String.format("%,d", s.countIdx()) : dash},
                {"count[j]", s.countIdx() >= 0 ? String.format("%,d", s.countAfter()) : dash}
            };
            double colW = (w - 36) / 2.0;
            for (int i = 0; i < items.length; i++) {
                double x = 18 + (i % 2) * colW, y = 42 + (i / 2) * 38;
                g.setFont(Gfx.ui(Font.PLAIN, 11.5f));
                g.setColor(t.muted);
                g.drawString(items[i][0], (float) x, (float) (y + 8));
                float size = 15f;                                   // shrink the monospace font until it fits
                g.setFont(Gfx.mono(Font.BOLD, size));
                while (size > 9f && g.getFontMetrics().stringWidth(items[i][1]) > colW - 12) {
                    size -= 1f;
                    g.setFont(Gfx.mono(Font.BOLD, size));
                }
                g.setColor(items[i][1].equals(dash) ? t.muted : t.text);
                g.drawString(items[i][1], (float) x, (float) (y + 26));
            }
        }
        g.dispose();
    }
}
