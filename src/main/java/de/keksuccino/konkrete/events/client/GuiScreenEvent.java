package de.keksuccino.konkrete.events.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphics;
import de.keksuccino.konkrete.events.EventBase;

@SuppressWarnings("all")
public class GuiScreenEvent extends EventBase {

	private Screen screen;
	
	public GuiScreenEvent(Screen screen) {
		this.screen = screen;
	}
	
	@Override
	public boolean isCancelable() {
		return true;
	}

    public Screen getScreen() {
        return this.screen;
    }

    @Deprecated
	public Screen getGui() {
		return this.screen;
	}

    public static class BackgroundDrawnEvent extends GuiScreenEvent {
    	
        private final GuiGraphics graphics;

        public BackgroundDrawnEvent(Screen gui, GuiGraphics graphics) {
            super(gui);
            this.graphics = graphics;
        }

        public GuiGraphics getGuiGraphics() {
            return this.graphics;
        }

        public PoseStack getPoseStack() {
            return this.graphics.pose();
        }

        @Deprecated
        public PoseStack getMatrixStack() {
            return this.graphics.pose();
        }

    }

    public static class MouseInputEvent extends GuiScreenEvent {
    	
        private final double mouseX;
        private final double mouseY;

        public MouseInputEvent(Screen gui, double mouseX, double mouseY) {
            super(gui);
            this.mouseX = mouseX;
            this.mouseY = mouseY;
        }

        public double getMouseX() {
            return mouseX;
        }

        public double getMouseY() {
            return mouseY;
        }
    }

    public static class MouseClickedEvent extends MouseInputEvent {
    	
        private final int button;

        public MouseClickedEvent(Screen gui, double mouseX, double mouseY, int button) {
            super(gui, mouseX, mouseY);
            this.button = button;
        }

        public int getButton() {
            return button;
        }

        public static class Pre extends MouseClickedEvent {
        	
            public Pre(Screen gui, double mouseX, double mouseY, int button) {
                super(gui, mouseX, mouseY, button);
            }
            
        }

        public static class Post extends MouseClickedEvent {
        	
            public Post(Screen gui, double mouseX, double mouseY, int button) {
                super(gui, mouseX, mouseY, button);
            }
            
        }
    }

    public static class MouseReleasedEvent extends MouseInputEvent {
    	
        private int button;

        public MouseReleasedEvent(Screen gui, double mouseX, double mouseY, int button) {
            super(gui, mouseX, mouseY);
            this.button = button;
        }

        public int getButton() {
            return button;
        }

        public static class Pre extends MouseReleasedEvent {
        	
            public Pre(Screen gui, double mouseX, double mouseY, int button) {
                super(gui, mouseX, mouseY, button);
            }
            
        }

        public static class Post extends MouseReleasedEvent {
        	
            public Post(Screen gui, double mouseX, double mouseY, int button) {
                super(gui, mouseX, mouseY, button);
            }
            
        }
    }

//    public static class MouseDragEvent extends MouseInputEvent {
//    	
//        private int mouseButton;
//        private double dragX;
//        private double dragY;
//
//        public MouseDragEvent(Screen gui, double mouseX, double mouseY, int mouseButton, double dragX, double dragY) {
//            super(gui, mouseX, mouseY);
//            this.mouseButton = mouseButton;
//            this.dragX = dragX;
//            this.dragY = dragY;
//        }
//
//        public int getMouseButton() {
//            return mouseButton;
//        }
//
//        public double getDragX() {
//            return dragX;
//        }
//
//        public double getDragY() {
//            return dragY;
//        }
//
//        public static class Pre extends MouseDragEvent {
//        	
//            public Pre(Screen gui, double mouseX, double mouseY, int mouseButton, double dragX, double dragY) {
//                super(gui, mouseX, mouseY, mouseButton, dragX, dragY);
//            }
//            
//        }
//
//        public static class Post extends MouseDragEvent {
//        	
//            public Post(Screen gui, double mouseX, double mouseY, int mouseButton, double dragX, double dragY) {
//                super(gui, mouseX, mouseY, mouseButton, dragX, dragY);
//            }
//            
//        }
//    }

