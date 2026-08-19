package de.keksuccino.konkrete.util.resource.resources.audio;

import de.keksuccino.konkrete.util.file.type.types.AudioFileType;
import de.keksuccino.konkrete.util.file.type.types.FileTypes;
import de.keksuccino.konkrete.util.resource.ResourceHandler;
import de.keksuccino.konkrete.util.resource.ResourceHandlers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;

/**
 * It is not recommended to make direct calls to this class!<br>
 * Use {@link ResourceHandlers#getAudioHandler()} instead.
 */
public class AudioResourceHandler extends ResourceHandler<IAudio, AudioFileType> {

    /**
     * It is not recommended to make direct calls to this instance!<br>
     * Use {@link ResourceHandlers#getAudioHandler()} instead.
     */
    public static final AudioResourceHandler INSTANCE = new AudioResourceHandler();

    /** Returns the allowed file types used by this audio resource instance. */
    @Override
    public @NotNull List<AudioFileType> getAllowedFileTypes() {
        return FileTypes.getAllAudioFileTypes();
    }

    /** Returns the fallback file type, or {@code null} when it is not available. */
    @Override
    public @Nullable AudioFileType getFallbackFileType() {
        return null;
    }

}
