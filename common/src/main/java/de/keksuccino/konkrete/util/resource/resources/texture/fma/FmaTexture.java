package de.keksuccino.konkrete.util.resource.resources.texture.fma;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.konkrete.util.CloseableUtils;
import de.keksuccino.konkrete.util.WebUtils;
import de.keksuccino.konkrete.util.input.TextValidators;
import de.keksuccino.konkrete.util.rendering.AspectRatio;
import de.keksuccino.konkrete.util.resource.PlayableResource;
import de.keksuccino.konkrete.util.resource.ResourceRuntime;
import de.keksuccino.konkrete.util.resource.ResourceSourceType;
import de.keksuccino.konkrete.util.resource.resources.texture.AnimatedTextureResetFrame;
import de.keksuccino.konkrete.util.resource.resources.texture.ITexture;
import de.keksuccino.konkrete.util.threading.KonkreteThreads;
import de.keksuccino.konkrete.util.threading.MainThreadTaskExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Owns asynchronous FMA decoding, animation playback, and dynamic-texture registration. */
public class FmaTexture implements ITexture, PlayableResource {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final int PREFETCH_QUEUE_SIZE = 4;
    private static final long MIN_FRAME_DELAY_MS = 10L;
    private static final long INACTIVITY_TIMEOUT_MS = 10000L;
    private static final long IDLE_SLEEP_MS = 10L;

    /** Decoded canvas width in pixels for this FMA decoder instance. */
    protected volatile int width = 10;
    /** Decoded canvas height in pixels for this FMA decoder instance. */
    protected volatile int height = 10;
    /** Aspect ratio derived from the decoded width and height. */
    @NotNull
    protected volatile AspectRatio aspectRatio = new AspectRatio(10, 10);

    /** Original resource-pack identifier, or null when another source kind is used. */
    protected volatile Identifier sourceLocation;
    /** Holds the sourceFile handle whose lifecycle follows this FMA decoder instance. */
    protected volatile File sourceFile;
    /** Original web URL, or null when another source kind is used. */
    protected volatile String sourceURL;

    /** Current last resource location call state for this FMA decoder instance. */
    protected volatile long lastResourceLocationCall = -1L;
    /** Whether decoded currently applies to this FMA decoder instance. */
    protected final AtomicBoolean decoded = new AtomicBoolean(false);
    /** Whether loading completed currently applies to this FMA decoder instance. */
    protected final AtomicBoolean loadingCompleted = new AtomicBoolean(false);
    /** Whether loading failed currently applies to this FMA decoder instance. */
    protected final AtomicBoolean loadingFailed = new AtomicBoolean(false);
    /** Whether closed currently applies to this FMA decoder instance. */
    protected final AtomicBoolean closed = new AtomicBoolean(false);

    /** Current cycles state for this FMA decoder instance. */
    protected final AtomicInteger cycles = new AtomicInteger(0);
    /** How many times the FMA should loop; values {@code <= 0} mean unlimited loops. */
    protected final AtomicInteger numPlays = new AtomicInteger(0);

    /** Whether max loops reached currently applies to this FMA decoder instance. */
    protected volatile boolean maxLoopsReached = false;
    /** Whether pending start event currently applies to this FMA decoder instance. */
    protected volatile boolean pendingStartEvent = true;
    /** Whether intro finished playing currently applies to this FMA decoder instance. */
    protected volatile boolean introFinishedPlaying = false;
    /** Whether play requested currently applies to this FMA decoder instance. */
    protected volatile boolean playRequested = true;
    /** Whether paused requested currently applies to this FMA decoder instance. */
    protected volatile boolean pausedRequested = false;

    /** Current frame count measured or selected by this FMA decoder instance. */
    protected volatile int frameCount = 0;
    /** Current intro frame count measured or selected by this FMA decoder instance. */
    protected volatile int introFrameCount = 0;
    /** Holds the frameDelaysMs collection used by this FMA decoder instance. */
    protected volatile long[] frameDelaysMs = new long[0];
    /** Holds the introFrameDelaysMs collection used by this FMA decoder instance. */
    protected volatile long[] introFrameDelaysMs = new long[0];

    /** Current decoder state for this FMA decoder instance. */
    @Nullable
    protected volatile FmaDecoder decoder = null;

    /** Process-unique suffix used for this FMA decoder instance's runtime identifiers. */
    protected final String uniqueId = ResourceRuntime.nextResourceId();

    // Single persistent texture that receives frame uploads.
    /** Holds the streamingTexture handle whose lifecycle follows this FMA decoder instance. */
    @Nullable
    protected volatile DynamicTexture streamingTexture = null;
    /** Current streaming resource location state for this FMA decoder instance. */
    @Nullable
    protected volatile Identifier streamingResourceLocation = null;
    /** Owned reset frame state for this FMA decoder instance. */
    @NotNull
    protected final AnimatedTextureResetFrame resetFrame = new AnimatedTextureResetFrame();

