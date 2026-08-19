package de.keksuccino.konkrete.util.rendering.ui.toast;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.konkrete.util.resource.ResourceSupplier;
import de.keksuccino.konkrete.util.resource.resources.texture.ITexture;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Displays a timed title, message, and optional icon through Minecraft's toast system. */
public class SimpleToast implements Toast {

    /** Vanilla sprite used when no custom background is configured. */
    protected static final Identifier BACKGROUND_SPRITE = Identifier.withDefaultNamespace("toast/tutorial");

    /** Width in GUI units for progress bar. */
    public static final int PROGRESS_BAR_WIDTH = 154;
    /** Height in GUI units for progress bar. */
    public static final int PROGRESS_BAR_HEIGHT = 1;
    /** Horizontal GUI coordinate for progress bar. */
    public static final int PROGRESS_BAR_X = 3;
    /** Vertical GUI coordinate for progress bar. */
    public static final int PROGRESS_BAR_Y = 28;

    /** Icon drawn at the leading edge of the toast. */
    @NotNull
    protected final Icon icon;
    /** Primary toast text. */
    @NotNull
    protected final Component title;
    /** Optional secondary toast text. */
    @Nullable
    protected final Component message;
    /** Visibility returned to Minecraft for the current toast frame. */
    protected Toast.Visibility visibility;
    /** Millisecond timestamp of the previous progress update. */
    protected long lastProgressTime;
    /** Last progress fraction in the range 0.0 through 1.0. */
    protected float lastProgress;
    /** Progress fraction in the range 0.0 through 1.0. */
    protected float progress;
    /** Whether the toast exposes determinate progress. */
    protected final boolean progressable;
    /** Optional texture replacing the vanilla toast background. */
    @Nullable
    protected ResourceSupplier<ITexture> customBackground;
    int width = 160;
    int height = 32;

    /** Builds a toast from its icon, title, optional detail text, and progress mode. */
    public SimpleToast(@NotNull Icon icon, @NotNull Component title, @Nullable Component message, boolean progressable) {
        this.visibility = Visibility.SHOW;
        this.icon = icon;
        this.title = title;
        this.message = message;
        this.progressable = progressable;
    }

    /** Returns wanted visibility. */
    @NotNull
    @Override
    public Visibility getWantedVisibility() {
        return this.visibility;
    }

    /** Refreshes this simple toast from current state. */
    @Override
    public void update(@NotNull ToastManager toastManager, long visibilityTime) {
    }

    /** Adds this component's complete draw state to the active GUI extraction pass. */
    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, @NotNull Font font, long visibilityTime) {

        Identifier customBack = this.getCustomBackground();
        if (customBack == null) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND_SPRITE, 0, 0, this.width(), this.height());
        } else {
            graphics.blit(RenderPipelines.GUI_TEXTURED, customBack, 0, 0, 0.0F, 0.0F, this.width(), this.height(), this.width(), this.height());
        }

        this.icon.extractRenderState(graphics, 6, 6);

        if (this.message == null) {
            graphics.text(font, this.title, 30, 12, -11534256, false);
        } else {
            graphics.text(font, this.title, 30, 7, -11534256, false);
            graphics.text(font, this.message, 30, 18, -16777216, false);
        }

        if (this.progressable) {
            graphics.fill(PROGRESS_BAR_X, PROGRESS_BAR_Y, PROGRESS_BAR_X + PROGRESS_BAR_WIDTH, PROGRESS_BAR_Y + PROGRESS_BAR_HEIGHT, -1);
            float clampProgress = Mth.clampedLerp(this.lastProgress, this.progress, (float)(visibilityTime - this.lastProgressTime) / 100.0F);
            int progressColor;
            if (this.progress >= this.lastProgress) {
                progressColor = -16755456;
            } else {
                progressColor = -11206656;
            }
            graphics.fill(PROGRESS_BAR_X, PROGRESS_BAR_Y, (int)((float)PROGRESS_BAR_X + (float)PROGRESS_BAR_WIDTH * clampProgress), PROGRESS_BAR_Y + PROGRESS_BAR_HEIGHT, progressColor);
            this.lastProgress = clampProgress;
            this.lastProgressTime = visibilityTime;
        }

    }

    /** Returns the visual width in GUI units. */
    @Override
    public int width() {
        return this.width;
    }

    /** Returns the visual height in GUI units. */
    @Override
    public int height() {
        return this.height;
    }

    /** Sets height for this simple toast. */
    @NotNull
    public SimpleToast setHeight(int height) {
        this.height = height;
        return this;
    }

    /** Sets width for this simple toast. */
    @NotNull
    public SimpleToast setWidth(int width) {
        this.width = width;
        return this;
    }

    /** Returns custom background. */
    @Nullable
    protected Identifier getCustomBackground() {
        if (this.customBackground != null) {
            ITexture tex = this.customBackground.get();
            if (tex != null) {
                return tex.getResourceLocation();
            }
        }
        return null;
    }

    /** Sets custom background for this simple toast. */
    @NotNull
    public SimpleToast setCustomBackground(@Nullable ResourceSupplier<ITexture> texture) {
        this.customBackground = texture;
        return this;
    }

    /** Closes this simple toast. */
    public void hide() {
        this.visibility = Visibility.HIDE;
    }

    /** Refreshes progress from current state. */
    public void updateProgress(float progress) {
        this.progress = progress;
    }

    /** Stores and renders icon icon glyph data. */
    public static class Icon {

        /** Texture location loaded by this resource supplier. */
        protected Identifier location;
        /** Loads the texture for {@link #location}. */
        protected ResourceSupplier<ITexture> supplier;

        /**
         * A 20x20 pixels icon texture for displaying in the toast.
         */
        public Icon(@NotNull Identifier textureLocation) {
            this.location = textureLocation;
        }

        /**
         * A 20x20 pixels icon texture for displaying in the toast.
         */
        public Icon(@NotNull ResourceSupplier<ITexture> textureSupplier) {
            this.supplier = textureSupplier;
        }

        /** Adds this component's complete draw state to the active GUI extraction pass. */
        public void extractRenderState(GuiGraphicsExtractor graphics, int x, int y) {
            Identifier icon = this.getIcon();
            if (icon != null) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, icon, x, y, 0.0F, 0.0F, 20, 20, 20, 20);
            }
        }

        /** Resolves the explicit texture or loaded supplier texture; returns {@code null} while unavailable. */
        @Nullable
        protected Identifier getIcon() {
            if (this.location != null) return this.location;
            if (this.supplier != null) {
                ITexture tex = this.supplier.get();
                if (tex != null) {
                    return tex.getResourceLocation();
                }
            }
            return null;
        }

    }

}
