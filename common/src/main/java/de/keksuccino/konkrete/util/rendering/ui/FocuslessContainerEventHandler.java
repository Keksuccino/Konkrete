package de.keksuccino.konkrete.util.rendering.ui;

import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import org.jetbrains.annotations.Nullable;

/**
 * {@link ContainerEventHandler}, but fires all events for every child instead of just focused/hovered ones in some cases.
 */
public interface FocuslessContainerEventHandler extends ContainerEventHandler, RoutableUIComponent {

    /** Routes a mouse-button press to eligible children and reports whether it was consumed. */
    default boolean mouseClicked(double mouseX, double mouseY, int button) {
        return this.mouseClicked(new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(button, 0)), false);
    }

    /** Routes a mouse-button release to eligible children and reports whether it was consumed. */
    default boolean mouseReleased(double mouseX, double mouseY, int button) {
        return this.mouseReleased(new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(button, 0)));
    }

    /** Routes pointer dragging to eligible children and reports whether it was consumed. */
    default boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return this.mouseDragged(new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(button, 0)), dragX, dragY);
    }

    /** Routes a key press to eligible children and reports whether it was consumed. */
    default boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return this.keyPressed(new KeyEvent(keyCode, scanCode, modifiers));
    }

    /** Routes a key release to eligible children and reports whether it was consumed. */
    default boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return this.keyReleased(new KeyEvent(keyCode, scanCode, modifiers));
    }

    /** Routes typed character input to eligible children and reports whether it was consumed. */
    default boolean charTyped(char codePoint, int modifiers) {
        return this.charTyped(new CharacterEvent(codePoint));
    }

    /** Routes a mouse-button release to eligible children and reports whether it was consumed. */
    @Override
    default boolean mouseReleased(MouseButtonEvent event) {
        this.setDragging(false);
        for(GuiEventListener child : this.children()) {
            if (child.mouseReleased(event)) return true;
        }
        return false;
    }

    /** Routes pointer dragging to eligible children and reports whether it was consumed. */
    @Override
    default boolean mouseDragged(MouseButtonEvent event, double $$3, double $$4) {
        if (this.isDragging() && (event.button() == 0)) {
            for (GuiEventListener child : this.children()) {
                if (child.mouseDragged(event, $$3, $$4)) return true;
            }
        }
        return false;
    }

    /** Routes wheel scrolling to eligible children and reports whether it was consumed. */
    @Override
    default boolean mouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
        for(GuiEventListener child : this.children()) {
            if (child.mouseScrolled(mouseX, mouseY, scrollDeltaX, scrollDeltaY)) return true;
        }
        return false;
    }

    /** Routes a key press to eligible children and reports whether it was consumed. */
    @Override
    default boolean keyPressed(KeyEvent event) {
        for(GuiEventListener child : this.children()) {
            if (child.keyPressed(event)) return true;
        }
        return false;
    }

    /** Routes a key release to eligible children and reports whether it was consumed. */
    @Override
    default boolean keyReleased(KeyEvent event) {
        for(GuiEventListener child : this.children()) {
            if (child.keyReleased(event)) return true;
        }
        return false;
    }

    /** Routes typed character input to eligible children and reports whether it was consumed. */
    @Override
    default boolean charTyped(CharacterEvent event) {
        for(GuiEventListener child : this.children()) {
            if (child.charTyped(event)) return true;
        }
        return false;
    }

    /** Returns focused for this widget. */
    @Nullable
    @Override
    default GuiEventListener getFocused() {
        return null;
    }

    /** Sets focused for this widget. */
    @Override
    default void setFocused(@Nullable GuiEventListener var1) {
    }

    /** Sets focused for this widget. */
    @Override
    default void setFocused(boolean $$0) {
    }

}
