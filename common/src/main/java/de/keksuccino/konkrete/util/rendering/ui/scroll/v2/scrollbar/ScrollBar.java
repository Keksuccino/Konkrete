package de.keksuccino.konkrete.util.rendering.ui.scroll.v2.scrollbar;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.konkrete.util.rendering.DrawableColor;
import de.keksuccino.konkrete.util.rendering.ui.UIBase;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Renders and drags a scrollbar synchronized with a {@link de.keksuccino.konkrete.util.rendering.ui.scroll.v2.ScrollArea}. */
@SuppressWarnings("unused")
public class ScrollBar extends UIBase implements GuiEventListener, Renderable, NarratableEntry {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Axis along which this scroll bar moves. */
    protected final ScrollBarDirection direction;
    /** Width in GUI units for grabber. */
    public float grabberWidth;
    /** Height in GUI units for grabber. */
    public float grabberHeight;
    /** Horizontal GUI coordinate for scroll area start. */
    public float scrollAreaStartX;
    /** Vertical GUI coordinate for scroll area start. */
    public float scrollAreaStartY;
    /** Horizontal GUI coordinate for scroll area end. */
    public float scrollAreaEndX;
    /** Vertical GUI coordinate for scroll area end. */
    public float scrollAreaEndY;
    /** Supplies the idle grabber color. */
    public Supplier<DrawableColor> idleBarColor;
    /** Supplies the hovered grabber color. */
    public Supplier<DrawableColor> hoverBarColor;
    /** Texture rendered for the idle grabber. */
    public Identifier idleBarTexture;
    /** Texture rendered for the hovered grabber. */
    public Identifier hoverBarTexture;
    /** Whether the control accepts interaction. */
    public boolean active = true;
    /** Whether wheel input moves this scroll bar. */
    protected boolean allowScrollWheel = false;
    /** Grabber scroll speed applied to each scroll-input step. */
    protected float grabberScrollSpeed = 1.0F;
    /** Wheel scroll speed applied to each scroll-input step. */
    protected float wheelScrollSpeed = 1.0F;
    /** Current scroll offset in content units. */
    protected float scroll = 0.0F;
    /** Whether the primary mouse button is dragging the scroll grabber. */
    protected boolean leftMouseDownOnGrabber = false;
    /** Horizontal GUI coordinate for left mouse down on grabber at mouse. */
    protected double leftMouseDownOnGrabberAtMouseX = 0;
    /** Vertical GUI coordinate for left mouse down on grabber at mouse. */
    protected double leftMouseDownOnGrabberAtMouseY = 0;
    /** Horizontal GUI coordinate for last grabber. */
    protected float lastGrabberX = 0;
    /** Vertical GUI coordinate for last grabber. */
    protected float lastGrabberY = 0;
    /** Scroll offset captured when grabber dragging began. */
    protected float leftMouseDownOnGrabberAtScroll = 0.0F;
    /** Grabber edge inset in GUI pixels. */
    protected float grabberEdgeInset = 1.0F;
    /** Scroll bar controlling scroll listeners. */
    protected List<Consumer<ScrollBar>> scrollListeners = new ArrayList<>();
    /** Whether the pointer currently hovers the scroll grabber. */
    protected boolean grabberHovered = false;
    /** Whether the scroll grabber uses rounded corners. */
    protected boolean roundedGrabber = false;

    /** Defines a directional scrollbar track and grabber using color or texture styling. */
    public ScrollBar(@NotNull ScrollBarDirection direction, float grabberWidth, float grabberHeight, float scrollAreaStartX, float scrollAreaStartY, float scrollAreaEndX, float scrollAreaEndY, @NotNull Supplier<DrawableColor> idleBarColor, @NotNull Supplier<DrawableColor> hoverBarColor) {
        this(direction, grabberWidth, grabberHeight, scrollAreaStartX, scrollAreaStartY, scrollAreaEndX, scrollAreaEndY);
        this.idleBarColor = idleBarColor;
        this.hoverBarColor = hoverBarColor;
    }

