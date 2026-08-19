package de.keksuccino.konkrete.util.rendering.ui.screen;

import de.keksuccino.konkrete.util.rendering.ui.pipwindow.PiPCellWindowBody;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.function.Consumer;

/** Implements the interactive window body for pi p cell string builder. */
public abstract class PiPCellStringBuilderWindowBody extends PiPCellWindowBody {

    /** Callback receiving the string assembled by this window body. */
    protected final Consumer<String> callback;

    /** Initializes a titled cell editor that returns its completed string. */
    protected PiPCellStringBuilderWindowBody(@NotNull Component title, @NotNull Consumer<String> callback) {
        super(title);
        this.callback = callback;
    }

    /** Handles cancel for this pi p cell string builder window body. */
    @Override
    protected void onCancel() {
        this.callback.accept(null);
    }

    /** Handles done for this pi p cell string builder window body. */
    @Override
    protected void onDone() {
        this.callback.accept(this.buildString());
    }

    /** Builds the current string from the window's editable cells. */
    @NotNull
    public abstract String buildString();

}
