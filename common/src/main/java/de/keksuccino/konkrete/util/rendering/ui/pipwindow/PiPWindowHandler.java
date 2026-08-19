package de.keksuccino.konkrete.util.rendering.ui.pipwindow;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.konkrete.util.rendering.RenderingUtils;
import de.keksuccino.konkrete.util.rendering.ui.Tickable;
import de.keksuccino.konkrete.util.rendering.ui.UIBase;
import de.keksuccino.konkrete.util.rendering.ui.UISounds;
import de.keksuccino.konkrete.util.window.WindowHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.jetbrains.annotations.NotNull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Coordinates pi p window lifecycle and event dispatch. */
public class PiPWindowHandler implements GuiEventListener, Tickable, Renderable {

    /** Shared handler instance. */
    public static final PiPWindowHandler INSTANCE = new PiPWindowHandler();
    private static final double DEFAULT_WINDOW_SIZE_SCALE_WIDTH = 0.4;
    private static final double DEFAULT_WINDOW_SIZE_SCALE_HEIGHT = 0.5;
    private static final long BLOCKED_INPUT_OVERLAY_DURATION_MS = 1500L;

    private final List<PiPWindow> windows = new ArrayList<>();
    @Nullable
    private PiPWindow focusedWindow;
    @Nullable
    private PiPWindow activePointerWindow;
    private int activePointerButton = -1;
    private boolean windowClickedThisTick = false;
    @Nullable
    private PiPWindow lastClickedWindowThisTick;
    private boolean isRendering = false;
    @Nullable
    private PiPWindow activeScreenRenderWindow;
    private double activeScreenRenderScaleFactor = 1.0;
    private long blockedInputOverlayUntilMs = -1L;
    @Nullable
    private PiPWindow blockedInputOverlayWindow;
    @Nullable
    private PiPWindow fullscreenInheritanceParentWindow;

    private PiPWindowHandler() {
    }

    /** Opens window. */
    public PiPWindow openWindow(@NotNull PiPWindow window, @Nullable PiPWindow parentWindow) {
        PiPWindow fullscreenInheritanceParent = (parentWindow != null) ? parentWindow : this.fullscreenInheritanceParentWindow;
        if (parentWindow != null) {
            parentWindow.registerChildWindow(window);
        }
        if (windows.contains(window)) {
            bringToFront(window);
            return window;
        }
        inheritTopLayerSettings(window);
        if (fullscreenInheritanceParent != null) {
            window.setMaximized(fullscreenInheritanceParent.isMaximized());
        }
        windows.add(window);
        window.addCloseCallback(() -> closeWindow(window));
        if (parentWindow == null && fullscreenInheritanceParent != null) {
            window.addCloseCallback(() -> fullscreenInheritanceParent.setMaximized(window.isMaximized()));
        }
        bringToFront(window);
        return window;
    }

    /** Opens window centered. */
    public PiPWindow openWindowCentered(@NotNull PiPWindow window, @Nullable PiPWindow parentWindow) {
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        int windowWidth = window.getWidth();
        int windowHeight = window.getHeight();
        int x = (screenWidth - windowWidth) / 2;
        int y = (screenHeight - windowHeight) / 2;
        window.setPosition(x, y);
        return openWindow(window, parentWindow);
    }

    /** Opens window with default size and position. */
    public PiPWindow openWindowWithDefaultSizeAndPosition(@NotNull PiPWindow window, @Nullable PiPWindow parentWindow) {
        Minecraft minecraft = Minecraft.getInstance();
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        int targetWidth = Math.max(1, (int) Math.round(screenWidth * DEFAULT_WINDOW_SIZE_SCALE_WIDTH));
        int targetHeight = Math.max(1, (int) Math.round(screenHeight * DEFAULT_WINDOW_SIZE_SCALE_HEIGHT));
        double guiScale = window.isSizeScaledToGuiScale() ? WindowHandler.getGuiScale() : 1.0;
        if (guiScale <= 1.0) {
            guiScale = 1.0;
        }
        int rawWidth = guiScale > 1.0 ? (int) Math.ceil(targetWidth * guiScale) : targetWidth;
        int rawHeight = guiScale > 1.0 ? (int) Math.ceil(targetHeight * guiScale) : targetHeight;
        int x = (screenWidth - targetWidth) / 2;
        int y = (screenHeight - targetHeight) / 2;
        window.setBounds(x, y, rawWidth, rawHeight);
        return openWindow(window, parentWindow);
    }

