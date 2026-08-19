package de.keksuccino.konkrete.util.ffmpeg.downloader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.keksuccino.konkrete.util.ffmpeg.downloader.FFMPEGDownloadConfiguration.Artifact;
import de.keksuccino.konkrete.util.ffmpeg.downloader.FFMPEGDownloadConfiguration.Distribution;
import de.keksuccino.konkrete.util.ffmpeg.downloader.FFMPEGDownloadConfiguration.Platform;
import de.keksuccino.konkrete.util.ffmpeg.downloader.FFMPEGDownloadConfiguration.PlatformFamily;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Thread-safe, instance-scoped FFmpeg installer with caller-owned storage, checksummed artifacts and explicit close/cancellation ownership.
 */
public final class FFMPEGDownloader implements AutoCloseable {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    private static final int METADATA_SCHEMA_VERSION = 1;

    private final FFMPEGDownloadConfiguration configuration;
    private final ExecutorService executor;
    private final boolean ownsExecutor;
    private final Path currentDirectory;
    private final Path tempDirectory;
    private final Object operationLock = new Object();
    private final AtomicReference<FFMPEGDownloadSnapshot> snapshot = new AtomicReference<>(FFMPEGDownloadSnapshot.idle());
    private final List<Consumer<FFMPEGDownloadSnapshot>> snapshotListeners = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final AtomicBoolean closed = new AtomicBoolean();
    @Nullable private volatile DownloadOperation currentOperation;
    @Nullable private volatile CompletableFuture<FFMPEGInstallation> currentFuture;

