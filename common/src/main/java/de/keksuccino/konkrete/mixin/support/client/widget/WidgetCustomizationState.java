package de.keksuccino.konkrete.mixin.support.client.widget;

import com.mojang.math.Axis;
import de.keksuccino.konkrete.util.rendering.DrawableColor;
import de.keksuccino.konkrete.util.rendering.RenderRotationUtil;
import de.keksuccino.konkrete.util.rendering.ui.widget.CustomizableWidget;
import de.keksuccino.konkrete.util.resource.PlayableResource;
import de.keksuccino.konkrete.util.resource.RenderableResource;
import de.keksuccino.konkrete.util.resource.resources.audio.IAudio;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** Holds reusable widget customization state outside the lightweight target Mixin. */
public final class WidgetCustomizationState {

    private static final int DEFAULT_NINE_SLICE_BORDER = 5;
    @Nullable private String widgetIdentifier;
    @Nullable private Component customLabel;
    @Nullable private Component hoverLabel;
    @Nullable private DrawableColor labelHoverColor;
    @Nullable private DrawableColor labelBaseColor;
    private float labelScale = 1.0F;
    private boolean labelShadow = true;
    private boolean underlineLabelOnHover;
    @Nullable private IAudio customClickSound;
    @Nullable private IAudio hoverSound;
    @Nullable private IAudio unhoverSound;
    private boolean hidden;
    @Nullable private RenderableResource customBackgroundNormal;
    @Nullable private RenderableResource customBackgroundHover;
    @Nullable private RenderableResource customBackgroundInactive;
    @NotNull private CustomizableWidget.CustomBackgroundResetBehavior customBackgroundResetBehavior = CustomizableWidget.CustomBackgroundResetBehavior.RESET_NEVER;
    private boolean nineSliceCustomBackground;
    private int nineSliceBorderX = DEFAULT_NINE_SLICE_BORDER;
    private int nineSliceBorderY = DEFAULT_NINE_SLICE_BORDER;
    private int nineSliceBorderTop = DEFAULT_NINE_SLICE_BORDER;
    private int nineSliceBorderRight = DEFAULT_NINE_SLICE_BORDER;
    private int nineSliceBorderBottom = DEFAULT_NINE_SLICE_BORDER;
    private int nineSliceBorderLeft = DEFAULT_NINE_SLICE_BORDER;
    @Nullable private Integer customWidth;
    @Nullable private Integer customHeight;
    @Nullable private Integer customX;
    @Nullable private Integer customY;
    @Nullable private Integer originalWidth;
    @Nullable private Integer originalHeight;
    private float hitboxRotationDegrees;
    private float hitboxVerticalTiltDegrees;
    private float hitboxHorizontalTiltDegrees;
    private boolean hitboxRotationActive;
    private float hitboxInverseRotation00 = 1.0F;
    private float hitboxInverseRotation01;
    private float hitboxInverseRotation10;
    private float hitboxInverseRotation11 = 1.0F;
    private final List<Runnable> resetCustomizationsListeners = new ArrayList<>();
    private final List<Consumer<Boolean>> hoverStateListeners = new ArrayList<>();
    private final List<Consumer<Boolean>> focusStateListeners = new ArrayList<>();
    private final List<Consumer<Boolean>> hoverOrFocusStateListeners = new ArrayList<>();
    private boolean lastHoverState;
    private boolean lastFocusState;
    private boolean lastHoverOrFocusState;

    public void resetCustomizationValues() {
        stop(this.customBackgroundNormal);
        stop(this.customBackgroundHover);
        stop(this.customBackgroundInactive);
        stop(this.customClickSound);
        stop(this.hoverSound);
        stop(this.unhoverSound);
        this.customBackgroundNormal = null;
        this.customBackgroundHover = null;
        this.customBackgroundInactive = null;
        this.customBackgroundResetBehavior = CustomizableWidget.CustomBackgroundResetBehavior.RESET_NEVER;
        this.nineSliceCustomBackground = false;
        this.resetNineSliceBorders();
        this.customClickSound = null;
        this.hoverSound = null;
        this.unhoverSound = null;
        this.hidden = false;
        this.customLabel = null;
        this.hoverLabel = null;
        this.underlineLabelOnHover = false;
        this.labelShadow = true;
        this.labelHoverColor = null;
        this.labelBaseColor = null;
        this.labelScale = 1.0F;
        this.clearCustomBounds();
        this.setHitboxRotation(0.0F, 0.0F, 0.0F);
    }

