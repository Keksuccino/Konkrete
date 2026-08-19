package de.keksuccino.konkrete.util.resource.resources.video;

import de.keksuccino.konkrete.util.file.type.types.FileTypes;
import de.keksuccino.konkrete.util.file.type.types.VideoFileType;
import de.keksuccino.konkrete.util.resource.ResourceHandler;
import de.keksuccino.konkrete.util.resource.ResourceHandlers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;

/**
 * It is not recommended to make direct calls to this class!<br>
 * Use {@link ResourceHandlers#getVideoHandler()} instead.
 */
public class VideoResourceHandler extends ResourceHandler<IVideo, VideoFileType> {

    /**
     * It is not recommended to make direct calls to this instance!<br>
     * Use {@link ResourceHandlers#getVideoHandler()} instead.
     */
    public static final VideoResourceHandler INSTANCE = new VideoResourceHandler();

    /** Returns the allowed file types used by this video resource instance. */
    @Override
    public @NotNull List<VideoFileType> getAllowedFileTypes() {
        return FileTypes.getAllVideoFileTypes();
    }

    /** Returns the fallback file type, or {@code null} when it is not available. */
    @Override
    public @Nullable VideoFileType getFallbackFileType() {
        return null;
    }

}
