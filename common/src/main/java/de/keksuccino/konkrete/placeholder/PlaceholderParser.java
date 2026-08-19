package de.keksuccino.konkrete.placeholder;

import de.keksuccino.konkrete.util.cache.BoundedConcurrentCache;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;
import java.util.function.UnaryOperator;

/**
 * Parser for Konkrete's serialized placeholder syntax.
 *
 * <p>Placeholder values may contain serialized placeholders without JSON escaping. The parser therefore uses a
 * quote-aware balanced-brace scanner instead of a general JSON parser, evaluates nested values first, and retains
 * bounded caches for high-frequency render paths.</p>
 */
public final class PlaceholderParser {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String PLACEHOLDER_PREFIX = "{\"placeholder\":\"";
    private static final String EMPTY_STRING = "";
    private static final String TOO_LONG_TO_PARSE_ERROR_MESSAGE = "ERROR: Text too long to parse placeholders!";
    private static final long CACHE_ENTRY_OVERHEAD_WEIGHT = 64L;
    private static final LogCooldownTracker LOG_COOLDOWN = new LogCooldownTracker(10_000L, 256, 262_144L);
    private static final BoundedConcurrentCache<String, Boolean> CONTAINS_PLACEHOLDERS = new BoundedConcurrentCache<>(2_048, 1_048_576L, (text, ignored) -> CACHE_ENTRY_OVERHEAD_WEIGHT + text.length());
    private static final BoundedConcurrentCache<String, CachedPlaceholder> PLACEHOLDER_CACHE = new BoundedConcurrentCache<>(512, 4_194_304L, (text, cached) -> CACHE_ENTRY_OVERHEAD_WEIGHT + text.length() + cached.replacement().length());
    private static final Object PROCESSOR_LOCK = new Object();
    private static final Object CACHING_LOCK = new Object();
    private static final Map<Long, UnaryOperator<String>> BEFORE_PROCESSORS = new LinkedHashMap<>();
    private static final Map<Long, UnaryOperator<String>> AFTER_PROCESSORS = new LinkedHashMap<>();

    private static volatile ProcessorSnapshot processorSnapshot = new ProcessorSnapshot(0L, List.of(), List.of());
    private static volatile CachingSnapshot cachingSnapshot = new CachingSnapshot(0L, PlaceholderCachingController.enabled(30L));
    private static volatile ParserLimits limits = new ParserLimits(17_000, 128);
    private static volatile ParserErrorListener errorListener = (message, exception) -> {
        if (exception == null) LOGGER.error(message);
        else LOGGER.error(message, exception);
    };
    private static long processorId;
    private static long processorRevision;
    private static long cachingRevision;
    private static final long FORMATTING_PROCESSOR_ID;

    static {
        FORMATTING_PROCESSOR_ID = addParsingProcessor(ParsingProcessorTiming.AFTER_REPLACING_PLACEHOLDERS, PlaceholderParser::replaceLegacyFormattingCodes);
    }

    private PlaceholderParser() {
    }

    /** Adds an ordered before- or after-replacement processor and returns its lifecycle ID. */
    public static long addParsingProcessor(@NotNull ParsingProcessorTiming timing, @NotNull UnaryOperator<String> processor) {
        Objects.requireNonNull(timing, "timing");
        Objects.requireNonNull(processor, "processor");
        synchronized (PROCESSOR_LOCK) {
            long id = Math.incrementExact(processorId);
            processorId = id;
            (timing == ParsingProcessorTiming.BEFORE_REPLACING_PLACEHOLDERS ? BEFORE_PROCESSORS : AFTER_PROCESSORS).put(id, processor);
            publishProcessorSnapshotLocked();
            return id;
        }
    }

    /** Removes a parsing processor if it is registered. */
    public static void removeParsingProcessor(long id) {
        synchronized (PROCESSOR_LOCK) {
            boolean removed = BEFORE_PROCESSORS.remove(id) != null;
            removed |= AFTER_PROCESSORS.remove(id) != null;
            if (removed) publishProcessorSnapshotLocked();
        }
    }

    /** Returns the active caching policy. */
    @NotNull
    public static PlaceholderCachingController getPlaceholderCachingController() {
        return cachingSnapshot.controller();
    }

    /** Replaces the caching policy and invalidates previously cached policy revisions. */
    public static void setPlaceholderCachingController(@NotNull PlaceholderCachingController controller) {
        Objects.requireNonNull(controller, "controller");
        synchronized (CACHING_LOCK) {
            long revision = Math.incrementExact(cachingRevision);
            cachingRevision = revision;
            cachingSnapshot = new CachingSnapshot(revision, controller);
        }
    }

