#!/bin/bash
mvn -o exec:java -Dexec.mainClass=com.fabsim.tool.ToolSimulator -Dexec.args="Etcher-1 ETCHER 5555" &
mvn -o exec:java -Dexec.mainClass=com.fabsim.tool.ToolSimulator -Dexec.args="CVD-1 CVD 5556" &
mvn -o exec:java -Dexec.mainClass=com.fabsim.tool.ToolSimulator -Dexec.args="CMP-1 CMP 5557" &
mvn -o exec:java -Dexec.mainClass=com.fabsim.tool.ToolSimulator -Dexec.args="Litho-1 LITHO 5558" &
echo "All 4 tools starting in background..."