    public void clearCustomBounds() {
        this.customWidth = null;
        this.customHeight = null;
        this.customX = null;
        this.customY = null;
    }

    public void clearOriginalSize() {
        this.originalWidth = null;
        this.originalHeight = null;
    }

    public void captureOriginalWidth(int width) {
        if (this.originalWidth == null) this.originalWidth = width;
    }

    public void captureOriginalHeight(int height) {
        if (this.originalHeight == null) this.originalHeight = height;
    }

    public void stopBackgrounds() {
        stop(this.customBackgroundNormal);
        stop(this.customBackgroundHover);
        stop(this.customBackgroundInactive);
    }

    private void resetNineSliceBorders() {
        this.nineSliceBorderX = DEFAULT_NINE_SLICE_BORDER;
        this.nineSliceBorderY = DEFAULT_NINE_SLICE_BORDER;
        this.nineSliceBorderTop = DEFAULT_NINE_SLICE_BORDER;
        this.nineSliceBorderRight = DEFAULT_NINE_SLICE_BORDER;
        this.nineSliceBorderBottom = DEFAULT_NINE_SLICE_BORDER;
        this.nineSliceBorderLeft = DEFAULT_NINE_SLICE_BORDER;
    }

    private static void stop(@Nullable Object resource) {
        if (resource instanceof PlayableResource playable) playable.stop();
    }

    public void setHitboxRotation(float rotationDegrees, float verticalTiltDegrees, float horizontalTiltDegrees) {
        this.hitboxRotationDegrees = rotationDegrees;
        this.hitboxVerticalTiltDegrees = verticalTiltDegrees;
        this.hitboxHorizontalTiltDegrees = horizontalTiltDegrees;
        this.updateHitboxRotationMatrix();
    }

    private void updateHitboxRotationMatrix() {
        float rotation = this.hitboxRotationDegrees;
        float verticalTilt = this.hitboxVerticalTiltDegrees;
        float horizontalTilt = this.hitboxHorizontalTiltDegrees;
        if (rotation == 0.0F && verticalTilt == 0.0F && horizontalTilt == 0.0F) {
            this.resetHitboxRotationMatrix();
            return;
        }
        RenderRotationUtil.RotationState state = new RenderRotationUtil.RotationState();
        if (verticalTilt != 0.0F) state.mul(Axis.XP.rotationDegrees(verticalTilt));
        if (horizontalTilt != 0.0F) state.mul(Axis.YP.rotationDegrees(horizontalTilt));
        if (rotation != 0.0F) state.mul(Axis.ZP.rotationDegrees(rotation));
        float m00 = 1.0F - (2.0F * state.y * state.y) - (2.0F * state.z * state.z);
        float m01 = (2.0F * state.x * state.y) - (2.0F * state.z * state.w);
        float m10 = (2.0F * state.x * state.y) + (2.0F * state.z * state.w);
        float m11 = 1.0F - (2.0F * state.x * state.x) - (2.0F * state.z * state.z);
        float determinant = (m00 * m11) - (m01 * m10);
        if (!Float.isFinite(determinant) || Math.abs(determinant) < 1.0E-6F) {
            this.resetHitboxRotationMatrix();
            return;
        }
        float inverseDeterminant = 1.0F / determinant;
        this.hitboxRotationActive = true;
        this.hitboxInverseRotation00 = m11 * inverseDeterminant;
        this.hitboxInverseRotation01 = -m01 * inverseDeterminant;
        this.hitboxInverseRotation10 = -m10 * inverseDeterminant;
        this.hitboxInverseRotation11 = m00 * inverseDeterminant;
    }

