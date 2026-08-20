package de.keksuccino.konkrete.util.rendering.ui.widget;

import de.keksuccino.konkrete.util.rendering.ui.widget.slider.UIWidget;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Displays and routes input for renderer. */
public class RendererWidget extends AbstractWidget implements UniqueWidget, NavigatableWidget, UIWidget {

    /** Visual body used by this control. */
    @NotNull
    protected RendererWidgetBody body;
    /** Optional stable identifier exposed through {@link UniqueWidget}. */
    @Nullable
    protected String identifier;

    /** Positions a callback-rendered widget within the supplied GUI bounds. */
    public RendererWidget(int x, int y, int width, int height, @NotNull RendererWidgetBody body) {
        super(x, y, width, height, Component.empty());
        this.body = body;
    }

    /** Adds this widget's draw state to the active GUI extraction pass. */
    @Override
    protected void extractWidgetRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        this.body.extractRenderState(graphics, mouseX, mouseY, partial, this.getX(), this.getY(), this.getWidth(), this.getHeight(), this);
    }

    /** Publishes this widget's current narration data. */
    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput var1) {
    }

    /** Sets body for this renderer widget. */
    public RendererWidget setBody(@NotNull RendererWidgetBody body) {
        this.body = body;
        return this;
    }

    /** Returns the optional stable identifier used for widget lookup. */
    @Override
    @Nullable
    public String getWidgetIdentifierKonkrete() {
        return this.identifier;
    }

    /** Sets widget identifier for this renderer widget. */
    @Override
    public RendererWidget setWidgetIdentifierKonkrete(@Nullable String identifier) {
        this.identifier = identifier;
        return this;
    }

    /** Reports whether this control may receive keyboard focus. */
    @Override
    public boolean isFocusable() {
        return false;
    }

    /** Sets focusable for this renderer widget. */
    @Override
    public void setFocusable(boolean focusable) {
        throw new RuntimeException("RendererWidgets are not focusable!");
    }

    /** Reports whether keyboard navigation may target this control. */
    @Override
    public boolean isNavigatable() {
        return false;
    }

    /** Sets navigatable for this renderer widget. */
    @Override
    public void setNavigatable(boolean navigatable) {
        throw new RuntimeException("RendererWidgets are not navigatable!");
    }

    /** Plays down sound through Minecraft's sound system. */
    @Override
    public void playDownSound(@NotNull SoundManager $$0) {
        //no click sound
    }

    /** Returns alpha. */
    public float getAlpha() {
        return this.alpha;
    }

    /** Supplies the custom draw state hosted by a renderer widget. */
    @FunctionalInterface
    public interface RendererWidgetBody {

        /** Queues the body's draw state inside the widget's current bounds. */
        void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial, int x, int y, int width, int height, @NotNull RendererWidget renderer);

    }

}
