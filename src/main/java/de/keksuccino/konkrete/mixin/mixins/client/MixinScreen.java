package de.keksuccino.konkrete.mixin.mixins.client;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.gui.GuiGraphics;
import de.keksuccino.konkrete.Konkrete;
import de.keksuccino.konkrete.events.client.GuiScreenEvent.BackgroundDrawnEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent.KeyboardCharTypedEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent.KeyboardKeyPressedEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent.KeyboardKeyReleasedEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent.MouseClickedEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent.MouseReleasedEvent;
import de.keksuccino.konkrete.mixin.MixinCache;

@Mixin(value = Screen.class)
public abstract class MixinScreen {
	
	@Final @Shadow private List<GuiEventListener> children;
	@Final @Shadow private List<Renderable> renderables;
	@Final @Shadow private List<NarratableEntry> narratables;

	protected void addButton(AbstractButton button) {
		this.renderables.add(button);
		this.children.add(button);
		this.narratables.add(button);
	}
	
	@Shadow protected abstract void init();

	@Inject(method = "renderWithTooltip", at = @At("RETURN"))
	private void afterRenderWithTooltipKonkrete(GuiGraphics graphics, int mouseX, int mouseY, float partial, CallbackInfo info) {
		//Set cache for MixinGameRenderer
		MixinCache.cachedRenderScreenGuiGraphics = graphics;
		MixinCache.cachedRenderScreenMouseX = mouseX;
		MixinCache.cachedRenderScreenMouseY = mouseY;
		MixinCache.cachedRenderScreenPartial = partial;
	}

