package de.keksuccino.konkrete.util.resource.resources.texture;

import com.mojang.blaze3d.platform.NativeImage;
import de.keksuccino.konkrete.util.CloseableUtils;
import de.keksuccino.konkrete.util.WebUtils;
import de.keksuccino.konkrete.util.input.TextValidators;
import de.keksuccino.konkrete.util.rendering.AspectRatio;
import de.keksuccino.konkrete.util.rendering.NativeImageUtil;
import de.keksuccino.konkrete.util.resource.ResourceRuntime;
import de.keksuccino.konkrete.util.threading.KonkreteThreads;
import de.keksuccino.konkrete.util.threading.MainThreadTaskExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Objects;
import java.util.Optional;

/** Decodes JPEG input into a PNG-backed dynamic texture and owns registered texture state. */
@SuppressWarnings("unused")
public class JpegTexture implements ITexture {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Holds the textureEntry handle whose lifecycle follows this texture resource instance. */
    protected final TextureManagerEntry<NativeImage, DynamicTexture> textureEntry = TextureManagerEntry.dynamicTexture();
    /** Decoded canvas width in pixels for this texture resource instance. */
    protected volatile int width = 10;
    /** Decoded canvas height in pixels for this texture resource instance. */
    protected volatile int height = 10;
    /** Aspect ratio derived from the decoded width and height. */
    protected volatile AspectRatio aspectRatio = new AspectRatio(10, 10);
    /** Whether decoded currently applies to this texture resource instance. */
    protected volatile boolean decoded = false;
    /** Original resource-pack identifier, or null when another source kind is used. */
    protected Identifier sourceLocation;
    /** Holds the sourceFile handle whose lifecycle follows this texture resource instance. */
    protected File sourceFile;
    /** Original web URL, or null when another source kind is used. */
    protected String sourceURL;
    /** Whether loading completed currently applies to this texture resource instance. */
    protected volatile boolean loadingCompleted = false;
    /** Whether loading failed currently applies to this texture resource instance. */
    protected volatile boolean loadingFailed = false;
    /** Whether closed currently applies to this texture resource instance. */
    protected volatile boolean closed = false;

    /**
     * Supports JPEG and PNG textures.
     */
    @NotNull
    public static JpegTexture location(@NotNull Identifier location) {
        return location(location, null);
    }

    /**
     * Supports JPEG and PNG textures.
     */
    @NotNull
    public static JpegTexture location(@NotNull Identifier location, @Nullable JpegTexture writeTo) {

        Objects.requireNonNull(location);
        JpegTexture texture = (writeTo != null) ? writeTo : new JpegTexture();

        texture.sourceLocation = location;

        try {
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(location);
            if (resource.isPresent()) {
                of(Objects.requireNonNull(resource.get().open()), location.toString(), texture);
            }
        } catch (Exception ex) {
            texture.loadingFailed = true;
            LOGGER.error("[KONKRETE] Failed to read texture from Identifier: " + location, ex);
        }
        return texture;

    }

    /**
     * Supports JPEG and PNG textures.
     */
    @NotNull
    public static JpegTexture local(@NotNull File textureFile) {
        return local(textureFile, null);
    }

    /**
     * Supports JPEG and PNG textures.
     */
    @NotNull
    public static JpegTexture local(@NotNull File textureFile, @Nullable JpegTexture writeTo) {

        Objects.requireNonNull(textureFile);
        JpegTexture texture = (writeTo != null) ? writeTo : new JpegTexture();

        texture.sourceFile = textureFile;

        if (!textureFile.isFile()) {
            texture.loadingFailed = true;
            LOGGER.error("[KONKRETE] Failed to read texture from file! File not found: " + textureFile.getPath());
            return texture;
        }

        try {
            InputStream in = new FileInputStream(textureFile);
            of(in, textureFile.getPath(), texture);
        } catch (Exception ex) {
            texture.loadingFailed = true;
            LOGGER.error("[KONKRETE] Failed to read texture from file: " + textureFile.getPath(), ex);
        }

        return texture;

    }

    /**
     * Supports JPEG and PNG textures.
     */
    @NotNull
    public static JpegTexture web(@NotNull String textureURL) {
        return web(textureURL, null);
    }

    /**
     * Supports JPEG and PNG textures.
     */
    @NotNull
    public static JpegTexture web(@NotNull String textureURL, @Nullable JpegTexture writeTo) {

        Objects.requireNonNull(textureURL);
        JpegTexture texture = (writeTo != null) ? writeTo : new JpegTexture();

        texture.sourceURL = textureURL;

        if (!TextValidators.BASIC_URL_TEXT_VALIDATOR.get(textureURL)) {
            texture.loadingFailed = true;
            LOGGER.error("[KONKRETE] Failed to read texture from URL! Invalid URL: " + textureURL);
            return texture;
        }

        KonkreteThreads.startDaemonThread(() -> {
            try {
                InputStream in = WebUtils.openResourceStream(textureURL, WebUtils.WebResourceType.IMAGE);
                if (in == null) throw new NullPointerException("Web resource input stream was NULL!");
                of(in, textureURL, texture);
            } catch (Exception ex) {
                texture.loadingFailed = true;
                LOGGER.error("[KONKRETE] Failed to read texture from URL: " + textureURL, ex);
            }
        }, "JpegTexture-WebLoader");

        return texture;

    }

