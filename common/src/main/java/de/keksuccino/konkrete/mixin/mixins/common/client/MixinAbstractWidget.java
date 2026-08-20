package de.keksuccino.konkrete.mixin.mixins.common.client;

import de.keksuccino.konkrete.mixin.support.client.widget.WidgetCustomizationState;
import de.keksuccino.konkrete.mixin.support.client.widget.WidgetLabelRenderer;
import de.keksuccino.konkrete.util.rendering.DrawableColor;
import de.keksuccino.konkrete.util.rendering.ui.widget.CustomizableWidget;
import de.keksuccino.konkrete.util.rendering.ui.widget.UniqueWidget;
import de.keksuccino.konkrete.util.resource.RenderableResource;
import de.keksuccino.konkrete.util.resource.resources.audio.IAudio;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.function.Consumer;

@SuppressWarnings("unused")
@Mixin(AbstractWidget.class)
public abstract class MixinAbstractWidget implements CustomizableWidget, UniqueWidget {

    @Shadow protected float alpha;
    @Shadow public boolean visible;
    @Shadow public boolean active;
    @Shadow protected boolean isHovered;
    @Shadow protected int height;
    @Shadow protected int width;
    @Shadow
    public abstract void setFocused(boolean focused);
    @Shadow
    public abstract boolean isFocused();
    @Shadow
    public abstract boolean isHoveredOrFocused();
    @Shadow
    public abstract int getX();
    @Shadow
    public abstract int getY();
    @Shadow
    public abstract int getWidth();
    @Shadow
    public abstract int getHeight();
    @Unique private final WidgetCustomizationState customizationState_Konkrete = new WidgetCustomizationState();
    @Unique @Nullable private GuiGraphicsExtractor activeGraphics_Konkrete;
    @Unique private boolean customizationInitialized_Konkrete;

    /** @reason Custom bounds, rotated hitboxes, and hidden state must be resolved before vanilla snapshots widget state. */
    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void before_extractRenderState_Konkrete(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial, CallbackInfo info) {
        this.activeGraphics_Konkrete = graphics;
        this.initializeCustomization_Konkrete();
        this.applyCustomDimensions_Konkrete();
        this.isHovered = this.visible && graphics.containsPointInScissor(mouseX, mouseY) && this.containsWidgetPoint_Konkrete(mouseX, mouseY);
        if (this.customizationState_Konkrete.isHidden()) {
            this.isHovered = false;
            this.setFocused(false);
        }
        this.tickHoverStateListenersKonkrete(this.isHovered);
        this.tickFocusStateListenersKonkrete(this.isFocused());
        this.tickHoverOrFocusStateListenersKonkrete(this.isHoveredOrFocused());
        if (this.customizationState_Konkrete.isHidden()) {
            this.activeGraphics_Konkrete = null;
            info.cancel();
        }
    }