    /** Defines a directional scrollbar track and grabber using color or texture styling. */
    public ScrollBar(@NotNull ScrollBarDirection direction, float grabberWidth, float grabberHeight, float scrollAreaStartX, float scrollAreaStartY, float scrollAreaEndX, float scrollAreaEndY, @NotNull Identifier idleBarTexture, @NotNull Identifier hoverBarTexture) {
        this(direction, grabberWidth, grabberHeight, scrollAreaStartX, scrollAreaStartY, scrollAreaEndX, scrollAreaEndY);
        this.idleBarTexture = idleBarTexture;
        this.hoverBarTexture = hoverBarTexture;
    }

    /** Defines a directional scrollbar track and grabber using color or texture styling. */
    protected ScrollBar(ScrollBarDirection direction, float grabberWidth, float grabberHeight, float scrollAreaStartX, float scrollAreaStartY, float scrollAreaEndX, float scrollAreaEndY) {
        this.direction = direction;
        this.grabberWidth = grabberWidth;
        this.grabberHeight = grabberHeight;
        this.scrollAreaStartX = scrollAreaStartX;
        this.scrollAreaStartY = scrollAreaStartY;
        this.scrollAreaEndX = scrollAreaEndX;
        this.scrollAreaEndY = scrollAreaEndY;
    }

    /** Adds this component's complete draw state to the active GUI extraction pass. */
    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {

        this.grabberHovered = this.isMouseOverGrabber(mouseX, mouseY);

        float effectiveStartX = this.scrollAreaStartX + this.grabberEdgeInset;
        float effectiveStartY = this.scrollAreaStartY + this.grabberEdgeInset;
        float effectiveEndX = Math.max(effectiveStartX, this.scrollAreaEndX - this.grabberEdgeInset);
        float effectiveEndY = Math.max(effectiveStartY, this.scrollAreaEndY - this.grabberEdgeInset);
        float x = effectiveEndX - this.grabberWidth;
        float y = effectiveEndY - this.grabberHeight;
        if (this.direction == ScrollBarDirection.VERTICAL) {
            float usableAreaHeight = Math.max(0.0F, effectiveEndY - effectiveStartY - this.grabberHeight);
            y = effectiveStartY + (usableAreaHeight * this.scroll);
        } else {
            float usableAreaWidth = Math.max(0.0F, effectiveEndX - effectiveStartX - this.grabberWidth);
            x = effectiveStartX + (usableAreaWidth * this.scroll);
        }
        this.lastGrabberX = x;
        this.lastGrabberY = y;

        resetShaderColor(graphics);
        DrawableColor normalC = this.idleBarColor.get();
        DrawableColor hoverC = this.hoverBarColor.get();
        if (this.isGrabberHovered() || this.isGrabberGrabbed()) {
            if (this.hoverBarTexture != null) {
                blitF(graphics, this.hoverBarTexture, x, y, 0.0F, 0.0F, this.grabberWidth, this.grabberHeight, this.grabberWidth, this.grabberHeight);
            } else if (hoverC != null) {
                if (this.roundedGrabber) {
                    float radius = UIBase.getWidgetCornerRoundingRadius();
                    renderRoundedRect(graphics, x, y, this.grabberWidth, this.grabberHeight, radius, radius, radius, radius, hoverC.getColorInt());
                } else {
                    fillF(graphics, x, y, x + this.grabberWidth, y + this.grabberHeight, hoverC.getColorInt());
                }
            }
        } else {
            if (this.idleBarTexture != null) {
                blitF(graphics, this.idleBarTexture, x, y, 0.0F, 0.0F, this.grabberWidth, this.grabberHeight, this.grabberWidth, this.grabberHeight);
            } else if (normalC != null) {
                if (this.roundedGrabber) {
                    float radius = UIBase.getWidgetCornerRoundingRadius();
                    renderRoundedRect(graphics, x, y, this.grabberWidth, this.grabberHeight, radius, radius, radius, radius, normalC.getColorInt());
                } else {
                    fillF(graphics, x, y, x + this.grabberWidth, y + this.grabberHeight, normalC.getColorInt());
                }
            }
        }

    }

