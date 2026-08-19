package de.keksuccino.konkrete.util.rendering.ui.screen;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.function.Consumer;

/** Base screen that assembles a string from editable cells and returns it on completion. */
public abstract class StringBuilderScreen extends CellScreen {

    /** Callback receiving the assembled string. */
    protected final Consumer<String> callback;

    /** Initializes a titled string editor and its completion callback. */
    protected StringBuilderScreen(@NotNull Component title, @NotNull Consumer<String> callback) {
        super(title);
        this.callback = callback;
    }

    /** Handles cancel for this string builder screen. */
    @Override
    protected void onCancel() {
        this.callback.accept(null);
    }

    /** Handles done for this string builder screen. */
    @Override
    protected void onDone() {
        this.callback.accept(this.buildString());
    }

    /** Builds the current string from this screen's cell values. */
    @NotNull
    public abstract String buildString();

}
