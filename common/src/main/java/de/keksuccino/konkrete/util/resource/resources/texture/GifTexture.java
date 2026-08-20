package de.keksuccino.konkrete.util.resource.resources.texture;

import com.madgag.gif.fmsware.GifDecoder;
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

/** Owns asynchronous GIF decoding, animation playback, and dynamic-texture registration. */
public class GifTexture implements ITexture, PlayableResource {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Owned frame store state for this texture resource instance. */
    protected final AnimatedTextureFrameStore<GifFrame> frameStore = new AnimatedTextureFrameStore<>();
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
    /** How many times the GIF should loop; values {@code <= 0} mean unlimited loops. */
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
    public static GifTexture location(@NotNull Identifier location) {
        return location(location, null);
    }

    /** Creates the location texture resource variant. */
    @NotNull
    public static GifTexture location(@NotNull Identifier location, @Nullable GifTexture writeTo) {

        Objects.requireNonNull(location);
        GifTexture texture = (writeTo != null) ? writeTo : new GifTexture();

        texture.sourceLocation = location;

        try {
            of(Minecraft.getInstance().getResourceManager().open(location), location.toString(), texture);
        } catch (Exception ex) {
            texture.loadingFailed.set(true);
            LOGGER.error("[KONKRETE] Failed to read GIF image from Identifier: " + location, ex);
        }

        return texture;

    }

    /** Creates the local texture resource variant. */
    @NotNull
    public static GifTexture local(@NotNull File apngFile) {
        return local(apngFile, null);
    }

    /** Creates the local texture resource variant. */
    @NotNull
    public static GifTexture local(@NotNull File gifFile, @Nullable GifTexture writeTo) {

        Objects.requireNonNull(gifFile);
        GifTexture texture = (writeTo != null) ? writeTo : new GifTexture();

        texture.sourceFile = gifFile;

        if (!gifFile.isFile()) {
            texture.loadingFailed.set(true);
            LOGGER.error("[KONKRETE] Failed to read GIF image from file! File not found: " + gifFile.getPath());
            return texture;
        }

        //Decode GIF image
        KonkreteThreads.startDaemonThread(() -> {
            try {
                InputStream in = new FileInputStream(gifFile);
                of(in, gifFile.getPath(), texture);
            } catch (Exception ex) {
                texture.loadingFailed.set(true);
                LOGGER.error("[KONKRETE] Failed to read GIF image from file: " + gifFile.getPath(), ex);
            }
        }, "GifTexture-LocalLoader");

        return texture;

    }

    /** Creates the web texture resource variant. */
    @NotNull
    public static GifTexture web(@NotNull String apngUrl) {
        return web(apngUrl, null);
    }

    /** Creates the web texture resource variant. */
    @NotNull
    public static GifTexture web(@NotNull String gifUrl, @Nullable GifTexture writeTo) {

        Objects.requireNonNull(gifUrl);
        GifTexture texture = (writeTo != null) ? writeTo : new GifTexture();

        texture.sourceURL = gifUrl;

        if (!TextValidators.BASIC_URL_TEXT_VALIDATOR.get(Objects.requireNonNull(gifUrl))) {
            texture.loadingFailed.set(true);
            LOGGER.error("[KONKRETE] Failed to read GIF image from URL! Invalid URL: " + gifUrl);
            return texture;
        }

        //Download and decode GIF image
        KonkreteThreads.startDaemonThread(() -> {
            try {
                populateTexture(texture, null, gifUrl);
                if (texture.closed.get()) MainThreadTaskExecutor.executeInMainThread(texture::close, MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK);
            } catch (Exception ex) {
                texture.loadingFailed.set(true);
                texture.decoded.set(true);
                LOGGER.error("[KONKRETE] Failed to read GIF image from URL: " + gifUrl, ex);
            }
        }, "GifTexture-WebLoader");

        return texture;

    }