    // Streaming state.
    /** Lock guarding stream state transitions in this FMA decoder instance. */
    protected final Object streamStateLock = new Object();
    /** Current prefetched frames state for this FMA decoder instance. */
    @NotNull
    protected final ArrayDeque<DecodedFrame> prefetchedFrames = new ArrayDeque<>();
    /** Owned pending upload frame state for this FMA decoder instance. */
    @NotNull
    protected final AtomicReference<DecodedFrame> pendingUploadFrame = new AtomicReference<>(null);
    /** Current stream generation state for this FMA decoder instance. */
    protected final AtomicInteger streamGeneration = new AtomicInteger(0);

    /** Holds the streamThread handle whose lifecycle follows this FMA decoder instance. */
    @Nullable
    protected volatile Thread streamThread = null;
    /** Whether playback initialized currently applies to this FMA decoder instance. */
    protected volatile boolean playbackInitialized = false;
    /** Whether playback intro currently applies to this FMA decoder instance. */
    protected volatile boolean playbackIntro = false;
    /** Current playback index measured or selected by this FMA decoder instance. */
    protected volatile int playbackIndex = -1;
    /** Current playback frame start ms state for this FMA decoder instance. */
    protected volatile long playbackFrameStartMs = 0L;
    /** Current playback frame delay ms state for this FMA decoder instance. */
    protected volatile long playbackFrameDelayMs = MIN_FRAME_DELAY_MS;

    /** Whether decode intro currently applies to this FMA decoder instance. */
    protected volatile boolean decodeIntro = false;
    /** Current decode index measured or selected by this FMA decoder instance. */
    protected volatile int decodeIndex = 0;

    /** Creates the location FMA decoder variant. */
    @NotNull
    public static FmaTexture location(@NotNull Identifier location) {
        return location(location, null);
    }

    /** Creates the location FMA decoder variant. */
    @NotNull
    public static FmaTexture location(@NotNull Identifier location, @Nullable FmaTexture writeTo) {
        Objects.requireNonNull(location);
        FmaTexture texture = (writeTo != null) ? writeTo : new FmaTexture();
        texture.sourceLocation = location;

        try {
            of(Minecraft.getInstance().getResourceManager().open(location), location.toString(), texture);
        } catch (Exception ex) {
            texture.markLoadingFailed();
            LOGGER.error("[KONKRETE] Failed to read FMA image from Identifier: " + location, ex);
        }

        return texture;
    }

    /** Creates the local FMA decoder variant. */
    @NotNull
    public static FmaTexture local(@NotNull File fmaFile) {
        return local(fmaFile, null);
    }

    /** Creates the local FMA decoder variant. */
    @NotNull
    public static FmaTexture local(@NotNull File fmaFile, @Nullable FmaTexture writeTo) {
        Objects.requireNonNull(fmaFile);
        FmaTexture texture = (writeTo != null) ? writeTo : new FmaTexture();
        texture.sourceFile = fmaFile;

        if (!fmaFile.isFile()) {
            texture.markLoadingFailed();
            LOGGER.error("[KONKRETE] Failed to read FMA image from file! File not found: " + fmaFile.getPath());
            return texture;
        }

        KonkreteThreads.startDaemonThread(() -> {
            try {
                InputStream in = new FileInputStream(fmaFile);
                of(in, fmaFile.getPath(), texture);
            } catch (Exception ex) {
                texture.markLoadingFailed();
                LOGGER.error("[KONKRETE] Failed to read FMA image from file: " + fmaFile.getPath(), ex);
            }
        }, "FmaTexture-LocalLoader");

        return texture;
    }

    /** Creates the web FMA decoder variant. */
    @NotNull
    public static FmaTexture web(@NotNull String fmaUrl) {
        return web(fmaUrl, null);
    }

    /** Creates the web FMA decoder variant. */
    @NotNull
    public static FmaTexture web(@NotNull String fmaUrl, @Nullable FmaTexture writeTo) {
        Objects.requireNonNull(fmaUrl);
        FmaTexture texture = (writeTo != null) ? writeTo : new FmaTexture();
        texture.sourceURL = fmaUrl;

        if (!TextValidators.BASIC_URL_TEXT_VALIDATOR.get(fmaUrl)) {
            texture.markLoadingFailed();
            LOGGER.error("[KONKRETE] Failed to read FMA image from URL! Invalid URL: " + fmaUrl);
            return texture;
        }

        // Stream directly into the decoder (decoder spools to a temp archive file internally).
        KonkreteThreads.startDaemonThread(() -> {
            InputStream in = null;
            try {
                in = WebUtils.openResourceStream(fmaUrl, WebUtils.WebResourceType.STREAMED_ANIMATED_ARCHIVE);
                if (in == null) throw new NullPointerException("Web resource input stream was NULL!");
                of(in, fmaUrl, texture);
            } catch (Exception ex) {
                texture.markLoadingFailed();
                LOGGER.error("[KONKRETE] Failed to read FMA image from URL: " + fmaUrl, ex);
                CloseableUtils.closeQuietly(in);
            }
        }, "FmaTexture-WebLoader");

        return texture;
    }

