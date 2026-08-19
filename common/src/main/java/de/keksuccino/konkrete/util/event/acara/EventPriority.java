//Acara - Simple Java Event System

//Copyright (c) 2023 Keksuccino.
//Acara is licensed under MIT.

package de.keksuccino.konkrete.util.event.acara;

/**
 * These priority presets can be used to easily prioritize events, but there's no need to use exactly these values.<br>
 * Events are simply sorted from the lowest priority value to the highest priority value.
 **/
public class EventPriority {

    /** Three steps below normal priority. */
    public static final int VERY_LOW = -3;
    /** Two steps below normal priority. */
    public static final int LOWER = -2;
    /** One step below normal priority. */
    public static final int LOW = -1;
    /** Default listener priority. */
    public static final int NORMAL = 0;
    /** One step above normal priority. */
    public static final int HIGH = 1;
    /** Two steps above normal priority. */
    public static final int HIGHER = 2;
    /** Three steps above normal priority. */
    public static final int VERY_HIGH = 3;

}
