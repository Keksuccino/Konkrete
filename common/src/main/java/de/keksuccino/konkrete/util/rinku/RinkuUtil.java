package de.keksuccino.konkrete.util.rinku;

import de.keksuccino.konkrete.platform.Services;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

/** Availability and lifecycle state for Konkrete's optional Rinku integration. */
public final class RinkuUtil {

    private static final String RINKU_MOD_ID = "rinku";
    /** Set by an integration owner after a terminal failure to suppress later optional-API access. */
    public static volatile boolean RINKU_CRITICAL_FAILURE = false;
    /** Set after Konkrete's Rinku handlers finish initialization for this client process. */
    public static volatile boolean RINKU_INITIALIZED = false;
    @Nullable private static volatile BooleanSupplier availabilityOverride;

    private RinkuUtil() {}

    /** Returns whether Rinku is present and enabled by the caller override. */
    public static boolean isRinkuLoaded() {
        if (RINKU_CRITICAL_FAILURE) return false;
        BooleanSupplier override = availabilityOverride;
        if (override != null && !override.getAsBoolean()) return false;
        return isRinkuPresent();
    }

    /** Sets a fast, thread-safe availability override; {@code null} restores loader detection. */
    public static void setAvailabilityOverride(@Nullable BooleanSupplier override) {
        availabilityOverride = override;
    }

    /**
     * Checks only whether Rinku's mod ID is loaded. Shutdown cleanup must ignore runtime availability overrides because they can change after Rinku resources were created.
     */
    public static boolean isRinkuPresent() {
        return isRinkuPresent(Services.PLATFORM::isModLoaded);
    }

    static boolean isRinkuPresent(Predicate<String> isModLoaded) {
        return isModLoaded.test(RINKU_MOD_ID);
    }

}
