package de.keksuccino.konkrete.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import de.keksuccino.konkrete.util.rendering.RenderingUtils;
import de.keksuccino.konkrete.util.rendering.ui.widget.CustomizableWidget;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ImageButton.class)
public abstract class MixinImageButton extends Button {

    @SuppressWarnings("unused")
    private MixinImageButton() {
        super(0, 0, 0, 0, Component.empty(), button -> {}, DEFAULT_NARRATION);
    }

    /** @reason ImageButton bypasses AbstractButton's default sprite/label paths and omits widget alpha from its sprite draw. */
    @WrapOperation(method = "extractContents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
    private void wrap_blitSprite_in_extractContents_Konkrete(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height, Operation<Void> original) {
        ImageButton button = (ImageButton) (Object) this;
        CustomizableWidget customizable = (CustomizableWidget) (Object) this;
        int previousColor = RenderingUtils.getShaderColor();
        try {
            boolean renderVanilla = customizable.renderCustomBackgroundKonkrete(button, graphics, button.getX(), button.getY(), button.getWidth(), button.getHeight());
            if (!renderVanilla) {
                this.renderCustomLabel_Konkrete(button, customizable, graphics);
                return;
            }
            int widgetAlpha = ARGB.white(button.getAlpha());
            RenderingUtils.setShaderColor(graphics, previousColor == -1 ? widgetAlpha : ARGB.multiply(previousColor, widgetAlpha));
            original.call(graphics, pipeline, sprite, x, y, width, height);
        } finally {
            RenderingUtils.setShaderColor(graphics, previousColor);
        }
    }

    @Unique
    private void renderCustomLabel_Konkrete(ImageButton button, CustomizableWidget customizable, GuiGraphicsExtractor graphics) {
        Component label = button.isHoveredOrFocused() && button.visible && button.active && customizable.getHoverLabelKonkrete() != null ? customizable.getHoverLabelKonkrete() : customizable.getCustomLabelKonkrete();
        if (label == null) return;
        Component renderedLabel = button.active ? button.getMessage() : AbstractWidget.WithInactiveMessage.defaultInactiveMessage(label);
        this.extractScrollingStringOverContents(graphics.textRendererForWidget(button, GuiGraphicsExtractor.HoveredTextEffects.NONE), renderedLabel, 2);
    }

}
