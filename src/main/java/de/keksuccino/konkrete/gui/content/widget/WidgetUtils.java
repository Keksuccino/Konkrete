package de.keksuccino.konkrete.gui.content.widget;

import java.lang.reflect.Field;

import de.keksuccino.konkrete.reflection.ReflectionHelper;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.PressableWidget;

public class WidgetUtils {
	
	public static ClickableWidget setHeight(ClickableWidget widget, int height) {
		try {
			Field f = ReflectionHelper.findField(ClickableWidget.class, "height", "field_22759");
			f.set(widget, height);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return widget;
	}

}
