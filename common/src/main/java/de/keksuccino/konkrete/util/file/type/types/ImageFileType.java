package de.keksuccino.konkrete.util.file.type.types;

import de.keksuccino.konkrete.util.file.type.FileCodec;
import de.keksuccino.konkrete.util.file.type.FileMediaType;
import de.keksuccino.konkrete.util.file.type.FileType;
import de.keksuccino.konkrete.util.resource.resources.texture.ITexture;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Describes image file type metadata. */
public class ImageFileType extends FileType<ITexture> {

    /** Whether matching images may contain multiple frames. */
    protected boolean animated = false;

    /** Creates an image descriptor with codec, optional MIME type, and normalized extensions. */
    public ImageFileType(@NotNull FileCodec<ITexture> codec, @Nullable String mimeType, @NotNull String... extensions) {
        super(codec, mimeType, FileMediaType.IMAGE, extensions);
    }

    /** Returns whether matching resources may contain multiple frames. */
    public boolean isAnimated() {
        return this.animated;
    }

    /** Configures multi-frame support and returns this descriptor. */
    public ImageFileType setAnimated(boolean animated) {
        this.animated = animated;
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public ImageFileType addExtension(@NotNull String extension) {
        return (ImageFileType) super.addExtension(extension);
    }

    /** {@inheritDoc} */
    @Override
    public ImageFileType removeExtension(@NotNull String extension) {
        return (ImageFileType) super.removeExtension(extension);
    }

    /** {@inheritDoc} */
    @Override
    public ImageFileType setCodec(@NotNull FileCodec<ITexture> codec) {
        return (ImageFileType) super.setCodec(codec);
    }

    /** {@inheritDoc} */
    @Override
    public ImageFileType setLocationAllowed(boolean allowLocation) {
        return (ImageFileType) super.setLocationAllowed(allowLocation);
    }

    /** {@inheritDoc} */
    @Override
    public ImageFileType setLocalAllowed(boolean allowLocal) {
        return (ImageFileType) super.setLocalAllowed(allowLocal);
    }

    /** {@inheritDoc} */
    @Override
    public ImageFileType setWebAllowed(boolean allowWeb) {
        return (ImageFileType) super.setWebAllowed(allowWeb);
    }

    /** {@inheritDoc} */
    @Override
    public ImageFileType setCustomDisplayName(@Nullable Component name) {
        return (ImageFileType) super.setCustomDisplayName(name);
    }

}
