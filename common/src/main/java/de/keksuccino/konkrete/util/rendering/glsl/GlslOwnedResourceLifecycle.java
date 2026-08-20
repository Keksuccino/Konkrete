package de.keksuccino.konkrete.util.rendering.glsl;

/** Pure ownership policy for releasing runtime resources when a GLSL owner stops producing render work. */
public final class GlslOwnedResourceLifecycle {

    private boolean resourcesMayBeOwned;

    /** Returns whether renderable area. */
    public static boolean hasRenderableArea(int width, int height) {
        return width > 0 && height > 0;
    }

    /** Records that one GUI extraction cycle completed without releasing owned resources. */
    public boolean completeExtractionCycle(boolean extractedThisCycle, boolean releaseAllowed) {
        if (extractedThisCycle) {
            this.resourcesMayBeOwned = true;
            return false;
        }
        if (!this.resourcesMayBeOwned || !releaseAllowed) {
            return false;
        }
        this.resourcesMayBeOwned = false;
        return true;
    }

    /** Marks resources released for the next lifecycle phase. */
    public void markResourcesReleased() {
        this.resourcesMayBeOwned = false;
    }

}
