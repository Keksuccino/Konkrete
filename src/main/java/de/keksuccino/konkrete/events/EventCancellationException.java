//Copyright (c) 2020 Keksuccino
package de.keksuccino.konkrete.events;

public class EventCancellationException extends Exception {

	private static final long serialVersionUID = 1L;
	
	public EventCancellationException(String msg) {
		super(msg);
	}

}
