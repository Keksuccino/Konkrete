package de.keksuccino.konkrete.util.window;

import com.mojang.blaze3d.platform.MacosUtil;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Reusable client-window operations backed by caller-supplied dynamic configuration. Mutating operations must run on Minecraft's client/window thread.
 */
public final class WindowHandler {

    private static final Logger LOGGER = LogManager.getLogger();

    private static volatile WindowConfiguration configuration = WindowConfiguration.disabled();

    private WindowHandler() {}

    /**
     * Replaces the dynamic configuration consulted by subsequent window operations.
     *
     * @param configuration new configuration
     */
    public static void configure(@NotNull WindowConfiguration configuration) {
        WindowHandler.configuration = Objects.requireNonNull(configuration, "configuration");
    }

    /** Restores the disabled default configuration. */
    public static void resetConfiguration() {
        configuration = WindowConfiguration.disabled();
    }

    /** Returns the active GLFW window handle. */
    public static long getWindowHandle() {
        return Minecraft.getInstance().getWindow().handle();
    }

    /** Returns the precise scale when the mixin bridge is active, otherwise vanilla's integer scale. */
    public static double getGuiScale() {
        Window window = Minecraft.getInstance().getWindow();
        return (Object)window instanceof PreciseGuiScaleWindow preciseWindow ? preciseWindow.getPreciseGuiScale_Konkrete() : window.getGuiScale();
    }

    /**
     * Applies a positive finite GUI scale and recalculates framebuffer-derived scaled dimensions.
     *
     * @param scale requested scale; invalid values fall back to one
     */
    public static void setGuiScale(double scale) {
        Window window = Minecraft.getInstance().getWindow();
        double safeScale = Double.isFinite(scale) && scale > 0.0D ? scale : 1.0D;
        int vanillaScale = Math.max(1, (int)Math.floor(safeScale));
        if (!((Object)window instanceof PreciseGuiScaleWindow) && safeScale != vanillaScale) {
            throw new IllegalStateException("Precise GUI scaling requires Konkrete's Window mixin bridge");
        }
        window.setGuiScale(vanillaScale);
        if (!((Object)window instanceof PreciseGuiScaleWindow preciseWindow)) {
            return;
        }
        preciseWindow.setPreciseGuiScale_Konkrete(safeScale);
        preciseWindow.setGuiScaledSize_Konkrete(ceilScaledDimension(window.getWidth(), safeScale), ceilScaledDimension(window.getHeight(), safeScale));
    }

    /** Switches to fullscreen when requested by the active configuration. */
    public static void handleForceFullscreen() {
        if (!configuration.forceFullscreen()) return;
        Window window = Minecraft.getInstance().getWindow();
        if (!window.isFullscreen()) {
            window.toggleFullScreen();
            LOGGER.info("[KONKRETE] Applied configured fullscreen window state");
        }
    }

    /** Returns whether custom icon application is currently enabled. */
    public static boolean isCustomWindowIconEnabled() {
        return configuration.customWindowIconEnabled();
    }

    /** Returns the configured 16x16 icon path. */
    @Nullable
    public static Path getCustomWindowIcon16() {
        return configuration.customWindowIcon16();
    }

    /** Returns the configured 32x32 icon path. */
    @Nullable
    public static Path getCustomWindowIcon32() {
        return configuration.customWindowIcon32();
    }

    /** Returns the configured macOS icon path. */
    @Nullable
    public static Path getCustomWindowIconMacOS() {
        return configuration.customWindowIconMacOS();
    }

    /** Returns whether every platform-specific configured icon exists as a regular file. */
    public static boolean allCustomWindowIconsSetAndFound() {
        Path icon16 = getCustomWindowIcon16();
        Path icon32 = getCustomWindowIcon32();
        Path iconMacOS = getCustomWindowIconMacOS();
        return icon16 != null && icon32 != null && iconMacOS != null && Files.isRegularFile(icon16) && Files.isRegularFile(icon32) && Files.isRegularFile(iconMacOS);
    }

    /** Applies the configured platform-specific icon when enabled. */
    public static void updateCustomWindowIcon() {
        if (!isCustomWindowIconEnabled()) return;
        if (isMacOS()) updateCustomWindowIconMacOS();
        else updateCustomWindowIconWindowsLinux();
    }

    /** Restores the icon from Minecraft's vanilla pack. */
    public static void resetWindowIcon() {
        try {
            if (isMacOS()) MacosUtil.loadIcon(getVanillaWindowIconFile("icons", "minecraft.icns"));
            else setIcon(getVanillaWindowIconFile("icons", "icon_16x16.png"), getVanillaWindowIconFile("icons", "icon_32x32.png"));
        } catch (Exception exception) {
            LOGGER.error("[KONKRETE] Failed to restore the vanilla window icon", exception);
        }
    }

    /** Applies the configured title, or asks vanilla to rebuild its normal title when none is configured. */
    public static void updateWindowTitle() {
        String customTitle = getCustomWindowTitle();
        if (customTitle == null) Minecraft.getInstance().updateTitle();
        else Minecraft.getInstance().getWindow().setTitle(customTitle);
    }