    /**
     * Closes the passed {@link InputStream}!
     */
    @NotNull
    public static FmaTexture of(@NotNull InputStream in, @Nullable String fmaTextureName, @Nullable FmaTexture writeTo) {
        Objects.requireNonNull(in);
        FmaTexture texture = (writeTo != null) ? writeTo : new FmaTexture();

        KonkreteThreads.startDaemonThread(() -> {
            populateTexture(texture, in, (fmaTextureName != null) ? fmaTextureName : "[Generic InputStream Source]");
            if (texture.closed.get()) {
                MainThreadTaskExecutor.executeInMainThread(texture::close, MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK);
            }
        }, "FmaTexture-Decoder");

        return texture;
    }

    /**
     * Closes the passed {@link InputStream}!
     */
    @NotNull
    public static FmaTexture of(@NotNull InputStream in) {
        return of(in, null, null);
    }

    /** Populates the texture for the FMA decoder. */
    protected static void populateTexture(@NotNull FmaTexture texture, @NotNull InputStream in, @NotNull String fmaTextureName) {
        DecodedFmaImage decodedImage = null;
        if (!texture.closed.get()) {
            decodedImage = decodeFma(in, fmaTextureName);
            if (decodedImage == null) {
                texture.decoded.set(true);
                texture.markLoadingFailed();
                LOGGER.error("[KONKRETE] Failed to read FMA image, because DecodedFmaImage was NULL: {}", fmaTextureName);
                CloseableUtils.closeQuietly(in);
                return;
            }

            try {
                texture.configureStreamingState(decodedImage);
            } catch (Exception ex) {
                texture.markLoadingFailed();
                LOGGER.error("[KONKRETE] Failed to initialize streaming state for FMA image: " + fmaTextureName, ex);
            }

            texture.decoded.set(true);
        }

        CloseableUtils.closeQuietly(in);
    }

    /** Initializes a new {@code FmaTexture} for FMA decoder use. */
    protected FmaTexture() {
    }

    /** Configures the streaming state for the FMA decoder. */
    protected void configureStreamingState(@NotNull DecodedFmaImage decodedImage) {
        this.resetFrame.clear();
        FmaDecoder previousDecoder = this.decoder;
        if ((previousDecoder != null) && (previousDecoder != decodedImage.decoder())) {
            CloseableUtils.closeQuietly(previousDecoder);
        }
        this.decoder = decodedImage.decoder();
        this.frameCount = this.decoder != null ? this.decoder.getFrameCount() : 0;
        this.introFrameCount = (this.decoder != null && this.decoder.hasIntroFrames()) ? this.decoder.getIntroFrameCount() : 0;

        if (this.frameCount <= 0) {
            throw new IllegalStateException("FMA image has no usable frames");
        }

        this.width = decodedImage.imageWidth();
        this.height = decodedImage.imageHeight();
        this.aspectRatio = new AspectRatio(decodedImage.imageWidth(), decodedImage.imageHeight());
        this.numPlays.set(decodedImage.numPlays());

        FmaDecoder.FmaMetadata metadata = Objects.requireNonNull(this.decoder.getMetadata(), "FmaDecoder returned NULL for metadata!");

        long[] normalDelays = new long[this.frameCount];
        for (int i = 0; i < normalDelays.length; i++) {
            normalDelays[i] = sanitizeDelay(metadata.getFrameTimeForFrame(i, false));
        }

        long[] introDelays = new long[this.introFrameCount];
        for (int i = 0; i < introDelays.length; i++) {
            introDelays[i] = sanitizeDelay(metadata.getFrameTimeForFrame(i, true));
        }

        this.frameDelaysMs = normalDelays;
        this.introFrameDelaysMs = introDelays;

        this.loadingCompleted.set(true);
        this.loadingFailed.set(false);

        this.requestPlaybackReset();
    }

    /** Normalizes the delay for the FMA decoder. */
    protected static long sanitizeDelay(long delayMs) {
        return Math.max(MIN_FRAME_DELAY_MS, delayMs);
    }

    /** Starts the ticker if needed for the FMA decoder. */
    protected void startTickerIfNeeded() {
        if (this.closed.get() || this.loadingFailed.get() || !this.decoded.get()) return;

        synchronized (this.streamStateLock) {
            Thread running = this.streamThread;
            if ((running != null) && running.isAlive()) return;

            int generation = this.streamGeneration.get();
            Thread stream = new Thread(() -> this.streamLoop(generation), "Konkrete-FmaStream-" + this.uniqueId);
            stream.setDaemon(true);
            this.streamThread = stream;
            stream.start();
        }
    }

