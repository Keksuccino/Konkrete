package de.keksuccino.konkrete.util.resource.resources.texture;

import com.mojang.blaze3d.platform.NativeImage;
import de.keksuccino.konkrete.util.CloseableUtils;
import de.keksuccino.konkrete.util.WebUtils;
import de.keksuccino.konkrete.util.input.TextValidators;
import de.keksuccino.konkrete.util.rendering.AspectRatio;
import de.keksuccino.konkrete.util.resource.PlayableResource;
import de.keksuccino.konkrete.util.resource.ResourceRuntime;
import de.keksuccino.konkrete.util.resource.ResourceSourceType;
import de.keksuccino.konkrete.util.threading.KonkreteThreads;
import de.keksuccino.konkrete.util.threading.MainThreadTaskExecutor;
import de.keksuccino.konkrete.util.watermedia.WatermediaAnimatedTextureBackend;
import de.keksuccino.konkrete.util.watermedia.WatermediaUtil;
import net.ellerton.japng.Png;
import net.ellerton.japng.argb8888.Argb8888Bitmap;
import net.ellerton.japng.argb8888.Argb8888BitmapSequence;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/** Owns asynchronous APNG decoding, animation playback, and dynamic-texture registration. */
public class ApngTexture implements ITexture, PlayableResource {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Owned frame store state for this texture resource instance. */
    protected final AnimatedTextureFrameStore<ApngFrame> frameStore = new AnimatedTextureFrameStore<>();
    /** Aspect ratio derived from the decoded width and height. */
    @NotNull
    protected volatile AspectRatio aspectRatio = new AspectRatio(10, 10);
    /** Decoded canvas width in pixels for this texture resource instance. */
    protected volatile int width = 10;
    /** Decoded canvas height in pixels for this texture resource instance. */
    protected volatile int height = 10;
    /** Current last resource location call state for this texture resource instance. */
    protected volatile long lastResourceLocationCall = -1;
    /** Whether ticker thread running currently applies to this texture resource instance. */
    protected final AtomicBoolean tickerThreadRunning = new AtomicBoolean(false);
    /** Whether decoded currently applies to this texture resource instance. */
    protected final AtomicBoolean decoded = new AtomicBoolean(false);
    /** Current cycles state for this texture resource instance. */
    protected final AtomicInteger cycles = new AtomicInteger(0);
    /** How many times the APNG should loop; values {@code <= 0} mean unlimited loops. */
    protected final AtomicInteger numPlays = new AtomicInteger(0);
    /** Original resource-pack identifier, or null when another source kind is used. */
    protected Identifier sourceLocation;
    /** Holds the sourceFile handle whose lifecycle follows this texture resource instance. */
    protected File sourceFile;
    /** Original web URL, or null when another source kind is used. */
    protected String sourceURL;
    /** Whether loading failed currently applies to this texture resource instance. */
    protected final AtomicBoolean loadingFailed = new AtomicBoolean(false);
    /** Process-unique suffix used for this texture resource instance's runtime identifiers. */
    protected final String uniqueId = ResourceRuntime.nextResourceId();
    /** Current frame registration counter state for this texture resource instance. */
    protected int frameRegistrationCounter = 0;
    /** Whether max loops reached currently applies to this texture resource instance. */
    protected volatile boolean maxLoopsReached = false;
    /** Whether pending start event currently applies to this texture resource instance. */
    protected volatile boolean pendingStartEvent = true;
    /** Whether closed currently applies to this texture resource instance. */
    protected final AtomicBoolean closed = new AtomicBoolean(false);
    /** Owned watermedia backend state for this texture resource instance. */
    @Nullable
    protected volatile WatermediaAnimatedTextureBackend watermediaBackend = null;
    /** Holds the watermediaFallbackData collection used by this texture resource instance. */
    @Nullable
    protected volatile byte[] watermediaFallbackData = null;
    /** Current source name state for this texture resource instance. */
    @Nullable
    protected volatile String sourceName = null;
    /** Whether watermedia fallback triggered currently applies to this texture resource instance. */
    protected final AtomicBoolean watermediaFallbackTriggered = new AtomicBoolean(false);

    /** Creates the location texture resource variant. */
    @NotNull
    public static ApngTexture location(@NotNull Identifier location) {
        return location(location, null);
    }

