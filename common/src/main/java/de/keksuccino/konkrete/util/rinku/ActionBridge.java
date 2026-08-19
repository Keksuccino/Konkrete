package de.keksuccino.konkrete.util.rinku;

import com.google.gson.*;
import de.keksuccino.rinku.Rinku;
import de.keksuccino.konkrete.util.threading.MainThreadTaskExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandlerAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Product-neutral JavaScript bridge shared by every wrapped Rinku browser.
 * Callers install action and placeholder adapters without exposing their own model types to Konkrete. Initialization
 * requires a ready Rinku client. Core adapters use the configured executor (Minecraft's client thread by default),
 * while custom request handlers run on CEF's query thread. Subframes are always denied. The bundled local player is
 * narrowly trusted by exact file path; every other main-frame origin is denied until its owner installs a policy.
 */
public final class ActionBridge {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final Pattern JAVASCRIPT_IDENTIFIER = Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*");
    private static final Pattern CUSTOM_REQUEST_TYPE = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_.-]+");
    private static final String REQUEST_TYPE_ACTION = "konkrete_action";
    private static final String REQUEST_TYPE_PLACEHOLDER = "konkrete_placeholder";
    @Nullable private static CefMessageRouter messageRouter;
    private static volatile String javaScriptNamespace = "konkrete";
    private static volatile String javaScriptAlias = "Konkrete";
    @Nullable private static volatile ActionHandler actionHandler;
    @Nullable private static volatile PlaceholderResolver placeholderResolver;
    @Nullable private static volatile PlaceholderResolver defaultPlaceholderResolver;
    private static volatile Executor callbackExecutor = command -> MainThreadTaskExecutor.executeInMainThread(command, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
    private static volatile BrowserRequestPolicy requestPolicy = (browser, frame, type) -> false;
    private static final Map<String, BrowserRequestHandler> CUSTOM_REQUEST_HANDLERS = new ConcurrentHashMap<>();
    private static boolean initialized;
    private static boolean shuttingDown;

    private ActionBridge() {}

    /** Installs or replaces the action adapter used for later browser requests. */
    public static void setActionHandler(@Nullable ActionHandler handler) {
        actionHandler = handler;
    }

    /** Installs a caller placeholder override; {@code null} restores Konkrete's gated registry adapter. */
    public static void setPlaceholderResolver(@Nullable PlaceholderResolver resolver) {
        placeholderResolver = resolver;
    }

    /** Keeps Konkrete's registry fallback separate so a caller override survives gated bridge initialization. */
    static void installDefaultPlaceholderResolver(@NotNull PlaceholderResolver resolver) {
        defaultPlaceholderResolver = Objects.requireNonNull(resolver, "resolver");
    }

    /** Selects the executor on which caller adapters run; the default is Minecraft's client thread. */
    public static void setCallbackExecutor(@NotNull Executor executor) {
        callbackExecutor = Objects.requireNonNull(executor, "executor");
    }

    /** Sets the policy for non-bundled main-frame origins; subframes remain denied regardless of its result. */
    public static void setRequestPolicy(@NotNull BrowserRequestPolicy policy) {
        requestPolicy = Objects.requireNonNull(policy, "policy");
    }

    /** Registers a {@code namespace:name} custom handler that runs on CEF's query thread. */
    public static void registerRequestHandler(@NotNull String requestType, @NotNull BrowserRequestHandler handler) {
        String type = Objects.requireNonNull(requestType, "requestType").trim();
        if (!CUSTOM_REQUEST_TYPE.matcher(type).matches()) throw new IllegalArgumentException("Custom request type must use lowercase namespace:name syntax: " + requestType);
        CUSTOM_REQUEST_HANDLERS.put(type, Objects.requireNonNull(handler, "handler"));
    }

    /** Removes a custom request handler if it is still mapped to the supplied instance. */
    public static void unregisterRequestHandler(@NotNull String requestType, @NotNull BrowserRequestHandler handler) {
        CUSTOM_REQUEST_HANDLERS.remove(requestType, handler);
    }

    /** Configures safe JavaScript global names used for future page injections. */
    public static void setJavaScriptNames(@NotNull String namespace, @Nullable String alias) {
        javaScriptNamespace = requireJavaScriptIdentifier(namespace, "namespace");
        javaScriptAlias = alias == null || alias.isBlank() ? "" : requireJavaScriptIdentifier(alias, "alias");
    }

    /** Initializes the global CEF message router; Rinku must already report initialized. */
    public static synchronized void initialize() {
        initializeIfNecessary();
    }

    /** Initializes the router if possible and reports whether it is ready. */
    public static synchronized boolean initializeIfNecessary() {
        if (shuttingDown) return false;
        if (initialized) return true;
        if (!RinkuUtil.isRinkuLoaded() || !Rinku.isInitialized()) {
            LOGGER.warn("[KONKRETE] Cannot initialize the Rinku JavaScript bridge before Rinku is ready");
            return false;
        }
        installDefaultPlaceholderResolver(KonkreteRinkuPlaceholderAdapter::resolve);
        RinkuShutdownIntegration.register();
        try {
            CefMessageRouter.CefMessageRouterConfig config = new CefMessageRouter.CefMessageRouterConfig();
            config.jsQueryFunction = "cefQuery";
            config.jsCancelFunction = "cefQueryCancel";
            CefMessageRouter router = CefMessageRouter.create(config);
            router.addHandler(createMessageHandler(), true);
            Rinku.getClient().getHandle().addMessageRouter(router);
            messageRouter = router;
            initialized = true;
            LOGGER.info("[KONKRETE] Rinku JavaScript bridge initialized");
            return true;
        } catch (Throwable throwable) {
            LOGGER.error("[KONKRETE] Failed to initialize the Rinku JavaScript bridge", throwable);
            return false;
        }
    }

    /** Permanently detaches and disposes the message router for this client process. */
    public static void dispose() {
        CefMessageRouter router;
        synchronized (ActionBridge.class) {
            shuttingDown = true;
            router = messageRouter;
            messageRouter = null;
            initialized = false;
            actionHandler = null;
            placeholderResolver = null;
            defaultPlaceholderResolver = null;
            CUSTOM_REQUEST_HANDLERS.clear();
        }
        if (router == null) return;
        try {
            if (Rinku.isInitialized()) Rinku.getClient().getHandle().removeMessageRouter(router);
        } catch (Throwable throwable) {
            LOGGER.error("[KONKRETE] Failed to detach the Rinku JavaScript bridge", throwable);
        }
        try {
            router.dispose();
        } catch (Throwable throwable) {
            LOGGER.error("[KONKRETE] Failed to dispose the Rinku JavaScript bridge", throwable);
        }
    }

    /** Returns the JavaScript API source using the currently configured global names. */
    @NotNull
    public static String getJavaScriptApi() {
        String alias = javaScriptAlias.isEmpty() ? "" : "window." + javaScriptAlias + " = window." + javaScriptNamespace + ";";
        return JAVASCRIPT_API_TEMPLATE.replace("%namespace%", javaScriptNamespace).replace("%alias_statement%", alias).replace("%placeholder_type%", REQUEST_TYPE_PLACEHOLDER).replace("%action_type%", REQUEST_TYPE_ACTION);
    }

    /** Creates a CEF handler that accepts Konkrete action and placeholder envelopes. */
    @NotNull
    public static CefMessageRouterHandlerAdapter createMessageHandler() {
        return new CefMessageRouterHandlerAdapter() {
            /** {@inheritDoc} */
            @Override
            public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request, boolean persistent, CefQueryCallback callback) {
                if (request == null || request.isEmpty()) {
                    callback.failure(400, "Empty request");
                    return true;
                }
                final JsonObject payload;
                try {
                    JsonElement parsed = JsonParser.parseString(request);
                    if (!parsed.isJsonObject()) {
                        callback.failure(400, "Invalid request payload");
                        return true;
                    }
                    payload = parsed.getAsJsonObject();
                } catch (JsonParseException exception) {
                    callback.failure(400, "Invalid JSON payload");
                    return true;
                }
                String type = getStringOrNull(payload, "type");
                BrowserRequestHandler customHandler = type == null ? null : CUSTOM_REQUEST_HANDLERS.get(type);
                boolean recognized = REQUEST_TYPE_ACTION.equals(type) || REQUEST_TYPE_PLACEHOLDER.equals(type) || customHandler != null;
                if (!recognized) return false;
                if (frame == null || !frame.isMain()) {
                    callback.failure(403, "Browser request rejected because only main frames may use the bridge");
                    return true;
                }
                boolean allowed = isBundledPlayerFrame(frame);
                if (!allowed) {
                    try {
                        allowed = requestPolicy.allow(browser, frame, type);
                    } catch (Throwable throwable) {
                        LOGGER.error("[KONKRETE] Browser request policy failed for '{}'", type, throwable);
                        callback.failure(500, safeMessage(throwable));
                        return true;
                    }
                }
                if (!allowed) {
                    callback.failure(403, "Browser request rejected by origin/frame policy");
                    return true;
                }
                if (REQUEST_TYPE_ACTION.equals(type)) return handleActionRequest(payload, callback);
                if (REQUEST_TYPE_PLACEHOLDER.equals(type)) return handlePlaceholderRequest(payload, callback);
                try {
                    return customHandler.handle(browser, frame, payload, callback);
                } catch (Throwable throwable) {
                    LOGGER.error("[KONKRETE] Custom browser request handler '{}' failed", type, throwable);
                    callback.failure(500, safeMessage(throwable));
                    return true;
                }
            }
        };
    }

    /** Parses an {@code action:value} request while preserving every colon in its value. */
    @NotNull
    public static BrowserAction parseBrowserAction(@NotNull String actionString) {
        Objects.requireNonNull(actionString, "actionString");
        int separator = actionString.indexOf(':');
        String type = separator < 0 ? actionString : actionString.substring(0, separator);
        String value = separator < 0 ? null : actionString.substring(separator + 1);
        if (type.isBlank()) throw new IllegalArgumentException("Action type must not be blank");
        return new BrowserAction(type, value);
    }

    private static boolean handleActionRequest(JsonObject payload, CefQueryCallback callback) {
        String actionString = getStringOrNull(payload, "action");
        if (actionString == null || actionString.isBlank()) {
            callback.failure(400, "Invalid action format");
            return true;
        }
        ActionHandler handler = actionHandler;
        if (handler == null) {
            callback.failure(503, "No action handler is registered");
            return true;
        }
        final BrowserAction action;
        try {
            action = parseBrowserAction(actionString);
        } catch (IllegalArgumentException exception) {
            callback.failure(400, exception.getMessage());
            return true;
        }
        executeCallback(() -> {
            try {
                ActionResult result = Objects.requireNonNull(handler.execute(action), "Action handler result");
                if (!result.success()) {
                    callback.failure(500, result.message());
                    return;
                }
                JsonObject response = new JsonObject();
                response.addProperty("success", true);
                response.addProperty("message", result.message());
                callback.success(GSON.toJson(response));
            } catch (Throwable throwable) {
                LOGGER.error("[KONKRETE] Browser action adapter failed for '{}'", action.type(), throwable);
                callback.failure(500, safeMessage(throwable));
            }
        }, callback);
        return true;
    }

    private static boolean handlePlaceholderRequest(JsonObject payload, CefQueryCallback callback) {
        String identifier = getStringOrNull(payload, "identifier");
        if (identifier == null || identifier.isBlank()) {
            sendPlaceholderFailure(callback, 400, "INVALID_VARIABLE", "Placeholder identifier is required");
            return true;
        }
        PlaceholderResolver resolver = placeholderResolver != null ? placeholderResolver : defaultPlaceholderResolver;
        if (resolver == null) {
            sendPlaceholderFailure(callback, 503, "NOT_FOUND", "No placeholder resolver is registered");
            return true;
        }
        final Map<String, String> variables;
        try {
            variables = parseVariables(payload.get("vars"));
        } catch (IllegalArgumentException exception) {
            sendPlaceholderFailure(callback, 400, "INVALID_VARIABLE", exception.getMessage());
            return true;
        }
        String normalizedIdentifier = identifier.trim();
        executeCallback(() -> {
            try {
                PlaceholderResult result = Objects.requireNonNull(resolver.resolve(normalizedIdentifier, variables), "Placeholder resolver result");
                if (!result.success()) {
                    sendPlaceholderFailure(callback, result.statusCode(), result.errorCode(), result.value());
                    return;
                }
                JsonObject response = new JsonObject();
                response.addProperty("success", true);
                response.addProperty("identifier", normalizedIdentifier);
                response.addProperty("value", result.value());
                callback.success(GSON.toJson(response));
            } catch (Throwable throwable) {
                LOGGER.error("[KONKRETE] Placeholder adapter failed for '{}'", normalizedIdentifier, throwable);
                sendPlaceholderFailure(callback, 500, "EVALUATION_ERROR", safeMessage(throwable));
            }
        }, callback);
        return true;
    }

    private static Map<String, String> parseVariables(@Nullable JsonElement element) {
        if (element == null || element.isJsonNull()) return Collections.emptyMap();
        if (!element.isJsonArray()) throw new IllegalArgumentException("Placeholder variables must be an array");
        LinkedHashMap<String, String> variables = new LinkedHashMap<>();
        JsonArray array = element.getAsJsonArray();
        for (JsonElement entry : array) {
            if (!entry.isJsonPrimitive() || !entry.getAsJsonPrimitive().isString()) throw new IllegalArgumentException("Placeholder variables must be strings");
            String raw = entry.getAsString();
            int separator = raw.indexOf(':');
            if (separator <= 0) throw new IllegalArgumentException("Invalid placeholder variable format: " + raw);
            String name = raw.substring(0, separator).trim();
            if (name.isEmpty()) throw new IllegalArgumentException("Placeholder variable name is empty");
            variables.put(name, raw.substring(separator + 1).trim());
        }
        return Collections.unmodifiableMap(variables);
    }

    private static void executeCallback(Runnable task, CefQueryCallback callback) {
        try {
            callbackExecutor.execute(task);
        } catch (Throwable throwable) {
            LOGGER.error("[KONKRETE] Failed to schedule a browser bridge callback", throwable);
            callback.failure(500, safeMessage(throwable));
        }
    }

    @Nullable
    private static String getStringOrNull(JsonObject payload, String key) {
        JsonElement element = payload.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsString() : null;
    }

    private static void sendPlaceholderFailure(CefQueryCallback callback, int statusCode, String code, String message) {
        JsonObject error = new JsonObject();
        error.addProperty("code", code);
        error.addProperty("message", message);
        callback.failure(statusCode, GSON.toJson(error));
    }

    private static String requireJavaScriptIdentifier(String value, String description) {
        String trimmed = Objects.requireNonNull(value, description).trim();
        if (!JAVASCRIPT_IDENTIFIER.matcher(trimmed).matches()) throw new IllegalArgumentException("Invalid JavaScript " + description + ": " + value);
        return trimmed;
    }

    private static String safeMessage(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
    }

    private static boolean isBundledPlayerFrame(@NotNull CefFrame frame) {
        try {
            URI frameUri = URI.create(frame.getURL());
            if (!"file".equalsIgnoreCase(frameUri.getScheme())) return false;
            URI pathOnlyUri = new URI(frameUri.getScheme(), frameUri.getAuthority(), frameUri.getPath(), null, null);
            Path framePath = Path.of(pathOnlyUri).toAbsolutePath().normalize();
            Path playerPath = RinkuIntegrationConfig.getDataDirectory().resolve("web").resolve("videoplayer").resolve("player.html").toAbsolutePath().normalize();
            return framePath.equals(playerPath);
        } catch (Exception ignored) {
            return false;
        }
    }

    /** Parsed browser action passed to the caller adapter. */
    public record BrowserAction(@NotNull String type, @Nullable String value) {}

    /** Result returned by a browser action adapter. */
    public record ActionResult(boolean success, @NotNull String message) {
        /** Creates a successful result. */
        public static ActionResult success(@NotNull String message) { return new ActionResult(true, message); }
        /** Creates a failed result. */
        public static ActionResult failure(@NotNull String message) { return new ActionResult(false, message); }
    }

    /** Result returned by a placeholder adapter. */
    public record PlaceholderResult(boolean success, int statusCode, @NotNull String errorCode, @NotNull String value) {
        /** Creates a successful result. */
        public static PlaceholderResult success(@Nullable String value) { return new PlaceholderResult(true, 200, "", value == null ? "" : value); }
        /** Creates a failed result. */
        public static PlaceholderResult failure(int statusCode, @NotNull String errorCode, @NotNull String message) { return new PlaceholderResult(false, statusCode, errorCode, message); }
    }

    /** Handles one parsed browser action. */
    @FunctionalInterface
    public interface ActionHandler {
        /** Executes an action and returns its browser-facing result. */
        @NotNull ActionResult execute(@NotNull BrowserAction action) throws Exception;
    }

    /** Resolves one placeholder request using immutable variables. */
    @FunctionalInterface
    public interface PlaceholderResolver {
        /** Resolves a placeholder and returns its browser-facing result. */
        @NotNull PlaceholderResult resolve(@NotNull String identifier, @NotNull Map<String, String> variables) throws Exception;
    }

    /** Decides which non-bundled main-frame origins may invoke a recognized request before caller code runs. */
    @FunctionalInterface
    public interface BrowserRequestPolicy {
        /** Returns whether this main-frame request's URL/origin is explicitly trusted. */
        boolean allow(@NotNull CefBrowser browser, @NotNull CefFrame frame, @NotNull String requestType);
    }

    /** Handles a caller-defined bridge request type. */
    @FunctionalInterface
    public interface BrowserRequestHandler {
        /** Handles a recognized payload and returns whether CEF should consider it consumed. */
        boolean handle(@NotNull CefBrowser browser, @NotNull CefFrame frame, @NotNull JsonObject payload, @NotNull CefQueryCallback callback);
    }

    private static final String JAVASCRIPT_API_TEMPLATE = """
            (function() {
                    if (typeof window.cefQuery === 'undefined') { console.error('[Konkrete] cefQuery was not registered for this browser context'); return; }
                    function query(payload) {
                        return new Promise(function(resolve, reject) {
                            window.cefQuery({ request: JSON.stringify(payload), onSuccess: function(response) {
                                try { resolve(JSON.parse(response)); } catch (ignored) { resolve(response); }
                            }, onFailure: function(status, message) {
                                var error = { code: 'INTERNAL_ERROR', message: message || 'Unknown error', details: { status: status } };
                                try { error = JSON.parse(message); error.details = { status: status }; } catch (ignored) {}
                                reject(error);
                            }});
                        });
                    }
                    function execute(type, value) {
                        var action = type;
                        if (value !== null && value !== undefined && value !== '') action += ':' + value;
                        return query({ type: '%action_type%', action: action });
                    }
                    window.%namespace% = {
                        actions: { execute: execute, executeWithCallback: function(type, value, success, failure) { execute(type, value).then(success, failure); } },
                        execute: execute,
                        executeWithCallback: function(type, value, success, failure) { execute(type, value).then(success, failure); },
                        placeholders: { get: function(identifier) { return this.getWithVars(identifier); }, getWithVars: function(identifier) {
                            var variables = Array.prototype.slice.call(arguments, 1);
                            return query({ type: '%placeholder_type%', identifier: identifier, vars: variables }).then(function(result) { return result.value; });
                        }}
                    };
                    %alias_statement%
                    window.dispatchEvent(new Event('%namespace%-ready'));
            })();
            """;
}
