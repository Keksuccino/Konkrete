//Copyright (c) 2020 Keksuccino
package de.keksuccino.konkrete.events;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface SubscribeEvent {
	
	EventPriority priority() default EventPriority.NORMAL;
	
}
