package de.keksuccino.konkrete.util.resource.resources.audio;

/** Exposes the owned OpenAL source handle of a closeable audio resource. */
public interface ALAudio {

    /** Returns the al source used by this audio resource instance. */
    public int getALSource();

}
