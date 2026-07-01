package com.fabsim.audit;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class H2AuditStore implements AuditStore {

    private final Connection conn;

    public H2AuditStore() {
        try {
            conn = DriverManager.getConnection("jdbc:h2:./fabsentry-audit;AUTO_SERVER=TRUE", "sa", "");
            createTables();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to open H2 audit store: " + e.getMessage(), e);
        }
    }

    private void createTables() throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS state_changes (" +
                    "id IDENTITY PRIMARY KEY, " +
                    "ts TIMESTAMP, " +
                    "tool VARCHAR(100), " +
                    "from_state VARCHAR(50), " +
                    "to_state VARCHAR(50), " +
                    "reason VARCHAR(255))");
            st.execute("CREATE TABLE IF NOT EXISTS alarms (" +
                    "id IDENTITY PRIMARY KEY, " +
                    "ts TIMESTAMP, " +
                    "tool VARCHAR(100), " +
                    "action VARCHAR(10), " +
                    "alarm_id VARCHAR(50), " +
                    "text VARCHAR(255))");
        }
    }

    @Override
    public synchronized void recordStateChange(String toolName, String fromState, String toState, String reason) {
        String sql = "INSERT INTO state_changes (ts, tool, from_state, to_state, reason) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(2, toolName);
            ps.setString(3, fromState);
            ps.setString(4, toState);
            ps.setString(5, reason);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("audit state change failed: " + e.getMessage());
        }
    }

    @Override
    public synchronized void recordAlarm(String toolName, boolean set, String alarmId, String text) {
        String sql = "INSERT INTO alarms (ts, tool, action, alarm_id, text) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(2, toolName);
            ps.setString(3, set ? "SET" : "CLEAR");
            ps.setString(4, alarmId);
            ps.setString(5, text);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("audit alarm failed: " + e.getMessage());
        }
    }

    @Override
    public synchronized List<String> recentEvents(int limit) {
        List<String> out = new ArrayList<>();
        String sql = "SELECT ts, tool, from_state, to_state, reason FROM state_changes ORDER BY id DESC LIMIT ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(rs.getTimestamp(1) + " " + rs.getString(2) + " "
                            + rs.getString(3) + ">" + rs.getString(4) + " (" + rs.getString(5) + ")");
                }
            }
        } catch (SQLException e) {
            System.err.println("audit read failed: " + e.getMessage());
        }
        return out;
    }

    @Override
    public void close() {
        try {
            conn.close();
        } catch (SQLException e) {
            System.err.println("audit close failed: " + e.getMessage());
        }
    }
}
