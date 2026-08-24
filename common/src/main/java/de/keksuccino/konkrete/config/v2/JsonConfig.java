package de.keksuccino.konkrete.config.v2;

import com.google.gson.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * A standalone, typed JSON store for configuration options and persistent data.
 * Values can live at the document root or inside named sections, and changes are saved immediately.
 */
public class JsonConfig {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path file;
    @Nullable
    private final Path legacyFile;
    private final Map<ValuePath, ConfigValue<?>> registeredValues = new LinkedHashMap<>();
    private JsonObject values = new JsonObject();
    private boolean dirty;
    private boolean migrationBlocked;
    private boolean migrationPending;

    public JsonConfig(@NotNull File file) {
        this(Objects.requireNonNull(file).toPath(), null);
    }

    public JsonConfig(@NotNull File file, @Nullable File legacyFile) {
        this(Objects.requireNonNull(file).toPath(), legacyFile != null ? legacyFile.toPath() : null);
    }

    public JsonConfig(@NotNull Path file) {
        this(file, null);
    }

    public JsonConfig(@NotNull Path file, @Nullable Path legacyFile) {
        this.file = normalize(file);
        this.legacyFile = legacyFile != null ? normalize(legacyFile) : null;
        if (this.file.equals(this.legacyFile)) throw new IllegalArgumentException("The JSON and legacy config paths must be different.");
        this.loadInitialValues();
    }

    /**
     * Creates a handle for a named section. Calling this method does not modify the document by itself.
     */
    @NotNull
    public ConfigSection section(@NotNull String name) {
        return new ConfigSection(this, requireName(name, "section"));
    }

    /**
     * Defines a root value and infers its stored type from its default value.
     */
    @NotNull
    public <T> ConfigValue<T> option(@NotNull String key, @NotNull T defaultValue) {
        return this.option(null, key, defaultValue);
    }

    /**
     * Defines a root value with an explicit type.
     */
    @NotNull
    public <T> ConfigValue<T> option(@NotNull String key, @NotNull Type type, @NotNull T defaultValue) {
        return this.option(null, key, type, defaultValue);
    }

    /**
     * Defines an optional root value that is absent until explicitly set.
     */
    @NotNull
    public <T> ConfigValue<T> optional(@NotNull String key, @NotNull Class<T> type) {
        return this.optional(null, key, type);
    }

    /**
     * Defines an optional root value with a parameterized or otherwise non-Class type.
     */
    @NotNull
    public <T> ConfigValue<T> optional(@NotNull String key, @NotNull Type type) {
        return this.optional(null, key, type);
    }

    @NotNull
    <T> ConfigValue<T> option(@Nullable String section, @NotNull String key, @NotNull T defaultValue) {
        Objects.requireNonNull(defaultValue);
        return this.option(section, key, defaultValue.getClass(), defaultValue);
    }

    @NotNull
    <T> ConfigValue<T> option(@Nullable String section, @NotNull String key, @NotNull Type type, @NotNull T defaultValue) {
        return this.define(section, key, type, Objects.requireNonNull(defaultValue), true);
    }

    @NotNull
    <T> ConfigValue<T> optional(@Nullable String section, @NotNull String key, @NotNull Type type) {
        return this.define(section, key, type, null, false);
    }

    /**
     * Writes all pending changes. This is useful after declaring several defaults because individual declarations are batched in memory.
     */
    public final synchronized boolean save() {
        if (this.migrationBlocked) {
            LOGGER.error("Refusing to write config {} because its legacy source {} could not be read.", this.file, this.legacyFile);
            return false;
        }
        if (!this.dirty && Files.exists(this.file)) {
            this.finishMigration();
            return true;
        }
        if (!this.writeDocument()) return false;
        this.dirty = false;
        this.finishMigration();
        return true;
    }

    /**
     * Reloads the document and reapplies declared defaults for missing or invalid values.
     */
    public final synchronized boolean reload() {
        this.values = new JsonObject();
        this.dirty = false;
        this.migrationBlocked = false;
        this.migrationPending = false;
        this.loadInitialValues();
        if (this.migrationBlocked) return false;
        for (ConfigValue<?> value : this.registeredValues.values()) {
            this.restoreDefaultIfNeeded(value);
        }
        return !this.dirty || this.save();
    }

    @NotNull
    public Path getFile() {
        return this.file;
    }

    @Nullable
    public Path getLegacyFile() {
        return this.legacyFile;
    }

    synchronized <T> void setValue(@NotNull ConfigValue<T> value, @NotNull T newValue) {
        JsonElement serializedValue = this.serialize(newValue, value.getType(), value.getPath());
        JsonElement previousValue = this.getElement(value.getPath());
        T previousStoredValue = value.getStoredValue();
        boolean wasDirty = this.dirty;
        if (serializedValue.equals(previousValue)) {
            if (this.dirty || Files.notExists(this.file)) this.save();
            return;
        }

        this.putElement(value.getPath(), serializedValue);
        value.setStoredValue(newValue);
        this.dirty = true;
        if (!this.save()) {
            this.restoreElement(value.getPath(), previousValue);
            value.setStoredValue(previousStoredValue);
            this.dirty = wasDirty;
        }
    }

