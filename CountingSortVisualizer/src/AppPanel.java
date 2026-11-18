import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.prefs.Preferences;

/**
 * The whole GUI as one panel (kept separate from the window so it can be tested headlessly).
 *
 * Responsibilities: build the layout, read the user's input, drive the engine step by step,
 * and run the play/pause animation clock. All drawing is done by the small components in
 * the other files (StagePanel, CodeView, ...).
 */
final class AppPanel extends JPanel {

    private static final double[] SPEEDS = {0.5, 1, 2, 4};
    private static final String[] SPEED_LABELS = {"0.5\u00D7", "1\u00D7", "2\u00D7", "4\u00D7", "Fast", "Turbo"};
    private static final int FAST = 4, TURBO = 5;
    /** Above this many numbers the input field shows a summary instead of every number. */
    private static final int SUMMARY_THRESHOLD = 50_000;
    private static final Preferences PREFS = Preferences.userNodeForPackage(AppPanel.class);

    final StagePanel stage = new StagePanel();
    private final PhaseStepper stepper = new PhaseStepper();
    private final CodeView code = new CodeView();
    private final VarsView vars = new VarsView();
    private final Scrubber scrubber = new Scrubber();
    final RoundField field = new RoundField("Whole numbers separated by commas or spaces \u2013 any amount, negatives allowed");

    private final JLabel title = new JLabel("Counting Sort");
    private final JLabel subtitle = new JLabel("See how it counts, accumulates and places every element.");
    private final JLabel messageLabel = new JLabel(" ");
    private final JLabel stepTitle = new JLabel();
    private final JLabel stepBadge = new JLabel();
    private final JLabel hint = new JLabel("\u2190 \u2192  step     Space  play / pause     Home  restart     T  theme",
            SwingConstants.CENTER);
    private final JTextArea explanation = new JTextArea(3, 40);
    private final FlatButton restartBtn, prevBtn, playBtn, nextBtn, themeBtn;
    private final List<Runnable> themers = new ArrayList<>();
    private final javax.swing.Timer clock;

    // ---- current run (engine == null means "nothing loaded yet")
    private CountingSortEngine engine;
    private int[] data;
    private String summaryText;
    private int index;
    private int total;
    private Step current;

    // ---- playback state
    private boolean playing;
    private double animT = 1;
    private double dwellLeft;
    private int speedIdx = 1;
    private long lastTick;
    private boolean messageIsError;

