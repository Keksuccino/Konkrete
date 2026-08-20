package de.keksuccino.konkrete.mixin.support.client.widget;

import de.keksuccino.konkrete.util.rendering.ui.widget.IExtendedWidget;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/** Reuses the extended-widget scrolling renderer for vanilla widgets with customized label presentation. */
public final class WidgetLabelRenderer implements IExtendedWidget {

    public static final WidgetLabelRenderer INSTANCE = new WidgetLabelRenderer();

    private WidgetLabelRenderer() {}

    /** Renders the exact component selected by the caller, preserving loader-specific styles applied before label extraction. */
    public void renderScrollingLabel(@NotNull AbstractWidget widget, @NotNull GuiGraphicsExtractor graphics, @NotNull Font font, @NotNull Component component, int margin, boolean shadow, int color) {
        int xMin = widget.getX() + margin;
        int xMax = widget.getX() + widget.getWidth() - margin;
        int yMin = widget.getY();
        int yMax = widget.getY() + widget.getHeight();
        float scale = this.resolveLabelScale(widget);
        if (scale == 0.0F) return;
        if (scale == 1.0F) this.renderScrollingLabelInternal(graphics, font, component, xMin, yMin, xMax, yMax, shadow, color);
        else this.renderScrollingLabelInternalScaled(graphics, font, component, xMin, yMin, xMax, yMax, shadow, color, scale);
    }

}
