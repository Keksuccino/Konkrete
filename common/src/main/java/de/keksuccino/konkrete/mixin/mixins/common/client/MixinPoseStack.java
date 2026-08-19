package de.keksuccino.konkrete.mixin.mixins.common.client;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.konkrete.util.rendering.RenderRotationUtil;
import de.keksuccino.konkrete.util.rendering.RenderScaleUtil;
import de.keksuccino.konkrete.util.rendering.RenderTranslationUtil;
import org.joml.Quaternionfc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayDeque;
import java.util.Deque;

@Mixin(PoseStack.class)
public class MixinPoseStack {

    @Unique private final Deque<Float> renderScaleStack_Konkrete = new ArrayDeque<>();
    @Unique private final Deque<RenderTranslationUtil.TranslationState> renderTranslationStack_Konkrete = new ArrayDeque<>();
    @Unique private final Deque<RenderRotationUtil.RotationState> renderRotationStack_Konkrete = new ArrayDeque<>();

    @Inject(method = "pushPose", at = @At("TAIL"))
    private void after_pushPose_Konkrete(CallbackInfo info) {
        ensureRenderScaleStackInitialized_Konkrete();
        this.renderScaleStack_Konkrete.addLast(this.renderScaleStack_Konkrete.getLast());
        ensureRenderTranslationStackInitialized_Konkrete();
        RenderTranslationUtil.TranslationState previousTranslation = this.renderTranslationStack_Konkrete.getLast();
        RenderTranslationUtil.TranslationState nextTranslation = new RenderTranslationUtil.TranslationState();
        nextTranslation.x = previousTranslation.x;
        nextTranslation.y = previousTranslation.y;
        nextTranslation.z = previousTranslation.z;
        this.renderTranslationStack_Konkrete.addLast(nextTranslation);
        ensureRenderRotationStackInitialized_Konkrete();
        this.renderRotationStack_Konkrete.addLast(new RenderRotationUtil.RotationState(this.renderRotationStack_Konkrete.getLast()));
        updateActiveRenderState_Konkrete();
    }

    @Inject(method = "popPose", at = @At("TAIL"))
    private void after_popPose_Konkrete(CallbackInfo info) {
        ensureRenderScaleStackInitialized_Konkrete();
        if (this.renderScaleStack_Konkrete.size() > 1) this.renderScaleStack_Konkrete.removeLast();
        ensureRenderTranslationStackInitialized_Konkrete();
        if (this.renderTranslationStack_Konkrete.size() > 1) this.renderTranslationStack_Konkrete.removeLast();
        ensureRenderRotationStackInitialized_Konkrete();
        if (this.renderRotationStack_Konkrete.size() > 1) this.renderRotationStack_Konkrete.removeLast();
        updateActiveRenderState_Konkrete();
    }

    @Inject(method = "scale", at = @At("TAIL"))
    private void after_scale_Konkrete(float x, float y, float z, CallbackInfo info) {
        ensureRenderScaleStackInitialized_Konkrete();
        float currentScale = this.renderScaleStack_Konkrete.removeLast();
        this.renderScaleStack_Konkrete.addLast(currentScale * RenderScaleUtil.getAbsoluteScaleFactor_Konkrete(x, y, z));
        updateActiveRenderScale_Konkrete();
    }

    @Inject(method = "mulPose", at = @At("TAIL"))
    private void after_mulPose_Konkrete(Quaternionfc quaternion, CallbackInfo info) {
        ensureRenderRotationStackInitialized_Konkrete();
        RenderRotationUtil.RotationState rotation = this.renderRotationStack_Konkrete.removeLast();
        rotation.mul(quaternion);
        this.renderRotationStack_Konkrete.addLast(rotation);
        updateActiveRenderRotation_Konkrete();
    }

    @Inject(method = "translate(FFF)V", at = @At("TAIL"))
    private void after_translate_Konkrete(float x, float y, float z, CallbackInfo info) {
        ensureRenderScaleStackInitialized_Konkrete();
        ensureRenderTranslationStackInitialized_Konkrete();
        RenderTranslationUtil.TranslationState translation = this.renderTranslationStack_Konkrete.removeLast();
        // Use this PoseStack's scale rather than the thread-global snapshot so interleaved stacks cannot contaminate translation tracking.
        float scale = this.renderScaleStack_Konkrete.getLast();
        translation.x += x * scale;
        translation.y += y * scale;
        translation.z += z * scale;
        this.renderTranslationStack_Konkrete.addLast(translation);
        updateActiveRenderTranslation_Konkrete();
    }

    @Unique
    private void ensureRenderScaleStackInitialized_Konkrete() {
        if (this.renderScaleStack_Konkrete.isEmpty()) this.renderScaleStack_Konkrete.addLast(1.0F);
    }

    @Unique
    private void ensureRenderTranslationStackInitialized_Konkrete() {
        if (this.renderTranslationStack_Konkrete.isEmpty()) this.renderTranslationStack_Konkrete.addLast(new RenderTranslationUtil.TranslationState());
    }

    @Unique
    private void ensureRenderRotationStackInitialized_Konkrete() {
        if (this.renderRotationStack_Konkrete.isEmpty()) this.renderRotationStack_Konkrete.addLast(new RenderRotationUtil.RotationState());
    }

    @Unique
    private void updateActiveRenderState_Konkrete() {
        updateActiveRenderScale_Konkrete();
        updateActiveRenderTranslation_Konkrete();
        updateActiveRenderRotation_Konkrete();
    }

    @Unique
    private void updateActiveRenderScale_Konkrete() {
        RenderScaleUtil.setActiveRenderScale_Konkrete(this.renderScaleStack_Konkrete.getLast());
    }

    @Unique
    private void updateActiveRenderTranslation_Konkrete() {
        RenderTranslationUtil.TranslationState translation = this.renderTranslationStack_Konkrete.getLast();
        RenderTranslationUtil.setActiveRenderTranslation_Konkrete(translation.x, translation.y, translation.z);
    }

    @Unique
    private void updateActiveRenderRotation_Konkrete() {
        RenderRotationUtil.setActiveRenderRotation_Konkrete(this.renderRotationStack_Konkrete.getLast());
    }

}
