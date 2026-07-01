package com.fabsim.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ToolStateTest {

    @Test
    void idleCanGoToSetupAndDown() {
        assertTrue(ToolState.IDLE.canTransitionTo(ToolState.SETUP));
        assertTrue(ToolState.IDLE.canTransitionTo(ToolState.DOWN));
    }

    @Test
    void idleCannotJumpToProcessing() {
        assertFalse(ToolState.IDLE.canTransitionTo(ToolState.PROCESSING));
        assertFalse(ToolState.IDLE.canTransitionTo(ToolState.COMPLETE));
    }

    @Test
    void processingCanPauseCompleteOrGoDown() {
        assertTrue(ToolState.PROCESSING.canTransitionTo(ToolState.PAUSED));
        assertTrue(ToolState.PROCESSING.canTransitionTo(ToolState.COMPLETE));
        assertTrue(ToolState.PROCESSING.canTransitionTo(ToolState.DOWN));
    }

    @Test
    void downCanOnlyRecoverToIdle() {
        assertTrue(ToolState.DOWN.canTransitionTo(ToolState.IDLE));
        assertFalse(ToolState.DOWN.canTransitionTo(ToolState.PROCESSING));
        assertFalse(ToolState.DOWN.canTransitionTo(ToolState.SETUP));
    }

    @Test
    void noStateCanTransitionToItself() {
        for (ToolState s : ToolState.values()) {
            assertFalse(s.canTransitionTo(s), s + " should not transition to itself");
        }
    }
}
