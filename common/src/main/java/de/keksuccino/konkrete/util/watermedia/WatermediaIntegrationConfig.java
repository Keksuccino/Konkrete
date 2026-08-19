package de.keksuccino.konkrete.util.watermedia;

import de.keksuccino.konkrete.Konkrete;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * Configures the optional Watermedia bridge without statically linking its API.
 * Set the class loader and paths before resolving media; changing them does not migrate or release live players.
 */
public final class WatermediaIntegrationConfig {

    private static volatile ClassLoader classLoader = Konkrete.class.getClassLoader();
    private static volatile Path dataDirectory = Path.of(System.getProperty("java.io.tmpdir"), "konkrete-media").toAbsolutePath().normalize();
    private static volatile Duration resolutionTimeout = Duration.ofSeconds(30L);
    @Nullable private static volatile BooleanSupplier availabilityOverride;
    @Nullable private static volatile BooleanSupplier binariesAvailabilityOverride;

    private WatermediaIntegrationConfig() {}

    /** Returns the isolated class loader used for optional Watermedia API resolution. */
    @NotNull public static ClassLoader getClassLoader() { return classLoader; }

    /** Sets the class loader for future optional-API lookups and invalidates cached reflection handles. */
    public static void setClassLoader(@NotNull ClassLoader loader) {
        classLoader = Objects.requireNonNull(loader, "loader");
        WatermediaReflectionBridge.clearReflectionCache();
    }

    /** Returns the directory used for temporary decoded-media sources. */
    @NotNull public static Path getDataDirectory() { return dataDirectory; }

    /** Sets the directory used by subsequently created temporary decoded-media sources. */
    public static void setDataDirectory(@NotNull Path directory) { dataDirectory = Objects.requireNonNull(directory, "directory").toAbsolutePath().normalize(); }

    /** Returns the maximum time allowed for an MRL resolution operation. */
    @NotNull public static Duration getResolutionTimeout() { return resolutionTimeout; }

    /** Sets the positive maximum time allowed for an MRL resolution operation. */
    public static void setResolutionTimeout(@NotNull Duration timeout) {
        Objects.requireNonNull(timeout, "timeout");
        if (timeout.isZero() || timeout.isNegative()) throw new IllegalArgumentException("timeout must be positive");
        try {
            timeout.toNanos();
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("timeout is too large", exception);
        }
        resolutionTimeout = timeout;
    }

    /** Sets a fast, thread-safe availability override; {@code null} restores classpath detection. */
    public static void setAvailabilityOverride(@Nullable BooleanSupplier override) { availabilityOverride = override; }

    /** Sets a fast, thread-safe native-binaries override; {@code null} restores classpath detection. */
    public static void setBinariesAvailabilityOverride(@Nullable BooleanSupplier override) { binariesAvailabilityOverride = override; }

    static boolean permitsWatermedia() {
        BooleanSupplier override = availabilityOverride;
        return override == null || override.getAsBoolean();
    }

    static boolean permitsBinaries() {
        BooleanSupplier override = binariesAvailabilityOverride;
        return override == null || override.getAsBoolean();
    }
}
