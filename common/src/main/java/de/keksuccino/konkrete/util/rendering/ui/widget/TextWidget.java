package de.keksuccino.konkrete.util.rendering.ui.widget;

import de.keksuccino.konkrete.util.rendering.DrawableColor;
import de.keksuccino.konkrete.util.rendering.RenderingUtils;
import de.keksuccino.konkrete.util.rendering.ui.UIBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import net.minecraft.util.StringDecomposer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Displays and routes input for text. */
public class TextWidget extends AbstractWidget implements UniqueWidget, NavigatableWidget, UIWidget {

    /** Optional stable identifier exposed through {@link UniqueWidget}. */
    @Nullable
    protected String widgetIdentifier;
    /** Horizontal alignment applied inside the widget bounds. */
    @NotNull
    protected TextAlignment alignment = TextAlignment.LEFT;
    /** Base text color before style or opacity adjustments. */
    @NotNull
    protected DrawableColor baseColor = DrawableColor.WHITE;
    /** Whether text or geometry is drawn with a shadow. */
    protected boolean shadow = true;
    /** Font used to measure and draw the text. */
    @NotNull
    protected Font font;
    /** Text-size multiplier applied during measurement and drawing. */
    protected float scale = 1.0F;
    /** Whether to use UI-scaled text metrics. */
    protected boolean forUI = false;

    /** Returns an empty value with no configured content. */
    @NotNull
    public static TextWidget empty(int x, int y, int width) {
        return new TextWidget(x, y, width, 9, Minecraft.getInstance().font, Component.empty());
    }

    /** Creates a component-backed text widget within the supplied bounds. */
    @NotNull
    public static TextWidget of(@NotNull Component text, int x, int y, int width) {
        return new TextWidget(x, y, width, 9, Minecraft.getInstance().font, text);
    }

    /** Creates a literal-text widget within the supplied bounds. */
    @NotNull
    public static TextWidget of(@NotNull String text, int x, int y, int width) {
        return of(Component.literal(text), x, y, width);
    }

    /** Positions component text with the supplied font inside fixed GUI bounds. */
    public TextWidget(int x, int y, int width, int height, @NotNull Font font, @NotNull Component text) {
        super(x, y, width, height, text);
        this.font = font;
        this.updateIntrinsicSize();
    }

