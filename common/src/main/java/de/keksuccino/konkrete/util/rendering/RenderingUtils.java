package de.keksuccino.konkrete.util.rendering;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.BlendFactor;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import de.keksuccino.konkrete.mixin.mixins.common.client.AccessorMixinGuiGraphicsExtractor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;
import org.lwjgl.opengl.GL33C;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Bridges immediate-style GUI helpers into Minecraft's deferred render-state extraction. */
@SuppressWarnings("unused")
public class RenderingUtils {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String VULKAN_BACKEND_NAME = "Vulkan";

    /** Magenta half of the missing-texture checkerboard. */
    public static final DrawableColor MISSING_TEXTURE_COLOR_MAGENTA = DrawableColor.of(Color.MAGENTA);
    /** Black half of the missing-texture checkerboard. */
    public static final DrawableColor MISSING_TEXTURE_COLOR_BLACK = DrawableColor.BLACK;
    /** One-pixel transparent texture for APIs that require a texture binding. */
    public static final Identifier FULLY_TRANSPARENT_TEXTURE = Identifier.fromNamespaceAndPath("konkrete", "textures/fully_transparent.png");
    /** Converts Minecraft's menu-blur setting into Konkrete's framebuffer blur radius. */
    public static final float VANILLA_BACKGROUND_BLUR_TO_GUI_BLUR_MULTIPLIER = 1.5F;

    private static final List<RenderingTask> PRE_RENDER_CONTEXTS = new ArrayList<>();
    private static final List<RenderingTask> POST_RENDER_CONTEXTS = new ArrayList<>();
    private static final List<RenderingTask> DEFERRED_SCREEN_RENDERING_TASKS = new ArrayList<>();
    private static int depthTestLockDepth = 0;
    private static int blurBlockDepth = 0;
    private static int tooltipRenderingBlockDepth = 0;
    private static int overrideBackgroundBlurRadius = -1000;
    private static int shaderColor = -1;

    /**
     * Returns whether Minecraft's initialized render device uses Vulkan.
     *
     * @throws IllegalStateException if called before Minecraft initializes its render device
     */
    public static boolean isVulkanActive() {
        GpuDevice device = RenderSystem.getDevice();
        return VULKAN_BACKEND_NAME.equals(device.getDeviceInfo().backendName());
    }

    /**
     * Parses a six-digit RGB or eight-digit RGBA hexadecimal color.
     *
     * @param hex color with an optional leading {@code #}
     * @return the parsed color, or {@code null} if the value is malformed
     */
    @Nullable
    public static Color getColorFromHexString(@NotNull String hex) {
        try {
            hex = hex.replace("#", "");
            if (hex.length() == 6) {
                return new Color(Integer.valueOf(hex.substring(0, 2), 16), Integer.valueOf(hex.substring(2, 4), 16), Integer.valueOf(hex.substring(4, 6), 16));
            }
            if (hex.length() == 8) {
                return new Color(Integer.valueOf(hex.substring(0, 2), 16), Integer.valueOf(hex.substring(2, 4), 16), Integer.valueOf(hex.substring(4, 6), 16), Integer.valueOf(hex.substring(6, 8), 16));
            }
        } catch (Exception ex) {
            LOGGER.error("Failed to build Color object from HEX color string!", ex);
        }
        return null;
    }

    /**
     * Registers a texture under an explicit identifier or a path in the {@code konkrete} namespace.
     *
     * @return the identifier registered with Minecraft's texture manager
     */
    @NotNull
    public static Identifier register(@NotNull String location, @NotNull AbstractTexture texture) {
        Objects.requireNonNull(location);
        Objects.requireNonNull(texture);
        Identifier identifier = location.contains(":") ? Identifier.parse(location) : Identifier.fromNamespaceAndPath("konkrete", location);
        Minecraft.getInstance().getTextureManager().register(identifier, texture);
        return identifier;
    }

    /** Restores color masks, depth writes, and face culling after direct OpenGL work. */
    public static void restoreRawRenderStateDefaults() {
        RenderSystem.assertOnRenderThread();
        if (isVulkanActive()) {
            return;
        }
        for (int i = 0; i < ColorTargetState.MAX_COLOR_TARGETS; i++) {
            GL33C.glColorMaski(i, true, true, true, true);
        }
        GL33C.glDepthMask(true);
        GL33C.glEnable(GL33C.GL_CULL_FACE);
    }

    /** Queues a full-screen blur matching Minecraft's current menu-blur preference. */
    public static void extractVanillaLikeFullscreenBlur(@NotNull GuiGraphicsExtractor graphics, @NotNull Screen screen, float partial) {
        float blurRadius = convertVanillaBackgroundBlurrinessToGuiBlurRadius();
        if (blurRadius > 0.0F) {
            GuiBlurRenderer.renderBlurArea(graphics, 0.0F, 0.0F, screen.width, screen.height, blurRadius, 0.0F, DrawableColor.FULLY_TRANSPARENT, partial);
        }
    }

