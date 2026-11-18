import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.util.function.IntConsumer;

/** Vector icons, so the UI never depends on emoji or symbol fonts. */
enum Glyph {
    PLAY, PAUSE, NEXT, PREV, RESTART, SUN, MOON, CHECK;

    /** Draws this icon centred at (cx, cy) with the given size and colour. */
    void draw(Graphics2D g, double cx, double cy, double s, Color c) {
        g.setColor(c);
        g.setStroke(new BasicStroke((float) (s * 0.13), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        double h = s / 2;
        switch (this) {
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
}
