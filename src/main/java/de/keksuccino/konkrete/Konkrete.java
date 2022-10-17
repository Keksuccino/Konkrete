package de.keksuccino.konkrete;

import de.keksuccino.konkrete.command.ClientExecutor;
import de.keksuccino.konkrete.gui.content.handling.AdvancedWidgetsHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;
import java.io.File;

import de.keksuccino.konkrete.events.EventHandler;
import de.keksuccino.konkrete.events.EventPriority;
import de.keksuccino.konkrete.events.SubscribeEvent;
import de.keksuccino.konkrete.events.client.GameInitializationCompletedEvent;
import de.keksuccino.konkrete.gui.content.AdvancedButtonHandler;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.KeyboardHandler;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.rendering.CurrentScreenHandler;
import de.keksuccino.konkrete.sound.SoundHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Konkrete implements ModInitializer {

	public static final String VERSION = "1.5.3";

    private static final EventHandler HANDLER = new EventHandler();

	public static Logger LOGGER = LogManager.getLogger();

    public static boolean isOptifineLoaded = false;
    
    @Override
    public void onInitialize() {
    	
    	if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {

			ClientExecutor.init();

			PopupHandler.init();

			AdvancedWidgetsHandler.init();

			KeyboardHandler.init();

			MouseInput.init();

			CurrentScreenHandler.init();
			
			AdvancedButtonHandler.init();

			HANDLER.registerEventsFrom(this);

			try {
				Class.forName("optifine.Installer");
				isOptifineLoaded = true;
				LOGGER.info("[KONKRETE] Optifine detected! ###############################");
			}
			catch (ClassNotFoundException e) {}
		
		}

		LOGGER.info("[KONKRETE] Successfully initialized!");
		LOGGER.info("[KONKRETE] Server-side libs ready to use!");
    	
    }
    
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onGameInitCompleted(GameInitializationCompletedEvent e) {

		SoundHandler.init();

		SoundHandler.updateVolume();

    	initLocals();
		
		LOGGER.info("[KONKRETE] Client-side libs ready to use!");
		
		PostLoadingHandler.runPostLoadingEvents();
    	
    }
    
    private static void initLocals() {
		File f = new File("config/konkrete/locals");
		if (!f.exists()) {
			f.mkdirs();
		}
		
		Locals.copyLocalsFileToDir(new ResourceLocation("keksuccino", "konkretelocals/en_us.local"), "en_us", f.getPath());
		Locals.copyLocalsFileToDir(new ResourceLocation("keksuccino", "konkretelocals/de_de.local"), "de_de", f.getPath());
		Locals.copyLocalsFileToDir(new ResourceLocation("keksuccino", "konkretelocals/pl_pl.local"), "pl_pl", f.getPath());
		Locals.copyLocalsFileToDir(new ResourceLocation("keksuccino", "konkretelocals/pt_br.local"), "pt_br", f.getPath());
		
		Locals.getLocalsFromDir(f.getPath());
	}

	/**
	 * ONLY WORKS CLIENT-SIDE! DOES NOTHING ON A SERVER!
	 */
    public static void addPostLoadingEvent(String modid, Runnable event) {
		PostLoadingHandler.addEvent(modid, event);
	}

    /**
     * Returns the {@link EventHandler} for the current game instance.<br>
     * Used to register events.
     */
    public static EventHandler getEventHandler() {
    	return HANDLER;
    }

}