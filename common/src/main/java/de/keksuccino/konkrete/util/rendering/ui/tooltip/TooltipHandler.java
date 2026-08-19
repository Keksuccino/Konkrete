package de.keksuccino.konkrete.util.rendering.ui.tooltip;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

/** Coordinates tooltip lifecycle and event dispatch. */
public class TooltipHandler implements Renderable {

    /** Shared handler instance. */
    public static final TooltipHandler INSTANCE = new TooltipHandler();

    private final List<HandledTooltip> tooltips = new ArrayList<>();
    private final Map<AbstractWidget, HandledTooltip> widgetTooltips = new HashMap<>();

    private TooltipHandler() {
    }

    /** Adds this component's complete draw state to the active GUI extraction pass. */
    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        HandledTooltip renderTooltip = null;
        for (HandledTooltip t : new ArrayList<>(this.tooltips)) {
            if (t.shouldRender.getAsBoolean()) {
                renderTooltip = t;
            }
            t.remove();
        }
        if (renderTooltip != null) {
            renderTooltip.UITooltip.extractRenderState(graphics, mouseX, mouseY, partial);
        }
    }

    /** Adds widget tooltip to this tooltip handler. */
    @Deprecated
    public HandledTooltip addWidgetTooltip(@NotNull AbstractWidget widget, @NotNull UITooltip UITooltip, boolean unusedBoolean1, boolean unusedBoolean2) {
        return addRenderTickWidgetTooltip(widget, UITooltip);
    }

    /** Adds render tick widget tooltip to this tooltip handler. */
    public HandledTooltip addRenderTickWidgetTooltip(@NotNull AbstractWidget widget, @NotNull UITooltip UITooltip) {
        if (this.widgetTooltips.containsKey(widget)) {
            this.removeTooltip(this.widgetTooltips.get(widget));
        }
        HandledTooltip t = this.addRenderTickTooltip(UITooltip, () -> widget.isHovered() && widget.visible);
        t.widget = widget;
        this.widgetTooltips.put(widget, t);
        return t;
    }

    /** Adds tooltip to this tooltip handler. */
    @Deprecated
    public HandledTooltip addTooltip(@NotNull UITooltip UITooltip, @NotNull BooleanSupplier shouldRender, boolean unusedBoolean1, boolean unusedBoolean2) {
        return addRenderTickTooltip(UITooltip, shouldRender);
    }

    /** Adds render tick tooltip to this tooltip handler. */
    public HandledTooltip addRenderTickTooltip(@NotNull UITooltip UITooltip, @NotNull BooleanSupplier shouldRender) {
        HandledTooltip t = new HandledTooltip(this, UITooltip, shouldRender);
        this.tooltips.add(t);
        return t;
    }

    /** Removes tooltip from this tooltip handler. */
    public void removeTooltip(HandledTooltip tooltip) {
        this.tooltips.remove(tooltip);
        if (tooltip.widget != null) {
            this.widgetTooltips.remove(tooltip.widget);
        }
    }

    /** Stores the active tooltip contents, position, and optional custom font. */
    public static class HandledTooltip {

        private final TooltipHandler parent;
        /** Tooltip contents and placement managed by this handle. */
        public final UITooltip UITooltip;
        /** Determines whether the tooltip should be rendered. */
        public final BooleanSupplier shouldRender;
        /** Widget whose hover state controls tooltip visibility. */
        protected AbstractWidget widget = null;

        private HandledTooltip(TooltipHandler parent, UITooltip UITooltip, BooleanSupplier shouldRender) {
            this.parent = parent;
            this.UITooltip = UITooltip;
            this.shouldRender = shouldRender;
        }

        /** Removes the tooltip from its handler. **/
        public void remove() {
            this.parent.removeTooltip(this);
        }

    }

}
