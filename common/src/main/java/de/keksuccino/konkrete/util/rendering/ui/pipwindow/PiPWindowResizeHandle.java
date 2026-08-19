package de.keksuccino.konkrete.util.rendering.ui.pipwindow;

/** Identifies a resize edge or corner for a PiP window. */
public enum PiPWindowResizeHandle {
    /** Resizes the PiP window from its none. */
    NONE,
    /** Resizes the PiP window from its left. */
    LEFT,
    /** Resizes the PiP window from its right. */
    RIGHT,
    /** Resizes the PiP window from its top. */
    TOP,
    /** Resizes the PiP window from its bottom. */
    BOTTOM,
    /** Resizes the PiP window from its top left. */
    TOP_LEFT,
    /** Resizes the PiP window from its top right. */
    TOP_RIGHT,
    /** Resizes the PiP window from its bottom left. */
    BOTTOM_LEFT,
    /** Resizes the PiP window from its bottom right. */
    BOTTOM_RIGHT;

    /** Returns whether left edge. */
    public boolean hasLeftEdge() {
        return this == LEFT || this == TOP_LEFT || this == BOTTOM_LEFT;
    }

    /** Returns whether right edge. */
    public boolean hasRightEdge() {
        return this == RIGHT || this == TOP_RIGHT || this == BOTTOM_RIGHT;
    }

    /** Returns whether top edge. */
    public boolean hasTopEdge() {
        return this == TOP || this == TOP_LEFT || this == TOP_RIGHT;
    }

    /** Returns whether bottom edge. */
    public boolean hasBottomEdge() {
        return this == BOTTOM || this == BOTTOM_LEFT || this == BOTTOM_RIGHT;
    }
}
