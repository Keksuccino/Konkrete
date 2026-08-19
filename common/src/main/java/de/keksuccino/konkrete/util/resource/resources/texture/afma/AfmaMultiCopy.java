package de.keksuccino.konkrete.util.resource.resources.texture.afma;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Models {@code AfmaMultiCopy} state used by the AFMA codec. */
public class AfmaMultiCopy {

    /** Holds the copy rects collection used by this AFMA codec instance. */
    @Nullable
    protected List<AfmaCopyRect> copy_rects;

    /** Initializes a new {@code AfmaMultiCopy} for AFMA codec use. */
    public AfmaMultiCopy() {
    }

    /** Initializes a new {@code AfmaMultiCopy} for AFMA codec use. */
    public AfmaMultiCopy(@NotNull List<AfmaCopyRect> copyRects) {
        Objects.requireNonNull(copyRects);
        if (copyRects.size() < 2) {
            throw new IllegalArgumentException("AFMA multi-copy operations require at least 2 copy rectangles");
        }
        ArrayList<AfmaCopyRect> copiedRects = new ArrayList<>(copyRects.size());
        for (AfmaCopyRect copyRect : copyRects) {
            copiedRects.add(Objects.requireNonNull(copyRect, "AFMA multi-copy rectangles cannot contain NULL entries"));
        }
        this.copy_rects = Collections.unmodifiableList(copiedRects);
    }

    /** Returns the copy rects used by this AFMA codec instance. */
    @NotNull
    public List<AfmaCopyRect> getCopyRects() {
        return (this.copy_rects != null) ? this.copy_rects : List.of();
    }

    /** Returns the copy rect count used by this AFMA codec instance. */
    public int getCopyRectCount() {
        return this.getCopyRects().size();
    }

    /** Returns the total area used by this AFMA codec instance. */
    public long getTotalArea() {
        long totalArea = 0L;
        for (AfmaCopyRect copyRect : this.getCopyRects()) {
            totalArea += copyRect.getArea();
        }
        return totalArea;
    }

    /** Validates the current state before AFMA codec processing. */
    public void validate(@NotNull String context, int canvasWidth, int canvasHeight) {
        List<AfmaCopyRect> copyRects = this.getCopyRects();
        if (copyRects.size() < 2) {
            throw new IllegalArgumentException(context + " requires at least 2 copy rectangles");
        }
        for (int copyIndex = 0; copyIndex < copyRects.size(); copyIndex++) {
            Objects.requireNonNull(copyRects.get(copyIndex), context + " contains a NULL copy rectangle")
                    .validate(context + " copy rectangle " + copyIndex, canvasWidth, canvasHeight);
        }
    }

}
