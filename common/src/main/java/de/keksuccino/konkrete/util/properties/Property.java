package de.keksuccino.konkrete.util.properties;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A reusable typed property with serialization codecs, normalization hooks, validation and change listeners. Instances are not thread-safe.
 *
 * @param <T> property value type
 */
public class Property<T> implements Cloneable {

    private final String key;
    private final List<ValueSetListener<T>> valueSetListeners = new ArrayList<>();
    @Nullable private T defaultValue;
    @Nullable private T currentValue;
    @Nullable private Function<String, T> deserializationCodec;
    @Nullable private Function<T, String> serializationCodec = value -> value == null ? null : value.toString();
    @Nullable private Predicate<String> userInputTextValidator;
    @Nullable private Function<T, T> valueSetProcessor;
    @Nullable private Function<T, T> valueGetProcessor;
    private String editorLabel;
    private boolean disabled;

    /**
     * Creates a generic property with explicit codecs.
     *
     * @param key serialized key
     * @param defaultValue fallback value
     * @param currentValue initial value
     * @param decoder string decoder
     * @param encoder string encoder
     * @return configured property
     * @param <T> value type
     */
    @NotNull
    public static <T> Property<T> of(@NotNull String key, @Nullable T defaultValue, @Nullable T currentValue, @NotNull Function<String, T> decoder, @NotNull Function<T, String> encoder) {
        Property<T> property = new Property<>(key, defaultValue, currentValue, key);
        property.setDeserializationCodec(decoder);
        property.setSerializationCodec(encoder);
        return property;
    }

    /**
     * Creates a string property.
     *
     * @param key serialized key
     * @param defaultValue fallback value
     * @param currentValue initial value
     * @param multiLine whether editor integrations may accept line breaks
     * @param placeholders whether editor integrations may accept placeholders
     * @param editorLabel caller-defined editor label or translation key
     * @return configured string property
     */
    @NotNull
    public static StringProperty stringProperty(@NotNull String key, @Nullable String defaultValue, @Nullable String currentValue, boolean multiLine, boolean placeholders, @NotNull String editorLabel) {
        return new StringProperty(key, defaultValue, currentValue, editorLabel, multiLine, placeholders);
    }

    /**
     * Creates a string property initialized to its default.
     *
     * @param key serialized key
     * @param defaultValue fallback and initial value
     * @param multiLine whether editor integrations may accept line breaks
     * @param placeholders whether editor integrations may accept placeholders
     * @param editorLabel caller-defined editor label or translation key
     * @return configured string property
     */
    @NotNull
    public static StringProperty stringProperty(@NotNull String key, @Nullable String defaultValue, boolean multiLine, boolean placeholders, @NotNull String editorLabel) {
        return stringProperty(key, defaultValue, defaultValue, multiLine, placeholders, editorLabel);
    }

    /**
     * Creates an integer property.
     *
     * @param key serialized key
     * @param defaultValue fallback value
     * @param currentValue initial value
     * @param editorLabel caller-defined editor label or translation key
     * @param inputBehavior optional editor behavior metadata
     * @return configured integer property
     */
    @NotNull
    public static IntegerProperty integerProperty(@NotNull String key, int defaultValue, int currentValue, @NotNull String editorLabel, @Nullable NumericInputBehavior<Integer> inputBehavior) {
        return new IntegerProperty(key, defaultValue, currentValue, editorLabel, inputBehavior);
    }

    /** Creates an integer property with free input metadata. */
    @NotNull
    public static IntegerProperty integerProperty(@NotNull String key, int defaultValue, int currentValue, @NotNull String editorLabel) {
        return integerProperty(key, defaultValue, currentValue, editorLabel, null);
    }

    /** Creates an integer property initialized to its default. */
    @NotNull
    public static IntegerProperty integerProperty(@NotNull String key, int defaultValue, @NotNull String editorLabel, @Nullable NumericInputBehavior<Integer> inputBehavior) {
        return integerProperty(key, defaultValue, defaultValue, editorLabel, inputBehavior);
    }

    /** Creates an integer property initialized to its default with free input metadata. */
    @NotNull
    public static IntegerProperty integerProperty(@NotNull String key, int defaultValue, @NotNull String editorLabel) {
        return integerProperty(key, defaultValue, defaultValue, editorLabel, null);
    }

    /** Creates a long property. */
    @NotNull
    public static LongProperty longProperty(@NotNull String key, long defaultValue, long currentValue, @NotNull String editorLabel, @Nullable NumericInputBehavior<Long> inputBehavior) {
        return new LongProperty(key, defaultValue, currentValue, editorLabel, inputBehavior);
    }

