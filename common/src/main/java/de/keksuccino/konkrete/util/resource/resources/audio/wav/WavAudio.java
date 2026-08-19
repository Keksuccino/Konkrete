package de.keksuccino.konkrete.util.resource.resources.audio.wav;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.konkrete.util.CloseableUtils;
import de.keksuccino.konkrete.util.WebUtils;
import de.keksuccino.konkrete.util.input.TextValidators;
import de.keksuccino.konkrete.util.resource.resources.audio.ALAudio;
import de.keksuccino.konkrete.util.resource.resources.audio.AudioPlayTimeTracker;
import de.keksuccino.konkrete.util.resource.resources.audio.AudioResourceReloadTracker;
import de.keksuccino.konkrete.util.resource.resources.audio.IAudio;
import de.keksuccino.konkrete.util.resource.resources.audio.OpenAlAudioClipFactory;
import de.keksuccino.konkrete.util.threading.KonkreteThreads;
import de.keksuccino.melody.resources.audio.openal.ALAudioBuffer;
import de.keksuccino.melody.resources.audio.openal.ALAudioClip;
import de.keksuccino.melody.resources.audio.openal.ALErrorHandler;
import de.keksuccino.melody.resources.audio.openal.ALUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.sounds.SoundSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.openal.AL10;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/** Owns asynchronous WAV decoding and its OpenAL playback source. */
@SuppressWarnings("unused")
public class WavAudio implements IAudio, ALAudio {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final int AL_SEC_OFFSET_KONKRETE = 0x1024;

    /** Current clip state for this audio resource instance. */
    @Nullable
    protected volatile ALAudioClip clip;
    /** Owned audio buffer state for this audio resource instance. */
    @Nullable
    protected volatile ALAudioBuffer audioBuffer;
    /** Original resource-pack identifier, or null when another source kind is used. */
    protected Identifier sourceLocation;
    /** Holds the sourceFile handle whose lifecycle follows this audio resource instance. */
    protected File sourceFile;
    /** Original web URL, or null when another source kind is used. */
    protected String sourceURL;
    /** Current duration state for this audio resource instance. */
    protected volatile float duration = 0.0f;
    /** Current play time tracker state for this audio resource instance. */
    protected final AudioPlayTimeTracker playTimeTracker = new AudioPlayTimeTracker();
    /** Whether decoded currently applies to this audio resource instance. */
    protected volatile boolean decoded = false;
    /** Whether loading completed currently applies to this audio resource instance. */
    protected volatile boolean loadingCompleted = false;
    /** Whether loading failed currently applies to this audio resource instance. */
    protected volatile boolean loadingFailed = false;
    /** Whether retry when open al ready currently applies to this audio resource instance. */
    protected volatile boolean retryWhenOpenAlReady = false;
    /** Whether closed currently applies to this audio resource instance. */
    protected volatile boolean closed = false;

    /** Creates the location audio resource variant. */
    @NotNull
    public static WavAudio location(@NotNull Identifier location) {
        return location(location, null);
    }

    /** Creates the location audio resource variant. */
    @NotNull
    public static WavAudio location(@NotNull Identifier location, @Nullable WavAudio writeTo) {

        Objects.requireNonNull(location);
        WavAudio audio = (writeTo != null) ? writeTo : new WavAudio();

        audio.sourceLocation = location;

        //Clips need to get created on the main thread, so make sure we're in the correct thread
        RenderSystem.assertOnRenderThread();

        if (isOpenAlNotReadyOrReloading()) {
            failBecauseOpenAlNotReady(audio, location.toString());
            return audio;
        }

        ALAudioClip clip = OpenAlAudioClipFactory.createSafe();
        if (clip == null) {
            failBecauseOpenAlReload(audio, location.toString(), "failed to allocate OpenAL source");
            return audio;
        }

        try {
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(location);
            if (resource.isPresent()) {
                InputStream in = resource.get().open();
                of(in, location.toString(), audio, clip);
            }
        } catch (Exception ex) {
            audio.loadingFailed = true;
            LOGGER.error("[KONKRETE] Failed to read WAV audio from Identifier: " + location, ex);
        }

        return audio;

    }

    /** Creates the local audio resource variant. */
    @NotNull
    public static WavAudio local(@NotNull File wavAudioFile) {
        return local(wavAudioFile, null);
    }

