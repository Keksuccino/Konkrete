package de.keksuccino.konkrete.util.ffmpeg;

import de.keksuccino.konkrete.util.ffmpeg.downloader.FFMPEGInstallation;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Process-building and bounded execution helpers for validated FFmpeg installations.
 */
public final class FFMPEGUtils {

    private FFMPEGUtils() {}

    /** Builds an FFmpeg process in the installation directory. */
    @NotNull
    public static ProcessBuilder ffmpegProcess(@NotNull FFMPEGInstallation installation, @NotNull List<String> arguments) {
        return process(installation, Binary.FFMPEG, arguments);
    }

    /** Builds an ffprobe process in the installation directory. */
    @NotNull
    public static ProcessBuilder ffprobeProcess(@NotNull FFMPEGInstallation installation, @NotNull List<String> arguments) {
        return process(installation, Binary.FFPROBE, arguments);
    }

    /**
     * Runs FFmpeg or ffprobe with merged stdout/stderr and a hard timeout.
     *
     * @param installation validated installation
     * @param binary binary to run
     * @param arguments process arguments
     * @param timeout positive timeout
     * @return exit result and UTF-8 output
     * @throws IOException when the process cannot start or output cannot be read
     * @throws InterruptedException when the calling thread is interrupted; the child process is forcibly terminated first
     */
    @NotNull
    public static ProcessResult run(@NotNull FFMPEGInstallation installation, @NotNull Binary binary, @NotNull List<String> arguments, @NotNull Duration timeout) throws IOException, InterruptedException {
        Duration checkedTimeout = Objects.requireNonNull(timeout, "timeout");
        if (checkedTimeout.isZero() || checkedTimeout.isNegative()) throw new IllegalArgumentException("timeout must be positive");
        Process process = process(installation, binary, arguments).redirectErrorStream(true).start();
        CompletableFuture<byte[]> outputFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return process.getInputStream().readAllBytes();
            } catch (IOException exception) {
                throw new java.util.concurrent.CompletionException(exception);
            }
        });
        try {
            if (!process.waitFor(checkedTimeout.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                process.waitFor();
                throw new IOException(binary + " timed out after " + checkedTimeout);
            }
        } catch (InterruptedException exception) {
            process.destroyForcibly();
            throw exception;
        }
        try {
            return new ProcessResult(process.exitValue(), new String(outputFuture.join(), StandardCharsets.UTF_8));
        } catch (java.util.concurrent.CompletionException exception) {
            if (exception.getCause() instanceof IOException ioException) throw ioException;
            throw exception;
        }
    }

    private static ProcessBuilder process(FFMPEGInstallation installation, Binary binary, List<String> arguments) {
        FFMPEGInstallation checkedInstallation = Objects.requireNonNull(installation, "installation");
        Binary checkedBinary = Objects.requireNonNull(binary, "binary");
        if (!checkedInstallation.isValid()) throw new IllegalArgumentException("FFmpeg installation is no longer valid");
        List<String> command = new ArrayList<>(Objects.requireNonNull(arguments, "arguments").size() + 1);
        command.add((checkedBinary == Binary.FFMPEG ? checkedInstallation.getFfmpegBinary() : checkedInstallation.getFfprobeBinary()).getAbsolutePath());
        command.addAll(arguments);
        return new ProcessBuilder(command).directory(checkedInstallation.getInstallDirectory());
    }

    /** Selectable installation binaries. */
    public enum Binary {
        FFMPEG,
        FFPROBE
    }

    /**
     * Completed process result.
     *
     * @param exitCode process exit code
     * @param output merged UTF-8 stdout/stderr
     */
    public record ProcessResult(int exitCode, @NotNull String output) {

        /** Validates captured process output. */
        public ProcessResult {
            output = Objects.requireNonNull(output, "output");
        }

        /** Returns whether the process exited successfully. */
        public boolean isSuccess() {
            return this.exitCode == 0;
        }

    }

}