	@Inject(method = "renderBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;renderTransparentBackground(Lnet/minecraft/client/gui/GuiGraphics;)V", shift = At.Shift.AFTER))
	private void onBackgroundRenderedInWorld(GuiGraphics graphics, int i, int j, float f, CallbackInfo ci) {
		BackgroundDrawnEvent e = new BackgroundDrawnEvent((Screen)((Object)this), graphics);
		Konkrete.getEventHandler().callEventsFor(e);
	}

	@Inject(method = "renderDirtBackground", at = @At(value = "TAIL"))
	private void onBackgroundTextureDrawn(GuiGraphics graphics, CallbackInfo ci) {
		BackgroundDrawnEvent e = new BackgroundDrawnEvent((Screen)((Object)this), graphics);
		Konkrete.getEventHandler().callEventsFor(e);
	}

	//MouseClickedEvent.Pre & MouseReleasedEvent.Pre
	@Inject(at = @At(value = "HEAD"), method = "wrapScreenError", cancellable = true)
	private static void onMouseClickedReleasedPre(Runnable task, String errorTitle, String screenName, CallbackInfo info) {
		int mX = (int)(Minecraft.getInstance().mouseHandler.xpos() * (double)Minecraft.getInstance().getWindow().getGuiScaledWidth() / (double)Minecraft.getInstance().getWindow().getScreenWidth());
        int mY = (int)(Minecraft.getInstance().mouseHandler.ypos() * (double)Minecraft.getInstance().getWindow().getGuiScaledHeight() / (double)Minecraft.getInstance().getWindow().getScreenHeight());
		if ((errorTitle != null) && errorTitle.equals("mouseClicked event handler")) {
			MouseClickedEvent.Pre e = new MouseClickedEvent.Pre(Minecraft.getInstance().screen, mX, mY, MixinCache.currentMouseButton);
			Konkrete.getEventHandler().callEventsFor(e);
			if (e.isCanceled()) {
				info.cancel();
			}
		}
		if ((errorTitle != null) && errorTitle.equals("mouseReleased event handler")) {
			MouseReleasedEvent.Pre e = new MouseReleasedEvent.Pre(Minecraft.getInstance().screen, mX, mY, MixinCache.currentMouseButton);
			Konkrete.getEventHandler().callEventsFor(e);
			if (e.isCanceled()) {
				info.cancel();
			}
		}
	}

	//MouseClickedEvent.Post & MouseReleasedEvent.Post
	@Inject(at = @At(value = "TAIL"), method = "wrapScreenError")
	private static void onMouseClickedReleasedPost(Runnable task, String errorTitle, String screenName, CallbackInfo info) {
		int mX = (int)(Minecraft.getInstance().mouseHandler.xpos() * (double)Minecraft.getInstance().getWindow().getGuiScaledWidth() / (double)Minecraft.getInstance().getWindow().getScreenWidth());
        int mY = (int)(Minecraft.getInstance().mouseHandler.ypos() * (double)Minecraft.getInstance().getWindow().getGuiScaledHeight() / (double)Minecraft.getInstance().getWindow().getScreenHeight());
		if ((errorTitle != null) && errorTitle.equals("mouseClicked event handler")) {
			MouseClickedEvent.Post e = new MouseClickedEvent.Post(Minecraft.getInstance().screen, mX, mY, MixinCache.currentMouseButton);
			Konkrete.getEventHandler().callEventsFor(e);
		}
		if ((errorTitle != null) && errorTitle.equals("mouseReleased event handler")) {
			MouseReleasedEvent.Post e = new MouseReleasedEvent.Post(Minecraft.getInstance().screen, mX, mY, MixinCache.currentMouseButton);
			Konkrete.getEventHandler().callEventsFor(e);
		}
	}
	
	//KeyboardKeyPressedEvent.Pre & KeyboardKeyReleasedEvent.Pre
	@Inject(at = @At(value = "HEAD"), method = "wrapScreenError", cancellable = true)
	private static void onKeyboardPressedReleasedPre(Runnable task, String errorTitle, String screenName, CallbackInfo info) {
		if ((errorTitle != null) && errorTitle.equals("keyPressed event handler")) {
			int i = MixinCache.currentKeyboardAction;
			if (i != 1 && (i != 2)) {
				if (i == 0) {
					KeyboardKeyReleasedEvent.Pre e = new KeyboardKeyReleasedEvent.Pre(Minecraft.getInstance().screen, MixinCache.currentKeyboardKey, MixinCache.currentKeyboardScancode, MixinCache.currentKeyboardModifiers);
					Konkrete.getEventHandler().callEventsFor(e);
					if (e.isCanceled()) {
						info.cancel();
					}
				}
			} else {
				KeyboardKeyPressedEvent.Pre e = new KeyboardKeyPressedEvent.Pre(Minecraft.getInstance().screen, MixinCache.currentKeyboardKey, MixinCache.currentKeyboardScancode, MixinCache.currentKeyboardModifiers);
				Konkrete.getEventHandler().callEventsFor(e);
				if (e.isCanceled()) {
					info.cancel();
				}
			}
		}
	}
	

	//KeyboardKeyPressedEvent.Post & KeyboardKeyReleasedEvent.Post
	@Inject(at = @At(value = "TAIL"), method = "wrapScreenError")
	private static void onKeyboardPressedReleasedPost(Runnable task, String errorTitle, String screenName, CallbackInfo info) {
		if ((errorTitle != null) && errorTitle.equals("keyPressed event handler")) {
			int i = MixinCache.currentKeyboardAction;
			if (i != 1 && (i != 2)) {
				if (i == 0) {
					KeyboardKeyReleasedEvent.Post e = new KeyboardKeyReleasedEvent.Post(Minecraft.getInstance().screen, MixinCache.currentKeyboardKey, MixinCache.currentKeyboardScancode, MixinCache.currentKeyboardModifiers);
					Konkrete.getEventHandler().callEventsFor(e);
				}
			} else {
				KeyboardKeyPressedEvent.Post e = new KeyboardKeyPressedEvent.Post(Minecraft.getInstance().screen, MixinCache.currentKeyboardKey, MixinCache.currentKeyboardScancode, MixinCache.currentKeyboardModifiers);
				Konkrete.getEventHandler().callEventsFor(e);
			}
		}
	}
	
	//KeyboardCharTypedEvent.Pre
	@Inject(at = @At(value = "HEAD"), method = "wrapScreenError", cancellable = true)
	private static void onKeyboardCharTypedPre(Runnable task, String errorTitle, String screenName, CallbackInfo info) {
		if ((errorTitle != null) && errorTitle.equals("charTyped event handler")) {
			KeyboardCharTypedEvent.Pre e = new KeyboardCharTypedEvent.Pre(Minecraft.getInstance().screen, (char)MixinCache.currentKeyboardChar, MixinCache.currentKeyboardCharModifiers);
			Konkrete.getEventHandler().callEventsFor(e);
			if (e.isCanceled()) {
				info.cancel();
			}
		}
	}

	//KeyboardCharTypedEvent.Post
	@Inject(at = @At(value = "TAIL"), method = "wrapScreenError")
	private static void onKeyboardCharTypedPost(Runnable task, String errorTitle, String screenName, CallbackInfo info) {
		if ((errorTitle != null) && errorTitle.equals("charTyped event handler")) {
			KeyboardCharTypedEvent.Post e = new KeyboardCharTypedEvent.Post(Minecraft.getInstance().screen, (char)MixinCache.currentKeyboardChar, MixinCache.currentKeyboardCharModifiers);
			Konkrete.getEventHandler().callEventsFor(e);
		}
	}
	
}
