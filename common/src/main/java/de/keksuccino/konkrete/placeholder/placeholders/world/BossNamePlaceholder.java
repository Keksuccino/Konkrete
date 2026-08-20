package de.keksuccino.konkrete.placeholder.placeholders.world;

import de.keksuccino.konkrete.mixin.mixins.common.client.AccessorMixinBossHealthOverlay;
import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import de.keksuccino.konkrete.util.SerializationHelper;
import de.keksuccino.konkrete.util.rendering.text.ComponentParser;
import de.keksuccino.konkrete.util.rendering.text.TextFormattingUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.LerpingBossEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Exposes a boss-bar name by stable vanilla overlay order, as text or component JSON. */
public class BossNamePlaceholder extends AbstractWorldPlaceholder {

    /** Creates the {@code boss_name} placeholder. */
    public BossNamePlaceholder() {
        super("boss_name");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        if (this.getPlayer() == null || this.getLevel() == null) return "";
        int index = SerializationHelper.INSTANCE.deserializeNumber(Integer.class, 0, dps.values.get("boss_index"));
        if (index < 0) return "";
        boolean asJson = SerializationHelper.INSTANCE.deserializeBoolean(false, dps.values.get("as_json"));
        AccessorMixinBossHealthOverlay overlay = (AccessorMixinBossHealthOverlay) Minecraft.getInstance().gui.hud.getBossOverlay();
        LerpingBossEvent event = overlay.get_events_Konkrete().values().stream().skip(index).findFirst().orElse(null);
        if (event == null) return "";
        return asJson ? ComponentParser.toJson(event.getName(), this.getLevel().registryAccess()) : TextFormattingUtils.convertComponentToString(event.getName());
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return List.of("boss_index", "as_json");
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "konkrete.placeholders.world.boss_name";
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("boss_index", "0");
        values.put("as_json", "false");
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }

}
