package de.keksuccino.konkrete.util.rendering.ui.widget.component;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.konkrete.placeholder.PlaceholderParser;
import de.keksuccino.konkrete.util.ConsumingSupplier;
import de.keksuccino.konkrete.util.rendering.DrawableColor;
import de.keksuccino.konkrete.util.rendering.ui.UIBase;
import de.keksuccino.konkrete.util.rendering.ui.widget.NavigatableWidget;
import de.keksuccino.konkrete.util.rendering.ui.widget.slider.UIWidget;
import de.keksuccino.konkrete.util.rendering.text.smooth.TextDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Displays and routes input for component. */
@SuppressWarnings("unused")
public class ComponentWidget extends AbstractWidget implements NavigatableWidget, UIWidget {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Computes the rendered component from the widget's current state. */
    @NotNull
    protected ConsumingSupplier<ComponentWidget, MutableComponent> textSupplier;
    /** Whether text or geometry is drawn with a shadow. */
    protected boolean shadow = true;
    /** Computes the base text color from the widget's current state. */
    @NotNull
    protected ConsumingSupplier<ComponentWidget, DrawableColor> baseColorSupplier = (var) -> UIBase.getUITheme().ui_interface_generic_text_color;
    /** Receives this widget when hover or focus begins. */
    protected Consumer<ComponentWidget> onHoverOrFocusStart;
    /** Receives this widget when hover and focus both end. */
    protected Consumer<ComponentWidget> onHoverOrFocusEnd;
    /** Receives this widget when it is activated. */
    protected Consumer<ComponentWidget> onClick;
    /** Parent widget that owns this node in the component tree. */
    @Nullable
    protected ComponentWidget parent;
    /** Nested component widgets in render order. */
    protected List<ComponentWidget> children = new ArrayList<>();
    /** Font used to measure and draw the component. */
    @NotNull
    protected Font font;
    /** Whether pointer hover or keyboard focus currently targets the widget. */
    protected boolean isCurrentlyHoveredOrFocused = false;
    /** Horizontal GUI coordinate for end. */
    protected int endX;
    /** Whether text uses UIBase metrics and rendering. */
    protected boolean useUIRendering = false;

    /** Creates a widget positioned around one mutable component. */
    public static ComponentWidget of(@NotNull MutableComponent component, int x, int y) {
        return new ComponentWidget(Minecraft.getInstance().font, x, y, component);
    }

    /** Creates a literal component from the supplied text. */
    public static ComponentWidget literal(@NotNull String text, int x, int y) {
        ComponentWidget w = new ComponentWidget(Minecraft.getInstance().font, x, y, Component.literal(""));
        w.setTextSupplier(consumes -> Component.literal(PlaceholderParser.replacePlaceholders(text)));
        return w;
    }

    /** Creates a translatable component from the supplied localization key. */
    public static ComponentWidget translatable(@NotNull String key, int x, int y) {
        ComponentWidget w = new ComponentWidget(Minecraft.getInstance().font, x, y, Component.literal(""));
        w.setTextSupplier(consumes -> Component.translatable(key));
        return w;
    }

    /** Returns an empty value with no configured content. */
    public static ComponentWidget empty(int x, int y) {
        return new ComponentWidget(Minecraft.getInstance().font, x, y, Component.literal(""));
    }

    /** Creates an editable component widget at the supplied GUI position. */
    protected ComponentWidget(@NotNull Font font, int x, int y, @NotNull MutableComponent text) {
        super(x, y, 0, 0, text);
        this.textSupplier = (var) -> text;
        this.font = font;
    }

    /** Adds this widget's draw state to the active GUI extraction pass. */
    @Override
    public void extractWidgetRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        this.width = this.getWidth();
        this.height = this.getHeight();

        this.handleComponentHover();

        this.endX = this.getX();
        if (this.useUIRendering) {
            if (UIBase.shouldUseMinecraftFontForUIRendering()) {
                graphics.text(Minecraft.getInstance().font, this.getText(), this.getX(), this.getY(), this.getBaseColor().getColorInt(), this.shadow);
                this.endX = this.getX() + Minecraft.getInstance().font.width(this.getText());
            } else {
                TextDimensions dimensions = UIBase.renderText(graphics, this.getText(), this.getX(), this.getY(), this.getBaseColor().getColorInt());
                this.endX = this.getX() + (int) Math.ceil(dimensions.width());
            }
        } else {
            graphics.text(this.font, this.getText(), this.getX(), this.getY(), this.getBaseColor().getColorInt(), this.shadow);
            this.endX = this.getX() + this.font.width(this.getText());
        }

