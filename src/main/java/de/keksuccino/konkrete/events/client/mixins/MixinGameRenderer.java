package de.keksuccino.konkrete.events.client.mixins;

import de.keksuccino.konkrete.events.client.GuiInitCompletedEvent;
import de.keksuccino.konkrete.events.client.mixinbase.MixinCache;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import de.keksuccino.konkrete.Konkrete;
import de.keksuccino.konkrete.events.client.GuiScreenEvent.DrawScreenEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;

@Mixin(value = GameRenderer.class)
public class MixinGameRenderer {

	@Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;render(Lnet/minecraft/client/util/math/MatrixStack;IIF)V"), method = "render")
	private void onRenderCurrentScreen(Screen instance, MatrixStack matrices, int mouseX, int mouseY, float delta) {

		MixinCache.cachedCurrentScreen = instance;

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
		DrawScreenEvent.Pre e = new DrawScreenEvent.Pre(MinecraftClient.getInstance().currentScreen, matrices, mX, mY, MinecraftClient.getInstance().getLastFrameDuration());
		Konkrete.getEventHandler().callEventsFor(e);
		if (!e.isCanceled()) {
			instance.render(matrices, mouseX, mouseY, delta);
		}
		//-----------------------------

		//DrawScreen post -------------
		int mX2 = (int)(MinecraftClient.getInstance().mouse.getX() * (double)MinecraftClient.getInstance().getWindow().getScaledWidth() / (double)MinecraftClient.getInstance().getWindow().getWidth());
		int mY2 = (int)(MinecraftClient.getInstance().mouse.getY() * (double)MinecraftClient.getInstance().getWindow().getScaledHeight() / (double)MinecraftClient.getInstance().getWindow().getHeight());
		DrawScreenEvent.Post e2 = new DrawScreenEvent.Post(MinecraftClient.getInstance().currentScreen, matrices, mX2, mY2, MinecraftClient.getInstance().getLastFrameDuration());
		Konkrete.getEventHandler().callEventsFor(e2);
		//TODO eventuell OF fix re-implementieren?
//		if (Konkrete.isOptifineLoaded) {
//			this.matrix = new MatrixStack();
//		}
		//-----------------------------

	}

}