    /** Converts Minecraft's integer menu-blur setting to a GUI-space blur radius. */
    public static float convertVanillaBackgroundBlurrinessToGuiBlurRadius() {
        int backgroundBlurriness = Minecraft.getInstance().options.getMenuBackgroundBlurriness();
        if (backgroundBlurriness < 1) {
            return 0.0F;
        }
        float convertedBackgroundBlurriness = (float)backgroundBlurriness * VANILLA_BACKGROUND_BLUR_TO_GUI_BLUR_MULTIPLIER;
        return GuiBlurRenderer.convertFramebufferBlurRadiusToGui(convertedBackgroundBlurriness);
    }

    /** Sets the ARGB tint applied to subsequently extracted helper geometry. */
    public static void setShaderColor(@NotNull GuiGraphicsExtractor graphics, float red, float green, float blue, float alpha) {
        shaderColor = ARGB.colorFromFloat(alpha, red, green, blue);
    }

    /** Sets the packed ARGB tint applied to subsequently extracted helper geometry. */
    public static void setShaderColor(@NotNull GuiGraphicsExtractor graphics, int color) {
        shaderColor = color;
    }

    /** Returns the active ARGB helper tint, or {@code -1} when tinting is disabled. */
    public static int getShaderColor() {
        return shaderColor;
    }

    /** Multiplies a packed ARGB color by the active helper tint. */
    public static int applyShaderColor(int color) {
        if (shaderColor == -1) {
            return color;
        }
        if (color == -1) {
            return shaderColor;
        }
        return ARGB.multiply(color, shaderColor);
    }

    /** Uses an AWT color as the tint for subsequently extracted helper geometry. */
    public static void setShaderColor(@NotNull GuiGraphicsExtractor graphics, @NotNull Color color) {
        shaderColor = color.getRGB();
    }

    /** Uses a drawable color as the tint for subsequently extracted helper geometry. */
    public static void setShaderColor(@NotNull GuiGraphicsExtractor graphics, @NotNull DrawableColor color) {
        shaderColor = color.getColorInt();
    }

    /** Uses an AWT color with a replacement alpha as the active helper tint. */
    public static void setShaderColor(@NotNull GuiGraphicsExtractor graphics, @NotNull Color color, float alpha) {
        shaderColor = replaceAlphaInColor(color.getRGB(), alpha);
    }

    /** Uses a drawable color with a replacement alpha as the active helper tint. */
    public static void setShaderColor(@NotNull GuiGraphicsExtractor graphics, @NotNull DrawableColor color, float alpha) {
        shaderColor = color.getColorIntWithAlpha(alpha);
    }

    /** Disables helper tinting for subsequently extracted geometry. */
    public static void resetShaderColor(@NotNull GuiGraphicsExtractor graphics) {
        shaderColor = -1;
    }

    /** Queues a callback immediately before deferred screen rendering. */
    public static void postPreRenderTask(@NotNull RenderingTask context) {
        PRE_RENDER_CONTEXTS.add(Objects.requireNonNull(context));
    }

    /** Queues a callback immediately after deferred screen rendering. */
    public static void postPostRenderTask(@NotNull RenderingTask context) {
        POST_RENDER_CONTEXTS.add(Objects.requireNonNull(context));
    }

    /** Drains the callbacks queued for the start of screen extraction. */
    @ApiStatus.Internal
    public static void executeAllPreRenderTasks(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        List<RenderingTask> copy = new ArrayList<>(PRE_RENDER_CONTEXTS);
        PRE_RENDER_CONTEXTS.clear();
        for (RenderingTask context : copy) {
            try {
                context.render(graphics, mouseX, mouseY, partial);
            } catch (Exception ex) {
                LOGGER.error("[KONKRETE] Failed to execute pre-screen-render task!", ex);
            }
        }
    }

    /** Drains the callbacks queued for the end of screen extraction. */
    @ApiStatus.Internal
    public static void executeAllPostRenderTasks(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        List<RenderingTask> copy = new ArrayList<>(POST_RENDER_CONTEXTS);
        POST_RENDER_CONTEXTS.clear();
        for (RenderingTask context : copy) {
            try {
                context.render(graphics, mouseX, mouseY, partial);
            } catch (Exception ex) {
                LOGGER.error("[KONKRETE] Failed to execute post-screen-render task!", ex);
            }
        }
    }

