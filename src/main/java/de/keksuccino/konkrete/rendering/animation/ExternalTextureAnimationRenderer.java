//AnimationRenderer
//Copyright (c) Keksuccino

package de.keksuccino.konkrete.rendering.animation;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.texture.TextureManager;
import com.google.common.io.Files;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.konkrete.rendering.RenderUtils;
import de.keksuccino.konkrete.resources.ExternalTextureResourceLocation;

public class ExternalTextureAnimationRenderer implements IAnimationRenderer {
	
	private String resourceDir;
	private int fps;
	private boolean loop;
	private int width;
	private int height;
	private int x;
	private int y;
	private List<ExternalTextureResourceLocation> resources = new ArrayList<ExternalTextureResourceLocation>();
	private List<String> resourcePaths = new ArrayList<String>();
	private boolean stretch = false;
	private boolean hide = false;
	private volatile boolean done = false;
	
	private volatile boolean ready = false;

	private int frame = 0;
	private long prevTime = 0;
	
	protected float opacity = 1.0F;

	/**
	 * Renders an animation out of multiple images (frames) stored outside of the mod JAR.<br><br>
	 * 
	 * Just create a new directory and put all animation frames in it.<br>
	 * The frames must be named like: 1.png, 2.png, 3.png, ...
	 * 
	 * @param resourceDir The path pointing to the animation resource directory.
	 * @param fps Frames per second. A value of -1 sets the fps to unlimited.
	 * @param loop If the animation should run in an endless loop or just a single time.
	 */
	public ExternalTextureAnimationRenderer(String resourceDir, int fps, boolean loop, int posX, int posY, int width, int height) {
		this.fps = fps;
		this.loop = loop;
		this.x = posX;
		this.y = posY;
		this.width = width;
		this.height = height;
		this.resourceDir = resourceDir;
	}
	
	public ExternalTextureAnimationRenderer(int fps, boolean loop, int posX, int posY, int width, int height, String... resourcePaths) {
		this.fps = fps;
		this.loop = loop;
		this.x = posX;
		this.y = posY;
		this.width = width;
		this.height = height;
		this.resourcePaths.addAll(Arrays.asList(resourcePaths));
	}
	
	/**
	 * Needs to be called before calling {@link ExternalTextureAnimationRenderer#render(PoseStack)} and after minecraft's {@link TextureManager} instance was loaded.
	 */
	@Override
	public void prepareAnimation() {
		try {
			
			List<String> pathsFinal = new ArrayList<String>();

			if (this.resourcePaths.isEmpty()) {
				File f = new File(this.resourceDir);
				if (f.isDirectory() && f.exists()) {
					for (File in : f.listFiles()) {
						if (isValidFrame(in)) {
							pathsFinal.add(in.getPath());
						}
					}
				}
			} else {
				for (String s : this.resourcePaths) {
					File in = new File(s);
					if (isValidFrame(in)) {
						pathsFinal.add(in.getPath());
					}
				}
			}
			
			CharacterFilter c = CharacterFilter.getIntegerCharacterFiler();
			
			List<String> nonNumberNames = new ArrayList<String>();
			List<String> numberNames = new ArrayList<String>();
			
			for (String s : pathsFinal) {
				String name = Files.getNameWithoutExtension(s);
				if (name != null) {
					String digit = c.filterForAllowedChars(name);
					if (!digit.equals("")) {
						numberNames.add(s);
					} else {
						nonNumberNames.add(s);
					}
				}
				
			}
			
			Collections.sort(nonNumberNames, String.CASE_INSENSITIVE_ORDER);
			
			Collections.sort(numberNames, new Comparator<String>() {
				@Override
				public int compare(String o1, String o2) {
					String s1 = Files.getNameWithoutExtension(o1);
					String s2 = Files.getNameWithoutExtension(o2);
					if ((s1 != null) && (s2 != null)) {
						String n1 = c.filterForAllowedChars(s1);
						String n2 = c.filterForAllowedChars(s2);
						if (MathUtils.isInteger(n1) && MathUtils.isInteger(n2)) {
							int i1 = Integer.parseInt(n1);
							int i2 = Integer.parseInt(n2);
							
							if (i1 > i2) {
								return 1;
							}
							if (i1 < i2) {
								return -1;
							}
							return 0;
							
						}
					}
					return 0;
				}
			});

			for (String s : nonNumberNames) {
				File in = new File(s);
				ExternalTextureResourceLocation er = new ExternalTextureResourceLocation(in.getAbsolutePath());
				er.loadTexture();
				if (er.isReady()) {
					this.resources.add(er);
				}
			}
			
			for (String s : numberNames) {
				File in = new File(s);
				ExternalTextureResourceLocation er = new ExternalTextureResourceLocation(in.getAbsolutePath());
				er.loadTexture();
				if (er.isReady()) {
					this.resources.add(er);
				}
			}

			if (!this.resources.isEmpty()) {
				if (this.width == 0) {
					this.width = this.resources.get(0).getWidth();
				}
				if (this.height == 0) {
					this.height = this.resources.get(0).getHeight();
				}
			}

			this.ready = true;
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void render(PoseStack matrix) {
		if ((this.resources == null) || (this.resources.isEmpty())) {
			return;
		}

		if (!this.ready) {
			return;
		}
		
		//A value of -1 sets the max fps to unlimited
		if (this.fps < 0) {
			this.fps = -1;
		}

		if (this.frame > this.resources.size()-1) {
			if (this.loop) {
				this.resetAnimation();
			} else {
				this.done = true;
				if (!this.hide) {
					this.frame = this.resources.size()-1;
				} else {
					return;
				}
			}
		}

		//Rendering the current frame
		this.renderFrame(matrix);
		
		//Updating the current frame based on the fps value
		long time = System.currentTimeMillis();
		if (this.fps == -1) {
			this.updateFrame(time);
		} else {
			if ((this.prevTime + (1000 / this.fps)) <= time) {
				this.updateFrame(time);
			}
		}
	}
	
	private void renderFrame(PoseStack matrix) {
		int h = this.height;
		int w = this.width;
		int x2 = this.x;
		int y2 = this.y;
		
		if (this.stretch) {
			h = Minecraft.getInstance().screen.height;
			w = Minecraft.getInstance().screen.width;
			x2 = 0;
			y2 = 0;
		}
		
		ExternalTextureResourceLocation r = this.resources.get(this.frame);
		if (!r.isReady()) {
			r.loadTexture();
		}

		RenderUtils.bindTexture(r.getResourceLocation());
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.opacity);
		GuiComponent.blit(matrix, x2, y2, 0.0F, 0.0F, w, h, w, h);
		RenderSystem.disableBlend();
	}
	
	public void setOpacity(float opacity) {
		this.opacity = opacity;
	}

	public float getOpacity() {
		return this.opacity;
	}
	
	private void updateFrame(long time) {
		this.frame++;
		this.prevTime = time;
	}
	
	private static boolean isValidFrame(File f) {
		if (!f.exists() || !f.isFile()) {
			return false;
		}
		String name = f.getName();
		if (!name.endsWith(".jpg") && !name.endsWith(".jpeg") && !name.endsWith(".png")) {
			System.out.println("############## ERROR [FANCYMENU] ##############");
			System.out.println("Invalid animation frame found! Unsupported filetype! Only JPG/JPEG and PNG are supported!");
			System.out.println("Frame path: " + f.getPath());
			System.out.println("###############################################");
			return false;
		}
		return true;
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
		return this.ready;
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
