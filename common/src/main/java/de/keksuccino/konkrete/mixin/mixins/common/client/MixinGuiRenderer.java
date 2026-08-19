package de.keksuccino.konkrete.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.konkrete.mixin.support.client.RenderPhaseActionEntry;
import de.keksuccino.konkrete.util.rendering.GuiRenderPhaseAction;
import de.keksuccino.konkrete.util.window.PreciseGuiScaleWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Supplier;

@Mixin(GuiRenderer.class)
public abstract class MixinGuiRenderer {

    @Shadow @Final private List<?> draws;
    @Shadow @Final private StagedVertexBuffer vertexBuffer;
    @Shadow @Nullable private ScreenRectangle previousScissorArea;
    @Shadow @Nullable private RenderPipeline previousPipeline;
    @Shadow @Nullable private TextureSetup previousTextureSetup;
    @Shadow @Nullable private StagedVertexBuffer.Draw previousDraw;
    @Shadow private int firstDrawIndexAfterBlur;
    @Unique private final Projection preciseGuiProjection_Konkrete = new Projection();
    @Unique private final List<RenderPhaseActionEntry> renderPhaseActions_Konkrete = new ArrayList<>();
    @Unique private GuiRenderState.TraverseRange activeTraverseRange_Konkrete = GuiRenderState.TraverseRange.ALL;
    @Unique private int nextRenderPhaseActionOrder_Konkrete;

    @Shadow
    private void enableScissor(ScreenRectangle rectangle, RenderPass renderPass) {
        throw new AssertionError();
    }

    @WrapMethod(method = "render")
    private void wrap_render_Konkrete(Operation<Void> original) {
        clearRenderPhaseActions_Konkrete();
        try {
            original.call();
        } finally {
            clearRenderPhaseActions_Konkrete();
        }
    }

    @Inject(method = "addElementsToMeshes", at = @At("HEAD"))
    private void before_addElementsToMeshes_Konkrete(GuiRenderState.TraverseRange range, CallbackInfo info) {
        this.activeTraverseRange_Konkrete = range;
    }

    @Inject(method = "addElementsToMeshes", at = @At("RETURN"))
    private void after_addElementsToMeshes_Konkrete(GuiRenderState.TraverseRange range, CallbackInfo info) {
        this.activeTraverseRange_Konkrete = GuiRenderState.TraverseRange.ALL;
    }

    /** @reason Render-phase actions are extraction markers, not mesh elements; force a batch boundary and retain their exact traversal position. */
    @Inject(method = "addElementToMesh", at = @At("HEAD"), cancellable = true)
    private void before_addElementToMesh_Konkrete(GuiElementRenderState elementState, CallbackInfo info) {
        if (!(elementState instanceof GuiRenderPhaseAction action)) return;
        this.previousDraw = null;
        this.previousPipeline = null;
        this.previousTextureSetup = null;
        this.previousScissorArea = null;
        this.renderPhaseActions_Konkrete.add(new RenderPhaseActionEntry(this.draws.size(), this.nextRenderPhaseActionOrder_Konkrete++, this.activeTraverseRange_Konkrete, action));
        info.cancel();
    }

    @Inject(method = "draw", at = @At("HEAD"))
    private void before_draw_Konkrete(CallbackInfo info) {
        if (this.renderPhaseActions_Konkrete.isEmpty()) return;
        if (this.draws.isEmpty()) {
            this.renderPhaseActions_Konkrete.stream().sorted(Comparator.comparingInt(RenderPhaseActionEntry::order)).forEach(entry -> entry.action().executeRender_Konkrete());
        } else if (this.firstDrawIndexAfterBlur == 0) {
            // Vanilla skips the before-blur draw range when it has no meshes, so execute markers at that otherwise-missing boundary here.
            executeBoundaryActions_Konkrete(GuiRenderState.TraverseRange.BEFORE_BLUR, 0);
        }
    }

    @Inject(method = "draw", at = @At("RETURN"))
    private void after_draw_Konkrete(CallbackInfo info) {
        if (this.draws.isEmpty() || this.firstDrawIndexAfterBlur != this.draws.size()) return;
        // Vanilla likewise skips the after-blur draw range when it contains only extraction markers.
        executeBoundaryActions_Konkrete(GuiRenderState.TraverseRange.AFTER_BLUR, this.firstDrawIndexAfterBlur);
    }

    /** @reason Vanilla owns one RenderPass per draw range; actions inside that range require the pass to end before external render work and resume afterward. */
    @Inject(method = "executeDrawRange", at = @At("HEAD"), cancellable = true)
    private void before_executeDrawRange_Konkrete(Supplier<String> label, RenderTarget mainRenderTarget, GpuBufferSlice dynamicTransforms, int startIndex, int endIndex, CallbackInfo info) {
        GuiRenderState.TraverseRange executeRange = resolveExecuteRange_Konkrete(startIndex);
        List<RenderPhaseActionEntry> actions = this.renderPhaseActions_Konkrete.stream().filter(entry -> entry.range() == executeRange).filter(entry -> entry.drawIndex() >= startIndex && entry.drawIndex() <= endIndex).sorted(Comparator.comparingInt(RenderPhaseActionEntry::drawIndex).thenComparingInt(RenderPhaseActionEntry::order)).toList();
        if (actions.isEmpty()) return;
        info.cancel();
        int currentIndex = startIndex;
        for (RenderPhaseActionEntry entry : actions) {
            int actionIndex = Math.max(startIndex, Math.min(endIndex, entry.drawIndex()));
            executePlainDrawRange_Konkrete(label, mainRenderTarget, dynamicTransforms, currentIndex, actionIndex);
            entry.action().executeRender_Konkrete();
            currentIndex = actionIndex;
        }
        executePlainDrawRange_Konkrete(label, mainRenderTarget, dynamicTransforms, currentIndex, endIndex);
    }

