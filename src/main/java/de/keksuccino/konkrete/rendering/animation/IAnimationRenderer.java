//AnimationRenderer
//Copyright (c) Keksuccino

package de.keksuccino.konkrete.rendering.animation;

import net.minecraft.client.gui.GuiGraphics;

public interface IAnimationRenderer {

	/**
	 * Needs to be called every tick in the render event.
	 */
	public void render(GuiGraphics graphics);

	/**
	 * This overrides the specified height and width values and stretches the animation over the whole screen.
	 */
	public void setStretchImageToScreensize(boolean b);
	
	public boolean isStretchedToStreensize();
	
	/**
	 * Only has affect if the animation isn't getting looped.
	 */
	public void setHideAfterLastFrame(boolean b);
	
	/**
	 * Returns true if the animation is finished.<br>
	 * <b>Thats never the case if the animation is getting looped!</b>
	 * 
	 * @return True if the animation is finished.
	 */
	public boolean isFinished();
	
	public void setWidth(int width);
	
	public int getWidth();
	
	public void setHeight(int height);
	
	public int getHeight();
	
	public void setPosX(int x);
	
	public int getPosX();
	
	public void setPosY(int y);
	
	public int getPosY();
	
	public int currentFrame();
	
	public int animationFrames();
	
	public void resetAnimation();
	
	public boolean isReady();
	
	public void prepareAnimation();
	
	public String getPath();
	
	public void setFPS(int fps);
	
	public boolean isGettingLooped();
	
	public void setLooped(boolean b);
	
	public int getFPS();

	public void setOpacity(float opacity);

}
