package de.keksuccino.konkrete.gui.content.widget;

import java.lang.reflect.Field;

import de.keksuccino.konkrete.reflection.ReflectionHelper;
import net.minecraft.client.gui.widget.AbstractButtonWidget;

public class WidgetUtils {
	
	public static AbstractButtonWidget setHeight(AbstractButtonWidget widget, int height) {
		try {
			Field f = ReflectionHelper.findField(AbstractButtonWidget.class, "height", "field_22759");
			f.set(widget, height);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return widget;
	}

}
