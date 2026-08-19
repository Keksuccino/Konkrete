package de.keksuccino.konkrete.util.rinku;

import de.keksuccino.rinku.Rinku;
import de.keksuccino.rinku.RinkuBrowser;
import de.keksuccino.konkrete.util.rendering.RenderingUtils;
import de.keksuccino.konkrete.util.rendering.ui.MouseButtonCaptureOwner;
import de.keksuccino.konkrete.util.rendering.ui.UIBase;
import de.keksuccino.konkrete.util.rendering.ui.widget.NavigatableWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.Closeable;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Renderable Minecraft widget that owns one off-screen Rinku browser. Rinku must be initialized before construction.
 * Rendering, resize, input, and {@link #close()} belong on Minecraft's client/render thread; closing releases the
 * native browser but never disposes the process-wide bridge.
 */
@SuppressWarnings("unused")
public class WrappedRinkuBrowser extends AbstractWidget implements Closeable, NavigatableWidget, MouseButtonCaptureOwner {

    /** Logger available to specialized browser wrappers. */
    protected static final Logger LOGGER = LogManager.getLogger();

    /** Native Rinku browser owned by this wrapper. */
    protected final RinkuBrowser browser;
    private final BrowserAudioMuteController audioMuteController;
    /** Active Minecraft client. */
    protected final Minecraft minecraft = Minecraft.getInstance();
    /** Generation counter used to discard stale navigation callbacks. */
    protected final AtomicLong mainFrameNavigationGeneration = new AtomicLong();
    /** Focus and mouse-button capture state for forwarded input. */
    protected final BrowserInputState inputState = new BrowserInputState();
    /** Expected URL for the current main-frame navigation. */
    @Nullable protected volatile String expectedMainFrameUrl;
    /** Whether user input is forwarded to Chromium. */
    protected boolean interactable = true;
    /** Widget render opacity. */
    protected float opacity = 1.0F;
    /** Whether the global browser handler owns ticking and cleanup. */
    protected boolean autoHandle = true;
    /** Caller-selected volume before Minecraft's master volume is applied. */
    protected volatile float volume = 1.0F;
    /** Whether page videos should request fullscreen. */
    protected volatile boolean fullscreenAllVideos = false;
    /** Whether page videos should start after navigation. */
    protected volatile boolean autoPlayAllVideosOnLoad = true;
    /** Whether page videos should loop. */
    protected volatile boolean loopAllVideos = false;
    /** Whether native page video controls should be hidden. */
    protected volatile boolean hideVideoControls = false;
    /** Stable identifier used by global handler registries. */
    protected final UUID genericIdentifier = UUID.randomUUID();
    /** Whether this wrapper has released its browser. */
    protected volatile boolean closed = false;

    // Track if initialization is complete for this browser
    private volatile boolean initialized = false;

    /** Creates an owned browser at default bounds on the client thread after Rinku initialization. */
    @NotNull
    public static WrappedRinkuBrowser build(@NotNull String url, boolean transparent, boolean autoHandle, @Nullable Consumer<Boolean> loadListener) {
        return build(url, transparent, autoHandle, false, loadListener);
    }

    /** Creates a browser wrapper with an initial native-audio mute state, failing when Rinku is absent or not ready. */
    @NotNull
    public static WrappedRinkuBrowser build(@NotNull String url, boolean transparent, boolean autoHandle, boolean muted, @Nullable Consumer<Boolean> loadListener) {
        if (!RinkuUtil.isRinkuLoaded()) throw new IllegalStateException("Rinku is unavailable");
        BrowserHandler.init();
        if (!Rinku.isInitialized()) throw new IllegalStateException("Rinku has not completed initialization; create browsers from a Rinku initialization callback");
        WrappedRinkuBrowser b = new WrappedRinkuBrowser(url, transparent, muted, loadListener);
        b.autoHandle = autoHandle;
        return b;
    }

    /** Creates an owned positioned browser on the client thread after Rinku initialization. */
    @NotNull
    public static WrappedRinkuBrowser build(@NotNull String url, boolean transparent, boolean autoHandle, int x, int y, int width, int height, @Nullable Consumer<Boolean> loadListener) {
        WrappedRinkuBrowser b = build(url, transparent, autoHandle, loadListener);
        b.setSize(width, height);
        b.setPosition(x, y);
        return b;
    }

    /** Creates an extensible owned wrapper and registers its load lifecycle before native browser creation. */
    protected WrappedRinkuBrowser(@NotNull String url, boolean transparent, boolean muted, @Nullable Consumer<Boolean> loadListener) {

        super(0, 0, 0, 0, Component.empty());

        if (!RinkuUtil.isRinkuLoaded()) throw new IllegalStateException("Rinku is unavailable");
        BrowserHandler.init();
        if (!Rinku.isInitialized()) throw new IllegalStateException("Rinku has not completed initialization; create browsers from a Rinku initialization callback");

        this.expectedMainFrameUrl = url;

        // Initialize the global message router if not already done
        ActionBridge.initialize();

        // Register the custom load listener handler to later register multiple load listeners.
        // Calling this method multiple times is fine, because there can only be one default listener active.
        BrowserLoadEventListenerManager.getInstance().initialize();

        this.browser = Rinku.createBrowser(url, transparent);
        this.audioMuteController = new BrowserAudioMuteController(this.browser::setAudioMuted, muted);

        String browserId = this.getIdentifier();

        BrowserLoadEventListenerManager.getInstance().registerPersistentListenerForBrowser(this, success -> {
            if (success) {
                initialized = true;
                // Apply settings once the page is loaded
                applyInitialSettings();
                // Inject the Konkrete JavaScript API
                injectJavaScriptAPI();
            } else {
                LOGGER.error("[KONKRETE] WrappedRinkuBrowser browser page failed to load (ID: {})", browserId, new Exception());
                initialized = false;
            }
        });

        if (loadListener != null) {
            BrowserLoadEventListenerManager.getInstance().registerListenerForBrowser(this, loadListener);
        }

        this.setVolume(this.volume);
        this.setSize(200, 200);
        this.setPosition(0, 0);

    }

    /** Creates an initially unmuted extensible wrapper. */
    protected WrappedRinkuBrowser(@NotNull String url, boolean transparent, @Nullable Consumer<Boolean> loadListener) {
        this(url, transparent, false, loadListener);
    }

    /** Applies the configured media settings after a successful main-frame load. */
    protected void applyInitialSettings() {
        this.setVolume(this.volume);
        this.setLoopAllVideos(this.loopAllVideos);
        this.setHideVideoControls(this.hideVideoControls);
        this.setAutoPlayAllVideosOnLoad(this.autoPlayAllVideosOnLoad);
        this.audioMuteController.reapply();
    }

    /** Injects the configured Konkrete JavaScript bridge API into the loaded main frame. */
    protected void injectJavaScriptAPI() {
        try {
            LOGGER.info("[KONKRETE] Injecting Konkrete JavaScript API into browser (ID: {})", this.getIdentifier());
            if (this.closed) return;
            this.browser.executeJavaScript(ActionBridge.getJavaScriptApi(), this.browser.getURL(), 0);
            LOGGER.debug("[KONKRETE] JavaScript API injection completed for browser {}", this.getIdentifier());
        } catch (Exception ex) {
            LOGGER.error("[KONKRETE] Failed to inject the JavaScript API", ex);
        }
    }

    /** Extracts this browser's current texture into the GUI render state. */
    @Override
    protected void extractWidgetRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {

        if (this.closed) {
            return;
        }

        try {

            if (this.autoHandle) BrowserHandler.notifyHandler(this.genericIdentifier.toString(), this);

            Identifier frameLocation = this.browser.getTextureIdentifier();
            if (frameLocation == null) {
                return;
            }

            RenderingUtils.setShaderColor(graphics, 1.0F, 1.0F, 1.0F, this.opacity);
            try {
                graphics.blit(RenderPipelines.GUI_TEXTURED, frameLocation, this.getX(), this.getY(), 0.0F, 0.0F, this.getWidth(), this.getHeight(), this.getWidth(), this.getHeight());
            } finally {
                RenderingUtils.setShaderColor(graphics, 1.0F, 1.0F, 1.0F, 1.0F);
            }

        } catch (Exception ex) {
            LOGGER.error("[KONKRETE] Failed to render RinkuBrowser!", ex);
        }

    }

    /** Reapplies effective volume after a Minecraft sound-category change. */
    public void onVolumeUpdated(@NotNull SoundSource soundSource, float newVolume) {
        this.setVolume(this.volume);
    }

    /** {@inheritDoc} */
    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean isDoubleClick) {
        return this.mouseClicked(event.x(), event.y(), event.button());
    }

    /** Forwards a legacy coordinate-based mouse press to Chromium. */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = this.inputState.forwardMousePress(this.interactable, this.isMouseOver(mouseX, mouseY), button, () -> this.browser.sendMousePress(this.convertMouseX(mouseX), this.convertMouseY(mouseY), button));
        this.setFocused(handled);
        return handled;
    }

    /** {@inheritDoc} */
    @Override
    public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        return this.mouseReleased(event.x(), event.y(), event.button());
    }

    /** Forwards a legacy coordinate-based mouse release to Chromium when captured. */
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return this.inputState.forwardMouseRelease(button, () -> {
            this.browser.sendMouseRelease(this.convertMouseX(mouseX), this.convertMouseY(mouseY), button);
            this.browser.setFocus(this.inputState.isFocused());
        });
    }

    /** {@inheritDoc} */
    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (!this.interactable) return;
        this.browser.sendMouseMove(this.convertMouseX(mouseX), this.convertMouseY(mouseY));
    }

    /** {@inheritDoc} */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return this.inputState.forwardMouseScroll(this.interactable, this.isMouseOver(mouseX, mouseY), () -> this.browser.sendMouseWheel(this.convertMouseX(mouseX), this.convertMouseY(mouseY), scrollY, 0));
    }

    /** {@inheritDoc} */
    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        return this.keyPressed(event.key(), event.scancode(), event.modifiers());
    }

    /** Forwards a legacy key press to Chromium while this browser owns focus. */
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return this.inputState.forwardKeyboardInput(this.interactable, () -> {
            this.browser.sendKeyPress(keyCode, scanCode, modifiers);
            this.browser.setFocus(true);
        });
    }

    /** {@inheritDoc} */
    @Override
    public boolean keyReleased(net.minecraft.client.input.KeyEvent event) {
        return this.keyReleased(event.key(), event.scancode(), event.modifiers());
    }

    /** Forwards a legacy key release to Chromium while this browser owns focus. */
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return this.inputState.forwardKeyboardInput(this.interactable, () -> {
            this.browser.sendKeyRelease(keyCode, scanCode, modifiers);
            this.browser.setFocus(true);
        });
    }

    /** {@inheritDoc} */
    @Override
    public boolean charTyped(net.minecraft.client.input.CharacterEvent event) {
        return this.charTyped((char)event.codepoint(), 0);
    }

    /** Forwards a legacy character event to Chromium while this browser owns focus. */
    public boolean charTyped(char codePoint, int modifiers) {
        return this.inputState.forwardCharacterInput(this.interactable, codePoint, () -> {
            this.browser.sendKeyTyped(codePoint, modifiers);
            this.browser.setFocus(true);
        });
    }

	/** {@inheritDoc} */
	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		return this.interactable && UIBase.isXYInArea(mouseX, mouseY, this.getX(), this.getY(), this.getWidth(), this.getHeight());
	}

    /** This media surface has no useful narration content. */
    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {
    }

    /** Updates widget and native browser dimensions. */
    @Override
    public void setSize(int width, int height) {
        super.setSize(width, height);
        this.browser.resize(this.convertWidth(width), this.convertHeight(height));
    }

    /** Updates widget and native browser width. */
    @Override
    public void setWidth(int width) {
        this.width = width;
        this.setSize(this.width, this.height);
    }

    /** Updates widget and native browser height. */
    @Override
    public void setHeight(int height) {
        this.height = height;
        this.setSize(this.width, this.height);
    }

    /** Converts a GUI-space mouse X coordinate to browser pixels. */
    protected int convertMouseX(double mouseX) {
        return (int)((mouseX - (double)this.getX()) * this.minecraft.getWindow().getGuiScale());
    }

    /** Converts a GUI-space mouse Y coordinate to browser pixels. */
    protected int convertMouseY(double mouseY) {
        return (int)((mouseY - (double)this.getY()) * this.minecraft.getWindow().getGuiScale());
    }

    /** Converts a GUI-space width to browser pixels. */
    protected int convertWidth(double width) {
        return (int) (width * this.minecraft.getWindow().getGuiScale());
    }

    /** Converts a GUI-space height to browser pixels. */
    protected int convertHeight(double height) {
        return (int) (height * this.minecraft.getWindow().getGuiScale());
    }

    /** Sets caller volume before Minecraft's master-volume multiplier. */
    public void setVolume(float volume) {
        this.volume = volume;
        if (initialized) {
            String code = "document.querySelectorAll('audio, video').forEach(el => el.volume = " + this.getActualVolume() + ");";
            this.browser.executeJavaScript(code, this.browser.getURL(), 0);
        }
    }

    /** Returns caller volume before Minecraft's master-volume multiplier. */
    public float getVolume() {
        return this.volume;
    }

    /** Returns the effective volume after Minecraft's master-volume multiplier. */
    public float getActualVolume() {
        float actualVolume = this.volume;
        float soundSourceVolume = Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.MASTER);
        actualVolume *= soundSourceVolume;
        return actualVolume;
    }

    /** Enables or disables browser input forwarding. */
    public void setInteractable(boolean interactable) {
        if (this.interactable == interactable) return;
        this.interactable = interactable;
        if (!this.interactable) {
            // Keep captured buttons until their matching releases, otherwise Chromium can retain a stuck pressed button.
            this.setFocused(false);
        }
    }

    /** Returns whether browser input forwarding is enabled. */
    public boolean isInteractable() {
        return this.interactable;
    }

    /** Changes native browser focus. */
    public void setBrowserFocused(boolean browserFocused) {
        this.setFocused(browserFocused);
    }

    /** Returns whether the native browser owns keyboard focus. */
    public boolean isBrowserFocused() {
        return this.inputState.isFocused();
    }

    /** Updates widget, input-state, and native-browser focus atomically. */
    @Override
    public void setFocused(boolean focused) {
        boolean acceptedFocus = focused && this.interactable;
        super.setFocused(acceptedFocus);
        this.inputState.setFocused(acceptedFocus);
        if (!this.closed && (this.browser != null)) this.browser.setFocus(acceptedFocus);
    }

    /** Enables or disables global handler ownership. */
    public void setAutoHandle(boolean autoHandle) {
        this.autoHandle = autoHandle;
    }

    /** Returns whether the global handler owns this browser. */
    public boolean isAutoHandle() {
        return this.autoHandle;
    }

    /** Configures fullscreen requests for page videos. */
    public void setFullscreenAllVideos(boolean fullscreenAllVideos) {
        this.fullscreenAllVideos = fullscreenAllVideos;
        if (initialized) {
            String code = """
                    document.querySelectorAll('video').forEach(video => {
                        if (video.requestFullscreen) {
                            video.requestFullscreen().catch(err => console.error('Fullscreen error:', err));
                        } else if (video.webkitRequestFullscreen) { // Safari compatibility
                            video.webkitRequestFullscreen().catch(err => console.error('Fullscreen error (webkit):', err));
                        } else if (video.msRequestFullscreen) { // IE/Edge compatibility
                            video.msRequestFullscreen().catch(err => console.error('Fullscreen error (ms):', err));
                        }
                    });
                    """;
            if (this.fullscreenAllVideos) this.browser.executeJavaScript(code, this.browser.getURL(), 0);
        }
    }

    /** Returns whether page videos request fullscreen. */
    public boolean isFullscreenAllVideos() {
        return fullscreenAllVideos;
    }

    /** Configures automatic playback for page videos. */
    public void setAutoPlayAllVideosOnLoad(boolean autoPlayAllVideosOnLoad) {
        this.autoPlayAllVideosOnLoad = autoPlayAllVideosOnLoad;
        if (initialized) {
            String code = """
                    document.querySelectorAll('video').forEach(video => {
                        video.play(); // Start playing the video
                    });
                    """;
            if (this.autoPlayAllVideosOnLoad) this.browser.executeJavaScript(code, this.browser.getURL(), 0);
        }
    }

    /** Returns whether page videos start automatically. */
    public boolean isAutoPlayAllVideosOnLoad() {
        return autoPlayAllVideosOnLoad;
    }

    /** Changes the complete Chromium browser's native audio mute state. */
    public void setMuted(boolean muted) {
        this.audioMuteController.setMuted(muted);
    }

    /** Returns the complete Chromium browser's native audio mute state. */
    public boolean isMuted() {
        return this.audioMuteController.isMuted();
    }

    /**
     * @deprecated Use {@link #setMuted(boolean)}. The setting now mutes the complete Chromium browser instead of individual media elements.
     */
    @Deprecated(forRemoval = false)
    public void setMuteAllMediaOnLoad(boolean muted) {
        this.setMuted(muted);
    }

    /**
     * @deprecated Use {@link #isMuted()}.
     */
    @Deprecated(forRemoval = false)
    public boolean isMuteAllMediaOnLoad() {
        return this.isMuted();
    }

    /** Configures looping for every page video. */
    public void setLoopAllVideos(boolean loopAllVideos) {
        this.loopAllVideos = loopAllVideos;
        if (initialized) {
            String code = """
                    document.querySelectorAll('video').forEach(video => {
                        video.loop = %loop%; // Set video to loop
                    });
                    """.replace("%loop%", "" + this.loopAllVideos);
            this.browser.executeJavaScript(code, this.browser.getURL(), 0);
        }
    }

    /** Returns whether every page video loops. */
    public boolean isLoopAllVideos() {
        return loopAllVideos;
    }

    /** Configures visibility of native controls for every page video. */
    public void setHideVideoControls(boolean hideVideoControls) {
        this.hideVideoControls = hideVideoControls;
        if (initialized) {
            // More aggressive approach to hiding controls
            String codeRemove = """
                    document.querySelectorAll('video').forEach(video => {
                        // Multiple methods to ensure controls are hidden
                        video.removeAttribute('controls');
                        video.setAttribute('nocontrols', '');
                        video.setAttribute('controlslist', 'nodownload nofullscreen noremoteplayback');
                        video.controls = false;

                        // Add style to hide controls
                        const style = document.createElement('style');
                        style.textContent = `
                            video::-webkit-media-controls,
                            video::-webkit-media-controls-enclosure,
                            video::-webkit-media-controls-panel,
                            video::-webkit-media-controls-panel-container,
                            video::-webkit-media-controls-play-button,
                            video::-webkit-media-controls-overlay-play-button {
                                display: none !important;
                                opacity: 0 !important;
                                pointer-events: none !important;
                            }
                        `;
                        if (!document.head.querySelector('style#hide-video-controls')) {
                            style.id = 'hide-video-controls';
                            document.head.appendChild(style);
                        }
                    });
                    """;
            String codeAdd = """
                    document.querySelectorAll('video').forEach(video => {
                        if (!video.hasAttribute('controls')) {
                            video.setAttribute('controls', 'controls'); // Add controls
                        }
                        video.removeAttribute('nocontrols');
                        // Remove style if it exists
                        const style = document.head.querySelector('style#hide-video-controls');
                        if (style) {
                            document.head.removeChild(style);
                        }
                    });
                    """;
            this.browser.executeJavaScript(this.hideVideoControls ? codeRemove : codeAdd, this.browser.getURL(), 0);
        }
    }

    /** Returns whether native page-video controls are hidden. */
    public boolean isHideVideoControls() {
        return hideVideoControls;
    }

    /** Navigates backward when Chromium has history. */
    public void goBack() {
        if (this.browser.canGoBack()) {
            this.mainFrameNavigationGeneration.incrementAndGet();
            this.browser.goBack();
        }
        if (initialized) {
            this.setVolume(this.volume);
        }
    }

    /** Navigates forward when Chromium has history. */
    public void goForward() {
        if (this.browser.canGoForward()) {
            this.mainFrameNavigationGeneration.incrementAndGet();
            this.browser.goForward();
        }
        if (initialized) {
            this.setVolume(this.volume);
        }
    }

    /** Returns the current main-frame URL. */
    public String getUrl() {
        return this.browser.getURL();
    }

    /** Starts a main-frame navigation to the supplied URL. */
    public void setUrl(@NotNull String url) {
        this.expectedMainFrameUrl = url;
        this.mainFrameNavigationGeneration.incrementAndGet();
        this.browser.loadURL(url);
    }

    void onMainFrameLoadStartedForTracking(@Nullable String url) {
        this.expectedMainFrameUrl = url;
        this.mainFrameNavigationGeneration.incrementAndGet();
        // CEF owns browser audio independently of the page DOM, so reapply here while the native browser is guaranteed to exist.
        this.audioMuteController.reapply();
    }

    @Nullable
    String getExpectedMainFrameUrlForTracking() {
        return this.expectedMainFrameUrl;
    }

    /** Reloads the current main frame. */
    public void reload() {
        this.expectedMainFrameUrl = this.browser.getURL();
        this.mainFrameNavigationGeneration.incrementAndGet();
        this.browser.reload();
        if (initialized) {
            this.setVolume(this.volume);
        }
    }

    /** Sets render opacity. */
    public void setOpacity(float opacity) {
        this.opacity = opacity;
    }

    /** Returns the native browser owned by this wrapper. */
    @NotNull
    public RinkuBrowser getBrowser() {
        return this.browser;
    }

    /** Returns the stable identifier used by handler and listener registries. */
    public String getIdentifier() {
        return this.genericIdentifier.toString();
    }

    /** Returns whether this wrapper has released its native browser. */
    public boolean isClosed() {
        return this.closed;
    }

    /** Returns the native browser frame texture, when available. */
    @Nullable
    public Identifier getFrameLocation() {
        if (this.autoHandle) BrowserHandler.notifyHandler(this.genericIdentifier.toString(), this);
        return this.browser.getTextureIdentifier();
    }

    /** Always returns true so screens can route pointer focus to the browser. */
    @Override
    public boolean isFocusable() {
        // Pointer focus is required for Screen to route browser input, but isNavigatable() remains false to keep it out of tab/arrow navigation.
        return true;
    }

    /** Ignored because browser pointer focus must remain available. */
    @Override
    public void setFocusable(boolean focusable) {
    }

    /** Always returns false to exclude the browser from keyboard navigation traversal. */
    @Override
    public boolean isNavigatable() {
        return false;
    }

    /** Ignored because browsers are intentionally excluded from navigation traversal. */
    @Override
    public void setNavigatable(boolean navigatable) {
    }

    /** Releases listeners and the native browser exactly once on Minecraft's client/render thread. */
    @Override
    public void close() throws IOException {
        if (this.closed) {
            return;
        }
        this.setFocused(false);
        this.closed = true;
        this.inputState.reset();
        this.mainFrameNavigationGeneration.incrementAndGet();
        // Unregister from the global handler manager
        if (this.browser != null) {
            BrowserLoadEventListenerManager.getInstance().unregisterAllListenersForBrowser(this.getIdentifier());
            this.browser.close();
        }
    }

    /** Returns whether this browser owns a forwarded press for the supplied button. */
    @Override
    public boolean hasMouseButtonCapture(int button) {
        return this.inputState.hasMouseButtonCapture(button);
    }

}