    synchronized <T> void updateValue(@NotNull ConfigValue<T> value, @NotNull UnaryOperator<T> updater) {
        this.setValue(value, Objects.requireNonNull(updater.apply(value.getValue())));
    }

    synchronized void removeValue(@NotNull ConfigValue<?> value) {
        JsonElement previousValue = this.getElement(value.getPath());
        if (previousValue == null) return;
        Object previousStoredValue = value.getStoredValue();
        boolean wasDirty = this.dirty;
        this.removeElement(value.getPath());
        value.setStoredValue(null);
        this.dirty = true;
        if (!this.save()) {
            this.restoreElement(value.getPath(), previousValue);
            this.restoreStoredValue(value, previousStoredValue);
            this.dirty = wasDirty;
        }
    }

    @NotNull
    private synchronized <T> ConfigValue<T> define(@Nullable String section, @NotNull String key, @NotNull Type type, @Nullable T defaultValue, boolean hasDefaultValue) {
        String validatedSection = section != null ? requireName(section, "section") : null;
        ValuePath path = new ValuePath(validatedSection, requireName(key, "key"));
        Objects.requireNonNull(type);
        if (this.registeredValues.containsKey(path)) throw new IllegalArgumentException("Config value '" + path + "' is already defined.");

        ConfigValue<T> value = new ConfigValue<>(this, path, type, defaultValue, hasDefaultValue);
        if (hasDefaultValue) {
            this.serialize(Objects.requireNonNull(defaultValue), type, path);
        }
        this.registeredValues.put(path, value);
        this.restoreDefaultIfNeeded(value);
        return value;
    }

    private <T> void restoreDefaultIfNeeded(@NotNull ConfigValue<T> value) {
        JsonElement storedValue = this.getElement(value.getPath());
        T deserializedValue = storedValue != null && !storedValue.isJsonNull() ? this.deserialize(storedValue, value.getType(), value.getPath()) : null;
        if (deserializedValue != null) {
            value.setStoredValue(deserializedValue);
            return;
        }
        value.setStoredValue(null);
        if (!value.hasDefaultValue()) return;
        T defaultValue = value.getDefaultValue();
        this.putElement(value.getPath(), this.serialize(defaultValue, value.getType(), value.getPath()));
        value.setStoredValue(defaultValue);
        this.dirty = true;
    }

    @SuppressWarnings("unchecked")
    private <T> void restoreStoredValue(@NotNull ConfigValue<T> value, @Nullable Object storedValue) {
        value.setStoredValue((T) storedValue);
    }

    private void loadInitialValues() {
        if (Files.exists(this.file)) {
            this.values = this.readDocument();
            return;
        }
        if (this.legacyFile == null || Files.notExists(this.legacyFile)) return;

        this.migrationPending = true;
        try {
            LegacyConfigReader.Result result = LegacyConfigReader.read(this.legacyFile);
            this.values = result.getValues();
            this.dirty = true;
            if (!result.isComplete()) {
                this.migrationBlocked = true;
                LOGGER.error("Legacy config {} contains unrecognized data. Its parsed values will be used in memory, but no files will be replaced.", this.legacyFile);
                return;
            }
        } catch (IOException | RuntimeException ex) {
            this.migrationBlocked = true;
            LOGGER.error("Failed to read legacy config {}. The replacement config will not be written until the legacy file can be read.", this.legacyFile, ex);
            return;
        }

        this.save();
    }