    /** Creates the local audio resource variant. */
    @NotNull
    public static WavAudio local(@NotNull File wavAudioFile, @Nullable WavAudio writeTo) {

        Objects.requireNonNull(wavAudioFile);
        WavAudio audio = (writeTo != null) ? writeTo : new WavAudio();

        audio.sourceFile = wavAudioFile;

        if (!wavAudioFile.isFile()) {
            audio.loadingFailed = true;
            LOGGER.error("[KONKRETE] Failed to read WAV audio from file! File not found: " + wavAudioFile.getPath());
            return audio;
        }

        //Clips need to get created on the main thread, so make sure we're in the correct thread
        RenderSystem.assertOnRenderThread();

        if (isOpenAlNotReadyOrReloading()) {
            failBecauseOpenAlNotReady(audio, wavAudioFile.getPath());
            return audio;
        }

        ALAudioClip clip = OpenAlAudioClipFactory.createSafe();
        if (clip == null) {
            failBecauseOpenAlReload(audio, wavAudioFile.getPath(), "failed to allocate OpenAL source");
            return audio;
        }

        try {
            InputStream in = new FileInputStream(wavAudioFile);
            of(in, wavAudioFile.getPath(), audio, clip);
        } catch (Exception ex) {
            audio.loadingFailed = true;
            LOGGER.error("[KONKRETE] Failed to read WAV audio from file: " + wavAudioFile.getPath(), ex);
        }

        return audio;

    }

    /** Creates the web audio resource variant. */
    @NotNull
    public static WavAudio web(@NotNull String wavAudioURL) {
        return web(wavAudioURL, null);
    }

    /** Creates the web audio resource variant. */
    @NotNull
    public static WavAudio web(@NotNull String wavAudioURL, @Nullable WavAudio writeTo) {

        Objects.requireNonNull(wavAudioURL);
        WavAudio audio = (writeTo != null) ? writeTo : new WavAudio();

        audio.sourceURL = wavAudioURL;

        if (!TextValidators.BASIC_URL_TEXT_VALIDATOR.get(wavAudioURL)) {
            audio.loadingFailed = true;
            LOGGER.error("[KONKRETE] Failed to read WAV audio from URL! Invalid URL: " + wavAudioURL);
            return audio;
        }

        //Clips need to get created on the main thread, so make sure we're in the correct thread
        RenderSystem.assertOnRenderThread();

        if (isOpenAlNotReadyOrReloading()) {
            failBecauseOpenAlNotReady(audio, wavAudioURL);
            return audio;
        }

        ALAudioClip clip = OpenAlAudioClipFactory.createSafe();
        if (clip == null) {
            failBecauseOpenAlReload(audio, wavAudioURL, "failed to allocate OpenAL source");
            return audio;
        }

        KonkreteThreads.startDaemonThread(() -> {
            try {
                InputStream in = WebUtils.openResourceStream(wavAudioURL, WebUtils.WebResourceType.AUDIO);
                if (in == null) throw new NullPointerException("Web resource input stream was NULL!");
                of(in, wavAudioURL, audio, clip);
            } catch (Exception ex) {
                audio.loadingFailed = true;
                LOGGER.error("[KONKRETE] Failed to read WAV audio from URL: " + wavAudioURL, ex);
            }
        }, "WavAudio-WebLoader");

        return audio;

    }

