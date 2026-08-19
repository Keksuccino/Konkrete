//Acara - Simple Java Event System

//Copyright (c) 2023 Keksuccino.
//Acara is licensed under MIT.

package de.keksuccino.konkrete.util.event.acara;

/** Describes an attempted cancellation of a non-cancelable event. */
public class EventCancellationException extends Exception {

    /** Creates the checked exception with a diagnostic message. */
    public EventCancellationException(String msg) {
        super(msg);
    }

}