    /** Returns whether the current policy requests placeholder caching. */
    public static boolean isCachingPlaceholders() {
        return cachingSnapshot.controller().shouldCachePlaceholders().getAsBoolean();
    }

    /** Returns the current requested caching duration. */
    public static long getPlaceholderCachingDurationMs() {
        return cachingSnapshot.controller().cachingDurationMillis().getAsLong();
    }

    /** Replaces parser safety limits and clears results produced under the previous pass budget. */
    public static void setLimits(@NotNull ParserLimits newLimits) {
        limits = Objects.requireNonNull(newLimits, "newLimits");
        clearCaches();
    }

    /** Returns parser safety limits. */
    @NotNull
    public static ParserLimits getLimits() {
        return limits;
    }

    /** Installs a non-UI listener for throttled parser failures. */
    public static void setErrorListener(@NotNull ParserErrorListener listener) {
        errorListener = Objects.requireNonNull(listener, "listener");
    }

    /** Clears all bounded parser result caches without changing registration or policies. */
    public static void clearCaches() {
        CONTAINS_PLACEHOLDERS.clear();
        PLACEHOLDER_CACHE.clear();
    }

    /** Performs a fast, deliberately permissive check for serialized placeholder syntax. */
    public static boolean containsPlaceholders(@Nullable String input) {
        return input != null && input.length() >= PLACEHOLDER_PREFIX.length() && input.contains("{\"placeholder\"");
    }

    /** Replaces all placeholders in a string. Null input becomes an empty string. */
    @NotNull
    public static String replacePlaceholders(@Nullable String input) {
        return replacePlaceholders(input, false);
    }

    /** Replaces placeholders without running the built-in ampersand formatting-code processor. */
    @NotNull
    public static String replacePlaceholdersPreservingFormattingCodes(@Nullable String input) {
        return replacePlaceholders(input, true);
    }

    /** Finds top-level placeholders in a string, retaining the supplied per-parse replacement cache. */
    @NotNull
    public static List<ParsedPlaceholder> findPlaceholders(@Nullable String input, @NotNull HashMap<String, String> parsed) {
        Objects.requireNonNull(parsed, "parsed");
        return findPlaceholders(input, parsed, createContext(false));
    }

    @NotNull
    private static String replacePlaceholders(@Nullable String input, boolean preserveFormattingCodes) {
        if (input == null) return EMPTY_STRING;
        ParserLimits currentLimits = limits;
        if (input.length() >= currentLimits.maximumTextLength()) return tooLongResult(currentLimits);
        return replacePlaceholders(input, null, createContext(preserveFormattingCodes));
    }

    @NotNull
    private static String replacePlaceholders(@Nullable String input, @Nullable HashMap<String, String> parsed, @NotNull ParsingContext context) {
        if (input == null) return EMPTY_STRING;
        if (input.length() >= context.limits().maximumTextLength()) return tooLongResult(context.limits());

        String cacheKey = input;
        if (context.cachePlaceholders()) {
            CachedPlaceholder cached = PLACEHOLDER_CACHE.get(cacheKey);
            if (cached != null && cached.isUsableFor(context, System.currentTimeMillis())) return cached.replacement();
        }

        for (RegisteredProcessor processor : context.processors().beforeReplacement()) {
            input = Objects.requireNonNull(processor.processor().apply(input), "Before processor returned null");
            if (input.length() >= context.limits().maximumTextLength()) return tooLongResult(context.limits());
        }
        if (input.length() < PLACEHOLDER_PREFIX.length()) return input;

        Boolean contains = CONTAINS_PLACEHOLDERS.get(input);
        if (contains == null) {
            contains = containsPlaceholders(input);
            CONTAINS_PLACEHOLDERS.put(input, contains);
        }
        if (!contains) return input;

        if (parsed == null) parsed = new HashMap<>();
        for (int pass = 0; pass < context.limits().maximumReplacementPasses(); pass++) {
            String previous = input;
            List<ParsedPlaceholder> found = findPlaceholders(input, parsed, context);
            for (int i = found.size() - 1; i >= 0; i--) {
                ParsedPlaceholder placeholder = found.get(i);
                String replacement = parsed.get(placeholder.placeholderString);
                if (replacement == null) {
                    replacement = placeholder.getReplacement();
                    if (replacement == null) replacement = placeholder.placeholderString;
                    parsed.put(placeholder.placeholderString, replacement);
                }
                input = input.replace(placeholder.placeholderString, replacement);
                if (input.length() >= context.limits().maximumTextLength()) return tooLongResult(context.limits());
            }
            if (input.equals(previous)) break;
            if (pass + 1 == context.limits().maximumReplacementPasses()) logError("[KONKRETE] Placeholder replacement pass limit reached", null);
        }

        for (RegisteredProcessor processor : context.processors().afterReplacement()) {
            if (context.preserveFormattingCodes() && processor.id() == FORMATTING_PROCESSOR_ID) continue;
            input = Objects.requireNonNull(processor.processor().apply(input), "After processor returned null");
            if (input.length() >= context.limits().maximumTextLength()) return tooLongResult(context.limits());
        }

        if (context.cachePlaceholders()) PLACEHOLDER_CACHE.put(cacheKey, new CachedPlaceholder(input, System.currentTimeMillis(), context.processors().revision(), context.cachingRevision(), context.limits()));
        return input;
    }

