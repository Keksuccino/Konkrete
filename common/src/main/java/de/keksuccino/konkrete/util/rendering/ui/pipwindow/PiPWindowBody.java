package de.keksuccino.konkrete.util.rendering.ui.pipwindow;

import de.keksuccino.konkrete.util.input.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Implements the interactive window body for pi p. */
public abstract class PiPWindowBody extends Screen implements PipableScreen {

    @Nullable
    private PiPWindow window;
    /** Whether Escape may close the window. */
    protected boolean allowCloseOnEsc = true;
    private int renderMouseX = 0;
    private int renderMouseY = 0;

    /** Initializes picture-in-picture content with the supplied title. */
    public PiPWindowBody(Component title) {
        super(title);
    }

    /** Creates an empty pi p window body with default state. */
    public PiPWindowBody() {
        super(Component.empty());
    }

    /** Closes window. */
    public void closeWindow() {
        PiPWindow resolvedWindow = resolveWindow();
        if (resolvedWindow == null) {
            onScreenClosed();
            return;
        }
        resolvedWindow.markClosingFromScreen();
        resolvedWindow.close();
    }

    /** Sets window visible for this pi p window body. */
    public void setWindowVisible(boolean visible) {
        PiPWindow resolvedWindow = resolveWindow();
        if (resolvedWindow != null) resolvedWindow.setVisible(visible);
    }

    /** Returns window. */
    public @Nullable PiPWindow getWindow() {
        return window;
    }

    /** Sets window for this pi p window body. */
    @ApiStatus.Internal
    public void setWindow(@Nullable PiPWindow window) {
        this.window = window;
    }

    @Nullable
    private PiPWindow resolveWindow() {
        if (this.window != null) {
            return this.window;
        }
        for (PiPWindow openWindow : PiPWindowHandler.INSTANCE.getOpenWindows()) {
            if (openWindow.getScreen() == this) {
                return openWindow;
            }
        }
        return null;
    }

    /** Returns whether allow close on esc. */
    public boolean isAllowCloseOnEsc() {
        return allowCloseOnEsc;
    }

    /** Sets allow close on esc for this pi p window body. */
    public PiPWindowBody setAllowCloseOnEsc(boolean allowCloseOnEsc) {
        this.allowCloseOnEsc = allowCloseOnEsc;
        return this;
    }

    /** Returns render mouse x. */
    public int getRenderMouseX() {
        return renderMouseX;
    }

    /** Returns render mouse y. */
    public int getRenderMouseY() {
        return renderMouseY;
    }

    /** Adds this component's complete draw state to the active GUI extraction pass. */
    @Override
    public final void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        this.renderMouseX = mouseX;
        this.renderMouseY = mouseY;
        this.renderBody(graphics, mouseX, mouseY, partial);
        super.extractRenderState(graphics, mouseX, mouseY, partial);
        this.renderLateBody(graphics, mouseX, mouseY, partial);
    }

    /** Adds this component's background draw state to the active GUI extraction pass. */
    @Override
    public void extractBackground(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        // PiP screens should render no background
    }

    /** Routes a key press and reports whether it was consumed. */
    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        return this.keyPressed(event.key(), event.scancode(), event.modifiers());
    }

    /** Routes a key press and reports whether it was consumed. */
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.allowCloseOnEsc && (keyCode == InputConstants.KEY_ESCAPE)) {
            this.closeWindow();
            this.onWindowClosedExternally();
            return true;
        }
        return super.keyPressed(new net.minecraft.client.input.KeyEvent(keyCode, scanCode, modifiers));
    }

    /** Routes a mouse-button press and reports whether it was consumed. */
    @Override
    public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean isDoubleClick) {
        for (GuiEventListener listener : this.children()) {
            if (listener.mouseClicked(event, isDoubleClick)) {
                if (listener.shouldTakeFocusAfterInteraction()) {
                    this.setFocused(listener);
                    if (event.button() == 0) {
                        this.setDragging(true);
                    }
                }
                return true;
            }
        }
        return false;
    }

    /** Routes wheel scrolling and reports whether it was consumed. */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
        for (GuiEventListener listener : this.children()) {
            if (listener.mouseScrolled(mouseX, mouseY, scrollDeltaX, scrollDeltaY)) {
                return true;
            }
        }
        return false;
    }

    /** Handles screen closed for this pi p window body. */
    @Override
    public void onScreenClosed() {
    }

    /** Handles window closed externally for this pi p window body. */
    @Override
    public void onWindowClosedExternally() {
    }

    /** Returns whether close on esc. */
    @Override
    public final boolean shouldCloseOnEsc() {
        return false;
    }

    /** Handles close for this pi p window body. */
    @Override
    public final void onClose() {
    }

    /** Removes d from this pi p window body. */
    @Override
    public final void removed() {
    }

}
