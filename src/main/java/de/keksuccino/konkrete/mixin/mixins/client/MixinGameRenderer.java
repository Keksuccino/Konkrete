package de.keksuccino.konkrete.mixin.mixins.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.konkrete.Konkrete;
import de.keksuccino.konkrete.events.client.GuiInitCompletedEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent.DrawScreenEvent;
import de.keksuccino.konkrete.mixin.MixinCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;

@Mixin(value = GameRenderer.class)
public class MixinGameRenderer {
	
	private PoseStack matrix = new PoseStack();

	//Storing MatrixStack for current screen rendering
	@ModifyVariable(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;render(Lcom/mojang/blaze3d/vertex/PoseStack;IIF)V"), ordinal = 0, method = "render")
	public PoseStack onMatrixStackCreation(PoseStack matrix) {
		if (!Konkrete.isOptifineLoaded) {
			this.matrix = matrix;
		}
		return matrix;
	}

	//DrawScreenEvent.Pre & (if pre canceled) DrawScreenEvent.Post & GuiInitCompletedEvent
	@SuppressWarnings("resource")
	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;render(Lcom/mojang/blaze3d/vertex/PoseStack;IIF)V"), method = "render", cancellable = true)
	public void onRenderPre(CallbackInfo i) {
		if (MixinCache.triggerInitCompleted) {
			GuiInitCompletedEvent e2 = new GuiInitCompletedEvent(Minecraft.getInstance().screen);
			Konkrete.getEventHandler().callEventsFor(e2);
			MixinCache.triggerInitCompleted = false;
		}
		
		int mX = (int)(Minecraft.getInstance().mouseHandler.xpos() * (double)Minecraft.getInstance().getWindow().getGuiScaledWidth() / (double)Minecraft.getInstance().getWindow().getScreenWidth());
        int mY = (int)(Minecraft.getInstance().mouseHandler.ypos() * (double)Minecraft.getInstance().getWindow().getGuiScaledHeight() / (double)Minecraft.getInstance().getWindow().getScreenHeight());
		DrawScreenEvent.Pre e = new DrawScreenEvent.Pre(Minecraft.getInstance().screen, this.matrix, mX, mY, Minecraft.getInstance().getDeltaFrameTime());
		Konkrete.getEventHandler().callEventsFor(e);
		if (e.isCanceled()) {
			i.cancel();
			DrawScreenEvent.Post e2 = new DrawScreenEvent.Post(Minecraft.getInstance().screen, this.matrix, mX, mY, Minecraft.getInstance().getDeltaFrameTime());
			Konkrete.getEventHandler().callEventsFor(e2);
		}
	}
	
	//DrawScreenEvent.Post (if pre NOT canceled)
	@SuppressWarnings("resource")
	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;render(Lcom/mojang/blaze3d/vertex/PoseStack;IIF)V", shift = Shift.AFTER), method = "render", cancellable = true)
	public void onRenderPost(CallbackInfo i) {
		int mX = (int)(Minecraft.getInstance().mouseHandler.xpos() * (double)Minecraft.getInstance().getWindow().getGuiScaledWidth() / (double)Minecraft.getInstance().getWindow().getScreenWidth());
        int mY = (int)(Minecraft.getInstance().mouseHandler.ypos() * (double)Minecraft.getInstance().getWindow().getGuiScaledHeight() / (double)Minecraft.getInstance().getWindow().getScreenHeight());
        DrawScreenEvent.Post e2 = new DrawScreenEvent.Post(Minecraft.getInstance().screen, this.matrix, mX, mY, Minecraft.getInstance().getDeltaFrameTime());
		Konkrete.getEventHandler().callEventsFor(e2);

		if (Konkrete.isOptifineLoaded) {
			this.matrix = new PoseStack();
		}
	}

}