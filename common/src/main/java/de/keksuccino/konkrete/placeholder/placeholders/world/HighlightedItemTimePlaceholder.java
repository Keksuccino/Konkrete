package de.keksuccino.konkrete.placeholder.placeholders.world;

import de.keksuccino.konkrete.mixin.mixins.common.client.AccessorMixinHud;
import de.keksuccino.konkrete.mixin.mixins.common.client.AccessorMixinSpectatorGui;
import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;

/** Exposes the remaining vanilla selected-item highlight time in ticks. */
public class HighlightedItemTimePlaceholder extends AbstractWorldPlaceholder {

    /** Creates the {@code highlighted_item_time} placeholder. */
    public HighlightedItemTimePlaceholder() {
        super("highlighted_item_time", "highlighted_item_time_fm");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        Minecraft minecraft = Minecraft.getInstance();
        int time = ((AccessorMixinHud) minecraft.gui.hud).get_toolHighlightTimer_Konkrete();
        if (minecraft.player != null && minecraft.player.isSpectator()) {
            AccessorMixinSpectatorGui spectatorGui = (AccessorMixinSpectatorGui) minecraft.gui.hud.getSpectatorGui();
            time = spectatorGui.invoke_getHotbarAlpha_Konkrete() > 0.0F ? (int) (40.0D * minecraft.options.notificationDisplayTime().get()) : 0;
        }
        return String.valueOf(time);
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "konkrete.placeholders.world.highlighted_item_time";
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        return new DeserializedPlaceholderString(this.getIdentifier(), null, "");
    }
}
