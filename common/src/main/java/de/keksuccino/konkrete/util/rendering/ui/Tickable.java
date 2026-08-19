package de.keksuccino.konkrete.util.rendering.ui;

/** Receives one update on each client tick. */
public interface Tickable {

    /** Advances this object by one client tick. */
    void tick();

}
