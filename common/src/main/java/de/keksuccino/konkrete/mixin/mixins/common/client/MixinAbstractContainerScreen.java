package de.keksuccino.konkrete.mixin.mixins.common.client;

import de.keksuccino.konkrete.util.rendering.ui.UIInputRouter;
import de.keksuccino.konkrete.util.rendering.ui.UIPointerTracker;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public class MixinAbstractContainerScreen extends Screen {

    @Unique private final UIPointerTracker pointerTracker_Konkrete = new UIPointerTracker();

    @SuppressWarnings("unused")
    private MixinAbstractContainerScreen() {
        super(Component.empty());
    }

    /** @reason Container screens need explicit routing for reusable UI widgets so their interactions take precedence over slot handling. */
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void before_mouseClicked_Konkrete(MouseButtonEvent event, boolean isDoubleClick, CallbackInfoReturnable<Boolean> info) {
        GuiEventListener listener = this.pointerTracker_Konkrete.routeMouseClicked(this.children(), event, isDoubleClick);
        if (listener == null) return;
        // Track the actual consumer because controls may intentionally consume without taking focus.
        if (listener.shouldTakeFocusAfterInteraction()) {
            this.setFocused(listener);
            if (event.button() == 0) this.setDragging(true);
        }
        info.setReturnValue(true);
    }

    /** @reason Container slot release logic runs before its super call and must not consume releases owned by reusable UI controls. */
    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void before_mouseReleased_Konkrete(MouseButtonEvent event, CallbackInfoReturnable<Boolean> info) {
        boolean released = this.pointerTracker_Konkrete.dispatchMouseReleased(event);
        if (!released) released = UIInputRouter.routeMouseReleased(this.children(), null, event, UIInputRouter.MouseReleaseRouting.CAPTURED_COMPONENTS_ONLY);
        if (!released) return;
        if (event.button() == 0 && this.isDragging()) this.setDragging(false);
        info.setReturnValue(true);
    }

    /** @reason Container screens must not start slot dragging while a reusable UI control owns the active press. */
    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void before_mouseDragged_Konkrete(MouseButtonEvent event, double dragX, double dragY, CallbackInfoReturnable<Boolean> info) {
        if (this.pointerTracker_Konkrete.dispatchMouseDragged(event, dragX, dragY)) info.setReturnValue(true);
    }

}
