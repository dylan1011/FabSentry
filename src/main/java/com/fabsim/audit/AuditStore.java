package com.fabsim.audit;

import java.util.List;

public interface AuditStore {

    void recordStateChange(String toolName, String fromState, String toState, String reason);

    void recordAlarm(String toolName, boolean set, String alarmId, String text);

    List<String> recentEvents(int limit);

    void close();
}
