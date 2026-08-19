package de.keksuccino.konkrete.util.file.type.types;

import de.keksuccino.konkrete.util.file.type.FileCodec;
import de.keksuccino.konkrete.util.file.type.FileMediaType;
import de.keksuccino.konkrete.util.file.type.FileType;
import de.keksuccino.konkrete.util.resource.resources.video.IVideo;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Describes video file type metadata. */
public class VideoFileType extends FileType<IVideo> {

    /** Creates a video descriptor with codec, optional MIME type, and normalized extensions. */
    public VideoFileType(@NotNull FileCodec<IVideo> codec, @Nullable String mimeType, @NotNull String... extensions) {
        super(codec, mimeType, FileMediaType.VIDEO, extensions);
    }

    /** {@inheritDoc} */
    @Override
    public VideoFileType addExtension(@NotNull String extension) {
        return (VideoFileType) super.addExtension(extension);
    }

    /** {@inheritDoc} */
    @Override
    public VideoFileType removeExtension(@NotNull String extension) {
        return (VideoFileType) super.removeExtension(extension);
    }

    /** {@inheritDoc} */
    @Override
    public VideoFileType setCodec(@NotNull FileCodec<IVideo> codec) {
        return (VideoFileType) super.setCodec(codec);
    }

    /** {@inheritDoc} */
    @Override
    public VideoFileType setLocationAllowed(boolean allowLocation) {
        return (VideoFileType) super.setLocationAllowed(allowLocation);
    }

    /** {@inheritDoc} */
    @Override
    public VideoFileType setLocalAllowed(boolean allowLocal) {
        return (VideoFileType) super.setLocalAllowed(allowLocal);
    }

    /** {@inheritDoc} */
    @Override
    public VideoFileType setWebAllowed(boolean allowWeb) {
        return (VideoFileType) super.setWebAllowed(allowWeb);
    }

    /** {@inheritDoc} */
    @Override
    public VideoFileType setCustomDisplayName(@Nullable Component name) {
        return (VideoFileType) super.setCustomDisplayName(name);
    }

}
