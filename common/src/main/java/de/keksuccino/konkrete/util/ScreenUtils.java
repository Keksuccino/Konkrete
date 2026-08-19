package de.keksuccino.konkrete.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** Utility methods for screen. */
@SuppressWarnings("unused")
public class ScreenUtils {

    private static int setScreenBlockDepth = 0;

    /** Enables or disables guarded screen transitions. */
    public static void blockSetScreenCalls(boolean blocked) {
        if (blocked) {
            setScreenBlockDepth++;
        } else if (setScreenBlockDepth > 0) {
            setScreenBlockDepth--;
        }
    }

    /** Reports whether guarded screen transitions are blocked. */
    public static boolean areSetScreenCallsBlocked() {
        return setScreenBlockDepth > 0;
    }

    /** Returns the screen. */
    @Nullable
    public static Screen getScreen() {
        return Minecraft.getInstance().gui.screen();
    }

    /** Sets screen. */
    public static void setScreen(@Nullable Screen screen) {
        Minecraft.getInstance().gui.setScreen(screen);
    }

    /**
     * Opens a live screen and restores the previously valid screen if synchronous initialization fails. Vanilla assigns
     * the new screen before calling its init method, so callers otherwise leave a partially initialized screen active.
     */
    public static void setScreenWithRollback(@Nullable Screen screen) {
        setScreenWithRollback(screen, ScreenUtils::getScreen, ScreenUtils::setScreen);
    }

    static <T> void setScreenWithRollback(@Nullable T screen, Supplier<T> currentScreenSupplier, Consumer<T> rawScreenSetter) {
        T previousScreen = currentScreenSupplier.get();
        try {
            rawScreenSetter.accept(screen);
        } catch (RuntimeException | Error openingFailure) {
            if (currentScreenSupplier.get() != previousScreen) {
                try {
                    // Call the raw setter so a rollback failure cannot recursively enter this recovery method.
                    rawScreenSetter.accept(previousScreen);
                } catch (RuntimeException | Error rollbackFailure) {
                    openingFailure.addSuppressed(rollbackFailure);
                }
            }
            throw openingFailure;
        }
    }

    /** Returns the screen width. */
    public static int getScreenWidth() {
        Screen s = getScreen();
        return (s != null) ? s.width : 0;
    }

    /** Returns the screen height. */
    public static int getScreenHeight() {
        Screen s = getScreen();
        return (s != null) ? s.height : 0;
    }

    /** Returns the screen center x. */
    public static int getScreenCenterX() {
        return Math.max(1, getScreenWidth()) / 2;
    }

    /** Returns the screen center y. */
    public static int getScreenCenterY() {
        return Math.max(1, getScreenHeight()) / 2;
    }

}
