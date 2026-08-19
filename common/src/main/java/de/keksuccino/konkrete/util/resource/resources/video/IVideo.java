package de.keksuccino.konkrete.util.resource.resources.video;

import de.keksuccino.konkrete.util.resource.RenderableResource;
import de.keksuccino.konkrete.util.resource.resources.audio.PlayableResourceWithAudio;

/** Controls synchronized video playback, seeking, looping, and texture access. */
public interface IVideo extends RenderableResource, PlayableResourceWithAudio {

    /**
     * Returns the duration in seconds.
     */
    default float getDuration() {
        return 0.0F;
    }

    /**
     * Returns the current play time in seconds.
     */
    default float getPlayTime() {
        return 0.0F;
    }

    /**
     * Seeks to the given play time in seconds.
     */
    default void setPlayTime(float playTime) {
    }

    /** Returns whether playback reached the terminal frame. */
    default boolean isEnded() {
        return false;
    }

    /** Configures whether playback restarts after reaching the end. */
    default void setLooping(boolean looping) {
    }

    /** Returns whether playback is configured to restart. */
    default boolean isLooping() {
        return false;
    }

}