    private void resetHitboxRotationMatrix() {
        this.hitboxRotationActive = false;
        this.hitboxInverseRotation00 = 1.0F;
        this.hitboxInverseRotation01 = 0.0F;
        this.hitboxInverseRotation10 = 0.0F;
        this.hitboxInverseRotation11 = 1.0F;
    }

    public boolean containsRotatedPoint(double mouseX, double mouseY, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return false;
        float centerX = x + (width / 2.0F);
        float centerY = y + (height / 2.0F);
        float dx = (float) mouseX - centerX;
        float dy = (float) mouseY - centerY;
        float localX = (this.hitboxInverseRotation00 * dx) + (this.hitboxInverseRotation01 * dy);
        float localY = (this.hitboxInverseRotation10 * dx) + (this.hitboxInverseRotation11 * dy);
        float halfWidth = width / 2.0F;
        float halfHeight = height / 2.0F;
        return localX >= -halfWidth && localX < halfWidth && localY >= -halfHeight && localY < halfHeight;
    }

    @Nullable public String getWidgetIdentifier() { return this.widgetIdentifier; }
    public void setWidgetIdentifier(@Nullable String widgetIdentifier) { this.widgetIdentifier = widgetIdentifier; }
    @Nullable public Component getCustomLabel() { return this.customLabel; }
    public void setCustomLabel(@Nullable Component customLabel) { this.customLabel = customLabel; }
    @Nullable public Component getHoverLabel() { return this.hoverLabel; }
    public void setHoverLabel(@Nullable Component hoverLabel) { this.hoverLabel = hoverLabel; }
    @Nullable public DrawableColor getLabelHoverColor() { return this.labelHoverColor; }
    public void setLabelHoverColor(@Nullable DrawableColor labelHoverColor) { this.labelHoverColor = labelHoverColor; }
    @Nullable public DrawableColor getLabelBaseColor() { return this.labelBaseColor; }
    public void setLabelBaseColor(@Nullable DrawableColor labelBaseColor) { this.labelBaseColor = labelBaseColor; }
    public float getLabelScale() { return this.labelScale; }
    public void setLabelScale(float labelScale) { this.labelScale = labelScale; }
    public boolean isLabelShadow() { return this.labelShadow; }
    public void setLabelShadow(boolean labelShadow) { this.labelShadow = labelShadow; }
    public boolean isUnderlineLabelOnHover() { return this.underlineLabelOnHover; }
    public void setUnderlineLabelOnHover(boolean underlineLabelOnHover) { this.underlineLabelOnHover = underlineLabelOnHover; }
    @Nullable public IAudio getCustomClickSound() { return this.customClickSound; }
    public void setCustomClickSound(@Nullable IAudio customClickSound) { this.customClickSound = customClickSound; }
    @Nullable public IAudio getHoverSound() { return this.hoverSound; }
    public void setHoverSound(@Nullable IAudio hoverSound) { this.hoverSound = hoverSound; }
    @Nullable public IAudio getUnhoverSound() { return this.unhoverSound; }
    public void setUnhoverSound(@Nullable IAudio unhoverSound) { this.unhoverSound = unhoverSound; }
    public boolean isHidden() { return this.hidden; }
    public void setHidden(boolean hidden) { this.hidden = hidden; }
    @Nullable public RenderableResource getCustomBackgroundNormal() { return this.customBackgroundNormal; }
    public void setCustomBackgroundNormal(@Nullable RenderableResource customBackgroundNormal) { this.customBackgroundNormal = customBackgroundNormal; }
    @Nullable public RenderableResource getCustomBackgroundHover() { return this.customBackgroundHover; }
    public void setCustomBackgroundHover(@Nullable RenderableResource customBackgroundHover) { this.customBackgroundHover = customBackgroundHover; }
    @Nullable public RenderableResource getCustomBackgroundInactive() { return this.customBackgroundInactive; }
    public void setCustomBackgroundInactive(@Nullable RenderableResource customBackgroundInactive) { this.customBackgroundInactive = customBackgroundInactive; }
    @NotNull public CustomizableWidget.CustomBackgroundResetBehavior getCustomBackgroundResetBehavior() { return this.customBackgroundResetBehavior; }
    public void setCustomBackgroundResetBehavior(@NotNull CustomizableWidget.CustomBackgroundResetBehavior behavior) { this.customBackgroundResetBehavior = Objects.requireNonNull(behavior, "behavior"); }
    public boolean isNineSliceCustomBackground() { return this.nineSliceCustomBackground; }
    public void setNineSliceCustomBackground(boolean nineSliceCustomBackground) { this.nineSliceCustomBackground = nineSliceCustomBackground; }
    public int getNineSliceBorderX() { return this.nineSliceBorderX; }
    public void setNineSliceBorderX(int borderX) { this.nineSliceBorderX = borderX; this.nineSliceBorderLeft = borderX; this.nineSliceBorderRight = borderX; }
    public int getNineSliceBorderY() { return this.nineSliceBorderY; }
    public void setNineSliceBorderY(int borderY) { this.nineSliceBorderY = borderY; this.nineSliceBorderTop = borderY; this.nineSliceBorderBottom = borderY; }
    public int getNineSliceBorderTop() { return this.nineSliceBorderTop; }
    public void setNineSliceBorderTop(int borderTop) { this.nineSliceBorderTop = borderTop; }
    public int getNineSliceBorderRight() { return this.nineSliceBorderRight; }
    public void setNineSliceBorderRight(int borderRight) { this.nineSliceBorderRight = borderRight; }
    public int getNineSliceBorderBottom() { return this.nineSliceBorderBottom; }
    public void setNineSliceBorderBottom(int borderBottom) { this.nineSliceBorderBottom = borderBottom; }
    public int getNineSliceBorderLeft() { return this.nineSliceBorderLeft; }
    public void setNineSliceBorderLeft(int borderLeft) { this.nineSliceBorderLeft = borderLeft; }
    @Nullable public Integer getCustomWidth() { return this.customWidth; }
    public void setCustomWidth(@Nullable Integer customWidth) { this.customWidth = customWidth; }
    @Nullable public Integer getCustomHeight() { return this.customHeight; }
    public void setCustomHeight(@Nullable Integer customHeight) { this.customHeight = customHeight; }
    @Nullable public Integer getCustomX() { return this.customX; }
    public void setCustomX(@Nullable Integer customX) { this.customX = customX; }
    @Nullable public Integer getCustomY() { return this.customY; }
    public void setCustomY(@Nullable Integer customY) { this.customY = customY; }
    @Nullable public Integer getOriginalWidth() { return this.originalWidth; }
    @Nullable public Integer getOriginalHeight() { return this.originalHeight; }
    public float getHitboxRotationDegrees() { return this.hitboxRotationDegrees; }
    public float getHitboxVerticalTiltDegrees() { return this.hitboxVerticalTiltDegrees; }
    public float getHitboxHorizontalTiltDegrees() { return this.hitboxHorizontalTiltDegrees; }
    public boolean isHitboxRotationActive() { return this.hitboxRotationActive; }
    @NotNull public List<Runnable> getResetCustomizationsListeners() { return this.resetCustomizationsListeners; }
    @NotNull public List<Consumer<Boolean>> getHoverStateListeners() { return this.hoverStateListeners; }
    @NotNull public List<Consumer<Boolean>> getFocusStateListeners() { return this.focusStateListeners; }
    @NotNull public List<Consumer<Boolean>> getHoverOrFocusStateListeners() { return this.hoverOrFocusStateListeners; }
    public boolean getLastHoverState() { return this.lastHoverState; }
    public void setLastHoverState(boolean state) { this.lastHoverState = state; }
    public boolean getLastFocusState() { return this.lastFocusState; }
    public void setLastFocusState(boolean state) { this.lastFocusState = state; }
    public boolean getLastHoverOrFocusState() { return this.lastHoverOrFocusState; }
    public void setLastHoverOrFocusState(boolean state) { this.lastHoverOrFocusState = state; }
}
