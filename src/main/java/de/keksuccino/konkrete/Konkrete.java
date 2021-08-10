package de.keksuccino.konkrete;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;

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

public class Konkrete implements ModInitializer {

	public static final String VERSION = "1.2.3";
    private static final EventHandler HANDLER = new EventHandler();

    public static boolean isOptifineLoaded = false;
    
    @Override
    public void onInitialize() {
    	
    	if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {

			PopupHandler.init();

			KeyboardHandler.init();

			MouseInput.init();

			CurrentScreenHandler.init();
			
			AdvancedButtonHandler.init();

			HANDLER.registerEventsFrom(this);

			try {
				Class.forName("optifine.Installer");
				isOptifineLoaded = true;
				System.out.println("[KONKRETE] Optifine detected! ###############################");
			}
			catch (ClassNotFoundException e) {}

//			handler.registerEventsFrom(new TestEvents());
			
			System.out.println("[KONKRETE] Successfully initialized!");
		
		} else {
			System.out.println("## WARNING ## 'Konkrete' is a client mod and has no effect when loaded on a server!");
		}
    	
    }
    
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onGameInitCompleted(GameInitializationCompletedEvent e) {

    	//TODO Neu in 1.17 (von oben nach unten verrutscht)
		SoundHandler.init();

		//TODO Neu in 1.17 (muss nun einmal manuall geupdatet werden)
		SoundHandler.updateVolume();

    	initLocals();
		
		System.out.println("[KONKRETE] Ready to use!");
		
		PostLoadingHandler.runPostLoadingEvents();
    	
    }
    
    private static void initLocals() {
		File f = new File("config/konkrete/locals");
		if (!f.exists()) {
			f.mkdirs();
		}
		
		Locals.copyLocalsFileToDir(new Identifier("keksuccino", "konkretelocals/en_us.local"), "en_us", f.getPath());
		Locals.copyLocalsFileToDir(new Identifier("keksuccino", "konkretelocals/de_de.local"), "de_de", f.getPath());
		Locals.copyLocalsFileToDir(new Identifier("keksuccino", "konkretelocals/pl_pl.local"), "pl_pl", f.getPath());
		Locals.copyLocalsFileToDir(new Identifier("keksuccino", "konkretelocals/pt_br.local"), "pt_br", f.getPath());
		
		Locals.getLocalsFromDir(f.getPath());
	}
    
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