package de.keksuccino.konkrete.placeholder.placeholders.world;

import de.keksuccino.konkrete.mixin.mixins.common.client.AccessorMixinHud;
import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import de.keksuccino.konkrete.util.SerializationHelper;
import de.keksuccino.konkrete.util.rendering.text.ComponentParser;
import de.keksuccino.konkrete.util.rendering.text.TextFormattingUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Exposes the current vanilla title or subtitle, as formatted text or component JSON. */
public class CurrentTitlePlaceholder extends AbstractWorldPlaceholder {

    /** Creates the {@code current_title} placeholder. */
    public CurrentTitlePlaceholder() {
        super("current_title");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        if (this.getPlayer() == null || this.getLevel() == null) return "";
        boolean subtitle = SerializationHelper.INSTANCE.deserializeBoolean(false, dps.values.get("is_subtitle"));
        boolean asJson = SerializationHelper.INSTANCE.deserializeBoolean(false, dps.values.get("as_json"));
        AccessorMixinHud hud = (AccessorMixinHud) Minecraft.getInstance().gui.hud;
        Component component = subtitle ? hud.get_subtitle_Konkrete() : hud.get_title_Konkrete();
        if (component == null) return "";
        return asJson ? ComponentParser.toJson(component, this.getLevel().registryAccess()) : TextFormattingUtils.convertComponentToString(component);
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return List.of("is_subtitle", "as_json");
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "konkrete.placeholders.world.current_title";
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("is_subtitle", "false");
        values.put("as_json", "false");
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }
}
