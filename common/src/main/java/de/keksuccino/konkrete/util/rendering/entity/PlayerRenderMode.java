package de.keksuccino.konkrete.util.rendering.entity;

/** Defines how an optional entity renderer presents a player model. */
public enum PlayerRenderMode {

    /** Renders the complete player normally. */
    NORMAL,

    /** Applies normal invisibility rules. */
    INVISIBLE,

    /** Renders the player as a spectator. */
    SPECTATOR,

    /** Renders the translucent ghost form. */
    GHOST

}