    /** Builds a resource value for the audio resource. */
    @NotNull
    public static WavAudio of(@NotNull InputStream in, @Nullable String wavAudioName, @Nullable WavAudio writeTo, @Nullable ALAudioClip clip) {
        String name = (wavAudioName != null) ? wavAudioName : "[Generic InputStream Source]";
        WavAudio audio = (writeTo != null) ? writeTo : new WavAudio();

        // Clips need to get created on the main thread, so make sure we're in the correct thread
        if (clip == null) RenderSystem.assertOnRenderThread();

        if (isOpenAlNotReadyOrReloading()) {
            CloseableUtils.closeQuietly(clip);
            failBecauseOpenAlNotReady(audio, name);
            return audio;
        }

        audio.clip = (clip != null) ? clip : OpenAlAudioClipFactory.createSafe();

        ALAudioClip cachedClip = audio.clip;
        if (cachedClip == null) {
            failBecauseOpenAlReload(audio, name, "failed to allocate OpenAL source");
            return audio;
        }
        if (!audio.configureNonPositionalSource(name)) {
            return audio;
        }

        KonkreteThreads.startDaemonThread(() -> {
            AudioInputStream stream = null;
            ByteArrayInputStream byteIn = null;
            try {
                // Read the full stream into a byte array
                byte[] fullData = in.readAllBytes();

                // Read header first - WAV header is minimum 44 bytes
                if (fullData.length >= 44) {
                    try {
                        // Create a new input stream that wraps your fullData
                        WavHeader header = null;
                        InputStream headerStream = null;
                        try {
                            headerStream = new ByteArrayInputStream(fullData);
                            header = WavHeader.read(headerStream);
                        } catch (IOException ex) {
                            LOGGER.error("[KONKRETE] Failed to read WAV header of WavAudio: " + name, ex);
                        }
                        CloseableUtils.closeQuietly(headerStream);
                        float calculatedDuration = 0;
                        if (header != null) calculatedDuration = header.getDurationInSeconds();
                        if (calculatedDuration > 0) {
                            audio.duration = calculatedDuration;
                        } else {
                            LOGGER.warn("[KONKRETE] Invalid WAV header duration calculated for: " + name);
                        }
                    } catch (Exception ex) {
                        LOGGER.warn("[KONKRETE] Failed to read WAV header of WavAudio: " + name, ex);
                    }
                } else {
                    LOGGER.warn("[KONKRETE] WAV file too small, missing header data: " + name);
                }

                // Continue with normal audio loading
                if (!audio.canContinueBackgroundLoading(cachedClip, name)) {
                    return;
                }
                byteIn = new ByteArrayInputStream(fullData);
                stream = AudioSystem.getAudioInputStream(byteIn);
                ByteBuffer byteBuffer = ALUtils.readStreamIntoBuffer(stream);
                ALAudioBuffer audioBuffer = new ALAudioBuffer(byteBuffer, stream.getFormat());
                audio.audioBuffer = audioBuffer;
                if (!audio.tryAttachDecodedBuffer(cachedClip, audioBuffer, name)) {
                    return;
                }
                audio.loadingFailed = false;
                audio.retryWhenOpenAlReady = false;
                audio.decoded = true;
                audio.loadingCompleted = true;
            } catch (Exception ex) {
                audio.loadingFailed = true;
                LOGGER.error("[KONKRETE] Failed to read WAV audio: " + name, ex);
            }
            CloseableUtils.closeQuietly(stream);
            CloseableUtils.closeQuietly(in);
            CloseableUtils.closeQuietly(byteIn);
        }, "WavAudio-Decoder");

        return audio;
    }

    /** Builds a resource value for the audio resource. */
    @NotNull
    public static WavAudio of(@NotNull InputStream in) {
        return of(in, null, null, null);
    }

    /** Initializes a new {@code WavAudio} for audio resource use. */
    protected WavAudio() {
        AudioResourceReloadTracker.registerAudioInstance(this);
    }

    private static boolean isOpenAlNotReadyOrReloading() {
        return !ALUtils.isOpenAlReady();
    }

    /** Returns the clip, or {@code null} when it is not available. */
    @Nullable
    public ALAudioClip getClip() {
        return this.clip;
    }

