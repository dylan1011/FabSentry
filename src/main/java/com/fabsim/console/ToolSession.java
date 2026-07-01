package com.fabsim.console;

import com.fabsim.domain.ToolState;
import com.fabsim.protocol.SecsMessage;
import com.fabsim.protocol.SecsMessages;
import com.fabsim.transport.SecsConnection;
import com.fabsim.audit.AuditStore;

import java.io.IOException;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class ToolSession {

    public interface UpdateListener {
        void onUpdate(ToolSession session);
    }

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final String name;
    private final String type;
    private final int port;

    private SecsConnection connection;
    private Thread reader;
    private volatile boolean connected = false;

    private volatile ToolState state = ToolState.IDLE;
    private final StringBuilder logText = new StringBuilder();
    private final List<Double> cycleTimes = new ArrayList<>();

    private ToolState lastState = null;
    private long cycleStartMillis = 0;
    private long cycleCount = 0;
    private long totalCycleMillis = 0;
    private long minCycleMillis = Long.MAX_VALUE;
    private long maxCycleMillis = 0;
    private long lastCycleMillis = 0;
    private long currentCycleMillis = 0;
    private boolean drift = false;
    private volatile boolean alarmActive = false;
    private volatile String alarmText = "";
    private volatile String lastCommandResult = "";

    private UpdateListener listener;
    private AuditStore auditStore;

    public ToolSession(String name, String type, int port) {
        this.name = name;
        this.type = type;
        this.port = port;
    }

    public void setListener(UpdateListener l) { this.listener = l; }
    public void setAuditStore(AuditStore store) { this.auditStore = store; }

    public String name() { return name; }
    public String type() { return type; }
    public int port() { return port; }
    public boolean isConnected() { return connected; }
    public ToolState state() { return state; }
    public String logText() { return logText.toString(); }
    public List<Double> cycleTimes() { return cycleTimes; }
    public long cycleCount() { return cycleCount; }
    public long lastCycleMillis() { return lastCycleMillis; }
    public long currentCycleMillis() { return currentCycleMillis; }
    public long minCycleMillis() { return minCycleMillis == Long.MAX_VALUE ? 0 : minCycleMillis; }
    public long maxCycleMillis() { return maxCycleMillis; }
    public long avgCycleMillis() { return cycleCount == 0 ? 0 : totalCycleMillis / cycleCount; }
    public boolean drift() { return drift; }
    public boolean alarmActive() { return alarmActive; }
    public String alarmText() { return alarmText; }
    public String lastCommandResult() { return lastCommandResult; }

    public boolean connect() {
        try {
            connection = new SecsConnection(new Socket("localhost", port));
            connected = true;
            log("Connected to " + name + " on port " + port);
            startReader();
            return true;
        } catch (IOException ex) {
            log("Connect failed: " + ex.getMessage());
            return false;
        }
    }

    private void startReader() {
        reader = new Thread(() -> {
            try {
                while (connection.isOpen()) {
                    SecsMessage m = connection.receive();
                    if (SecsMessages.isEventReport(m)) {
                        handleEvent(m.body());
                    } else if (SecsMessages.isAlarmReport(m)) {
                        handleAlarm(m.body());
                    } else if (SecsMessages.isCommandAck(m)) {
                        boolean ok = m.body().equals("ACK");
                        lastCommandResult = ok ? "" : "Command rejected by tool (not allowed in current state)";
                        if (!ok) log("COMMAND REJECTED by tool");
                        fire();
                    } else if (SecsMessages.isOnLineData(m)) {
                        log("Tool identity: " + m.body().replace("|", " rev "));
                        fire();
                    }
                }
            } catch (IOException ex) {
                connected = false;
                log("Disconnected: " + ex.getMessage());
                fire();
            }
        }, "reader-" + name);
        reader.setDaemon(true);
        reader.start();
    }

    private void handleAlarm(String body) {
        // body format: ALARM|SET|<id>|<text>
        String[] parts = body.split("\\|");
        boolean set = parts.length > 1 && parts[1].equals("SET");
        String id = parts.length > 2 ? parts[2] : "";
        String text = parts.length > 3 ? parts[3] : "";
        alarmActive = set;
        alarmText = set ? (id + ": " + text) : "";
        if (auditStore != null) auditStore.recordAlarm(name, set, id, text);
        log((set ? "ALARM SET " : "ALARM CLEAR ") + id + "  (" + text + ")");
        fire();
    }

    private void handleEvent(String body) {
        String[] parts = body.split("\\|");
        String transition = parts.length > 1 ? parts[1] : "";
        String reason = parts.length > 2 ? parts[2] : "";
        int gt = transition.indexOf('>');
        String toName = (gt >= 0 ? transition.substring(gt + 1) : transition).trim();
        log("EVENT " + transition + "  (" + reason + ")");
        try {
            ToolState to = ToolState.valueOf(toName);
            ToolState from = state;
            state = to;
            if (auditStore != null) auditStore.recordStateChange(name, String.valueOf(from), to.name(), reason);
            updateTiming(to);
        } catch (IllegalArgumentException ex) {
            log("PARSE FAIL toName=[" + toName + "]");
        }
        fire();
    }

    private void updateTiming(ToolState to) {
        long now = System.currentTimeMillis();
        if (to == ToolState.SETUP && (lastState == ToolState.IDLE || lastState == null)) {
            cycleStartMillis = now;
        }
        if (to == ToolState.IDLE && cycleStartMillis > 0) {
            long cycle = now - cycleStartMillis;
            lastCycleMillis = cycle;
            cycleCount++;
            totalCycleMillis += cycle;
            if (cycle < minCycleMillis) minCycleMillis = cycle;
            if (cycle > maxCycleMillis) maxCycleMillis = cycle;
            cycleTimes.add(cycle / 1000.0);
            double avg = totalCycleMillis / (double) cycleCount;
            drift = (cycleCount >= 3 && cycle > avg * 1.5);
            cycleStartMillis = 0;
        }
        if (cycleStartMillis > 0) {
            currentCycleMillis = now - cycleStartMillis;
        }
        lastState = to;
    }

    public void sendCommand(String cmd) {
        if (connection == null) return;
        try {
            connection.send(SecsMessages.s2f41(cmd));
            log("SEND command " + cmd);
            fire();
        } catch (IOException ex) {
            log("Command failed: " + ex.getMessage());
        }
    }

    private void log(String message) {
        logText.append("[").append(LocalTime.now().format(TIME)).append("] ").append(message).append("\n");
    }

    private void fire() {
        if (listener != null) listener.onUpdate(this);
    }

    public static boolean isPortAvailable(int port) {
        try (Socket s = new Socket()) {
            s.connect(new java.net.InetSocketAddress("localhost", port), 200);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