    AppPanel() {
        super(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(14, 20, 10, 20));
        Theme.cur = PREFS.getBoolean("dark", false) ? Theme.DARK : Theme.LIGHT;
        clock = new javax.swing.Timer(16, e -> tick());

        // ------------------------------------------------------------ header
        JPanel titles = new JPanel();
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.setOpaque(false);
        title.setFont(Gfx.ui(Font.BOLD, 24f));
        subtitle.setFont(Gfx.ui(Font.PLAIN, 14f));
        titles.add(title);
        titles.add(Box.createVerticalStrut(2));
        titles.add(subtitle);

        FlatButton resultsBtn = new FlatButton("Results", null, FlatButton.Style.SECONDARY,
                "Show the full input and the sorted output", this::showResults);
        FlatButton testsBtn = new FlatButton("Test cases", null, FlatButton.Style.SECONDARY,
                "Run the built-in normal, boundary and invalid test cases", () -> SelfTestDialog.show(this));
        themeBtn = new FlatButton(null, Glyph.MOON, FlatButton.Style.GHOST, "Switch theme (T)", this::toggleTheme);
        JPanel headerButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        headerButtons.setOpaque(false);
        headerButtons.add(resultsBtn);
        headerButtons.add(testsBtn);
        headerButtons.add(themeBtn);
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(titles, BorderLayout.WEST);
        header.add(headerButtons, BorderLayout.EAST);

        // ------------------------------------------------------------ input card
        RoundPanel inputCard = new RoundPanel(new BorderLayout(0, 8), 14);
        inputCard.setBorder(BorderFactory.createEmptyBorder(12, 14, 8, 14));
        JLabel arrayLabel = new JLabel("Numbers");
        arrayLabel.setFont(Gfx.ui(Font.BOLD, 14f));
        field.setFont(Gfx.ui(Font.PLAIN, 15f));
        field.addActionListener(e -> applyField());
        field.getDocument().addDocumentListener(new DocumentListener() {
            void edited() {
                if (field.hasError()) {
                    field.setError(false);
                    setMessage(" ", false);
                }
            }
            @Override public void insertUpdate(DocumentEvent e) { edited(); }
            @Override public void removeUpdate(DocumentEvent e) { edited(); }
            @Override public void changedUpdate(DocumentEvent e) { edited(); }
        });
        FlatButton loadBtn = new FlatButton("Load file\u2026", null, FlatButton.Style.SECONDARY,
                "Load numbers from a text file", this::loadFile);
        FlatButton goBtn = new FlatButton("Visualize", null, FlatButton.Style.PRIMARY,
                "Run counting sort on these numbers", this::applyField);
        goBtn.fixedWidth(110);
        JPanel actionButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionButtons.setOpaque(false);
        actionButtons.add(loadBtn);
        actionButtons.add(goBtn);
        JPanel row1 = new JPanel(new BorderLayout(12, 0));
        row1.setOpaque(false);
        row1.add(arrayLabel, BorderLayout.WEST);
        row1.add(field, BorderLayout.CENTER);
        row1.add(actionButtons, BorderLayout.EAST);

        JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        chips.setOpaque(false);
        JLabel tryLabel = new JLabel("Try");
        tryLabel.setFont(Gfx.ui(Font.BOLD, 13f));
        chips.add(tryLabel);
        chips.add(Box.createHorizontalStrut(2));
        addPreset(chips, "Classic", () -> new int[]{4, 2, 2, 8, 3, 3, 1});
        addPreset(chips, "Duplicates", () -> new int[]{3, 3, 3, 1, 1, 2, 2, 3});
        addPreset(chips, "Negatives", () -> new int[]{5, -3, 0, -3, 2, -1});
        addPreset(chips, "Reversed", () -> new int[]{9, 8, 7, 6, 5, 4, 3, 2, 1});
        chips.add(new FlatButton("Generate\u2026", null, FlatButton.Style.CHIP,
                "Generate random numbers (you choose how many and the range)", this::generate));
        messageLabel.setFont(Gfx.ui(Font.BOLD, 12.5f));
        messageLabel.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));
        JPanel row2 = new JPanel(new BorderLayout());
        row2.setOpaque(false);
        row2.add(chips, BorderLayout.WEST);
        row2.add(messageLabel, BorderLayout.CENTER);
        inputCard.add(row1, BorderLayout.NORTH);
        inputCard.add(row2, BorderLayout.CENTER);

        JPanel north = new JPanel(new BorderLayout(0, 12));
        north.setOpaque(false);
        north.add(header, BorderLayout.NORTH);
        north.add(inputCard, BorderLayout.CENTER);
        add(north, BorderLayout.NORTH);

        // ------------------------------------------------------------ centre
        RoundPanel stepperCard = new RoundPanel(new BorderLayout(), 14);
        stepperCard.add(stepper, BorderLayout.CENTER);
        stepper.onPick(this::jumpToGroup);

        stepTitle.setFont(Gfx.ui(Font.BOLD, 16f));
        stepBadge.setFont(Gfx.ui(Font.PLAIN, 13f));
        explanation.setEditable(false);
        explanation.setFocusable(false);
        explanation.setLineWrap(true);
        explanation.setWrapStyleWord(true);
        explanation.setOpaque(false);
        explanation.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
        explanation.setFont(Gfx.ui(Font.PLAIN, 15f));
        RoundPanel explainCard = new RoundPanel(new BorderLayout(), 14);
        explainCard.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        JPanel explainHeader = new JPanel(new BorderLayout());
        explainHeader.setOpaque(false);
        explainHeader.add(stepTitle, BorderLayout.WEST);
        explainHeader.add(stepBadge, BorderLayout.EAST);
        explainCard.add(explainHeader, BorderLayout.NORTH);
        explainCard.add(explanation, BorderLayout.CENTER);
        explainCard.setPreferredSize(new Dimension(300, 108));

        JPanel left = new JPanel(new BorderLayout(0, 12));
        left.setOpaque(false);
        left.add(stepperCard, BorderLayout.NORTH);
        left.add(stage, BorderLayout.CENTER);
        left.add(explainCard, BorderLayout.SOUTH);

        JPanel right = new JPanel(new BorderLayout(0, 12));
        right.setOpaque(false);
        right.add(code, BorderLayout.CENTER);
        right.add(vars, BorderLayout.SOUTH);
        right.setPreferredSize(new Dimension(320, 300));

        JPanel center = new JPanel(new BorderLayout(14, 0));
        center.setOpaque(false);
        center.add(left, BorderLayout.CENTER);
        center.add(right, BorderLayout.EAST);
        add(center, BorderLayout.CENTER);

        // ------------------------------------------------------------ transport bar
        restartBtn = new FlatButton(null, Glyph.RESTART, FlatButton.Style.GHOST, "Restart (Home)",
                () -> { pause(); goTo(0, false); });
        prevBtn = new FlatButton(null, Glyph.PREV, FlatButton.Style.SECONDARY, "Previous step (\u2190)",
                () -> { pause(); goTo(index - 1, false); });
        playBtn = new FlatButton("Play", Glyph.PLAY, FlatButton.Style.PRIMARY, "Play / pause (Space)",
                this::togglePlay).fixedWidth(110);
        nextBtn = new FlatButton(null, Glyph.NEXT, FlatButton.Style.SECONDARY, "Next step (\u2192)",
                () -> { pause(); goTo(index + 1, true); });
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(restartBtn);
        buttons.add(prevBtn);
        buttons.add(playBtn);
        buttons.add(nextBtn);

        Segmented speed = new Segmented(SPEED_LABELS, speedIdx,
                "Playback speed. Fast skips the animation; Turbo finishes any input in a few seconds.",
                i -> speedIdx = i);
        JLabel speedLabel = new JLabel("Speed");
        speedLabel.setFont(Gfx.ui(Font.BOLD, 13f));
        JPanel speedBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        speedBox.setOpaque(false);
        speedBox.add(speedLabel);
        speedBox.add(speed);

        RoundPanel transport = new RoundPanel(new BorderLayout(16, 0), 14);
        transport.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        transport.add(buttons, BorderLayout.WEST);
        transport.add(scrubber, BorderLayout.CENTER);
        transport.add(speedBox, BorderLayout.EAST);

        hint.setFont(Gfx.ui(Font.PLAIN, 12f));
        hint.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(transport, BorderLayout.CENTER);
        bottom.add(hint, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);

        // ------------------------------------------------------------ theming + keyboard
        themers.add(() -> {
            Theme t = Theme.cur;
            setBackground(t.bg);
            title.setForeground(t.text);
            subtitle.setForeground(t.muted);
            arrayLabel.setForeground(t.text);
            tryLabel.setForeground(t.muted);
            speedLabel.setForeground(t.muted);
            hint.setForeground(t.muted);
            stepBadge.setForeground(t.muted);
            explanation.setForeground(t.text);
            field.setForeground(t.text);
            field.setCaretColor(t.accent);
            field.setSelectionColor(t.accentSoft);
            field.setSelectedTextColor(t.text);
            messageLabel.setForeground(messageIsError ? t.danger : t.muted);
            themeBtn.setGlyph(t.dark ? Glyph.SUN : Glyph.MOON);
        });
        bind("LEFT", () -> { pause(); goTo(index - 1, false); });
        bind("RIGHT", () -> { pause(); goTo(index + 1, true); });
        bind("SPACE", this::togglePlay);
        bind("HOME", () -> { pause(); goTo(0, false); });
        bind("END", () -> { pause(); goTo(total - 1, false); });
        bind("T", this::toggleTheme);

        setOpaque(true);
        applyTheme();
        showNothingLoaded();
    }

    /** Keyboard shortcut that is ignored while the user is typing in the text field. */
    private void bind(String key, Runnable action) {
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), key);
        getActionMap().put(key, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (field.hasFocus()) return;
                action.run();
            }
        });
    }

    private void addPreset(JPanel row, String name, Supplier<int[]> values) {
        row.add(new FlatButton(name, null, FlatButton.Style.CHIP, "Use the \"" + name + "\" example", () -> {
            try {
                loadData(values.get());
            } catch (IllegalArgumentException ex) {
                showError(ex.getMessage());
            }
        }));
    }

    // ==================================================================
    // Theme
    // ==================================================================

    private void toggleTheme() {
        Theme.cur = Theme.cur.dark ? Theme.LIGHT : Theme.DARK;
        try {
            PREFS.putBoolean("dark", Theme.cur.dark);
        } catch (RuntimeException ignored) {
            // preferences are a nicety; ignore storage problems
        }
        applyTheme();
    }

    void applyTheme() {
        for (Runnable r : themers) r.run();
        refresh(current);
        repaint();
    }

    // ==================================================================
    // Getting data in
    // ==================================================================

    private void setMessage(String text, boolean error) {
        messageIsError = error;
        messageLabel.setText(text);
        messageLabel.setForeground(error ? Theme.cur.danger : Theme.cur.muted);
    }

    private void showError(String text) {
        field.setError(true);
        setMessage(text, true);
    }

    /** Parses the text field (or re-runs the loaded data when the field only shows a summary). */
    void applyField() {
        try {
            String text = field.getText();
            if (summaryText != null && summaryText.equals(text) && data != null) {
                loadData(data);
            } else {
                loadData(InputParser.parse(text));
            }
            stage.requestFocusInWindow();
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
            field.requestFocusInWindow();
        }
    }

    private void loadFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Load numbers from a text file");
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();
        try {
            loadData(InputParser.readFile(file));
        } catch (IllegalArgumentException ex) {
            showError(file.getName() + ": " + ex.getMessage());
        }
    }

    private void generate() {
        JTextField countField = new JTextField("20", 8);
        JTextField minField = new JTextField("0", 8);
        JTextField maxField = new JTextField("99", 8);
        JPanel form = new JPanel(new GridLayout(3, 2, 8, 6));
        form.add(new JLabel("How many numbers?"));
        form.add(countField);
        form.add(new JLabel("Smallest value:"));
        form.add(minField);
        form.add(new JLabel("Largest value:"));
        form.add(maxField);
        while (true) {
            if (JOptionPane.showConfirmDialog(this, form, "Generate random numbers",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
            try {
                int[] values = InputParser.generate(parseInt(countField, "How many numbers"),
                        parseInt(minField, "Smallest value"), parseInt(maxField, "Largest value"));
                loadData(values);
                return;
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Please check the values",
                        JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private static int parseInt(JTextField f, String what) {
        try {
            return Integer.parseInt(f.getText().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(what + " must be a whole number.");
        }
    }

    /**
     * Starts a new run.
     *
     * @throws IllegalArgumentException if the data cannot be visualised (see CountingSortEngine.validate)
     */
    void loadData(int[] values) {
        CountingSortEngine newEngine = new CountingSortEngine(values);     // validates
        pause();
        engine = newEngine;
        data = values;
        total = newEngine.totalSteps();
        index = 0;

        if (values.length > SUMMARY_THRESHOLD) {
            summaryText = String.format("(%,d numbers loaded \u2013 open Results to see them all)", values.length);
            field.setText(summaryText);
        } else {
            summaryText = null;
            field.setText(InputParser.join(values));
        }
        field.setCaretPosition(0);
        field.setError(false);
        setMessage(String.format("%,d numbers   |   range %d \u2026 %d   |   k = %,d   |   %,d steps",
                values.length, newEngine.min(), newEngine.max(), newEngine.k(), total), false);

        scrubber.configure(total, i -> "Step " + (i + 1) + ": " + newEngine.phaseAt(i).title,
                i -> { pause(); goTo(i, false); });
        goTo(0, false);
    }

    private void showNothingLoaded() {
        engine = null;
        current = null;
        stage.clear();
        scrubber.deactivate();
        refresh(null);
    }

    private void showResults() {
        if (data == null) {
            setMessage("Nothing to show yet \u2013 visualize some numbers first.", true);
            return;
        }
        ResultsDialog.show(this, data);
    }

    // ==================================================================
    // Navigation
    // ==================================================================

    private void jumpToGroup(int group) {
        if (engine == null) return;
        pause();
        goTo(engine.firstStepOfGroup(group), false);
    }

    static int group(Phase p) {
        return switch (p) {
            case INTRO -> -1;
            case RANGE -> 0;
            case INIT -> 1;
            case COUNT -> 2;
            case PREFIX -> 3;
            case ALLOCATE, PLACE -> 4;
            case DONE -> 5;
        };
    }

    /** Moves to a step; 'animate' = play the transition (only used for a single step forward). */
    void goTo(int i, boolean animate) {
        if (engine == null) return;
        int old = index;
        index = Math.max(0, Math.min(total - 1, i));
        Step cur = engine.goTo(index);
        boolean anim = animate && speedIdx < FAST && index == old + 1 && StagePanel.hasMotion(cur);
        animT = anim ? 0 : 1;
        current = cur;
        stage.show(engine, cur, anim, animT);
        dwellLeft = anim ? 0 : (StagePanel.hasMotion(cur) ? 700 : 1700) / speedFactor();
        refresh(cur);
        if (playing || anim) startClock();
    }

    private void refresh(Step cur) {
        Theme t = Theme.cur;
        boolean loaded = engine != null && cur != null;
        stepper.setActive(loaded ? group(cur.phase()) : -1);
        code.setStep(cur);
        vars.setStep(cur);
        scrubber.setValue(index);
        if (loaded) {
            stepTitle.setText(cur.phase().title);
            stepTitle.setForeground(cur.phase() == Phase.DONE ? t.out : t.text);
            stepBadge.setText(String.format("Step %,d of %,d", index + 1, total));
            explanation.setText(cur.text());
        } else {
            stepTitle.setText("Ready");
            stepTitle.setForeground(t.text);
            stepBadge.setText("");
            explanation.setText("Enter whole numbers (any amount, negatives allowed) and press Visualize. "
                    + "Counting sort needs the value range (max - min + 1) to fit in memory.");
        }
        restartBtn.setEnabled(loaded && index > 0);
        prevBtn.setEnabled(loaded && index > 0);
        nextBtn.setEnabled(loaded && index < total - 1);
        playBtn.setEnabled(loaded);
        updatePlayButton();
    }

    // ==================================================================
    // Playback
    // ==================================================================

    private double speedFactor() {
        return SPEEDS[Math.min(speedIdx, SPEEDS.length - 1)];
    }

    private void startClock() {
        lastTick = System.nanoTime();
        if (!clock.isRunning()) clock.start();
    }

    private void tick() {
        long now = System.nanoTime();
        double dt = (now - lastTick) / 1e6;
        lastTick = now;
        if (engine == null) {
            clock.stop();
            return;
        }
        if (playing && speedIdx >= FAST) {                       // no animation: just advance
            dwellLeft -= dt;
            if (dwellLeft <= 0) {
                int stride = speedIdx == TURBO ? Math.max(1, (int) Math.ceil(total / 400.0)) : 1;
                if (index >= total - 1) {
                    pause();
                } else {
                    goTo(Math.min(total - 1, index + stride), false);
                    dwellLeft = speedIdx == TURBO ? 16 : 45;       // after goTo, which resets the dwell time
                }
            }
            return;
        }
        double speed = speedFactor();
        if (animT < 1) {
            animT = Math.min(1, animT + dt * speed / 1000.0);
            stage.setT(animT);
            if (animT >= 1) dwellLeft = 450 / speed;
        } else if (playing) {
            dwellLeft -= dt;
            if (dwellLeft <= 0) {
                if (index < total - 1) goTo(index + 1, true);
                else pause();
            }
        } else {
            clock.stop();
        }
    }

    private void togglePlay() {
        if (engine == null) return;
        if (playing) {
            pause();
            return;
        }
        if (index >= total - 1) goTo(0, false);
        playing = true;
        updatePlayButton();
        dwellLeft = 500 / speedFactor();
        startClock();
    }

    private void pause() {
        playing = false;
        updatePlayButton();
    }

    private void updatePlayButton() {
        boolean atEnd = engine != null && index >= total - 1;
        playBtn.setText(playing ? "Pause" : atEnd ? "Replay" : "Play");
        playBtn.setGlyph(playing ? Glyph.PAUSE : atEnd ? Glyph.RESTART : Glyph.PLAY);
    }

    // ---- accessors used by tests
    CountingSortEngine engineForTest() { return engine; }
    int stepIndex() { return index; }
    int stepCount() { return total; }
    void setSpeedIndex(int i) { speedIdx = i; }
    void startPlaying() { togglePlay(); }
}
