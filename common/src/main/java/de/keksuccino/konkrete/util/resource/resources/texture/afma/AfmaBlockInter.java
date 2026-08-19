package de.keksuccino.konkrete.util.resource.resources.texture.afma;

import org.jetbrains.annotations.NotNull;

/** Models {@code AfmaBlockInter} state used by the AFMA codec. */
public class AfmaBlockInter {

    /** Holds the tile size value used by this AFMA codec instance. */
    protected int tile_size;

    /** Initializes a new {@code AfmaBlockInter} for AFMA codec use. */
    public AfmaBlockInter() {
    }

    /** Initializes a new {@code AfmaBlockInter} for AFMA codec use. */
    public AfmaBlockInter(int tileSize) {
        this.tile_size = tileSize;
    }

    /** Returns the tile size used by this AFMA codec instance. */
    public int getTileSize() {
        return this.tile_size;
    }

    /** Validates the current state before AFMA codec processing. */
    public void validate(@NotNull String context, int width, int height) {
        if (this.tile_size <= 0) {
            throw new IllegalArgumentException(context + " has an invalid block tile size: " + this.tile_size);
        }
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException(context + " has an invalid block region size: " + width + "x" + height);
        }
    }

}
