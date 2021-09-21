//AnimationRenderer
//Copyright (c) Keksuccino

package de.keksuccino.konkrete.rendering.animation;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import com.google.common.io.Files;

import de.keksuccino.konkrete.rendering.GifDecoder;
import de.keksuccino.konkrete.rendering.GifDecoder.GifImage;
import de.keksuccino.konkrete.resources.ExternalTextureResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureManager;

public class ExternalGifAnimationRenderer implements IAnimationRenderer {
	
	private String resourceDir;
	private int fps = 0;
	private boolean loop;
	private int width;
	private int height;
	private int x;
	private int y;
	private List<ExternalTextureResourceLocation> resources = new ArrayList<ExternalTextureResourceLocation>();
	private List<Integer> delays = new ArrayList<Integer>();
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
	 * @param resourcePath The path pointing to the GIF.
	 * @param loop If the animation should run in an endless loop or just a single time.
	 */
	public ExternalGifAnimationRenderer(String resourcePath, boolean loop, int posX, int posY, int width, int height) {
		this.loop = loop;
		this.x = posX;
		this.y = posY;
		this.width = width;
		this.height = height;
		this.resourceDir = resourcePath;
	}
	
	/**
	 * Needs to be called before calling {@link ExternalGifAnimationRenderer#render()} and after minecraft's {@link TextureManager} instance was loaded.
	 */
	@Override
	public void prepareAnimation() {
		if (this.ready) {
			return;
		}
		
		try {
			File f = new File(this.resourceDir);
			
			if (f.exists() && f.isFile() && Files.getFileExtension(f.getName()).equalsIgnoreCase("gif")) {
				
				//Loading all frames into ResourceLocations so minecraft can render them
				this.resources.clear();
				this.delays.clear();

				for (GifFramePackage g : getGifFrames(this.resourceDir)) {
					ExternalTextureResourceLocation er = new ExternalTextureResourceLocation(g.gif);
					er.loadTexture();
					if (er.isReady()) {
						this.resources.add(er);
						this.delays.add(g.delay);
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
				
			} else {
				System.out.println("################ ERROR [KONKRETE] ################");
				System.out.println("Unable to load GIF animation from " + this.resourceDir + "!");
				System.out.println("File not found or isn't a GIF!");
				System.out.println("###################################################");
			}
			
			this.ready = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void render() {
		if ((this.resources == null) || (this.resources.isEmpty())) {
			this.done = true;
			return;
		}

		if (!this.ready) {
			return;
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
		this.renderFrame();
		
		//Updating the current frame based on the fps value
		long time = System.currentTimeMillis();
		if (this.fps == -1) {
			this.updateFrame(time);
		} else if (this.fps == 0) {
			if ((this.prevTime + (this.delays.get(this.frame) * 10)) <= time) {
				this.updateFrame(time);
			}
		} else {
			if ((this.prevTime + (1000 / this.fps)) <= time) {
				this.updateFrame(time);
			}
		}
	}
	
	private void renderFrame() {
		int h = this.height;
		int w = this.width;
		int x2 = this.x;
		int y2 = this.y;
		
		if (this.stretch) {
			h = Minecraft.getMinecraft().currentScreen.height;
			w = Minecraft.getMinecraft().currentScreen.width;
			x2 = 0;
			y2 = 0;
		}
		
		Minecraft.getMinecraft().getTextureManager().bindTexture(this.resources.get(this.frame).getResourceLocation());
		GlStateManager.enableBlend();
		GlStateManager.color(1.0F, 1.0F, 1.0F, this.opacity);
		GuiIngame.drawModalRectWithCustomSizedTexture(x2, y2, 0.0F, 0.0F, w, h, w, h);
		GlStateManager.disableBlend();
	}
	
	@Override
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

	private static List<GifFramePackage> getGifFrames(String gifPath) {
		File f = new File(gifPath);
		List<GifFramePackage> l = new ArrayList<GifFramePackage>();
		
		try {
			
		    if (f.exists() && f.isFile() && Files.getFileExtension(f.getName()).equalsIgnoreCase("gif")) {
		    	
			    FileInputStream is = new FileInputStream(f);
			    GifImage gif = GifDecoder.read(is);

			    int noi = gif.getFrameCount();

			    for (int i = 0; i < noi; i++) {
			    	try {
			    		
			    		int delay = gif.getDelay(i);
				        BufferedImage image = gif.getFrame(i);
				        ByteArrayOutputStream os = new ByteArrayOutputStream();
				        
				        ImageIO.write(image, "PNG", os);
				        
				        ByteArrayInputStream bis = new ByteArrayInputStream(os.toByteArray());
				        
				        l.add(new GifFramePackage(bis, delay));
				        
			    	} catch (Exception e) {
			    		System.out.println("################ ERROR [KONKRETE] ################");
			    		System.out.println("An error happened while trying to read frame " + (i + 1) + " of GIF file '" + gifPath + "'!");
			    		System.out.println("This most probably happened because the GIF is slightly corrupted. Reconverting it could fix this.");
			    		System.out.println("###################################################");
			    		e.printStackTrace();
			    	}
			    }
			    
		    }
		    
		} catch (IOException e) {
		    e.printStackTrace();
		}
		
		return l;
	}
	
//	private static List<GifFramePackage> getGifFrames(String gifPath) {
//		File f = new File(gifPath);
//		List<GifFramePackage> l = new ArrayList<GifFramePackage>();
//		
//		try {
//			
//		    if (f.exists() && f.isFile() && Files.getFileExtension(f.getName()).equalsIgnoreCase("gif")) {
//		    	String[] imageatt = new String[]{
//			            "imageLeftPosition",
//			            "imageTopPosition",
//			            "imageWidth",
//			            "imageHeight"
//			    };    
//
//			    ImageReader reader = new PatchedGIFImageReader(null);
//			    ImageInputStream ciis = ImageIO.createImageInputStream(f);
//			    reader.setInput(ciis, false);
//
//			    int noi = reader.getNumImages(true);
//			    BufferedImage master = null;
//			    
//			    System.out.println("GIF IMAGE COUNT: " + noi);
//			    
//			    for (int i = 0; i < noi; i++) {
//			    	try {
//			    		int delay = 0;
//				        BufferedImage image = reader.read(i);
//				        IIOMetadata metadata = reader.getImageMetadata(i);
//
//				        Node tree = metadata.getAsTree("javax_imageio_gif_image_1.0");
//				        NodeList children = tree.getChildNodes();
//
//				        for (int j = 0; j < children.getLength(); j++) {
//				            Node nodeItem = children.item(j);
//
//				            if (nodeItem.getNodeName().equals("ImageDescriptor")) {
//				                Map<String, Integer> imageAttr = new HashMap<String, Integer>();
//
//				                for (int k = 0; k < imageatt.length; k++) {
//				                    NamedNodeMap attr = nodeItem.getAttributes();
//				                    Node attnode = attr.getNamedItem(imageatt[k]);
//				                    imageAttr.put(imageatt[k], Integer.valueOf(attnode.getNodeValue()));
//				                }
//				                if (i == 0) {
//				                    master = new BufferedImage(imageAttr.get("imageWidth"), imageAttr.get("imageHeight"), BufferedImage.TYPE_INT_ARGB);
//				                }
//				                master.getGraphics().drawImage(image, imageAttr.get("imageLeftPosition"), imageAttr.get("imageTopPosition"), null);
//				            }
//				            
//				            if (nodeItem.getNodeName().equals("GraphicControlExtension")) {
//	                            String d = ((IIOMetadataNode)nodeItem).getAttribute("delayTime");
//	                            
//	                            if ((d != null) && MathUtils.isInteger(d)) {
//	                            	delay = Integer.parseInt(d);
//	                            }
//				            }
//				            
//				        }
//				        
//				        ByteArrayOutputStream os = new ByteArrayOutputStream();
//				        ImageIO.write(master, "PNG", os);
//				        ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
//				        l.add(new GifFramePackage(is, delay));
//			    	} catch (Exception e) {
//			    		System.out.println("################ ERROR [KONKRETE] ################");
//			    		System.out.println("An error happened while trying to read frame " + (i + 1) + " of GIF file '" + gifPath + "'!");
//			    		System.out.println("This most probably happened because the GIF is slightly corrupted. Reconverting it could fix this.");
//			    		System.out.println("###################################################");
//			    		e.printStackTrace();
//			    	}
//			    }
//		    }
//		    
//		} catch (IOException e) {
//		    e.printStackTrace();
//		}
//		
//		return l;
//	}
	
	public static class GifFramePackage {
		
		ByteArrayInputStream gif;
		int delay;
		
		public GifFramePackage(ByteArrayInputStream gif, int delay) {
			this.gif = gif;
			this.delay = delay;
		}
		
	}

}
