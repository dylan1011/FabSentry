package com.fabsim.console;

import com.fabsim.domain.ToolState;
import com.fabsim.audit.AuditStore;
import com.fabsim.audit.H2AuditStore;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ConsoleApp extends JFrame {

    private static final Map<String, Integer> PORTS = new LinkedHashMap<>();
    static {
        PORTS.put("ETCHER", 5555);
        PORTS.put("CVD", 5556);
        PORTS.put("CMP", 5557);
        PORTS.put("LITHO", 5558);
    }

    private final JComboBox<String> typeDropdown = new JComboBox<>(new String[]{"ETCHER", "CVD", "CMP", "LITHO"});
    private final RoundButton addButton = new RoundButton("Add Tool");
    private final JToggleButton themeToggle = new JToggleButton("Light Mode");

    private final DefaultListModel<ToolSession> toolListModel = new DefaultListModel<>();
    private final JList<ToolSession> toolList = new JList<>(toolListModel);

    private final JLabel stateBanner = new JLabel("NO TOOL", SwingConstants.CENTER);
    private final StateDiagramPanel diagram = new StateDiagramPanel();
    private final FlowBarPanel flowBar = new FlowBarPanel();
    private final WaferPanel wafer = new WaferPanel();
    private final CycleChartPanel cycleChart = new CycleChartPanel();
    private final JTextArea eventLog = new JTextArea();

    private final RoundButton startAutoButton = new RoundButton("Start Auto Cycle");
    private final RoundButton stopAutoButton = new RoundButton("Stop Auto Cycle");
    private final RoundButton pauseButton = new RoundButton("Pause");
    private final RoundButton resumeButton = new RoundButton("Resume");
    private final RoundButton downButton = new RoundButton("Set Down");
    private final RoundButton recoverButton = new RoundButton("Recover");
    private final RoundButton raiseAlarmButton = new RoundButton("Raise Alarm");
    private final RoundButton clearAlarmButton = new RoundButton("Clear Alarm");
    private final RoundButton historyButton = new RoundButton("View History");

    private final JLabel lastCycleValue = new JLabel("-");
    private final JLabel avgCycleValue = new JLabel("-");
    private final JLabel minCycleValue = new JLabel("-");
    private final JLabel maxCycleValue = new JLabel("-");
    private final JLabel cycleCountValue = new JLabel("0");
    private final JLabel currentCycleValue = new JLabel("-");
    private final JLabel driftLabel = new JLabel(" ");
    private final JLabel oeeValue = new JLabel("-");
    private final JLabel availValue = new JLabel("-");
    private final JLabel perfValue = new JLabel("-");
    private final JLabel qualValue = new JLabel("-");
    private final JLabel alarmLabel = new JLabel(" ");
    private final JPanel alarmBanner = new JPanel(new BorderLayout(8, 0));
    private final RoundButton bannerClearButton = new RoundButton("Clear Alarm");
    private final RoundButton bannerCancelButton = new RoundButton("Cancel");
    private boolean bannerCancelled = false;

    private CardPanel diagramCard;
    private CardPanel timingCard;
    private CardPanel chartCard;
    private CardPanel oeeCard;
    private CardPanel cmdCard;
    private CardPanel logCard;
    private CardPanel listCard;

    private JPanel centerPanel;
    private JPanel topBarPanel;
    private JPanel leftTopPanel;
    private final AuditStore auditStore = new H2AuditStore();
    private boolean darkMode = true;
    private ToolSession selected;
    private int shownChartCount = 0;

    public ConsoleApp() {
        super("Fab Equipment Monitoring Console");
        buildUi();
        wireActions();
        setCommandsEnabled(false);
        cycleChart.applyTheme(true);
    }

    private final java.util.List<JPanel> statInners = new java.util.ArrayList<>();
    private final java.util.List<JLabel> statValues = new java.util.ArrayList<>();

    private JPanel statCard(String label, JLabel valueLabel, Color strip) {
        JPanel card = new JPanel(new BorderLayout());
        card.setOpaque(false);
        card.setBorder(new MatteBorder(3, 0, 0, 0, strip));
        JPanel inner = new JPanel(new BorderLayout());
        inner.setOpaque(true);
        inner.setBorder(new EmptyBorder(8, 10, 8, 10));
        JLabel top = new JLabel(label);
        top.setForeground(new Color(150, 154, 164));
        top.setFont(top.getFont().deriveFont(11f));
        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD, 18f));
        inner.add(top, BorderLayout.NORTH);
        inner.add(valueLabel, BorderLayout.CENTER);
        card.add(inner, BorderLayout.CENTER);
        statInners.add(inner);
        statValues.add(valueLabel);
        applyStatColor(inner, valueLabel);
        return card;
    }

    private void applyStatColor(JPanel inner, JLabel value) {
        inner.setBackground(darkMode ? new Color(28, 31, 37) : new Color(255, 255, 255));
        value.setForeground(darkMode ? new Color(230, 232, 238) : new Color(30, 34, 44));
    }

    private void refreshStatColors() {
        for (int i = 0; i < statInners.size(); i++) {
            applyStatColor(statInners.get(i), statValues.get(i));
        }
    }

    private JPanel miniStat(String label, JLabel valueLabel) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        JLabel top = new JLabel(label, SwingConstants.CENTER);
        top.setForeground(new Color(150, 154, 164));
        top.setFont(top.getFont().deriveFont(11f));
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);
        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD, 15f));
        p.add(top, BorderLayout.NORTH);
        p.add(valueLabel, BorderLayout.CENTER);
        return p;
    }

    private JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(new Color(150, 154, 164));
        l.setFont(l.getFont().deriveFont(Font.BOLD, 12f));
        l.setBorder(new EmptyBorder(0, 2, 6, 0));
        return l;
    }

    private void buildUi() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1360, 880);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(12, 12));
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(14, 14, 14, 14));

        JPanel topBar = new JPanel(new BorderLayout());
        this.topBarPanel = topBar;
        JLabel title = new JLabel("SECS/GEM Equipment Monitor");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 17f));
        JPanel leftTop = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        this.leftTopPanel = leftTop;
        leftTop.add(title);
        leftTop.add(new JLabel("   Add:"));
        typeDropdown.setPreferredSize(new Dimension(120, 34));
        leftTop.add(typeDropdown);
        leftTop.add(addButton);
        themeToggle.setFocusPainted(false);
        themeToggle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        topBar.add(leftTop, BorderLayout.WEST);
        topBar.add(themeToggle, BorderLayout.EAST);

        toolList.setCellRenderer(new ToolListRenderer());
        toolList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        toolList.setOpaque(false);
        toolList.setFixedCellHeight(46);
        listCard = new CardPanel(new BorderLayout(6, 6));
        listCard.add(sectionLabel("EQUIPMENT"), BorderLayout.NORTH);
        listCard.add(toolList, BorderLayout.CENTER);
        JPanel leftCol = new JPanel(new BorderLayout());
        leftCol.setOpaque(false);
        leftCol.setPreferredSize(new Dimension(220, 100));
        leftCol.add(listCard, BorderLayout.CENTER);

        stateBanner.setFont(stateBanner.getFont().deriveFont(Font.BOLD, 26f));
        stateBanner.setOpaque(true);
        stateBanner.setBackground(new Color(107, 114, 128));
        stateBanner.setForeground(Color.WHITE);
        stateBanner.setBorder(new EmptyBorder(16, 10, 16, 10));

        diagramCard = new CardPanel(new BorderLayout(10, 6));
        JPanel diagRow = new JPanel(new GridLayout(1, 2, 12, 0));
        diagRow.setOpaque(false);
        JPanel treeWrap = new JPanel(new BorderLayout());
        treeWrap.setOpaque(false);
        treeWrap.add(sectionLabel("STATE MODEL"), BorderLayout.NORTH);
        treeWrap.add(diagram, BorderLayout.CENTER);
        JPanel waferWrap = new JPanel(new BorderLayout());
        waferWrap.setOpaque(false);
        waferWrap.add(sectionLabel("WAFER"), BorderLayout.NORTH);
        waferWrap.add(wafer, BorderLayout.CENTER);
        diagRow.add(treeWrap);
        diagRow.add(waferWrap);
        diagramCard.add(diagRow, BorderLayout.CENTER);

        JPanel cards = new JPanel(new GridLayout(2, 3, 10, 10));
        cards.setOpaque(false);
        cards.add(statCard("Last cycle", lastCycleValue, new Color(47, 127, 224)));
        cards.add(statCard("Average", avgCycleValue, new Color(47, 168, 79)));
        cards.add(statCard("Current", currentCycleValue, new Color(124, 92, 214)));
        cards.add(statCard("Min cycle", minCycleValue, new Color(47, 168, 79)));
        cards.add(statCard("Max cycle", maxCycleValue, new Color(224, 164, 35)));
        cards.add(statCard("Cycles done", cycleCountValue, new Color(47, 127, 224)));

        timingCard = new CardPanel(new BorderLayout(6, 6));
        timingCard.add(sectionLabel("TIMING ANALYSIS"), BorderLayout.NORTH);
        timingCard.add(cards, BorderLayout.CENTER);
        driftLabel.setForeground(new Color(224, 164, 35));
        driftLabel.setBorder(new EmptyBorder(4, 2, 0, 2));
        timingCard.add(driftLabel, BorderLayout.SOUTH);

        chartCard = new CardPanel(new BorderLayout(6, 6));
        chartCard.add(sectionLabel("CYCLE TIME TREND"), BorderLayout.NORTH);
        chartCard.add(cycleChart, BorderLayout.CENTER);

        oeeCard = new CardPanel(new BorderLayout(6, 6));
        oeeCard.add(sectionLabel("OEE  (OVERALL EQUIPMENT EFFECTIVENESS)"), BorderLayout.NORTH);
        JPanel oeeCenter = new JPanel(new BorderLayout(4, 4));
        oeeCenter.setOpaque(false);
        oeeValue.setFont(oeeValue.getFont().deriveFont(Font.BOLD, 34f));
        oeeValue.setHorizontalAlignment(SwingConstants.CENTER);
        oeeCenter.add(oeeValue, BorderLayout.NORTH);
        JPanel oeeParts = new JPanel(new GridLayout(1, 3, 10, 0));
        oeeParts.setOpaque(false);
        oeeParts.add(miniStat("Availability", availValue));
        oeeParts.add(miniStat("Performance", perfValue));
        oeeParts.add(miniStat("Quality", qualValue));
        oeeCenter.add(oeeParts, BorderLayout.CENTER);
        oeeCard.add(oeeCenter, BorderLayout.CENTER);

        JPanel center = new JPanel();
        center.setOpaque(false);
        this.centerPanel = center;
        center.setOpaque(true);
        center.setBackground(new Color(13, 15, 18));
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        stateBanner.setAlignmentX(Component.LEFT_ALIGNMENT);
        flowBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        diagramCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        timingCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        chartCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        stateBanner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        flowBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        center.add(stateBanner);
        alarmLabel.setForeground(Color.WHITE);
        alarmLabel.setFont(alarmLabel.getFont().deriveFont(Font.BOLD, 13f));
        bannerClearButton.setPreferredSize(new Dimension(120, 30));
        bannerCancelButton.setPreferredSize(new Dimension(90, 30));
        JPanel bannerButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        bannerButtons.setOpaque(false);
        bannerButtons.add(bannerClearButton);
        bannerButtons.add(bannerCancelButton);
        alarmBanner.setBackground(new Color(214, 69, 69));
        alarmBanner.setBorder(new EmptyBorder(8, 12, 8, 12));
        alarmBanner.add(alarmLabel, BorderLayout.WEST);
        alarmBanner.add(bannerButtons, BorderLayout.EAST);
        alarmBanner.setAlignmentX(Component.LEFT_ALIGNMENT);
        alarmBanner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        alarmBanner.setVisible(false);
        center.add(Box.createVerticalStrut(8));
        center.add(alarmBanner);
        center.add(Box.createVerticalStrut(10));
        center.add(flowBar);
        center.add(Box.createVerticalStrut(10));
        center.add(diagramCard);
        center.add(Box.createVerticalStrut(10));
        center.add(timingCard);
        center.add(Box.createVerticalStrut(10));
        center.add(chartCard);
        center.add(Box.createVerticalStrut(10));
        oeeCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        center.add(oeeCard);

        JScrollPane centerScroll = new JScrollPane(center);
        centerScroll.setBorder(null);
        centerScroll.setOpaque(false);
        centerScroll.getViewport().setOpaque(false);
        centerScroll.getVerticalScrollBar().setUnitIncrement(16);

        cmdCard = new CardPanel(new GridLayout(0, 1, 8, 8));
        cmdCard.add(startAutoButton);
        cmdCard.add(stopAutoButton);
        cmdCard.add(pauseButton);
        cmdCard.add(resumeButton);
        cmdCard.add(downButton);
        cmdCard.add(recoverButton);
        cmdCard.add(raiseAlarmButton);
        cmdCard.add(clearAlarmButton);
        cmdCard.add(historyButton);

        eventLog.setEditable(false);
        eventLog.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        eventLog.setBackground(new Color(22, 24, 29));
        eventLog.setForeground(new Color(210, 213, 220));
        logCard = new CardPanel(new BorderLayout(6, 6));
        logCard.add(sectionLabel("EVENT TIMELINE (S6F11)"), BorderLayout.NORTH);
        JScrollPane logScroll = new JScrollPane(eventLog);
        logScroll.setBorder(null);
        logCard.add(logScroll, BorderLayout.CENTER);

        JPanel rightCol = new JPanel(new BorderLayout(10, 10));
        rightCol.setOpaque(false);
        rightCol.setPreferredSize(new Dimension(340, 100));
        rightCol.add(cmdCard, BorderLayout.NORTH);
        rightCol.add(logCard, BorderLayout.CENTER);

        add(topBar, BorderLayout.NORTH);
        add(leftCol, BorderLayout.WEST);
        add(centerScroll, BorderLayout.CENTER);
        add(rightCol, BorderLayout.EAST);
    }

    private void wireActions() {
        addButton.addActionListener(e -> addTool());
        toolList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) selectTool(toolList.getSelectedValue());
        });
        startAutoButton.addActionListener(e -> sendCmd("START_AUTO"));
        stopAutoButton.addActionListener(e -> sendCmd("STOP_AUTO"));
        pauseButton.addActionListener(e -> sendCmd("PAUSE"));
        resumeButton.addActionListener(e -> sendCmd("RESUME"));
        downButton.addActionListener(e -> sendCmd("DOWN"));
        recoverButton.addActionListener(e -> sendCmd("RECOVER"));
        raiseAlarmButton.addActionListener(e -> sendCmd("RAISE_ALARM"));
        clearAlarmButton.addActionListener(e -> sendCmd("CLEAR_ALARM"));
        bannerClearButton.addActionListener(e -> { sendCmd("CLEAR_ALARM"); alarmBanner.setVisible(false); });
        bannerCancelButton.addActionListener(e -> { bannerCancelled = true; alarmBanner.setVisible(false); });
        historyButton.addActionListener(e -> showHistory());
        themeToggle.addActionListener(e -> toggleTheme());
    }

    private void addTool() {
        String type = (String) typeDropdown.getSelectedItem();
        int port = PORTS.get(type);

        for (int i = 0; i < toolListModel.size(); i++) {
            if (toolListModel.get(i).port() == port) {
                JOptionPane.showMessageDialog(this, type + " is already added.");
                return;
            }
        }
        if (!ToolSession.isPortAvailable(port)) {
            JOptionPane.showMessageDialog(this,
                type + " is not running on port " + port + ".\nStart it first, then Add.");
            return;
        }
        ToolSession session = new ToolSession(type + "-1", type, port);
        session.setAuditStore(auditStore);
        session.setListener(s -> SwingUtilities.invokeLater(() -> {
            toolList.repaint();
            if (s == selected) refreshSelected();
        }));
        if (session.connect()) {
            toolListModel.addElement(session);
            toolList.setSelectedValue(session, true);
        }
    }

    private void selectTool(ToolSession session) {
        this.selected = session;
        this.bannerCancelled = false;
        this.shownChartCount = 0;
        cycleChart.reset();
        if (session == null) {
            setCommandsEnabled(false);
            return;
        }
        setCommandsEnabled(true);
        refreshSelected();
    }

    private void refreshSelected() {
        if (selected == null) return;
        ToolState s = selected.state();
        diagram.setCurrent(s);
        flowBar.setCurrent(s);
        wafer.setCurrent(s);
        stateBanner.setText(selected.name() + "  |  " + s.name());
        stateBanner.setBackground(colorFor(s));

        lastCycleValue.setText(fmt(selected.lastCycleMillis()));
        avgCycleValue.setText(fmt(selected.avgCycleMillis()));
        minCycleValue.setText(fmt(selected.minCycleMillis()));
        maxCycleValue.setText(fmt(selected.maxCycleMillis()));
        currentCycleValue.setText(fmt(selected.currentCycleMillis()));
        cycleCountValue.setText(String.valueOf(selected.cycleCount()));
        driftLabel.setText(selected.drift() ? "DRIFT ALERT: last cycle much slower than average" : " ");
        double oee = selected.oee();
        oeeValue.setText(String.format("%.0f%%", oee * 100));
        if (oee >= 0.85) oeeValue.setForeground(new Color(47, 168, 79));
        else if (oee >= 0.60) oeeValue.setForeground(new Color(224, 164, 35));
        else oeeValue.setForeground(new Color(214, 69, 69));
        availValue.setText(String.format("%.0f%%", selected.availability() * 100));
        perfValue.setText(String.format("%.0f%%", selected.performance() * 100));
        qualValue.setText(String.format("%.0f%%", selected.quality() * 100));
        String reject = selected.lastCommandResult();
        if (selected.alarmActive()) {
            if (!bannerCancelled) {
                alarmLabel.setText("ALARM  -  " + selected.alarmText());
                alarmBanner.setBackground(new Color(214, 69, 69));
                alarmBanner.setVisible(true);
            }
        } else if (selected.lastCycleAnomaly()) {
            alarmLabel.setText(selected.anomalyNote());
            alarmBanner.setBackground(new Color(224, 120, 35));
            bannerClearButton.setVisible(false);
            alarmBanner.setVisible(true);
        } else if (reject != null && !reject.isEmpty()) {
            alarmLabel.setText(reject);
            alarmBanner.setBackground(new Color(224, 120, 35));
            bannerClearButton.setVisible(false);
            alarmBanner.setVisible(true);
        } else {
            alarmBanner.setVisible(false);
            bannerClearButton.setVisible(true);
            bannerCancelled = false;
        }

        java.util.List<Double> times = selected.cycleTimes();
        java.util.List<Boolean> anomalies = selected.cycleAnomalies();
        while (shownChartCount < times.size()) {
            boolean anom = shownChartCount < anomalies.size() && anomalies.get(shownChartCount);
            cycleChart.addCycle(times.get(shownChartCount), anom);
            shownChartCount++;
        }

        eventLog.setText(selected.logText());
        eventLog.setCaretPosition(eventLog.getDocument().getLength());
    }

    private void showHistory() {
        java.util.List<String> rows = auditStore.recentEvents(50);
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        if (rows.isEmpty()) {
            area.setText("No saved events yet.");
        } else {
            StringBuilder sb = new StringBuilder();
            for (String r : rows) sb.append(r).append("\n");
            area.setText(sb.toString());
        }
        JScrollPane sp = new JScrollPane(area);
        sp.setPreferredSize(new Dimension(600, 400));
        JOptionPane.showMessageDialog(this, sp, "Audit History (last 50 state changes)", JOptionPane.PLAIN_MESSAGE);
    }

    private void sendCmd(String cmd) {
        if (selected != null) selected.sendCommand(cmd);
    }

    private void toggleTheme() {
        darkMode = !darkMode;
        try {
            if (darkMode) {
                UIManager.setLookAndFeel(new FlatDarkLaf());
                applyDarkTweaks();
                themeToggle.setText("Light Mode");
            } else {
                UIManager.setLookAndFeel(new FlatLightLaf());
                applyLightTweaks();
                themeToggle.setText("Dark Mode");
            }
            SwingUtilities.updateComponentTreeUI(this);
            Color pageBg = darkMode ? new Color(13, 15, 18) : new Color(255, 255, 255);
            getContentPane().setBackground(pageBg);
            if (centerPanel != null) { centerPanel.setOpaque(true); centerPanel.setBackground(pageBg); }
            if (topBarPanel != null) { topBarPanel.setOpaque(true); topBarPanel.setBackground(pageBg); }
            if (leftTopPanel != null) { leftTopPanel.setOpaque(true); leftTopPanel.setBackground(pageBg); }
            diagram.setDarkMode(darkMode);
            flowBar.setDarkMode(darkMode);
            wafer.setDarkMode(darkMode);
            diagramCard.setDarkMode(darkMode);
            timingCard.setDarkMode(darkMode);
            chartCard.setDarkMode(darkMode);
            oeeCard.setDarkMode(darkMode);
            cmdCard.setDarkMode(darkMode);
            logCard.setDarkMode(darkMode);
            listCard.setDarkMode(darkMode);
            startAutoButton.setDarkMode(darkMode);
            stopAutoButton.setDarkMode(darkMode);
            pauseButton.setDarkMode(darkMode);
            resumeButton.setDarkMode(darkMode);
            downButton.setDarkMode(darkMode);
            recoverButton.setDarkMode(darkMode);
            raiseAlarmButton.setDarkMode(darkMode);
            clearAlarmButton.setDarkMode(darkMode);
            historyButton.setDarkMode(darkMode);
            bannerClearButton.setDarkMode(darkMode);
            bannerCancelButton.setDarkMode(darkMode);
            refreshStatColors();
            eventLog.setBackground(darkMode ? new Color(22, 24, 29) : new Color(255, 255, 255));
            eventLog.setForeground(darkMode ? new Color(210, 213, 220) : new Color(40, 44, 54));
            cycleChart.applyTheme(darkMode);
        } catch (Exception ex) {
            eventLog.append("Theme switch failed: " + ex.getMessage() + "\n");
        }
    }

    private void setCommandsEnabled(boolean on) {
        startAutoButton.setEnabled(on);
        stopAutoButton.setEnabled(on);
        pauseButton.setEnabled(on);
        resumeButton.setEnabled(on);
        downButton.setEnabled(on);
        recoverButton.setEnabled(on);
        raiseAlarmButton.setEnabled(on);
        clearAlarmButton.setEnabled(on);
    }

    private String fmt(long millis) {
        return String.format("%.1fs", millis / 1000.0);
    }

    private Color colorFor(ToolState state) {
        switch (state) {
            case PROCESSING: return new Color(47, 168, 79);
            case PAUSED:     return new Color(224, 164, 35);
            case DOWN:       return new Color(214, 69, 69);
            case COMPLETE:   return new Color(47, 127, 224);
            case SETUP:      return new Color(124, 92, 214);
            default:         return new Color(107, 114, 128);
        }
    }

    private static void applyLightTweaks() {
        UIManager.put("Panel.background", new Color(255, 255, 255));
        UIManager.put("control", new Color(245, 246, 248));
        UIManager.put("ScrollPane.background", new Color(255, 255, 255));
        UIManager.put("Viewport.background", new Color(255, 255, 255));
        UIManager.put("TextArea.background", new Color(248, 249, 251));
    }

    private static void applyDarkTweaks() {
        UIManager.put("Panel.background", new Color(13, 15, 18));
        UIManager.put("control", new Color(13, 15, 18));
        UIManager.put("ScrollPane.background", new Color(13, 15, 18));
        UIManager.put("Viewport.background", new Color(13, 15, 18));
        UIManager.put("TextArea.background", new Color(22, 24, 29));
    }

    private final class ToolListRenderer extends JPanel implements ListCellRenderer<ToolSession> {
        private final JLabel nameLabel = new JLabel();
        private final JLabel stateLabel = new JLabel();

        ToolListRenderer() {
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(6, 10, 6, 10));
            nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 13f));
            stateLabel.setFont(stateLabel.getFont().deriveFont(11f));
            JPanel text = new JPanel(new GridLayout(2, 1));
            text.setOpaque(false);
            text.add(nameLabel);
            text.add(stateLabel);
            add(text, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends ToolSession> list, ToolSession value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            setOpaque(true);
            setBackground(isSelected ? new Color(47, 127, 224) : new Color(0, 0, 0, 0));
            nameLabel.setText(value.name());
            nameLabel.setForeground(isSelected ? Color.WHITE : (darkMode ? new Color(220, 223, 230) : new Color(40, 44, 54)));
            String dot = value.isConnected() ? "\u25CF " : "\u25CB ";
            if (value.alarmActive()) {
                stateLabel.setText("\u25CF ALARM");
                stateLabel.setForeground(isSelected ? Color.WHITE : new Color(214, 69, 69));
            } else {
                stateLabel.setText(dot + value.state().name());
                stateLabel.setForeground(isSelected ? Color.WHITE : colorFor(value.state()));
            }
            return this;
        }
    }

    public static void main(String[] args) {
        FlatDarkLaf.setup();
        applyDarkTweaks();
        SwingUtilities.invokeLater(() -> new ConsoleApp().setVisible(true));
    }
}
