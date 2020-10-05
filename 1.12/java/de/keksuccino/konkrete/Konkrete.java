package de.keksuccino.konkrete;

import java.io.File;

import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.KeyboardHandler;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.sound.SoundHandler;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod(modid = "konkrete", acceptedMinecraftVersions="[1.12,1.12.2]")
public class Konkrete {
	
	public static final String VERSION = "1.0.3";

	public Konkrete() {
		if (FMLClientHandler.instance().getSide() == Side.CLIENT) {
			
			PopupHandler.init();

			KeyboardHandler.init();

			SoundHandler.init();

			System.out.println("[KONKRETE] Successfully initialized!");
			
		} else {
			System.out.println("## WARNING ## 'Konkrete' is a client mod and has no effect when loaded on a server!");
		}
	}
	
	@EventHandler
	private void onClientSetup(FMLPostInitializationEvent e) {
		if (FMLClientHandler.instance().getSide() == Side.CLIENT) {
			
			MouseInput.init();

			initLocals();

			System.out.println("[KONKRETE] Ready to use!");

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

	public static void addPostLoadingEvent(String modid, Runnable event) {
		PostLoadingHandler.addEvent(modid, event);
	}
	
}
