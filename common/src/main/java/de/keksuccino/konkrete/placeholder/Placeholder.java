package de.keksuccino.konkrete.placeholder;

import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/** Base contract for serialized text placeholders. */
public abstract class Placeholder {

    private static volatile ExecutionPolicy executionPolicy = Placeholder::defaultExecutionCheck;
    private static volatile ExecutionFailureListener executionFailureListener = (placeholder, reason) -> { };

    /** Namespace-local identifier serialized in the {@code placeholder} field. */
    protected final String id;
    private final AtomicBoolean executionFailureReported = new AtomicBoolean();

    /** Creates a placeholder with a non-blank namespace-local identifier that cannot contain quote or brace syntax. */
    protected Placeholder(@NotNull String id) {
        this.id = validateIdentifier(id);
    }

    /** Returns replacement text, or {@code null} to leave the original serialized occurrence unchanged. */
    @Nullable
    public abstract String getReplacementFor(@NotNull DeserializedPlaceholderString placeholder);

    /** Returns accepted serialized value names, or {@code null}/empty for a valueless placeholder; unknown names are ignored by the parser. */
    @Nullable
    public abstract List<String> getValueNames();

    /** Returns the localized, human-readable picker name. */
    @NotNull
    public abstract String getDisplayName();

    /** Returns localized picker description lines, or {@code null} when no description is available. */
    @Nullable
    public abstract List<String> getDescription();

    /** Returns the localized picker category. */
    @NotNull
    public abstract String getCategory();

    /** Returns representative syntax whose identifier and values can be inserted by a picker. */
    @NotNull
    public abstract DeserializedPlaceholderString getDefaultPlaceholderString();

    /** Returns the local or fully qualified identifier declared by this placeholder. */
    @NotNull
    public final String getIdentifier() {
        return this.id;
    }

    /** Returns namespace-local backward-compatible aliases captured immutably when registration starts. */
    @Nullable
    public List<String> getAlternativeIdentifiers() {
        return null;
    }

    /** Returns whether evaluating this placeholder away from the Minecraft client thread is safe. */
    public boolean canRunAsync() {
        return true;
    }

    /** Checks the process-wide policy and reports only the first denial per instance; policy and listener exceptions propagate to the caller. */
    public final boolean checkExecutionContext() {
        ExecutionDecision decision = Objects.requireNonNull(executionPolicy.evaluate(this), "Execution policy returned null");
        if (!decision.allowed() && this.executionFailureReported.compareAndSet(false, true)) executionFailureListener.onDenied(this, decision.reason());
        return decision.allowed();
    }

    /** Compatibility alias for {@link #checkExecutionContext()}. */
    public final boolean checkAsync() {
        return this.checkExecutionContext();
    }

    /** Allows the next policy denial for this instance to notify the configured listener again. */
    public final void resetExecutionFailureNotification() {
        this.executionFailureReported.set(false);
    }

    /**
     * Runs synchronously on the registering thread without the registry lock while identifiers are reserved but not
     * queryable. Throwing prevents publication and releases the reservation; reentrant registry calls are supported.
     */
    protected void onRegistered(@NotNull PlaceholderRegistry.Registration registration) {
        this.onRegistered();
    }

    /** Legacy registration callback invoked by the metadata-aware hook under the same pre-publication contract. */
    public void onRegistered() {
    }

    /** Runs synchronously without the registry lock after removal is visible; throwing never restores the registration. */
    protected void onUnregistered(@NotNull PlaceholderRegistry.Registration registration) {
    }

    /** Atomically replaces the process-wide execution policy used by subsequent parser evaluations. */
    public static void setExecutionPolicy(@NotNull ExecutionPolicy policy) {
        executionPolicy = Objects.requireNonNull(policy, "policy");
    }

    /** Returns the current global execution policy. */
    @NotNull
    public static ExecutionPolicy getExecutionPolicy() {
        return executionPolicy;
    }

    /** Atomically replaces the non-UI listener notified by the first denial in each instance's guard window. */
    public static void setExecutionFailureListener(@NotNull ExecutionFailureListener listener) {
        executionFailureListener = Objects.requireNonNull(listener, "listener");
    }

    /** Restores the Minecraft-thread-aware default policy and no-op listener. */
    public static void resetExecutionConfiguration() {
        executionPolicy = Placeholder::defaultExecutionCheck;
        executionFailureListener = (placeholder, reason) -> { };
    }

    @NotNull
    private static ExecutionDecision defaultExecutionCheck(@NotNull Placeholder placeholder) {
        if (placeholder.canRunAsync()) return ExecutionDecision.allow();
        try {
            return Minecraft.getInstance().isSameThread() ? ExecutionDecision.allow() : ExecutionDecision.deny("Placeholder requires the Minecraft client thread");
        } catch (Throwable ignored) {
            return ExecutionDecision.deny("Minecraft client thread is unavailable");
        }
    }

    @NotNull
    private static String validateIdentifier(@NotNull String identifier) {
        Objects.requireNonNull(identifier, "identifier");
        if (identifier.isBlank()) throw new IllegalArgumentException("Placeholder identifier must not be blank");
        if (identifier.indexOf('"') >= 0 || identifier.indexOf('{') >= 0 || identifier.indexOf('}') >= 0) throw new IllegalArgumentException("Placeholder identifier contains reserved syntax: " + identifier);
        return identifier;
    }

    /** Decides whether a placeholder can run in the current execution context. */
    @FunctionalInterface
    public interface ExecutionPolicy {
        /** Returns a non-null allow or deny decision; thrown exceptions abort the current parse. */
        @NotNull ExecutionDecision evaluate(@NotNull Placeholder placeholder);
    }

    /** Receives policy denials without imposing a UI implementation. */
    @FunctionalInterface
    public interface ExecutionFailureListener {
        /** Called once per placeholder until its notification guard is reset; thrown exceptions abort the current parse. */
        void onDenied(@NotNull Placeholder placeholder, @NotNull String reason);
    }

    /** Immutable execution-policy result. */
    public record ExecutionDecision(boolean allowed, @NotNull String reason) {
        /** Creates an allowed decision with an empty diagnostic reason. */
        @NotNull public static ExecutionDecision allow() {
            return new ExecutionDecision(true, "");
        }

        /** Creates a denied decision carrying the listener-facing reason. */
        @NotNull public static ExecutionDecision deny(@NotNull String reason) {
            return new ExecutionDecision(false, Objects.requireNonNull(reason, "reason"));
        }
    }
}