    /** @reason Vanilla overwrites isHovered with its unrotated rectangle immediately before extracting widget content. */
    @Inject(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractWidget;extractWidgetRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V", shift = At.Shift.BEFORE))
    private void before_extractWidgetRenderState_in_extractRenderState_Konkrete(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial, CallbackInfo info) {
        this.isHovered = this.visible && graphics.containsPointInScissor(mouseX, mouseY) && this.containsWidgetPoint_Konkrete(mouseX, mouseY);
    }

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void after_extractRenderState_Konkrete(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial, CallbackInfo info) {
        this.activeGraphics_Konkrete = null;
    }

    @Inject(method = "playDownSound", at = @At("HEAD"), cancellable = true)
    private void before_playDownSound_Konkrete(SoundManager soundManager, CallbackInfo info) {
        IAudio sound = this.customizationState_Konkrete.getCustomClickSound();
        if (sound == null) return;
        sound.stop();
        sound.play();
        info.cancel();
    }

    @Inject(method = "getMessage", at = @At("RETURN"), cancellable = true)
    private void after_getMessage_Konkrete(CallbackInfoReturnable<Component> info) {
        Component result = info.getReturnValue();
        boolean hovered = this.isHoveredOrFocused() && this.visible && this.active;
        if (hovered && this.customizationState_Konkrete.getHoverLabel() != null) result = this.customizationState_Konkrete.getHoverLabel();
        else if (this.customizationState_Konkrete.getCustomLabel() != null) result = this.customizationState_Konkrete.getCustomLabel();
        if (result == null) return;
        DrawableColor appliedColor = hovered && this.customizationState_Konkrete.getLabelHoverColor() != null ? this.customizationState_Konkrete.getLabelHoverColor() : this.customizationState_Konkrete.getLabelBaseColor();
        boolean underline = hovered && this.customizationState_Konkrete.isUnderlineLabelOnHover();
        if (!underline && appliedColor == null) {
            info.setReturnValue(result);
            return;
        }
        int color = appliedColor != null ? appliedColor.getColorInt() & 0xFFFFFF : 0;
        info.setReturnValue(result.copy().withStyle(style -> appliedColor != null ? (underline ? style.withUnderlined(true) : style).withColor(color) : style.withUnderlined(true)));
    }

    /** @reason Minecraft's active text collector always uses a shadow and cannot express a scaled widget label. */
    @Inject(method = "extractScrollingStringOverContents", at = @At("HEAD"), cancellable = true)
    private void before_extractScrollingStringOverContents_Konkrete(ActiveTextCollector output, Component component, int margin, CallbackInfo info) {
        GuiGraphicsExtractor graphics = this.activeGraphics_Konkrete;
        float scale = this.customizationState_Konkrete.getLabelScale();
        boolean shadow = this.customizationState_Konkrete.isLabelShadow();
        if (graphics == null || scale == 1.0F && shadow) return;
        WidgetLabelRenderer.INSTANCE.renderScrollingLabel(this.asWidget_Konkrete(), graphics, Minecraft.getInstance().font, component, margin, shadow, ARGB.white(this.alpha));
        info.cancel();
    }

    @Inject(method = "isMouseOver", at = @At("HEAD"), cancellable = true)
    private void before_isMouseOver_Konkrete(double mouseX, double mouseY, CallbackInfoReturnable<Boolean> info) {
        if (this.customizationState_Konkrete.isHidden()) {
            info.setReturnValue(false);
            return;
        }
        if (this.customizationState_Konkrete.isHitboxRotationActive()) info.setReturnValue(this.visible && this.active && this.containsWidgetPoint_Konkrete(mouseX, mouseY));
    }

    @Inject(method = "isValidClickButton", at = @At("HEAD"), cancellable = true)
    private void before_isValidClickButton_Konkrete(MouseButtonInfo buttonInfo, CallbackInfoReturnable<Boolean> info) {
        if (this.customizationState_Konkrete.isHidden()) info.setReturnValue(false);
    }

    @Inject(method = "nextFocusPath", at = @At("HEAD"), cancellable = true)
    private void before_nextFocusPath_Konkrete(FocusNavigationEvent event, CallbackInfoReturnable<ComponentPath> info) {
        if (this.customizationState_Konkrete.isHidden()) info.setReturnValue(null);
    }

    @Inject(method = "getX", at = @At("RETURN"), cancellable = true)
    private void after_getX_Konkrete(CallbackInfoReturnable<Integer> info) {
        if (this.customizationState_Konkrete.getCustomX() != null) info.setReturnValue(this.customizationState_Konkrete.getCustomX());
    }

    @Inject(method = "getY", at = @At("RETURN"), cancellable = true)
    private void after_getY_Konkrete(CallbackInfoReturnable<Integer> info) {
        if (this.customizationState_Konkrete.getCustomY() != null) info.setReturnValue(this.customizationState_Konkrete.getCustomY());
    }

    @Inject(method = "getWidth", at = @At("RETURN"), cancellable = true)
    private void after_getWidth_Konkrete(CallbackInfoReturnable<Integer> info) {
        Integer customWidth = this.customizationState_Konkrete.getCustomWidth();
        if (customWidth != null && customWidth > 0) info.setReturnValue(customWidth);
    }

    @Inject(method = "getHeight", at = @At("RETURN"), cancellable = true)
    private void after_getHeight_Konkrete(CallbackInfoReturnable<Integer> info) {
        Integer customHeight = this.customizationState_Konkrete.getCustomHeight();
        if (customHeight != null && customHeight > 0) info.setReturnValue(customHeight);
    }

    @Unique
    private void initializeCustomization_Konkrete() {
        if (this.customizationInitialized_Konkrete) return;
        this.customizationInitialized_Konkrete = true;
        this.addHoverOrFocusStateListenerKonkrete(hovered -> {
            if (hovered && !this.customizationState_Konkrete.isHidden() && this.visible && this.active) this.playHoverSound_Konkrete();
        });
        this.addHoverOrFocusStateListenerKonkrete(hovered -> {
            if (!hovered && !this.customizationState_Konkrete.isHidden() && this.visible && this.active) this.playUnhoverSound_Konkrete();
        });
        this.addHoverOrFocusStateListenerKonkrete(hovered -> {
            CustomBackgroundResetBehavior behavior = this.customizationState_Konkrete.getCustomBackgroundResetBehavior();
            if (hovered && (behavior == CustomBackgroundResetBehavior.RESET_ON_HOVER || behavior == CustomBackgroundResetBehavior.RESET_ON_HOVER_AND_UNHOVER)) this.customizationState_Konkrete.stopBackgrounds();
            if (!hovered && (behavior == CustomBackgroundResetBehavior.RESET_ON_UNHOVER || behavior == CustomBackgroundResetBehavior.RESET_ON_HOVER_AND_UNHOVER)) this.customizationState_Konkrete.stopBackgrounds();
        });
    }

    @Unique
    private void applyCustomDimensions_Konkrete() {
        Integer customWidth = this.customizationState_Konkrete.getCustomWidth();
        if (customWidth != null && customWidth > 0) {
            this.customizationState_Konkrete.captureOriginalWidth(this.width);
            this.width = customWidth;
        }
        Integer customHeight = this.customizationState_Konkrete.getCustomHeight();
        if (customHeight != null && customHeight > 0) {
            this.customizationState_Konkrete.captureOriginalHeight(this.height);
            this.height = customHeight;
        }
    }

    @Unique
    private void playHoverSound_Konkrete() {
        IAudio sound = this.customizationState_Konkrete.getHoverSound();
        if (sound == null) return;
        sound.stop();
        sound.play();
    }

    @Unique
    private void playUnhoverSound_Konkrete() {
        IAudio sound = this.customizationState_Konkrete.getUnhoverSound();
        if (sound == null) return;
        sound.stop();
        sound.play();
    }

    @Unique
    private boolean containsWidgetPoint_Konkrete(double mouseX, double mouseY) {
        int width = this.getWidth();
        int height = this.getHeight();
        if (this.customizationState_Konkrete.isHitboxRotationActive()) return this.customizationState_Konkrete.containsRotatedPoint(mouseX, mouseY, this.getX(), this.getY(), width, height);
        return width > 0 && height > 0 && mouseX >= this.getX() && mouseY >= this.getY() && mouseX < this.getX() + width && mouseY < this.getY() + height;
    }

    @Unique
    private AbstractWidget asWidget_Konkrete() {
        return (AbstractWidget) (Object) this;
    }

    @Unique
    @Override
    public void resetWidgetCustomizationsKonkrete() {
        Integer originalWidth = this.customizationState_Konkrete.getOriginalWidth();
        Integer originalHeight = this.customizationState_Konkrete.getOriginalHeight();
        this.customizationState_Konkrete.resetCustomizationValues();
        this.tickHoverStateListenersKonkrete(false);
        this.tickFocusStateListenersKonkrete(false);
        this.tickHoverOrFocusStateListenersKonkrete(false);
        for (Runnable listener : List.copyOf(this.getResetCustomizationsListenersKonkrete())) listener.run();
        if (originalWidth != null) this.width = originalWidth;
        if (originalHeight != null) this.height = originalHeight;
        this.customizationState_Konkrete.clearOriginalSize();
    }

    @Unique
    @Override
    public void resetWidgetSizeAndPositionKonkrete() {
        Integer originalWidth = this.customizationState_Konkrete.getOriginalWidth();
        Integer originalHeight = this.customizationState_Konkrete.getOriginalHeight();
        this.customizationState_Konkrete.clearCustomBounds();
        if (originalWidth != null) this.width = originalWidth;
        if (originalHeight != null) this.height = originalHeight;
        this.customizationState_Konkrete.clearOriginalSize();
    }

    @Unique
    @Override
    public void addResetCustomizationsListenerKonkrete(@NotNull Runnable listener) { this.customizationState_Konkrete.getResetCustomizationsListeners().add(listener); }
    @Unique
    @Override
    public @NotNull List<Runnable> getResetCustomizationsListenersKonkrete() { return this.customizationState_Konkrete.getResetCustomizationsListeners(); }
    @Unique
    @Override
    public void addHoverStateListenerKonkrete(@NotNull Consumer<Boolean> listener) { this.customizationState_Konkrete.getHoverStateListeners().add(listener); }
    @Unique
    @Override
    public void addFocusStateListenerKonkrete(@NotNull Consumer<Boolean> listener) { this.customizationState_Konkrete.getFocusStateListeners().add(listener); }
    @Unique
    @Override
    public void addHoverOrFocusStateListenerKonkrete(@NotNull Consumer<Boolean> listener) { this.customizationState_Konkrete.getHoverOrFocusStateListeners().add(listener); }
    @Unique
    @Override
    public @NotNull List<Consumer<Boolean>> getHoverStateListenersKonkrete() { return this.customizationState_Konkrete.getHoverStateListeners(); }
    @Unique
    @Override
    public @NotNull List<Consumer<Boolean>> getFocusStateListenersKonkrete() { return this.customizationState_Konkrete.getFocusStateListeners(); }
    @Unique
    @Override
    public @NotNull List<Consumer<Boolean>> getHoverOrFocusStateListenersKonkrete() { return this.customizationState_Konkrete.getHoverOrFocusStateListeners(); }
    @Unique
    @Override
    public boolean getLastHoverStateKonkrete() { return this.customizationState_Konkrete.getLastHoverState(); }
    @Unique
    @Override
    public void setLastHoverStateKonkrete(boolean hovered) { this.customizationState_Konkrete.setLastHoverState(hovered); }
    @Unique
    @Override
    public boolean getLastFocusStateKonkrete() { return this.customizationState_Konkrete.getLastFocusState(); }
    @Unique
    @Override
    public void setLastFocusStateKonkrete(boolean focused) { this.customizationState_Konkrete.setLastFocusState(focused); }
    @Unique
    @Override
    public boolean getLastHoverOrFocusStateKonkrete() { return this.customizationState_Konkrete.getLastHoverOrFocusState(); }
    @Unique
    @Override
    public void setLastHoverOrFocusStateKonkrete(boolean state) { this.customizationState_Konkrete.setLastHoverOrFocusState(state); }
    @Unique
    @Override
    public void setCustomLabelKonkrete(@Nullable Component label) { this.customizationState_Konkrete.setCustomLabel(label); }
    @Unique
    @Override
    public @Nullable Component getCustomLabelKonkrete() { return this.customizationState_Konkrete.getCustomLabel(); }
    @Unique
    @Override
    public void setHoverLabelKonkrete(@Nullable Component label) { this.customizationState_Konkrete.setHoverLabel(label); }
    @Unique
    @Override
    public @Nullable Component getHoverLabelKonkrete() { return this.customizationState_Konkrete.getHoverLabel(); }
    @Unique
    @Override
    public void setLabelHoverColorKonkrete(@Nullable DrawableColor color) { this.customizationState_Konkrete.setLabelHoverColor(color); }
    @Unique
    @Override
    public @Nullable DrawableColor getLabelHoverColorKonkrete() { return this.customizationState_Konkrete.getLabelHoverColor(); }
    @Unique
    @Override
    public void setLabelBaseColorKonkrete(@Nullable DrawableColor color) { this.customizationState_Konkrete.setLabelBaseColor(color); }
    @Unique
    @Override
    public @Nullable DrawableColor getLabelBaseColorKonkrete() { return this.customizationState_Konkrete.getLabelBaseColor(); }
    @Unique
    @Override
    public void setLabelScaleKonkrete(float scale) { this.customizationState_Konkrete.setLabelScale(scale); }
    @Unique
    @Override
    public float getLabelScaleKonkrete() { return this.customizationState_Konkrete.getLabelScale(); }
    @Unique
    @Override
    public void setUnderlineLabelOnHoverKonkrete(boolean underline) { this.customizationState_Konkrete.setUnderlineLabelOnHover(underline); }
    @Unique
    @Override
    public boolean isUnderlineLabelOnHoverKonkrete() { return this.customizationState_Konkrete.isUnderlineLabelOnHover(); }
    @Unique
    @Override
    public void setLabelShadowKonkrete(boolean shadow) { this.customizationState_Konkrete.setLabelShadow(shadow); }
    @Unique
    @Override
    public boolean isLabelShadowKonkrete() { return this.customizationState_Konkrete.isLabelShadow(); }
    @Unique
    @Override
    public void setCustomClickSoundKonkrete(@Nullable IAudio sound) { this.customizationState_Konkrete.setCustomClickSound(sound); }
    @Unique
    @Override
    public @Nullable IAudio getCustomClickSoundKonkrete() { return this.customizationState_Konkrete.getCustomClickSound(); }
    @Unique
    @Override
    public void setHoverSoundKonkrete(@Nullable IAudio sound) { this.customizationState_Konkrete.setHoverSound(sound); }
    @Unique
    @Override
    public @Nullable IAudio getHoverSoundKonkrete() { return this.customizationState_Konkrete.getHoverSound(); }
    @Unique
    @Override
    public void setUnhoverSoundKonkrete(@Nullable IAudio sound) { this.customizationState_Konkrete.setUnhoverSound(sound); }
    @Unique
    @Override
    public @Nullable IAudio getUnhoverSoundKonkrete() { return this.customizationState_Konkrete.getUnhoverSound(); }
    @Unique
    @Override
    public void setHiddenKonkrete(boolean hidden) { this.customizationState_Konkrete.setHidden(hidden); }
    @Unique
    @Override
    public boolean isHiddenKonkrete() { return this.customizationState_Konkrete.isHidden(); }
    @Unique
    @Override
    public void setCustomBackgroundNormalKonkrete(@Nullable RenderableResource background) { this.customizationState_Konkrete.setCustomBackgroundNormal(background); }
    @Unique
    @Override
    public @Nullable RenderableResource getCustomBackgroundNormalKonkrete() { return this.customizationState_Konkrete.getCustomBackgroundNormal(); }
    @Unique
    @Override
    public void setCustomBackgroundHoverKonkrete(@Nullable RenderableResource background) { this.customizationState_Konkrete.setCustomBackgroundHover(background); }
    @Unique
    @Override
    public @Nullable RenderableResource getCustomBackgroundHoverKonkrete() { return this.customizationState_Konkrete.getCustomBackgroundHover(); }
    @Unique
    @Override
    public void setCustomBackgroundInactiveKonkrete(@Nullable RenderableResource background) { this.customizationState_Konkrete.setCustomBackgroundInactive(background); }
    @Unique
    @Override
    public @Nullable RenderableResource getCustomBackgroundInactiveKonkrete() { return this.customizationState_Konkrete.getCustomBackgroundInactive(); }
    @Unique
    @Override
    public void setNineSliceCustomBackground_Konkrete(boolean nineSlice) { this.customizationState_Konkrete.setNineSliceCustomBackground(nineSlice); }
    @Unique
    @Override
    public boolean isNineSliceCustomBackgroundTexture_Konkrete() { return this.customizationState_Konkrete.isNineSliceCustomBackground(); }
    @Unique
    @Override
    public void setNineSliceBorderX_Konkrete(int borderX) { this.customizationState_Konkrete.setNineSliceBorderX(borderX); }
    @Unique
    @Override
    public int getNineSliceCustomBackgroundBorderX_Konkrete() { return this.customizationState_Konkrete.getNineSliceBorderX(); }
    @Unique
    @Override
    public void setNineSliceBorderY_Konkrete(int borderY) { this.customizationState_Konkrete.setNineSliceBorderY(borderY); }
    @Unique
    @Override
    public int getNineSliceCustomBackgroundBorderY_Konkrete() { return this.customizationState_Konkrete.getNineSliceBorderY(); }
    @Unique
    @Override
    public void setNineSliceBorderTop_Konkrete(int border) { this.customizationState_Konkrete.setNineSliceBorderTop(border); }
    @Unique
    @Override
    public int getNineSliceCustomBackgroundBorderTop_Konkrete() { return this.customizationState_Konkrete.getNineSliceBorderTop(); }
    @Unique
    @Override
    public void setNineSliceBorderRight_Konkrete(int border) { this.customizationState_Konkrete.setNineSliceBorderRight(border); }
    @Unique
    @Override
    public int getNineSliceCustomBackgroundBorderRight_Konkrete() { return this.customizationState_Konkrete.getNineSliceBorderRight(); }
    @Unique
    @Override
    public void setNineSliceBorderBottom_Konkrete(int border) { this.customizationState_Konkrete.setNineSliceBorderBottom(border); }
    @Unique
    @Override
    public int getNineSliceCustomBackgroundBorderBottom_Konkrete() { return this.customizationState_Konkrete.getNineSliceBorderBottom(); }
    @Unique
    @Override
    public void setNineSliceBorderLeft_Konkrete(int border) { this.customizationState_Konkrete.setNineSliceBorderLeft(border); }
    @Unique
    @Override
    public int getNineSliceCustomBackgroundBorderLeft_Konkrete() { return this.customizationState_Konkrete.getNineSliceBorderLeft(); }
    @Unique
    @Override
    public void setCustomBackgroundResetBehaviorKonkrete(@NotNull CustomBackgroundResetBehavior behavior) { this.customizationState_Konkrete.setCustomBackgroundResetBehavior(behavior); }
    @Unique
    @Override
    public @NotNull CustomBackgroundResetBehavior getCustomBackgroundResetBehaviorKonkrete() { return this.customizationState_Konkrete.getCustomBackgroundResetBehavior(); }
    @Unique
    @Override
    public @Nullable Integer getCustomWidthKonkrete() { return this.customizationState_Konkrete.getCustomWidth(); }
    @Unique
    @Override
    public void setCustomWidthKonkrete(@Nullable Integer width) { this.customizationState_Konkrete.setCustomWidth(width); }
    @Unique
    @Override
    public @Nullable Integer getCustomHeightKonkrete() { return this.customizationState_Konkrete.getCustomHeight(); }
    @Unique
    @Override
    public void setCustomHeightKonkrete(@Nullable Integer height) { this.customizationState_Konkrete.setCustomHeight(height); }
    @Unique
    @Override
    public @Nullable Integer getCustomXKonkrete() { return this.customizationState_Konkrete.getCustomX(); }
    @Unique
    @Override
    public void setCustomXKonkrete(@Nullable Integer x) { this.customizationState_Konkrete.setCustomX(x); }
    @Unique
    @Override
    public @Nullable Integer getCustomYKonkrete() { return this.customizationState_Konkrete.getCustomY(); }
    @Unique
    @Override
    public void setCustomYKonkrete(@Nullable Integer y) { this.customizationState_Konkrete.setCustomY(y); }
    @Unique
    @Override
    public void setHitboxRotationKonkrete(float rotation, float verticalTilt, float horizontalTilt) { this.customizationState_Konkrete.setHitboxRotation(rotation, verticalTilt, horizontalTilt); }
    @Unique
    @Override
    public float getHitboxRotationDegreesKonkrete() { return this.customizationState_Konkrete.getHitboxRotationDegrees(); }
    @Unique
    @Override
    public float getHitboxVerticalTiltDegreesKonkrete() { return this.customizationState_Konkrete.getHitboxVerticalTiltDegrees(); }
    @Unique
    @Override
    public float getHitboxHorizontalTiltDegreesKonkrete() { return this.customizationState_Konkrete.getHitboxHorizontalTiltDegrees(); }
    @Unique
    @Override
    public AbstractWidget setWidgetIdentifierKonkrete(@Nullable String identifier) { this.customizationState_Konkrete.setWidgetIdentifier(identifier); return this.asWidget_Konkrete(); }
    @Unique
    @Override
    public @Nullable String getWidgetIdentifierKonkrete() { return this.customizationState_Konkrete.getWidgetIdentifier(); }

}
