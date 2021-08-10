//Copyright (c) 2020 Keksuccino
package de.keksuccino.konkrete.events;

/**
 * The base class for all events.
 */
public abstract class EventBase {
	
	private boolean canceled = false;

	public abstract boolean isCancelable();

	public void setCanceled(boolean b) {
		try {
			if (!this.isCancelable()) {
				System.out.println("################# ERROR [KONKRETE] #################");
				System.out.println("Tried to cancel non-cancelable event: " + this.getClass().getName());
				System.out.println("####################################################");
				throw new EventCancellationException("Event not cancelable: " + this.getClass().getName());
			} else {
				this.canceled = b;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public boolean isCanceled() {
		return this.canceled;
	}

}
