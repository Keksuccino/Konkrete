package de.keksuccino.konkrete.placeholder.placeholders.world;

import de.keksuccino.konkrete.mixin.mixins.common.client.AccessorMixinBossHealthOverlay;
import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import de.keksuccino.konkrete.util.SerializationHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.LerpingBossEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Exposes a boss bar's current progress as an integer percentage. */
public class CurrentBossHealthPlaceholder extends AbstractWorldPlaceholder {

    /** Creates the {@code current_boss_health} placeholder. */
    public CurrentBossHealthPlaceholder() {
        super("current_boss_health");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        if (this.getPlayer() == null || this.getLevel() == null) return "0";
        int index = SerializationHelper.INSTANCE.deserializeNumber(Integer.class, -1, dps.values.get("boss_index"));
        if (index < 0) return "0";
        AccessorMixinBossHealthOverlay overlay = (AccessorMixinBossHealthOverlay) Minecraft.getInstance().gui.hud.getBossOverlay();
        LerpingBossEvent event = overlay.get_events_Konkrete().values().stream().skip(index).findFirst().orElse(null);
        return event != null ? Integer.toString((int) (event.getProgress() * 100.0F)) : "0";
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return List.of("boss_index");
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "konkrete.placeholders.world.current_boss_health";
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("boss_index", "0");
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }

}
