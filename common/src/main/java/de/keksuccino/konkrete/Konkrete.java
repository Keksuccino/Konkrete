
/*

KONKRETE SHIPS AND USES THE FOLLOWING LIBRARIES:

Open Imaging Copyright © 2014 Dhyan Blum.
Open Imaging is licensed under Apache-2.0.

JsonPath Copyright © 2017 Jayway.
JsonPath is licensed under Apache-2.0.

Json-smart Copyright © netplex.
Json-smart is licensed under Apache-2.0.

Exp4j Copyright © Frank Asseg.
Exp4j is licensed under Apache-2.0. https://github.com/fasseg/exp4j

 */

package de.keksuccino.konkrete;

import de.keksuccino.konkrete.platform.Services;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class Konkrete {

	private static final Logger LOGGER = LogManager.getLogger();

	public static final String MOD_ID = "konkrete";
	public static final String VERSION = "1.9.13";
	public static final String MOD_LOADER = Services.PLATFORM.getPlatformName();

	@Deprecated(forRemoval = true)
    public static boolean isOptifineLoaded = false;

	public static void init() {

		if (Services.PLATFORM.isOnClient()) {
			LOGGER.info("[KONKRETE] Loading v" + VERSION + " in client-side mode on " + MOD_LOADER.toUpperCase() + "!");
		} else {
			LOGGER.info("[KONKRETE] Loading v" + VERSION + " in server-side mode on " + MOD_LOADER.toUpperCase() + "!");
		}

    	if (Services.PLATFORM.isOnClient()) {

			try {
				Class.forName("optifine.Installer");
				isOptifineLoaded = true;
			}
			catch (ClassNotFoundException ignore) {}
		
		}

		//Nothing server-side needs to get initialized here
		LOGGER.info("[KONKRETE] Server-side modules initialized and ready to use!");
    	
    }

	public static void onGameInitCompleted() {

		PostClientInitTaskExecutor.executeAll();

	}

	/**
	 * ONLY WORKS CLIENT-SIDE! DOES NOTHING ON A SERVER!
	 */
	public static void addPostClientInitTask(@NotNull String modId, @NotNull Runnable task) {
		PostClientInitTaskExecutor.addTask(modId, task);
	}

}