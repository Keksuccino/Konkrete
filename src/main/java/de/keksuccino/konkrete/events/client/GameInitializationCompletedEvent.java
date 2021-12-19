package de.keksuccino.konkrete.events.client;

import de.keksuccino.konkrete.events.EventBase;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

/**
 * Is fired after all mandatory instances of the {@link MinecraftClient} were set.<br>
 * Can be used as "ModLoadingCompletedEvent" too.
 */
public class GameInitializationCompletedEvent extends EventBase {

	private MinecraftClient mc;
	private FabricLoader fabric;
	
	public GameInitializationCompletedEvent(MinecraftClient mc, FabricLoader fabric) {
		this.mc = mc;
		this.fabric = fabric;
	}
	
	@Override
	public boolean isCancelable() {
		return false;
	}
	
	public MinecraftClient getMinecraft() {
		return this.mc;
	}
	
	public FabricLoader getFabric() {
		return this.fabric;
	}

}
