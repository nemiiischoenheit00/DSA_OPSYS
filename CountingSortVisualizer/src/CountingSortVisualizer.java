import javax.swing.SwingUtilities;

/**
 * Counting Sort Visualizer - entry point.
 *
 * A graphical, step-by-step demonstration of counting sort for whole numbers (negatives
 * included). There is no fixed limit on how many numbers can be entered; see README.md
 * for the (memory-based) restrictions.
 *
 *   javac *.java
 *   java CountingSortVisualizer
 */
public final class CountingSortVisualizer {

    private CountingSortVisualizer() { }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainWindow().setVisible(true));
    }
}
