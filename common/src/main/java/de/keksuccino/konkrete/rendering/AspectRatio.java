package de.keksuccino.konkrete.rendering;

public class AspectRatio {

    protected final int width;
    protected final int height;

    public AspectRatio(int originalWidth, int originalHeight) {
        this.width = originalWidth;
        this.height = originalHeight;
    }

    public int getInputWidth() {
        return this.width;
    }

    public int getInputHeight() {
        return this.height;
    }

    public int getAspectRatioWidth(int givenHeight) {
        double ratio = (double)this.getInputWidth() / (double)this.getInputHeight();
        return (int)((double)givenHeight * ratio);
    }

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
