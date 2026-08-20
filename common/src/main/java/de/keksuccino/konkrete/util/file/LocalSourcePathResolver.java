package de.keksuccino.konkrete.util.file;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Resolves read-only local-source syntax without allowing it to leave its advertised roots.
 *
 * <p>Ordinary relative paths are rooted at the active game instance. A single leading slash is retained as Konkrete's
 * long-standing instance-root spelling, while absolute paths already inside an allowed root retain their host-absolute
 * meaning. The default Minecraft directory is available only to consumers that explicitly opt into the documented
 * {@code .minecraft/...} shorthand.</p>
 */
public final class LocalSourcePathResolver {

    private static final Pattern WINDOWS_DRIVE_PATH = Pattern.compile("^[A-Za-z]:.*");
    private static final Object CACHE_LOCK = new Object();
    private static volatile CachedResolver gameDirectoryCache;
    private static volatile CachedResolver gameAndMinecraftDirectoriesCache;

    private final RootBoundary gameDirectoryRoot;
    private final RootBoundary minecraftDirectoryRoot;

    private LocalSourcePathResolver(@NotNull Path gameDirectoryRoot, Path minecraftDirectoryRoot) throws IOException {
        this.gameDirectoryRoot = RootBoundary.capture(AllowedRoot.GAME_DIRECTORY, gameDirectoryRoot);
        this.minecraftDirectoryRoot = (minecraftDirectoryRoot != null) ? RootBoundary.capture(AllowedRoot.DEFAULT_MINECRAFT_DIRECTORY, minecraftDirectoryRoot) : null;
    }

    /** Returns a cached resolver confined to the active game directory, recapturing it when the root changes. */
    @NotNull
    public static LocalSourcePathResolver createForGameDirectory() throws IOException {
        Path gameDirectoryRoot = normalizeRoot(GameDirectoryUtils.getGameDirectory().toPath());
        CachedResolver cached = gameDirectoryCache;
        if ((cached != null) && cached.matches(gameDirectoryRoot, null)) return cached.resolver;
        synchronized (CACHE_LOCK) {
            cached = gameDirectoryCache;
            if ((cached == null) || !cached.matches(gameDirectoryRoot, null)) {
                cached = new CachedResolver(gameDirectoryRoot, null, new LocalSourcePathResolver(gameDirectoryRoot, null));
                gameDirectoryCache = cached;
            }
            return cached.resolver;
        }
    }

    /** Returns a cached resolver for the active game directory plus the default Minecraft-directory shorthand. */
    @NotNull
    public static LocalSourcePathResolver createForGameAndMinecraftDirectories() throws IOException {
        Path gameDirectoryRoot = normalizeRoot(GameDirectoryUtils.getGameDirectory().toPath());
        Path minecraftDirectoryRoot = normalizeRoot(DotMinecraftUtils.getMinecraftDirectory());
        CachedResolver cached = gameAndMinecraftDirectoriesCache;
        if ((cached != null) && cached.matches(gameDirectoryRoot, minecraftDirectoryRoot)) return cached.resolver;
        synchronized (CACHE_LOCK) {
            cached = gameAndMinecraftDirectoriesCache;
            if ((cached == null) || !cached.matches(gameDirectoryRoot, minecraftDirectoryRoot)) {
                cached = new CachedResolver(gameDirectoryRoot, minecraftDirectoryRoot, new LocalSourcePathResolver(gameDirectoryRoot, minecraftDirectoryRoot));
                gameAndMinecraftDirectoriesCache = cached;
            }
            return cached.resolver;
        }
    }

    /** Captures a resolver confined to the supplied game directory. */
    @NotNull
    public static LocalSourcePathResolver createForGameDirectory(@NotNull Path gameDirectoryRoot) throws IOException {
        return new LocalSourcePathResolver(gameDirectoryRoot, null);
    }

    /** Captures independent boundaries for the supplied game and default Minecraft directories. */
    @NotNull
    public static LocalSourcePathResolver createForGameAndMinecraftDirectories(@NotNull Path gameDirectoryRoot, @NotNull Path minecraftDirectoryRoot) throws IOException {
        return new LocalSourcePathResolver(gameDirectoryRoot, minecraftDirectoryRoot);
    }

