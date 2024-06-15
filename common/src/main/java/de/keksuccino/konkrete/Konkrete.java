package de.keksuccino.konkrete;

import de.keksuccino.konkrete.platform.Services;
import net.minecraft.resources.ResourceLocation;
import java.io.File;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.sound.SoundHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("all")
public class Konkrete {

	private static final Logger LOGGER = LogManager.getLogger();

	public static final String MOD_ID = "konkrete";
	public static final String VERSION = "1.9.9";
	public static final String MOD_LOADER = Services.PLATFORM.getPlatformName();

	@Deprecated(forRemoval = true)
    public static boolean isOptifineLoaded = false;

	public static void init() {

		if (Services.PLATFORM.isOnClient()) {
			LOGGER.info("[KONKRETE] Loading v" + VERSION + " in client-side mode on " + MOD_LOADER.toUpperCase() + "!");
		} else {
			LOGGER.info("[KONKRETE] Loading v" + VERSION + " in server-side mode on " + MOD_LOADER.toUpperCase() + "!");
		}

		LOGGER.info("---------------------------");
		LOGGER.info("KONKRETE SHIPS AND USES THE FOLLOWING LIBRARIES:");
		LOGGER.info(" ");
		LOGGER.info("Open Imaging Copyright © 2014 Dhyan Blum.");
		LOGGER.info("Open Imaging is licensed under Apache-2.0.");
		LOGGER.info(" ");
		LOGGER.info("JsonPath Copyright © 2017 Jayway.");
		LOGGER.info("JsonPath is licensed under Apache-2.0.");
		LOGGER.info(" ");
		LOGGER.info("Json-smart Copyright © netplex.");
		LOGGER.info("Json-smart is licensed under Apache-2.0.");
		LOGGER.info(" ");
		LOGGER.info("Exp4j Copyright © Frank Asseg.");
		LOGGER.info("Exp4j is licensed under Apache-2.0. https://github.com/fasseg/exp4j");
		LOGGER.info("---------------------------");

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

		SoundHandler.init();

		SoundHandler.updateVolume();

		initLocals();

		LOGGER.info("[KONKRETE] Client-side modules initialized and ready to use!");

		PostClientInitTaskExecutor.executeAll();

	}

    private static void initLocals() {
		File f = new File("config/konkrete/locals");
		if (!f.exists()) {
			f.mkdirs();
		}
		Locals.copyLocalsFileToDir(ResourceLocation.parse("konkrete:locals/en_us.local"), "en_us", f.getPath());
		Locals.copyLocalsFileToDir(ResourceLocation.parse("konkrete:locals/de_de.local"), "de_de", f.getPath());
		Locals.copyLocalsFileToDir(ResourceLocation.parse("konkrete:locals/pl_pl.local"), "pl_pl", f.getPath());
		Locals.copyLocalsFileToDir(ResourceLocation.parse("konkrete:locals/pt_br.local"), "pt_br", f.getPath());
		Locals.getLocalsFromDir(f.getPath());
	}

	/**
	 * ONLY WORKS CLIENT-SIDE! DOES NOTHING ON A SERVER!
	 */
	public static void addPostClientInitTask(@NotNull String modId, @NotNull Runnable task) {
		PostClientInitTaskExecutor.addTask(modId, task);
	}

	/**
	 * @deprecated Use {@link Konkrete#addPostClientInitTask(String, Runnable)} instead.
	 */
	@Deprecated
    public static void addPostLoadingEvent(@NotNull String modId, @NotNull Runnable task) {
		addPostClientInitTask(modId, task);
	}

}