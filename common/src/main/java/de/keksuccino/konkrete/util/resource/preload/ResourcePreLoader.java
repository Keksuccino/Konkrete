package de.keksuccino.konkrete.util.resource.preload;

import de.keksuccino.konkrete.util.resource.Resource;
import de.keksuccino.konkrete.util.resource.ResourceHandler;
import de.keksuccino.konkrete.util.resource.ResourceHandlers;
import de.keksuccino.konkrete.util.resource.ResourceRuntime;
import de.keksuccino.konkrete.util.resource.ResourceSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

/**
 * Loads and optionally waits for configured resources before a caller begins using them.
 * Custom aggregate source types such as slideshows can be installed through {@link #registerSourceType}.
 */
public final class ResourcePreLoader {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String SOURCE_SEPARATOR = "%!source_end!%";
    private static final CopyOnWriteArrayList<SourceTypeRegistration<?>> SOURCE_TYPES = new CopyOnWriteArrayList<>();

    private ResourcePreLoader() {}

    /** Preloads all sources read from the configured {@link ResourceRuntime} persistence hook. */
    public static void preLoadAll(long waitForCompletedMillis) {
        preLoad(getRegisteredResourceSources(null), waitForCompletedMillis);
    }

    /** Preloads the supplied sources, continuing after individual source failures. */
    public static void preLoad(@NotNull Iterable<? extends ResourceSource> sources, long waitForCompletedMillis) {
        if (waitForCompletedMillis < 0L) throw new IllegalArgumentException("waitForCompletedMillis must not be negative");
        LOGGER.info("[KONKRETE] Pre-loading resources..");
        for (ResourceSource source : Objects.requireNonNull(sources, "sources")) {
            if (source == null) continue;
            try {
                SourceTypeRegistration<?> registration = findRegistrationForSource(source);
                if (registration != null) {
                    registration.preLoadUnchecked(source, waitForCompletedMillis);
                } else {
                    preLoadResource(source, waitForCompletedMillis);
                }
            } catch (Exception ex) {
                LOGGER.error("[KONKRETE] Error while pre-loading resource: " + source.getSourceWithPrefix(), ex);
            }
        }
    }

    /** Registers a custom serialized source type and its caller-owned preload operation. */
    @NotNull
    public static <S extends ResourceSource> ResourceRuntime.Registration registerSourceType(@NotNull String serializationPrefix, @NotNull Class<S> sourceClass, @NotNull Function<String, S> factory, @NotNull PreLoadHandler<S> preLoadHandler) {
        String checkedPrefix = Objects.requireNonNull(serializationPrefix, "serializationPrefix");
        if (checkedPrefix.isBlank()) throw new IllegalArgumentException("serializationPrefix must not be blank");
        SourceTypeRegistration<S> registration = new SourceTypeRegistration<>(checkedPrefix, Objects.requireNonNull(sourceClass, "sourceClass"), Objects.requireNonNull(factory, "factory"), Objects.requireNonNull(preLoadHandler, "preLoadHandler"));
        SOURCE_TYPES.add(registration);
        return () -> SOURCE_TYPES.remove(registration);
    }

    /** Parses the serialized preload registry, or reads it from {@link ResourceRuntime} when {@code null}. */
    @NotNull
    public static List<ResourceSource> getRegisteredResourceSources(@Nullable String serialized) {
        String configured = serialized != null ? serialized : ResourceRuntime.readPreLoadConfiguration();
        List<ResourceSource> sources = new ArrayList<>();
        if (configured.isBlank()) return sources;
        for (String source : configured.split(SOURCE_SEPARATOR, -1)) {
            if (source.isBlank()) continue;
            try {
                sources.add(buildSourceFromString(source));
            } catch (RuntimeException ex) {
                LOGGER.error("[KONKRETE] Failed to parse a configured preload source: " + source, ex);
            }
        }
        return sources;
    }

    /** Returns whether an equivalent serialized source is already registered. */
    public static boolean isResourceSourceRegistered(@NotNull ResourceSource source, @Nullable String serialized) {
        String serializationSource = Objects.requireNonNull(source, "source").getSerializationSource();
        for (ResourceSource registered : getRegisteredResourceSources(serialized)) {
            if (registered.getSerializationSource().equals(serializationSource)) return true;
        }
        return false;
    }

