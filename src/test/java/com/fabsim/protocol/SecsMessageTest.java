package com.fabsim.protocol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SecsMessageTest {

    @Test
    void encodeThenDecodeRoundTrips() {
        SecsMessage original = new SecsMessage(6, 11, false, "StateChange|IDLE>SETUP|auto");
        SecsMessage decoded = SecsMessage.decode(original.encode());
        assertEquals(original.stream(), decoded.stream());
        assertEquals(original.function(), decoded.function());
        assertEquals(original.replyExpected(), decoded.replyExpected());
        assertEquals(original.body(), decoded.body());
    }

    @Test
    void replyExpectedBitSurvivesRoundTrip() {
        SecsMessage withW = new SecsMessage(1, 1, true, "");
        assertTrue(SecsMessage.decode(withW.encode()).replyExpected());
        SecsMessage withoutW = new SecsMessage(1, 2, false, "x");
        assertFalse(SecsMessage.decode(withoutW.encode()).replyExpected());
    }

    @Test
    void sxfyFormatsCorrectly() {
        assertEquals("S1F1 W", new SecsMessage(1, 1, true, "").sxfy());
        assertEquals("S6F11", new SecsMessage(6, 11, false, "").sxfy());
    }

    @Test
    void nullBodyBecomesEmpty() {
        assertEquals("", new SecsMessage(1, 1, false, null).body());
    }

    @Test
    void emptyBodyRoundTrips() {
        SecsMessage m = new SecsMessage(1, 1, true, "");
        assertEquals("", SecsMessage.decode(m.encode()).body());
    }
}
