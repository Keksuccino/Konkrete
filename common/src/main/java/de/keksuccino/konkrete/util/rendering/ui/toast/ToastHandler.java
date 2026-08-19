package de.keksuccino.konkrete.util.rendering.ui.toast;

import de.keksuccino.konkrete.util.threading.KonkreteThreads;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

/** Coordinates toast lifecycle and event dispatch. */
public class ToastHandler {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Opens toast. */
    public static void showToast(@NotNull final SimpleToast toast, final long durationMs) {
        final long start = System.currentTimeMillis();
        Minecraft.getInstance().gui.toastManager().addToast(toast);
        KonkreteThreads.startDaemonThread(() -> {
            try {
                while (true) {
                    long now = System.currentTimeMillis();
                    if ((start + durationMs) < now) {
                        toast.hide();
                        break;
                    }
                    Thread.sleep(100);
                }
            } catch (Exception ex) {
                try {
                    toast.hide();
                } catch (Exception ignore) {}
                LOGGER.error("[KONKRETE] Error in timer thread of SimpleToast in ToastHandler!", ex);
            }
        }, "Toast-DurationTimer");
    }

}
