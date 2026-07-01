# FabSentry

A SECS/GEM Equipment Simulator and Monitoring Console

FabSentry is a Java Swing desktop application that simulates semiconductor fab equipment using SECS/GEM concepts, and a monitoring console (host) that connects to many tools at once, tracks their state, raises and handles alarms, measures OEE, detects anomalies, and keeps an audit trail in a database.

This is a simulator and monitor, not production fab software. It does not talk to real hardware. It is built to demonstrate SECS/GEM domain knowledge, Java Swing UI work, and clean software design.

## What it does

- Simulates fab equipment (Etcher, CVD, CMP, Litho), each as its own process on its own port.
- Each tool runs a GEM-style state machine: IDLE, SETUP, PROCESSING, PAUSED, COMPLETE, DOWN.
- A monitoring console (host) connects to running tools using an HSMS-style TCP link.
- Tools report state changes as SECS-II style event messages (S6F11). The host sends remote commands (S2F41) and gets back an ACK or NAK (S2F42).
- S5 alarms: tools raise and clear alarms (S5F1), both automatically (random process anomaly) and by manual command.
- Alarm banner in the UI with Clear Alarm and Cancel, plus feedback when a command is rejected in the current state.
- Audit store: every state change and every alarm is saved to an H2 database on disk.
- OEE metrics per tool: Availability, Performance, Quality, and the overall OEE score.
- Anomaly detection on cycle times using a z-score method, marked on the chart and logged.
- View History button and the H2 web console to inspect the saved data.
- Live per-tool dashboard: state banner, process flow bar, state model diagram, wafer view, timing cards, OEE card, and a cycle time trend chart.
- Multi-tool support: add tools from a dropdown, see them all in a list, click to switch.
- Dark and light themes.

## SECS/GEM concepts shown

- SECS-II style messages (stream/function, W-bit, message body).
- HSMS-style framing over TCP sockets.
- GEM equipment states and legal state transitions.
- Event reporting (S6F11), remote commands (S2F41 / S2F42), and alarms (S5F1).
- Host and equipment roles (console = host, simulator = equipment).

Note: message bodies are simplified (pipe-delimited text), not full binary SECS-II encoding. This is a learning simulator, so the protocol is GEM-flavored but simplified.

## OEE metrics

OEE means Overall Equipment Effectiveness, a core fab KPI. It is three parts multiplied:

- Availability = up time / total time. Down time is measured from DOWN state periods.
- Performance = ideal cycle time / actual average cycle time.
- Quality = good cycles / total cycles. A cycle that ends in an anomaly or DOWN counts as bad.
- OEE = Availability x Performance x Quality.

These are computed from real data the app already collects (state timestamps and cycle times). The OEE card colors green, amber, or red based on the score.

## Anomaly detection

FabSentry uses a simple statistical method on each tool's cycle times:

- It tracks the mean and standard deviation of past cycles.
- For each new cycle it computes a z-score: (cycle - mean) / standard deviation.
- If the z-score is 2.5 or more, the cycle is flagged as an anomaly.
- Anomalies are marked as red dots on the cycle time chart, shown as a note in the banner, and logged to the audit store.

This is a lightweight, explainable form of predictive maintenance. A natural next step would be a model such as isolation forest, run as a separate service.

## Tech stack

- Java 17, Maven
- Swing with FlatLaf (modern look and feel)
- JFreeChart (cycle time chart)
- H2 database (audit store)
- SLF4J / Logback (logging)
- JUnit 5 (tests)

## Architecture and design

FabSentry is split into two runnable parts and clear layers.

Two runnable parts:

- ToolSimulator (the equipment): a small TCP server. One process per tool, each on its own port. It runs the GEM state machine, cycles through states, and broadcasts events and alarms.
- ConsoleApp (the host): the Swing UI. It connects to running tools, sends commands, and shows live state, timing, OEE, and anomalies.

Layers (packages):

