package com.fabsim.protocol;

import java.nio.charset.StandardCharsets;

public final class SecsMessage {

    private final int stream;
    private final int function;
    private final boolean replyExpected;
    private final String body;

    public SecsMessage(int stream, int function, boolean replyExpected, String body) {
        this.stream = stream;
        this.function = function;
        this.replyExpected = replyExpected;
        this.body = body == null ? "" : body;
    }

    public int stream() { return stream; }
    public int function() { return function; }
    public boolean replyExpected() { return replyExpected; }
    public String body() { return body; }

    public String sxfy() {
        return "S" + stream + "F" + function + (replyExpected ? " W" : "");
    }

    public byte[] encode() {
        byte[] payload = body.getBytes(StandardCharsets.UTF_8);
        byte[] frame = new byte[4 + payload.length];
        frame[0] = (byte) stream;
        frame[1] = (byte) function;
        frame[2] = (byte) (replyExpected ? 1 : 0);
        frame[3] = (byte) 0;
        System.arraycopy(payload, 0, frame, 4, payload.length);
        return frame;
    }

    public static SecsMessage decode(byte[] frame) {
        int s = frame[0] & 0xFF;
        int f = frame[1] & 0xFF;
        boolean w = (frame[2] & 0xFF) == 1;
        String body = new String(frame, 4, frame.length - 4, StandardCharsets.UTF_8);
        return new SecsMessage(s, f, w, body);
    }

    @Override
    public String toString() {
        return sxfy() + (body.isEmpty() ? "" : " <" + body + ">");
    }
}
