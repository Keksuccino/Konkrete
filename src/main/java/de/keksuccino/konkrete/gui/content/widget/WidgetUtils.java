package de.keksuccino.konkrete.gui.content.widget;

import de.keksuccino.konkrete.mixin.mixins.client.IMixinAbstractWidget;
import net.minecraft.client.gui.components.AbstractWidget;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("all")
public class WidgetUtils {

	public static AbstractWidget setHeight(@NotNull AbstractWidget widget, int height) {
		((IMixinAbstractWidget)widget).setHeightKonkrete(height);
		return widget;
	}

}
