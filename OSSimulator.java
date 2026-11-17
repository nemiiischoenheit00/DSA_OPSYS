import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Supplier;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;

public class OSSimulator extends JFrame {

    private static final Color ACCENT = new Color(33, 97, 140);
    private static final Color OK_BG = new Color(212, 239, 223);
    private static final Color OK_FG = new Color(20, 90, 50);
    private static final Color ERR_BG = new Color(250, 219, 216);
    private static final Color ERR_FG = new Color(120, 40, 31);
    private static final Font BOLD = new Font(Font.SANS_SERIF, Font.BOLD, 12);

    private interface Task {
        void run() throws Exception;
    }

    private final JLabel statusBar = new JLabel(" Ready. Pick a module, enter data, then click Run.");

    public OSSimulator() {
        super("Operating System Simulator");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 700);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);

        JLabel title = new JLabel("Operating System Simulator");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        title.setForeground(Color.WHITE);
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(ACCENT);
        header.setBorder(new EmptyBorder(10, 14, 10, 14));
        header.add(title, BorderLayout.WEST);

        statusBar.setOpaque(true);
        statusBar.setBorder(new EmptyBorder(6, 10, 6, 10));
        setStatus("Ready. Pick a module, enter data, then click Run.", false);

        JTabbedPane tabs = new JTabbedPane(JTabbedPane.LEFT);
        tabs.addTab("CPU Scheduling", cpuTab());
        tabs.addTab("Banker's Algorithm", bankersTab());
        tabs.addTab("Paging", pagingTab());
        tabs.addTab("Page Replacement", pageReplacementTab());
        tabs.addTab("Segmentation", segmentationTab());
        tabs.addTab("Fragmentation", fragmentationTab());
        tabs.addTab("Disk Scheduling", diskTab());
        tabs.addTab("Multiprogramming", multiprogrammingTab());

        add(header, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);
    }

    private void setStatus(String msg, boolean error) {
        statusBar.setText(" " + (error ? "Error: " : "") + msg);
        statusBar.setBackground(error ? ERR_BG : OK_BG);
        statusBar.setForeground(error ? ERR_FG : OK_FG);
    }

    private void showError(Exception ex) {
        String msg = (ex instanceof OSAlgorithms.InputError || ex instanceof IllegalArgumentException)
                ? ex.getMessage() : "Unexpected error: " + ex;
        setStatus(msg, true);
        JOptionPane.showMessageDialog(this, msg, "Invalid input", JOptionPane.WARNING_MESSAGE);
    }

    private static void load(Task t) {
        try {
            t.run();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private JButton button(String text, Task task) {
        JButton b = new JButton(text);
        b.addActionListener(e -> {
            try {
                task.run();
            } catch (Exception ex) {
                showError(ex);
            }
        });
        return b;
    }

    private JButton runButton(String text, Task task) {
        JButton b = button(text, task);
        b.setFont(BOLD);
        return b;
    }

    private static JPanel box(String title, JComponent content) {
        JPanel p = new JPanel(new BorderLayout(4, 4));
        p.setBorder(BorderFactory.createCompoundBorder(new TitledBorder(title), new EmptyBorder(4, 6, 6, 6)));
        p.add(content, BorderLayout.CENTER);
        return p;
    }

    private static JPanel row(Component... items) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 3));
        for (Component c : items) p.add(c);
        return p;
    }

    private static JPanel fieldRow(String label, JTextField field, Component... buttons) {
        JPanel p = new JPanel(new BorderLayout(6, 0));
        p.setBorder(new EmptyBorder(3, 6, 3, 6));
        p.add(new JLabel(label), BorderLayout.WEST);
        p.add(field, BorderLayout.CENTER);
        if (buttons.length > 0) p.add(row(buttons), BorderLayout.EAST);
        return p;
    }

    private static JPanel stack(JComponent... rows) {
        JPanel p = new JPanel(new GridLayout(rows.length, 1));
        for (JComponent r : rows) p.add(r);
        return p;
    }

    private static DefaultTableModel model(int firstEditableCol, Object... cols) {
        return new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return c >= firstEditableCol;
            }
        };
    }

    private static JTable table(DefaultTableModel m) {
        JTable t = new JTable(m);
        t.setRowHeight(22);
        t.setFillsViewportHeight(true);
        t.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        return t;
    }

    private static JScrollPane scroll(JTable t) {
        return new JScrollPane(t);
    }

    private static JPanel pane(JComponent north, JComponent center, JComponent south) {
        JPanel p = new JPanel(new BorderLayout(4, 4));
        if (north != null) p.add(north, BorderLayout.NORTH);
        p.add(center, BorderLayout.CENTER);
        if (south != null) p.add(south, BorderLayout.SOUTH);
        return p;
    }

    private JPanel editButtons(JTable t, DefaultTableModel m, Supplier<Object[]> newRow) {
        return row(
                button("Add", () -> m.addRow(newRow.get())),
                button("Remove Selected", () -> {
                    if (t.isEditing()) t.getCellEditor().stopCellEditing();
                    int[] sel = t.getSelectedRows();
                    for (int i = sel.length - 1; i >= 0; i--) m.removeRow(sel[i]);
                }),
                button("Clear", () -> m.setRowCount(0)));
    }

    private static String cell(DefaultTableModel m, int r, int c) {
        return String.valueOf(m.getValueAt(r, c));
    }

    private static JLabel info(String text) {
        JLabel l = new JLabel(text);
        l.setFont(BOLD);
        l.setBorder(new EmptyBorder(4, 2, 2, 2));
        return l;
    }

    private static JPanel tab(JComponent north, JComponent center) {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(new EmptyBorder(8, 8, 8, 8));
        p.add(north, BorderLayout.NORTH);
        p.add(center, BorderLayout.CENTER);
        return p;
    }

    private JPanel cpuTab() {
        DefaultTableModel in = model(0, "PID", "Arrival", "Burst", "Priority");
        JTable inTable = table(in);
        DefaultTableModel out = model(Integer.MAX_VALUE,
                "PID", "Arrival", "Burst", "Priority", "Completion", "Turnaround", "Waiting", "Response");
        JTable outTable = table(out);

        JComboBox<String> algoBox = new JComboBox<>(OSAlgorithms.CPU_ALGOS);
        JTextField quantum = new JTextField("2", 4);
        JLabel metrics = info("Average waiting / turnaround / response and CPU utilization appear here.");
        GanttPanel gantt = new GanttPanel();
        JScrollPane ganttScroll = new JScrollPane(gantt,
                JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        ganttScroll.setPreferredSize(new Dimension(0, 100));

        algoBox.addActionListener(e -> quantum.setEnabled("Round Robin".equals(algoBox.getSelectedItem())));
        quantum.setEnabled(false);

        Task small = () -> {
            in.setRowCount(0);
            in.addRow(new Object[]{"P1", "0", "5", "2"});
            in.addRow(new Object[]{"P2", "1", "3", "1"});
            in.addRow(new Object[]{"P3", "2", "8", "4"});
            in.addRow(new Object[]{"P4", "3", "6", "3"});
            quantum.setText("2");
            setStatus("Loaded small example (4 processes).", false);
        };
        Task large = () -> {
            in.setRowCount(0);
            Random rnd = new Random(42);
            for (int i = 1; i <= 25; i++)
                in.addRow(new Object[]{"P" + i, rnd.nextInt(15), 1 + rnd.nextInt(12), 1 + rnd.nextInt(5)});
            quantum.setText("3");
            setStatus("Loaded large example (25 processes).", false);
        };
        Task run = () -> {
            List<OSAlgorithms.Proc> procs = new ArrayList<>();
            for (int i = 0; i < in.getRowCount(); i++) {
                String pid = cell(in, i, 0).trim();
                if (pid.isEmpty()) throw new OSAlgorithms.InputError("Row " + (i + 1) + ": PID is blank.");
                int at = OSAlgorithms.toInt(cell(in, i, 1), "Row " + (i + 1) + " Arrival", 0, null);
                int bt = OSAlgorithms.toInt(cell(in, i, 2), "Row " + (i + 1) + " Burst", 1, null);
                int pr = OSAlgorithms.toInt(cell(in, i, 3), "Row " + (i + 1) + " Priority", null, null);
                procs.add(new OSAlgorithms.Proc(pid, at, bt, pr));
            }
            String algo = (String) algoBox.getSelectedItem();
            int q = "Round Robin".equals(algo) ? OSAlgorithms.toInt(quantum.getText(), "Time Quantum", 1, null) : 1;
            OSAlgorithms.CpuResult res = OSAlgorithms.cpuSchedule(procs, algo, q);
            gantt.setSegments(res.gantt);
            out.setRowCount(0);
            for (OSAlgorithms.CpuRow r : res.rows)
                out.addRow(new Object[]{r.pid, r.at, r.bt, r.pr, r.ct, r.tat, r.wt, r.rt});
            metrics.setText(String.format("Avg Waiting: %.2f   Avg Turnaround: %.2f   Avg Response: %.2f   CPU Utilization: %.2f%%",
                    res.avgWt, res.avgTat, res.avgRt, res.util));
            setStatus(algo + " finished for " + procs.size() + " processes.", false);
        };
        load(small);

        JPanel controls = stack(
                row(new JLabel("Algorithm:"), algoBox, new JLabel("Time quantum:"), quantum,
                        button("Small Example", small), button("Large Example", large), runButton("Run", run)),
                editButtons(inTable, in, () -> new Object[]{"P" + (in.getRowCount() + 1), "0", "4", "1"}));

        JPanel input = box("Processes (lower priority number = higher priority)",
                pane(controls, scroll(inTable), null));

        JPanel results = pane(ganttScroll, scroll(outTable), metrics);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, input, box("Results", results));
        split.setResizeWeight(0.4);
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new EmptyBorder(8, 8, 8, 8));
        p.add(split, BorderLayout.CENTER);
        return p;
    }

    private static class GanttPanel extends JPanel {
        private List<OSAlgorithms.Seg> segs = new ArrayList<>();

        GanttPanel() {
            setPreferredSize(new Dimension(600, 80));
        }

        void setSegments(List<OSAlgorithms.Seg> list) {
            segs = list == null ? new ArrayList<>() : list;
            int span = segs.isEmpty() ? 0 : segs.get(segs.size() - 1).end - segs.get(0).start;
            setPreferredSize(new Dimension(Math.max(600, span * 28 + 40), 80));
            revalidate();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (segs.isEmpty()) {
                g.setColor(Color.GRAY);
                g.drawString("Gantt chart appears here after you click Run.", 12, 40);
                return;
            }
            int t0 = segs.get(0).start;
            int tEnd = segs.get(segs.size() - 1).end;
            int span = Math.max(1, tEnd - t0);
            int margin = 16, y = 14, h = 36;
            int width = Math.max(200, getWidth() - 2 * margin);
            for (OSAlgorithms.Seg s : segs) {
                int x1 = margin + (int) ((long) (s.start - t0) * width / span);
                int x2 = margin + (int) ((long) (s.end - t0) * width / span);
                g.setColor("Idle".equals(s.pid) ? Color.LIGHT_GRAY
                        : Color.getHSBColor((float) ((Math.abs((long) s.pid.hashCode()) * 0.618034) % 1.0), 0.4f, 0.95f));
                g.fillRect(x1, y, x2 - x1, h);
                g.setColor(Color.DARK_GRAY);
                g.drawRect(x1, y, x2 - x1, h);
                if (x2 - x1 >= 24) g.drawString(s.pid, x1 + 4, y + 22);
                g.drawString(String.valueOf(s.start), x1, y + h + 16);
            }
            g.drawString(String.valueOf(tEnd), margin + width - 6, y + h + 16);
        }
    }

    private JPanel bankersTab() {
        JTextField nField = new JTextField("4", 3);
        JTextField mField = new JTextField("3", 3);
        JTextField availField = new JTextField("3 3 2", 14);
        JTextField reqPid = new JTextField("1", 3);
        JTextField reqVec = new JTextField("1 0 2", 10);

        DefaultTableModel alloc = model(1, "Process");
        DefaultTableModel max = model(1, "Process");
        DefaultTableModel need = model(Integer.MAX_VALUE, "Process");
        DefaultTableModel steps = model(Integer.MAX_VALUE, "Step", "Process", "Work Before", "Work After");

        JLabel banner = new JLabel("Click \"Check Safety\" to compute Need and find a safe sequence.");
        banner.setOpaque(true);
        banner.setFont(BOLD);
        banner.setBorder(new EmptyBorder(6, 8, 6, 8));

        Task resize = () -> {
            int n = OSAlgorithms.toInt(nField.getText(), "Processes", 1, null);
            int m = OSAlgorithms.toInt(mField.getText(), "Resource types", 1, null);
            String[] cols = new String[m + 1];
            cols[0] = "Process";
            for (int j = 1; j <= m; j++) cols[j] = "R" + j;
            for (DefaultTableModel d : new DefaultTableModel[]{alloc, max, need}) {
                d.setRowCount(0);
                d.setColumnIdentifiers(cols);
            }
            for (int i = 0; i < n; i++) {
                Object[] a = new Object[m + 1], b = new Object[m + 1];
                a[0] = "P" + i;
                b[0] = "P" + i;
                for (int j = 1; j <= m; j++) {
                    a[j] = "0";
                    b[j] = "0";
                }
                alloc.addRow(a);
                max.addRow(b);
            }
        };
        Task small = () -> {
            nField.setText("4");
            mField.setText("3");
            availField.setText("3 3 2");
            reqPid.setText("1");
            reqVec.setText("1 0 2");
            resize.run();
            int[][] a = {{0, 1, 0}, {2, 0, 0}, {3, 0, 2}, {2, 1, 1}};
            int[][] x = {{7, 5, 3}, {3, 2, 2}, {9, 0, 2}, {2, 2, 2}};
            for (int i = 0; i < 4; i++)
                for (int j = 0; j < 3; j++) {
                    alloc.setValueAt(String.valueOf(a[i][j]), i, j + 1);
                    max.setValueAt(String.valueOf(x[i][j]), i, j + 1);
                }
            setStatus("Loaded small example (4 processes, 3 resources).", false);
        };
        Task large = () -> {
            nField.setText("12");
            mField.setText("5");
            availField.setText("10 12 9 11 8");
            reqPid.setText("1");
            reqVec.setText("1 0 0 1 0");
            resize.run();
            Random rnd = new Random(7);
            for (int i = 0; i < 12; i++)
                for (int j = 0; j < 5; j++) {
                    int a = rnd.nextInt(3);
                    alloc.setValueAt(String.valueOf(a), i, j + 1);
                    max.setValueAt(String.valueOf(a + rnd.nextInt(4)), i, j + 1);
                }
            setStatus("Loaded large example (12 processes, 5 resources).", false);
        };
        load(small);

        JTable allocTable = table(alloc), maxTable = table(max), needTable = table(need), stepTable = table(steps);

        Supplier<int[][][]> read = () -> {
            int n = alloc.getRowCount(), m = alloc.getColumnCount() - 1;
            int[][] a = new int[n][m], x = new int[n][m];
            for (int i = 0; i < n; i++)
                for (int j = 0; j < m; j++) {
                    a[i][j] = OSAlgorithms.toInt(cell(alloc, i, j + 1), "Allocation P" + i + " R" + (j + 1), 0, null);
                    x[i][j] = OSAlgorithms.toInt(cell(max, i, j + 1), "Max P" + i + " R" + (j + 1), 0, null);
                }
            return new int[][][]{a, x};
        };
        Supplier<int[]> readAvail = () -> {
            int m = alloc.getColumnCount() - 1;
            int[] v = OSAlgorithms.parseList(availField.getText(), "Available", 0);
            if (v.length != m)
                throw new OSAlgorithms.InputError("Available needs " + m + " values (you entered " + v.length + ").");
            return v;
        };

        Task check = () -> {
            int[][][] data = read.get();
            int n = data[0].length, m = alloc.getColumnCount() - 1;
            OSAlgorithms.BankerResult br = OSAlgorithms.bankers(data[0], data[1], readAvail.get());
            need.setRowCount(0);
            for (int i = 0; i < n; i++) {
                Object[] r = new Object[m + 1];
                r[0] = "P" + i;
                for (int j = 0; j < m; j++) r[j + 1] = br.need[i][j];
                need.addRow(r);
            }
            steps.setRowCount(0);
            int k = 1;
            for (OSAlgorithms.BStep s : br.steps)
                steps.addRow(new Object[]{k++, "P" + s.proc, Arrays.toString(s.before), Arrays.toString(s.after)});
            if (br.safe) {
                List<String> names = new ArrayList<>();
                for (int id : br.sequence) names.add("P" + id);
                banner.setText("SAFE. Safe sequence: " + String.join(" -> ", names));
            } else {
                banner.setText("UNSAFE. No safe sequence exists. Stuck processes: " + br.stuck);
            }
            banner.setBackground(br.safe ? OK_BG : ERR_BG);
            banner.setForeground(br.safe ? OK_FG : ERR_FG);
            setStatus("Banker's Algorithm: system is " + (br.safe ? "safe." : "unsafe."), false);
        };
        Task request = () -> {
            int[][][] data = read.get();
            int pid = OSAlgorithms.toInt(reqPid.getText(), "Request process", 0, data[0].length - 1);
            int[] req = OSAlgorithms.parseList(reqVec.getText(), "Request", 0);
            OSAlgorithms.RequestResult rr = OSAlgorithms.bankersRequest(data[0], data[1], readAvail.get(), pid, req);
            banner.setText(rr.message);
            banner.setBackground(rr.granted ? OK_BG : ERR_BG);
            banner.setForeground(rr.granted ? OK_FG : ERR_FG);
            setStatus(rr.message, !rr.granted);
        };

        JPanel controls = stack(
                row(new JLabel("Processes:"), nField, new JLabel("Resource types:"), mField,
                        button("Create Tables", () -> {
                            resize.run();
                            setStatus("Created empty tables. Fill in Allocation and Max.", false);
                        }),
                        new JLabel("Available:"), availField,
                        button("Small Example", small), button("Large Example", large)),
                row(runButton("Check Safety", check), new JLabel("   Request: process"), reqPid,
                        new JLabel("resources"), reqVec, button("Test Request", request)));

        JPanel matrices = new JPanel(new GridLayout(1, 3, 8, 0));
        matrices.add(box("Allocation", scroll(allocTable)));
        matrices.add(box("Maximum", scroll(maxTable)));
        matrices.add(box("Need (Max - Allocation)", scroll(needTable)));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, matrices,
                box("Result and Safety Steps", pane(banner, scroll(stepTable), null)));
        split.setResizeWeight(0.5);
        return tab(controls, split);
    }

    private JPanel pagingTab() {
        JTextField pageSize = new JTextField("1024", 6);
        JTextField addrs = new JTextField();
        DefaultTableModel pt = model(0, "Page", "Frame");
        JTable ptTable = table(pt);
        DefaultTableModel out = model(Integer.MAX_VALUE,
                "Logical Address", "Page", "Offset", "Frame", "Physical Address", "Status");
        JTable outTable = table(out);
        JLabel summary = info("Translations: 0   Valid: 0   Page faults: 0");

        Task small = () -> {
            pageSize.setText("1024");
            addrs.setText("0, 500, 1024, 2048, 4096");
            pt.setRowCount(0);
            pt.addRow(new Object[]{"0", "5"});
            pt.addRow(new Object[]{"1", "2"});
            pt.addRow(new Object[]{"2", "9"});
            setStatus("Loaded small example.", false);
        };
        Task large = () -> {
            pageSize.setText("256");
            pt.setRowCount(0);
            for (int p = 0; p < 20; p++) if (p % 4 != 3) pt.addRow(new Object[]{p, 30 - p});
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 30; i++) sb.append(i > 0 ? ", " : "").append(i * 190 + 37);
            addrs.setText(sb.toString());
            setStatus("Loaded large example (16 mapped pages, 30 addresses).", false);
        };
        Task run = () -> {
            int size = OSAlgorithms.toInt(pageSize.getText(), "Page size", 1, null);
            int[] list = OSAlgorithms.parseList(addrs.getText(), "Logical addresses", 0);
            Map<Integer, Integer> map = new LinkedHashMap<>();
            for (int i = 0; i < pt.getRowCount(); i++) {
                int p = OSAlgorithms.toInt(cell(pt, i, 0), "Row " + (i + 1) + " Page", 0, null);
                int f = OSAlgorithms.toInt(cell(pt, i, 1), "Row " + (i + 1) + " Frame", 0, null);
                map.put(p, f);
            }
            List<Object[]> rows = OSAlgorithms.pagingTranslate(size, map, list);
            out.setRowCount(0);
            int ok = 0;
            for (Object[] r : rows) {
                out.addRow(r);
                if ("OK".equals(r[5])) ok++;
            }
            summary.setText("Translations: " + rows.size() + "   Valid: " + ok + "   Page faults: " + (rows.size() - ok));
            setStatus("Paging translation finished.", false);
        };
        load(small);

        JPanel controls = stack(
                row(new JLabel("Page size:"), pageSize, button("Small Example", small),
                        button("Large Example", large), runButton("Translate", run)),
                fieldRow("Logical addresses:", addrs));

        JPanel left = box("Page Table",
                pane(editButtons(ptTable, pt, () -> new Object[]{pt.getRowCount(), "0"}), scroll(ptTable), null));
        left.setPreferredSize(new Dimension(300, 0));

        JPanel right = box("Address Translation", pane(null, scroll(outTable), summary));

        JPanel center = new JPanel(new BorderLayout(8, 8));
        center.add(left, BorderLayout.WEST);
        center.add(right, BorderLayout.CENTER);
        return tab(controls, center);
    }

    private JPanel pageReplacementTab() {
        JComboBox<String> algoBox = new JComboBox<>(OSAlgorithms.PR_ALGOS);
        JTextField frames = new JTextField("3", 4);
        JTextField refs = new JTextField();
        DefaultTableModel out = model(Integer.MAX_VALUE, "Step", "Page", "Result", "Replaced", "Frames");
        JTable outTable = table(out);
        JLabel stats = info("References: 0   Faults: 0   Hits: 0   Hit ratio: 0.00%");

        Task small = () -> {
            frames.setText("3");
            refs.setText("7 0 1 2 0 3 0 4 2 3 0 3 2");
            setStatus("Loaded small example.", false);
        };
        Task large = () -> {
            frames.setText("5");
            Random rnd = new Random(19);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 40; i++) sb.append(i > 0 ? " " : "").append(rnd.nextInt(9));
            refs.setText(sb.toString());
            setStatus("Loaded large example (40 references, 5 frames).", false);
        };
        Task run = () -> {
            String algo = (String) algoBox.getSelectedItem();
            int nf = OSAlgorithms.toInt(frames.getText(), "Frames", 1, null);
            int[] list = OSAlgorithms.parseList(refs.getText(), "Reference string", 0);
            OSAlgorithms.PRResult res = OSAlgorithms.pageReplace(list, nf, algo);
            out.setRowCount(0);
            for (OSAlgorithms.PRStep s : res.steps)
                out.addRow(new Object[]{s.index, s.page, s.hit ? "HIT" : "FAULT",
                        s.replaced < 0 ? "-" : s.replaced, Arrays.toString(s.slots)});
            int hits = list.length - res.faults;
            stats.setText(String.format("References: %d   Faults: %d   Hits: %d   Hit ratio: %.2f%%",
                    list.length, res.faults, hits, 100.0 * hits / list.length));
            setStatus(algo + " finished.", false);
        };
        load(small);

        JPanel controls = stack(
                row(new JLabel("Algorithm:"), algoBox, new JLabel("Frames:"), frames,
                        button("Small Example", small), button("Large Example", large), runButton("Run", run)),
                fieldRow("Reference string:", refs));

        return tab(controls, box("Frame Changes, Hits and Faults", pane(null, scroll(outTable), stats)));
    }

    private JPanel segmentationTab() {
        DefaultTableModel seg = model(0, "Segment", "Base", "Limit");
        DefaultTableModel req = model(0, "Segment", "Offset");
        DefaultTableModel out = model(Integer.MAX_VALUE, "Segment", "Offset", "Limit", "Base", "Physical Address", "Status");
        JTable segTable = table(seg), reqTable = table(req), outTable = table(out);
        JLabel overlap = info("Overlap check: not run yet.");

        Task small = () -> {
            seg.setRowCount(0);
            seg.addRow(new Object[]{"0", "1000", "400"});
            seg.addRow(new Object[]{"1", "2000", "200"});
            seg.addRow(new Object[]{"2", "3000", "500"});
            req.setRowCount(0);
            req.addRow(new Object[]{"0", "120"});
            req.addRow(new Object[]{"1", "250"});
            req.addRow(new Object[]{"2", "100"});
            req.addRow(new Object[]{"5", "10"});
            setStatus("Loaded small example.", false);
        };
        Task large = () -> {
            seg.setRowCount(0);
            req.setRowCount(0);
            for (int s = 0; s < 15; s++) {
                seg.addRow(new Object[]{s, s * 1000, 500 + (s % 4) * 100});
                req.addRow(new Object[]{s, (s * 73) % 900});
            }
            setStatus("Loaded large example (15 segments, 15 requests).", false);
        };
        Task run = () -> {
            Map<Integer, long[]> segs = new LinkedHashMap<>();
            for (int i = 0; i < seg.getRowCount(); i++) {
                int id = OSAlgorithms.toInt(cell(seg, i, 0), "Row " + (i + 1) + " Segment", 0, null);
                int base = OSAlgorithms.toInt(cell(seg, i, 1), "Row " + (i + 1) + " Base", 0, null);
                int limit = OSAlgorithms.toInt(cell(seg, i, 2), "Row " + (i + 1) + " Limit", 1, null);
                segs.put(id, new long[]{base, limit});
            }
            List<int[]> reqs = new ArrayList<>();
            for (int i = 0; i < req.getRowCount(); i++) {
                int id = OSAlgorithms.toInt(cell(req, i, 0), "Request " + (i + 1) + " Segment", 0, null);
                int off = OSAlgorithms.toInt(cell(req, i, 1), "Request " + (i + 1) + " Offset", 0, null);
                reqs.add(new int[]{id, off});
            }
            List<String> overlaps = OSAlgorithms.segOverlaps(segs);
            overlap.setText(overlaps.isEmpty() ? "Overlap check: no overlapping segments."
                    : "Overlap warning: " + String.join("; ", overlaps));
            out.setRowCount(0);
            for (Object[] r : OSAlgorithms.segTranslate(segs, reqs)) out.addRow(r);
            setStatus("Segmentation finished.", false);
        };
        load(small);

        JPanel segBox = box("Segment Table",
                pane(editButtons(segTable, seg, () -> new Object[]{seg.getRowCount(), "0", "256"}), scroll(segTable), null));
        JPanel reqBox = box("Logical Addresses (segment, offset)",
                pane(editButtons(reqTable, req, () -> new Object[]{"0", "0"}), scroll(reqTable), null));

        JPanel left = new JPanel(new GridLayout(2, 1, 0, 8));
        left.add(segBox);
        left.add(reqBox);
        left.setPreferredSize(new Dimension(340, 0));

        JPanel right = box("Translation Results", pane(null, scroll(outTable), overlap));

        JPanel center = new JPanel(new BorderLayout(8, 8));
        center.add(left, BorderLayout.WEST);
        center.add(right, BorderLayout.CENTER);
        return tab(row(button("Small Example", small), button("Large Example", large),
                runButton("Validate and Translate", run)), center);
    }

    private JPanel fragmentationTab() {
        JTextField blocks = new JTextField(30);
        JTextField procs = new JTextField(30);
        JComboBox<String> strategy = new JComboBox<>(OSAlgorithms.FRAG_STRATEGIES);
        JComboBox<String> mode = new JComboBox<>(new String[]{
                "Fixed partitions (internal fragmentation)", "Dynamic split (external fragmentation)"});
        DefaultTableModel alloc = model(Integer.MAX_VALUE, "Process", "Size", "Block", "Internal Frag.", "Status");
        DefaultTableModel mem = model(Integer.MAX_VALUE, "Block", "Initial Size", "Remaining Free");
        JTable allocTable = table(alloc), memTable = table(mem);
        JLabel summary = info("Internal / external fragmentation totals appear here.");

        Task small = () -> {
            blocks.setText("100 500 200 300 600");
            procs.setText("212 417 112 426");
            setStatus("Loaded small example.", false);
        };
        Task large = () -> {
            blocks.setText("120 250 80 400 160 500 90 300 220 600 150 350");
            procs.setText("110 230 95 310 140 280 75 410 190 200 130 175");
            setStatus("Loaded large example (12 blocks, 12 processes).", false);
        };
        Task run = () -> {
            int[] b = OSAlgorithms.parseList(blocks.getText(), "Memory blocks", 1);
            int[] p = OSAlgorithms.parseList(procs.getText(), "Processes", 1);
            String strat = (String) strategy.getSelectedItem();
            OSAlgorithms.FragResult res = OSAlgorithms.allocate(b, p, strat, mode.getSelectedIndex() == 1);
            alloc.setRowCount(0);
            for (int i = 0; i < p.length; i++) {
                int blk = res.assign[i];
                String status = blk >= 0 ? "Allocated"
                        : res.extProblem.contains(i) ? "Not allocated (external fragmentation)"
                        : "Not allocated (too large)";
                alloc.addRow(new Object[]{"P" + i, p[i], blk < 0 ? "-" : "Block " + blk + " (" + b[blk] + ")",
                        res.internal[i], status});
            }
            mem.setRowCount(0);
            for (int i = 0; i < b.length; i++) mem.addRow(new Object[]{"Block " + i, b[i], res.remaining[i]});
            summary.setText(String.format("Internal fragmentation: %d   External fragmentation: %d   Unallocated: %d   Blocked by external fragmentation: %d",
                    res.internalTotal, res.externalTotal, res.unallocated.size(), res.extProblem.size()));
            setStatus(strat + " allocation finished.", false);
        };
        load(small);

        JPanel controls = stack(
                row(new JLabel("Memory blocks:"), blocks, new JLabel("Processes:"), procs),
                row(new JLabel("Strategy:"), strategy, new JLabel("Mode:"), mode,
                        button("Small Example", small), button("Large Example", large), runButton("Run", run)));

        JPanel tables = new JPanel(new GridLayout(1, 2, 8, 0));
        tables.add(box("Process Allocation", scroll(allocTable)));
        tables.add(box("Memory Blocks", scroll(memTable)));

        return tab(controls, pane(null, tables, summary));
    }

    private JPanel diskTab() {
        JComboBox<String> algoBox = new JComboBox<>(OSAlgorithms.DISK_ALGOS);
        JTextField size = new JTextField("200", 5);
        JTextField head = new JTextField("53", 5);
        JTextField reqs = new JTextField();
        JComboBox<String> dir = new JComboBox<>(new String[]{"Right (increasing)", "Left (decreasing)"});
        JCheckBox jump = new JCheckBox("Count circular jump", true);
        DefaultTableModel out = model(Integer.MAX_VALUE, "Step", "From", "To", "Type", "Distance");
        JTable outTable = table(out);
        JLabel summary = info("Total head movement and service order appear here.");
        DiskChart chart = new DiskChart();
        chart.setPreferredSize(new Dimension(360, 0));

        algoBox.addActionListener(e -> {
            int i = algoBox.getSelectedIndex();
            dir.setEnabled(i >= 2);
            jump.setEnabled(i == 3 || i == 5);
        });
        dir.setEnabled(false);
        jump.setEnabled(false);

        Task small = () -> {
            size.setText("200");
            head.setText("53");
            reqs.setText("98 183 37 122 14 124 65 67");
            dir.setSelectedIndex(0);
            setStatus("Loaded small example.", false);
        };
        Task large = () -> {
            size.setText("500");
            head.setText("250");
            Random rnd = new Random(31);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 30; i++) sb.append(i > 0 ? " " : "").append(rnd.nextInt(500));
            reqs.setText(sb.toString());
            dir.setSelectedIndex(0);
            setStatus("Loaded large example (30 requests, 500 cylinders).", false);
        };
        Task run = () -> {
            String algo = (String) algoBox.getSelectedItem();
            int disk = OSAlgorithms.toInt(size.getText(), "Disk size", 2, null);
            int h = OSAlgorithms.toInt(head.getText(), "Head position", 0, disk - 1);
            int[] list = OSAlgorithms.parseList(reqs.getText(), "Requests", 0);
            OSAlgorithms.DiskResult res = OSAlgorithms.diskSchedule(algo, list, h, disk,
                    dir.getSelectedIndex() == 0, jump.isSelected());
            chart.setData(res.path, disk);
            out.setRowCount(0);
            for (int i = 1; i < res.path.size(); i++) {
                int[] prev = res.path.get(i - 1), cur = res.path.get(i);
                String type = cur[2] == 1 ? "Circular jump" : cur[1] == 1 ? "Request" : "Disk end";
                out.addRow(new Object[]{i, prev[0], cur[0], type, res.moves.get(i - 1)});
            }
            summary.setText("Total head movement: " + res.total + "   Service order: " + res.order);
            setStatus(algo + " finished.", false);
        };
        load(small);

        JPanel controls = stack(
                row(new JLabel("Algorithm:"), algoBox, new JLabel("Disk size:"), size, new JLabel("Head:"), head,
                        dir, jump),
                fieldRow("Requests:", reqs, button("Small Example", small),
                        button("Large Example", large), runButton("Run", run)));

        JPanel center = new JPanel(new BorderLayout(8, 8));
        center.add(box("Service Order and Seek Distance", pane(null, scroll(outTable), summary)), BorderLayout.CENTER);
        center.add(box("Head Movement", chart), BorderLayout.EAST);
        return tab(controls, center);
    }

    private static class DiskChart extends JPanel {
        private List<int[]> path = new ArrayList<>();
        private int diskSize = 200;

        void setData(List<int[]> p, int size) {
            path = p == null ? new ArrayList<>() : p;
            diskSize = Math.max(2, size);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (path.size() < 2) {
                g.setColor(Color.GRAY);
                g.drawString("Head movement plot appears here.", 14, 28);
                return;
            }
            int left = 44, right = 16, top = 16, bottom = 16;
            int w = Math.max(50, getWidth() - left - right);
            int h = Math.max(50, getHeight() - top - bottom);
            g.setColor(Color.LIGHT_GRAY);
            g.drawRect(left, top, w, h);
            g.setColor(Color.DARK_GRAY);
            g.drawString("0", left - 16, top + 10);
            g.drawString(String.valueOf(diskSize - 1), left - 36, top + h);
            int steps = path.size() - 1;
            for (int i = 0; i < steps; i++) {
                int[] a = path.get(i), b = path.get(i + 1);
                int x1 = left + (int) ((long) i * w / steps);
                int x2 = left + (int) ((long) (i + 1) * w / steps);
                int y1 = top + (int) ((long) a[0] * h / (diskSize - 1));
                int y2 = top + (int) ((long) b[0] * h / (diskSize - 1));
                g.setColor(b[2] == 1 ? Color.RED : ACCENT);
                g.drawLine(x1, y1, x2, y2);
                g.fillOval(x2 - 3, y2 - 3, 6, 6);
            }
        }
    }

    private JPanel multiprogrammingTab() {
        JTextField memory = new JTextField("512", 6);
        JTextField quantum = new JTextField("3", 4);
        JTextField degree = new JTextField("3", 4);
        DefaultTableModel jobs = model(0, "PID", "Memory", "Arrival", "Burst");
        JTable jobTable = table(jobs);
        DefaultTableModel rows = model(Integer.MAX_VALUE,
                "PID", "Memory", "Arrival", "Burst", "Admitted", "Finished", "Turnaround", "Waiting / Status");
        DefaultTableModel slices = model(Integer.MAX_VALUE,
                "Start", "End", "Active", "Memory Used", "Degree", "In Memory", "Job Queue");
        JTable rowTable = table(rows), sliceTable = table(slices);
        JLabel util = info("CPU and memory utilization appear here.");

        Task small = () -> {
            memory.setText("512");
            quantum.setText("3");
            degree.setText("3");
            jobs.setRowCount(0);
            jobs.addRow(new Object[]{"J1", "200", "0", "7"});
            jobs.addRow(new Object[]{"J2", "180", "1", "5"});
            jobs.addRow(new Object[]{"J3", "220", "2", "6"});
            jobs.addRow(new Object[]{"J4", "150", "3", "4"});
            jobs.addRow(new Object[]{"J5", "600", "4", "5"});
            setStatus("Loaded small example (J5 is larger than total memory).", false);
        };
        Task large = () -> {
            memory.setText("2048");
            quantum.setText("2");
            degree.setText("0");
            jobs.setRowCount(0);
            Random rnd = new Random(55);
            for (int i = 1; i <= 20; i++)
                jobs.addRow(new Object[]{"J" + i, 120 + rnd.nextInt(450), rnd.nextInt(10), 2 + rnd.nextInt(8)});
            setStatus("Loaded large example (20 jobs).", false);
        };
        Task run = () -> {
            int mem = OSAlgorithms.toInt(memory.getText(), "Total memory", 1, null);
            int q = OSAlgorithms.toInt(quantum.getText(), "Quantum", 1, null);
            int deg = OSAlgorithms.toInt(degree.getText(), "Max degree", 0, null);
            List<OSAlgorithms.MProc> list = new ArrayList<>();
            for (int i = 0; i < jobs.getRowCount(); i++) {
                String pid = cell(jobs, i, 0).trim();
                if (pid.isEmpty()) throw new OSAlgorithms.InputError("Row " + (i + 1) + ": PID is blank.");
                int m = OSAlgorithms.toInt(cell(jobs, i, 1), "Row " + (i + 1) + " Memory", 1, null);
                int at = OSAlgorithms.toInt(cell(jobs, i, 2), "Row " + (i + 1) + " Arrival", 0, null);
                int bt = OSAlgorithms.toInt(cell(jobs, i, 3), "Row " + (i + 1) + " Burst", 1, null);
                list.add(new OSAlgorithms.MProc(pid, m, at, bt));
            }
            OSAlgorithms.MultiResult res = OSAlgorithms.multiprogramming(list, mem, q, deg);
            rows.setRowCount(0);
            for (Object[] r : res.rows) rows.addRow(r);
            slices.setRowCount(0);
            for (Object[] s : res.slices) slices.addRow(s);
            util.setText(String.format("CPU: %.2f%%   Memory: %.2f%%   Avg degree: %.2f   Throughput: %.3f   Completed: %d   Rejected: %d",
                    res.cpuUtil, res.memUtil, res.avgDegree, res.throughput, res.completed, res.rejected));
            setStatus("Multiprogramming simulation finished.", false);
        };
        load(small);

        JPanel controls = row(new JLabel("Total memory:"), memory, new JLabel("Quantum:"), quantum,
                new JLabel("Max degree (0 = no limit):"), degree,
                button("Small Example", small), button("Large Example", large), runButton("Run", run));

        JPanel left = box("Jobs", pane(
                editButtons(jobTable, jobs, () -> new Object[]{"J" + (jobs.getRowCount() + 1), "128", "0", "4"}),
                scroll(jobTable), null));
        left.setPreferredSize(new Dimension(330, 0));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scroll(rowTable), scroll(sliceTable));
        split.setResizeWeight(0.45);
        JPanel right = box("Job Results and Time Slices", pane(null, split, util));

        JPanel center = new JPanel(new BorderLayout(8, 8));
        center.add(left, BorderLayout.WEST);
        center.add(right, BorderLayout.CENTER);
        return tab(controls, center);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            new OSSimulator().setVisible(true);
        });
    }

    static final class OSAlgorithms {
        private OSAlgorithms() {}

        public static class InputError extends RuntimeException {
            public InputError(String m) { super(m); }
        }

        public static int toInt(String text, String name, Integer min, Integer max) {
            String s = text == null ? "" : text.trim();
            if (s.isEmpty()) throw new InputError(name + ": field is blank.");
            int v;
            try {
                v = Integer.parseInt(s);
            } catch (NumberFormatException e) {
                throw new InputError(name + ": '" + s + "' is not a whole number (or is too large).");
            }
            if (min != null && v < min) throw new InputError(name + ": must be at least " + min + " (got " + v + ").");
            if (max != null && v > max) throw new InputError(name + ": must be at most " + max + " (got " + v + ").");
            return v;
        }

        public static int[] parseList(String text, String name, int min) {
            String t = text == null ? "" : text.replace(',', ' ').replace(';', ' ').trim();
            if (t.isEmpty()) throw new InputError(name + ": the list is empty.");
            String[] tok = t.split("\\s+");
            int[] out = new int[tok.length];
            for (int i = 0; i < tok.length; i++) out[i] = toInt(tok[i], name, min, null);
            return out;
        }

        public static final String[] CPU_ALGOS = {"FCFS", "SJF (Non-preemptive)", "SRTF (Preemptive SJF)",
                "Priority (Non-preemptive)", "Priority (Preemptive)", "Round Robin"};

        public static class Proc {
            public final String pid; public final int at, bt, pr;
            public Proc(String pid, int at, int bt, int pr) { this.pid = pid; this.at = at; this.bt = bt; this.pr = pr; }
        }
        public static class Seg {
            public final String pid; public final int start, end;
            public Seg(String pid, int start, int end) { this.pid = pid; this.start = start; this.end = end; }
        }
        public static class CpuRow {
            public String pid; public int at, bt, pr, ct, tat, wt, rt;
        }
        public static class CpuResult {
            public List<Seg> gantt = new ArrayList<>();
            public List<CpuRow> rows = new ArrayList<>();
            public double avgWt, avgTat, avgRt, util;
        }

        private static void addSeg(List<Seg> g, String pid, int s, int e) {
            if (e <= s) return;
            if (!g.isEmpty()) {
                Seg last = g.get(g.size() - 1);
                if (last.pid.equals(pid) && last.end == s) { g.set(g.size() - 1, new Seg(pid, last.start, e)); return; }
            }
            g.add(new Seg(pid, s, e));
        }

        private static long[] cpuKey(List<Proc> p, int[] rem, int mode, int i) {
            switch (mode) {
                case 0: return new long[]{p.get(i).at, i, 0};
                case 1: return new long[]{p.get(i).bt, p.get(i).at, i};
                case 2: return new long[]{rem[i], p.get(i).at, i};
                default: return new long[]{p.get(i).pr, p.get(i).at, i};
            }
        }

        private static int cmp(long[] a, long[] b) {
            for (int i = 0; i < a.length; i++) if (a[i] != b[i]) return Long.compare(a[i], b[i]);
            return 0;
        }

        public static CpuResult cpuSchedule(List<Proc> procs, String algo, int quantum) {
            int n = procs.size();
            if (n == 0) throw new InputError("Add at least one process.");
            int mode = Arrays.asList(CPU_ALGOS).indexOf(algo);
            if (mode < 0) throw new InputError("Unknown algorithm: " + algo);
            int[] rem = new int[n], done = new int[n], first = new int[n];
            Arrays.fill(first, -1);
            for (int i = 0; i < n; i++) rem[i] = procs.get(i).bt;
            List<Seg> gantt = new ArrayList<>();
            int t = 0, finished = 0;
            if (mode == 5) {
                if (quantum < 1) throw new InputError("Time quantum must be at least 1.");
                Integer[] order = new Integer[n];
                for (int i = 0; i < n; i++) order[i] = i;
                Arrays.sort(order, (a, b) -> {
                    int c = Integer.compare(procs.get(a).at, procs.get(b).at);
                    return c != 0 ? c : Integer.compare(a, b);
                });
                ArrayDeque<Integer> q = new ArrayDeque<>();
                int nxt = 0;
                while (finished < n) {
                    while (nxt < n && procs.get(order[nxt]).at <= t) q.add(order[nxt++]);
                    if (q.isEmpty()) {
                        int a = procs.get(order[nxt]).at;
                        addSeg(gantt, "Idle", t, a); t = a;
                        continue;
                    }
                    int i = q.poll();
                    if (first[i] < 0) first[i] = t;
                    int run = Math.min(quantum, rem[i]);
                    addSeg(gantt, procs.get(i).pid, t, t + run); t += run; rem[i] -= run;
                    while (nxt < n && procs.get(order[nxt]).at <= t) q.add(order[nxt++]);
                    if (rem[i] > 0) q.add(i); else { done[i] = t; finished++; }
                }
            } else {
                boolean pre = mode == 2 || mode == 4;
                while (finished < n) {
                    int best = -1; long[] bk = null;
                    for (int i = 0; i < n; i++) {
                        if (rem[i] > 0 && procs.get(i).at <= t) {
                            long[] k = cpuKey(procs, rem, mode, i);
                            if (best < 0 || cmp(k, bk) < 0) { best = i; bk = k; }
                        }
                    }
                    if (best < 0) {
                        int a = Integer.MAX_VALUE;
                        for (int i = 0; i < n; i++) if (rem[i] > 0) a = Math.min(a, procs.get(i).at);
                        addSeg(gantt, "Idle", t, a); t = a;
                        continue;
                    }
                    int i = best;
                    if (first[i] < 0) first[i] = t;
                    int run = rem[i];
                    if (pre) {
                        int nextArr = Integer.MAX_VALUE;
                        for (int j = 0; j < n; j++) if (rem[j] > 0 && procs.get(j).at > t) nextArr = Math.min(nextArr, procs.get(j).at);
                        if (nextArr != Integer.MAX_VALUE) run = Math.min(run, nextArr - t);
                    }
                    addSeg(gantt, procs.get(i).pid, t, t + run); t += run; rem[i] -= run;
                    if (rem[i] == 0) { done[i] = t; finished++; }
                }
            }
            CpuResult r = new CpuResult();
            r.gantt = gantt;
            for (int i = 0; i < n; i++) {
                Proc p = procs.get(i);
                CpuRow row = new CpuRow();
                row.pid = p.pid; row.at = p.at; row.bt = p.bt; row.pr = p.pr; row.ct = done[i];
                row.tat = done[i] - p.at; row.wt = row.tat - p.bt; row.rt = first[i] - p.at;
                r.rows.add(row);
                r.avgWt += row.wt; r.avgTat += row.tat; r.avgRt += row.rt;
            }
            r.avgWt /= n; r.avgTat /= n; r.avgRt /= n;
            long busy = 0;
            for (Seg s : gantt) if (!s.pid.equals("Idle")) busy += s.end - s.start;
            int span = Math.max(1, gantt.get(gantt.size() - 1).end - gantt.get(0).start);
            r.util = 100.0 * busy / span;
            return r;
        }

        public static class BStep {
            public final int proc; public final int[] before, after;
            BStep(int p, int[] b, int[] a) { proc = p; before = b; after = a; }
        }
        public static class BankerResult {
            public int[][] need; public boolean safe;
            public List<Integer> sequence = new ArrayList<>();
            public List<BStep> steps = new ArrayList<>();
            public List<Integer> stuck = new ArrayList<>();
        }
        public static class RequestResult {
            public boolean granted; public String message; public BankerResult result;
        }

        public static int[][] computeNeed(int[][] alloc, int[][] max) {
            int[][] need = new int[alloc.length][];
            for (int i = 0; i < alloc.length; i++) {
                need[i] = new int[alloc[i].length];
                for (int j = 0; j < alloc[i].length; j++) {
                    need[i][j] = max[i][j] - alloc[i][j];
                    if (need[i][j] < 0) throw new InputError("P" + i + ": Max is smaller than Allocation for resource R" + (j + 1) + ".");
                }
            }
            return need;
        }

        public static BankerResult bankers(int[][] alloc, int[][] max, int[] avail) {
            int n = alloc.length, m = avail.length;
            if (n == 0 || m == 0) throw new InputError("Need at least one process and one resource type.");
            for (int i = 0; i < n; i++)
                if (alloc[i].length != m || max[i].length != m) throw new InputError("Row P" + i + " must have exactly " + m + " values per matrix.");
            BankerResult r = new BankerResult();
            r.need = computeNeed(alloc, max);
            int[] work = avail.clone();
            boolean[] fin = new boolean[n];
            boolean progress = true;
            while (r.sequence.size() < n && progress) {
                progress = false;
                for (int i = 0; i < n; i++) {
                    if (fin[i]) continue;
                    boolean ok = true;
                    for (int j = 0; j < m; j++) if (r.need[i][j] > work[j]) { ok = false; break; }
                    if (ok) {
                        int[] before = work.clone();
                        for (int j = 0; j < m; j++) work[j] += alloc[i][j];
                        r.steps.add(new BStep(i, before, work.clone()));
                        fin[i] = true; r.sequence.add(i); progress = true;
                    }
                }
            }
            r.safe = r.sequence.size() == n;
            for (int i = 0; i < n; i++) if (!fin[i]) r.stuck.add(i);
            return r;
        }

        public static RequestResult bankersRequest(int[][] alloc, int[][] max, int[] avail, int pid, int[] req) {
            int n = alloc.length, m = avail.length;
            if (pid < 0 || pid >= n) throw new InputError("Process index must be between 0 and " + (n - 1) + ".");
            if (req.length != m) throw new InputError("Request needs exactly " + m + " values.");
            int[][] need = computeNeed(alloc, max);
            RequestResult rr = new RequestResult();
            for (int j = 0; j < m; j++) {
                if (req[j] < 0) throw new InputError("Request values cannot be negative.");
                if (req[j] > need[pid][j]) { rr.message = "DENIED: P" + pid + " requests more than its remaining Need."; return rr; }
            }
            for (int j = 0; j < m; j++)
                if (req[j] > avail[j]) { rr.message = "P" + pid + " must WAIT: resources are not currently available."; return rr; }
            int[][] a2 = new int[n][];
            for (int i = 0; i < n; i++) a2[i] = alloc[i].clone();
            int[] av2 = avail.clone();
            for (int j = 0; j < m; j++) { a2[pid][j] += req[j]; av2[j] -= req[j]; }
            rr.result = bankers(a2, max, av2);
            rr.granted = rr.result.safe;
            rr.message = rr.granted ? "GRANTED: the system stays safe." : "DENIED: granting would leave the system in an UNSAFE state.";
            return rr;
        }

        public static List<Object[]> pagingTranslate(int pageSize, Map<Integer, Integer> table, int[] addrs) {
            List<Object[]> rows = new ArrayList<>();
            for (int a : addrs) {
                int page = a / pageSize, off = a % pageSize;
                Integer f = table.get(page);
                if (f != null) rows.add(new Object[]{a, page, off, f, (long) f * pageSize + off, "OK"});
                else rows.add(new Object[]{a, page, off, "-", "-", "INVALID: page " + page + " is not mapped (page fault)"});
            }
            return rows;
        }

        public static final String[] PR_ALGOS = {"FIFO", "LRU", "Optimal", "Clock (Second Chance)"};

        public static class PRStep {
            public final int index, page, replaced; public final int[] slots; public final boolean hit;
            PRStep(int i, int p, int[] s, boolean h, int r) { index = i; page = p; slots = s; hit = h; replaced = r; }
        }
        public static class PRResult {
            public List<PRStep> steps = new ArrayList<>(); public int faults;
        }

        private static int indexOf(int[] a, int v) {
            for (int i = 0; i < a.length; i++) if (a[i] == v) return i;
            return -1;
        }

        private static int nextUse(List<Integer> pos, int i) {
            int lo = 0, hi = pos.size();
            while (lo < hi) { int mid = (lo + hi) >>> 1; if (pos.get(mid) <= i) lo = mid + 1; else hi = mid; }
            return lo < pos.size() ? pos.get(lo) : Integer.MAX_VALUE;
        }

        public static PRResult pageReplace(int[] refs, int nf, String algo) {
            if (nf < 1) throw new InputError("Number of frames must be at least 1.");
            if (!Arrays.asList(PR_ALGOS).contains(algo)) throw new InputError("Unknown algorithm: " + algo);
            int[] slots = new int[nf], bits = new int[nf];
            Arrays.fill(slots, -1);
            int ptr = 0;
            Map<Integer, Integer> last = new HashMap<>();
            Map<Integer, List<Integer>> positions = new HashMap<>();
            if (algo.equals("Optimal"))
                for (int i = 0; i < refs.length; i++) positions.computeIfAbsent(refs[i], k -> new ArrayList<>()).add(i);
            PRResult res = new PRResult();
            for (int i = 0; i < refs.length; i++) {
                int p = refs[i], idx = indexOf(slots, p), replaced = -1;
                boolean hit = idx >= 0;
                if (hit) {
                    if (algo.startsWith("Clock")) bits[idx] = 1;
                } else {
                    res.faults++;
                    int v, empty = indexOf(slots, -1);
                    if (empty >= 0 && (algo.equals("LRU") || algo.equals("Optimal"))) {
                        v = empty;
                    } else if (algo.equals("FIFO")) {
                        v = ptr; ptr = (ptr + 1) % nf;
                    } else if (algo.equals("LRU")) {
                        v = 0;
                        for (int k = 1; k < nf; k++) if (last.get(slots[k]) < last.get(slots[v])) v = k;
                    } else if (algo.equals("Optimal")) {
                        v = 0; long best = -1;
                        for (int k = 0; k < nf; k++) {
                            long nu = nextUse(positions.get(slots[k]), i);
                            if (nu > best) { best = nu; v = k; }
                        }
                    } else {
                        while (slots[ptr] != -1 && bits[ptr] == 1) { bits[ptr] = 0; ptr = (ptr + 1) % nf; }
                        v = ptr; ptr = (ptr + 1) % nf; bits[v] = 1;
                    }
                    replaced = slots[v]; slots[v] = p;
                }
                last.put(p, i);
                res.steps.add(new PRStep(i + 1, p, slots.clone(), hit, replaced));
            }
            return res;
        }

        public static List<String> segOverlaps(Map<Integer, long[]> segs) {
            List<Map.Entry<Integer, long[]>> items = new ArrayList<>(segs.entrySet());
            items.sort(Comparator.comparingLong(e -> e.getValue()[0]));
            List<String> out = new ArrayList<>();
            for (int i = 0; i + 1 < items.size(); i++) {
                long[] a = items.get(i).getValue(), b = items.get(i + 1).getValue();
                if (a[0] + a[1] > b[0]) out.add("Segments " + items.get(i).getKey() + " and " + items.get(i + 1).getKey() + " overlap in physical memory.");
            }
            return out;
        }

        public static List<Object[]> segTranslate(Map<Integer, long[]> segs, List<int[]> reqs) {
            List<Object[]> rows = new ArrayList<>();
            for (int[] r : reqs) {
                long[] s = segs.get(r[0]);
                if (s == null) rows.add(new Object[]{r[0], r[1], "-", "-", "-", "ERROR: segment " + r[0] + " does not exist"});
                else if (r[1] >= s[1]) rows.add(new Object[]{r[0], r[1], s[1], s[0], "-", "ERROR: offset " + r[1] + " >= limit " + s[1] + " (trap)"});
                else rows.add(new Object[]{r[0], r[1], s[1], s[0], s[0] + r[1], "OK"});
            }
            return rows;
        }

        public static final String[] FRAG_STRATEGIES = {"First Fit", "Best Fit", "Worst Fit", "Next Fit"};

        public static class FragResult {
            public int[] assign, remaining, internal;
            public long internalTotal, externalTotal;
            public List<Integer> unallocated = new ArrayList<>(), extProblem = new ArrayList<>();
        }

        public static FragResult allocate(int[] blocks, int[] procs, String strategy, boolean dynamic) {
            if (!Arrays.asList(FRAG_STRATEGIES).contains(strategy)) throw new InputError("Unknown strategy: " + strategy);
            int nb = blocks.length;
            if (nb == 0 || procs.length == 0) throw new InputError("Enter at least one memory block and one process.");
            int[] free = blocks.clone();
            boolean[] occ = new boolean[nb];
            FragResult r = new FragResult();
            r.assign = new int[procs.length]; r.internal = new int[procs.length];
            int last = 0;
            for (int pi = 0; pi < procs.length; pi++) {
                int ps = procs[pi], b = -1;
                for (int x = 0; x < nb; x++) {
                    if (free[x] < ps || (!dynamic && occ[x])) continue;
                    if (b < 0) { b = x; if (strategy.equals("First Fit")) break; continue; }
                    switch (strategy) {
                        case "Best Fit": if (free[x] < free[b]) b = x; break;
                        case "Worst Fit": if (free[x] > free[b]) b = x; break;
                        default: if ((x - last + nb) % nb < (b - last + nb) % nb) b = x;
                    }
                }
                r.assign[pi] = b;
                if (b < 0) continue;
                if (strategy.equals("Next Fit")) last = b;
                occ[b] = true;
                if (dynamic) free[b] -= ps; else { r.internal[pi] = free[b] - ps; free[b] = 0; }
            }
            r.remaining = free;
            for (int v : r.internal) r.internalTotal += v;
            for (int x = 0; x < nb; x++) r.externalTotal += dynamic ? free[x] : (occ[x] ? 0 : blocks[x]);
            for (int pi = 0; pi < procs.length; pi++) {
                if (r.assign[pi] < 0) {
                    r.unallocated.add(pi);
                    if (procs[pi] <= r.externalTotal) r.extProblem.add(pi);
                }
            }
            return r;
        }

        public static final String[] DISK_ALGOS = {"FCFS", "SSTF", "SCAN", "C-SCAN", "LOOK", "C-LOOK"};

        public static class DiskResult {
            public List<Integer> order = new ArrayList<>();
            public List<int[]> path = new ArrayList<>();
            public List<Integer> moves = new ArrayList<>();
            public long total;
        }

        public static DiskResult diskSchedule(String algo, int[] reqs, int head, int size, boolean goRight, boolean countJump) {
            if (!Arrays.asList(DISK_ALGOS).contains(algo)) throw new InputError("Unknown algorithm: " + algo);
            if (size < 2) throw new InputError("Disk size (cylinders) must be at least 2.");
            if (head < 0 || head >= size) throw new InputError("Head position must be between 0 and " + (size - 1) + ".");
            if (reqs.length == 0) throw new InputError("Enter at least one disk request.");
            for (int r : reqs) if (r < 0 || r >= size) throw new InputError("Request " + r + " is outside the disk range 0.." + (size - 1) + ".");
            DiskResult d = new DiskResult();
            d.path.add(new int[]{head, 0, 0});
            if (algo.equals("FCFS")) {
                for (int r : reqs) d.path.add(new int[]{r, 1, 0});
            } else if (algo.equals("SSTF")) {
                List<Integer> pend = new ArrayList<>();
                for (int r : reqs) pend.add(r);
                int cur = head;
                while (!pend.isEmpty()) {
                    int bi = 0;
                    for (int i = 1; i < pend.size(); i++) {
                        int a = Math.abs(pend.get(i) - cur), b = Math.abs(pend.get(bi) - cur);
                        if (a < b || (a == b && pend.get(i) < pend.get(bi))) bi = i;
                    }
                    cur = pend.remove(bi);
                    d.path.add(new int[]{cur, 1, 0});
                }
            } else {
                List<Integer> first = new ArrayList<>(), second = new ArrayList<>();
                for (int r : reqs) {
                    if (goRight) { if (r >= head) first.add(r); else second.add(r); }
                    else { if (r <= head) first.add(r); else second.add(r); }
                }
                Collections.sort(first); Collections.sort(second);
                if (!goRight) Collections.reverse(first);
                if (goRight) Collections.reverse(second);
                int end1 = goRight ? size - 1 : 0, end2 = goRight ? 0 : size - 1;
                for (int r : first) d.path.add(new int[]{r, 1, 0});
                List<Integer> cs = new ArrayList<>(second);
                Collections.reverse(cs);
                if (algo.equals("SCAN")) {
                    if (d.path.get(d.path.size() - 1)[0] != end1) d.path.add(new int[]{end1, 0, 0});
                    for (int r : second) d.path.add(new int[]{r, 1, 0});
                } else if (algo.equals("LOOK")) {
                    for (int r : second) d.path.add(new int[]{r, 1, 0});
                } else if (algo.equals("C-SCAN")) {
                    if (d.path.get(d.path.size() - 1)[0] != end1) d.path.add(new int[]{end1, 0, 0});
                    if (!cs.isEmpty()) {
                        d.path.add(new int[]{end2, 0, 1});
                        for (int r : cs) d.path.add(new int[]{r, 1, 0});
                    }
                } else if (!cs.isEmpty()) {
                    d.path.add(new int[]{cs.get(0), 1, 1});
                    for (int i = 1; i < cs.size(); i++) d.path.add(new int[]{cs.get(i), 1, 0});
                }
            }
            for (int k = 1; k < d.path.size(); k++) {
                int mv = Math.abs(d.path.get(k)[0] - d.path.get(k - 1)[0]);
                if (d.path.get(k)[2] == 1 && !countJump) mv = 0;
                d.moves.add(mv); d.total += mv;
            }
            for (int[] p : d.path) if (p[1] == 1) d.order.add(p[0]);
            return d;
        }

        public static class MProc {
            public final String pid; public final int mem, at, bt;
            public MProc(String pid, int mem, int at, int bt) { this.pid = pid; this.mem = mem; this.at = at; this.bt = bt; }
        }
        public static class MultiResult {
            public List<Object[]> slices = new ArrayList<>();
            public List<Object[]> rows = new ArrayList<>();
            public int totalTime, completed, rejected;
            public double cpuUtil, memUtil, avgDegree, throughput;
        }

        private static String names(Collection<Integer> ids, List<MProc> p) {
            List<Integer> s = new ArrayList<>(ids);
            Collections.sort(s);
            StringBuilder sb = new StringBuilder();
            for (int k = 0; k < s.size() && k < 8; k++) sb.append(k > 0 ? "," : "").append(p.get(s.get(k)).pid);
            if (s.size() > 8) sb.append("...");
            return sb.length() == 0 ? "-" : sb.toString();
        }

        public static MultiResult multiprogramming(List<MProc> procs, int totalMem, int quantum, int maxDegree) {
            if (totalMem < 1 || quantum < 1) throw new InputError("Total memory and quantum must be at least 1.");
            if (procs.isEmpty()) throw new InputError("Add at least one process.");
            int n = procs.size();
            Integer[] order = new Integer[n];
            for (int i = 0; i < n; i++) order[i] = i;
            Arrays.sort(order, (a, b) -> {
                int c = Integer.compare(procs.get(a).at, procs.get(b).at);
                return c != 0 ? c : Integer.compare(a, b);
            });
            MultiResult r = new MultiResult();
            boolean[] rejected = new boolean[n];
            ArrayDeque<Integer> pend = new ArrayDeque<>();
            int[] rem = new int[n], admit = new int[n], finish = new int[n];
            for (int i : order) {
                if (procs.get(i).mem > totalMem) { rejected[i] = true; r.rejected++; } else { pend.add(i); rem[i] = procs.get(i).bt; }
            }
            List<Integer> jobq = new ArrayList<>();
            ArrayDeque<Integer> ready = new ArrayDeque<>();
            TreeSet<Integer> inMem = new TreeSet<>();
            long used = 0, busy = 0, memArea = 0, degArea = 0;
            int t = 0;
            while (!pend.isEmpty() || !jobq.isEmpty() || !ready.isEmpty()) {
                while (!pend.isEmpty() && procs.get(pend.peek()).at <= t) jobq.add(pend.poll());
                for (Integer i : new ArrayList<>(jobq)) {
                    if (used + procs.get(i).mem <= totalMem && (maxDegree <= 0 || inMem.size() < maxDegree)) {
                        jobq.remove(i); inMem.add(i); ready.add(i); used += procs.get(i).mem; admit[i] = t;
                    }
                }
                if (ready.isEmpty()) {
                    if (pend.isEmpty()) break;
                    int nt = procs.get(pend.peek()).at;
                    r.slices.add(new Object[]{t, nt, "Idle", used, 0, "-", names(jobq, procs)});
                    t = nt;
                    continue;
                }
                int i = ready.poll();
                int run = Math.min(quantum, rem[i]);
                r.slices.add(new Object[]{t, t + run, procs.get(i).pid, used, inMem.size(), names(inMem, procs), names(jobq, procs)});
                busy += run; memArea += used * run; degArea += (long) inMem.size() * run;
                t += run; rem[i] -= run;
                if (rem[i] == 0) { finish[i] = t; inMem.remove(i); used -= procs.get(i).mem; r.completed++; }
                else ready.add(i);
            }
            for (int i : order) {
                MProc p = procs.get(i);
                if (rejected[i]) r.rows.add(new Object[]{p.pid, p.mem, p.at, p.bt, "-", "-", "-", "REJECTED (memory > total)"});
                else { int tat = finish[i] - p.at; r.rows.add(new Object[]{p.pid, p.mem, p.at, p.bt, admit[i], finish[i], tat, tat - p.bt}); }
            }
            int T = Math.max(1, t);
            r.totalTime = t; r.cpuUtil = 100.0 * busy / T; r.memUtil = 100.0 * memArea / ((double) totalMem * T);
            r.avgDegree = (double) degArea / T; r.throughput = (double) r.completed / T;
            return r;
        }
    }
}
