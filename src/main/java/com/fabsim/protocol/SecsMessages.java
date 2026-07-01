package com.fabsim.protocol;

public final class SecsMessages {

    private SecsMessages() {}

    public static SecsMessage s1f1() {
        return new SecsMessage(1, 1, true, "");
    }

    public static SecsMessage s1f2(String model, String softwareRev) {
        return new SecsMessage(1, 2, false, model + "|" + softwareRev);
    }

    public static boolean isAreYouThere(SecsMessage m) {
        return m.stream() == 1 && m.function() == 1;
    }

    public static boolean isOnLineData(SecsMessage m) {
        return m.stream() == 1 && m.function() == 2;
    }

    public static SecsMessage s6f11(String eventName, String detail) {
        return new SecsMessage(6, 11, false, eventName + "|" + detail);
    }

    public static boolean isEventReport(SecsMessage m) {
        return m.stream() == 6 && m.function() == 11;
    }

    public static SecsMessage s2f41(String command) {
        return new SecsMessage(2, 41, true, command);
    }

    public static boolean isRemoteCommand(SecsMessage m) {
        return m.stream() == 2 && m.function() == 41;
    }

    public static SecsMessage s2f42(boolean accepted) {
        return new SecsMessage(2, 42, false, accepted ? "ACK" : "NAK");
    }

    public static boolean isCommandAck(SecsMessage m) {
        return m.stream() == 2 && m.function() == 42;
    }

    public static SecsMessage s5f1(boolean set, String alarmId, String text) {
        return new SecsMessage(5, 1, false, "ALARM|" + (set ? "SET" : "CLEAR") + "|" + alarmId + "|" + text);
    }

    public static boolean isAlarmReport(SecsMessage m) {
        return m.stream() == 5 && m.function() == 1;
    }
}
