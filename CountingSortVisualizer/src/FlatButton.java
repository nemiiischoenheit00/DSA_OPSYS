import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.util.function.IntConsumer;

/** One button class for the whole app: primary / secondary / ghost / chip. */
final class FlatButton extends Widget {
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
            glyph.draw(g, x + gw / 2.0, h / 2.0, 15, fg);
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
