package de.keksuccino.konkrete.events.client;

import java.util.List;
import java.util.function.Consumer;

import de.keksuccino.konkrete.events.EventBase;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.util.math.MatrixStack;

public class GuiScreenEvent extends EventBase {

	private Screen screen;
	
	public GuiScreenEvent(Screen screen) {
		this.screen = screen;
	}
	
	@Override
	public boolean isCancelable() {
		return true;
	}
	
	public Screen getGui() {
		return this.screen;
	}
	
	public static class InitGuiEvent extends GuiScreenEvent {
		
		private Consumer<PressableWidget> add;
		private Consumer<PressableWidget> remove;
		
		private List<PressableWidget> buttons;
		
		public InitGuiEvent(Screen screen, List<PressableWidget> buttons, Consumer<PressableWidget> addButton, Consumer<PressableWidget> removeButton) {
			super(screen);
			this.add = addButton;
			this.remove = removeButton;
			this.buttons = buttons;
		}
		
		public List<PressableWidget> getWidgetList() {
			return this.buttons;
		}
		
		public void addWidget(PressableWidget widget) {
			this.add.accept(widget);
		}
		
		public void removeWidget(PressableWidget widget) {
			this.remove.accept(widget);
		}
		
		public static class Pre extends InitGuiEvent {

			public Pre(Screen screen, List<PressableWidget> buttons, Consumer<PressableWidget> addButton, Consumer<PressableWidget> removeButton) {
				super(screen, buttons, addButton, removeButton);
			}
			
		}
		
		public static class Post extends InitGuiEvent {

			public Post(Screen screen, List<PressableWidget> buttons, Consumer<PressableWidget> addButton, Consumer<PressableWidget> removeButton) {
				super(screen, buttons, addButton, removeButton);
			}
			
		}
		
	}
	
	public static class DrawScreenEvent extends GuiScreenEvent {

		private MatrixStack matrix;
		private int mouseX;
		private int mouseY;
		private float renderTicks;
		
		public DrawScreenEvent(Screen screen, MatrixStack matrix, int mouseX, int mouseY, float renderPartialTicks) {
			super(screen);
			this.mouseX = mouseX;
			this.mouseY = mouseY;
			this.renderTicks = renderPartialTicks;
			this.matrix = matrix;
		}
		
		public MatrixStack getMatrixStack() {
			return this.matrix;
		}
		
		public int getMouseX() {
			return this.mouseX;
		}
		
		public int getMouseY() {
			return this.mouseY;
		}
		
		public float getRenderPartialTicks() {
			return this.renderTicks;
		}
		
		public static class Pre extends DrawScreenEvent {

			public Pre(Screen screen, MatrixStack matrix, int mouseX, int mouseY, float renderPartialTicks) {
				super(screen, matrix, mouseX, mouseY, renderPartialTicks);
			}

		}
		
		public static class Post extends DrawScreenEvent {

			public Post(Screen screen, MatrixStack matrix, int mouseX, int mouseY, float renderPartialTicks) {
				super(screen, matrix, mouseX, mouseY, renderPartialTicks);
			}

		}
		
	}

    public static class BackgroundDrawnEvent extends GuiScreenEvent {
    	
        private final MatrixStack matrix;

        public BackgroundDrawnEvent(Screen gui, MatrixStack matrix) {
            super(gui);
            this.matrix = matrix;
        }

        public MatrixStack getMatrixStack() {
            return matrix;
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
