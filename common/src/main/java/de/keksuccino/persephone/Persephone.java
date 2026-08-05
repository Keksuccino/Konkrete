
/*

PERSEPHONE SHIPS AND USES THE FOLLOWING LIBRARIES:

Open Imaging Copyright © 2014 Dhyan Blum.
Open Imaging is licensed under Apache-2.0.

JsonPath Copyright © 2017 Jayway.
JsonPath is licensed under Apache-2.0.

Json-smart Copyright © netplex.
Json-smart is licensed under Apache-2.0.

Exp4j Copyright © Frank Asseg.
Exp4j is licensed under Apache-2.0. https://github.com/fasseg/exp4j

 */

package de.keksuccino.persephone;

import de.keksuccino.persephone.platform.Services;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Persephone {

	private static final Logger LOGGER = LogManager.getLogger();

	public static final String MOD_ID = "persephone";
	public static final String VERSION = "2.0.0";
	public static final String MOD_LOADER = Services.PLATFORM.getPlatformName();

	public static void init() {

		if (Services.PLATFORM.isOnClient()) {
			LOGGER.info("[PERSEPHONE] Loading v" + VERSION + " in client-side mode on " + MOD_LOADER.toUpperCase() + "..");
		} else {
			LOGGER.info("[PERSEPHONE] Loading v" + VERSION + " in server-side mode on " + MOD_LOADER.toUpperCase() + "..");
		}
    	
    }

	public static void onGameInitCompleted() {

	}

}