    /** Queues a magenta-and-black missing-texture checkerboard. */
    public static void renderMissing(@NotNull GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        int partW = width / 2;
        int partH = height / 2;
        //Top-left
        graphics.fill(x, y, x + partW, y + partH, MISSING_TEXTURE_COLOR_MAGENTA.getColorInt());
        //Top-right
        graphics.fill(x + partW, y, x + width, y + partH, MISSING_TEXTURE_COLOR_BLACK.getColorInt());
        //Bottom-left
        graphics.fill(x, y + partH, x + partW, y + height, MISSING_TEXTURE_COLOR_BLACK.getColorInt());
        //Bottom-right
        graphics.fill(x + partW, y + partH, x + width, y + height, MISSING_TEXTURE_COLOR_MAGENTA.getColorInt());
    }

    /** Selects a temporary integer radius in place of Minecraft's menu blur. */
    public static void setOverrideBackgroundBlurRadius(int radius) {
        overrideBackgroundBlurRadius = radius;
    }

    /** Restores menu blur to Minecraft's configured radius. */
    public static void resetOverrideBackgroundBlurRadius() {
        overrideBackgroundBlurRadius = -1000;
    }

    /** Reports whether a temporary menu-blur radius is active. */
    public static boolean shouldOverrideBackgroundBlurRadius() {
        return overrideBackgroundBlurRadius != -1000;
    }

    /** Returns the temporary menu-blur radius, or the internal disabled sentinel. */
    public static int getOverrideBackgroundBlurRadius() {
        return overrideBackgroundBlurRadius;
    }

    /** Enters or leaves a nestable lock that prevents GUI code from changing depth testing. */
    public static void setDepthTestLocked(boolean locked) {
        if (locked) {
            depthTestLockDepth++;
            return;
        }
        if (depthTestLockDepth > 0) {
            depthTestLockDepth--;
        }
    }

    /** Reports whether at least one caller holds the depth-test lock. */
    public static boolean isDepthTestLocked() {
        return depthTestLockDepth > 0;
    }

    /** Enters or leaves the nestable vanilla menu-blur suppression scope. */
    public static void setMenuBlurringBlocked(boolean blocked) {
        setVanillaMenuBlurringBlocked(blocked);
    }

    /** Reports whether vanilla menu blur is currently suppressed. */
    public static boolean isMenuBlurringBlocked() {
        return isVanillaMenuBlurringBlocked();
    }

    /** Enters or leaves the nestable vanilla menu-blur suppression scope. */
    public static void setVanillaMenuBlurringBlocked(boolean blocked) {
        if (blocked) {
            blurBlockDepth++;
            return;
        }
        if (blurBlockDepth > 0) {
            blurBlockDepth--;
        }
    }

    /** Reports whether at least one caller suppresses vanilla menu blur. */
    public static boolean isVanillaMenuBlurringBlocked() {
        return blurBlockDepth > 0;
    }

    /** Enters or leaves a nestable scope that suppresses tooltip extraction. */
    public static void setTooltipRenderingBlocked(boolean blocked) {
        if (blocked) {
            tooltipRenderingBlockDepth++;
            return;
        }
        if (tooltipRenderingBlockDepth > 0) {
            tooltipRenderingBlockDepth--;
        }
    }

    /** Reports whether at least one caller suppresses tooltip extraction. */
    public static boolean isTooltipRenderingBlocked() {
        return tooltipRenderingBlockDepth > 0;
    }

    /** Queues screen draw-state extraction until the current screen has finished. */
    public static void addDeferredScreenRenderingTask(@NotNull RenderingTask task) {
        DEFERRED_SCREEN_RENDERING_TASKS.add(task);
    }

    /** Snapshots the screen tasks waiting for deferred extraction. */
    @NotNull
    public static List<RenderingTask> getDeferredScreenRenderingTasks() {
        return new ArrayList<>(DEFERRED_SCREEN_RENDERING_TASKS);
    }

    /** Drains all deferred screen tasks into the supplied extraction context. */
    public static void executeAndClearDeferredScreenRenderingTasks(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        List<RenderingTask> tasks = getDeferredScreenRenderingTasks();
        DEFERRED_SCREEN_RENDERING_TASKS.clear();
        tasks.forEach(task -> task.render(graphics, mouseX, mouseY, partial));
    }

    /**
     * Draws a textured quad, mirrored horizontally.
     *
     * @param graphics      The GuiGraphicsExtractor context, which manages transformations and rendering.
     * @param atlasLocation The texture resource location.
     * @param x             The x coordinate on screen.
     * @param y             The y coordinate on screen.
     * @param u             The u coordinate in the texture (top-left of the sprite).
     * @param v             The v coordinate in the texture (top-left of the sprite).
     * @param spriteWidth   The width of the sprite quad on screen and in the texture.
     * @param spriteHeight  The height of the sprite quad on screen and in the texture.
     * @param textureWidth  The total width of the texture atlas.
     * @param textureHeight The total height of the texture atlas.
     */
    public static void blitMirrored(@NotNull GuiGraphicsExtractor graphics, Identifier atlasLocation, int x, int y, int u, int v, int spriteWidth, int spriteHeight, int textureWidth, int textureHeight) {
        // Delegate to the scaled version with a default white tint (-1)
        blitMirroredScaled(graphics, atlasLocation, x, y, u, v, spriteWidth, spriteHeight, spriteWidth, spriteHeight, textureWidth, textureHeight, -1);
    }

