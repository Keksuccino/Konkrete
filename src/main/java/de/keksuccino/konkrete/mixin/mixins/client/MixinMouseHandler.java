package de.keksuccino.konkrete.mixin.mixins.client;

import de.keksuccino.konkrete.events.client.ScreenMouseClickedEvent;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import de.keksuccino.konkrete.Konkrete;
import de.keksuccino.konkrete.events.client.GuiScreenEvent.MouseScrollEvent;
import de.keksuccino.konkrete.mixin.MixinCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(MouseHandler.class)
public class MixinMouseHandler {

	private static boolean scrollPre = false;

	@Inject(at = @At(value = "HEAD"), method = "onPress")
	private void onCacheMouseButton(long window, int button, int action, int mods, CallbackInfo info) {
		MixinCache.currentMouseButton = button;
	}
	
	//GuiScreenEvent.MouseScrollEvent.Pre
	@SuppressWarnings("resource")
	@Inject(at = @At(value = "HEAD"), method = "onScroll", cancellable = true)
	private void onGuiMouseScrollPre(long window, double horizontal, double vertical, CallbackInfo info) {
		if (window == Minecraft.getInstance().getWindow().getWindow()) {
			double d = (Minecraft.getInstance().options.discreteMouseScroll ? Math.signum(vertical) : vertical) * Minecraft.getInstance().options.mouseWheelSensitivity;
			if (Minecraft.getInstance().getOverlay() == null) {
				if (Minecraft.getInstance().screen != null) {
					double x = Minecraft.getInstance().mouseHandler.xpos();
					double y = Minecraft.getInstance().mouseHandler.ypos();
					int mX = (int)(x * (double)Minecraft.getInstance().getWindow().getGuiScaledWidth() / (double)Minecraft.getInstance().getWindow().getScreenWidth());
			        int mY = (int)(y * (double)Minecraft.getInstance().getWindow().getGuiScaledHeight() / (double)Minecraft.getInstance().getWindow().getScreenHeight());
					MouseScrollEvent.Pre e = new MouseScrollEvent.Pre(Minecraft.getInstance().screen, mX, mY, d);
					Konkrete.getEventHandler().callEventsFor(e);
					scrollPre = true;
					if (e.isCanceled()) {
						info.cancel();
					}
				}
			}
		}
	}

	//GuiScreenEvent.MouseScrollEvent.Post
	@SuppressWarnings("resource")
	@Inject(at = @At(value = "TAIL"), method = "onScroll")
	private void onGuiMouseScrollPost(long window, double horizontal, double vertical, CallbackInfo info) {
		if (scrollPre) {
			double d = (Minecraft.getInstance().options.discreteMouseScroll ? Math.signum(vertical) : vertical) * Minecraft.getInstance().options.mouseWheelSensitivity;
			double x = Minecraft.getInstance().mouseHandler.xpos();
			double y = Minecraft.getInstance().mouseHandler.ypos();
			int mX = (int)(x * (double)Minecraft.getInstance().getWindow().getGuiScaledWidth() / (double)Minecraft.getInstance().getWindow().getScreenWidth());
	        int mY = (int)(y * (double)Minecraft.getInstance().getWindow().getGuiScaledHeight() / (double)Minecraft.getInstance().getWindow().getScreenHeight());
			MouseScrollEvent.Post e = new MouseScrollEvent.Post(Minecraft.getInstance().screen, mX, mY, d);
			Konkrete.getEventHandler().callEventsFor(e);
		}
		scrollPre = false;
	}

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;afterMouseAction()V", shift = At.Shift.AFTER), method = "onPress", locals = LocalCapture.CAPTURE_FAILEXCEPTION)
	private void onOnPress(long window, int i1, int i2, int i3, CallbackInfo info, boolean bl, int m, boolean[] bls, double d, double e, Screen screen) {
		Screen.wrapScreenError(() -> {
			Konkrete.getEventHandler().callEventsFor(new ScreenMouseClickedEvent(d, e, m));
		}, "Konkrete mouseClicked event handler", this.getClass().getCanonicalName());
	}

}