    /**
     * Interprets local-source syntax and validates the result against its advertised root.
     * Relative and single-leading-slash inputs use the game directory; {@code .minecraft/} uses the optional
     * default-Minecraft root. Returned paths must be {@link ResolvedPath#revalidate() revalidated} before I/O.
     *
     * @throws SecurityException for parent traversal, disallowed drive/UNC paths, root-prefix spoofing, or link escape
     * @throws IOException when the root or candidate cannot be inspected
     */
    @NotNull
    public ResolvedPath resolve(@NotNull String rawPath) throws IOException {
        Objects.requireNonNull(rawPath, "rawPath");
        String portablePath = rawPath.replace('\\', '/');
        if (containsParentTraversal(portablePath)) {
            throw new SecurityException("Local source contains parent traversal: " + rawPath);
        }
        if ((this.minecraftDirectoryRoot != null) && isMinecraftShorthand(portablePath)) {
            String relativePath = portablePath.equals(".minecraft") ? "" : portablePath.substring(".minecraft/".length());
            return this.resolveRelative(this.minecraftDirectoryRoot, relativePath);
        }

        Path suppliedPath = Path.of(portablePath);
        if (suppliedPath.isAbsolute()) {
            Path normalizedAbsolutePath = suppliedPath.toAbsolutePath().normalize();
            RootBoundary containingRoot = this.findContainingRoot(normalizedAbsolutePath);
            if (containingRoot != null) {
                return this.resolveAbsolute(containingRoot, normalizedAbsolutePath);
            }
            if (hasSiblingPrefix(normalizedAbsolutePath, this.gameDirectoryRoot.resolver.rootPath()) || ((this.minecraftDirectoryRoot != null) && hasSiblingPrefix(normalizedAbsolutePath, this.minecraftDirectoryRoot.resolver.rootPath()))) {
                throw new SecurityException("Absolute local source uses an allowed-root string prefix without being inside that root: " + rawPath);
            }
            if (portablePath.startsWith("//")) {
                throw new SecurityException("UNC and network-root local sources are outside their allowed roots: " + rawPath);
            }
            return this.resolveRelative(this.gameDirectoryRoot, stripLeadingSlashes(portablePath));
        }
        if (WINDOWS_DRIVE_PATH.matcher(portablePath).matches() || portablePath.startsWith("//")) {
            throw new SecurityException("Drive-qualified and UNC local sources are outside their allowed roots: " + rawPath);
        }
        if (portablePath.startsWith("/")) {
            return this.resolveRelative(this.gameDirectoryRoot, stripLeadingSlashes(portablePath));
        }
        return this.resolveRelative(this.gameDirectoryRoot, portablePath);
    }

    private ResolvedPath resolveRelative(RootBoundary root, String relativePath) throws IOException {
        return this.resolveAbsolute(root, root.resolver.rootPath().resolve(relativePath));
    }

    private ResolvedPath resolveAbsolute(RootBoundary root, Path path) throws IOException {
        return new ResolvedPath(root, root.resolver.resolve(path));
    }

    private RootBoundary findContainingRoot(Path path) {
        if (path.startsWith(this.gameDirectoryRoot.resolver.rootPath())) {
            return this.gameDirectoryRoot;
        }
        if ((this.minecraftDirectoryRoot != null) && path.startsWith(this.minecraftDirectoryRoot.resolver.rootPath())) {
            return this.minecraftDirectoryRoot;
        }
        return null;
    }

    private static boolean isMinecraftShorthand(String path) {
        return path.equals(".minecraft") || path.startsWith(".minecraft/");
    }

    private static boolean containsParentTraversal(String path) {
        for (String component : path.split("/", -1)) {
            if (component.equals("..")) return true;
        }
        return false;
    }

    private static boolean hasSiblingPrefix(Path path, Path root) {
        return path.toString().startsWith(root.toString()) && !path.startsWith(root);
    }

    private static String stripLeadingSlashes(String path) {
        int firstNonSlash = 0;
        while ((firstNonSlash < path.length()) && (path.charAt(firstNonSlash) == '/')) {
            firstNonSlash++;
        }
        return path.substring(firstNonSlash);
    }

    private static Path normalizeRoot(Path root) {
        return root.toAbsolutePath().normalize();
    }

    /** Identifies a permitted path root. */
    public enum AllowedRoot {

        /** The active game instance directory. */
        GAME_DIRECTORY,
        /** The user's default Minecraft directory. */
        DEFAULT_MINECRAFT_DIRECTORY

    }

    /** A validated path paired with its permitted root. */
    public static final class ResolvedPath {

        private final RootBoundary root;
        private final ConfinedPathResolver.ResolvedPath confinedPath;

        private ResolvedPath(RootBoundary root, ConfinedPathResolver.ResolvedPath confinedPath) {
            this.root = root;
            this.confinedPath = confinedPath;
        }

        /** Returns the normalized candidate; call {@link #revalidate()} immediately before I/O. */
        @NotNull
        public Path path() {
            return this.confinedPath.path();
        }

        /** Returns the normalized root that confines this path. */
        @NotNull
        public Path rootPath() {
            return this.root.resolver.rootPath();
        }

        /** Identifies whether the game or default Minecraft directory owns this path. */
        @NotNull
        public AllowedRoot allowedRoot() {
            return this.root.allowedRoot;
        }

        /**
         * Rechecks symbolic-link and root identity confinement immediately before filesystem access.
         *
         * @throws SecurityException when the captured boundary changed or the path now escapes it
         * @throws IOException when current filesystem entries cannot be inspected
         */
        @NotNull
        public Path revalidate() throws IOException {
            return this.confinedPath.revalidate();
        }

    }

    private record RootBoundary(AllowedRoot allowedRoot, ConfinedPathResolver resolver) {

        private static RootBoundary capture(AllowedRoot allowedRoot, Path path) throws IOException {
            return new RootBoundary(allowedRoot, ConfinedPathResolver.create(path));
        }

    }

    private record CachedResolver(Path gameDirectoryRoot, Path minecraftDirectoryRoot, LocalSourcePathResolver resolver) {

        private boolean matches(Path gameDirectoryRoot, Path minecraftDirectoryRoot) {
            return this.gameDirectoryRoot.equals(gameDirectoryRoot) && Objects.equals(this.minecraftDirectoryRoot, minecraftDirectoryRoot);
        }

    }

}
