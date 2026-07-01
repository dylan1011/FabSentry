package com.fabsim.tool;

import com.fabsim.domain.GemStateMachine;
import com.fabsim.domain.ToolState;
import com.fabsim.protocol.SecsMessage;
import com.fabsim.protocol.SecsMessages;
import com.fabsim.transport.SecsConnection;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ToolSimulator {

    private final String name;
    private final String type;
    private final int port;
    private final long setupTime;
    private final long processBase;
    private final long processVar;

    private final ExecutorService pool = Executors.newCachedThreadPool();
    private final GemStateMachine gem = new GemStateMachine();
    private final CopyOnWriteArrayList<SecsConnection> clients = new CopyOnWriteArrayList<>();
    private final AtomicBoolean autoRunning = new AtomicBoolean(false);

    public ToolSimulator(String name, String type, int port) {
        this.name = name;
        this.type = type;
        this.port = port;
        switch (type.toUpperCase()) {
            case "ETCHER": setupTime = 1200; processBase = 2000; processVar = 3000; break;
            case "CVD":    setupTime = 1800; processBase = 4000; processVar = 4000; break;
            case "CMP":    setupTime = 1000; processBase = 3000; processVar = 2500; break;
            case "LITHO":  setupTime = 2000; processBase = 3500; processVar = 3500; break;
            default:       setupTime = 1500; processBase = 3000; processVar = 3000; break;
        }
    }

    public static void main(String[] args) throws IOException {
        String name = args.length > 0 ? args[0] : "Tool-1";
        String type = args.length > 1 ? args[1] : "ETCHER";
        int port = args.length > 2 ? Integer.parseInt(args[2]) : 5555;
        new ToolSimulator(name, type, port).start();
    }

    public void start() throws IOException {
        gem.addListener(new GemStateMachine.Listener() {
            @Override
            public void onTransition(ToolState from, ToolState to, String reason) {
                log("STATE " + from + " -> " + to + " (" + reason + ")");
                broadcast(SecsMessages.s6f11("StateChange", from + ">" + to + "|" + reason));
            }
            @Override
            public void onRejected(ToolState current, ToolState attempted) {
                log("REJECTED transition " + current + " -> " + attempted);
            }
        });

        pool.submit(this::autoCycleLoop);

        try (ServerSocket server = new ServerSocket(port)) {
            log(name + " (" + type + ") listening on port " + port + " (state=" + gem.current() + ")");
            while (true) {
                Socket socket = server.accept();
                log("Console connected from " + socket.getRemoteSocketAddress());
                SecsConnection conn = new SecsConnection(socket);
                clients.add(conn);
                pool.submit(() -> handle(conn));
            }
        }
    }

    private void handle(SecsConnection conn) {
        try {
            conn.send(SecsMessages.s1f2(name + " " + type, "1.0.0"));
            while (conn.isOpen()) {
                SecsMessage incoming = conn.receive();
                log("RECV " + incoming.sxfy());
                if (SecsMessages.isAreYouThere(incoming)) {
                    conn.send(SecsMessages.s1f2(name + " " + type, "1.0.0"));
                } else if (SecsMessages.isRemoteCommand(incoming)) {
                    boolean ok = handleCommand(incoming.body());
                    conn.send(SecsMessages.s2f42(ok));
                }
            }
        } catch (IOException e) {
            log("Console disconnected: " + e.getMessage());
        } finally {
            clients.remove(conn);
        }
    }

    private boolean handleCommand(String command) {
        switch (command) {
            case "START_AUTO":
                autoRunning.set(true);
                log("Auto-cycle enabled");
                return true;
            case "STOP_AUTO":
                autoRunning.set(false);
                log("Auto-cycle disabled");
                return true;
            case "PAUSE":
                return gem.transition(ToolState.PAUSED, "manual pause");
            case "RESUME":
                return gem.transition(ToolState.PROCESSING, "manual resume");
            case "DOWN":
                autoRunning.set(false);
                return gem.transition(ToolState.DOWN, "manual down");
            case "RECOVER":
                return gem.transition(ToolState.IDLE, "manual recover");
            default:
                log("Unknown command: " + command);
                return false;
        }
    }

    private void autoCycleLoop() {
        while (true) {
            try {
                if (!autoRunning.get()) {
                    Thread.sleep(300);
                    continue;
                }
                if (!advance()) {
                    Thread.sleep(400);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private boolean advance() throws InterruptedException {
        ToolState now = gem.current();
        switch (now) {
            case IDLE:
                return run(ToolState.SETUP, "auto: load wafer", setupTime);
            case SETUP:
                long extra = (long) (Math.random() * processVar);
                return run(ToolState.PROCESSING, "auto: begin process", processBase + extra);
            case PROCESSING:
                return run(ToolState.COMPLETE, "auto: process done", 1000);
            case COMPLETE:
                return run(ToolState.IDLE, "auto: unload wafer", 1000);
            case PAUSED:
            case DOWN:
            default:
                return false;
        }
    }

    private boolean run(ToolState target, String reason, long holdMillis) throws InterruptedException {
        if (!gem.transition(target, reason)) {
            return false;
        }
        Thread.sleep(holdMillis);
        return true;
    }

    private void broadcast(SecsMessage message) {
        for (SecsConnection conn : clients) {
            try {
                conn.send(message);
            } catch (IOException e) {
                clients.remove(conn);
            }
        }
    }

    private void log(String message) {
        System.out.println("[" + name + "] " + message);
    }
}
