import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.util.function.IntConsumer;

/** Five-stage progress indicator; click a stage to jump to it. */
final class PhaseStepper extends Widget {
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
                Glyph.CHECK.draw(g, cx, cy, 14, Gfx.onColor(t.accent));
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
