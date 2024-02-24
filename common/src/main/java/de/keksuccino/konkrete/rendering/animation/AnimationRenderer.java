package de.keksuccino.konkrete.rendering.animation;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Does not work anymore. Don't use this class.
 */
@Deprecated(forRemoval = true)
public class AnimationRenderer implements IAnimationRenderer {
	
	private String resourceDir;
	private int fps;
	private boolean loop;
	private int width;
	private int height;
	private int x;
	private int y;
	protected List<ResourceLocation> resources = new ArrayList<>();
	private boolean stretch = false;
	private boolean hide = false;
	private volatile boolean done = false;
	private String modid;
	private int frame = 0;
	private long prevTime = 0;
	protected float opacity = 1.0F;

	/**
	 * Does not work anymore. Don't use this class.
	 */
	@Deprecated
	public AnimationRenderer(String resourceDir, int fps, boolean loop, int posX, int posY, int width, int height, String modid) {
		this.fps = fps;
		this.loop = loop;
		this.x = posX;
		this.y = posY;
		this.width = width;
		this.height = height;
		this.resourceDir = resourceDir;
		this.modid = modid;
	}

	@Override
	public void render(GuiGraphics graphics) {
	}
	
	protected void renderFrame(GuiGraphics graphics) {
	}
	
	public void setOpacity(float opacity) {
		this.opacity = opacity;
	}
	
	private void updateFrame(long time) {
		this.frame++;
		this.prevTime = time;
	}
	
	@Override
	public void resetAnimation() {
		this.frame = 0;
		this.prevTime = 0;
		this.done = false;
	}

	@Override
	public void setStretchImageToScreensize(boolean b) {
		this.stretch = b;
	}

	@Override
	public void setHideAfterLastFrame(boolean b) {
		this.hide = b;
	}

	@Override
	public boolean isFinished() {
		return this.done;
	}
	
	@Override
	public void setWidth(int width) {
		this.width = width;
	}
	
	@Override
	public void setHeight(int height) {
		this.height = height;
	}
	
	@Override
	public int currentFrame() {
		return this.frame;
	}

	@Override
	public boolean isReady() {
		return true;
	}

	@Override
	public void setPosX(int x) {
		this.x = x;
		
	}

	@Override
	public void setPosY(int y) {
		this.y = y;
	}
	
	@Override
	public int animationFrames() {
		return this.resources.size();
	}

	@Override
	public String getPath() {
		return this.resourceDir;
	}

	@Override
	public void setFPS(int fps) {
		this.fps = fps;
	}

	@Override
	public int getFPS() {
		return this.fps;
	}

	@Override
	public void setLooped(boolean b) {
		this.loop = b;
	}

	@Override
	public void prepareAnimation() {
	}

	@Override
	public boolean isGettingLooped() {
		return this.loop;
	}

	@Override
	public boolean isStretchedToStreensize() {
		return this.stretch;
	}

	@Override
	public int getWidth() {
		return this.width;
	}

	@Override
	public int getHeight() {
		return this.height;
	}

	@Override
	public int getPosX() {
		return this.x;
	}

	@Override
	public int getPosY() {
		return this.y;
	}

}
