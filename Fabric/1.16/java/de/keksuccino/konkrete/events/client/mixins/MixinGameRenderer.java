package de.keksuccino.konkrete.events.client.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import de.keksuccino.konkrete.Konkrete;
import de.keksuccino.konkrete.events.client.GuiScreenEvent.DrawScreenEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;

@Mixin(value = GameRenderer.class)
public class MixinGameRenderer {
	
	private MatrixStack matrix = new MatrixStack();

//	//DrawScreenEvent.Pre & DrawScreenEvent.Post
//	@Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;render(Lnet/minecraft/client/util/math/MatrixStack;IIF)V"), method = "render")
//	public void onRender(Screen s, MatrixStack matrix, int mouseX, int mouseY, float delta) {
//		int mX = (int)(MinecraftClient.getInstance().mouse.getX() * (double)MinecraftClient.getInstance().getWindow().getScaledWidth() / (double)MinecraftClient.getInstance().getWindow().getWidth());
//        int mY = (int)(MinecraftClient.getInstance().mouse.getY() * (double)MinecraftClient.getInstance().getWindow().getScaledHeight() / (double)MinecraftClient.getInstance().getWindow().getHeight());
//		DrawScreenEvent.Pre e = new DrawScreenEvent.Pre(s, matrix, mX, mY, MinecraftClient.getInstance().getLastFrameDuration());
//		Konkrete.getEventHandler().callEventsFor(e);
//		if (!e.isCanceled()) {
//			s.render(matrix, mouseX, mouseY, delta);
//		}
//		DrawScreenEvent.Post e2 = new DrawScreenEvent.Post(s, matrix, mX, mY, MinecraftClient.getInstance().getLastFrameDuration());
//		Konkrete.getEventHandler().callEventsFor(e2);
//	}
	
	//Storing MatrixStack for current screen rendering
	@ModifyVariable(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;render(Lnet/minecraft/client/util/math/MatrixStack;IIF)V", shift = Shift.BY, by = -2), method = "render")
	public MatrixStack onMatrixStackCreation(MatrixStack matrix) {
		this.matrix = matrix;
		return matrix;
	}

	//DrawScreenEvent.Pre & (if pre canceled) DrawScreenEvent.Post
	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;render(Lnet/minecraft/client/util/math/MatrixStack;IIF)V"), method = "render", cancellable = true)
	public void onRenderPre(CallbackInfo i) {
		int mX = (int)(MinecraftClient.getInstance().mouse.getX() * (double)MinecraftClient.getInstance().getWindow().getScaledWidth() / (double)MinecraftClient.getInstance().getWindow().getWidth());
        int mY = (int)(MinecraftClient.getInstance().mouse.getY() * (double)MinecraftClient.getInstance().getWindow().getScaledHeight() / (double)MinecraftClient.getInstance().getWindow().getHeight());
		DrawScreenEvent.Pre e = new DrawScreenEvent.Pre(MinecraftClient.getInstance().currentScreen, this.matrix, mX, mY, MinecraftClient.getInstance().getLastFrameDuration());
		Konkrete.getEventHandler().callEventsFor(e);
		if (e.isCanceled()) {
			i.cancel();
			DrawScreenEvent.Post e2 = new DrawScreenEvent.Post(MinecraftClient.getInstance().currentScreen, this.matrix, mX, mY, MinecraftClient.getInstance().getLastFrameDuration());
			Konkrete.getEventHandler().callEventsFor(e2);
		}
	}
	
	//DrawScreenEvent.Post (if pre NOT canceled)
	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;render(Lnet/minecraft/client/util/math/MatrixStack;IIF)V", shift = Shift.AFTER), method = "render", cancellable = true)
	public void onRenderPost(CallbackInfo i) {
		int mX = (int)(MinecraftClient.getInstance().mouse.getX() * (double)MinecraftClient.getInstance().getWindow().getScaledWidth() / (double)MinecraftClient.getInstance().getWindow().getWidth());
        int mY = (int)(MinecraftClient.getInstance().mouse.getY() * (double)MinecraftClient.getInstance().getWindow().getScaledHeight() / (double)MinecraftClient.getInstance().getWindow().getHeight());
        DrawScreenEvent.Post e2 = new DrawScreenEvent.Post(MinecraftClient.getInstance().currentScreen, this.matrix, mX, mY, MinecraftClient.getInstance().getLastFrameDuration());
		Konkrete.getEventHandler().callEventsFor(e2);
	}

}