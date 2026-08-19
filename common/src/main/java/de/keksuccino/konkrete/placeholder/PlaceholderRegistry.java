package de.keksuccino.konkrete.placeholder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Thread-safe namespace-aware registry for placeholder lifecycle management. */
public final class PlaceholderRegistry {

    /** Namespace used by Konkrete's built-ins and by the compatibility registration overload. */
    public static final String KONKRETE_NAMESPACE = "konkrete";

    private static final Object LOCK = new Object();
    private static final LinkedHashMap<String, Registration> PENDING_REGISTRATIONS = new LinkedHashMap<>();
    private static volatile RegistrySnapshot snapshot = RegistrySnapshot.empty();

    private PlaceholderRegistry() {
    }

    /** Registers a Konkrete-owned placeholder using the same synchronous lifecycle contract as the namespace overload. */
    @NotNull
    public static Registration register(@NotNull Placeholder placeholder) {
        return register(KONKRETE_NAMESPACE, placeholder);
    }

    /**
     * Reserves an identifier, invokes {@link Placeholder#onRegistered(Registration)} on the caller thread without the
     * registry lock, and publishes only after the hook succeeds. Reserved identifiers and aliases reject competing
     * registrations but remain invisible to lookups; a hook failure releases the reservation and is rethrown.
     * Reentrant registry calls are supported, callbacks may overlap on different caller threads, and unrelated
     * reentrant changes are not rolled back when this registration fails.
     */
    @NotNull
    public static Registration register(@NotNull String namespace, @NotNull Placeholder placeholder) {
        Objects.requireNonNull(placeholder, "placeholder");
        String normalizedNamespace = normalizeNamespace(namespace);
        String localIdentifier = localIdentifier(placeholder.getIdentifier(), normalizedNamespace);
        String qualifiedIdentifier = normalizedNamespace + ":" + localIdentifier;
        Registration registration = new Registration(normalizedNamespace, localIdentifier, qualifiedIdentifier, placeholder, sanitizeAliases(placeholder.getAlternativeIdentifiers()));
        synchronized (LOCK) {
            if (snapshot.byQualifiedIdentifier().containsKey(qualifiedIdentifier) || PENDING_REGISTRATIONS.containsKey(qualifiedIdentifier)) throw new IllegalStateException("[KONKRETE] Placeholder already registered: " + qualifiedIdentifier);
            LinkedHashMap<String, Registration> reservations = new LinkedHashMap<>(snapshot.byQualifiedIdentifier());
            reservations.putAll(PENDING_REGISTRATIONS);
            reservations.put(qualifiedIdentifier, registration);
            RegistrySnapshot.create(reservations); // Validates every reserved identifier and alias before invoking external code.
            PENDING_REGISTRATIONS.put(qualifiedIdentifier, registration);
        }
        try {
            placeholder.onRegistered(registration);
        } catch (RuntimeException | Error exception) {
            synchronized (LOCK) {
                PENDING_REGISTRATIONS.remove(qualifiedIdentifier, registration);
            }
            throw exception;
        }
        synchronized (LOCK) {
            if (!PENDING_REGISTRATIONS.remove(qualifiedIdentifier, registration)) throw new IllegalStateException("Placeholder registration reservation was lost: " + qualifiedIdentifier);
            LinkedHashMap<String, Registration> registrations = new LinkedHashMap<>(snapshot.byQualifiedIdentifier());
            registrations.put(qualifiedIdentifier, registration);
            snapshot = RegistrySnapshot.create(registrations);
        }
        return registration;
    }

    /** Removes one published placeholder, then invokes its unregistration hook on the caller thread without the registry lock. Hook failures propagate after removal. */
    public static boolean unregister(@NotNull String namespace, @NotNull String identifier) {
        String normalizedNamespace = normalizeNamespace(namespace);
        String qualifiedIdentifier = normalizedNamespace + ":" + localIdentifier(identifier, normalizedNamespace);
        Registration removed;
        synchronized (LOCK) {
            removed = snapshot.byQualifiedIdentifier().get(qualifiedIdentifier);
            if (removed == null) return false;
            LinkedHashMap<String, Registration> registrations = new LinkedHashMap<>(snapshot.byQualifiedIdentifier());
            registrations.remove(qualifiedIdentifier);
            snapshot = RegistrySnapshot.create(registrations);
        }
        removed.placeholder().onUnregistered(removed);
        return true;
    }