    /**
     * Draws a textured quad with a color tint, mirrored horizontally.
     *
     * @param graphics      The GuiGraphicsExtractor context, which manages transformations and rendering.
     * @param atlasLocation The texture resource location.
     * @param x             The x coordinate on screen.
     * @param y             The y coordinate on screen.
     * @param u             The u coordinate in the texture (top-left of the sprite).
     * @param v             The v coordinate in the texture (top-left of the sprite).
     * @param spriteWidth   The width of the sprite quad on screen and in the texture.
     * @param spriteHeight  The height of the sprite quad on screen and in the texture.
     * @param textureWidth  The total width of the texture atlas.
     * @param textureHeight The total height of the texture atlas.
     * @param colorTint     The color tint to apply (ARGB format).
     */
    public static void blitMirrored(@NotNull GuiGraphicsExtractor graphics, Identifier atlasLocation, int x, int y, int u, int v, int spriteWidth, int spriteHeight, int textureWidth, int textureHeight, int colorTint) {
        blitMirroredScaled(graphics, atlasLocation, x, y, u, v, spriteWidth, spriteHeight, spriteWidth, spriteHeight, textureWidth, textureHeight, colorTint);
    }

    /**
     * Draws a textured quad scaled to a specific render size, mirrored horizontally.
     * <p>
     * Minecraft 1.21.11 defers GUI quads through render states. Mirroring with a negative X scale flips
     * the quad winding and can make the renderer cull it, so keep the geometry normal and flip the UVs.
     *
     * @param graphics      The GuiGraphicsExtractor context.
     * @param atlasLocation The texture resource location.
     * @param x             The x coordinate on screen (top-left of the rendered quad).
     * @param y             The y coordinate on screen (top-left of the rendered quad).
     * @param u             The u coordinate in the texture (top-left of the source sprite region).
     * @param v             The v coordinate in the texture (top-left of the source sprite region).
     * @param spriteWidth   The width of the source sprite region in the texture atlas.
     * @param spriteHeight  The height of the source sprite region in the texture atlas.
     * @param renderWidth   The desired width of the quad to render on screen.
     * @param renderHeight  The desired height of the quad to render on screen.
     * @param textureWidth  The total width of the texture atlas.
     * @param textureHeight The total height of the texture atlas.
     * @param color         The color tint to apply (ARGB format, -1 for white/no tint).
     */
    public static void blitMirroredScaled(@NotNull GuiGraphicsExtractor graphics, Identifier atlasLocation, int x, int y, int u, int v, int spriteWidth, int spriteHeight, int renderWidth, int renderHeight, int textureWidth, int textureHeight, int color) {
        // Starting the source region at its right edge and using a negative source width flips U coordinates.
        graphics.blit(RenderPipelines.GUI_TEXTURED,
                atlasLocation,
                x, y,
                (float)(u + spriteWidth), (float)v,
                renderWidth, renderHeight,
                -spriteWidth, spriteHeight,
                textureWidth, textureHeight,
                color
        );

    }

    /**
     * Repeatedly renders a tileable (seamless) texture inside an area. Fills the area with the texture.
     *
     * @param graphics The {@link GuiGraphicsExtractor} instance.
     * @param location The {@link Identifier} of the texture.
     * @param x The X position the area should get rendered at.
     * @param y The Y position the area should get rendered at.
     * @param areaRenderWidth The width of the area.
     * @param areaRenderHeight The height of the area.
     * @param texWidth The full width (in pixels) of the texture.
     * @param texHeight The full height (in pixels) of the texture.
     */
    public static void blitRepeat(@NotNull GuiGraphicsExtractor graphics, @NotNull Identifier location, int x, int y, int areaRenderWidth, int areaRenderHeight, int texWidth, int texHeight, int color) {
        blitRepeat(graphics, RenderPipelines.GUI_TEXTURED, location, x, y, areaRenderWidth, areaRenderHeight, texWidth, texHeight, color);
    }

    /** Tiles a texture across the requested GUI rectangle using the active tint. */
    public static void blitRepeat(@NotNull GuiGraphicsExtractor graphics, @NotNull Identifier location, int x, int y, int areaRenderWidth, int areaRenderHeight, int texWidth, int texHeight) {
        blitRepeat(graphics, location, x, y, areaRenderWidth, areaRenderHeight, texWidth, texHeight, shaderColor);
    }

