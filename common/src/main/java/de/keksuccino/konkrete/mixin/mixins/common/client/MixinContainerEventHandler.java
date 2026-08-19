package de.keksuccino.konkrete.mixin.mixins.common.client;

import de.keksuccino.konkrete.util.rendering.ui.UIInputRouter;
import de.keksuccino.konkrete.util.rendering.ui.screen.VanillaMouseClickHandlingScreen;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ContainerEventHandler.class)
public interface MixinContainerEventHandler {

    /** @reason Reusable UI controls may be logical children without vanilla hover ownership, so route only those controls before vanilla's normal child search. */
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void before_mouseClicked_Konkrete(MouseButtonEvent event, boolean isDoubleClick, CallbackInfoReturnable<Boolean> info) {
        if (this instanceof VanillaMouseClickHandlingScreen || this instanceof AbstractContainerScreen<?>) return;
        GuiEventListener listener = UIInputRouter.routeMouseClicked(this.children(), event, isDoubleClick);
        if (listener == null) return;
        if (listener.shouldTakeFocusAfterInteraction()) {
            this.setFocused(listener);
            if (event.button() == 0) this.setDragging(true);
        }
        info.setReturnValue(true);
    }

    /** @reason Captured reusable controls must receive their matching release even when vanilla focus moved after the press. */
    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void before_mouseReleased_Konkrete(MouseButtonEvent event, CallbackInfoReturnable<Boolean> info) {
        if (this instanceof VanillaMouseClickHandlingScreen) return;
        if (!UIInputRouter.routeMouseReleased(this.children(), this.getFocused(), event, UIInputRouter.MouseReleaseRouting.BROADCAST_UI_COMPONENTS)) return;
        if (event.button() == 0 && this.isDragging()) this.setDragging(false);
        info.setReturnValue(true);
    }

    @Shadow List<? extends GuiEventListener> children();

    @Shadow @Nullable GuiEventListener getFocused();

    @Shadow void setFocused(GuiEventListener focused);

    @Shadow boolean isDragging();

    @Shadow void setDragging(boolean dragging);

}
