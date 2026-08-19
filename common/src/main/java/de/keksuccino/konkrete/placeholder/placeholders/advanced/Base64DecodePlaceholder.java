package de.keksuccino.konkrete.placeholder.placeholders.advanced;

import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import de.keksuccino.konkrete.util.cache.BoundedConcurrentCache;
import de.keksuccino.konkrete.placeholder.Placeholder;
import de.keksuccino.konkrete.util.LocalizationUtils;
import net.minecraft.client.resources.language.I18n;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Applies the base64 decode transformation for {@code base64_decode}. */
public class Base64DecodePlaceholder extends Placeholder {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final BoundedConcurrentCache<String, String> DECODE_CACHE = new BoundedConcurrentCache<>(512, 4_194_304L, (input, output) -> 64L + input.length() * 2L + output.length() * 2L);

    /** Creates the {@code base64_decode} placeholder. */
    public Base64DecodePlaceholder() {
        super("base64_decode");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        try {
            String text = dps.values.get("text");
            if (text == null) {
                text = dps.values.get("value");
            }
            if (text == null) {
                LOGGER.error("[KONKRETE] Missing 'text' value for 'Decode From Base64' placeholder: {}", dps.placeholderString);
                return null;
            }
            String cached = DECODE_CACHE.get(text);
            if (cached != null) {
                return cached;
            }
            byte[] decoded = Base64.getDecoder().decode(text);
            String decodedText = new String(decoded, StandardCharsets.UTF_8);
            DECODE_CACHE.put(text, decodedText);
            return decodedText;
        } catch (IllegalArgumentException ex) {
            LOGGER.error("[KONKRETE] Invalid Base64 input for placeholder: {}", dps.placeholderString, ex);
        } catch (Exception ex) {
            LOGGER.error("[KONKRETE] Failed to decode Base64 for placeholder: {}", dps.placeholderString, ex);
        }
        return null;
    }

    @Override
    public @Nullable List<String> getValueNames() {
        List<String> values = new ArrayList<>();
        values.add("text");
        values.add("value");
        return values;
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("konkrete.placeholders.base64_decode");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("konkrete.placeholders.base64_decode.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("konkrete.requirements.categories.advanced");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        Map<String, String> values = new HashMap<>();
        values.put("text", "dGV4dA==");
        return DeserializedPlaceholderString.build(this.getIdentifier(), values);
    }

}
