//TODO übernehmen
package de.keksuccino.konkrete;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostLoadingHandler {
	
	private static Map<String, List<Runnable>> events = new HashMap<String, List<Runnable>>();
	
	protected static void runPostLoadingEvents() {
		for (Map.Entry<String, List<Runnable>> m : events.entrySet()) {
			System.out.println("[KONKRETE] Running PostLoadingEvents for mod: " + m.getKey());
			
			for (Runnable r : m.getValue()) {
				r.run();
			}
			
			System.out.println("[KONKRETE] PostLoadingEvents completed for mod: " + m.getKey());
		}
	}
	
	protected static void addEvent(String modid, Runnable event) {
		if (!events.containsKey(modid)) {
			events.put(modid, new ArrayList<Runnable>());
		}
		events.get(modid).add(event);
	}

}
