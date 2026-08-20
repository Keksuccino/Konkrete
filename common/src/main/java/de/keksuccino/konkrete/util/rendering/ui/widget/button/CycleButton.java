package de.keksuccino.konkrete.util.rendering.ui.widget.button;

import de.keksuccino.konkrete.util.cycle.ILocalizedValueCycle;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;

/** Advances through an {@link ILocalizedValueCycle} and displays its selected label. */
public class CycleButton<T> extends ExtendedButton {

    /** Localized value cycle advanced by button activation. */
    protected final ILocalizedValueCycle<T> cycle;
    /** Listener receiving the newly selected cycle value after a click. */
    protected final CycleButtonClickFeedback<T> clickFeedback;

    /** Initializes a bounded button over a localized value cycle and click-feedback callback. */
    public CycleButton(int x, int y, int width, int height, @NotNull ILocalizedValueCycle<T> cycle, @NotNull CycleButtonClickFeedback<T> clickFeedback) {
        super(x, y, width, height, Component.empty(), var1 -> {});
        this.cycle = cycle;
        this.clickFeedback = clickFeedback;
        this.setPressAction(var1 -> {
            this.click();
        });
        this.setLabel(this.cycle.getCycleComponent());
    }

    /** Cycles this button to its next value and invokes its click callback. */
    public void click() {
        this.cycle.next();
        this.clickFeedback.onClick(cycle.current(), this);
    }

    /** Returns selected value. */
    @NotNull
    public T getSelectedValue() {
        return this.cycle.current();
    }

    /** Sets selected value for this cycle button. */
    public CycleButton<T> setSelectedValue(@NotNull T value) {
        return this.setSelectedValue(value, true);
    }

    /** Sets selected value for this cycle button. */
    public CycleButton<T> setSelectedValue(@NotNull T value, boolean notifyListeners) {
        this.cycle.setCurrentValue(Objects.requireNonNull(value), notifyListeners);
        return this;
    }

    /** Adds this component's content draw state to the active GUI extraction pass. */
    @Override
    protected void extractContents(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        this.setLabel(this.cycle.getCycleComponent());
        super.extractContents(graphics, mouseX, mouseY, partial);
    }

    /** Receives the value selected by a cycle button click. */
    @FunctionalInterface
    public interface CycleButtonClickFeedback<T> {

        /** Handles the selected value and originating button. */
        void onClick(T value, CycleButton<T> button);

    }

}
