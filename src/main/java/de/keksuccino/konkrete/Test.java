package de.keksuccino.konkrete;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.content.scrollarea.ScrollArea;
import de.keksuccino.konkrete.gui.content.scrollarea.ScrollAreaEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class Test {

    ScrollArea area = null;
    AdvancedButton xPlus = null;
    AdvancedButton xMinus = null;
    AdvancedButton yPlus = null;
    AdvancedButton yMinus = null;

    @SubscribeEvent
    public void onRenderScreenPost(ScreenEvent.Render.Post e) {

        if (area == null) {
            area = new ScrollArea(10, 10, 300, 100);
            area.backgroundColor = new Color(0, 0, 0);
            area.addEntry(new TextEntry(area, "This is a text.", false));
            area.addEntry(new TextEntry(area, "This is a text.", false));
            area.addEntry(new TextEntry(area, "This is a text.", false));
            area.addEntry(new TextEntry(area, "This is a text.", false));
            area.addEntry(new TextEntry(area, "This is a text.", false));
            area.addEntry(new TextEntry(area, "This is a text.", false));
            area.addEntry(new TextEntry(area, "This is a text.", false));
            area.addEntry(new TextEntry(area, "This is a text.", false));
            area.addEntry(new TextEntry(area, "This is a text.", false));
            area.addEntry(new TextEntry(area, "This is a text.", false));
            area.addEntry(new TextEntry(area, "This is a text.", false));
            area.addEntry(new TextEntry(area, "This is a text.", false));
            area.addEntry(new TextEntry(area, "This is a text.", false));
            area.addEntry(new TextEntry(area, "This is a text.", false));
            area.addEntry(new TextEntry(area, "This is a text.", false));
            area.addEntry(new TextEntry(area, "This is a text.", false));
            xPlus = new AdvancedButton(10, 60, 100, 20, "x +", true, (press) -> {
                area.x = area.x + 10;
            });
            xMinus = new AdvancedButton(10, 80, 100, 20, "x -", true, (press) -> {
                area.x = area.x - 10;
            });
            yPlus = new AdvancedButton(10, 100, 100, 20, "y +", true, (press) -> {
                area.y = area.y + 10;
            });
            yMinus = new AdvancedButton(10, 120, 100, 20, "y -", true, (press) -> {
                area.y = area.y - 10;
            });
        }

        area.render(e.getPoseStack());

        xPlus.render(e.getPoseStack(), e.getMouseX(), e.getMouseY(), e.getPartialTick());
        xMinus.render(e.getPoseStack(), e.getMouseX(), e.getMouseY(), e.getPartialTick());
        yPlus.render(e.getPoseStack(), e.getMouseX(), e.getMouseY(), e.getPartialTick());
        yMinus.render(e.getPoseStack(), e.getMouseX(), e.getMouseY(), e.getPartialTick());

    }

    public static class ScrollAreaEntryBase extends ScrollAreaEntry {

        protected int entryHeight = 25;
        protected java.util.List<String> description = null;
        protected Consumer<EntryRenderCallback> renderBody;

        protected boolean isOverlayButtonHovered = false;

        public ScrollAreaEntryBase(ScrollArea parent, Consumer<EntryRenderCallback> renderBody) {
            super(parent);
            this.renderBody = renderBody;
        }

        @Override
        public void renderEntry(PoseStack matrix) {

            EntryRenderCallback c = new EntryRenderCallback();
            c.entry = this;
            c.matrix = matrix;

            this.renderBody.accept(c);

        }

        @Override
        public int getHeight() {
            return this.entryHeight;
        }

        public void setHeight(int height) {
            this.entryHeight = height;
        }

        public java.util.List<String> getDescription() {
            return this.description;
        }

        public boolean isOverlayButtonHoveredAndOverlapsArea() {
            return (this.isOverlayButtonHovered && this.isHoveredOrFocused());
        }

        public void setDescription(List<String> desc) {
            this.description = desc;
        }

        public void setDescription(String[] desc) {
            this.description = Arrays.asList(desc);
        }

        public static class EntryRenderCallback {

            public ScrollAreaEntryBase entry;
            public PoseStack matrix;

        }

    }

    public static class TextEntry extends ScrollAreaEntryBase {

        public String text;
        public boolean bold;

        public TextEntry(ScrollArea parent, String text, boolean bold) {
            super(parent, null);
            this.text = text;
            this.bold = bold;
            this.renderBody = (render) -> {
                if (this.text != null) {
                    Font font = Minecraft.getInstance().font;
                    int xCenter = render.entry.x + (render.entry.getWidth() / 2);
                    int yCenter = render.entry.y + (render.entry.getHeight() / 2);
                    String s = this.text;
                    if (this.bold) {
                        s = "§l" + this.text;
                    }
                    drawCenteredString(render.matrix, font, s, xCenter, yCenter - (font.lineHeight / 2), -1);
                }
            };
            this.setHeight(18);
        }

    }

}
