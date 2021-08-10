package de.keksuccino.konkrete;

import java.awt.Color;

import de.keksuccino.konkrete.events.SubscribeEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent.BackgroundDrawnEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent.DrawScreenEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;

public class TestEvents {

//	@SubscribeEvent
//	public void onOpenGui(InitGuiEvent.Pre e) {
//		if (e.getGui() instanceof TitleScreen) {
//			e.setCanceled(true);
//			MinecraftClient.getInstance().openScreen(new SimpleLoadingScreen(MinecraftClient.getInstance()));
//		}
//	}
	
//	@SubscribeEvent
//	public void onBackgroundDrawn(BackgroundDrawnEvent e) {
//		System.out.println("BACKGROUND DRAWN!");
//	}
	
	@SubscribeEvent
	public void onDrawPre(DrawScreenEvent.Pre e) {
		DrawableHelper.drawCenteredText(e.getMatrixStack(), MinecraftClient.getInstance().textRenderer, "Huhuuuu", 20, 50, Color.GREEN.getRGB());
	}
	
	@SubscribeEvent
	public void onDrawPost(DrawScreenEvent.Post e) {
		DrawableHelper.drawCenteredText(e.getMatrixStack(), MinecraftClient.getInstance().textRenderer, "Huhu", 50, 50, Color.CYAN.getRGB());
	}
	
//	@SubscribeEvent
//	public void onGameInitCompleted(GameInitializationCompletedEvent e) {
//		System.out.println("######## GAME INIT DONE! [via GameInitCompleteEvent]");
//	}
//	
//	@SubscribeEvent
//	public void onTitleScreen(GuiScreenEvent.InitGuiEvent.Pre e) {
//		if (e.getGui() instanceof TitleScreen) {
//			System.out.println("######## INIT TITLE SCREEN [via InitGuiEvent]");
//		}
//	}
	
//	@SubscribeEvent
//	public void onMouseClickedPre(MouseClickedEvent.Pre e) {
//		System.out.println("MOUSE CLICK PRE:");
//		System.out.println("Screen: " + e.getGui().getClass().getCanonicalName());
//		System.out.println("Button: " + e.getButton());
//		System.out.println("mX: " + e.getMouseX());
//		System.out.println("mY: " + e.getMouseY());
//		System.out.println("------------------------------");
//	}
//	
//	@SubscribeEvent
//	public void onMouseClickedPost(MouseClickedEvent.Post e) {
//		System.out.println("MOUSE CLICK POST:");
//		System.out.println("Screen: " + e.getGui().getClass().getCanonicalName());
//		System.out.println("Button: " + e.getButton());
//		System.out.println("mX: " + e.getMouseX());
//		System.out.println("mY: " + e.getMouseY());
//		System.out.println("------------------------------");
//	}
	
//	@SubscribeEvent
//	public void onGuiMouseScrollPre(MouseScrollEvent.Pre e) {
//		System.out.println("SCROLL PRE DELTA: " + e.getScrollDelta());
//	}
//	
//	@SubscribeEvent
//	public void onKeyboardCharPre(KeyboardCharTypedEvent.Pre e) {
//		System.out.println("Char Pre: " + e.getCodePoint());
//		System.out.println("Code Pre: " + e.getModifiers());
//	}
//	
//	@SubscribeEvent
//	public void onKeyboardCharPost(KeyboardCharTypedEvent.Post e) {
//		System.out.println("Char Post: " + e.getCodePoint());
//		System.out.println("Code Post: " + e.getModifiers());
//	}
//	
//	@SubscribeEvent
//	public void onKeyboardPress(KeyboardKeyPressedEvent.Pre e) {
//		System.out.println("KEY PRESS PRE: " + e.getKeyCode());
//	}
//	
//	@SubscribeEvent
//	public void onKeyboardRelease(KeyboardKeyReleasedEvent.Pre e) {
//		System.out.println("KEY RELEASE PRE: " + e.getKeyCode());
//	}
//	
//	@SubscribeEvent
//	public void onMouseReleasedPre(MouseReleasedEvent.Pre e) {
//		System.out.println("MOUSE RELEASE PRE:");
//		System.out.println("Screen: " + e.getGui().getClass().getCanonicalName());
//		System.out.println("Button: " + e.getButton());
//		System.out.println("mX: " + e.getMouseX());
//		System.out.println("mY: " + e.getMouseY());
//		System.out.println("------------------------------");
//	}
//	
//	@SubscribeEvent
//	public void onMouseReleasedPost(MouseReleasedEvent.Post e) {
//		System.out.println("MOUSE RELEASE POST:");
//		System.out.println("Screen: " + e.getGui().getClass().getCanonicalName());
//		System.out.println("Button: " + e.getButton());
//		System.out.println("mX: " + e.getMouseX());
//		System.out.println("mY: " + e.getMouseY());
//		System.out.println("------------------------------");
//	}
	
//	@SubscribeEvent
//	public void onInitGuiPre(InitGuiEvent.Pre e) {
//		e.setCanceled(true);
//	}
//	
//	@SubscribeEvent
//	public void onInitGuiPost(InitGuiEvent.Post e) {
//
//	}	
//	@SubscribeEvent
//	public void onClientTickPre(ClientTickEvent.Pre e) {
//		System.out.println("TICK PRE");
//	}
//	
//	@SubscribeEvent
//	public void onClientTickPost(ClientTickEvent.Post e) {
//		System.out.println("TICK POST");
//	}
	
}