    /**
     * Repeatedly renders a tileable (seamless) texture inside an area. Fills the area with the texture.
     *
     * @param graphics The {@link GuiGraphicsExtractor} instance.
     * @param renderType The render type.
     * @param location The {@link Identifier} of the texture.
     * @param x The X position the area should get rendered at.
     * @param y The Y position the area should get rendered at.
     * @param areaRenderWidth The width of the area.
     * @param areaRenderHeight The height of the area.
     * @param texWidth The full width (in pixels) of the texture.
     * @param texHeight The full height (in pixels) of the texture.
     */
    public static void blitRepeat(@NotNull GuiGraphicsExtractor graphics, @NotNull RenderPipeline renderType, @NotNull Identifier location, int x, int y, int areaRenderWidth, int areaRenderHeight, int texWidth, int texHeight, int color) {
        graphics.blit(renderType, location, x, y, 0.0F, 0.0F, areaRenderWidth, areaRenderHeight, texWidth, texHeight, color);
    }

    /** Tiles a texture across the requested GUI rectangle and render pipeline using the active tint. */
    public static void blitRepeat(@NotNull GuiGraphicsExtractor graphics, @NotNull RenderPipeline renderType, @NotNull Identifier location, int x, int y, int areaRenderWidth, int areaRenderHeight, int texWidth, int texHeight) {
        blitRepeat(graphics, renderType, location, x, y, areaRenderWidth, areaRenderHeight, texWidth, texHeight, shaderColor);
    }

    /**
     * Renders a texture using nine-slice scaling with tiled edges and center.
     *
     * @param graphics The GuiGraphicsExtractor instance to use for rendering
     * @param texture The texture Identifier to render
     * @param x The x position to render at
     * @param y The y position to render at
     * @param width The desired width to render
     * @param height The desired height to render
     * @param textureWidth The actual width of the texture
     * @param textureHeight The actual height of the texture
     * @param borderTop The size of the top border
     * @param borderRight The size of the right border
     * @param borderBottom The size of the bottom border
     * @param borderLeft The size of the left border
     * @param color The color to tint the texture with
     */
    public static void blitNineSlicedTexture(GuiGraphicsExtractor graphics, Identifier texture, int x, int y, int width, int height,
                                             int textureWidth, int textureHeight,
                                             int borderTop, int borderRight, int borderBottom, int borderLeft, int color) {

        blitNineSlicedTexture(graphics, RenderPipelines.GUI_TEXTURED, texture, x, y, width, height, textureWidth, textureHeight, borderTop, borderRight, borderBottom, borderLeft, color);

    }

    /** Queues a nine-slice texture using the active helper tint. */
    public static void blitNineSlicedTexture(GuiGraphicsExtractor graphics, Identifier texture, int x, int y, int width, int height,
                                             int textureWidth, int textureHeight,
                                             int borderTop, int borderRight, int borderBottom, int borderLeft) {

        blitNineSlicedTexture(graphics, texture, x, y, width, height, textureWidth, textureHeight, borderTop, borderRight, borderBottom, borderLeft, shaderColor);

    }

