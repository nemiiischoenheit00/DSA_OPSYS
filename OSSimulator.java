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
}