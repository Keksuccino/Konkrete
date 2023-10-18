package de.keksuccino.konkrete.events.client;

import de.keksuccino.konkrete.events.EventBase;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;

public class AfterRenderScreenEvent extends EventBase {

    protected final Screen screen;
    protected final GuiGraphics graphics;
    protected final int mouseX;
    protected final int mouseY;
    protected final float partial;

    public AfterRenderScreenEvent(@NotNull Screen screen, @NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        this.screen = Objects.requireNonNull(screen);
        this.graphics = Objects.requireNonNull(graphics);
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.partial = partial;
    }

    public Screen getScreen() {
        return screen;
    }

    public GuiGraphics getGraphics() {
        return graphics;
    }

    public int getMouseX() {
        return mouseX;
    }

    public int getMouseY() {
        return mouseY;
    }

    public float getPartial() {
        return partial;
    }

    @Override
    public boolean isCancelable() {
        return false;
    }

}
