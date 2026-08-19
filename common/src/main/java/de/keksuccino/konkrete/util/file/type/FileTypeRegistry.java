package de.keksuccino.konkrete.util.file.type;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;

/** Maintains file-type descriptors in deterministic registration order. */
@SuppressWarnings("unused")
public class FileTypeRegistry {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final Map<String, FileType<?>> FILE_TYPES = new LinkedHashMap<>();

    /** Registers a non-null descriptor under a unique non-null name. */
    public static void register(@NotNull String fileTypeName, @NotNull FileType<?> fileType) {
        if (FILE_TYPES.containsKey(Objects.requireNonNull(fileTypeName))) {
            throw new RuntimeException("[KONKRETE] Failed to register FileType! FileType name already registered: " + fileTypeName);
        }
        FILE_TYPES.put(fileTypeName, Objects.requireNonNull(fileType));
    }

    /** Returns the descriptor registered under {@code fileTypeName}, or {@code null}. */
    @Nullable
    public static FileType<?> getFileType(@NotNull String fileTypeName) {
        return FILE_TYPES.get(fileTypeName);
    }

    /** Returns a mutable snapshot in registration order. */
    @NotNull
    public static List<FileType<?>> getFileTypes() {
        return new ArrayList<>(FILE_TYPES.values());
    }

}