    /** Updates the stream loop state in the FMA decoder. */
    protected void streamLoop(int generation) {
        while (!this.closed.get() && (generation == this.streamGeneration.get())) {
            if (!this.playRequested || this.maxLoopsReached) {
                sleepQuietly(IDLE_SLEEP_MS);
                continue;
            }
            if (this.pausedRequested && this.playbackInitialized) {
                sleepQuietly(IDLE_SLEEP_MS);
                continue;
            }

            long now = System.currentTimeMillis();
            if ((this.lastResourceLocationCall > 0L) && ((this.lastResourceLocationCall + INACTIVITY_TIMEOUT_MS) < now)) {
                synchronized (this.streamStateLock) {
                    this.clearPrefetchedFramesLocked();
                }
                this.clearPendingUploadFrame();
                sleepQuietly(100L);
                continue;
            }

            try {
                if (!this.playbackInitialized) {
                    if (!this.initializeFirstFrame(generation)) {
                        sleepQuietly(IDLE_SLEEP_MS);
                    }
                    continue;
                }

                if (this.shouldIdleOnSingleInfiniteMainFrame()) {
                    synchronized (this.streamStateLock) {
                        this.clearPrefetchedFramesLocked();
                    }
                    sleepQuietly(100L);
                    continue;
                }

                if (this.pendingUploadFrame.get() != null) {
                    sleepQuietly(IDLE_SLEEP_MS);
                    continue;
                }

                this.fillPrefetchQueue(generation);

                long elapsed = now - this.playbackFrameStartMs;
                long delay = Math.max(MIN_FRAME_DELAY_MS, this.playbackFrameDelayMs);
                if (elapsed < delay) {
                    sleepQuietly(Math.min(IDLE_SLEEP_MS, delay - elapsed));
                    continue;
                }

                if (this.isAtNormalCycleBoundary()) {
                    boolean willRestart = this.handleCycleBoundary();
                    if (!willRestart) {
                        this.maxLoopsReached = true;
                        this.playRequested = false;
                        continue;
                    }
                }

                DecodedFrame next = this.pollPrefetchedFrame();
                if (next == null) {
                    sleepQuietly(IDLE_SLEEP_MS);
                    continue;
                }
                if (generation != this.streamGeneration.get()) {
                    next.close();
                    break;
                }

                if (!this.publishDecodedFrame(next, generation, now)) {
                    break;
                }
                this.maybeEmitStartEvent(next.intro, next.index);
            } catch (Exception ex) {
                LOGGER.error("[KONKRETE] An error happened in the streaming thread of an FMA texture!", ex);
                sleepQuietly(50L);
            }
        }

        synchronized (this.streamStateLock) {
            if (generation == this.streamGeneration.get()) {
                this.streamThread = null;
            }
        }
    }

    /** Initializes the first frame for the FMA decoder. */
    protected boolean initializeFirstFrame(int generation) {
        this.fillPrefetchQueue(generation);

        DecodedFrame first = this.pollPrefetchedFrame();
        if (first == null) return false;
        if (generation != this.streamGeneration.get()) {
            first.close();
            return false;
        }

        long now = System.currentTimeMillis();
        if (!this.publishDecodedFrame(first, generation, now)) return false;
        this.maybeEmitStartEvent(first.intro, first.index);
        this.loadingCompleted.set(true);
        return true;
    }

    /** Fills the prefetch queue buffer for the FMA decoder. */
    protected void fillPrefetchQueue(int generation) {
        synchronized (this.streamStateLock) {
            while (!this.closed.get() && (generation == this.streamGeneration.get()) && (this.prefetchedFrames.size() < PREFETCH_QUEUE_SIZE)) {
                DecodedFrame decodedFrame = this.decodeNextFrame();
                if (decodedFrame == null) {
                    break;
                }
                if (generation != this.streamGeneration.get()) {
                    decodedFrame.close();
                    break;
                }
                this.prefetchedFrames.addLast(decodedFrame);
            }
        }
    }

    /** Decodes the next frame for the FMA decoder. */
    @Nullable
    protected DecodedFrame decodeNextFrame() {
        boolean intro = this.decodeIntro;
        int index = this.decodeIndex;

        long delay = this.resolveFrameDelay(intro, index);
        NativeImage frameImage = this.decodeFrameImage(intro, index);
        if (frameImage == null) {
            if (this.loadingCompleted.get()) {
                this.markLoadingFailed();
            }
            return null;
        }

        this.advanceDecodeCursor();
        return new DecodedFrame(intro, index, delay, frameImage);
    }

    /** Resolves the frame delay for the FMA decoder. */
    protected long resolveFrameDelay(boolean intro, int index) {
        if (intro) {
            if ((index >= 0) && (index < this.introFrameDelaysMs.length)) {
                return this.introFrameDelaysMs[index];
            }
            return MIN_FRAME_DELAY_MS;
        }

        if ((index >= 0) && (index < this.frameDelaysMs.length)) {
            return this.frameDelaysMs[index];
        }

        return MIN_FRAME_DELAY_MS;
    }

    /** Decodes the frame image for the FMA decoder. */
    @Nullable
    protected NativeImage decodeFrameImage(boolean intro, int index) {
        FmaDecoder activeDecoder = this.decoder;
        if (activeDecoder == null) return null;

        try {
            InputStream frameInput = intro ? activeDecoder.getIntroFrame(index) : activeDecoder.getFrame(index);
            if (frameInput == null) return null;

            try (InputStream closeableInput = frameInput) {
                return NativeImage.read(closeableInput);
            }
        } catch (Exception ex) {
            LOGGER.error("[KONKRETE] Failed to decode {} frame {} of FMA stream", intro ? "intro" : "normal", index, ex);
            return null;
        }
    }

