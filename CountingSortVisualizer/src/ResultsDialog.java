import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Shows the complete original input and the complete sorted output, with copy / save. */
final class ResultsDialog {

    private ResultsDialog() { }

    static void show(Component parent, int[] original) {
        int[] sorted = CountingSortEngine.sort(original);
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent), "Results");
        dialog.setContentPane(buildPanel(original, sorted, dialog::dispose, dialog));
        dialog.setSize(760, 560);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    /** Builds the dialog content (separate from the JDialog so it can also be tested headlessly). */
    static JPanel buildPanel(int[] original, int[] sorted, Runnable onClose, Component owner) {
        Theme t = Theme.cur;
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(t.bg);
        root.setBorder(BorderFactory.createEmptyBorder(16, 18, 14, 18));

        JLabel summary = new JLabel(String.format("%,d numbers   |   min %d   |   max %d   |   range k = %,d",
                original.length, sorted[0], sorted[sorted.length - 1],
                (long) sorted[sorted.length - 1] - sorted[0] + 1));
        summary.setFont(Gfx.ui(Font.BOLD, 14f));
        summary.setForeground(t.text);
        root.add(summary, BorderLayout.NORTH);

        JPanel texts = new JPanel(new GridLayout(2, 1, 0, 12));
        texts.setOpaque(false);
        texts.add(textBlock("Original input", InputParser.join(original), t));
        texts.add(textBlock("Sorted output (counting sort)", InputParser.join(sorted), t));
        root.add(texts, BorderLayout.CENTER);

        FlatButton copy = new FlatButton("Copy sorted", null, FlatButton.Style.SECONDARY,
                "Copy the sorted numbers to the clipboard", () -> {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(InputParser.join(sorted)), null);
        });
        FlatButton save = new FlatButton("Save sorted\u2026", null, FlatButton.Style.SECONDARY,
                "Save the sorted numbers to a text file", () -> saveToFile(owner, sorted));
        FlatButton close = new FlatButton("Close", null, FlatButton.Style.PRIMARY, "Close this window", onClose);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(copy);
        buttons.add(save);
        buttons.add(close);
        root.add(buttons, BorderLayout.SOUTH);
        return root;
    }

    private static JComponent textBlock(String title, String content, Theme t) {
        JLabel label = new JLabel(title);
        label.setFont(Gfx.ui(Font.BOLD, 13f));
        label.setForeground(t.muted);
        label.setBorder(BorderFactory.createEmptyBorder(0, 2, 6, 0));

        JTextArea area = new JTextArea(content);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(Gfx.mono(Font.PLAIN, 13f));
        area.setBackground(t.surface);
        area.setForeground(t.text);
        area.setCaretPosition(0);
        area.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        JScrollPane scroll = new JScrollPane(area);
        scroll.setBorder(BorderFactory.createLineBorder(t.border));

        JPanel box = new JPanel(new BorderLayout());
        box.setOpaque(false);
        box.add(label, BorderLayout.NORTH);
        box.add(scroll, BorderLayout.CENTER);
        return box;
    }

    private static void saveToFile(Component owner, int[] sorted) {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("sorted.txt"));
        if (chooser.showSaveDialog(owner) != JFileChooser.APPROVE_OPTION) return;
        try {
            Files.writeString(chooser.getSelectedFile().toPath(), InputParser.join(sorted) + "\n",
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(owner, "Could not save the file: " + e.getMessage(),
                    "Save failed", JOptionPane.ERROR_MESSAGE);
        }
    }
}
