package de.keksuccino.konkrete;

import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.content.ContextMenu;
import de.keksuccino.konkrete.input.MouseInput;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class Test {

	ContextMenu m = new ContextMenu(100, 16, -1);
	ContextMenu m2 = new ContextMenu(100, 16, -1);
	
	AdvancedButton b = new AdvancedButton(20, 20, 100, 20, "roflmao", true, (press) -> {
		m.openMenuAt(MouseInput.getMouseX(), MouseInput.getMouseY());
	});
	
	public Test() {
		
		m.addChild(m2);
		m.setAutoclose(true);
		m.setParentButton(this.b);
		
		b.setDescription("FUUUUUUUUUUUUUUUUUUUUUU", "ROFL", "LOL");
		
		int i = 0;
		while (i < 10) {
			
			AdvancedButton b = new AdvancedButton(20, 20, 100, 20, "roflmao", true, (press) -> {
				System.out.println("lul");
			});
//			b.setDescription("I HATE THIS", "VERY MUCH");
			
			m.addContent(b);
			
			i++;
		}
		
		AdvancedButton b2 = new AdvancedButton(20, 20, 100, 20, "open child", true, (press) -> {
			m2.openMenuAt(0, press.y);
		});
		
		m.addContent(b2);
		
		i = 0;
		
		while (i < 10) {
			
			AdvancedButton b = new AdvancedButton(20, 20, 100, 20, "roflmao", true, (press) -> {
				System.out.println("lul");
			});
//			b.setDescription("I HATE THIS", "VERY MUCH");
			
			m2.addContent(b);
			
			i++;
		}
		
	}
	
	@SubscribeEvent
	public void onTest(GuiScreenEvent.DrawScreenEvent.Post e) {

		b.render(e.getMatrixStack(), e.getMouseX(), e.getMouseY(), e.getRenderPartialTicks());
		m.render(e.getMatrixStack(), e.getMouseX(), e.getMouseY());
		
		if (MouseInput.isRightMouseDown()) {
			m.openMenuAt(MouseInput.getMouseX(), MouseInput.getMouseY());
		}
	}

}