    @NotNull
    private static String tooLongResult(@NotNull ParserLimits currentLimits) {
        return TOO_LONG_TO_PARSE_ERROR_MESSAGE + " " + currentLimits.maximumTextLength() + " characters at max!";
    }

    @NotNull
    private static List<ParsedPlaceholder> findPlaceholders(@Nullable String input, @NotNull HashMap<String, String> parsed, @NotNull ParsingContext context) {
        List<ParsedPlaceholder> placeholders = new ArrayList<>();
        if (input == null) return placeholders;
        for (int start = 0; start < input.length(); start++) {
            if (input.charAt(start) != '{') continue;
            int end = findPlaceholderEndIndex(input, start, 0, context.limits().maximumReplacementPasses());
            if (end < 0) continue;
            String candidate = input.substring(start, end + 1);
            if (normalizePlaceholderString(candidate, context.limits().maximumReplacementPasses()).startsWith(PLACEHOLDER_PREFIX)) {
                placeholders.add(new ParsedPlaceholder(candidate, start, end + 1, parsed, context));
                start = end;
            }
        }
        return placeholders;
    }

    private static int findPlaceholderEndIndex(@NotNull String input, int startIndex, int nestingDepth, int maximumNestingDepth) {
        if (nestingDepth >= maximumNestingDepth) return -1;
        int depth = 0;
        boolean escaped = false;
        boolean quoted = false;
        for (int index = startIndex + 1; index < input.length(); index++) {
            char c = input.charAt(index);
            if (!escaped && quoted && c == '{' && input.startsWith(PLACEHOLDER_PREFIX, index)) {
                int nestedEnd = findPlaceholderEndIndex(input, index, nestingDepth + 1, maximumNestingDepth);
                if (nestedEnd < 0) return -1;
                index = nestedEnd;
                continue;
            }
            if (!escaped && c == '"') quoted = !quoted;
            else if (!quoted && !escaped && c == '{') depth++;
            else if (!quoted && !escaped && c == '}') {
                if (depth == 0) return index;
                depth--;
            }
            if (escaped) escaped = false;
            else escaped = c == '\\';
        }
        return -1;
    }

    @NotNull
    private static String normalizePlaceholderString(@NotNull String placeholderString, int maximumNestingDepth) {
        StringBuilder result = new StringBuilder(placeholderString.length());
        boolean quoted = false;
        boolean escaped = false;
        for (int i = 0; i < placeholderString.length(); i++) {
            char c = placeholderString.charAt(i);
            if (i > 0 && !escaped && c == '{' && placeholderString.startsWith(PLACEHOLDER_PREFIX, i)) {
                int nestedEnd = findPlaceholderEndIndex(placeholderString, i, 0, maximumNestingDepth);
                if (nestedEnd > i) {
                    result.append(normalizePlaceholderString(placeholderString.substring(i, nestedEnd + 1), maximumNestingDepth));
                    i = nestedEnd;
                    continue;
                }
            }
            if (!escaped && i + 2 < placeholderString.length() && c == '%' && placeholderString.charAt(i + 1) == 'n' && placeholderString.charAt(i + 2) == '%') {
                if (quoted) result.append("%n%");
                i += 2;
                continue;
            }
            if (escaped) {
                result.append('\\').append(c);
                escaped = false;
                continue;
            }
            if (c == '\\') {
                escaped = true;
                continue;
            }
            if (c == '"') {
                quoted = !quoted;
                result.append(c);
                continue;
            }
            if (quoted || !Character.isWhitespace(c)) result.append(c);
        }
        if (escaped) result.append('\\');
        return result.toString();
    }