    /** Creates a long property with free input metadata. */
    @NotNull
    public static LongProperty longProperty(@NotNull String key, long defaultValue, long currentValue, @NotNull String editorLabel) {
        return longProperty(key, defaultValue, currentValue, editorLabel, null);
    }

    /** Creates a long property initialized to its default. */
    @NotNull
    public static LongProperty longProperty(@NotNull String key, long defaultValue, @NotNull String editorLabel, @Nullable NumericInputBehavior<Long> inputBehavior) {
        return longProperty(key, defaultValue, defaultValue, editorLabel, inputBehavior);
    }

    /** Creates a long property initialized to its default with free input metadata. */
    @NotNull
    public static LongProperty longProperty(@NotNull String key, long defaultValue, @NotNull String editorLabel) {
        return longProperty(key, defaultValue, defaultValue, editorLabel, null);
    }

    /** Creates a float property. */
    @NotNull
    public static FloatProperty floatProperty(@NotNull String key, float defaultValue, float currentValue, @NotNull String editorLabel, @Nullable NumericInputBehavior<Float> inputBehavior) {
        return new FloatProperty(key, defaultValue, currentValue, editorLabel, inputBehavior);
    }

    /** Creates a float property with free input metadata. */
    @NotNull
    public static FloatProperty floatProperty(@NotNull String key, float defaultValue, float currentValue, @NotNull String editorLabel) {
        return floatProperty(key, defaultValue, currentValue, editorLabel, null);
    }

    /** Creates a float property initialized to its default. */
    @NotNull
    public static FloatProperty floatProperty(@NotNull String key, float defaultValue, @NotNull String editorLabel, @Nullable NumericInputBehavior<Float> inputBehavior) {
        return floatProperty(key, defaultValue, defaultValue, editorLabel, inputBehavior);
    }

    /** Creates a float property initialized to its default with free input metadata. */
    @NotNull
    public static FloatProperty floatProperty(@NotNull String key, float defaultValue, @NotNull String editorLabel) {
        return floatProperty(key, defaultValue, defaultValue, editorLabel, null);
    }

    /** Creates a double property. */
    @NotNull
    public static DoubleProperty doubleProperty(@NotNull String key, double defaultValue, double currentValue, @NotNull String editorLabel, @Nullable NumericInputBehavior<Double> inputBehavior) {
        return new DoubleProperty(key, defaultValue, currentValue, editorLabel, inputBehavior);
    }

    /** Creates a double property with free input metadata. */
    @NotNull
    public static DoubleProperty doubleProperty(@NotNull String key, double defaultValue, double currentValue, @NotNull String editorLabel) {
        return doubleProperty(key, defaultValue, currentValue, editorLabel, null);
    }

    /** Creates a double property initialized to its default. */
    @NotNull
    public static DoubleProperty doubleProperty(@NotNull String key, double defaultValue, @NotNull String editorLabel, @Nullable NumericInputBehavior<Double> inputBehavior) {
        return doubleProperty(key, defaultValue, defaultValue, editorLabel, inputBehavior);
    }

    /** Creates a double property initialized to its default with free input metadata. */
    @NotNull
    public static DoubleProperty doubleProperty(@NotNull String key, double defaultValue, @NotNull String editorLabel) {
        return doubleProperty(key, defaultValue, defaultValue, editorLabel, null);
    }

    /** Creates a boolean property. */
    @NotNull
    public static BooleanProperty booleanProperty(@NotNull String key, boolean defaultValue, boolean currentValue, @NotNull String editorLabel) {
        return new BooleanProperty(key, defaultValue, currentValue, editorLabel);
    }

    /** Creates a boolean property initialized to its default. */
    @NotNull
    public static BooleanProperty booleanProperty(@NotNull String key, boolean defaultValue, @NotNull String editorLabel) {
        return booleanProperty(key, defaultValue, defaultValue, editorLabel);
    }

    /** Creates a hexadecimal color string property. */
    @NotNull
    public static ColorProperty hexColorProperty(@NotNull String key, @Nullable String defaultValue, @Nullable String currentValue, boolean placeholders, @NotNull String editorLabel) {
        return new ColorProperty(key, defaultValue, currentValue, editorLabel, placeholders);
    }

