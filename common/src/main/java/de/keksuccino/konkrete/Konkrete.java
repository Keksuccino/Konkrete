
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
import de.keksuccino.konkrete.threading.ClientThreadTaskExecutor;
import de.keksuccino.konkrete.threading.ManagedExecutors;
import de.keksuccino.konkrete.web.WebUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Konkrete {

	private static final Logger LOGGER = LogManager.getLogger();

	public static final String MOD_ID = "konkrete";
	public static final String VERSION = "1.11.1";
	public static final String MOD_LOADER = Services.PLATFORM.getPlatformName();

	public static void init() {

		if (Services.PLATFORM.isOnClient()) {

			LOGGER.info("[KONKRETE] Loading v" + VERSION + " in client-side mode on " + MOD_LOADER.toUpperCase() + "!");

			ClientThreadTaskExecutor.init();

		} else {
			LOGGER.info("[KONKRETE] Loading v" + VERSION + " in server-side mode on " + MOD_LOADER.toUpperCase() + "!");
		}

		ManagedExecutors.init();

		WebUtils.init();
    	
    }

}