    /** Creates the location texture resource variant. */
    @NotNull
    public static ApngTexture location(@NotNull Identifier location, @Nullable ApngTexture writeTo) {

        Objects.requireNonNull(location);
        ApngTexture texture = (writeTo != null) ? writeTo : new ApngTexture();

        texture.sourceLocation = location;

        try {
            of(Minecraft.getInstance().getResourceManager().open(location), location.toString(), texture);
        } catch (Exception ex) {
            texture.loadingFailed.set(true);
            LOGGER.error("[KONKRETE] Failed to read APNG image from Identifier: " + location, ex);
        }

        return texture;

    }

    /** Creates the local texture resource variant. */
    @NotNull
    public static ApngTexture local(@NotNull File apngFile) {
        return local(apngFile, null);
    }

    /** Creates the local texture resource variant. */
    @NotNull
    public static ApngTexture local(@NotNull File apngFile, @Nullable ApngTexture writeTo) {

        Objects.requireNonNull(apngFile);
        ApngTexture texture = (writeTo != null) ? writeTo : new ApngTexture();

        texture.sourceFile = apngFile;

        if (!apngFile.isFile()) {
            texture.loadingFailed.set(true);
            LOGGER.error("[KONKRETE] Failed to read APNG image from file! File not found: " + apngFile.getPath());
            return texture;
        }

        //Decode APNG image
        KonkreteThreads.startDaemonThread(() -> {
            try {
                InputStream in = new FileInputStream(apngFile);
                of(in, apngFile.getPath(), texture);
            } catch (Exception ex) {
                texture.loadingFailed.set(true);
                LOGGER.error("[KONKRETE] Failed to read APNG image from file: " + apngFile.getPath(), ex);
            }
        }, "ApngTexture-LocalLoader");

        return texture;

    }

    /** Creates the web texture resource variant. */
    @NotNull
    public static ApngTexture web(@NotNull String apngUrl) {
        return web(apngUrl, null);
    }

    /** Creates the web texture resource variant. */
    @NotNull
    public static ApngTexture web(@NotNull String apngUrl, @Nullable ApngTexture writeTo) {

        Objects.requireNonNull(apngUrl);
        ApngTexture texture = (writeTo != null) ? writeTo : new ApngTexture();

        texture.sourceURL = apngUrl;

        if (!TextValidators.BASIC_URL_TEXT_VALIDATOR.get(Objects.requireNonNull(apngUrl))) {
            texture.loadingFailed.set(true);
            LOGGER.error("[KONKRETE] Failed to read APNG image from URL! Invalid URL: " + apngUrl);
            return texture;
        }

        //Download and decode APNG image
        KonkreteThreads.startDaemonThread(() -> {
            try {
                populateTexture(texture, null, apngUrl);
                if (texture.closed.get()) MainThreadTaskExecutor.executeInMainThread(texture::close, MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK);
            } catch (Exception ex) {
                texture.loadingFailed.set(true);
                texture.decoded.set(true);
                LOGGER.error("[KONKRETE] Failed to read APNG image from URL: " + apngUrl, ex);
            }
        }, "ApngTexture-WebLoader");

        return texture;

    }

    /**
     * Closes the passed {@link InputStream}!
     */
    @NotNull
    public static ApngTexture of(@NotNull InputStream in, @Nullable String apngTextureName, @Nullable ApngTexture writeTo) {

        Objects.requireNonNull(in);

        ApngTexture texture = (writeTo != null) ? writeTo : new ApngTexture();

        //Decode APNG image
        KonkreteThreads.startDaemonThread(() -> {
            populateTexture(texture, in, (apngTextureName != null) ? apngTextureName : "[Generic InputStream Source]");
            if (texture.closed.get()) MainThreadTaskExecutor.executeInMainThread(texture::close, MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK);
        }, "ApngTexture-Decoder");

        return texture;

    }

    /**
     * Closes the passed {@link InputStream}!
     */
    @NotNull
    public static ApngTexture of(@NotNull InputStream in) {
        return of(in, null, null);
    }