    /** Creates a hexadecimal color string property initialized to its default. */
    @NotNull
    public static ColorProperty hexColorProperty(@NotNull String key, @Nullable String defaultValue, boolean placeholders, @NotNull String editorLabel) {
        return hexColorProperty(key, defaultValue, defaultValue, placeholders, editorLabel);
    }

    /**
     * Creates a property where editor-specific presentation is supplied by callers rather than embedded in the property model.
     *
     * @param key serialized key
     * @param defaultValue fallback value
     * @param currentValue initial value
     * @param editorLabel caller-defined editor label or translation key
     */
    protected Property(@NotNull String key, @Nullable T defaultValue, @Nullable T currentValue, @NotNull String editorLabel) {
        this.key = Objects.requireNonNull(key, "key");
        this.defaultValue = defaultValue;
        this.currentValue = currentValue;
        this.editorLabel = Objects.requireNonNull(editorLabel, "editorLabel");
    }

    /** Creates a property initialized to its default value. */
    protected Property(@NotNull String key, @Nullable T defaultValue, @NotNull String editorLabel) {
        this(key, defaultValue, defaultValue, editorLabel);
    }

    /** Returns the serialized key. */
    @NotNull
    public String getKey() {
        return this.key;
    }

    /** Returns the fallback value. */
    @Nullable
    public T getDefault() {
        return this.defaultValue;
    }

    /** Changes the fallback value without changing the current value. */
    public Property<T> setDefault(@Nullable T value) {
        this.defaultValue = value;
        return this;
    }

    /** Returns whether current and default values are equal. */
    public boolean isDefault() {
        return Objects.equals(this.currentValue, this.defaultValue);
    }

    /** Resets the property through its normal set pipeline. */
    public Property<T> resetToDefault() {
        return this.set(this.defaultValue);
    }

    /** Returns the current value after optional get processing. */
    @Nullable
    public T get() {
        return this.processGet(this.currentValue);
    }

    /** Returns the current value or non-null default, throwing when both are null. */
    @NotNull
    public T tryGetNonNull() {
        T value = this.get();
        return value != null ? value : Objects.requireNonNull(this.processGet(this.defaultValue), "property and default are null");
    }

    /** Returns the current value, default, or supplied final fallback. */
    @NotNull
    public T tryGetNonNullElse(@NotNull T elseValue) {
        T value = this.get();
        if (value != null) return value;
        T fallback = this.processGet(this.defaultValue);
        return fallback != null ? fallback : Objects.requireNonNull(elseValue, "elseValue");
    }

    /** Applies the configured get processor for subclasses. */
    @Nullable
    protected T processGet(@Nullable T value) {
        return value == null || this.valueGetProcessor == null ? value : this.valueGetProcessor.apply(value);
    }

    /** Sets the current value through normalization and listeners. */
    public Property<T> set(@Nullable T value) {
        T processed = value == null || this.valueSetProcessor == null ? value : this.valueSetProcessor.apply(value);
        T oldValue = this.currentValue;
        this.currentValue = processed;
        this.notifyValueSetListeners(oldValue, processed);
        return this;
    }

    /** Notifies a stable listener snapshot for subclasses that expose alternate value sources. */
    protected void notifyValueSetListeners(@Nullable T oldValue, @Nullable T newValue) {
        for (ValueSetListener<T> listener : List.copyOf(this.valueSetListeners)) listener.onSet(oldValue, newValue);
    }

    /** Performs an unchecked set for reflective and erased generic call sites. */
    @SuppressWarnings("unchecked")
    public Property<T> forceSet(@Nullable Object value) {
        return this.set((T)value);
    }

    /** Returns whether editor integrations should disable this property. */
    public boolean isDisabled() {
        return this.disabled;
    }

    /** Changes whether editor integrations should disable this property. */
    public Property<T> setDisabled(boolean disabled) {
        this.disabled = disabled;
        return this;
    }

    /** Returns the optional raw-input validator. */
    @Nullable
    public Predicate<String> getUserInputTextValidator() {
        return this.userInputTextValidator;
    }

    /** Changes the optional raw-input validator. */
    public Property<T> setUserInputTextValidator(@Nullable Predicate<String> validator) {
        this.userInputTextValidator = validator;
        return this;
    }

    /** Returns the optional set normalizer. */
    @Nullable
    public Function<T, T> getValueSetProcessor() {
        return this.valueSetProcessor;
    }

    /** Changes the optional set normalizer. */
    public Property<T> setValueSetProcessor(@Nullable Function<T, T> processor) {
        this.valueSetProcessor = processor;
        return this;
    }