    private static boolean isEscaped(@NotNull String value, int index) {
        int slashCount = 0;
        for (int i = index - 1; i >= 0 && value.charAt(i) == '\\'; i--) slashCount++;
        return (slashCount & 1) == 1;
    }

    private static void publishProcessorSnapshotLocked() {
        long revision = Math.incrementExact(processorRevision);
        processorRevision = revision;
        processorSnapshot = new ProcessorSnapshot(revision, snapshotProcessors(BEFORE_PROCESSORS), snapshotProcessors(AFTER_PROCESSORS));
    }

    @NotNull
    private static List<RegisteredProcessor> snapshotProcessors(@NotNull Map<Long, UnaryOperator<String>> processors) {
        return processors.entrySet().stream().map(entry -> new RegisteredProcessor(entry.getKey(), entry.getValue())).toList();
    }

    @NotNull
    private static ParsingContext createContext(boolean preserveFormattingCodes) {
        ProcessorSnapshot processors = processorSnapshot;
        CachingSnapshot caching = cachingSnapshot;
        long duration = 0L;
        boolean cache = false;
        if (!preserveFormattingCodes && caching.controller().shouldCachePlaceholders().getAsBoolean()) {
            duration = caching.controller().cachingDurationMillis().getAsLong();
            cache = duration > 0L;
        }
        return new ParsingContext(processors, caching.revision(), cache, duration, preserveFormattingCodes, limits);
    }

    private static void logError(@NotNull String message, @Nullable Exception exception) {
        if (LOG_COOLDOWN.tryAcquire(message, System.currentTimeMillis())) errorListener.onError(message, exception);
    }

    @NotNull
    private static String replaceLegacyFormattingCodes(@NotNull String input) {
        StringBuilder output = null;
        for (int i = 0; i + 1 < input.length(); i++) {
            if (input.charAt(i) != '&' || "0123456789abcdefklmnorABCDEFKLMNOR".indexOf(input.charAt(i + 1)) < 0) continue;
            if (output == null) output = new StringBuilder(input);
            output.setCharAt(i, '§');
        }
        return output == null ? input : output.toString();
    }

    /** Parsed location and lazily decoded data for one placeholder occurrence. */
    public static class ParsedPlaceholder {
        /** Exact serialized substring. */
        public final String placeholderString;
        /** Inclusive source start index. */
        public final int startIndex;
        /** Exclusive source end index. */
        public final int endIndex;

        private final HashMap<String, String> parsed;
        private final ParsingContext context;
        private String normalizedString;
        private String identifier;
        private boolean identifierParsed;
        private Placeholder placeholder;
        private boolean placeholderResolved;

        /** Constructor for specialized parser integrations. */
        protected ParsedPlaceholder(@NotNull String placeholderString, int startIndex, int endIndex, @NotNull HashMap<String, String> parsed, boolean preserveFormattingCodes) {
            this(placeholderString, startIndex, endIndex, parsed, createContext(preserveFormattingCodes));
        }

        private ParsedPlaceholder(@NotNull String placeholderString, int startIndex, int endIndex, @NotNull HashMap<String, String> parsed, @NotNull ParsingContext context) {
            this.placeholderString = Objects.requireNonNull(placeholderString, "placeholderString");
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.parsed = Objects.requireNonNull(parsed, "parsed");
            this.context = Objects.requireNonNull(context, "context");
        }

        /** Returns the decoded identifier, or {@code null} for malformed syntax. */
        @Nullable
        public String getIdentifier() {
            if (this.identifierParsed) return this.identifier;
            this.identifierParsed = true;
            try {
                String normalized = this.normalized();
                if (!normalized.startsWith(PLACEHOLDER_PREFIX)) return null;
                int endQuote = normalized.indexOf('"', PLACEHOLDER_PREFIX.length());
                if (endQuote > PLACEHOLDER_PREFIX.length()) this.identifier = normalized.substring(PLACEHOLDER_PREFIX.length(), endQuote);
            } catch (RuntimeException exception) {
                logError("[KONKRETE] Failed to parse placeholder identifier: " + this.placeholderString, exception);
            }
            return this.identifier;
        }