    /** Advances the decode cursor in the FMA decoder. */
    protected void advanceDecodeCursor() {
        if (this.decodeIntro) {
            if ((this.decodeIndex + 1) < this.introFrameCount) {
                this.decodeIndex++;
                return;
            }

            this.decodeIntro = false;
            this.decodeIndex = 0;
            return;
        }

        if (this.frameCount <= 0) {
            this.decodeIndex = 0;
            return;
        }

        if ((this.decodeIndex + 1) < this.frameCount) {
            this.decodeIndex++;
        } else {
            this.decodeIndex = 0;
        }
    }

    /** Retrieves the prefetched frame from the FMA decoder. */
    @Nullable
    protected DecodedFrame pollPrefetchedFrame() {
        synchronized (this.streamStateLock) {
            return this.prefetchedFrames.pollFirst();
        }
    }

    /** Clears the prefetched frames locked from this FMA decoder component. */
    protected void clearPrefetchedFramesLocked() {
        DecodedFrame frame;
        while ((frame = this.prefetchedFrames.pollFirst()) != null) {
            frame.close();
        }
    }

    /** Publishes the decoded frame state to the FMA decoder. */
    protected boolean publishDecodedFrame(@NotNull DecodedFrame nextFrame, int generation, long frameStartMs) {
        DecodedFrame oldPending;
        synchronized (this.streamStateLock) {
            if (generation != this.streamGeneration.get()) {
                nextFrame.close();
                return false;
            }
            if (this.closed.get()) {
                nextFrame.close();
                return false;
            }
            boolean switchedIntroToNormal = this.playbackIntro && !nextFrame.intro;
            if (switchedIntroToNormal) {
                this.introFinishedPlaying = true;
            }
            this.playbackInitialized = true;
            this.playbackIntro = nextFrame.intro;
            this.playbackIndex = nextFrame.index;
            this.playbackFrameStartMs = frameStartMs;
            this.playbackFrameDelayMs = sanitizeDelay(nextFrame.delayMs);
            oldPending = this.pendingUploadFrame.getAndSet(nextFrame);
        }
        if (oldPending != null) {
            oldPending.close();
        }
        return true;
    }

    /** Clears the pending upload frame from this FMA decoder component. */
    protected void clearPendingUploadFrame() {
        DecodedFrame pending = this.pendingUploadFrame.getAndSet(null);
        if (pending != null) {
            pending.close();
        }
    }

    /** Returns whether at normal cycle boundary. */
    protected boolean isAtNormalCycleBoundary() {
        return this.playbackInitialized && !this.playbackIntro && (this.frameCount > 0) && (this.playbackIndex == (this.frameCount - 1));
    }

    /** Handles the cycle boundary transition for the FMA decoder. */
    protected boolean handleCycleBoundary() {
        int plays = this.numPlays.get();
        if (plays > 0) {
            int newCycles = this.cycles.incrementAndGet();
            boolean willRestart = newCycles < plays;
            this.notifyAnimatedTextureFinished(willRestart);
            if (willRestart) {
                this.pendingStartEvent = true;
            }
            return willRestart;
        }

        this.notifyAnimatedTextureFinished(true);
        this.pendingStartEvent = true;
        return true;
    }

    /** Conditionally updates the emit start event for the FMA decoder. */
    protected void maybeEmitStartEvent(boolean isIntroFrame, int frameIndex) {
        if (!this.pendingStartEvent) return;

        boolean isFirstFrame;
        if (isIntroFrame) {
            isFirstFrame = (frameIndex == 0) && !this.introFinishedPlaying;
        } else if (this.introFrameCount > 0) {
            isFirstFrame = this.introFinishedPlaying && (frameIndex == 0);
        } else {
            isFirstFrame = (frameIndex == 0);
        }

        if (!isFirstFrame) return;

        this.pendingStartEvent = false;
        this.notifyAnimatedTextureStarted(this.willRestartAfterCurrentCycle());
    }

    /** Returns whether will restart after current cycle. */
    protected boolean willRestartAfterCurrentCycle() {
        int plays = this.numPlays.get();
        if (plays <= 0) return true;
        return (this.cycles.get() + 1) < plays;
    }

    /** Returns whether idle on single infinite main frame. */
    protected boolean shouldIdleOnSingleInfiniteMainFrame() {
        return this.playbackInitialized
                && !this.playbackIntro
                && (this.playbackIndex == 0)
                && (this.frameCount == 1)
                && (this.numPlays.get() <= 0);
    }

    /** Requests the playback reset from the FMA decoder. */
    protected void requestPlaybackReset() {
        synchronized (this.streamStateLock) {
            this.streamGeneration.incrementAndGet();
            this.cycles.set(0);
            this.maxLoopsReached = false;
            this.pendingStartEvent = true;
            this.introFinishedPlaying = this.introFrameCount <= 0;
            this.playbackInitialized = false;
            this.playbackIntro = this.introFrameCount > 0;
            this.playbackIndex = -1;
            this.playbackFrameStartMs = 0L;
            this.playbackFrameDelayMs = MIN_FRAME_DELAY_MS;
            this.decodeIntro = this.introFrameCount > 0;
            this.decodeIndex = 0;
            this.clearPrefetchedFramesLocked();
            this.clearPendingUploadFrame();
        }

        Thread running = this.streamThread;
        if (running != null) {
            running.interrupt();
        }
        this.streamThread = null;
    }

