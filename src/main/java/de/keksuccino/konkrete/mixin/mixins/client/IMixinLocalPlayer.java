//TODO übernehmen 1.4.0
package de.keksuccino.konkrete.mixin.mixins.client;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LocalPlayer.class)
public interface IMixinLocalPlayer {

    @Invoker("getPermissionLevel") public int getPermissionLevelKonkrete();

}