    /** Closes window. */
    public void closeWindow(@NotNull PiPWindow window) {
        if (!window.isClosingViaCallback()) {
            window.close();
            return;
        }
        closeWindowInternal(window);
    }

    private void closeWindowInternal(@NotNull PiPWindow window) {
        PiPWindow parent = window.getParentWindow();
        boolean closedByScreen = window.consumeClosingFromScreen();
        if (!closedByScreen) {
            var screen = window.getScreen();
            if (screen instanceof PipableScreen pipableScreen) {
                pipableScreen.onWindowClosedExternally();
            }
        }
        if (window.shouldCloseScreenWithWindow()) {
            window.setScreen(null);
        }
        if (!windows.remove(window)) {
            return;
        }
        window.handleClosed();
        if (parent != null) {
            parent.setMaximized(window.isMaximized());
            parent.unregisterChildWindow(window);
        }
        if (focusedWindow == window) {
            focusedWindow = getTopVisibleWindow();
        }
        if (activePointerWindow == window) {
            activePointerWindow = null;
            activePointerButton = -1;
        }
        enforceForceFocus();
    }

    /** Closes all windows. */
    public void closeAllWindows() {
        closeAllWindows(false);
    }

    /** Closes every active PiP window without consulting close vetoes. */
    public void forceCloseAllWindows() {
        closeAllWindows(true);
    }

    private void closeAllWindows(boolean force) {
        List<PiPWindow> copy = new ArrayList<>(windows);
        for (PiPWindow window : copy) {
            if (force) {
                window.setCloseWindowCheck(null);
            }
            closeWindow(window);
        }
        focusedWindow = null;
        activePointerWindow = null;
        activePointerButton = -1;
    }

    /** Returns open windows. */
    public List<PiPWindow> getOpenWindows() {
        return Collections.unmodifiableList(windows);
    }

    /** Returns whether any window open. */
    public boolean isAnyWindowOpen() {
        return !windows.isEmpty();
    }

    /** Returns top window at. */
    @Nullable
    public PiPWindow getTopWindowAt(double mouseX, double mouseY) {
        for (int i = windows.size() - 1; i >= 0; i--) {
            PiPWindow window = windows.get(i);
            if (!window.isVisible()) {
                continue;
            }
            if (window.isMouseOver(mouseX, mouseY)) {
                return window;
            }
        }
        return null;
    }

    /** Returns whether point visible for window. */
    public boolean isPointVisibleForWindow(@NotNull PiPWindow window, double mouseX, double mouseY) {
        return getTopWindowAt(mouseX, mouseY) == window;
    }

    /** Returns whether mouse reach window. */
    public boolean canMouseReachWindow(@NotNull PiPWindow window, double mouseX, double mouseY) {
        PiPWindow blockingWindow = getTopInputBlockingWindow();
        if (blockingWindow != null && blockingWindow != window) {
            return false;
        }
        return isPointVisibleForWindow(window, mouseX, mouseY);
    }

    /** Refreshes all screens from current state. */
    public void refreshAllScreens() {
        for (PiPWindow window : new ArrayList<>(windows)) {
            window.refreshScreen();
        }
    }

    /** Moves the supplied PiP window above its siblings and gives it focus. */
    public void bringToFront(@NotNull PiPWindow window) {
        if (windows.remove(window)) {
            int insertIndex = getInsertIndexForFront(window);
            windows.add(insertIndex, window);
        }
        focusedWindow = window;
        enforceForceFocus();
    }

    void refreshWindowOrder(@NotNull PiPWindow window) {
        if (!windows.remove(window)) {
            return;
        }
        int insertIndex = getInsertIndexForFront(window);
        windows.add(insertIndex, window);
        enforceForceFocus();
    }

