package de.keksuccino.konkrete.gui.content.handling;

import de.keksuccino.konkrete.Konkrete;
import de.keksuccino.konkrete.events.*;
import de.keksuccino.konkrete.events.client.*;
import de.keksuccino.konkrete.input.CharData;
import de.keksuccino.konkrete.input.KeyboardData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdvancedWidgetsHandler {

    protected static Map<IAdvancedWidgetBase, Long> widgets = new HashMap<>();

    public static void init() {

        Konkrete.getEventHandler().registerEventsFrom(new AdvancedWidgetsHandler());

    }

    protected AdvancedWidgetsHandler() {
    }

    /** Call this in the widget's render method! */
    public static void handleWidget(IAdvancedWidgetBase widget) {
        widgets.put(widget, System.currentTimeMillis());
    }

    @SubscribeEvent
    public void onScreenCharTyped(ScreenCharTypedEvent e) {
        for (Map.Entry<IAdvancedWidgetBase, Long> m : widgets.entrySet()) {
            CharData d = new CharData(e.character, e.modifiers);
            m.getKey().onCharTyped(d);
        }
    }

    @SubscribeEvent
    public void onScreenKeyPressed(ScreenKeyPressedEvent e) {
        for (Map.Entry<IAdvancedWidgetBase, Long> m : widgets.entrySet()) {
            KeyboardData d = new KeyboardData(e.keyCode, e.scanCode, e.modifiers);
            m.getKey().onKeyPress(d);
        }
    }

    @SubscribeEvent
    public void onScreenKeyReleased(ScreenKeyReleasedEvent e) {
        for (Map.Entry<IAdvancedWidgetBase, Long> m : widgets.entrySet()) {
            KeyboardData d = new KeyboardData(e.keyCode, e.scanCode, e.modifiers);
            m.getKey().onKeyReleased(d);
        }
    }

    @SubscribeEvent
    public void onScreenMouseClicked(ScreenMouseClickedEvent e) {
        for (Map.Entry<IAdvancedWidgetBase, Long> m : widgets.entrySet()) {
            m.getKey().onMouseClicked(e.mouseX, e.mouseY, e.mouseButton);
        }
    }

    @SubscribeEvent
    public void onScreenTick(ScreenTickEvent e) {
        for (Map.Entry<IAdvancedWidgetBase, Long> m : widgets.entrySet()) {
            m.getKey().onTick();
        }
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Pre e) {
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

    @SubscribeEvent
    public void onInitScreen(GuiScreenEvent.InitGuiEvent.Pre e) {
        widgets.clear();
    }

}
