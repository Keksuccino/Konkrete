package de.keksuccino.konkrete.util.resource.resources.texture.fma;

import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.keksuccino.konkrete.util.MathUtils;
import de.keksuccino.konkrete.util.file.FileUtils;
import de.keksuccino.konkrete.util.resource.ResourceInputLimits;
import de.keksuccino.konkrete.util.resource.ResourceRuntime;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads bounded FMA archives, validates metadata and PNG frames, and exposes decoded frame state. */
public class FmaDecoder implements Closeable {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().create();
    private static final File TEMP_DIR = ResourceRuntime.temporaryDirectory("decoded_fma_images");
    private static final Pattern FRAME_ENTRY_PATTERN = Pattern.compile("^frames/(\\d+)\\.png$", Pattern.CASE_INSENSITIVE);
    private static final Pattern INTRO_FRAME_ENTRY_PATTERN = Pattern.compile("^intro_frames/(\\d+)\\.png$", Pattern.CASE_INSENSITIVE);
    private static final int EXPENSIVE_FRAME_SAMPLE_COUNT = 10;
    private static final int PNG_HEADER_PROBE_BYTES = 33;
    private static final byte[] PNG_SIGNATURE = new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10};

    /** Current zip file state for this FMA decoder instance. */
    @Nullable
    protected ZipFile zipFile = null;
    /** Current metadata state for this FMA decoder instance. */
    @Nullable
    protected FmaMetadata metadata = null;
    /** Holds the orderedFramePaths collection used by this FMA decoder instance. */
    @NotNull
    protected final List<String> orderedFramePaths = new ArrayList<>();
    /** Holds the orderedIntroFramePaths collection used by this FMA decoder instance. */
    @NotNull
    protected final List<String> orderedIntroFramePaths = new ArrayList<>();
    /** Holds the entriesByNormalizedPath collection used by this FMA decoder instance. */
    @NotNull
    protected final Map<String, ZipArchiveEntry> entriesByNormalizedPath = new HashMap<>();

    /** Holds the tempArchiveFile handle whose lifecycle follows this FMA decoder instance. */
    @Nullable
    protected File tempArchiveFile = null;
    /** Whether delete temp archive on close currently applies to this FMA decoder instance. */
    protected boolean deleteTempArchiveOnClose = false;

    /**
     * Reads an FMA file from an {@link InputStream}. Spools the stream into a temporary archive file.
     * Closes the provided {@link InputStream} at the end.
     */
    public void read(@NotNull InputStream in) throws IOException {
        Objects.requireNonNull(in);
        if (this.zipFile != null) throw new IllegalStateException("The decoder is already reading a file!");

        try {
            File tempArchive = this.spoolToTempArchive(in);
            this.openArchive(tempArchive, true);
            this.initializeArchiveState();
        } catch (Exception ex) {
            this.close();
            throw new IOException(ex);
        } finally {
            try {
                in.close();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Reads an FMA file from a {@link File}.
     */
    public void read(@NotNull File fmaFile) throws IOException {
        Objects.requireNonNull(fmaFile);
        if (this.zipFile != null) throw new IllegalStateException("The decoder is already reading a file!");
        if (fmaFile.length() > ResourceRuntime.getSafetyLimits().maxArchiveBytes()) throw new IOException("FMA archive exceeds resource safety limits");

        try {
            this.openArchive(fmaFile, false);
            this.initializeArchiveState();
        } catch (Exception ex) {
            this.close();
            throw new IOException(ex);
        }
    }

    /** Copies a streamed FMA container into a size-bounded temporary archive for {@link #openArchive(File, boolean)}. */
    @NotNull
    protected File spoolToTempArchive(@NotNull InputStream in) throws IOException {
        File tempArchive = File.createTempFile("fma_stream_", ".fma", TEMP_DIR);
        try (BufferedInputStream bufferedIn = new BufferedInputStream(in);
             BufferedOutputStream bufferedOut = new BufferedOutputStream(new FileOutputStream(tempArchive))) {
            ResourceInputLimits.copy(bufferedIn, bufferedOut, ResourceRuntime.getSafetyLimits().maxArchiveBytes(), "FMA archive");
            bufferedOut.flush();
        } catch (Exception ex) {
            if (tempArchive.exists() && !tempArchive.delete()) {
                LOGGER.warn("[KONKRETE] Failed to delete temporary FMA archive after spool failure: {}", tempArchive.getAbsolutePath());
            }
            throw ex;
        }
        return tempArchive;
    }

    /** Opens the archive for the FMA decoder. */
    protected void openArchive(@NotNull File archiveFile, boolean deleteOnClose) throws IOException {
        this.zipFile = new ZipFile(archiveFile);
        this.tempArchiveFile = deleteOnClose ? archiveFile : null;
        this.deleteTempArchiveOnClose = deleteOnClose;
    }

    /** Initializes the archive state for the FMA decoder. */
    protected void initializeArchiveState() throws IOException {
        this.indexEntries();
        this.readMetadata();
        boolean loadedFromFrameIndex = this.readFrameIndexIfPresent();
        if (!loadedFromFrameIndex) {
            this.readFramePaths();
            this.readIntroFramePaths();
        }

        if (this.orderedFramePaths.isEmpty()) {
            throw new FileNotFoundException("No frames found in FMA file!");
        }
        ResourceRuntime.getSafetyLimits().validateFrameCount((long) this.orderedFramePaths.size() + this.orderedIntroFramePaths.size(), "FMA");
        this.validateFrameDimensions();
    }

    /**
     * Optional fast-path index for newer FMA files.
     * If this index is absent or invalid, decoder falls back to classic directory scanning.
     */
    protected boolean readFrameIndexIfPresent() throws IOException {
        Objects.requireNonNull(this.zipFile);
        this.orderedFramePaths.clear();
        this.orderedIntroFramePaths.clear();

        ZipArchiveEntry frameIndexEntry = this.findEntry("frame_index.json");
        if (frameIndexEntry == null) return false;

        try {
            String indexJson;
            try (InputStream indexIn = this.zipFile.getInputStream(frameIndexEntry)) {
                long frameIndexByteLimit = Math.min(ResourceRuntime.getSafetyLimits().maxDecompressedBytes(), Math.max(1024L, Math.multiplyExact((long) ResourceRuntime.getSafetyLimits().maxFrameCount(), 256L)));
                indexJson = new String(ResourceInputLimits.readAllBytes(indexIn, frameIndexByteLimit, "FMA frame index"), StandardCharsets.UTF_8);
            }

            FmaFrameIndex frameIndex = GSON.fromJson(indexJson, FmaFrameIndex.class);
            if (frameIndex == null) return false;

            if (frameIndex.frames != null) {
                for (String rawPath : frameIndex.frames) {
                    if (rawPath == null) continue;
                    String normalizedPath = normalizeEntryPath(rawPath).toLowerCase(Locale.ROOT);
                    if (!FRAME_ENTRY_PATTERN.matcher(normalizedPath).matches()) continue;
                    if (!this.entriesByNormalizedPath.containsKey(normalizedPath)) continue;
                    this.orderedFramePaths.add(normalizedPath);
                }
            }

            if (frameIndex.intro_frames != null) {
                for (String rawPath : frameIndex.intro_frames) {
                    if (rawPath == null) continue;
                    String normalizedPath = normalizeEntryPath(rawPath).toLowerCase(Locale.ROOT);
                    if (!INTRO_FRAME_ENTRY_PATTERN.matcher(normalizedPath).matches()) continue;
                    if (!this.entriesByNormalizedPath.containsKey(normalizedPath)) continue;
                    this.orderedIntroFramePaths.add(normalizedPath);
                }
            }

            if (this.orderedFramePaths.isEmpty()) {
                this.orderedIntroFramePaths.clear();
                return false;
            }

            return true;
        } catch (Exception ex) {
            // Any issue in optional index should gracefully fall back to classic scan path.
            this.orderedFramePaths.clear();
            this.orderedIntroFramePaths.clear();
            return false;
        }
    }

    /** Indexes the entries for bounded FMA decoder lookup. */
    protected void indexEntries() throws IOException {
        Objects.requireNonNull(this.zipFile);
        this.entriesByNormalizedPath.clear();

        var entries = this.zipFile.getEntries();
        int entryCount = 0;
        long declaredBytes = 0L;
        while (entries.hasMoreElements()) {
            ZipArchiveEntry entry = entries.nextElement();
            if (entry == null || entry.isDirectory()) continue;
            entryCount++;
            if (entryCount > ResourceRuntime.getSafetyLimits().maxArchiveEntries()) throw new IOException("FMA archive entry count exceeds resource safety limits");
            long size = entry.getSize();
            if (size > ResourceRuntime.getSafetyLimits().maxDecompressedBytes()) throw new IOException("FMA archive entry exceeds resource safety limits: " + entry.getName());
            if (size > 0L) {
                declaredBytes = Math.addExact(declaredBytes, size);
                if (declaredBytes > ResourceRuntime.getSafetyLimits().maxDecompressedBytes()) throw new IOException("FMA declared decompressed size exceeds resource safety limits");
            }
            String normalized = normalizeEntryPath(entry.getName()).toLowerCase(Locale.ROOT);
            this.entriesByNormalizedPath.putIfAbsent(normalized, entry);
        }
    }

    /** Reads the frame paths from the FMA decoder input. */
    protected void readFramePaths() throws IOException {
        Objects.requireNonNull(this.zipFile);
        this.orderedFramePaths.clear();

        try {
            for (String normalizedPath : this.entriesByNormalizedPath.keySet()) {
                Matcher matcher = FRAME_ENTRY_PATTERN.matcher(normalizedPath);
                if (!matcher.matches()) continue;

                String indexString = matcher.group(1);
                if (MathUtils.isInteger(indexString)) {
                    this.orderedFramePaths.add(normalizedPath);
                } else {
                    LOGGER.error("[KONKRETE] Invalid PNG frame found in FMA file!", new IllegalStateException("Frame file name is not a valid number: " + normalizedPath));
                }
            }

            this.orderedFramePaths.sort(Comparator.comparingInt(FmaDecoder::readFrameIndex));
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    /** Reads the intro frame paths from the FMA decoder input. */
    protected void readIntroFramePaths() throws IOException {
        Objects.requireNonNull(this.zipFile);
        this.orderedIntroFramePaths.clear();

        try {
            for (String normalizedPath : this.entriesByNormalizedPath.keySet()) {
                Matcher matcher = INTRO_FRAME_ENTRY_PATTERN.matcher(normalizedPath);
                if (!matcher.matches()) continue;

                String indexString = matcher.group(1);
                if (MathUtils.isInteger(indexString)) {
                    this.orderedIntroFramePaths.add(normalizedPath);
                } else {
                    LOGGER.error("[KONKRETE] Invalid PNG intro frame found in FMA file!", new IllegalStateException("Frame file name is not a valid number: " + normalizedPath));
                }
            }

            this.orderedIntroFramePaths.sort(Comparator.comparingInt(FmaDecoder::readFrameIndex));
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    /** Reads the metadata from the FMA decoder input. */
    protected void readMetadata() throws IOException {
        Objects.requireNonNull(this.zipFile);

        try {
            ZipArchiveEntry metadataEntry = this.findEntry("metadata.json");
            if (metadataEntry == null) {
                throw new FileNotFoundException("No metadata.json found in FMA file! Unable to read metadata!");
            }

            String metadataString;
            try (InputStream metadataIn = this.zipFile.getInputStream(metadataEntry)) {
                metadataString = new String(ResourceInputLimits.readAllBytes(metadataIn, ResourceRuntime.getSafetyLimits().maxDecompressedBytes(), "FMA metadata"), StandardCharsets.UTF_8);
            }

            if (metadataString.trim().isEmpty()) {
                throw new IOException("metadata.json of FMA file is empty!");
            }

            FmaMetadata parsedMetadata = GSON.fromJson(metadataString, FmaMetadata.class);
            if (parsedMetadata == null) {
                throw new IOException("Unable to parse metadata.json of FMA file!");
            }

            this.metadata = parsedMetadata;
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    /** Returns the frame count used by this FMA decoder instance. */
    public int getFrameCount() {
        return this.orderedFramePaths.size();
    }

    /** Returns the intro frame count used by this FMA decoder instance. */
    public int getIntroFrameCount() {
        return this.orderedIntroFramePaths.size();
    }

    /** Returns whether intro frames. */
    public boolean hasIntroFrames() {
        return (this.getIntroFrameCount() > 0);
    }

    /** Returns the metadata, or {@code null} when it is not available. */
    @Nullable
    public FmaMetadata getMetadata() {
        return this.metadata;
    }

    /** Resolves the expensive frame sample for the FMA decoder. */
    @Nullable
    public ExpensiveFrameSample findExpensiveFrameSample() throws IOException {
        Objects.requireNonNull(this.zipFile);

        for (String framePath : this.pickSampleFramePaths()) {
            PngFrameHeader pngHeader = this.readPngFrameHeader(framePath);
            if ((pngHeader != null) && pngHeader.isExpensive()) {
                return new ExpensiveFrameSample(framePath, pngHeader.width(), pngHeader.height(), pngHeader.bitDepth(), pngHeader.colorType(), pngHeader.interlaceMethod());
            }
        }

        return null;
    }

    /** Returns the frame, or {@code null} when it is not available. */
    @Nullable
    public InputStream getFrame(int index) throws IOException {
        Objects.requireNonNull(this.zipFile);
        if (this.orderedFramePaths.isEmpty()) return null;
        if (index < 0) return null;
        if (this.getFrameCount() - 1 < index) return null;

        try {
            String framePath = this.orderedFramePaths.get(index);
            ZipArchiveEntry frameEntry = this.findEntry(framePath);
            if (frameEntry == null) {
                throw new FileNotFoundException("Frame file of FMA not found: " + framePath);
            }
            return ResourceInputLimits.bounded(this.zipFile.getInputStream(frameEntry), ResourceRuntime.getSafetyLimits().maxDecompressedBytes(), "FMA frame");
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    /** Returns the first frame, or {@code null} when it is not available. */
    @Nullable
    public InputStream getFirstFrame() throws IOException {
        return this.getFrame(0);
    }

    /** Returns the first frame as buffered image, or {@code null} when it is not available. */
    @Nullable
    public BufferedImage getFirstFrameAsBufferedImage() throws IOException {
        InputStream in = this.getFirstFrame();
        if (in == null) return null;

        try (InputStream closeableIn = in) {
            return ImageIO.read(closeableIn);
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    /** Returns the intro frame, or {@code null} when it is not available. */
    @Nullable
    public InputStream getIntroFrame(int index) throws IOException {
        Objects.requireNonNull(this.zipFile);
        if (this.orderedIntroFramePaths.isEmpty()) return null;
        if (index < 0) return null;
        if (this.getIntroFrameCount() - 1 < index) return null;

        try {
            String framePath = this.orderedIntroFramePaths.get(index);
            ZipArchiveEntry frameEntry = this.findEntry(framePath);
            if (frameEntry == null) {
                throw new FileNotFoundException("Intro frame file of FMA not found: " + framePath);
            }
            return ResourceInputLimits.bounded(this.zipFile.getInputStream(frameEntry), ResourceRuntime.getSafetyLimits().maxDecompressedBytes(), "FMA intro frame");
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    /**
     * Returns the background.png image of the FMA file, if present. Returns NULL if no background.png file is present.<br>
     * This is UNUSED at the moment. The background is not used by Konkrete's {@link FmaTexture} class.
     */
    @Nullable
    public InputStream getBackgroundImage() throws IOException {
        Objects.requireNonNull(this.zipFile);

        ZipArchiveEntry backgroundImageEntry = this.findEntry("background.png");
        if (backgroundImageEntry == null) return null;

        try {
            return ResourceInputLimits.bounded(this.zipFile.getInputStream(backgroundImageEntry), ResourceRuntime.getSafetyLimits().maxDecompressedBytes(), "FMA background image");
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    /** Resolves the entry for the FMA decoder. */
    @Nullable
    protected ZipArchiveEntry findEntry(@NotNull String path) {
        String normalizedPath = normalizeEntryPath(path).toLowerCase(Locale.ROOT);
        return this.entriesByNormalizedPath.get(normalizedPath);
    }

    /** Selects the sample frame paths for the FMA decoder. */
    @NotNull
    protected List<String> pickSampleFramePaths() {
        List<String> allFramePaths = new ArrayList<>(this.orderedIntroFramePaths.size() + this.orderedFramePaths.size());
        allFramePaths.addAll(this.orderedIntroFramePaths);
        allFramePaths.addAll(this.orderedFramePaths);

        if (allFramePaths.isEmpty()) return List.of();

        int sampleCount = Math.min(EXPENSIVE_FRAME_SAMPLE_COUNT, allFramePaths.size());
        if (sampleCount >= allFramePaths.size()) {
            return allFramePaths;
        }

        Set<Integer> sampleIndexes = new LinkedHashSet<>();
        if (sampleCount == 1) {
            sampleIndexes.add(0);
        } else {
            for (int i = 0; i < sampleCount; i++) {
                int index = (int)Math.round((i * (allFramePaths.size() - 1D)) / (sampleCount - 1D));
                sampleIndexes.add(Math.max(0, Math.min(allFramePaths.size() - 1, index)));
            }
        }

        for (int i = 0; (sampleIndexes.size() < sampleCount) && (i < allFramePaths.size()); i++) {
            sampleIndexes.add(i);
        }

        List<String> sampledPaths = new ArrayList<>(sampleIndexes.size());
        for (int index : sampleIndexes) {
            sampledPaths.add(allFramePaths.get(index));
        }
        return sampledPaths;
    }

    /** Validates every PNG header before ImageIO can allocate frame storage. */
    protected void validateFrameDimensions() throws IOException {
        List<String> framePaths = new ArrayList<>(this.orderedIntroFramePaths.size() + this.orderedFramePaths.size());
        framePaths.addAll(this.orderedIntroFramePaths);
        framePaths.addAll(this.orderedFramePaths);
        for (String framePath : framePaths) {
            PngFrameHeader header = this.readPngFrameHeader(framePath);
            if (header == null) throw new IOException("FMA frame has no valid PNG header: " + framePath);
            try {
                ResourceRuntime.getSafetyLimits().validateImageDimensions(header.width(), header.height(), "FMA frame " + framePath);
            } catch (IllegalArgumentException ex) {
                throw new IOException(ex.getMessage(), ex);
            }
        }
    }

    /** Reads the png frame header from the FMA decoder input. */
    @Nullable
    protected PngFrameHeader readPngFrameHeader(@NotNull String framePath) throws IOException {
        Objects.requireNonNull(this.zipFile);

        ZipArchiveEntry frameEntry = this.findEntry(framePath);
        if (frameEntry == null) return null;

        byte[] headerBytes = new byte[PNG_HEADER_PROBE_BYTES];
        int totalRead = 0;
        try (InputStream frameIn = this.zipFile.getInputStream(frameEntry)) {
            while (totalRead < headerBytes.length) {
                int read = frameIn.read(headerBytes, totalRead, headerBytes.length - totalRead);
                if (read < 0) break;
                totalRead += read;
            }
        } catch (Exception ex) {
            throw new IOException(ex);
        }

        if (totalRead < PNG_HEADER_PROBE_BYTES) return null;
        if (!matchesPngSignature(headerBytes)) return null;
        if ((headerBytes[12] != 'I') || (headerBytes[13] != 'H') || (headerBytes[14] != 'D') || (headerBytes[15] != 'R')) return null;

        return new PngFrameHeader(
                readIntBigEndian(headerBytes, 16),
                readIntBigEndian(headerBytes, 20),
                Byte.toUnsignedInt(headerBytes[24]),
                Byte.toUnsignedInt(headerBytes[25]),
                Byte.toUnsignedInt(headerBytes[28])
        );
    }

    /** Returns whether matches png signature. */
    protected static boolean matchesPngSignature(@NotNull byte[] headerBytes) {
        if (headerBytes.length < PNG_SIGNATURE.length) return false;
        for (int i = 0; i < PNG_SIGNATURE.length; i++) {
            if (headerBytes[i] != PNG_SIGNATURE[i]) return false;
        }
        return true;
    }

    /** Reads the int big endian from the FMA decoder input. */
    protected static int readIntBigEndian(@NotNull byte[] bytes, int offset) {
        return ((bytes[offset] & 255) << 24)
                | ((bytes[offset + 1] & 255) << 16)
                | ((bytes[offset + 2] & 255) << 8)
                | (bytes[offset + 3] & 255);
    }

    /** Reads the frame index from the FMA decoder input. */
    protected static int readFrameIndex(@NotNull String normalizedPath) {
        String fileName = normalizedPath;
        int separatorIndex = normalizedPath.lastIndexOf('/');
        if (separatorIndex >= 0 && separatorIndex < (normalizedPath.length() - 1)) {
            fileName = normalizedPath.substring(separatorIndex + 1);
        }
        return Integer.parseInt(Files.getNameWithoutExtension(fileName));
    }

    /** Normalizes the entry path for the FMA decoder. */
    @NotNull
    protected static String normalizeEntryPath(@NotNull String entryPath) {
        String normalized = entryPath.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.startsWith("./")) {
            normalized = normalized.substring(2);
        }
        return normalized;
    }

    /** Clears indexed metadata, closes the ZIP archive, and deletes a decoder-created temporary archive; safe to repeat. */
    @Override
    public void close() throws IOException {
        this.orderedFramePaths.clear();
        this.orderedIntroFramePaths.clear();
        this.entriesByNormalizedPath.clear();
        this.metadata = null;

        if (this.zipFile != null) {
            try {
                this.zipFile.close();
            } catch (Exception ex) {
                LOGGER.error("[KONKRETE] Failed to close FMA ZipFile", ex);
            }
            this.zipFile = null;
        }

        File tempArchive = this.tempArchiveFile;
        boolean shouldDeleteTempArchive = this.deleteTempArchiveOnClose;
        this.tempArchiveFile = null;
        this.deleteTempArchiveOnClose = false;

        if (shouldDeleteTempArchive && (tempArchive != null) && tempArchive.exists() && !tempArchive.delete()) {
            LOGGER.warn("[KONKRETE] Failed to delete temporary FMA archive: {}", tempArchive.getAbsolutePath());
        }
    }

    /** Describes serialized {@code FmaMetadata} structure consumed by the FMA decoder. */
    public static class FmaMetadata {

        /** Holds the loop count value used by this FMA decoder instance. */
        protected int loop_count;
        /** Holds the frame time value used by this FMA decoder instance. */
        protected long frame_time;
        /** Holds the frame time intro value used by this FMA decoder instance. */
        protected long frame_time_intro;
        /** Holds the custom frame times collection used by this FMA decoder instance. */
        protected Map<Integer, Long> custom_frame_times;
        /** Holds the custom frame times intro collection used by this FMA decoder instance. */
        protected Map<Integer, Long> custom_frame_times_intro;

        /** Returns the loop count used by this FMA decoder instance. */
        public int getLoopCount() {
            return this.loop_count;
        }

        /** Returns the frame time used by this FMA decoder instance. */
        public long getFrameTime() {
            return this.frame_time;
        }

        /** Returns the frame time intro used by this FMA decoder instance. */
        public long getFrameTimeIntro() {
            return this.frame_time_intro;
        }

        /** Returns the custom frame times, or {@code null} when it is not available. */
        @Nullable
        public Map<Integer, Long> getCustomFrameTimes() {
            return this.custom_frame_times;
        }

        /** Returns the custom frame times intro, or {@code null} when it is not available. */
        @Nullable
        public Map<Integer, Long> getCustomFrameTimesIntro() {
            return this.custom_frame_times_intro;
        }

        /** Returns the frame time for frame used by this FMA decoder instance. */
        public long getFrameTimeForFrame(int frame, boolean isIntroFrame) {
            if (isIntroFrame) {
                if ((custom_frame_times_intro != null) && custom_frame_times_intro.containsKey(frame)) return custom_frame_times_intro.get(frame);
            } else {
                if ((custom_frame_times != null) && custom_frame_times.containsKey(frame)) return custom_frame_times.get(frame);
            }
            return isIntroFrame ? this.getFrameTimeIntro() : this.getFrameTime();
        }

    }

    /** Describes serialized {@code FmaFrameIndex} structure consumed by the FMA decoder. */
    public static class FmaFrameIndex {
        /** Holds the frames collection used by this FMA decoder instance. */
        @Nullable
        protected List<String> frames;
        /** Holds the intro frames collection used by this FMA decoder instance. */
        @Nullable
        protected List<String> intro_frames;
    }

    /** Carries {@code ExpensiveFrameSample} data between validated stages of the FMA decoder. */
    public record ExpensiveFrameSample(@NotNull String framePath, int width, int height, int bitDepth, int colorType, int interlaceMethod) {
    }

    /** Describes serialized {@code PngFrameHeader} structure consumed by the FMA decoder. */
    protected record PngFrameHeader(int width, int height, int bitDepth, int colorType, int interlaceMethod) {
        /** Returns whether expensive. */
        protected boolean isExpensive() {
            return this.bitDepth > 8;
        }
    }

}