- protocol: the SECS-II style message model (SecsMessage) and helpers (SecsMessages) for building and detecting message types.
- transport: HSMS-style framing over TCP (SecsConnection) with length-prefixed frames.
- domain: the tool state enum (ToolState) and the thread-safe GEM state machine (GemStateMachine) with legal transition rules.
- tool: the equipment simulator (ToolSimulator).
- console: the monitoring UI (ConsoleApp) and per-tool state holder (ToolSession), plus custom UI panels (flow bar, wafer, chart, state diagram).
- audit: a storage interface (AuditStore) and an H2 implementation (H2AuditStore).

Key design choices:

- Host and equipment are separate processes that talk only through the SECS-style protocol over TCP. This mirrors the real host/equipment split.
- Each tool has its own ToolSession in the console, holding its own state, timing, cycle history, alarms, and anomaly flags. This makes multi-tool support clean.
- The audit store is behind an interface. Today it uses H2. To scale up later, an implementation for a server database like PostgreSQL can be swapped in without changing the rest of the app.
- UI panels are custom-painted for the flow bar, wafer, and state diagram, and theme-aware for dark and light modes.

## Project structure

- protocol/ - SECS-II style message model and helpers
- transport/ - HSMS-style connection framing over TCP
- domain/ - tool state enum and GEM state machine
- tool/ - the equipment simulator (TCP server)
- console/ - the monitoring console (Swing UI)
- audit/ - audit store interface and H2 implementation

## How to run

Build:

    mvn -o clean compile

Start the tools you want (each in the background):

    mvn -o exec:java -Dexec.mainClass=com.fabsim.tool.ToolSimulator -Dexec.args="Etcher-1 ETCHER 5555" &
    mvn -o exec:java -Dexec.mainClass=com.fabsim.tool.ToolSimulator -Dexec.args="CVD-1 CVD 5556" &
    mvn -o exec:java -Dexec.mainClass=com.fabsim.tool.ToolSimulator -Dexec.args="CMP-1 CMP 5557" &
    mvn -o exec:java -Dexec.mainClass=com.fabsim.tool.ToolSimulator -Dexec.args="Litho-1 LITHO 5558" &

Or use the helper script:

    ./start-tools.sh

Start the console:

    mvn -o exec:java -Dexec.mainClass=com.fabsim.console.ConsoleApp

In the console: pick a tool type in the dropdown, click Add Tool, then click a tool in the list and press Start Auto Cycle.

Stop all tools:

    ./stop-tools.sh

## Alarms

- Tools may raise an alarm automatically when a random process anomaly happens (the tool goes DOWN).
- You can also raise or clear an alarm by hand using the Raise Alarm and Clear Alarm buttons.
- When a tool has an active alarm, a red banner appears with the alarm text, plus Clear Alarm and Cancel.
- Recovery flow: Clear Alarm, then Recover (DOWN to IDLE), then Start Auto Cycle.

## Audit store and viewing the database

Every state change and alarm is saved to an H2 database file named fabsentry-audit.mv.db in the project folder.

Two ways to see the data:

1. In the app, click View History to see the last saved state changes.

2. Open the H2 web console:

       mvn -o exec:java -Dexec.mainClass=org.h2.tools.Console

   This opens a page in your browser (usually http://localhost:8082). Connect with:

   - JDBC URL: jdbc:h2:./fabsentry-audit;AUTO_SERVER=TRUE
   - User Name: sa
   - Password: (leave blank)

   Then query, for example:

       SELECT * FROM state_changes ORDER BY id DESC;
       SELECT * FROM alarms ORDER BY id DESC;

## Tests

    mvn -o test

## Ports

- Etcher: 5555
- CVD: 5556
- CMP: 5557
- Litho: 5558

## Roadmap

Planned or possible next steps:

- Real ML anomaly model (isolation forest) as a separate service.
- Yield correlation between equipment events and downstream results.
- Reliability: auto-reconnect and safer message handling.
- Configurable ports, tool names, and timings via a config file.
- Message bus (Kafka) telemetry path to show how it scales to many tools.

## Scope and honesty

FabSentry is a simulator built to show understanding of SECS/GEM, Java Swing, and clean design. It is not production software and does not connect to real equipment. The protocol is simplified. Real fab host software adds full binary SECS-II encoding, security, high-availability, and integration with MES and databases.
