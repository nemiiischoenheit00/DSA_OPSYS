import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.util.function.IntConsumer;

/** Segmented control (used for playback speed). */
final class Segmented extends Widget {
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

    @Override public Dimension getPreferredSize() { return new Dimension(labels.length * 48 + 8, 34); }
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
