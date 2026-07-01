# FabSentry

A SECS/GEM Equipment Simulator and Monitoring Console

FabSentry is a Java Swing desktop application that simulates semiconductor fab equipment using SECS/GEM concepts, and a monitoring console (host) that connects to many tools at once, tracks their state, raises and handles alarms, and keeps an audit trail in a database.

This is a simulator and monitor, not production fab software. It does not talk to real hardware. It is built to demonstrate SECS/GEM domain knowledge, Java Swing UI work, and clean software design.

## What it does

- Simulates fab equipment (Etcher, CVD, CMP, Litho), each as its own process on its own port.
- Each tool runs a GEM-style state machine: IDLE, SETUP, PROCESSING, PAUSED, COMPLETE, DOWN.
- A monitoring console (host) connects to running tools using an HSMS-style TCP link.
- Tools report state changes as SECS-II style event messages (S6F11). The host sends remote commands (S2F41) and gets back an ACK or NAK (S2F42).
- S5 alarms: tools raise and clear alarms (S5F1), both automatically (random process anomaly) and by manual command.
- Alarm banner in the UI with Clear Alarm and Cancel, plus feedback when a command is rejected in the current state.
- Audit store: every state change and every alarm is saved to an H2 database on disk.
- View History button shows the last saved events, and the H2 web console lets you query the data directly.
- Live per-tool dashboard: state banner, process flow bar, state model diagram, wafer view, timing cards, and a cycle time trend chart.
- Multi-tool support: add tools from a dropdown, see them all in a list, click to switch.
- Dark and light themes.

## SECS/GEM concepts shown

- SECS-II style messages (stream/function, W-bit, message body).
- HSMS-style framing over TCP sockets.
- GEM equipment states and legal state transitions.
- Event reporting (S6F11), remote commands (S2F41 / S2F42), and alarms (S5F1).
- Host and equipment roles (console = host, simulator = equipment).

Note: message bodies are simplified (pipe-delimited text), not full binary SECS-II encoding. This is a learning simulator, so the protocol is GEM-flavored but simplified.

## Tech stack

- Java 17, Maven
- Swing with FlatLaf (modern look and feel)
- JFreeChart (cycle time chart)
- H2 database (audit store)
- SLF4J / Logback (logging)
- JUnit 5 (tests)

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

- OEE metrics (Availability, Performance, Quality, and overall OEE) per tool.
- Anomaly detection on cycle times to flag tools drifting before failure.
- Load and analyze a real equipment log file.
- Reliability: auto-reconnect and safer message handling.
- Configurable ports, tool names, and timings via a config file.

## Scope and honesty

FabSentry is a simulator built to show understanding of SECS/GEM, Java Swing, and clean design. It is not production software and does not connect to real equipment. The protocol is simplified. Real fab host software adds full binary SECS-II encoding, security, high-availability, and integration with MES and databases.