    /** Populates the texture for the texture resource. */
    protected static void populateTexture(@NotNull ApngTexture texture, @Nullable InputStream in, @NotNull String apngTextureName) {
        InputStream readInput = in;
        texture.sourceName = apngTextureName;
        texture.watermediaFallbackTriggered.set(false);
        texture.watermediaFallbackData = null;
        if (!texture.closed.get()) {
            boolean decodedByWatermedia = false;
            if (WatermediaUtil.isWatermediaRenderingAvailable()) {
                decodedByWatermedia = populateTextureWithWatermediaDirectSource(texture, apngTextureName);
            }

            byte[] apngData = null;
            if (!decodedByWatermedia) {
                if (readInput == null) {
                    try {
                        readInput = texture.open();
                    } catch (Exception ex) {
                        texture.loadingFailed.set(true);
                        texture.decoded.set(true);
                        LOGGER.error("[KONKRETE] Failed to open APNG image data stream: " + apngTextureName, ex);
                        CloseableUtils.closeQuietly(readInput);
                        return;
                    }
                }
                if (readInput == null) {
                    texture.loadingFailed.set(true);
                    texture.decoded.set(true);
                    LOGGER.error("[KONKRETE] Failed to open APNG image data stream: {}", apngTextureName);
                    CloseableUtils.closeQuietly(readInput);
                    return;
                }
                try {
                    apngData = readInput.readAllBytes();
                } catch (Exception ex) {
                    texture.loadingFailed.set(true);
                    texture.decoded.set(true);
                    LOGGER.error("[KONKRETE] Failed to read APNG image data: " + apngTextureName, ex);
                    CloseableUtils.closeQuietly(readInput);
                    return;
                }
            }

            if (!decodedByWatermedia) {
                if (apngData == null) {
                    texture.loadingFailed.set(true);
                    LOGGER.error("[KONKRETE] Failed to read APNG image data: {}", apngTextureName);
                    texture.decoded.set(true);
                    CloseableUtils.closeQuietly(readInput);
                    return;
                }
                populateTextureWithPrimitiveDecoder(texture, apngData, apngTextureName);
            }
        }
        CloseableUtils.closeQuietly(readInput);
    }

    /** Populates the texture with watermedia direct source for the texture resource. */
    protected static boolean populateTextureWithWatermediaDirectSource(@NotNull ApngTexture texture, @NotNull String apngTextureName) {
        String directSource = null;
        if (texture.sourceURL != null) {
            directSource = texture.sourceURL;
        } else if ((texture.sourceFile != null) && texture.sourceFile.isFile()) {
            // Java's URLConnection usually reports ".apng" files as unknown media type.
            // If we pass that directly, Watermedia can resolve it as a generic source and use FF instead of Tx.
            // Fall back to the byte path for local APNG files so we can provide a ".png" source extension.
            return false;
        }
        if (directSource == null) return false;

        LOGGER.info("[KONKRETE] Starting APNG loading via Watermedia direct source: {}", apngTextureName);
        WatermediaAnimatedTextureBackend backend = new WatermediaAnimatedTextureBackend(texture.uniqueId, "apng");
        backend.setLoopCount(-1);
        boolean initialized = backend.initializeFromSource(directSource, apngTextureName);
        if (!initialized) {
            backend.close();
            return false;
        }
        texture.watermediaBackend = backend;
        texture.watermediaFallbackData = null;
        texture.decoded.set(true);
        return true;
    }

    /** Populates the texture with primitive decoder for the texture resource. */
    protected static void populateTextureWithPrimitiveDecoder(@NotNull ApngTexture texture, @NotNull byte[] apngData, @NotNull String apngTextureName) {
        long frameGeneration = texture.frameStore.generation();
        if (!texture.frameStore.runIfGenerationActive(frameGeneration, () -> texture.watermediaFallbackData = null)) return;
        DecodedApngImage decodedImage = decodeApng(new ByteArrayInputStream(apngData), apngTextureName);
        if (decodedImage == null) {
            LOGGER.error("[KONKRETE] Failed to read APNG image, because DecodedApngImage was NULL: " + apngTextureName);
            texture.frameStore.runIfGenerationActive(frameGeneration, texture::publishPrimitiveFailure);
            return;
        }
        if (!texture.frameStore.runIfGenerationActive(frameGeneration, () -> texture.publishPrimitiveMetadata(decodedImage))) return;
        try {
            deliverApngFrames(decodedImage.sequence(), apngTextureName, true, frame -> {
                if (frame != null) {
                    try {
                        NativeImage image = NativeImage.read(frame.frameInputStream);
                        if (image != null) frame.textureEntry.adopt(image);
                    } catch (Exception ex) {
                        LOGGER.error("[KONKRETE] Failed to read frame of APNG image into NativeImage: " + apngTextureName, ex);
                    }
                    frame.closeDecoderInputs();
                    texture.frameStore.add(frameGeneration, frame);
                }
            });
            texture.frameStore.markComplete(frameGeneration);
        } catch (Exception ex) {
            texture.frameStore.runIfGenerationActive(frameGeneration, () -> texture.loadingFailed.set(true));
            LOGGER.error("[KONKRETE] Failed to read frames of APNG image: " + apngTextureName, ex);
        }
    }