    /** Adds this widget's draw state to the active GUI extraction pass. */
    @Override
    public void extractWidgetRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        double drawX = this.getRenderX();
        double drawY = this.getRenderY();
        float currentScale = this.scale;
        RenderingUtils.resetShaderColor(graphics);
        if (this.forUI && !UIBase.shouldUseMinecraftFontForUIRendering()) {
            UIBase.renderText(graphics, this.getMessage(), (float) drawX, (float) drawY, this.baseColor.getColorInt(), this.resolveUITextSize());
        } else {
            graphics.pose().pushMatrix();
            if (currentScale != 1.0F) {
                graphics.pose().scale(currentScale, currentScale);
                drawX /= currentScale;
                drawY /= currentScale;
            }
            if (this.forUI) {
                UIBase.renderText(graphics, this.getMessage(), (float) drawX, (float) drawY, this.baseColor.getColorInt(), UIBase.getUITextSizeNormal());
            } else {
                graphics.text(this.font, this.getMessage(), Mth.floor(drawX), Mth.floor(drawY), this.baseColor.getColorInt(), this.shadow);
            }
            graphics.pose().popMatrix();
        }
        RenderingUtils.resetShaderColor(graphics);
    }

    /** Returns the measured text width in GUI units. */
    public int getTextWidth() {
        return Mth.ceil(this.getScaledTextWidth());
    }

    /** Returns scaled text width. */
    public double getScaledTextWidth() {
        if (this.forUI) {
            if (UIBase.shouldUseMinecraftFontForUIRendering()) {
                return UIBase.getUITextWidthNormal(this.getMessage()) * this.scale;
            }
            return UIBase.getUITextWidth(this.getMessage(), this.resolveUITextSize());
        }
        return this.font.width(this.getMessage().getVisualOrderText()) * this.scale;
    }

    /** Returns scaled text height. */
    public double getScaledTextHeight() {
        if (this.forUI) {
            if (UIBase.shouldUseMinecraftFontForUIRendering()) {
                return UIBase.getUITextHeightNormal() * this.scale;
            }
            return UIBase.getUITextHeight(this.resolveUITextSize());
        }
        return this.font.lineHeight * this.scale;
    }

    /** Returns render x. */
    public double getRenderX() {
        double textWidth = this.getScaledTextWidth();
        double x = this.getX();
        if (this.alignment == TextAlignment.CENTER) {
            x = this.getX() + (this.getWidth() / 2.0) - (textWidth / 2.0);
        } else if (this.alignment == TextAlignment.RIGHT) {
            x = this.getX() + this.getWidth() - textWidth;
        }
        return x;
    }

    /** Returns render y. */
    public double getRenderY() {
        return this.getY();
    }

    /** Returns the current render-scale multiplier. */
    public float getScale() {
        return this.scale;
    }

    /** Sets scale for this text widget. */
    public TextWidget setScale(float scale) {
        this.scale = Math.max(0.0001F, scale);
        this.updateIntrinsicSize();
        return this;
    }

    /** Returns whether for UI. */
    public boolean isForUI() {
        return this.forUI;
    }

    /** Sets for UI for this text widget. */
    public TextWidget setForUI(boolean forUI) {
        this.forUI = forUI;
        this.updateIntrinsicSize();
        return this;
    }

    /** Returns text alignment. */
    public @NotNull TextAlignment getTextAlignment() {
        return this.alignment;
    }

    /** Sets text alignment for this text widget. */
    public TextWidget setTextAlignment(@NotNull TextAlignment alignment) {
        this.alignment = alignment;
        return this;
    }

    /** Returns the base color resolved before state-specific styling. */
    public @NotNull DrawableColor getBaseColor() {
        return this.baseColor;
    }

    /** Sets base color for this text widget. */
    public TextWidget setBaseColor(@NotNull DrawableColor baseColor) {
        this.baseColor = baseColor;
        return this;
    }

    /** Returns whether shadow enabled. */
    public boolean isShadowEnabled() {
        return this.shadow;
    }

    /** Sets shadow enabled for this text widget. */
    public TextWidget setShadowEnabled(boolean enabled) {
        this.shadow = enabled;
        return this;
    }

    /** Returns the font used for measurement and drawing. */
    public @NotNull Font getFont() {
        return this.font;
    }

    /** Sets font for this text widget. */
    public TextWidget setFont(@NotNull Font font) {
        this.font = font;
        this.updateIntrinsicSize();
        return this;
    }

    /** Centers this widget around the supplied GUI position. */
    public TextWidget centerWidget(@NotNull Screen parent) {
        this.setX((parent.width / 2) - (this.getWidth() / 2));
        return this;
    }

    /** Returns whether text hovered. */
    public boolean isTextHovered(double mouseX, double mouseY) {
        double x = this.getRenderX();
        double y = this.getRenderY();
        double width = this.getScaledTextWidth();
        double height = this.getScaledTextHeight();
        return mouseX >= x && mouseX <= (x + width) && mouseY >= y && mouseY <= (y + height);
    }

    /** Returns style at mouse x. */
    @Nullable
    public Style getStyleAtMouseX(double mouseX) {
        double left = this.getRenderX();
        double right = left + this.getScaledTextWidth();
        if (mouseX < left || mouseX > right) {
            return null;
        }
        float safeScale = Math.max(0.0001F, this.scale);
        double relative = (mouseX - left) / safeScale;
        final float[] width = {0.0F};
        final Style[] hoveredStyle = {null};
        StringDecomposer.iterateFormatted(this.getMessage(), Style.EMPTY, (index, style, codePoint) -> {
            float glyphWidth = this.font.width(FormattedText.of(new String(Character.toChars(codePoint)), style));
            if (width[0] + glyphWidth >= relative) {
                hoveredStyle[0] = style;
                return false;
            }
            width[0] += glyphWidth;
            return true;
        });
        return hoveredStyle[0];
    }

    /** Sets widget identifier for this text widget. */
    @Override
    public TextWidget setWidgetIdentifierKonkrete(@Nullable String identifier) {
        this.widgetIdentifier = identifier;
        return this;
    }

    /** Returns the optional stable identifier used for widget lookup. */
    @Nullable
    @Override
    public String getWidgetIdentifierKonkrete() {
        return this.widgetIdentifier;
    }

    /** Publishes this widget's current narration data. */
    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput var1) {
    }

    /** Reports whether this control may receive keyboard focus. */
    @Override
    public boolean isFocusable() {
        return false;
    }

    /** Sets focusable for this text widget. */
    @Override
    public void setFocusable(boolean focusable) {
        throw new RuntimeException("TextWidgets are not focusable!");
    }

    /** Reports whether keyboard navigation may target this control. */
    @Override
    public boolean isNavigatable() {
        return false;
    }

    /** Sets navigatable for this text widget. */
    @Override
    public void setNavigatable(boolean navigatable) {
        throw new RuntimeException("TextWidgets are not navigatable!");
    }

    /** Plays down sound through Minecraft's sound system. */
    @Override
    public void playDownSound(@NotNull SoundManager $$0) {
        //no click sound
    }

    /** Refreshes intrinsic size from current state. */
    protected void updateIntrinsicSize() {
        this.height = Math.max(1, Mth.ceil(this.getScaledTextHeight()));
    }

    /** Computes UI text size from the supplied inputs. */
    protected float resolveUITextSize() {
        return UIBase.getUITextSizeNormal() * this.scale;
    }

    /** Identifies one supported text option. */
    public enum TextAlignment {

        /** Positions the element at left. */
        LEFT,
        /** Positions the element at right. */
        RIGHT,
        /** Positions the element at center. */
        CENTER

    }

}
