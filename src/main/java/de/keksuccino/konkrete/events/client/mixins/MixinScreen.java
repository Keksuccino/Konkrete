package de.keksuccino.konkrete.events.client.mixins;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import de.keksuccino.konkrete.events.client.GuiInitCompletedEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import de.keksuccino.konkrete.Konkrete;
import de.keksuccino.konkrete.events.client.GuiScreenEvent.BackgroundDrawnEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent.InitGuiEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent.KeyboardCharTypedEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent.KeyboardKeyPressedEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent.KeyboardKeyReleasedEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent.MouseClickedEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent.MouseReleasedEvent;
import de.keksuccino.konkrete.events.client.mixinbase.MixinCache;
import de.keksuccino.konkrete.reflection.ReflectionHelper;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.util.math.MatrixStack;

@SuppressWarnings("resource")
@Mixin(value = Screen.class)
public abstract class MixinScreen {
	
	@Shadow protected List<Element> children;
	@Shadow protected List<Drawable> drawables;
	@Shadow protected List<Selectable> selectables;
	
//	@Shadow protected abstract <T extends Element & Drawable & Selectable> T addDrawableChild(T drawableElement);
	//Like that until I figured out how to make this thing work as shadow method
	protected void addButton(PressableWidget button) {
		try {
			Method m = ReflectionHelper.findMethod(Screen.class, "addDrawableChild", "method_37063", Element.class);
			m.invoke(((Screen)((Object)this)), button);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Shadow protected abstract void init();

	//BackgroundDrawnEvent
	@Inject(at = @At(value = "TAIL"), method = "renderBackground(Lnet/minecraft/client/util/math/MatrixStack;I)V")
	private void onBackgroundDrawn(MatrixStack matrix, int vOffset, CallbackInfo info) {
		if (MinecraftClient.getInstance().world != null) {
			BackgroundDrawnEvent e = new BackgroundDrawnEvent((Screen)((Object)this), matrix);
			Konkrete.getEventHandler().callEventsFor(e);
		}
	}
	
	//BackgroundDrawnEvent
	@Inject(at = @At(value = "TAIL"), method = "renderBackgroundTexture")
	private void onBackgroundTextureDrawn(int vOffset, CallbackInfo info) {
		BackgroundDrawnEvent e = new BackgroundDrawnEvent((Screen)((Object)this), new MatrixStack());
		Konkrete.getEventHandler().callEventsFor(e);
	}

	@Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;init()V"), method = "init(Lnet/minecraft/client/MinecraftClient;II)V")
	private void onInit(Screen instance) {

		//Init pre --------------------
		Consumer<PressableWidget> remove = new Consumer<PressableWidget>() {
			@Override
			public void accept(PressableWidget t) {
				drawables.remove(t);
				selectables.remove(t);
				children.remove(t);
			}
		};
		List<PressableWidget> buttons = new ArrayList<PressableWidget>();
		for (Drawable d : this.drawables) {
			if (d instanceof PressableWidget) {
				buttons.add((PressableWidget) d);
			}
		}
		InitGuiEvent.Pre e = new InitGuiEvent.Pre((Screen)((Object)this), buttons, this::addButton, remove);
		Konkrete.getEventHandler().callEventsFor(e);
		if (e.isCanceled()) {
			InitGuiEvent.Post e2 = new InitGuiEvent.Post((Screen)((Object)this), buttons, this::addButton, remove);
			Konkrete.getEventHandler().callEventsFor(e2);
			return;
		}
		//------------------------------

		this.init();

		//Init post --------------------
		List<PressableWidget> buttons2 = new ArrayList<PressableWidget>();
		for (Drawable d : this.drawables) {
			if (d instanceof PressableWidget) {
				buttons2.add((PressableWidget) d);
			}
		}
		InitGuiEvent.Post e2 = new InitGuiEvent.Post((Screen)((Object)this), buttons2, this::addButton, remove);
		Konkrete.getEventHandler().callEventsFor(e2);
		MixinCache.triggerInitCompleted = true;
		//-----------------------------

//		//InitCompleted ---------------
//		GuiInitCompletedEvent e3 = new GuiInitCompletedEvent(MinecraftClient.getInstance().currentScreen);
//		Konkrete.getEventHandler().callEventsFor(e3);
//		//-----------------------------

	}

	//MouseClickedEvent.Pre & MouseReleasedEvent.Pre
	@Inject(at = @At(value = "HEAD"), method = "wrapScreenError", cancellable = true)
	private static void onMouseClickedReleasedPre(Runnable task, String errorTitle, String screenName, CallbackInfo info) {
		int mX = (int)(MinecraftClient.getInstance().mouse.getX() * (double)MinecraftClient.getInstance().getWindow().getScaledWidth() / (double)MinecraftClient.getInstance().getWindow().getWidth());
        int mY = (int)(MinecraftClient.getInstance().mouse.getY() * (double)MinecraftClient.getInstance().getWindow().getScaledHeight() / (double)MinecraftClient.getInstance().getWindow().getHeight());
		if ((errorTitle != null) && errorTitle.equals("mouseClicked event handler")) {
			MouseClickedEvent.Pre e = new MouseClickedEvent.Pre(MinecraftClient.getInstance().currentScreen, mX, mY, MixinCache.currentMouseButton);
			Konkrete.getEventHandler().callEventsFor(e);
			if (e.isCanceled()) {
				info.cancel();
			}
		}
		if ((errorTitle != null) && errorTitle.equals("mouseReleased event handler")) {
			MouseReleasedEvent.Pre e = new MouseReleasedEvent.Pre(MinecraftClient.getInstance().currentScreen, mX, mY, MixinCache.currentMouseButton);
			Konkrete.getEventHandler().callEventsFor(e);
			if (e.isCanceled()) {
				info.cancel();
			}
		}
	}

	//MouseClickedEvent.Post & MouseReleasedEvent.Post
	@Inject(at = @At(value = "TAIL"), method = "wrapScreenError", cancellable = false)
	private static void onMouseClickedReleasedPost(Runnable task, String errorTitle, String screenName, CallbackInfo info) {
		int mX = (int)(MinecraftClient.getInstance().mouse.getX() * (double)MinecraftClient.getInstance().getWindow().getScaledWidth() / (double)MinecraftClient.getInstance().getWindow().getWidth());
        int mY = (int)(MinecraftClient.getInstance().mouse.getY() * (double)MinecraftClient.getInstance().getWindow().getScaledHeight() / (double)MinecraftClient.getInstance().getWindow().getHeight());
		if ((errorTitle != null) && errorTitle.equals("mouseClicked event handler")) {
			MouseClickedEvent.Post e = new MouseClickedEvent.Post(MinecraftClient.getInstance().currentScreen, mX, mY, MixinCache.currentMouseButton);
			Konkrete.getEventHandler().callEventsFor(e);
		}
		if ((errorTitle != null) && errorTitle.equals("mouseReleased event handler")) {
			MouseReleasedEvent.Post e = new MouseReleasedEvent.Post(MinecraftClient.getInstance().currentScreen, mX, mY, MixinCache.currentMouseButton);
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
					KeyboardKeyReleasedEvent.Pre e = new KeyboardKeyReleasedEvent.Pre(MinecraftClient.getInstance().currentScreen, MixinCache.currentKeyboardKey, MixinCache.currentKeyboardScancode, MixinCache.currentKeyboardModifiers);
					Konkrete.getEventHandler().callEventsFor(e);
					if (e.isCanceled()) {
						info.cancel();
					}
				}
			} else {
				KeyboardKeyPressedEvent.Pre e = new KeyboardKeyPressedEvent.Pre(MinecraftClient.getInstance().currentScreen, MixinCache.currentKeyboardKey, MixinCache.currentKeyboardScancode, MixinCache.currentKeyboardModifiers);
				Konkrete.getEventHandler().callEventsFor(e);
				if (e.isCanceled()) {
					info.cancel();
				}
			}
		}
	}
	