    /** Populates the texture with watermedia for the texture resource. */
    protected static boolean populateTextureWithWatermedia(@NotNull ApngTexture texture, @NotNull byte[] apngData, @NotNull String apngTextureName) {
        WatermediaAnimatedTextureBackend backend = new WatermediaAnimatedTextureBackend(texture.uniqueId, "apng");
        backend.setLoopCount(readApngLoopCount(apngData));
        // Use ".png" extension so Watermedia classifies the media as IMAGE and routes playback through TxMediaPlayer.
        boolean initialized = backend.initializeFromBytes(apngData, ".png", apngTextureName);
        if (!initialized) {
            backend.close();
            return false;
        }
        texture.watermediaBackend = backend;
        texture.watermediaFallbackData = apngData;
        texture.decoded.set(true);
        return true;
    }

    /** Reads the apng loop count from the texture resource input. */
    protected static int readApngLoopCount(@NotNull byte[] apngData) {
        // Default to infinite loops if the acTL chunk cannot be read.
        if (apngData.length < 8) return -1;
        int index = 8; // skip PNG signature
        while ((index + 12) <= apngData.length) {
            int chunkLength = ((apngData[index] & 255) << 24)
                    | ((apngData[index + 1] & 255) << 16)
                    | ((apngData[index + 2] & 255) << 8)
                    | (apngData[index + 3] & 255);
            int chunkType = ((apngData[index + 4] & 255) << 24)
                    | ((apngData[index + 5] & 255) << 16)
                    | ((apngData[index + 6] & 255) << 8)
                    | (apngData[index + 7] & 255);
            if (chunkType == 0x6163544C) { // acTL
                int dataStart = index + 8;
                if ((dataStart + 8) <= apngData.length) {
                    int plays = ((apngData[dataStart + 4] & 255) << 24)
                            | ((apngData[dataStart + 5] & 255) << 16)
                            | ((apngData[dataStart + 6] & 255) << 8)
                            | (apngData[dataStart + 7] & 255);
                    return (plays == 0) ? -1 : Math.max(1, plays);
                }
                return -1;
            }
            long next = index + 12L + chunkLength;
            if (next > apngData.length) break;
            index = (int) next;
        }
        return -1;
    }

    /** Initializes a new {@code ApngTexture} for texture resource use. */
    protected ApngTexture() {
    }

    /** Publishes the primitive metadata state to the texture resource. */
    protected void publishPrimitiveMetadata(@NotNull DecodedApngImage decodedImage) {
        this.width = decodedImage.imageWidth;
        this.height = decodedImage.imageHeight;
        this.aspectRatio = new AspectRatio(decodedImage.imageWidth, decodedImage.imageHeight);
        this.numPlays.set(decodedImage.numPlays);
        this.decoded.set(true);
    }

    /** Publishes the primitive failure state to the texture resource. */
    protected void publishPrimitiveFailure() {
        this.loadingFailed.set(true);
        this.decoded.set(true);
    }

