package de.keksuccino.konkrete.util.input;

import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;

/** Counts clicks in a rolling one-second window. */
public final class ClicksPerSecondTracker {

    private static final long TIME_WINDOW_MS_KONKRETE = 1000L;
    private static final Object LOCK_KONKRETE = new Object();
    private static final ArrayDeque<Long> LEFT_CLICKS_KONKRETE = new ArrayDeque<>();
    private static final ArrayDeque<Long> RIGHT_CLICKS_KONKRETE = new ArrayDeque<>();

    private ClicksPerSecondTracker() {
    }

    /** Records click. */
    public static void recordClick(int mouseButton) {
        long now = Util.getMillis();

        synchronized (LOCK_KONKRETE) {
            pruneOldClicks_Konkrete(LEFT_CLICKS_KONKRETE, now);
            pruneOldClicks_Konkrete(RIGHT_CLICKS_KONKRETE, now);

            if (mouseButton == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                LEFT_CLICKS_KONKRETE.addLast(now);
            } else if (mouseButton == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                RIGHT_CLICKS_KONKRETE.addLast(now);
            }
        }
    }

    /** Returns the clicks per second. */
    public static int getClicksPerSecond(boolean rightMouseButton) {
        long now = Util.getMillis();

        synchronized (LOCK_KONKRETE) {
            ArrayDeque<Long> target = rightMouseButton ? RIGHT_CLICKS_KONKRETE : LEFT_CLICKS_KONKRETE;
            pruneOldClicks_Konkrete(target, now);
            return target.size();
        }
    }

    private static void pruneOldClicks_Konkrete(ArrayDeque<Long> clicks, long now) {
        long threshold = now - TIME_WINDOW_MS_KONKRETE;
        while (!clicks.isEmpty()) {
            Long first = clicks.peekFirst();
            if (first == null || first >= threshold) {
                return;
            }
            clicks.removeFirst();
        }
    }
}
