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
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;

/** Evaluates file md5 inputs for {@code file_md5}. */
public class FileMd5Placeholder extends Placeholder {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Creates the {@code file_md5} placeholder. */
    public FileMd5Placeholder() {
        super("file_md5");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String filePath = dps.values.get("path");

        if (filePath == null || filePath.isEmpty()) {
            LOGGER.warn("[KONKRETE] File MD5 placeholder: No path provided");
            return "";
        }

        // Don't allow URLs, only local files
        if (filePath.startsWith("http://") || filePath.startsWith("https://")) {
            LOGGER.warn("[KONKRETE] File MD5 placeholder: URLs are not supported, only local files");
            return "";
        }

        try {
            ResourceSource source = ResourceSource.of(filePath, ResourceSourceType.LOCAL);
            File validatedFile = source.getValidatedLocalFile();
            if (validatedFile == null) return "";
            Path path = validatedFile.toPath();

            if (!Files.exists(path)) {
                LOGGER.warn("[KONKRETE] File MD5 placeholder: File not found: " + filePath);
                return "";
            }

            if (!Files.isRegularFile(path)) {
                LOGGER.warn("[KONKRETE] File MD5 placeholder: Path is not a regular file: " + filePath);
                return "";
            }

            // Calculate MD5 hash
            MessageDigest md = MessageDigest.getInstance("MD5");
            validatedFile = source.getValidatedLocalFile();
            if (validatedFile == null) return "";
            try (InputStream is = new FileInputStream(validatedFile)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = is.read(buffer)) > 0) {
                    md.update(buffer, 0, read);
                }
            }

            // Convert byte array to hex string
            byte[] digest = md.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();

        } catch (Exception e) {
            LOGGER.error("[KONKRETE] File MD5 placeholder: Failed to calculate MD5 hash for: " + filePath, e);
            return "";
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
        return I18n.get("konkrete.placeholders.file_md5");
    }

    @Override
    public @Nullable List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("konkrete.placeholders.file_md5.desc"));
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