    /** Starts the ticker if needed for the texture resource. */
    @SuppressWarnings("all")
    protected void startTickerIfNeeded() {
        if (!this.tickerThreadRunning.get() && !this.frameStore.isEmpty() && !this.maxLoopsReached && !this.closed.get()) {

            this.tickerThreadRunning.set(true);
            this.lastResourceLocationCall = System.currentTimeMillis();

            KonkreteThreads.startDaemonThread(() -> {

                //Automatically stop thread if APNG was inactive for >=10 seconds
                while ((this.lastResourceLocationCall + 10000) > System.currentTimeMillis()) {
                    if (this.frameStore.isEmpty() || this.closed.get()) break;
                    //Don't tick frame if max loops reached
                    if (this.maxLoopsReached) break;
                    boolean sleep = false;
                    try {
                        AnimatedTextureFrameStore.Snapshot<ApngFrame> frameSnapshot = this.frameStore.snapshot();
                        List<ApngFrame> cachedFrames = frameSnapshot.frames();
                        if (!this.frameStore.isGenerationActive(frameSnapshot.generation())) continue;
                        boolean cachedAllDecoded = frameSnapshot.complete();
                        if (!cachedFrames.isEmpty()) {
                            //Set initial (first) frame if current is NULL
                            ApngFrame currentFrame = frameSnapshot.current();
                            if (currentFrame == null) {
                                currentFrame = cachedFrames.get(0);
                                if (this.frameStore.setCurrent(frameSnapshot.generation(), currentFrame)) {
                                    this.maybeEmitStartEvent(cachedFrames, currentFrame);
                                    Thread.sleep(Math.max(20, currentFrame.delayMs));
                                } else {
                                    continue;
                                }
                            } else {
                                this.maybeEmitStartEvent(cachedFrames, currentFrame);
                            }
                            //Cache current frame to make sure it stays the same instance while working with it
                            ApngFrame cachedCurrent = currentFrame;
                            if (cachedCurrent != null) {
                                ApngFrame newCurrent = null;
                                int currentIndexIncrement = cachedCurrent.index + 1;
                                //Check if there's a frame after the current one and if so, go to the next frame
                                if (currentIndexIncrement < cachedFrames.size()) {
                                    newCurrent = cachedFrames.get(currentIndexIncrement);
                                } else if (cachedAllDecoded) {
                                    int cachedNumPlays = this.numPlays.get();
                                    //Count cycles up if APNG should not loop infinitely (numPlays > 0 = finite loops)
                                    if (cachedNumPlays > 0) {
                                        int newCycles = this.cycles.incrementAndGet();
                                        boolean willRestart = newCycles < cachedNumPlays;
                                        this.notifyAnimatedTextureFinished(willRestart);
                                        if (!willRestart) {
                                            this.maxLoopsReached = true;
                                            break; //end the while loop of the frame ticker
                                        }
                                        //If APNG has a finite number of loops but did not reach its max loops yet, reset to first frame, because end reached
                                        newCurrent = cachedFrames.get(0);
                                        this.pendingStartEvent = true;
                                    } else {
                                        //If APNG loops infinitely, reset to first frame, because end reached
                                        this.notifyAnimatedTextureFinished(true);
                                        newCurrent = cachedFrames.get(0);
                                        this.pendingStartEvent = true;
                                    }
                                }
                                if (newCurrent != null) {
                                    if (this.frameStore.setCurrent(frameSnapshot.generation(), newCurrent)) {
                                        this.maybeEmitStartEvent(cachedFrames, newCurrent);
                                    } else {
                                        continue;
                                    }
                                }
                                //Sleep for the new current frame's delay or sleep for 100ms if there's no new frame
                                Thread.sleep(Math.max(20, (newCurrent != null) ? newCurrent.delayMs : 100));
                            } else {
                                sleep = true;
                            }
                        } else {
                            sleep = true;
                        }
                    } catch (Exception ex) {
                        sleep = true;
                        LOGGER.error("[KONKRETE] An error happened in the frame ticker thread on an APNG!", ex);
                    }
                    if (sleep) {
                        try {
                            Thread.sleep(100);
                        } catch (Exception ex) {
                            LOGGER.error("[KONKRETE] An error happened in the frame ticker thread on an APNG!", ex);
                        }
                    }
                }

                this.tickerThreadRunning.set(false);

            }, "ApngTexture-FrameTicker");

        }
    }

    /** Returns the resource location, or {@code null} when it is not available. */
    @Nullable
    @Override
    public Identifier getResourceLocation() {
        WatermediaAnimatedTextureBackend backend = this.resolveWatermediaBackend();
        if (backend != null) {
            this.width = backend.getWidth();
            this.height = backend.getHeight();
            this.aspectRatio = backend.getAspectRatio();
            Identifier resourceLocation = backend.getResourceLocation();
            return (resourceLocation != null) ? resourceLocation : FULLY_TRANSPARENT_TEXTURE;
        }
        if (this.closed.get()) return FULLY_TRANSPARENT_TEXTURE;
        this.lastResourceLocationCall = System.currentTimeMillis();
        this.startTickerIfNeeded();
        ApngFrame frame = this.frameStore.current();
        if (frame != null) {
            if (frame.textureEntry.canRegister()) {
                try {
                    this.frameRegistrationCounter++;
                    frame.textureEntry.register(ResourceRuntime.identifier("dynamic/apng_frame_" + this.uniqueId + "_" + this.frameRegistrationCounter));
                } catch (Exception ex) {
                    LOGGER.error("[KONKRETE] Failed to register APNG frame to Minecraft's TextureManager!", ex);
                }
            }
            Identifier resourceLocation = frame.textureEntry.getIdentifier();
            return (resourceLocation != null) ? resourceLocation : FULLY_TRANSPARENT_TEXTURE;
        }
        return null;
    }

    /** Returns the width used by this texture resource instance. */
    @Override
    public int getWidth() {
        WatermediaAnimatedTextureBackend backend = this.resolveWatermediaBackend();
        if (backend != null) return backend.getWidth();
        return this.width;
    }

    /** Returns the height used by this texture resource instance. */
    @Override
    public int getHeight() {
        WatermediaAnimatedTextureBackend backend = this.resolveWatermediaBackend();
        if (backend != null) return backend.getHeight();
        return this.height;
    }

