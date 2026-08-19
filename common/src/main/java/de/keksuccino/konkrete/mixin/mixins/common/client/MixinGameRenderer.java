package de.keksuccino.konkrete.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import de.keksuccino.konkrete.util.rendering.GuiBlurRenderer;
import net.minecraft.client.renderer.GameRenderer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {

    @Unique private static final Logger LOGGER_KONKRETE = LogManager.getLogger();

    /** @reason Release Konkrete's reusable blur textures and GPU buffers only after vanilla has stopped issuing render work. */
    @WrapMethod(method = "close")
    private void wrap_close_Konkrete(Operation<Void> original) {
        try {
            original.call();
        } finally {
            try {
                GuiBlurRenderer.close_Konkrete();
            } catch (Throwable throwable) {
                LOGGER_KONKRETE.error("[KONKRETE] Failed to release GUI blur GPU resources", throwable);
            }
        }
    }

}