    /**
     * Closes the passed {@link InputStream}!
     */
    @NotNull
    public static GifTexture of(@NotNull InputStream in, @Nullable String gifTextureName, @Nullable GifTexture writeTo) {

        Objects.requireNonNull(in);

        GifTexture texture = (writeTo != null) ? writeTo : new GifTexture();

        //Decode GIF image
        KonkreteThreads.startDaemonThread(() -> {
            populateTexture(texture, in, (gifTextureName != null) ? gifTextureName : "[Generic InputStream Source]");
            if (texture.closed.get()) MainThreadTaskExecutor.executeInMainThread(texture::close, MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK);
        }, "GifTexture-Decoder");

        return texture;

    }

    /**
     * Closes the passed {@link InputStream}!
     */
    @NotNull
    public static GifTexture of(@NotNull InputStream in) {
        return of(in, null, null);
    }

    /** Populates the texture for the texture resource. */
    protected static void populateTexture(@NotNull GifTexture texture, @Nullable InputStream in, @NotNull String gifTextureName) {
        InputStream readInput = in;
        texture.sourceName = gifTextureName;
        texture.watermediaFallbackTriggered.set(false);
        texture.watermediaFallbackData = null;
        if (!texture.closed.get()) {
            boolean decodedByWatermedia = false;
            if (WatermediaUtil.isWatermediaRenderingAvailable()) {
                LOGGER.info("[KONKRETE] Starting GIF loading via Watermedia (direct source preferred): {}", gifTextureName);
                decodedByWatermedia = populateTextureWithWatermediaDirectSource(texture, gifTextureName);
            }

            byte[] gifData = null;
            if (!decodedByWatermedia) {
                if (readInput == null) {
                    try {
                        readInput = texture.open();
                    } catch (Exception ex) {
                        texture.loadingFailed.set(true);
                        texture.decoded.set(true);
                        LOGGER.error("[KONKRETE] Failed to open GIF image data stream: " + gifTextureName, ex);
                        CloseableUtils.closeQuietly(readInput);
                        return;
                    }
                }
                if (readInput == null) {
                    texture.loadingFailed.set(true);
                    texture.decoded.set(true);
                    LOGGER.error("[KONKRETE] Failed to open GIF image data stream: {}", gifTextureName);
                    CloseableUtils.closeQuietly(readInput);
                    return;
                }
                try {
                    gifData = readInput.readAllBytes();
                } catch (Exception ex) {
                    texture.loadingFailed.set(true);
                    texture.decoded.set(true);
                    LOGGER.error("[KONKRETE] Failed to read GIF image data: " + gifTextureName, ex);
                    CloseableUtils.closeQuietly(readInput);
                    return;
                }
            }

            if (!decodedByWatermedia && WatermediaUtil.isWatermediaRenderingAvailable()) {
                decodedByWatermedia = populateTextureWithWatermedia(texture, gifData, gifTextureName);
                if (decodedByWatermedia) {
                    WatermediaUtil.WATERMEDIA_INITIALIZED = true;
                } else {
                    LOGGER.warn("[KONKRETE] Watermedia GIF decoding failed, falling back to primitive decoder: {}", gifTextureName);
                }
            }

            if (!decodedByWatermedia) {
                if (gifData == null) {
                    texture.loadingFailed.set(true);
                    LOGGER.error("[KONKRETE] Failed to read GIF image data: {}", gifTextureName);
                    texture.decoded.set(true);
                    CloseableUtils.closeQuietly(readInput);
                    return;
                }
                populateTextureWithPrimitiveDecoder(texture, gifData, gifTextureName);
            }
        }
        CloseableUtils.closeQuietly(readInput);
    }

