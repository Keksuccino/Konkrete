package de.keksuccino.konkrete.util.resource.resources.texture.afma;

import org.jetbrains.annotations.NotNull;

/** Carries {@code AfmaCopyRect} data between validated stages of the AFMA codec. */
public class AfmaCopyRect {

    /** Holds the src x value used by this AFMA codec instance. */
    protected int src_x;
    /** Holds the src y value used by this AFMA codec instance. */
    protected int src_y;
    /** Holds the dst x value used by this AFMA codec instance. */
    protected int dst_x;
    /** Holds the dst y value used by this AFMA codec instance. */
    protected int dst_y;
    /** Decoded canvas width in pixels for this AFMA codec instance. */
    protected int width;
    /** Decoded canvas height in pixels for this AFMA codec instance. */
    protected int height;

    /** Initializes a new {@code AfmaCopyRect} for AFMA codec use. */
    public AfmaCopyRect() {
    }

    /** Initializes a new {@code AfmaCopyRect} for AFMA codec use. */
    public AfmaCopyRect(int srcX, int srcY, int dstX, int dstY, int width, int height) {
        this.src_x = srcX;
        this.src_y = srcY;
        this.dst_x = dstX;
        this.dst_y = dstY;
        this.width = width;
        this.height = height;
    }

    /** Returns the src x used by this AFMA codec instance. */
    public int getSrcX() {
        return this.src_x;
    }

    /** Returns the src y used by this AFMA codec instance. */
    public int getSrcY() {
        return this.src_y;
    }

    /** Returns the dst x used by this AFMA codec instance. */
    public int getDstX() {
        return this.dst_x;
    }

    /** Returns the dst y used by this AFMA codec instance. */
    public int getDstY() {
        return this.dst_y;
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
    public void validate(@NotNull String context, int canvasWidth, int canvasHeight) {
        if (this.width <= 0 || this.height <= 0) {
            throw new IllegalArgumentException(context + " has invalid copy size " + this.width + "x" + this.height);
        }
        if (this.src_x < 0 || this.src_y < 0 || this.dst_x < 0 || this.dst_y < 0) {
            throw new IllegalArgumentException(context + " has negative copy coordinates");
        }
        if ((this.src_x + this.width) > canvasWidth || (this.dst_x + this.width) > canvasWidth) {
            throw new IllegalArgumentException(context + " copy rectangle exceeds canvas width");
        }
        if ((this.src_y + this.height) > canvasHeight || (this.dst_y + this.height) > canvasHeight) {
            throw new IllegalArgumentException(context + " copy rectangle exceeds canvas height");
        }
    }

}
