package de.keksuccino.konkrete.util.rendering.ui.pipwindow;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Defines rendering, lifecycle, and pointer-coordinate hooks for content hosted in a PiP window. */
public interface PipableScreen {

    /** Extracts the main PiP body into the active GUI render state. */
    default void renderBody(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
    }

    /** Extracts content that must render after the main PiP body. */
    default void renderLateBody(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
    }

    /**
     * Closes this screen's parent {@link PiPWindow} and itself.
     */
    void closeWindow();

    /**
     * The {@link PiPWindow} that is currently the parent of this screen.
     */
    @Nullable PiPWindow getWindow();

    /**
     * Gets set automatically by the parent {@link PiPWindow}. This should not get set manually.
     */
    @ApiStatus.Internal
    void setWindow(@Nullable PiPWindow window);

    /**
     * Gets called when the screen gets closed in any way, which means when it gets replaced by a new screen or when the screen's parent window gets closed.
     */
    void onScreenClosed();

    /**
     * Gets called when the parent {@link PiPWindow} gets closed by anything but the screen itself.
     */
    void onWindowClosedExternally();

    /** Returns the pointer x-coordinate transformed into this PiP body's space. */
    int getRenderMouseX();

    /** Returns the pointer y-coordinate transformed into this PiP body's space. */
    int getRenderMouseY();

}
