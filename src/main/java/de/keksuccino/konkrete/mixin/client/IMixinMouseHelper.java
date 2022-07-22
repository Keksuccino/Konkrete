package de.keksuccino.konkrete.mixin.client;

import net.minecraft.client.MouseHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MouseHelper.class)
public interface IMixinMouseHelper {

    @Accessor("activeButton") public int getActiveButtonKonkrete();

    @Accessor("activeButton") public void setActiveButtonKonkrete(int i);

}
