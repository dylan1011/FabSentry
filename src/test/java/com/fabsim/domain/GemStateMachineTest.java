package com.fabsim.domain;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GemStateMachineTest {

    @Test
    void startsIdle() {
        assertEquals(ToolState.IDLE, new GemStateMachine().current());
    }

    @Test
    void legalTransitionSucceedsAndChangesState() {
        GemStateMachine gem = new GemStateMachine();
        assertTrue(gem.transition(ToolState.SETUP, "test"));
        assertEquals(ToolState.SETUP, gem.current());
    }

    @Test
    void illegalTransitionFailsAndKeepsState() {
        GemStateMachine gem = new GemStateMachine();
        assertFalse(gem.transition(ToolState.PROCESSING, "illegal"));
        assertEquals(ToolState.IDLE, gem.current());
    }

    @Test
    void listenerReceivesTransitionAndReject() {
        GemStateMachine gem = new GemStateMachine();
        List<String> events = new ArrayList<>();
        gem.addListener(new GemStateMachine.Listener() {
            @Override
            public void onTransition(ToolState from, ToolState to, String reason) {
                events.add(from + ">" + to);
            }
            @Override
            public void onRejected(ToolState current, ToolState attempted) {
                events.add("REJECT:" + current + ">" + attempted);
            }
        });
        gem.transition(ToolState.SETUP, "ok");
        gem.transition(ToolState.PROCESSING, "ok");
        gem.transition(ToolState.IDLE, "illegal");
        assertEquals("IDLE>SETUP", events.get(0));
        assertEquals("SETUP>PROCESSING", events.get(1));
        assertEquals("REJECT:PROCESSING>IDLE", events.get(2));
    }

    @Test
    void fullCyclePathIsLegal() {
        GemStateMachine gem = new GemStateMachine();
        assertTrue(gem.transition(ToolState.SETUP, ""));
        assertTrue(gem.transition(ToolState.PROCESSING, ""));
        assertTrue(gem.transition(ToolState.COMPLETE, ""));
        assertTrue(gem.transition(ToolState.IDLE, ""));
    }
}
