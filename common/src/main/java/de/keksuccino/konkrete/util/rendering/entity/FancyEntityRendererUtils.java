package de.keksuccino.konkrete.util.rendering.entity;

import de.keksuccino.konkrete.platform.Services;

import javax.annotation.Nullable;

/** Provides safe availability checks for the optional Fancy Entity Renderer integration. */
public final class FancyEntityRendererUtils {

    /** Fancy Entity Renderer's loader identifier. */
    public static final String MOD_ID = "fancy_entity_renderer";

    private FancyEntityRendererUtils() {
    }

    /** Returns whether the loader reports Fancy Entity Renderer as installed. */
    public static boolean isFerLoaded() {
        return Services.PLATFORM.isModLoaded(MOD_ID);
    }

    /**
     * Returns whether the client-side Fancy Entity Renderer widget API is installed and binary-compatible.
     */
    public static boolean isAvailable() {
        return Services.PLATFORM.isOnClient() && isFerLoaded() && FancyPlayerWidgetBridge.isAvailable();
    }

    /**
     * Describes why the integration is unavailable, or returns {@code null} when it can be used.
     */
    @Nullable
    public static String getUnavailableReason() {
        if (!Services.PLATFORM.isOnClient()) return "Fancy Entity Renderer player widgets are client-only";
        if (!isFerLoaded()) return "Fancy Entity Renderer is not loaded";
        return FancyPlayerWidgetBridge.getUnavailableReason();
    }

    static void requireAvailable() {
        String reason = getUnavailableReason();
        if (reason != null) throw new IllegalStateException(reason);
    }

}
