package de.keksuccino.konkrete.util.resource;

import de.keksuccino.konkrete.util.enums.LocalizedCycleEnum;
import de.keksuccino.konkrete.util.input.TextValidators;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Objects;

/** Identifies how a serialized source is opened: local file, URL, or resource pack. */
public enum ResourceSourceType implements LocalizedCycleEnum<ResourceSourceType> {

    /** Loads the source from the active Minecraft resource manager. */
    LOCATION("location"),
    /** Loads the source from a validated local filesystem path. */
    LOCAL("local"),
    /** Loads the source through the configured web transport. */
    WEB("web");

    private final String name;

    ResourceSourceType(@NotNull String name) {
        this.name = name;
    }

    /** Returns the source prefix used by this resource runtime instance. */
    @NotNull
    public String getSourcePrefix() {
        return "[source:" + this.name + "]";
    }

    /** Returns whether the source starts with any recognized serialization prefix. */
    public static boolean hasSourcePrefix(@NotNull String resourceSource) {
        Objects.requireNonNull(resourceSource, "resourceSource");
        if (resourceSource.startsWith(LOCATION.getSourcePrefix())) return true;
        if (resourceSource.startsWith(LOCAL.getSourcePrefix())) return true;
        if (resourceSource.startsWith(WEB.getSourcePrefix())) return true;
        return false;
    }

    /** Removes one recognized leading serialization prefix without altering prefix-like payload text. */
    @NotNull
    public static String getWithoutSourcePrefix(@NotNull String resourceSource) {
        Objects.requireNonNull(resourceSource, "resourceSource");
        for (ResourceSourceType type : values()) {
            if (resourceSource.startsWith(type.getSourcePrefix())) return resourceSource.substring(type.getSourcePrefix().length());
        }
        return resourceSource;
    }

    /**
     * Tries to find the {@link ResourceSourceType} of a resource's source.
     *
     * @param resourceSource Can be a URL to a web source, a path to a local source or a resource location (namespace:path).
     */
    @NotNull
    public static ResourceSourceType getSourceTypeOf(@NotNull String resourceSource) {

        Objects.requireNonNull(resourceSource);

        //Check for source prefix
        if (resourceSource.startsWith(LOCAL.getSourcePrefix())) return LOCAL;
        if (resourceSource.startsWith(WEB.getSourcePrefix())) return WEB;
        if (resourceSource.startsWith(LOCATION.getSourcePrefix())) return LOCATION;

        //If no prefix, try to get source type the classic way
        if (TextValidators.BASIC_URL_TEXT_VALIDATOR.get(getWithoutSourcePrefix(resourceSource))) return WEB;
        if (resourceSource.contains(":")) {
            if (Identifier.tryParse(getWithoutSourcePrefix(resourceSource)) != null) return LOCATION;
        }

        //Fallback type and no-prefix return, if source is not WEB and not LOCATION
        return LOCAL;

    }

    /** Returns the value component style used by this resource runtime instance. */
    @Override
    public @NotNull Style getValueComponentStyle() {
        return WARNING_TEXT_STYLE.get();
    }

    /** Returns the localization key base used by this resource runtime instance. */
    @Override
    public @NotNull String getLocalizationKeyBase() {
        return "konkrete.resources.source_type";
    }

    /** Returns the name used by this resource runtime instance. */
    @Override
    public @NotNull String getName() {
        return this.name;
    }

    /** Returns the values used by this resource runtime instance. */
    @Override
    public @NotNull ResourceSourceType[] getValues() {
        return ResourceSourceType.values();
    }

    /** Returns the by name internal, or {@code null} when it is not available. */
    @Override
    public @Nullable ResourceSourceType getByNameInternal(@NotNull String name) {
        return getByName(name);
    }

    /** Returns the by name, or {@code null} when it is not available. */
    @Nullable
    public static ResourceSourceType getByName(@NotNull String name) {
        for (ResourceSourceType t : ResourceSourceType.values()) {
            if (t.name.equals(name)) return t;
        }
        return null;
    }

}
