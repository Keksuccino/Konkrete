package de.keksuccino.konkrete.mixin.mixins.common.client;

import de.keksuccino.konkrete.util.rendering.ui.widget.NavigatableWidget;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerEventHandler.class)
public abstract class MixinAbstractContainerEventHandler implements ContainerEventHandler {

    @Inject(method = "setFocused", at = @At("HEAD"), cancellable = true)
    private void before_setFocused_Konkrete(GuiEventListener listener, CallbackInfo info) {
        if (listener instanceof NavigatableWidget navigatable && !navigatable.isFocusable()) info.cancel();
    }

}