    /** Uploads the pending frame to texture to the active texture for the FMA decoder. */
    protected void uploadPendingFrameToTexture() {
        DecodedFrame frame = this.pendingUploadFrame.getAndSet(null);
        if (frame == null) return;
        if (frame.nativeImage == null) {
            frame.close();
            return;
        }

        try {
            if ((frame.index == 0) && (frame.intro || (this.introFrameCount <= 0))) {
                this.resetFrame.captureIfAbsent(frame.nativeImage);
            }
            DynamicTexture currentTexture = this.streamingTexture;

            if ((currentTexture == null) || !this.canUploadFrameToTexture(currentTexture, frame.nativeImage)) {
                this.replaceStreamingTexture(frame);
                return;
            }

            RenderSystem.getDevice().createCommandEncoder().writeToTexture(currentTexture.getTexture(), frame.nativeImage);
        } catch (Exception ex) {
            this.markLoadingFailed();
            LOGGER.error("[KONKRETE] Failed to upload streamed FMA frame into DynamicTexture", ex);
        } finally {
            frame.close();
        }
    }

    /** Returns whether upload frame to texture. */
    protected boolean canUploadFrameToTexture(@NotNull DynamicTexture texture, @NotNull NativeImage frameImage) {
        NativeImage texturePixels = texture.getPixels();
        return (texturePixels != null)
                && (texturePixels.getWidth() == frameImage.getWidth())
                && (texturePixels.getHeight() == frameImage.getHeight());
    }

    /** Updates the replace streaming texture state in the FMA decoder. */
    protected void replaceStreamingTexture(@NotNull DecodedFrame frame) {
        NativeImage frameImage = Objects.requireNonNull(frame.nativeImage, "FMA frame NativeImage was NULL");
        DynamicTexture previousTexture = this.streamingTexture;
        Identifier resourceLocation = this.streamingResourceLocation;
        boolean hadResourceLocation = resourceLocation != null;
        if (resourceLocation == null) {
            resourceLocation = ResourceRuntime.identifier("dynamic/fma_stream_" + this.uniqueId);
            this.streamingResourceLocation = resourceLocation;
        } else {
            Minecraft.getInstance().getTextureManager().release(resourceLocation);
        }
        if ((previousTexture != null) && !hadResourceLocation) {
            previousTexture.close();
        }

        Identifier textureLocation = resourceLocation;
        this.streamingTexture = new DynamicTexture(textureLocation::toString, frameImage);
        frame.nativeImage = null;
        Minecraft.getInstance().getTextureManager().register(textureLocation, this.streamingTexture);
    }

    /** Returns the resource location, or {@code null} when it is not available. */
    @Nullable
    @Override
    public Identifier getResourceLocation() {
        if (this.closed.get()) return FULLY_TRANSPARENT_TEXTURE;

        this.lastResourceLocationCall = System.currentTimeMillis();
        try {
            this.restoreResetFrameIfRequested();
        } catch (Exception ex) {
            this.markLoadingFailed();
            LOGGER.error("[KONKRETE] Failed to restore the first FMA frame after a playback reset", ex);
            return FULLY_TRANSPARENT_TEXTURE;
        }
        this.startTickerIfNeeded();
        this.uploadPendingFrameToTexture();

        if (this.loadingFailed.get()) return FULLY_TRANSPARENT_TEXTURE;
        return (this.streamingResourceLocation != null) ? this.streamingResourceLocation : FULLY_TRANSPARENT_TEXTURE;
    }

    /** Returns the width used by this FMA decoder instance. */
    @Override
    public int getWidth() {
        return this.width;
    }

    /** Returns the height used by this FMA decoder instance. */
    @Override
    public int getHeight() {
        return this.height;
    }

    /** Returns the aspect ratio used by this FMA decoder instance. */
    @Override
    public @NotNull AspectRatio getAspectRatio() {
        return this.aspectRatio;
    }

    /** Opens the resource for the FMA decoder. */
    @Override
    public @Nullable InputStream open() throws IOException {
        if (this.sourceURL != null) return WebUtils.openResourceStream(this.sourceURL, WebUtils.WebResourceType.STREAMED_ANIMATED_ARCHIVE);
        if (this.sourceFile != null) return new FileInputStream(this.sourceFile);
        if (this.sourceLocation != null) return Minecraft.getInstance().getResourceManager().open(this.sourceLocation);
        return null;
    }

    /** Returns whether ready. */
    @Override
    public boolean isReady() {
        return this.decoded.get();
    }

    /** Returns whether loading completed. */
    @Override
    public boolean isLoadingCompleted() {
        return !this.closed.get() && !this.loadingFailed.get() && this.loadingCompleted.get();
    }

