package de.keksuccino.konkrete.util.file.type.types;

import de.keksuccino.konkrete.util.file.type.FileCodec;
import de.keksuccino.konkrete.util.file.type.FileMediaType;
import de.keksuccino.konkrete.util.file.type.FileType;
import de.keksuccino.konkrete.util.resource.resources.audio.IAudio;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Describes audio file type metadata. */
public class AudioFileType extends FileType<IAudio> {

    /** Creates an audio descriptor with codec, optional MIME type, and normalized extensions. */
    public AudioFileType(@NotNull FileCodec<IAudio> codec, @Nullable String mimeType, @NotNull String... extensions) {
        super(codec, mimeType, FileMediaType.AUDIO, extensions);
    }

    /** {@inheritDoc} */
    @Override
    public AudioFileType addExtension(@NotNull String extension) {
        return (AudioFileType) super.addExtension(extension);
    }

    /** {@inheritDoc} */
    @Override
    public AudioFileType removeExtension(@NotNull String extension) {
        return (AudioFileType) super.removeExtension(extension);
    }

    /** {@inheritDoc} */
    @Override
    public AudioFileType setCodec(@NotNull FileCodec<IAudio> codec) {
        return (AudioFileType) super.setCodec(codec);
    }

    /** {@inheritDoc} */
    @Override
    public AudioFileType setLocationAllowed(boolean allowLocation) {
        return (AudioFileType) super.setLocationAllowed(allowLocation);
    }

    /** {@inheritDoc} */
    @Override
    public AudioFileType setLocalAllowed(boolean allowLocal) {
        return (AudioFileType) super.setLocalAllowed(allowLocal);
    }

    /** {@inheritDoc} */
    @Override
    public AudioFileType setWebAllowed(boolean allowWeb) {
        return (AudioFileType) super.setWebAllowed(allowWeb);
    }

    /** {@inheritDoc} */
    @Override
    public AudioFileType setCustomDisplayName(@Nullable Component name) {
        return (AudioFileType) super.setCustomDisplayName(name);
    }

}
