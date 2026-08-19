package de.keksuccino.konkrete.platform;

import de.keksuccino.konkrete.platform.services.IPlatformCompatibilityLayer;
import de.keksuccino.konkrete.platform.services.IPlatformHelper;

import java.util.ServiceLoader;

/**
 * Provides the loader-specific services available to common code.
 */
public class Services {

    /** The active loader and registry service. */
    public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);

    /** The active loader compatibility service. */
    public static final IPlatformCompatibilityLayer COMPAT = load(IPlatformCompatibilityLayer.class);

    /**
     * Loads the first implementation registered for a service type.
     *
     * @param clazz service interface to load
     * @param <T> service type
     * @return the registered service implementation
     * @throws NullPointerException if no implementation is registered
     */
    public static <T> T load(Class<T> clazz) {
        return ServiceLoader.load(clazz).findFirst().orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
    }

}
