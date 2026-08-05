package de.keksuccino.persephone.gui.content.widget;

import net.minecraft.client.gui.components.AbstractWidget;
import org.jetbrains.annotations.NotNull;

public class WidgetUtils {

    @Deprecated
	public static AbstractWidget setHeight(@NotNull AbstractWidget widget, int height) {
		widget.setHeight(height);
		return widget;
	}

}
