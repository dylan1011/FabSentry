# SECS/GEM Equipment Simulator and Monitoring Console

A Java Swing desktop application that simulates semiconductor fab equipment using SECS/GEM concepts, and a monitoring console (host) that connects to many tools at once, tracks their state, and shows live timing analysis.

This is a simulator and monitor, not production fab software. It does not talk to real hardware. It is built to demonstrate SECS/GEM domain knowledge, Java Swing UI work, and clean software design.

## What it does

- Simulates fab equipment (Etcher, CVD, CMP, Litho), each as its own process on its own port.
- Each tool runs a GEM-style state machine: IDLE, SETUP, PROCESSING, PAUSED, COMPLETE, DOWN.
- A monitoring console (host) connects to running tools using an HSMS-style TCP link.
- Tools report state changes as SECS-II style event messages (S6F11). The host sends remote commands (S2F41).
- Live per-tool dashboard: state banner, process flow bar, state model diagram, wafer view, timing cards, and a cycle time trend chart.
- Multi-tool support: add tools from a dropdown, see them all in a list, click to switch.
- Dark and light themes.

## SECS/GEM concepts shown

- SECS-II style messages (stream/function, W-bit, message body).
- HSMS-style framing over TCP sockets.
- GEM equipment states and legal state transitions.
- Event reporting (S6F11) and remote commands (S2F41 / S2F42).
- Host and equipment roles (console = host, simulator = equipment).

Note: message bodies are simplified (pipe-delimited text), not full binary SECS-II encoding. This is a learning simulator, so the protocol is GEM-flavored but simplified.

## Tech stack

- Java 17, Maven
- Swing with FlatLaf (modern look and feel)
- JFreeChart (cycle time chart)
- SLF4J / Logback (logging)
- JUnit 5 (tests)

## Project structure

- protocol/ - SECS-II style message model and helpers
- transport/ - HSMS-style connection framing over TCP
- domain/ - tool state enum and GEM state machine
- tool/ - the equipment simulator (TCP server)
- console/ - the monitoring console (Swing UI)

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

## Tests

    mvn -o test

## Ports

- Etcher: 5555
- CVD: 5556
- CMP: 5557
- Litho: 5558

## Scope and honesty

This project is a simulator built to show understanding of SECS/GEM, Java Swing, and clean design. It is not production software and does not connect to real equipment. The protocol is simplified. Real fab host software adds full binary SECS-II encoding, security, high-availability, and integration with MES and databases.