    /** Returns the configured non-blank title, or {@code null}. */
    @Nullable
    public static String getCustomWindowTitle() {
        String title = configuration.customWindowTitle();
        return title == null || title.isBlank() ? null : title;
    }

    /**
     * Decodes and applies two GLFW icons, freeing all native buffers even when application fails.
     *
     * @param icon16 16x16 icon stream supplier
     * @param icon32 32x32 icon stream supplier
     * @throws IOException when either stream cannot be read
     */
    protected static void setIcon(@NotNull IoSupplier<InputStream> icon16, @NotNull IoSupplier<InputStream> icon32) throws IOException {
        ByteBuffer firstPixels = null;
        ByteBuffer secondPixels = null;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);
            GLFWImage.Buffer icons = GLFWImage.malloc(2, stack);
            firstPixels = readIconPixels(icon16, width, height, channels);
            if (firstPixels == null) throw new IOException("Could not decode 16x16 icon: " + STBImage.stbi_failure_reason());
            icons.position(0).width(width.get(0)).height(height.get(0)).pixels(firstPixels);
            secondPixels = readIconPixels(icon32, width, height, channels);
            if (secondPixels == null) throw new IOException("Could not decode 32x32 icon: " + STBImage.stbi_failure_reason());
            icons.position(1).width(width.get(0)).height(height.get(0)).pixels(secondPixels);
            icons.position(0);
            GLFW.glfwSetWindowIcon(getWindowHandle(), icons);
        } finally {
            if (firstPixels != null) STBImage.stbi_image_free(firstPixels);
            if (secondPixels != null) STBImage.stbi_image_free(secondPixels);
        }
    }

    /**
     * Reads an encoded image stream into an STB-owned pixel buffer.
     *
     * @param supplier image stream supplier
     * @param width decoded width output
     * @param height decoded height output
     * @param channels decoded channel output
     * @return STB-owned pixel buffer, or {@code null} when decoding fails
     * @throws IOException when the encoded bytes cannot be read
     */
    @Nullable
    protected static ByteBuffer readIconPixels(@NotNull IoSupplier<InputStream> supplier, @NotNull IntBuffer width, @NotNull IntBuffer height, @NotNull IntBuffer channels) throws IOException {
        ByteBuffer encoded = null;
        try (InputStream input = supplier.get()) {
            encoded = TextureUtil.readResource(input);
            encoded.rewind();
            return STBImage.stbi_load_from_memory(encoded, width, height, channels, 0);
        } finally {
            if (encoded != null) MemoryUtil.memFree(encoded);
        }
    }

    private static int ceilScaledDimension(int framebufferDimension, double scale) {
        return Math.max(1, (int)Math.ceil(framebufferDimension / scale));
    }

    private static boolean isMacOS() {
        return Util.getPlatform() == Util.OS.OSX;
    }

    private static void updateCustomWindowIconMacOS() {
        Path icon = getCustomWindowIconMacOS();
        if (icon == null || !Files.isRegularFile(icon)) {
            LOGGER.error("[KONKRETE] Configured macOS window icon was not found");
            return;
        }
        try {
            MacosUtil.loadIcon(IoSupplier.create(icon));
            LOGGER.info("[KONKRETE] Applied configured macOS window icon");
        } catch (Exception exception) {
            LOGGER.error("[KONKRETE] Failed to apply configured macOS window icon", exception);
        }
    }

    private static void updateCustomWindowIconWindowsLinux() {
        Path icon16 = getCustomWindowIcon16();
        Path icon32 = getCustomWindowIcon32();
        if (icon16 == null || icon32 == null || !hasResolution(icon16, 16, 16) || !hasResolution(icon32, 32, 32)) return;
        try {
            setIcon(IoSupplier.create(icon16), IoSupplier.create(icon32));
            LOGGER.info("[KONKRETE] Applied configured window icons");
        } catch (Exception exception) {
            LOGGER.error("[KONKRETE] Failed to apply configured window icons", exception);
        }
    }

    private static boolean hasResolution(Path icon, int width, int height) {
        if (!Files.isRegularFile(icon)) {
            LOGGER.error("[KONKRETE] Configured {}x{} window icon was not found: {}", width, height, icon);
            return false;
        }
        try {
            BufferedImage image = ImageIO.read(icon.toFile());
            if (image == null || image.getWidth() != width || image.getHeight() != height) {
                LOGGER.error("[KONKRETE] Configured window icon must be exactly {}x{} pixels: {}", width, height, icon);
                return false;
            }
            return true;
        } catch (IOException exception) {
            LOGGER.error("[KONKRETE] Failed to inspect configured window icon: {}", icon, exception);
            return false;
        }
    }

    private static IoSupplier<InputStream> getVanillaWindowIconFile(String... path) throws IOException {
        IoSupplier<InputStream> resource = Minecraft.getInstance().getVanillaPackResources().getRootResource(path);
        if (resource == null) throw new FileNotFoundException(String.join("/", path));
        return resource;
    }

}
