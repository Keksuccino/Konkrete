package de.keksuccino.konkrete.mixin.support.client.rinku;

import de.keksuccino.konkrete.util.input.Utf16CodeUnitDispatcher;
import de.keksuccino.konkrete.util.rinku.WrappedRinkuBrowser;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;

/** Optional Rinku-specific screen input dispatch, loaded only behind the Rinku mixin gate. */
public final class RinkuKeyboardInputBridge {

    private RinkuKeyboardInputBridge() {
    }

    public static boolean routeKey(Screen screen, int action, KeyEvent event) {
        for (GuiEventListener listener : screen.children()) {
            if (!(listener instanceof WrappedRinkuBrowser)) continue;
            boolean handled = action == GLFW.GLFW_RELEASE ? listener.keyReleased(event) : (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT) && listener.keyPressed(event);
            if (handled) return true;
        }
        return false;
    }

    public static boolean routeCharacter(Screen screen, CharacterEvent event) {
        for (GuiEventListener listener : screen.children()) {
            if (listener instanceof WrappedRinkuBrowser && Utf16CodeUnitDispatcher.dispatch(event, listener::charTyped)) return true;
        }
        return false;
    }

}