    /** Adds a source to the serialized registry and optionally persists the result through {@link ResourceRuntime}. */
    @NotNull
    public static String addResourceSource(@NotNull ResourceSource source, @Nullable String serialized, boolean syncToConfig) {
        String configured = serialized != null ? serialized : ResourceRuntime.readPreLoadConfiguration();
        if (isResourceSourceRegistered(source, configured)) return configured;
        String result = configured + source.getSerializationSource() + SOURCE_SEPARATOR;
        if (syncToConfig) ResourceRuntime.writePreLoadConfiguration(result);
        return result;
    }

    /** Removes matching sources from the serialized registry and optionally persists the result. */
    @NotNull
    public static String removeResourceSource(@NotNull ResourceSource source, @Nullable String serialized, boolean syncToConfig) {
        String serializationSource = Objects.requireNonNull(source, "source").getSerializationSource();
        StringBuilder builder = new StringBuilder();
        for (ResourceSource registered : getRegisteredResourceSources(serialized)) {
            if (!registered.getSerializationSource().equals(serializationSource)) builder.append(registered.getSerializationSource()).append(SOURCE_SEPARATOR);
        }
        String result = builder.toString();
        if (syncToConfig) ResourceRuntime.writePreLoadConfiguration(result);
        return result;
    }

    /** Builds a core or caller-registered resource source from its serialized form. */
    @NotNull
    public static ResourceSource buildSourceFromString(@NotNull String serializedSource) {
        String checkedSource = Objects.requireNonNull(serializedSource, "serializedSource");
        for (SourceTypeRegistration<?> registration : SOURCE_TYPES) {
            if (checkedSource.startsWith(registration.serializationPrefix)) return registration.factory.apply(checkedSource);
        }
        return ResourceSource.of(checkedSource);
    }

    private static void preLoadResource(@NotNull ResourceSource source, long waitForCompletedMillis) throws InterruptedException {
        ResourceHandler<?, ?> handler = ResourceHandlers.findHandlerForSource(source, true);
        if (handler == null) {
            LOGGER.error("[KONKRETE] Failed to pre-load resource because no handler accepted: " + source.getSourceWithPrefix());
            return;
        }
        Resource resource = handler.get(source);
        if (resource == null) {
            LOGGER.error("[KONKRETE] Failed to pre-load resource because its handler returned null: " + source.getSourceWithPrefix());
            return;
        }
        if (waitForCompletedMillis == 0L) return;
        resource.waitForLoadingCompletedOrFailed(waitForCompletedMillis);
        if (resource.isLoadingFailed()) {
            LOGGER.error("[KONKRETE] Resource failed while pre-loading: " + source.getSourceWithPrefix());
        } else if (!resource.isLoadingCompleted()) {
            LOGGER.error("[KONKRETE] Resource pre-loading timed out: " + source.getSourceWithPrefix(), new TimeoutException("Resource loading exceeded " + waitForCompletedMillis + " ms"));
        }
    }

    @Nullable
    private static SourceTypeRegistration<?> findRegistrationForSource(@NotNull ResourceSource source) {
        for (SourceTypeRegistration<?> registration : SOURCE_TYPES) {
            if (registration.sourceClass.isInstance(source)) return registration;
        }
        return null;
    }

    /** Loads caller-specific aggregate or virtual sources for a registered aggregate or virtual resource source. */
    @FunctionalInterface
    public interface PreLoadHandler<S extends ResourceSource> {

        /** Starts loading the source and optionally waits up to the supplied timeout. */
        void preLoad(@NotNull S source, long waitForCompletedMillis) throws Exception;

    }

    private static final class SourceTypeRegistration<S extends ResourceSource> {

        private final String serializationPrefix;
        private final Class<S> sourceClass;
        private final Function<String, S> factory;
        private final PreLoadHandler<S> preLoadHandler;

        private SourceTypeRegistration(String serializationPrefix, Class<S> sourceClass, Function<String, S> factory, PreLoadHandler<S> preLoadHandler) {
            this.serializationPrefix = serializationPrefix;
            this.sourceClass = sourceClass;
            this.factory = factory;
            this.preLoadHandler = preLoadHandler;
        }

        private void preLoadUnchecked(ResourceSource source, long waitForCompletedMillis) throws Exception {
            this.preLoadHandler.preLoad(this.sourceClass.cast(source), waitForCompletedMillis);
        }

    }

}
