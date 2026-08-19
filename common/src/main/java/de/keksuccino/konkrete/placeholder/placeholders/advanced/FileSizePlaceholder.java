package de.keksuccino.konkrete.placeholder.placeholders.advanced;

import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import de.keksuccino.konkrete.placeholder.Placeholder;
import de.keksuccino.konkrete.util.LocalizationUtils;
import de.keksuccino.konkrete.util.resource.ResourceSource;
import de.keksuccino.konkrete.util.resource.ResourceSourceType;
import net.minecraft.client.resources.language.I18n;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Evaluates file size inputs for {@code file_size}. */
public class FileSizePlaceholder extends Placeholder {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Creates the {@code file_size} placeholder. */
    public FileSizePlaceholder() {
        super("file_size");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String filePath = dps.values.get("path");

        if (filePath == null || filePath.isEmpty()) {
            LOGGER.warn("[KONKRETE] File Size placeholder: No path provided");
            return "0";
        }

        // Don't allow URLs, only local files
        if (filePath.startsWith("http://") || filePath.startsWith("https://")) {
            LOGGER.warn("[KONKRETE] File Size placeholder: URLs are not supported, only local files");
            return "0";
        }

        try {
            ResourceSource source = ResourceSource.of(filePath, ResourceSourceType.LOCAL);
            File validatedFile = source.getValidatedLocalFile();
            if (validatedFile == null) return "0";
            Path path = validatedFile.toPath();

            if (!Files.exists(path)) {
                LOGGER.warn("[KONKRETE] File Size placeholder: File not found: " + filePath);
                return "0";
            }

            if (!Files.isRegularFile(path)) {
                LOGGER.warn("[KONKRETE] File Size placeholder: Path is not a regular file: " + filePath);
                return "0";
            }

            // Get file size in bytes
            validatedFile = source.getValidatedLocalFile();
            if (validatedFile == null) return "0";
            long sizeInBytes = Files.size(validatedFile.toPath());
            return String.valueOf(sizeInBytes);

        } catch (Exception e) {
            LOGGER.error("[KONKRETE] File Size placeholder: Failed to get file size for: " + filePath, e);
            return "0";
        }
    }

    @Override
    public @Nullable List<String> getValueNames() {
        List<String> l = new ArrayList<>();
        l.add("path");
        return l;
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("konkrete.placeholders.file_size");
    }

    @Override
    public @Nullable List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("konkrete.placeholders.file_size.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("konkrete.requirements.categories.advanced");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        values.put("path", "/config/example.txt");
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }

}
