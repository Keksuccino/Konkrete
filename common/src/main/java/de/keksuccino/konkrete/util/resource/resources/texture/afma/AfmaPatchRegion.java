package de.keksuccino.konkrete.util.resource.resources.texture.afma;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Carries {@code AfmaPatchRegion} data between validated stages of the AFMA codec. */
public class AfmaPatchRegion {

    /** Current path state for this AFMA codec instance. */
    @Nullable
    protected String path;
    /** Current x state for this AFMA codec instance. */
    protected int x;
    /** Current y state for this AFMA codec instance. */
    protected int y;
    /** Decoded canvas width in pixels for this AFMA codec instance. */
    protected int width;
    /** Decoded canvas height in pixels for this AFMA codec instance. */
    protected int height;

    /** Initializes a new {@code AfmaPatchRegion} for AFMA codec use. */
    public AfmaPatchRegion() {
    }

    /** Initializes a new {@code AfmaPatchRegion} for AFMA codec use. */
    public AfmaPatchRegion(@Nullable String path, int x, int y, int width, int height) {
        this.path = path;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /** Returns the path, or {@code null} when it is not available. */
    @Nullable
    public String getPath() {
        return this.path;
    }

    /** Returns the x used by this AFMA codec instance. */
    public int getX() {
        return this.x;
    }

    /** Returns the y used by this AFMA codec instance. */
    public int getY() {
        return this.y;
    }

    /** Returns the width used by this AFMA codec instance. */
    public int getWidth() {
        return this.width;
    }

    /** Returns the height used by this AFMA codec instance. */
    public int getHeight() {
        return this.height;
    }

    /** Returns the area used by this AFMA codec instance. */
    public long getArea() {
        return (long)this.width * (long)this.height;
    }

    /** Validates the current state before AFMA codec processing. */
    public void validate(@NotNull String context, int canvasWidth, int canvasHeight, boolean requirePath) {
        if (requirePath && ((this.path == null) || this.path.isBlank())) {
            throw new IllegalArgumentException(context + " is missing its patch path");
        }
        if (this.width <= 0 || this.height <= 0) {
            throw new IllegalArgumentException(context + " has invalid patch size " + this.width + "x" + this.height);
        }
        if (this.x < 0 || this.y < 0) {
            throw new IllegalArgumentException(context + " has negative patch coordinates");
        }
        if ((this.x + this.width) > canvasWidth || (this.y + this.height) > canvasHeight) {
            throw new IllegalArgumentException(context + " patch rectangle exceeds canvas bounds");
        }
    }

}
