package de.keksuccino.konkrete.util.ffmpeg.downloader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FFMPEGDownloaderTest {

    @TempDir
    Path tempDirectory;

    @Test
    void normalizesSupportedPlatformAliasesAndRejectsUnsupportedWindowsArm() {
        assertEquals("linux-amd64", FFMPEGDownloader.detectPlatform("Linux", "x86_64").id());
        assertEquals("macos-arm64", FFMPEGDownloader.detectPlatform("Mac OS X", "aarch64").id());
        assertThrows(IllegalStateException.class, () -> FFMPEGDownloader.detectPlatform("Windows 11", "arm64"));
    }

    @Test
    void configurationOwnsStorageUserAgentAndProviderPolicy() {
        FFMPEGDownloadConfiguration configuration = FFMPEGDownloadConfiguration.defaults(this.tempDirectory.resolve("mod-owned-cache"), "ExampleMod/1.0");

        try (FFMPEGDownloader downloader = new FFMPEGDownloader(configuration)) {
            assertEquals(this.tempDirectory.resolve("mod-owned-cache").toAbsolutePath().normalize().toFile(), downloader.getDownloadDirectory());
            assertFalse(downloader.isDownloadRunning());
            assertEquals(FFMPEGDownloadSnapshot.Stage.IDLE, downloader.getSnapshot().getStage());
        }
    }

    @Test
    void snapshotsClampProgressAndScreenResultsPreserveCacheOutcome() {
        FFMPEGDownloadSnapshot snapshot = new FFMPEGDownloadSnapshot(FFMPEGDownloadSnapshot.Stage.COMPLETE, "done", null, 2.0D, -1L, -1L, null, null, true);
        FFMPEGDownloadSnapshot nonFiniteSnapshot = new FFMPEGDownloadSnapshot(FFMPEGDownloadSnapshot.Stage.IDLE, "idle", null, Double.NaN, 0L, 0L, null, null, false);
        FFMPEGDownloaderScreenResult result = FFMPEGDownloaderScreenResult.fromSnapshot(snapshot);

        assertEquals(1.0D, snapshot.getProgress());
        assertEquals(0.0D, nonFiniteSnapshot.getProgress());
        assertEquals(0L, snapshot.getDownloadedBytes());
        assertEquals(FFMPEGDownloaderScreenResult.Outcome.ALREADY_AVAILABLE, result.outcome());
        assertTrue(result.isReady());
    }

    @Test
    void configurationRejectsUnsafeAndDuplicateArtifactIds() {
        assertThrows(IllegalArgumentException.class, () -> FFMPEGDownloadConfiguration.defaults(this.tempDirectory, " "));
        assertThrows(IllegalArgumentException.class, () -> new FFMPEGDownloadConfiguration.Artifact("../escape", "archive", "https://example.invalid/archive.zip"));
        FFMPEGDownloadConfiguration.Artifact artifact = new FFMPEGDownloadConfiguration.Artifact("bundle", "archive", "https://example.invalid/archive.zip");
        assertThrows(IllegalArgumentException.class, () -> new FFMPEGDownloadConfiguration.Distribution("provider", "Provider", List.of(artifact, artifact)));
    }

    @Test
    void closeCompletesQueuedOperationByCancellation() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CompletableFuture<Void> blocker = new CompletableFuture<>();
        executor.submit(blocker::join);
        FFMPEGDownloader downloader = new FFMPEGDownloader(FFMPEGDownloadConfiguration.defaults(this.tempDirectory, "ExampleMod/1.0"), executor);
        CompletableFuture<FFMPEGInstallation> operation = downloader.ensureInstalledAsync();

        downloader.close();
        blocker.complete(null);
        executor.shutdownNow();

        assertTrue(operation.isCancelled());
    }

    @Test
    void rejectedExecutorProducesFailedFutureAndTerminalSnapshot() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.shutdownNow();
        FFMPEGDownloader downloader = new FFMPEGDownloader(FFMPEGDownloadConfiguration.defaults(this.tempDirectory, "ExampleMod/1.0"), executor);

        CompletableFuture<FFMPEGInstallation> operation = downloader.ensureInstalledAsync();

        assertTrue(operation.isCompletedExceptionally());
        assertEquals(FFMPEGDownloadSnapshot.Stage.FAILED, downloader.getSnapshot().getStage());
        downloader.close();
    }

}
