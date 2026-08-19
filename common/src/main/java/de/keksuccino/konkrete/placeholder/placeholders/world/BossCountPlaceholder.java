package de.keksuccino.konkrete.placeholder.placeholders.world;

import de.keksuccino.konkrete.mixin.mixins.common.client.AccessorMixinBossHealthOverlay;
import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;

/** Exposes the number of boss bars currently tracked by the vanilla HUD. */
public class BossCountPlaceholder extends AbstractWorldPlaceholder {

    /** Creates the {@code boss_count} placeholder. */
    public BossCountPlaceholder() {
        super("boss_count");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        if (this.getPlayer() == null || this.getLevel() == null) return "0";
        AccessorMixinBossHealthOverlay overlay = (AccessorMixinBossHealthOverlay) Minecraft.getInstance().gui.hud.getBossOverlay();
        return Integer.toString(overlay.get_events_Konkrete().size());
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "konkrete.placeholders.world.boss_count";
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        return new DeserializedPlaceholderString(this.getIdentifier(), null, "");
    }
}