    @WrapOperation(method = "draw", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ProjectionMatrixBuffer;getBuffer(Lnet/minecraft/client/renderer/Projection;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;"))
    private GpuBufferSlice wrap_getBuffer_Konkrete(ProjectionMatrixBuffer projectionBuffer, Projection projection, Operation<GpuBufferSlice> original) {
        Window window = Minecraft.getInstance().getWindow();
        if (!((Object) window instanceof PreciseGuiScaleWindow)) return original.call(projectionBuffer, projection);
        PreciseGuiScaleWindow preciseWindow = (PreciseGuiScaleWindow) (Object) window;
        double preciseScale = preciseWindow.getPreciseGuiScale_Konkrete();
        if (Math.abs(preciseScale - window.getGuiScale()) < 1.0E-6D) return original.call(projectionBuffer, projection);
        this.preciseGuiProjection_Konkrete.setupOrtho(projection.zNear(), projection.zFar(), (float) (window.getWidth() / preciseScale), (float) (window.getHeight() / preciseScale), projection.invertY());
        return original.call(projectionBuffer, this.preciseGuiProjection_Konkrete);
    }

    @Inject(method = "enableScissor", at = @At("HEAD"), cancellable = true)
    private void before_enableScissor_Konkrete(ScreenRectangle rectangle, RenderPass renderPass, CallbackInfo info) {
        Window window = Minecraft.getInstance().getWindow();
        if (!((Object) window instanceof PreciseGuiScaleWindow)) return;
        PreciseGuiScaleWindow preciseWindow = (PreciseGuiScaleWindow) (Object) window;
        double preciseScale = preciseWindow.getPreciseGuiScale_Konkrete();
        if (Math.abs(preciseScale - window.getGuiScale()) < 1.0E-6D) return;
        info.cancel();
        int windowWidth = window.getWidth();
        int windowHeight = window.getHeight();
        int left = (int) Math.max(0.0D, rectangle.left() * preciseScale);
        int top = (int) Math.max(0.0D, rectangle.top() * preciseScale);
        int right = (int) Math.min(windowWidth, rectangle.right() * preciseScale);
        int bottom = (int) Math.min(windowHeight, rectangle.bottom() * preciseScale);
        int width = right - left;
        int height = bottom - top;
        if (width > 0 && height > 0) renderPass.enableScissor(left, windowHeight - bottom, width, height);
        else if (windowWidth > 0 && windowHeight > 0) renderPass.enableScissor(0, 0, 1, 1);
        else renderPass.disableScissor();
    }

    @Unique
    private void clearRenderPhaseActions_Konkrete() {
        this.renderPhaseActions_Konkrete.clear();
        this.nextRenderPhaseActionOrder_Konkrete = 0;
        this.activeTraverseRange_Konkrete = GuiRenderState.TraverseRange.ALL;
    }

    @Unique
    private GuiRenderState.TraverseRange resolveExecuteRange_Konkrete(int startIndex) {
        return startIndex >= this.firstDrawIndexAfterBlur ? GuiRenderState.TraverseRange.AFTER_BLUR : GuiRenderState.TraverseRange.BEFORE_BLUR;
    }

    @Unique
    private void executeBoundaryActions_Konkrete(GuiRenderState.TraverseRange range, int drawIndex) {
        this.renderPhaseActions_Konkrete.stream().filter(entry -> entry.range() == range && entry.drawIndex() == drawIndex).sorted(Comparator.comparingInt(RenderPhaseActionEntry::order)).forEach(entry -> entry.action().executeRender_Konkrete());
    }

    @Unique
    private void executePlainDrawRange_Konkrete(Supplier<String> label, RenderTarget mainRenderTarget, GpuBufferSlice dynamicTransforms, int startIndex, int endIndex) {
        if (startIndex >= endIndex) return;
        try (RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(label, mainRenderTarget.getColorTextureView(), Optional.empty(), mainRenderTarget.useDepth ? mainRenderTarget.getDepthTextureView() : null, OptionalDouble.empty())) {
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", dynamicTransforms);
            for (int index = startIndex; index < endIndex; index++) executeDraw_Konkrete(this.draws.get(index), renderPass);
        }
    }

    @Unique
    private void executeDraw_Konkrete(Object drawObject, RenderPass renderPass) {
        AccessorMixinGuiRendererDraw draw = (AccessorMixinGuiRendererDraw) drawObject;
        StagedVertexBuffer.ExecuteInfo executeInfo = this.vertexBuffer.getExecuteInfo(draw.get_draw_Konkrete());
        if (executeInfo == null) return;
        TextureSetup textureSetup = draw.get_textureSetup_Konkrete();
        renderPass.setPipeline(draw.get_pipeline_Konkrete());
        renderPass.setVertexBuffer(0, executeInfo.vertexBuffer().slice());
        ScreenRectangle scissorArea = draw.get_scissorArea_Konkrete();
        if (scissorArea != null) this.enableScissor(scissorArea, renderPass);
        else renderPass.disableScissor();
        if (textureSetup.texure0() != null) renderPass.bindTexture("Sampler0", textureSetup.texure0(), textureSetup.sampler0());
        if (textureSetup.texure1() != null) renderPass.bindTexture("Sampler1", textureSetup.texure1(), textureSetup.sampler1());
        if (textureSetup.texure2() != null) renderPass.bindTexture("Sampler2", textureSetup.texure2(), textureSetup.sampler2());
        renderPass.setIndexBuffer(executeInfo.indexBuffer(), executeInfo.indexType());
        renderPass.drawIndexed(executeInfo.indexCount(), 1, executeInfo.firstIndex(), executeInfo.baseVertex(), 0);
    }

}
