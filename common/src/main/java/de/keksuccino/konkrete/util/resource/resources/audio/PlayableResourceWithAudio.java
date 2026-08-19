package de.keksuccino.konkrete.util.resource.resources.audio;

import de.keksuccino.konkrete.util.resource.PlayableResource;

/** Adds normalized volume control to a playable resource. */
public interface PlayableResourceWithAudio extends PlayableResource {

    /** Sets the resource-local linear playback volume. */
    void setVolume(float volume);

    /** Returns the resource-local linear playback volume. */
    float getVolume();

}
