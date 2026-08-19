package de.keksuccino.konkrete.util.rendering.ui.screen;

import de.keksuccino.konkrete.util.rendering.ui.pipwindow.PiPCellWindowBody;
import de.keksuccino.konkrete.util.rendering.ui.pipwindow.PiPWindow;
import de.keksuccino.konkrete.util.rendering.ui.pipwindow.PiPWindowHandler;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.function.Consumer;

/** Presents a filtered list of strings and returns the selected value. */
public class StringListChooserScreen extends PiPCellWindowBody {

    /** Width in GUI units for PiP window. */
    public static final int PIP_WINDOW_WIDTH = 520;
    /** Height in GUI units for PiP window. */
    public static final int PIP_WINDOW_HEIGHT = 360;

    /** Callback receiving the selected string. */
    protected Consumer<String> callback;
    /** Candidate strings displayed in chooser order. */
    protected List<String> list;

    /** Builds a titled string chooser and installs its selection callback. */
    public StringListChooserScreen(@NotNull Component title, @NotNull List<String> stringList, @NotNull Consumer<String> callback) {
        super(title);
        this.list = stringList;
        this.callback = callback;
    }

    /** Initializes cells. */
    @Override
    protected void initCells() {

        for (String s : this.list) {
            this.addCell(new StringCell(s)).setSelectable(true);
        }

        this.addSpacerCell(20);

    }

    /** Reports whether the current state permits completion. */
    @Override
    public boolean allowDone() {
        return (this.getSelectedCell() != null);
    }

    /** Handles cancel for this string list chooser screen. */
    @Override
    protected void onCancel() {
        this.callback.accept(null);
        this.closeWindow();
    }

    /** Handles done for this string list chooser screen. */
    @Override
    protected void onDone() {
        RenderCell cell = this.getSelectedCell();
        if (cell instanceof StringCell s) {
            this.callback.accept(s.string);
        }
        this.closeWindow();
    }

    /** Handles window closed externally for this string list chooser screen. */
    @Override
    public void onWindowClosedExternally() {
        this.callback.accept(null);
    }

    /** Opens in window. */
    public static @NotNull PiPWindow openInWindow(@NotNull StringListChooserScreen screen, @Nullable PiPWindow parentWindow) {
        PiPWindow window = new PiPWindow(screen.getTitle())
                .setScreen(screen)
                .setForceKonkreteUiScale(true)
                .setAlwaysOnTop(false)
                .setForceFocus(false)
                .setBlockMinecraftScreenInputs(false)
                .setMinSize(PIP_WINDOW_WIDTH, PIP_WINDOW_HEIGHT)
                .setSize(PIP_WINDOW_WIDTH, PIP_WINDOW_HEIGHT);
        PiPWindowHandler.INSTANCE.openWindowCentered(window, parentWindow);
        return window;
    }

    /** Opens in window. */
    public static @NotNull PiPWindow openInWindow(@NotNull StringListChooserScreen screen) {
        return openInWindow(screen, null);
    }

    /** Represents one configurable string cell in a cell-based screen. */
    public class StringCell extends LabelCell {

        /** Immutable value returned when this cell is selected. */
        public String string;

        /** Creates a chooser cell for one immutable string value. */
        public StringCell(@NotNull String string) {
            super(Component.literal(string));
            this.string = string;
        }

    }

}
