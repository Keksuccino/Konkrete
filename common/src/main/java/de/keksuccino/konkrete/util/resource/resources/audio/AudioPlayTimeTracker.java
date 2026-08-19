package de.keksuccino.konkrete.util.resource.resources.audio;

/** Tracks elapsed audio playback time across pause, seek, and restart transitions. */
public class AudioPlayTimeTracker {

    private long playStartTime = -1;
    private long totalPlayedTime = 0;
    private boolean isPaused = false;

    /** Updates audio resource state when play occurs. */
    public void onPlay() {
        if (isPaused) {
            // Resume from pause
            isPaused = false;
            playStartTime = System.currentTimeMillis();
        } else {
            // Fresh start
            reset();
            playStartTime = System.currentTimeMillis();
        }
    }

    /** Updates audio resource state when pause occurs. */
    public void onPause() {
        if (!isPaused && playStartTime != -1) {
            totalPlayedTime += System.currentTimeMillis() - playStartTime;
            isPaused = true;
        }
    }

    /** Updates audio resource state when stop occurs. */
    public void onStop() {
        reset();
    }

    /** Sets the play time used by subsequent audio resource operations. */
    public void setPlayTime(float seconds, boolean paused) {
        if (!Float.isFinite(seconds) || seconds < 0.0F) {
            seconds = 0.0F;
        }
        this.totalPlayedTime = (long) (seconds * 1000.0F);
        this.playStartTime = System.currentTimeMillis();
        this.isPaused = paused;
    }

    /** Restores initial audio resource state without transferring ownership. */
    public void reset() {
        playStartTime = -1;
        totalPlayedTime = 0;
        isPaused = false;
    }

    /** Returns the current play time used by this audio resource instance. */
    public float getCurrentPlayTime() {
        if (playStartTime == -1) return 0f;

        long currentTime = isPaused ? totalPlayedTime :
                totalPlayedTime + (System.currentTimeMillis() - playStartTime);

        return currentTime / 1000f; // Convert to seconds
    }

}