    public static class MouseScrollEvent extends MouseInputEvent {
    	
        private double scrollDelta;

        public MouseScrollEvent(Screen gui, double mouseX, double mouseY, double scrollDelta) {
            super(gui, mouseX, mouseY);
            this.scrollDelta = scrollDelta;
        }

        public double getScrollDelta() {
            return scrollDelta;
        }

        public static class Pre extends MouseScrollEvent {
        	
            public Pre(Screen gui, double mouseX, double mouseY, double scrollDelta) {
                super(gui, mouseX, mouseY, scrollDelta);
            }
            
        }

        public static class Post extends MouseScrollEvent {
        	
            public Post(Screen gui, double mouseX, double mouseY, double scrollDelta) {
                super(gui, mouseX, mouseY, scrollDelta);
            }
            
        }
    }

    public static class KeyboardKeyEvent extends GuiScreenEvent {
    	
        private int keyCode;
        private int scanCode;
        private int modifiers;

        public KeyboardKeyEvent(Screen gui, int keyCode, int scanCode, int modifiers) {
            super(gui);
            this.keyCode = keyCode;
            this.scanCode = scanCode;
            this.modifiers = modifiers;
        }

        public int getKeyCode() {
            return keyCode;
        }

        public int getScanCode() {
            return scanCode;
        }

        public int getModifiers() {
            return modifiers;
        }
    }

    public static class KeyboardKeyPressedEvent extends KeyboardKeyEvent {
    	
        public KeyboardKeyPressedEvent(Screen gui, int keyCode, int scanCode, int modifiers) {
            super(gui,  keyCode, scanCode, modifiers);
        }

        public static class Pre extends KeyboardKeyPressedEvent {
        	
            public Pre(Screen gui, int keyCode, int scanCode, int modifiers) {
                super(gui, keyCode, scanCode, modifiers);
            }
            
        }

        public static class Post extends KeyboardKeyPressedEvent {
        	
            public Post(Screen gui, int keyCode, int scanCode, int modifiers) {
                super(gui, keyCode, scanCode, modifiers);
            }
            
        }
    }

    public static class KeyboardKeyReleasedEvent extends KeyboardKeyEvent {
    	
        public KeyboardKeyReleasedEvent(Screen gui, int keyCode, int scanCode, int modifiers) {
            super(gui, keyCode, scanCode, modifiers);
        }

        public static class Pre extends KeyboardKeyReleasedEvent {
        	
            public Pre(Screen gui, int keyCode, int scanCode, int modifiers) {
                super(gui, keyCode, scanCode, modifiers);
            }
            
        }

        public static class Post extends KeyboardKeyReleasedEvent {
        	
            public Post(Screen gui, int keyCode, int scanCode, int modifiers) {
                super(gui, keyCode, scanCode, modifiers);
            }
            
        }
    }

    public static class KeyboardCharTypedEvent extends GuiScreenEvent {
        private char codePoint;
        private int modifiers;

        public KeyboardCharTypedEvent(Screen gui, char codePoint, int modifiers) {
            super(gui);
            this.codePoint = codePoint;
            this.modifiers = modifiers;
        }

        public char getCodePoint() {
            return codePoint;
        }

        public int getModifiers() {
            return modifiers;
        }

        public static class Pre extends KeyboardCharTypedEvent {
        	
            public Pre(Screen gui, char codePoint, int modifiers) {
                super(gui, codePoint, modifiers);
            }
            
        }

        public static class Post extends KeyboardCharTypedEvent {
        	
            public Post(Screen gui, char codePoint, int modifiers) {
                super(gui, codePoint, modifiers);
            }
            
        }
    }

}
