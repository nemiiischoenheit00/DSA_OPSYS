import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.util.function.IntConsumer;

/** Small drawing helpers shared by all custom components. */
final class Gfx {
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

    /** Shrinks the font (down to 8 pt) until the text fits in maxWidth. */
    static void fitFont(Graphics2D g, String s, int style, float base, double maxWidth) {
        float size = base;
        g.setFont(ui(style, size));
        while (size > 8f && g.getFontMetrics().stringWidth(s) > maxWidth) {
            size -= 1f;
            g.setFont(ui(style, size));
        }
    }
}