    /** Starts playback for this audio resource. */
    @Override
    public void play() {
        this.forClip(alAudioClip -> {
            try {
                alAudioClip.play();
                this.playTimeTracker.onPlay();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    /** Returns whether playing. */
    @Override
    public boolean isPlaying() {
        try {
            ALAudioClip cached = this.clip;
            if (cached != null) return cached.isPlaying();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }

    /** Pauses playback without releasing this audio resource. */
    @Override
    public void pause() {
        this.forClip(alAudioClip -> {
            try {
                alAudioClip.pause();
                this.playTimeTracker.onPause();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    /** Returns whether paused. */
    @Override
    public boolean isPaused() {
        try {
            ALAudioClip cached = this.clip;
            if (cached != null) return cached.isPaused();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }

    /** Stops playback and resets this audio resource. */
    @Override
    public void stop() {
        this.forClip(alAudioClip -> {
            try {
                alAudioClip.stop();
                this.playTimeTracker.onStop();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    /** Sets the volume used by subsequent audio resource operations. */
    @Override
    public void setVolume(float volume) {
        this.forClip(alAudioClip -> {
            try {
                alAudioClip.setVolume(volume);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    /** Returns the volume used by this audio resource instance. */
    @Override
    public float getVolume() {
        ALAudioClip cached = this.clip;
        return (cached != null) ? cached.getVolume() : 0.0F;
    }

    /** Sets the sound channel used by subsequent audio resource operations. */
    public void setSoundChannel(@NotNull SoundSource channel) {
        this.forClip(oggAudioClip -> oggAudioClip.setSoundChannel(channel));
    }

    /** Returns the sound channel used by this audio resource instance. */
    @NotNull
    public SoundSource getSoundChannel() {
        ALAudioClip cached = this.clip;
        return (cached != null) ? cached.getSoundChannel() : SoundSource.MASTER;
    }

    /** Returns the duration used by this audio resource instance. */
    @Override
    public float getDuration() {
        return this.duration;
    }

    /** Returns the play time used by this audio resource instance. */
    @Override
    public float getPlayTime() {
        return this.playTimeTracker.getCurrentPlayTime();
    }

    /** Sets the play time used by subsequent audio resource operations. */
    @Override
    public void setPlayTime(float playTime) {
        float clamped = playTime;
        if (!Float.isFinite(clamped) || clamped < 0.0F) clamped = 0.0F;
        float duration = this.getDuration();
        if (duration > 0.0F) {
            clamped = Math.min(clamped, duration);
        }
        ALAudioClip cachedClip = this.clip;
        if (cachedClip == null || cachedClip.isClosed() || !cachedClip.isValidOpenAlSource()) {
            return;
        }
        int source = this.getALSource();
        if (source == 0) {
            return;
        }
        boolean playing = this.isPlaying();
        boolean paused = this.isPaused();
        try {
            AL10.alSourcef(source, AL_SEC_OFFSET_KONKRETE, clamped);
            ALErrorHandler.checkOpenAlError();
            this.playTimeTracker.setPlayTime(clamped, paused || !playing);
        } catch (Exception ex) {
            LOGGER.error("[KONKRETE] Failed to seek WAV audio preview play time!", ex);
        }
    }

    /** Creates a audio resource value for the clip. */
    protected void forClip(@NotNull Consumer<ALAudioClip> clip) {
        ALAudioClip cached = this.clip;
        if (cached != null) clip.accept(cached);
    }

    private static void failBecauseOpenAlNotReady(@NotNull WavAudio audio, @NotNull String sourceName) {
        audio.loadingFailed = true;
        audio.loadingCompleted = false;
        audio.decoded = false;
        audio.retryWhenOpenAlReady = true;
        LOGGER.warn("[KONKRETE] Delaying WAV audio load because OpenAL is not ready yet or still reloading. It will retry automatically once ready again: " + sourceName);
    }

    private static void failBecauseOpenAlReload(@NotNull WavAudio audio, @NotNull String sourceName, @NotNull String reason) {
        audio.loadingFailed = true;
        audio.loadingCompleted = false;
        audio.decoded = false;
        audio.retryWhenOpenAlReady = true;
        LOGGER.warn("[KONKRETE] Delaying WAV audio load because OpenAL is reloading (" + reason + "). It will retry automatically once ready again: " + sourceName);
    }

    /** Opens the resource for the audio resource. */
    @Override
    public @Nullable InputStream open() throws IOException {
        if (this.sourceURL != null) return WebUtils.openResourceStream(this.sourceURL, WebUtils.WebResourceType.AUDIO);
        if (this.sourceFile != null) return new FileInputStream(this.sourceFile);
        if (this.sourceLocation != null) return Minecraft.getInstance().getResourceManager().open(this.sourceLocation);
        return null;
    }

    /** Returns whether ready. */
    @Override
    public boolean isReady() {
        if (this.closed || !this.decoded) return false;
        ALAudioClip cachedClip = this.clip;
        if (cachedClip != null) {
            if (cachedClip.isClosed()) return false;
            if (cachedClip.isValidOpenAlSource()) return true;
        }
        return false;
    }

    /** Returns whether loading completed. */
    @Override
    public boolean isLoadingCompleted() {
        return !this.closed && !this.loadingFailed && this.loadingCompleted;
    }

    /** Returns whether loading failed. */
    @Override
    public boolean isLoadingFailed() {
        return this.loadingFailed;
    }

    /** Returns whether loading failure retryable. */
    @Override
    public boolean isLoadingFailureRetryable() {
        return this.retryWhenOpenAlReady;
    }

    /** Returns whether valid open al source. */
    public boolean isValidOpenAlSource() {
        ALAudioClip cached = this.clip;
        return (cached != null) && cached.isValidOpenAlSource();
    }

    /** Returns the al source used by this audio resource instance. */
    public int getALSource() {
        if (this.clip == null) return 0;
        try {
            Field f = ALAudioClip.class.getDeclaredField("source");
            f.setAccessible(true);
            return f.getInt(this.clip);
        } catch (Exception ex) {
            LOGGER.error("[KONKRETE] Failed to get AL source in WavAudio!", ex);
        }
        return 0;
    }

    private boolean configureNonPositionalSource(@NotNull String sourceName) {
        ALAudioClip cachedClip = this.clip;
        if ((cachedClip == null) || this.closed || cachedClip.isClosed()) return false;
        if (isOpenAlNotReadyOrReloading()) {
            failBecauseOpenAlNotReady(this, sourceName);
            return false;
        }
        if (!cachedClip.isValidOpenAlSource()) {
            failBecauseOpenAlReload(this, sourceName, "OpenAL source became invalid");
            return false;
        }
        int source = this.getALSource();
        if (source == 0) {
            failBecauseOpenAlReload(this, sourceName, "OpenAL source handle was 0");
            return false;
        }
        try {
            AL10.alSourcei(source, AL10.AL_SOURCE_RELATIVE, AL10.AL_TRUE);
            AL10.alSource3f(source, AL10.AL_POSITION, 0.0F, 0.0F, 0.0F);
            AL10.alSourcef(source, AL10.AL_ROLLOFF_FACTOR, 0.0F);
            ALErrorHandler.checkOpenAlError();
            return true;
        } catch (Exception ex) {
            failBecauseOpenAlReload(this, sourceName, "failed to configure non-positional source");
            LOGGER.debug("[KONKRETE] WAV source configuration error details: " + sourceName, ex);
        }
        return false;
    }

    private boolean canContinueBackgroundLoading(@NotNull ALAudioClip targetClip, @NotNull String sourceName) {
        if (this.closed || (this.clip != targetClip) || targetClip.isClosed()) return false;
        if (isOpenAlNotReadyOrReloading()) {
            failBecauseOpenAlNotReady(this, sourceName);
            return false;
        }
        if (!targetClip.isValidOpenAlSource()) {
            failBecauseOpenAlReload(this, sourceName, "OpenAL source became invalid");
            return false;
        }
        return true;
    }

    private boolean tryAttachDecodedBuffer(@NotNull ALAudioClip targetClip, @NotNull ALAudioBuffer decodedBuffer, @NotNull String sourceName) {
        if (!this.canContinueBackgroundLoading(targetClip, sourceName)) return false;
        Integer preparedBuffer = decodedBuffer.getSource();
        if ((preparedBuffer == null) || !decodedBuffer.isValidOpenAlSource()) {
            failBecauseOpenAlReload(this, sourceName, "failed to prepare OpenAL buffer");
            return false;
        }
        try {
            targetClip.setStaticBuffer(decodedBuffer);
            if (!targetClip.isValidOpenAlSource()) {
                failBecauseOpenAlReload(this, sourceName, "OpenAL source became invalid while attaching decoded audio");
                return false;
            }
            return true;
        } catch (Exception ex) {
            failBecauseOpenAlReload(this, sourceName, "failed to attach decoded audio buffer");
            LOGGER.debug("[KONKRETE] WAV buffer attach error details: " + sourceName, ex);
        }
        return false;
    }

    /** Closes the OpenAL clip, deletes its buffer, and prevents reload retries; repeated calls are harmless. */
    @Override
    public void close() {
        this.closed = true;
        this.retryWhenOpenAlReady = false;
        try {
            ALAudioClip cachedClip = this.clip;
            if (cachedClip != null) cachedClip.close();
        } catch (Exception ex) {
            LOGGER.error("[KONKRETE] Failed to close WAV audio clip!", ex);
        }
        this.clip = null;
        try {
            ALAudioBuffer cachedBuffer = this.audioBuffer;
            if (cachedBuffer != null) cachedBuffer.delete();
        } catch (Exception ex) {
            LOGGER.error("[KONKRETE] Failed to delete WAV audio buffer!", ex);
        }
        this.audioBuffer = null;
        this.decoded = false;
    }

    /** Returns whether closed. */
    @Override
    public boolean isClosed() {
        if (!this.closed) {
            if (this.retryWhenOpenAlReady) {
                try {
                    if (!isOpenAlNotReadyOrReloading()) {
                        this.close();
                    }
                } catch (Exception ignored) {
                }
            }
            ALAudioClip cachedClip = this.clip;
            if ((cachedClip != null) && !cachedClip.isValidOpenAlSource()) {
                this.close();
            }
        }
        return this.closed;
    }

}
