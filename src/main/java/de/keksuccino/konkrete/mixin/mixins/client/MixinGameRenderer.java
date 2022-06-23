package de.keksuccino.konkrete.mixin.mixins.client;

import de.keksuccino.konkrete.events.client.GuiInitCompletedEvent;
import de.keksuccino.konkrete.mixin.MixinCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.konkrete.Konkrete;
import de.keksuccino.konkrete.events.client.GuiScreenEvent.DrawScreenEvent;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GameRenderer.class)
public class MixinGameRenderer {

	private PoseStack cachedStack = null;

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;render(Lcom/mojang/blaze3d/vertex/PoseStack;IIF)V"), method = "render", cancellable = true)
	private void beforeRenderScreen(float tickDelta, long startTime, boolean tick, CallbackInfo info) {

		//TODO improve this (cache original stack instead of new instance)
		this.cachedStack = new PoseStack();

		MixinCache.cachedCurrentScreen = Minecraft.getInstance().screen;

		//InitCompleted ---------------
		if (MixinCache.triggerInitCompleted) {
			GuiInitCompletedEvent e2 = new GuiInitCompletedEvent(Minecraft.getInstance().screen);
			Konkrete.getEventHandler().callEventsFor(e2);
			MixinCache.triggerInitCompleted = false;
		}
		//-----------------------------

		//DrawScreen pre --------------
		//TODO übernehmen 1.3.3-1
		if (Minecraft.getInstance().screen != null) {
			int mX = (int)(Minecraft.getInstance().mouseHandler.xpos() * (double)Minecraft.getInstance().getWindow().getGuiScaledWidth() / (double)Minecraft.getInstance().getWindow().getScreenWidth());
			int mY = (int)(Minecraft.getInstance().mouseHandler.ypos() * (double)Minecraft.getInstance().getWindow().getGuiScaledHeight() / (double)Minecraft.getInstance().getWindow().getScreenHeight());
			DrawScreenEvent.Pre e = new DrawScreenEvent.Pre(Minecraft.getInstance().screen, this.cachedStack, mX, mY, Minecraft.getInstance().getDeltaFrameTime());
			Konkrete.getEventHandler().callEventsFor(e);
			if (e.isCanceled()) {
				info.cancel();
				//DrawScreen post if pre canceled
				DrawScreenEvent.Post e2 = new DrawScreenEvent.Post(Minecraft.getInstance().screen, this.cachedStack, mX, mY, Minecraft.getInstance().getDeltaFrameTime());
				Konkrete.getEventHandler().callEventsFor(e2);
			}
		}
		//-----------------------------

	}

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;render(Lcom/mojang/blaze3d/vertex/PoseStack;IIF)V", shift = At.Shift.AFTER), method = "render")
	private void afterRenderScreen(float tickDelta, long startTime, boolean tick, CallbackInfo info) {

		if (this.cachedStack == null) {
			Konkrete.LOGGER.error("[KONKRETE] ERROR: MixinGameRenderer: afterRenderScreen: Cached screen render stack was NULL!");
			this.cachedStack = new PoseStack();
		}

		//DrawScreen post -------------
		//TODO übernehmen 1.3.3-1
		if (Minecraft.getInstance().screen != null) {
			int mX2 = (int) (Minecraft.getInstance().mouseHandler.xpos() * (double) Minecraft.getInstance().getWindow().getGuiScaledWidth() / (double) Minecraft.getInstance().getWindow().getScreenWidth());
			int mY2 = (int) (Minecraft.getInstance().mouseHandler.ypos() * (double) Minecraft.getInstance().getWindow().getGuiScaledHeight() / (double) Minecraft.getInstance().getWindow().getScreenHeight());
			DrawScreenEvent.Post e2 = new DrawScreenEvent.Post(Minecraft.getInstance().screen, this.cachedStack, mX2, mY2, Minecraft.getInstance().getDeltaFrameTime());
			Konkrete.getEventHandler().callEventsFor(e2);
		}
		//-----------------------------

	}

}