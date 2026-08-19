//Acara - Simple Java Event System

//Copyright (c) 2023 Keksuccino.
//Acara is licensed under MIT.

package de.keksuccino.konkrete.util.event.acara;

/** The base class for events. **/
public abstract class EventBase {

    private boolean canceled = false;

    /** Declares whether this event accepts cancellation state changes. */
    public abstract boolean isCancelable();

    /** Sets cancellation when supported; unsupported attempts are reported without changing state. */
    public void setCanceled(boolean b) {
        try {
            if (!this.isCancelable()) {
                throw new EventCancellationException("[ACARA] Tried to cancel non-cancelable event: " + this.getClass().getName());
            } else {
                this.canceled = b;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Returns the current cancellation flag. */
    public boolean isCanceled() {
        return this.canceled;
    }

}