        /** Evaluates this placeholder, recursively replacing nested values. */
        @Nullable
        public String getReplacement() {
            Placeholder resolved = this.getPlaceholder();
            if (resolved == null || !resolved.checkExecutionContext()) return this.placeholderString;
            HashMap<String, String> values = this.getValues();
            if (values == null) return this.placeholderString;
            DeserializedPlaceholderString deserialized = new DeserializedPlaceholderString(Objects.requireNonNull(this.getIdentifier()), null, this.placeholderString);
            for (Map.Entry<String, String> value : values.entrySet()) deserialized.values.put(value.getKey(), replacePlaceholders(value.getValue(), this.parsed, this.context));
            try {
                return resolved.getReplacementFor(deserialized);
            } catch (RuntimeException exception) {
                logError("[KONKRETE] Placeholder evaluation failed: " + this.placeholderString, exception);
                return this.placeholderString;
            }
        }

        /** Decodes values accepted by the registered placeholder. */
        @Nullable
        public HashMap<String, String> getValues() {
            Placeholder resolved = this.getPlaceholder();
            if (resolved == null) return null;
            List<String> names = resolved.getValueNames();
            HashMap<String, String> values = new LinkedHashMap<>();
            if (names == null || names.isEmpty()) return values;
            try {
                String normalized = this.normalized();
                int valuesStart = normalized.indexOf(",\"values\":{");
                if (valuesStart < 0) return values;
                String valueSection = normalized.substring(valuesStart + ",\"values\":{".length(), normalized.length() - 2);
                int cursor = 0;
                while (cursor < valueSection.length()) {
                    if (valueSection.charAt(cursor) != '"') return null;
                    int nameEnd = findUnescapedQuote(valueSection, cursor + 1);
                    if (nameEnd < 0 || nameEnd + 2 >= valueSection.length() || valueSection.charAt(nameEnd + 1) != ':' || valueSection.charAt(nameEnd + 2) != '"') return null;
                    String name = unescape(valueSection.substring(cursor + 1, nameEnd));
                    int contentStart = nameEnd + 3;
                    int contentEnd = findValueEnd(valueSection, contentStart);
                    if (contentEnd < 0) return null;
                    if (names.contains(name)) values.put(name, unescape(valueSection.substring(contentStart, contentEnd)));
                    cursor = contentEnd + 1;
                    if (cursor == valueSection.length()) break;
                    if (valueSection.charAt(cursor) != ',') return null;
                    cursor++;
                }
                return values;
            } catch (RuntimeException exception) {
                logError("[KONKRETE] Failed to parse placeholder values: " + this.placeholderString, exception);
                return null;
            }
        }

        /** Returns whether the registered placeholder accepts values. */
        public boolean hasValues() {
            Placeholder resolved = this.getPlaceholder();
            return resolved != null && resolved.getValueNames() != null && !resolved.getValueNames().isEmpty();
        }

        /** Resolves the placeholder from the registry. */
        @Nullable
        public Placeholder getPlaceholder() {
            if (!this.placeholderResolved) {
                this.placeholderResolved = true;
                this.placeholder = PlaceholderRegistry.getPlaceholder(this.getIdentifier());
            }
            return this.placeholder;
        }

        @NotNull
        private String normalized() {
            if (this.normalizedString == null) this.normalizedString = normalizePlaceholderString(this.placeholderString, this.context.limits().maximumReplacementPasses());
            return this.normalizedString;
        }

        private static int findUnescapedQuote(@NotNull String value, int start) {
            for (int i = start; i < value.length(); i++) if (value.charAt(i) == '"' && !isEscaped(value, i)) return i;
            return -1;
        }

        private int findValueEnd(@NotNull String value, int start) {
            for (int i = start; i < value.length(); i++) {
                char c = value.charAt(i);
                if (isEscaped(value, i)) continue;
                if (c == '{' && value.startsWith(PLACEHOLDER_PREFIX, i)) {
                    int nestedEnd = findPlaceholderEndIndex(value, i, 0, this.context.limits().maximumReplacementPasses());
                    if (nestedEnd < 0) return -1;
                    i = nestedEnd;
                    continue;
                }
                if (c == '"' && (i + 1 == value.length() || value.charAt(i + 1) == ',')) return i;
            }
            return -1;
        }

