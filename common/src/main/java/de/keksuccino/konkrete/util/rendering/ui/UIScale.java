package de.keksuccino.konkrete.util.rendering.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Enumerates logical scale presets used by Konkrete UI components. */
public enum UIScale {

    /** Uses the auto UI scale preset. */
    AUTO("auto", 0.0F),
    /** Uses the micro UI scale preset. */
    MICRO("micro", 1.0F),
    /** Uses the extra small UI scale preset. */
    EXTRA_SMALL("extra_small", 1.25F),
    /** Uses the small UI scale preset. */
    SMALL("small", 1.5F),
    /** Uses the medium UI scale preset. */
    MEDIUM("medium", 2.0F),
    /** Uses the medium large UI scale preset. */
    MEDIUM_LARGE("medium_large", 2.25F),
    /** Uses the large UI scale preset. */
    LARGE("large", 2.5F),
    /** Uses the extra large UI scale preset. */
    EXTRA_LARGE("extra_large", 3.0F),
    /** Uses the huge UI scale preset. */
    HUGE("huge", 3.25F),
    /** Uses the gigantic UI scale preset. */
    GIGANTIC("gigantic", 3.5F),
    /** Uses the colossal UI scale preset. */
    COLOSSAL("colossal", 4.0F),
    /** Uses the titanic UI scale preset. */
    TITANIC("titanic", 4.5F),
    /** Uses the massive UI scale preset. */
    MASSIVE("massive", 5.0F),
    /** Uses the immense UI scale preset. */
    IMMENSE("immense", 5.5F),
    /** Uses the maximum UI scale preset. */
    MAXIMUM("maximum", 6.0F);

    final String name;
    final float scale;

    UIScale(String name, float scale) {
        this.name = name;
        this.scale = scale;
    }

    /** Returns the stable identifier used by configuration and localization keys. */
    public String getName() {
        return name;
    }

    /** Returns the logical multiplier, or zero for {@link #AUTO}. */
    public float getScale() {
        return scale;
    }

    /** Builds the localized preset label, including the numeric scale where applicable. */
    @NotNull
    public Component getDisplayName() {
        if (this == AUTO) return Component.translatable("konkrete.ui.scales." + this.getName());
        return Component.translatable("konkrete.ui.scales." + this.getName(), this.getScale());
    }

    /** Finds a preset by its stable configuration identifier. */
    @Nullable
    public static UIScale getByName(String name) {
        if (name == null) {
            return null;
        }
        for (UIScale scale : UIScale.values()) {
            if (scale.name.equals(name)) {
                return scale;
            }
        }
        return null;
    }

    /** Returns the preset selected by the active {@link UIConfiguration}. */
    @NotNull
    public static UIScale getUIScale() {
        return UIConfiguration.get().uiScale();
    }

    /**
     * Returns the logical UI scale used for Konkrete's UI elements, after applying
     * automatic adjustments (2K/4K auto-scale and Unicode font enforcement).
     */
    public static float getUIScaleFloat() {
        UIScale scale = getUIScale();
        float uiScale = scale.getScale();
        //Handle "Auto" scale (use SMALL for 2K-ish windows, MEDIUM for 4K-ish windows)
        if (scale == AUTO) {
            uiScale = EXTRA_SMALL.getScale();
            int windowWidth = Minecraft.getInstance().getWindow().getWidth();
            int windowHeight = Minecraft.getInstance().getWindow().getHeight();
            if ((windowWidth >= 2400) || (windowHeight >= 1300)) {
                uiScale = SMALL.getScale();
            }
            if ((windowWidth > 3000) || (windowHeight > 1700)) {
                uiScale = MEDIUM.getScale();
            }
        }
        //Force a scale of 2 or bigger if Unicode font is enabled
        if (UIBase.shouldUseMinecraftFontForUIRendering() && Minecraft.getInstance().isEnforceUnicode() && (uiScale < 2F)) {
            uiScale = MEDIUM.getScale();
        }
        return uiScale;
    }

}
