package de.keksuccino.konkrete.gui.content.widget;

import de.keksuccino.konkrete.mixin.mixins.client.IMixinAbstractWidget;
import net.minecraft.client.gui.components.AbstractWidget;

public class WidgetUtils {

	public static AbstractWidget setHeight(AbstractWidget widget, int height) {
		((IMixinAbstractWidget)widget).setHeightKonkrete(height);
		return widget;
	}

}
