package de.keksuccino.konkrete.util.rendering.ui.widget.component;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;
import java.io.StringReader;

/** Encodes and decodes component. */
public class ComponentSerialization {

    /** Encodes and decodes serializer. */
    public static class Serializer {

        private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

        private Serializer() {
        }

        static MutableComponent deserialize(JsonElement json, HolderLookup.Provider provider) {
            return (MutableComponent) net.minecraft.network.chat.ComponentSerialization.CODEC.parse(provider.createSerializationContext(JsonOps.INSTANCE), json).getOrThrow(JsonParseException::new);
        }

        static JsonElement serialize(Component component, HolderLookup.Provider provider) {
            return net.minecraft.network.chat.ComponentSerialization.CODEC.encodeStart(provider.createSerializationContext(JsonOps.INSTANCE), component).getOrThrow(JsonParseException::new);
        }

        /** Converts the value to JSON. */
        public static String toJson(Component component, HolderLookup.Provider registries) {
            return GSON.toJson(serialize(component, registries));
        }

        /** Parses strict component JSON using the supplied registries. */
        @Nullable
        public static MutableComponent fromJson(String json, HolderLookup.Provider registries) {
            JsonElement jsonElement = JsonParser.parseString(json);
            return jsonElement == null ? null : deserialize(jsonElement, registries);
        }

        /** Decodes a component from an already parsed JSON element. */
        @Nullable
        public static MutableComponent fromJson(@Nullable JsonElement json, HolderLookup.Provider registries) {
            return json == null ? null : deserialize(json, registries);
        }

        /** Parses lenient component JSON using the supplied registries. */
        @Nullable
        public static MutableComponent fromJsonLenient(String json, HolderLookup.Provider registries) {
            JsonReader jsonReader = new JsonReader(new StringReader(json));
            jsonReader.setLenient(true);
            JsonElement jsonElement = JsonParser.parseReader(jsonReader);
            return jsonElement == null ? null : deserialize(jsonElement, registries);
        }

    }

}