    /** Returns the optional read transformer. */
    @Nullable
    public Function<T, T> getValueGetProcessor() {
        return this.valueGetProcessor;
    }

    /** Changes the optional read transformer. */
    public Property<T> setValueGetProcessor(@Nullable Function<T, T> processor) {
        this.valueGetProcessor = processor;
        return this;
    }

    /** Registers a value-change listener. */
    public Property<T> addValueSetListener(@NotNull ValueSetListener<T> listener) {
        this.valueSetListeners.add(Objects.requireNonNull(listener, "listener"));
        return this;
    }

    /** Removes a value-change listener. */
    public boolean removeValueSetListener(@NotNull ValueSetListener<T> listener) {
        return this.valueSetListeners.remove(Objects.requireNonNull(listener, "listener"));
    }

    /** Returns the string decoder. */
    @Nullable
    public Function<String, T> getDeserializationCodec() {
        return this.deserializationCodec;
    }

    /** Changes the string decoder. */
    public Property<T> setDeserializationCodec(@NotNull Function<String, T> decoder) {
        this.deserializationCodec = Objects.requireNonNull(decoder, "decoder");
        return this;
    }

    /** Returns the string encoder. */
    @Nullable
    public Function<T, String> getSerializationCodec() {
        return this.serializationCodec;
    }

    /** Changes the string encoder. */
    public Property<T> setSerializationCodec(@NotNull Function<T, String> encoder) {
        this.serializationCodec = Objects.requireNonNull(encoder, "encoder");
        return this;
    }

    /** Reads this property from a container, using the default when absent. */
    public Property<T> deserialize(@NotNull PropertyContainer properties) {
        Function<String, T> decoder = Objects.requireNonNull(this.deserializationCodec, "No decoder configured for property " + this.key);
        String serialized = Objects.requireNonNull(properties, "properties").getValue(this.key);
        return this.set(serialized == null ? this.defaultValue : decoder.apply(serialized));
    }

    /** Writes this property's raw stored value into a container. */
    public Property<T> serialize(@NotNull PropertyContainer properties) {
        Function<T, String> encoder = Objects.requireNonNull(this.serializationCodec, "No encoder configured for property " + this.key);
        properties.putProperty(this.key, this.currentValue == null ? null : encoder.apply(this.currentValue));
        return this;
    }

    /** Returns caller-defined editor label metadata with no dependency on a UI framework. */
    @NotNull
    public String getContextMenuEntryLocalizationKeyBase() {
        return this.editorLabel;
    }

    /** Changes caller-defined editor label metadata. */
    public Property<T> setContextMenuEntryLocalizationKeyBase(@NotNull String editorLabel) {
        this.editorLabel = Objects.requireNonNull(editorLabel, "editorLabel");
        return this;
    }

    /** Creates a shallow property copy while preserving configuration and listener registrations. */
    @Override
    public Property<T> clone() {
        Property<T> clone = this.newCopyInstance();
        this.copyConfigurationTo(clone);
        return clone;
    }

    /** Creates the concrete instance used by {@link #clone()}. */
    protected Property<T> newCopyInstance() {
        return new Property<>(this.key, this.defaultValue, this.currentValue, this.editorLabel);
    }

    /** Copies common property configuration to a new concrete instance. */
    protected void copyConfigurationTo(@NotNull Property<T> target) {
        target.defaultValue = this.defaultValue;
        target.currentValue = this.currentValue;
        target.deserializationCodec = this.deserializationCodec;
        target.serializationCodec = this.serializationCodec;
        target.userInputTextValidator = this.userInputTextValidator;
        target.valueSetProcessor = this.valueSetProcessor;
        target.valueGetProcessor = this.valueGetProcessor;
        target.editorLabel = this.editorLabel;
        target.disabled = this.disabled;
        target.valueSetListeners.addAll(this.valueSetListeners);
    }

    /** {@inheritDoc} */
    @Override
    @NotNull
    public String toString() {
        Function<T, String> encoder = this.serializationCodec;
        if (this.currentValue == null) return "null";
        return encoder == null ? this.currentValue.toString() : Objects.requireNonNullElse(encoder.apply(this.currentValue), "null");
    }

    /** Receives the raw old and new stored values after a successful set. */
    @FunctionalInterface
    public interface ValueSetListener<T> {

        /** Called after a value is stored. */
        void onSet(@Nullable T oldValue, @Nullable T newValue);

    }

