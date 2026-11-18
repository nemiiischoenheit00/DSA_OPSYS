import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.List;

/** Runs the built-in test cases and shows the outcome in a table. */
final class SelfTestDialog {

    private static final String[] COLUMNS = {"#", "Category", "Test case", "Input", "Expected", "Actual", "Result"};

    private SelfTestDialog() { }

    static void show(Component parent) {
        parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        List<SelfTest.Result> results;
        try {
            results = SelfTest.runAll();
        } finally {
            parent.setCursor(Cursor.getDefaultCursor());
        }
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent), "Test cases");
        dialog.setContentPane(buildPanel(results, dialog::dispose));
        dialog.setSize(1100, 640);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    /** Builds the dialog content (separate from the JDialog so it can be rendered in tests). */
    static JPanel buildPanel(List<SelfTest.Result> results, Runnable onClose) {
        Theme t = Theme.cur;
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(t.bg);
        root.setBorder(BorderFactory.createEmptyBorder(16, 18, 14, 18));

        boolean allPassed = results.stream().allMatch(SelfTest.Result::passed);
        JLabel summary = new JLabel(SelfTest.summary(results));
        summary.setFont(Gfx.ui(Font.BOLD, 16f));
        summary.setForeground(allPassed ? t.out : t.danger);
        JLabel hint = new JLabel("Each accepted input is sorted by the visualizer's own engine, compared with "
                + "Arrays.sort, then stepped back to the start and forward again.");
        hint.setFont(Gfx.ui(Font.PLAIN, 12.5f));
        hint.setForeground(t.muted);
        JPanel top = new JPanel(new BorderLayout(0, 4));
        top.setOpaque(false);
        top.add(summary, BorderLayout.NORTH);
        top.add(hint, BorderLayout.CENTER);
        root.add(top, BorderLayout.NORTH);

        JTable table = new JTable(new AbstractTableModel() {
            @Override public int getRowCount() { return results.size(); }
            @Override public int getColumnCount() { return COLUMNS.length; }
            @Override public String getColumnName(int c) { return COLUMNS[c]; }
            @Override public Object getValueAt(int row, int col) {
                SelfTest.Result r = results.get(row);
                return switch (col) {
                    case 0 -> row + 1;
                    case 1 -> r.testCase().kind().label;
                    case 2 -> r.testCase().name();
                    case 3 -> r.testCase().inputLabel();
                    case 4 -> r.expected();
                    case 5 -> r.actual();
                    default -> r.passed() ? "PASS" : "FAIL";
                };
            }
        });
        table.setRowHeight(28);
        table.setFont(Gfx.ui(Font.PLAIN, 13f));
        table.setForeground(t.text);
        table.setBackground(t.surface);
        table.setGridColor(t.border);
        table.setShowVerticalLines(false);
        table.setSelectionBackground(t.accentSoft);
        table.setSelectionForeground(t.text);
        int[] widths = {34, 92, 230, 210, 230, 250, 64};
        for (int i = 0; i < widths.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tb, Object value, boolean sel, boolean focus,
                                                           int row, int col) {
                JLabel c = (JLabel) super.getTableCellRendererComponent(tb, value, sel, focus, row, col);
                c.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                c.setToolTipText(String.valueOf(value));
                // The renderer label is shared by all cells, so set colour and font explicitly every time.
                c.setFont(Gfx.ui(Font.PLAIN, 13f));
                c.setForeground(t.text);
                if (col == 6) {
                    c.setForeground("PASS".equals(value) ? t.out : t.danger);
                    c.setFont(Gfx.ui(Font.BOLD, 13f));
                } else if (col == 0 || col == 1) {
                    c.setForeground(t.muted);
                }
                return c;
            }
        };
        for (int i = 0; i < COLUMNS.length; i++) table.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);

        JTableHeader header = table.getTableHeader();
        header.setFont(Gfx.ui(Font.BOLD, 12.5f));
        header.setBackground(t.surface2);
        header.setForeground(t.muted);
        header.setReorderingAllowed(false);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(t.border));
        scroll.getViewport().setBackground(t.surface);
        root.add(scroll, BorderLayout.CENTER);

        FlatButton copy = new FlatButton("Copy report", null, FlatButton.Style.SECONDARY,
                "Copy the results as a Markdown table", () -> Toolkit.getDefaultToolkit().getSystemClipboard()
                        .setContents(new StringSelection(SelfTest.toMarkdown(results)), null));
        FlatButton close = new FlatButton("Close", null, FlatButton.Style.PRIMARY, "Close this window", onClose);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(copy);
        buttons.add(close);
        root.add(buttons, BorderLayout.SOUTH);
        return root;
    }
}
