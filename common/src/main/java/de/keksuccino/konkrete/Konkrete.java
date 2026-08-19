
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
import de.keksuccino.konkrete.util.WebUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

/** Shared Konkrete bootstrap and stable mod metadata. */
public class Konkrete {

	private static final Logger LOGGER = LogManager.getLogger();

	/** Stable namespace used by Konkrete resources and loader metadata. */
	public static final String MOD_ID = "konkrete";
	/** Public Konkrete library version reported at runtime. */
	public static final String VERSION = "1.11.1";
	/** Name of the active loader platform. */
	public static final String MOD_LOADER = Services.PLATFORM.getPlatformName();

	/**
	 * Legacy client-side OptiFine presence flag populated during {@link #init()}.
	 *
	 * @deprecated Consumers should perform capability checks instead of depending on OptiFine presence.
	 */
	@Deprecated
    public static boolean isOptifineLoaded = false;

	/** Initializes shared web state, detects optional client software, and logs the active environment. */
	public static void init() {

		WebUtils.init();

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

	/** Runs client completion hooks when applicable, then drains registered post-init tasks. */
	public static void onGameInitCompleted() {

		if (Services.PLATFORM.isOnClient()) KonkreteClient.onGameInitCompleted();
		PostClientInitTaskExecutor.executeAll();

	}

	/**
	 * Registers a client-only task for execution after game initialization; server calls are ignored by the executor.
	 *
	 * @deprecated Register through the current lifecycle API instead.
	 */
    @Deprecated
	public static void addPostClientInitTask(@NotNull String modId, @NotNull Runnable task) {
		PostClientInitTaskExecutor.addTask(modId, task);
	}

}
