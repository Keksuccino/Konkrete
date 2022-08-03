package de.keksuccino.konkrete;

import java.io.File;

import de.keksuccino.konkrete.gui.content.AdvancedButtonHandler;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.KeyboardHandler;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.sound.SoundHandler;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = "konkrete", acceptedMinecraftVersions="[1.12,1.12.2]")
public class  Konkrete {

	public static final String VERSION = "1.5.0";

	public static Logger LOGGER = LogManager.getLogger();

	public static boolean isOptifineLoaded = false;

	public Konkrete() {

		if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
			
			PopupHandler.init();

			KeyboardHandler.init();

			SoundHandler.init();
			
			AdvancedButtonHandler.init();

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
	
	@EventHandler
	private void onClientSetup(FMLPostInitializationEvent e) {

		if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
			
			MouseInput.init();

			initLocals();

			LOGGER.info("[KONKRETE] Client-side libs ready to use!");

			PostLoadingHandler.runPostLoadingEvents();
			
		}

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
	
}
