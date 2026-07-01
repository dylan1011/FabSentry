package com.fabsim.domain;

import java.util.EnumSet;
import java.util.Set;

public enum ToolState {

    IDLE,
    SETUP,
    PROCESSING,
    PAUSED,
    COMPLETE,
    DOWN;

    private Set<ToolState> allowed;

    static {
        IDLE.allowed = EnumSet.of(SETUP, DOWN);
        SETUP.allowed = EnumSet.of(PROCESSING, IDLE, DOWN);
        PROCESSING.allowed = EnumSet.of(PAUSED, COMPLETE, DOWN);
        PAUSED.allowed = EnumSet.of(PROCESSING, DOWN);
        COMPLETE.allowed = EnumSet.of(IDLE, DOWN);
        DOWN.allowed = EnumSet.of(IDLE);
    }

    public boolean canTransitionTo(ToolState target) {
        return allowed.contains(target);
    }

    public Set<ToolState> allowedTargets() {
        return EnumSet.copyOf(allowed);
    }
}
