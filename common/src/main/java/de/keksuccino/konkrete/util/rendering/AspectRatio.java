package de.keksuccino.konkrete.util.rendering;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Preserves source dimensions while fitting them within requested bounds. */
public class AspectRatio {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Source width used to compute the ratio. */
    protected final int width;
    /** Source height used to compute the ratio. */
    protected final int height;

    /** Reduces original pixel dimensions to their simplest integer ratio. */
    public AspectRatio(int originalWidth, int originalHeight) {
        this.width = originalWidth;
        this.height = originalHeight;
    }

    /** Returns input width. */
    public int getInputWidth() {
        return this.width;
    }

    /** Returns input height. */
    public int getInputHeight() {
        return this.height;
    }

    /** Returns aspect ratio width. */
    public int getAspectRatioWidth(int givenHeight) {
        double ratio = (double)this.getInputWidth() / (double)this.getInputHeight();
        return (int)((double)givenHeight * ratio);
    }

    /** Returns aspect ratio height. */
    public int getAspectRatioHeight(int givenWidth) {
        double ratio = (double)this.getInputWidth() / (double)this.getInputHeight();
        return (int)((double)givenWidth / ratio);
    }

    /**
     * Will calculate the aspect ratio and never gets <b>smaller</b> than the given width and height.
     *
     * @return The nearest possible width (index 0) and height (index 1).
     **/
    public int[] getAspectRatioSizeByMinimumSize(int givenWidth, int givenHeight) {
        int aw = this.getAspectRatioWidth(givenHeight);
        int ah = givenHeight;
        if (aw < givenWidth) {
            ah = this.getAspectRatioHeight(givenWidth);
            aw = givenWidth;
        }
        return new int[]{aw,ah};
    }

    /**
     * Will calculate the aspect ratio and never gets <b>bigger</b> than the given width and height.
     *
     * @return The nearest possible width (index 0) and height (index 1).
     **/
    public int[] getAspectRatioSizeByMaximumSize(int givenWidth, int givenHeight) {
        int aw = this.getAspectRatioWidth(givenHeight);
        int ah = givenHeight;
        if (aw > givenWidth) {
            ah = this.getAspectRatioHeight(givenWidth);
            aw = givenWidth;
        }
        return new int[]{aw,ah};
    }

}