    /**
     * Renders a texture using nine-slice scaling with tiled edges and center.
     *
     * @param graphics The GuiGraphicsExtractor instance to use for rendering
     * @param renderType The render type.
     * @param texture The texture Identifier to render
     * @param x The x position to render at
     * @param y The y position to render at
     * @param width The desired width to render
     * @param height The desired height to render
     * @param textureWidth The actual width of the texture
     * @param textureHeight The actual height of the texture
     * @param borderTop The size of the top border
     * @param borderRight The size of the right border
     * @param borderBottom The size of the bottom border
     * @param borderLeft The size of the left border
     * @param color The color to tint the texture with
     */
    public static void blitNineSlicedTexture(GuiGraphicsExtractor graphics, @NotNull RenderPipeline renderType, Identifier texture, int x, int y, int width, int height,
                                             int textureWidth, int textureHeight,
                                             int borderTop, int borderRight, int borderBottom, int borderLeft, int color) {

        // Correct border sizes if they're too large
        if (borderLeft + borderRight >= textureWidth) {
            float scale = (float)(textureWidth - 2) / (borderLeft + borderRight);
            borderLeft = (int)(borderLeft * scale);
            borderRight = (int)(borderRight * scale);
        }
        if (borderTop + borderBottom >= textureHeight) {
            float scale = (float)(textureHeight - 2) / (borderTop + borderBottom);
            borderTop = (int)(borderTop * scale);
            borderBottom = (int)(borderBottom * scale);
        }

        // Corner pieces
        // Top left
        graphics.blit(renderType, texture, x, y, 0, 0, borderLeft, borderTop, textureWidth, textureHeight, color);
        // Top right
        graphics.blit(renderType, texture, x + width - borderRight, y, textureWidth - borderRight, 0, borderRight, borderTop, textureWidth, textureHeight, color);
        // Bottom left
        graphics.blit(renderType, texture, x, y + height - borderBottom, 0, textureHeight - borderBottom, borderLeft, borderBottom, textureWidth, textureHeight, color);
        // Bottom right
        graphics.blit(renderType, texture, x + width - borderRight, y + height - borderBottom, textureWidth - borderRight, textureHeight - borderBottom, borderRight, borderBottom, textureWidth, textureHeight, color);

        // Edges - Tiled
        int centerWidth = textureWidth - borderLeft - borderRight;
        int centerHeight = textureHeight - borderTop - borderBottom;

        // Top edge
        for (int i = borderLeft; i < width - borderRight; i += centerWidth) {
            int pieceWidth = Math.min(centerWidth, width - borderRight - i);
            graphics.blit(renderType, texture, x + i, y, borderLeft, 0, pieceWidth, borderTop, textureWidth, textureHeight, color);
        }

        // Bottom edge
        for (int i = borderLeft; i < width - borderRight; i += centerWidth) {
            int pieceWidth = Math.min(centerWidth, width - borderRight - i);
            graphics.blit(renderType, texture, x + i, y + height - borderBottom, borderLeft, textureHeight - borderBottom, pieceWidth, borderBottom, textureWidth, textureHeight, color);
        }

        // Left edge
        for (int j = borderTop; j < height - borderBottom; j += centerHeight) {
            int pieceHeight = Math.min(centerHeight, height - borderBottom - j);
            graphics.blit(renderType, texture, x, y + j, 0, borderTop, borderLeft, pieceHeight, textureWidth, textureHeight, color);
        }

        // Right edge
        for (int j = borderTop; j < height - borderBottom; j += centerHeight) {
            int pieceHeight = Math.min(centerHeight, height - borderBottom - j);
            graphics.blit(renderType, texture, x + width - borderRight, y + j, textureWidth - borderRight, borderTop, borderRight, pieceHeight, textureWidth, textureHeight, color);
        }

        // Center - Tiled
        for (int i = borderLeft; i < width - borderRight; i += centerWidth) {
            int pieceWidth = Math.min(centerWidth, width - borderRight - i);
            for (int j = borderTop; j < height - borderBottom; j += centerHeight) {
                int pieceHeight = Math.min(centerHeight, height - borderBottom - j);
                graphics.blit(renderType, texture, x + i, y + j, borderLeft, borderTop, pieceWidth, pieceHeight, textureWidth, textureHeight, color);
            }
        }

    }

    /** Returns Minecraft's current game-time interpolation fraction. */
    public static float getPartialTick() {
        return Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
    }

    /** Tests an integer point against a left/top-inclusive, right/bottom-exclusive rectangle. */
    public static boolean isXYInArea(int targetX, int targetY, int x, int y, int width, int height) {
        return isXYInArea((double)targetX, targetY, x, y, width, height);
    }

    /** Tests a point against a left/top-inclusive, right/bottom-exclusive rectangle. */
    public static boolean isXYInArea(double targetX, double targetY, double x, double y, double width, double height) {
        return (targetX >= x) && (targetX < (x + width)) && (targetY >= y) && (targetY < (y + height));
    }

    /** Recalculates and reapplies Minecraft's configured GUI scale. */
    public static void resetGuiScale() {
        Window m = Minecraft.getInstance().getWindow();
        m.setGuiScale(m.calculateScale(Minecraft.getInstance().options.guiScale().get(), Minecraft.getInstance().options.forceUnicodeFont().get()));
    }

    /**
     * Replaces the alpha channel of a packed ARGB color.
     *
     * @param color packed ARGB color
     * @param newAlpha alpha channel, clamped at 255
     * @return {@code color} with its alpha channel replaced
     */
    public static int replaceAlphaInColor(int color, int newAlpha) {
        newAlpha = Math.min(newAlpha, 255);
        return color & 16777215 | newAlpha << 24;
    }

    /**
     * Replaces the alpha channel of a packed ARGB color from a normalized value.
     *
     * @param color packed ARGB color
     * @param newAlpha normalized alpha
     * @return {@code color} with its alpha channel replaced
     */
    public static int replaceAlphaInColor(int color, float newAlpha) {
        return replaceAlphaInColor(color, (int)(newAlpha * 255.0F));
    }

    /** Queues a solid rectangle with floating-point GUI coordinates. */
    public static void fillF(@NotNull GuiGraphicsExtractor graphics, float minX, float minY, float maxX, float maxY, int color) {
        submitColoredRectangle(graphics, RenderPipelines.GUI, TextureSetup.noTexture(), minX, minY, maxX, maxY, color, null);
    }

    /** Queues a tinted texture quad with floating-point destination coordinates. */
    public static void blitF(@NotNull GuiGraphicsExtractor graphics, Identifier location, float x, float y, float f3, float f4, float width, float height, float width2, float height2, int color) {
        blitF(graphics, RenderPipelines.GUI_TEXTURED, location, x, y, f3, f4, width, height, width2, height2, color);
    }