    /** Returns whether loading failed. */
    @Override
    public boolean isLoadingFailed() {
        return this.loadingFailed.get();
    }

    /** Restores initial FMA decoder state without transferring ownership. */
    public void reset() {
        if (this.closed.get()) return;

        this.playRequested = true;
        this.pausedRequested = false;
        this.requestPlaybackReset();
        this.resetFrame.requestRestore();
        this.startTickerIfNeeded();
    }

    private void notifyAnimatedTextureStarted(boolean willRestart) {
        ResourceRuntime.fireAnimatedTextureEvent(this.resolveTextureSource(), this.resolveTextureSourceType(), willRestart, ResourceRuntime.AnimatedTextureStatus.STARTED);
    }

    private void notifyAnimatedTextureFinished(boolean willRestart) {
        ResourceRuntime.fireAnimatedTextureEvent(this.resolveTextureSource(), this.resolveTextureSourceType(), willRestart, ResourceRuntime.AnimatedTextureStatus.FINISHED);
    }

    private String resolveTextureSource() {
        if (this.sourceURL != null) return this.sourceURL;
        if (this.sourceFile != null) return this.sourceFile.getPath();
        if (this.sourceLocation != null) return this.sourceLocation.toString();
        return "ERROR";
    }

    private ResourceSourceType resolveTextureSourceType() {
        if (this.sourceURL != null) return ResourceSourceType.WEB;
        if (this.sourceLocation != null) return ResourceSourceType.LOCATION;
        return ResourceSourceType.LOCAL;
    }

    /** Starts playback for this FMA decoder. */
    @Override
    public void play() {
        if (this.closed.get() || this.loadingFailed.get()) return;

        if (this.maxLoopsReached) return;

        if (this.pausedRequested) {
            this.playbackFrameStartMs = System.currentTimeMillis();
        }
        this.playRequested = true;
        this.pausedRequested = false;
        this.startTickerIfNeeded();
    }

    /** Returns whether playing. */
    @Override
    public boolean isPlaying() {
        return !this.closed.get() && this.playRequested && !this.pausedRequested && !this.maxLoopsReached;
    }

    /** Pauses playback without releasing this FMA decoder. */
    @Override
    public void pause() {
        if (this.closed.get() || this.loadingFailed.get()) return;
        this.pausedRequested = true;
    }

    /** Returns whether paused. */
    @Override
    public boolean isPaused() {
        return this.pausedRequested;
    }

    /** Stops playback and resets this FMA decoder. */
    @Override
    public void stop() {
        this.reset();
    }

    /** Returns whether closed. */
    @Override
    public boolean isClosed() {
        return this.closed.get();
    }

    /**
     * Stops streaming, invalidates pending frame work, and closes queued images, the reset image, decoder, and texture.
     * Repeated calls are harmless.
     */
    @Override
    public void close() {
        this.closed.set(true);
        this.playRequested = false;
        this.pausedRequested = false;

        this.streamGeneration.incrementAndGet();

        Thread running = this.streamThread;
        if (running != null) {
            running.interrupt();
        }
        this.streamThread = null;

        synchronized (this.streamStateLock) {
            this.clearPrefetchedFramesLocked();
        }
        this.clearPendingUploadFrame();

        this.releaseStreamingTextureNow();
        this.resetFrame.close();

        FmaDecoder activeDecoder = this.decoder;
        this.decoder = null;
        if (activeDecoder != null) {
            CloseableUtils.closeQuietly(activeDecoder);
        }

        this.sourceLocation = null;
        this.sourceFile = null;
        this.sourceURL = null;
    }

    /** Restores the reset frame if requested state for the FMA decoder. */
    protected void restoreResetFrameIfRequested() {
        if (this.resetFrame.restoreTo(this.streamingTexture)) return;
        AnimatedTextureResetFrame.RequestedFrame requestedFrame = this.resetFrame.copyForRequestedRestore();
        if (requestedFrame == null) return;
        try (requestedFrame) {
            DecodedFrame frame = new DecodedFrame(this.introFrameCount > 0, 0, this.resolveFrameDelay(this.introFrameCount > 0, 0), requestedFrame.takeImage());
            try {
                this.replaceStreamingTexture(frame);
                this.resetFrame.markRestored(requestedFrame.restoreVersion());
            } finally {
                frame.close();
            }
        }
    }

    /** Marks the loading failed state for the FMA decoder. */
    protected void markLoadingFailed() {
        this.loadingFailed.set(true);
        this.resetFrame.clear();
    }

    /** Detaches and immediately releases the current streaming texture and texture-manager identifier. */
    protected void releaseStreamingTextureNow() {
        DynamicTexture activeTexture = this.streamingTexture;
        Identifier activeLocation = this.streamingResourceLocation;
        this.streamingTexture = null;
        this.streamingResourceLocation = null;
        this.releaseStreamingTexture(activeLocation, activeTexture);
    }

