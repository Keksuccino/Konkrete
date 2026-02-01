
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
	public static final String VERSION = "1.9.15";
	public static final String MOD_LOADER = Services.PLATFORM.getPlatformName();

    public static final boolean JSON_PATH_LIBRARY_LOADED = isJsonPathLibraryLoaded();
	@Deprecated
    public static boolean isOptifineLoaded = false;

	public static void init() {

		if (Services.PLATFORM.isOnClient()) {

			LOGGER.info("[KONKRETE] Loading v" + VERSION + " in client-side mode on " + MOD_LOADER.toUpperCase() + "!");

            if (JSON_PATH_LIBRARY_LOADED) {
                LOGGER.info("[KONKRETE] Jayway JsonPath library found! Seems to be loaded correctly!");
            } else {
                LOGGER.warn("[KONKRETE] Jayway JsonPath library not found! Something went wrong here, but what? o.O");
            }

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
    @Deprecated
	public static void addPostClientInitTask(@NotNull String modId, @NotNull Runnable task) {
		PostClientInitTaskExecutor.addTask(modId, task);
	}

    public static boolean isJsonPathLibraryLoaded() {
        try {
            Class.forName("com.jayway.jsonpath.JsonPath", false, Konkrete.class.getClassLoader());
            return true;
        } catch (Exception ignored) {}
        return false;
    }

}