    /** Queues a texture quad with floating-point coordinates and the active tint. */
    public static void blitF(@NotNull GuiGraphicsExtractor graphics, Identifier location, float x, float y, float f3, float f4, float width, float height, float width2, float height2) {
        blitF(graphics, RenderPipelines.GUI_TEXTURED, location, x, y, f3, f4, width, height, width2, height2, shaderColor);
    }

    private static void submitColoredRectangle(@NotNull GuiGraphicsExtractor graphics, RenderPipeline pipeline, TextureSetup textureSetup, float minX, float minY, float maxX, float maxY, int color, @Nullable Integer endColor) {
        ((AccessorMixinGuiGraphicsExtractor)graphics).get_guiRenderState_Konkrete().addGuiElement(
                new FloatColoredRectangleRenderState(pipeline, textureSetup, new Matrix3x2f(graphics.pose()), minX, minY, maxX, maxY, color, endColor != null ? endColor : color, GuiScissorUtil.getActiveScissor(graphics))
        );
    }

    /** Queues a tinted texture quad whose source size matches its destination size. */
    public static void blitF(@NotNull GuiGraphicsExtractor graphics, RenderPipeline renderTypeFunc, Identifier location, float $$2, float $$3, float $$4, float $$5, float $$6, float $$7, float $$8, float $$9, int color) {
        blitF(graphics, renderTypeFunc, location, $$2, $$3, $$4, $$5, $$6, $$7, $$6, $$7, $$8, $$9, color);
    }

    /** Queues a texture quad whose source size matches its destination size using the active tint. */
    public static void blitF(@NotNull GuiGraphicsExtractor graphics, RenderPipeline renderTypeFunc, Identifier location, float $$2, float $$3, float $$4, float $$5, float $$6, float $$7, float $$8, float $$9) {
        blitF(graphics, renderTypeFunc, location, $$2, $$3, $$4, $$5, $$6, $$7, $$6, $$7, $$8, $$9, shaderColor);
    }

    /** Queues a scaled texture quad with floating-point coordinates and the active tint. */
    public static void blitF(@NotNull GuiGraphicsExtractor graphics, RenderPipeline renderTypeFunc, Identifier location, float $$2, float $$3, float $$4, float $$5, float $$6, float $$7, float $$8, float $$9, float $$10, float $$11) {
        blitF(graphics, renderTypeFunc, location, $$2, $$3, $$4, $$5, $$6, $$7, $$8, $$9, $$10, $$11, shaderColor);
    }

    /** Queues a scaled, tinted texture quad with floating-point destination and UV coordinates. */
    public static void blitF(@NotNull GuiGraphicsExtractor graphics, RenderPipeline renderTypeFunc, Identifier location, float $$2, float $$3, float $$4, float $$5, float $$6, float $$7, float $$8, float $$9, float $$10, float $$11, int color) {
        innerBlit(
                graphics,
                renderTypeFunc,
                location,
                $$2,
                $$2 + $$6,
                $$3,
                $$3 + $$7,
                ($$4 + 0.0F) / (float)$$10,
                ($$4 + (float)$$8) / (float)$$10,
                ($$5 + 0.0F) / (float)$$11,
                ($$5 + (float)$$9) / (float)$$11,
                color
        );
    }

    private static void innerBlit(@NotNull GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier texture, float minX, float maxX, float minY, float maxY, float minU, float maxU, float minV, float maxV, int color) {
        AbstractTexture absTex = Minecraft.getInstance().getTextureManager().getTexture(texture);
        submitBlit(graphics, pipeline, absTex.getTextureView(), absTex.getSampler(), minX, minY, maxX, maxY, minU, maxU, minV, maxV, color);
    }

    static void submitBlit(@NotNull GuiGraphicsExtractor graphics, RenderPipeline pipeline, GpuTextureView textureView, GpuSampler gpuSampler, float minX, float minY, float maxX, float maxY, float minU, float maxU, float minV, float maxV, int color) {
        ((AccessorMixinGuiGraphicsExtractor)graphics).get_guiRenderState_Konkrete().addGuiElement(
                new FloatBlitRenderState(
                        pipeline, TextureSetup.singleTexture(textureView, gpuSampler), new Matrix3x2f(graphics.pose()), minX, minY, maxX, maxY, minU, maxU, minV, maxV, color, GuiScissorUtil.getActiveScissor(graphics)
                )
        );
    }

    /** Configures independent source and destination blend factors for color and alpha. */
    public static void blendFuncSeparate(SourceFactor sourceFactor, DestFactor destFactor, SourceFactor sourceFactor2, DestFactor destFactor2) {
        RenderSystem.assertOnRenderThread();
        Objects.requireNonNull(sourceFactor);
        Objects.requireNonNull(destFactor);
        Objects.requireNonNull(sourceFactor2);
        Objects.requireNonNull(destFactor2);
        if (isVulkanActive()) {
            return;
        }
        GL33C.glBlendFuncSeparate(sourceFactor.value, destFactor.value, sourceFactor2.value, destFactor2.value);
    }