    /** Removes a published namespace snapshot before running every hook in registration order without the registry lock; pending registrations are unaffected, and hook failures are aggregated after all hooks run. */
    public static int unregisterNamespace(@NotNull String namespace) {
        String normalizedNamespace = normalizeNamespace(namespace);
        List<Registration> removed = new ArrayList<>();
        synchronized (LOCK) {
            LinkedHashMap<String, Registration> registrations = new LinkedHashMap<>(snapshot.byQualifiedIdentifier());
            registrations.entrySet().removeIf(entry -> {
                if (!entry.getValue().namespace().equals(normalizedNamespace)) return false;
                removed.add(entry.getValue());
                return true;
            });
            if (removed.isEmpty()) return 0;
            snapshot = RegistrySnapshot.create(registrations);
        }
        notifyUnregistered(removed);
        return removed.size();
    }

    /** Returns all placeholders in deterministic registration order. */
    @NotNull
    public static List<Placeholder> getPlaceholders() {
        return snapshot.placeholders();
    }

    /** Resolves a qualified identifier, alias, or an unqualified Konkrete built-in identifier. */
    @Nullable
    public static Placeholder getPlaceholder(@Nullable String identifier) {
        if (identifier == null || identifier.isBlank()) return null;
        RegistrySnapshot current = snapshot;
        Registration registration = current.byLookupIdentifier().get(identifier);
        return registration != null ? registration.placeholder() : null;
    }

    /** Returns immutable registration metadata for an identifier. */
    @Nullable
    public static Registration getRegistration(@Nullable String identifier) {
        if (identifier == null || identifier.isBlank()) return null;
        return snapshot.byLookupIdentifier().get(identifier);
    }

    /** Returns registrations owned by a namespace. */
    @NotNull
    public static List<Registration> getRegistrations(@NotNull String namespace) {
        String normalizedNamespace = normalizeNamespace(namespace);
        return snapshot.byQualifiedIdentifier().values().stream().filter(registration -> registration.namespace().equals(normalizedNamespace)).toList();
    }

    /** Clears every published registration before running all hooks without the registry lock; pending reservations are unaffected, and hook failures are aggregated after all hooks run. */
    public static void clear() {
        List<Registration> removed;
        synchronized (LOCK) {
            removed = List.copyOf(snapshot.byQualifiedIdentifier().values());
            snapshot = RegistrySnapshot.empty();
        }
        notifyUnregistered(removed);
    }

    @NotNull
    private static String normalizeNamespace(@NotNull String namespace) {
        Objects.requireNonNull(namespace, "namespace");
        String normalized = namespace.trim().toLowerCase(java.util.Locale.ROOT);
        if (!normalized.matches("[a-z0-9_.-]+")) throw new IllegalArgumentException("Invalid placeholder namespace: " + namespace);
        return normalized;
    }

    @NotNull
    private static String localIdentifier(@NotNull String identifier, @NotNull String namespace) {
        int separator = identifier.indexOf(':');
        if (separator < 0) return validateLocalIdentifier(identifier);
        String declaredNamespace = normalizeNamespace(identifier.substring(0, separator));
        if (!declaredNamespace.equals(namespace)) throw new IllegalArgumentException("Placeholder namespace does not match owner: " + identifier);
        return validateLocalIdentifier(identifier.substring(separator + 1));
    }

    @NotNull
    private static String validateLocalIdentifier(@NotNull String identifier) {
        if (identifier.isBlank() || identifier.indexOf(':') >= 0) throw new IllegalArgumentException("Invalid local placeholder identifier: " + identifier);
        return identifier;
    }

    @NotNull
    private static List<String> sanitizeAliases(@Nullable List<String> aliases) {
        if (aliases == null || aliases.isEmpty()) return List.of();
        ArrayList<String> sanitized = new ArrayList<>(aliases.size());
        for (String alias : aliases) if (alias != null && !alias.isBlank()) sanitized.add(alias);
        return List.copyOf(sanitized);
    }

