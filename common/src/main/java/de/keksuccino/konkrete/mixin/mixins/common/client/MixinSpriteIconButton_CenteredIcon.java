package de.keksuccino.konkrete.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.keksuccino.konkrete.util.rendering.ui.widget.CustomizableWidget;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SpriteIconButton.CenteredIcon.class)
public abstract class MixinSpriteIconButton_CenteredIcon extends Button {

    @SuppressWarnings("unused")
    private MixinSpriteIconButton_CenteredIcon() {
        super(0, 0, 0, 0, Component.empty(), button -> {}, DEFAULT_NARRATION);
    }

    /** @reason Centered icon buttons omit their message, so an explicit custom label must replace the icon while preserving the normal background. */
    @WrapOperation(method = "extractContents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/SpriteIconButton$CenteredIcon;extractSprite(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V"))
    private void wrap_extractSprite_in_extractContents_Konkrete(SpriteIconButton.CenteredIcon instance, GuiGraphicsExtractor graphics, int x, int y, Operation<Void> original) {
        Component customLabel = this.getCustomLabelToRender_Konkrete();
        if (customLabel != null) {
            Component renderedLabel = this.active ? this.getMessage() : AbstractWidget.WithInactiveMessage.defaultInactiveMessage(customLabel);
            this.extractScrollingStringOverContents(graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE), renderedLabel, 2);
            return;
        }
        // extractSprite already uses the float-alpha blit overload. Applying alpha through the shared shader color too would square the widget alpha.
        original.call(instance, graphics, x, y);
    }

    @Unique
    @Nullable
    private Component getCustomLabelToRender_Konkrete() {
        CustomizableWidget widget = (CustomizableWidget) this;
        Component hoverLabel = widget.getHoverLabelKonkrete();
        if (hoverLabel != null && this.isHoveredOrFocused() && this.visible && this.active) return hoverLabel;
        return widget.getCustomLabelKonkrete();
    }
}
