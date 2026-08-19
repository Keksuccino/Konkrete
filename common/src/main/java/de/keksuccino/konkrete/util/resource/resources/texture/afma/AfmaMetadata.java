package de.keksuccino.konkrete.util.resource.resources.texture.afma;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/** Describes serialized {@code AfmaMetadata} structure consumed by the AFMA codec. */
public class AfmaMetadata {

    /** Canonical AFMA format name written to archive metadata. */
    public static final String FORMAT_NAME = "AFMA";
    /** Serialized version value for legacy zip format data. */
    public static final int LEGACY_ZIP_FORMAT_VERSION = 7;
    /** Serialized version value for current format data. */
    public static final int CURRENT_FORMAT_VERSION = 8;
    /** Default keyframe interval used by the AFMA codec. */
    public static final int DEFAULT_KEYFRAME_INTERVAL = 30;

    /** Current format state for this AFMA codec instance. */
    @Nullable
    protected String format;
    /** Holds the format version value used by this AFMA codec instance. */
    protected int format_version;
    /** Holds the canvas width value used by this AFMA codec instance. */
    protected int canvas_width;
    /** Holds the canvas height value used by this AFMA codec instance. */
    protected int canvas_height;
    /** Holds the loop count value used by this AFMA codec instance. */
    protected int loop_count;
    /** Holds the frame time value used by this AFMA codec instance. */
    protected long frame_time;
    /** Holds the frame time intro value used by this AFMA codec instance. */
    protected long frame_time_intro;
    /** Holds the custom frame times collection used by this AFMA codec instance. */
    @Nullable
    protected Map<Integer, Long> custom_frame_times;
    /** Holds the custom frame times intro collection used by this AFMA codec instance. */
    @Nullable
    protected Map<Integer, Long> custom_frame_times_intro;
    /** Holds the keyframe interval value used by this AFMA codec instance. */
    protected int keyframe_interval;
    /** Current encoding state for this AFMA codec instance. */
    @Nullable
    protected Encoding encoding;
    /** Current creator state for this AFMA codec instance. */
    @Nullable
    protected Creator creator;

    /** Initializes a new {@code AfmaMetadata} for AFMA codec use. */
    public AfmaMetadata() {
    }

    /** Builds a resource value for the AFMA codec. */
    @NotNull
    public static AfmaMetadata create(int canvasWidth, int canvasHeight, int loopCount, long frameTime, long frameTimeIntro,
                                      @Nullable Map<Integer, Long> customFrameTimes, @Nullable Map<Integer, Long> customFrameTimesIntro,
                                      int keyframeInterval, boolean rectCopyEnabled, boolean duplicateFrameElision) {
        AfmaMetadata metadata = new AfmaMetadata();
        metadata.format = FORMAT_NAME;
        metadata.format_version = CURRENT_FORMAT_VERSION;
        metadata.canvas_width = canvasWidth;
        metadata.canvas_height = canvasHeight;
        metadata.loop_count = loopCount;
        metadata.frame_time = frameTime;
        metadata.frame_time_intro = (frameTimeIntro != frameTime) ? frameTimeIntro : 0L;
        metadata.custom_frame_times = copyIfNotEmpty(customFrameTimes);
        metadata.custom_frame_times_intro = copyIfNotEmpty(customFrameTimesIntro);
        metadata.keyframe_interval = keyframeInterval;
        metadata.encoding = (rectCopyEnabled && duplicateFrameElision) ? null : new Encoding("bin_intra", null, rectCopyEnabled, duplicateFrameElision);
        metadata.creator = null;
        return metadata;
    }

    /** Copies a non-empty timing map while preserving insertion order, or keeps the metadata field absent. */
    protected static @Nullable LinkedHashMap<Integer, Long> copyIfNotEmpty(@Nullable Map<Integer, Long> values) {
        if ((values == null) || values.isEmpty()) {
            return null;
        }
        return new LinkedHashMap<>(values);
    }

    /** Returns the format, or {@code null} when it is not available. */
    @Nullable
    public String getFormat() {
        return this.format;
    }

    /** Returns the format version used by this AFMA codec instance. */
    public int getFormatVersion() {
        return this.format_version;
    }

    /** Returns the canvas width used by this AFMA codec instance. */
    public int getCanvasWidth() {
        return this.canvas_width;
    }

    /** Returns the canvas height used by this AFMA codec instance. */
    public int getCanvasHeight() {
        return this.canvas_height;
    }

    /** Returns the loop count used by this AFMA codec instance. */
    public int getLoopCount() {
        return this.loop_count;
    }

    /** Returns the frame time used by this AFMA codec instance. */
    public long getFrameTime() {
        return this.frame_time;
    }

    /** Returns the frame time intro used by this AFMA codec instance. */
    public long getFrameTimeIntro() {
        return (this.frame_time_intro > 0L) ? this.frame_time_intro : this.frame_time;
    }

    /** Returns the custom frame times used by this AFMA codec instance. */
    @NotNull
    public Map<Integer, Long> getCustomFrameTimes() {
        return (this.custom_frame_times != null) ? this.custom_frame_times : Map.of();
    }

