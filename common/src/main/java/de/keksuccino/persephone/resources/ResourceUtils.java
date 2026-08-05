package de.keksuccino.persephone.resources;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;

public class ResourceUtils {

    @NotNull
    public static Identifier registerTexture(@NotNull String texLocation, @NotNull AbstractTexture texture) {
        Objects.requireNonNull(texLocation);
        Objects.requireNonNull(texture);
        Identifier loc = texLocation.contains(":") ? Identifier.parse(texLocation) : Identifier.fromNamespaceAndPath("persephone", texLocation);
        Minecraft.getInstance().getTextureManager().register(loc, texture);
        return loc;
    }

}
