package de.keksuccino.konkrete.util.resource;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.serialization.DataResult;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/** Normalizes namespace-root pack enumeration without changing non-root lookups. */
@ApiStatus.Internal
public final class PackResourcesRootEnumeration {

    private PackResourcesRootEnumeration() {}

    /** Normalizes the archive prefix for the resource runtime. */
    @NotNull
    public static String normalizeArchivePrefix(@NotNull String prefix, @NotNull String directory) {
        if (!directory.isEmpty() || !prefix.endsWith("//")) return prefix;
        return prefix.substring(0, prefix.length() - 1);
    }

    /** Represents the namespace root with no path segments and delegates non-root decomposition unchanged. */
    @NotNull
    public static DataResult<List<String>> decomposeDirectory(@NotNull String directory, @NotNull Operation<DataResult<List<String>>> original) {
        return directory.isEmpty() ? DataResult.success(List.of()) : original.call(directory);
    }

}
