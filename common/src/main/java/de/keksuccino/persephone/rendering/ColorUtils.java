package de.keksuccino.persephone.rendering;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.awt.*;

public class ColorUtils {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Returns the converted color or NULL if the color could not be converted.
     */
    @Nullable
    public static Color getColorFromHexString(@NotNull String hex) {
        try {
            hex = hex.replace("#", "");
            if (hex.length() == 6) {
                return new Color(
                        Integer.valueOf(hex.substring(0, 2), 16),
                        Integer.valueOf(hex.substring(2, 4), 16),
                        Integer.valueOf(hex.substring(4, 6), 16));
            }
            if (hex.length() == 8) {
                return new Color(
                        Integer.valueOf(hex.substring(0, 2), 16),
                        Integer.valueOf(hex.substring(2, 4), 16),
                        Integer.valueOf(hex.substring(4, 6), 16),
                        Integer.valueOf(hex.substring(6, 8), 16));
            }
        } catch (Exception ex) {
            LOGGER.error("[PERSEPHONE] Failed to build Color object from HEX color string!", ex);
        }
        return null;
    }

}
