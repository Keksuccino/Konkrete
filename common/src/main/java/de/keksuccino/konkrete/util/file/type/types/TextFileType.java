package de.keksuccino.konkrete.util.file.type.types;

import de.keksuccino.konkrete.util.file.type.FileCodec;
import de.keksuccino.konkrete.util.file.type.FileMediaType;
import de.keksuccino.konkrete.util.file.type.FileType;
import de.keksuccino.konkrete.util.resource.resources.text.IText;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Describes text file type metadata. */
public class TextFileType extends FileType<IText> {

    /** Creates a text descriptor with codec, optional MIME type, and normalized extensions. */
    public TextFileType(@NotNull FileCodec<IText> codec, @Nullable String mimeType, @NotNull String... extensions) {
        super(codec, mimeType, FileMediaType.TEXT, extensions);
    }

    /** {@inheritDoc} */
    @Override
    public TextFileType addExtension(@NotNull String extension) {
        return (TextFileType) super.addExtension(extension);
    }

    /** {@inheritDoc} */
    @Override
    public TextFileType removeExtension(@NotNull String extension) {
        return (TextFileType) super.removeExtension(extension);
    }

    /** {@inheritDoc} */
    @Override
    public TextFileType setCodec(@NotNull FileCodec<IText> codec) {
        return (TextFileType) super.setCodec(codec);
    }

    /** {@inheritDoc} */
    @Override
    public TextFileType setLocationAllowed(boolean allowLocation) {
        return (TextFileType) super.setLocationAllowed(allowLocation);
    }

    /** {@inheritDoc} */
    @Override
    public TextFileType setLocalAllowed(boolean allowLocal) {
        return (TextFileType) super.setLocalAllowed(allowLocal);
    }

    /** {@inheritDoc} */
    @Override
    public TextFileType setWebAllowed(boolean allowWeb) {
        return (TextFileType) super.setWebAllowed(allowWeb);
    }

    /** {@inheritDoc} */
    @Override
    public TextFileType setCustomDisplayName(@Nullable Component name) {
        return (TextFileType) super.setCustomDisplayName(name);
    }

}
