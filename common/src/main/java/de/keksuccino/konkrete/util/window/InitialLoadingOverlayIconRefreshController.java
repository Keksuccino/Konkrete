package de.keksuccino.konkrete.util.window;

/**
 * Requests one final icon refresh when the client's initial loading overlay lifecycle finishes.
 * Later resource-pack reload overlays are deliberately ignored.
 */
public final class InitialLoadingOverlayIconRefreshController {

    private Phase phase = Phase.WAITING_FOR_INITIAL_LOADING_OVERLAY;

    /** Creates a controller waiting for the first loading-overlay assignment. */
    public InitialLoadingOverlayIconRefreshController() {}

    /**
     * Observes the overlay state after Minecraft assigns an overlay transition.
     *
     * @param loadingOverlayActive whether the newly assigned overlay is a loading overlay
     * @return whether the configured icon should now be refreshed
     */
    public boolean afterOverlayAssignment(boolean loadingOverlayActive) {
        if (this.phase == Phase.WAITING_FOR_INITIAL_LOADING_OVERLAY) {
            if (loadingOverlayActive) this.phase = Phase.INITIAL_LOADING_OVERLAY_ACTIVE;
            return false;
        }
        if (this.phase == Phase.INITIAL_LOADING_OVERLAY_ACTIVE && !loadingOverlayActive) {
            this.phase = Phase.COMPLETE;
            return true;
        }
        return false;
    }

    private enum Phase {

        WAITING_FOR_INITIAL_LOADING_OVERLAY,
        INITIAL_LOADING_OVERLAY_ACTIVE,
        COMPLETE

    }

}