    @NotNull
    private JsonObject readDocument() {
        try (Reader reader = Files.newBufferedReader(this.file, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (root != null && root.isJsonObject()) return root.getAsJsonObject();
            LOGGER.warn("Ignoring config {} because its JSON root is not an object.", this.file);
        } catch (IOException | RuntimeException ex) {
            LOGGER.warn("Failed to read config {}. It will be repaired when a value is next saved.", this.file, ex);
        }
        return new JsonObject();
    }

    private boolean writeDocument() {
        Path temporaryFile = null;
        try {
            Path parent = this.file.getParent();
            Files.createDirectories(parent);
            String temporaryPrefix = this.file.getFileName().toString();
            // Files.createTempFile requires at least three prefix characters, including for otherwise valid one-character file names.
            if (temporaryPrefix.length() < 3) temporaryPrefix = (temporaryPrefix + "___").substring(0, 3);
            temporaryFile = Files.createTempFile(parent, temporaryPrefix, ".tmp");
            Files.writeString(temporaryFile, GSON.toJson(this.values) + System.lineSeparator(), StandardCharsets.UTF_8);
            this.replaceWith(temporaryFile);
            return true;
        } catch (IOException | RuntimeException ex) {
            LOGGER.error("Failed to write config {}.", this.file, ex);
            return false;
        } finally {
            if (temporaryFile != null) {
                try {
                    Files.deleteIfExists(temporaryFile);
                } catch (IOException ex) {
                    LOGGER.warn("Failed to clean up temporary config file {}.", temporaryFile, ex);
                }
            }
        }
    }

    private void replaceWith(@NotNull Path temporaryFile) throws IOException {
        // Keeping the temporary file beside the target guarantees that an atomic move cannot cross file-system boundaries.
        try {
            Files.move(temporaryFile, this.file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(temporaryFile, this.file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void finishMigration() {
        if (!this.migrationPending || this.legacyFile == null) return;
        try {
            Files.deleteIfExists(this.legacyFile);
            this.migrationPending = false;
            LOGGER.info("Converted legacy config {} to {}.", this.legacyFile, this.file);
        } catch (IOException ex) {
            LOGGER.warn("Converted legacy config {} but could not remove the old file.", this.legacyFile, ex);
        }
    }

    @Nullable
    private <T> T deserialize(@NotNull JsonElement element, @NotNull Type type, @NotNull ValuePath path) {
        try {
            if (!isCompatibleJsonType(element, type)) throw new JsonParseException("Unexpected JSON value type");
            T value = GSON.fromJson(element, type);
            if (value instanceof Float && !Float.isFinite((Float) value)) throw new JsonParseException("Non-finite float");
            if (value instanceof Double && !Double.isFinite((Double) value)) throw new JsonParseException("Non-finite double");
            return value;
        } catch (RuntimeException ex) {
            LOGGER.warn("Ignoring invalid value '{}' in config {}.", path, this.file, ex);
            return null;
        }
    }

    @NotNull
    private JsonElement serialize(@NotNull Object value, @NotNull Type type, @NotNull ValuePath path) {
        try {
            JsonElement serializedValue = GSON.toJsonTree(value, type);
            if (serializedValue == null || serializedValue.isJsonNull()) throw new IllegalArgumentException("Serialized value is null");
            return serializedValue;
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Failed to serialize config value '" + path + "'.", ex);
        }
    }

    @Nullable
    private JsonElement getElement(@NotNull ValuePath path) {
        if (path.section == null) return this.values.get(path.key);
        JsonElement section = this.values.get(path.section);
        return section != null && section.isJsonObject() ? section.getAsJsonObject().get(path.key) : null;
    }

    private void putElement(@NotNull ValuePath path, @NotNull JsonElement value) {
        if (path.section == null) {
            this.values.add(path.key, value);
            return;
        }
        JsonElement existingSection = this.values.get(path.section);
        JsonObject section = existingSection != null && existingSection.isJsonObject() ? existingSection.getAsJsonObject() : new JsonObject();
        section.add(path.key, value);
        this.values.add(path.section, section);
    }

    private void removeElement(@NotNull ValuePath path) {
        if (path.section == null) {
            this.values.remove(path.key);
            return;
        }
        JsonElement existingSection = this.values.get(path.section);
        if (existingSection == null || !existingSection.isJsonObject()) return;
        JsonObject section = existingSection.getAsJsonObject();
        section.remove(path.key);
        if (section.size() == 0) this.values.remove(path.section);
    }

    private void restoreElement(@NotNull ValuePath path, @Nullable JsonElement previousValue) {
        if (previousValue == null) {
            this.removeElement(path);
        } else {
            this.putElement(path, previousValue);
        }
    }

    private static boolean isCompatibleJsonType(@NotNull JsonElement element, @NotNull Type type) {
        if (!(type instanceof Class<?>)) return true;
        Class<?> valueClass = (Class<?>) type;
        if (valueClass == String.class || valueClass == Character.class || valueClass == char.class || valueClass.isEnum()) return element.isJsonPrimitive() && element.getAsJsonPrimitive().isString();
        if (valueClass == Boolean.class || valueClass == boolean.class) return element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean();
        if (Number.class.isAssignableFrom(valueClass) || valueClass.isPrimitive()) return element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber();
        if (JsonObject.class.isAssignableFrom(valueClass)) return element.isJsonObject();
        return true;
    }

    @NotNull
    private static Path normalize(@NotNull Path path) {
        return Objects.requireNonNull(path).toAbsolutePath().normalize();
    }

    @NotNull
    private static String requireName(@NotNull String name, @NotNull String kind) {
        String validatedName = Objects.requireNonNull(name).trim();
        if (validatedName.isEmpty()) throw new IllegalArgumentException("Config " + kind + " must not be empty.");
        return validatedName;
    }

    static final class ValuePath {

        @Nullable
        private final String section;
        private final String key;

        private ValuePath(@Nullable String section, @NotNull String key) {
            this.section = section;
            this.key = key;
        }

        @Nullable
        String getSection() {
            return this.section;
        }

        @NotNull
        String getKey() {
            return this.key;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (!(object instanceof ValuePath)) return false;
            ValuePath valuePath = (ValuePath) object;
            return Objects.equals(this.section, valuePath.section) && this.key.equals(valuePath.key);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.section, this.key);
        }

        @Override
        public String toString() {
            return this.section != null ? this.section + "." + this.key : this.key;
        }

    }

}
