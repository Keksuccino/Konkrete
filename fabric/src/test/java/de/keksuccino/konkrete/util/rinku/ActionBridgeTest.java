package de.keksuccino.konkrete.util.rinku;

import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import de.keksuccino.konkrete.placeholder.Placeholder;
import de.keksuccino.konkrete.placeholder.PlaceholderRegistry;
import de.keksuccino.konkrete.util.threading.MainThreadTaskExecutor;
import org.cef.browser.CefFrame;
import org.cef.callback.CefQueryCallback;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActionBridgeTest {

    @AfterEach
    void restoreJavaScriptNames() {
        ActionBridge.setJavaScriptNames("konkrete", "Konkrete");
        ActionBridge.setActionHandler(null);
        ActionBridge.setPlaceholderResolver(null);
        ActionBridge.installDefaultPlaceholderResolver(KonkreteRinkuPlaceholderAdapter::resolve);
        ActionBridge.setRequestPolicy((browser, frame, requestType) -> false);
        ActionBridge.setCallbackExecutor(command -> MainThreadTaskExecutor.executeInMainThread(command, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK));
        PlaceholderRegistry.unregisterNamespace("rinku_bridge_test");
    }

    @Test
    void actionParsingPreservesColonsInsideTheValue() {
        ActionBridge.BrowserAction action = ActionBridge.parseBrowserAction("open:https://example.com:8443/path");

        assertEquals("open", action.type());
        assertEquals("https://example.com:8443/path", action.value());
    }

    @Test
    void actionWithoutValueRemainsDistinctFromAnEmptyValue() {
        ActionBridge.BrowserAction absent = ActionBridge.parseBrowserAction("reload");
        ActionBridge.BrowserAction empty = ActionBridge.parseBrowserAction("reload:");

        assertNull(absent.value());
        assertEquals("", empty.value());
    }

    @Test
    void generatedApiUsesCallerSuppliedSafeGlobalNames() {
        ActionBridge.setJavaScriptNames("exampleMedia", "ExampleMedia");

        String source = ActionBridge.getJavaScriptApi();

        assertTrue(source.contains("window.exampleMedia ="));
        assertTrue(source.contains("window.ExampleMedia = window.exampleMedia"));
        assertTrue(source.contains("konkrete_action"));
        assertTrue(source.contains("konkrete_placeholder"));
        assertFalse(source.contains("setTimeout"));
    }

    @Test
    void generatedApiRejectsNamesThatCouldInjectJavaScript() {
        assertThrows(IllegalArgumentException.class, () -> ActionBridge.setJavaScriptNames("unsafe-name", null));
        assertThrows(IllegalArgumentException.class, () -> ActionBridge.setJavaScriptNames("safe", "x;alert(1)"));
    }

    @Test
    void customHandlersRequireLowercaseNamespacedTypes() {
        ActionBridge.BrowserRequestHandler handler = (browser, frame, payload, callback) -> true;

        assertThrows(IllegalArgumentException.class, () -> ActionBridge.registerRequestHandler("unnamespaced", handler));
        assertThrows(IllegalArgumentException.class, () -> ActionBridge.registerRequestHandler("Example:request", handler));
        ActionBridge.registerRequestHandler("example:request", handler);
        ActionBridge.unregisterRequestHandler("example:request", handler);
    }

    @Test
    void actionRequestsDispatchThroughTheConfiguredAdapter() {
        AtomicReference<ActionBridge.BrowserAction> received = new AtomicReference<>();
        RecordingCallback callback = new RecordingCallback();
        ActionBridge.setCallbackExecutor(Runnable::run);
        ActionBridge.setRequestPolicy((browser, frame, requestType) -> true);
        ActionBridge.setActionHandler(action -> {
            received.set(action);
            return ActionBridge.ActionResult.success("opened");
        });

        boolean handled = ActionBridge.createMessageHandler().onQuery(null, frame(true), 1L, "{\"type\":\"konkrete_action\",\"action\":\"open:https://example.com:8443\"}", false, callback);

        assertTrue(handled);
        assertEquals(new ActionBridge.BrowserAction("open", "https://example.com:8443"), received.get());
        assertTrue(callback.success.get().contains("opened"));
        assertEquals(0, callback.failureCode);
    }

    @Test
    void placeholderVariablesRemainImmutableAndPreserveValueColons() {
        AtomicReference<Map<String, String>> received = new AtomicReference<>();
        RecordingCallback callback = new RecordingCallback();
        ActionBridge.setCallbackExecutor(Runnable::run);
        ActionBridge.setRequestPolicy((browser, frame, requestType) -> true);
        ActionBridge.setPlaceholderResolver((identifier, variables) -> {
            received.set(variables);
            return ActionBridge.PlaceholderResult.success(identifier + "=" + variables.get("url"));
        });

        ActionBridge.createMessageHandler().onQuery(null, frame(true), 2L, "{\"type\":\"konkrete_placeholder\",\"identifier\":\"media\",\"vars\":[\"url:https://example.com:8443/path\"]}", false, callback);

        assertEquals("https://example.com:8443/path", received.get().get("url"));
        assertThrows(UnsupportedOperationException.class, () -> received.get().put("other", "value"));
        assertTrue(callback.success.get().contains("media=https://example.com:8443/path"));
    }

    @Test
    void defaultPolicyRejectsRemoteMainFrameBeforeDispatch() {
        RecordingCallback callback = new RecordingCallback();
        ActionBridge.setCallbackExecutor(Runnable::run);
        ActionBridge.setActionHandler(action -> ActionBridge.ActionResult.success("unexpected"));

        boolean handled = ActionBridge.createMessageHandler().onQuery(null, frame(true, "https://untrusted.example/page"), 3L, "{\"type\":\"konkrete_action\",\"action\":\"open\"}", false, callback);

        assertTrue(handled);
        assertNull(callback.success.get());
        assertEquals(403, callback.failureCode);
        assertFalse(callback.failureMessage.isBlank());
    }

    @Test
    void explicitPolicyAllowsRemoteMainFrame() {
        RecordingCallback callback = new RecordingCallback();
        ActionBridge.setCallbackExecutor(Runnable::run);
        ActionBridge.setRequestPolicy((browser, frame, requestType) -> frame.getURL().startsWith("https://trusted.example/"));
        ActionBridge.setActionHandler(action -> ActionBridge.ActionResult.success("allowed"));

        ActionBridge.createMessageHandler().onQuery(null, frame(true, "https://trusted.example/page"), 4L, "{\"type\":\"konkrete_action\",\"action\":\"open\"}", false, callback);

        assertTrue(callback.success.get().contains("allowed"));
        assertEquals(0, callback.failureCode);
    }

    @Test
    void subframeIsDeniedEvenWhenCallerPolicyAllowsIt() {
        RecordingCallback callback = new RecordingCallback();
        ActionBridge.setCallbackExecutor(Runnable::run);
        ActionBridge.setRequestPolicy((browser, frame, requestType) -> true);
        ActionBridge.setActionHandler(action -> ActionBridge.ActionResult.success("unexpected"));

        ActionBridge.createMessageHandler().onQuery(null, frame(false, "https://trusted.example/frame"), 5L, "{\"type\":\"konkrete_action\",\"action\":\"open\"}", false, callback);

        assertNull(callback.success.get());
        assertEquals(403, callback.failureCode);
    }

    @Test
    void thrownPolicyFailsClosedBeforeDispatch() {
        RecordingCallback callback = new RecordingCallback();
        AtomicReference<ActionBridge.BrowserAction> received = new AtomicReference<>();
        ActionBridge.setCallbackExecutor(Runnable::run);
        ActionBridge.setRequestPolicy((browser, frame, requestType) -> { throw new IllegalStateException("policy failed"); });
        ActionBridge.setActionHandler(action -> {
            received.set(action);
            return ActionBridge.ActionResult.success("unexpected");
        });

        ActionBridge.createMessageHandler().onQuery(null, frame(true, "https://trusted.example/page"), 6L, "{\"type\":\"konkrete_action\",\"action\":\"open\"}", false, callback);

        assertNull(received.get());
        assertNull(callback.success.get());
        assertEquals(500, callback.failureCode);
        assertTrue(callback.failureMessage.contains("policy failed"));
    }

    @Test
    void exactBundledPlayerFileIsTrustedWithoutCallerPolicy() {
        RecordingCallback callback = new RecordingCallback();
        ActionBridge.setCallbackExecutor(Runnable::run);
        ActionBridge.setActionHandler(action -> ActionBridge.ActionResult.success("local"));
        String playerUrl = RinkuIntegrationConfig.getDataDirectory().resolve("web").resolve("videoplayer").resolve("player.html").toUri() + "?volume=1";

        ActionBridge.createMessageHandler().onQuery(null, frame(true, playerUrl), 7L, "{\"type\":\"konkrete_action\",\"action\":\"open\"}", false, callback);

        assertTrue(callback.success.get().contains("local"));
        assertEquals(0, callback.failureCode);
    }

    @Test
    void defaultAdapterResolvesKonkreteRegistryPlaceholders() {
        RecordingCallback callback = new RecordingCallback();
        PlaceholderRegistry.register("rinku_bridge_test", new TestPlaceholder());
        ActionBridge.installDefaultPlaceholderResolver(KonkreteRinkuPlaceholderAdapter::resolve);
        ActionBridge.setCallbackExecutor(Runnable::run);
        ActionBridge.setRequestPolicy((browser, frame, requestType) -> true);

        ActionBridge.createMessageHandler().onQuery(null, frame(true), 8L, "{\"type\":\"konkrete_placeholder\",\"identifier\":\"rinku_bridge_test:value\",\"vars\":[\"text:hello\"]}", false, callback);

        assertTrue(callback.success.get().contains("HELLO"));
        assertEquals(0, callback.failureCode);
    }

    @Test
    void callerPlaceholderResolverOverridesTheDefaultAdapter() {
        RecordingCallback callback = new RecordingCallback();
        ActionBridge.installDefaultPlaceholderResolver((identifier, variables) -> ActionBridge.PlaceholderResult.success("default"));
        ActionBridge.setPlaceholderResolver((identifier, variables) -> ActionBridge.PlaceholderResult.success("caller"));
        ActionBridge.setCallbackExecutor(Runnable::run);
        ActionBridge.setRequestPolicy((browser, frame, requestType) -> true);

        ActionBridge.createMessageHandler().onQuery(null, frame(true), 9L, "{\"type\":\"konkrete_placeholder\",\"identifier\":\"anything\"}", false, callback);

        assertTrue(callback.success.get().contains("caller"));
    }

    @Test
    void defaultAdapterReportsMissingRequiredVariables() {
        PlaceholderRegistry.register("rinku_bridge_test", new TestPlaceholder());

        ActionBridge.PlaceholderResult result = KonkreteRinkuPlaceholderAdapter.resolve("rinku_bridge_test:value", Map.of());

        assertFalse(result.success());
        assertEquals(400, result.statusCode());
        assertEquals("MISSING_VARIABLE", result.errorCode());
    }

    private static CefFrame frame(boolean main) {
        return frame(main, "https://example.test/");
    }

    private static CefFrame frame(boolean main, String url) {
        return (CefFrame)Proxy.newProxyInstance(ActionBridgeTest.class.getClassLoader(), new Class<?>[]{CefFrame.class}, (proxy, method, args) -> {
            if (method.getName().equals("isMain")) return main;
            if (method.getName().equals("getURL")) return url;
            if (method.getReturnType() == boolean.class) return false;
            if (method.getReturnType() == int.class) return 0;
            return null;
        });
    }

    private static final class RecordingCallback implements CefQueryCallback {

        private final AtomicReference<String> success = new AtomicReference<>();
        private int failureCode;
        private String failureMessage = "";

        @Override
        public void success(String response) {
            this.success.set(response);
        }

        @Override
        public void failure(int errorCode, String errorMessage) {
            this.failureCode = errorCode;
            this.failureMessage = errorMessage;
        }

    }

    private static final class TestPlaceholder extends Placeholder {

        private TestPlaceholder() {
            super("value");
        }

        /** {@inheritDoc} */
        @Override
        public String getReplacementFor(DeserializedPlaceholderString placeholder) {
            return placeholder.values.get("text").toUpperCase();
        }

        /** {@inheritDoc} */
        @Override
        public List<String> getValueNames() {
            return List.of("text");
        }

        /** {@inheritDoc} */
        @Override
        public String getDisplayName() {
            return "Rinku bridge test";
        }

        /** {@inheritDoc} */
        @Override
        public List<String> getDescription() {
            return List.of();
        }

        /** {@inheritDoc} */
        @Override
        public String getCategory() {
            return "test";
        }

        /** {@inheritDoc} */
        @Override
        public DeserializedPlaceholderString getDefaultPlaceholderString() {
            return DeserializedPlaceholderString.build(this.getIdentifier(), Map.of("text", "value"));
        }

    }

}
