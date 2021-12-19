package de.keksuccino.konkrete.events.client.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import de.keksuccino.konkrete.Konkrete;
import de.keksuccino.konkrete.events.client.GuiScreenEvent.MouseScrollEvent;
import de.keksuccino.konkrete.events.client.mixinbase.MixinCache;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;

@Mixin(Mouse.class)
public class MixinMouse {

	private static boolean scrollPre = false;

	@Inject(at = @At(value = "HEAD"), method = "onMouseButton")
	private void onCacheMouseButton(long window, int button, int action, int mods, CallbackInfo info) {
		MixinCache.currentMouseButton = button;
	}
	
	//GuiScreenEvent.MouseScrollEvent.Pre
	@SuppressWarnings("resource")
	@Inject(at = @At(value = "HEAD"), method = "onMouseScroll", cancellable = true)
	private void onGuiMouseScrollPre(long window, double horizontal, double vertical, CallbackInfo info) {
		if (window == MinecraftClient.getInstance().getWindow().getHandle()) {
			double d = (MinecraftClient.getInstance().options.discreteMouseScroll ? Math.signum(vertical) : vertical) * MinecraftClient.getInstance().options.mouseWheelSensitivity;
			if (MinecraftClient.getInstance().getOverlay() == null) {
				if (MinecraftClient.getInstance().currentScreen != null) {
					double x = MinecraftClient.getInstance().mouse.getX();
					double y = MinecraftClient.getInstance().mouse.getY();
					int mX = (int)(x * (double)MinecraftClient.getInstance().getWindow().getScaledWidth() / (double)MinecraftClient.getInstance().getWindow().getWidth());
			        int mY = (int)(y * (double)MinecraftClient.getInstance().getWindow().getScaledHeight() / (double)MinecraftClient.getInstance().getWindow().getHeight());
					MouseScrollEvent.Pre e = new MouseScrollEvent.Pre(MinecraftClient.getInstance().currentScreen, mX, mY, d);
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
	@Inject(at = @At(value = "TAIL"), method = "onMouseScroll", cancellable = false)
	private void onGuiMouseScrollPost(long window, double horizontal, double vertical, CallbackInfo info) {
		if (scrollPre) {
			double d = (MinecraftClient.getInstance().options.discreteMouseScroll ? Math.signum(vertical) : vertical) * MinecraftClient.getInstance().options.mouseWheelSensitivity;
			double x = MinecraftClient.getInstance().mouse.getX();
			double y = MinecraftClient.getInstance().mouse.getY();
			int mX = (int)(x * (double)MinecraftClient.getInstance().getWindow().getScaledWidth() / (double)MinecraftClient.getInstance().getWindow().getWidth());
	        int mY = (int)(y * (double)MinecraftClient.getInstance().getWindow().getScaledHeight() / (double)MinecraftClient.getInstance().getWindow().getHeight());
			MouseScrollEvent.Post e = new MouseScrollEvent.Post(MinecraftClient.getInstance().currentScreen, mX, mY, d);
			Konkrete.getEventHandler().callEventsFor(e);
		}
		scrollPre = false;
	}

}