    /** Adds this component's complete draw state to the active GUI extraction pass. */
    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        PiPWindow overlayTargetWindow = resolveBlockedInputOverlayWindow();
        isRendering = true;
        try {
            for (PiPWindow window : new ArrayList<>(windows)) {
                if (overlayTargetWindow == window) {
                    renderBlockedInputOverlay(graphics);
                }
                window.extractRenderState(graphics, mouseX, mouseY, partial);
            }
        } finally {
            isRendering = false;
        }
    }

    /** Returns whether window clicked this tick. */
    public boolean wasWindowClickedThisTick() {
        return windowClickedThisTick;
    }

    /** Returns last clicked window this tick. */
    @Nullable
    public PiPWindow getLastClickedWindowThisTick() {
        return lastClickedWindowThisTick;
    }

    /** Returns whether rendering. */
    public boolean isRendering() {
        return isRendering;
    }

    /** Begins the scaled render scope for a PiP window's embedded screen. */
    public void beginScreenRender(@NotNull PiPWindow window, double scaleFactor) {
        activeScreenRenderWindow = window;
        activeScreenRenderScaleFactor = scaleFactor;
    }

    /** Restores render state after a PiP window's embedded screen finishes. */
    public void endScreenRender(@NotNull PiPWindow window) {
        if (activeScreenRenderWindow == window) {
            activeScreenRenderWindow = null;
            activeScreenRenderScaleFactor = 1.0;
        }
    }

    /** Returns active screen render offset x. */
    public int getActiveScreenRenderOffsetX() {
        return activeScreenRenderWindow != null ? activeScreenRenderWindow.getBodyX() : 0;
    }

    /** Returns active screen render offset y. */
    public int getActiveScreenRenderOffsetY() {
        return activeScreenRenderWindow != null ? activeScreenRenderWindow.getBodyY() : 0;
    }

    /** Returns active screen render scale factor. */
    public double getActiveScreenRenderScaleFactor() {
        return activeScreenRenderWindow != null ? activeScreenRenderScaleFactor : 1.0;
    }

    /** Returns whether screen render active. */
    public boolean isScreenRenderActive() {
        return activeScreenRenderWindow != null;
    }

    /** Advances this object's lifecycle by one client tick. */
    @Override
    public void tick() {
        windowClickedThisTick = false;
        lastClickedWindowThisTick = null;
        if (this.blockedInputOverlayUntilMs <= System.currentTimeMillis()) {
            this.blockedInputOverlayWindow = null;
        }
        for (PiPWindow window : new ArrayList<>(windows)) {
            window.tick();
        }
    }

    /** Updates hover state for the supplied pointer position. */
    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        for (PiPWindow window : new ArrayList<>(windows)) {
            window.mouseMoved(mouseX, mouseY);
        }
    }

    /** Routes a mouse-button press and reports whether it was consumed. */
    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean isDoubleClick) {
        return this.mouseClicked(event.x(), event.y(), event.button());
    }

    /** Routes a mouse-button press and reports whether it was consumed. */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        PiPWindow forcedWindow = getTopInputBlockingWindow();
        if (forcedWindow != null) {
            if (!isWindowFocused(forcedWindow)) {
                bringToFront(forcedWindow);
            }
            focusedWindow = forcedWindow;
            if (!forcedWindow.isMouseOver(mouseX, mouseY)) {
                triggerBlockedInputOverlay(forcedWindow);
                UISounds.playDefaultBeep();
                return true;
            }
            if (forcedWindow.isInputLocked()) {
                activePointerWindow = forcedWindow;
                activePointerButton = button;
                return true;
            }
            if (forcedWindow.isMouseOver(mouseX, mouseY)) {
                forcedWindow.mouseClicked(mouseX, mouseY, button);
                if (windows.contains(forcedWindow)) {
                    if (!forcedWindow.isVisible()) {
                        this.focusTopVisibleWindowAfterClickedWindowUnavailable();
                        return true;
                    }
                    PiPWindow topBlocking = getTopInputBlockingWindow();
                    if (topBlocking != null && topBlocking != forcedWindow) {
                        activePointerWindow = forcedWindow;
                        activePointerButton = button;
                        enforceForceFocus();
                        return true;
                    }
                    bringToFront(forcedWindow);
                    activePointerWindow = forcedWindow;
                    activePointerButton = button;
                } else {
                    this.focusTopVisibleWindowAfterClickedWindowUnavailable();
                }
            }
            return true;
        }
        List<PiPWindow> snapshot = new ArrayList<>(windows);
        for (int i = snapshot.size() - 1; i >= 0; i--) {
            PiPWindow window = snapshot.get(i);
            if (!window.isVisible()) {
                continue;
            }
            if (!window.isMouseOver(mouseX, mouseY)) {
                continue;
            }
            windowClickedThisTick = true;
            lastClickedWindowThisTick = window;
            if (window.isInputLocked()) {
                bringToFront(window);
                focusedWindow = window;
                activePointerWindow = window;
                activePointerButton = button;
                enforceForceFocus();
                return true;
            }
            if (!isWindowFocused(window)) {
                bringToFront(window);
            }
            window.mouseClicked(mouseX, mouseY, button);
            if (windows.contains(window)) {
                if (!window.isVisible()) {
                    this.focusTopVisibleWindowAfterClickedWindowUnavailable();
                    return true;
                }
                PiPWindow topBlocking = getTopInputBlockingWindow();
                if (topBlocking != null && topBlocking != window) {
                    activePointerWindow = window;
                    activePointerButton = button;
                    enforceForceFocus();
                    return true;
                }
                bringToFront(window);
                focusedWindow = window;
                activePointerWindow = window;
                activePointerButton = button;
                enforceForceFocus();
            } else {
                this.focusTopVisibleWindowAfterClickedWindowUnavailable();
            }
            return true;
        }

        focusedWindow = null;
        activePointerWindow = null;
        activePointerButton = -1;
        enforceForceFocus();
        return isAnyWindowBlockingMinecraftScreenInputs();
    }

    void handleWindowVisibilityChanged(@NotNull PiPWindow window) {
        if (!window.isVisible()) {
            if (focusedWindow == window) {
                focusedWindow = getTopVisibleWindow();
            }
            if (activePointerWindow == window) {
                activePointerWindow = null;
                activePointerButton = -1;
            }
        } else if (isInputBlockingWindow(window)) {
            focusedWindow = window;
        }
        enforceForceFocus();
    }

    private void focusTopVisibleWindowAfterClickedWindowUnavailable() {
        PiPWindow topVisible = getTopVisibleWindow();
        if (topVisible != null) {
            bringToFront(topVisible);
        } else {
            focusedWindow = null;
            enforceForceFocus();
        }
        activePointerWindow = null;
        activePointerButton = -1;
    }

    /** Routes a mouse-button release and reports whether it was consumed. */
    @Override
    public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        return this.mouseReleased(event.x(), event.y(), event.button());
    }

    /** Routes a mouse-button release and reports whether it was consumed. */
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        PiPWindow forcedWindow = getTopInputBlockingWindow();
        if (forcedWindow != null && activePointerWindow != null && activePointerWindow != forcedWindow) {
            if (button == activePointerButton) {
                activePointerWindow = null;
                activePointerButton = -1;
            }
            return true;
        }
        if (forcedWindow != null && activePointerWindow == null) {
            return true;
        }
        if (activePointerWindow != null) {
            boolean handled = activePointerWindow.mouseReleased(mouseX, mouseY, button);
            if (button == activePointerButton) {
                activePointerWindow = null;
                activePointerButton = -1;
            }
            if (handled) {
                return true;
            }
        }
        return forcedWindow != null || isAnyWindowBlockingMinecraftScreenInputs();
    }

    /** Routes pointer dragging and reports whether it was consumed. */
    @Override
    public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double dragX, double dragY) {
        return this.mouseDragged(event.x(), event.y(), event.button(), dragX, dragY);
    }

    /** Routes pointer dragging and reports whether it was consumed. */
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        PiPWindow forcedWindow = getTopInputBlockingWindow();
        if (forcedWindow != null && activePointerWindow != null && activePointerWindow != forcedWindow) {
            return true;
        }
        if (forcedWindow != null && activePointerWindow == null) {
            return true;
        }
        if (activePointerWindow != null) {
            boolean handled = activePointerWindow.mouseDragged(mouseX, mouseY, button, dragX, dragY);
            if (handled) {
                return true;
            }
        }
        return forcedWindow != null || isAnyWindowBlockingMinecraftScreenInputs();
    }

    /** Routes wheel scrolling and reports whether it was consumed. */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
        PiPWindow forcedWindow = getTopInputBlockingWindow();
        if (forcedWindow != null) {
            if (forcedWindow.isInputLocked()) {
                return true;
            }
            if (forcedWindow.isMouseOver(mouseX, mouseY)) {
                forcedWindow.mouseScrolled(mouseX, mouseY, scrollDeltaX, scrollDeltaY);
            }
            return true;
        }
        List<PiPWindow> snapshot = new ArrayList<>(windows);
        for (int i = snapshot.size() - 1; i >= 0; i--) {
            PiPWindow window = snapshot.get(i);
            if (!window.isVisible() || !window.isMouseOver(mouseX, mouseY)) {
                continue;
            }
            if (window.isInputLocked()) {
                return true;
            }
            return window.mouseScrolled(mouseX, mouseY, scrollDeltaX, scrollDeltaY);
        }
        return isAnyWindowBlockingMinecraftScreenInputs();
    }

    /** Routes a key press and reports whether it was consumed. */
    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        return this.keyPressed(event.key(), event.scancode(), event.modifiers());
    }

    /** Routes a key press and reports whether it was consumed. */
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        PiPWindow window = getFocusedWindow();
        if (window == null) {
            return false;
        }
        window.keyPressed(keyCode, scanCode, modifiers);
        return true;
    }

    /** Routes a key release and reports whether it was consumed. */
    @Override
    public boolean keyReleased(net.minecraft.client.input.KeyEvent event) {
        return this.keyReleased(event.key(), event.scancode(), event.modifiers());
    }

    /** Routes a key release and reports whether it was consumed. */
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        PiPWindow window = getFocusedWindow();
        if (window == null) {
            return false;
        }
        window.keyReleased(keyCode, scanCode, modifiers);
        return true;
    }

    /** Routes typed character input and reports whether it was consumed. */
    @Override
    public boolean charTyped(net.minecraft.client.input.CharacterEvent event) {
        return this.charTyped((char)event.codepoint(), 0);
    }

    /** Routes typed character input and reports whether it was consumed. */
    public boolean charTyped(char codePoint, int modifiers) {
        PiPWindow window = getFocusedWindow();
        if (window == null) {
            return false;
        }
        window.charTyped(codePoint, modifiers);
        return true;
    }

    /** Sets focused for this pi p window handler. */
    @Override
    public void setFocused(boolean focused) {
    }

    /** Reports whether keyboard focus currently targets this control. */
    @Override
    public boolean isFocused() {
        return false;
    }

    @Nullable
    private PiPWindow getFocusedWindow() {
        if (focusedWindow != null && focusedWindow.isVisible()) {
            return focusedWindow;
        }
        focusedWindow = null;
        return null;
    }

    @Nullable
    private PiPWindow getTopVisibleWindow() {
        for (int i = windows.size() - 1; i >= 0; i--) {
            PiPWindow window = windows.get(i);
            if (window.isVisible()) {
                return window;
            }
        }
        return null;
    }

    /** Returns whether window focused. */
    public boolean isWindowFocused(@NotNull PiPWindow window) {
        PiPWindow forced = getTopInputBlockingWindow();
        if (forced != null && !forced.isInputLocked()) {
            return forced == window;
        }
        return focusedWindow == window;
    }

    /** Copies fullscreen and scaling behavior from the supplied parent window. */
    public void withFullscreenInheritanceFrom(@Nullable PiPWindow parentWindow, @NotNull Runnable action) {
        PiPWindow previousParent = this.fullscreenInheritanceParentWindow;
        this.fullscreenInheritanceParentWindow = parentWindow;
        try {
            action.run();
        } finally {
            this.fullscreenInheritanceParentWindow = previousParent;
        }
    }

    /** Returns whether any window blocking minecraft screen inputs. */
    public boolean isAnyWindowBlockingMinecraftScreenInputs() {
        if (getTopForceFocusWindow() != null) {
            return true;
        }
        for (PiPWindow window : windows) {
            if (window.isVisible() && window.isBlockingMinecraftScreenInputs()) {
                return true;
            }
        }
        return false;
    }

    /** Returns whether force focus window open. */
    public boolean isForceFocusWindowOpen() {
        return getTopForceFocusWindow() != null;
    }

    @Nullable
    private PiPWindow getTopForceFocusWindow() {
        for (int i = windows.size() - 1; i >= 0; i--) {
            PiPWindow window = windows.get(i);
            if (window.isVisible() && window.isForceFocusEnabled()) {
                return window;
            }
        }
        return null;
    }

    private boolean isInputBlockingWindow(@NotNull PiPWindow window) {
        return window.isForceFocusEnabled() || window.isBlockingMinecraftScreenInputs();
    }

    @Nullable
    private PiPWindow getTopInputBlockingWindow() {
        for (int i = windows.size() - 1; i >= 0; i--) {
            PiPWindow window = windows.get(i);
            if (window.isVisible() && isInputBlockingWindow(window)) {
                return window;
            }
        }
        return null;
    }

    private int getInsertIndexForFront(@NotNull PiPWindow window) {
        int targetLayer = getWindowLayer(window);
        for (int i = 0; i < windows.size(); i++) {
            if (getWindowLayer(windows.get(i)) > targetLayer) {
                return i;
            }
        }
        return windows.size();
    }

    private int getWindowLayer(@NotNull PiPWindow window) {
        if (window.isForceFocusEnabled() || window.isBlockingMinecraftScreenInputs()) {
            return 2;
        }
        if (window.isAlwaysOnTop()) {
            return 1;
        }
        return 0;
    }

    private void inheritTopLayerSettings(@NotNull PiPWindow window) {
        boolean shouldForceFocus = false;
        boolean shouldAlwaysOnTop = false;
        for (PiPWindow openWindow : windows) {
            if (!openWindow.isVisible()) {
                continue;
            }
            if (openWindow.isForceFocusEnabled()) {
                shouldForceFocus = true;
            }
            if (openWindow.isAlwaysOnTop()) {
                shouldAlwaysOnTop = true;
            }
            if (shouldForceFocus && shouldAlwaysOnTop) {
                break;
            }
        }
        if (shouldForceFocus) {
            window.setForceFocus(true);
        }
        if (shouldAlwaysOnTop) {
            window.setAlwaysOnTop(true);
        }
    }

    private void enforceForceFocus() {
        PiPWindow forced = getTopInputBlockingWindow();
        if (forced != null && forced.isVisible() && !forced.isInputLocked()) {
            focusedWindow = forced;
        }
    }

    private void triggerBlockedInputOverlay(@NotNull PiPWindow blockedWindow) {
        this.blockedInputOverlayUntilMs = System.currentTimeMillis() + BLOCKED_INPUT_OVERLAY_DURATION_MS;
        this.blockedInputOverlayWindow = blockedWindow;
    }

    @Nullable
    private PiPWindow resolveBlockedInputOverlayWindow() {
        if (this.blockedInputOverlayUntilMs <= System.currentTimeMillis()) {
            this.blockedInputOverlayWindow = null;
            return null;
        }
        if (this.blockedInputOverlayWindow != null
                && this.windows.contains(this.blockedInputOverlayWindow)
                && this.blockedInputOverlayWindow.isVisible()
                && isInputBlockingWindow(this.blockedInputOverlayWindow)) {
            return this.blockedInputOverlayWindow;
        }
        this.blockedInputOverlayWindow = getTopInputBlockingWindow();
        return this.blockedInputOverlayWindow;
    }

    private void renderBlockedInputOverlay(@NotNull GuiGraphicsExtractor graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        RenderingUtils.setDepthTestLocked(true);
        graphics.fill(0, 0, screenWidth, screenHeight, UIBase.getUITheme().pip_input_blocked_overlay_color.getColorInt());
        RenderingUtils.setDepthTestLocked(false);
    }

}
