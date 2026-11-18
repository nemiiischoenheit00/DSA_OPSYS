import javax.accessibility.AccessibleRole;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;

/** Timeline with one tick per step; click or drag to jump anywhere. */
final class Scrubber extends Widget {
    private int max = 1;
    private int value;
    private boolean active;
    private IntFunction<String> tooltip = i -> "";
    private IntConsumer onChange = v -> { };

    Scrubber() {
        super(AccessibleRole.SLIDER);
        setPreferredSize(new Dimension(200, 34));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setToolTipText("Timeline");
        getAccessibleContext().setAccessibleName("Step timeline");
        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { if (active) onChange.accept(indexAt(e.getX())); }
            @Override public void mouseDragged(MouseEvent e) { if (active) onChange.accept(indexAt(e.getX())); }
            @Override public void mouseMoved(MouseEvent e) {
                if (active) setToolTipText(tooltip.apply(indexAt(e.getX())));
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    /** Enables the timeline for a run with the given number of steps. */
    void configure(int totalSteps, IntFunction<String> tooltipForStep, IntConsumer onChange) {
        this.max = Math.max(1, totalSteps - 1);
        this.tooltip = tooltipForStep;
        this.onChange = onChange;
        this.active = true;
        repaint();
    }

    void deactivate() {
        active = false;
        value = 0;
        repaint();
    }

    void setValue(int v) {
        value = v;
        repaint();
    }

    private int indexAt(int x) {
        double x0 = 14, x1 = getWidth() - 14;
        return (int) Math.round(Gfx.clamp((x - x0) / (x1 - x0), 0, 1) * max);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = Gfx.aa(g0);
        Theme t = Theme.cur;
        double x0 = 14, x1 = getWidth() - 14, cy = getHeight() / 2.0;
        g.setColor(t.border);
        g.fill(new RoundRectangle2D.Double(x0, cy - 3, x1 - x0, 6, 6, 6));
        if (!active) {
            g.dispose();
            return;
        }
        double xv = x0 + (x1 - x0) * value / max;
        g.setColor(t.accent);
        g.fill(new RoundRectangle2D.Double(x0, cy - 3, xv - x0, 6, 6, 6));
        if ((x1 - x0) / max >= 8) {                      // tick marks only when they are not crowded
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
