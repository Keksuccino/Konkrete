package de.keksuccino.konkrete.rendering;

import java.awt.Color;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("all")
public class RenderUtils {

    /**
     * Returns the converted color or NULL if the color could not be converted.
     */
    public static Color getColorFromHexString(@NotNull String hex) {
		try {
			hex = hex.replace("#", "");
			if (hex.length() == 6) {
				return new Color(
						Integer.valueOf(hex.substring(0, 2), 16),
						Integer.valueOf(hex.substring(2, 4), 16),
						Integer.valueOf(hex.substring(4, 6), 16));
			}
			if (hex.length() == 8) {
				return new Color(
						Integer.valueOf(hex.substring(0, 2), 16),
						Integer.valueOf(hex.substring(2, 4), 16),
						Integer.valueOf(hex.substring(4, 6), 16),
						Integer.valueOf(hex.substring(6, 8), 16));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@NotNull
	public static ResourceLocation register(@NotNull String location, @NotNull AbstractTexture texture) {
		Objects.requireNonNull(location);
		Objects.requireNonNull(texture);
		ResourceLocation loc = location.contains(":") ? ResourceLocation.parse(location) : ResourceLocation.fromNamespaceAndPath("konkrete", location);
		Minecraft.getInstance().getTextureManager().register(loc, texture);
		return loc;
	}

}
