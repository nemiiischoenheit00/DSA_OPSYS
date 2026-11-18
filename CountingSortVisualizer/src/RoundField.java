import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.util.function.IntConsumer;

/** Text field with rounded border, placeholder and inline error state. */
final class RoundField extends JTextField {
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

    boolean hasError() { return error; }

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
