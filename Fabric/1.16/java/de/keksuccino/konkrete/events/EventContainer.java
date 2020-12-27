//Copyright (c) 2020 Keksuccino
package de.keksuccino.konkrete.events;

import java.lang.reflect.Method;

public class EventContainer {
	
	public final Method event;
	public final Object instance;
	public final String identifier;
	public final EventPriority priority;

	public EventContainer(String identifier, Method event, Object instance, EventPriority priority) {
		this.event = event;
		this.instance = instance;
		this.identifier = identifier;
		this.priority = priority;
	}
}
