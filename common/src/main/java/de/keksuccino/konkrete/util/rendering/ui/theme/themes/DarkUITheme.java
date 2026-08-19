package de.keksuccino.konkrete.util.rendering.ui.theme.themes;

import de.keksuccino.konkrete.util.rendering.DrawableColor;
import de.keksuccino.konkrete.util.rendering.ui.theme.UITheme;
import java.awt.Color;

/** Defines the built-in dark UI color palette. */
public class DarkUITheme extends UITheme {

    /** Creates an empty dark UI theme with default state. */
    public DarkUITheme() {

        super("dark", "konkrete.ui.themes.dark");

        allow_blur = true;
        ui_interface_background_color = DrawableColor.of(new Color(33, 33, 33));
        pip_docking_overlay_color = DrawableColor.of(new Color(64, 150, 255, 80));
        pip_docking_overlay_border_color = DrawableColor.of(new Color(64, 150, 255, 200));

    }

}
