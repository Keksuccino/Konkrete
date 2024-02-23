package de.keksuccino.konkrete;

import de.keksuccino.konkrete.command.ClientExecutor;
import de.keksuccino.konkrete.gui.content.handling.AdvancedWidgetsHandler;
import de.keksuccino.konkrete.platform.Services;
import net.minecraft.resources.ResourceLocation;
import java.io.File;
import de.keksuccino.konkrete.gui.content.AdvancedButtonHandler;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.KeyboardHandler;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.sound.SoundHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class Konkrete {

	private static final Logger LOGGER = LogManager.getLogger();

	public static final String VERSION = "1.9.0";
	public static final String MOD_LOADER = Services.PLATFORM.getPlatformName();

	@Deprecated
    public static boolean isOptifineLoaded = false;

	public static void init() {

		if (Services.PLATFORM.isOnClient()) {
			LOGGER.info("[KONKRETE] Loading v" + VERSION + " in client-side mode on " + MOD_LOADER.toUpperCase() + "!");
		} else {
			LOGGER.info("[KONKRETE] Loading v" + VERSION + " in server-side mode on " + MOD_LOADER.toUpperCase() + "!");
		}
    	
    	if (Services.PLATFORM.isOnClient()) {

			ClientExecutor.init();

			PopupHandler.init();

			AdvancedWidgetsHandler.init();

			KeyboardHandler.init();

			MouseInput.init();
			
			AdvancedButtonHandler.init();

			try {
				Class.forName("optifine.Installer");
				isOptifineLoaded = true;
			}
			catch (ClassNotFoundException ignore) {}
		
		}

		//Nothing server-side needs to get initialized here
		LOGGER.info("[KONKRETE] Server-side modules initialized and ready to use!");
    	
    }

	//TODO call this in all mod loaders
	public static void onGameInitCompleted() {

		SoundHandler.init();

		SoundHandler.updateVolume();

		initLocals();

		LOGGER.info("[KONKRETE] Client-side modules initialized and ready to use!");

		PostClientInitTaskExecutor.executeAll();

	}

	@SuppressWarnings("all")
    private static void initLocals() {
		File f = new File("config/konkrete/locals");
		if (!f.exists()) {
			f.mkdirs();
		}
		Locals.copyLocalsFileToDir(new ResourceLocation("konkrete", "locals/en_us.local"), "en_us", f.getPath());
		Locals.copyLocalsFileToDir(new ResourceLocation("konkrete", "locals/de_de.local"), "de_de", f.getPath());
		Locals.copyLocalsFileToDir(new ResourceLocation("konkrete", "locals/pl_pl.local"), "pl_pl", f.getPath());
		Locals.copyLocalsFileToDir(new ResourceLocation("konkrete", "locals/pt_br.local"), "pt_br", f.getPath());
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