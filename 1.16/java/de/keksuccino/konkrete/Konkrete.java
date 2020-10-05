package de.keksuccino.konkrete;

import java.io.File;

import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.KeyboardHandler;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.rendering.CurrentScreenHandler;
import de.keksuccino.konkrete.sound.SoundHandler;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod("konkrete")
public class Konkrete {
	
	public static final String VERSION = "1.0.3";

	public Konkrete() {
		if (FMLEnvironment.dist == Dist.CLIENT) {

			PopupHandler.init();

			KeyboardHandler.init();

			MouseInput.init();

			SoundHandler.init();

			CurrentScreenHandler.init();

			FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onClientSetup);

			System.out.println("[KONKRETE] Successfully initialized!");
		
		} else {
			System.out.println("## WARNING ## 'Konkrete' is a client mod and has no effect when loaded on a server!");
		}
	}
	
	private void onClientSetup(FMLClientSetupEvent e) {
		
		initLocals();
		
		System.out.println("[KONKRETE] Ready to use!");
		
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

	public static void addPostLoadingEvent(String modid, Runnable event) {
		PostLoadingHandler.addEvent(modid, event);
	}
	
}
