package de.keksuccino.konkrete.util.resource;

import de.keksuccino.konkrete.util.ListUtils;
import de.keksuccino.konkrete.util.file.type.FileType;
import de.keksuccino.konkrete.util.file.type.types.*;
import de.keksuccino.konkrete.util.resource.resources.audio.AudioResourceHandler;
import de.keksuccino.konkrete.util.resource.resources.audio.IAudio;
import de.keksuccino.konkrete.util.resource.resources.text.IText;
import de.keksuccino.konkrete.util.resource.resources.text.TextResourceHandler;
import de.keksuccino.konkrete.util.resource.resources.texture.ITexture;
import de.keksuccino.konkrete.util.resource.resources.texture.ImageResourceHandler;
import de.keksuccino.konkrete.util.resource.resources.video.IVideo;
import de.keksuccino.konkrete.util.resource.resources.video.VideoResourceHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/** Owns the process-wide resource-handler registry and coordinates release and shutdown across media types. */
@SuppressWarnings("unused")
public class ResourceHandlers {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final AtomicBoolean SHUTDOWN_STARTED = new AtomicBoolean();

    /** Process-wide image handler used for subsequent dispatch. */
    @NotNull
    protected static ResourceHandler<ITexture, ImageFileType> imageHandler = ImageResourceHandler.INSTANCE;
    /** Process-wide audio handler used for subsequent dispatch. */
    @NotNull
    protected static ResourceHandler<IAudio, AudioFileType> audioHandler = AudioResourceHandler.INSTANCE;
    /** Process-wide video handler used for subsequent dispatch. */
    @NotNull
    protected static ResourceHandler<IVideo, VideoFileType> videoHandler = VideoResourceHandler.INSTANCE;
    /** Process-wide text handler used for subsequent dispatch. */
    @NotNull
    protected static ResourceHandler<IText, TextFileType> textHandler = TextResourceHandler.INSTANCE;

    /** Returns the image handler used by this resource runtime instance. */
    @NotNull
    public static ResourceHandler<ITexture, ImageFileType> getImageHandler() {
        return imageHandler;
    }

    /** Sets the image handler used by subsequent resource runtime operations. */
    public static void setImageHandler(@NotNull ResourceHandler<ITexture, ImageFileType> imageHandler) {
        ResourceHandlers.imageHandler = Objects.requireNonNull(imageHandler);
    }

    /** Returns the audio handler used by this resource runtime instance. */
    @NotNull
    public static ResourceHandler<IAudio, AudioFileType> getAudioHandler() {
        return audioHandler;
    }

    /** Sets the audio handler used by subsequent resource runtime operations. */
    public static void setAudioHandler(@NotNull ResourceHandler<IAudio, AudioFileType> audioHandler) {
        ResourceHandlers.audioHandler = Objects.requireNonNull(audioHandler);
    }

    /** Returns the video handler used by this resource runtime instance. */
    @NotNull
    public static ResourceHandler<IVideo, VideoFileType> getVideoHandler() {
        return videoHandler;
    }

    /** Sets the video handler used by subsequent resource runtime operations. */
    public static void setVideoHandler(@NotNull ResourceHandler<IVideo, VideoFileType> videoHandler) {
        ResourceHandlers.videoHandler = Objects.requireNonNull(videoHandler);
    }

    /** Returns the text handler used by this resource runtime instance. */
    @NotNull
    public static ResourceHandler<IText, TextFileType> getTextHandler() {
        return textHandler;
    }

    /** Sets the text handler used by subsequent resource runtime operations. */
    public static void setTextHandler(@NotNull ResourceHandler<IText, TextFileType> textHandler) {
        ResourceHandlers.textHandler = Objects.requireNonNull(textHandler);
    }

    /** Returns a new ordered list containing the current image, audio, video, and text handlers. */
    @NotNull
    public static List<ResourceHandler<?,?>> getHandlers() {
        return ListUtils.of(imageHandler, audioHandler, videoHandler, textHandler);
    }

    /** Resolves the handler for source for the resource runtime. */
    @Nullable
    public static ResourceHandler<?,?> findHandlerForSource(@NotNull ResourceSource source, boolean doAdvancedWebChecks) {
        FileType<?> type = FileTypes.getType(source, doAdvancedWebChecks);
        if (type instanceof ImageFileType) return getImageHandler();
        if (type instanceof AudioFileType) return getAudioHandler();
        if (type instanceof VideoFileType) return getVideoHandler();
        if (type instanceof TextFileType) return getTextHandler();
        return null;
    }

    /** Reloads all owned resource runtime instances. */
    public static void reloadAll() {
        if (SHUTDOWN_STARTED.get()) return;
        LOGGER.info("[KONKRETE] Reloading resources..");
        getHandlers().forEach(ResourceHandler::releaseAll);
    }

    /** Shuts down all owned resource runtime instances and rejects late work. */
    public static void shutdownAll() {
        if (!SHUTDOWN_STARTED.compareAndSet(false, true)) return;
        ResourceRuntime.markShuttingDown();
        LOGGER.info("[KONKRETE] Releasing all resources during client shutdown..");
        for (ResourceHandler<?, ?> handler : getHandlers()) {
            try {
                handler.shutdown();
            } catch (Throwable throwable) {
                LOGGER.error("[KONKRETE] Failed to release resources from handler {} during client shutdown!", handler.getClass().getName(), throwable);
            }
        }
    }

}