    /** Returns whether grabber hovered. */
    public boolean isGrabberHovered() {
        if (!this.active) return false;
        return this.grabberHovered;
    }

    /** Returns whether rounded grabber enabled. */
    public boolean isRoundedGrabberEnabled() {
        return this.roundedGrabber;
    }

    /** Sets rounded grabber enabled for this scroll bar. */
    public ScrollBar setRoundedGrabberEnabled(boolean roundedGrabber) {
        this.roundedGrabber = roundedGrabber;
        return this;
    }

    /** Returns whether mouse over grabber. */
    public boolean isMouseOverGrabber(double mouseX, double mouseY) {
        if (!this.active) return false;
        float x = this.lastGrabberX;
        float y = this.lastGrabberY;
        return ((mouseX >= x) && (mouseX <= (x + this.grabberWidth)) && (mouseY >= y) && (mouseY <= (y + this.grabberHeight)));
    }

    /** Returns whether grabber grabbed. */
    public boolean isGrabberGrabbed() {
        return this.active && this.leftMouseDownOnGrabber;
    }

    /** Returns whether mouse inside scroll area. */
    public boolean isMouseInsideScrollArea(double mouseX, double mouseY, boolean ignoreGrabber) {
        if (!this.active) {
            return false;
        }
        if (!ignoreGrabber && (this.isGrabberGrabbed() || this.isGrabberHovered())) {
            return false;
        }
        float x = this.scrollAreaStartX;
        float y = this.scrollAreaStartY;
        float width = this.scrollAreaEndX - this.scrollAreaStartX;
        float height = this.scrollAreaEndY - this.scrollAreaStartY;
        return ((mouseX >= x) && (mouseX <= (x + width)) && (mouseY >= y) && (mouseY <= (y + height)));
    }

    /** Returns scroll. */
    public float getScroll() {
        return this.scroll;
    }

    /** Sets scroll for this scroll bar. */
    public void setScroll(float scroll) {
        this.setScroll(scroll, true);
    }

    /** Sets scroll for this scroll bar. */
    public void setScroll(float scroll, boolean informScrollListeners) {
        this.scroll = Math.min(1.0F, Math.max(0.0F, scroll));
        if (informScrollListeners) {
            for (Consumer<ScrollBar> listener : this.scrollListeners) {
                listener.accept(this);
            }
        }
    }

    /** Returns grabber scroll speed. */
    public float getGrabberScrollSpeed() {
        return this.grabberScrollSpeed;
    }

    /** Sets grabber scroll speed for this scroll bar. */
    public void setGrabberScrollSpeed(float speed) {
        this.grabberScrollSpeed = Math.max(0.0F, speed);
    }

    /** Returns grabber edge inset. */
    public float getGrabberEdgeInset() {
        return this.grabberEdgeInset;
    }

    /** Sets grabber edge inset for this scroll bar. */
    public void setGrabberEdgeInset(float grabberEdgeInset) {
        this.grabberEdgeInset = Math.max(0.0F, grabberEdgeInset);
    }

    /** Returns wheel scroll speed. */
    public float getWheelScrollSpeed() {
        return this.wheelScrollSpeed;
    }

    /** Sets wheel scroll speed for this scroll bar. */
    public void setWheelScrollSpeed(float speed) {
        this.wheelScrollSpeed = Math.max(0.0F, speed);
    }

    /** Returns whether scroll wheel allowed. */
    public boolean isScrollWheelAllowed() {
        return this.allowScrollWheel;
    }

    /** Sets scroll wheel allowed for this scroll bar. */
    public void setScrollWheelAllowed(boolean allowed) {
        this.allowScrollWheel = allowed;
    }

    /** Returns direction. */
    public ScrollBarDirection getDirection() {
        return this.direction;
    }

    /** Registers scroll listener for later lookup. */
    public void registerScrollListener(Consumer<ScrollBar> listener) {
        this.scrollListeners.add(listener);
    }

