package de.keksuccino.konkrete.util;

/** Interruption-aware thread timing utilities. */
public class ThreadUtils {

    /**
     * Sleeps for {@code millis}; interruption restores the current thread's interrupted status.
     *
     * @throws IllegalArgumentException when {@code millis} is negative
     */
    public static void sleep(long millis) {
        try {
             Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

}