    /**
     * Creates a downloader with a dedicated daemon worker.
     *
     * @param configuration downloader configuration
     */
    public FFMPEGDownloader(@NotNull FFMPEGDownloadConfiguration configuration) {
        this(configuration, Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "Konkrete-FFmpeg-Downloader");
            thread.setDaemon(true);
            return thread;
        }), true);
    }

    /**
     * Creates a downloader using a caller-owned executor.
     *
     * @param configuration downloader configuration
     * @param executor worker executor; it is not closed by this downloader
     */
    public FFMPEGDownloader(@NotNull FFMPEGDownloadConfiguration configuration, @NotNull ExecutorService executor) {
        this(configuration, executor, false);
    }

    private FFMPEGDownloader(FFMPEGDownloadConfiguration configuration, ExecutorService executor, boolean ownsExecutor) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.executor = Objects.requireNonNull(executor, "executor");
        this.ownsExecutor = ownsExecutor;
        this.currentDirectory = configuration.storageDirectory().resolve("current");
        this.tempDirectory = configuration.storageDirectory().resolve("temp");
    }

    /** Returns the isolated downloader storage root. */
    @NotNull
    public java.io.File getDownloadDirectory() {
        return this.configuration.storageDirectory().toFile();
    }

    /** Returns the latest immutable operation snapshot. */
    @NotNull
    public FFMPEGDownloadSnapshot getSnapshot() {
        return this.snapshot.get();
    }

    /**
     * Registers a snapshot listener and immediately supplies the current snapshot.
     *
     * The initial callback runs synchronously on the registering thread; later callbacks run synchronously on the thread publishing each change.
     *
     * @param listener listener to notify; a failing initial callback is not retained
     * @return closeable subscription
     */
    @NotNull
    public SnapshotSubscription addSnapshotListener(@NotNull Consumer<FFMPEGDownloadSnapshot> listener) {
        Consumer<FFMPEGDownloadSnapshot> checkedListener = Objects.requireNonNull(listener, "listener");
        this.snapshotListeners.add(checkedListener);
        try {
            checkedListener.accept(this.snapshot.get());
        } catch (RuntimeException exception) {
            this.snapshotListeners.remove(checkedListener);
            throw exception;
        }
        return new SnapshotSubscription(() -> this.snapshotListeners.remove(checkedListener));
    }

    /** Returns whether an installation operation is active. */
    public boolean isDownloadRunning() {
        return this.snapshot.get().isActive();
    }

    /** Returns whether no valid installation is cached for the current platform. */
    public boolean isDownloadNeeded() {
        return this.getCachedInstallation() == null;
    }

    /** Returns a validated cached installation for the current platform. */
    @Nullable
    public FFMPEGInstallation getCachedInstallation() {
        try {
            return this.loadCurrentInstallation(detectPlatform());
        } catch (RuntimeException exception) {
            LOGGER.debug("[KONKRETE] Cached FFmpeg installation is unavailable", exception);
            return null;
        }
    }

    /** Returns the cached FFmpeg binary. */
    @Nullable
    public java.io.File getInstalledFfmpegBinary() {
        FFMPEGInstallation installation = this.getCachedInstallation();
        return installation == null ? null : installation.getFfmpegBinary();
    }

    /** Returns the cached ffprobe binary. */
    @Nullable
    public java.io.File getInstalledFfprobeBinary() {
        FFMPEGInstallation installation = this.getCachedInstallation();
        return installation == null ? null : installation.getFfprobeBinary();
    }

    /** Returns the cached platform installation directory. */
    @Nullable
    public java.io.File getInstalledDirectory() {
        FFMPEGInstallation installation = this.getCachedInstallation();
        return installation == null ? null : installation.getInstallDirectory();
    }

    /** Requests cooperative cancellation of the current transfer or extraction. */
    public void cancelCurrentDownload() {
        DownloadOperation operation = this.currentOperation;
        if (operation != null) operation.cancelled.set(true);
    }

    /** Ensures an installation exists, sharing an already active operation. */
    @NotNull
    public CompletableFuture<FFMPEGInstallation> ensureInstalledAsync() {
        return this.startDownloadIfNeededAsync(false);
    }

    /** Ensures an installation exists, sharing an already active operation. */
    @NotNull
    public CompletableFuture<FFMPEGInstallation> startDownloadIfNeededAsync() {
        return this.startDownloadIfNeededAsync(false);
    }

    /**
     * Starts or shares an asynchronous installation operation.
     *
     * @param forceRedownload whether a valid cache should be replaced
     * @return shared operation future
     */
    @NotNull
    public CompletableFuture<FFMPEGInstallation> startDownloadIfNeededAsync(boolean forceRedownload) {
        if (this.closed.get()) return CompletableFuture.failedFuture(new IllegalStateException("FFMPEGDownloader is closed"));
        synchronized (this.operationLock) {
            if (this.closed.get()) return CompletableFuture.failedFuture(new IllegalStateException("FFMPEGDownloader is closed"));
            if (!forceRedownload) {
                FFMPEGInstallation cached = this.getCachedInstallation();
                if (cached != null) {
                    this.publish(completeSnapshot(cached, true));
                    return CompletableFuture.completedFuture(cached);
                }
            }
            CompletableFuture<FFMPEGInstallation> activeFuture = this.currentFuture;
            if (activeFuture != null && !activeFuture.isDone()) return activeFuture;
            DownloadOperation operation = new DownloadOperation();
            CompletableFuture<FFMPEGInstallation> future = new CompletableFuture<>();
            this.currentOperation = operation;
            this.currentFuture = future;
            future.whenComplete((result, throwable) -> {
                synchronized (this.operationLock) {
                    if (this.currentOperation == operation) this.currentOperation = null;
                    if (this.currentFuture == future) this.currentFuture = null;
                }
            });
            this.publish(new FFMPEGDownloadSnapshot(FFMPEGDownloadSnapshot.Stage.CHECKING, "Checking FFmpeg installation", null, 0.02D, 0L, 0L, null, null, false));
            if (this.closed.get()) return future;
            try {
                this.executor.execute(() -> {
                    if (future.isCancelled()) return;
                    try {
                        future.complete(this.install(operation, forceRedownload));
                    } catch (CancellationException exception) {
                        future.completeExceptionally(exception);
                    } catch (Exception exception) {
                        future.completeExceptionally(new java.util.concurrent.CompletionException(exception));
                    }
                });
            } catch (RuntimeException exception) {
                future.completeExceptionally(exception);
                this.publish(new FFMPEGDownloadSnapshot(FFMPEGDownloadSnapshot.Stage.FAILED, "FFmpeg download could not start", exception.getMessage(), 0.0D, 0L, 0L, exception.getMessage(), null, false));
            }
            return future;
        }
    }

    /**
     * Cancels active work, completes its exposed future by cancellation, removes listeners and closes the dedicated executor when owned.
     */
    @Override
    public void close() {
        if (!this.closed.compareAndSet(false, true)) return;
        synchronized (this.operationLock) {
            DownloadOperation activeOperation = this.currentOperation;
            CompletableFuture<FFMPEGInstallation> activeFuture = this.currentFuture;
            boolean operationActive = activeOperation != null || activeFuture != null && !activeFuture.isDone();
            if (activeOperation != null) activeOperation.cancelled.set(true);
            if (activeFuture != null && !activeFuture.isDone()) {
                activeFuture.cancel(true);
            }
            if (operationActive) this.publish(new FFMPEGDownloadSnapshot(FFMPEGDownloadSnapshot.Stage.CANCELLED, "FFmpeg downloader closed", null, 0.0D, 0L, 0L, "Downloader closed", null, false));
        }
        this.snapshotListeners.clear();
        if (this.ownsExecutor) this.executor.shutdownNow();
    }

    /** Detects and normalizes the current OS and architecture. */
    @NotNull
    public static Platform detectPlatform() {
        return detectPlatform(System.getProperty("os.name", ""), System.getProperty("os.arch", ""));
    }

    /**
     * Normalizes explicit OS and architecture strings.
     *
     * @param osName operating-system name
     * @param architecture architecture name
     * @return normalized platform
     */
    @NotNull
    public static Platform detectPlatform(@NotNull String osName, @NotNull String architecture) {
        String normalizedOs = Objects.requireNonNull(osName, "osName").toLowerCase(Locale.ROOT);
        String normalizedArchitecture = Objects.requireNonNull(architecture, "architecture").toLowerCase(Locale.ROOT);
        PlatformFamily family;
        String osSegment;
        if (normalizedOs.contains("win")) {
            family = PlatformFamily.WINDOWS;
            osSegment = "windows";
        } else if (normalizedOs.contains("mac") || normalizedOs.contains("darwin")) {
            family = PlatformFamily.MACOS;
            osSegment = "macos";
        } else if (normalizedOs.contains("linux")) {
            family = PlatformFamily.LINUX;
            osSegment = "linux";
        } else {
            throw new IllegalStateException("Unsupported operating system: " + normalizedOs);
        }
        String architectureSegment;
        String displayArchitecture;
        if (normalizedArchitecture.equals("amd64") || normalizedArchitecture.equals("x86_64") || normalizedArchitecture.equals("x64")) {
            architectureSegment = "amd64";
            displayArchitecture = "x64";
        } else if (normalizedArchitecture.equals("aarch64") || normalizedArchitecture.equals("arm64")) {
            architectureSegment = "arm64";
            displayArchitecture = "arm64";
        } else {
            throw new IllegalStateException("Unsupported system architecture: " + normalizedArchitecture);
        }
        if (family == PlatformFamily.WINDOWS && !architectureSegment.equals("amd64")) throw new IllegalStateException("Windows FFmpeg download is currently supported only on x64");
        String displayName = switch (family) {
            case WINDOWS -> "Windows " + displayArchitecture;
            case LINUX -> "Linux " + displayArchitecture;
            case MACOS -> "macOS " + displayArchitecture;
        };
        return new Platform(family, osSegment, architectureSegment, family == PlatformFamily.WINDOWS ? ".exe" : "", osSegment + '-' + architectureSegment, displayName);
    }

    /**
     * Resolves Konkrete's default public binary providers for a supported platform.
     *
     * @param platform normalized platform
     * @return default distribution
     */
    @NotNull
    public static Distribution resolveDefaultDistribution(@NotNull Platform platform) {
        Objects.requireNonNull(platform, "platform");
        if (platform.family() == PlatformFamily.WINDOWS && platform.architectureSegment().equals("amd64")) {
            return new Distribution("gyan", "Gyan.dev", List.of(new Artifact("bundle", "FFmpeg bundle", "https://www.gyan.dev/ffmpeg/builds/ffmpeg-release-essentials.zip")));
        }
        if (platform.family() == PlatformFamily.LINUX || platform.family() == PlatformFamily.MACOS) {
            String base = "https://ffmpeg.martin-riedl.de/redirect/latest/" + platform.osSegment() + '/' + platform.architectureSegment() + "/release";
            return new Distribution("martin-riedl", "Martin Riedl", List.of(new Artifact("ffmpeg", "ffmpeg", base + "/ffmpeg.zip"), new Artifact("ffprobe", "ffprobe", base + "/ffprobe.zip")));
        }
        throw new IllegalStateException("No default FFmpeg provider is configured for " + platform.displayName());
    }

    private FFMPEGInstallation install(DownloadOperation operation, boolean forceRedownload) throws Exception {
        Platform platform = detectPlatform();
        Distribution distribution = this.configuration.distributionResolver().resolve(platform);
        Files.createDirectories(this.tempDirectory);
        Path tempRoot = Files.createTempDirectory(this.tempDirectory, platform.id() + '_');
        Path stagingDirectory = Files.createDirectory(tempRoot.resolve("install"));
        Path downloadsDirectory = Files.createDirectory(tempRoot.resolve("downloads"));
        try {
            checkCancelled(operation);
            if (!forceRedownload) {
                FFMPEGInstallation cached = this.loadCurrentInstallation(platform);
                if (cached != null) {
                    this.publish(completeSnapshot(cached, true));
                    return cached;
                }
            }
            this.publish(new FFMPEGDownloadSnapshot(FFMPEGDownloadSnapshot.Stage.CHECKING, "Resolving FFmpeg download sources", platform.displayName() + " | " + distribution.providerDisplayName(), 0.05D, 0L, 0L, null, null, false));
            List<ResolvedArtifact> artifacts = this.resolveArtifacts(operation, distribution);
            long totalDownloadBytes = artifacts.stream().mapToLong(artifact -> Math.max(0L, artifact.remote().contentLength())).sum();
            List<Path> archives = new ArrayList<>();
            long downloadedBytes = 0L;
            for (ResolvedArtifact artifact : artifacts) {
                Path archive = downloadsDirectory.resolve(artifact.artifact().id() + ".zip");
                downloadedBytes += this.downloadArtifact(operation, artifact, archive, downloadedBytes, totalDownloadBytes);
                archives.add(archive);
            }
            long totalExtractBytes = 0L;
            for (Path archive : archives) totalExtractBytes += zipUncompressedSize(archive);
            long extractedBytes = 0L;
            for (int index = 0; index < archives.size(); index++) extractedBytes += this.extractZip(operation, archives.get(index), artifacts.get(index), stagingDirectory, extractedBytes, totalExtractBytes);
            this.publish(new FFMPEGDownloadSnapshot(FFMPEGDownloadSnapshot.Stage.VALIDATING, "Validating downloaded FFmpeg binaries", platform.displayName() + " | " + distribution.providerDisplayName(), 0.93D, 0L, 0L, null, null, false));
            Path ffmpegBinary = findBinary(stagingDirectory, "ffmpeg" + platform.binarySuffix());
            Path ffprobeBinary = findBinary(stagingDirectory, "ffprobe" + platform.binarySuffix());
            if (ffmpegBinary == null || ffprobeBinary == null) throw new IOException("Downloaded archives did not contain both ffmpeg and ffprobe binaries");
            ensureExecutable(ffmpegBinary, platform);
            ensureExecutable(ffprobeBinary, platform);
            InstallationMetadata metadata = new InstallationMetadata();
            metadata.schemaVersion = METADATA_SCHEMA_VERSION;
            metadata.platformId = platform.id();
            metadata.providerId = distribution.providerId();
            metadata.ffmpegRelativePath = relativePath(stagingDirectory, ffmpegBinary);
            metadata.ffprobeRelativePath = relativePath(stagingDirectory, ffprobeBinary);
            metadata.ffmpegVersionLine = validateBinary(ffmpegBinary, "ffmpeg", this.configuration.validationTimeout());
            metadata.ffprobeVersionLine = validateBinary(ffprobeBinary, "ffprobe", this.configuration.validationTimeout());
            writeMetadata(stagingDirectory.resolve("installation.json"), metadata);
            FFMPEGInstallation installation = this.activateInstall(platform, stagingDirectory);
            this.publish(completeSnapshot(installation, false));
            return installation;
        } catch (CancellationException exception) {
            this.publish(new FFMPEGDownloadSnapshot(FFMPEGDownloadSnapshot.Stage.CANCELLED, "FFmpeg download cancelled", null, 0.0D, 0L, 0L, "Cancelled", null, false));
            throw exception;
        } catch (Exception exception) {
            String message = exception.getMessage() == null || exception.getMessage().isBlank() ? exception.getClass().getSimpleName() : exception.getMessage();
            this.publish(new FFMPEGDownloadSnapshot(FFMPEGDownloadSnapshot.Stage.FAILED, "FFmpeg download failed", message, 0.0D, 0L, 0L, message, null, false));
            LOGGER.error("[KONKRETE] FFmpeg download failed", exception);
            throw exception;
        } finally {
            deleteRecursively(tempRoot);
        }
    }

    private List<ResolvedArtifact> resolveArtifacts(DownloadOperation operation, Distribution distribution) throws Exception {
        List<ResolvedArtifact> result = new ArrayList<>();
        for (Artifact artifact : distribution.artifacts()) {
            checkCancelled(operation);
            ResolvedRemote remote = this.resolveRemote(artifact.archiveUrl());
            String checksumUrl = remote.url() + ".sha256";
            String checksum = parseSha256(this.downloadText(checksumUrl));
            result.add(new ResolvedArtifact(artifact, remote, checksum));
        }
        return result;
    }

    private long downloadArtifact(DownloadOperation operation, ResolvedArtifact artifact, Path target, long downloadedBefore, long totalBytes) throws Exception {
        HttpURLConnection connection = this.openConnection(artifact.remote().url(), "GET", true);
        try {
            requireSuccess(connection, "download " + artifact.artifact().displayName());
            MessageDigest digest = sha256Digest();
            long downloaded = 0L;
            try (InputStream input = new BufferedInputStream(connection.getInputStream()); OutputStream output = Files.newOutputStream(target)) {
                byte[] buffer = new byte[65_536];
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    checkCancelled(operation);
                    output.write(buffer, 0, read);
                    digest.update(buffer, 0, read);
                    downloaded += read;
                    long current = downloadedBefore + downloaded;
                    this.publish(new FFMPEGDownloadSnapshot(FFMPEGDownloadSnapshot.Stage.DOWNLOADING, "Downloading " + artifact.artifact().displayName(), formatBytes(current) + " / " + formatBytes(totalBytes), rangeProgress(0.10D, 0.65D, current, totalBytes), current, totalBytes, null, null, false));
                }
            }
            if (!toHex(digest.digest()).equalsIgnoreCase(artifact.sha256())) throw new IOException("Checksum verification failed for " + artifact.artifact().displayName());
            return downloaded;
        } finally {
            connection.disconnect();
        }
    }

    private long extractZip(DownloadOperation operation, Path archive, ResolvedArtifact artifact, Path targetRoot, long extractedBefore, long totalBytes) throws Exception {
        long extracted = 0L;
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            byte[] buffer = new byte[65_536];
            while (entries.hasMoreElements()) {
                checkCancelled(operation);
                ZipEntry entry = entries.nextElement();
                Path target = targetRoot.resolve(entry.getName()).normalize();
                if (!target.startsWith(targetRoot)) throw new IOException("Blocked unsafe ZIP entry: " + entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                    continue;
                }
                Path parent = target.getParent();
                if (parent != null) Files.createDirectories(parent);
                try (InputStream input = new BufferedInputStream(zip.getInputStream(entry)); OutputStream output = Files.newOutputStream(target)) {
                    int read;
                    while ((read = input.read(buffer)) >= 0) {
                        checkCancelled(operation);
                        output.write(buffer, 0, read);
                        extracted += read;
                        long current = extractedBefore + extracted;
                        this.publish(new FFMPEGDownloadSnapshot(FFMPEGDownloadSnapshot.Stage.EXTRACTING, "Extracting " + artifact.artifact().displayName(), entry.getName(), rangeProgress(0.65D, 0.90D, current, totalBytes), current, totalBytes, null, null, false));
                    }
                }
            }
        }
        return extracted;
    }

    private FFMPEGInstallation activateInstall(Platform platform, Path stagingDirectory) throws IOException {
        Path platformDirectory = this.currentDirectory.resolve(platform.id());
        Files.createDirectories(this.currentDirectory);
        Path backupDirectory = this.currentDirectory.resolve(platform.id() + ".backup");
        deleteRecursively(backupDirectory);
        boolean backupCreated = false;
        try {
            if (Files.exists(platformDirectory)) {
                moveDirectory(platformDirectory, backupDirectory);
                backupCreated = true;
            }
            moveDirectory(stagingDirectory, platformDirectory);
            FFMPEGInstallation installation = Objects.requireNonNull(this.loadCurrentInstallation(platform), "activated installation failed validation");
            deleteRecursively(backupDirectory);
            return installation;
        } catch (IOException | RuntimeException exception) {
            deleteRecursively(platformDirectory);
            if (backupCreated && Files.exists(backupDirectory)) {
                try {
                    moveDirectory(backupDirectory, platformDirectory);
                } catch (IOException restoreException) {
                    exception.addSuppressed(restoreException);
                }
            }
            throw exception;
        }
    }

    @Nullable
    private FFMPEGInstallation loadCurrentInstallation(Platform platform) {
        Path platformDirectory = this.currentDirectory.resolve(platform.id()).normalize();
        Path metadataFile = platformDirectory.resolve("installation.json");
        if (!Files.isRegularFile(metadataFile)) return null;
        try (BufferedReader reader = Files.newBufferedReader(metadataFile, StandardCharsets.UTF_8)) {
            InstallationMetadata metadata = GSON.fromJson(reader, InstallationMetadata.class);
            if (metadata == null || metadata.schemaVersion != METADATA_SCHEMA_VERSION || !platform.id().equals(metadata.platformId)) return null;
            Path ffmpegBinary = safeResolve(platformDirectory, metadata.ffmpegRelativePath);
            Path ffprobeBinary = safeResolve(platformDirectory, metadata.ffprobeRelativePath);
            FFMPEGInstallation installation = new FFMPEGInstallation(platformDirectory.toFile(), ffmpegBinary.toFile(), ffprobeBinary.toFile(), metadata.platformId, Objects.requireNonNullElse(metadata.providerId, "unknown"), Objects.requireNonNullElse(metadata.ffmpegVersionLine, ""), Objects.requireNonNullElse(metadata.ffprobeVersionLine, ""));
            return installation.isValid() ? installation : null;
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("[KONKRETE] Failed to read cached FFmpeg installation metadata", exception);
            return null;
        }
    }

    private ResolvedRemote resolveRemote(String sourceUrl) throws IOException {
        URL current = URI.create(sourceUrl).toURL();
        for (int redirect = 0; redirect < 8; redirect++) {
            HttpURLConnection connection = (HttpURLConnection)current.openConnection();
            this.configureConnection(connection, "HEAD", false);
            int responseCode;
            try {
                responseCode = connection.getResponseCode();
            } catch (IOException exception) {
                connection.disconnect();
                return this.resolveRemoteViaGet(current.toString());
            }
            if (isRedirect(responseCode)) {
                String location = connection.getHeaderField("Location");
                connection.disconnect();
                if (location == null || location.isBlank()) throw new IOException("Redirect response omitted Location");
                current = new URL(current, location);
                continue;
            }
            if (responseCode >= 200 && responseCode < 300) {
                long contentLength = connection.getContentLengthLong();
                connection.disconnect();
                return new ResolvedRemote(current.toString(), contentLength);
            }
            connection.disconnect();
            return this.resolveRemoteViaGet(current.toString());
        }
        throw new IOException("Too many redirects while resolving FFmpeg download");
    }

    private ResolvedRemote resolveRemoteViaGet(String url) throws IOException {
        HttpURLConnection connection = this.openConnection(url, "GET", true);
        try {
            requireSuccess(connection, "resolve download");
            return new ResolvedRemote(connection.getURL().toString(), connection.getContentLengthLong());
        } finally {
            connection.disconnect();
        }
    }

    private String downloadText(String url) throws IOException {
        HttpURLConnection connection = this.openConnection(url, "GET", true);
        try {
            requireSuccess(connection, "fetch checksum");
            try (InputStream input = connection.getInputStream()) {
                byte[] content = input.readNBytes(16_385);
                if (content.length > 16_384) throw new IOException("Remote SHA-256 response exceeded 16 KiB");
                return new String(content, StandardCharsets.UTF_8);
            }
        } finally {
            connection.disconnect();
        }
    }

    private HttpURLConnection openConnection(String url, String method, boolean followRedirects) throws IOException {
        HttpURLConnection connection = (HttpURLConnection)URI.create(url).toURL().openConnection();
        this.configureConnection(connection, method, followRedirects);
        return connection;
    }

    private void configureConnection(HttpURLConnection connection, String method, boolean followRedirects) throws java.net.ProtocolException {
        connection.setInstanceFollowRedirects(followRedirects);
        connection.setConnectTimeout(timeoutMillis(this.configuration.connectTimeout()));
        connection.setReadTimeout(timeoutMillis(this.configuration.readTimeout()));
        connection.setRequestMethod(method);
        connection.setRequestProperty("User-Agent", this.configuration.userAgent());
        connection.setRequestProperty("Accept-Encoding", "identity");
    }

    private void publish(FFMPEGDownloadSnapshot newSnapshot) {
        this.snapshot.set(newSnapshot);
        for (Consumer<FFMPEGDownloadSnapshot> listener : this.snapshotListeners) {
            try {
                listener.accept(newSnapshot);
            } catch (RuntimeException exception) {
                LOGGER.warn("[KONKRETE] FFmpeg snapshot listener failed", exception);
            }
        }
    }

    private static FFMPEGDownloadSnapshot completeSnapshot(FFMPEGInstallation installation, boolean cached) {
        return new FFMPEGDownloadSnapshot(FFMPEGDownloadSnapshot.Stage.COMPLETE, cached ? "FFmpeg is already installed" : "FFmpeg download completed", installation.getPlatformId() + " | " + installation.getProviderId(), 1.0D, 0L, 0L, null, installation, cached);
    }

    private static String validateBinary(Path binary, String binaryName, Duration timeout) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(binary.toAbsolutePath().toString(), "-version").directory(binary.getParent().toFile()).redirectErrorStream(true).start();
        CompletableFuture<byte[]> outputFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return process.getInputStream().readAllBytes();
            } catch (IOException exception) {
                throw new java.util.concurrent.CompletionException(exception);
            }
        });
        try {
            if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                process.waitFor();
                throw new IOException("Timed out while validating " + binaryName);
            }
        } catch (InterruptedException exception) {
            process.destroyForcibly();
            throw exception;
        }
        String output;
        try {
            output = new String(outputFuture.join(), StandardCharsets.UTF_8);
        } catch (java.util.concurrent.CompletionException exception) {
            if (exception.getCause() instanceof IOException ioException) throw ioException;
            throw exception;
        }
        if (process.exitValue() != 0) throw new IOException("Validation process for " + binaryName + " exited with code " + process.exitValue());
        String firstLine = output.lines().findFirst().orElse("").trim();
        if (!firstLine.toLowerCase(Locale.ROOT).contains(binaryName + " version")) throw new IOException("Validation output for " + binaryName + " was invalid");
        return firstLine;
    }

    private static void ensureExecutable(Path binary, Platform platform) {
        if (platform.family() != PlatformFamily.WINDOWS) binary.toFile().setExecutable(true, false);
    }

    @Nullable
    private static Path findBinary(Path root, String binaryName) throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            return paths.filter(Files::isRegularFile).filter(path -> path.getFileName().toString().equalsIgnoreCase(binaryName)).findFirst().orElse(null);
        }
    }

    private static long zipUncompressedSize(Path archive) throws IOException {
        long size = 0L;
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory() && entry.getSize() > 0L) size += entry.getSize();
            }
        }
        return size;
    }

    private static void moveDirectory(Path source, Path target) throws IOException {
        Path parent = target.getParent();
        if (parent != null) Files.createDirectories(parent);
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicFailure) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void deleteRecursively(Path path) {
        if (path == null || !Files.exists(path)) return;
        try (Stream<Path> paths = Files.walk(path)) {
            paths.sorted(Comparator.reverseOrder()).forEach(candidate -> {
                try {
                    Files.deleteIfExists(candidate);
                } catch (IOException exception) {
                    LOGGER.debug("[KONKRETE] Failed to remove temporary FFmpeg path {}", candidate, exception);
                }
            });
        } catch (IOException exception) {
            LOGGER.debug("[KONKRETE] Failed to enumerate temporary FFmpeg path {}", path, exception);
        }
    }

    private static void writeMetadata(Path target, InstallationMetadata metadata) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(target), StandardCharsets.UTF_8))) {
            GSON.toJson(metadata, writer);
        }
    }

    private static Path safeResolve(Path root, @Nullable String relative) throws IOException {
        if (relative == null || relative.isBlank()) throw new IOException("Installation metadata omitted a binary path");
        Path resolved = root.resolve(relative).normalize();
        if (!resolved.startsWith(root)) throw new IOException("Installation metadata escaped its installation root");
        return resolved;
    }

    private static String relativePath(Path root, Path file) {
        return root.relativize(file).toString().replace('\\', '/');
    }

    private static String parseSha256(String raw) throws IOException {
        String candidate = raw.trim().split("\\s+", 2)[0];
        if (!candidate.matches("(?i)[0-9a-f]{64}")) throw new IOException("Remote SHA-256 response was invalid");
        return candidate.toLowerCase(Locale.ROOT);
    }

    private static MessageDigest sha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("JVM does not provide SHA-256", exception);
        }
    }

    private static void checkCancelled(DownloadOperation operation) {
        if (operation.cancelled.get()) throw new CancellationException("FFmpeg download cancelled");
    }

    private static void requireSuccess(HttpURLConnection connection, String action) throws IOException {
        int responseCode = connection.getResponseCode();
        if (responseCode < 200 || responseCode >= 300) throw new IOException("Failed to " + action + " (HTTP " + responseCode + ')');
    }

    private static boolean isRedirect(int responseCode) {
        return responseCode == 301 || responseCode == 302 || responseCode == 303 || responseCode == 307 || responseCode == 308;
    }

    private static int timeoutMillis(Duration timeout) {
        return (int)Math.min(Integer.MAX_VALUE, timeout.toMillis());
    }

    private static double rangeProgress(double start, double end, long current, long total) {
        if (total <= 0L) return start;
        double ratio = Math.max(0.0D, Math.min(1.0D, (double)current / total));
        return start + (end - start) * ratio;
    }

    private static String formatBytes(long bytes) {
        if (bytes <= 0L) return "0 B";
        double value = bytes;
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unit = 0;
        while (value >= 1024.0D && unit < units.length - 1) {
            value /= 1024.0D;
            unit++;
        }
        return String.format(Locale.ROOT, "%.1f %s", value, units[unit]);
    }

    private static String toHex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) result.append(Character.forDigit((value >>> 4) & 0xF, 16)).append(Character.forDigit(value & 0xF, 16));
        return result.toString();
    }

    /** Closeable snapshot-listener subscription. */
    public static final class SnapshotSubscription implements AutoCloseable {

        private final AtomicBoolean closed = new AtomicBoolean();
        private final Runnable remover;

        private SnapshotSubscription(Runnable remover) {
            this.remover = remover;
        }

        /** Unregisters the snapshot listener once. */
        @Override
        public void close() {
            if (this.closed.compareAndSet(false, true)) this.remover.run();
        }

    }

    private static final class DownloadOperation {
        private final AtomicBoolean cancelled = new AtomicBoolean();
    }

    private record ResolvedRemote(String url, long contentLength) {}

    private record ResolvedArtifact(Artifact artifact, ResolvedRemote remote, String sha256) {}

    private static final class InstallationMetadata {
        private int schemaVersion;
        @Nullable private String platformId;
        @Nullable private String providerId;
        @Nullable private String ffmpegRelativePath;
        @Nullable private String ffprobeRelativePath;
        @Nullable private String ffmpegVersionLine;
        @Nullable private String ffprobeVersionLine;
    }

}
