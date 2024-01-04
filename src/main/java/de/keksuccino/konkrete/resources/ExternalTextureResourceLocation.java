package de.keksuccino.konkrete.resources;

import java.awt.image.BufferedImage;
import java.io.*;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import javax.imageio.ImageIO;

public class ExternalTextureResourceLocation implements ITextureResourceLocation {

	private static final Logger LOGGER = LogManager.getLogger();

	protected InputStream in;
	protected boolean isJpeg;
	protected String path;
	protected ResourceLocation location;
	protected boolean loaded = false;
	protected int width = 0;
	protected int height = 0;
	
	public ExternalTextureResourceLocation(String path) {
		this.path = path;
	}

	/**
	 * Make sure to only use PNG texture {@link InputStream}s here! JPEGs are not natively supported by Minecraft anymore!
	 */
	public ExternalTextureResourceLocation(InputStream in) {
		this.in = in;
	}

	public ExternalTextureResourceLocation(InputStream in, boolean isJpeg) {
		this.in = in;
		this.isJpeg = isJpeg;
	}
	
	/**
	 * Loads the external texture to a {@link ResourceLocation}.<br>
	 * The main instance of minecraft's {@link TextureManager} needs to be loaded before calling this method.<br><br>
	 * 
	 * After loading the texture, {@link ExternalTextureResourceLocation#isReady()} will return true.
	 */
	public void loadTexture() {
		if (this.loaded) {
			return;
		}
		try {
			if (Minecraft.getInstance().getTextureManager() == null) {
				LOGGER.error("Minecraft TextureManager instance not ready yet! Unable to load texture: " + this.path);
				return;
			}
			boolean isImageJpeg = this.isJpeg;
			if (this.in == null) {
				File f = new File(path);
				isImageJpeg = (f.getAbsolutePath().toLowerCase().endsWith(".jpeg") || f.getAbsolutePath().toLowerCase().endsWith(".jpg"));
				this.in = new FileInputStream(f);
			}
			if (isImageJpeg) {
				LOGGER.warn("[KONKRETE] Loading JPEG image! JPEGs are not natively supported by Minecraft anymore and cause freezes on load! Consider replacing JPEG with PNG image: " + this.path);
			}
			NativeImage i = isImageJpeg ? convertJpegToPng(in) : NativeImage.read(this.in);
			if (i == null) throw new NullPointerException("Failed to load image! NativeImage was null!");
			this.width = i.getWidth();
			this.height = i.getHeight();
			this.location = Minecraft.getInstance().getTextureManager().register("konkrete_external_texture", new SelfcleaningDynamicTexture(i));
			this.in.close();
			loaded = true;
		} catch (Exception ex) {
			LOGGER.error("[KONKRETE] Failed to load image!", ex);
		}
	}

	/**
	 * Converts JPEG images to PNG, because Minecraft dropped support for JPEGs.
	 */
	@Nullable
	protected static NativeImage convertJpegToPng(@NotNull InputStream in) {
		NativeImage nativeImage = null;
		ByteArrayOutputStream byteArrayOut = null;
		try {
			BufferedImage bufferedImage = ImageIO.read(in);
			byteArrayOut = new ByteArrayOutputStream();
			ImageIO.write(bufferedImage, "png", byteArrayOut);
			//ByteArrayInputStream is important, because using NativeImage#read(byte[]) causes OutOfMemoryExceptions
			nativeImage = NativeImage.read(new ByteArrayInputStream(byteArrayOut.toByteArray()));
		} catch (Exception ex) {
			LOGGER.error("[KONKRETE] Failed to convert JPEG image to PNG!", ex);
		}
		IOUtils.closeQuietly(in);
		IOUtils.closeQuietly(byteArrayOut);
		return nativeImage;
	}
	
	public ResourceLocation getResourceLocation() {
		return this.location;
	}

	public String getPath() {
		return this.path;
	}
	
	public boolean isReady() {
		return this.loaded;
	}
	
	public int getHeight() {
		return this.height;
	}
	
	public int getWidth() {
		return this.width;
	}

}
