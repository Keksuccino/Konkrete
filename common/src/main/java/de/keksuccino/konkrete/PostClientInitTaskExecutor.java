package de.keksuccino.konkrete;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.util.*;

public class PostClientInitTaskExecutor {

	private static final Logger LOGGER = LogManager.getLogger();
	private static final Map<String, List<Runnable>> EVENTS = new LinkedHashMap<>();
	
	protected static void executeAll() {
		EVENTS.forEach((modId, tasks) -> {
			try {
				LOGGER.info("[KONKRETE] Executing post-client-init tasks of '" + modId + "'...");
				tasks.forEach(Runnable::run);
			} catch (Exception ex) {
				LOGGER.error("[KONKRETE] Error while executing post-client-init tasks of '" + modId + "'!", ex);
			}
		});
	}
	
	protected static void addTask(@NotNull String modId, @NotNull Runnable task) {
		if (!EVENTS.containsKey(modId)) {
			EVENTS.put(modId, new ArrayList<>());
		}
		EVENTS.get(modId).add(task);
	}

}
