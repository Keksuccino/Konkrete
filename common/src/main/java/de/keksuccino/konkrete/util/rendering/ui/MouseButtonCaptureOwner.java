package de.keksuccino.konkrete.util.rendering.ui;

/**
 * Marks a component whose focused state alone is not enough to determine whether it owns a mouse release.
 * The capture query is intentionally made before dispatching the release because dispatch normally clears ownership.
 */
public interface MouseButtonCaptureOwner {

    /** Returns whether this component currently owns the supplied mouse button. */
    boolean hasMouseButtonCapture(int button);

}
