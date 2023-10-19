package de.keksuccino.konkrete.gui.screens.popup;

import de.keksuccino.konkrete.Konkrete;
import de.keksuccino.konkrete.events.SubscribeEvent;
import de.keksuccino.konkrete.events.client.AfterRenderScreenEvent;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.rendering.RenderUtils;

@Deprecated
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
	public static void onRender(AfterRenderScreenEvent e) {
		if ((popup != null) && popup.isDisplayed()) {
			MouseInput.blockVanillaInput("popupgui");
			RenderUtils.setZLevelPre(e.getGraphics(), 500);
			popup.render(e.getGraphics(), e.getMouseX(), e.getMouseY(), e.getScreen());
			RenderUtils.setZLevelPost(e.getGraphics());
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
