import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.util.function.IntConsumer;

/** Base for custom-painted widgets: plain JComponent has no accessible context, so provide one. */
class Widget extends JComponent {
    private final AccessibleRole role;
    private AccessibleContext ac;

    Widget() { this(AccessibleRole.PANEL); }
    Widget(AccessibleRole role) { this.role = role; }

    @Override
    public AccessibleContext getAccessibleContext() {
        if (ac == null) {
            ac = new AccessibleJComponent() {
                @Override public AccessibleRole getAccessibleRole() { return role; }
            };
        }
        return ac;
    }
}
