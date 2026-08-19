package de.keksuccino.konkrete.mixin.mixins.common.client;

import de.keksuccino.konkrete.util.rendering.RenderRotationUtil;
import de.keksuccino.konkrete.util.rendering.RenderScaleUtil;
import de.keksuccino.konkrete.util.rendering.RenderTranslationUtil;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayDeque;
import java.util.Deque;

@Mixin(value = Matrix3x2fStack.class, remap = false)
public abstract class MixinMatrix3x2fStack extends Matrix3x2f {

    @Unique private final Deque<Float> renderScaleStack_Konkrete = new ArrayDeque<>();
    @Unique private final Deque<RenderTranslationUtil.TranslationState> renderTranslationStack_Konkrete = new ArrayDeque<>();
    @Unique private final Deque<RenderRotationUtil.RotationState> renderRotationStack_Konkrete = new ArrayDeque<>();

    @Inject(method = "pushMatrix", at = @At("TAIL"), remap = false)
    private void after_pushMatrix_Konkrete(CallbackInfoReturnable<Matrix3x2fStack> info) {
        ensureRenderScaleStackInitialized_Konkrete();
        this.renderScaleStack_Konkrete.addLast(this.renderScaleStack_Konkrete.getLast());
        ensureRenderTranslationStackInitialized_Konkrete();
        this.renderTranslationStack_Konkrete.addLast(copyTranslation_Konkrete(this.renderTranslationStack_Konkrete.getLast()));
        ensureRenderRotationStackInitialized_Konkrete();
        this.renderRotationStack_Konkrete.addLast(new RenderRotationUtil.RotationState(this.renderRotationStack_Konkrete.getLast()));
        updateActiveRenderState_Konkrete();
    }

    @Inject(method = "popMatrix", at = @At("TAIL"), remap = false)
    private void after_popMatrix_Konkrete(CallbackInfoReturnable<Matrix3x2fStack> info) {
        ensureRenderScaleStackInitialized_Konkrete();
        if (this.renderScaleStack_Konkrete.size() > 1) this.renderScaleStack_Konkrete.removeLast();
        ensureRenderTranslationStackInitialized_Konkrete();
        if (this.renderTranslationStack_Konkrete.size() > 1) this.renderTranslationStack_Konkrete.removeLast();
        ensureRenderRotationStackInitialized_Konkrete();
        if (this.renderRotationStack_Konkrete.size() > 1) this.renderRotationStack_Konkrete.removeLast();
        updateActiveRenderState_Konkrete();
    }

    @Inject(method = "clear", at = @At("TAIL"), remap = false)
    private void after_clear_Konkrete(CallbackInfoReturnable<Matrix3x2fStack> info) {
        this.renderScaleStack_Konkrete.clear();
        this.renderTranslationStack_Konkrete.clear();
        this.renderRotationStack_Konkrete.clear();
        ensureRenderScaleStackInitialized_Konkrete();
        ensureRenderTranslationStackInitialized_Konkrete();
        ensureRenderRotationStackInitialized_Konkrete();
        updateActiveRenderState_Konkrete();
    }

    /**
     * Matrix3x2fStack inherits these mutation methods instead of declaring them. Mixin injection cannot
     * target an inherited body, so the overrides intentionally call JOML first and then mirror state.
     */
    @Override
    public Matrix3x2f translate(float x, float y) {
        Matrix3x2f result = super.translate(x, y);
        afterTranslate_Konkrete(x, y);
        return result;
    }

    @Override
    public Matrix3x2f scale(float x, float y) {
        Matrix3x2f result = super.scale(x, y);
        afterScale_Konkrete(x, y);
        return result;
    }

    @Override
    public Matrix3x2f scale(float scale) {
        // JOML's one-argument overload delegates virtually to scale(x, y); call its destination overload directly to avoid recording this transform twice.
        Matrix3x2f result = super.scale(scale, scale, this);
        afterScale_Konkrete(scale, scale);
        return result;
    }

    @Override
    public Matrix3x2f rotate(float angle) {
        Matrix3x2f result = super.rotate(angle);
        afterRotate_Konkrete(angle);
        return result;
    }

    @Unique
    private void afterTranslate_Konkrete(float x, float y) {
        ensureRenderScaleStackInitialized_Konkrete();
        ensureRenderTranslationStackInitialized_Konkrete();
        RenderTranslationUtil.TranslationState translation = this.renderTranslationStack_Konkrete.removeLast();
        float scale = this.renderScaleStack_Konkrete.getLast();
        translation.x += x * scale;
        translation.y += y * scale;
        this.renderTranslationStack_Konkrete.addLast(translation);
        updateActiveRenderTranslation_Konkrete();
    }

    @Unique
    private void afterScale_Konkrete(float x, float y) {
        ensureRenderScaleStackInitialized_Konkrete();
        float currentScale = this.renderScaleStack_Konkrete.removeLast();
        this.renderScaleStack_Konkrete.addLast(currentScale * RenderScaleUtil.getAbsoluteScaleFactor_Konkrete(x, y, 1.0F));
        updateActiveRenderScale_Konkrete();
    }

    @Unique
    private void afterRotate_Konkrete(float angle) {
        ensureRenderRotationStackInitialized_Konkrete();
        RenderRotationUtil.RotationState rotation = this.renderRotationStack_Konkrete.removeLast();
        multiplyZRotation_Konkrete(rotation, angle);
        this.renderRotationStack_Konkrete.addLast(rotation);
        updateActiveRenderRotation_Konkrete();
    }

    @Unique
    private static void multiplyZRotation_Konkrete(RenderRotationUtil.RotationState state, float angle) {
        float halfAngle = angle * 0.5F;
        float qz = (float) Math.sin(halfAngle);
        float qw = (float) Math.cos(halfAngle);
        float nextX = state.x * qw + state.y * qz;
        float nextY = -state.x * qz + state.y * qw;
        float nextZ = state.w * qz + state.z * qw;
        float nextW = state.w * qw - state.z * qz;
        state.x = nextX;
        state.y = nextY;
        state.z = nextZ;
        state.w = nextW;
        float length = (float) Math.sqrt(state.x * state.x + state.y * state.y + state.z * state.z + state.w * state.w);
        if (length <= 0.0F || !Float.isFinite(length)) return;
        float inverseLength = 1.0F / length;
        state.x *= inverseLength;
        state.y *= inverseLength;
        state.z *= inverseLength;
        state.w *= inverseLength;
    }

    @Unique
    private static RenderTranslationUtil.TranslationState copyTranslation_Konkrete(RenderTranslationUtil.TranslationState translation) {
        RenderTranslationUtil.TranslationState copy = new RenderTranslationUtil.TranslationState();
        copy.x = translation.x;
        copy.y = translation.y;
        copy.z = translation.z;
        return copy;
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
