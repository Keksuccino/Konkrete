package de.keksuccino.konkrete.mixin.mixins.common.client;

import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import java.util.UUID;

@Mixin(BossHealthOverlay.class)
public interface AccessorMixinBossHealthOverlay {

    @Accessor("events") Map<UUID, LerpingBossEvent> get_events_Konkrete();

}