    /** Removes the supplied texture-manager registration and closes any remaining direct texture reference. */
    protected void releaseStreamingTexture(@Nullable Identifier resourceLocation, @Nullable DynamicTexture texture) {
        if (resourceLocation != null) {
            Minecraft.getInstance().getTextureManager().release(resourceLocation);
        }
        if (texture != null) {
            try {
                texture.close();
            } catch (Exception ex) {
                LOGGER.error("[KONKRETE] Failed to close streaming DynamicTexture of FMA", ex);
            }
        }
    }

    /** Decodes the fma for the FMA decoder. */
    @Nullable
    public static DecodedFmaImage decodeFma(@NotNull InputStream in, @NotNull String fmaName) {
        FmaDecoder decoder = null;
        try {
            decoder = new FmaDecoder();
            decoder.read(in);
            warnAboutExpensiveFmaFrames(decoder, fmaName);

            FmaDecoder.FmaMetadata metadata = Objects.requireNonNull(decoder.getMetadata(), "FmaDecoder returned NULL for metadata!");
            InputStream firstFrameStream = decoder.hasIntroFrames() ? decoder.getIntroFrame(0) : decoder.getFirstFrame();
            if (firstFrameStream == null) {
                throw new NullPointerException("Failed to get first frame of FMA image!");
            }

            int imageWidth;
            int imageHeight;
            try (InputStream closeableFirstFrame = firstFrameStream; NativeImage firstFrameImage = NativeImage.read(closeableFirstFrame)) {
                imageWidth = firstFrameImage.getWidth();
                imageHeight = firstFrameImage.getHeight();
            }

            return new DecodedFmaImage(decoder, imageWidth, imageHeight, metadata.getLoopCount());
        } catch (Exception ex) {
            LOGGER.error("[KONKRETE] Failed to decode FMA image: " + fmaName, ex);
            CloseableUtils.closeQuietly(decoder);
            return null;
        }
    }

    /** Publishes the about expensive fma frames through the configured FMA decoder warning hook. */
    protected static void warnAboutExpensiveFmaFrames(@NotNull FmaDecoder decoder, @NotNull String fmaName) {
        try {
            FmaDecoder.ExpensiveFrameSample expensiveFrameSample = decoder.findExpensiveFrameSample();
            if (expensiveFrameSample == null) return;

            LOGGER.warn("[KONKRETE] Detected expensive sampled frame while loading FMA {}. Frame: {}, bit depth: {}, color type: {}, interlace method: {}, resolution: {}x{}",
                    fmaName,
                    expensiveFrameSample.framePath(),
                    expensiveFrameSample.bitDepth(),
                    expensiveFrameSample.colorType(),
                    expensiveFrameSample.interlaceMethod(),
                    expensiveFrameSample.width(),
                    expensiveFrameSample.height());

            String displayName = resolveFmaDisplayName(fmaName);
            MainThreadTaskExecutor.executeInMainThread(() -> ResourceRuntime.warnAboutExpensiveFma(displayName), MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK);
        } catch (Exception ex) {
            LOGGER.warn("[KONKRETE] Failed to scan FMA for expensive sampled frames before loading: {}", fmaName, ex);
        }
    }

    /** Resolves the fma display name for the FMA decoder. */
    @NotNull
    protected static String resolveFmaDisplayName(@NotNull String fmaName) {
        String normalized = fmaName.replace('\\', '/');
        int slashIndex = normalized.lastIndexOf('/');
        if ((slashIndex >= 0) && (slashIndex < (normalized.length() - 1))) {
            return normalized.substring(slashIndex + 1);
        }
        return normalized;
    }

    /** Sleeps for at least one millisecond and preserves interruption without failing the streaming worker. */
    protected static void sleepQuietly(long millis) {
        try {
            Thread.sleep(Math.max(1L, millis));
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } catch (Exception ignored) {
        }
    }

    /** Carries {@code DecodedFrame} data between validated stages of the FMA decoder. */
    protected static class DecodedFrame implements AutoCloseable {
        /** Whether intro currently applies to this FMA decoder instance. */
        protected final boolean intro;
        /** Current index state for this FMA decoder instance. */
        protected final int index;
        /** Current delay ms state for this FMA decoder instance. */
        protected final long delayMs;
        /** Holds the nativeImage handle whose lifecycle follows this FMA decoder instance. */
        @Nullable
        protected NativeImage nativeImage;

        /** Initializes a new {@code DecodedFrame} for FMA decoder use. */
        protected DecodedFrame(boolean intro, int index, long delayMs, @NotNull NativeImage nativeImage) {
            this.intro = intro;
            this.index = index;
            this.delayMs = delayMs;
            this.nativeImage = nativeImage;
        }

        /** Closes the decoded native image unless it was already transferred; repeated calls are harmless. */
        @Override
        public void close() {
            if (this.nativeImage != null) {
                try {
                    this.nativeImage.close();
                } catch (Exception ignored) {
                }
                this.nativeImage = null;
            }
        }
    }

    /** Carries {@code DecodedFmaImage} data between validated stages of the FMA decoder. */
    public record DecodedFmaImage(@NotNull FmaDecoder decoder, int imageWidth, int imageHeight, int numPlays) {
    }

}