    private static void notifyUnregistered(@NotNull List<Registration> registrations) {
        RuntimeException runtimeFailure = null;
        Error errorFailure = null;
        for (Registration registration : registrations) {
            try {
                registration.placeholder().onUnregistered(registration);
            } catch (RuntimeException exception) {
                if (runtimeFailure == null) runtimeFailure = exception;
                else runtimeFailure.addSuppressed(exception);
            } catch (Error error) {
                if (errorFailure == null) errorFailure = error;
                else errorFailure.addSuppressed(error);
            }
        }
        if (errorFailure != null) {
            if (runtimeFailure != null) errorFailure.addSuppressed(runtimeFailure);
            throw errorFailure;
        }
        if (runtimeFailure != null) throw runtimeFailure;
    }

    /** Immutable ownership, aliases, and placeholder instance captured for one successful or pending registration. */
    public record Registration(@NotNull String namespace, @NotNull String localIdentifier, @NotNull String qualifiedIdentifier, @NotNull Placeholder placeholder, @NotNull List<String> aliases) {
        /** Rejects null metadata and defensively copies aliases so later placeholder mutations cannot change registry lookups. */
        public Registration {
            Objects.requireNonNull(namespace, "namespace");
            Objects.requireNonNull(localIdentifier, "localIdentifier");
            Objects.requireNonNull(qualifiedIdentifier, "qualifiedIdentifier");
            Objects.requireNonNull(placeholder, "placeholder");
            aliases = List.copyOf(Objects.requireNonNull(aliases, "aliases"));
        }
    }

    private record RegistrySnapshot(@NotNull Map<String, Registration> byQualifiedIdentifier, @NotNull Map<String, Registration> byLookupIdentifier, @NotNull List<Placeholder> placeholders) {
        @NotNull private static RegistrySnapshot empty() {
            return new RegistrySnapshot(Map.of(), Map.of(), List.of());
        }

        @NotNull private static RegistrySnapshot create(@NotNull LinkedHashMap<String, Registration> registrations) {
            LinkedHashMap<String, Registration> lookups = new LinkedHashMap<>();
            for (Registration registration : registrations.values()) {
                putUnique(lookups, registration.qualifiedIdentifier(), registration);
                if (registration.namespace().equals(KONKRETE_NAMESPACE)) putUnique(lookups, registration.localIdentifier(), registration);
                for (String alias : registration.aliases()) {
                    String qualifiedAlias;
                    int separator = alias.indexOf(':');
                    if (separator >= 0) {
                        String aliasNamespace = normalizeNamespace(alias.substring(0, separator));
                        if (!aliasNamespace.equals(registration.namespace())) throw new IllegalArgumentException("Placeholder alias namespace does not match owner: " + alias);
                        qualifiedAlias = aliasNamespace + ":" + validateLocalIdentifier(alias.substring(separator + 1));
                    } else {
                        qualifiedAlias = registration.namespace() + ":" + validateLocalIdentifier(alias);
                    }
                    putUnique(lookups, qualifiedAlias, registration);
                    if (registration.namespace().equals(KONKRETE_NAMESPACE) && separator < 0) putUnique(lookups, alias, registration);
                }
            }
            Map<String, Registration> qualifiedSnapshot = Collections.unmodifiableMap(new LinkedHashMap<>(registrations));
            Map<String, Registration> lookupSnapshot = Collections.unmodifiableMap(new LinkedHashMap<>(lookups));
            return new RegistrySnapshot(qualifiedSnapshot, lookupSnapshot, registrations.values().stream().map(Registration::placeholder).toList());
        }

        private static void putUnique(@NotNull Map<String, Registration> lookups, @NotNull String identifier, @NotNull Registration registration) {
            Registration existing = lookups.putIfAbsent(identifier, registration);
            if (existing != null && existing != registration) throw new IllegalStateException("[KONKRETE] Placeholder alias already registered: " + identifier);
        }
    }
}
