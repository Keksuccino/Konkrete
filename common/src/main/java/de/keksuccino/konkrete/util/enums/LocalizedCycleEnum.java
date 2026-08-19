package de.keksuccino.konkrete.util.enums;

import de.keksuccino.konkrete.util.cycle.LocalizedGenericValueCycle;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @param <E> The enum type.
 */
public interface LocalizedCycleEnum<E> extends LocalizedEnum<E> {

    /** Returns the localized component describing this cycle and its current value. */
    @NotNull
    default MutableComponent getCycleComponent() {
        return Component.translatable(this.getLocalizationKeyBase(), this.getValueComponent()).withStyle(this.getCycleComponentStyle());
    }

    /** Returns the style applied to the cycle component. */
    @NotNull
    default Style getCycleComponentStyle() {
        return Style.EMPTY;
    }

    /** Creates a localized value cycle with the supplied selection. */
    @NotNull
    default LocalizedGenericValueCycle<E> cycle(@Nullable E selected) {
        LocalizedGenericValueCycle<E> cycle = LocalizedGenericValueCycle.of(this.getLocalizationKeyBase(), this.getValues());
        cycle.setCycleComponentStyleSupplier(consumes -> this.getCycleComponentStyle());
        cycle.setValueComponentStyleSupplier(consumes -> this.getValueComponentStyle());
        cycle.setValueNameSupplier(consumes -> {
            if (consumes instanceof LocalizedCycleEnum<?> e) return I18n.get(e.getValueLocalizationKey());
            return consumes.toString();
        });
        if (selected != null) cycle.setCurrentValue(selected);
        return cycle;
    }

    /** Creates a localized value cycle selected to this enum value. */
    default LocalizedGenericValueCycle<E> cycle() {
        return cycle(this.getByNameInternal(this.getName()));
    }

}
