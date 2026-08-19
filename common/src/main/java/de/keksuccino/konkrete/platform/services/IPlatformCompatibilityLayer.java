package de.keksuccino.konkrete.platform.services;

import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Exposes loader-owned client behavior that common code cannot access uniformly.
 */
public interface IPlatformCompatibilityLayer {

    /**
     * Returns the left-aligned title-screen branding block in visual top-to-bottom order.
     * Right-aligned status lines above the copyright notice are not part of this block.
     *
     * @return branding lines in visual order, or an immutable empty list when unsupported
     */
    default List<Component> getTitleScreenBrandingLines() {
        return List.of();
    }

}
