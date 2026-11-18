import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.util.function.IntConsumer;

/** Design tokens. Every colour in the GUI comes from here. */
final class Theme {
    final boolean dark;
    final Color bg, surface, surface2, border, text, muted, accent, accentSoft,
            input, count, out, danger, cell, cellBorder;

    Theme(boolean dark, int bg, int surface, int surface2, int border, int text, int muted,
          int accent, int accentSoft, int input, int count, int out, int danger,
          int cell, int cellBorder) {
        this.dark = dark;
        this.bg = new Color(bg);
        this.surface = new Color(surface);
        this.surface2 = new Color(surface2);
        this.border = new Color(border);
        this.text = new Color(text);
        this.muted = new Color(muted);
        this.accent = new Color(accent);
        this.accentSoft = new Color(accentSoft);
        this.input = new Color(input);
        this.count = new Color(count);
        this.out = new Color(out);
        this.danger = new Color(danger);
        this.cell = new Color(cell);
        this.cellBorder = new Color(cellBorder);
    }

    static final Theme LIGHT = new Theme(false, 0xF3F5FA, 0xFFFFFF, 0xEDF0F7, 0xD8DEEA,
            0x1B2333, 0x5A6678, 0x2F5BEA, 0xE2EAFE, 0xF2A20E, 0x7C4DFF, 0x0E9F6E, 0xD63A3A,
            0xFFFFFF, 0xBCC5D6);
    static final Theme DARK = new Theme(true, 0x0E1320, 0x171E2E, 0x1F2839, 0x2B3550,
            0xE7EBF3, 0x9AA7BC, 0x6C92FF, 0x1F2F5C, 0xF7B84B, 0xA78BFF, 0x3DD598, 0xFF6B6B,
            0x1D2638, 0x3B4765);
    static Theme cur = LIGHT;
}
