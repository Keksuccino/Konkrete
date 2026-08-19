package de.keksuccino.konkrete.util.rendering.ui.icon;

import de.keksuccino.konkrete.util.rendering.AspectRatio;
import de.keksuccino.konkrete.util.resource.resources.texture.ITexture;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.InputStream;
import java.util.Objects;

/** Rasterizes one Material Icons glyph into a lazily uploaded GUI texture. */
public class MaterialIconTexture implements ITexture {

    /** Material glyph rasterized by this texture. */
    @NotNull
    protected final MaterialIcon icon;
    /** Width in GUI units for render. */
    protected float renderWidth;
    /** Height in GUI units for render. */
    protected float renderHeight;
    /** GUI-to-pixel multiplier used to choose a raster size. */
    protected float renderScale;
    /** Whether the generated texture has been released. */
    protected boolean closed = false;

    /** Binds this dynamic texture wrapper to one Material icon. */
    public MaterialIconTexture(@NotNull MaterialIcon icon) {
        this.icon = Objects.requireNonNull(icon, "icon");
    }

    /**
     * It's important to call this before getting anything from the texture (width, height, resource location, etc.)!
     */
    public MaterialIconTexture updateRenderContext(float renderWidth, float renderHeight, float renderScale) {
        this.renderWidth = renderWidth;
        this.renderHeight = renderHeight;
        this.renderScale = renderScale;
        return this;
    }

    /** Returns the Material glyph rasterized by this texture. */
    @NotNull
    public MaterialIcon getIcon() {
        return this.icon;
    }

    /** Returns the current width in GUI units. */
    @Override
    public int getWidth() {
        int size = calculateBestTextureSize(renderWidth, renderHeight, renderScale);
        return this.icon.getWidth(size);
    }

    /** Returns the current height in GUI units. */
    @Override
    public int getHeight() {
        int size = calculateBestTextureSize(renderWidth, renderHeight, renderScale);
        return this.icon.getHeight(size);
    }

    /** Computes best texture size from the supplied inputs. */
    public int calculateBestTextureSize(float renderWidth, float renderHeight, float renderScale) {
        return this.icon.calculateBestTextureSize(renderWidth, renderHeight, renderScale);
    }

    /** Returns resource location. */
    @Override
    public @Nullable Identifier getResourceLocation() {
        if (this.closed) {
            return null;
        }
        return this.icon.getTextureLocation(this.renderWidth, this.renderHeight, this.renderScale);
    }

    /** Returns aspect ratio. */
    @Override
    public @NotNull AspectRatio getAspectRatio() {
        return new AspectRatio(getWidth(), getHeight());
    }

    /** Makes this material icon texture visible. */
    @Override
    public @Nullable InputStream open() {
        return null;
    }

    /** Returns whether ready. */
    @Override
    public boolean isReady() {
        if (this.closed) {
            return false;
        }
        int size = this.calculateBestTextureSize(renderWidth, renderHeight, renderScale);
        return this.icon.isLoaded(size) && !this.icon.isFailed(size);
    }

    /** Returns whether loading completed. */
    @Override
    public boolean isLoadingCompleted() {
        if (this.closed) {
            return false;
        }
        int size = this.calculateBestTextureSize(renderWidth, renderHeight, renderScale);
        return this.icon.isLoaded(size) && !this.icon.isFailed(size);
    }

    /** Returns whether loading failed. */
    @Override
    public boolean isLoadingFailed() {
        if (this.closed) {
            return false;
        }
        int size = this.calculateBestTextureSize(renderWidth, renderHeight, renderScale);
        return this.icon.isFailed(size);
    }

    /** Restores this material icon texture to its initial state. */
    @Override
    public void reset() {
    }

    /** Returns whether closed. */
    @Override
    public boolean isClosed() {
        return this.closed;
    }

    /** Closes this instance and releases its native or GPU resources. */
    @Override
    public void close() {
        this.closed = true;
    }

}