    /**
     * UI-neutral metadata describing how a numeric value may be edited.
     *
     * @param <N> numeric type
     */
    public static final class NumericInputBehavior<N extends Number> {

        /** Available editing modes. */
        public enum Mode {

            FREE_INPUT,
            RANGE_INPUT,
            CYCLE_INPUT

        }

        private final Mode mode;
        @Nullable private final N minValue;
        @Nullable private final N maxValue;
        @Nullable private final List<N> cycleValues;

        private NumericInputBehavior(Mode mode, @Nullable N minValue, @Nullable N maxValue, @Nullable List<N> cycleValues) {
            this.mode = mode;
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.cycleValues = cycleValues == null ? null : List.copyOf(cycleValues);
        }

        /** Returns the editing mode. */
        @NotNull
        public Mode getMode() {
            return this.mode;
        }

        /** Returns the inclusive range minimum when configured. */
        @Nullable
        public N getMinValue() {
            return this.minValue;
        }

        /** Returns the inclusive range maximum when configured. */
        @Nullable
        public N getMaxValue() {
            return this.maxValue;
        }

        /** Returns immutable cycle values when configured. */
        @Nullable
        public List<N> getCycleValues() {
            return this.cycleValues;
        }

        /** Creates an editing-metadata builder. */
        @NotNull
        public static <N extends Number> Builder<N> builder() {
            return new Builder<>();
        }

        /** Builds validated numeric editing metadata. */
        public static final class Builder<N extends Number> {

            private Mode mode = Mode.FREE_INPUT;
            @Nullable private N minValue;
            @Nullable private N maxValue;
            @Nullable private List<N> cycleValues;

            private Builder() {}

            /** Selects unrestricted input. */
            @NotNull
            public Builder<N> freeInput() {
                this.mode = Mode.FREE_INPUT;
                this.minValue = null;
                this.maxValue = null;
                this.cycleValues = null;
                return this;
            }

            /** Selects inclusive range input. */
            @NotNull
            public Builder<N> rangeInput(@NotNull N minValue, @NotNull N maxValue) {
                this.mode = Mode.RANGE_INPUT;
                this.minValue = Objects.requireNonNull(minValue, "minValue");
                this.maxValue = Objects.requireNonNull(maxValue, "maxValue");
                this.cycleValues = null;
                return this;
            }

            /** Selects a non-empty fixed-value cycle. */
            @NotNull
            public Builder<N> cycleInput(@NotNull List<N> cycleValues) {
                List<N> checkedValues = List.copyOf(Objects.requireNonNull(cycleValues, "cycleValues"));
                if (checkedValues.isEmpty()) throw new IllegalArgumentException("cycleValues must not be empty");
                this.mode = Mode.CYCLE_INPUT;
                this.cycleValues = checkedValues;
                this.minValue = null;
                this.maxValue = null;
                return this;
            }

            /** Creates immutable editing metadata. */
            @NotNull
            public NumericInputBehavior<N> build() {
                return new NumericInputBehavior<>(this.mode, this.minValue, this.maxValue, this.cycleValues);
            }

        }

    }

    /** String property carrying UI-neutral editor capabilities. */
    public static class StringProperty extends Property<String> {

        private final boolean multiLine;
        private final boolean placeholders;

        /** Creates a string property. */
        protected StringProperty(String key, @Nullable String defaultValue, @Nullable String currentValue, String editorLabel, boolean multiLine, boolean placeholders) {
            super(key, defaultValue, currentValue, editorLabel);
            this.multiLine = multiLine;
            this.placeholders = placeholders;
            this.setDeserializationCodec(Function.identity());
        }

        /** Returns the resolved value, or an empty string when both current and default are null. */
        @NotNull
        public String getString() {
            return this.tryGetNonNullElse("");
        }

        /** Returns whether editor integrations may accept line breaks. */
        public boolean isMultiLine() {
            return this.multiLine;
        }

        /** Returns whether editor integrations may accept placeholder syntax. */
        public boolean allowsPlaceholders() {
            return this.placeholders;
        }

        /** {@inheritDoc} */
        @Override
        protected Property<String> newCopyInstance() {
            return new StringProperty(this.getKey(), this.getDefault(), this.get(), this.getContextMenuEntryLocalizationKeyBase(), this.multiLine, this.placeholders);
        }

    }

    /** Hexadecimal color property with UI-neutral validation. */
    public static class ColorProperty extends StringProperty {

