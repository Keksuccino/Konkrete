package de.keksuccino.konkrete.mixin.mixins.common.client;

import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SplashRenderer.class)
public interface AccessorMixinSplashRenderer {

    @Accessor("splash") Component get_splash_Konkrete();

}
