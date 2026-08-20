package de.keksuccino.konkrete.util.rendering.ui.widget.button;

import de.keksuccino.konkrete.util.resource.resources.texture.ITexture;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Toggles and renders a boolean selection with optional custom state textures. */
public class CheckboxButton extends ExtendedButton {

    /** Default checkbox background while idle. */
    public static final Identifier CHECKBOX_BACKGROUND_TEXTURE_NORMAL_DEFAULT = Identifier.fromNamespaceAndPath("konkrete", "textures/widgets/checkbox/background_normal.png");
    /** Default checkbox background while hovered. */
    public static final Identifier CHECKBOX_BACKGROUND_TEXTURE_HOVER_DEFAULT = Identifier.fromNamespaceAndPath("konkrete", "textures/widgets/checkbox/background_hover.png");
    /** Default checkbox background while inactive. */
    public static final Identifier CHECKBOX_BACKGROUND_TEXTURE_INACTIVE_DEFAULT = Identifier.fromNamespaceAndPath("konkrete", "textures/widgets/checkbox/background_inactive.png");
    /** Default checkmark texture. */
    public static final Identifier CHECKBOX_CHECKMARK_TEXTURE_DEFAULT = Identifier.fromNamespaceAndPath("konkrete", "textures/widgets/checkbox/checkmark.png");

    /** Whether the checkbox is selected. */
    protected boolean checkboxState = false;
    /** Listener invoked after the checked state changes. */
    @NotNull
    protected StateChangedAction onStateChanged;
    /** Optional texture replacing the default checkmark. */
    @Nullable
    protected ITexture customCheckmarkTexture = null;
    /** Texture used for custom background normal. */
    @Nullable
    protected ITexture customBackgroundTextureNormal = null;
    /** Texture used for custom background hover. */
    @Nullable
    protected ITexture customBackgroundTextureHover = null;
    /** Texture used for custom background inactive. */
    @Nullable
    protected ITexture customBackgroundTextureInactive = null;

    /** Initializes an unchecked button with GUI bounds and a state-change callback. */
    public CheckboxButton(int x, int y, int width, int height, @NotNull StateChangedAction onStateChanged) {
        super(x, y, width, height, Component.empty(), button -> {});
        this.onStateChanged = onStateChanged;
        this.setPressAction(button -> {
            this.checkboxState = !this.checkboxState;
            onStateChanged.onStateChanged(this, checkboxState);
        });
    }

    /** Adds this component's content draw state to the active GUI extraction pass. */
    @Override
    protected void extractContents(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {

        super.extractContents(graphics, mouseX, mouseY, partial);

        if (this.checkboxState && this.isActive()) {
            this.extractCheckboxTexture(graphics, this.getCheckboxCheckmarkTexture());
        }

    }

    /** Adds this component's background draw state to the active GUI extraction pass. */
    @Override
    protected void extractBackground(@NotNull GuiGraphicsExtractor graphics, float partial) {

        this.extractCheckboxTexture(graphics, this.getCheckboxBackground());

    }

    /** Renders label text into the active GUI extraction pass. */
    @Override
    protected void renderLabelText(@NotNull GuiGraphicsExtractor graphics) {
        // do nothing
    }

    /** Extracts checkbox texture from the supplied UI state. */
    protected void extractCheckboxTexture(@NotNull GuiGraphicsExtractor graphics, @NotNull Identifier texture) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, this.getX(), this.getY(), 0.0F, 0.0F, this.getWidth(), this.getHeight(), this.getWidth(), this.getHeight(), getTextureRenderColor(this.alpha));
    }

    static int getTextureRenderColor(float alpha) {
        return ARGB.white(alpha);
    }

    /** Returns checkbox checkmark texture. */
    @NotNull
    public Identifier getCheckboxCheckmarkTexture() {
        if (this.customCheckmarkTexture != null) {
            Identifier loc = this.customCheckmarkTexture.getResourceLocation();
            if (loc != null) return loc;
        }
        return CHECKBOX_CHECKMARK_TEXTURE_DEFAULT;
    }

    /** Sets custom checkbox checkmark texture for this checkbox button. */
    public void setCustomCheckboxCheckmarkTexture(@Nullable ITexture customCheckmarkTexture) {
        this.customCheckmarkTexture = customCheckmarkTexture;
    }

    /** Sets custom background texture normal for this checkbox button. */
    public void setCustomBackgroundTextureNormal(@Nullable ITexture customBackgroundTextureNormal) {
        this.customBackgroundTextureNormal = customBackgroundTextureNormal;
    }

    /** Sets custom background texture hover for this checkbox button. */
    public void setCustomBackgroundTextureHover(@Nullable ITexture customBackgroundTextureHover) {
        this.customBackgroundTextureHover = customBackgroundTextureHover;
    }

    /** Sets custom background texture inactive for this checkbox button. */
    public void setCustomBackgroundTextureInactive(@Nullable ITexture customBackgroundTextureInactive) {
        this.customBackgroundTextureInactive = customBackgroundTextureInactive;
    }

    /** Returns checkbox background. */
    @NotNull
    public Identifier getCheckboxBackground() {
        if (!this.isActive()) {
            return this.getCheckboxBackgroundTextureInactive();
        }
        if (this.isHoveredOrFocused()) {
            return this.getCheckboxBackgroundTextureHover();
        }
        return this.getCheckboxBackgroundTextureNormal();
    }

    /** Returns checkbox background texture normal. */
    @NotNull
    public Identifier getCheckboxBackgroundTextureNormal() {
        if (this.customBackgroundTextureNormal != null) {
            Identifier loc = this.customBackgroundTextureNormal.getResourceLocation();
            if (loc != null) return loc;
        }
        return CHECKBOX_BACKGROUND_TEXTURE_NORMAL_DEFAULT;
    }

    /** Returns checkbox background texture hover. */
    @NotNull
    public Identifier getCheckboxBackgroundTextureHover() {
        if (this.customBackgroundTextureHover != null) {
            Identifier loc = this.customBackgroundTextureHover.getResourceLocation();
            if (loc != null) return loc;
        }
        return CHECKBOX_BACKGROUND_TEXTURE_HOVER_DEFAULT;
    }

    /** Returns checkbox background texture inactive. */
    @NotNull
    public Identifier getCheckboxBackgroundTextureInactive() {
        if (this.customBackgroundTextureInactive != null) {
            Identifier loc = this.customBackgroundTextureInactive.getResourceLocation();
            if (loc != null) return loc;
        }
        return CHECKBOX_BACKGROUND_TEXTURE_INACTIVE_DEFAULT;
    }

    /** Returns checkbox state. */
    public boolean getCheckboxState() {
        return checkboxState;
    }

    /** Sets checkbox state for this checkbox button. */
    public void setCheckboxState(boolean state, boolean callOnStateChanged) {
        this.checkboxState = state;
        if (callOnStateChanged) {
            this.onStateChanged.onStateChanged(this, this.checkboxState);
        }
    }

    /** Receives checkbox state changes initiated through this button. */
    @FunctionalInterface
    public interface StateChangedAction {

        /** Handles the checkbox's new selected state. */
        void onStateChanged(@NotNull CheckboxButton checkbox, boolean state);

    }

}
