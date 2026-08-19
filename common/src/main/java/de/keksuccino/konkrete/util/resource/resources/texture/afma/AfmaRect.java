package de.keksuccino.konkrete.util.resource.resources.texture.afma;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Carries {@code AfmaRect} data between validated stages of the AFMA codec. */
public record AfmaRect(int x, int y, int width, int height) {

    /** Validates constructor components before storing this {@code AfmaRect}. */
    public AfmaRect {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Rectangle size must be positive");
        }
    }

    /** Calculates the area value for the AFMA codec. */
    public long area() {
        return (long)this.width * (long)this.height;
    }

    /** Converts the patch region from the AFMA codec representation. */
    @NotNull
    public AfmaPatchRegion toPatchRegion(@Nullable String path) {
        return new AfmaPatchRegion(path, this.x, this.y, this.width, this.height);
    }

    /** Combines resource state for the AFMA codec. */
    @Nullable
    public static AfmaRect union(@Nullable AfmaRect first, @Nullable AfmaRect second) {
        if (first == null) return second;
        if (second == null) return first;

        int minX = Math.min(first.x, second.x);
        int minY = Math.min(first.y, second.y);
        int maxX = Math.max(first.x + first.width, second.x + second.width);
        int maxY = Math.max(first.y + first.height, second.y + second.height);
        return new AfmaRect(minX, minY, maxX - minX, maxY - minY);
    }

}
