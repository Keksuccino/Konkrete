package de.keksuccino.konkrete.platform;

import de.keksuccino.konkrete.platform.services.IPlatformCompatibilityLayer;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Fabric implementation of loader-specific client compatibility behavior.
 */
public class FabricCompatibilityLayer implements IPlatformCompatibilityLayer {

    /** {@inheritDoc} */
    @Override
    public List<Component> getTitleScreenBrandingLines() {
        String branding = "Minecraft " + SharedConstants.getCurrentVersion().name();
        if (Minecraft.getInstance().isDemo()) {
            branding = branding + " Demo";
        }
        if (Minecraft.checkModStatus().shouldReportAsModified()) {
            branding = branding + I18n.get("menu.modded");
        }
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(branding));
        return lines;
    }

}
