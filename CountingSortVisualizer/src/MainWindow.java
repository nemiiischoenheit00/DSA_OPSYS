import javax.swing.*;
import java.awt.*;

/** Top-level window. */
final class MainWindow extends JFrame {
    static {
        // crisper text on most platforms
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
    }

    MainWindow() {
        super("Counting Sort Visualizer");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        AppPanel panel = new AppPanel();
        setContentPane(panel);
        setSize(1220, 840);
        setMinimumSize(new Dimension(1020, 780));
        setLocationRelativeTo(null);
        SwingUtilities.invokeLater(panel.field::requestFocusInWindow);
    }
}
