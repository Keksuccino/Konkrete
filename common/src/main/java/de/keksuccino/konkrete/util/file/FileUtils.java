package de.keksuccino.konkrete.util.file;

import net.minecraft.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/** Provides reusable UTF-8 file, directory, and ZIP helpers. */
public class FileUtils {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Writes the supplied UTF-8 text to a file, optionally appending to existing content. */
    public static void writeTextToFile(@NotNull File file, boolean append, String... text) throws IOException {
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(text, "text");
        StandardOpenOption[] options = append ? new StandardOpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.APPEND} : new StandardOpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING};
        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8, options)) {
            if (text.length == 1) {
                writer.write(Objects.requireNonNull(text[0], "text entry"));
            } else {
                for (String line : text) {
                    writer.write(Objects.requireNonNull(line, "text entry"));
                    writer.newLine();
                }
            }
        }
    }

    /** Reads UTF-8 lines, logging failures and returning an empty list when the file cannot be read. */
    public static List<String> getFileLines(@NotNull File file) {
        try {
            return readTextLinesFrom(file);
        } catch (Exception ex) {
            LOGGER.error("[KONKRETE] Failed to read text lines of file: " + file.getAbsolutePath(), ex);
            return new ArrayList<>();
        }
    }

    /** Returns the absolute paths of direct children of the supplied directory. */
    public static List<String> getFiles(@NotNull String path) {
        File[] files = new File(Objects.requireNonNull(path, "path")).listFiles();
        List<String> result = new ArrayList<>();
        if (files != null) for (File file : files) result.add(file.getAbsolutePath());
        return result;
    }

    /** Returns direct child names, optionally omitting their final extension. */
    public static List<String> getFilenames(@NotNull String path, boolean includeExtension) {
        File[] files = new File(Objects.requireNonNull(path, "path")).listFiles();
        List<String> result = new ArrayList<>();
        if (files != null) for (File file : files) result.add(includeExtension ? file.getName() : getNameWithoutExtension(file.getName()));
        return result;
    }

    /** Returns an unused filename in the directory using a numeric suffix when necessary. */
    public static String generateAvailableFilename(@NotNull String directory, @NotNull String baseName, @NotNull String extension) {
        File folder = new File(Objects.requireNonNull(directory, "directory"));
        if (!folder.exists() && !folder.mkdirs()) LOGGER.warn("[KONKRETE] Failed to create directory while generating a filename: {}", folder);
        String normalizedExtension = Objects.requireNonNull(extension, "extension").replace(".", "");
        String suffix = normalizedExtension.isEmpty() ? "" : "." + normalizedExtension;
        File candidate = new File(folder, Objects.requireNonNull(baseName, "baseName") + suffix);
        int count = 1;
        while (candidate.exists()) candidate = new File(folder, baseName + "_" + count++ + suffix);
        return candidate.getName();
    }

    /** Copies a file without replacing an existing destination. */
    @Deprecated
    public static boolean copyFile(@NotNull File from, @NotNull File to) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        if (!from.isFile() || sameNormalizedPath(from, to)) return false;
        try {
            Path parent = to.toPath().getParent();
            if (parent != null) Files.createDirectories(parent);
            Files.copy(from.toPath(), to.toPath());
            return Files.isRegularFile(to.toPath());
        } catch (IOException ex) {
            LOGGER.error("[KONKRETE] Failed to copy file from {} to {}.", from, to, ex);
            return false;
        }
    }

    /** Moves a file without replacing an existing destination. */
    @Deprecated
    public static boolean moveFile(@NotNull File from, @NotNull File to) throws InterruptedException {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        if (!from.isFile() || sameNormalizedPath(from, to)) return false;
        try {
            Path parent = to.toPath().getParent();
            if (parent != null) Files.createDirectories(parent);
            Files.move(from.toPath(), to.toPath());
            return Files.isRegularFile(to.toPath());
        } catch (IOException moveFailure) {
            if (!copyFile(from, to)) return false;
            try {
                Files.delete(from.toPath());
                return true;
            } catch (IOException deleteFailure) {
                try {
                    Files.deleteIfExists(to.toPath());
                } catch (IOException cleanupFailure) {
                    deleteFailure.addSuppressed(cleanupFailure);
                }
                LOGGER.error("[KONKRETE] Failed to remove source after copying {} to {}.", from, to, deleteFailure);
                return false;
            }
        }
    }

    /** Compresses a file or directory tree into a ZIP archive. */
    public static void compressToZip(@NotNull String pathToCompress, @NotNull String zipFile) {
        Path source = Path.of(Objects.requireNonNull(pathToCompress, "pathToCompress"));
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(Path.of(Objects.requireNonNull(zipFile, "zipFile"))))) {
            Path base = source.getParent();
            if (base == null) base = source.toAbsolutePath().getParent();
            writeZipTree(output, source, base);
        } catch (Exception ex) {
            LOGGER.error("[KONKRETE] Error while trying to compress ZIP: " + zipFile, ex);
        }
    }

    /** Compresses the supplied files into a ZIP archive. */
    public static void compressToZip(@NotNull List<String> filePathsToCompress, @NotNull String zipFile) {
        Objects.requireNonNull(filePathsToCompress, "filePathsToCompress");
        Path destination = Path.of(Objects.requireNonNull(zipFile, "zipFile"));
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(destination))) {
            String rootName = getNameWithoutExtension(destination.getFileName().toString());
            for (String filePath : filePathsToCompress) {
                Path source = Path.of(filePath);
                if (!Files.isRegularFile(source)) continue;
                output.putNextEntry(new ZipEntry(rootName + "/" + source.getFileName()));
                Files.copy(source, output);
                output.closeEntry();
            }
        } catch (Exception ex) {
            LOGGER.error("[KONKRETE] Error while trying to compress ZIP: " + zipFile, ex);
        }
    }

    /** Extracts a ZIP archive while rejecting entries that escape the output directory. */
    public static void unpackZip(@NotNull String zipPath, @NotNull String outputDirectory) throws IOException {
        Path outputRoot = Path.of(Objects.requireNonNull(outputDirectory, "outputDirectory")).toAbsolutePath().normalize();
        Files.createDirectories(outputRoot);
        try (ZipFile zipFile = new ZipFile(Objects.requireNonNull(zipPath, "zipPath"))) {
            var entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                Path destination = outputRoot.resolve(entry.getName()).normalize();
                if (!destination.startsWith(outputRoot)) throw new IOException("ZIP entry escapes output directory: " + entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(destination);
                } else {
                    Path parent = destination.getParent();
                    if (parent != null) Files.createDirectories(parent);
                    try (InputStream input = zipFile.getInputStream(entry); OutputStream output = Files.newOutputStream(destination)) {
                        input.transferTo(output);
                    }
                }
            }
        }
    }

    /** Reads every UTF-8 line while leaving the caller-owned stream open. */
    @NotNull
    public static List<String> readTextLinesFrom(@NotNull InputStream input) throws IOException {
        Objects.requireNonNull(input, "input");
        List<String> lines = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
        for (String line = reader.readLine(); line != null; line = reader.readLine()) lines.add(line);
        return lines;
    }

    /** Opens a file, reads every UTF-8 line, and closes the internally owned stream. */
    @NotNull
    public static List<String> readTextLinesFrom(@NotNull File file) throws IOException {
        return readTextLinesFrom(file, source -> Files.newInputStream(source.toPath()));
    }

    @NotNull
    static List<String> readTextLinesFrom(@NotNull File file, @NotNull OwnedInputStreamOpener inputStreamOpener) throws IOException {
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(inputStreamOpener, "inputStreamOpener");
        try (InputStream input = inputStreamOpener.open(file)) {
            return readTextLinesFrom(input);
        }
    }

    /** Returns a non-conflicting path derived from the supplied file or directory. */
    @NotNull
    public static File generateUniqueFileName(@NotNull File fileOrFolder, boolean isDirectory) {
        Objects.requireNonNull(fileOrFolder, "fileOrFolder");
        if (isDirectory && !fileOrFolder.isDirectory()) return fileOrFolder;
        if (!isDirectory && !fileOrFolder.isFile()) return fileOrFolder;
        File candidate = fileOrFolder;
        int count = 1;
        while (isDirectory ? candidate.isDirectory() : candidate.isFile()) candidate = new File(fileOrFolder.getPath() + "_" + count++);
        return candidate;
    }

    /** Creates a directory if necessary and returns it. */
    @NotNull
    public static File createDirectory(@NotNull File directory) {
        Objects.requireNonNull(directory, "directory");
        try {
            Files.createDirectories(directory.toPath());
        } catch (Exception ex) {
            LOGGER.error("[KONKRETE] Failed to create directory: " + directory.getAbsolutePath(), ex);
            return directory;
        }
        if (directory.getName().startsWith(".")) {
            try {
                Files.setAttribute(directory.toPath(), "dos:hidden", true);
            } catch (Exception ignored) {
            }
        }
        return directory;
    }

    /** Opens a local file with the operating system's default application. */
    public static void openFile(@NotNull File file) {
        Objects.requireNonNull(file, "file");
        try {
            String url = file.toURI().toURL().toString();
            String operatingSystem = System.getProperty("os.name").toLowerCase(Locale.ROOT);
            if (Util.getPlatform() == Util.OS.OSX) Runtime.getRuntime().exec(new String[]{"open", url});
            else if (operatingSystem.contains("win")) Runtime.getRuntime().exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", url});
            else Runtime.getRuntime().exec(new String[]{"xdg-open", url.replace("file:", "file://")});
        } catch (Exception ex) {
            LOGGER.error("[KONKRETE] Failed to open file: " + file.getAbsolutePath(), ex);
        }
    }

    private static boolean sameNormalizedPath(File first, File second) {
        return first.toPath().toAbsolutePath().normalize().equals(second.toPath().toAbsolutePath().normalize());
    }

    private static void writeZipTree(ZipOutputStream output, Path source, Path base) throws IOException {
        if (Files.isRegularFile(source)) {
            writeZipEntry(output, source, base);
            return;
        }
        try (Stream<Path> paths = Files.walk(source)) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) writeZipEntry(output, path, base);
        }
    }

    private static void writeZipEntry(ZipOutputStream output, Path source, Path base) throws IOException {
        String entryName = base.relativize(source).toString().replace(File.separatorChar, '/');
        output.putNextEntry(new ZipEntry(entryName));
        Files.copy(source, output);
        output.closeEntry();
    }

    private static String getNameWithoutExtension(String name) {
        int separator = name.lastIndexOf('.');
        return separator > 0 ? name.substring(0, separator) : name;
    }

    @FunctionalInterface
    interface OwnedInputStreamOpener {

        InputStream open(File file) throws IOException;

    }

}
