package de.keksuccino.konkrete.mixin.mixins.client;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.konkrete.Konkrete;
import de.keksuccino.konkrete.events.client.GuiScreenEvent.BackgroundDrawnEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent.InitGuiEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent.KeyboardCharTypedEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent.KeyboardKeyPressedEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent.KeyboardKeyReleasedEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent.MouseClickedEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent.MouseReleasedEvent;
import de.keksuccino.konkrete.mixin.MixinCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;

@Mixin(Screen.class)
public abstract class MixinScreen {

	@Shadow protected List<GuiEventListener> children;
	@Shadow protected List<AbstractWidget> buttons;

	@Shadow protected abstract <T extends AbstractWidget> T addButton(T button);
	
	@Shadow protected abstract void init();

	//BackgroundDrawnEvent
	@Inject(at = @At(value = "TAIL"), method = "renderBackground(Lcom/mojang/blaze3d/vertex/PoseStack;I)V")
	private void onBackgroundDrawn(PoseStack matrix, int vOffset, CallbackInfo info) {
		if (Minecraft.getInstance().level != null) {
			BackgroundDrawnEvent e = new BackgroundDrawnEvent((Screen)((Object)this), matrix);
			Konkrete.getEventHandler().callEventsFor(e);
		}
	}
	
	//BackgroundDrawnEvent
	@Inject(at = @At(value = "TAIL"), method = "renderDirtBackground")
	private void onBackgroundTextureDrawn(int vOffset, CallbackInfo info) {
		BackgroundDrawnEvent e = new BackgroundDrawnEvent((Screen)((Object)this), new PoseStack());
		Konkrete.getEventHandler().callEventsFor(e);
	}
	
	//InitGuiEvent.Pre
	@Inject(at = @At(value = "INVOKE", target = "Ljava/util/List;clear()V", ordinal = 0), method = "init(Lnet/minecraft/client/Minecraft;II)V", cancellable = true)
	public void onInitPre(CallbackInfo info) {
		Consumer<AbstractWidget> remove = (t) -> {
			buttons.remove(t);
			children.remove(t);
		};

		InitGuiEvent.Pre e = new InitGuiEvent.Pre((Screen)((Object)this), this.buttons, this::addButton, remove);
		Konkrete.getEventHandler().callEventsFor(e);
		if (e.isCanceled()) {
			InitGuiEvent.Post e2 = new InitGuiEvent.Post((Screen)((Object)this), this.buttons, this::addButton, remove);
			Konkrete.getEventHandler().callEventsFor(e2);
			info.cancel();
		}
		
	}

	//InitGuiEvent.Post
	@Inject(at = @At(value = "TAIL"), method = "init(Lnet/minecraft/client/Minecraft;II)V")
	public void onInitPost(CallbackInfo info) {
		Consumer<AbstractWidget> remove = (t) -> {
			buttons.remove(t);
			children.remove(t);
		};

		InitGuiEvent.Post e = new InitGuiEvent.Post((Screen)((Object)this), this.buttons, this::addButton, remove);
		Konkrete.getEventHandler().callEventsFor(e);
		MixinCache.triggerInitCompleted = true;
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
			if (i != 1 && (i != 2 || !getRepeatEvents())) {
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
			if (i != 1 && (i != 2 || !getRepeatEvents())) {
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
	
	private static boolean getRepeatEvents() {
		return ((IMixinKeyboardHandler)Minecraft.getInstance().keyboardHandler).getSendRepeatsToGuiKonkrete();
	}
	
}
