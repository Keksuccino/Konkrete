package de.keksuccino.konkrete.util.enums;

import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Supplier;

/** Configures semantic styles used by localized enums without coupling them to a UI theme system. */
public final class LocalizedEnumStyles {

    private static final Supplier<Style> DEFAULT_SUCCESS = () -> Style.EMPTY.withColor(0x55FF55);
    private static final Supplier<Style> DEFAULT_WARNING = () -> Style.EMPTY.withColor(0xFFFF55);
    private static final Supplier<Style> DEFAULT_ERROR = () -> Style.EMPTY.withColor(0xFF5555);

    private static final SemanticStyleSuppliers DEFAULTS = new SemanticStyleSuppliers(DEFAULT_SUCCESS, DEFAULT_WARNING, DEFAULT_ERROR);
    private static volatile SemanticStyleSuppliers suppliers = DEFAULTS;

    private LocalizedEnumStyles() {
    }

    /** Configures the success, warning, and error style suppliers. */
    public static void configure(@NotNull Supplier<Style> success, @NotNull Supplier<Style> warning, @NotNull Supplier<Style> error) {
        suppliers = new SemanticStyleSuppliers(Objects.requireNonNull(success, "success"), Objects.requireNonNull(warning, "warning"), Objects.requireNonNull(error, "error"));
    }

    /** Restores the default semantic colors. */
    public static void reset() {
        suppliers = DEFAULTS;
    }

    /** Returns the configured success style. */
    @NotNull
    public static Style success() {
        return Objects.requireNonNull(suppliers.success().get(), "success style");
    }

    /** Returns the configured warning style. */
    @NotNull
    public static Style warning() {
        return Objects.requireNonNull(suppliers.warning().get(), "warning style");
    }

    /** Returns the configured error style. */
    @NotNull
    public static Style error() {
        return Objects.requireNonNull(suppliers.error().get(), "error style");
    }

    private record SemanticStyleSuppliers(Supplier<Style> success, Supplier<Style> warning, Supplier<Style> error) {
    }
}
