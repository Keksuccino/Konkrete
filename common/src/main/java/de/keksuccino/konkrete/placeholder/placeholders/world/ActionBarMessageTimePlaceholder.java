package de.keksuccino.konkrete.placeholder.placeholders.world;

import de.keksuccino.konkrete.mixin.mixins.common.client.AccessorMixinHud;
import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;

/** Exposes the remaining vanilla action-bar display time in ticks. */
public class ActionBarMessageTimePlaceholder extends AbstractWorldPlaceholder {

    /** Creates the {@code action_bar_message_time} placeholder. */
    public ActionBarMessageTimePlaceholder() {
        super("action_bar_message_time", "action_bar_message_time_fm");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        return String.valueOf(((AccessorMixinHud) Minecraft.getInstance().gui.hud).get_overlayMessageTime_Konkrete());
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "konkrete.placeholders.world.action_bar_message_time";
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        return new DeserializedPlaceholderString(this.getIdentifier(), null, "");
    }
}
