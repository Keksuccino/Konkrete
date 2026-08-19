package de.keksuccino.konkrete.util.rendering.ui.pipwindow;

import de.keksuccino.konkrete.util.input.InputConstants;
import de.keksuccino.konkrete.util.rendering.ui.UIBase;
import de.keksuccino.konkrete.util.rendering.ui.screen.CellScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Implements the interactive window body for pi p cell. */
public abstract class PiPCellWindowBody extends CellScreen implements PipableScreen {

    @Nullable
    private PiPWindow window;
    /** Whether Escape may close the window. */
    protected boolean allowCloseOnEsc = true;
    private int renderMouseX = 0;
    private int renderMouseY = 0;

    /** Initializes a cell-based picture-in-picture body with the supplied title. */
    public PiPCellWindowBody(Component title) {
        super(title);
    }

    /** Creates an empty pi p cell window body with default state. */
    public PiPCellWindowBody() {
        super(Component.empty());
    }

    /** Initializes resources required by this pi p cell window body. */
    @Override
    protected void init() {

        super.init();

        boolean blur = UIBase.shouldBlur();

        UIBase.applyDefaultWidgetSkinTo(this.searchBar, blur);
        UIBase.applyDefaultWidgetSkinTo(this.doneButton, blur);
        UIBase.applyDefaultWidgetSkinTo(this.cancelButton, blur);
        for (AbstractWidget widget : this.rightSideWidgets) {
            UIBase.applyDefaultWidgetSkinTo(widget, blur);
        }

        this.scrollArea.setSetupForBlurInterface(blur);
        if (this.descriptionScrollArea != null) {
            this.descriptionScrollArea.setSetupForBlurInterface(blur);
        }

    }

    /** Adds right side widget to this pi p cell window body. */
    @Override
    protected <T extends AbstractWidget> T addRightSideWidget(@NotNull T widget) {
        return UIBase.applyDefaultWidgetSkinTo(super.addRightSideWidget(widget), UIBase.shouldBlur());
    }

    /** Adds widget cell to this pi p cell window body. */
    @Override
    protected @NotNull CellScreen.WidgetCell addWidgetCell(@NotNull AbstractWidget widget, boolean applyDefaultButtonSkin) {
        WidgetCell c = super.addWidgetCell(widget, applyDefaultButtonSkin);
        if (applyDefaultButtonSkin) UIBase.applyDefaultWidgetSkinTo(widget, UIBase.shouldBlur());
        return c;
    }

    /** Adds cell to this pi p cell window body. */
    @Override
    protected <T extends RenderCell> @NotNull T addCell(@NotNull T cell) {
        if (cell instanceof TextInputCell tc) {
            UIBase.applyDefaultWidgetSkinTo(tc.editBox, UIBase.shouldBlur());
            UIBase.applyDefaultWidgetSkinTo(tc.openEditorButton, UIBase.shouldBlur());
        }
        return super.addCell(cell);
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

    /** Returns window. */
    public @Nullable PiPWindow getWindow() {
        return window;
    }

    /** Sets window for this pi p cell window body. */
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

    /** Sets allow close on esc for this pi p cell window body. */
    public PiPCellWindowBody setAllowCloseOnEsc(boolean allowCloseOnEsc) {
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

    /** Returns whether scale screen. */
    @Override
    protected void autoScaleScreen(AbstractWidget topRightSideWidget) {
        // PiP screens should not scale itself
    }

    /** Renders cell screen background into the active GUI extraction pass. */
    @Override
    public void renderCellScreenBackground(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        // PiP screens should render no background
    }

    /** Renders title into the active GUI extraction pass. */
    @Override
    protected void renderTitle(@NotNull GuiGraphicsExtractor graphics) {
        // PiP screens render no title, because it gets set as PiPWindow title instead
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
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /**
     * Cell rows precede their scroll area in the screen's child list. Minecraft only forwards a wheel event to the
     * first hovered child, so a row can prevent the scroll area from receiving it even though the row does not handle
     * scrolling. PiP cell windows need the legacy all-children routing used by {@link PiPWindowBody}.
     */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
        for (GuiEventListener listener : this.children()) {
            if (listener.mouseScrolled(mouseX, mouseY, scrollDeltaX, scrollDeltaY)) {
                return true;
            }
        }
        return false;
    }

    /** Returns whether close on esc. */
    @Override
    public final boolean shouldCloseOnEsc() {
        return false;
    }

    /** Handles screen closed for this pi p cell window body. */
    @Override
    public void onScreenClosed() {
    }

    /** Handles window closed externally for this pi p cell window body. */
    @Override
    public void onWindowClosedExternally() {
    }

    /** Handles close for this pi p cell window body. */
    @Override
    public final void onClose() {
    }

    /** Removes d from this pi p cell window body. */
    @Override
    public final void removed() {
    }

}
