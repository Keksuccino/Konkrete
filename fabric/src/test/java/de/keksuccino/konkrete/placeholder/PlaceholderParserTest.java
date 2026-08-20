package de.keksuccino.konkrete.placeholder;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ResourceLock("PlaceholderParser global state")
@ResourceLock("PlaceholderRegistry global state")
class PlaceholderParserTest {

    private PlaceholderParser.ParserLimits originalLimits;
    private PlaceholderParser.PlaceholderCachingController originalCaching;

    @BeforeEach
    void configureParser() {
        this.originalLimits = PlaceholderParser.getLimits();
        this.originalCaching = PlaceholderParser.getPlaceholderCachingController();
        PlaceholderParser.setPlaceholderCachingController(PlaceholderParser.PlaceholderCachingController.disabled());
        PlaceholderRegistry.register("test", new TransformPlaceholder("upper", String::toUpperCase));
        PlaceholderRegistry.register("test", new TransformPlaceholder("echo", value -> value));
    }

    @AfterEach
    void restoreGlobalState() {
        PlaceholderRegistry.clear();
        PlaceholderParser.setLimits(this.originalLimits);
        PlaceholderParser.setPlaceholderCachingController(this.originalCaching);
        PlaceholderParser.clearCaches();
        Placeholder.resetExecutionConfiguration();
    }

    @Test
    void serializerRoundTripsEscapesAndNestedPlaceholders() {
        DeserializedPlaceholderString nested = DeserializedPlaceholderString.build("test:upper", Map.of("text", "line one\nline two"));
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        values.put("text", nested.toString());
        String outer = DeserializedPlaceholderString.build("test:echo", values).toString();

        assertEquals("LINE ONE\nLINE TWO", PlaceholderParser.replacePlaceholders(outer));
    }

    @Test
    void legacyRawNestedSyntaxRetainsWhitespaceInsideNestedValues() {
        String rawNested = "{\"placeholder\":\"test:echo\",\"values\":{\"text\":\"{\"placeholder\":\"test:upper\",\"values\":{\"text\":\"hello world\"}}\"}}";

        assertEquals("HELLO WORLD", PlaceholderParser.replacePlaceholders(rawNested));
    }

    @Test
    void beforeAndAfterProcessorsRunInRegistrationOrderAndCanBeRemoved() {
        long before = PlaceholderParser.addParsingProcessor(PlaceholderParser.ParsingProcessorTiming.BEFORE_REPLACING_PLACEHOLDERS, value -> value.replace("alias", DeserializedPlaceholderString.build("test:upper", Map.of("text", "hello")).toString()));
        long after = PlaceholderParser.addParsingProcessor(PlaceholderParser.ParsingProcessorTiming.AFTER_REPLACING_PLACEHOLDERS, value -> "[" + value + "]");
        try {
            assertEquals("[HELLO]", PlaceholderParser.replacePlaceholders("alias"));
        } finally {
            PlaceholderParser.removeParsingProcessor(before);
            PlaceholderParser.removeParsingProcessor(after);
        }
        assertEquals("alias", PlaceholderParser.replacePlaceholders("alias"));
    }

    @Test
    void malformedUnknownAndBoundaryInputsFailWithoutPartialReplacement() {
        String malformed = "before {\"placeholder\":\"test:upper\",\"values\":{\"text\":\"unterminated}} after";
        String unknown = "{\"placeholder\":\"unknown\"}";

        assertEquals(malformed, PlaceholderParser.replacePlaceholders(malformed));
        assertEquals(unknown, PlaceholderParser.replacePlaceholders(unknown));
        PlaceholderParser.setLimits(new PlaceholderParser.ParserLimits(32, 4));
        assertEquals("ERROR: Text too long to parse placeholders! 32 characters at max!", PlaceholderParser.replacePlaceholders("x".repeat(32)));
        assertThrows(IllegalArgumentException.class, () -> new PlaceholderParser.ParserLimits(1, 1));
        assertThrows(IllegalArgumentException.class, () -> new PlaceholderParser.ParserLimits(32, 0));
    }