    /** Returns the custom frame times intro used by this AFMA codec instance. */
    @NotNull
    public Map<Integer, Long> getCustomFrameTimesIntro() {
        return (this.custom_frame_times_intro != null) ? this.custom_frame_times_intro : Map.of();
    }

    /** Returns the keyframe interval used by this AFMA codec instance. */
    public int getKeyframeInterval() {
        return (this.keyframe_interval > 0) ? this.keyframe_interval : DEFAULT_KEYFRAME_INTERVAL;
    }

    /** Returns the encoding, or {@code null} when it is not available. */
    @Nullable
    public Encoding getEncoding() {
        return this.encoding;
    }

    /** Returns the creator, or {@code null} when it is not available. */
    @Nullable
    public Creator getCreator() {
        return this.creator;
    }

    /** Returns the frame time for frame used by this AFMA codec instance. */
    public long getFrameTimeForFrame(int frameIndex, boolean introFrame) {
        Long customDelay = introFrame ? this.getCustomFrameTimesIntro().get(frameIndex) : this.getCustomFrameTimes().get(frameIndex);
        if ((customDelay != null) && (customDelay > 0L)) {
            return customDelay;
        }
        return introFrame ? this.getFrameTimeIntro() : this.getFrameTime();
    }

    /** Validates the current state before AFMA codec processing. */
    public void validate() {
        if ((this.format == null) || !FORMAT_NAME.equalsIgnoreCase(this.format)) {
            throw new IllegalArgumentException("metadata.json is missing the AFMA format marker");
        }
        if ((this.format_version != LEGACY_ZIP_FORMAT_VERSION) && (this.format_version != CURRENT_FORMAT_VERSION)) {
            throw new IllegalArgumentException("Unsupported AFMA format version: " + this.format_version);
        }
        if (this.canvas_width <= 0 || this.canvas_height <= 0) {
            throw new IllegalArgumentException("AFMA canvas size is invalid");
        }
        if (this.frame_time <= 0L) {
            throw new IllegalArgumentException("AFMA frame_time must be greater than 0");
        }
        if ((this.frame_time_intro <= 0L) && (this.frame_time <= 0L)) {
            throw new IllegalArgumentException("AFMA frame_time_intro must be greater than 0");
        }
    }

    /** Models {@code Encoding} state used by the AFMA codec. */
    public static class Encoding {

        /** Holds the intra payload codec value used by this AFMA codec instance. */
        @Nullable
        protected String intra_payload_codec;
        /** Holds the color model value used by this AFMA codec instance. */
        @Nullable
        protected String color_model;
        /** Tracks whether rect copy enabled is active for this AFMA codec instance. */
        protected boolean rect_copy_enabled;
        /** Tracks whether duplicate frame elision is active for this AFMA codec instance. */
        protected boolean duplicate_frame_elision;

        /** Initializes a new {@code Encoding} for AFMA codec use. */
        public Encoding() {
        }

        /** Initializes a new {@code Encoding} for AFMA codec use. */
        public Encoding(@Nullable String intraPayloadCodec, @Nullable String colorModel, boolean rectCopyEnabled, boolean duplicateFrameElision) {
            this.intra_payload_codec = intraPayloadCodec;
            this.color_model = colorModel;
            this.rect_copy_enabled = rectCopyEnabled;
            this.duplicate_frame_elision = duplicateFrameElision;
        }

        /** Returns the intra payload codec, or {@code null} when it is not available. */
        @Nullable
        public String getIntraPayloadCodec() {
            return this.intra_payload_codec;
        }

        /** Returns the color model, or {@code null} when it is not available. */
        @Nullable
        public String getColorModel() {
            return this.color_model;
        }

        /** Returns whether rect copy enabled. */
        public boolean isRectCopyEnabled() {
            return this.rect_copy_enabled;
        }

        /** Returns whether duplicate frame elision. */
        public boolean isDuplicateFrameElision() {
            return this.duplicate_frame_elision;
        }

    }

    /** Models {@code Creator} state used by the AFMA codec. */
    public static class Creator {

        /** Current tool state for this AFMA codec instance. */
        @Nullable
        protected String tool;
        /** Holds the tool version value used by this AFMA codec instance. */
        @Nullable
        protected String tool_version;
        /** Holds the created at utc value used by this AFMA codec instance. */
        @Nullable
        protected String created_at_utc;

        /** Initializes a new {@code Creator} for AFMA codec use. */
        public Creator() {
        }

        /** Initializes a new {@code Creator} for AFMA codec use. */
        public Creator(@Nullable String tool, @Nullable String toolVersion, @Nullable String createdAtUtc) {
            this.tool = tool;
            this.tool_version = toolVersion;
            this.created_at_utc = createdAtUtc;
        }

        /** Returns the tool, or {@code null} when it is not available. */
        @Nullable
        public String getTool() {
            return this.tool;
        }

        /** Returns the tool version, or {@code null} when it is not available. */
        @Nullable
        public String getToolVersion() {
            return this.tool_version;
        }

        /** Returns the created at utc, or {@code null} when it is not available. */
        @Nullable
        public String getCreatedAtUtc() {
            return this.created_at_utc;
        }

    }

}
