package de.keksuccino.konkrete.util.resource;

import de.keksuccino.konkrete.util.rendering.AspectRatio;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Exposes dimensions and a borrowed or owned texture identifier for a closeable resource. */
public interface RenderableResource extends Resource {

    /** Fallback identifier shown when a renderable resource has no decoded texture. */
    public static final Identifier MISSING_TEXTURE_LOCATION = TextureManager.INTENTIONAL_MISSING_TEXTURE;
    /** Builds the namespace and path for the resource runtime. */
    public static final Identifier FULLY_TRANSPARENT_TEXTURE = Identifier.fromNamespaceAndPath("konkrete", "textures/fully_transparent.png");

    /**
     * Some resource types asynchronously update their current {@link Identifier},
     * so make sure to always cache the location returned by this method before using it.
     */
    @Nullable Identifier getResourceLocation();

    /** Returns the current render width in pixels. */
    int getWidth();

    /** Returns the current render height in pixels. */
    int getHeight();

    /** Returns the current render aspect ratio. */
    @NotNull AspectRatio getAspectRatio();

    /** Resets playback or frame state to the resource's initial visual. */
    void reset();

}
