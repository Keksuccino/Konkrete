package de.keksuccino.konkrete.events.client.mixins;

import de.keksuccino.konkrete.events.client.GuiInitCompletedEvent;
import de.keksuccino.konkrete.events.client.mixinbase.MixinCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

import de.keksuccino.konkrete.Konkrete;
import de.keksuccino.konkrete.events.client.GuiScreenEvent.DrawScreenEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GameRenderer.class)
public class MixinGameRenderer {

	private MatrixStack cachedStack = null;

//	@ModifyVariable(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/DiffuseLighting;enableGuiDepthLighting()V"), method = "render", ordinal = 0)
//	private MatrixStack cacheScreenRenderStack(MatrixStack stack) {
//		this.cachedStack = stack;
//		return stack;
//	}

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;render(Lnet/minecraft/client/util/math/MatrixStack;IIF)V"), method = "render", cancellable = true)
	private void beforeRenderScreen(float tickDelta, long startTime, boolean tick, CallbackInfo info) {

		//TODO improve this (cache original stack instead of new instance)
		this.cachedStack = new MatrixStack();

		MixinCache.cachedCurrentScreen = MinecraftClient.getInstance().currentScreen;

		//InitCompleted ---------------
		if (MixinCache.triggerInitCompleted) {
			GuiInitCompletedEvent e2 = new GuiInitCompletedEvent(MinecraftClient.getInstance().currentScreen);
			Konkrete.getEventHandler().callEventsFor(e2);
			MixinCache.triggerInitCompleted = false;
		}
		//-----------------------------

		//DrawScreen pre --------------
		int mX = (int)(MinecraftClient.getInstance().mouse.getX() * (double)MinecraftClient.getInstance().getWindow().getScaledWidth() / (double)MinecraftClient.getInstance().getWindow().getWidth());
		int mY = (int)(MinecraftClient.getInstance().mouse.getY() * (double)MinecraftClient.getInstance().getWindow().getScaledHeight() / (double)MinecraftClient.getInstance().getWindow().getHeight());
		DrawScreenEvent.Pre e = new DrawScreenEvent.Pre(MinecraftClient.getInstance().currentScreen, this.cachedStack, mX, mY, MinecraftClient.getInstance().getLastFrameDuration());
		Konkrete.getEventHandler().callEventsFor(e);
		if (e.isCanceled()) {
			info.cancel();
			//DrawScreen post if pre canceled
			DrawScreenEvent.Post e2 = new DrawScreenEvent.Post(MinecraftClient.getInstance().currentScreen, this.cachedStack, mX, mY, MinecraftClient.getInstance().getLastFrameDuration());
			Konkrete.getEventHandler().callEventsFor(e2);
		}
		//-----------------------------

	}

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;render(Lnet/minecraft/client/util/math/MatrixStack;IIF)V", shift = At.Shift.AFTER), method = "render")
	private void afterRenderScreen(float tickDelta, long startTime, boolean tick, CallbackInfo info) {

		if (this.cachedStack == null) {
			Konkrete.LOGGER.error("[KONKRETE] ERROR: MixinGameRenderer: afterRenderScreen: Cached screen render stack was NULL!");
			this.cachedStack = new MatrixStack();
		}

		//DrawScreen post -------------
		int mX2 = (int)(MinecraftClient.getInstance().mouse.getX() * (double)MinecraftClient.getInstance().getWindow().getScaledWidth() / (double)MinecraftClient.getInstance().getWindow().getWidth());
		int mY2 = (int)(MinecraftClient.getInstance().mouse.getY() * (double)MinecraftClient.getInstance().getWindow().getScaledHeight() / (double)MinecraftClient.getInstance().getWindow().getHeight());
		DrawScreenEvent.Post e2 = new DrawScreenEvent.Post(MinecraftClient.getInstance().currentScreen, this.cachedStack, mX2, mY2, MinecraftClient.getInstance().getLastFrameDuration());
		Konkrete.getEventHandler().callEventsFor(e2);
		//-----------------------------

	}



//	@Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;render(Lnet/minecraft/client/util/math/MatrixStack;IIF)V"), method = "render")
//	private void onRenderCurrentScreen(Screen instance, MatrixStack matrices, int mouseX, int mouseY, float delta) {
//
//		MixinCache.cachedCurrentScreen = instance;
//
//		ModCompatibilityHelper.lastScreenRenderStack = matrices;
//
//		//InitCompleted ---------------
//		if (MixinCache.triggerInitCompleted) {
//			GuiInitCompletedEvent e2 = new GuiInitCompletedEvent(MinecraftClient.getInstance().currentScreen);
//			Konkrete.getEventHandler().callEventsFor(e2);
//			MixinCache.triggerInitCompleted = false;
//		}
//		//-----------------------------
//
//		//DrawScreen pre --------------
//		int mX = (int)(MinecraftClient.getInstance().mouse.getX() * (double)MinecraftClient.getInstance().getWindow().getScaledWidth() / (double)MinecraftClient.getInstance().getWindow().getWidth());
//		int mY = (int)(MinecraftClient.getInstance().mouse.getY() * (double)MinecraftClient.getInstance().getWindow().getScaledHeight() / (double)MinecraftClient.getInstance().getWindow().getHeight());
//		DrawScreenEvent.Pre e = new DrawScreenEvent.Pre(MinecraftClient.getInstance().currentScreen, matrices, mX, mY, MinecraftClient.getInstance().getLastFrameDuration());
//		Konkrete.getEventHandler().callEventsFor(e);
//		if (!e.isCanceled()) {
//			instance.render(matrices, mouseX, mouseY, delta);
//		}
//		for (Runnable r : ModCompatibilityHelper.preScreenRenderTasks) {
//			r.run();
//		}
//		ModCompatibilityHelper.preScreenRenderTasks.clear();
//		//-----------------------------
//
//		//DrawScreen post -------------
//		int mX2 = (int)(MinecraftClient.getInstance().mouse.getX() * (double)MinecraftClient.getInstance().getWindow().getScaledWidth() / (double)MinecraftClient.getInstance().getWindow().getWidth());
//		int mY2 = (int)(MinecraftClient.getInstance().mouse.getY() * (double)MinecraftClient.getInstance().getWindow().getScaledHeight() / (double)MinecraftClient.getInstance().getWindow().getHeight());
//		DrawScreenEvent.Post e2 = new DrawScreenEvent.Post(MinecraftClient.getInstance().currentScreen, matrices, mX2, mY2, MinecraftClient.getInstance().getLastFrameDuration());
//		Konkrete.getEventHandler().callEventsFor(e2);
//		for (Runnable r : ModCompatibilityHelper.postScreenRenderTasks) {
//			r.run();
//		}
//		ModCompatibilityHelper.postScreenRenderTasks.clear();
//		//-----------------------------
//
//	}

}