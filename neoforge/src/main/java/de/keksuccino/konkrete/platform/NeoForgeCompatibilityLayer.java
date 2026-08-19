package de.keksuccino.konkrete.platform;

import de.keksuccino.konkrete.platform.services.IPlatformCompatibilityLayer;
import de.keksuccino.konkrete.platform.services.TitleScreenBrandingLineCollector;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.internal.BrandingControl;

import java.util.List;

/**
 * NeoForge implementation of loader-specific client compatibility behavior.
 */
public class NeoForgeCompatibilityLayer implements IPlatformCompatibilityLayer {

    /** {@inheritDoc} */
    @Override
    public List<Component> getTitleScreenBrandingLines() {
        return TitleScreenBrandingLineCollector.collectTopToBottom(BrandingControl::forEachLine, Component::literal);
    }

}
