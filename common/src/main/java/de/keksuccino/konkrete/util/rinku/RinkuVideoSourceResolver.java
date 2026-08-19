package de.keksuccino.konkrete.util.rinku;

import de.keksuccino.konkrete.util.file.LocalSourcePathResolver;
import de.keksuccino.konkrete.util.resource.ResourceSource;
import de.keksuccino.konkrete.util.resource.ResourceSourceType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Resolves placeholder-expanded Rinku sources immediately before handing them to Chromium. */
public final class RinkuVideoSourceResolver {

    private RinkuVideoSourceResolver() {
    }

    /** Resolves a source with Konkrete's default confined game-directory policy. */
    @Nullable
    public static String resolve(@NotNull ResourceSource rawSource, @NotNull String placeholderExpandedSource) {
        if (rawSource.getSourceType() != ResourceSourceType.LOCAL) return placeholderExpandedSource;
        try {
            return resolve(rawSource, placeholderExpandedSource, LocalSourcePathResolver.createForGameAndMinecraftDirectories());
        } catch (IOException | RuntimeException ignored) {
            return null;
        }
    }

    /** Resolves a source with a caller-supplied confined local-path policy. */
    @Nullable
    public static String resolve(@NotNull ResourceSource rawSource, @NotNull String placeholderExpandedSource, @NotNull LocalSourcePathResolver resolver) {
        return (rawSource.getSourceType() == ResourceSourceType.LOCAL) ? resolveLocal(placeholderExpandedSource, resolver) : placeholderExpandedSource;
    }

    @Nullable
    static String resolveLocal(@NotNull String placeholderExpandedSource, @NotNull LocalSourcePathResolver resolver) {
        try {
            LocalSourcePathResolver.ResolvedPath resolvedPath = resolver.resolve(placeholderExpandedSource);
            Path path = resolvedPath.revalidate();
            if (!Files.isRegularFile(path)) return null;
            // Chromium opens the URI after this handoff, so portable Java cannot make validation and Chromium's open
            // atomic. Revalidating at the handoff closes placeholder traversal and ancestor-swap paths as far as the
            // Java boundary can guarantee.
            return resolvedPath.revalidate().toUri().toString();
        } catch (IOException | RuntimeException ignored) {
            return null;
        }
    }
}