    /** Restores conventional source-alpha color blending and overwrite alpha blending. */
    public static void defaultBlendFunc() {
        blendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
    }

    /** Builds a blend function with independent color and alpha factors. */
    @NotNull
    public static BlendFunction createBlendFunction(SourceFactor sourceFactor, DestFactor destFactor, SourceFactor sourceFactor2, DestFactor destFactor2) {
        return new BlendFunction(
                Objects.requireNonNull(sourceFactor).toBlendFactor(),
                Objects.requireNonNull(destFactor).toBlendFactor(),
                Objects.requireNonNull(sourceFactor2).toBlendFactor(),
                Objects.requireNonNull(destFactor2).toBlendFactor()
        );
    }

    /** Enumerates source blend factors accepted by the GUI pipeline. */
    public enum SourceFactor {

        /** Uses constant alpha as the blend factor. */
        CONSTANT_ALPHA(32771),
        /** Uses constant color as the blend factor. */
        CONSTANT_COLOR(32769),
        /** Uses destination alpha as the blend factor. */
        DST_ALPHA(772),
        /** Uses destination color as the blend factor. */
        DST_COLOR(774),
        /** Uses one as the blend factor. */
        ONE(1),
        /** Uses one minus constant alpha as the blend factor. */
        ONE_MINUS_CONSTANT_ALPHA(32772),
        /** Uses one minus constant color as the blend factor. */
        ONE_MINUS_CONSTANT_COLOR(32770),
        /** Uses one minus destination alpha as the blend factor. */
        ONE_MINUS_DST_ALPHA(773),
        /** Uses one minus destination color as the blend factor. */
        ONE_MINUS_DST_COLOR(775),
        /** Uses one minus source alpha as the blend factor. */
        ONE_MINUS_SRC_ALPHA(771),
        /** Uses one minus source color as the blend factor. */
        ONE_MINUS_SRC_COLOR(769),
        /** Uses source alpha as the blend factor. */
        SRC_ALPHA(770),
        /** Uses source alpha saturate as the blend factor. */
        SRC_ALPHA_SATURATE(776),
        /** Uses source color as the blend factor. */
        SRC_COLOR(768),
        /** Uses zero as the blend factor. */
        ZERO(0);

        /** OpenGL numeric constant retained for compatibility with legacy callers. */
        public final int value;

        SourceFactor(final int value) {
            this.value = value;
        }

        /** Resolves this legacy OpenGL factor to Minecraft's pipeline factor. */
        @NotNull
        public BlendFactor toBlendFactor() {
            return BlendFactor.valueOf(this.name());
        }

    }

    /** Enumerates destination blend factors accepted by the GUI pipeline. */
    public enum DestFactor {

        /** Uses constant alpha as the blend factor. */
        CONSTANT_ALPHA(32771),
        /** Uses constant color as the blend factor. */
        CONSTANT_COLOR(32769),
        /** Uses destination alpha as the blend factor. */
        DST_ALPHA(772),
        /** Uses destination color as the blend factor. */
        DST_COLOR(774),
        /** Uses one as the blend factor. */
        ONE(1),
        /** Uses one minus constant alpha as the blend factor. */
        ONE_MINUS_CONSTANT_ALPHA(32772),
        /** Uses one minus constant color as the blend factor. */
        ONE_MINUS_CONSTANT_COLOR(32770),
        /** Uses one minus destination alpha as the blend factor. */
        ONE_MINUS_DST_ALPHA(773),
        /** Uses one minus destination color as the blend factor. */
        ONE_MINUS_DST_COLOR(775),
        /** Uses one minus source alpha as the blend factor. */
        ONE_MINUS_SRC_ALPHA(771),
        /** Uses one minus source color as the blend factor. */
        ONE_MINUS_SRC_COLOR(769),
        /** Uses source alpha as the blend factor. */
        SRC_ALPHA(770),
        /** Uses source color as the blend factor. */
        SRC_COLOR(768),
        /** Uses zero as the blend factor. */
        ZERO(0);

        /** OpenGL numeric constant retained for compatibility with legacy callers. */
        public final int value;

        DestFactor(final int value) {
            this.value = value;
        }

        /** Resolves this legacy OpenGL factor to Minecraft's pipeline factor. */
        @NotNull
        public BlendFactor toBlendFactor() {
            return BlendFactor.valueOf(this.name());
        }

    }

    /** Draws custom screen content through the deferred GUI extractor. */
    @FunctionalInterface
    public interface RenderingTask {

        /** Queues the task's draw state for the current frame. */
        void render(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial);

    }

    /** Marks a rendering task whose execution is deferred until screen extraction completes. */
    @FunctionalInterface
    public interface DeferredScreenRenderingTask extends RenderingTask {

    }

}
