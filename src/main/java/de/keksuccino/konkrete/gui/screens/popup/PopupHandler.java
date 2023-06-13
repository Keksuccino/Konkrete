package de.keksuccino.konkrete.gui.screens.popup;

import de.keksuccino.konkrete.Konkrete;
import de.keksuccino.konkrete.events.SubscribeEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.rendering.RenderUtils;

public class PopupHandler {
	
	private static Popup popup;
	private static boolean initDone = false;
	
	public static void init() {
		if (!initDone) {
			Konkrete.getEventHandler().registerEventsFrom(PopupHandler.class);
			initDone = true;
		}
	}
	
	@SubscribeEvent
	public static void onRender(GuiScreenEvent.DrawScreenEvent.Post e) {
		if ((popup != null) && popup.isDisplayed()) {
			MouseInput.blockVanillaInput("popupgui");
			RenderUtils.setZLevelPre(e.getMatrixStack(), 500);
			popup.render(e.getGuiGraphics(), e.getMouseX(), e.getMouseY(), e.getGui());
			RenderUtils.setZLevelPost(e.getMatrixStack());
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