    /** Routes a mouse-button press and reports whether it was consumed. */
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        return this.mouseClicked(event.x(), event.y(), event.button());
    }

    /** Routes a mouse-button press and reports whether it was consumed. */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (!this.active) {
                return false;
            }
            if (this.isGrabberHovered()) {
                this.leftMouseDownOnGrabber = true;
                this.leftMouseDownOnGrabberAtMouseX = mouseX;
                this.leftMouseDownOnGrabberAtMouseY = mouseY;
                this.leftMouseDownOnGrabberAtScroll = this.scroll;
                return true;
            }
        }
        return false;
    }

    /** Routes a mouse-button release and reports whether it was consumed. */
    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return this.mouseReleased(event.x(), event.y(), event.button());
    }

    /** Routes a mouse-button release and reports whether it was consumed. */
    public boolean mouseReleased(double mouseX, double mouseY, int button) {

        this.leftMouseDownOnGrabber = false;

        return false;

    }

    /** Routes pointer dragging and reports whether it was consumed. */
    @Override
    public boolean mouseDragged(MouseButtonEvent event, double $$3, double $$4) {
        return this.mouseDragged(event.x(), event.y(), event.button(), $$3, $$4);
    }

    /** Routes pointer dragging and reports whether it was consumed. */
    public boolean mouseDragged(double mouseX, double mouseY, int button, double $$3, double $$4) {

        if (this.leftMouseDownOnGrabber) {

            float effectiveStartX = this.scrollAreaStartX + this.grabberEdgeInset;
            float effectiveStartY = this.scrollAreaStartY + this.grabberEdgeInset;
            float effectiveEndX = Math.max(effectiveStartX, this.scrollAreaEndX - this.grabberEdgeInset);
            float effectiveEndY = Math.max(effectiveStartY, this.scrollAreaEndY - this.grabberEdgeInset);
            float usableAreaWidth = Math.max(0.0F, effectiveEndX - effectiveStartX - this.grabberWidth);
            float usableAreaHeight = Math.max(0.0F, effectiveEndY - effectiveStartY - this.grabberHeight);

            float offsetX = (float) (mouseX - this.leftMouseDownOnGrabberAtMouseX);
            float offsetY = (float) (mouseY - this.leftMouseDownOnGrabberAtMouseY);

            float scrollOffset;
            if (this.direction == ScrollBarDirection.VERTICAL) {
                scrollOffset = offsetY / usableAreaHeight;
            } else {
                scrollOffset = offsetX / usableAreaWidth;
            }
            this.setScroll(this.leftMouseDownOnGrabberAtScroll + scrollOffset);

            return true;

        }

        return false;

    }

    /** Routes wheel scrolling and reports whether it was consumed. */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {

        if (this.active && this.allowScrollWheel && this.isMouseInsideScrollArea(mouseX, mouseY, true) && !this.leftMouseDownOnGrabber) {
            float scrollOffset = 0.1F * this.wheelScrollSpeed;
            if (scrollDeltaY > 0) {
                scrollOffset = -(scrollOffset);
            }
            this.setScroll(this.getScroll() + scrollOffset);
            return true;
        }

        return false;

    }

    /** Reports whether the current pointer position lies inside this element's hitbox. */
    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (this.leftMouseDownOnGrabber) return true;
        return this.isMouseInsideScrollArea(mouseX, mouseY, true);
    }

    /** Sets focused for this scroll bar. */
    @Override
    public void setFocused(boolean var1) {
    }

    /** Reports whether keyboard focus currently targets this control. */
    @Override
    public boolean isFocused() {
        return false;
    }

    /** Returns this component's priority in the narration order. */
    @Override
    public @NotNull NarrationPriority narrationPriority() {
        return NarrationPriority.NONE;
    }

    /** Refreshes narration from current state. */
    @Override
    public void updateNarration(@NotNull NarrationElementOutput var1) {
    }

    /** Identifies one supported scroll bar option. */
    public enum ScrollBarDirection {
        /** Positions the element at horizontal. */
        HORIZONTAL,
        /** Positions the element at vertical. */
        VERTICAL
    }

}
