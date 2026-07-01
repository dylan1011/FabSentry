package com.fabsim.domain;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class GemStateMachine {

    public interface Listener {
        void onTransition(ToolState from, ToolState to, String reason);
        void onRejected(ToolState current, ToolState attempted);
    }

    private final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private volatile ToolState state = ToolState.IDLE;

    public synchronized ToolState current() {
        return state;
    }

    public synchronized boolean transition(ToolState target, String reason) {
        if (!state.canTransitionTo(target)) {
            for (Listener l : listeners) {
                l.onRejected(state, target);
            }
            return false;
        }
        ToolState from = state;
        state = target;
        for (Listener l : listeners) {
            l.onTransition(from, target, reason);
        }
        return true;
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }
}