    /** Populates the texture with watermedia direct source for the texture resource. */
    protected static boolean populateTextureWithWatermediaDirectSource(@NotNull GifTexture texture, @NotNull String gifTextureName) {
        String directSource = null;
        if (texture.sourceURL != null) {
            directSource = texture.sourceURL;
        } else if ((texture.sourceFile != null) && texture.sourceFile.isFile()) {
            directSource = texture.sourceFile.getAbsolutePath();
        }
        if (directSource == null) return false;

        WatermediaAnimatedTextureBackend backend = new WatermediaAnimatedTextureBackend(texture.uniqueId, "gif");
        backend.setLoopCount(-1);
        boolean initialized = backend.initializeFromSource(directSource, gifTextureName);
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
    protected static void populateTextureWithPrimitiveDecoder(@NotNull GifTexture texture, @NotNull byte[] gifData, @NotNull String gifTextureName) {
        long frameGeneration = texture.frameStore.generation();
        if (!texture.frameStore.runIfGenerationActive(frameGeneration, () -> texture.watermediaFallbackData = null)) return;
        DecodedGifImage decodedImage = decodeGif(new ByteArrayInputStream(gifData), gifTextureName);
        if (decodedImage == null) {
            LOGGER.error("[KONKRETE] Failed to read GIF image, because DecodedGifImage was NULL: " + gifTextureName);
            texture.frameStore.runIfGenerationActive(frameGeneration, texture::publishPrimitiveFailure);
            return;
        }
        if (!texture.frameStore.runIfGenerationActive(frameGeneration, () -> texture.publishPrimitiveMetadata(decodedImage))) return;
        try {
            deliverGifFrames(decodedImage.decoder(), gifTextureName, frame -> {
                if (frame != null) {
                    try {
                        NativeImage image = NativeImage.read(frame.frameInputStream);
                        if (image != null) frame.textureEntry.adopt(image);
                    } catch (Exception ex) {
                        LOGGER.error("[KONKRETE] Failed to read frame of GIF image into NativeImage: " + gifTextureName, ex);
                    }
                    frame.closeDecoderInputs();
                    texture.frameStore.add(frameGeneration, frame);
                }
            });
            texture.frameStore.markComplete(frameGeneration);
        } catch (Exception ex) {
            texture.frameStore.runIfGenerationActive(frameGeneration, () -> texture.loadingFailed.set(true));
            LOGGER.error("[KONKRETE] Failed to read frames of GIF image: " + gifTextureName, ex);
        }
    }

    /** Populates the texture with watermedia for the texture resource. */
    protected static boolean populateTextureWithWatermedia(@NotNull GifTexture texture, @NotNull byte[] gifData, @NotNull String gifTextureName) {
        WatermediaAnimatedTextureBackend backend = new WatermediaAnimatedTextureBackend(texture.uniqueId, "gif");
        backend.setLoopCount(readGifLoopCount(gifData));
        boolean initialized = backend.initializeFromBytes(gifData, ".gif", gifTextureName);
        if (!initialized) {
            backend.close();
            return false;
        }
        texture.watermediaBackend = backend;
        texture.watermediaFallbackData = gifData;
        texture.decoded.set(true);
        return true;
    }

    /** Reads the gif loop count from the texture resource input. */
    protected static int readGifLoopCount(@NotNull byte[] gifData) {
        // Defaults to one playback if no loop extension is present.
        if (gifData.length < 13) return 1;
        int index = 6;
        int packed = gifData[index + 4] & 255;
        index += 7;
        if ((packed & 0x80) != 0) {
            int gctSize = 3 * (1 << ((packed & 0x07) + 1));
            index += gctSize;
        }
        while (index < gifData.length) {
            int block = gifData[index++] & 255;
            if (block == 0x3B) break; // trailer
            if (block == 0x21) { // extension
                if (index >= gifData.length) break;
                int label = gifData[index++] & 255;
                String appIdentifier = null;
                if ((label == 0xFF) && (index < gifData.length)) {
                    int appBlockSize = gifData[index++] & 255;
                    if ((appBlockSize > 0) && ((index + appBlockSize) <= gifData.length)) {
                        appIdentifier = new String(gifData, index, appBlockSize);
                        index += appBlockSize;
                    } else {
                        return 1;
                    }
                }
                while (index < gifData.length) {
                    int size = gifData[index++] & 255;
                    if (size == 0) break;
                    if ((index + size) > gifData.length) return 1;
                    if ((appIdentifier != null)
                            && (size >= 3)
                            && (("NETSCAPE2.0".equals(appIdentifier)) || ("ANIMEXTS1.0".equals(appIdentifier)))
                            && ((gifData[index] & 255) == 1)) {
                        int loops = (gifData[index + 1] & 255) | ((gifData[index + 2] & 255) << 8);
                        return (loops == 0) ? -1 : Math.max(1, loops);
                    }
                    index += size;
                    if (index > gifData.length) return 1;
                }
            } else if (block == 0x2C) { // image descriptor
                if ((index + 9) > gifData.length) break;
                int localPacked = gifData[index + 8] & 255;
                index += 9;
                if ((localPacked & 0x80) != 0) {
                    int lctSize = 3 * (1 << ((localPacked & 0x07) + 1));
                    index += lctSize;
                }
                if (index >= gifData.length) break;
                index++; // LZW min code size
                while (index < gifData.length) {
                    int size = gifData[index++] & 255;
                    if (size == 0) break;
                    index += size;
                    if (index > gifData.length) return 1;
                }
            } else {
                break;
            }
        }
        return 1;
    }

    /** Initializes a new {@code GifTexture} for texture resource use. */
    protected GifTexture() {
    }

    /** Publishes the primitive metadata state to the texture resource. */
    protected void publishPrimitiveMetadata(@NotNull DecodedGifImage decodedImage) {
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
                        AnimatedTextureFrameStore.Snapshot<GifFrame> frameSnapshot = this.frameStore.snapshot();
                        List<GifFrame> cachedFrames = frameSnapshot.frames();
                        if (!this.frameStore.isGenerationActive(frameSnapshot.generation())) continue;
                        boolean cachedAllDecoded = frameSnapshot.complete();
                        if (!cachedFrames.isEmpty()) {
                            //Set initial (first) frame if current is NULL
                            GifFrame currentFrame = frameSnapshot.current();
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
                            GifFrame cachedCurrent = currentFrame;
                            if (cachedCurrent != null) {
                                GifFrame newCurrent = null;
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
                        LOGGER.error("[KONKRETE] An error happened in the frame ticker thread on an GIF!", ex);
                    }
                    if (sleep) {
                        try {
                            Thread.sleep(100);
                        } catch (Exception ex) {
                            LOGGER.error("[KONKRETE] An error happened in the frame ticker thread on an GIF!", ex);
                        }
                    }
                }

                this.tickerThreadRunning.set(false);

            }, "GifTexture-FrameTicker");

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
        GifFrame frame = this.frameStore.current();
        if (frame != null) {
            if (frame.textureEntry.canRegister()) {
                try {
                    this.frameRegistrationCounter++;
                    frame.textureEntry.register(ResourceRuntime.identifier("dynamic/gif_frame_" + this.uniqueId + "_" + this.frameRegistrationCounter));
                } catch (Exception ex) {
                    LOGGER.error("[KONKRETE] Failed to register GIF frame to Minecraft's TextureManager!", ex);
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
        this.pendingStartEvent = true;
        this.frameStore.resetToFirst();
        if (!this.frameStore.isEmpty()) this.cycles.set(0);
    }

    private void maybeEmitStartEvent(@NotNull List<GifFrame> frames, @Nullable GifFrame currentFrame) {
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
        String gifTextureName = this.resolveTextureName();
        LOGGER.warn("[KONKRETE] Watermedia GIF playback failed, falling back to primitive decoder: {}", gifTextureName);
        this.preparePrimitiveFallbackState();
        KonkreteThreads.startDaemonThread(() -> this.loadPrimitiveFallback(gifTextureName), "GifTexture-PrimitiveFallback");
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
    protected void loadPrimitiveFallback(@NotNull String gifTextureName) {
        InputStream in = null;
        try {
            byte[] gifData = this.watermediaFallbackData;
            if (gifData == null) {
                in = this.open();
                if (in == null) {
                    this.loadingFailed.set(true);
                    LOGGER.error("[KONKRETE] Failed to reopen GIF image data stream for Watermedia fallback: {}", gifTextureName);
                    return;
                }
                gifData = in.readAllBytes();
            }
            if (!this.closed.get()) {
                populateTextureWithPrimitiveDecoder(this, gifData, gifTextureName);
            }
        } catch (Exception ex) {
            this.loadingFailed.set(true);
            LOGGER.error("[KONKRETE] Failed to decode GIF image after Watermedia fallback: " + gifTextureName, ex);
        } finally {
            this.decoded.set(true);
            this.watermediaFallbackData = null;
            CloseableUtils.closeQuietly(in);
            if (this.closed.get()) {
                MainThreadTaskExecutor.executeInMainThread(this::close, MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK);
            }
        }
    }

    /** Detaches and closes all primitive GIF frames, permanently closing the frame store after resource closure. */
    protected void releasePrimitiveFrames() {
        if (this.closed.get()) this.frameStore.close();
        else this.frameStore.clear();
    }

    /** Decodes the gif for the texture resource. */
    @Nullable
    public static DecodedGifImage decodeGif(@NotNull InputStream in, @NotNull String gifName) {
        try {
            GifDecoder decoder = new GifDecoder();
            decoder.read(in);
            BufferedImage firstFrame = decoder.getImage();
            return new DecodedGifImage(decoder, firstFrame.getWidth(), firstFrame.getHeight(), decoder.getLoopCount()); //loopCount == 0 == infinite loops | loopCount > 0 == number of loops
        } catch (Exception ex) {
            LOGGER.error("[KONKRETE] Failed to decode GIF image: " + gifName, ex);
        }
        return null;
    }

    /** Delivers the gif frames to the texture resource owner. */
    public static void deliverGifFrames(@NotNull GifDecoder decoder, @NotNull String gifName, @NotNull Consumer<GifFrame> frameDelivery) {
        int gifFrameCount = decoder.getFrameCount();
        int i = 0;
        int index = 0;
        while (i < gifFrameCount) {
            try {
                double delay = decoder.getDelay(i);
                BufferedImage image = decoder.getFrame(i);
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                ImageIO.write(image, "PNG", os);
                ByteArrayInputStream bis = new ByteArrayInputStream(os.toByteArray());
                frameDelivery.accept(new GifFrame(index, bis, (long)delay, os));
                index++;
            } catch (Exception ex) {
                LOGGER.error("[KONKRETE] Failed to get frame '" + i + "' of GIF image '" + gifName + "!", ex);
            }
            i++;
        }
    }

    /** Carries {@code GifFrame} data between validated stages of the texture resource. */
    public static class GifFrame implements AutoCloseable {

        /** Current index state for this texture resource instance. */
        protected final int index;
        /** Current frame input stream state for this texture resource instance. */
        protected final ByteArrayInputStream frameInputStream;
        /** Current delay ms state for this texture resource instance. */
        protected final long delayMs;
        /** Current close after loading state for this texture resource instance. */
        protected final ByteArrayOutputStream closeAfterLoading;
        /** Holds the textureEntry handle whose lifecycle follows this texture resource instance. */
        protected final TextureManagerEntry<NativeImage, DynamicTexture> textureEntry = TextureManagerEntry.dynamicTexture();

        /** Initializes a new {@code GifFrame} for texture resource use. */
        protected GifFrame(int index, ByteArrayInputStream frameInputStream, long delayMs, ByteArrayOutputStream closeAfterLoading) {
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

    /** Carries {@code DecodedGifImage} data between validated stages of the texture resource. */
    public record DecodedGifImage(@NotNull GifDecoder decoder, int imageWidth, int imageHeight, int numPlays) {

    }

}
