package de.keksuccino.konkrete.util.cycle;

import de.keksuccino.konkrete.util.ConsumingSupplier;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;

/** Cycles through ilocalized value cycle values. */
@SuppressWarnings("unused")
public interface ILocalizedValueCycle<T> extends IValueCycle<T> {

    /** Returns the translation key used for the cycle label. */
    String getCycleLocalizationKey();

    /** Returns the localized component for the cycle and current value. */
    MutableComponent getCycleComponent();

    /** Returns the localized component for the current value. */
    MutableComponent getCurrentValueComponent();

    /** Sets the style supplier used by cycle components. */
    ILocalizedValueCycle<T> setCycleComponentStyleSupplier(@NotNull ConsumingSupplier<T, Style> supplier);

}