    /**
     * Supports JPEG and PNG textures.<br>
     * Closes the passed {@link InputStream}!
     */
    @NotNull
    public static JpegTexture of(@NotNull InputStream in, @Nullable String textureName, @Nullable JpegTexture writeTo) {

        Objects.requireNonNull(in);
        JpegTexture texture = (writeTo != null) ? writeTo : new JpegTexture();

        KonkreteThreads.startDaemonThread(() -> {
            populateTexture(texture, in, (textureName != null) ? textureName : "[Generic InputStream Source]");
            if (texture.closed) MainThreadTaskExecutor.executeInMainThread(texture::close, MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK);
        }, "JpegTexture-Decoder");

        return texture;

    }

    /**
     * Supports JPEG and PNG textures.<br>
     * Closes the passed {@link InputStream}!
     */
    @NotNull
    public static JpegTexture of(@NotNull InputStream in) {
        return of(in, null, null);
    }

    /** Builds a resource value for the texture resource. */
    @NotNull
    public static JpegTexture of(@NotNull NativeImage nativeImage) {

        Objects.requireNonNull(nativeImage);

        JpegTexture texture = new JpegTexture();

        texture.textureEntry.adopt(nativeImage);
        texture.width = nativeImage.getWidth();
        texture.height = nativeImage.getHeight();
        texture.aspectRatio = new AspectRatio(nativeImage.getWidth(), nativeImage.getHeight());
        texture.decoded = true;
        texture.loadingCompleted = true;

        return texture;

    }

    /** Initializes a new {@code JpegTexture} for texture resource use. */
    protected JpegTexture() {
    }

    /** Populates the texture for the texture resource. */
    protected static void populateTexture(@NotNull JpegTexture texture, @NotNull InputStream in, @NotNull String textureName) {
        if (!texture.closed) {
            try {
                SizedNativeImage image = convertJpegToPng(in);
                if (image != null) {
                    texture.width = image.width;
                    texture.height = image.height;
                    texture.aspectRatio = new AspectRatio(texture.width, texture.height);
                    if (texture.textureEntry.adopt(image.image)) texture.loadingCompleted = true;
                } else {
                    texture.loadingFailed = true;
                    LOGGER.error("[KONKRETE] Failed to read texture, NativeImage was NULL: " + textureName);
                }
            } catch (Exception ex) {
                texture.loadingFailed = true;
                LOGGER.error("[KONKRETE] Failed to load texture: " + textureName, ex);
            }
        }
        texture.decoded = true;
        CloseableUtils.closeQuietly(in);
    }

    /**
     * Converts JPEG images to PNG, because Minecraft dropped support for JPEGs.
     */
    @Nullable
    protected static SizedNativeImage convertJpegToPng(@NotNull InputStream in) {
        int w = 1;
        int h = 1;
        NativeImage nativeImage = null;
        ByteArrayOutputStream byteArrayOut = null;
        try {
            BufferedImage bufferedImage = ImageIO.read(in);
            w = bufferedImage.getWidth();
            h = bufferedImage.getHeight();
            byteArrayOut = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", byteArrayOut);
            //ByteArrayInputStream is important, because using NativeImage#read(byte[]) causes OutOfMemoryExceptions
            nativeImage = NativeImage.read(new ByteArrayInputStream(byteArrayOut.toByteArray()));
        } catch (Exception ex) {
            LOGGER.error("[KONKRETE] Failed to convert JPEG image to PNG!", ex);
        }
        CloseableUtils.closeQuietly(in);
        CloseableUtils.closeQuietly(byteArrayOut);
        return (nativeImage != null) ? new SizedNativeImage(nativeImage, w, h) : null;
    }

    /** Returns the resource location, or {@code null} when it is not available. */
    @Nullable
    public Identifier getResourceLocation() {
        if (this.closed) return FULLY_TRANSPARENT_TEXTURE;
        if (this.textureEntry.canRegister()) {
            try {
                this.textureEntry.register(ResourceRuntime.identifier("dynamic/simple_jpeg_texture_" + ResourceRuntime.nextResourceId()));
            } catch (Exception ex) {
                LOGGER.error("[KONKRETE] Failed to get Identifier of JpegTexture!", ex);
            }
        }
        return this.textureEntry.getIdentifier();
    }

    /** Returns the width used by this texture resource instance. */
    public int getWidth() {
        return this.width;
    }

    /** Returns the height used by this texture resource instance. */
    public int getHeight() {
        return this.height;
    }

    /** Returns the aspect ratio used by this texture resource instance. */
    @NotNull
    public AspectRatio getAspectRatio() {
        return this.aspectRatio;
    }

    /** Opens the resource for the texture resource. */
    @Override
    public @Nullable InputStream open() throws IOException {
        NativeImage image = this.textureEntry.getImage();
        if (image != null) return new ByteArrayInputStream(NativeImageUtil.asByteArray(image));
        return null;
    }

    /** Returns whether ready. */
    @Override
    public boolean isReady() {
        //Everything important (like size) is set at this point, so it is considered ready
        return this.decoded;
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

    /** Restores initial texture resource state without transferring ownership. */
    @Override
    public void reset() {
    }

    /** Returns whether closed. */
    @Override
    public boolean isClosed() {
        return this.closed;
    }

    /**
     * Releases the owned dynamic entry through Minecraft's texture manager, which disposes both the DynamicTexture and
     * its NativeImage. JPEG resources loaded from an Identifier are decoded into owned dynamic entries too.
     */
    @Override
    public void close() {
        this.closed = true;
        this.textureEntry.close();
        this.decoded = false;
    }

    /** Carries {@code SizedNativeImage} data between validated stages of the texture resource. */
    protected record SizedNativeImage(@NotNull NativeImage image, int width, int height) {
    }

}
