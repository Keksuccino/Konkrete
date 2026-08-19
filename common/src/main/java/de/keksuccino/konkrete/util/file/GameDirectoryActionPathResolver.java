package de.keksuccino.konkrete.util.file;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Resolves paths used by game-directory file actions against one of their two advertised roots.
 *
 * <p>The lexical root is selected first and never changes for the lifetime of this resolver. The root's projected real
 * path is captured at construction time, so a configured root may itself be a symlink, but replacing that symlink with
 * one pointing somewhere else does not silently move the allowed boundary. Every resolved path is also checked through
 * its final entry or nearest existing ancestor, which rejects descendants that escape through symlinks while still
 * allowing safe descendants that do not exist yet.</p>
 *
 * <p>These checks minimize path-swap windows, but callers must still invoke {@link ResolvedPath#revalidate()} immediately
 * before each filesystem mutation. Java's portable path APIs cannot make an ancestor check and a later mutation one
 * atomic operation on every supported filesystem.</p>
 */
public final class GameDirectoryActionPathResolver {

    private static final Pattern WINDOWS_DRIVE_PATH = Pattern.compile("^[A-Za-z]:.*");

    private final RootBoundary gameDirectoryRoot;
    private final RootBoundary minecraftDirectoryRoot;

    private GameDirectoryActionPathResolver(@Nonnull Path gameDirectoryRoot, @Nonnull Path minecraftDirectoryRoot) throws IOException {
        this.gameDirectoryRoot = RootBoundary.capture(AllowedRoot.GAME_DIRECTORY, gameDirectoryRoot);
        this.minecraftDirectoryRoot = RootBoundary.capture(AllowedRoot.DEFAULT_MINECRAFT_DIRECTORY, minecraftDirectoryRoot);
    }

    /**
     * Captures confinement boundaries for the active game and default Minecraft directories.
     *
     * @throws IOException when either root cannot be inspected
     */
    @Nonnull
    public static GameDirectoryActionPathResolver create() throws IOException {
        return new GameDirectoryActionPathResolver(GameDirectoryUtils.getGameDirectory().toPath(), DotMinecraftUtils.getMinecraftDirectory());
    }

    @Nonnull
    static GameDirectoryActionPathResolver create(@Nonnull Path gameDirectoryRoot, @Nonnull Path minecraftDirectoryRoot) throws IOException {
        return new GameDirectoryActionPathResolver(gameDirectoryRoot, minecraftDirectoryRoot);
    }

    /**
     * Resolves action-path syntax against the game root or {@code .minecraft/} root and validates confinement.
     *
     * @throws SecurityException for disallowed drive/UNC paths, prefix spoofing, or a lexical/link escape
     * @throws IOException when the candidate or either root cannot be inspected
     */
    @Nonnull
    public ResolvedPath resolve(@Nonnull String rawPath) throws IOException {
        Objects.requireNonNull(rawPath, "rawPath");
        String portablePath = rawPath.replace('\\', '/');
        if (isMinecraftShorthand(portablePath)) {
            String relativePath = portablePath.equals(".minecraft") ? "" : portablePath.substring(".minecraft/".length());
            return this.resolveAgainstRoot(this.minecraftDirectoryRoot, relativePath);
        }

        Path suppliedPath = Path.of(portablePath);
        if (suppliedPath.isAbsolute()) {
            Path normalizedAbsolutePath = suppliedPath.toAbsolutePath().normalize();
            RootBoundary absoluteRoot = this.findContainingRoot(normalizedAbsolutePath);
            if (absoluteRoot != null) {
                return this.createResolvedPath(absoluteRoot, normalizedAbsolutePath);
            }
            if (hasSiblingPrefix(normalizedAbsolutePath, this.gameDirectoryRoot.resolver.rootPath()) || hasSiblingPrefix(normalizedAbsolutePath, this.minecraftDirectoryRoot.resolver.rootPath())) {
                throw new SecurityException("Absolute path uses an allowed-root string prefix without being inside that root: " + rawPath);
            }
            if (portablePath.startsWith("//")) {
                throw new SecurityException("UNC and network-root paths are not allowed: " + rawPath);
            }
            // A single leading slash is Konkrete's long-standing syntax for a path relative to the instance root.
            portablePath = stripLeadingSlashes(portablePath);
        } else if (portablePath.startsWith("/") && !portablePath.startsWith("//")) {
            // Some Windows providers do not classify Konkrete's virtual-root syntax as host-absolute.
            portablePath = stripLeadingSlashes(portablePath);
        } else if (WINDOWS_DRIVE_PATH.matcher(portablePath).matches() || portablePath.startsWith("//")) {
            // Reject foreign-platform absolute/drive-relative forms instead of interpreting them differently per host OS.
            throw new SecurityException("Drive-qualified and UNC paths are not allowed outside an advertised root: " + rawPath);
        }
        return this.resolveAgainstRoot(this.gameDirectoryRoot, portablePath);
    }

    private ResolvedPath resolveAgainstRoot(RootBoundary root, String relativePath) throws IOException {
        Path candidate = root.resolver.rootPath().resolve(relativePath).toAbsolutePath().normalize();
        return this.createResolvedPath(root, candidate);
    }

    private ResolvedPath createResolvedPath(RootBoundary root, Path candidate) throws IOException {
        return new ResolvedPath(root, root.resolver.resolve(candidate));
    }

    private RootBoundary findContainingRoot(Path absolutePath) {
        if (absolutePath.startsWith(this.gameDirectoryRoot.resolver.rootPath())) {
            return this.gameDirectoryRoot;
        }
        if (absolutePath.startsWith(this.minecraftDirectoryRoot.resolver.rootPath())) {
            return this.minecraftDirectoryRoot;
        }
        return null;
    }

    private static boolean isMinecraftShorthand(String path) {
        return path.equals(".minecraft") || path.startsWith(".minecraft/");
    }

    private static boolean hasSiblingPrefix(Path path, Path root) {
        String pathString = path.toString();
        String rootString = root.toString();
        return pathString.startsWith(rootString) && !path.startsWith(root);
    }

    private static String stripLeadingSlashes(String path) {
        int firstNonSlash = 0;
        while ((firstNonSlash < path.length()) && (path.charAt(firstNonSlash) == '/')) {
            firstNonSlash++;
        }
        return path.substring(firstNonSlash);
    }

    /** Identifies a permitted path root. */
    public enum AllowedRoot {
        /** The active game instance directory. */
        GAME_DIRECTORY,
        /** The user's default Minecraft directory. */
        DEFAULT_MINECRAFT_DIRECTORY
    }

    /** A validated path paired with its permitted root. */
    public final class ResolvedPath {

        private final RootBoundary root;
        private final ConfinedPathResolver.ResolvedPath confinedPath;

        private ResolvedPath(RootBoundary root, ConfinedPathResolver.ResolvedPath confinedPath) {
            this.root = root;
            this.confinedPath = confinedPath;
        }

        /** Returns the normalized candidate; call {@link #revalidate()} immediately before mutation. */
        @Nonnull
        public Path path() {
            return this.confinedPath.path();
        }

        /** Returns the normalized root that confines this action path. */
        @Nonnull
        public Path rootPath() {
            return this.root.resolver.rootPath();
        }

        /** Identifies whether the game or default Minecraft directory owns this path. */
        @Nonnull
        public AllowedRoot allowedRoot() {
            return this.root.allowedRoot;
        }

        /** Returns whether this action targets its allowed root itself. */
        public boolean isRoot() {
            return this.confinedPath.isRoot();
        }

        /** Returns this path when it is a descendant, throwing {@link SecurityException} for the root itself. */
        @Nonnull
        public ResolvedPath requireDescendant() {
            this.confinedPath.requireDescendant();
            return this;
        }

        /** Resolves and validates a non-empty relative descendant; rejects absolute or escaping syntax. */
        @Nonnull
        public ResolvedPath resolveRelativeChild(@Nonnull String relativePath) throws IOException {
            return new ResolvedPath(this.root, this.confinedPath.resolveRelativeChild(relativePath));
        }

        /** Resolves and validates one non-dot filename below this path. */
        @Nonnull
        public ResolvedPath resolveSingleComponentChild(@Nonnull String fileName) throws IOException {
            return new ResolvedPath(this.root, this.confinedPath.resolveSingleComponentChild(fileName));
        }

        /** Resolves and validates one non-dot filename beside this path. */
        @Nonnull
        public ResolvedPath resolveSingleComponentSibling(@Nonnull String fileName) throws IOException {
            return new ResolvedPath(this.root, this.confinedPath.resolveSingleComponentSibling(fileName));
        }

        /**
         * Rechecks root identity and symbolic-link confinement immediately before filesystem mutation.
         *
         * @throws SecurityException when the boundary changed or the path now escapes it
         * @throws IOException when current filesystem entries cannot be inspected
         */
        @Nonnull
        public Path revalidate() throws IOException {
            return this.confinedPath.revalidate();
        }
    }

    private record RootBoundary(AllowedRoot allowedRoot, ConfinedPathResolver resolver) {

        private static RootBoundary capture(AllowedRoot allowedRoot, Path path) throws IOException {
            return new RootBoundary(allowedRoot, ConfinedPathResolver.create(path));
        }
    }
}