    /** Returns the aspect ratio used by this texture resource instance. */
    @Override
    public @NotNull AspectRatio getAspectRatio() {
        WatermediaAnimatedTextureBackend backend = this.resolveWatermediaBackend();
        if (backend != null) return backend.getAspectRatio();
        return this.aspectRatio;
    }

    /** Opens the resource for the texture resource. */
    @Override
    public @Nullable InputStream open() throws IOException {
        if (this.sourceURL != null) return WebUtils.openResourceStream(this.sourceURL, WebUtils.WebResourceType.BUFFERED_ANIMATED_TEXTURE);
        if (this.sourceFile != null) return new FileInputStream(this.sourceFile);
        if (this.sourceLocation != null) return Minecraft.getInstance().getResourceManager().open(this.sourceLocation);
        return null;
    }

    /** Returns whether ready. */
    @Override
    public boolean isReady() {
        WatermediaAnimatedTextureBackend backend = this.resolveWatermediaBackend();
        if (backend != null) return backend.isReady();
        //Everything important (like size) is set at this point, so it is considered ready
        return this.decoded.get();
    }

    /** Returns whether loading completed. */
    @Override
    public boolean isLoadingCompleted() {
        WatermediaAnimatedTextureBackend backend = this.resolveWatermediaBackend();
        if (backend != null) return backend.isLoadingCompleted();
        return !this.closed.get() && !this.loadingFailed.get() && this.frameStore.isComplete();
    }

    /** Returns whether loading failed. */
    @Override
    public boolean isLoadingFailed() {
        WatermediaAnimatedTextureBackend backend = this.resolveWatermediaBackend();
        if (backend != null) return backend.isLoadingFailed();
        return this.loadingFailed.get();
    }

    /** Restores initial texture resource state without transferring ownership. */
    public void reset() {
        WatermediaAnimatedTextureBackend backend = this.resolveWatermediaBackend();
        if (backend != null) {
            backend.reset();
            return;
        }
        this.maxLoopsReached = false;
        this.pendingStartEvent = true;
        this.frameStore.resetToFirst();
        this.cycles.set(0);
    }

    private void maybeEmitStartEvent(@NotNull List<ApngFrame> frames, @Nullable ApngFrame currentFrame) {
        if (!this.pendingStartEvent || currentFrame == null || frames.isEmpty()) return;
        if (currentFrame != frames.get(0)) return;
        this.pendingStartEvent = false;
        this.notifyAnimatedTextureStarted(this.willRestartAfterCurrentCycle());
    }