        /** Creates a color property. */
        protected ColorProperty(String key, @Nullable String defaultValue, @Nullable String currentValue, String editorLabel, boolean placeholders) {
            super(key, defaultValue, currentValue, editorLabel, false, placeholders);
            this.setUserInputTextValidator(ColorProperty::isHexColor);
        }

        /** Returns the stored hexadecimal text, or an empty string. */
        @NotNull
        public String getHex() {
            return this.getString();
        }

        /** Tests RGB or ARGB hexadecimal color text with an optional leading '#'. */
        public static boolean isHexColor(@Nullable String value) {
            return value != null && value.matches("#?(?:[0-9a-fA-F]{6}|[0-9a-fA-F]{8})");
        }

        /** {@inheritDoc} */
        @Override
        protected Property<String> newCopyInstance() {
            return new ColorProperty(this.getKey(), this.getDefault(), this.get(), this.getContextMenuEntryLocalizationKeyBase(), this.allowsPlaceholders());
        }

    }

    /** Numeric or boolean property that can retain unresolved caller-defined input. */
    public abstract static class ManualInputProperty<T> extends Property<T> {

        @Nullable private String manualInput;
        private Function<String, String> manualInputResolver = Function.identity();
        @Nullable private final NumericInputBehavior<? extends Number> inputBehavior;

        /** Creates a manual-input property. */
        protected ManualInputProperty(String key, @Nullable T defaultValue, @Nullable T currentValue, String editorLabel, @Nullable NumericInputBehavior<? extends Number> inputBehavior) {
            super(key, defaultValue, currentValue, editorLabel);
            this.inputBehavior = inputBehavior;
        }

        /** Returns the raw unresolved input. */
        @Nullable
        public String getManualInput() {
            return this.manualInput;
        }

        /** Returns whether non-empty manual input is present. */
        public boolean hasManualInput() {
            return this.manualInput != null && !this.manualInput.isEmpty();
        }

        /** Sets raw input, with null or empty text clearing it. */
        @NotNull
        public ManualInputProperty<T> setManualInput(@Nullable String manualInput) {
            T oldValue = this.get();
            this.manualInput = manualInput == null || manualInput.isEmpty() ? null : manualInput;
            T newValue = this.get();
            if (!Objects.equals(oldValue, newValue)) this.notifyManualChange(oldValue, newValue);
            return this;
        }

        /** Clears raw input. */
        @NotNull
        public ManualInputProperty<T> clearManualInput() {
            return this.setManualInput(null);
        }

        /** Configures caller-owned placeholder or expression resolution. */
        @NotNull
        public ManualInputProperty<T> setManualInputResolver(@NotNull Function<String, String> resolver) {
            this.manualInputResolver = Objects.requireNonNull(resolver, "resolver");
            return this;
        }

        /** Returns numeric editor metadata when this is a numeric property. */
        @Nullable
        public NumericInputBehavior<? extends Number> getInputBehavior() {
            return this.inputBehavior;
        }

        /** Creates an immutable raw/typed value snapshot. */
        @NotNull
        public ManualInputSnapshot<T> createValueSnapshot() {
            return new ManualInputSnapshot<>(this.manualInput, super.get());
        }

        /** Restores a raw/typed snapshot. */
        public void applyValueSnapshot(@NotNull ManualInputSnapshot<T> snapshot) {
            ManualInputSnapshot<T> checkedSnapshot = Objects.requireNonNull(snapshot, "snapshot");
            this.manualInput = checkedSnapshot.manualInput();
            super.set(checkedSnapshot.currentValue());
        }

        /** Returns raw input or the formatted typed/default value. */
        @Nullable
        public String getRawInputOrFormattedValue() {
            if (this.manualInput != null) return this.manualInput;
            T value = super.get();
            if (value == null) value = this.getDefault();
            return value == null ? null : this.formatValue(value);
        }

        /** Resolves and formats the effective value as text. */
        @Nullable
        public String getAsString() {
            T value = this.get();
            return value == null ? null : this.formatValue(value);
        }

        /** Parses resolved manual input into the value type. */
        @Nullable
        protected abstract T parseInput(@NotNull String input);

        /** Tests whether serialized text is a literal typed value rather than caller-defined input. */
        protected abstract boolean isSerializedValueValid(@NotNull String value);

        /** Parses a validated literal serialized value. */
        @Nullable
        protected abstract T parseSerializedValue(@NotNull String value);

        /** Formats a typed value. */
        @NotNull
        protected String formatValue(@NotNull T value) {
            return value.toString();
        }

