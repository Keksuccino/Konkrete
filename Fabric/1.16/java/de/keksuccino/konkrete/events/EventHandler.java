//Copyright (c) 2020 Keksuccino
package de.keksuccino.konkrete.events;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public class EventHandler {
	
	private static final boolean DEBUG = false;
	private Map<String, List<EventContainer>> events = new HashMap<String, List<EventContainer>>();
	
	/**
	 * This will register all public (static and non-static) methods of the given object instance annotated with {@link SubscribeEvent}.
	 */
	public void registerEventsFrom(Object instance) {
		for (Method m : instance.getClass().getDeclaredMethods()) {
			if (Modifier.isPublic(m.getModifiers()) && m.isAnnotationPresent(SubscribeEvent.class)) {
				EventPriority p = m.getAnnotation(SubscribeEvent.class).priority();
				for (Class<?> cc : m.getParameterTypes()) {
					if (EventBase.class.isAssignableFrom(cc)) {
						if (!eventsExistForType((Class<? extends EventBase>) cc)) {
							events.put(cc.getName(), new ArrayList<EventContainer>());
						}
						events.get(cc.getName()).add(new EventContainer(cc.getName(), m, instance, p));
						if (DEBUG) {
							System.out.println("[KONKRETE] Registered event '" + m.getName() + "' (" + cc.getTypeName() + ") from '" + instance.getClass().getName() + "'!");
						}
						break;
					}
				}
			}
		}
	}
	
	/**
	 * This will register all public static methods of the given class annotated with {@link SubscribeEvent}.
	 */
	public void registerEventsFrom(Class<?> c) {
		for (Method m : c.getDeclaredMethods()) {
			if (Modifier.isPublic(m.getModifiers()) && Modifier.isStatic(m.getModifiers()) && m.isAnnotationPresent(SubscribeEvent.class)) {
				EventPriority p = m.getAnnotation(SubscribeEvent.class).priority();
				for (Class<?> cc : m.getParameterTypes()) {
					if (EventBase.class.isAssignableFrom(cc)) {
						if (!eventsExistForType((Class<? extends EventBase>) cc)) {
							events.put(cc.getName(), new ArrayList<EventContainer>());
						}
						events.get(cc.getName()).add(new EventContainer(cc.getName(), m, c, p));
						if (DEBUG) {
							System.out.println("[KONKRETE] Registered static event '" + m.getName() + "' (" + cc.getTypeName() + ") from '" + c.getName() + "'!");
						}
						break;
					}
				}
			}
		}
	}
	
	/**
	 * Needs to be called when all events for a specific event type needs to be executed.<br><br>
	 * 
	 * <b>Usage:</b><br><br>
	 * 
	 * {@code ExampleEvent e = new ExampleEvent();}<br>
	 * {@code EventHandler.callEventsFor(e);}
	 */
	public void callEventsFor(EventBase listener) {
		if (eventsExistForType(listener.getClass())) {
			List<EventContainer> highest = new ArrayList<EventContainer>();
			List<EventContainer> high = new ArrayList<EventContainer>();
			List<EventContainer> normal = new ArrayList<EventContainer>();
			List<EventContainer> low = new ArrayList<EventContainer>();
			List<EventContainer> lowest = new ArrayList<EventContainer>();
			for (EventContainer c : events.get(listener.getClass().getName())) {
				if (c.priority == EventPriority.HIGHEST) {
					highest.add(c);
				}
				if (c.priority == EventPriority.HIGH) {
					high.add(c);
				}
				if (c.priority == EventPriority.NORMAL) {
					normal.add(c);
				}
				if (c.priority == EventPriority.LOW) {
					low.add(c);
				}
				if (c.priority == EventPriority.LOWEST) {
					lowest.add(c);
				}
			}
			for (EventContainer c : highest) {
				this.invokeEvent(c, listener);
			}
			for (EventContainer c : high) {
				this.invokeEvent(c, listener);
			}
			for (EventContainer c : normal) {
				this.invokeEvent(c, listener);
			}
			for (EventContainer c : low) {
				this.invokeEvent(c, listener);
			}
			for (EventContainer c : lowest) {
				this.invokeEvent(c, listener);
			}
		}
	}
	
	private void invokeEvent(EventContainer c, EventBase listener) {
		try {
			c.event.invoke(c.instance, listener);
		} catch (Exception e) {
			System.out.println("################# ERROR [KONKRETE] #################");
			System.out.println("Failed to invoke event!");
			System.out.println("Method Name: " + c.event.getName());
			System.out.println("Event Name: " + listener.getClass().getName());
			System.out.println("####################################################");
			System.out.println("");
			e.printStackTrace();
		}
	}
	
	public boolean eventsExistForType(Class<? extends EventBase> listenerType) {
		return (events.get(listenerType.getName()) != null);
	}

}
