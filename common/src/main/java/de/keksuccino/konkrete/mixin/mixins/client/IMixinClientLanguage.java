package de.keksuccino.konkrete.mixin.mixins.client;

import net.minecraft.client.resources.language.ClientLanguage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.Map;

@Mixin(ClientLanguage.class)
public interface IMixinClientLanguage {

    @Accessor("storage") public Map<String, String> getStorageKonkrete();

}
