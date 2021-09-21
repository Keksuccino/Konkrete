package de.keksuccino.konkrete;

import java.io.File;

import net.minecraft.resources.ResourceLocation;

import de.keksuccino.konkrete.gui.content.AdvancedButtonHandler;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.KeyboardHandler;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.rendering.CurrentScreenHandler;
import de.keksuccino.konkrete.sound.SoundHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod("konkrete")
public class Konkrete {

	//TODO übernehmen
	public static final String VERSION = "1.3.0";

	public Konkrete() {

//		ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST, () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (a, b) -> true));
		
		if (FMLEnvironment.dist == Dist.CLIENT) {

			PopupHandler.init();

			KeyboardHandler.init();

			MouseInput.init();

			CurrentScreenHandler.init();

			AdvancedButtonHandler.init();

			FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onClientSetup);

			System.out.println("[KONKRETE] Successfully initialized!");
		
		} else {
			System.out.println("## WARNING ## 'Konkrete' is a client mod and has no effect when loaded on a server!");
		}
	}
	
	private void onClientSetup(FMLClientSetupEvent e) {

		SoundHandler.init();

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