    private boolean willRestartAfterCurrentCycle() {
        int plays = this.numPlays.get();
        if (plays <= 0) return true;
        return (this.cycles.get() + 1) < plays;
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

    /** Resolves the texture name for the texture resource. */
    @NotNull
    protected String resolveTextureName() {
        return (this.sourceName != null) ? this.sourceName : this.resolveTextureSource();
    }

    private ResourceSourceType resolveTextureSourceType() {
        if (this.sourceURL != null) return ResourceSourceType.WEB;
        if (this.sourceLocation != null) return ResourceSourceType.LOCATION;
        return ResourceSourceType.LOCAL;
    }

    /** Starts playback for this texture resource. */
    @Override
    public void play() {
        WatermediaAnimatedTextureBackend backend = this.resolveWatermediaBackend();
        if (backend != null) {
            backend.play();
        }
    }

    /** Returns whether playing. */
    @Override
    public boolean isPlaying() {
        WatermediaAnimatedTextureBackend backend = this.resolveWatermediaBackend();
        if (backend != null) return backend.isPlaying();
        return !this.maxLoopsReached;
    }

    /** Pauses playback without releasing this texture resource. */
    @Override
    public void pause() {
        WatermediaAnimatedTextureBackend backend = this.resolveWatermediaBackend();
        if (backend != null) {
            backend.pause();
        }
    }

    /** Returns whether paused. */
    @Override
    public boolean isPaused() {
        WatermediaAnimatedTextureBackend backend = this.resolveWatermediaBackend();
        if (backend != null) return backend.isPaused();
        return false;
    }

    /** Stops playback and resets this texture resource. */
    @Override
    public void stop() {
        WatermediaAnimatedTextureBackend backend = this.resolveWatermediaBackend();
        if (backend != null) {
            backend.stop();
            return;
        }
        this.reset();
    }

    /** Returns whether closed. */
    @Override
    public boolean isClosed() {
        return this.closed.get();
    }

    /** Closes the Watermedia backend and every primitive frame, then rejects further decoder output. */
    @Override
    public void close() {
        WatermediaAnimatedTextureBackend backend = this.watermediaBackend;
        if (backend != null) {
            backend.close();
            this.watermediaBackend = null;
        }
        this.watermediaFallbackData = null;
        this.closed.set(true);
        this.sourceLocation = null;
        this.releasePrimitiveFrames();
    }

    /** Resolves the watermedia backend for the texture resource. */
    @Nullable
    protected WatermediaAnimatedTextureBackend resolveWatermediaBackend() {
        WatermediaAnimatedTextureBackend backend = this.watermediaBackend;
        if ((backend != null) && backend.isLoadingFailed()) {
            this.startPrimitiveFallbackAfterWatermediaFailure(backend);
            return null;
        }
        return backend;
    }

    /** Starts the primitive fallback after watermedia failure for the texture resource. */
    protected void startPrimitiveFallbackAfterWatermediaFailure(@NotNull WatermediaAnimatedTextureBackend failedBackend) {
        if (!this.watermediaFallbackTriggered.compareAndSet(false, true)) return;
        if (this.watermediaBackend == failedBackend) {
            this.watermediaBackend = null;
        }
        failedBackend.close();
        String apngTextureName = this.resolveTextureName();
        LOGGER.warn("[KONKRETE] Watermedia APNG playback failed, falling back to primitive decoder: {}", apngTextureName);
        this.preparePrimitiveFallbackState();
        KonkreteThreads.startDaemonThread(() -> this.loadPrimitiveFallback(apngTextureName), "ApngTexture-PrimitiveFallback");
    }

    /** Prepares the primitive fallback state for the texture resource. */
    protected void preparePrimitiveFallbackState() {
        this.releasePrimitiveFrames();
        this.loadingFailed.set(false);
        this.decoded.set(false);
        this.maxLoopsReached = false;
        this.pendingStartEvent = true;
        this.cycles.set(0);
    }

    /** Loads the primitive fallback for the texture resource. */
    protected void loadPrimitiveFallback(@NotNull String apngTextureName) {
        InputStream in = null;
        try {
            byte[] apngData = this.watermediaFallbackData;
            if (apngData == null) {
                in = this.open();
                if (in == null) {
                    this.loadingFailed.set(true);
                    LOGGER.error("[KONKRETE] Failed to reopen APNG image data stream for Watermedia fallback: {}", apngTextureName);
                    return;
                }
                apngData = in.readAllBytes();
            }
            if (!this.closed.get()) {
                populateTextureWithPrimitiveDecoder(this, apngData, apngTextureName);
            }
        } catch (Exception ex) {
            this.loadingFailed.set(true);
            LOGGER.error("[KONKRETE] Failed to decode APNG image after Watermedia fallback: " + apngTextureName, ex);
        } finally {
            this.decoded.set(true);
            this.watermediaFallbackData = null;
            CloseableUtils.closeQuietly(in);
            if (this.closed.get()) {
                MainThreadTaskExecutor.executeInMainThread(this::close, MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK);
            }
        }
    }

    /** Detaches and closes all primitive APNG frames, permanently closing the frame store after resource closure. */
    protected void releasePrimitiveFrames() {
        if (this.closed.get()) this.frameStore.close();
        else this.frameStore.clear();
    }

    /** Decodes the apng for the texture resource. */
    @Nullable
    public static DecodedApngImage decodeApng(@NotNull InputStream in, @NotNull String apngName) {
        try {
            return decodeApng(Png.readArgb8888BitmapSequence(in));
        } catch (Exception ex) {
            LOGGER.error("[KONKRETE] Failed to decode APNG image: " + apngName, ex);
        }
        return null;
    }

    /** Decodes the apng for the texture resource. */
    @NotNull
    public static ApngTexture.DecodedApngImage decodeApng(@NotNull Argb8888BitmapSequence sequence) {
        int numPlays = -1;
        try {
            if (sequence.isAnimated()) {
                numPlays = sequence.getAnimationControl().loopForever() ? -1 : sequence.getAnimationControl().numPlays;
            }
        } catch (Exception ex) {
            LOGGER.error("[KONKRETE] An error happened while trying to decode an APNG image!", ex);
        }
        return new DecodedApngImage(sequence, sequence.header.width, sequence.header.height, numPlays);
    }

    /** Delivers the apng frames to the texture resource owner. */
    public static void deliverApngFrames(@NotNull Argb8888BitmapSequence sequence, @NotNull String apngName, boolean includeFirstFrame, @NotNull Consumer<ApngFrame> frameDelivery) {
        try {
            if (sequence.isAnimated()) {
                boolean defaultDelivered = false;
                if (sequence.hasDefaultImage() && includeFirstFrame) {
                    try {
                        BufferedImage frameImage = getBufferedImageFromBitmap(sequence.defaultImage, sequence.header.width, sequence.header.height, 0, 0);
                        ByteArrayOutputStream frameOut = new ByteArrayOutputStream();
                        ImageIO.write(frameImage, "PNG", frameOut);
                        ByteArrayInputStream frameIn = new ByteArrayInputStream(frameOut.toByteArray());
                        frameDelivery.accept(new ApngFrame(0, frameIn, 0, frameOut));
                        defaultDelivered = true;
                    } catch (Exception ex) {
                        LOGGER.error("[KONKRETE] Failed to decode default frame of APNG image: " + apngName, ex);
                    }
                }
                int index = defaultDelivered ? 1 : 0;
                int frameCount = 0;
                for (Argb8888BitmapSequence.Frame frame : sequence.getAnimationFrames()) {
                    try {
                        BufferedImage frameImage = getBufferedImageFromBitmap(frame.bitmap, sequence.header.width, sequence.header.height, frame.control.xOffset, frame.control.yOffset);
                        ByteArrayOutputStream frameOut = new ByteArrayOutputStream();
                        ImageIO.write(frameImage, "PNG", frameOut);
                        ByteArrayInputStream frameIn = new ByteArrayInputStream(frameOut.toByteArray());
                        boolean skip = !includeFirstFrame && (index == 0);
                        if (!skip) frameDelivery.accept(new ApngFrame(index, frameIn, frame.control.getDelayMilliseconds(), frameOut));
                        index++;
                    } catch (Exception ex) {
                        LOGGER.error("[KONKRETE] Failed to decode frame " + frameCount + " of APNG image: " + apngName, ex);
                    }
                    frameCount++;
                }
            }
        } catch (Exception ex) {
            LOGGER.error("[KONKRETE] Failed to decode APNG image: " + apngName, ex);
        }
        frameDelivery.accept(null);
    }

    /** Returns the buffered image from bitmap used by this texture resource instance. */
    @NotNull
    protected static BufferedImage getBufferedImageFromBitmap(@NotNull Argb8888Bitmap bitmap, int imageWidth, int imageHeight, int frameXOffset, int frameYOffset) {
        int[] framePixels = bitmap.getPixelArray();
        BufferedImage frameImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
        int frameWidth = bitmap.getWidth();
        int frameHeight = bitmap.getHeight();
        int frameXOff = Math.max(0, frameXOffset);
        int frameYOff = Math.max(0, frameYOffset);
        frameImage.setRGB(frameXOff, frameYOff, frameWidth, frameHeight, framePixels, 0, frameWidth);
        return frameImage;
    }

    /** Carries {@code ApngFrame} data between validated stages of the texture resource. */
    public static class ApngFrame implements AutoCloseable {

        /** Current index state for this texture resource instance. */
        protected final int index;
        /** Current frame input stream state for this texture resource instance. */
        protected final ByteArrayInputStream frameInputStream;
        /** Current delay ms state for this texture resource instance. */
        protected final int delayMs;
        /** Current close after loading state for this texture resource instance. */
        protected final ByteArrayOutputStream closeAfterLoading;
        /** Holds the textureEntry handle whose lifecycle follows this texture resource instance. */
        protected final TextureManagerEntry<NativeImage, DynamicTexture> textureEntry = TextureManagerEntry.dynamicTexture();

        /** Initializes a new {@code ApngFrame} for texture resource use. */
        protected ApngFrame(int index, ByteArrayInputStream frameInputStream, int delayMs, ByteArrayOutputStream closeAfterLoading) {
            this.index = index;
            this.frameInputStream = frameInputStream;
            this.delayMs = delayMs;
            this.closeAfterLoading = closeAfterLoading;
        }

        /** Closes the decoder inputs for the texture resource. */
        protected void closeDecoderInputs() {
            CloseableUtils.closeQuietly(this.closeAfterLoading);
            CloseableUtils.closeQuietly(this.frameInputStream);
        }

        /** Closes the encoded-frame streams and releases the frame's dynamic texture entry; safe to repeat. */
        @Override
        public void close() {
            this.closeDecoderInputs();
            this.textureEntry.close();
        }

    }

    /** Carries {@code DecodedApngImage} data between validated stages of the texture resource. */
    public record DecodedApngImage(@NotNull Argb8888BitmapSequence sequence, int imageWidth, int imageHeight, int numPlays) {
    }

}
