package de.keksuccino.konkrete.events;

import net.minecraftforge.common.MinecraftForge;

public class EventHooks {

    public static void onScreenTick() {
        MinecraftForge.EVENT_BUS.post(new ScreenTickEvent());
    }

    public static void onScreenCharTyped(char character, int modifiers) {
        MinecraftForge.EVENT_BUS.post(new ScreenCharTypedEvent(character, modifiers));
    }

    public static void onScreenKeyPressed(int keyCode, int scanCode, int modifiers) {
        MinecraftForge.EVENT_BUS.post(new ScreenKeyPressedEvent(keyCode, scanCode, modifiers));
    }

    public static void onScreenKeyReleased(int keyCode, int scanCode, int modifiers) {
        MinecraftForge.EVENT_BUS.post(new ScreenKeyReleasedEvent(keyCode, scanCode, modifiers));
    }

    public static void onScreenMouseClicked(double mouseX, double mouseY, int mouseButton) {
        MinecraftForge.EVENT_BUS.post(new ScreenMouseClickedEvent(mouseX, mouseY, mouseButton));
    }

}
