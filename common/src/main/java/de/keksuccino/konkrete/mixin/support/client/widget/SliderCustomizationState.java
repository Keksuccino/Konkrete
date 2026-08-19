package de.keksuccino.konkrete.mixin.support.client.widget;

import de.keksuccino.konkrete.util.resource.PlayableResource;
import de.keksuccino.konkrete.util.resource.RenderableResource;
import org.jetbrains.annotations.Nullable;

/** Holds custom slider-track and handle state shared by vanilla and extended sliders. */
public final class SliderCustomizationState {

    private static final int DEFAULT_NINE_SLICE_BORDER = 5;
    @Nullable private RenderableResource backgroundNormal;
    @Nullable private RenderableResource backgroundHighlighted;
    private boolean nineSliceBackground;
    private int backgroundBorderX = DEFAULT_NINE_SLICE_BORDER;
    private int backgroundBorderY = DEFAULT_NINE_SLICE_BORDER;
    private int backgroundBorderTop = DEFAULT_NINE_SLICE_BORDER;
    private int backgroundBorderRight = DEFAULT_NINE_SLICE_BORDER;
    private int backgroundBorderBottom = DEFAULT_NINE_SLICE_BORDER;
    private int backgroundBorderLeft = DEFAULT_NINE_SLICE_BORDER;
    private boolean nineSliceHandle;
    private int handleBorderX = DEFAULT_NINE_SLICE_BORDER;
    private int handleBorderY = DEFAULT_NINE_SLICE_BORDER;
    private int handleBorderTop = DEFAULT_NINE_SLICE_BORDER;
    private int handleBorderRight = DEFAULT_NINE_SLICE_BORDER;
    private int handleBorderBottom = DEFAULT_NINE_SLICE_BORDER;
    private int handleBorderLeft = DEFAULT_NINE_SLICE_BORDER;

    public void reset() {
        this.stopBackgrounds();
        this.backgroundNormal = null;
        this.backgroundHighlighted = null;
        this.nineSliceBackground = false;
        this.backgroundBorderX = DEFAULT_NINE_SLICE_BORDER;
        this.backgroundBorderY = DEFAULT_NINE_SLICE_BORDER;
        this.backgroundBorderTop = DEFAULT_NINE_SLICE_BORDER;
        this.backgroundBorderRight = DEFAULT_NINE_SLICE_BORDER;
        this.backgroundBorderBottom = DEFAULT_NINE_SLICE_BORDER;
        this.backgroundBorderLeft = DEFAULT_NINE_SLICE_BORDER;
        this.nineSliceHandle = false;
        this.handleBorderX = DEFAULT_NINE_SLICE_BORDER;
        this.handleBorderY = DEFAULT_NINE_SLICE_BORDER;
        this.handleBorderTop = DEFAULT_NINE_SLICE_BORDER;
        this.handleBorderRight = DEFAULT_NINE_SLICE_BORDER;
        this.handleBorderBottom = DEFAULT_NINE_SLICE_BORDER;
        this.handleBorderLeft = DEFAULT_NINE_SLICE_BORDER;
    }

    public void stopBackgrounds() {
        if (this.backgroundNormal instanceof PlayableResource playable) playable.stop();
        if (this.backgroundHighlighted instanceof PlayableResource playable) playable.stop();
    }

    @Nullable public RenderableResource getBackgroundNormal() { return this.backgroundNormal; }
    public void setBackgroundNormal(@Nullable RenderableResource backgroundNormal) { this.backgroundNormal = backgroundNormal; }
    @Nullable public RenderableResource getBackgroundHighlighted() { return this.backgroundHighlighted; }
    public void setBackgroundHighlighted(@Nullable RenderableResource backgroundHighlighted) { this.backgroundHighlighted = backgroundHighlighted; }
    public boolean isNineSliceBackground() { return this.nineSliceBackground; }
    public void setNineSliceBackground(boolean nineSliceBackground) { this.nineSliceBackground = nineSliceBackground; }
    public int getBackgroundBorderX() { return this.backgroundBorderX; }
    public void setBackgroundBorderX(int border) { this.backgroundBorderX = border; this.backgroundBorderLeft = border; this.backgroundBorderRight = border; }
    public int getBackgroundBorderY() { return this.backgroundBorderY; }
    public void setBackgroundBorderY(int border) { this.backgroundBorderY = border; this.backgroundBorderTop = border; this.backgroundBorderBottom = border; }
    public int getBackgroundBorderTop() { return this.backgroundBorderTop; }
    public void setBackgroundBorderTop(int border) { this.backgroundBorderTop = border; }
    public int getBackgroundBorderRight() { return this.backgroundBorderRight; }
    public void setBackgroundBorderRight(int border) { this.backgroundBorderRight = border; }
    public int getBackgroundBorderBottom() { return this.backgroundBorderBottom; }
    public void setBackgroundBorderBottom(int border) { this.backgroundBorderBottom = border; }
    public int getBackgroundBorderLeft() { return this.backgroundBorderLeft; }
    public void setBackgroundBorderLeft(int border) { this.backgroundBorderLeft = border; }
    public boolean isNineSliceHandle() { return this.nineSliceHandle; }
    public void setNineSliceHandle(boolean nineSliceHandle) { this.nineSliceHandle = nineSliceHandle; }
    public int getHandleBorderX() { return this.handleBorderX; }
    public void setHandleBorderX(int border) { this.handleBorderX = border; this.handleBorderLeft = border; this.handleBorderRight = border; }
    public int getHandleBorderY() { return this.handleBorderY; }
    public void setHandleBorderY(int border) { this.handleBorderY = border; this.handleBorderTop = border; this.handleBorderBottom = border; }
    public int getHandleBorderTop() { return this.handleBorderTop; }
    public void setHandleBorderTop(int border) { this.handleBorderTop = border; }
    public int getHandleBorderRight() { return this.handleBorderRight; }
    public void setHandleBorderRight(int border) { this.handleBorderRight = border; }
    public int getHandleBorderBottom() { return this.handleBorderBottom; }
    public void setHandleBorderBottom(int border) { this.handleBorderBottom = border; }
    public int getHandleBorderLeft() { return this.handleBorderLeft; }
    public void setHandleBorderLeft(int border) { this.handleBorderLeft = border; }
}