        for (ComponentWidget c : this.children) {
            c.setX(this.endX);
            c.setY(this.getY());
            c.extractRenderState(graphics, mouseX, mouseY, partial);
            this.endX = c.endX;
        }

    }

    /** Appends a child and inherits this widget's UI-rendering mode. */
    public ComponentWidget append(@NotNull ComponentWidget child) {
        child.parent = this;
        child.useUIRendering = this.useUIRendering;
        this.children.add(child);
        return this;
    }

    /** Returns children. */
    public List<ComponentWidget> getChildren() {
        return this.children;
    }

    /** Returns the containing menu, widget, or entry. */
    @Nullable
    public ComponentWidget getParent() {
        return this.parent;
    }

    /** Returns text supplier. */
    @NotNull
    public ConsumingSupplier<ComponentWidget, MutableComponent> getTextSupplier() {
        return this.textSupplier;
    }

    /** Sets text supplier for this component widget. */
    public ComponentWidget setTextSupplier(@NotNull ConsumingSupplier<ComponentWidget, MutableComponent> textSupplier) {
        this.textSupplier = textSupplier;
        return this;
    }

    /** Returns text. */
    @NotNull
    public MutableComponent getText() {
        MutableComponent c = this.textSupplier.get(this);
        if (c == null) c = Component.literal("");
        return c;
    }

    /** Sets text for this component widget. */
    public ComponentWidget setText(@NotNull MutableComponent text) {
        this.textSupplier = (var) -> text;
        return this;
    }

    /** Reports whether text or geometry is drawn with a shadow. */
    public boolean hasShadow() {
        return this.shadow;
    }

    /** Sets shadow for this component widget. */
    public ComponentWidget setShadow(boolean shadow) {
        this.shadow = shadow;
        for (ComponentWidget w : this.children) {
            w.shadow = shadow;
        }
        return this;
    }

    /** Sets use UI font for this component widget. */
    public ComponentWidget setUseUIFont(boolean useUIRendering) {
        this.useUIRendering = useUIRendering;
        for (ComponentWidget w : this.children) {
            w.useUIRendering = useUIRendering;
        }
        return this;
    }

    /** Sets base color for this component widget. */
    public ComponentWidget setBaseColor(@NotNull DrawableColor baseColor) {
        this.baseColorSupplier = (var) -> baseColor;
        for (ComponentWidget w : this.children) {
            w.baseColorSupplier = this.baseColorSupplier;
        }
        return this;
    }

    /** Returns the base color resolved before state-specific styling. */
    @NotNull
    public DrawableColor getBaseColor() {
        DrawableColor c = this.baseColorSupplier.get(this);
        if (c == null) c = DrawableColor.WHITE;
        return c;
    }

    /** Sets base color supplier for this component widget. */
    public ComponentWidget setBaseColorSupplier(@NotNull ConsumingSupplier<ComponentWidget, DrawableColor> baseColorSupplier) {
        this.baseColorSupplier = baseColorSupplier;
        for (ComponentWidget w : this.children) {
            w.baseColorSupplier = baseColorSupplier;
        }
        return this;
    }

    /** Returns base color supplier. */
    @NotNull
    public ConsumingSupplier<ComponentWidget, DrawableColor> getBaseColorSupplier() {
        return this.baseColorSupplier;
    }

    /** Sets on hover or focus start for this component widget. */
    public ComponentWidget setOnHoverOrFocusStart(@Nullable Consumer<ComponentWidget> onHoverOrFocusStart) {
        this.onHoverOrFocusStart = onHoverOrFocusStart;
        return this;
    }

    /** Sets on hover or focus end for this component widget. */
    public ComponentWidget setOnHoverOrFocusEnd(@Nullable Consumer<ComponentWidget> onHoverOrFocusEnd) {
        this.onHoverOrFocusEnd = onHoverOrFocusEnd;
        return this;
    }

    /** Sets on click for this component widget. */
    public ComponentWidget setOnClick(@Nullable Consumer<ComponentWidget> onClick) {
        this.onClick = onClick;
        return this;
    }

    /** Returns the current width in GUI units. */
    @Override
    public int getWidth() {
        int w = this.useUIRendering
                ? (int) Math.ceil(UIBase.getUITextWidthNormal(this.getText()))
                : this.font.width(this.getText());
        for (ComponentWidget c : this.children) {
            w += c.getWidth();
        }
        return w;
    }

    /** Returns the current height in GUI units. */
    @Override
    public int getHeight() {
        if (this.useUIRendering) {
            return (int) Math.ceil(UIBase.getUITextHeightNormal());
        }
        return this.font.lineHeight;
    }

    /** Handles component hover for this component widget. */
    protected void handleComponentHover() {
        if (!this.isCurrentlyHoveredOrFocused) {
            if (this.isHoveredOrFocused()) {
                if (this.onHoverOrFocusStart != null) {
                    this.onHoverOrFocusStart.accept(this);
                }
                this.isCurrentlyHoveredOrFocused = true;
            }
        } else if (!this.isHoveredOrFocused()) {
            if (this.onHoverOrFocusEnd != null) {
                this.onHoverOrFocusEnd.accept(this);
            }
            this.isCurrentlyHoveredOrFocused = false;
        }
    }

    /** Routes a mouse-button press and reports whether it was consumed. */
    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean isDoubleClick) {
        return this.mouseClicked(event.x(), event.y(), event.button());
    }

    /** Routes a mouse-button press and reports whether it was consumed. */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.active && this.visible && this.isHoveredOrFocused() && (button == 0)) {
            for (ComponentWidget w : this.children) {
                if (w.mouseClicked(mouseX, mouseY, button)) return true;
            }
            if (this.onClick != null) {
                this.onClick.accept(this);
                return true;
            }
        }
        return false;
    }

    /** Publishes this widget's current narration data. */
    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput var1) {
    }

    /** Sets message for this component widget. */
    @Deprecated
    @Override
    public void setMessage(@NotNull Component content) {
        if (content instanceof MutableComponent m) this.textSupplier = (var) -> m;
    }

    /** Returns the component currently exposed for rendering and narration. */
    @Deprecated
    @Override
    public @NotNull Component getMessage() {
        return this.getText();
    }

    /** Reports whether this control may receive keyboard focus. */
    @Override
    public boolean isFocusable() {
        return false;
    }

    /** Sets focusable for this component widget. */
    @Override
    public void setFocusable(boolean focusable) {
        throw new RuntimeException("ComponentWidgets are not focusable!");
    }

    /** Reports whether keyboard navigation may target this control. */
    @Override
    public boolean isNavigatable() {
        return false;
    }

    /** Sets navigatable for this component widget. */
    @Override
    public void setNavigatable(boolean navigatable) {
        throw new RuntimeException("ComponentWidgets are not navigatable!");
    }

}
