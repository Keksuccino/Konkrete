package de.keksuccino.konkrete.gui.screens.popup;

import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.rendering.RenderUtils;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class PopupHandler {
	
	private static Popup popup;
	private static boolean initDone = false;
	
	public static void init() {
		if (!initDone) {
			MinecraftForge.EVENT_BUS.register(PopupHandler.class);
			initDone = true;
		}
	}
	
	@SubscribeEvent
	public static void onRender(GuiScreenEvent.DrawScreenEvent.Post e) {
		if ((popup != null) && popup.isDisplayed()) {
			MouseInput.blockVanillaInput("popupgui");
			RenderUtils.setZLevelPre(500);
			popup.render(e.getMouseX(), e.getMouseY(), e.getGui());
			RenderUtils.setZLevelPost();
		} else {
			MouseInput.unblockVanillaInput("popupgui");
		}
	}
	
	public static boolean isPopupActive() {
		if (popup == null) {
			return false;
		}
		return popup.isDisplayed();
	}
	
	public static Popup getCurrentPopup() {
		return popup;
	}
	
	public static void displayPopup(Popup p) {
		if (popup != null) {
			popup.setDisplayed(false);
		}
		popup = p;
		popup.setDisplayed(true);
	}

}
