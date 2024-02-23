package de.keksuccino.konkrete.gui.content.handling;

import de.keksuccino.konkrete.input.CharData;
import de.keksuccino.konkrete.input.KeyboardData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Deprecated(forRemoval = true)
public class AdvancedWidgetsHandler {

    protected static Map<IAdvancedWidgetBase, Long> widgets = new HashMap<>();

    protected AdvancedWidgetsHandler() {
    }

    public static void handleWidget(IAdvancedWidgetBase widget) {
        widgets.put(widget, System.currentTimeMillis());
    }

    public static void onScreenCharTyped(char character, int modifiers) {
        for (Map.Entry<IAdvancedWidgetBase, Long> m : widgets.entrySet()) {
            CharData d = new CharData(character, modifiers);
            m.getKey().onCharTyped(d);
        }
    }

    public static void onScreenKeyPressed(int keyCode, int scanCode, int modifiers) {
        for (Map.Entry<IAdvancedWidgetBase, Long> m : widgets.entrySet()) {
            KeyboardData d = new KeyboardData(keyCode, scanCode, modifiers);
            m.getKey().onKeyPress(d);
        }
    }

    public static void onClientTick() {
        long now = System.currentTimeMillis();
        List<IAdvancedWidgetBase> garbageCollected = new ArrayList<>();
        for (Map.Entry<IAdvancedWidgetBase, Long> m : widgets.entrySet()) {
            long lastRenderTick = m.getValue();
            IAdvancedWidgetBase widget = m.getKey();
            if ((lastRenderTick + 2000) < now) {
                garbageCollected.add(widget);
            }
        }
        for (IAdvancedWidgetBase widget : garbageCollected) {
            widgets.remove(widget);
        }
    }

    public static void onOpenScreen() {
        widgets.clear();
    }

}