	//KeyboardKeyPressedEvent.Post & KeyboardKeyReleasedEvent.Post
	@Inject(at = @At(value = "TAIL"), method = "wrapScreenError", cancellable = false)
	private static void onKeyboardPressedReleasedPost(Runnable task, String errorTitle, String screenName, CallbackInfo info) {
		if ((errorTitle != null) && errorTitle.equals("keyPressed event handler")) {
			int i = MixinCache.currentKeyboardAction;
			if (i != 1 && (i != 2 || !getRepeatEvents())) {
				if (i == 0) {
					KeyboardKeyReleasedEvent.Post e = new KeyboardKeyReleasedEvent.Post(MinecraftClient.getInstance().currentScreen, MixinCache.currentKeyboardKey, MixinCache.currentKeyboardScancode, MixinCache.currentKeyboardModifiers);
					Konkrete.getEventHandler().callEventsFor(e);
				}
			} else {
				KeyboardKeyPressedEvent.Post e = new KeyboardKeyPressedEvent.Post(MinecraftClient.getInstance().currentScreen, MixinCache.currentKeyboardKey, MixinCache.currentKeyboardScancode, MixinCache.currentKeyboardModifiers);
				Konkrete.getEventHandler().callEventsFor(e);
			}
		}
	}
	
	//KeyboardCharTypedEvent.Pre
	@Inject(at = @At(value = "HEAD"), method = "wrapScreenError", cancellable = true)
	private static void onKeyboardCharTypedPre(Runnable task, String errorTitle, String screenName, CallbackInfo info) {
		if ((errorTitle != null) && errorTitle.equals("charTyped event handler")) {
			KeyboardCharTypedEvent.Pre e = new KeyboardCharTypedEvent.Pre(MinecraftClient.getInstance().currentScreen, (char)MixinCache.currentKeyboardChar, MixinCache.currentKeyboardCharModifiers);
			Konkrete.getEventHandler().callEventsFor(e);
			if (e.isCanceled()) {
				info.cancel();
			}
		}
	}

	//KeyboardCharTypedEvent.Post
	@Inject(at = @At(value = "TAIL"), method = "wrapScreenError", cancellable = false)
	private static void onKeyboardCharTypedPost(Runnable task, String errorTitle, String screenName, CallbackInfo info) {
		if ((errorTitle != null) && errorTitle.equals("charTyped event handler")) {
			KeyboardCharTypedEvent.Post e = new KeyboardCharTypedEvent.Post(MinecraftClient.getInstance().currentScreen, (char)MixinCache.currentKeyboardChar, MixinCache.currentKeyboardCharModifiers);
			Konkrete.getEventHandler().callEventsFor(e);
		}
	}
	
	private static boolean getRepeatEvents() {
		try {
			Field f = ReflectionHelper.findField(Keyboard.class, "repeatEvents", "field_1683");
			return f.getBoolean(MinecraftClient.getInstance().keyboard);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
}