    @Test
    void beforeProcessorCannotExpandTextPastConfiguredSafetyLimit() {
        PlaceholderParser.setLimits(new PlaceholderParser.ParserLimits(32, 4));
        long processor = PlaceholderParser.addParsingProcessor(PlaceholderParser.ParsingProcessorTiming.BEFORE_REPLACING_PLACEHOLDERS, ignored -> "x".repeat(32));
        try {
            assertEquals("ERROR: Text too long to parse placeholders! 32 characters at max!", PlaceholderParser.replacePlaceholders("short"));
        } finally {
            PlaceholderParser.removeParsingProcessor(processor);
        }
    }

    @Test
    void replacementsAndAfterProcessorsCannotExpandTextPastConfiguredSafetyLimit() {
        PlaceholderRegistry.register("test", new TransformPlaceholder("expand", ignored -> "x".repeat(128)));
        PlaceholderParser.setLimits(new PlaceholderParser.ParserLimits(128, 4));
        String serialized = DeserializedPlaceholderString.build("test:expand", Map.of("text", "short")).toString();
        assertEquals("ERROR: Text too long to parse placeholders! 128 characters at max!", PlaceholderParser.replacePlaceholders(serialized));

        PlaceholderRegistry.unregister("test", "expand");
        long processor = PlaceholderParser.addParsingProcessor(PlaceholderParser.ParsingProcessorTiming.AFTER_REPLACING_PLACEHOLDERS, ignored -> "x".repeat(128));
        try {
            assertEquals("ERROR: Text too long to parse placeholders! 128 characters at max!", PlaceholderParser.replacePlaceholders(DeserializedPlaceholderString.build("test:upper", Map.of("text", "short")).toString()));
        } finally {
            PlaceholderParser.removeParsingProcessor(processor);
        }
    }

    @Test
    void executionDenialsUseConfigurableNonUiListenerOncePerGuardWindow() {
        AtomicInteger denials = new AtomicInteger();
        Placeholder restricted = new TransformPlaceholder("restricted", value -> value) {
            @Override
            public boolean canRunAsync() {
                return false;
            }
        };
        PlaceholderRegistry.register("test", restricted);
        Placeholder.setExecutionPolicy(placeholder -> placeholder.canRunAsync() ? Placeholder.ExecutionDecision.allow() : Placeholder.ExecutionDecision.deny("restricted"));
        Placeholder.setExecutionFailureListener((placeholder, reason) -> denials.incrementAndGet());
        String serialized = DeserializedPlaceholderString.build("test:restricted", Map.of("text", "value")).toString();

        assertEquals(serialized, PlaceholderParser.replacePlaceholders(serialized));
        assertEquals(serialized, PlaceholderParser.replacePlaceholders(serialized));
        assertEquals(1, denials.get());
        restricted.resetExecutionFailureNotification();
        assertEquals(serialized, PlaceholderParser.replacePlaceholders(serialized));
        assertEquals(2, denials.get());
    }

    private static class TransformPlaceholder extends Placeholder {

        private final java.util.function.UnaryOperator<String> transformation;

        private TransformPlaceholder(String identifier, java.util.function.UnaryOperator<String> transformation) {
            super(identifier);
            this.transformation = transformation;
        }

        @Override
        public String getReplacementFor(DeserializedPlaceholderString placeholder) {
            return this.transformation.apply(placeholder.values.getOrDefault("text", ""));
        }

        @Override
        public List<String> getValueNames() {
            return List.of("text");
        }

        @Override
        public String getDisplayName() {
            return this.getIdentifier();
        }

        @Override
        public List<String> getDescription() {
            return List.of();
        }

        @Override
        public String getCategory() {
            return "test";
        }

        @Override
        public DeserializedPlaceholderString getDefaultPlaceholderString() {
            return DeserializedPlaceholderString.build(this.getIdentifier(), Map.of("text", "example"));
        }

    }

}
