package de.keksuccino.konkrete.util.rendering.entity;

import de.keksuccino.konkrete.util.rendering.ui.widget.NavigatableWidget;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.UUID;

/**
 * Optional-dependency-safe wrapper around Fancy Entity Renderer's player widget.
 * The class can be linked without Fancy Entity Renderer; {@link #build(int, int, int, int)} fails with a concrete
 * compatibility reason when the optional API is unavailable. Call {@link #close()} when permanently discarding a widget
 * to release Konkrete's reference to the dependency-owned delegate.
 */
@SuppressWarnings("unused")
public class WrappedFancyPlayerWidget extends AbstractWidget implements NavigatableWidget, AutoCloseable {

    @Nullable
    private volatile FancyPlayerWidgetBridge wrapped;

    /** Creates a player widget after validating the complete optional API contract. */
    @Nonnull
    public static WrappedFancyPlayerWidget build(int x, int y, int width, int height) {
        return new WrappedFancyPlayerWidget(x, y, width, height);
    }

    protected WrappedFancyPlayerWidget(int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty());
        FancyEntityRendererUtils.requireAvailable();
        this.wrapped = FancyPlayerWidgetBridge.create(x, y, width, height);
    }

    @Override
    protected void extractWidgetRenderState(@Nonnull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        FancyPlayerWidgetBridge current = this.wrapped;
        if (current != null) current.invoke(FancyPlayerWidgetBridge.Operation.EXTRACT_RENDER_STATE, graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void updateWidgetNarration(@Nonnull NarrationElementOutput output) {
        FancyPlayerWidgetBridge current = this.wrapped;
        if (current != null) current.invoke(FancyPlayerWidgetBridge.Operation.UPDATE_NARRATION, output);
    }

    /** Synchronizes the wrapper and dependency widget's horizontal position. */
    @Override
    public void setX(int x) {
        FancyPlayerWidgetBridge current = this.requireWrapped();
        super.setX(x);
        current.invoke(FancyPlayerWidgetBridge.Operation.SET_X, x);
    }

    /** Synchronizes the wrapper and dependency widget's vertical position. */
    @Override
    public void setY(int y) {
        FancyPlayerWidgetBridge current = this.requireWrapped();
        super.setY(y);
        current.invoke(FancyPlayerWidgetBridge.Operation.SET_Y, y);
    }

    /** Synchronizes the wrapper and dependency widget's width. */
    @Override
    public void setWidth(int width) {
        FancyPlayerWidgetBridge current = this.requireWrapped();
        super.setWidth(width);
        current.invoke(FancyPlayerWidgetBridge.Operation.SET_WIDTH, width);
    }

    /** Synchronizes the wrapper and dependency widget's height. */
    @Override
    public void setHeight(int height) {
        FancyPlayerWidgetBridge current = this.requireWrapped();
        super.setHeight(height);
        current.invoke(FancyPlayerWidgetBridge.Operation.SET_HEIGHT, height);
    }

    /** Synchronizes the wrapper and dependency widget's dimensions. */
    @Override
    public void setSize(int width, int height) {
        FancyPlayerWidgetBridge current = this.requireWrapped();
        super.setSize(width, height);
        current.invoke(FancyPlayerWidgetBridge.Operation.SET_SIZE, width, height);
    }

    /** Controls whether the whole model follows the mouse, restoring the previous manual rotation when disabled. */
    public WrappedFancyPlayerWidget setBodyFollowsMouse(boolean followsMouse) {
        this.invoke(FancyPlayerWidgetBridge.Operation.SET_BODY_FOLLOWS_MOUSE, followsMouse);
        return this;
    }

    /** Controls whether the head follows the mouse, restoring the previous manual rotation when disabled. */
    public WrappedFancyPlayerWidget setHeadFollowsMouse(boolean followsMouse) {
        this.invoke(FancyPlayerWidgetBridge.Operation.SET_HEAD_FOLLOWS_MOUSE, followsMouse);
        return this;
    }

    /** Sets the head rotation from radians. */
    public WrappedFancyPlayerWidget setHeadRotation(@Nonnull EntityRotation rotation) {
        FancyPlayerWidgetBridge current = this.requireWrapped();
        current.invoke(FancyPlayerWidgetBridge.Operation.SET_HEAD_ROTATION, current.createRotation(Objects.requireNonNull(rotation, "rotation")));
        return this;
    }

    /** Sets the head rotation from degree values. */
    public WrappedFancyPlayerWidget setHeadRotation(float x, float y, float z) {
        this.invoke(FancyPlayerWidgetBridge.Operation.SET_HEAD_ROTATION_DEGREES, x, y, z);
        return this;
    }

    /** Sets the whole-model rotation from radians. */
    public WrappedFancyPlayerWidget setBodyRotation(@Nonnull EntityRotation rotation) {
        FancyPlayerWidgetBridge current = this.requireWrapped();
        current.invoke(FancyPlayerWidgetBridge.Operation.SET_BODY_ROTATION, current.createRotation(Objects.requireNonNull(rotation, "rotation")));
        return this;
    }

    /** Sets the whole-model rotation from degree values. */
    public WrappedFancyPlayerWidget setBodyRotation(float x, float y, float z) {
        this.invoke(FancyPlayerWidgetBridge.Operation.SET_BODY_ROTATION_DEGREES, x, y, z);
        return this;
    }

    /** Sets the left-arm rotation from radians. */
    public WrappedFancyPlayerWidget setLeftArmRotation(@Nonnull EntityRotation rotation) {
        FancyPlayerWidgetBridge current = this.requireWrapped();
        current.invoke(FancyPlayerWidgetBridge.Operation.SET_LEFT_ARM_ROTATION, current.createRotation(Objects.requireNonNull(rotation, "rotation")));
        return this;
    }

    /** Sets the left-arm rotation from degree values. */
    public WrappedFancyPlayerWidget setLeftArmRotation(float x, float y, float z) {
        this.invoke(FancyPlayerWidgetBridge.Operation.SET_LEFT_ARM_ROTATION_DEGREES, x, y, z);
        return this;
    }

    /** Sets the right-arm rotation from radians. */
    public WrappedFancyPlayerWidget setRightArmRotation(@Nonnull EntityRotation rotation) {
        FancyPlayerWidgetBridge current = this.requireWrapped();
        current.invoke(FancyPlayerWidgetBridge.Operation.SET_RIGHT_ARM_ROTATION, current.createRotation(Objects.requireNonNull(rotation, "rotation")));
        return this;
    }

    /** Sets the right-arm rotation from degree values. */
    public WrappedFancyPlayerWidget setRightArmRotation(float x, float y, float z) {
        this.invoke(FancyPlayerWidgetBridge.Operation.SET_RIGHT_ARM_ROTATION_DEGREES, x, y, z);
        return this;
    }

    /** Sets the left-leg rotation from radians. */
    public WrappedFancyPlayerWidget setLeftLegRotation(@Nonnull EntityRotation rotation) {
        FancyPlayerWidgetBridge current = this.requireWrapped();
        current.invoke(FancyPlayerWidgetBridge.Operation.SET_LEFT_LEG_ROTATION, current.createRotation(Objects.requireNonNull(rotation, "rotation")));
        return this;
    }

    /** Sets the left-leg rotation from degree values. */
    public WrappedFancyPlayerWidget setLeftLegRotation(float x, float y, float z) {
        this.invoke(FancyPlayerWidgetBridge.Operation.SET_LEFT_LEG_ROTATION_DEGREES, x, y, z);
        return this;
    }

    /** Sets the right-leg rotation from radians. */
    public WrappedFancyPlayerWidget setRightLegRotation(@Nonnull EntityRotation rotation) {
        FancyPlayerWidgetBridge current = this.requireWrapped();
        current.invoke(FancyPlayerWidgetBridge.Operation.SET_RIGHT_LEG_ROTATION, current.createRotation(Objects.requireNonNull(rotation, "rotation")));
        return this;
    }

    /** Sets the right-leg rotation from degree values. */
    public WrappedFancyPlayerWidget setRightLegRotation(float x, float y, float z) {
        this.invoke(FancyPlayerWidgetBridge.Operation.SET_RIGHT_LEG_ROTATION_DEGREES, x, y, z);
        return this;
    }

    /** Selects the slim or wide player model, choosing a matching default skin when no custom skin is active. */
    public WrappedFancyPlayerWidget setSlim(boolean slim) {
        this.invoke(FancyPlayerWidgetBridge.Operation.SET_SLIM, slim);
        return this;
    }

    /** Sets a custom skin, or restores the configured default model when {@code null}. */
    public WrappedFancyPlayerWidget setSkin(@Nullable PlayerSkin skin) {
        this.invoke(FancyPlayerWidgetBridge.Operation.SET_SKIN, skin);
        return this;
    }

    /** Copies the local player's profile and skin into the widget. */
    public WrappedFancyPlayerWidget copyLocalPlayer() {
        this.invoke(FancyPlayerWidgetBridge.Operation.COPY_LOCAL_PLAYER);
        return this;
    }

    /** Sets the displayed player name when no copied profile overrides it. */
    public WrappedFancyPlayerWidget setName(@Nonnull String name) {
        this.invoke(FancyPlayerWidgetBridge.Operation.SET_NAME, Objects.requireNonNull(name, "name"));
        return this;
    }

    /** Pins the name tag so it does not rotate with the player model. */
    public WrappedFancyPlayerWidget setPinName(boolean pin) {
        this.invoke(FancyPlayerWidgetBridge.Operation.SET_PIN_NAME, pin);
        return this;
    }

    /** Controls player name-tag visibility. */
    public WrappedFancyPlayerWidget setShowName(boolean showName) {
        this.invoke(FancyPlayerWidgetBridge.Operation.SET_SHOW_NAME, showName);
        return this;
    }

    /** Controls cape visibility. */
    public WrappedFancyPlayerWidget setShowCape(boolean showCape) {
        this.invoke(FancyPlayerWidgetBridge.Operation.SET_SHOW_CAPE, showCape);
        return this;
    }

    /** Controls left-arm visibility. */
    public WrappedFancyPlayerWidget setShowLeftArm(boolean showLeftArm) {
        this.invoke(FancyPlayerWidgetBridge.Operation.SET_SHOW_LEFT_ARM, showLeftArm);
        return this;
    }

    /** Controls left-sleeve visibility. */
    public WrappedFancyPlayerWidget setShowLeftSleeve(boolean showLeftSleeve) {
        this.invoke(FancyPlayerWidgetBridge.Operation.SET_SHOW_LEFT_SLEEVE, showLeftSleeve);
        return this;
    }

    /** Controls right-arm visibility. */
    public WrappedFancyPlayerWidget setShowRightArm(boolean showRightArm) {
        this.invoke(FancyPlayerWidgetBridge.Operation.SET_SHOW_RIGHT_ARM, showRightArm);
        return this;
    }

    /** Controls right-sleeve visibility. */
    public WrappedFancyPlayerWidget setShowRightSleeve(boolean showRightSleeve) {
        this.invoke(FancyPlayerWidgetBridge.Operation.SET_SHOW_RIGHT_SLEEVE, showRightSleeve);
        return this;
    }

    /** Controls left-leg visibility. */
    public WrappedFancyPlayerWidget setShowLeftLeg(boolean showLeftLeg) {
        this.invoke(FancyPlayerWidgetBridge.Operation.SET_SHOW_LEFT_LEG, showLeftLeg);
        return this;
    }

    /** Controls left-pants-layer visibility. */
    public WrappedFancyPlayerWidget setShowLeftPants(boolean showLeftPants) {
        this.invoke(FancyPlayerWidgetBridge.Operation.SET_SHOW_LEFT_PANTS, showLeftPants);
        return this;
    }

    /** Controls right-leg visibility. */
    public WrappedFancyPlayerWidget setShowRightLeg(boolean showRightLeg) {
        this.invoke(FancyPlayerWidgetBridge.Operation.SET_SHOW_RIGHT_LEG, showRightLeg);
        return this;
    }

    /** Controls right-pants-layer visibility. */
    public WrappedFancyPlayerWidget setShowRightPants(boolean showRightPants) {
        this.invoke(FancyPlayerWidgetBridge.Operation.SET_SHOW_RIGHT_PANTS, showRightPants);
        return this;
    }

    /** Controls head visibility. */
    public WrappedFancyPlayerWidget setShowHead(boolean showHead) {
        this.invoke(FancyPlayerWidgetBridge.Operation.SET_SHOW_HEAD, showHead);
        return this;
    }

    /** Controls hat-layer visibility. */
    public WrappedFancyPlayerWidget setShowHat(boolean showHat) {
        this.invoke(FancyPlayerWidgetBridge.Operation.SET_SHOW_HAT, showHat);
        return this;
    }

    /** Controls torso visibility. */
    public WrappedFancyPlayerWidget setShowBody(boolean showBody) {
        this.invoke(FancyPlayerWidgetBridge.Operation.SET_SHOW_BODY, showBody);
        return this;
    }

    /** Controls jacket-layer visibility. */
    public WrappedFancyPlayerWidget setShowJacket(boolean showJacket) {
        this.invoke(FancyPlayerWidgetBridge.Operation.SET_SHOW_JACKET, showJacket);
        return this;
    }

    /** Controls whether the model is rendered upside-down. */
    public WrappedFancyPlayerWidget setUpsideDown(boolean upsideDown) {
        this.invoke(FancyPlayerWidgetBridge.Operation.SET_UPSIDE_DOWN, upsideDown);
        return this;
    }

    /** Selects the player pose; unsupported poses are rejected by Fancy Entity Renderer. */
    public WrappedFancyPlayerWidget setPose(@Nonnull Pose pose) {
        this.invoke(FancyPlayerWidgetBridge.Operation.SET_POSE, Objects.requireNonNull(pose, "pose"));
        return this;
    }

    /** Selects the visibility and translucency mode. */
    public WrappedFancyPlayerWidget setRenderMode(@Nonnull PlayerRenderMode renderMode) {
        FancyPlayerWidgetBridge current = this.requireWrapped();
        current.invoke(FancyPlayerWidgetBridge.Operation.SET_RENDER_MODE, current.resolveRenderMode(Objects.requireNonNull(renderMode, "renderMode")));
        return this;
    }

    /** Sets the experimental outline color; Fancy Entity Renderer 0.5.4 does not currently render the outline. */
    @ApiStatus.Experimental
    public WrappedFancyPlayerWidget setGlowing(int glowColor) {
        this.invoke(FancyPlayerWidgetBridge.Operation.SET_GLOWING, glowColor);
        return this;
    }

    /** Enables or disables idle player movement. */
    @ApiStatus.Experimental
    public WrappedFancyPlayerWidget setMoving(boolean moving) {
        this.invoke(FancyPlayerWidgetBridge.Operation.SET_MOVING, moving);
        return this;
    }

    /** Controls the standard fire animation. */
    public WrappedFancyPlayerWidget setOnFire(boolean onFire) {
        this.invoke(FancyPlayerWidgetBridge.Operation.SET_ON_FIRE, onFire);
        return this;
    }

    /** Controls the fire animation and selects a Prometheus fire type when that integration is installed. */
    public WrappedFancyPlayerWidget setOnFire(boolean onFire, @Nonnull Identifier fireType) {
        this.invoke(FancyPlayerWidgetBridge.Operation.SET_ON_FIRE_TYPE, onFire, Objects.requireNonNull(fireType, "fireType"));
        return this;
    }

    /** Controls the baby player model and its shoulder-parrot visibility. */
    public WrappedFancyPlayerWidget setBaby(boolean baby) {
        this.invoke(FancyPlayerWidgetBridge.Operation.SET_BABY, baby);
        return this;
    }

    /** Sets the left and right shoulder parrots; {@code null} removes the corresponding parrot. */
    public WrappedFancyPlayerWidget setParrots(@Nullable Parrot.Variant leftParrot, @Nullable Parrot.Variant rightParrot) {
        this.invoke(FancyPlayerWidgetBridge.Operation.SET_PARROTS, leftParrot, rightParrot);
        return this;
    }

    /** Alias for {@link #setMoving(boolean)} retained for source compatibility. */
    public WrappedFancyPlayerWidget setBodyMovement(boolean shouldMove) {
        return this.setMoving(shouldMove);
    }

    /** Sets the right-hand item, or empties the hand when {@code null}. */
    public WrappedFancyPlayerWidget setRightHandItem(@Nullable Item item) {
        this.invoke(FancyPlayerWidgetBridge.Operation.SET_RIGHT_HAND_ITEM, item);
        return this;
    }

    /** Sets the right-hand stack, or empties the hand when {@code null}. */
    public WrappedFancyPlayerWidget setRightHandItem(@Nullable ItemStack item) {
        this.invoke(FancyPlayerWidgetBridge.Operation.SET_RIGHT_HAND_ITEM_STACK, item);
        return this;
    }

    /** Sets the left-hand item, or empties the hand when {@code null}. */
    public WrappedFancyPlayerWidget setLeftHandItem(@Nullable Item item) {
        this.invoke(FancyPlayerWidgetBridge.Operation.SET_LEFT_HAND_ITEM, item);
        return this;
    }

    /** Sets the left-hand stack, or empties the hand when {@code null}. */
    public WrappedFancyPlayerWidget setLeftHandItem(@Nullable ItemStack item) {
        this.invoke(FancyPlayerWidgetBridge.Operation.SET_LEFT_HAND_ITEM_STACK, item);
        return this;
    }

    /** Parses and equips a command-format item on the head; {@code null} removes it. */
    public WrappedFancyPlayerWidget setHeadWearable(@Nullable String item, @Nonnull HolderLookup.Provider provider) {
        this.invoke(FancyPlayerWidgetBridge.Operation.SET_HEAD_WEARABLE_STRING, item, Objects.requireNonNull(provider, "provider"));
        return this;
    }

    /** Parses and equips a command-format item on the chest; {@code null} removes it. */
    public WrappedFancyPlayerWidget setChestWearable(@Nullable String item, @Nonnull HolderLookup.Provider provider) {
        this.invoke(FancyPlayerWidgetBridge.Operation.SET_CHEST_WEARABLE_STRING, item, Objects.requireNonNull(provider, "provider"));
        return this;
    }

    /** Parses and equips a command-format item on the legs; {@code null} removes it. */
    public WrappedFancyPlayerWidget setLegsWearable(@Nullable String item, @Nonnull HolderLookup.Provider provider) {
        this.invoke(FancyPlayerWidgetBridge.Operation.SET_LEGS_WEARABLE_STRING, item, Objects.requireNonNull(provider, "provider"));
        return this;
    }

    /** Parses and equips a command-format item on the feet; {@code null} removes it. */
    public WrappedFancyPlayerWidget setFeetWearable(@Nullable String item, @Nonnull HolderLookup.Provider provider) {
        this.invoke(FancyPlayerWidgetBridge.Operation.SET_FEET_WEARABLE_STRING, item, Objects.requireNonNull(provider, "provider"));
        return this;
    }

    /** Equips an item on the head, or removes it when {@code null}. */
    public WrappedFancyPlayerWidget setHeadWearable(@Nullable Item item) {
        this.invoke(FancyPlayerWidgetBridge.Operation.SET_HEAD_WEARABLE, item);
        return this;
    }

    /** Equips an item on the chest, or removes it when {@code null}. */
    public WrappedFancyPlayerWidget setChestWearable(@Nullable Item item) {
        this.invoke(FancyPlayerWidgetBridge.Operation.SET_CHEST_WEARABLE, item);
        return this;
    }

    /** Equips an item on the legs, or removes it when {@code null}. */
    public WrappedFancyPlayerWidget setLegsWearable(@Nullable Item item) {
        this.invoke(FancyPlayerWidgetBridge.Operation.SET_LEGS_WEARABLE, item);
        return this;
    }

    /** Equips an item on the feet, or removes it when {@code null}. */
    public WrappedFancyPlayerWidget setFeetWearable(@Nullable Item item) {
        this.invoke(FancyPlayerWidgetBridge.Operation.SET_FEET_WEARABLE, item);
        return this;
    }

    /** Equips a stack on the head, or removes it when {@code null}. */
    public WrappedFancyPlayerWidget setHeadWearable(@Nullable ItemStack item) {
        this.invoke(FancyPlayerWidgetBridge.Operation.SET_HEAD_WEARABLE_STACK, item);
        return this;
    }

    /** Equips a stack on the chest, or removes it when {@code null}. */
    public WrappedFancyPlayerWidget setChestWearable(@Nullable ItemStack item) {
        this.invoke(FancyPlayerWidgetBridge.Operation.SET_CHEST_WEARABLE_STACK, item);
        return this;
    }

    /** Equips a stack on the legs, or removes it when {@code null}. */
    public WrappedFancyPlayerWidget setLegsWearable(@Nullable ItemStack item) {
        this.invoke(FancyPlayerWidgetBridge.Operation.SET_LEGS_WEARABLE_STACK, item);
        return this;
    }

    /** Equips a stack on the feet, or removes it when {@code null}. */
    public WrappedFancyPlayerWidget setFeetWearable(@Nullable ItemStack item) {
        this.invoke(FancyPlayerWidgetBridge.Operation.SET_FEET_WEARABLE_STACK, item);
        return this;
    }

    /** Starts copying a player resolved by profile name. */
    public WrappedFancyPlayerWidget copyPlayer(@Nonnull String profileName) {
        this.invoke(FancyPlayerWidgetBridge.Operation.COPY_PLAYER_NAME, Objects.requireNonNull(profileName, "profileName"));
        return this;
    }

    /** Starts copying a player resolved by profile UUID. */
    public WrappedFancyPlayerWidget copyPlayer(@Nonnull UUID profileId) {
        this.invoke(FancyPlayerWidgetBridge.Operation.COPY_PLAYER_ID, Objects.requireNonNull(profileId, "profileId"));
        return this;
    }

    /** Stops copying a profile and restores the manually configured name and skin. */
    public WrappedFancyPlayerWidget uncopyPlayer() {
        this.invoke(FancyPlayerWidgetBridge.Operation.UNCOPY_PLAYER);
        return this;
    }

    /** Returns whether Fancy Entity Renderer currently considers the widget to be copying a player. */
    public boolean isCopyingPlayer() {
        FancyPlayerWidgetBridge current = this.wrapped;
        return current != null && current.invokeBoolean(FancyPlayerWidgetBridge.Operation.IS_COPYING_PLAYER);
    }

    /** Returns whether this wrapper has released its optional dependency delegate. */
    public boolean isClosed() {
        return this.wrapped == null;
    }

    /**
     * Releases the dependency-owned widget reference. Rendering and narration become no-ops; later mutator calls fail.
     * Fancy Entity Renderer 0.5.4 exposes no cancellation or close hook for an in-flight profile lookup.
     */
    @Override
    public void close() {
        this.wrapped = null;
    }

    /** Player display widgets never receive keyboard focus. */
    @Override
    public boolean isFocusable() {
        return false;
    }

    /** Player display widgets ignore attempts to enable keyboard focus. */
    @Override
    public void setFocusable(boolean focusable) {
    }

    /** Player display widgets are never directional-navigation targets. */
    @Override
    public boolean isNavigatable() {
        return false;
    }

    /** Player display widgets ignore attempts to enable directional navigation. */
    @Override
    public void setNavigatable(boolean navigatable) {
    }

    private void invoke(FancyPlayerWidgetBridge.Operation operation, Object... arguments) {
        this.requireWrapped().invoke(operation, arguments);
    }

    private FancyPlayerWidgetBridge requireWrapped() {
        FancyPlayerWidgetBridge current = this.wrapped;
        if (current == null) throw new IllegalStateException("Wrapped Fancy Entity Renderer player widget is closed");
        return current;
    }

}
