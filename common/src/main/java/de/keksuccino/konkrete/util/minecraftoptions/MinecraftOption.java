package de.keksuccino.konkrete.util.minecraftoptions;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.world.entity.player.PlayerModelPart;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * String-serializable view of one vanilla option, key mapping, or player model-part toggle. Mutations must run on Minecraft's client thread.
 */
public final class MinecraftOption {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new Gson();

    private final String name;
    private final Kind kind;
    @Nullable private final OptionInstance<Object> optionInstance;
    @Nullable private final KeyMapping keyMapping;
    @Nullable private final PlayerModelPart modelPart;
    private final Options options;

    private MinecraftOption(String name, Kind kind, @Nullable OptionInstance<Object> optionInstance, @Nullable KeyMapping keyMapping, @Nullable PlayerModelPart modelPart, Options options) {
        this.name = name;
        this.kind = kind;
        this.optionInstance = optionInstance;
        this.keyMapping = keyMapping;
        this.modelPart = modelPart;
        this.options = options;
    }

    /**
     * Wraps a codec-backed option instance.
     *
     * @param name serialized vanilla option name
     * @param optionInstance option instance
     * @param options owning options object
     * @return option wrapper
     */
    @NotNull
    public static MinecraftOption of(@NotNull String name, @NotNull OptionInstance<?> optionInstance, @NotNull Options options) {
        @SuppressWarnings("unchecked") OptionInstance<Object> castInstance = (OptionInstance<Object>)Objects.requireNonNull(optionInstance, "optionInstance");
        return new MinecraftOption(Objects.requireNonNull(name, "name"), Kind.OPTION_INSTANCE, castInstance, null, null, Objects.requireNonNull(options, "options"));
    }

    /**
     * Wraps a key mapping.
     *
     * @param keyMapping key mapping
     * @param options owning options object
     * @return option wrapper
     */
    @NotNull
    public static MinecraftOption of(@NotNull KeyMapping keyMapping, @NotNull Options options) {
        KeyMapping checkedMapping = Objects.requireNonNull(keyMapping, "keyMapping");
        return new MinecraftOption("key_" + checkedMapping.getName(), Kind.KEY_MAPPING, null, checkedMapping, null, Objects.requireNonNull(options, "options"));
    }

    /**
     * Wraps a player model-part toggle.
     *
     * @param modelPart model part
     * @param options owning options object
     * @return option wrapper
     */
    @NotNull
    public static MinecraftOption of(@NotNull PlayerModelPart modelPart, @NotNull Options options) {
        PlayerModelPart checkedPart = Objects.requireNonNull(modelPart, "modelPart");
        return new MinecraftOption("modelPart_" + checkedPart.getId(), Kind.MODEL_PART, null, null, checkedPart, Objects.requireNonNull(options, "options"));
    }

    /**
     * Encodes the current value exactly as vanilla's option file representation expects.
     *
     * @return encoded value, or {@code null} when the codec reports an error
     */
    @Nullable
    public String get() {
        if (this.optionInstance != null) {
            DataResult<JsonElement> result = this.optionInstance.codec().encodeStart(JsonOps.INSTANCE, this.optionInstance.get());
            if (result.error().isPresent()) {
                LOGGER.warn("[KONKRETE] Failed to encode Minecraft option {}: {}", this.name, result.error().get().message());
                return null;
            }
            return result.result().map(GSON::toJson).orElse(null);
        }
        if (this.keyMapping != null) return this.keyMapping.saveString();
        if (this.modelPart != null) return Boolean.toString(this.options.isModelPartEnabled(this.modelPart));
        return null;
    }

    /**
     * Parses and applies an option-file representation.
     *
     * @param value encoded value
     * @return whether the value was valid and applied
     */
    public boolean set(@NotNull String value) {
        String checkedValue = Objects.requireNonNull(value, "value");
        try {
            if (this.optionInstance != null) {
                JsonElement json = JsonParser.parseString(checkedValue.isEmpty() ? "\"\"" : checkedValue);
                DataResult<Object> result = this.optionInstance.codec().parse(JsonOps.INSTANCE, json);
                if (result.error().isPresent()) {
                    LOGGER.warn("[KONKRETE] Failed to parse Minecraft option {}: {}", this.name, result.error().get().message());
                    return false;
                }
                Object parsed = result.result().orElse(null);
                if (parsed == null) return false;
                this.optionInstance.set(parsed);
                return true;
            }
            if (this.keyMapping != null) {
                this.keyMapping.setKey(InputConstants.getKey(checkedValue));
                KeyMapping.resetMapping();
                return true;
            }
            if (this.modelPart != null) {
                if (!"true".equalsIgnoreCase(checkedValue) && !"false".equalsIgnoreCase(checkedValue)) return false;
                this.options.setModelPart(this.modelPart, Boolean.parseBoolean(checkedValue));
                return true;
            }
        } catch (RuntimeException exception) {
            LOGGER.warn("[KONKRETE] Failed to apply Minecraft option {}", this.name, exception);
        }
        return false;
    }

    /** Returns the wrapped option instance, or {@code null} for key/model-part wrappers. */
    @Nullable
    public OptionInstance<?> getOptionInstance() {
        return this.optionInstance;
    }

    /** Returns the serialized vanilla option name. */
    @NotNull
    public String getName() {
        return this.name;
    }

    /** Returns the kind of wrapped value. */
    @NotNull
    public Kind getKind() {
        return this.kind;
    }

    /** Supported vanilla option representations. */
    public enum Kind {

        OPTION_INSTANCE,
        KEY_MAPPING,
        MODEL_PART

    }

}