        @NotNull
        private static String unescape(@NotNull String value) {
            StringBuilder result = new StringBuilder(value.length());
            boolean escaped = false;
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                if (escaped) {
                    result.append(switch (c) {
                        case 'n' -> '\n';
                        case 'r' -> '\r';
                        case 't' -> '\t';
                        default -> c;
                    });
                    escaped = false;
                } else if (c == '\\') escaped = true;
                else if (i + 2 < value.length() && c == '%' && value.charAt(i + 1) == 'n' && value.charAt(i + 2) == '%') {
                    result.append('\n');
                    i += 2;
                } else result.append(c);
            }
            if (escaped) result.append('\\');
            return result.toString();
        }

        /** Value equality is based on exact source location and contents. */
        @Override
        public boolean equals(Object object) {
            return object == this || object instanceof ParsedPlaceholder other && this.startIndex == other.startIndex && this.endIndex == other.endIndex && this.placeholderString.equals(other.placeholderString);
        }

        /** Hash code matches source-location equality. */
        @Override
        public int hashCode() {
            return Objects.hash(this.placeholderString, this.startIndex, this.endIndex);
        }
    }

    /** Parsing processor phase. */
    public enum ParsingProcessorTiming {
        /** Runs before placeholder discovery. */
        BEFORE_REPLACING_PLACEHOLDERS,
        /** Runs after successful placeholder discovery and replacement. */
        AFTER_REPLACING_PLACEHOLDERS
    }

    /** Public policy controlling whether and how long parser results are cached. */
    public record PlaceholderCachingController(@NotNull BooleanSupplier shouldCachePlaceholders, @NotNull LongSupplier cachingDurationMillis) {
        /** Rejects null suppliers; their values are sampled once at the start of each parse. */
        public PlaceholderCachingController {
            Objects.requireNonNull(shouldCachePlaceholders, "shouldCachePlaceholders");
            Objects.requireNonNull(cachingDurationMillis, "cachingDurationMillis");
        }

        /** Creates a fixed enabled policy. */
        @NotNull public static PlaceholderCachingController enabled(long durationMillis) {
            if (durationMillis <= 0L) throw new IllegalArgumentException("durationMillis must be positive");
            return new PlaceholderCachingController(() -> true, () -> durationMillis);
        }

        /** Creates a disabled policy. */
        @NotNull public static PlaceholderCachingController disabled() {
            return new PlaceholderCachingController(() -> false, () -> 0L);
        }
    }

    /** Configurable parser resource limits. */
    public record ParserLimits(int maximumTextLength, int maximumReplacementPasses) {
        /** Rejects an unusable text ceiling or non-positive replacement-pass budget. */
        public ParserLimits {
            if (maximumTextLength <= PLACEHOLDER_PREFIX.length()) throw new IllegalArgumentException("maximumTextLength is too small");
            if (maximumReplacementPasses <= 0) throw new IllegalArgumentException("maximumReplacementPasses must be positive");
        }
    }

    /** Receives throttled parser errors without imposing a dialog or logging implementation. */
    @FunctionalInterface
    public interface ParserErrorListener {
        /** Called when a parser error acquires its log cooldown. */
        void onError(@NotNull String message, @Nullable Exception exception);
    }

    private record RegisteredProcessor(long id, @NotNull UnaryOperator<String> processor) {
    }

    private record ProcessorSnapshot(long revision, @NotNull List<RegisteredProcessor> beforeReplacement, @NotNull List<RegisteredProcessor> afterReplacement) {
        private ProcessorSnapshot {
            beforeReplacement = List.copyOf(beforeReplacement);
            afterReplacement = List.copyOf(afterReplacement);
        }
    }

    private record CachingSnapshot(long revision, @NotNull PlaceholderCachingController controller) {
    }

    private record ParsingContext(@NotNull ProcessorSnapshot processors, long cachingRevision, boolean cachePlaceholders, long cachingDurationMillis, boolean preserveFormattingCodes, @NotNull ParserLimits limits) {
    }

    private record CachedPlaceholder(@NotNull String replacement, long cachedAtMillis, long processorRevision, long cachingRevision, @NotNull ParserLimits limits) {
        private boolean isUsableFor(@NotNull ParsingContext context, long nowMillis) {
            if (this.processorRevision != context.processors().revision() || this.cachingRevision != context.cachingRevision() || !this.limits.equals(context.limits())) return false;
            if (nowMillis < this.cachedAtMillis) return false;
            long elapsed = nowMillis - this.cachedAtMillis;
            return elapsed >= 0L && elapsed < context.cachingDurationMillis();
        }
    }
}
