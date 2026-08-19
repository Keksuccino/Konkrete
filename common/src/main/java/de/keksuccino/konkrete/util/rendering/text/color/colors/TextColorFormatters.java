package de.keksuccino.konkrete.util.rendering.text.color.colors;

import de.keksuccino.konkrete.util.rendering.text.color.DynamicTextColorFormatter;
import de.keksuccino.konkrete.util.rendering.text.color.TextColorFormatterRegistry;
import de.keksuccino.konkrete.util.rendering.ui.UIBase;

/** Catalogs built-in custom text-color formatters. */
public class TextColorFormatters {

    /** Registers all for later lookup. */
    public static void registerAll() {

        TextColorFormatterRegistry.register("orange", new DynamicTextColorFormatter('z', () -> UIBase.getUITheme().warning_color));
        TextColorFormatterRegistry.register("green", new DynamicTextColorFormatter('y', () -> UIBase.getUITheme().success_color));
        TextColorFormatterRegistry.register("red", new DynamicTextColorFormatter('x', () -> UIBase.getUITheme().error_color));

    }

}