        /** {@inheritDoc} */
        @Override
        @Nullable
        public T get() {
            if (this.manualInput != null) {
                try {
                    T parsed = this.parseInput(this.manualInputResolver.apply(this.manualInput));
                    if (parsed != null) return this.processGet(parsed);
                } catch (RuntimeException ignored) {
                }
            }
            return super.get();
        }

        /** {@inheritDoc} */
        @Override
        public boolean isDefault() {
            return this.manualInput == null && super.isDefault();
        }

        /** {@inheritDoc} */
        @Override
        public Property<T> set(@Nullable T value) {
            this.manualInput = null;
            return super.set(value);
        }

        /** {@inheritDoc} */
        @Override
        public Property<T> deserialize(@NotNull PropertyContainer properties) {
            String serialized = properties.getValue(this.getKey());
            if (serialized == null) {
                this.manualInput = null;
                return super.set(this.getDefault());
            }
            if (this.isSerializedValueValid(serialized)) {
                this.manualInput = null;
                return super.set(this.parseSerializedValue(serialized));
            }
            this.manualInput = serialized;
            return super.set(this.getDefault());
        }

        /** {@inheritDoc} */
        @Override
        public Property<T> serialize(@NotNull PropertyContainer properties) {
            if (this.manualInput != null) {
                properties.putProperty(this.getKey(), this.manualInput);
                return this;
            }
            return super.serialize(properties);
        }

        /** Copies manual-input configuration after common configuration. */
        @Override
        protected void copyConfigurationTo(@NotNull Property<T> target) {
            super.copyConfigurationTo(target);
            ManualInputProperty<T> manualTarget = (ManualInputProperty<T>)target;
            manualTarget.manualInput = this.manualInput;
            manualTarget.manualInputResolver = this.manualInputResolver;
        }

        private void notifyManualChange(@Nullable T oldValue, @Nullable T newValue) {
            this.notifyValueSetListeners(oldValue, newValue);
        }

        /** Immutable raw/typed manual-input snapshot. */
        public record ManualInputSnapshot<T>(@Nullable String manualInput, @Nullable T currentValue) {

        }

    }

    /** Integer property with optional unresolved input. */
    public static class IntegerProperty extends ManualInputProperty<Integer> {

        /** Creates an integer property. */
        protected IntegerProperty(String key, Integer defaultValue, Integer currentValue, String editorLabel, @Nullable NumericInputBehavior<Integer> inputBehavior) {
            super(key, defaultValue, currentValue, editorLabel, inputBehavior);
            this.setDeserializationCodec(Integer::valueOf);
        }

        /** Returns the effective integer, falling back to one. */
        public int getInteger() {
            return this.tryGetNonNullElse(1);
        }

