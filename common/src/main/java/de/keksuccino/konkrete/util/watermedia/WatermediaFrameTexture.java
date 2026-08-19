package de.keksuccino.konkrete.util.watermedia;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.opengl.FrameBufferCache;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.vulkan.VulkanDevice;
import com.mojang.blaze3d.vulkan.VulkanGpuTexture;
import de.keksuccino.konkrete.util.rendering.RenderingUtils;
import de.keksuccino.konkrete.util.watermedia.vulkan.WatermediaVulkanInterop;
import de.keksuccino.konkrete.util.watermedia.vulkan.WatermediaVulkanTextureView;
import net.minecraft.client.renderer.texture.AbstractTexture;
import org.jetbrains.annotations.Nullable;

/**
 * Render-thread-only Minecraft facade over a Watermedia-owned OpenGL texture or Vulkan image view.
 * Konkrete owns only the wrapper and Vulkan placeholder; Watermedia retains every published external handle.
 */
public class WatermediaFrameTexture extends AbstractTexture {

    private static final String LABEL_KONKRETE = "Konkrete WaterMedia frame";
    private static final int TEXTURE_USAGE_KONKRETE = GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_COPY_SRC | GpuTexture.USAGE_TEXTURE_BINDING;
    private static final FrameBufferCache FRAME_BUFFER_CACHE_KONKRETE = new FrameBufferCache();

    /** Active non-owning OpenGL facade, when using OpenGL. */
    @Nullable protected WatermediaGlTexture watermediaGlTexture;
    /** Active logical Vulkan placeholder texture, when using Vulkan. */
    @Nullable protected WatermediaVulkanTexture watermediaVulkanTexture;
    /** Active non-owning external Vulkan view facade, when using Vulkan. */
    @Nullable protected WatermediaVulkanTextureView watermediaVulkanTextureView;
    /** Graphics backend selected when this texture was created. */
    protected final boolean vulkan;

    /** Creates the facade on the render thread; Vulkan interop must already be registered when Vulkan is active. */
    public WatermediaFrameTexture(long handle) {
        this.vulkan = RenderingUtils.isVulkanActive();
        if (this.vulkan) {
            VulkanDevice device = WatermediaVulkanInterop.device();
            if (device == null) throw new IllegalStateException("Minecraft's Vulkan device is unavailable for the Watermedia frame texture");
            this.watermediaVulkanTexture = new WatermediaVulkanTexture(device, TEXTURE_USAGE_KONKRETE, LABEL_KONKRETE, GpuFormat.RGBA8_UNORM, 100, 100, 1, 1);
            this.watermediaVulkanTextureView = new WatermediaVulkanTextureView(device, this.watermediaVulkanTexture);
            this.texture = this.watermediaVulkanTexture;
            this.textureView = this.watermediaVulkanTextureView;
        } else {
            this.watermediaGlTexture = new WatermediaGlTexture(TEXTURE_USAGE_KONKRETE, LABEL_KONKRETE, GpuFormat.RGBA8_UNORM, 100, 100, 1, 1, WatermediaReflectionBridge.openGlTextureId(handle));
            this.texture = this.watermediaGlTexture;
            this.textureView = RenderSystem.getDevice().createTextureView(this.texture);
        }
        this.sampler = RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST, false);
        this.setHandle(handle);
    }

    /** Replaces the borrowed Watermedia handle on the render thread without taking ownership. */
    public void setHandle(long handle) {
        if (this.vulkan) {
            if (this.watermediaVulkanTextureView != null) this.watermediaVulkanTextureView.setExternalImageView(handle);
            return;
        }
        int id = WatermediaReflectionBridge.openGlTextureId(handle);
        if (this.watermediaGlTexture == null || this.watermediaGlTexture.glId() == id) return;
        int width = Math.max(1, this.watermediaGlTexture.getWidth(0));
        int height = Math.max(1, this.watermediaGlTexture.getHeight(0));
        this.watermediaGlTexture = new WatermediaGlTexture(TEXTURE_USAGE_KONKRETE, LABEL_KONKRETE, GpuFormat.RGBA8_UNORM, width, height, 1, 1, id);
        this.texture = this.watermediaGlTexture;
        if (this.textureView != null) {
            this.textureView.close();
        }
        this.textureView = RenderSystem.getDevice().createTextureView(this.texture);
    }

    /** Updates the logical frame width on the render thread without reallocating the external image. */
    public void setWidth(int width) {
        if (this.watermediaGlTexture != null) this.watermediaGlTexture.setWidth(width);
        if (this.watermediaVulkanTexture != null) this.watermediaVulkanTexture.setWidth(width);
    }

    /** Updates the logical frame height on the render thread without reallocating the external image. */
    public void setHeight(int height) {
        if (this.watermediaGlTexture != null) this.watermediaGlTexture.setHeight(height);
        if (this.watermediaVulkanTexture != null) this.watermediaVulkanTexture.setHeight(height);
    }

    /** Non-owning OpenGL texture facade. */
    protected static class WatermediaGlTexture extends GlTexture {

        /** Logical media width. */
        protected int width;
        /** Logical media height. */
        protected int height;

        /** Creates a logical facade over a Watermedia-owned OpenGL name. */
        protected WatermediaGlTexture(int usage, String label, GpuFormat format, int width, int height, int depthOrLayers, int mipLevels, int glId) {
            super(usage, label, format, width, height, depthOrLayers, mipLevels, glId, FRAME_BUFFER_CACHE_KONKRETE);
            this.width = width;
            this.height = height;
        }

        /** {@inheritDoc} */
        @Override
        public int getWidth(int mipLevel) {
            return this.width >> mipLevel;
        }

        /** Updates the logical media width. */
        protected void setWidth(int width) {
            this.width = width;
        }

        /** {@inheritDoc} */
        @Override
        public int getHeight(int mipLevel) {
            return this.height >> mipLevel;
        }

        /** Updates the logical media height. */
        protected void setHeight(int height) {
            this.height = height;
        }

        /** Avoids deleting the Watermedia-owned OpenGL texture name. */
        @Override
        public void close() {
            // Watermedia owns the OpenGL texture object.
        }

    }

    /** Owned placeholder texture whose view is replaced by Watermedia's borrowed VkImageView. */
    protected static class WatermediaVulkanTexture extends VulkanGpuTexture {

        /** Logical media width. */
        protected int width;
        /** Logical media height. */
        protected int height;

        /** Creates the one-pixel owned placeholder and logical media dimensions. */
        protected WatermediaVulkanTexture(VulkanDevice device, int usage, String label, GpuFormat format, int width, int height, int depthOrLayers, int mipLevels) {
            // Only a 1x1 owned placeholder is allocated. Minecraft reads the overridden logical size while rendering WaterMedia's external image view.
            super(device, usage, label, format, 1, 1, depthOrLayers, mipLevels);
            this.width = width;
            this.height = height;
        }

        /** {@inheritDoc} */
        @Override
        public int getWidth(int mipLevel) {
            return this.width >> mipLevel;
        }

        /** Updates the logical media width. */
        protected void setWidth(int width) {
            this.width = width;
        }

        /** {@inheritDoc} */
        @Override
        public int getHeight(int mipLevel) {
            return this.height >> mipLevel;
        }

        /** Updates the logical media height. */
        protected void setHeight(int height) {
            this.height = height;
        }

    }

}
