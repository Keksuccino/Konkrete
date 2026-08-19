package de.keksuccino.konkrete.util.enums;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import java.util.function.Supplier;

/**
 * @param <E> The enum type.
 */
public interface LocalizedEnum<E> extends NamedEnum<E> {

    /** Default success style that remains independent of any UI theme implementation. */
    Supplier<Style> SUCCESS_TEXT_STYLE = LocalizedEnumStyles::success;
    /** Default warning style that remains independent of any UI theme implementation. */
    Supplier<Style> WARNING_TEXT_STYLE = LocalizedEnumStyles::warning;
    /** Default error style that remains independent of any UI theme implementation. */
    Supplier<Style> ERROR_TEXT_STYLE = LocalizedEnumStyles::error;

    /** Returns the translation-key prefix for this enum family. */
    @NotNull
    String getLocalizationKeyBase();

    /** Returns the translation key for this enum value. */
    @NotNull
    default String getValueLocalizationKey() {
        return this.getLocalizationKeyBase() + "." + this.getName();
    }

    /** Returns the translated and styled component for this enum value. */
    @NotNull
    default MutableComponent getValueComponent() {
        return Component.translatable(this.getValueLocalizationKey()).withStyle(this.getValueComponentStyle());
    }

    /** Returns the style applied to this enum value's component. */
    @NotNull
    default Style getValueComponentStyle() {
        return Style.EMPTY;
    }

}