        /** {@inheritDoc} */
        @Override
        protected Integer parseInput(@NotNull String input) {
            try {
                return (int)Double.parseDouble(input.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        /** {@inheritDoc} */
        @Override
        protected boolean isSerializedValueValid(@NotNull String value) {
            try {
                Integer.parseInt(value);
                return true;
            } catch (NumberFormatException ignored) {
                return false;
            }
        }

        /** {@inheritDoc} */
        @Override
        protected Integer parseSerializedValue(@NotNull String value) {
            return Integer.valueOf(value);
        }

        /** {@inheritDoc} */
        @Override
        protected Property<Integer> newCopyInstance() {
            return new IntegerProperty(this.getKey(), this.getDefault(), super.get(), this.getContextMenuEntryLocalizationKeyBase(), (NumericInputBehavior<Integer>)this.getInputBehavior());
        }

    }

    /** Long property with optional unresolved input. */
    public static class LongProperty extends ManualInputProperty<Long> {

        /** Creates a long property. */
        protected LongProperty(String key, Long defaultValue, Long currentValue, String editorLabel, @Nullable NumericInputBehavior<Long> inputBehavior) {
            super(key, defaultValue, currentValue, editorLabel, inputBehavior);
            this.setDeserializationCodec(Long::valueOf);
        }

        /** Returns the effective long, falling back to one. */
        public long getLong() {
            return this.tryGetNonNullElse(1L);
        }

        /** {@inheritDoc} */
        @Override
        protected Long parseInput(@NotNull String input) {
            try {
                return Long.parseLong(input.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        /** {@inheritDoc} */
        @Override
        protected boolean isSerializedValueValid(@NotNull String value) {
            return this.parseInput(value) != null;
        }

        /** {@inheritDoc} */
        @Override
        protected Long parseSerializedValue(@NotNull String value) {
            return Long.valueOf(value);
        }

        /** {@inheritDoc} */
        @Override
        protected Property<Long> newCopyInstance() {
            return new LongProperty(this.getKey(), this.getDefault(), super.get(), this.getContextMenuEntryLocalizationKeyBase(), (NumericInputBehavior<Long>)this.getInputBehavior());
        }

    }

    /** Float property with optional unresolved input. */
    public static class FloatProperty extends ManualInputProperty<Float> {

        /** Creates a float property. */
        protected FloatProperty(String key, Float defaultValue, Float currentValue, String editorLabel, @Nullable NumericInputBehavior<Float> inputBehavior) {
            super(key, defaultValue, currentValue, editorLabel, inputBehavior);
            this.setDeserializationCodec(Float::valueOf);
        }

        /** Returns the effective float, falling back to one. */
        public float getFloat() {
            return this.tryGetNonNullElse(1.0F);
        }

        /** {@inheritDoc} */
        @Override
        protected Float parseInput(@NotNull String input) {
            try {
                float value = Float.parseFloat(input.trim());
                return Float.isFinite(value) ? value : null;
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        /** {@inheritDoc} */
        @Override
        protected boolean isSerializedValueValid(@NotNull String value) {
            return this.parseInput(value) != null;
        }

        /** {@inheritDoc} */
        @Override
        protected Float parseSerializedValue(@NotNull String value) {
            return Float.valueOf(value);
        }

        /** {@inheritDoc} */
        @Override
        protected Property<Float> newCopyInstance() {
            return new FloatProperty(this.getKey(), this.getDefault(), super.get(), this.getContextMenuEntryLocalizationKeyBase(), (NumericInputBehavior<Float>)this.getInputBehavior());
        }

    }

    /** Double property with optional unresolved input. */
    public static class DoubleProperty extends ManualInputProperty<Double> {

        /** Creates a double property. */
        protected DoubleProperty(String key, Double defaultValue, Double currentValue, String editorLabel, @Nullable NumericInputBehavior<Double> inputBehavior) {
            super(key, defaultValue, currentValue, editorLabel, inputBehavior);
            this.setDeserializationCodec(Double::valueOf);
        }

        /** Returns the effective double, falling back to one. */
        public double getDouble() {
            return this.tryGetNonNullElse(1.0D);
        }

        /** {@inheritDoc} */
        @Override
        protected Double parseInput(@NotNull String input) {
            try {
                double value = Double.parseDouble(input.trim());
                return Double.isFinite(value) ? value : null;
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        /** {@inheritDoc} */
        @Override
        protected boolean isSerializedValueValid(@NotNull String value) {
            return this.parseInput(value) != null;
        }

        /** {@inheritDoc} */
        @Override
        protected Double parseSerializedValue(@NotNull String value) {
            return Double.valueOf(value);
        }

        /** {@inheritDoc} */
        @Override
        protected Property<Double> newCopyInstance() {
            return new DoubleProperty(this.getKey(), this.getDefault(), super.get(), this.getContextMenuEntryLocalizationKeyBase(), (NumericInputBehavior<Double>)this.getInputBehavior());
        }

    }

    /** Boolean property with optional unresolved input. */
    public static class BooleanProperty extends ManualInputProperty<Boolean> {

        /** Creates a boolean property. */
        protected BooleanProperty(String key, Boolean defaultValue, Boolean currentValue, String editorLabel) {
            super(key, defaultValue, currentValue, editorLabel, null);
            this.setDeserializationCodec(Boolean::valueOf);
        }

        /** Returns the effective boolean, falling back to false. */
        public boolean getBoolean() {
            return this.tryGetNonNullElse(false);
        }

        /** {@inheritDoc} */
        @Override
        protected Boolean parseInput(@NotNull String input) {
            if ("true".equalsIgnoreCase(input.trim())) return true;
            if ("false".equalsIgnoreCase(input.trim())) return false;
            return null;
        }

        /** {@inheritDoc} */
        @Override
        protected boolean isSerializedValueValid(@NotNull String value) {
            return this.parseInput(value) != null;
        }

        /** {@inheritDoc} */
        @Override
        protected Boolean parseSerializedValue(@NotNull String value) {
            return this.parseInput(value);
        }

        /** {@inheritDoc} */
        @Override
        protected Property<Boolean> newCopyInstance() {
            return new BooleanProperty(this.getKey(), this.getDefault(), super.get(), this.getContextMenuEntryLocalizationKeyBase());
        }

    }

}
