package de.keksuccino.konkrete.util.rendering.ui.contextmenu;

import de.keksuccino.konkrete.util.rendering.ui.Tickable;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Coordinates context menu lifecycle and event dispatch. */
public class ContextMenuHandler extends AbstractContainerEventHandler implements Renderable, Tickable {

    /** Shared handler instance. */
    public static final ContextMenuHandler INSTANCE = new ContextMenuHandler();

    private final List<GuiEventListener> children = new ArrayList<>();

    private ContextMenuHandler() {
    }

    /** Sets and open for this context menu handler. */
    public void setAndOpen(@NotNull ContextMenu menu, float x, float y) {
        this.setAndOpen(menu, x, y, null);
    }

    /** Sets and open for this context menu handler. */
    public void setAndOpen(@NotNull ContextMenu menu, float x, float y, @Nullable List<String> entryPath) {
        removeCurrent();
        this.children.add(menu);
        menu.openMenuAt(x, y);
    }

    /** Sets and open at mouse for this context menu handler. */
    public void setAndOpenAtMouse(@NotNull ContextMenu menu) {
        this.setAndOpenAtMouse(menu, null);
    }

    /** Sets and open at mouse for this context menu handler. */
    public void setAndOpenAtMouse(@NotNull ContextMenu menu, @Nullable List<String> entryPath) {
        removeCurrent();
        this.children.add(menu);
        menu.openMenuAtMouse();
    }

    /** Removes current from this context menu handler. */
    public void removeCurrent() {
        ContextMenu current = this.getCurrent();
        if (current != null) current.closeMenuChain();
        this.children.clear();
    }

    /** Returns current. */
    @Nullable
    public ContextMenu getCurrent() {
        if (!this.children.isEmpty()) return (ContextMenu) this.children.get(0);
        return null;
    }

    /** Advances this object's lifecycle by one client tick. */
    @Override
    public void tick() {
    }

    /** Adds this component's complete draw state to the active GUI extraction pass. */
    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        ContextMenu current = this.getCurrent();
        if (current != null) {
            current.extractRenderState(graphics, mouseX, mouseY, partial);
        }
    }

    /** Routes a mouse-button press and reports whether it was consumed. */
    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean isDoubleClick) {
        return this.mouseClicked(event.x(), event.y(), event.button());
    }

    /** Routes a mouse-button press and reports whether it was consumed. */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        ContextMenu current = this.getCurrent();
        if (current != null) {
            if (!current.isUserNavigatingInMenu()) {
                removeCurrent();
                return false;
            }
            return current.mouseClicked(mouseX, mouseY, button);
        }
        return false;
    }

    /** Routes wheel scrolling and reports whether it was consumed. */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        ContextMenu current = this.getCurrent();
        if (current != null) {
            return current.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        return false;
    }

    /** Routes a mouse-button release and reports whether it was consumed. */
    @Override
    public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        return this.mouseReleased(event.x(), event.y(), event.button());
    }

    /** Routes a mouse-button release and reports whether it was consumed. */
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        ContextMenu current = this.getCurrent();
        if (current != null) {
            return current.mouseReleased(mouseX, mouseY, button);
        }
        return false;
    }

    /** Routes a key press and reports whether it was consumed. */
    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        return this.keyPressed(event.key(), event.scancode(), event.modifiers());
    }

    /** Routes a key press and reports whether it was consumed. */
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        ContextMenu current = this.getCurrent();
        if (current != null) {
            return current.keyPressed(keyCode, scanCode, modifiers);
        }
        return false;
    }

    /** Routes a key release and reports whether it was consumed. */
    @Override
    public boolean keyReleased(net.minecraft.client.input.KeyEvent event) {
        return this.keyReleased(event.key(), event.scancode(), event.modifiers());
    }

    /** Routes a key release and reports whether it was consumed. */
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        ContextMenu current = this.getCurrent();
        if (current != null) {
            return current.keyReleased(keyCode, scanCode, modifiers);
        }
        return false;
    }

    /** Routes typed character input and reports whether it was consumed. */
    @Override
    public boolean charTyped(net.minecraft.client.input.CharacterEvent event) {
        return this.charTyped((char)event.codepoint(), 0);
    }

    /** Routes typed character input and reports whether it was consumed. */
    public boolean charTyped(char codePoint, int modifiers) {
        ContextMenu current = this.getCurrent();
        if (current != null) {
            return current.charTyped(codePoint, modifiers);
        }
        return false;
    }

    /** Returns child at. */
    @Override
    public Optional<GuiEventListener> getChildAt(double mouseX, double mouseY) {
        ContextMenu current = this.getCurrent();
        if (current != null && current.isMouseOverMenu(mouseX, mouseY)) {
            return Optional.of(current);
        }
        return Optional.empty();
    }

    /** Returns child listeners used for focus and input routing. */
    @Override
    public @NotNull List<? extends GuiEventListener> children() {
        return this.children;
    }

    /** Reports whether keyboard focus currently targets this control. */
    @Override
    public boolean isFocused() {
        return false;
    }

}
